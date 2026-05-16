package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.antmedia.rest.RestServiceBase;

public class RestServiceBaseRtspRewriteTest {

	static class TestableRestServiceBase extends RestServiceBase {
		public String callBuild(String inputAddress, String username, String password, String returnedRtsp) {
			return buildRtspUrlWithAuthPreservingPublicHost(inputAddress, username, password, returnedRtsp);
		}
	}

	@Test
	public void testRewritePrivateHostWithInputPort() {
		TestableRestServiceBase svc = new TestableRestServiceBase();
		String input = "http://public.example.com:8554/onvif/device_service";
		String returnedRtsp = "rtsp://192.168.1.100:554/stream1";
		String out = svc.callBuild(input, "user", "pass", returnedRtsp);
		assertEquals("rtsp://user:pass@public.example.com:554/stream1", out);
	}

	@Test
	public void testRewritePrivateHostPreserveReturnedPortWhenInputNoPort() {
		TestableRestServiceBase svc = new TestableRestServiceBase();
		String input = "http://public.example.com";
		String returnedRtsp = "rtsp://192.168.0.10:8554/live.sdp";
		String out = svc.callBuild(input, "u", "p", returnedRtsp);
		assertEquals("rtsp://u:p@public.example.com:8554/live.sdp", out);
	}

	@Test
	public void testPreserveWhenReturnedIsPublic() {
		TestableRestServiceBase svc = new TestableRestServiceBase();
		String input = "http://public.example.com:8554";
		String returnedRtsp = "rtsp://203.0.113.10:8554/channel=1";
		String out = svc.callBuild(input, "a", "b", returnedRtsp);
		assertEquals("rtsp://a:b@203.0.113.10:8554/channel=1", out);
	}

	@Test
	public void testStripReturnedCredentials() {
		TestableRestServiceBase svc = new TestableRestServiceBase();
		String input = "http://example.net";
		String returnedRtsp = "rtsp://cam:secret@192.168.1.50:554/path/to/stream";
		String out = svc.callBuild(input, "newU", "newP", returnedRtsp);
		assertEquals("rtsp://newU:newP@example.net:554/path/to/stream", out);
	}

	@Test
	public void testParseInputWithSchemeCredsAndPath() {
		TestableRestServiceBase svc = new TestableRestServiceBase();
		String input = "https://inpUser:inpPass@gw.my.org:7441/onvif";
		String returnedRtsp = "rtsp://192.168.2.20/stream"; // no port -> keep returned no-port
		String out = svc.callBuild(input, "U", "P", returnedRtsp);
		assertEquals("rtsp://U:P@gw.my.org/stream", out);
	}

	@Test
	public void testNullReturnedRtsp() {
		TestableRestServiceBase svc = new TestableRestServiceBase();
		String out = svc.callBuild("http://host", "u", "p", null);
		assertEquals("rtsp://u:p@", out);
	}
}


