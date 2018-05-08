package io.antmedia.streamsource;

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

import java.util.List;

import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVIOContext;
import org.bytedeco.javacpp.avformat.AVStream;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.rest.model.Result;
import io.antmedia.storage.StorageClient;

public class StreamFetcher {

	protected static Logger logger = LoggerFactory.getLogger(StreamFetcher.class);

	private Broadcast stream;

	private WorkerThread thread;

	private AVPacket pkt = new AVPacket();

	/**
	 * Connection setup timeout value
	 */
	private int timeout;

	public boolean exceptionInThread = false;

	/**
	 * Last packet received time
	 */
	private long lastPacketReceivedTime = 0;

	private boolean threadActive = false;
	
	private Result cameraError=new Result(false,"");



	private static final int PACKET_RECEIVED_INTERVAL_TIMEOUT = 3000;
	
	private IScope scope;



	public StreamFetcher(Broadcast stream, IScope scope) {
		this.stream = stream;
		this.scope=scope;
		
		logger.debug(":::::::::::scope is ::::::::" + String.valueOf(scope));

	}

	public StreamFetcher(Broadcast stream) {
		this.stream = stream;

	}

	public Result prepareInput(AVFormatContext inputFormatContext) {

		setConnectionTimeout(5000);

		Result result = new Result(false);
		if (inputFormatContext == null) {
			logger.info("cannot allocate input context");
			return result;
		}

		if (stream == null || stream.getStreamUrl() == null) {
			logger.info("stream is null");
			return result;
		}
		
		AVDictionary optionsDictionary = new AVDictionary();

		String streamUrl = stream.getStreamUrl();
		if (streamUrl.startsWith("rtsp://")) {
			av_dict_set(optionsDictionary, "rtsp_transport", "tcp", 0);
		}
		else {
			
		}
		
		String timeout = String.valueOf(this.timeout);
		av_dict_set(optionsDictionary, "stimeout", timeout, 0);


		int ret;

		logger.info("stream url:  " + stream.getStreamUrl());

		if ((ret = avformat_open_input(inputFormatContext, stream.getStreamUrl(), null, optionsDictionary)) < 0) {

			byte[] data = new byte[1024];
			avutil.av_strerror(ret, data, data.length);

			String errorStr=new String(data, 0, data.length);
			
			result.setMessage(errorStr);		
			
			logger.info("cannot open input context with error::" +result.getMessage());
			return result;
		}
		
		
		
		av_dict_free(optionsDictionary);

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			
			result.setMessage("Could not find stream information\n");
			logger.info(result.getMessage());
			return result;
		}

		result.setSuccess(true);
		return result;

	}


	public Result prepare(AVFormatContext inputFormatContext) {


		Result result = prepareInput(inputFormatContext);
		
		setCameraError(result);

		return result;

	}

	public class WorkerThread extends Thread {

		private volatile boolean stopRequestReceived = false;


		@Override
		public void run() {
		
			setThreadActive(true);
			AVFormatContext inputFormatContext = new AVFormatContext(null); // avformat.avformat_alloc_context();
			//AVFormatContext outputRTMPFormatContext = new AVFormatContext(null);
	
			logger.info("before prepare");
			

			Result result = prepare(inputFormatContext);


			try {

				if (result.isSuccess()) {

					
					MuxAdaptor muxAdaptor = new MuxAdaptor(null);
					muxAdaptor.setHLSMuxingEnabled(true);

					muxAdaptor.setMp4MuxingEnabled(true, false, "aac_adtstoasc");
					
					muxAdaptor.setHlsListSize("5");

					muxAdaptor.setHlsTime("2");
					
					muxAdaptor.setHlsPlayListType("");
					
					
					muxAdaptor.init(scope, stream.getStreamId(), false);
					logger.info("stream count in stream {} is {}", stream.getStreamUrl(), inputFormatContext.nb_streams());
					muxAdaptor.prepareMuxers(inputFormatContext);
					
					
					//muxAdaptor.start();


					while (true) {
						int ret = av_read_frame(inputFormatContext, pkt);
						if (ret < 0) {
							logger.info("cannot read frame from input context");

							break;
						}

						lastPacketReceivedTime = System.currentTimeMillis();

						muxAdaptor.writePacketToMuxers(inputFormatContext, pkt);

						if (ret < 0) {
							logger.info("cannot write frame to muxer");
							break;
						}

						if (stopRequestReceived) {
							logger.warn("breaking the loop");
							break;
						}

					}
					
					avformat_close_input(inputFormatContext);
					inputFormatContext = null;
					
					muxAdaptor.writeTrailer(inputFormatContext);

				}
				else {

					if (inputFormatContext != null) {
						avformat_close_input(inputFormatContext);
					}

					logger.warn("Prepare for " + stream.getName() + " returned false");
				}
				
				setCameraError(result);
				
				logger.info("Leaving StreamFetcher Thread");

			} catch (Exception e) {
				logger.info("---Exception in thread---");
				e.printStackTrace();
				exceptionInThread  = true;
			}
			
			setThreadActive(false);
			
		}

		public void setStopRequestReceived() {
			logger.warn("inside of setStopRequestReceived");
			stopRequestReceived = true;

		}

		public boolean isStopRequestReceived() {
			return stopRequestReceived;
		}
	}

	public void startStream() {
		new Thread() {
			public void run() {
				try {
					while (threadActive) {
						Thread.sleep(100);
					}
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}

				exceptionInThread = false;
				thread = new WorkerThread();
				thread.start();
				logger.info("StartStream called, new thread is started");
			};
		}.start();

	}
	
	
	public void functi() {
		IContext context = getScope().getContext(); 
		ApplicationContext appCtx = context.getApplicationContext(); 
		
		
		
		boolean mp4MuxingEnabled = true;
		boolean addDateTimeToMp4FileName=false;
		boolean webRTCEnabled = false;
		StorageClient storageClient = null;
		boolean hlsMuxingEnabled = true;
		List<EncoderSettings> adaptiveResolutionList = null;
		String hlsListSize = "5";
		String hlsTime = "2";
		String hlsPlayListType = null;
		boolean deleteHLSFilesOnExit = true;
		boolean isPreviewOverwrite = false;
		if (appCtx.containsBean("app.settings"))  {
			
			logger.warn("::::: settings found::::::::.");
			
			AppSettings appSettings = (AppSettings) appCtx.getBean("app.settings");
			mp4MuxingEnabled = appSettings.isMp4MuxingEnabled();
			addDateTimeToMp4FileName = appSettings.isAddDateTimeToMp4FileName();
			hlsMuxingEnabled = appSettings.isHlsMuxingEnabled();
			adaptiveResolutionList = appSettings.getAdaptiveResolutionList();
			hlsListSize = appSettings.getHlsListSize();
			hlsTime = appSettings.getHlsTime();
			hlsPlayListType = appSettings.getHlsPlayListType();
			webRTCEnabled = appSettings.isWebRTCEnabled();
			deleteHLSFilesOnExit = appSettings.isDeleteHLSFilesOnExit();
			isPreviewOverwrite = appSettings.isPreviewOverwrite();
			
			logger.info("::::: adaptiveResolutionList::::::::." + String.valueOf(adaptiveResolutionList));
		}
	}

	/**
	 * If thread is alive and receiving packet with in the {@link PACKET_RECEIVED_INTERVAL_TIMEOUT} time
	 * mean it is running
	 * @return true if it is running and false it is not
	 */
	public boolean isStreamAlive() {

		return ((System.currentTimeMillis() - lastPacketReceivedTime) < PACKET_RECEIVED_INTERVAL_TIMEOUT);
	}

	public boolean isStopped() {
		return thread.isInterrupted();
	}

	public void stopStream() {

		if(getThread()!=null) {
			logger.warn("stop stream called");
			getThread().setStopRequestReceived();

		}else {

			logger.warn("thread is null");
		}
	}

	public boolean isStopRequestReceived() {
		return getThread().isStopRequestReceived();
	}

	public WorkerThread getThread() {
		return thread;
	}

	public void setThread(WorkerThread thread) {
		this.thread = thread;
	}

	public Broadcast getStream() {
		return stream;
	}

	public void restart() {
		stopStream();
		new Thread() {
			public void run() {
				try {
					while (threadActive) {
						Thread.sleep(100);

					}

					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					Thread.currentThread().interrupt();
				}

				startStream();

			};
		}.start();

	}
	

	
	

	/**
	 * Set timeout when establishing connection
	 * @param timeout in ms
	 */
	public void setConnectionTimeout(int timeout) {
		this.timeout = timeout * 1000;
	}

	public boolean isExceptionInThread() {
		return exceptionInThread;
	}

	public void setThreadActive(boolean threadActive) {
		this.threadActive = threadActive;
	}

	public boolean isThreadActive() {
		return threadActive;
	}
	public Result getCameraError() {
		return cameraError;
	}


	public void setCameraError(Result cameraError) {
		this.cameraError = cameraError;
	}
	public IScope getScope() {
		return scope;
	}

	public void setScope(IScope scope) {
		this.scope = scope;
	}

}
