package io.antmedia.test.webrtc.adaptor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.red5.server.api.scope.IScope;

import io.antmedia.webrtc.WebRTCMuxer;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;

public class WebRTCMuxerTest {
	
	@Test
	public void testWebRTCMuxerRegister() {
		
		IWebRTCAdaptor webRTCAdaptor = mock(IWebRTCAdaptor.class);
		WebRTCMuxer muxer = new WebRTCMuxer(null, webRTCAdaptor);
		
		IScope scope =  mock(IScope.class);
		String streamId = "steram" + (int)(Math.random()*100000);
		muxer.init(scope, streamId, 480);
		
		assertEquals(streamId, muxer.getStreamId());
		
		muxer.registerToAdaptor();
		
		verify(webRTCAdaptor).registerMuxer(streamId, muxer);
		
		muxer.writeTrailer();
		
		verify(webRTCAdaptor).unRegisterMuxer(streamId, muxer);
		
	}
	
	@Test
	public void testRegisterWebRTCClientAndSendData() {
		IWebRTCAdaptor webRTCAdaptor = mock(IWebRTCAdaptor.class);
		WebRTCMuxer muxer = new WebRTCMuxer(null, webRTCAdaptor);
		
		int clientCount = (int)(Math.random()*999);
		List<IWebRTCClient> clientList = new ArrayList<>();
		for (int i = 0; i< clientCount; i++) {
			IWebRTCClient client = mock(IWebRTCClient.class);
			clientList.add(client);
			muxer.registerWebRTCClient(client);
		}
		
		assertEquals(clientCount, muxer.getClientCount());
		assertEquals(clientCount, muxer.getClientList().size());
		
		muxer.unRegisterWebRTCClient(mock(IWebRTCClient.class));
		assertEquals(clientCount, muxer.getClientCount());
		

		byte[] videoPacket = "this is a video packet".getBytes();
		long timestamp = System.currentTimeMillis();
		byte[] videoConf = "this is a videoc conf packet".getBytes();
		
		muxer.setVideoConf(videoConf);
		muxer.sendVideoConfPacket(videoPacket, timestamp);
		
		for (IWebRTCClient iWebRTCClient : clientList) {
			verify(iWebRTCClient).sendVideoConfPacket(videoConf, videoPacket, timestamp);
		}

		boolean isKeyFrame = false;
		videoPacket = "this is another video packet".getBytes();
		timestamp = System.currentTimeMillis();
		muxer.sendVideoPacket(videoPacket, isKeyFrame, timestamp);
		
		for (IWebRTCClient iWebRTCClient : clientList) {
			verify(iWebRTCClient).sendVideoPacket(videoPacket, isKeyFrame, timestamp);
		}
		
		byte[] audioPacket = "this is a audio packet".getBytes();
		timestamp = System.currentTimeMillis();
		muxer.sendAudioPacket(audioPacket, timestamp);
		
		for (IWebRTCClient iWebRTCClient : clientList) {
			verify(iWebRTCClient).sendAudioPacket(audioPacket, timestamp);
		}
		
		muxer.writeTrailer();
		
		for (IWebRTCClient iWebRTCClient : clientList) {
			verify(iWebRTCClient).stop();
		}
		
		
		
		
	}
	
	

}
