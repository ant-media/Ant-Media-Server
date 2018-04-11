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
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;
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
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.integration.RestServiceTest;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.streamsource.StreamFetcher;

@ContextConfiguration(locations = { "test.xml" })
public class StreamSchedularUnitTest extends AbstractJUnit4SpringContextTests {

	public AntMediaApplicationAdapter app = null;
	private WebScope appScope;
	private MapDBStore dbStore;
	private ApplicationContext appCtx;
	private IScope scope;
	protected static Logger logger = LoggerFactory.getLogger(StreamSchedularUnitTest.class);
	private BroadcastRestService rest;

	@Context
	private ServletContext servletContext;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");


	}

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
			app = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
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

	@Test
	public void testStreamSchedular() throws InterruptedException {

		AVFormatContext inputFormatContext = new AVFormatContext();

		Broadcast newCam = new Broadcast("testSchedular", "10.2.40.63:8080", "admin", "admin",
				"rtsp://10.2.40.63:8554/live1.sdp", "ipCamera");

		StreamFetcher camScheduler = new StreamFetcher(newCam);
		
		camScheduler.startStream();

		//this should be false becase this rtsp url cannot be used
		
		assertFalse(camScheduler.isStreamAlive());

		camScheduler.stopStream();

		Thread.sleep(5000);

		assertFalse(camScheduler.isStreamAlive());

	}

	@Test
	public void testStreamSchedularConnectionTimeout() throws InterruptedException {

		long size = 1232;

		AVFormatContext inputFormatContext = new AVFormatContext();

		Broadcast newCam = new Broadcast("testSchedular", "10.2.40.63:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", "ipCamera");



		StreamFetcher streamScheduler = new StreamFetcher(newCam);

		assertFalse(streamScheduler.isExceptionInThread());

		streamScheduler.startStream();

		streamScheduler.setConnectionTimeout(3000);

		//this should be false because stream is not alive 
		assertFalse(streamScheduler.isStreamAlive());

		streamScheduler.stopStream();

		Thread.sleep(6000);

		assertFalse(streamScheduler.isStreamAlive());

		assertFalse(streamScheduler.isExceptionInThread());
	}

	@Test
	public void testPrepareInput() throws InterruptedException {

		long size = 1232;

		AVFormatContext inputFormatContext = new AVFormatContext();

		Broadcast newCam = null;

		StreamFetcher streamScheduler = new StreamFetcher(newCam);

		assertFalse(streamScheduler.prepareInput(inputFormatContext));

		Broadcast newCam2 = new Broadcast("test", "10.2.40.63:8080", "admin", "admin", null, "ipCamera");

		StreamFetcher streamScheduler2 = new StreamFetcher(newCam2);

		assertFalse(streamScheduler2.prepareInput(inputFormatContext));

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

		RestServiceTest restService = new RestServiceTest();

		Broadcast newSource = new Broadcast("test1", "10.2.40.63:8080", "admin", "admin", "rtsp://184.72.239.149/vod/mp4:BigBuckBunny_115k.mov",
				"streamSource");

		restService.save(newSource);

		List<Broadcast> streams = new ArrayList<>();

		streams.add(newSource);

		app.getSources().startStreams(streams);




		try {
			Thread.sleep(20000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.info("before first control");
		
		assertEquals("good", restService.getBroadcast(newSource.getStreamId()).getQuality());	
		
		assertTrue(1 < restService.getBroadcast(newSource.getStreamId()).getSpeed());
		
	



		/*

		ProcessBuilder pb = new ProcessBuilder("/usr/local/wondershaper/limit.sh" , "myArg1", "myArg2");
		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		 */	

		String[] argsStop = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a wlan0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop2 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a eth0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop2).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop3 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop3).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop4 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic1 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop4).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop5 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic2 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop5).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] argsStop6 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic3 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop6).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop7 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a nic4 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop7).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop8 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a vmnet0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop8).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop9 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a vmnet1 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop9).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop10 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a em1 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop10).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsStop11 = new String[] { "/bin/bash", "-c",
		"sudo wondershaper -a em0 -d 100 -u 100" };
		try {
			Process procStop = new ProcessBuilder(argsStop11).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		


		try {
			Thread.sleep(40000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.info("before second control");
		
		assertNotEquals("poor", restService.getBroadcast(newSource.getStreamId()).getQuality());

	
		/*
		ProcessBuilder pb2 = new ProcessBuilder("/usr/local/wondershaper/reset.sh" , "myArg1", "myArg2");
		Process p2 = null;
		try {
			p2 = pb2.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		 */
		
		String[] argsReset= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a wlan0" };
		try {
			Process procStop = new ProcessBuilder(argsReset).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String[] argsReset2= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a eth0" };
		try {
			Process procStop = new ProcessBuilder(argsReset2).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset3= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic0" };
		try {
			Process procStop = new ProcessBuilder(argsReset3).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset4= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic1" };
		try {
			Process procStop = new ProcessBuilder(argsReset4).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset5= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic2" };
		try {
			Process procStop = new ProcessBuilder(argsReset5).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset6= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic3" };
		try {
			Process procStop = new ProcessBuilder(argsReset6).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset7= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a nic4" };
		try {
			Process procStop = new ProcessBuilder(argsReset7).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset8= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a vmnet0" };
		try {
			Process procStop = new ProcessBuilder(argsReset8).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset9= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a vmnet1" };
		try {
			Process procStop = new ProcessBuilder(argsReset9).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset10= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a em1" };
		try {
			Process procStop = new ProcessBuilder(argsReset10).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String[] argsReset11= new String[] { "/bin/bash", "-c",
		"sudo wondershaper -c -a em0" };
		try {
			Process procStop = new ProcessBuilder(argsReset11).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}




}


