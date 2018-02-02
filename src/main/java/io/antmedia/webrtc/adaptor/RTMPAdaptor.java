package io.antmedia.webrtc.adaptor;

import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_YUV420P;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.webrtc.AudioSink;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRenderer.Callbacks;
import org.webrtc.VideoRenderer.I420Frame;
import org.webrtc.VideoTrack;

import io.antmedia.recorder.FFmpegFrameRecorder;
import io.antmedia.recorder.Frame;
import io.antmedia.recorder.FrameRecorder;

public class RTMPAdaptor extends Adaptor {

	FFmpegFrameRecorder recorder;
	protected long startTime;


	private ExecutorService videoEncoderExecutor; 

	private ExecutorService audioEncoderExecutor;
	private boolean isStopped = false; 

	public RTMPAdaptor(FFmpegFrameRecorder recorder) {
		this.recorder = recorder;

		setSdpMediaConstraints(new MediaConstraints());
		getSdpMediaConstraints().mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
		getSdpMediaConstraints().mandatory.add(
				new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
	}

	@Override
	public void start() {
		videoEncoderExecutor = Executors.newSingleThreadExecutor();
		audioEncoderExecutor = Executors.newSingleThreadExecutor();
	}

	@Override
	public void stop() {
		if (isStopped) {
			return;
		}
		isStopped  = true;

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "notification");
		jsonObject.put("definition", "publish_finished");

		try {
			getWsConnection().send(jsonObject.toJSONString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		
		audioEncoderExecutor.shutdown();
		videoEncoderExecutor.shutdown();
		
		try {
			videoEncoderExecutor.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		try {
			if (peerConnection != null) {
				this.peerConnection.close();
				recorder.stop();
				this.peerConnection.dispose();
				this.peerConnectionFactory.dispose();
				peerConnection = null;
			}
		} catch (FrameRecorder.Exception e) {
			e.printStackTrace();
		}
	}



	@Override
	public void onAddStream(MediaStream stream) {
		log.warn("onAddStream");

		if (stream.getAudioTracks().size() > 0) {
			
			AudioTrack audioTrack = stream.getAudioTracks().getFirst();
			if (audioTrack != null) {
				audioTrack.addSink(new AudioSink() {
					private int audioFrameCount = 0;

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

						audioFrameCount++;
						if (bits_per_sample == 16)  {
							audioEncoderExecutor.execute(new Runnable() {

								@Override
								public void run() {
									long timeDiff = (System.currentTimeMillis() - startTime) * 1000;

									short[] data = new short[number_of_frames * number_of_channels];
									tempAudioBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data, 0, data.length);

									ShortBuffer audioBuffer = ShortBuffer.wrap(data);
									try {
										boolean result = recorder.recordSamples(sample_rate, number_of_channels, timeDiff, audioBuffer);
										if (!result) {
											System.out.println("could not audio sample");
										}
									} catch (FrameRecorder.Exception e) {
										e.printStackTrace();
									}

								}
							});
						}

					}
				});
			}
		}

		if (stream.getVideoTracks().size() > 0) {

			VideoTrack videoTrack = stream.getVideoTracks().getFirst();
			if (videoTrack != null) {
				videoTrack.addRenderer(new VideoRenderer(new Callbacks() {

					private int frameCount;

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
						videoEncoderExecutor.execute(new Runnable() {

							@Override
							public void run() {

								long pts = (System.currentTimeMillis() - startTime) * 1000;

								Frame frameCV = new Frame(frame.width, frame.height, Frame.DEPTH_UBYTE, 2);

								((ByteBuffer)(frameCV.image[0].position(0))).put(frame.yuvPlanes[0]);
								((ByteBuffer)(frameCV.image[0])).put(frame.yuvPlanes[1]);
								((ByteBuffer)(frameCV.image[0])).put(frame.yuvPlanes[2]);

								try {
									//boolean result = recorder.recordImage(frame.width, frame.height, 2, Frame.DEPTH_UBYTE, frame.yuvStrides[0], AV_PIX_FMT_YUV420P, frame.yuvPlanes);
								
									recorder.recordImage(frameCV.imageWidth, frameCV.imageHeight, frameCV.imageDepth,
											frameCV.imageChannels, frameCV.imageStride, AV_PIX_FMT_YUV420P, pts, frameCV.image);

								} catch (FrameRecorder.Exception e) {
									e.printStackTrace();
								}
								VideoRenderer.renderFrameDone(frame);

							}
						});
					}
				}));
			}
		}

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("command", "notification");
		jsonObject.put("definition", "publish_started");

		try {
			getWsConnection().send(jsonObject.toJSONString());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onSetSuccess() {
		peerConnection.createAnswer(this, getSdpMediaConstraints());
	}

}
