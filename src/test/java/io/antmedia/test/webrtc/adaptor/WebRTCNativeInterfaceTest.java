package io.antmedia.test.webrtc.adaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.BuiltinAudioDecoderFactoryFactory;
import org.webrtc.CapturerObserver;
import org.webrtc.DataChannel;
import org.webrtc.H264Utils;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.IceServer.Builder;
import org.webrtc.PeerConnection.Observer;
import org.webrtc.PeerConnection.SdpSemantics;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnection.TcpCandidatePolicy;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpSender;
import org.webrtc.RtpTransceiver;
import org.webrtc.RtpTransceiver.RtpTransceiverDirection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoDecoderFactory;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoCodecType;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.WebRtcAudioRecord;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.webrtc.api.IAudioRecordListener;
import io.antmedia.webrtc.api.IAudioTrackListener;

public class WebRTCNativeInterfaceTest {

	private String stunServerUri = "stun:stun1.l.google.com:19302";

	protected static Logger logger = LoggerFactory.getLogger(WebRTCNativeInterfaceTest.class);


	private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
	private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
	private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
	private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
	private static final String FALSE = "false";

	private SessionDescription sdp;

	Observer peerConnectionObserver = new Observer() {

		@Override
		public void onSignalingChange(SignalingState newState) {

		}

		@Override
		public void onRenegotiationNeeded() {

		}

		@Override
		public void onRemoveStream(MediaStream stream) {

		}

		@Override
		public void onIceGatheringChange(IceGatheringState newState) {

		}

		@Override
		public void onIceConnectionReceivingChange(boolean receiving) {

		}

		@Override
		public void onIceConnectionChange(IceConnectionState newState) {

		}

		@Override
		public void onIceCandidatesRemoved(IceCandidate[] candidates) {

		}

		@Override
		public void onIceCandidate(IceCandidate candidate) {

		}

		@Override
		public void onDataChannel(DataChannel dataChannel) {

		}

		@Override
		public void onAddStream(MediaStream stream) {

		}
	};

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};

	@Before
	public void setup() {

	}

	@BeforeClass
	public static void beforeClass() {
		logger.info("Loading native library - 0");
		PeerConnectionFactory.initialize(
				PeerConnectionFactory.InitializationOptions.builder()
				.setFieldTrials(null)
				.createInitializationOptions());
		
		logger.info("Loading native library - 1");


	}

	@Test
	public void testCheckNotifyEncodedData() {	

		JavaAudioDeviceModule adm =	(JavaAudioDeviceModule)
				JavaAudioDeviceModule.builder(null)
				.setUseHardwareAcousticEchoCanceler(false)
				.setUseHardwareNoiseSuppressor(false)
				.setAudioRecordErrorCallback(null)
				.setAudioTrackErrorCallback(null)
				.setAudioTrackListener(new IAudioTrackListener() {

					@Override
					public void playoutStopped() {
						//no need to implement
					}

					@Override
					public void playoutStarted() {
					}
				})
				.createAudioDeviceModule();

		//initialize to get native fields filled
		PeerConnectionFactory peerConnectionFactory = getPeerConnectionFactory(null, null, adm);
		assertNotNull(peerConnectionFactory);

		WebRtcAudioRecord audioRecord = spy(adm.getAudioRecord());

		String trackId = "track1";
		audioRecord.getEncodedByteBuffers().put(trackId, ByteBuffer.allocate(1000));
		ByteBuffer audio = ByteBuffer.allocate(100);
		audioRecord.notifyEncodedData(trackId, audio);

		Mockito.verify(audioRecord).encodedDataIsReady(anyLong(), anyString(), anyInt());


	}

	@Test
	public void testAudioRecordListener() {

		{
			JavaAudioDeviceModule adm =	(JavaAudioDeviceModule)
					JavaAudioDeviceModule.builder(null)
					.setUseHardwareAcousticEchoCanceler(false)
					.setUseHardwareNoiseSuppressor(false)
					.setAudioRecordErrorCallback(null)
					.setAudioTrackErrorCallback(null)
					.setAudioTrackListener(new IAudioTrackListener() {

						@Override
						public void playoutStopped() {
							//no need to implement
						}

						@Override
						public void playoutStarted() {
						}
					})
					.setAudioRecordListener(new IAudioRecordListener() {

						@Override
						public void audioRecordStoppped() {

						}

						@Override
						public void audioRecordStarted() {

						}
					})
					.createAudioDeviceModule();

			//initialize to get native fields filled
			PeerConnectionFactory peerConnectionFactory = getPeerConnectionFactory(null, null, adm);
			assertNotNull(peerConnectionFactory);

			WebRtcAudioRecord audioRecord = spy(adm.getAudioRecord());

			String trackId = "track1";
			audioRecord.getEncodedByteBuffers().put(trackId, ByteBuffer.allocate(1000));
			ByteBuffer audio = ByteBuffer.allocate(100);
			audioRecord.notifyEncodedData(trackId, audio);

			Mockito.verify(audioRecord).encodedDataIsReady(anyLong(), anyString(), anyInt());

			audioRecord.triggerAudioRecordStarted();
			assertNotNull(audioRecord.getAudioRecordListener());	
			audioRecord.triggerAudioRecordStopped();
			//Check for https://github.com/ant-media/Ant-Media-Server/issues/5808
			assertNotNull(audioRecord.getAudioRecordListener());
		}
		
		{
			JavaAudioDeviceModule adm =	(JavaAudioDeviceModule)
					JavaAudioDeviceModule.builder(null)
					.setUseHardwareAcousticEchoCanceler(false)
					.setUseHardwareNoiseSuppressor(false)
					.setAudioRecordErrorCallback(null)
					.setAudioTrackErrorCallback(null)
					.setAudioTrackListener(new IAudioTrackListener() {

						@Override
						public void playoutStopped() {
							//no need to implement
						}

						@Override
						public void playoutStarted() {
						}
					})
					.createAudioDeviceModule();

			//initialize to get native fields filled
			PeerConnectionFactory peerConnectionFactory = getPeerConnectionFactory(null, null, adm);
			assertNotNull(peerConnectionFactory);

			WebRtcAudioRecord audioRecord = spy(adm.getAudioRecord());

			String trackId = "track1";
			audioRecord.getEncodedByteBuffers().put(trackId, ByteBuffer.allocate(1000));
			ByteBuffer audio = ByteBuffer.allocate(100);
			audioRecord.notifyEncodedData(trackId, audio);

			Mockito.verify(audioRecord).encodedDataIsReady(anyLong(), anyString(), anyInt());

			audioRecord.triggerAudioRecordStarted();
			assertNull(audioRecord.getAudioRecordListener());	
			audioRecord.triggerAudioRecordStopped();
			//Check for https://github.com/ant-media/Ant-Media-Server/issues/5808
			assertNull(audioRecord.getAudioRecordListener());
		}



	}



	public static final String VIDEO_TRACK_ID = "ARDAMSv";

	public static final String AUDIO_TRACK_ID = "ARDAMSa";

	boolean onCreateSuccess = false;
	boolean onSetSuccess = false;

	public void initPeerConnection(VideoEncoderFactory encoderFactory, VideoDecoderFactory decoderFactory) {


		PeerConnectionFactory peerConnectionFactory = getPeerConnectionFactory(encoderFactory, decoderFactory);


		assertNotNull(peerConnectionFactory);

		List<IceServer> iceServers = new ArrayList<>();
		Builder iceServerBuilder = IceServer.builder(stunServerUri);
		iceServers.add(iceServerBuilder.createIceServer());

		PeerConnection.RTCConfiguration rtcConfig =
				new PeerConnection.RTCConfiguration(iceServers);

		rtcConfig.disableIpv6 = false ;
		rtcConfig.enableCpuOveruseDetection = false;
		rtcConfig.tcpCandidatePolicy = TcpCandidatePolicy.ENABLED;


		rtcConfig.sdpSemantics = SdpSemantics.UNIFIED_PLAN;

		PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, peerConnectionObserver);

		assertNotNull(peerConnection);

		VideoSource videoSource = peerConnectionFactory.createVideoSource(false);
		CapturerObserver capturerObserver = videoSource.getCapturerObserver();
		List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");

		String trackId = "trackId";
		VideoTrack videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID+trackId, videoSource);

		RtpSender videoSender = peerConnection.addTrack(videoTrack, mediaStreamLabels);
		assertNotNull(videoSender);

		List<RtpTransceiver> transceivers = peerConnection.getTransceivers();
		RtpTransceiver rtpTransceiver = null;
		boolean found = false;
		for (RtpTransceiver transceiver : transceivers) 
		{

			if(transceiver.getSender().id().contentEquals(VIDEO_TRACK_ID+trackId)) {
				found = true;
				rtpTransceiver = transceiver;
			}
		}
		assertTrue(found);
		RtpTransceiverDirection direction = rtpTransceiver.getDirection();
		assertEquals(RtpTransceiverDirection.SEND_RECV, direction);
		rtpTransceiver.setDirection(RtpTransceiverDirection.SEND_ONLY);


		MediaConstraints audioConstraints = new MediaConstraints();
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, FALSE));
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, FALSE));
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, FALSE));
		audioConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, FALSE));

		AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
		AudioTrack audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID+trackId, audioSource);
		RtpSender audioSender = peerConnection.addTrack(audioTrack, mediaStreamLabels);

		assertNotNull(audioSender);



		transceivers = peerConnection.getTransceivers();
		found = true;
		for (RtpTransceiver transceiver : transceivers) 
		{

			if(transceiver.getSender().id().contentEquals(AUDIO_TRACK_ID+trackId)) {
				found = true;
				rtpTransceiver = transceiver;
			}
		}

		assertTrue(found);
		direction = rtpTransceiver.getDirection();
		assertEquals(RtpTransceiverDirection.SEND_RECV, direction);
		rtpTransceiver.setDirection(RtpTransceiverDirection.SEND_ONLY);

		onCreateSuccess = false;
		peerConnection.createOffer(new SdpObserver() {


			@Override
			public void onSetSuccess() {
				logger.info("onSetSuccess");

			}

			@Override
			public void onSetFailure(String error) {
				logger.info("onSetFailure");
			}

			@Override
			public void onCreateSuccess(SessionDescription sdp) {
				logger.info("onCreateSuccess: " + sdp.description);
				WebRTCNativeInterfaceTest.this.sdp = sdp;
				onCreateSuccess = true;

			}

			@Override
			public void onCreateFailure(String error) {
				logger.info("onCreateFailure");

			}
		}, new MediaConstraints());


		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { return onCreateSuccess; });

		assertTrue(this.sdp.description.contains("H264"));

		assertFalse(this.sdp.description.contains("a=sendrecv"));

		assertTrue(this.sdp.description.contains("a=sendonly"));


		onSetSuccess = false;
		peerConnection.setLocalDescription(new SdpObserver() {

			@Override
			public void onCreateSuccess(SessionDescription sdp) {
				logger.info("onCreateSuccess");
			}

			@Override
			public void onSetSuccess() {
				onSetSuccess = true;
				logger.info("onSetSuccess");
			}

			@Override
			public void onCreateFailure(String error) {
				logger.info("onCreateFailure");
			}

			@Override
			public void onSetFailure(String error) {
				logger.info("onSetFailure");

			}

		}, this.sdp);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> { return onSetSuccess; });

	}

	private PeerConnectionFactory getPeerConnectionFactory(VideoEncoderFactory encoderFactory,
			VideoDecoderFactory decoderFactory) {
		return getPeerConnectionFactory(encoderFactory, decoderFactory, null);
	}

	private PeerConnectionFactory getPeerConnectionFactory(VideoEncoderFactory encoderFactory,
			VideoDecoderFactory decoderFactory, JavaAudioDeviceModule adm) {

		PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		options.disableNetworkMonitor = true;
		options.networkIgnoreMask = PeerConnectionFactory.Options.ADAPTER_TYPE_LOOPBACK;

		BuiltinAudioDecoderFactoryFactory audioDecoderFactoryFactory = new BuiltinAudioDecoderFactoryFactory();

		if (adm == null) {
			// in receiving stream only Audio Track should be enabled
			// in sending stream only AudioRecord should be enabled 
			adm = (JavaAudioDeviceModule)
					JavaAudioDeviceModule.builder(null)
					.setUseHardwareAcousticEchoCanceler(false)
					.setUseHardwareNoiseSuppressor(false)
					.setAudioRecordErrorCallback(null)
					.setAudioTrackErrorCallback(null)
					.setAudioTrackListener(new IAudioTrackListener() {

						@Override
						public void playoutStopped() {
							//no need to implement
						}

						@Override
						public void playoutStarted() {
						}
					})
					.createAudioDeviceModule();

		}

		WebRtcAudioTrack webRtcAudioTrack = adm.getAudioTrack();



		PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory.builder()
				.setOptions(options)
				.setAudioDeviceModule(adm)
				.setVideoEncoderFactory(encoderFactory)
				.setVideoDecoderFactory(decoderFactory)
				.setAudioDecoderFactoryFactory(audioDecoderFactoryFactory)
				.createPeerConnectionFactory();
		return peerConnectionFactory;
	}

	@Test
	public void testPeerConnection() {

		initPeerConnection(null, null);
	}


	@Test
	public void testBugNotSetLocalDescription() {


		VideoEncoderFactory encoderFactory = new VideoEncoderFactory() {

			@Override
			public VideoEncoder createEncoder(VideoCodecInfo info) {
				return null;
			}

			@Override
			public VideoCodecInfo[] getSupportedCodecs() 
			{
				List<VideoCodecInfo> supportedCodecInfos = new ArrayList<>();
				VideoCodecInfo videoCodecInfo = new VideoCodecInfo(VideoCodecType.H264.name(), H264Utils.getDefaultH264Params(false));
				supportedCodecInfos.add(videoCodecInfo);
				return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);

			}

		};
		SoftwareVideoDecoderFactory decoderFactory = new SoftwareVideoDecoderFactory();
		initPeerConnection(encoderFactory, decoderFactory);
	}
}
