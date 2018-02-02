package io.antmedia.test.webrtc.adaptor;

import org.junit.Before;
import org.junit.Test;
import org.red5.net.websocket.WebSocketConnection;
import org.webrtc.MediaStream;

import io.antmedia.webrtc.adaptor.RTMPAdaptor;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class RTMPAdaptorTest {
	
	@Before
	public void setup() {
		
	}
	
	@Test
	public void testNoAudioNoVideoInStream() {
		
		try {
			RTMPAdaptor rtmpAdaptor = new RTMPAdaptor(null);
			
			MediaStream stream = mock(MediaStream.class);
			
			WebSocketConnection conn = mock(WebSocketConnection.class);
			
			rtmpAdaptor.setWsConnection(conn);
			
			rtmpAdaptor.onAddStream(stream);
			
			
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			
		}
		
	}

}
