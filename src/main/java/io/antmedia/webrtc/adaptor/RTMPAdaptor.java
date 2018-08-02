package io.antmedia.webrtc.adaptor;

import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.AudioSink;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.Callbacks;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoTrack;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.recorder.Frame;
import io.antmedia.recorder.FrameRecorder;
import io.antmedia.websocket.WebSocketCommunityHandler;

public class RTMPAdaptor extends Adaptor {

	FFmpegFrameRecorder recorder;
	protected long startTime;

	private static Logger logger = LoggerFactory.getLogger(RTMPAdaptor.class);

	private ExecutorService videoEncoderExecutor; 

	private ExecutorService audioEncoderExecutor;
	private volatile boolean isStopped = false;
	private ExecutorService signallingExecutor;
	private boolean enableAudio = false;

	private int audioFrameCount = 0;
	private boolean started = false;

	public static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";

	public RTMPAdaptor(FFmpegFrameRecorder recorder, WebSocketCommunityHandler webSocketHandler) {
		super(webSocketHandler);
		this.recorder = recorder;

		setSdpMediaConstraints(new MediaConstraints());
		getSdpMediaConstraints().mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		getSdpMediaConstraints().mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
	}

	public PeerConnectionFactory createPeerConnectionFactory(){
		PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		options.networkIgnoreMask = 0;
		return new PeerConnectionFactory(options);

	}

	@Override
	public void start() {
		videoEncoderExecutor = Executors.newSingleThreadExecutor();
		audioEncoderExecutor = Executors.newSingleThreadExecutor();
		signallingExecutor = Executors.newSingleThreadExecutor();

		signallingExecutor.execute(() -> {

			List<IceServer> iceServers = new ArrayList<>();
			iceServers.add(new IceServer("stun:stun.l.google.com:19302"));
			PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);

			MediaConstraints pcConstraints = new MediaConstraints();
			pcConstraints.optional.add(
					new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));

			peerConnectionFactory = createPeerConnectionFactory();
			peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pcConstraints, RTMPAdaptor.this);

			webSocketCommunityHandler.sendStartMessage(getStreamId(), getSession());

			started  = true;


		});

	}

	@Override
	public void stop() {
		if (isStopped) {
			return;
		}
		isStopped  = true;

		signallingExecutor.execute(() -> {

			webSocketCommunityHandler.sendPublishFinishedMessage(getStreamId(), getSession());


			audioEncoderExecutor.shutdownNow();
			videoEncoderExecutor.shutdownNow();

			try {
				videoEncoderExecutor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (InterruptedException e1) {
				logger.error(ExceptionUtils.getStackTrace(e1));
				Thread.currentThread().interrupt();
			}
			try {
				if (peerConnection != null) {
					peerConnection.close();
					recorder.stop();
					peerConnection.dispose();
					peerConnectionFactory.dispose();
					peerConnection = null;
				}
			} catch (FrameRecorder.Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}

		});
		signallingExecutor.shutdown();
	}
	
	public ExecutorService getSignallingExecutor() {
		return signallingExecutor;
	}



	@Override
	public void onAddStream(MediaStream stream) {
		log.warn("onAddStream for stream: {}", getStreamId());

		if (!stream.getAudioTracks().isEmpty()) {

			AudioTrack audioTrack = stream.getAudioTracks().getFirst();
			if (audioTrack != null) {

				enableAudio  = true;
				audioTrack.addSink(new AudioSink() {


					@Override
					public void onData(byte[] audio_data, int bits_per_sample, final int sample_rate, final int number_of_channels,
							final int number_of_frames) {
						final ByteBuffer tempAudioBuffer = ByteBuffer.wrap(audio_data);

						if (startTime == 0) {
							startTime = System.currentTimeMillis();
						}

						if (audioEncoderExecutor == null || audioEncoderExecutor.isShutdown()) {
							return;
						}


						if (bits_per_sample == 16)  {
							audioFrameCount++;

							audioEncoderExecutor.execute(() -> {

								short[] data = new short[number_of_frames * number_of_channels];
								tempAudioBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data, 0, data.length);

								ShortBuffer audioBuffer = ShortBuffer.wrap(data);
								try {
									boolean result = recorder.recordSamples(sample_rate, number_of_channels, audioBuffer);
									if (!result) {
										logger.info("could not audio sample for stream Id {}", getStreamId());
									}
								} catch (FrameRecorder.Exception e) {
									logger.error(ExceptionUtils.getStackTrace(e));
								}


							});
						}

					}
				});
			}
		}

		if (!stream.getVideoTracks().isEmpty()) {

			VideoTrack videoTrack = stream.getVideoTracks().getFirst();
			if (videoTrack != null) {
				videoTrack.addRenderer(new VideoRenderer(new Callbacks() {

					private int frameCount;
					private int dropFrameCount = 0;

					@Override
					public void renderFrame(final I420Frame frame) {
						if (startTime == 0) {
							startTime = System.currentTimeMillis();
						}

						if (videoEncoderExecutor == null || videoEncoderExecutor.isShutdown()) {
							VideoRenderer.renderFrameDone(frame);
							return;
						}

						frameCount++;
						videoEncoderExecutor.execute(() -> {

							long pts;
							if (enableAudio) {
								//each audio frame is 10 ms and then multiply with 1000 for microseconds
								pts = (long)audioFrameCount * 10;
								logger.trace("audio frame count: {}", audioFrameCount);
							}
							else {
								pts = (System.currentTimeMillis() - startTime) * 1000;
							}

							int frameNumber = (int)(pts * recorder.getFrameRate() / 1000);
							if (frameNumber > recorder.getFrameNumber()) {

								recorder.setFrameNumber(frameNumber);

								Frame frameCV = new Frame(frame.width, frame.height, Frame.DEPTH_UBYTE, 2);

								((ByteBuffer)(frameCV.image[0].position(0))).put(frame.yuvPlanes[0]);
								((ByteBuffer)(frameCV.image[0])).put(frame.yuvPlanes[1]);
								((ByteBuffer)(frameCV.image[0])).put(frame.yuvPlanes[2]);

								try {
									recorder.recordImage(frameCV.imageWidth, frameCV.imageHeight, frameCV.imageDepth,
											frameCV.imageChannels, frameCV.imageStride, AV_PIX_FMT_YUV420P, frameCV.image);

								} catch (FrameRecorder.Exception e) {
									logger.error(ExceptionUtils.getStackTrace(e));
								}
							}
							else {
								dropFrameCount ++;
								logger.debug("dropping video, total drop count: {}", dropFrameCount);
							}
							VideoRenderer.renderFrameDone(frame);

						});
					}
				}));
			}
		}

		webSocketCommunityHandler.sendPublishStartedMessage(getStreamId(), getSession());

	}

	@Override
	public void onSetSuccess() {
		peerConnection.createAnswer(this, getSdpMediaConstraints());
	}

	public void setRemoteDescription(final SessionDescription sdp) {
		signallingExecutor.execute(new Runnable() {

			@Override
			public void run() {
				peerConnection.setRemoteDescription(RTMPAdaptor.this, sdp);

			}
		});

	}

	public void addIceCandidate(final IceCandidate iceCandidate) {
		signallingExecutor.execute(new Runnable() {

			@Override
			public void run() {
				if (!peerConnection.addIceCandidate(iceCandidate))
				{
					log.error("Add ice candidate failed");
				}

			}
		});
	}

	public boolean isStarted() {
		return started;
	}



}
