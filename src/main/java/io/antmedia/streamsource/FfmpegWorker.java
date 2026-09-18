package io.antmedia.streamsource;

import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_ref;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.AVSEEK_FLAG_BACKWARD;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_seek_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVERROR_EOF;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVIOInterruptCB;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.Pointer;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher.Reason;
import io.vertx.core.Vertx;

/**
 * The real {@link StreamFetcherWorker}, and the only class in the package that knows about ffmpeg or
 * {@link MuxAdaptor}. It opens the source, pulls until the source ends or {@link #abortRequested} is
 * set, closes everything it opened and returns why.
 */
public class FfmpegWorker extends StreamFetcherWorker {

	private static final Logger logger = LoggerFactory.getLogger(FfmpegWorker.class);

	/** ffmpeg read and write timeout for the protocols that honour it, rtsp has its own setting. */
	private static final int UDP_TCP_HTTP_TIMEOUT_MS = 15000;

	private static final int PACKET_WRITER_PERIOD_IN_MS = 10;

	private static final int COUNT_TO_LOG_BUFFER = 5000;

	private static final String RTSP_ALLOWED_MEDIA_TYPES = "allowed_media_types";

	/**
	 * Read by ffmpeg from inside a blocking open or read, which is the only way to abort one. The
	 * worker keeps a strong reference to it, javacpp collects function pointers otherwise.
	 */
	private static class AbortCallback extends AVIOInterruptCB.Callback_Pointer {

		private final AtomicBoolean abortRequested;

		AbortCallback(AtomicBoolean abortRequested) {
			this.abortRequested = abortRequested;
		}

		@Override
		public int call(Pointer opaque) {
			return abortRequested.get() ? 1 : 0;
		}
	}

	private final String streamId;
	private final String streamType;
	private final IScope scope;
	private final Vertx vertx;
	private final AppSettings appSettings;

	private final AbortCallback abortCallback = new AbortCallback(abortRequested);
	private final AVIOInterruptCB interruptCallback = new AVIOInterruptCB().callback(abortCallback);

	private String streamUrl;
	private AntMediaApplicationAdapter appInstance;

	private AVFormatContext inputFormatContext;
	private volatile MuxAdaptor muxAdaptor;

	private long[] lastSentDTS;
	private long[] lastReceivedDTS;
	private boolean streamPublished;
	private long lastSycnCheckTime;

	private final AtomicBoolean seekRequested = new AtomicBoolean(false);
	private volatile long seekTimeMs;

	/** First packet wall clock time and dts, the pair that paces a VoD source in real time. */
	private long firstPacketTime;
	private long firstPacketDtsInMs;

	/** How long to buffer packages before pushing to muxer downstream (in ms) **/
	private final int bufferTime;
	private volatile ConcurrentSkipListSet<AVPacket> bufferQueue;
	/** Set while the queue fills, cleared once it holds bufferTime of media and it can pace again. */
	private final AtomicBoolean buffering = new AtomicBoolean(true);

	private final AtomicBoolean writerJobRunning = new AtomicBoolean(false);
	private long packetWriterJobName = -1L;
	private volatile boolean closed;
	private long bufferingFinishTimeMs;
	private long firstPacketReadyToSentTimeMs;
	private long firstPacketTimeMsInQueue;
	private long lastPacketTimeMsInQueue;
	private int bufferLogCounter;

	public FfmpegWorker(String streamUrl, String streamId, String streamType, long seekTimeMs, IScope scope, Vertx vertx, AppSettings appSettings) {
		this.streamUrl = streamUrl;
		this.streamId = streamId;
		this.streamType = streamType;
		this.seekTimeMs = seekTimeMs;
		this.scope = scope;
		this.vertx = vertx;
		this.appSettings = appSettings;
		this.bufferTime = appSettings.getStreamFetcherBufferTime();
	}

	@Override
	public Reason run(Broadcast broadcast) {
		AVPacket pkt = null;
		Reason reason = Reason.OPEN_FAILED;

		try {
			inputFormatContext = avformat_alloc_context();
			if (inputFormatContext == null) {
				logger.warn("Cannot allocate the input context for streamId:{}", streamId);
				error = new Result(false, "Cannot allocate the input context");
				return reason;
			}
			inputFormatContext.interrupt_callback(interruptCallback);

			pkt = av_packet_alloc();

			if (prepareInputContext(broadcast)) {
				reason = readUntilEnd(pkt);
			}
			else if (abortRequested.get()) {
				reason = Reason.TIMEOUT;
			}
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			reason = Reason.READ_ERROR;
		}
		finally {
			close(pkt);
		}

		return reason;
	}

	@Override
	public MuxAdaptor getMuxAdaptor() {
		return muxAdaptor;
	}

	@Override
	public void seek(long seekTimeMs) {
		this.seekTimeMs = seekTimeMs;
		seekRequested.set(true);
	}

	private Reason readUntilEnd(AVPacket pkt) {
		while (!abortRequested.get()) {

			int result = readNextPacket(pkt);

			if (result >= 0) {
				packetRead(pkt);
				av_packet_unref(pkt);
			}
			else if (AntMediaApplicationAdapter.VOD.equals(streamType) && result != AVERROR_EOF) {
				//for a VoD, one unreadable frame must not end the attempt, it would replay the file from the start
				logger.warn("Frame can't be read for VOD {} error is {}", streamUrl, Muxer.getErrorDefinition(result));
				av_packet_unref(pkt);
			}
			else {
				logger.warn("Cannot read the next packet for url:{} and error is {}", streamUrl, Muxer.getErrorDefinition(result));
				if (abortRequested.get()) {
					return Reason.TIMEOUT;
				}
				return result == AVERROR_EOF ? Reason.EOF : Reason.READ_ERROR;
			}
		}

		return Reason.TIMEOUT;
	}

	private int readNextPacket(AVPacket pkt) {
		if (seekRequested.compareAndSet(true, false)) {
			seekFrame();
		}
		return av_read_frame(inputFormatContext, pkt);
	}

	private boolean prepareInputContext(Broadcast broadcast) throws Exception {
		logger.info("Preparing the stream fetcher for {} and streamId:{}", streamUrl, streamId);

		//HTTP sources are files or segment lists, so ffmpeg hands us a whole segment at once instead of
		//pacing it. Only VoD is paced against the wall clock here, everything else needs the buffer
		if (bufferTime <= 0 && !AntMediaApplicationAdapter.VOD.equals(streamType)
				&& (streamUrl.startsWith("http://") || streamUrl.startsWith("https://"))) {
			logger.warn("Source {} is pulled over HTTP and streamFetcherBufferTime is not set for streamId:{}."
					+ " Packets will arrive in bursts and playback will not be smooth."
					+ " Set streamFetcherBufferTime to 1000 or more in the application settings", streamUrl, streamId);
		}

		Result result = prepareInput();
		error = result;

		if (!result.isSuccess()) {
			return false;
		}

		if (abortRequested.get()) {
			//the open finished after a stop or an abandon. Registering a MuxAdaptor now would replace the
			//one a newer worker already owns, and its own cleanup would then tear that live one down
			logger.info("Source opened after the abort was requested, dropping this attempt for streamId:{}", streamId);
			return false;
		}

		boolean audioExist = false;
		boolean videoExist = false;
		for (int i = 0; i < inputFormatContext.nb_streams(); i++) {
			if (inputFormatContext.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
				audioExist = true;
				if (avcodec.avcodec_find_decoder(inputFormatContext.streams(i).codecpar().codec_id()) == null) {
					logger.error("avcodec_find_decoder() error: Unsupported audio format or codec not found");
					audioExist = false;
				}
			}
			else if (inputFormatContext.streams(i).codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) {
				videoExist = true;
				if (avcodec.avcodec_find_decoder(inputFormatContext.streams(i).codecpar().codec_id()) == null) {
					logger.error("avcodec_find_decoder() error: Unsupported video format or codec not found");
					videoExist = false;
				}
			}
		}

		MuxAdaptor adaptor = MuxAdaptor.initializeMuxAdaptor(null, broadcast, true, scope);
		// if there is only audio, firstKeyFrameReceivedChecked should be true in advance because there is no video frame
		adaptor.setFirstKeyFrameReceivedChecked(!videoExist);
		adaptor.setEnableVideo(videoExist);
		adaptor.setEnableAudio(audioExist);
		adaptor.setBroadcast(broadcast);
		//if stream is rtsp, then it's not AVC
		adaptor.setAvc(!streamUrl.toLowerCase().startsWith("rtsp"));
		muxAdaptor = adaptor;

		MuxAdaptor.setUpEndPoints(adaptor, broadcast, vertx);
		adaptor.init(scope, streamId, false);

		logger.info("{} stream count in stream {} is {}", streamId, streamUrl, inputFormatContext.nb_streams());

		if (adaptor.prepareFromInputFormatContext(inputFormatContext)) {
			return true;
		}

		logger.warn("MuxAdaptor.Prepare for {} returned false", streamId);
		return false;
	}

	private Result prepareInput() {
		Result result = new Result(false);
		AVDictionary optionsDictionary = new AVDictionary();

		String transportType = appSettings.getRtspPullTransportType();
		if (streamUrl.startsWith("rtsp://") && !transportType.isEmpty()) {

			int timeoutMicroSeconds = appSettings.getRtspTimeoutDurationMs() * 1000;
			logger.info("Setting rtsp transport type to {} for stream source: {} and timeout:{}us", transportType, streamUrl, timeoutMicroSeconds);

			av_dict_set(optionsDictionary, "rtsp_transport", transportType, 0);
			av_dict_set(optionsDictionary, "timeout", String.valueOf(timeoutMicroSeconds), 0);

			// RTSP url parameter format rtsp://ip:port/id?key=value&key=value
			parseRtspUrlParams(optionsDictionary);
		}
		else if (streamUrl.startsWith("udp://") || streamUrl.startsWith("tcp://") || streamUrl.startsWith("http://")) {
			// for UDP/HTTP/TCP "rw_timeout" makes avformat_open_input fail if no packet arrives,
			// "timeout" is the fallback for the protocols that read it instead, both in microseconds
			String timeoutStr = String.valueOf(UDP_TCP_HTTP_TIMEOUT_MS * 1000);
			av_dict_set(optionsDictionary, "rw_timeout", timeoutStr, 0);
			av_dict_set(optionsDictionary, "timeout", timeoutStr, 0);
		}

		//analyze duration is a generic parameter
		av_dict_set(optionsDictionary, "analyzeduration", String.valueOf(appSettings.getMaxAnalyzeDurationMS() * 1000), 0);

		logger.debug("open stream url: {}  ", streamUrl);

		int ret = avformat_open_input(inputFormatContext, streamUrl, null, optionsDictionary);
		av_dict_free(optionsDictionary);
		optionsDictionary.close();

		if (ret < 0) {
			result.setMessage(Muxer.getErrorDefinition(ret));
			logger.warn("cannot open stream: {} with error:: {} and streamId:{}", streamUrl, result.getMessage(), streamId);
			return result;
		}

		logger.debug("find stream info: {}  ", streamUrl);

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			result.setMessage("Could not find stream information");
			logger.warn("{} for streamId:{}", result.getMessage(), streamId);
			return result;
		}

		initDTSArrays(inputFormatContext.nb_streams());

		if (seekTimeMs != 0) {
			seekFrame();
		}

		result.setSuccess(true);
		return result;
	}

	/**
	 * Pulls the {@value #RTSP_ALLOWED_MEDIA_TYPES} parameter out of the url into an ffmpeg option and
	 * leaves the rest of the query untouched.
	 */
	public void parseRtspUrlParams(AVDictionary optionsDictionary) {
		try {
			URI.create(streamUrl);
		}
		catch (IllegalArgumentException | NullPointerException e) {
			logger.warn("cannot parse URL parameters incorrect URL format");
			return;
		}

		int questionMarkIndex = streamUrl.indexOf('?');
		if (questionMarkIndex == -1) {
			return;
		}

		StringBuilder newQuery = new StringBuilder();
		boolean first = true;

		for (String param : streamUrl.substring(questionMarkIndex + 1).split("&")) {
			String[] keyValue = param.split("=", 2);
			String key = keyValue[0];
			String value = keyValue.length > 1 ? keyValue[1] : "";

			if (RTSP_ALLOWED_MEDIA_TYPES.equals(key)) {
				try {
					if (!value.isEmpty()) {
						av_dict_set(optionsDictionary, RTSP_ALLOWED_MEDIA_TYPES, URLDecoder.decode(value, StandardCharsets.UTF_8.name()), 0);
					}
				}
				catch (Exception e) {
					logger.warn("Cannot decode value for key: {} value: {}", key, value);
				}
				continue;
			}

			if (!first) {
				newQuery.append("&");
			}
			newQuery.append(param);
			first = false;
		}

		String baseUrl = streamUrl.substring(0, questionMarkIndex);
		streamUrl = newQuery.length() > 0 ? baseUrl + "?" + newQuery : baseUrl;
	}

	private void initDTSArrays(int nbStreams) {
		lastSentDTS = new long[nbStreams];
		lastReceivedDTS = new long[nbStreams];

		for (int i = 0; i < lastSentDTS.length; i++) {
			lastSentDTS[i] = -1;
			lastReceivedDTS[i] = -1;
		}
	}

	private int seekFrame() {
		AVRational streamTimeBase = inputFormatContext.streams(0).time_base();
		long seekTimeInStreamTimebase = av_rescale_q(seekTimeMs, MuxAdaptor.TIME_BASE_FOR_MS, streamTimeBase);
		long lastSentPacketTimeInMs = av_rescale_q(lastSentDTS[0], MuxAdaptor.TIME_BASE_FOR_MS, streamTimeBase);

		int flags = lastSentPacketTimeInMs > seekTimeInStreamTimebase ? AVSEEK_FLAG_BACKWARD : 0;

		//try seeking if seekTime is less than duration or duration value is undefined
		if (seekTimeInStreamTimebase >= inputFormatContext.streams(0).duration() && inputFormatContext.streams(0).duration() >= 0) {
			logger.warn("Cannot seek because seektime:{} is bigger than the duration:{} for streamId:{} streamUrl:{}", seekTimeInStreamTimebase,
					inputFormatContext.streams(0).duration(), streamId, streamUrl);
			return 0;
		}

		logger.info("Seeking in time for streamId:{} to {} ms", streamId, seekTimeMs);

		int ret = av_seek_frame(inputFormatContext, 0, seekTimeInStreamTimebase, flags);
		if (ret >= 0) {
			//reset firstPacketTime so the VoD pacing starts over from the new position
			firstPacketTime = 0;
		}
		else {
			logger.error("Error in seeking for streamId:{} and seekTimeInMs:{} url:{}. Error is {}", streamId, seekTimeMs, streamUrl, Muxer.getErrorDefinition(ret));
		}

		return ret;
	}

	private void packetRead(AVPacket pkt) {
		if (!streamPublished) {
			streamPublished = true;
			muxAdaptor.setStartTime(System.currentTimeMillis());

			if (bufferTime > 0) {
				//the input context has to exist before the queue, the comparator reads the stream time bases.
				//a Set drops whatever compares equal, and audio and video regularly land on the same
				//millisecond, so the address tells them apart. Any stable order does, they are a millisecond apart
				bufferQueue = new ConcurrentSkipListSet<>((a, b) -> {
					int byTime = Long.compare(toMs(a, a.dts()), toMs(b, b.dts()));
					return byTime != 0 ? byTime : Long.compare(a.address(), b.address());
				});

				//a tick that finds the writer still running must not take a worker thread only to block on
				//the monitor, at 100 ticks a second they would pile up and starve the pool for everyone
				packetWriterJobName = vertx.setPeriodic(PACKET_WRITER_PERIOD_IN_MS, l -> {
					if (writerJobRunning.compareAndSet(false, true)) {
						vertx.executeBlocking(() -> { writeBufferedPacket(); return null; }, false)
								.onComplete(ar -> writerJobRunning.set(false));
					}
				});
			}

			onFirstPacket.run();
		}

		lastActivityMs = System.currentTimeMillis();

		if (bufferTime > 0) {
			AVPacket packet = new AVPacket();
			av_packet_ref(packet, pkt);
			bufferQueue.add(packet);
		}
		else {
			if (AntMediaApplicationAdapter.VOD.equals(streamType)) {
				paceVodPacket(pkt);
			}
			writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
		}
	}

	/** Holds a VoD packet back until its own timeline catches up with the wall clock. */
	private void paceVodPacket(AVPacket pkt) {
		AVRational timeBase = inputFormatContext.streams(pkt.stream_index()).time_base();

		if (firstPacketTime == 0) {
			firstPacketTime = System.currentTimeMillis();
			firstPacketDtsInMs = Math.max(av_rescale_q(pkt.dts(), timeBase, MuxAdaptor.TIME_BASE_FOR_MS), 0);
		}

		long latestTime = System.currentTimeMillis();
		long dtsInMS = av_rescale_q(pkt.dts(), timeBase, MuxAdaptor.TIME_BASE_FOR_MS) - firstPacketDtsInMs;

		while (dtsInMS > System.currentTimeMillis() - firstPacketTime) {
			try {
				Thread.sleep(1);
			}
			catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
				return;
			}

			long elapsedTime = System.currentTimeMillis() - latestTime;
			if (elapsedTime > 1000) {
				logger.warn("Elapsed time is: {} to send the packet for streamId:{}", elapsedTime, streamId);
			}
		}
	}

	/**
	 * Keeps the dts of each stream monotonically increasing. A dts that went backwards is either a
	 * corrupt packet or a restarted/seeked source, and the two are told apart by the received dts.
	 */
	private void writePacket(AVStream stream, AVPacket pkt) {
		int packetIndex = pkt.stream_index();
		long pktDts = pkt.dts();

		if (lastSentDTS[packetIndex] >= pkt.dts()) {
			if (pkt.dts() > lastReceivedDTS[packetIndex]) {
				//the source restarted or seeked, carry the offset over and re-check the audio/video synch
				pktDts = lastSentDTS[packetIndex] + pkt.dts() - lastReceivedDTS[packetIndex];
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

		if (pkt.dts() > pkt.pts()) {
			pkt.pts(pkt.dts());
		}

		muxAdaptor.writePacket(stream, pkt);
	}

	/** Pulls audio and video back together when their sent timestamps have drifted apart. */
	private void checkAndFixSynch() {
		long now = System.currentTimeMillis();
		if (lastSycnCheckTime == 0) {
			lastSycnCheckTime = now;
		}

		if (lastSentDTS.length < 2 || now - lastSycnCheckTime <= 2000) {
			return;
		}
		lastSycnCheckTime = now;

		List<Long> lastSentDTSInMsList = new ArrayList<>();
		for (int i = 0; i < lastSentDTS.length; i++) {
			if (isAudioOrVideo(i)) {
				lastSentDTSInMsList.add(av_rescale_q(lastSentDTS[i], getStreamTimebase(i), MuxAdaptor.TIME_BASE_FOR_MS));
			}
		}

		long minValueInMilliseconds = -1;
		long maxValueInMilliseconds = -1;
		for (Long value : lastSentDTSInMsList) {
			if (minValueInMilliseconds > value || minValueInMilliseconds == -1) {
				minValueInMilliseconds = value;
			}
			if (maxValueInMilliseconds < value || maxValueInMilliseconds == -1) {
				maxValueInMilliseconds = value;
			}
		}

		//the assumption is that we receive synched audio and video, so a gap this big is accumulated drift
		long asyncThreshold = 150;
		if (Math.abs(maxValueInMilliseconds - minValueInMilliseconds) > asyncThreshold) {
			logger.warn("Audio/Video sync is more than {}ms for stream:{} and trying to synch the packets", asyncThreshold, streamId);
			for (int i = 0; i < lastSentDTS.length; i++) {
				if (isAudioOrVideo(i)) {
					lastSentDTS[i] = av_rescale_q(maxValueInMilliseconds, MuxAdaptor.TIME_BASE_FOR_MS, getStreamTimebase(i));
				}
			}
		}
	}

	private boolean isAudioOrVideo(int streamIndex) {
		int codecType = inputFormatContext.streams(streamIndex).codecpar().codec_type();
		return codecType == AVMEDIA_TYPE_VIDEO || codecType == AVMEDIA_TYPE_AUDIO;
	}

	private AVRational getStreamTimebase(int streamIndex) {
		return inputFormatContext.streams(streamIndex).time_base();
	}

	/** Rescales one of a packet's timestamps into milliseconds, in its own stream's time base. */
	private long toMs(AVPacket pkt, long timestamp) {
		return av_rescale_q(timestamp, getStreamTimebase(pkt.stream_index()), MuxAdaptor.TIME_BASE_FOR_MS);
	}

	//TODO: Code duplication with MuxAdaptor.writeBufferedPacket. It should be refactored.
	private void writeBufferedPacket() {
		synchronized (this) {
			if (closed) {
				//a tick already handed to a worker outlives the cancelled timer, and close owns everything now
				return;
			}

			if (bufferQueue.isEmpty()) {
				//an underrun, wait for it to fill again. Everything below needs a head and a tail
				buffering.set(true);
				return;
			}

			try {
				calculateBufferStatus();

				if (!buffering.get()) {
					while (!bufferQueue.isEmpty()) {
						AVPacket tempPacket = bufferQueue.first();

						long pktTime = toMs(tempPacket, tempPacket.pts());

						if (pktTime - firstPacketReadyToSentTimeMs >= System.currentTimeMillis() - bufferingFinishTimeMs) {
							//it is not the time to send this packet yet, don't block the thread waiting for it
							break;
						}

						//out of the queue first, writePacket rewrites the very dts the queue is sorted by and
						//the packet would no longer be found where the set put it
						bufferQueue.remove(tempPacket);
						writePacket(inputFormatContext.streams(tempPacket.stream_index()), tempPacket);
						av_packet_unref(tempPacket);
					}

					//if the queue is drained, start buffering again
					buffering.set(bufferQueue.isEmpty());
				}

				logBufferStatus();
			}
			catch (Exception e) {
				//nobody watches the future this runs in, an escaping exception would be lost
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
	}

	private void calculateBufferStatus() {
		//the caller holds the monitor and has checked the queue, so both of these have a packet
		AVPacket pktHead = bufferQueue.first();
		AVPacket pktTrailer = bufferQueue.last();

		lastPacketTimeMsInQueue = toMs(pktTrailer, pktTrailer.dts());
		firstPacketTimeMsInQueue = toMs(pktHead, pktHead.pts());

		if (lastPacketTimeMsInQueue - firstPacketTimeMsInQueue > bufferTime) {
			if (buffering.get()) {
				bufferingFinishTimeMs = System.currentTimeMillis();
				firstPacketReadyToSentTimeMs = firstPacketTimeMsInQueue;
			}
			buffering.set(false);
		}
	}

	private void logBufferStatus() {
		bufferLogCounter++;
		if (bufferLogCounter % COUNT_TO_LOG_BUFFER == 0) {
			logger.info("WriteBufferedPacket -> Buffering status {}, buffer duration {}ms buffer time {}ms stream: {}",
					buffering, lastPacketTimeMsInQueue - firstPacketTimeMsInQueue, bufferTime, streamId);
			bufferLogCounter = 0;
		}
	}

	private void writeAllBufferedPackets() {
		synchronized (this) {
			if (bufferQueue == null) {
				return;
			}

			logger.info("write all buffered packets for stream: {}", streamId);
			AVPacket pkt;
			while ((pkt = bufferQueue.pollFirst()) != null) {
				writePacket(inputFormatContext.streams(pkt.stream_index()), pkt);
				av_packet_unref(pkt);
			}
		}
	}

	private synchronized void closeInputFormatContext() {
		if (inputFormatContext == null) {
			return;
		}

		try {
			avformat_close_input(inputFormatContext);
		}
		catch (Exception e) {
			logger.info(e.getMessage());
		}
		inputFormatContext = null;
	}

	private void close(AVPacket pkt) {
		try {
			closed = true;

			if (packetWriterJobName != -1) {
				logger.info("Removing packet writer job {}", packetWriterJobName);
				vertx.cancelTimer(packetWriterJobName);
				packetWriterJobName = -1;
			}

			//an abandoned worker that a newer attempt has already replaced must not touch the outputs
			if (abandonedAtMs != 0 && getInstance().getMuxAdaptor(streamId) != muxAdaptor) {
				logger.error("Abandoned stream fetcher worker returned {}ms after it was given up on, for url:{} streamId:{}."
						+ " Its buffered packets and trailer are dropped because a newer attempt owns the stream",
						System.currentTimeMillis() - abandonedAtMs, streamUrl, streamId);
			}
			else {
				writeAllBufferedPackets();

				if (muxAdaptor != null) {
					logger.info("Writing trailer in MuxAdaptor for streamId:{}", streamId);
					muxAdaptor.writeTrailer();
				}
			}

			if (muxAdaptor != null) {
				getInstance().muxAdaptorRemoved(muxAdaptor);
				muxAdaptor = null;
			}

			if (pkt != null) {
				av_packet_free(pkt);
				pkt.close();
			}

			closeInputFormatContext();
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	private AntMediaApplicationAdapter getInstance() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
		}
		return appInstance;
	}
}
