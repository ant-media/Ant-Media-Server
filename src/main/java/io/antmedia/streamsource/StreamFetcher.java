package io.antmedia.streamsource;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.model.Result;
import io.vertx.core.Vertx;

public class StreamFetcher {

	protected static Logger logger = LoggerFactory.getLogger(StreamFetcher.class);
	private WorkerThread thread;
	/**
	 * Connection setup timeout value
	 */
	private int timeoutMicroSeconds;
	private boolean exceptionInThread = false;

	/**
	 * Last packet received time
	 */
	private long lastPacketReceivedTime = 0;
	private AtomicBoolean threadActive = new AtomicBoolean(false);
	private Result cameraError = new Result(false,"");
	private static final int PACKET_RECEIVED_INTERVAL_TIMEOUT = 3000;
	private IScope scope;
	private AntMediaApplicationAdapter appInstance;
	private long[] lastSentDTS;
	private long[] lastReceivedDTS;
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

	private static final int COUNT_TO_LOG_BUFFER = 5000;

	private int bufferLogCounter;

	private AppSettings appSettings;
	private Vertx vertx;

	private DataStore dataStore;

	private long readNextPacketStartTime;

	private long readNextPacketCompleteTime;

	public interface IStreamFetcherListener {

		void streamFinished(IStreamFetcherListener listener);

	}

	IStreamFetcherListener streamFetcherListener;

	private String streamUrl;

	private String streamId;

	private String streamType;

	private AtomicBoolean seekTimeRequestReceived = new AtomicBoolean(false);

	private AtomicLong seekTimeInMs = new AtomicLong(0);

	public IStreamFetcherListener getStreamFetcherListener() {
		return streamFetcherListener;
	}

	public void setStreamFetcherListener(IStreamFetcherListener streamFetcherListener) {
		this.streamFetcherListener = streamFetcherListener;
	}

	public StreamFetcher(String streamUrl, String streamId, String streamType, IScope scope, Vertx vertx, long seekTimeInMs)  {
		if (streamUrl == null  || streamId == null) {

			throw new NullPointerException("Stream is not initialized properly. Check "
					+ " stream id ("+ streamId +") and stream url ("+ streamUrl + ") values");
		}

		this.streamUrl = streamUrl;
		this.streamType = streamType;
		this.streamId = streamId;
		this.scope = scope;
		this.vertx = vertx;
		this.seekTimeInMs.set(seekTimeInMs);

		this.bufferTime = getAppSettings().getStreamFetcherBufferTime();
	}

	

	public void initDTSArrays(int nbStreams) 
	{
		lastSentDTS = new long[nbStreams];
		lastReceivedDTS = new long[nbStreams];

		for (int i = 0; i < lastSentDTS.length; i++) {
			lastSentDTS[i] = -1;
			lastReceivedDTS[i] = -1;
		}


	}

	public class WorkerThread extends Thread {

		private static final int PACKET_WRITER_PERIOD_IN_MS = 10;

		private static final long STREAM_FETCH_RE_TRY_PERIOD_MS = 3000;

		private volatile boolean streamPublished = false;
		protected AtomicBoolean isJobRunning = new AtomicBoolean(false);
		AVFormatContext inputFormatContext = null;



		private AtomicBoolean buffering = new AtomicBoolean(false);

		private ConcurrentSkipListSet<AVPacket> bufferQueue = null;


		private volatile long bufferingFinishTimeMs;

		private volatile long firstPacketReadyToSentTimeMs;

		private long lastPacketTimeMsInQueue;

		long firstPacketTime = 0;
		long bufferDuration = 0;
		long timeOffsetInMs = 0;
		long packetWriterJobName = -1L;

		private long firstPacketDtsInMs;

		private long lastSycnCheckTime = 0;;
		
		public Result prepare(AVFormatContext inputFormatContext) {
			Result result = prepareInput(inputFormatContext);

			setCameraError(result);

			return result;

		}
		
		public Result prepareInput(AVFormatContext inputFormatContext) {
			int timeout = appSettings.getRtspTimeoutDurationMs();
			setConnectionTimeout(timeout);

			Result result = new Result(false);
			if (inputFormatContext == null) {
				logger.info("cannot allocate input context for {}", streamId);
				return result;
			}

			AVDictionary optionsDictionary = new AVDictionary();

			String transportType = appSettings.getRtspPullTransportType();
			if (streamUrl.startsWith("rtsp://") && !transportType.isEmpty()) {


				logger.info("Setting rtsp transport type to {} for stream source: {} and timeout:{}us", transportType, streamUrl, StreamFetcher.this.timeoutMicroSeconds);
				/*
				 * AppSettings#rtspPullTransportType
				 */
				av_dict_set(optionsDictionary, "rtsp_transport", transportType, 0);

				/*
				 * AppSettings#rtspTimeoutDurationMs 
				 */
				String timeoutStr = String.valueOf(StreamFetcher.this.timeoutMicroSeconds);
				av_dict_set(optionsDictionary, "timeout", timeoutStr, 0);



			}

			//analyze duration is a generic parameter 
			int analyzeDurationUs = appSettings.getMaxAnalyzeDurationMS() * 1000;
			String analyzeDuration = String.valueOf(analyzeDurationUs);
			av_dict_set(optionsDictionary, "analyzeduration", analyzeDuration, 0);


			int ret;

			logger.debug("open stream url: {}  " , streamUrl);

			if ((ret = avformat_open_input(inputFormatContext, streamUrl, null, optionsDictionary)) < 0) {

				String errorStr = Muxer.getErrorDefinition(ret);
				result.setMessage(errorStr);		

				logger.error("cannot open stream: {} with error:: {} and streamId:{}",  streamUrl, result.getMessage(), streamId);
				av_dict_free(optionsDictionary);
				optionsDictionary.close();
				return result;
			}

			av_dict_free(optionsDictionary);
			optionsDictionary.close();

			logger.debug("find stream info: {}  " , streamUrl);

			ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
			if (ret < 0) {
				result.setMessage("Could not find stream information\n");
				logger.error(result.getMessage());
				return result;
			}

			initDTSArrays(inputFormatContext.nb_streams());
			
			if (seekTimeInMs.get() != 0) {
				seekFrame();	
			}

			result.setSuccess(true);
			return result;

		}

		@Override
		public void run() {


			AVPacket pkt = null;
			try {
				//update broadcast status to preparing 

				Broadcast broadcast = getDataStore().get(streamId);
				if (broadcast == null) {
					//if broadcast null, it means it's deleted
					logger.info("Broadcast with streamId:{} should be deleted before its thread is started", streamId);
					return;
				}
				else if (AntMediaApplicationAdapter.isStreaming(broadcast)) {
					logger.info("Broadcast with streamId:{} is streaming mode so it will not pull it here again", streamId);

					return;
				}

				getInstance().updateBroadcastStatus(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_PULL, broadcast, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);

				setThreadActive(true);

				inputFormatContext = new AVFormatContext(null); 
				pkt = avcodec.av_packet_alloc();
				if(prepareInputContext(broadcast)) 
				{

					boolean readTheNextFrame = true;
					//In some odd cases stopRequest is received immediately and status of the stream changed to finished 
					//after that readMore -> packetRead method calls "getInstance().startPublish(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_PULL);" 
					//this method runs async, it means that its status changed to broadcasting and stays there
					//I figure out this problem by analyzing a testSkipPlayList test that is failing time to time
					//Mar 31, 2024 - @mekya
					while (!stopRequestReceived && readTheNextFrame) {
						try {
							//stay in the loop if exception occurs
							readTheNextFrame = readMore(pkt);
						}
						catch (Exception e) {
							logger.error(ExceptionUtils.getStackTrace(e));
							exceptionInThread  = true;
						}
					}
					logger.info("Leaving the stream fetcher loop for stream: {}", streamId);

				}
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				exceptionInThread  = true;
			}
			finally {

				close(pkt);

				setThreadActive(false);
			}

		}	



		public boolean readMore(AVPacket pkt) {
			boolean readTheNextFrame = true;
			readNextPacketStartTime = System.currentTimeMillis();
			int readResult = readNextPacket(pkt);
			readNextPacketCompleteTime = System.currentTimeMillis();
			if(readResult >= 0) {
				packetRead(pkt);
				unReferencePacket(pkt);
			}
			else if(AntMediaApplicationAdapter.VOD.equals(streamType) && readResult != AVERROR_EOF) {
				/* 
				 * For VOD stream source, if the return of read frame is error (but not end of file), 
				 * don't break the loop immediately. Instead jump to next frame. 
				 * Otherwise same VOD will be streamed from the beginning of the file again.
				 */
				String errorDefinition = Muxer.getErrorDefinition(readResult);
				logger.warn("Frame can't be read for VOD {} error is {}", streamUrl,  errorDefinition);
				unReferencePacket(pkt);
			}
			else {
				//break the loop except above case
				logger.warn("Cannot read next packet for url:{} and error is {}", streamUrl, Muxer.getErrorDefinition(readResult));
				readTheNextFrame = false;
			}

			if (stopRequestReceived) {
				logger.warn("Stop request received, breaking the loop for {} ", streamId);
				readTheNextFrame = false;
			}
			return readTheNextFrame;
		}
		
		public int seekFrame() {
			AVRational streamTimeBase = inputFormatContext.streams(0).time_base();
			long seekTimeInStreamTimebase = av_rescale_q(seekTimeInMs.get(), MuxAdaptor.TIME_BASE_FOR_MS, streamTimeBase);
			long lastSentPacketTimeInMs = av_rescale_q(getLastSentDTS()[0], MuxAdaptor.TIME_BASE_FOR_MS, streamTimeBase);
			
			int flags = 0;
			if (lastSentPacketTimeInMs > seekTimeInStreamTimebase) {
				flags = AVSEEK_FLAG_BACKWARD;
			}

			int ret = 0;
			//try seeking if seekTime is less than duration or duration value is undefined
			if (seekTimeInStreamTimebase < inputFormatContext.streams(0).duration() || inputFormatContext.streams(0).duration() < 0) 
			{
				logger.info("Seeking in time for streamId:{} to {} ms", streamId, seekTimeInMs.get());
				if((ret = av_seek_frame(inputFormatContext, 0, seekTimeInStreamTimebase,  flags)) >= 0)
				{
					//reset firstPackeTime to initalized again
					firstPacketTime = 0;
				}
				else
				{
					logger.error("Error in seeking for streamId:{} and seekTimeInMs:{} url:{}. Error is {}", streamId, seekTimeInMs.get(), streamUrl, Muxer.getErrorDefinition(ret));
				}
			}
			else {
				logger.warn("Cannot seek because seektime:{} is bigger than the duration:{} for StreamId:{} streamUrl:{}", seekTimeInStreamTimebase,
						inputFormatContext.streams(0).duration(), streamId, streamUrl);
			}
			
			return ret;

		}

		public int readNextPacket(AVPacket pkt) {
			if (getSeekTimeRequestReceived().get()) 
			{
				
				seekFrame();
				getSeekTimeRequestReceived().set(false);
			}
			return av_read_frame(inputFormatContext, pkt);
		}

		public void unReferencePacket(AVPacket pkt) {
			av_packet_unref(pkt);
		}

		public boolean prepareInputContext(Broadcast broadcast) throws Exception {
			logger.info("Preparing the StreamFetcher for {} for streamId:{}", streamUrl, streamId);
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
				muxAdaptor.setBroadcast(broadcast);
				//if stream is rtsp, then it's not AVC
				muxAdaptor.setAvc(!streamUrl.toLowerCase().startsWith("rtsp"));

				MuxAdaptor.setUpEndPoints(muxAdaptor, broadcast, vertx);

				muxAdaptor.init(scope, streamId, false);

				logger.info("{} stream count in stream {} is {}", streamId, streamUrl, inputFormatContext.nb_streams());

				if(muxAdaptor.prepareFromInputFormatContext(inputFormatContext)) {
					return true;
				}
				else {
					logger.error("MuxAdaptor.Prepare for {} returned false", streamId);
				}
			} 
			else {
				logger.error("Prepare for opening the {} has failed for streamId:{}", streamUrl, streamId);
				setCameraError(result);
			}
			return false;
		}

		public void packetRead(AVPacket pkt) 
		{
			if(!streamPublished) {
				long currentTime = System.currentTimeMillis();
				muxAdaptor.setStartTime(currentTime);

				getInstance().startPublish(streamId, 0, IAntMediaStreamHandler.PUBLISH_TYPE_PULL);

				if (bufferTime > 0) {

					//we need to init here because inputFormatContext should be created before initializing the bufferQueue
					bufferQueue = new ConcurrentSkipListSet<>((a, b) ->
					{ 
						long packet1TimeStamp = av_rescale_q(a.dts(), inputFormatContext.streams(a.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
						long packet2TimeStamp = av_rescale_q(b.dts(), inputFormatContext.streams(b.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);

						return Long.compare(packet1TimeStamp, packet2TimeStamp);
					});

					packetWriterJobName = vertx.setPeriodic(PACKET_WRITER_PERIOD_IN_MS, l-> 
					vertx.executeBlocking(() -> {
						writeBufferedPacket();
						return null;
					}, false));



				}

			}

			streamPublished = true;
			lastPacketReceivedTime = System.currentTimeMillis();




			/***************************************************
			 * 
			 * 
			 * Check that dts values are monotically increasing for each stream
			 * 
			 * int packetIndex = pkt.stream_index();
			 *	if (lastDTS[packetIndex] >= pkt.dts()) {
			 *		logger.info("last dts: {} is bigger than incoming dts: {} for stream index:{}", pkt.dts(), lastDTS[packetIndex], packetIndex);
			 *		pkt.dts(lastDTS[packetIndex] + 1);
			 *	}
			 *
			 *
			 *	lastDTS[packetIndex] = pkt.dts();
			 *	if (pkt.dts() > pkt.pts()) {
			 * 		logger.info("dts ({}) is bigger than pts({})", pkt.dts(), pkt.pts());
			 *		pkt.pts(pkt.dts());
			 *  }
			 *  
			 *  The code snippet above is moved to the writePacket method.  Story below is about 5 years old and 
			 *  I'm still working to improve Ant Media Server. I'm still in this journey and I hope what we're doing makes us be part of the solution 
			 *  and inspire some people in the world. 
			 *  @mekya, Dec 26, 2023.
			 *    
			 *  
			 * 			
			 *  Memory of being paranoid or failing while looking for excellence without understanding the whole picture
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

				try {
					//NoSuchElementException may be thrown 
					AVPacket pktHead = bufferQueue.first();
					//NoSuchElementException may be thrown here as well - it's multithread @mekya
					AVPacket pktTrailer = bufferQueue.last();
					/**
					 * BufferQueue may be polled in writer thread. 
					 * It's a very rare case to happen so that check if it's null
					 */

					lastPacketTimeMsInQueue = av_rescale_q(pktTrailer.dts(), inputFormatContext.streams(pkt.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);

					firstPacketTime = av_rescale_q(pktHead.pts(), inputFormatContext.streams(pktHead.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);

					bufferDuration = (lastPacketTimeMsInQueue - firstPacketTime);

					if ( bufferDuration > bufferTime) {

						if (buffering.get()) {
							//have the buffering finish time ms
							bufferingFinishTimeMs = System.currentTimeMillis();
							//have the first packet sent time
							firstPacketReadyToSentTimeMs  = firstPacketTime;
						}
						buffering.set(false);
					}

					logBufferStatus();

				}
				catch (NoSuchElementException e) {
					//You may or may not ignore this exception @mekya
					logger.warn("You may or may not ignore this exception. I mean It can happen time to time in multithread environment -> {}", e.getMessage());
				}
			}
			else {

				if(AntMediaApplicationAdapter.VOD.equals(streamType)) {


					int streamIndex = pkt.stream_index();
					AVRational timeBase = inputFormatContext.streams(streamIndex).time_base();

					if(firstPacketTime == 0) 
					{
						firstPacketTime = System.currentTimeMillis();
						firstPacketDtsInMs = av_rescale_q(pkt.dts(), timeBase, MuxAdaptor.TIME_BASE_FOR_MS);
						if (firstPacketDtsInMs < 0) {
							firstPacketDtsInMs = 0;
						}
						timeOffsetInMs = 0;
					}

					long latestTime = System.currentTimeMillis();

					long pktTimeMs = av_rescale_q(pkt.dts(), timeBase, MuxAdaptor.TIME_BASE_FOR_MS);

					long durationInMs = latestTime - firstPacketTime;

					long dtsInMS = pktTimeMs - firstPacketDtsInMs;
					
					while(dtsInMS > durationInMs) {
						durationInMs = System.currentTimeMillis() - firstPacketTime;
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							logger.error(ExceptionUtils.getStackTrace(e));
							Thread.currentThread().interrupt();
						}
						long elapsedTime = System.currentTimeMillis() - latestTime;
						if (elapsedTime > 1000) {
							logger.warn("Elapsed time is: {} to send the packet for streamId:{}", elapsedTime, streamId);
						}
					}

				}

				writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);

			}

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
				if (packetWriterJobName != -1) {
					logger.info("Removing packet writer job {}", packetWriterJobName);
					vertx.cancelTimer(packetWriterJobName);
				}
				writeAllBufferedPackets();


				if (muxAdaptor != null) {
					logger.info("Writing trailer in Muxadaptor {}", streamId);
					muxAdaptor.writeTrailer();
					getInstance().muxAdaptorRemoved(muxAdaptor);
					muxAdaptor = null;
				}

				if (pkt != null) {
					av_packet_free(pkt);
					pkt.close();
				}

				closeInputFormatContext();

				boolean closeCalled = false;
				if(streamPublished) {
					//If stream is not getting started, this is not called
					getInstance().closeBroadcast(streamId);
					streamPublished=false;
					closeCalled = true;
				}
				




				if(streamFetcherListener != null) 
				{	
					stopRequestReceived = true;
					restartStream = false;
					logger.info("Calling streamFinished listener for streamId:{} and it will not restart the stream automatically because callback is getting the responsbility", streamId);
					streamFetcherListener.streamFinished(streamFetcherListener);
				}

				if(!stopRequestReceived && restartStream) {
					logger.info("Stream fetcher will try to fetch source {} after {} ms for streamId:{}", streamUrl, STREAM_FETCH_RE_TRY_PERIOD_MS, streamId);

					//Update status to finished in all cases
					getDataStore().updateStatus(streamId, IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
					
					vertx.setTimer(STREAM_FETCH_RE_TRY_PERIOD_MS, l -> {

						thread = new WorkerThread();
						thread.start();
					});
				}
				else 
				{
					logger.info("Stream fetcher will not try again for streamUrl:{} because stopRequestReceived:{} and restartStream:{}", 
							streamUrl, stopRequestReceived, restartStream);

					if (!closeCalled) {
						getInstance().closeBroadcast(streamId);
					}
				}

				logger.debug("Leaving thread for {}", streamUrl);

				stopRequestReceived = false;
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));

			}
		}

		private void writeAllBufferedPackets() 
		{
			synchronized (this) {
				//different threads may write writeBufferedPacket and this method at the same time

				if (bufferQueue != null) 
				{
					logger.info("write all buffered packets for stream: {}", streamId);
					while (!bufferQueue.isEmpty()) {

						AVPacket pkt = bufferQueue.pollFirst();
						writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
						unReferencePacket(pkt);
					}

					AVPacket pkt;
					while ((pkt = bufferQueue.pollFirst()) != null) {
						pkt.close();
					}
				}
			}

		}

		public void writePacket(AVStream stream, AVPacket pkt) {
			int packetIndex = pkt.stream_index();

			long pktDts = pkt.dts();

			//if last sent DTS is bigger than incoming dts, it may be corrupt packet (due to network, etc) or stream is restarted 

			if (lastSentDTS[packetIndex] >= pkt.dts()) 
			{
				//it may be corrupt packet
				if (pkt.dts() > lastReceivedDTS[packetIndex]) {
					//it may be seeked or restarted
					pktDts = lastSentDTS[packetIndex] + pkt.dts() - lastReceivedDTS[packetIndex];
					//if stream is restarted, audio/video sync may be accumulated and we need to check the audio/video synch
					checkAndFixSynch();
				}
				else {
					logger.info("Last dts:{} is bigger than incoming dts: {} for stream index:{} and streamId:{}-"
							+ " If you see this log frequently and it's not related to playlist, you may TRY TO FIX it by setting \"streamFetcherBufferTime\"(to ie. 1000) in Application Settings", 
							lastSentDTS[packetIndex], pkt.dts(), packetIndex, streamId);
					pktDts = lastSentDTS[packetIndex] + 1;
				}
			}
			
			lastReceivedDTS[packetIndex] = pkt.dts();
			pkt.dts(pktDts);			
			lastSentDTS[packetIndex] = pkt.dts();


			if (pkt.dts() > pkt.pts()) 
			{
				pkt.pts(pkt.dts());
			}

			muxAdaptor.writePacket(stream, pkt);
		}
		
		public int getCodecType(int streamIndex) {
			return inputFormatContext.streams(streamIndex).codecpar().codec_type();
		}
		
		public AVRational getStreamTimebase(int streamIndex) {
			return  inputFormatContext.streams(streamIndex).time_base();
		}
		
		public void checkAndFixSynch() 
		{
			long now = System.currentTimeMillis();
			if (lastSycnCheckTime == 0) {
				lastSycnCheckTime = now;
			}
			long timeDifferenceInMs =  now - lastSycnCheckTime;
			//check synch for every 2 seconds
			if (lastSentDTS.length >= 2 && timeDifferenceInMs > 2000 ) 
			{
				lastSycnCheckTime = now;
				
				//put audio and video lastSentDTS into an array
				List<Long> lastSendDTSInMsList = new ArrayList<>();
				for(int i = 0; i < lastSentDTS.length; i++) 
				{
					if (getCodecType(i) == AVMEDIA_TYPE_VIDEO || getCodecType(i)  == AVMEDIA_TYPE_AUDIO) { 
						long dtsInMs = av_rescale_q(lastSentDTS[i], getStreamTimebase(i), MuxAdaptor.TIME_BASE_FOR_MS);
						lastSendDTSInMsList.add(dtsInMs);
					}
				}
				
				long minValueInMilliseconds = -1;
				long maxValueInMilliseconds = -1;
				
				//get the minimum and max values 
				for (Long value : lastSendDTSInMsList) {
					if (minValueInMilliseconds > value || minValueInMilliseconds == -1) {
						minValueInMilliseconds = value;
					}
					if (maxValueInMilliseconds < value || maxValueInMilliseconds == -1) {
						maxValueInMilliseconds = value;
					}
				}
				
				long asyncThreshold = 150;
				//if lastSentDTS is more than 150 ms, it means that there is a accumulated problem. 
				//The assumption is that we receive sync video/audio
				if (Math.abs(maxValueInMilliseconds-minValueInMilliseconds) > asyncThreshold) 
				{
					logger.warn("Audio/Video sync is more than {}ms for stream:{} and trying to synch the packets", asyncThreshold, streamId);
					for(int i = 0; i < lastSentDTS.length; i++) 
					{
						if (getCodecType(i) == AVMEDIA_TYPE_VIDEO || getCodecType(i) == AVMEDIA_TYPE_AUDIO) { 
							long dtsInMs = av_rescale_q(maxValueInMilliseconds, MuxAdaptor.TIME_BASE_FOR_MS, getStreamTimebase(i));
							lastSentDTS[i] = dtsInMs;
						}
					}
				}
			}
		}


		//TODO: Code duplication with MuxAdaptor.writeBufferedPacket. It should be refactored.
		public void writeBufferedPacket() 
		{
			synchronized (this) {

				if (isJobRunning.compareAndSet(false, true)) 
				{
					try {
						if (!buffering.get()) 
						{
							while(!bufferQueue.isEmpty()) 
							{
								AVPacket tempPacket = bufferQueue.first(); 

								long pktTime = av_rescale_q(tempPacket.pts(), inputFormatContext.streams(tempPacket.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);

								long now = System.currentTimeMillis();

								long pktTimeDifferenceMs = pktTime - firstPacketReadyToSentTimeMs; 

								long passedTime = now - bufferingFinishTimeMs;

								if (pktTimeDifferenceMs < passedTime) 
								{

									writePacket(inputFormatContext.streams(tempPacket.stream_index()), tempPacket);
									unReferencePacket(tempPacket);
									bufferQueue.remove(tempPacket); //remove the packet from the queue
								}
								else {
									//break the loop and don't block the thread because it's not correct time to send the packet
									break;
								}

							}

							//update buffering. If bufferQueue is empty, it should start buffering
							buffering.set(bufferQueue.isEmpty());
						}

						logBufferStatus();
					}
					finally {

						isJobRunning.compareAndSet(true, false);
					}
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

			if (bufferQueue != null && !bufferQueue.isEmpty()) {
				AVPacket pktHead = bufferQueue.first();
				long firstPacketInQueueTime = av_rescale_q(pktHead.pts(), inputFormatContext.streams(pktHead.stream_index()).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
				return lastPacketTimeMsInQueue - firstPacketInQueueTime;
			}
			return 0;
		}


		public ConcurrentSkipListSet<AVPacket> getBufferQueue() {
			return bufferQueue;
		}


		public void setInputFormatContext(AVFormatContext inputFormatContext) {
			this.inputFormatContext = inputFormatContext;
		}
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
							logger.info("waiting for thread to be finished for stream {}", streamUrl);
							i = 0;
						}
						i++;
					}
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

	public boolean isStreamBlocked() {
		return Math.abs(readNextPacketCompleteTime - readNextPacketStartTime) > PACKET_RECEIVED_INTERVAL_TIMEOUT;
	}

	//TODO: why we're using isInterruped here? It may not give correct value about the status of the stream
	//@mekya
	public boolean isStopped() {
		return thread.isInterrupted();
	}

	public void stopStream() 
	{
		logger.info("stop stream called for {} and streamId:{}", streamUrl, streamId);
		stopRequestReceived = true;
	}

	public void seekTime(long seekTimeInMilliseconds) {
		this.seekTimeInMs.set(seekTimeInMilliseconds); 
		seekTimeRequestReceived.set(true);
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
					while (threadActive.get()) {
						Thread.sleep(100);
					}

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
		this.timeoutMicroSeconds = timeoutMs * 1000;
	}

	public boolean isExceptionInThread() {
		return exceptionInThread;
	}

	public void setThreadActive(boolean threadActive) {
		this.threadActive.set(threadActive);
	}

	public boolean isThreadActive() {
		return threadActive.get();
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
			appInstance = (AntMediaApplicationAdapter) scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
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

	public AtomicBoolean getSeekTimeRequestReceived() {
		return seekTimeRequestReceived;
	}

	public void setSeekTimeRequestReceived(AtomicBoolean seekTimeRequestReceived) {
		this.seekTimeRequestReceived = seekTimeRequestReceived;
	}

	public long[] getLastSentDTS() {
		return lastSentDTS;
	}

}
