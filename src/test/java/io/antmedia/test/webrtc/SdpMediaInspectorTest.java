package io.antmedia.test.webrtc;

import static io.antmedia.whip.SdpMediaInspector.AUDIO;
import static io.antmedia.whip.SdpMediaInspector.VIDEO;
import static io.antmedia.whip.SdpMediaInspector.isMediaEnabled;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SdpMediaInspectorTest {

	private static final String SESSION = "v=0\n"
			+ "o=- 4611731400430051336 2 IN IP4 127.0.0.1\n"
			+ "s=-\n"
			+ "t=0 0\n";

	private static final String AUDIO_SECTION = "m=audio 9 UDP/TLS/RTP/SAVPF 111\n"
			+ "c=IN IP4 0.0.0.0\n"
			+ "a=mid:0\n"
			+ "a=rtpmap:111 opus/48000/2\n";

	private static final String VIDEO_SECTION = "m=video 9 UDP/TLS/RTP/SAVPF 96\n"
			+ "c=IN IP4 0.0.0.0\n"
			+ "a=mid:1\n"
			+ "a=rtpmap:96 H264/90000\n";

	@Test
	public void testNoSdpDoesNotChangeTheFlags() {
		//there is no information to inspect, so the query parameters stay as they are
		assertTrue(isMediaEnabled(null, AUDIO));
		assertTrue(isMediaEnabled("", VIDEO));
		assertTrue(isMediaEnabled("   ", AUDIO));
	}

	@Test
	public void testAudioAndVideo() {
		//no direction attribute means sendrecv
		String sdp = SESSION + AUDIO_SECTION + VIDEO_SECTION;

		assertTrue(isMediaEnabled(sdp, AUDIO));
		assertTrue(isMediaEnabled(sdp, VIDEO));
	}

	@Test
	public void testVideoOnly() {
		//this is the drone case, the offer has no audio section at all
		String sdp = SESSION + VIDEO_SECTION;

		assertFalse(isMediaEnabled(sdp, AUDIO));
		assertTrue(isMediaEnabled(sdp, VIDEO));
	}

	@Test
	public void testAudioOnly() {
		String sdp = SESSION + AUDIO_SECTION;

		assertTrue(isMediaEnabled(sdp, AUDIO));
		assertFalse(isMediaEnabled(sdp, VIDEO));
	}

	@Test
	public void testRejectedSectionIsNotEnabled() {
		//port 0 means the section is rejected
		String sdp = SESSION + "m=audio 0 UDP/TLS/RTP/SAVPF 111\na=mid:0\n" + VIDEO_SECTION;

		assertFalse(isMediaEnabled(sdp, AUDIO));
		assertTrue(isMediaEnabled(sdp, VIDEO));
	}

	@Test
	public void testPortWithNumberOfPorts() {
		String sdp = SESSION + "m=audio 9/2 UDP/TLS/RTP/SAVPF 111\na=mid:0\n";

		assertTrue(isMediaEnabled(sdp, AUDIO));
	}

	@Test
	public void testDirectionOfMediaSection() {
		assertTrue(isMediaEnabled(SESSION + AUDIO_SECTION + "a=sendrecv\n", AUDIO));
		assertTrue(isMediaEnabled(SESSION + AUDIO_SECTION + "a=sendonly\n", AUDIO));

		//publisher does not send audio in these directions
		assertFalse(isMediaEnabled(SESSION + AUDIO_SECTION + "a=recvonly\n", AUDIO));
		assertFalse(isMediaEnabled(SESSION + AUDIO_SECTION + "a=inactive\n", AUDIO));
	}

	@Test
	public void testDirectionDoesNotLeakToTheNextSection() {
		//recvonly belongs to the audio section only
		String sdp = SESSION + AUDIO_SECTION + "a=recvonly\n" + VIDEO_SECTION;

		assertFalse(isMediaEnabled(sdp, AUDIO));
		assertTrue(isMediaEnabled(sdp, VIDEO));
	}

	@Test
	public void testSessionLevelDirection() {
		//session level direction applies when the media section does not have one
		String sdp = SESSION + "a=recvonly\n" + AUDIO_SECTION;
		assertFalse(isMediaEnabled(sdp, AUDIO));

		//media section overrides the session level direction
		sdp = SESSION + "a=recvonly\n" + AUDIO_SECTION + "a=sendonly\n";
		assertTrue(isMediaEnabled(sdp, AUDIO));
	}

	@Test
	public void testOneSendingSectionIsEnough() {
		//simulcast like offers may have more than one section of the same type
		String sdp = SESSION + VIDEO_SECTION + "a=recvonly\n" + VIDEO_SECTION + "a=sendonly\n";

		assertTrue(isMediaEnabled(sdp, VIDEO));
	}

	@Test
	public void testCarriageReturnLineEndings() {
		String sdp = (SESSION + VIDEO_SECTION).replace("\n", "\r\n");

		assertFalse(isMediaEnabled(sdp, AUDIO));
		assertTrue(isMediaEnabled(sdp, VIDEO));
	}

	@Test
	public void testMalformedLinesDoNotDisableTheTrack() {
		//don't break the publisher because of an unexpected media line
		assertTrue(isMediaEnabled(SESSION + "m=audio\n", AUDIO));
		assertTrue(isMediaEnabled(SESSION + "m=audio notaport UDP/TLS/RTP/SAVPF 111\n", AUDIO));
	}

	@Test
	public void testMediaTypeIsNotMatchedPartially() {
		//"m=videoo" is not a video section
		assertFalse(isMediaEnabled(SESSION + "m=videoo 9 UDP/TLS/RTP/SAVPF 96\n", VIDEO));
	}

}
