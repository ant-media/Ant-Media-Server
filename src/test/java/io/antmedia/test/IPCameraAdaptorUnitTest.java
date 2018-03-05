package io.antmedia.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
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

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.ipcamera.CameraScheduler;
import io.antmedia.ipcamera.IPCameraApplicationAdapter;

@ContextConfiguration(locations = { "test.xml" })
public class IPCameraAdaptorUnitTest extends AbstractJUnit4SpringContextTests {

	@Context
	private ServletContext servletContext;
	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(IPCameraAdaptorUnitTest.class);
	public IPCameraApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	@BeforeClass
	public static void beforeClass() {
		avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);

	}

	@Before
	public void before() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		if (app == null) {
			app = (IPCameraApplicationAdapter) applicationContext.getBean("web.handler");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

	}

	@After
	public void after() {

	}

	@Test
	public void testCameraCheckerStartStop() {

		BufferedWriter writer = null;

		// define camera according to onvif emulator parameters

		Broadcast newCam = new Broadcast("test", "127.0.0.1:8080", "admin", "admin", "rtsp://127.0.0.1:6554/test.flv",
				"ipCamera");

		List<Broadcast> cameras = new ArrayList<>();

		cameras.add(newCam);

		cameraChecker(cameras);

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean flag3 = false;
		for (CameraScheduler camScheduler : app.getCamSchedulerList()) {
			if (camScheduler.getCamera().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be false because emulator has not been started yet
				assertFalse(camScheduler.isRunning());
				flag3 = true;

			}
		}

		assertTrue(flag3);

		ProcessBuilder pb = new ProcessBuilder("/utilities/happytime-rtsp-server/runme.sh", "myArg1", "myArg2");
		Process p = null;
		try {
			p = pb.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			Thread.sleep(35000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean flag = false;
		for (CameraScheduler camScheduler : app.getCamSchedulerList()) {
			if (camScheduler.getCamera().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be true because emulater has been started
				assertTrue(camScheduler.isRunning());
				flag = true;

			}
		}

		assertTrue(flag);

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// close emulator in order to simulate cut-off
		String[] argsStop = new String[] { "/bin/bash", "-c",
				"kill -9 $(ps aux | grep 'onvifser' | awk '{print $2}')" };
		String[] argsStop2 = new String[] { "/bin/bash", "-c",
				"kill -9 $(ps aux | grep 'rtspserve' | awk '{print $2}')" };
		try {
			Process procStop = new ProcessBuilder(argsStop).start();
			Process procStop2 = new ProcessBuilder(argsStop2).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			Thread.sleep(15000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean flag2 = false;
		for (CameraScheduler camScheduler : app.getCamSchedulerList()) {
			if (camScheduler.getCamera().getIpAddr().equals(newCam.getIpAddr())) {
				// it should be false because connection is down between
				// emulator and server
				assertFalse(camScheduler.isRunning());
				flag2 = true;
			}

		}
		assertTrue(flag2);
		// after some time, emulator has been started so connection is back
		ProcessBuilder pb2 = new ProcessBuilder("/utilities/happytime-rtsp-server/runme.sh", "myArg1", "myArg2");
		Process p2 = null;
		try {
			p2 = pb2.start();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			Thread.sleep(35000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		boolean flag5 = false;
		for (CameraScheduler camScheduler : app.getCamSchedulerList()) {
			if (camScheduler.getCamera().getIpAddr().equals(newCam.getIpAddr())) {
				// after 30 seconds, adaptor should check and start because
				// thread was not working
				assertTrue(camScheduler.isRunning());
				flag5 = true;
			}

		}
		assertTrue(flag5);

		String[] argsStop3 = new String[] { "/bin/bash", "-c",
				"kill -9 $(ps aux | grep 'onvifser' | awk '{print $2}')" };
		String[] argsStop4 = new String[] { "/bin/bash", "-c",
				"kill -9 $(ps aux | grep 'rtspserve' | awk '{print $2}')" };
		try {
			Process procStop3 = new ProcessBuilder(argsStop3).start();
			Process procStop4 = new ProcessBuilder(argsStop4).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void cameraChecker(List<Broadcast> cameras) {

		app.setCameraCheckerInterval(30000);

		app.startCameras(cameras);

	}

}
