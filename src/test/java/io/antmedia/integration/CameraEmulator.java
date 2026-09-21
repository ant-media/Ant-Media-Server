package io.antmedia.integration;

import java.io.IOException;

/**
 * The ONVIF camera simulator the RTSP tests pull from. It serves ONVIF on 127.0.0.1:8080 and RTSP on
 * 127.0.0.1:6554. Installed at /usr/local/onvif on the CI runner, so anything using this only runs there.
 */
public class CameraEmulator {

	/** runme.sh returns before the emulator accepts connections, and it has no readiness signal. */
	private static final long READY_WAIT_MS = 5000;

	private CameraEmulator() {
		//static only
	}

	public static void start() {
		stop();

		try {
			new ProcessBuilder("/usr/local/onvif/runme.sh").start();
			Thread.sleep(READY_WAIT_MS);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/** Kills the emulator, which is also how the tests simulate a camera being cut off. */
	public static void stop() {
		String[] stopOnvif = { "/bin/bash", "-c", "kill -9 $(ps aux | grep 'onvifser' | awk '{print $2}')" };
		String[] stopRtsp = { "/bin/bash", "-c", "kill -9 $(ps aux | grep 'rtspserve' | awk '{print $2}')" };

		try {
			new ProcessBuilder(stopOnvif).start();
			new ProcessBuilder(stopRtsp).start();
			Thread.sleep(2000);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
