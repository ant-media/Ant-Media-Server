package io.antmedia.test.rest;

import io.antmedia.rest.model.BasicStreamInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.antmedia.webrtc.VideoCodec;
import org.junit.jupiter.api.Test;

public class BasicStreamInfoTest {

	@Test
	public void testBasicStreamInfo() {
		BasicStreamInfo streamInfo = new BasicStreamInfo(720, 1080, 3000000, 128000, VideoCodec.H264);
		
		assertEquals(1080, streamInfo.getVideoWidth());
		
		assertEquals(720, streamInfo.getVideoHeight());
		
		assertEquals(3000000, streamInfo.getVideoBitrate());
		
		assertEquals(128000, streamInfo.getAudioBitrate());
		
		assertEquals(VideoCodec.H264, streamInfo.getVideoCodec());
	}
}
