package io.antmedia.test.webrtc.adaptor;

import org.junit.Before;
import org.junit.Test;
import org.red5.net.websocket.WebSocketConnection;
import org.springframework.context.ApplicationContext;
import org.webrtc.MediaStream;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import javax.websocket.Session;

public class RTMPAdaptorTest {
	
	@Before
	public void setup() {
		
	}
	
	
	@Test
	public void testGetFileFormat() {

		
		FFmpegFrameRecorder recorder = WebSocketCommunityHandler.initRecorder("rtmp://test");
		
		assertEquals("flv", recorder.getFormat());
	}
	
	@Test
	public void testNoAudioNoVideoInStream() {
		
		try {
			
			WebSocketCommunityHandler handler = mock(WebSocketCommunityHandler.class);
			
			RTMPAdaptor rtmpAdaptor = new RTMPAdaptor(null, handler);
			
			MediaStream stream = mock(MediaStream.class);
			
			Session session = mock(Session.class);
			
			rtmpAdaptor.setSession(session);
			
			rtmpAdaptor.onAddStream(stream);
			
			
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			
		}
		
	}

}
