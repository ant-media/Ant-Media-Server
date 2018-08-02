package io.antmedia.test.webrtc.adaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;

import org.junit.Test;
import org.mockito.Mockito;
import org.red5.net.websocket.WebSocketConnection;

import io.antmedia.webrtc.WebSocketClientConnection;

public class WebSocketClientConnectionTest {

	
	@Test
	public void testWebSocketClientConnection() {
		WebSocketConnection wsConnection = Mockito.mock(WebSocketConnection.class);
		
		long value = (int)(Math.random() * 1000);
		
		Mockito.when(wsConnection.getId()).thenReturn(value);
		
		WebSocketClientConnection connection = new WebSocketClientConnection(wsConnection);
		
		String data = "any data";
		connection.send(data);
		
		try {
			Mockito.verify(wsConnection).send(data);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		assertEquals(String.valueOf(value), connection.getId());
	}
}
