package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.mina.core.buffer.IoBuffer;
import org.awaitility.Awaitility;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVReader;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.WebScope;
import org.red5.server.service.mp4.impl.MP4Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.antstreaming.rtsp.PacketSenderRunnable;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.integration.AppFunctionalTest;
import io.antmedia.integration.MuxingTest;
import io.antmedia.integration.RestServiceTest;
//import io.antmedia.enterprise.adaptive.TransraterAdaptor;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.social.endpoint.VideoServiceEndpoint;

@ContextConfiguration(locations = { 
		"test.xml" 
})
//@ContextConfiguration(classes = {AppConfig.class})
public class MuxerUnitTest extends AbstractJUnit4SpringContextTests{

	protected static Logger logger = LoggerFactory.getLogger(MuxerUnitTest.class);
	protected static final int BUFFER_SIZE = 10240;

	protected WebScope appScope;
	private RestServiceTest rest=new RestServiceTest();
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
		
		
		AppSettings defaultSettings = new AppSettings();

		//reset values in the bean
		getAppSettings().setMp4MuxingEnabled(defaultSettings.isMp4MuxingEnabled());
		getAppSettings().setHlsMuxingEnabled(defaultSettings.isHlsMuxingEnabled());
		getAppSettings().setAddDateTimeToMp4FileName(false);

		getAppSettings().setMp4MuxingEnabled(defaultSettings.isMp4MuxingEnabled());
		getAppSettings().setAddDateTimeToMp4FileName(defaultSettings.isAddDateTimeToMp4FileName());
		getAppSettings().setHlsMuxingEnabled(defaultSettings.isHlsMuxingEnabled());
		getAppSettings().setWebRTCEnabled(defaultSettings.isWebRTCEnabled());
		getAppSettings().setDeleteHLSFilesOnEnded(defaultSettings.isDeleteHLSFilesOnExit());
		getAppSettings().setHlsListSize(defaultSettings.getHlsListSize());
		getAppSettings().setHlsTime(defaultSettings.getHlsTime());
		getAppSettings().setHlsPlayListType(defaultSettings.getHlsPlayListType());
		getAppSettings().setAdaptiveResolutionList(defaultSettings.getAdaptiveResolutionList());
		
	}

	@After
	public void after() {
	
		
		try {
			AppFunctionalTest.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		AppSettings defaultSettings = new AppSettings();

		//reset values in the bean
		getAppSettings().setMp4MuxingEnabled(defaultSettings.isMp4MuxingEnabled());
		getAppSettings().setHlsMuxingEnabled(defaultSettings.isHlsMuxingEnabled());
		getAppSettings().setAddDateTimeToMp4FileName(false);

		getAppSettings().setMp4MuxingEnabled(defaultSettings.isMp4MuxingEnabled());
		getAppSettings().setAddDateTimeToMp4FileName(defaultSettings.isAddDateTimeToMp4FileName());
		getAppSettings().setHlsMuxingEnabled(defaultSettings.isHlsMuxingEnabled());
		getAppSettings().setWebRTCEnabled(defaultSettings.isWebRTCEnabled());
		getAppSettings().setDeleteHLSFilesOnEnded(defaultSettings.isDeleteHLSFilesOnExit());
		getAppSettings().setHlsListSize(defaultSettings.getHlsListSize());
		getAppSettings().setHlsTime(defaultSettings.getHlsTime());
		getAppSettings().setHlsPlayListType(defaultSettings.getHlsPlayListType());
		getAppSettings().setAdaptiveResolutionList(defaultSettings.getAdaptiveResolutionList());
		
	}
	
	@Rule
	public TestRule watcher = new TestWatcher() {
	   protected void starting(Description description) {
	      System.out.println("Starting test: " + description.getMethodName());
	   }
	   
	   protected void failed(Throwable e, Description description) {
		   System.out.println("Failed test: " + description.getMethodName());
	   };
	   protected void finished(Description description) {
		   System.out.println("Finishing test: " + description.getMethodName());
	   };
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
	};

	@Test
	public void testMuxingSimultaneously()  {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(false);
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null,false, appScope);
		
		//this value should be -1. It means it is uninitialized
		assertEquals( -1, muxAdaptor.getFirstPacketTime());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			logger.info("name    "+String.valueOf(scheduler.getJobName().toCharArray()));

			assertEquals(scheduler.getScheduledJobNames().size(),1);

			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "test", false);
			assertTrue(result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			}

			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}


			assertFalse(muxAdaptor.isRecording());

			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath()));
			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath()));

		}
		catch (Exception e) {
			fail("excsereption:" + e );
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
			
			getAppSettings().setHlsMuxingEnabled(false);
			getAppSettings().setMp4MuxingEnabled(true);;
			getAppSettings().setAddDateTimeToMp4FileName(false);

			List<MuxAdaptor> muxAdaptorList = new ArrayList<MuxAdaptor>();
			for (int j = 0; j < 20; j++) {
				MuxAdaptor muxAdaptor =  MuxAdaptor.initializeMuxAdaptor(null, false, appScope);
				muxAdaptorList.add(muxAdaptor);
			}
			{

				File file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
				final FLVReader flvReader = new FLVReader(file);

				logger.debug("f path: " + file.getAbsolutePath());
				assertTrue(file.exists());

				for (Iterator<MuxAdaptor> iterator = muxAdaptorList.iterator(); iterator.hasNext();) {
					MuxAdaptor muxAdaptor = (MuxAdaptor) iterator.next();
					boolean result = muxAdaptor.init(appScope, "test" + (int)(Math.random() * 991000), false);
					assertTrue(result);
					muxAdaptor.start();
					logger.info("Mux adaptor instance " + muxAdaptor);
				}


				while (flvReader.hasMoreTags()) 
				{
					ITag readTag = flvReader.readTag();
					StreamPacket streamPacket = new StreamPacket(readTag);
					for (MuxAdaptor muxAdaptor : muxAdaptorList) 
					{
						muxAdaptor.packetReceived(null, streamPacket);
						streamPacket.getData().rewind();
					}

				}

				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					Awaitility.await().atMost(50, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
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

			//Thread.sleep(15000);

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

			//by default, stream fethcer
			assertEquals(1, scheduler.getScheduledJobNames().size());


		}
		catch (Exception e) {

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
	public void testMp4MuxingWithSameName() 
	{
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
	public void testVideoServiceEndpoint() {
		AntMediaApplicationAdapter appAdaptor = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		assertNotNull(appAdaptor);

		VideoServiceEndpoint endpointService = Mockito.mock(VideoServiceEndpoint.class);
		SocialEndpointCredentials credentials = Mockito.mock(SocialEndpointCredentials.class);
		String id = "" + (Math.random()*10000);
		Mockito.when(credentials.getId()).thenReturn(id);

		appAdaptor.getVideoServiceEndpoints().put(id, endpointService);

		Mockito.when(endpointService.getCredentials()).thenReturn(credentials);

		String id2 = "" + (Math.random()*10000);
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
		System.out.println("running testMp4MuxingAndNotifyCallback");
		Application.resetFields();
		assertEquals(null, Application.notifyHookAction);
		assertEquals(null,Application.notitfyURL);
		assertEquals(null, Application.notifyId);
		assertEquals(null, Application.notifyStreamName);
		assertEquals(null, Application.notifyCategory);
		assertEquals(null, Application.notifyVodName);

		AntMediaApplicationAdapter appAdaptor = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		assertNotNull(appAdaptor);
		//just check below value that it is not null, this is not related to this case but it should be tested
		assertNotNull(appAdaptor.getVideoServiceEndpoints());
		String hookUrl = "hook_url";
		String name = "namer123";
		Broadcast broadcast = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, name);
		broadcast.setListenerHookURL(hookUrl);
		String streamId = appAdaptor.getDataStore().save(broadcast);

		
		testMp4Muxing(streamId, false, true);
		

		assertEquals(Application.id, streamId);
		assertEquals(Application.file.getName(), streamId + ".mp4");
		assertEquals(697132L, Application.duration);

		broadcast = appAdaptor.getDataStore().get(streamId);
		//we do not save duration of the finished live streams
		//assertEquals((long)broadcast.getDuration(), 697132L);

		assertEquals(Application.HOOK_ACTION_VOD_READY, Application.notifyHookAction);
		assertEquals(Application.notitfyURL, hookUrl);
		assertEquals(Application.notifyId, streamId);
		assertEquals(null, Application.notifyStreamName);
		assertEquals(null, Application.notifyCategory);
		assertEquals(Application.notifyVodName, streamId);



		Application.resetFields();
		//test with same id again
		testMp4Muxing(streamId, true, true);

		assertEquals(Application.id, streamId);
		assertEquals(Application.file.getName(), streamId + "_1.mp4");
		assertEquals(10080L, Application.duration);

		broadcast = appAdaptor.getDataStore().get(streamId);
		//we do not save duration of the finished live streams
		//assertEquals((long)broadcast.getDuration(), 10080L);

		assertEquals(Application.HOOK_ACTION_VOD_READY, Application.notifyHookAction);
		assertEquals(Application.notitfyURL, hookUrl);
		assertEquals(Application.notifyId, streamId);
		assertEquals(null, Application.notifyStreamName);
		assertEquals(null, Application.notifyCategory);
		assertEquals(Application.notifyVodName, streamId + "_1"); //vod name must be changed
		
		
		System.out.println("leaving testMp4MuxingAndNotifyCallback");
	}

	@Test
	public void testMp4Muxing() {
		testMp4Muxing("lkdlfkdlfkdlfk");
	}


	public File testMp4Muxing(String name) {
		return testMp4Muxing(name, false, true);
	}


	public File testMp4Muxing(String name, boolean shortVersion, boolean checkDuration) {

		logger.info("running testMp4Muxing");
		
		

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		MuxAdaptor muxAdaptor =  MuxAdaptor.initializeMuxAdaptor(null, false, appScope);
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setHlsMuxingEnabled(false);
		
		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);

			//by default, stream source job is scheduled
			assertEquals(scheduler.getScheduledJobNames().size(), 1);

			if (shortVersion) {
				file = new File("target/test-classes/test_short.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			}
			else {
				file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			}

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, name, false);
			
			assertTrue(result);


			muxAdaptor.start();

			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);

			}

			Thread.sleep(100);

			for (String jobName : scheduler.getScheduledJobNames()) {
				logger.info("testMP4Muxing -- Scheduler job name {}", jobName);
			}
			
			//2 jobs in the scheduler one of them is the job streamFetcherManager and and the other one is 
			//job in MuxAdaptor
			assertEquals(scheduler.getScheduledJobNames().size(), 2);
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());

			// if there is listenerHookURL, a task will be scheduled, so wait a little to make the call happen
			Thread.sleep(200);
			for (String jobName : scheduler.getScheduledJobNames()) {
				logger.info("--Scheduler job name {}", jobName);
			}
			assertEquals(scheduler.getScheduledJobNames().size(), 1);
			int duration = 697000;
			if (shortVersion) {
				duration = 10080;
			}

			if (checkDuration) {
				assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), duration));
			}
			return muxAdaptor.getMuxerList().get(0).getFile();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e );
		}
		logger.info("leaving testMp4Muxing");
		return null;
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
		
		MuxAdaptor muxAdaptor =  MuxAdaptor.initializeMuxAdaptor(null, false, appScope);

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(scheduler.getScheduledJobNames().size(), 1);


			file = new File("target/test-classes/test_video_360p_subtitle.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));


			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "video_with_subtitle_stream", false);
			assertTrue(result);


			muxAdaptor.start();

			while (flvReader.hasMoreTags()) 
			{
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);

			}

			Thread.sleep(500);

			assertEquals(scheduler.getScheduledJobNames().size(), 2);
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop();

			flvReader.close();

			while (muxAdaptor.isRecording()) {
				Thread.sleep(50);
			}

			assertFalse(muxAdaptor.isRecording());

			// if there is listenerHookURL, a task will be scheduled, so wait a little to make the call happen
			Thread.sleep(200);

			assertEquals(1, scheduler.getScheduledJobNames().size());
			int duration = 146401;

			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), duration));
			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath()));

		}
		catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e );
		}
	}

	@Test
	public void testChangeAppSettingsMP4andHLS() {
		//fail("implement this test");

		//change appsettings and make sure that mp4 and hls whether relavant files are created properly
	}

	@Test
	public void testCheckDefaultAppSettings(){
		//fail("implement this test");
	}


	@Test
	public void testHLSNormal() {
		testHLSMuxing("hlsmuxing_test");
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
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, false, appScope);

		QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
		assertNotNull(scheduler);
		
		assertEquals(scheduler.getScheduledJobNames().size(),1);
		
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
			assert(result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) 
			{
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
			assertEquals(2, scheduler.getScheduledJobNames().size());


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
			int lastIndex =  hlsFilePath.lastIndexOf(".m3u8");

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
			assertTrue(files.length < (int)Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

			//wait to let hls muxer delete ts and m3u8 file
			Thread.sleep(hlsListSize*hlsTime * 1000 + 3000);


			assertFalse(hlsFile.exists());

			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}


	public void testHLSMuxing(String name)  {
		
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
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, false, appScope);

		File file = null;
		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(scheduler.getScheduledJobNames().size(), 1);

			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.info("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, name, false);
			assert(result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) 
			{
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
			assertEquals(2, scheduler.getScheduledJobNames().size());


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
			int lastIndex =  hlsFilePath.lastIndexOf(".m3u8");

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
			assertTrue(files.length < (int)Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));



			//wait to let hls muxer delete ts and m3u8 file
			Thread.sleep(hlsListSize*hlsTime * 1000 + 3000);


			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);

		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		getAppSettings().setDeleteHLSFilesOnEnded(false);

	}
	
	
	@Test
	public void testHLSMuxingWithSubtitle()  {

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
		
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, false, appScope);

		File file = null;
		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			assertEquals(scheduler.getScheduledJobNames().size(),1);

			file = new File("target/test-classes/test_video_360p_subtitle.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.info("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, "hls_video_subtitle", false);
			assert(result);

			muxAdaptor.start();

			while (flvReader.hasMoreTags()) 
			{
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
			assertEquals(2, scheduler.getScheduledJobNames().size());


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
			int lastIndex =  hlsFilePath.lastIndexOf(".m3u8");

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

			logger.info("hls list:{}",(int)Integer.valueOf(hlsMuxer.getHlsListSize()));
			
			logger.info("hls time:{}",(int)Integer.valueOf(hlsMuxer.getHlsTime()));
			
			assertTrue(files.length < (int)Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

			//wait to let hls muxer delete ts and m3u8 file
			Thread.sleep(hlsListSize*hlsTime * 1000 + 3000);


			assertFalse(hlsFile.exists());

			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);	
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	@Test
	public void testRTSPMuxing() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		try {

			File file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			PacketSenderRunnable rtspPacketSender = new PacketSenderRunnable(null);

			String sdpDescription = rtspPacketSender.getSdpDescription(file.getAbsolutePath());
			assertNotNull(sdpDescription);
			assertTrue(sdpDescription.length() > 0 );
			int[] clientPort = new int[2];
			clientPort[0] = 23458;
			clientPort[1] = 45567;
			int[] serverPort = new int[2];
			boolean result = rtspPacketSender.prepareOutputContext(0, "127.0.0.1", clientPort, serverPort);
			assertTrue(result);

			int[] clientPort2 = new int[2];
			clientPort2[0] = 23452;
			clientPort2[1] = 44557;
			int[] serverPort2 = new int[2];
			result = rtspPacketSender.prepareOutputContext(1, "127.0.0.1", clientPort2, serverPort2);
			assertTrue(result);

			ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) applicationContext.getBean("scheduler");
			assertNotNull(scheduler);

			scheduler.scheduleAtFixedRate(rtspPacketSender, 10);

			Thread.sleep(10000);


		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testSDPCreateBug() 
	{
		String file = "target/test-classes/test.flv";
		PacketSenderRunnable packetSender = new PacketSenderRunnable(null);
		String sdpDescription = packetSender.getSdpDescription(file);
		assertNotNull(sdpDescription);
		assertTrue(sdpDescription.length() > 0 );
		System.out.println(sdpDescription);

	}
	
	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

}
