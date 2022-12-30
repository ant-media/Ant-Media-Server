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
		int videoPacketCount = (int)(Math.random() * 999999);
		int audioPacketCount = (int)(Math.random() * 999999);
		int clientId = (int)(Math.random() * 999999);
		String clientInfo = "info";
		String clientIp = "192.168.1.1";

		WebRTCClientStats clientStats = new WebRTCClientStats(measuredBitrate, sendBitrate, videoFrameSendPeriod, audioFrameSendPeriod, 
				videoPacketCount, audioPacketCount, clientId,"notinfo", clientIp);
		clientStats.setClientInfo(clientInfo);
	
		assertEquals(measuredBitrate, clientStats.getMeasuredBitrate());
		assertEquals(sendBitrate, clientStats.getSendBitrate());
		assertEquals(videoFrameSendPeriod, clientStats.getVideoFrameSendPeriod(), 0.02);
		assertEquals(audioFrameSendPeriod, clientStats.getAudioFrameSendPeriod(), 0.02);
		assertEquals(videoPacketCount, clientStats.getVideoPacketCount());
		assertEquals(audioPacketCount, clientStats.getAudioPacketCount());
		assertEquals(clientId, clientStats.getClientId());
		assertEquals(clientInfo, clientStats.getClientInfo());

	}
}
