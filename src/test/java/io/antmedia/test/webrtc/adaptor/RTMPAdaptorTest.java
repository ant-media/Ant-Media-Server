package io.antmedia.test.webrtc.adaptor;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.json.simple.JSONObject;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.webrtc.IceCandidate;
import org.webrtc.JavaI420Buffer;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;
import org.webrtc.VideoFrame;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.integration.MuxingTest;
import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.recorder.Frame;
import io.antmedia.webrtc.AudioFrameContext;
import io.antmedia.webrtc.MockWebRTCAdaptor;
import io.antmedia.webrtc.VideoCodec;
import io.antmedia.webrtc.VideoFrameContext;
import io.antmedia.webrtc.adaptor.RTMPAdaptor;
import io.antmedia.webrtc.adaptor.RTMPAdaptor.AudioFrame;
import io.antmedia.webrtc.adaptor.RTMPAdaptor.WebRTCVideoSink;
import io.antmedia.websocket.WebSocketCommunityHandler;
import io.antmedia.websocket.WebSocketConstants;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;

@Tag("fast")
public class RTMPAdaptorTest {

	//moved to enterprise because it's crashing time to time in travis ci while loading the lib and I could not resolve
	//mekya

	@Test
	public void testSetStunServerUri() {
		//construction and setters don't touch the native webrtc lib, so they're safe to run here
		RTMPAdaptor adaptor = new RTMPAdaptor("rtmp://localhost/live/test", null, 480);

		//4-arg overload also stores the iceServers config
		adaptor.setStunServerUri("stun:stun1.l.google.com:19302", "user", "cred", "[{\"urls\":\"turn:turn.antmedia.io\"}]");
		assertEquals("stun:stun1.l.google.com:19302", adaptor.getStunServerUri());

		//3-arg overload delegates with a null iceServers config
		adaptor.setStunServerUri("stun:stun2.l.google.com:19302", "user2", "cred2");
		assertEquals("stun:stun2.l.google.com:19302", adaptor.getStunServerUri());
	}
}
