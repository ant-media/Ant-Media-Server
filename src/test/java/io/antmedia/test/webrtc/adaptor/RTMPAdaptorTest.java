package io.antmedia.test.webrtc.adaptor;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.red5.net.websocket.WebSocketConnection;
import org.springframework.context.ApplicationContext;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.websocket.WebSocketCommunityHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import javax.websocket.Session;

public class RTMPAdaptorTest {

	@Before
	public void setup() {

	}

	
	@Test
	public void testOnAddStream() {
		
		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandlerReal = new WebSocketCommunityHandler() {

			@Override
			public ApplicationContext getAppContext() {
				return null;
			}
		};

		WebSocketCommunityHandler webSocketHandler = spy(webSocketHandlerReal);

		RTMPAdaptor adaptorReal = new RTMPAdaptor(recorder, webSocketHandler);
		RTMPAdaptor rtmpAdaptor = spy(adaptorReal);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		rtmpAdaptor.setSession(session);
		
		MediaStream stream = mock(MediaStream.class);
		rtmpAdaptor.onAddStream(stream);
		
		verify(webSocketHandler).sendPublishStartedMessage(streamId, session);
	}

	@Test
	public void testStartandStop() {

		FFmpegFrameRecorder recorder = mock(FFmpegFrameRecorder.class);

		WebSocketCommunityHandler webSocketHandlerReal = new WebSocketCommunityHandler() {

			@Override
			public ApplicationContext getAppContext() {
				return null;
			}
		};

		WebSocketCommunityHandler webSocketHandler = spy(webSocketHandlerReal);

		RTMPAdaptor adaptorReal = new RTMPAdaptor(recorder, webSocketHandler);
		RTMPAdaptor rtmpAdaptor = spy(adaptorReal);

		String streamId = "stramId" + (int)(Math.random()*10000);
		rtmpAdaptor.setStreamId(streamId);
		Session session = mock(Session.class);
		rtmpAdaptor.setSession(session);

		doReturn(mock(PeerConnectionFactory.class)).when(rtmpAdaptor).createPeerConnectionFactory();

		rtmpAdaptor.start();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
			rtmpAdaptor.isStarted()
		);

		verify(webSocketHandler).sendStartMessage(streamId, session);


		rtmpAdaptor.stop();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> 
			rtmpAdaptor.getSignallingExecutor().isShutdown()
		);
		
		verify(webSocketHandler).sendPublishFinishedMessage(streamId, session);

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
