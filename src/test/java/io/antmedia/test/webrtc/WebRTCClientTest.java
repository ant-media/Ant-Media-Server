package io.antmedia.test.webrtc;


import org.junit.Test;
import org.red5.net.websocket.WebSocketConnection;

import io.antmedia.webrtc.WebRTCAdaptor;
import io.antmedia.webrtc.WebRTCClient;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class WebRTCClientTest {

	//call stop function with out start and it will fail

	@Test
	public void testWebRTCClientStartandStop() {

		try {

			System.out.println("java.library.path: " + System.getProperty("java.library.path"));
			WebSocketConnection socketConnection = mock(WebSocketConnection.class);
			String streamId = "" + (int)(Math.random() * 100000);

			WebRTCAdaptor webRTCAdaptor = mock(WebRTCAdaptor.class);

			WebRTCClient client1 = new WebRTCClient(socketConnection, streamId); 

			assertFalse(client1.isInitialized());
			assertFalse(client1.isStreaming());
			
			client1.start();

			Thread.sleep(500);

			assertTrue(client1.isInitialized());
			assertFalse(client1.isStreaming());
			
			client1.stop();
			
			Thread.sleep(500);
			
			assertFalse(client1.isInitialized());
			assertFalse(client1.isStreaming());

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
