package io.antmedia.streamsource;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IApplicationAdaptorFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.RtmpMuxer;
import io.antmedia.rest.model.Result;
import io.vertx.core.Vertx;

public class StreamFetcher {

	private static final String STREAM_TYPE_VOD = "VoD";

	protected static Logger logger = LoggerFactory.getLogger(StreamFetcher.class);
	private WorkerThread thread;
	/**
	 * Connection setup timeout value
	 */
	private int timeout;
	private boolean exceptionInThread = false;

	/**
	 * Last packet received time
	 */
	private long lastPacketReceivedTime = 0;
	private boolean threadActive = false;
	private Result cameraError = new Result(false,"");
	private static final int PACKET_RECEIVED_INTERVAL_TIMEOUT = 3000;
	private IScope scope;
	private AntMediaApplicationAdapter appInstance;
	private long[] lastDTS;
	private MuxAdaptor muxAdaptor = null;

	/**
	 * If it is true, it restarts fetching everytime it disconnects
	 * if it is false, it does not restart
	 */
	private boolean restartStream = true;

	/**
	 * This flag closes the stream in the worker thread. It should be a field of StreamFetcher. 
	 * Because WorkerThread instance can be re-created and we can lost the flag value.
	 * This case causes stream NOT TO BE STOPPED
	 */
	private volatile boolean stopRequestReceived = false;

	/**
	 * Buffer time in milliseconds
	 */
	private int bufferTime = 0;
	
	private static final int COUNT_TO_LOG_BUFFER = 500;
	
	private int bufferLogCounter;
	
	private ConcurrentLinkedQueue<AVPacket> availableBufferQueue = new ConcurrentLinkedQueue<>();

	private AppSettings appSettings;
	private Vertx vertx;
	
	private DataStore dataStore;

	public interface IStreamFetcherListener {

		void streamFinished(IStreamFetcherListener listener);

	}

	IStreamFetcherListener streamFetcherListener;

	private String streamUrl;

	private String streamId;

	private String streamType;
	
	public boolean isSeek;
	
	public int streamTime = 0;

	public IStreamFetcherListener getStreamFetcherListener() {
		return streamFetcherListener;
	}

	public void setStreamFetcherListener(IStreamFetcherListener streamFetcherListener) {
		this.streamFetcherListener = streamFetcherListener;
	}

	public StreamFetcher(String streamUrl, String streamId, String streamType, IScope scope, Vertx vertx)  {
		if (streamUrl == null  || streamId == null) {
			
			throw new NullPointerException("Stream is not initialized properly. Check "
					+ " stream id ("+ streamId +") and stream url ("+ streamUrl + ") values");
		}

		this.streamUrl = streamUrl;
		this.streamType = streamType;
		this.streamId = streamId;
		this.scope = scope;
		this.vertx = vertx;


		if (getAppSettings() == null) {
			throw new NullPointerException("App Settings is null in StreamFetcher");
		}

		this.bufferTime = getAppSettings().getStreamFetcherBufferTime();
	}

	public Result prepareInput(AVFormatContext inputFormatContext) {

		setConnectionTimeout(5000);

		Result result = new Result(false);
		if (inputFormatContext == null) {
			logger.info("cannot allocate input context for {}", streamId);
			return result;
		}

		AVDictionary optionsDictionary = new AVDictionary();

		String transportType = appSettings.getRtspPullTransportType();
		if (streamUrl.startsWith("rtsp://") && !transportType.isEmpty()) {
			logger.info("Setting rtsp transport type to {} for stream source: {}", transportType, streamUrl);
			av_dict_set(optionsDictionary, "rtsp_transport", transportType, 0);
		}

		String timeoutStr = String.valueOf(this.timeout);
		av_dict_set(optionsDictionary, "stimeout", timeoutStr, 0);

		int ret;

		logger.debug("stream url: {}  " , streamUrl);

		if ((ret = avformat_open_input(inputFormatContext, streamUrl, null, optionsDictionary)) < 0) {

			byte[] data = new byte[100];
			avutil.av_strerror(ret, data, data.length);

			String errorStr=new String(data, 0, data.length);

			result.setMessage(errorStr);		

			logger.error("cannot open stream: {} with error:: {}",  streamUrl, result.getMessage());
			av_dict_free(optionsDictionary);
			optionsDictionary.close();
			return result;
		}

		av_dict_free(optionsDictionary);
		optionsDictionary.close();

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			result.setMessage("Could not find stream information\n");
			logger.error(result.getMessage());
			return result;
		}

		lastDTS = new long[inputFormatContext.nb_streams()];

		for (int i = 0; i < lastDTS.length; i++) {
			lastDTS[i] = -1;
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

		private static final int PACKET_WRITER_PERIOD_IN_MS = 10;

		private static final long STREAM_FETCH_RE_TRY_PERIOD_MS = 3000;

		private volatile boolean streamPublished = false;
		protected AtomicBoolean isJobRunning = new AtomicBoolean(false);
		AVFormatContext inputFormatContext = null;



		private volatile boolean buffering = false;
		private ConcurrentLinkedQueue<AVPacket> bufferQueue = new ConcurrentLinkedQueue<>();

		private volatile long bufferingFinishTimeMs;

		private volatile long firstPacketReadyToSentTimeMs;

		private long lastPacketTimeMsInQueue;

		private long seekDurationMS = 0;

		@Override
		public void run() {

			setThreadActive(true);
			long firstPacketTime = 0;
			long bufferDuration = 0;
			long timeOffset = 0;

			AVPacket pkt = null;
			long packetWriterJobName = -1L;
			try {
				inputFormatContext = new AVFormatContext(null); 
				pkt = avcodec.av_packet_alloc();
				logger.info("Preparing the StreamFetcher for {}", streamUrl);
				Result result = prepare(inputFormatContext);


				if (result.isSuccess()) {
					boolean audioExist = false;
					boolean videoExist = false;
					for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
						if (inputFormatContext.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
							audioExist = true;
							if(avcodec.avcodec_find_decoder(inputFormatContext.streams(i).codecpar().codec_id()) == null) {
								logger.error("avcodec_find_decoder() error: Unsupported audio format or codec not found");
								audioExist = false;
							}
						}
						else if (inputFormatContext.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
							videoExist = true;
							if(avcodec.avcodec_find_decoder(inputFormatContext.streams(i).codecpar().codec_id()) == null) {
								logger.error("avcodec_find_decoder() error: Unsupported video format or codec not found");
								videoExist = false;
							}
						}
					}
					
					
					muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null,true, scope);
					// if there is only audio, firstKeyFrameReceivedChecked should be true in advance
					// because there is no video frame
					muxAdaptor.setFirstKeyFrameReceivedChecked(!videoExist); 
					muxAdaptor.setEnableVideo(videoExist);
					muxAdaptor.setEnableAudio(audioExist);
					muxAdaptor.setBroadcast(getDataStore().get(streamId));
					//if stream is rtsp, then it's not AVC
					muxAdaptor.setAvc(!streamUrl.toLowerCase().startsWith("rtsp"));
										
					setUpEndPoints(streamId, muxAdaptor);

					muxAdaptor.init(scope, streamId, false);

					logger.info("{} stream count in stream {} is {}", streamId, streamUrl, inputFormatContext.nb_streams());

					if(muxAdaptor.prepareFromInputFormatContext(inputFormatContext)) {
						
						while (av_read_frame(inputFormatContext, pkt) >= 0) {
							
							if(!streamPublished) {
								long currentTime = System.currentTimeMillis();
								muxAdaptor.setStartTime(currentTime);

								getInstance().startPublish(streamId, 0, "Pull");

								if (bufferTime > 0) {
									packetWriterJobName = vertx.setPeriodic(PACKET_WRITER_PERIOD_IN_MS, l-> 
										vertx.executeBlocking(h-> {
											writeBufferedPacket();
											h.complete();
										}, false, null)
									);
								}
							}

							streamPublished = true;
							lastPacketReceivedTime = System.currentTimeMillis();

							/**
							 * Check that dts values are monotically increasing for each stream
							 */
							int packetIndex = pkt.stream_index();
							
							if(!STREAM_TYPE_VOD.equals(streamType) && ! streamUrl.contains(".mp4") ) {
								if (lastDTS[packetIndex] >= pkt.dts()) {
									logger.info("last dts{} is bigger than incoming dts {}", pkt.dts(), lastDTS[packetIndex]);
									pkt.dts(lastDTS[packetIndex] + 1);
								}
								lastDTS[packetIndex] = pkt.dts();
								if (pkt.dts() > pkt.pts()) {
									logger.info("dts ({}) is bigger than pts({})", pkt.dts(), pkt.pts());
									pkt.pts(pkt.dts());
								}
							}
							/***************************************************
							 *  Memory of being paranoid or failing while looking for excellence without understanding the whole picture
							 *  
							 *  Increasing pkt.dts plus 1 is a simple hack for fixing dts error if current dts has a value lower 
							 *  than the last received dts. Because dts should be monotonically increasing. I made this simple hack and it is working. 
							 *  After that I thought the same may happen for the pts value as well and I have added below fix. 
							 *  Actually not a fix, it is a bug. Because pts values does not have to be monotonically increasing
							 *  and if stream has B-Frames then pts value can be lower than the last PTS value. So below
							 *  code snippet make the stream does not play smoothly. It took about 10 hours to find it this error.
							 *  
							 *  I have written this simple memory for me
							 *  and for the guys who is developing or reviewing this code. 
							 *  Even if it is time consuming or not reasonable, these kind of tryouts sometimes makes me excited. 
							 *  I think I may expect to find something great by trying something crazy :) 
							 *  
							 *  @mekya - June 12, 2018
							 *  
							 *  ---------------------------------------------------
							 *  
							 *  if (lastPTS[packetIndex] >= pkt.pts()) {
							 * 	   pkt.pts(lastPTS[packetIndex] + 1);
							 *  }
							 *  lastPTS[packetIndex] = pkt.pts();
							 *
							 ******************************************************/
							if (bufferTime > 0) 
							{
								/*
								 * If there is a bufferTime in the server.
								 * Generally we don't use this feature most of the time
								 */
								AVPacket packet = getAVPacket();
								av_packet_ref(packet, pkt);
								bufferQueue.add(packet);

								AVPacket pktHead = bufferQueue.peek();
								/**
								 * BufferQueue may be polled in writer thread. 
								 * It's a very rare case to happen so that check if it's null
								 */
								if (pktHead != null) {
									lastPacketTimeMsInQueue = av_rescale_q(pkt.pts(), inputFormatContext.streams(pkt.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
									firstPacketTime = av_rescale_q(pktHead.pts(), inputFormatContext.streams(pktHead.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
									bufferDuration = (lastPacketTimeMsInQueue - firstPacketTime);
									
									if ( bufferDuration > bufferTime) {
										
										if (buffering) {
											//have the buffering finish time ms
											bufferingFinishTimeMs = System.currentTimeMillis();
											//have the first packet sent time
											firstPacketReadyToSentTimeMs  = firstPacketTime;
										}
										buffering = false;
									}

									logBufferStatus();
								}
							}
							else {
								if(STREAM_TYPE_VOD.equals(streamType) ||  streamUrl.contains(".mp4")  ) {
									long latestTime = System.currentTimeMillis();
									int streamIndex = pkt.stream_index();
									AVRational timeBase = inputFormatContext.streams(streamIndex).time_base();

									if(firstPacketTime == 0 ) { 
										firstPacketTime = System.currentTimeMillis();
										long firstPacketDtsInMs = av_rescale_q(pkt.dts(), timeBase, MuxAdaptor.TIME_BASE_FOR_MS);
										timeOffset = 0 - firstPacketDtsInMs;
									}
									long pktTime = av_rescale_q(pkt.dts(), timeBase, MuxAdaptor.TIME_BASE_FOR_MS);
								
									long durationInMs = latestTime - firstPacketTime;
									long dtsInMS= timeOffset + pktTime + seekDurationMS;						
							
									 while(dtsInMS > durationInMs) {
										durationInMs = System.currentTimeMillis() - firstPacketTime;
										Thread.sleep(1);
									}
									
									 if(isSeek) {
										 if(streamTime == 0) {
											 streamTime = 1;
										 }
										  seekDurationMS = durationInMs - streamTime*1000;

										 int flags = avformat.AVSEEK_FLAG_ANY;
										 int seekTarget = streamTime * timeBase.den() / timeBase.num();
									
										 if(avformat.av_seek_frame(inputFormatContext, streamIndex , seekTarget,  flags) < 0) {
											 logger.error("There is an error with av_seek_frame");
										 }
										 
										av_read_frame(inputFormatContext, pkt);
										 isSeek = false;			
										 }
										if(seekDurationMS != 0) {
											pkt.dts(av_rescale_q(dtsInMS, MuxAdaptor.TIME_BASE_FOR_MS , timeBase));
											pkt.pts(av_rescale_q(dtsInMS, MuxAdaptor.TIME_BASE_FOR_MS, timeBase));
										}
									 }
								muxAdaptor.writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
							}
							av_packet_unref(pkt);
							
							if (stopRequestReceived) {
								logger.warn("Stop request received, breaking the loop for {} ", streamId);
								break;
							}
						}
						logger.info("Leaving the stream fetcher loop for stream: {}", streamId);

					}
					else {
						logger.error("MuxAdaptor.Prepare for {} returned false", streamId);
					}

					setCameraError(result);
				} 
				else {
					logger.error("Prepare for opening the {} has failed", streamUrl);
				}
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			catch (OutOfMemoryError | Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				exceptionInThread  = true;
			}


			if (packetWriterJobName != -1) {
				logger.info("Removing packet writer job {}", packetWriterJobName);
				vertx.cancelTimer(packetWriterJobName);
			}
			writeAllBufferedPackets();


			if (muxAdaptor != null) {
				logger.info("Writing trailer in Muxadaptor {}", streamId);
				muxAdaptor.writeTrailer();
				appInstance.muxAdaptorRemoved(muxAdaptor);
				muxAdaptor = null;
			}

			if (pkt != null) {
				av_packet_free(pkt);
				pkt.close();
			}

			if (inputFormatContext != null) {
				try {
					avformat_close_input(inputFormatContext);
				}
				catch (Exception e) {
					logger.info(e.getMessage());
				}
				inputFormatContext = null;
			}

			if(streamPublished) {
				getInstance().closeBroadcast(streamId);
				streamPublished=false;
			}


			setThreadActive(false);

			if(streamFetcherListener != null) {	
				stopRequestReceived = true;
				restartStream = false;

				streamFetcherListener.streamFinished(streamFetcherListener);
			}

			if(!stopRequestReceived && restartStream) {
				logger.info("Stream fetcher will try to fetch source {} after {} ms", streamUrl, STREAM_FETCH_RE_TRY_PERIOD_MS);
				vertx.setTimer(STREAM_FETCH_RE_TRY_PERIOD_MS, l -> {

					thread = new WorkerThread();
					thread.start();
				});
			}

			logger.debug("Leaving thread for {}", streamUrl);

			stopRequestReceived = false;
		}

		private void setUpEndPoints(String publishedName, MuxAdaptor muxAdaptor) {
			Broadcast broadcast = getDataStore().get(publishedName);
			if (broadcast != null) {
				List<Endpoint> endPointList = broadcast.getEndPointList();

				if (endPointList != null && !endPointList.isEmpty()) 
				{
					for (Endpoint endpoint : endPointList) {
						muxAdaptor.addMuxer(new RtmpMuxer(endpoint.getRtmpUrl(), vertx));
					}
				}
			}

		}

		private void writeAllBufferedPackets() 
		{
			synchronized (this) {
				//different threads may write writeBufferedPacket and this method at the same time
				
				logger.info("write all buffered packets for stream: {}", streamId);
				while (!bufferQueue.isEmpty()) {
	
					AVPacket pkt = bufferQueue.poll();
					muxAdaptor.writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
					av_packet_unref(pkt);
				}
	
				AVPacket pkt;
				while ((pkt = bufferQueue.poll()) != null) {
					pkt.close();
				}
				
				while ((pkt = availableBufferQueue.poll()) != null) {
					pkt.close();
				}
			}
			
		}

		//TODO: Code dumplication with MuxAdaptor.writeBufferedPacket. It should be refactored.
		public void writeBufferedPacket() 
		{
			synchronized (this) {
			
				if (isJobRunning.compareAndSet(false, true)) 
				{
					if (!buffering) {
						while(!bufferQueue.isEmpty()) {
							AVPacket tempPacket = bufferQueue.peek(); 
							long pktTime = av_rescale_q(tempPacket.pts(), inputFormatContext.streams(tempPacket.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
							long now = System.currentTimeMillis();
							long pktTimeDifferenceMs = pktTime - firstPacketReadyToSentTimeMs; 
							long passedTime = now - bufferingFinishTimeMs;
							if (pktTimeDifferenceMs < passedTime) {
								muxAdaptor.writePacket(inputFormatContext.streams(tempPacket.stream_index()), tempPacket);
								av_packet_unref(tempPacket);
								bufferQueue.remove(); //remove the packet from the queue
								availableBufferQueue.offer(tempPacket);
							}
							else {
								//break the loop and don't block the thread because it's not correct time to send the packet
								break;
							}
							
						}
						
						//update buffering. If bufferQueue is empty, it should start buffering
						buffering = bufferQueue.isEmpty();
					}
					
					logBufferStatus();
					
					isJobRunning.compareAndSet(true, false);
				}
			
			}
		}
		
		
		public void logBufferStatus() {
			bufferLogCounter++; //we use this parameter in execute method as well 
			if (bufferLogCounter % COUNT_TO_LOG_BUFFER  == 0) {
				logger.info("WriteBufferedPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {}", buffering, getBufferedDurationMs(), bufferTime, streamId);
				bufferLogCounter = 0;
			}
		}
		
		public long getBufferedDurationMs() {
			AVPacket pktHead = bufferQueue.peek();
			if (pktHead != null) {
				long firstPacketInQueueTime = av_rescale_q(pktHead.pts(), inputFormatContext.streams(pktHead.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
				return lastPacketTimeMsInQueue - firstPacketInQueueTime;
			}
			return 0;
		}
	}
	
	

	public void startStream() {
		new Thread() {
			@Override
			public void run() {
				try {
					int i = 0;
					while (threadActive) {
						Thread.sleep(100);
						if (i % 50 == 0) {
							logger.info("waiting for thread to be finished for stream {}", streamUrl);
							i = 0;
						}
						i++;
					}
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
					Thread.currentThread().interrupt();
				}

				exceptionInThread = false;
				thread = new WorkerThread();
				thread.start();
				logger.info("StartStream called, new thread is started for {}", streamId);
			}
		}.start();

	}
	
	public DataStore getDataStore() {
		if (dataStore == null) {
			dataStore = getInstance().getDataStore();
		}
		return dataStore;
	}
	
	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	

	public AVPacket getAVPacket() {
		if (!availableBufferQueue.isEmpty()) {
			return availableBufferQueue.poll();
		}
		return new AVPacket();
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

	public void stopStream() 
	{
		logger.warn("stop stream called for {}", streamUrl);
		stopRequestReceived = true;
	}	

	public boolean isStopRequestReceived() {
		return stopRequestReceived;
	}

	public WorkerThread getThread() {
		return thread;
	}

	public void setThread(WorkerThread thread) {
		this.thread = thread;
	}

	public void restart() {
		stopStream();
		new Thread() {
			@Override
			public void run() {
				try {
					while (threadActive) {
						Thread.sleep(100);
					}

					Thread.sleep(2000);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
					Thread.currentThread().interrupt();
				}
				startStream();
			}
		}.start();

	}
	/**
	 * Set timeout when establishing connection
	 * @param timeoutMs in ms
	 */
	public void setConnectionTimeout(int timeoutMs) {
		this.timeout = timeoutMs * 1000;
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

	public AntMediaApplicationAdapter getInstance() {
		if (appInstance == null) {
			appInstance = ((IApplicationAdaptorFactory) scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppAdaptor();
		}
		return appInstance;
	}

	public MuxAdaptor getMuxAdaptor() {
		return muxAdaptor;
	}

	public void setMuxAdaptor(MuxAdaptor muxAdaptor) {
		this.muxAdaptor = muxAdaptor;
	}

	public boolean isRestartStream() {
		return restartStream;
	}

	public void setRestartStream(boolean restartStream) {
		this.restartStream = restartStream;
	}

	public int getBufferTime() {
		return bufferTime;
	}

	public void setBufferTime(int bufferTime) {
		this.bufferTime = bufferTime;
	}

	private AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) scope.getContext().getApplicationContext().getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	/**
	 * This is for test purposes
	 * @param stopRequest
	 */
	public void debugSetStopRequestReceived(boolean stopRequest) {
		stopRequestReceived = stopRequest;
	}

	public String getStreamId() {
		return streamId;
	}

	public String getStreamUrl() {
		return streamUrl;
	}
	
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
	
	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}

}
