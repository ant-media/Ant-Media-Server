package io.antmedia.webrtc.adaptor;

import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avutil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;
import org.webrtc.WrappedNativeI420Buffer;
import org.webrtc.VideoFrame.Buffer;
import org.webrtc.audio.WebRtcAudioTrack;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.recorder.Frame;
import io.antmedia.recorder.FrameRecorder;
import io.vertx.core.Vertx;

public class RTMPRecorder extends FFmpegFrameRecorder implements VideoSink{
	private static final String RTMP_URL_ROOT = "rtmp://127.0.0.1/StreamApp/";
	private Vertx vertx;
	private String streamId;
	private static Logger logger = LoggerFactory.getLogger(RTMPRecorder.class);
	private int frameCount;
	private int dropFrameCount = 0;
	private long pts;
	private int frameNumber;
	private int videoFrameLogCounter = 0;
	private int lastFrameNumber = -1;
	private long startTime;
	private boolean enableAudio = true;
	private long audioFrameCount;

	public RTMPRecorder(String streamId, Vertx vertx) {
		super(RTMP_URL_ROOT + streamId, 640, 480);
		this.vertx = vertx;
		setStreamId(streamId);
		setFormat("flv");
		setSampleRate(44100);
		// Set in the surface changed method
		setFrameRate(20);
		setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
		setVideoCodec(avcodec.AV_CODEC_ID_H264);
		setAudioCodec(avcodec.AV_CODEC_ID_AAC);
		setAudioChannels(2);
		setGopSize(40);
		setVideoQuality(29);
	}
	
	public void startAudioStreaming(WebRtcAudioTrack webRtcAudioTrack) {
		vertx.setPeriodic(10, l -> {
			if (startTime == 0) {
				startTime = System.currentTimeMillis();
			}
			
			ByteBuffer playoutData = webRtcAudioTrack.getPlayoutData();
			if(playoutData != null) {
				audioFrameCount++;
				vertx.runOnContext(h -> {

					ShortBuffer audioBuffer = playoutData.asShortBuffer();
					try {
						boolean result = recordSamples(webRtcAudioTrack.getSampleRate(), webRtcAudioTrack.getChannels(), audioBuffer);
						if (!result) {
							logger.info("could not audio sample for stream Id {}", getStreamId());
						}
					} catch (FrameRecorder.Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				});
			}
		});
	}
	
	@Override
	public void onFrame(VideoFrame frame) {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
		}
		frame.retain();
		frameCount++;
		videoFrameLogCounter++;

		if (videoFrameLogCounter % 100 == 0) {
			logger.info("Received total video frames: {}  received fps: {}" , 
					frameCount, frameCount/((System.currentTimeMillis() - startTime)/1000));
			videoFrameLogCounter = 0;

		}

		vertx.executeBlocking(bh -> {
			if (enableAudio ) {
				//each audio frame is 10 ms 
				pts = (long)audioFrameCount * 10;
				logger.trace("audio frame count: {}", audioFrameCount);
			}
			else {
				pts = (System.currentTimeMillis() - startTime);
			}

			frameNumber = (int)(pts * getFrameRate() / 1000f);

			if (frameNumber > lastFrameNumber) {

				setFrameNumber(frameNumber);
				lastFrameNumber = frameNumber;

				Frame frameCV = new Frame(frame.getRotatedWidth(), frame.getRotatedHeight(), Frame.DEPTH_UBYTE, 2);

				Buffer buffer = frame.getBuffer();
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
						recordImage(frameCV.imageWidth, frameCV.imageHeight, frameCV.imageDepth,
								frameCV.imageChannels, stride, AV_PIX_FMT_YUV420P, frameCV.image);

					} catch (FrameRecorder.Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
				else {
					logger.error("Buffer is not type of WrappedNativeI420Buffer for stream: {}", getFilename());
				}
			}
			else {
				dropFrameCount ++;
				logger.trace("dropping video, total drop count: {} frame number: {} recorder frame number: {}", 
						dropFrameCount, frameNumber, lastFrameNumber);
			}
			frame.release();
		}, rh->{});

	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

}
