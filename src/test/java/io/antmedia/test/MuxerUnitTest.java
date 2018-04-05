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

import org.apache.mina.core.buffer.IoBuffer;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.io.ITag;
import org.red5.io.flv.impl.FLVReader;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.WebScope;
import org.red5.server.service.mp4.impl.MP4Service;
import org.red5.server.stream.RemoteBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import com.antstreaming.rtsp.PacketSenderRunnable;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.integration.MuxingTest;
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

	protected WebScope appScope;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	//TODO: rtsp ile yayın yapılacak, rtmp ile hls ile ve rtsp ile izlenecek
	//TODO: rtsp yayını bitince mp4 dosyası kontrol edilecek
	//TODO: desteklenmeyen bir codec ile rtsp datası gelince muxer bunu kontrol edecek


	@BeforeClass
	public static void beforeClass() {
		avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.	AV_LOG_ERROR);

		
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
	}

	@After
	public void after() {
	
		try {
			delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public static void delete(File file)
			throws IOException{

		if(file.isDirectory()){

			//directory is empty, then delete it
			if(file.list().length==0){

				file.delete();
				//System.out.println("Directory is deleted : " 
				//	+ file.getAbsolutePath());

			}else{

				//list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					//construct the file structure
					File fileDelete = new File(file, temp);

					//recursive delete
					delete(fileDelete);
				}

				//check the directory again, if empty then delete it
				if(file.list().length==0){
					file.delete();
					//System.out.println("Directory is deleted : " 
					//		+ file.getAbsolutePath());
				}
			}

		}else{
			//if file, then delete it
			file.delete();
			//System.out.println("File is deleted : " + file.getAbsolutePath());
		}
	}

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

	//TODO: when prepare fails, there is memorly leak or thread leak?


	@Test
	public void testMuxingSimultaneously()  {

		MuxAdaptor muxAdaptor = new MuxAdaptor(null);
		muxAdaptor.setMp4MuxingEnabled(true,false);
		muxAdaptor.setHLSMuxingEnabled(true);
		muxAdaptor.setHLSFilesDeleteOnExit(false);

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

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
	public void testRemoteBroadcastStreamStartStop() 
	{
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
		assertNotNull(scheduler);
		
		RemoteBroadcastStream rbs = new RemoteBroadcastStream();
		rbs.setRemoteStreamUrl("src/test/resources/test_short.flv");
		//boolean containsScheduler = thisScope.getParent().getContext().getApplicationContext().containsBean("rtmpScheduler");
		//if (containsScheduler) 
		
		rbs.setScheduler(scheduler);
		
		rbs.setName(UUID.randomUUID().toString());
		rbs.setConnection(null);
		rbs.setScope(appScope);
		
		rbs.start();
		
	
		
		while(scheduler.getScheduledJobNames().size() != 1) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	
		
		assertEquals(0, rbs.getReferenceCountInQueue());
		
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

			List<MuxAdaptor> muxAdaptorList = new ArrayList<MuxAdaptor>();
			for (int j = 0; j < 20; j++) {
				MuxAdaptor muxAdaptor = new MuxAdaptor(null);
				muxAdaptor.setMp4MuxingEnabled(true, true);
				muxAdaptor.setHLSMuxingEnabled(false);
				muxAdaptorList.add(muxAdaptor);
			}
			{

				File file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
				final FLVReader flvReader = new FLVReader(file);

				logger.debug("f path: " + file.getAbsolutePath());
				assertTrue(file.exists());

				for (Iterator<MuxAdaptor> iterator = muxAdaptorList.iterator(); iterator.hasNext();) {
					MuxAdaptor muxAdaptor = (MuxAdaptor) iterator.next();
					boolean result = muxAdaptor.init(appScope, "test" + (int)(Math.random() * 1000), false);
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
					while (!muxAdaptor.isRecording()) {
						Thread.sleep(50);
					}
				}
				
				//Thread.sleep(5000);


				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					muxAdaptor.stop();
				}


				flvReader.close();
				
				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					while (muxAdaptor.isRecording()) {
						Thread.sleep(50);
					}
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


		}
		catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}

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
		
		Application.resetFields();
		
		assertEquals(Application.id, null);
		assertEquals(Application.file, null);
		assertEquals(Application.duration, 0);
		
		File file = testMp4Muxing("test_test");
		assertEquals("test_test.mp4", file.getName());
		
		assertEquals(Application.id, "test_test");
		assertEquals(Application.file.getName(), "test_test.mp4");
		assertNotEquals(Application.duration, 0L);
		
		Application.resetFields();
		
		assertEquals(Application.id, null);
		assertEquals(Application.file, null);
		assertEquals(Application.duration, 0);
		
		file = testMp4Muxing("test_test");
		assertEquals("test_test_1.mp4", file.getName());
		
		assertEquals(Application.id, "test_test");
		assertEquals(Application.file.getName(), "test_test_1.mp4");
		assertNotEquals(Application.duration, 0L);
	
		Application.resetFields();
		
		assertEquals(Application.id, null);
		assertEquals(Application.file, null);
		assertEquals(Application.duration, 0);
		
		file = testMp4Muxing("test_test");
		assertEquals("test_test_2.mp4", file.getName());
		
		assertEquals(Application.id, "test_test");
		assertEquals(Application.file.getName(), "test_test_2.mp4");
		assertNotEquals(Application.duration, 0L);
	}


	@Test
	public void testBaseStreamFileServiceBug() {
		MP4Service mp4Service = new MP4Service();

		String fileName = mp4Service.prepareFilename("mp4:1");
		assertEquals(fileName, "1.mp4");

		fileName = mp4Service.prepareFilename("mp4:12");
		assertEquals(fileName, "12.mp4");

		fileName = mp4Service.prepareFilename("mp4:123");
		assertEquals(fileName, "123.mp4");

		fileName = mp4Service.prepareFilename("mp4:1234");
		assertEquals(fileName, "1234.mp4");

		fileName = mp4Service.prepareFilename("mp4:12345");
		assertEquals(fileName, "12345.mp4");

		fileName = mp4Service.prepareFilename("mp4:123456");
		assertEquals(fileName, "123456.mp4");

		fileName = mp4Service.prepareFilename("mp4:1.mp4");
		assertEquals(fileName, "1.mp4");

		fileName = mp4Service.prepareFilename("mp4:123456789.mp4");
		assertEquals(fileName, "123456789.mp4");

	}
	
	@Test
	public void testVideoServiceEndpoint() {
		AntMediaApplicationAdapter appAdaptor = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		assertNotNull(appAdaptor);
		
		VideoServiceEndpoint endpointService = Mockito.mock(VideoServiceEndpoint.class);
		SocialEndpointCredentials credentials = Mockito.mock(SocialEndpointCredentials.class);
		String id = "" + (Math.random()*10000);
		Mockito.when(credentials.getId()).thenReturn(id);
				
		appAdaptor.getVideoServiceEndpoints().add(endpointService);
		
		Mockito.when(endpointService.getCredentials()).thenReturn(credentials);
		
		String id2 = "" + (Math.random()*10000);
		VideoServiceEndpoint endpointService2 = Mockito.mock(VideoServiceEndpoint.class);
		SocialEndpointCredentials credentials2 = Mockito.mock(SocialEndpointCredentials.class);
		Mockito.when(credentials2.getId()).thenReturn(id2);
		Mockito.when(endpointService2.getCredentials()).thenReturn(credentials2);
		
		appAdaptor.getVideoServiceEndpoints().add(endpointService2);
		
		VideoServiceEndpoint videoServiceEndPoint = appAdaptor.getVideoServiceEndPoint(id);
		assertNotNull(videoServiceEndPoint);
		assertEquals(endpointService, videoServiceEndPoint);
		
		videoServiceEndPoint = appAdaptor.getVideoServiceEndPoint(id2);
		assertNotNull(videoServiceEndPoint);
		assertEquals(endpointService2, videoServiceEndPoint);
		
		
	}

	@Test
	public void testMp4MuxingAndNotifyCallback() {
		Application.resetFields();
		assertEquals(Application.notifyHookAction, null);
		assertEquals(Application.notitfyURL, null);
		assertEquals(Application.notifyId, null);
		assertEquals(Application.notifyStreamName, null);
		assertEquals(Application.notifyCategory, null);
		assertEquals(Application.notifyVodName, null);
		
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
		assertEquals(Application.duration, 697132L);
		
		broadcast = appAdaptor.getDataStore().get(streamId);
		assertEquals((long)broadcast.getDuration(), 697132L);
		
		assertEquals(Application.notifyHookAction, Application.HOOK_ACTION_VOD_READY);
		assertEquals(Application.notitfyURL, hookUrl);
		assertEquals(Application.notifyId, streamId);
		assertEquals(Application.notifyStreamName, null);
		assertEquals(Application.notifyCategory, null);
		assertEquals(Application.notifyVodName, streamId);
		
		
		
		Application.resetFields();
		//test with same id again
		testMp4Muxing(streamId, true, true);
		
		assertEquals(Application.id, streamId);
		assertEquals(Application.file.getName(), streamId + "_1.mp4");
		assertEquals(Application.duration, 10080L);
		
		broadcast = appAdaptor.getDataStore().get(streamId);
		assertEquals((long)broadcast.getDuration(), 10080L);
		
		assertEquals(Application.notifyHookAction, Application.HOOK_ACTION_VOD_READY);
		assertEquals(Application.notitfyURL, hookUrl);
		assertEquals(Application.notifyId, streamId);
		assertEquals(Application.notifyStreamName, null);
		assertEquals(Application.notifyCategory, null);
		assertEquals(Application.notifyVodName, streamId + "_1"); //vod name must be changed
	}

	public void testMp4Muxing() {
		testMp4Muxing("lkdlfkdlfkdlfk");
	}
	
	
	public File testMp4Muxing(String name) {
		return testMp4Muxing(name, false, true);
	}
	

	public File testMp4Muxing(String name, boolean shortVersion, boolean checkDuration) {

		MuxAdaptor muxAdaptor = new MuxAdaptor(null);
		muxAdaptor.setMp4MuxingEnabled(true, false);

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			
			//by default, stream source job is schedule
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
		return null;
	}
	
	@Test
	public void testMp4MuxingSubtitledVideo() {

		MuxAdaptor muxAdaptor = new MuxAdaptor(null);
		muxAdaptor.setMp4MuxingEnabled(true, false);
		muxAdaptor.setHLSMuxingEnabled(true);
		muxAdaptor.setHLSFilesDeleteOnExit(false);

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

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

			assertEquals(scheduler.getScheduledJobNames().size(), 1);
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
		MuxAdaptor muxAdaptor = new MuxAdaptor(null);
		muxAdaptor.setHLSMuxingEnabled(true);
		
		muxAdaptor.setMp4MuxingEnabled(false, false);
		int hlsListSize = 5;
		muxAdaptor.setHlsListSize(hlsListSize + "");
		int hlsTime = 2;
		muxAdaptor.setHlsTime(hlsTime + "");


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		appScope.createChildScope("child");
		
		IScope childScope = appScope.getScope("child");

		childScope.createChildScope("child2");
		IScope childScope2 = childScope.getScope("child2");
		
		childScope2.createChildScope("child3");
		IScope childScope3 = childScope2.getScope("child3");

		File file = null;
		try {

			QuartzSchedulingService scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
			assertNotNull(scheduler);
			//assertEquals(scheduler.getScheduledJobNames().size(),1);

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

		MuxAdaptor muxAdaptor = new MuxAdaptor(null);
		muxAdaptor.setHLSMuxingEnabled(true);
		
		muxAdaptor.setMp4MuxingEnabled(false, false);
		int hlsListSize = 5;
		muxAdaptor.setHlsListSize(hlsListSize + "");
		int hlsTime = 2;
		muxAdaptor.setHlsTime(hlsTime + "");


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

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
	public void testHLSMuxingWithSubtitle()  {

		//av_log_set_level (40);

		MuxAdaptor muxAdaptor = new MuxAdaptor(null);
		muxAdaptor.setHLSMuxingEnabled(true);
		
		muxAdaptor.setMp4MuxingEnabled(false, false);
		int hlsListSize = 5;
		muxAdaptor.setHlsListSize(hlsListSize + "");
		int hlsTime = 2;
		muxAdaptor.setHlsTime(hlsTime + "");


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

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

}
