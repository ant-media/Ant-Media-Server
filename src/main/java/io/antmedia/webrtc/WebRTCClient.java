package io.antmedia.webrtc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.red5.net.websocket.WebSocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.AudioSource;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.Observer;
import org.webrtc.PeerConnection.SignalingState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SessionDescription.Type;
import org.webrtc.VideoSource;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;
import io.antmedia.webrtc.api.IWebRTCMuxer;


public class WebRTCClient implements IWebRTCClient, Observer, SdpObserver {

	private MediaConstraints pcConstraints;

	private MediaConstraints sdpMediaConstraints;

	private PeerConnectionFactory factory;

	private PeerConnection peerConnection;

	private VideoSource videoSource;
	private AudioSource audioSource;

	private int width;

	private int height;

	private MediaStream mediaStream;

	private WebSocketConnection wsConnection;

	private IWebRTCAdaptor webRTCAdaptor;

	private String streamId;

	private IWebRTCMuxer webRTCMuxer;

	private static Logger logger = LoggerFactory.getLogger(WebRTCClient.class);

	private ScheduledExecutorService executor;

	private ExecutorService audioStreamExecutor;

	private ExecutorService videoStreamExecutor;

	public static final int ADAPTIVE_RESET_COUNT = 90;  //frames

	public static final int ADAPTIVE_QUALITY_CHECK_TIME_MS = 5000;

	//int bufferSize = 480 * 2 * 2; // 480 sample, 2 byte per sample, stereo
	//byte[] dataSegmented = new byte[bufferSize];

	private volatile boolean isInitialized = false;

	private volatile boolean isStreaming = false;



	private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";

	private ScheduledFuture<?> adaptStreamScheduledFuture;

	protected boolean settingRemoteDescription = false;

	private boolean remoteDescriptionSet = false;

	public WebRTCClient(WebSocketConnection wsConnection, String streamId) {
		this.wsConnection = wsConnection;
		this.streamId = streamId;
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
				.setNameFormat("webrtc-client-"+streamId+"-%d").build();
		executor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

		namedThreadFactory = new ThreadFactoryBuilder()
				.setNameFormat("webrtc-streamer-"+streamId+"-%d").build();

		audioStreamExecutor =  Executors.newSingleThreadExecutor(namedThreadFactory);
		videoStreamExecutor = Executors.newSingleThreadExecutor(namedThreadFactory);

	}


	public String getStreamId() {
		return streamId;
	}

	@Override
	public void setWebRTCAdaptor(IWebRTCAdaptor webRTCAdaptor) {
		this.webRTCAdaptor = webRTCAdaptor;
	}

	@Override
	public void sendVideoConfPacket(final byte[] videoConfData, final byte[] videoPacket, final long timestamp) {
		if (isStreaming) {
			videoStreamExecutor.execute(new Runnable() 
			{
				@Override
				public void run() {
					factory.addVideoConfPacket(videoConfData, videoConfData.length, videoPacket, videoPacket.length, width, height, true, timestamp);
				}
			});
		}
	}

	@Override
	public void sendVideoPacket(final byte[] videoPacket, final boolean isKeyFrame, final long timestamp) {
		if (isStreaming) {
			videoStreamExecutor.execute(new Runnable() {

				@Override
				public void run() {
					factory.addVideoPacket(videoPacket, videoPacket.length, width, height, isKeyFrame, timestamp);
				}
			});
		}
	}

	@Override
	public void sendAudioPacket(final byte[] audioPacket, final long timestamp) {
		if (isStreaming) {
			audioStreamExecutor.execute(new Runnable() {

				@Override
				public void run() {
					factory.addAudioPacket(audioPacket, audioPacket.length, timestamp, 960);
					//960 is 20ms audio data which can be send by webrtc

				}
			});

		}

	}

	@Override
	public int getTargetBitrate() {
		return (int)factory.getTargetedBitrate();
	}

	public static PeerConnectionFactory createPeerConnectionFactory(){
		PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		options.networkIgnoreMask = 0;
		return new PeerConnectionFactory(options);
	}

	public void start() {

		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					List<IceServer> iceServers = new ArrayList<IceServer>();
					iceServers.add(new IceServer("stun:stun.l.google.com:19302"));
					PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
					createMediaConstraintsInternal();

					factory = createPeerConnectionFactory();

					peerConnection = factory.createPeerConnection(rtcConfig, pcConstraints, WebRTCClient.this);

					mediaStream = factory.createLocalMediaStream("local_stream");

					audioSource = factory.createAudioSource(pcConstraints);
					mediaStream.addTrack(factory.createAudioTrack("audio", audioSource));

					videoSource = factory.createVideoSource();
					mediaStream.addTrack(factory.createVideoTrack("video", videoSource));

					peerConnection.addStream(mediaStream);

					peerConnection.createOffer(WebRTCClient.this, sdpMediaConstraints);

					isInitialized = true;
					
					logger.info("... Start Initialized...");
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});



	}

	private void createMediaConstraintsInternal() {
		// Create peer connection constraints.
		pcConstraints = new MediaConstraints();

		pcConstraints.optional.add(
				new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));


		// Create SDP constraints.
		sdpMediaConstraints = new MediaConstraints();

		sdpMediaConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"));

		sdpMediaConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));


	}

	@Override
	public void onSignalingChange(SignalingState newState) {
		logger.info("onSignalingChange : " + newState);
	}

	@Override
	public void onIceConnectionChange(IceConnectionState newState) 
	{
		logger.info("IceConnectionState: " + newState );

		if (newState == IceConnectionState.CONNECTED) 
		{

			logger.info("Signalling state: " + peerConnection.signalingState());

			if (remoteDescriptionSet) {
				startStreaming();
			}

		}
		else if (newState == IceConnectionState.FAILED || newState == IceConnectionState.CLOSED) {
			// newState == IceConnectionState.DISCONNECTED ||
			//webRTCAdaptor.deregisterWebRTCClient(streamId, this);
			stop();
		}

	}

	private void startStreaming() {
		if (isStreaming) {
			return;
		}

		logger.info("Starting streaming....");
		isStreaming = true;

		executor.schedule(new Runnable() {

			@Override
			public void run() {
				webRTCAdaptor.registerWebRTCClient(streamId, WebRTCClient.this);

				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("command", "notification");
					jsonObject.put("definition", "play_started");
					wsConnection.send(jsonObject.toJSONString());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}	

			}
		}, 0, TimeUnit.MILLISECONDS);


		adaptStreamScheduledFuture = executor.scheduleWithFixedDelay(new Runnable() {

			@Override
			public void run() {
				webRTCAdaptor.adaptStreamingQuality(streamId, WebRTCClient.this);

			}

		}, ADAPTIVE_QUALITY_CHECK_TIME_MS, ADAPTIVE_QUALITY_CHECK_TIME_MS, TimeUnit.MILLISECONDS);
	}

	@Override
	public void onIceConnectionReceivingChange(boolean receiving) {
		logger.info("onIceConnectionReceivingChange : " + receiving);
	}

	@Override
	public void onIceGatheringChange(IceGatheringState newState) {
		logger.info("onIceGatheringChange : " + newState);
	}

	@Override
	public void onIceCandidate(IceCandidate candidate) {

		logger.info("onIceCandidate : " + candidate);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "takeCandidate");
		jsonObject.put("label", candidate.sdpMLineIndex);
		jsonObject.put("id", candidate.sdpMid);
		jsonObject.put("candidate", candidate.sdp);

		try {
			wsConnection.send(jsonObject.toJSONString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}	
	}



	@Override
	public void onIceCandidatesRemoved(IceCandidate[] candidates) {
		logger.info("onIceCandidatesRemoved : " + candidates);
	}

	@Override
	public void onAddStream(MediaStream stream) {
		logger.info("onAddStream : " + stream);
	}

	@Override
	public void onRemoveStream(MediaStream stream) {
		logger.info("onRemoveStream : " + stream);
	}

	@Override
	public void onDataChannel(DataChannel dataChannel) {
		logger.info("onDataChannel : " + dataChannel);
	}

	@Override
	public void onRenegotiationNeeded() {
		logger.info("onRenegotiationNeeded");
	}

	@Override
	public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
		logger.info("onAddTrack : " + receiver);
	}

	@Override
	public void onCreateSuccess(final SessionDescription sdp) {
		logger.info("onCreateSuccess : " + sdp);
		executor.execute(new Runnable() {

			@Override
			public void run() {

				logger.info("setting local description");
				peerConnection.setLocalDescription(WebRTCClient.this, sdp);


				JSONObject jsonObject = new JSONObject();
				jsonObject.put("command", "takeConfiguration");
				jsonObject.put("sdp", sdp.description);
				String type;
				if (sdp.type == Type.ANSWER) {
					type = "answer";
				}
				else  {
					type = "offer";
				}
				jsonObject.put("type", type);

				try {
					wsConnection.send(jsonObject.toJSONString());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		});

	}

	@Override
	public void onSetSuccess() {
		logger.info("onSetSuccess");
		if (settingRemoteDescription) {
			remoteDescriptionSet = true;

			switch (peerConnection.iceConnectionState()) {
			case CONNECTED:
			case COMPLETED:
				startStreaming();
				break;
			}
		}

	}

	@Override
	public void onCreateFailure(String error) {
		logger.info("onCreateFailure : " + error);
	}

	@Override
	public void onSetFailure(String error) {
		logger.info("onSetFailure : " + error);
	}


	@Override
	public void setRemoteDescription(final SessionDescription sdp) {
		executor.execute(new Runnable() {

			@Override
			public void run() {

				if (peerConnection != null) {
					settingRemoteDescription = true;
					logger.info("setting remote description ");
					peerConnection.setRemoteDescription(WebRTCClient.this, sdp);
				}
				else {
					logger.warn("Peer connection is null. It cannot add ice candidate");
				}	

			}
		});
	}


	public void addIceCandidate(final IceCandidate iceCandidate) {

		logger.info("addIceCandidate : " + iceCandidate);
		executor.execute(new Runnable() {

			@Override
			public void run() {
				if (peerConnection != null) {
					peerConnection.addIceCandidate(iceCandidate);
				}
				else {
					logger.warn("Peer connection is null. It cannot set remote description");
				}
			}
		});


	}


	@Override
	public void setVideoResolution(int width, int height) {
		this.width = width;
		this.height = height;

	}


	@Override
	public void setWebRTCMuxer(IWebRTCMuxer webRTCMuxer) {
		this.webRTCMuxer = webRTCMuxer;	
	}

	@Override
	public IWebRTCMuxer getWebRTCMuxer() {
		return webRTCMuxer;
	}


	public void stop() {
		if (!isInitialized) {
			logger.info("Stop is already called");
			return;
		}
		logger.info("Stopping webrtc client");
		//make isRunning false immediately to not let other threads enter this function
		isInitialized = false;
		isStreaming = false;

		executor.execute(new Runnable() {
			@Override
			public void run() {
				if (webRTCMuxer != null) {
					webRTCMuxer.unRegisterWebRTCClient(WebRTCClient.this);
				}

				audioStreamExecutor.shutdownNow();
				videoStreamExecutor.shutdownNow();

				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("command", "notification");
					jsonObject.put("definition", "play_finished");
					wsConnection.send(jsonObject.toJSONString());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}	


				try {
					audioStreamExecutor.awaitTermination(10, TimeUnit.SECONDS);
					videoStreamExecutor.awaitTermination(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}


				peerConnection.removeStream(mediaStream);

				if (adaptStreamScheduledFuture != null) {
					adaptStreamScheduledFuture.cancel(false);
					adaptStreamScheduledFuture = null;
				}

				logger.info("Disposing peerconnection objects");

				if (audioSource != null) {
					logger.info("Disposing audio source");
					audioSource.dispose();
					audioSource = null;
				}

				if (videoSource != null ) {
					logger.info("Disposing video source");
					videoSource.dispose();
					videoSource = null;
				}



				if (peerConnection != null) {
					logger.info("Closing peer connection: " + peerConnection.iceConnectionState());

					peerConnection.close();


					logger.info("Disposing peer connection");
					peerConnection.dispose();
					peerConnection = null;
				}

				if (factory != null) {
					logger.info("Closing peer connection factory ");
					factory.dispose();
					factory = null;
				}




			}
		});
		executor.shutdown();

	}


	public boolean isInitialized() {
		return isInitialized;
	}


	public void setInitialized(boolean isInitialized) {
		this.isInitialized = isInitialized;
	}


	public boolean isStreaming() {
		return isStreaming;
	}


	public void setStreaming(boolean isStreaming) {
		this.isStreaming = isStreaming;
	}

}
