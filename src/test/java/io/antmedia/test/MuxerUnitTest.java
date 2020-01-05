package io.antmedia.test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.integration.AppFunctionalTest;
import io.antmedia.integration.MuxingTest;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.MuxAdaptor.InputContext;
import io.antmedia.muxer.Muxer;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.av_dict_get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.tika.io.IOUtils;
import org.awaitility.Awaitility;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.*;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVDictionaryEntry;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.red5.codec.StreamCodecInfo;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVReader;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.WebScope;
import org.red5.server.service.mp4.impl.MP4Service;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.av_dict_get;
import static org.junit.Assert.*;


@ContextConfiguration(locations = {"test.xml"})
//@ContextConfiguration(classes = {AppConfig.class})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class MuxerUnitTest extends AbstractJUnit4SpringContextTests {

	protected static Logger logger = LoggerFactory.getLogger(MuxerUnitTest.class);
	protected static final int BUFFER_SIZE = 10240;

	protected WebScope appScope;
	private AppSettings appSettings;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}


	@BeforeClass
	public static void beforeClass() {
		avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_INFO);
	}

	@Before
	public void before() {
		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}


		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}

	@After
	public void after() {


		try {
			AppFunctionalTest.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}


		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		}

		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		}

	};

	public class StreamPacket implements IStreamPacket {

		private ITag readTag;
		private IoBuffer data;

		public StreamPacket(ITag tag) {
			readTag = tag;
			data = readTag.getBody();
		}

		@Override
		public int getTimestamp() {
			return readTag.getTimestamp();
		}

		@Override
		public byte getDataType() {
			return readTag.getDataType();
		}

		@Override
		public IoBuffer getData() {
			return data;
		}

		public void setData(IoBuffer data) {
			this.data = data;
		}
	}


	@Test
	public void testMuxAdaptorEnableSettingsPreviewCreatePeriod() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, false, appScope);
		int createPreviewPeriod = (int) (Math.random() * 10000);
		assertNotEquals(0, createPreviewPeriod);
		getAppSettings().setCreatePreviewPeriod(createPreviewPeriod);

		boolean result = muxAdaptor.init(appScope, "test", false);
		assertTrue(result);

		assertEquals(createPreviewPeriod, muxAdaptor.getPreviewCreatePeriod());
	}

	@Test
	public void testMuxingSimultaneously() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(false);
		
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(true);
		info.setHasVideo(true);
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);

		//this value should be -1. It means it is uninitialized
		assertEquals(-1, muxAdaptor.getFirstPacketTime());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			logger.info("name    " + String.valueOf(scheduler.getJobName().toCharArray()));

			assertEquals(0, scheduler.getScheduledJobNames().size());

			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "test", false);
			assertTrue(result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}

			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();


			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			assertFalse(muxAdaptor.isRecording());

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				File f1 = new File(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath());
				File f2 = new File(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath());
				return f1.exists() && f2.exists();
			});


			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath()));
			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath()));

		} catch (Exception e) {
			fail("excsereption:" + e);
		}

	}


	@Test
	public void testStressMp4Muxing() {

		long startTime = System.nanoTime();
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		try {
			
			ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
			StreamCodecInfo info = new StreamCodecInfo();
			info.setHasAudio(true);
			info.setHasVideo(true);
			clientBroadcastStream.setCodecInfo(info);

			getAppSettings().setHlsMuxingEnabled(false);
			getAppSettings().setMp4MuxingEnabled(true);
			getAppSettings().setAddDateTimeToMp4FileName(false);

			List<MuxAdaptor> muxAdaptorList = new ArrayList<>();
			for (int j = 0; j < 5; j++) {
				MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);
				muxAdaptorList.add(muxAdaptor);
			}
			{

				File file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
				final FLVReader flvReader = new FLVReader(file);

				logger.debug("f path: " + file.getAbsolutePath());
				assertTrue(file.exists());

				for (Iterator<MuxAdaptor> iterator = muxAdaptorList.iterator(); iterator.hasNext(); ) {
					MuxAdaptor muxAdaptor = (MuxAdaptor) iterator.next();
					String streamId = "test" + (int) (Math.random() * 991000);
					boolean result = muxAdaptor.init(appScope, streamId, false);
					assertTrue(result);
					muxAdaptor.start();
					logger.info("Mux adaptor instance initialized for {}", streamId);
				}


				while (flvReader.hasMoreTags()) {
					ITag readTag = flvReader.readTag();
					StreamPacket streamPacket = new StreamPacket(readTag);
					for (MuxAdaptor muxAdaptor : muxAdaptorList) {
						muxAdaptor.packetReceived(null, streamPacket);
						streamPacket.getData().rewind();
					}

				}

				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					logger.info("Check if is recording: {}", muxAdaptor.getStreamId());
					Awaitility.await().atMost(50, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
						return muxAdaptor.isRecording();
					});
				}

				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					muxAdaptor.stop();
				}


				flvReader.close();

				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					Awaitility.await().atMost(50, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
						return !muxAdaptor.isRecording();
					});
				}


			}

			int count = 0;
			for (MuxAdaptor muxAdaptor : muxAdaptorList) {
				List<Muxer> muxerList = muxAdaptor.getMuxerList();
				for (Muxer abstractMuxer : muxerList) {
					if (abstractMuxer instanceof Mp4Muxer) {
						assertTrue(MuxingTest.testFile(abstractMuxer.getFile().getAbsolutePath(), 697000));
						count++;
					}
				}
			}

			assertEquals(muxAdaptorList.size(), count);

			long diff = (System.nanoTime() - startTime) / 1000000;

			System.out.println(" time diff: " + diff + " ms");

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);

			assertEquals(0, scheduler.getScheduledJobNames().size());


		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setHlsMuxingEnabled(true);

	}

	@Test
	public void testMp4MuxingWithWithMultipleDepth() {
		File file = testMp4Muxing("test_test/test");
		assertEquals("test.mp4", file.getName());

		file = testMp4Muxing("dir1/dir2/file");
		assertTrue(file.exists());

		file = testMp4Muxing("dir1/dir2/dir3/file");
		assertTrue(file.exists());

		file = testMp4Muxing("dir1/dir2/dir3/dir4/file");
		assertTrue(file.exists());
	}

	@Test
	public void testMp4MuxingWithSameName() {
		logger.info("running testMp4MuxingWithSameName");

		Application.resetFields();

		assertEquals(null, Application.id);
		assertEquals(null, Application.file);
		assertEquals(0, Application.duration);

		File file = testMp4Muxing("test_test");
		assertEquals("test_test.mp4", file.getName());

		assertEquals("test_test", Application.id);
		assertEquals("test_test.mp4", Application.file.getName());
		assertNotEquals(0L, Application.duration);

		Application.resetFields();

		assertEquals(null, Application.id);
		assertEquals(null, Application.file);
		assertEquals(0, Application.duration);

		file = testMp4Muxing("test_test");
		assertEquals("test_test_1.mp4", file.getName());

		assertEquals("test_test", Application.id);
		assertEquals("test_test_1.mp4", Application.file.getName());
		assertNotEquals(0L, Application.duration);

		Application.resetFields();

		assertEquals(null, Application.id);
		assertEquals(null, Application.file);
		assertEquals(0, Application.duration);

		file = testMp4Muxing("test_test");
		assertEquals("test_test_2.mp4", file.getName());

		assertEquals("test_test", Application.id);
		assertEquals("test_test_2.mp4", Application.file.getName());
		assertNotEquals(0L, Application.duration);

		logger.info("leaving testMp4MuxingWithSameName");
	}


	@Test
	public void testBaseStreamFileServiceBug() {
		MP4Service mp4Service = new MP4Service();

		String fileName = mp4Service.prepareFilename("mp4:1");
		assertEquals("1.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:12");
		assertEquals("12.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:123");
		assertEquals("123.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:1234");
		assertEquals("1234.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:12345");
		assertEquals("12345.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:123456");
		assertEquals("123456.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:1.mp4");
		assertEquals("1.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:123456789.mp4");
		assertEquals("123456789.mp4", fileName);

	}

	@Test
	public void testPublishAndUnpublishSocialEndpoints() {
		AntMediaApplicationAdapter appAdaptor = ((IApplicationAdaptorFactory) applicationContext.getBean("web.handler")).getAppAdaptor();
		assertNotNull(appAdaptor);



		Broadcast broadcast = new Broadcast();
		appAdaptor.getDataStore().save(broadcast);
		IBroadcastStream stream = Mockito.mock(IBroadcastStream.class);
		Mockito.when(stream.getPublishedName()).thenReturn(broadcast.getStreamId());

		VideoServiceEndpoint endpointService = Mockito.mock(VideoServiceEndpoint.class);
		String endpointServiceId = "" + (Math.random()*10000);
		appAdaptor.getVideoServiceEndpoints().put(endpointServiceId, endpointService);	

		Endpoint endpoint = new Endpoint(null, broadcast.getStreamId(), "name", "rtmp url", null, endpointServiceId, null);

		appAdaptor.getDataStore().addEndpoint(broadcast.getStreamId(), endpoint);

		appAdaptor.streamPublishStart(stream);

		Awaitility.await()
		.atMost(5, TimeUnit.SECONDS)
		.pollInterval(1, TimeUnit.SECONDS)
		.until(() -> 
		appAdaptor.getDataStore().get(broadcast.getStreamId())
		.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));


		try {
			Mockito.verify(endpointService).publishBroadcast(endpoint);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		//this zombi trick will let us have a proper await method
		broadcast.setZombi(true);
		appAdaptor.streamBroadcastClose(stream);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> appAdaptor.getDataStore().get(broadcast.getStreamId()) == null);

		try {
			Mockito.verify(endpointService).stopBroadcast(endpoint);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testVideoServiceEndpoint() {
		AntMediaApplicationAdapter appAdaptor = ((IApplicationAdaptorFactory) applicationContext.getBean("web.handler")).getAppAdaptor();
		assertNotNull(appAdaptor);

		VideoServiceEndpoint endpointService = Mockito.mock(VideoServiceEndpoint.class);
		SocialEndpointCredentials credentials = Mockito.mock(SocialEndpointCredentials.class);
		String id = "" + (Math.random() * 10000);
		Mockito.when(credentials.getId()).thenReturn(id);

		appAdaptor.getVideoServiceEndpoints().put(id, endpointService);

		Mockito.when(endpointService.getCredentials()).thenReturn(credentials);

		String id2 = "" + (Math.random() * 10000);
		VideoServiceEndpoint endpointService2 = Mockito.mock(VideoServiceEndpoint.class);
		SocialEndpointCredentials credentials2 = Mockito.mock(SocialEndpointCredentials.class);
		Mockito.when(credentials2.getId()).thenReturn(id2);
		Mockito.when(endpointService2.getCredentials()).thenReturn(credentials2);

		appAdaptor.getVideoServiceEndpoints().put(id2, endpointService2);

		VideoServiceEndpoint videoServiceEndPoint = appAdaptor.getVideoServiceEndPoint(id);
		assertNotNull(videoServiceEndPoint);
		assertEquals(endpointService, videoServiceEndPoint);

		videoServiceEndPoint = appAdaptor.getVideoServiceEndPoint(id2);
		assertNotNull(videoServiceEndPoint);
		assertEquals(endpointService2, videoServiceEndPoint);


	}

	@Test
	public void testMp4MuxingAndNotifyCallback() {

		Application app =  (Application) applicationContext.getBean("web.handler");
		AntMediaApplicationAdapter appAdaptorReal = app.getAppAdaptor();
		AntMediaApplicationAdapter appAdaptor = Mockito.spy(appAdaptorReal);
		app.setAdaptor(appAdaptor);
		doReturn(new StringBuilder("")).when(appAdaptor).notifyHook(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
		assertNotNull(appAdaptor);

		//just check below value that it is not null, this is not related to this case but it should be tested
		assertNotNull(appAdaptor.getVideoServiceEndpoints());
		String hookUrl = "http://hook_url";
		String name = "namer123";
		Broadcast broadcast = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, name);
		broadcast.setListenerHookURL(hookUrl);
		String streamId = appAdaptor.getDataStore().save(broadcast);

		testMp4Muxing(streamId, false, true);

		assertEquals(Application.id, streamId);
		assertEquals(Application.file.getName(), streamId + ".mp4");
		assertTrue(Math.abs(697202l - Application.duration) < 250);

		broadcast = appAdaptor.getDataStore().get(streamId);
		//we do not save duration of the finished live streams
		//assertEquals((long)broadcast.getDuration(), 697132L);

		verify(appAdaptor, times(1)).notifyHook(eq(hookUrl), eq(streamId), eq(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY), eq(null), eq(null), eq(streamId), anyString());
		Application.resetFields();
		//test with same id again
		testMp4Muxing(streamId, true, true);

		verify(appAdaptor, times(1)).notifyHook(eq(hookUrl), eq(streamId), eq(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY), eq(null), eq(null), eq(streamId+"_1"), anyString());

		assertEquals(Application.id, streamId);
		assertEquals(Application.file.getName(), streamId + "_1.mp4");
		assertEquals(10080L, Application.duration);

		broadcast = appAdaptor.getDataStore().get(streamId);
		//we do not save duration of the finished live streams
		//assertEquals((long)broadcast.getDuration(), 10080L);

		app.setAdaptor(appAdaptorReal);
	}

	@Test
	public void testMp4Muxing() {
		testMp4Muxing("lkdlfkdlfkdlfk");
	}


	public File testMp4Muxing(String name) {
		return testMp4Muxing(name, false, true);
	}
	
	@Test
	public void testMuxAdaptorClose() {
		
		appScope = (WebScope) applicationContext.getBean("web.scope");
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, false, appScope);
		String streamId = "stream_id" + (int)(Math.random()*10000);
		
		boolean result = muxAdaptor.init(appScope, streamId, false);

		assertTrue(result);
		
		muxAdaptor.closeResources();
	}
	
	boolean prepareCalledMuxAdaptorStopWhilePreparing = false;
	boolean prepareReturnedMuxAdaptorStopWhilePreparing = false;
	@Test
	public void testMuxAdaptorStopWhilePreparing() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(false);
		info.setHasVideo(false);
		clientBroadcastStream.setCodecInfo(info);
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);
		String streamId = "stream_id" + (int)(Math.random()*10000);
		
		boolean result = muxAdaptor.init(appScope, streamId, false);
		
		new Thread() {
			public void run() {
				try {
					prepareCalledMuxAdaptorStopWhilePreparing = true;
					muxAdaptor.prepare();
					prepareReturnedMuxAdaptorStopWhilePreparing = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
		
		Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(50, TimeUnit.SECONDS).until(() -> 
			 prepareCalledMuxAdaptorStopWhilePreparing
		);
		
		assertNotNull(muxAdaptor.getInputFormatContext());
		
		muxAdaptor.stop();
		
		Awaitility.await().pollInterval(1, TimeUnit.SECONDS).atMost(50, TimeUnit.SECONDS).until(() -> 
			prepareReturnedMuxAdaptorStopWhilePreparing
		);
		
	}
	
	public File testMp4Muxing(String name, boolean shortVersion, boolean checkDuration) {

		logger.info("running testMp4Muxing");

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(true);
		info.setHasVideo(true);
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setHlsMuxingEnabled(false);

		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {


			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(0, scheduler.getScheduledJobNames().size());

			if (shortVersion) {
				file = new File("target/test-classes/test_short.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			} else {
				file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			}

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, name, false);

			assertTrue(result);


			muxAdaptor.start();

			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}

			for (String jobName : scheduler.getScheduledJobNames()) {
				logger.info("testMP4Muxing -- Scheduler job name {}", jobName);
			}

			//2 jobs in the scheduler one of them is the job streamFetcherManager and and the other one is
			//job in MuxAdaptor
			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> scheduler.getScheduledJobNames().size() == 1);
			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

			assertEquals(1, scheduler.getScheduledJobNames().size());
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();


			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			assertFalse(muxAdaptor.isRecording());

			// if there is listenerHookURL, a task will be scheduled, so wait a little to make the call happen
			for (String jobName : scheduler.getScheduledJobNames()) {
				logger.info("--Scheduler job name {}", jobName);
			}

			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> scheduler.getScheduledJobNames().size() == 0);
			assertEquals(0, scheduler.getScheduledJobNames().size());
			int duration = 697000;
			if (shortVersion) {
				duration = 10080;
			}

			if (checkDuration) {
				assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), duration));
			}
			return muxAdaptor.getMuxerList().get(0).getFile();
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
		logger.info("leaving testMp4Muxing");
		return null;
	}
	
	/**
	 * 
	 */
	boolean checkStreamReturned = false;
	@Test
	public void testCheckStreams() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(false);
		info.setHasVideo(false);
		clientBroadcastStream.setCodecInfo(info);
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);
		
		checkStreamReturned = false;
		new Thread() {
			public void run() {
				try {
					muxAdaptor.checkStreams();
					checkStreamReturned = true;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
		}.start();

		
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return checkStreamReturned;
		});

	}
	
	@Test
	public void testNonZeroStartingFrame() {
		
		try {
			ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
			StreamCodecInfo info = new StreamCodecInfo();
			info.setHasAudio(true);
			info.setHasVideo(true);
			clientBroadcastStream.setCodecInfo(info);
			
			appScope = (WebScope) applicationContext.getBean("web.scope");
			
			MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);
	
			boolean result = muxAdaptor.init(appScope, "video_with_subtitle_stream", false);
			assertTrue(result);
			muxAdaptor.setEnableAudio(false);
			muxAdaptor.setEnableVideo(false);
	
			File file = new File("target/test-classes/test_video_360p_subtitle.flv"); 
	
			final FLVReader flvReader = new FLVReader(file);
			
			ITag readTag = flvReader.readTag();
			readTag.setTimestamp(5000 + readTag.getTimestamp());
			StreamPacket streamPacket = new StreamPacket(readTag);
			muxAdaptor.packetReceived(null, streamPacket);
		
		
			muxAdaptor.checkStreams();
			
			assertTrue(muxAdaptor.isEnableAudio());
			assertTrue(muxAdaptor.isEnableVideo());
			
			flvReader.close();
		
		} 
		catch (InterruptedException | IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
	}

	@Test
	public void testMp4MuxingSubtitledVideo() {
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setAddDateTimeToMp4FileName(true);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(false);


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(true);
		info.setHasVideo(true);
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(0, scheduler.getScheduledJobNames().size());


			file = new File("target/test-classes/test_video_360p_subtitle.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));


			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "video_with_subtitle_stream", false);
			assertTrue(result);


			muxAdaptor.start();

			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);

			}

			Thread.sleep(500);

			assertEquals(1, scheduler.getScheduledJobNames().size());
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());

			// if there is listenerHookURL, a task will be scheduled, so wait a little to make the call happen
			Thread.sleep(200);

			assertEquals(0, scheduler.getScheduledJobNames().size());
			int duration = 146401;

			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), duration));
			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath()));

		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
	}

	@Test
	public void testChangeAppSettingsMP4andHLS() {
		//fail("implement this test");

		//change appsettings and make sure that mp4 and hls whether relavant files are created properly
	}

	@Test
	public void testCheckDefaultAppSettings() {
		//fail("implement this test");
	}


	@Test
	public void testHLSNormal() {
		testHLSMuxing("hlsmuxing_test");
	}

	@Test
	public void testMp4MuxingWithDirectParams() {
		QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
		assertNotNull(scheduler);

		Mp4Muxer mp4Muxer = new Mp4Muxer(null, scheduler);

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		String streamName = "stream_name_" + (int) (Math.random() * 10000);
		//init
		mp4Muxer.init(appScope, streamName, 0);

		//add stream
		int width = 640;
		int height = 480;
		boolean addStreamResult = mp4Muxer.addVideoStream(width, height, null, AV_CODEC_ID_H264, 0, false, null);
		assertTrue(addStreamResult);

		//prepare io
		boolean prepareIOresult = mp4Muxer.prepareIO();
		assertTrue(prepareIOresult);

		try {
			FileInputStream fis = new FileInputStream("src/test/resources/frame0");
			byte[] byteArray = IOUtils.toByteArray(fis);

			fis.close();

			long now = System.currentTimeMillis();
			ByteBuffer encodedVideoFrame = ByteBuffer.wrap(byteArray);

			for (int i = 0; i < 100; i++) {
				//add packet
				mp4Muxer.writeVideoBuffer(encodedVideoFrame, now + i * 100, 0, 0, true, 0);
			}

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		//write trailer
		mp4Muxer.writeTrailer();

		Awaitility.await().atMost(10, TimeUnit.SECONDS)
		.pollInterval(1, TimeUnit.SECONDS)
		.until(() -> {
			return MuxingTest.testFile("webapps/junit/streams/" + streamName + ".mp4", 10000);
		});


	}

	@Test
	public void testHLSMuxingWithinChildScope() {

		int hlsTime = 2;
		int hlsListSize = 5;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(true);
		info.setHasVideo(true);
		clientBroadcastStream.setCodecInfo(info);
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);

		QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
		assertNotNull(scheduler);

		assertEquals(0, scheduler.getScheduledJobNames().size());

		appScope.createChildScope("child");


		IScope childScope = appScope.getScope("child");

		childScope.createChildScope("child2");
		IScope childScope2 = childScope.getScope("child2");

		childScope2.createChildScope("child3");
		IScope childScope3 = childScope2.getScope("child3");

		File file = null;
		try {


			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(childScope3, "test_within_childscope", false);
			assert (result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}

			Thread.sleep(1000);
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());
			assertEquals(1, scheduler.getScheduledJobNames().size());


			List<Muxer> muxerList = muxAdaptor.getMuxerList();
			HLSMuxer hlsMuxer = null;
			for (Muxer muxer : muxerList) {
				if (muxer instanceof HLSMuxer) {
					hlsMuxer = (HLSMuxer) muxer;
					break;
				}
			}
			assertNotNull(hlsMuxer);
			File hlsFile = hlsMuxer.getFile();

			String hlsFilePath = hlsFile.getAbsolutePath();
			int lastIndex = hlsFilePath.lastIndexOf(".m3u8");

			String mp4Filename = hlsFilePath.substring(0, lastIndex) + ".mp4";

			//just check mp4 file is not created
			File mp4File = new File(mp4Filename);
			assertFalse(mp4File.exists());

			assertTrue(MuxingTest.testFile(hlsFile.getAbsolutePath()));

			File dir = new File(hlsFile.getAbsolutePath()).getParentFile();
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts");
				}
			});

			System.out.println("ts file count:" + files.length);

			assertTrue(files.length > 0);
			assertTrue(files.length < (int) Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

			//wait to let hls muxer delete ts and m3u8 file
			Thread.sleep(hlsListSize * hlsTime * 1000 + 3000);


			assertFalse(hlsFile.exists());

			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}


	public void testHLSMuxing(String name) {

		//av_log_set_level (40);
		int hlsListSize = 5;
		int hlsTime = 2;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(true);
		info.setHasVideo(true);
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);

		File file = null;
		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(0, scheduler.getScheduledJobNames().size());

			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.info("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, name, false);
			assert (result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}

			Thread.sleep(1000);
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());
			assertEquals(1, scheduler.getScheduledJobNames().size());


			List<Muxer> muxerList = muxAdaptor.getMuxerList();
			HLSMuxer hlsMuxer = null;
			for (Muxer muxer : muxerList) {
				if (muxer instanceof HLSMuxer) {
					hlsMuxer = (HLSMuxer) muxer;
					break;
				}
			}
			assertNotNull(hlsMuxer);
			File hlsFile = hlsMuxer.getFile();

			String hlsFilePath = hlsFile.getAbsolutePath();
			int lastIndex = hlsFilePath.lastIndexOf(".m3u8");

			String mp4Filename = hlsFilePath.substring(0, lastIndex) + ".mp4";

			//just check mp4 file is not created
			File mp4File = new File(mp4Filename);
			assertFalse(mp4File.exists());

			assertTrue(MuxingTest.testFile(hlsFile.getAbsolutePath()));

			File dir = new File(hlsFile.getAbsolutePath()).getParentFile();
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts");
				}
			});

			System.out.println("ts file count:" + files.length);

			assertTrue(files.length > 0);
			assertTrue(files.length < (int) Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));


			//wait to let hls muxer delete ts and m3u8 file
			Thread.sleep(hlsListSize * hlsTime * 1000 + 3000);


			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(false);

	}


	@Test
	public void testHLSMuxingWithSubtitle() {

		//av_log_set_level (40);
		int hlsListSize = 5;
		int hlsTime = 2;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(true);
		info.setHasVideo(true);
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);

		File file = null;
		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(0, scheduler.getScheduledJobNames().size());

			file = new File("target/test-classes/test_video_360p_subtitle.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.info("f path: {}" , file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "hls_video_subtitle", false);
			assert (result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}

			Thread.sleep(1000);
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());
			// delete job in the list
			assertEquals(1, scheduler.getScheduledJobNames().size());


			List<Muxer> muxerList = muxAdaptor.getMuxerList();
			HLSMuxer hlsMuxer = null;
			for (Muxer muxer : muxerList) {
				if (muxer instanceof HLSMuxer) {
					hlsMuxer = (HLSMuxer) muxer;
					break;
				}
			}
			assertNotNull(hlsMuxer);
			File hlsFile = hlsMuxer.getFile();

			String hlsFilePath = hlsFile.getAbsolutePath();
			int lastIndex = hlsFilePath.lastIndexOf(".m3u8");

			String mp4Filename = hlsFilePath.substring(0, lastIndex) + ".mp4";

			//just check mp4 file is not created
			File mp4File = new File(mp4Filename);
			assertFalse(mp4File.exists());

			assertTrue(MuxingTest.testFile(hlsFile.getAbsolutePath()));

			File dir = new File(hlsFile.getAbsolutePath()).getParentFile();
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts");
				}
			});

			System.out.println("ts file count:" + files.length);

			assertTrue(files.length > 0);

			logger.info("hls list:{}", (int) Integer.valueOf(hlsMuxer.getHlsListSize()));

			logger.info("hls time:{}", (int) Integer.valueOf(hlsMuxer.getHlsTime()));

			assertTrue(files.length < (int) Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

			//wait to let hls muxer delete ts and m3u8 file
			Thread.sleep(hlsListSize * hlsTime * 1000 + 3000);


			assertFalse(hlsFile.exists());

			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	@Test
	public void testRecording() {
		testRecording("dasss", true);
	}

	public void testRecording(String name, boolean checkDuration) {
		logger.info("running testMp4Muxing");

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		info.setHasAudio(true);
		info.setHasVideo(true);
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, false, appScope);
		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setHlsMuxingEnabled(false);

		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);

			//by default, stream source job is scheduled
			assertEquals(0, scheduler.getScheduledJobNames().size());

			file = new File("target/test-classes/test.flv");

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			logger.info("1");
			for (String jobName : scheduler.getScheduledJobNames()) {
				logger.info("testMP4Muxing1 -- Scheduler job name {}", jobName);
			}
			boolean result = muxAdaptor.init(appScope, name, false);

			assertTrue(result);


			muxAdaptor.start();
			logger.info("2");
			int packetNumber = 0;
			int lastTimeStamp = 0;
			int startOfRecordingTimeStamp = 0;
			while (flvReader.hasMoreTags()) {

				ITag readTag = flvReader.readTag();

				StreamPacket streamPacket = new StreamPacket(readTag);
				lastTimeStamp = streamPacket.getTimestamp();
				if(packetNumber == 0){
					logger.info("timeStamp 1 "+streamPacket.getTimestamp());
				}

				muxAdaptor.packetReceived(null, streamPacket);


				if (packetNumber == 40000) {
					logger.info("----input queue size: {}", muxAdaptor.getInputQueueSize());
					Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.getInputQueueSize() == 0);
					logger.info("----input queue size: {}", muxAdaptor.getInputQueueSize());
					startOfRecordingTimeStamp = streamPacket.getTimestamp();
					muxAdaptor.startRecording();
				}
				packetNumber++;

				if (packetNumber % 1000 == 0) {
					logger.info("packetNumber " + packetNumber);
				}
			}

			for (String jobName : scheduler.getScheduledJobNames()) {
				logger.info("testMP4Muxing -- Scheduler job name {}", jobName);
			}

			//2 jobs in the scheduler one of them is the job streamFetcherManager and and the other one is
			//job in MuxAdaptor
			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> scheduler.getScheduledJobNames().size() == 1);
			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(muxAdaptor::isRecording);

			assertEquals(1, scheduler.getScheduledJobNames().size());
			assertTrue(muxAdaptor.isRecording());
			final String finalFilePath = muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath();


			muxAdaptor.stopRecording();
			muxAdaptor.stop();

			flvReader.close();

			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			assertFalse(muxAdaptor.isRecording());

			// if there is listenerHookURL, a task will be scheduled, so wait a little to make the call happen
			for (String jobName : scheduler.getScheduledJobNames()) {
				logger.info("--Scheduler job name {}", jobName);
			}

			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> scheduler.getScheduledJobNames().isEmpty());
			assertEquals(0, scheduler.getScheduledJobNames().size());
			assertTrue(MuxingTest.testFile(finalFilePath, lastTimeStamp-startOfRecordingTimeStamp));

		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
		logger.info("leaving testRecording");
	}



	@Test
	public void testRemux() {
		String input = "target/test-classes/sample_MP4_480.mp4";
		String rotated = "rotated.mp4";

		Mp4Muxer.remux(input, rotated, 90);
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();

		int ret;
		if (inputFormatContext == null) {
			System.out.println("cannot allocate input context");
		}

		if ((ret = avformat_open_input(inputFormatContext, rotated, null, (AVDictionary) null)) < 0) {
			System.out.println("cannot open input context: " + rotated);
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			System.out.println("Could not find stream information\n");
		}

		int streamCount = inputFormatContext.nb_streams();

		for (int i = 0; i < streamCount; i++) {
			AVCodecContext codecContext = inputFormatContext.streams(i).codec();
			if (codecContext.codec_type() == AVMEDIA_TYPE_VIDEO) {
				AVStream videoStream = inputFormatContext.streams(i);

				AVDictionaryEntry entry = av_dict_get(videoStream.metadata(), "rotate", null, 0);

				assertEquals("90", entry.value().getString());
			}
		}

		avformat_close_input(inputFormatContext);

		new File(rotated).delete();

	}
}
