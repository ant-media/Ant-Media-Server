package io.antmedia.test.webrtc.datachannel;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.antmedia.webrtc.datachannel.event.AudioLevelEvent;
import io.antmedia.webrtc.datachannel.event.RoleEvent;

public class EventTypeTest {
	
	@Test
	public void testAudiLevel() {
		AudioLevelEvent audioLevel = new AudioLevelEvent("stream1");
		assertEquals("stream1", audioLevel.getStreamId());
		
		audioLevel.setAudioLevel(0);
		assertEquals(0, audioLevel.getAudioLevel());
		
		audioLevel.setAudioLevel(127);
		assertEquals(127, audioLevel.getAudioLevel());
		
		audioLevel.setStreamId("stream2");
		assertEquals("stream2", audioLevel.getStreamId());
	}
	
	@Test
	public void testRoleEvent() {
		RoleEvent roleEvent = new RoleEvent("stream1");
		roleEvent.setRole("publisher");
		assertEquals("publisher", roleEvent.getRole());
		
		roleEvent.setRole("subscriber");
		assertEquals("subscriber", roleEvent.getRole());
		
		
		
	}

}
