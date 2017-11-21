package io.antmedia.webrtc;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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


	private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";


	//TODO: clear resources when connection drop or live stream ended
	public WebRTCClient(WebSocketConnection wsConnection, String streamId) {
		this.wsConnection = wsConnection;
		this.streamId = streamId;
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
		int result = factory.addVideoConfPacket(videoConfData, videoConfData.length, videoPacket, videoPacket.length, width, height, true, timestamp);
		videoSource.writeMockFrame(width, height);		
	}

	@Override
	public void sendVideoPacket(byte[] videoPacket, boolean isKeyFrame, long timestamp) {

		factory.addVideoPacket(videoPacket, videoPacket.length, width, height, isKeyFrame, timestamp);
		videoSource.writeMockFrame(width, height);



	}

	@Override
	public void sendAudioPacket(byte[] audioPacket, long timestamp) {
		factory.addAudioPacket(audioPacket, audioPacket.length, timestamp);
		int bufferSize = 480 * 2 * 2; // 480 sample, 2 byte per sample, stereo
		byte[] dataSegmented = new byte[bufferSize];

		audioSource.writeAudioFrame(dataSegmented, 480);
		audioSource.writeAudioFrame(dataSegmented, 480);

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
		List<IceServer> iceServers = new ArrayList();
		iceServers.add(new IceServer("stun:stun.l.google.com:19302"));
		PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
		createMediaConstraintsInternal();

		factory = createPeerConnectionFactory();

		peerConnection = factory.createPeerConnection(rtcConfig, pcConstraints, this);

		MediaStream mediaStream = factory.createLocalMediaStream("local_stream");

		audioSource = factory.createAudioSource(pcConstraints);
		mediaStream.addTrack(factory.createAudioTrack("audio", audioSource));

		videoSource = factory.createVideoSource();
		mediaStream.addTrack(factory.createVideoTrack("video", videoSource));

		peerConnection.addStream(mediaStream);

		peerConnection.createOffer(this, sdpMediaConstraints);


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
		//		    if (videoCallEnabled || peerConnectionParameters.loopback) {
		sdpMediaConstraints.mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
		//		    } else {
		//		      sdpMediaConstraints.mandatory.add(
		//		          new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
		//		    }
	}

	@Override
	public void onSignalingChange(SignalingState newState) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onIceConnectionChange(IceConnectionState newState) {
		if (newState == IceConnectionState.CONNECTED) {
			//TODO: start sending live stream
			webRTCAdaptor.registerWebRTCClient(streamId, this);
			try {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("command", "notification");
				jsonObject.put("definition", "play_started");
				wsConnection.send(jsonObject.toJSONString());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}	
		}
		else if (newState == IceConnectionState.DISCONNECTED) {
			webRTCAdaptor.deregisterWebRTCClient(streamId, this);
			stop();

		}

	}

	@Override
	public void onIceConnectionReceivingChange(boolean receiving) {

	}

	@Override
	public void onIceGatheringChange(IceGatheringState newState) {

	}

	@Override
	public void onIceCandidate(IceCandidate candidate) {
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

	}

	@Override
	public void onAddStream(MediaStream stream) {

	}

	@Override
	public void onRemoveStream(MediaStream stream) {

	}

	@Override
	public void onDataChannel(DataChannel dataChannel) {

	}

	@Override
	public void onRenegotiationNeeded() {

	}

	@Override
	public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {

	}

	@Override
	public void onCreateSuccess(SessionDescription sdp) {
		peerConnection.setLocalDescription(this, sdp);

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

	@Override
	public void onSetSuccess() {

	}

	@Override
	public void onCreateFailure(String error) {

	}

	@Override
	public void onSetFailure(String error) {

	}


	@Override
	public void setRemoteDescription(SessionDescription sdp) {
		if (this.peerConnection != null) {
			this.peerConnection.setRemoteDescription(this, sdp);
		}
		else {
			logger.warn("Peer connection is null. It cannot add ice candidate");
		}	
	}


	public void addIceCandidate(IceCandidate iceCandidate) {
		if (this.peerConnection != null) {
			this.peerConnection.addIceCandidate(iceCandidate);
		}
		else {
			logger.warn("Peer connection is null. It cannot set remote description");
		}
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
		if (peerConnection != null) {
			this.peerConnection.close();
			this.peerConnection = null;
		}
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("command", "notification");
			jsonObject.put("definition", "play_finished");
			wsConnection.send(jsonObject.toJSONString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}	
	}

}
