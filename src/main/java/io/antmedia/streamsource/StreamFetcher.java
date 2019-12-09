package io.antmedia.streamsource;

import static org.bytedeco.javacpp.avcodec.av_packet_free;
import static org.bytedeco.javacpp.avcodec.av_packet_ref;
import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.javacpp.avutil.av_dict_free;
import static org.bytedeco.javacpp.avutil.av_dict_set;
import static org.bytedeco.javacpp.avutil.av_rescale_q;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVRational;
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

	protected static Logger logger = LoggerFactory.getLogger(StreamFetcher.class);
	private Broadcast stream;
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

	private ConcurrentLinkedQueue<AVPacket> availableBufferQueue = new ConcurrentLinkedQueue<>();

	private AVRational avRationalTimeBaseMS;
	private AppSettings appSettings;
	private Vertx vertx;

	public StreamFetcher(Broadcast stream, IScope scope, Vertx vertx)  {
		if (stream == null || stream.getStreamId() == null || stream.getStreamUrl() == null) {
			String streamId = null;
			if (stream != null) {
				streamId = stream.getStreamId();
			}
			String streamUrl = null;
			if (stream != null) {
				streamUrl = stream.getStreamUrl();
			}
			throw new NullPointerException("Stream is not initialized properly. Check stream("+stream+"), "
					+ " stream id ("+ streamId +") and stream url ("+ streamUrl + ") values");
		}

		this.stream = stream;
		this.scope = scope;
		this.vertx = vertx;


		if (getAppSettings() == null) {
			throw new NullPointerException("App Settings is null in StreamFetcher");
		}

		this.bufferTime = getAppSettings().getStreamFetcherBufferTime();

		avRationalTimeBaseMS = new AVRational();
		avRationalTimeBaseMS.num(1);
		avRationalTimeBaseMS.den(1000);

	}

	public Result prepareInput(AVFormatContext inputFormatContext) {

		setConnectionTimeout(5000);

		Result result = new Result(false);
		if (inputFormatContext == null) {
			logger.info("cannot allocate input context for {}", stream.getStreamId());
			return result;
		}

		AVDictionary optionsDictionary = new AVDictionary();

		String streamUrl = stream.getStreamUrl();
		if (streamUrl.startsWith("rtsp://")) {
			av_dict_set(optionsDictionary, "rtsp_transport", "tcp", 0);
		}

		String timeoutStr = String.valueOf(this.timeout);
		av_dict_set(optionsDictionary, "stimeout", timeoutStr, 0);

		int ret;

		logger.debug("stream url: {}  " , stream.getStreamUrl());

		if ((ret = avformat_open_input(inputFormatContext, stream.getStreamUrl(), null, optionsDictionary)) < 0) {

			byte[] data = new byte[100];
			avutil.av_strerror(ret, data, data.length);

			String errorStr=new String(data, 0, data.length);

			result.setMessage(errorStr);		

			logger.error("cannot open stream: {} with error:: {}",  stream.getStreamUrl(), result.getMessage());
			return result;
		}

		av_dict_free(optionsDictionary);

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

		@Override
		public void run() {

			setThreadActive(true);
			long lastPacketTime = 0;
			long firstPacketTime = 0;
			long bufferDuration = 0;

			AVPacket pkt = null;
			long packetWriterJobName = -1L;
			try {
				inputFormatContext = new AVFormatContext(null); 
				pkt = avcodec.av_packet_alloc();
				logger.info("Preparing the StreamFetcher for {}", stream.getStreamUrl());
				Result result = prepare(inputFormatContext);


				if (result.isSuccess()) {
					boolean audioOnly = false;
					if(inputFormatContext.nb_streams() == 1) {
						audioOnly  = (inputFormatContext.streams(0).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO);
						logger.debug(" codec: {}", inputFormatContext.streams(0).codecpar().codec_id());

					}
					muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null,true, scope);
					// if there is only audio, firstKeyFrameReceivedChecked should be true in advance
					// because there is no video frame

					muxAdaptor.setFirstKeyFrameReceivedChecked(audioOnly); 
					setUpEndPoints(stream.getStreamId(), muxAdaptor);

					muxAdaptor.init(scope, stream.getStreamId(), false);

					logger.info("{} stream count in stream {} is {}", stream.getStreamId(), stream.getStreamUrl(), inputFormatContext.nb_streams());

					if(muxAdaptor.prepareInternal(inputFormatContext)) {

						long currentTime = System.currentTimeMillis();
						muxAdaptor.setStartTime(currentTime);

						getInstance().startPublish(stream.getStreamId());

						if (bufferTime > 0) {
							packetWriterJobName = vertx.setPeriodic(PACKET_WRITER_PERIOD_IN_MS, l->execute());
						}

						int bufferLogCounter = 0;
						while (av_read_frame(inputFormatContext, pkt) >= 0) {

							streamPublished = true;
							lastPacketReceivedTime = System.currentTimeMillis();

							/**
							 * Check that dts values are monotically increasing for each stream
							 */
							int packetIndex = pkt.stream_index();
							if (lastDTS[packetIndex] >= pkt.dts()) {
								pkt.dts(lastDTS[packetIndex] + 1);
							}
							lastDTS[packetIndex] = pkt.dts();
							if (pkt.dts() > pkt.pts()) {
								logger.info("dts ({}) is bigger than pts({})", pkt.dts(), pkt.pts());
								pkt.pts(pkt.dts());
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
									lastPacketTime = av_rescale_q(pkt.pts(), inputFormatContext.streams(pkt.stream_index()).time_base(), avRationalTimeBaseMS);
									firstPacketTime = av_rescale_q(pktHead.pts(), inputFormatContext.streams(pktHead.stream_index()).time_base(), avRationalTimeBaseMS);
									bufferDuration = (lastPacketTime - firstPacketTime);

									if ( bufferDuration > bufferTime) {
										buffering = false;
									}

									bufferLogCounter++;
									if (bufferLogCounter % 100 == 0) {
										logger.debug("Buffer status {}, buffer duration {}ms buffer time {}ms", buffering, bufferDuration, bufferTime);
										bufferLogCounter = 0;
									}
								}
							}
							else {
								muxAdaptor.writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
							}
							av_packet_unref(pkt);
							if (stopRequestReceived) {
								logger.warn("Stop request received, breaking the loop for {} ", stream.getStreamId());
								break;
							}
						}
						logger.info("Leaving the loop for {}", stream.getStreamId());

					}
					else {
						logger.error("MuxAdaptor.Prepare for {} returned false", stream.getName());
					}

					setCameraError(result);
				} 
				else {
					logger.error("Prepare for opening the {} has failed", stream.getStreamUrl());
				}
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
				logger.info("Writing trailer in Muxadaptor {}", stream.getStreamId());
				muxAdaptor.writeTrailer();
				appInstance.muxAdaptorRemoved(muxAdaptor);
				muxAdaptor = null;
			}

			if (pkt != null) {
				av_packet_free(pkt);
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
				getInstance().closeBroadcast(stream.getStreamId());
				streamPublished=false;
			}


			setThreadActive(false);
			if(!stopRequestReceived && restartStream) {
				logger.info("Stream fetcher will try to fetch source {} after {} ms", stream.getStreamUrl(), STREAM_FETCH_RE_TRY_PERIOD_MS);
				vertx.setTimer(STREAM_FETCH_RE_TRY_PERIOD_MS, l -> {
					thread = new WorkerThread();
					thread.start();
				});
			}

			logger.debug("Leaving thread for {}", stream.getStreamUrl());


		}

		private void setUpEndPoints(String publishedName, MuxAdaptor muxAdaptor) {
			DataStore dataStore = getInstance().getDataStore();
			Broadcast broadcast = dataStore.get(publishedName);
			if (broadcast != null) {
				List<Endpoint> endPointList = broadcast.getEndPointList();

				if (endPointList != null && !endPointList.isEmpty()) 
				{
					for (Endpoint endpoint : endPointList) {
						muxAdaptor.addMuxer(new RtmpMuxer(endpoint.getRtmpUrl()));
					}
				}
			}

		}

		private void writeAllBufferedPackets() 
		{
			logger.info("write all buffered packets for stream: {}", stream.getStreamId());
			while (!bufferQueue.isEmpty()) {
				
				AVPacket pkt = bufferQueue.poll();
				muxAdaptor.writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
				av_packet_unref(pkt);
			}

			AVPacket pkt;
			while ((pkt = bufferQueue.poll()) != null) {
				pkt.close();
			}
		}

		public void execute() 
		{
			if (isJobRunning.compareAndSet(false, true)) 
			{
				if (!buffering) {
					AVPacket pkt = bufferQueue.poll();
					if (pkt != null) {
						muxAdaptor.writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
						av_packet_unref(pkt);
						availableBufferQueue.offer(pkt);
					}
					else {
						buffering = true;
					}
				}
				isJobRunning.compareAndSet(true, false);
			}
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
							logger.info("waiting for thread to be finished for stream {}", stream.getStreamUrl());
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
				logger.info("StartStream called, new thread is started for {}", stream.getStreamId());
			}
		}.start();

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
		logger.warn("stop stream called for {}", stream.getStreamId());
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

	public Broadcast getStream() {
		return stream;
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

	public void setStream(Broadcast stream) {
		this.stream = stream;
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


}
