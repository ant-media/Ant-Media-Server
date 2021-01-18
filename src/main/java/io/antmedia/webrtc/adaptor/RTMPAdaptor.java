package io.antmedia.webrtc.adaptor;

import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceServer;
import org.webrtc.PeerConnection.TcpCandidatePolicy;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.SoftwareVideoEncoderFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoFrame.Buffer;
import org.webrtc.VideoSink;
import org.webrtc.VideoTrack;
import org.webrtc.WrappedNativeI420Buffer;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.recorder.Frame;
import io.antmedia.recorder.FrameRecorder;
import io.antmedia.webrtc.AudioFrameContext;
import io.antmedia.webrtc.VideoFrameContext;
import io.antmedia.webrtc.api.IAudioTrackListener;
import io.antmedia.websocket.WebSocketCommunityHandler;

public class RTMPAdaptor extends Adaptor {


	public static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
	public static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
	public static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
	public static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";

	FFmpegFrameRecorder recorder;
	private volatile long startTime;

	private static Logger logger = LoggerFactory.getLogger(RTMPAdaptor.class);

	private ScheduledExecutorService videoEncoderExecutor; 

	private ScheduledExecutorService audioEncoderExecutor;
	private AtomicBoolean isStopped = new AtomicBoolean(false);
	private ScheduledExecutorService signallingExecutor;
	private boolean enableAudio = false;

	private volatile int audioFrameCount = 0;
	private boolean started = false;
	private ScheduledFuture<?> audioDataSchedulerFuture;
	private WebRtcAudioTrack webRtcAudioTrack;

	public static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";

	private String stunServerUri ="stun:stun1.l.google.com:19302";
	private int portRangeMin = 0; 
	private int portRangeMax = 0;
	private boolean tcpCandidatesEnabled = true;
	private int height;
	private String outputURL;

	private int errorLoopCount = 0;
	private String format = "flv";

	private ConcurrentLinkedQueue<VideoFrameContext> videoFrameQueue = new ConcurrentLinkedQueue<>();

	private ConcurrentLinkedQueue<AudioFrame> audioFrameQueue = new ConcurrentLinkedQueue<>();

	private int lastFrameNumber = -1;
	private int dropFrameCount = 0;
	private ScheduledFuture<?> videoEncoderFuture = null;
	private ScheduledFuture<?> audioEncoderFuture = null;
	private int videoFrameCount = 0;

	private long videoFrameLastTimestampMs;

	public static class AudioFrame 
	{
		public final ByteBuffer data;
		public final int channels;
		public final int sampleRate;

		public AudioFrame(ByteBuffer data, int channels, int sampleRate) {
			this.data = data;
			this.channels = channels;
			this.sampleRate = sampleRate;
		}
	}

	public class WebRTCVideoSink implements VideoSink {

		private int videoFrameLogCounter = 0;

		@Override
		public void onFrame(VideoFrame frame) {
			if (startTime == 0) {
				startTime = System.currentTimeMillis();
				logger.info("Set startTime to {} in onFrame for stream:{}", startTime, getStreamId());
			}

			if (videoEncoderExecutor == null || videoEncoderExecutor.isShutdown()) {
				logger.warn("Video Encoder is null or shutdown for stream: {}", getStreamId());
				return;
			}

			frame.retain();
			videoFrameCount++;
			videoFrameLogCounter++;

			if (videoFrameLogCounter % 100 == 0) {
				logger.info("Received total video frames: {}  received fps: {} frame rotated width:{} rotated height:{} width:{} height:{} rotation:{}" , 
						videoFrameCount, videoFrameCount/((System.currentTimeMillis() - startTime)/1000), frame.getRotatedWidth(), frame.getRotatedHeight(), frame.getBuffer().getWidth(), frame.getBuffer().getHeight(), frame.getRotation());
				videoFrameLogCounter = 0;
			}

			long timestampMS = System.currentTimeMillis() - startTime;
			videoFrameLastTimestampMs = timestampMS;

			VideoFrameContext videoFrameContext = new VideoFrameContext(frame, timestampMS);

			videoFrameQueue.offer(videoFrameContext);

		}

	}


	public static FFmpegFrameRecorder initRecorder(String outputURL, int width, int height, String format) {
		FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputURL, width, height, 1);
		recorder.setFormat(format);
		recorder.setSampleRate(44100);
		// Set in the surface changed method
		recorder.setFrameRate(20);
		recorder.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
		recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
		recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
		recorder.setAudioChannels(2);
		recorder.setGopSize(40);
		recorder.setVideoQuality(29);
		recorder.setMaxBFrames(0);
		recorder.setVideoOption("tune", "zerolatency");
		return recorder;
	}

	public FFmpegFrameRecorder getNewRecorder(String outputURL, int width, int height, String format) {

		FFmpegFrameRecorder recorder = initRecorder(outputURL, width, height, format);

		try {
			recorder.start();
		} catch (FrameRecorder.Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			webSocketCommunityHandler.sendServerError(getStreamId(), getSession());
			//close the connection because it's useless
			stop();
		}

		return recorder;
	}

	public RTMPAdaptor(String outputURL, WebSocketCommunityHandler webSocketHandler, int height) {
		this(outputURL, webSocketHandler, height, "flv");
	}

	public RTMPAdaptor(String outputURL, WebSocketCommunityHandler webSocketHandler, int height, String format) {
		super(webSocketHandler);
		this.outputURL =  outputURL;
		this.format  = format;
		this.height = height;

		setSdpMediaConstraints(new MediaConstraints());
		getSdpMediaConstraints().mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		getSdpMediaConstraints().mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
	}

	public org.webrtc.VideoDecoderFactory getVideoDecoderFactory() {
		//let webrtc decode it
		return null;
	}

	public PeerConnectionFactory createPeerConnectionFactory(){
		PeerConnectionFactory.initialize(
				PeerConnectionFactory.InitializationOptions.builder()
				.createInitializationOptions());

		//support internal webrtc codecs
		SoftwareVideoEncoderFactory encoderFactory = null;
		org.webrtc.VideoDecoderFactory decoderFactory = getVideoDecoderFactory();

		PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
		options.disableNetworkMonitor = true;
		options.networkIgnoreMask = PeerConnectionFactory.Options.ADAPTER_TYPE_LOOPBACK;


		// in receiving stream only Audio Track should be enabled
		// in sending stream only AudioRecord should be enabled 
		JavaAudioDeviceModule adm = (JavaAudioDeviceModule)
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
						initAudioTrackExecutor();
					}
				})
				.createAudioDeviceModule();

		webRtcAudioTrack = adm.getAudioTrack();
		return  PeerConnectionFactory.builder()
				.setOptions(options)
				.setAudioDeviceModule(adm)
				.setVideoEncoderFactory(encoderFactory)
				.setVideoDecoderFactory(decoderFactory)
				.createPeerConnectionFactory();
	}

	@Override
	public void start() {
		videoEncoderExecutor = Executors.newSingleThreadScheduledExecutor();
		audioEncoderExecutor = Executors.newSingleThreadScheduledExecutor();
		signallingExecutor = Executors.newSingleThreadScheduledExecutor();

		signallingExecutor.execute(() -> {

			try {

				peerConnectionFactory = createPeerConnectionFactory();

				List<IceServer> iceServers = new ArrayList<>();
				iceServers.add(IceServer.builder(getStunServerUri()).createIceServer());

				PeerConnection.RTCConfiguration rtcConfig =
						new PeerConnection.RTCConfiguration(iceServers);


				// Enable DTLS for normal calls and disable for loopback calls.
				rtcConfig.enableDtlsSrtp = true;
				rtcConfig.minPort = portRangeMin;
				rtcConfig.maxPort = portRangeMax;
				rtcConfig.tcpCandidatePolicy = tcpCandidatesEnabled 
						? TcpCandidatePolicy.ENABLED 
								: TcpCandidatePolicy.DISABLED;

				peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, RTMPAdaptor.this);

				webSocketCommunityHandler.sendStartMessage(getStreamId(), getSession());

				videoEncoderFuture = videoEncoderExecutor.scheduleWithFixedDelay(this::encodeVideo, 10, 10, TimeUnit.MILLISECONDS);
				audioEncoderFuture = audioEncoderExecutor.scheduleWithFixedDelay(this::encodeAudio, 10, 10, TimeUnit.MILLISECONDS);
				started  = true;
			}catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}


		});

	}

	@Override
	public void stop() {
		if (isStopped.get()) {
			logger.info("Stopped already called. It's returning for stream: {}", getStreamId());
			return;
		}
		isStopped.set(true);

		if (audioDataSchedulerFuture != null) {
			audioDataSchedulerFuture.cancel(false);
		}

		if (videoEncoderFuture != null) {
			videoEncoderFuture.cancel(false);
		}
		logger.info("Video queue size: {} video frame last timestamp: {}", videoFrameQueue.size(), videoFrameLastTimestampMs);

		if (audioEncoderFuture != null) {
			audioEncoderFuture.cancel(false);
		}

		logger.info("Audio queue size: {} audio frame count: {}", audioFrameQueue.size(), audioFrameCount);

		logger.info("Scheduling stop procedure for stream: {}", getStreamId());
		signallingExecutor.execute(() -> {

			logger.info("Executing stop procedure for stream: {}", getStreamId());
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

	public void initAudioTrackExecutor() {
		audioDataSchedulerFuture = signallingExecutor.scheduleAtFixedRate(() -> {

			if (startTime == 0) {
				startTime = System.currentTimeMillis();
				logger.info("Set startTime to {} in Audio Track executor:{}", startTime, getStreamId());
			}

			if (audioEncoderExecutor == null || audioEncoderExecutor.isShutdown()) {
				logger.warn("Audio encoder is null or shutdown for stream:{} ", getStreamId());
				return;
			}

			audioFrameCount++;
			ByteBuffer playoutData = webRtcAudioTrack.getPlayoutData();

			audioFrameQueue.offer(new AudioFrame(playoutData, webRtcAudioTrack.getChannels(), webRtcAudioTrack.getSampleRate()));			

		}, 0, 10, TimeUnit.MILLISECONDS);
	}


	private void encodeAudio() 
	{	
		//null-check recorder because it's asynch and it may not be initialized in video encoder thread
		if (recorder != null) 
		{
			AudioFrame audioFrameContext = null;
			while ((audioFrameContext = audioFrameQueue.poll()) != null) 
			{
				if (!isStopped.get()) 
				{
					recordSamples(audioFrameContext);
				}
				else {
					logger.error("Stream has stopped but audio encoder is running for stream:{}", getStreamId());
				}
			}
		}

	}

	public void recordSamples(AudioFrame audioFrameContext) 
	{
		try {

			ShortBuffer audioBuffer = audioFrameContext.data.asShortBuffer();

			boolean result = recorder.recordSamples(audioFrameContext.sampleRate, audioFrameContext.channels, audioBuffer);
			if (!result) {
				logger.info("could not audio sample for stream Id {}", getStreamId());
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	public void initializeRecorder(VideoFrame frame) {
		if (recorder == null) 
		{
			long recorderStartTime = System.currentTimeMillis();
			int width = (frame.getRotatedWidth() * height) / frame.getRotatedHeight();
			if (width % 2 == 1) {
				width++;
			}
			recorder = getNewRecorder(outputURL, width, height, format);
			long diff = System.currentTimeMillis() - recorderStartTime;
			logger.info("Initialize recorder takes {}ms for stream: {}", diff, getStreamId());
		}
	}


	public void encodeVideo() 
	{
		VideoFrameContext videoFrameContext = null;
		while ((videoFrameContext = videoFrameQueue.poll()) != null)
		{
			if (!isStopped.get()) {

				//initialize recorder if it's not initialized
				initializeRecorder(videoFrameContext.videoFrame);

				int frameNumber = (int)(videoFrameContext.timestampMS * recorder.getFrameRate() / 1000f);

				if (frameNumber > lastFrameNumber) 
				{
					recorder.setFrameNumber(frameNumber);
					lastFrameNumber = frameNumber;

					Frame frameCV = new Frame(videoFrameContext.videoFrame.getRotatedWidth(), videoFrameContext.videoFrame.getRotatedHeight(), Frame.DEPTH_UBYTE, 2);

					Buffer buffer = videoFrameContext.videoFrame.getBuffer();
					int[] stride = new int[3];
					if (buffer instanceof WrappedNativeI420Buffer) {
						WrappedNativeI420Buffer wrappedBuffer = (WrappedNativeI420Buffer) buffer;
						((ByteBuffer)(frameCV.image[0].position(0))).put(wrappedBuffer.getDataY());
						((ByteBuffer)(frameCV.image[0])).put(wrappedBuffer.getDataU());
						((ByteBuffer)(frameCV.image[0])).put(wrappedBuffer.getDataV());

						stride[0] = wrappedBuffer.getStrideY();
						stride[1] = wrappedBuffer.getStrideU();
						stride[2] = wrappedBuffer.getStrideV();

						try {
							recorder.recordImage(frameCV.imageWidth, frameCV.imageHeight, frameCV.imageDepth,
									frameCV.imageChannels, stride, AV_PIX_FMT_YUV420P, frameCV.image);

						} catch (FrameRecorder.Exception e) {
							logger.error(ExceptionUtils.getStackTrace(e));
							errorLoopCount += 1;
							if (errorLoopCount > 5){
								webSocketCommunityHandler.sendServerError(getStreamId(), getSession());
								stop();
							}
						}
					}
					else {
						logger.error("Buffer is not type of WrappedNativeI420Buffer for stream: {}", recorder.getFilename());
					}
				}
				else {
					dropFrameCount ++;
					logger.debug("dropping video, total drop count: {} frame number: {} recorder frame number: {}", 
							dropFrameCount, frameNumber, lastFrameNumber);
				}
			}
			else {
				logger.error("Stream has stopped but video encoder is running for stream:{}", getStreamId());
			}
			videoFrameContext.videoFrame.release();

		}
	}

	@Override
	public void onAddStream(MediaStream stream) {
		log.warn("onAddStream for stream: {}", getStreamId());

		if (!stream.audioTracks.isEmpty()) {
			enableAudio = true;
		}

		if (!stream.videoTracks.isEmpty()) {

			VideoTrack videoTrack = stream.videoTracks.get(0);
			if (videoTrack != null) {

				videoTrack.addSink(new WebRTCVideoSink());
			}
		}
		else {
			logger.warn("There is no video track for stream: {}", getStreamId());
		}


		webSocketCommunityHandler.sendPublishStartedMessage(getStreamId(), getSession(), null);

	}

	@Override
	public void onSetSuccess() {
		peerConnection.createAnswer(this, getSdpMediaConstraints());
	}

	public void setRemoteDescription(final SessionDescription sdp) {
		signallingExecutor.execute(() -> 
		peerConnection.setRemoteDescription(RTMPAdaptor.this, sdp)
				);

	}

	public void addIceCandidate(final IceCandidate iceCandidate) {
		signallingExecutor.execute(() -> {

			if (!peerConnection.addIceCandidate(iceCandidate))
			{
				log.error("Add ice candidate failed for {}", iceCandidate);
			}

		});
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isStopped() {
		return isStopped.get();
	}

	public ScheduledFuture getAudioDataSchedulerFuture() {
		return audioDataSchedulerFuture;
	}

	public long getStartTime() {
		return startTime;
	}

	public String getStunServerUri() {
		return stunServerUri;
	}

	public void setStunServerUri(String stunServerUri) {
		this.stunServerUri = stunServerUri;
	}

	public void setPortRange(int webRTCPortRangeMin, int webRTCPortRangeMax) {
		this.portRangeMin = webRTCPortRangeMin;
		this.portRangeMax = webRTCPortRangeMax;
	}

	public void setTcpCandidatesEnabled(boolean tcpCandidatesEnabled) {
		this.tcpCandidatesEnabled  = tcpCandidatesEnabled;
	}

	public int getHeight() {
		return height;
	}

	public String getOutputURL() {
		return outputURL;
	}

	public void setRecorder(FFmpegFrameRecorder recorder) {
		this.recorder = recorder;
	}

	public void setWebRtcAudioTrack(WebRtcAudioTrack webRtcAudioTrack) {
		this.webRtcAudioTrack = webRtcAudioTrack;
	}

	public Queue<VideoFrameContext> getVideoFrameQueue() {
		return videoFrameQueue;
	}
	
	public Queue<AudioFrame> getAudioFrameQueue() {
		return audioFrameQueue;
	}
	
	public FFmpegFrameRecorder getRecorder() {
		return recorder;
	}
	
	public ScheduledExecutorService getVideoEncoderExecutor() {
		return videoEncoderExecutor;
	}
	
	public ScheduledExecutorService getAudioEncoderExecutor() {
		return audioEncoderExecutor;
	}
	
}
