package io.antmedia.test;

import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.avformat_alloc_context;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.integration.RestServiceTest;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher;

@ContextConfiguration(locations = { "test.xml" })
public class StreamSchedularUnitTest extends AbstractJUnit4SpringContextTests {

	public Application app = null;
	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(StreamSchedularUnitTest.class);

	@Context
	private ServletContext servletContext;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");


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
	private AntMediaApplicationAdapter appInstance;

	@BeforeClass
	public static void beforeClass() {
		avformat.av_register_all();
		avformat.avformat_network_init();
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

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		if (app == null) {
			app = (Application) applicationContext.getBean("web.handler");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		//reset to default
		Application.enableSourceHealthUpdate = false;
		
	}

	@After
	public void after() {

		try {
			delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//reset to default
		Application.enableSourceHealthUpdate = false;

	}

	public static void delete(File file) throws IOException {

		if (file.isDirectory()) {

			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();
				// System.out.println("Directory is deleted : "
				// + file.getAbsolutePath());

			} else {

				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					delete(fileDelete);
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
					// System.out.println("Directory is deleted : "
					// + file.getAbsolutePath());
				}
			}

		} else {
			// if file, then delete it
			file.delete();
			// System.out.println("File is deleted : " +
			// file.getAbsolutePath());
		}

	}

	/*
	 *@Test This test is commented out, because isStreamAlive is controlled instead of just controlling thread aliveness in {@link testThreadStopStart}
	 */
	public void testStreamSchedular() throws InterruptedException {

		try {
			AVFormatContext inputFormatContext = new AVFormatContext();

			Broadcast newCam = new Broadcast("testSchedular", "10.2.40.63:8080", "admin", "admin",
					"rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov", "streamSource");

			StreamFetcher camScheduler = new StreamFetcher(newCam, appScope);

			camScheduler.setConnectionTimeout(10000);

			camScheduler.startStream();
			Thread.sleep(7000);

			//this should be false because this rtsp url cannot be used

			assertTrue(camScheduler.isStreamAlive());

			camScheduler.stopStream();

			Thread.sleep(5000);

			assertFalse(camScheduler.isStreamAlive());
			assertFalse(camScheduler.isThreadActive());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	@Test
	public void testStreamSchedularConnectionTimeout() throws InterruptedException {
		logger.info("running testStreamSchedularConnectionTimeout");
		try {

			AVFormatContext inputFormatContext = new AVFormatContext();

			Broadcast newCam = new Broadcast("testSchedular2", "10.2.40.64:8080", "admin", "admin",
					"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);

			newCam.setStreamId("new_cam" + (int)(Math.random()*10000));

			StreamFetcher streamScheduler = new StreamFetcher(newCam, appScope);

			assertFalse(streamScheduler.isExceptionInThread());

			streamScheduler.startStream();

			streamScheduler.setConnectionTimeout(3000);

			//this should be false because stream is not alive 
			assertFalse(streamScheduler.isStreamAlive());

			Thread.sleep(2500);

			streamScheduler.stopStream();

			Thread.sleep(3000);

			assertFalse(streamScheduler.isStreamAlive());

			assertFalse(streamScheduler.isExceptionInThread());
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		logger.info("leaving testStreamSchedularConnectionTimeout");
	}

	@Test
	public void testPrepareInput() throws InterruptedException {
		try {

			Broadcast newCam = null;

			new StreamFetcher(newCam, appScope);

			fail("it should throw exception above");
		}
		catch (Exception e) {
		}

		try {
			AVFormatContext inputFormatContext = new AVFormatContext();

			Broadcast newCam2 = new Broadcast("test", "10.2.40.63:8080", "admin", "admin", null, AntMediaApplicationAdapter.IP_CAMERA);
			newCam2.setStreamId("newcam2_" + (int)(Math.random()*10000));

			new StreamFetcher(newCam2, appScope);

			fail("it should throw exception above");
		}
		catch (Exception e) {
		}
	}
	
	
	@Test
	public void testAddCameraBug() {
		
		Result result;
		IDataStore dataStore = new MapDBStore("target/testAddCamera.db"); //applicationContext.getBean(IDataStore.BEAN_NAME);

		assertNotNull(dataStore);
		app.setDataStore(dataStore);

		//set mapdb datastore to stream fetcher because in memory datastore just have references and updating broadcst
		// object updates the reference in inmemorydatastore
		app.getStreamFetcherManager().setDatastore(dataStore);
		

		logger.info("running testAddCameraBug");
		Application.enableSourceHealthUpdate = true;
		assertNotNull(dataStore);
		
		startCameraEmulator();
		
		Broadcast newCam = new Broadcast("testAddCamera", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.IP_CAMERA);

		//add stream to data store
		dataStore.save(newCam);
		
		result=getInstance().startStreaming(newCam);
		
		//check whether answer from StreamFetcherManager is true or not after new IPCamera is added
		assertTrue(result.isSuccess());
		
		stopCameraEmulator();
	}
	
	
	public void testIPTVStream() {

		AVFormatContext inputFormatContext = avformat_alloc_context();
		int ret;
		String url = "http://kaptaniptv.com:8000/live/oguzmermer2/jNwNLK1VLk/10476.ts";

		AVDictionary optionsDictionary = new AVDictionary();


		if ((ret = avformat_open_input(inputFormatContext, url, null, optionsDictionary)) < 0) {

			byte[] data = new byte[1024];
			avutil.av_strerror(ret, data, data.length);
			logger.error("cannot open input context with error: " + new String(data, 0, data.length) + "ret value = "+ String.valueOf(ret));
			return;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			logger.error("Could not find stream information\n");
			return;
		}

		AVPacket pkt = new AVPacket();

		long startTime = System.currentTimeMillis();

		int i = 0;
		while (true) {
			ret = av_read_frame(inputFormatContext, pkt);
			if (ret < 0) {
				byte[] data = new byte[1024];
				avutil.av_strerror(ret, data, data.length);

				logger.error("cannot read frame from input context: " + new String(data, 0, data.length));	
			}

			av_packet_unref(pkt);
			i++;
			if (i % 150 == 0) {
				long duration = System.currentTimeMillis() - startTime;

				logger.info("running duration: " + (duration/1000));
			}
		}
		/*
		long duration = System.currentTimeMillis() - startTime;

		logger.info("total duration: " + (duration/1000));
		avformat_close_input(inputFormatContext);
		inputFormatContext = null;

		 */

	}

	@Test
	public void testBandwidth() {
		
		IDataStore dataStore = new MapDBStore("target/test.db"); //applicationContext.getBean(IDataStore.BEAN_NAME);

		assertNotNull(dataStore);
		app.setDataStore(dataStore);

		//set mapdb datastore to stream fetcher because in memory datastore just have references and updating broadcst
		// object updates the reference in inmemorydatastore
		app.getStreamFetcherManager().setDatastore(dataStore);
		

		logger.info("running testBandwidth");
		Application.enableSourceHealthUpdate = true;
		assertNotNull(dataStore);

		Broadcast newSource = new Broadcast("testBandwidth", "10.2.40.63:8080", "admin", "admin", "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov",
				AntMediaApplicationAdapter.STREAM_SOURCE);

		//add stream to data store
		dataStore.save(newSource);

		Broadcast newZombiSource = new Broadcast("testBandwidth", "10.2.40.63:8080", "admin", "admin", "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov",
				AntMediaApplicationAdapter.STREAM_SOURCE);

		newZombiSource.setZombi(true);
		//add second stream to datastore
		dataStore.save(newZombiSource);


		List<Broadcast> streams = new ArrayList<>();

		streams.add(newSource);
		streams.add(newZombiSource);

		//let stream fetching start
		app.getStreamFetcherManager().startStreams(streams);

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		logger.info("before first control");

		List<Broadcast> broadcastList =  dataStore.getBroadcastList(0,  20);

		Broadcast fetchedBroadcast = null;

		for (Broadcast broadcast : broadcastList) {

			logger.info("broadcast name: " + broadcast.getName() + " broadcast status :" + broadcast.getStatus() + " broadcast is zombi: " + broadcast.isZombi());
			if(broadcast.isZombi()) {

				fetchedBroadcast=broadcast;	
				break;
			}
		}

		assertNotNull(fetchedBroadcast);
		assertNotNull(fetchedBroadcast.getQuality());
		assertNotNull(fetchedBroadcast.getSpeed());
		
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		Broadcast stream = dataStore.get(newSource.getStreamId());
		assertEquals("good", stream.getQuality());	

		logger.info("speed {}" , stream.getSpeed()) ;
		


		assertTrue(1 < stream.getSpeed());

		limitNetworkInterfaceBandwidth();

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		logger.info("before second control");

		assertNotEquals("poor", dataStore.get(newSource.getStreamId()).getQuality());

		resetNetworkInterface();

		for (Broadcast broadcast: broadcastList) {
			app.getStreamFetcherManager().stopStreaming(broadcast);
		}
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//list size should be zero
		assertEquals(0, app.getStreamFetcherManager().getStreamFetcherList().size());
		logger.info("leaving testBandwidth");


	}

	private void resetNetworkInterface() {
		String[] argsReset= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a wlan0" };
		try {
			Process procStop = new ProcessBuilder(argsReset).start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] argsReset2= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a eth0" };
		try {
			Process procStop = new ProcessBuilder(argsReset2).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset3= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic0" };
		try {
			Process procStop = new ProcessBuilder(argsReset3).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset4= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic1" };
		try {		
			Process procStop = new ProcessBuilder(argsReset4).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset5 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic2" };
		try {
			Process procStop = new ProcessBuilder(argsReset5).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset6= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic3" };
		try {
			Process procStop = new ProcessBuilder(argsReset6).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset7= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic4" };
		try {
			Process procStop = new ProcessBuilder(argsReset7).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset8= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a vmnet0" };
		try {
			Process procStop = new ProcessBuilder(argsReset8).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset9= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a vmnet1" };
		try {
			Process procStop = new ProcessBuilder(argsReset9).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset10= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a em1" };
		try {
			Process procStop = new ProcessBuilder(argsReset10).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsReset11= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a em0" };
		try {
			Process procStop = new ProcessBuilder(argsReset11).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void limitNetworkInterfaceBandwidth() {
		String[] argsStop = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a wlan0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop2 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a eth0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop2).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop3 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop3).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop4 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic1 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop4).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop5 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic2 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop5).start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] argsStop6 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic3 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop6).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop7 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic4 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop7).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop8 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a vmnet0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop8).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop9 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a vmnet1 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop9).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop10 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a em1 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop10).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String[] argsStop11 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a em0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop11).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public AntMediaApplicationAdapter getInstance() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
		}
		return appInstance;
	}
	
	private void startCameraEmulator() {
		stopCameraEmulator();

		ProcessBuilder pb = new ProcessBuilder("/usr/local/onvif/runme.sh");
		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	private void stopCameraEmulator() {
		// close emulator in order to simulate cut-off
		String[] argsStop = new String[] { "/bin/bash", "-c",
		"kill -9 $(ps aux | grep 'onvifser' | awk '{print $2}')" };
		String[] argsStop2 = new String[] { "/bin/bash", "-c",
		"kill -9 $(ps aux | grep 'rtspserve' | awk '{print $2}')" };
		try {
			Process procStop = new ProcessBuilder(argsStop).start();
			Process procStop2 = new ProcessBuilder(argsStop2).start();


		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}



}


