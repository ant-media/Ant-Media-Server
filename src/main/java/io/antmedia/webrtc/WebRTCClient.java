package io.antmedia.webrtc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.red5.net.websocket.WebSocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.*;


import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.IceGatheringState;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.Observer;
import org.webrtc.PeerConnection.SignalingState;

import org.webrtc.SessionDescription.Type;

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

	private WebSocketConnection wsConnection;

	private IWebRTCAdaptor webRTCAdaptor;

	private String streamId;

	private IWebRTCMuxer webRTCMuxer;

	private static Logger logger = LoggerFactory.getLogger(WebRTCClient.class);

	private ScheduledExecutorService executor;
	
	public static final int ADAPTIVE_RESET_COUNT = 90;  //frames
	
	public static final int ADAPTIVE_QUALITY_CHECK_TIME_MS = 5000;
	
	//int bufferSize = 480 * 2 * 2; // 480 sample, 2 byte per sample, stereo
	//byte[] dataSegmented = new byte[bufferSize];

	private volatile boolean isRunning = false;
	
	private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";


	public WebRTCClient(WebSocketConnection wsConnection, String streamId) {
		this.wsConnection = wsConnection;
		this.streamId = streamId;
		ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
						.setNameFormat("webrtc-client-"+streamId+"-%d").build();
		executor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);
		
	}


	public String getStreamId() {
		return streamId;
	}

	@Override
	public void setWebRTCAdaptor(IWebRTCAdaptor webRTCAdaptor) {
		this.webRTCAdaptor = webRTCAdaptor;
	}

	@Override
	public void sendVideoConfPacket(byte[] videoConfData, byte[] videoPacket, long timestamp) {
		if (isRunning) {
			factory.addVideoConfPacket(videoConfData, videoConfData.length, videoPacket, videoPacket.length, width, height, true, timestamp);
			videoSource.writeMockFrame(width, height);
		}
	}

	@Override
	public void sendVideoPacket(byte[] videoPacket, boolean isKeyFrame, long timestamp) {
		if (isRunning) {
			factory.addVideoPacket(videoPacket, videoPacket.length, width, height, isKeyFrame, timestamp);
			videoSource.writeMockFrame(width, height);
		}
	}

	@Override
	public void sendAudioPacket(byte[] audioPacket, long timestamp) {
		if (isRunning) {
			factory.addAudioPacket(audioPacket, audioPacket.length, timestamp);
			
			
			audioSource.writeMockFrame(480);
			audioSource.writeMockFrame(480);

			//audioSource.writeAudioFrame(dataSegmented, 480);
			//audioSource.writeAudioFrame(dataSegmented, 480);
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
				List<IceServer> iceServers = new ArrayList();
				iceServers.add(new IceServer("stun:stun.l.google.com:19302"));
				PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
				createMediaConstraintsInternal();

				factory = createPeerConnectionFactory();

				peerConnection = factory.createPeerConnection(rtcConfig, pcConstraints, WebRTCClient.this);

				MediaStream mediaStream = factory.createLocalMediaStream("local_stream");

				audioSource = factory.createAudioSource(pcConstraints);
				mediaStream.addTrack(factory.createAudioTrack("audio", audioSource));

				videoSource = factory.createVideoSource();
				mediaStream.addTrack(factory.createVideoTrack("video", videoSource));

				peerConnection.addStream(mediaStream);

				peerConnection.createOffer(WebRTCClient.this, sdpMediaConstraints);
				
				isRunning = true;
				

			}
		});



	}

	private void createMediaConstraintsInternal() {
		// Create peer connection constraints.
		pcConstraints = new MediaConstraints();
		// Enable DTLS for normal calls and disable for loopback calls.
		//if (peerConnectionParameters.loopback) {
		//  pcConstraints.optional.add(
		//      new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "false"));
		//} 
		//else 
		{
			pcConstraints.optional.add(
					new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
		}

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
	public void onIceConnectionChange(IceConnectionState newState) {
		if (newState == IceConnectionState.COMPLETED) {
			webRTCAdaptor.registerWebRTCClient(streamId, this);
			try {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("command", "notification");
				jsonObject.put("definition", "play_started");
				wsConnection.send(jsonObject.toJSONString());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}	
			
			executor.scheduleWithFixedDelay(new Runnable() {
		
				@Override
				public void run() {
					webRTCAdaptor.adaptStreamingQuality(streamId, WebRTCClient.this);
				}
				
			}, ADAPTIVE_QUALITY_CHECK_TIME_MS, ADAPTIVE_QUALITY_CHECK_TIME_MS, TimeUnit.MILLISECONDS);
			
		}
		else if (newState == IceConnectionState.DISCONNECTED) {
			//webRTCAdaptor.deregisterWebRTCClient(streamId, this);
			stop();

		}

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
				// TODO Auto-generated method stub


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
		logger.info("setRemoteDescription : " + sdp);
		executor.execute(new Runnable() {

			@Override
			public void run() {

				if (peerConnection != null) {
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
		if (!isRunning) {
			return;
		}
		//make isRunning false immediately to not let other threads enter this function
		isRunning = false;
		if (webRTCMuxer != null) {
			webRTCMuxer.unRegisterWebRTCClient(this);
		}
		executor.execute(new Runnable() {

			@Override
			public void run() {
				logger.info("Disposing peerconnection objects");

				if (peerConnection != null) {
					peerConnection.close();
					peerConnection.dispose();
					peerConnection = null;
				}
				if (audioSource != null) {
					audioSource.dispose();
					audioSource = null;
				}
				if (videoSource != null ) {
					videoSource.dispose();
					videoSource = null;
				}
				
				if (factory != null) {
					factory.dispose();
					factory = null;
				}
				
				try {
					JSONObject jsonObject = new JSONObject();
					jsonObject.put("command", "notification");
					jsonObject.put("definition", "play_finished");
					wsConnection.send(jsonObject.toJSONString());
					logger.info("leaving from disposing peerconnection objects");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}	

			}
		});
		executor.shutdown();
	}

}
