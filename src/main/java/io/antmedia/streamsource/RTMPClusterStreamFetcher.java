package io.antmedia.streamsource;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.messaging.IConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;
import io.antmedia.filter.JWTFilter;
import io.antmedia.muxer.RtmpProvider;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.IStreamFetcherListener;
import io.vertx.core.Vertx;

public class RTMPClusterStreamFetcher {

	private String rtmpUrl;
	private IScope scope;
	private Vertx vertx;
	public  RtmpProvider rtmpProvider;
	private AppSettings appSettings;
	private Logger logger = LoggerFactory.getLogger(RTMPClusterStreamFetcher.class);
	private RtmpFetcherThread thread;
	private AtomicBoolean threadActive = new AtomicBoolean(false);
	private IStreamFetcherListener streamFetcherListener;
	private String streamId;
	int checkNumberOfViewers = 0;
	private AtomicBoolean finishing = new AtomicBoolean(false);

	
	
	public class RtmpFetcherThread extends Thread {

		protected AtomicBoolean isJobRunning = new AtomicBoolean(false);
		AVFormatContext inputFormatContext = null;

		long firstPacketTime = 0;
		long bufferDuration = 0;
		long timeOffsetInMs = 0;
		private AVRational videoTimeBase;
		private AVRational audioTimeBase;


		public Result prepareInput(AVFormatContext inputFormatContext) {

			Result result = new Result(false);

			AVDictionary optionsDictionary = new AVDictionary();

			//analyze duration is a generic parameter
			int analyzeDurationUs = getAppSettings().getMaxAnalyzeDurationMS() * 1000;
			String analyzeDuration = String.valueOf(analyzeDurationUs);
			av_dict_set(optionsDictionary, "analyzeduration", analyzeDuration, 0);


			int ret;
			
			String rtmpUrlWithToken = getStreamUrl();

			logger.info("open stream url: {}", rtmpUrlWithToken);

			if ((ret = avformat_open_input(inputFormatContext, rtmpUrlWithToken, null, optionsDictionary)) < 0) {

				String errorStr = Muxer.getErrorDefinition(ret);
				result.setMessage(errorStr);

				logger.error("cannot open stream: {} with error:: {}",  rtmpUrlWithToken, result.getMessage());
				av_dict_free(optionsDictionary);
				optionsDictionary.close();
				return result;
			}

			av_dict_free(optionsDictionary);
			optionsDictionary.close();

			logger.debug("find stream info: {}  " , rtmpUrlWithToken);

			ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
			if (ret < 0) {
				result.setMessage("Could not find stream information\n");
				logger.error(result.getMessage());
				return result;
			}


			result.setSuccess(true);
			return result;

		}
		
		@SuppressWarnings("java:S2095")
		@Override
		public void run() {


			AVPacket pkt = null;
			videoTimeBase = new AVRational().num(1).den(1000);
			audioTimeBase = new AVRational().num(1).den(1000);

			rtmpProvider = initRtmpProvider(videoTimeBase, audioTimeBase);

			try {
				//update broadcast status to preparing

				setThreadActive(true);

				inputFormatContext = new AVFormatContext(null);
				pkt = avcodec.av_packet_alloc();
				if(prepareInputContext())
				{
					
					rtmpProvider.init(scope, streamId, 0, "", 0);

					for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
						
						rtmpProvider.addStream(inputFormatContext.streams(i).codecpar(), inputFormatContext.streams(i).time_base(), i);
					}
					
					boolean prepareIO = rtmpProvider.prepareIO();
					boolean readTheNextFrame = true;
					if(!prepareIO) {
						logger.error("Cannot prepare IO for stream: {}", rtmpUrl);
						close(pkt);
						return;
					}
					logger.info("Starting the RTMPClusterStream fetcher loop for stream: {}", rtmpUrl);
					
					while (readTheNextFrame) {
						try {
							//stay in the loop if exception occurs
							readTheNextFrame = readMore(pkt);
						}
						catch (Exception e) {
							logger.error(ExceptionUtils.getStackTrace(e));
						}
					}
					logger.info("Leaving the stream fetcher loop for stream: {}", rtmpUrl);

				}
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			finally {

				close(pkt);

				setThreadActive(false);
			}

		}

		public boolean readMore(AVPacket pkt) {
			boolean readTheNextFrame = true;
			int readResult = readNextPacket(pkt);
			if(readResult >= 0) {
				AVStream stream = inputFormatContext.streams(pkt.stream_index());
			
				rtmpProvider.writePacket(pkt, stream.time_base(), videoTimeBase, stream.codecpar().codec_type());
				
				unReferencePacket(pkt);
			}
			else {
				//break the loop except above case
				logger.warn("Cannot read next packet for url:{} and error is {}", rtmpUrl, Muxer.getErrorDefinition(readResult));
				readTheNextFrame = false;
			}
			
			checkNumberOfViewers++;
			if (checkNumberOfViewers % 500  == 0) 
			{
				IBroadcastScope broadcastScope = rtmpProvider.getBroadcastScope();
				if (broadcastScope != null) 
				{
					List<IConsumer> consumers = broadcastScope.getConsumers();
					logger.info("Checking the number of viewers for stream: {} and viewers: {}", rtmpUrl, consumers);
					checkNumberOfViewers = 0;
					if (consumers == null || consumers.isEmpty()) 
					{
						logger.info("No viewers, stopping the stream fetcher for stream: {}", rtmpUrl);
						readTheNextFrame = false;
						finishing.set(true);
					}
				}
			}
			

			return readTheNextFrame;
		}

		
		

		public int readNextPacket(AVPacket pkt) {
			return av_read_frame(inputFormatContext, pkt);
		}

		public void unReferencePacket(AVPacket pkt) {
			av_packet_unref(pkt);
		}

		public boolean prepareInputContext() throws Exception 
		{
			Result result = prepareInput(inputFormatContext);

			if (result.isSuccess()) {
				
				for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
					if (inputFormatContext.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
						if(avcodec.avcodec_find_decoder(inputFormatContext.streams(i).codecpar().codec_id()) == null) {
							logger.error("avcodec_find_decoder() error: Unsupported audio format or codec not found");
						}
					}
					else if (inputFormatContext.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
						if(avcodec.avcodec_find_decoder(inputFormatContext.streams(i).codecpar().codec_id()) == null) {
							logger.error("avcodec_find_decoder() error: Unsupported video format or codec not found");
						}
					}
				}
			}
			else {
				logger.error("Prepare for opening the {} has failed", rtmpUrl);
			}
			return result.isSuccess();
		}



		public synchronized void closeInputFormatContext() {
			if (inputFormatContext != null) {
				try {
					avformat_close_input(inputFormatContext);
				}
				catch (Exception e) {
					logger.info(e.getMessage());
				}
				inputFormatContext = null;
			}

		}
		public void close(AVPacket pkt) {
			try {

				if (pkt != null) {
					av_packet_free(pkt);
					pkt.close();
				}

				closeInputFormatContext();
				
				rtmpProvider.writeTrailer();
				
				streamFetcherListener.streamFinished(streamFetcherListener);

				logger.info("Leaving thread for {}", rtmpUrl);

			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));

			}
		}

	}



	public RTMPClusterStreamFetcher(String streamUrl, String streamId, IScope scope) {
		this.rtmpUrl = streamUrl;
		this.scope = scope;
		this.streamId = streamId;
		
		//init app settings
		getAppSettings();
	}

	
	public RtmpProvider initRtmpProvider(AVRational videoTimeBase, AVRational audioTimeBase) {
		return new RtmpProvider(scope, vertx, streamId, videoTimeBase, audioTimeBase);
	}

	public boolean isFinishing() {
		return finishing.get();
	}

	
	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) scope.getContext().getApplicationContext().getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	
	public void setStreamFetcherListener(IStreamFetcherListener streamFetcherListener) {
		this.streamFetcherListener = streamFetcherListener;
	}


	public String getStreamUrl(){
		String jwtToken = JWTFilter.generateJwtToken(
				getAppSettings().getClusterCommunicationKey(),
				System.currentTimeMillis() + 30000);
		return this.rtmpUrl + "?token=" + jwtToken;
	}
	
	public void setThreadActive(boolean threadActive) {
		this.threadActive.set(threadActive);
	}
	
	public AtomicBoolean getThreadActive() {
		return threadActive;
	}
	
	public void startStream() {

		new Thread() {
			@Override
			public void run() {
				try {
					int i = 0;
					while (threadActive.get()) {
						Thread.sleep(100);
						if (i % 50 == 0) {
							logger.info("waiting for thread to be finished for stream {}", rtmpUrl);
							i = 0;
						}
						i++;
					}
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
					Thread.currentThread().interrupt();
				}

				thread = new RtmpFetcherThread();
				thread.start();
				logger.info("StartStream called, new thread is started for {}", rtmpUrl);
			}
		}.start();

	}
	
	public RtmpProvider getRtmpProvider() {
		return rtmpProvider;
	}

}
