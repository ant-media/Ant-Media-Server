package io.antmedia.test.webrtc.adaptor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.antmedia.rest.WebRTCClientStats;

public class WebRTCClientStatsTest {

	@Test
	public void testStatsClass() {
		int measuredBitrate = (int)(Math.random() * 999999);
		int sendBitrate = (int)(Math.random() * 999999);
		double videoFrameSendPeriod = (Math.random() * 9999);
		double audioFrameSendPeriod = (Math.random() * 999999);
		double videoThreadCheckInterval = (Math.random() * 9999);
		double audioThreadCheckInterval = (Math.random() * 9999);
		WebRTCClientStats clientStats = new WebRTCClientStats(measuredBitrate, sendBitrate, videoFrameSendPeriod, audioFrameSendPeriod, videoThreadCheckInterval, audioThreadCheckInterval);
	
		assertEquals(measuredBitrate, clientStats.getMeasuredBitrate());
		assertEquals(sendBitrate, clientStats.getSendBitrate());
		assertEquals(videoFrameSendPeriod, clientStats.getVideoFrameSendPeriod(), 0.02);
		assertEquals(audioFrameSendPeriod, clientStats.getAudioFrameSendPeriod(), 0.02);
		assertEquals(videoThreadCheckInterval, clientStats.getVideoThreadCheckInterval(), 0.02);
		assertEquals(audioThreadCheckInterval, clientStats.getAudioThreadCheckInterval(), 0.02);
	
	}
}
