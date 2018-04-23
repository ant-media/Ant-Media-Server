package io.antmedia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.streamsource.StreamFetcher;

@ContextConfiguration(locations = { "test.xml" })
public class StreamFetcherUnitTest extends AbstractJUnit4SpringContextTests {

	@Context
	private ServletContext servletContext;
	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(StreamFetcherUnitTest.class);
	public AntMediaApplicationAdapter app = null;


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

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		if (app == null) 
		{
			
			app = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}



	}

	@After
	public void after() {
		appScope = null;
		app = null;
	}


	@Test
	public void testBugUpdateStreamFetcherStatus() {

		logger.info("starting testBugUpdateStreamFetcherStatus");
		//create ip camera broadcast
		IDataStore dataStore = new MapDBStore("target/testbug.db"); //applicationContext.getBean(IDataStore.BEAN_NAME);

		assertNotNull(dataStore);
		app.setDataStore(dataStore);

		//set mapdb datastore to stream fetcher because in memory datastore just have references and updating broadcst
		// object updates the reference in inmemorydatastore
		app.getStreamFetcherManager().setDatastore(dataStore);
		
		List<StreamFetcher> camSchedulerList = app.getStreamFetcherManager().getCamSchedulerList();
		for (Iterator iterator = camSchedulerList.iterator(); iterator.hasNext();) {
			StreamFetcher streamFetcher = (StreamFetcher) iterator.next();
			app.getStreamFetcherManager().stopStreaming(streamFetcher.getStream());
			
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		
		assertEquals(0, app.getStreamFetcherManager().getCamSchedulerList().size());

		//save it data store
		Broadcast newCam = new Broadcast("testOnvif", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				"ipCamera");
		String id = dataStore.save(newCam);


		//set status to broadcasting
		dataStore.updateStatus(id, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		Broadcast broadcast = dataStore.get(id);
		logger.info("broadcast stream id {}" , id);
		assertEquals(broadcast.getStatus(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

		//start StreamFetcher
		app.getStreamFetcherManager().startStreams(Arrays.asList(broadcast));

		assertEquals(1, app.getStreamFetcherManager().getCamSchedulerList().size());

		//wait 5seconds because connectivity time out is 4sec by default
		try {
			Thread.sleep(7000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		//check that it is not started
		boolean flag3 = false;
		for (StreamFetcher camScheduler : app.getStreamFetcherManager().getCamSchedulerList()) {
			if (camScheduler.getStream().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be false because emulator has not been started yet
				assertFalse(camScheduler.isStreamAlive());
				flag3 = true;

			}
		}

		assertTrue(flag3);

		//check that broadcast status in datastore in finished or not broadcasting
		broadcast = dataStore.get(id);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast.getStatus());

		app.getStreamFetcherManager().stopStreaming(newCam);
		assertEquals(0, app.getStreamFetcherManager().getCamSchedulerList().size());

		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		logger.info("leaving testBugUpdateStreamFetcherStatus");
		
	}

	@Test
	public void testThreadStopStart() {
		
		logger.info("starting testThreadStopStart");
		
		try {

			// start stream fetcher

			Broadcast newCam = new Broadcast("onvifCam1", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
					"ipCamera");
			assertNotNull(newCam.getStreamUrl());
			
			try {
				newCam.setStreamId((int)Math.random()*100000 + "");
			} catch (Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
			
			assertNotNull(newCam.getStreamId());
			
			StreamFetcher fetcher = new StreamFetcher(newCam);


			ProcessBuilder pb = new ProcessBuilder("/usr/local/onvif/runme.sh");
			Process p = null;
			try {
				p = pb.start();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			// thread start 
			fetcher.startStream();

			Thread.sleep(10000);

			//check that thread is running
			assertTrue(fetcher.isThreadActive());
			assertTrue(fetcher.isStreamAlive());


			//stop thread
			fetcher.stopStream();

			Thread.sleep(6000);

			assertFalse(fetcher.isStreamAlive());
			assertFalse(fetcher.isThreadActive());

			//change the flag that shows thread is still running
			fetcher.setThreadActive(true);

			//start thread
			fetcher.startStream();

			Thread.sleep(8000);
			//check that thread is not started because thread active is true
			assertFalse(fetcher.isStreamAlive());
			assertTrue(fetcher.isThreadActive());


			logger.info("Change the flag that previous thread is stopped");
			//change the flag that previous thread is stopped
			fetcher.setThreadActive(false);

			//wait a little
			Thread.sleep(8000);

			//check that thread is started
			assertTrue(fetcher.isStreamAlive());
			assertTrue(fetcher.isThreadActive());

			fetcher.stopStream();

			Thread.sleep(6000);
			assertFalse(fetcher.isStreamAlive());
			assertFalse(fetcher.isThreadActive());

			stopCameraEmulator();

			Thread.sleep(3000);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		logger.info("leaving testThreadStopStart");
		

	}

	@Test
	public void testCameraCheckerStartStop() {


		logger.info("starting testCameraCheckerStartStop");
		
		// define camera according to onvif emulator parameters

		Broadcast newCam = new Broadcast("testOnvif", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				"ipCamera");

		try {
			newCam.setStreamId("stream_" + (int)(Math.random() * 10000));
		} catch (Exception e2) {
			e2.printStackTrace();
			fail(e2.getMessage());
		}

		List<Broadcast> cameras = new ArrayList<>();

		cameras.add(newCam);
		
		List<StreamFetcher> camSchedulerList = app.getStreamFetcherManager().getCamSchedulerList();
		for (Iterator iterator = camSchedulerList.iterator(); iterator.hasNext();) {
			StreamFetcher streamFetcher = (StreamFetcher) iterator.next();
			app.getStreamFetcherManager().stopStreaming(streamFetcher.getStream());
			
		}
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}
		
		assertEquals(0, app.getStreamFetcherManager().getCamSchedulerList().size());


		//sets stream fetcher configuration, it checks streams in every 30sec
		app.getStreamFetcherManager().setStreamCheckerInterval(30000);
		logger.info("starting new streams in testCameraCheckerStartStop");
		app.getStreamFetcherManager().startStreams(cameras);
		logger.info("started new streams in testCameraCheckerStartStop");
		

		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean flag3 = false;
		for (StreamFetcher camScheduler : app.getStreamFetcherManager().getCamSchedulerList()) {
			if (camScheduler.getStream().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be false because emulator has not been started yet
				assertFalse(camScheduler.isStreamAlive());
				assertFalse(camScheduler.isThreadActive());
				flag3 = true;

			}
		}

		assertTrue(flag3);

		ProcessBuilder pb = new ProcessBuilder("/usr/local/onvif/runme.sh");
		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		logger.warn("emulater has been started");

		try {

			//wait more than 30sec to make sure scheduler start the stream again
			Thread.sleep(35000);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		boolean flag = false;
		for (StreamFetcher camScheduler : app.getStreamFetcherManager().getCamSchedulerList()) {
			if (camScheduler.getStream().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be true because emulator has been started
				assertTrue(camScheduler.isStreamAlive());
				flag = true;
			}
		}

		assertTrue(flag);

		stopCameraEmulator();

		try {
			//waiting 5 sec is ok. Because stream is not alive if last packet time is older than 3 secs.
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		boolean flag2 = false;
		for (StreamFetcher camScheduler : app.getStreamFetcherManager().getCamSchedulerList()) {
			if (camScheduler.getStream().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be false because connection is down between
				// emulator and server
				assertFalse(camScheduler.isStreamAlive());
				flag2 = true;
			}

		}
		assertTrue(flag2);
		// after some time, emulator has been started so connection is back
		ProcessBuilder pb2 = new ProcessBuilder("/usr/local/onvif/runme.sh", "myArg1", "myArg2");
		Process p2 = null;
		try {
			p2 = pb2.start();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			//wait more than 30sec to make sure stream is started again
			Thread.sleep(35000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		boolean flag5 = false;
		for (StreamFetcher camScheduler : app.getStreamFetcherManager().getCamSchedulerList()) {
			if (camScheduler.getStream().getIpAddr().equals(newCam.getIpAddr())) {
				// after 30 seconds, adaptor should check and start because
				// thread was not working
				assertTrue(camScheduler.isStreamAlive());
				flag5 = true;
			}

		}
		assertTrue(flag5);
		stopCameraEmulator();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		app.getStreamFetcherManager().stopStreaming(newCam);
		assertEquals(0, app.getStreamFetcherManager().getCamSchedulerList().size());

		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		logger.info("leaving testCameraCheckerStartStop");
		

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
	}

	/*
	public void cameraChecker(List<Broadcast> cameras, int interval) {

	}
	*/

}
