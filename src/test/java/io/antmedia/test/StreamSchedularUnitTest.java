package io.antmedia.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.streamsource.StreamFetcher;

@ContextConfiguration(locations = { "test.xml" })
public class StreamSchedularUnitTest extends AbstractJUnit4SpringContextTests {

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

		long size = 1232;

		AVFormatContext inputFormatContext = new AVFormatContext();

		Broadcast newCam = new Broadcast("testSchedular", "10.2.40.63:8080", "admin", "admin",
				"rtsp://10.2.40.63:8554/live1.sdp", "ipCamera");

		StreamFetcher camScheduler = new StreamFetcher(newCam);
		camScheduler.startStream();

		assertTrue(camScheduler.isRunning());

		camScheduler.stopStream();

		Thread.sleep(5000);
		
		assertFalse(camScheduler.isRunning());

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

		assertTrue(streamScheduler.isRunning());

		streamScheduler.stopStream();

		Thread.sleep(5000);
		
		assertFalse(streamScheduler.isRunning());

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

}
