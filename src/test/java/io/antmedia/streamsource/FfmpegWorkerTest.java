package io.antmedia.streamsource;

import static io.antmedia.streamsource.StreamSourceFixture.peek;
import static io.antmedia.streamsource.StreamSourceFixture.poke;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_get;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.red5.server.api.scope.IScope;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.streamsource.StreamFetcher.Reason;
import io.antmedia.test.UnitTestBase;
import io.vertx.core.Context;

/**
 * The one class that talks to ffmpeg, run against real files and real sockets. The MuxAdaptor is a
 * mock, so every packet the worker hands on is recorded and nothing is written to disk.
 */
@Tag("fast")
class FfmpegWorkerTest extends UnitTestBase<FfmpegWorker> {

	private static final String STREAM_ID = "ffmpeg-worker";

	/** 10s, h264 + aac on a 1/1000 time base. */
	private static final String SHORT_FLV = "src/test/resources/test_short.flv";

	/** 146s, so even unpaced it takes a while to read. */
	private static final String LONG_FLV = "src/test/resources/test_video_360p.flv";

	/** 30s, h264 + aac, a key frame every 3s and a known duration. */
	private static final String MP4 = "src/test/resources/sample_MP4_480.mp4";

	/** 10s, audio only. */
	private static final String MP3 = "src/test/resources/test.mp3";

	/** What the adaptor was handed. byWriter is the buffered path's writer, not the thread reading the source. */
	private record Written(int stream, long dts, long pts, long dtsMs, boolean byWriter) { }

	private StreamSourceFixture fixture;
	private MuxAdaptor adaptor;

	private final Broadcast broadcast = new Broadcast();
	private final Queue<Written> written = new ConcurrentLinkedQueue<>();
	private final AtomicInteger firstPackets = new AtomicInteger();
	private final List<FfmpegWorker> started = new CopyOnWriteArrayList<>();

	@BeforeAll
	static void beforeAll() {
		avformat_network_init();
	}

	@BeforeEach
	void before() throws Exception {
		fixture = new StreamSourceFixture();

		adaptor = mock(MuxAdaptor.class);
		when(adaptor.prepareFromInputFormatContext(any())).thenReturn(true);

		//the packet is unreferenced right after this returns, so copy out what the tests look at
		doAnswer(call -> {
			AVStream stream = call.getArgument(0);
			AVPacket pkt = call.getArgument(1);
			written.add(new Written(pkt.stream_index(), pkt.dts(), pkt.pts(),
					av_rescale_q(pkt.dts(), stream.time_base(), MuxAdaptor.TIME_BASE_FOR_MS), Context.isOnWorkerThread()));
			return null;
		}).when(adaptor).writePacket(any(), any());
	}

	@AfterEach
	void after() {
		started.forEach(worker -> worker.abortRequested.set(true));
		fixture.close();
	}

	@Test
	void pullsAFileToItsEndThroughTheMuxAdaptor() throws Exception {
		FfmpegWorker worker = worker(SHORT_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0);

		assertThat(run(worker)).isEqualTo(Reason.EOF);
		assertThat(worker.error.get().isSuccess()).isTrue();

		verify(adaptor).setEnableVideo(true);
		verify(adaptor).setEnableAudio(true);
		verify(adaptor).setFirstKeyFrameReceivedChecked(false);
		verify(adaptor).setAvc(true);
		verify(adaptor).setBroadcast(broadcast);
		verify(adaptor).init(fixture.scope, STREAM_ID, false);
		verify(adaptor).prepareFromInputFormatContext(any());

		//the broadcast starts on the first packet, not on every one of them
		verify(adaptor).setStartTime(anyLong());
		assertThat(firstPackets).hasValue(1);
		assertThat(worker.lastActivityMs).isPositive();

		assertThat(lastDtsMs(0)).as("video reached the end of the 10s file").isBetween(9500L, 10500L);
		assertThat(lastDtsMs(1)).as("audio reached the end of the 10s file").isBetween(9500L, 10500L);

		InOrder closing = inOrder(adaptor, fixture.app);
		closing.verify(adaptor).writeTrailer();
		closing.verify(fixture.app).muxAdaptorRemoved(adaptor);
		assertThat(worker.getMuxAdaptor()).isNull();
	}

	@Test
	void anAudioOnlySourceHasNoKeyFrameToWaitFor() throws Exception {
		assertThat(run(worker(MP3, AntMediaApplicationAdapter.STREAM_SOURCE, 0))).isEqualTo(Reason.EOF);

		verify(adaptor).setEnableVideo(false);
		verify(adaptor).setEnableAudio(true);
		verify(adaptor).setFirstKeyFrameReceivedChecked(true);
		assertThat(lastDtsMs(0)).isBetween(9500L, 10500L);
	}

	/**
	 * With a buffer, packets take a detour through a sorted set and a 10ms writer. What comes out the
	 * other end has to be exactly what the direct path hands on, packet for packet.
	 */
	@Test
	void theBufferedPathHandsOnExactlyWhatTheDirectOneDoes() throws Exception {
		assertThat(run(worker(LONG_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0))).isEqualTo(Reason.EOF);
		Map<Integer, List<Long>> direct = dtsByStream();

		//1ms is full at once, so the writer paces packets out while the file is still being read.
		//10 minutes never fills from a 146s file, so everything is still queued when the attempt closes
		for (int bufferTimeMs : new int[] { 1, 600000 }) {
			written.clear();
			fixture.appSettings.setStreamFetcherBufferTime(bufferTimeMs);
			FfmpegWorker buffered = worker(LONG_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0);

			assertThat(run(buffered)).isEqualTo(Reason.EOF);

			//audio and video land on the same millisecond all the time, and a set drops what compares equal
			assertThat(dtsByStream()).as("buffer of %dms", bufferTimeMs).isEqualTo(direct);
			assertThat(written.stream().anyMatch(Written::byWriter)).as("paced out by the writer, buffer of %dms", bufferTimeMs)
					.isEqualTo(bufferTimeMs == 1);
			assertThat(peek(buffered, "packetWriterJobName")).as("the writer timer goes with the attempt").isEqualTo(-1L);
		}
	}

	@Test
	void aVodPlaysInRealTimeAndSeeksWhileItPlays() throws Exception {
		FfmpegWorker worker = worker(MP4, AntMediaApplicationAdapter.VOD, 0);
		CompletableFuture<Reason> running = start(worker);

		await().atMost(10, SECONDS).until(() -> !written.isEmpty());
		worker.seek(15000);

		//playing through to 15s would take 15s, so this only passes when the seek landed
		await().atMost(5, SECONDS).until(() -> lastDtsMs(0) >= 15000);
		worker.abortRequested.set(true);

		assertThat(running.get(10, SECONDS)).isEqualTo(Reason.TIMEOUT);
		assertThat(lastDtsMs(0)).as("read as fast as it can be, the 30s file would be over long before the abort")
				.isLessThan(17000);
		verify(adaptor).writeTrailer();
	}

	@Test
	void aSeekTimeGivenAtStartIsWhereThePullBegins() throws Exception {
		assertThat(run(worker(MP4, AntMediaApplicationAdapter.STREAM_SOURCE, 15000))).isEqualTo(Reason.EOF);
		assertThat(firstDtsMs(0)).as("the key frame at or after the seek time").isBetween(15000L, 18000L);

		//past the end of the file there is nothing to seek to, so it plays from the top instead of not at all
		written.clear();
		assertThat(run(worker(MP4, AntMediaApplicationAdapter.STREAM_SOURCE, 60000))).isEqualTo(Reason.EOF);
		assertThat(firstDtsMs(0)).isZero();
	}

	@Test
	void aSourceThatCannotBeOpenedFailsWithTheReasonFfmpegGave() throws Exception {
		//an empty transport lets ffmpeg pick, and the rtsp options are then left out altogether
		fixture.appSettings.setRtspPullTransportType("");

		for (String url : List.of("src/test/resources/no_such_file.flv", "tcp://127.0.0.1:1", "http://127.0.0.1:1/live.flv",
				"https://127.0.0.1:1/live.m3u8", "rtsp://127.0.0.1:1/live")) {
			FfmpegWorker worker = worker(url, AntMediaApplicationAdapter.STREAM_SOURCE, 0);

			assertThat(run(worker)).as(url).isEqualTo(Reason.OPEN_FAILED);
			assertThat(worker.error.get().isSuccess()).as(url).isFalse();
			assertThat(worker.error.get().getMessage()).as(url).isNotBlank();
		}

		verifyNoInteractions(adaptor);
		assertThat(firstPackets).hasValue(0);
	}

	@Test
	void anAbortCutsABlockedOpenShort() throws Exception {
		int port;
		try (DatagramSocket free = new DatagramSocket(0, InetAddress.getLoopbackAddress())) {
			port = free.getLocalPort();
		}

		//nothing ever arrives on this port, ffmpeg would sit out its 15s read timeout in there
		FfmpegWorker worker = worker("udp://127.0.0.1:" + port, AntMediaApplicationAdapter.STREAM_SOURCE, 0);
		CompletableFuture<Reason> running = start(worker);
		await().during(500, MILLISECONDS).atMost(5, SECONDS).until(() -> !running.isDone());

		worker.abortRequested.set(true);

		assertThat(running.get(5, SECONDS)).isEqualTo(Reason.TIMEOUT);
		verifyNoInteractions(adaptor);
	}

	@Test
	void anAbortCutsABlockedReadShort() throws Exception {
		try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
			server.setSoTimeout(10000);
			FfmpegWorker worker = worker("tcp://127.0.0.1:" + server.getLocalPort(), AntMediaApplicationAdapter.STREAM_SOURCE, 0);
			CompletableFuture<Reason> running = start(worker);

			//the whole file and then nothing, the connection stays up like a live source that went quiet
			try (Socket source = server.accept()) {
				source.getOutputStream().write(Files.readAllBytes(Path.of(SHORT_FLV)));
				await().atMost(10, SECONDS).until(() -> lastDtsMs(0) >= 9500 && lastDtsMs(1) >= 9500);

				assertThat(running).as("a quiet source is not the end of it").isNotDone();
				worker.abortRequested.set(true);

				assertThat(running.get(5, SECONDS)).isEqualTo(Reason.TIMEOUT);
			}
		}
	}

	@Test
	void aSourceTheMuxAdaptorCannotTakeIsLetGoOf() throws Exception {
		when(adaptor.prepareFromInputFormatContext(any())).thenReturn(false);
		FfmpegWorker worker = worker(SHORT_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0);

		assertThat(run(worker)).isEqualTo(Reason.OPEN_FAILED);

		assertThat(written).isEmpty();
		assertThat(firstPackets).hasValue(0);
		//init registered it with the application, so it has to go again or a dead stream stays listed
		verify(fixture.app).muxAdaptorRemoved(adaptor);
		assertThat(worker.getMuxAdaptor()).isNull();
	}

	@Test
	void anAttemptThatThrowsStillTakesItsAdaptorDown() throws Exception {
		when(adaptor.init(any(IScope.class), any(), anyBoolean())).thenThrow(new IllegalStateException("broken muxer"));
		FfmpegWorker worker = worker(SHORT_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0);

		assertThat(run(worker)).isEqualTo(Reason.READ_ERROR);

		assertThat(firstPackets).hasValue(0);
		verify(fixture.app).muxAdaptorRemoved(adaptor);
		assertThat(worker.getMuxAdaptor()).isNull();
	}

	@Test
	void anAbandonedWorkerLeavesTheStreamToTheAttemptThatReplacedIt() throws Exception {
		//a buffer that never fills, so every packet is still queued when the attempt closes
		fixture.appSettings.setStreamFetcherBufferTime(600000);
		MuxAdaptor newerAttempt = mock(MuxAdaptor.class);
		when(fixture.app.getMuxAdaptor(STREAM_ID)).thenReturn(newerAttempt);

		//the engine stamps this when it stops waiting, the worker only looks at it on its way out
		FfmpegWorker abandoned = worker(SHORT_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0);
		abandoned.abandonedAtMs = System.currentTimeMillis();

		assertThat(run(abandoned)).isEqualTo(Reason.EOF);
		assertThat(written).as("the queue is dropped, not written over the newer attempt").isEmpty();
		verify(adaptor, never()).writeTrailer();
		verify(fixture.app).muxAdaptorRemoved(adaptor);

		//stamped as well, but nothing replaced it yet, so the stream is still its own to finish
		when(fixture.app.getMuxAdaptor(STREAM_ID)).thenReturn(adaptor);
		FfmpegWorker stillTheOwner = worker(SHORT_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0);
		stillTheOwner.abandonedAtMs = System.currentTimeMillis();

		assertThat(run(stillTheOwner)).isEqualTo(Reason.EOF);
		assertThat(written).isNotEmpty();
		verify(adaptor).writeTrailer();
	}

	@Test
	void allowedMediaTypesMovesFromTheRtspUrlIntoTheOptions() {
		String[][] cases = {
				//url, what ffmpeg is asked to open, allowed_media_types
				{ "rtsp://127.0.0.1:6554/test.flv?allowed_media_types=audio", "rtsp://127.0.0.1:6554/test.flv", "audio" },
				{ "rtsp://127.0.0.1:6554/test.flv?testParam=testParam", "rtsp://127.0.0.1:6554/test.flv?testParam=testParam", null },
				{ "rtsp://127.0.0.1:6554/test.flv", "rtsp://127.0.0.1:6554/test.flv", null },
				{ "rtsp://127.0.0.1:6554/live?allowed_media_types=video%2Baudio", "rtsp://127.0.0.1:6554/live", "video+audio" },
				{ "rtsp://127.0.0.1:6554/live?nodelay&allowed_media_types=&b=2", "rtsp://127.0.0.1:6554/live?nodelay&b=2", null },
				//what stays keeps its order, and the rest of the url is never rebuilt, so escapes and a raw @ survive
				{ "rtsp://test:asdf%2499@127.0.0.1:554/cam/realmonitor?channel=2&subtype=1&allowed_media_types=video",
						"rtsp://test:asdf%2499@127.0.0.1:554/cam/realmonitor?channel=2&subtype=1", "video" },
				{ "rtsp://user:pass@word@127.0.0.1:6554/stream?allowed_media_types=video", "rtsp://user:pass@word@127.0.0.1:6554/stream", "video" },
				//java cannot parse this one, so ffmpeg gets it exactly as it was given
				{ "rtsp://127.0.0.1:  space  6554/test.flv?allowed_media_types=video",
						"rtsp://127.0.0.1:  space  6554/test.flv?allowed_media_types=video", null },
		};

		for (String[] testCase : cases) {
			FfmpegWorker worker = worker(testCase[0], AntMediaApplicationAdapter.IP_CAMERA, 0);

			try (AVDictionary options = new AVDictionary()) {
				worker.parseRtspUrlParams(options);

				AVDictionaryEntry mediaTypes = av_dict_get(options, "allowed_media_types", null, 0);
				assertThat(peek(worker, "streamUrl")).as(testCase[0]).isEqualTo(testCase[1]);
				assertThat(mediaTypes == null ? null : mediaTypes.value().getString()).as(testCase[0]).isEqualTo(testCase[2]);
				av_dict_free(options);
			}
		}
	}

	@Test
	void everyRetryAsksForTheMediaTypesAgain() {
		//the first attempt used to strip them off the fetcher's own url, so every retry after it lost them
		StreamFetcher fetcher = new StreamFetcher("rtsp://127.0.0.1:6554/live?allowed_media_types=video", STREAM_ID,
				AntMediaApplicationAdapter.IP_CAMERA, fixture.scope, fixture.vertx, 0);

		for (int attempt = 1; attempt <= 2; attempt++) {
			try (AVDictionary options = new AVDictionary()) {
				((FfmpegWorker) fetcher.createWorker()).parseRtspUrlParams(options);

				assertThat(av_dict_get(options, "allowed_media_types", null, 0).value().getString()).as("attempt %d", attempt)
						.isEqualTo("video");
				av_dict_free(options);
			}
		}
	}

	/**
	 * What a stream sends only ever moves forward. 15 after 20 is a stray packet and goes out just past
	 * 20. 0 after 30 is the source starting over: it goes out just past 30 too, and 10 and 20 keep their
	 * spacing from there on.
	 */
	@Test
	void theSentTimelineOnlyEverMovesForward() throws Exception {
		AVFormatContext input = open(SHORT_FLV);
		try {
			FfmpegWorker worker = attachedTo(input);
			AVStream audio = input.streams(1);

			for (long dts : new long[] { 10, 20, 15, 25, 30, 0, 10, 20 }) {
				AVPacket pkt = av_packet_alloc();
				pkt.stream_index(1).dts(dts).pts(dts);
				invoke(worker, "writePacket", new Class<?>[] { AVStream.class, AVPacket.class }, audio, pkt);
				av_packet_free(pkt);
			}

			assertThat(written).extracting(Written::dts).containsExactly(10L, 20L, 21L, 25L, 30L, 31L, 41L, 51L);
			//a packet can not be shown before it is decoded, so a dts pushed forward drags its pts along
			assertThat(written).allMatch(packet -> packet.pts() >= packet.dts());
		}
		finally {
			avformat_close_input(input);
		}
	}

	@Test
	void audioAndVideoThatDriftApartArePulledBackTogether() throws Exception {
		AVFormatContext input = open(SHORT_FLV);
		try {
			FfmpegWorker worker = attachedTo(input);
			long[] sent = (long[]) peek(worker, "lastSentDTS");
			sent[0] = 0;
			sent[1] = 200;

			//looked at no more than once every 2s
			poke(worker, "lastSycnCheckTime", System.currentTimeMillis());
			invoke(worker, "checkAndFixSynch", new Class<?>[0]);
			assertThat(sent).containsExactly(0L, 200L);

			poke(worker, "lastSycnCheckTime", 1L);
			invoke(worker, "checkAndFixSynch", new Class<?>[0]);
			assertThat(sent).as("the stream that fell behind is moved up to the one ahead").containsExactly(200L, 200L);

			//under 150ms is ordinary interleaving and is left alone
			sent[0] = 100;
			poke(worker, "lastSycnCheckTime", 1L);
			invoke(worker, "checkAndFixSynch", new Class<?>[0]);
			assertThat(sent).containsExactly(100L, 200L);
		}
		finally {
			avformat_close_input(input);
		}
	}

	private FfmpegWorker worker(String url, String type, long seekTimeMs) {
		FfmpegWorker worker = new FfmpegWorker(url, STREAM_ID, type, seekTimeMs, fixture.scope, fixture.vertx, fixture.appSettings);
		worker.onFirstPacket = firstPackets::incrementAndGet;
		return worker;
	}

	/** Static mocks only hold on the thread that made them, so the attempt runs on that same thread. */
	private CompletableFuture<Reason> start(FfmpegWorker worker) {
		started.add(worker);

		return CompletableFuture.supplyAsync(() -> {
			try (MockedStatic<MuxAdaptor> statics = mockStatic(MuxAdaptor.class)) {
				statics.when(() -> MuxAdaptor.initializeMuxAdaptor(any(), any(), anyBoolean(), any())).thenReturn(adaptor);
				return worker.run(broadcast);
			}
		}, fixture.pool);
	}

	private Reason run(FfmpegWorker worker) throws Exception {
		return start(worker).get(60, SECONDS);
	}

	/** A worker past its open, so its packet path can be driven one packet at a time. */
	@SuppressWarnings("unchecked")
	private FfmpegWorker attachedTo(AVFormatContext input) throws Exception {
		FfmpegWorker worker = worker(SHORT_FLV, AntMediaApplicationAdapter.STREAM_SOURCE, 0);
		poke(worker, "inputFormatContext", input);
		invoke(worker, "initDTSArrays", new Class<?>[] { int.class }, input.nb_streams());
		((AtomicReference<MuxAdaptor>) peek(worker, "muxAdaptor")).set(adaptor);
		return worker;
	}

	private static AVFormatContext open(String file) {
		AVFormatContext input = avformat_alloc_context();
		assertThat(avformat_open_input(input, file, (AVInputFormat) null, (AVDictionary) null)).isZero();
		assertThat(avformat_find_stream_info(input, (AVDictionary) null)).isNotNegative();
		return input;
	}

	private static void invoke(FfmpegWorker worker, String name, Class<?>[] types, Object... args) throws Exception {
		Method method = FfmpegWorker.class.getDeclaredMethod(name, types);
		method.setAccessible(true);
		method.invoke(worker, args);
	}

	private long lastDtsMs(int stream) {
		return written.stream().filter(packet -> packet.stream() == stream).mapToLong(Written::dtsMs).max().orElse(-1);
	}

	private long firstDtsMs(int stream) {
		return written.stream().filter(packet -> packet.stream() == stream).findFirst().orElseThrow().dtsMs();
	}

	private Map<Integer, List<Long>> dtsByStream() {
		return written.stream().collect(groupingBy(Written::stream, mapping(Written::dts, toList())));
	}
}
