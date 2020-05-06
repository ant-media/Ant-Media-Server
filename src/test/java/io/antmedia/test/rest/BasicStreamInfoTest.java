package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.antmedia.rest.model.BasicStreamInfo;
import io.antmedia.webrtc.VideoCodec;

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
