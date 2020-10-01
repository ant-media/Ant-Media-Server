package io.antmedia.test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.integration.MuxingTest;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.MuxAdaptor.InputContext;
import io.antmedia.muxer.Muxer;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.vertx.core.Vertx;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_get;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.tika.io.IOUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
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
import org.red5.server.scope.WebScope;
import org.red5.server.service.mp4.impl.MP4Service;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.integration.MuxingTest;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.vertx.core.Vertx;

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
			AppFunctionalV2Test.delete(new File("webapps"));
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
			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());
			String streamId = "test" + (int) (Math.random() * 991000);
			Broadcast broadcast = new Broadcast();
			broadcast.setStreamId(streamId);
			getDataStore().save(broadcast);
			boolean result = muxAdaptor.init(appScope, streamId, false);
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
			
			getAppSettings().setMaxAnalyzeDurationMS(50000);
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
					Broadcast broadcast = new Broadcast();
					broadcast.setStreamId(streamId);
					getDataStore().save(broadcast);
					
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

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { 
			return "test_test".equals(Application.id);
		});
		
		assertEquals("test_test", Application.id);
		
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { 
			return "test_test.mp4".equals(Application.file.getName());
		});
		
		assertEquals("test_test.mp4", Application.file.getName());
		assertNotEquals(0L, Application.duration);

		Application.resetFields();

		assertEquals(null, Application.id);
		assertEquals(null, Application.file);
		assertEquals(0, Application.duration);

		file = testMp4Muxing("test_test");
		assertEquals("test_test_1.mp4", file.getName());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test".equals(Application.id);
		});
		
		assertEquals("test_test", Application.id);
		
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test_1.mp4".equals(Application.file.getName());
		});
		
		assertEquals("test_test_1.mp4", Application.file.getName());
		assertNotEquals(0L, Application.duration);

		Application.resetFields();

		assertEquals(null, Application.id);
		assertEquals(null, Application.file);
		assertEquals(0, Application.duration);

		file = testMp4Muxing("test_test");
		assertEquals("test_test_2.mp4", file.getName());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { 
			return "test_test".equals(Application.id);
		});
		
		assertEquals("test_test", Application.id);
		
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { 
			return "test_test_2.mp4".equals(Application.file.getName());
		});
		
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
		broadcast.setListenerHookURL("any_url");
		appAdaptor.getDataStore().save(broadcast);
		broadcast.setWebRTCViewerCount(10);
		broadcast.setHlsViewerCount(20);
		IBroadcastStream stream = Mockito.mock(IBroadcastStream.class);
		Mockito.when(stream.getPublishedName()).thenReturn(broadcast.getStreamId());

		VideoServiceEndpoint endpointService = Mockito.mock(VideoServiceEndpoint.class);
		String endpointServiceId = "" + (Math.random()*10000);
		appAdaptor.getVideoServiceEndpoints().put(endpointServiceId, endpointService);	

		Endpoint endpoint = new Endpoint(null, broadcast.getStreamId(), "name", "rtmp url", null, endpointServiceId, null);

		appAdaptor.getDataStore().addEndpoint(broadcast.getStreamId(), endpoint);

		appAdaptor.streamPublishStart(stream);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS) 
		.until(() -> 
			appAdaptor.getDataStore().get(broadcast.getStreamId()) 
			.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));

		Broadcast dtBroadcast = appAdaptor.getDataStore().get(broadcast.getStreamId()); 
		assertEquals(0, dtBroadcast.getWebRTCViewerCount());
		assertEquals(0, dtBroadcast.getHlsViewerCount());

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
		String hookUrl = "http://google.com";
		String name = "namer123";
		Broadcast broadcast = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, name);
		broadcast.setListenerHookURL(hookUrl);
		String streamId = appAdaptor.getDataStore().save(broadcast);

		testMp4Muxing(streamId, false, true);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { 
			return streamId.equals(Application.id);
		});
		
		assertEquals(Application.id, streamId);
		assertEquals(Application.file.getName(), streamId + ".mp4");
		assertTrue(Math.abs(697202l - Application.duration) < 250);

		broadcast = appAdaptor.getDataStore().get(streamId);
		//we do not save duration of the finished live streams
		//assertEquals((long)broadcast.getDuration(), 697132L);

		verify(appAdaptor,  timeout(1000).times(1)).notifyHook(eq(hookUrl), eq(streamId), eq(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY), eq(null), eq(null), eq(streamId), anyString());
		Application.resetFields();
		//test with same id again
		testMp4Muxing(streamId, true, true);

		verify(appAdaptor, timeout(1000).times(1)).notifyHook(eq(hookUrl), eq(streamId), eq(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY), eq(null), eq(null), eq(streamId+"_1"), anyString());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { 
			return streamId.equals(Application.id);
		});
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
	
	@Test
	public void testWriteBufferedPacket() {
		
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, false, appScope));
		
		muxAdaptor.setBuffering(true);
		muxAdaptor.writeBufferedPacket();
		assertTrue(muxAdaptor.isBuffering());
		
		muxAdaptor.setBuffering(false);
		muxAdaptor.writeBufferedPacket();
		//it should false because there is no packet in the queue
		assertTrue(muxAdaptor.isBuffering());
		
		Queue<AVPacket> bufferQueue = muxAdaptor.getBufferQueue();
		muxAdaptor.setBuffering(false);
		AVFormatContext inputFormatContext = Mockito.mock(AVFormatContext.class);
		AVStream stream = Mockito.mock(AVStream.class);
		when(stream.time_base()).thenReturn(MuxAdaptor.TIME_BASE_FOR_MS);
		
		when(inputFormatContext.streams(0)).thenReturn(stream);
		
		muxAdaptor.setInputFormatContext(inputFormatContext);
		AVPacket pkt = new AVPacket();
		pkt.pts(1000L);
		pkt.stream_index(0);
		
		bufferQueue.add(pkt);
		
		doNothing().when(muxAdaptor).writePacket(any(), any());
		muxAdaptor.writeBufferedPacket();
		verify(muxAdaptor).writePacket(any(), any());
		assertTrue(muxAdaptor.isBuffering());
		
		muxAdaptor.setBuffering(false);
		pkt.pts(System.currentTimeMillis()+1000);
		bufferQueue.add(pkt);
		muxAdaptor.writeBufferedPacket();
		assertFalse(muxAdaptor.isBuffering());
		
	}
	
	@Test
	public void testRtmpIngestBufferTime() 
	{

		try {
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

			//increase max analyze duration to some higher value because it's also to close connections if packet is not received
			getAppSettings().setMaxAnalyzeDurationMS(5000); 
			getAppSettings().setRtmpIngestBufferTimeMs(1000);
			getAppSettings().setMp4MuxingEnabled(false);
			getAppSettings().setHlsMuxingEnabled(false);

			File file = new File("target/test-classes/test.flv");

			String streamId = "streamId" + (int)(Math.random()*10000);

			boolean result = muxAdaptor.init(appScope, streamId, false);
			assertTrue(result);

			muxAdaptor.start();


			final FLVReader flvReader = new FLVReader(file);

			long lastTimeStamp = 0;
			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();
				lastTimeStamp = readTag.getTimestamp();
				if (lastTimeStamp < 6000) {
					StreamPacket streamPacket = new StreamPacket(readTag);
					muxAdaptor.packetReceived(null, streamPacket);
				}
				else {
					break;
				}
			}
			System.gc();

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(muxAdaptor::isRecording);	
			//let the buffered time finish and buffering state is true
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(muxAdaptor::isBuffering);	

			//load again for 6 more seconds
			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();

				if (readTag.getTimestamp() - lastTimeStamp  < 6000) {
					StreamPacket streamPacket = new StreamPacket(readTag);
					muxAdaptor.packetReceived(null, streamPacket);
				}
				else {
					break;
				}
			}
			//buffering should be false after a while because it's loaded with 5 seconds
			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !muxAdaptor.isBuffering());	

			//after 6 seconds buffering should be also true again because it's finished
			Awaitility.await().atMost(6, TimeUnit.SECONDS).until(muxAdaptor::isBuffering);

			muxAdaptor.stop();

			Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setRtmpIngestBufferTimeMs(0);

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
		
		if(getDataStore().get(name) == null) {
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId(name);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			getDataStore().save(broadcast);
		}
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setHlsMuxingEnabled(false);

		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

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


			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();


			Awaitility.await().atMost(40, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());
			assertFalse(muxAdaptor.isRecording()); 

			int duration = 697000;
			if (shortVersion) {
				duration = 10080;
			}

			if (checkDuration) {
				int finalDuration = duration; 
				Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(()-> 
					MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), finalDuration)); 
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
	private DataStore datastore;
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

			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording()); 
			muxAdaptor.stop();

			flvReader.close();

			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			// if there is listenerHookURL, a task will be scheduled, so wait a little to make the call happen
			Thread.sleep(200);

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
		Vertx vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
		assertNotNull(vertx);

		Mp4Muxer mp4Muxer = new Mp4Muxer(null, vertx);

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

			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());
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

			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

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

			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording()); 

			// delete job in the list 
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
	
	public DataStore getDataStore() {
		if (datastore == null) {
			datastore = ((DataStoreFactory) applicationContext.getBean(DataStoreFactory.BEAN_NAME)).getDataStore();
		}
		return datastore;
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

			file = new File("target/test-classes/test.flv");

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			logger.info("1");
			boolean result = muxAdaptor.init(appScope, name, false);

			assertTrue(result);


			muxAdaptor.start();
			logger.info("2");
			int packetNumber = 0;
			int lastTimeStamp = 0;
			int startOfRecordingTimeStamp = 0;
			ArrayList<Integer> timeStamps = new ArrayList<>();
			while (flvReader.hasMoreTags()) {

				ITag readTag = flvReader.readTag();

				StreamPacket streamPacket = new StreamPacket(readTag);
				lastTimeStamp = streamPacket.getTimestamp();
				if(packetNumber == 0){
					logger.info("timeStamp 1 "+streamPacket.getTimestamp());
				}
				timeStamps.add(lastTimeStamp);
				muxAdaptor.packetReceived(null, streamPacket);


				if (packetNumber == 40000) {
					logger.info("----input queue size: {}", muxAdaptor.getInputQueueSize());
					Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.getInputQueueSize() == 0);
					logger.info("----input queue size: {}", muxAdaptor.getInputQueueSize());
					startOfRecordingTimeStamp = streamPacket.getTimestamp();
					assertTrue(muxAdaptor.startRecording(RecordType.MP4));
				}
				packetNumber++;

				if (packetNumber % 1000 == 0) {
					logger.info("packetNumber " + packetNumber);
				}
			}

			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(muxAdaptor::isRecording);

			assertTrue(muxAdaptor.isRecording());
			final String finalFilePath = muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath();

			int inputQueueSize = muxAdaptor.getInputQueueSize();
			logger.info("----input queue size before stop recording: {}", inputQueueSize);
			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.getInputQueueSize() == 0);
			
			inputQueueSize = muxAdaptor.getInputQueueSize(); 
			int estimatedLastTimeStamp = lastTimeStamp;
			if (inputQueueSize > 0) {
				estimatedLastTimeStamp = timeStamps.get((timeStamps.size() - inputQueueSize));
			}
			assertTrue(muxAdaptor.stopRecording(RecordType.MP4));
			
			muxAdaptor.stop();

			flvReader.close();

			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			assertFalse(muxAdaptor.isRecording());
			
			assertTrue(MuxingTest.testFile(finalFilePath, estimatedLastTimeStamp-startOfRecordingTimeStamp));

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
	
	@Test
	public void testMp4MuxingWithSameNameWhileRecording() {

		/*
		 * In this test we create such a case with spy Files
		 * In record directory
		 * test.mp4						existing
		 * test_1.mp4 					non-existing
		 * test_2.mp4 					non-existing
		 * test.mp4.tmp_extension		non-existing
		 * test_1.mp4.tmp_extension		existing
		 * test_2.mp4.tmp_extension		non-existing
		 * 
		 * So we check new record file must be temp_2.mp4
		 */
		
		String streamId = "test";
		Muxer mp4Muxer = spy(new Mp4Muxer(null, null));
		IScope scope = mock(IScope.class);
		
		File parent = mock(File.class);
		when(parent.exists()).thenReturn(true);
		
		File existingFile = spy(new File(streamId+".mp4"));
		doReturn(true).when(existingFile).exists();
		doReturn(parent).when(existingFile).getParentFile();

		File nonExistingFile_1 = spy(new File(streamId+"_1.mp4"));
		doReturn(false).when(nonExistingFile_1).exists();
		
		File nonExistingFile_2 = spy(new File(streamId+"_2.mp4"));
		doReturn(false).when(nonExistingFile_2).exists();
		
		
		File nonExistingTempFile = spy(new File(streamId+".mp4"+Muxer.TEMP_EXTENSION));
		doReturn(true).when(nonExistingTempFile).exists();
		
		File existingTempFile_1 = spy(new File(streamId+"_1.mp4"+Muxer.TEMP_EXTENSION));
		doReturn(true).when(existingTempFile_1).exists();
		
		File nonExistingTempFile_2 = spy(new File(streamId+"_2.mp4"+Muxer.TEMP_EXTENSION));
		doReturn(false).when(nonExistingTempFile_2).exists();

		doReturn(existingFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4"));
		doReturn(nonExistingFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId+"_1"), eq(".mp4"));
		doReturn(nonExistingFile_2).when(mp4Muxer).getResourceFile(any(), eq(streamId+"_2"), eq(".mp4"));

		doReturn(nonExistingTempFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4"+Muxer.TEMP_EXTENSION));
		doReturn(existingTempFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId+"_1"), eq(".mp4"+Muxer.TEMP_EXTENSION));
		doReturn(nonExistingTempFile_2).when(mp4Muxer).getResourceFile(any(), eq(streamId+"_2"), eq(".mp4"+Muxer.TEMP_EXTENSION));
		
		mp4Muxer.init(scope, streamId, 0, false);
		
		assertEquals(nonExistingFile_2, mp4Muxer.getFile());
		
	}
	
	@Test
	public void testMp4MuxingWhileTempFileExist() {

		/*
		 * In this test we create such a case with spy Files
		 * In record directory
		 * test.mp4						non-existing
		 * test_1.mp4 					non-existing
		 * test.mp4.tmp_extension		existing
		 * test_1.mp4.tmp_extension		non-existing
		 * 
		 * So we check new record file must be temp_1.mp4
		 */
		
		String streamId = "test";
		Muxer mp4Muxer = spy(new Mp4Muxer(null, null));
		IScope scope = mock(IScope.class);
		
		File parent = mock(File.class);
		when(parent.exists()).thenReturn(true);
		
		File nonExistingFile = spy(new File(streamId+".mp4"));
		doReturn(false).when(nonExistingFile).exists();
		doReturn(parent).when(nonExistingFile).getParentFile();

		File nonExistingFile_1 = spy(new File(streamId+"_1.mp4"));
		doReturn(false).when(nonExistingFile_1).exists();
		
		File existingTempFile = spy(new File(streamId+".mp4"+Muxer.TEMP_EXTENSION));
		doReturn(true).when(existingTempFile).exists();
		
		File nonExistingTempFile_1 = spy(new File(streamId+"_1.mp4"+Muxer.TEMP_EXTENSION));
		doReturn(false).when(nonExistingTempFile_1).exists();
		
		doReturn(nonExistingFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4"));
		doReturn(nonExistingFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId+"_1"), eq(".mp4"));

		doReturn(existingTempFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4"+Muxer.TEMP_EXTENSION));
		doReturn(nonExistingTempFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId+"_1"), eq(".mp4"+Muxer.TEMP_EXTENSION));

		mp4Muxer.init(scope, streamId, 0, false);
		
		assertEquals(nonExistingFile_1, mp4Muxer.getFile());
		
	}
}
