package io.antmedia.ipcamera;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_FLAG_GLOBAL_HEADER;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avcodec.avcodec_parameters_copy;
import static org.bytedeco.javacpp.avformat.AVFMT_GLOBALHEADER;
import static org.bytedeco.javacpp.avformat.AVFMT_NOFILE;
import static org.bytedeco.javacpp.avformat.AVIO_FLAG_WRITE;
import static org.bytedeco.javacpp.avformat.av_interleaved_write_frame;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_write_trailer;
import static org.bytedeco.javacpp.avformat.avformat_alloc_output_context2;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_free_context;
import static org.bytedeco.javacpp.avformat.avformat_new_stream;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avformat.avformat_write_header;
import static org.bytedeco.javacpp.avformat.avio_closep;
import static org.bytedeco.javacpp.avutil.AV_ROUND_NEAR_INF;
import static org.bytedeco.javacpp.avutil.AV_ROUND_PASS_MINMAX;
import static org.bytedeco.javacpp.avutil.av_dict_free;
import static org.bytedeco.javacpp.avutil.av_dict_set;
import static org.bytedeco.javacpp.avutil.av_rescale_q;
import static org.bytedeco.javacpp.avutil.av_rescale_q_rnd;

import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Broadcast;

class CameraScheduler implements ICameraStream {

	protected static Logger logger = LoggerFactory.getLogger(CameraScheduler.class);

	private Broadcast camera;

	private MultiThreadedApplicationAdapter appAdaptor;

	private WorkerThread thread;

	private AVPacket pkt = new AVPacket();

	private long[] lastDTS;

	CameraScheduler(Broadcast camera, MultiThreadedApplicationAdapter appAdaptor) {
		this.camera = camera;
		this.appAdaptor = appAdaptor;
	}

	public boolean prepareInput(AVFormatContext inputFormatContext) {

		if (inputFormatContext == null) {
			logger.info("cannot allocate input context");
			return false;
		}

		AVDictionary optionsDictionary = new AVDictionary();

		av_dict_set(optionsDictionary, "rtsp_transport", "tcp", 0);
		int ret;
		logger.info("camera rtsp url" + camera.getRtspUrl());

		if ((ret = avformat_open_input(inputFormatContext, camera.getRtspUrl(), null, optionsDictionary)) < 0) {

			byte[] data = new byte[1024];
			avutil.av_strerror(ret, data, data.length);
			logger.info("cannot open input context with error: " + new String(data, 0, data.length));
			return false;
		}
		av_dict_free(optionsDictionary);

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			logger.info("Could not find stream information\n");
			return false;
		}

		lastDTS = new long[inputFormatContext.nb_streams()];

		for (int i = 0; i < lastDTS.length; i++) {
			lastDTS[i] = -1;
		}
		return true;
	}

	public boolean prepare(AVFormatContext inputFormatContext, AVFormatContext outputRTMPFormatContext) {
		if (prepareInput(inputFormatContext)) {
			return prepareOutput(inputFormatContext, outputRTMPFormatContext);
		}
		return false;
	}

	/*
	 * public AVFormatContext getInputContext() { return inputFormatContext; }
	 */

	private boolean prepareOutput(AVFormatContext inputFormatContext, AVFormatContext outputRTMPFormatContext) {
		// outputRTMPFormatContext = new AVFormatContext(null);

		int ret = avformat_alloc_output_context2(outputRTMPFormatContext, null, "flv", null);

		for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
			AVStream in_stream = inputFormatContext.streams(i);
			AVStream out_stream = avformat_new_stream(outputRTMPFormatContext, in_stream.codec().codec());

			ret = avcodec_parameters_copy(out_stream.codecpar(), in_stream.codecpar());
			if (ret < 0) {
				logger.warn("Cannot get codec parameters\n");
				return false;
			}

			out_stream.codec().codec_tag(0);
		}

		if ((outputRTMPFormatContext.oformat().flags() & AVFMT_GLOBALHEADER) != 0) {
			// out_stream->codec->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
			outputRTMPFormatContext.oformat()
					.flags(outputRTMPFormatContext.oformat().flags() | AV_CODEC_FLAG_GLOBAL_HEADER);
		}

		if ((outputRTMPFormatContext.flags() & AVFMT_NOFILE) == 0) {
			AVIOContext pb = new AVIOContext(null);

			// TODO: get application name from red5 context, do not use embedded
			// url
			String urlStr = "rtmp://localhost/LiveApp/" + camera.getName();
			// logger.debug("rtmp url: " + urlStr);
			//
			ret = avformat.avio_open(pb, urlStr, AVIO_FLAG_WRITE);
			if (ret < 0) {
				byte[] data = new byte[1024];
				avutil.av_strerror(ret, data, data.length);
				logger.debug("Cannot open url: " + urlStr + " error is " + new String(data, 0, data.length));
				return false;
			}
			outputRTMPFormatContext.pb(pb);

			ret = avformat_write_header(outputRTMPFormatContext, (AVDictionary) null);
			if (ret < 0) {
				logger.debug("Cannot write header to rtmp\n");
				return false;
			}
		}

		return true;
	}

	public class WorkerThread extends Thread {

		private volatile boolean stopRequestReceived = false;

		@Override
		public void run() {

			AVFormatContext inputFormatContext = new AVFormatContext(null); // avformat.avformat_alloc_context();
			AVFormatContext outputRTMPFormatContext = new AVFormatContext(null);
			;

			System.out.println("before prepare....................");
			if (!prepare(inputFormatContext, outputRTMPFormatContext)) {
				if (inputFormatContext != null) {
					avformat_close_input(inputFormatContext);
				}

				if (outputRTMPFormatContext != null) {
					if (outputRTMPFormatContext.pb() != null) {
						avio_closep(outputRTMPFormatContext.pb());
					}
					avformat_free_context(outputRTMPFormatContext);
				}

				logger.warn("Prepare for " + camera.getName() + " returned false");
				return;
			}

			try {

				while (true) {
					int ret = av_read_frame(inputFormatContext, pkt);
					if (ret < 0) {
						logger.info("cannot read frame from input context");

						break;
					}

					int packetIndex = pkt.stream_index();
					AVStream in_stream = inputFormatContext.streams(packetIndex);
					AVStream out_stream = outputRTMPFormatContext.streams(packetIndex);

					if (pkt.dts() < 0) {
						av_packet_unref(pkt);
						continue;
					}

					if (lastDTS[packetIndex] >= pkt.dts()) {
						// logger.warn("dts timestamps are not in correct order
						// last dts:" + lastDTS[packetIndex]
						// + " current dts:" + pkt.dts() + " fixing problem by
						// adding offset");

						pkt.dts(lastDTS[packetIndex] + 1);
					}

					lastDTS[packetIndex] = pkt.dts();
					if (pkt.dts() > pkt.pts()) {
						pkt.pts(pkt.dts());
					}

					pkt.pts(av_rescale_q_rnd(pkt.pts(), in_stream.time_base(), out_stream.time_base(),
							AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
					pkt.dts(av_rescale_q_rnd(pkt.dts(), in_stream.time_base(), out_stream.time_base(),
							AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
					pkt.duration(av_rescale_q(pkt.duration(), in_stream.time_base(), out_stream.time_base()));
					pkt.pos(-1);

					ret = av_interleaved_write_frame(outputRTMPFormatContext, pkt);
					if (ret < 0) {
						logger.info("cannot write frame to muxer");
						break;
					}
					av_packet_unref(pkt);

					if (stopRequestReceived) {
						logger.warn("breaking the loop");
						break;
					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			avformat_close_input(inputFormatContext);
			inputFormatContext = null;

			av_write_trailer(outputRTMPFormatContext);

			if ((outputRTMPFormatContext.flags() & AVFMT_NOFILE) == 0) {
				logger.warn("before avio_closep(outputRTMPFormatContext.pb());");
				avio_closep(outputRTMPFormatContext.pb());
				outputRTMPFormatContext.pb(null);
			}

			logger.warn("before avformat_free_context(outputRTMPFormatContext);");
			avformat_free_context(outputRTMPFormatContext);
			outputRTMPFormatContext = null;

		}

		public void setStopRequestReceived() {

			logger.warn("inside of setStopRequestReceived");
			stopRequestReceived = true;

		}

		public boolean isStopRequestReceived() {
			return stopRequestReceived;
		}
	}

	@Override
	public void startStream() {

		thread = new WorkerThread();
		thread.start();

		// this.appAdaptor.addScheduledOnceJob(10, this);
	}

	public boolean isRunning() {
		return thread.isAlive();
	}

	@Override
	public void stopStream() {
		logger.warn("stop stream called");
		thread.setStopRequestReceived();
	}

	public boolean isStopRequestReceived() {
		return thread.isStopRequestReceived();
	}

	public Broadcast getCamera() {
		return camera;
	}

	public void restart() {
		stopStream();
		new Thread() {
			public void run() {
				try {
					while (isRunning()) {
						logger.warn("thread isRunning");
						Thread.sleep(100);

					}

					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				startStream();

			};
		}.start();

	}

	/*
	 * @Override public void execute(ISchedulingService arg0) throws
	 * CloneNotSupportedException { SimpleDateFormat dtFormat = new
	 * SimpleDateFormat("yyyy-MM-dd_HH-mm"); Date dt = new Date(); String[]
	 * command = new String[8]; command[0] = "ffmpeg"; command[1] = "-i";
	 * command[2] = camera.rtspUrl; //
	 * "rtsp://10.10.31.58:554/user=admin&password=tlJwpbo6&channel=1&stream=0.sdp?real_stream";
	 * command[3] = "-codec"; command[4] = "copy"; command[5] = "-f"; command[6]
	 * = "flv"; command[7] = "rtmp://localhost/IPCamera/" + camera.name + "-" +
	 * dtFormat.format(dt); try { process = Runtime.getRuntime().exec(command);
	 * 
	 * Thread thread = new Thread() { public void run() { InputStream
	 * errorStream = process.getErrorStream(); byte[] data = new byte[2056]; int
	 * length;
	 * 
	 * try { while ((length = errorStream.read(data, 0, data.length))>0) {
	 * //System.err.println(new String(data, 0, length)); } } catch (IOException
	 * e) { e.printStackTrace(); }
	 * 
	 * 
	 * }; }; thread.setDaemon(true);
	 * 
	 * thread.setName(dtFormat.format(dt)); thread.start(); } catch (IOException
	 * e1) { // TODO Auto-generated catch block e1.printStackTrace(); }
	 * 
	 * }
	 */

}
