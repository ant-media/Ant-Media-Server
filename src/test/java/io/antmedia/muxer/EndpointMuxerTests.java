package io.antmedia.muxer;

import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AC3;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_new_stream;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_free;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.red5.server.scope.WebScope;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.antmedia.AppSettings;
import io.antmedia.muxer.EndpointMuxerPacingEngine.TimeBase;
import io.antmedia.muxer.EndpointMuxerPacingPolicy.Action;
import io.antmedia.muxer.EndpointMuxerPacingPolicy.PacingDecision;
import io.antmedia.muxer.parser.SPSParser;
import io.vertx.core.Vertx;

/**
 * Everything that pushes to an external endpoint: the muxer, the pacing engine, both policies
 * and the analytics.
 *
 * Policies run against real packets and a real engine, so a decision is proved by what lands on
 * the queue. Thresholds longer than the grace window are reached by rewinding the policy's own
 * nanoTime stamp rather than waiting them out.
 */
@ContextConfiguration(locations = {"classpath:io/antmedia/test/test.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class EndpointMuxerTests {

	private static final String URL = "endpoint://test";
	private static final int VIDEO = 0;
	private static final int AUDIO = 1;
	private static final int NO_VIDEO = -1;

	/** Mirrors of the policy constants under test. */
	private static final long GRACE_MS = 1000L;
	private static final long RESUME_WAIT_LIMIT_MS = 10_000L;
	private static final long LAG_THRESHOLD_MS = 3000L;

	private static final long FRAME_MS = 40L;
	/** Well under the 2ms gap that separates a burst from ordinary interleave. */
	private static final long WRITE_NANOS = 50_000L;

	private static final byte[] EXTRADATA_ORIGINAL = new byte[]{0x00, 0x00, 0x00, 0x01, 0x67, 0x64, 0x00, 0x15, (byte) 0xAC,
			(byte) 0xB2, 0x03, (byte) 0xC1, 0x7F, (byte) 0xCB, (byte) 0x80, (byte) 0x88, 0x00, 0x00, 0x03, 0x00, 0x08, 0x00,
			0x00, 0x03, 0x01, (byte) 0x94, 0x78, (byte) 0xB1, 0x72, 0x40, 0x00, 0x00, 0x00, 0x01, 0x68, (byte) 0xEB,
			(byte) 0xC3, (byte) 0xCB, (byte) 0x22, (byte) 0xC0};

	private static final byte[] SPS_PPS_AVC = new byte[]{0x01, 0x64, 0x00, 0x15, (byte) 0xFF, (byte) 0xE1, 0x00, 0x1A, 0x67,
			0x64, 0x00, 0x15, (byte) 0xAC, (byte) 0xB2, 0x03, (byte) 0xC1, 0x7F, (byte) 0xCB, (byte) 0x80, (byte) 0x88, 0x00,
			0x00, 0x03, 0x00, 0x08, 0x00, 0x00, 0x03, 0x01, (byte) 0x94, (byte) 0x78, (byte) 0xB1, 0x72, 0x40, 0x01, 0x00,
			0x06, 0x68, (byte) 0xEB, (byte) 0xC3, (byte) 0xCB, 0x22, (byte) 0xC0};

	/** Held for the run: codecpar keeps only the native address, so a local would be collected. */
	private static final BytePointer SPS_PPS_POINTER = new BytePointer(SPS_PPS_AVC);

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	@Autowired
	private ApplicationContext applicationContext;

	private WebScope appScope;
	private Vertx vertx;
	private ListAppender<ILoggingEvent> logAppender;

	@BeforeAll
	public static void beforeClass() {
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
	}

	@BeforeEach
	public void before() {
		new File("webapps/junit").mkdirs();
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);
		getAppSettings().resetDefaults();
	}

	@AfterEach
	public void after() {
		getAppSettings().resetDefaults();
		// The endpoint logger is static, so a leftover appender would collect for the whole JVM.
		if (logAppender != null) {
			((Logger) LoggerFactory.getLogger(EndpointMuxer.class)).detachAppender(logAppender);
			logAppender = null;
		}
	}

	private AppSettings getAppSettings() {
		return (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
	}

	private static AVPacket packet(int streamIndex, long dts, boolean keyFrame) {
		AVPacket pkt = av_packet_alloc();
		pkt.stream_index(streamIndex);
		pkt.pts(dts);
		pkt.dts(dts);
		if (keyFrame) {
			pkt.flags(pkt.flags() | AV_PKT_FLAG_KEY);
		}
		return pkt;
	}

	/** Two streams on a millisecond time base, so a packet dts reads directly as output ms. */
	private static EndpointMuxerPacingEngine newEngine(EndpointMuxerPacingPolicy policy, int videoStreamIndex) {
		TimeBase[] timeBases = {new TimeBase(1, 1000), new TimeBase(1, 1000)};
		return new EndpointMuxerPacingEngine(policy, new EndpointMuxerAnalytics(URL, policy.queueCapacity()),
				timeBases, videoStreamIndex);
	}

	private static AVRational msTimeBase() {
		return new AVRational().num(1).den(1000);
	}

	/** Mirrors the muxer: the producer keeps its own packet, the engine queues a clone. */
	private static void submitAndFree(EndpointMuxerPacingEngine engine, AVPacket pkt) {
		engine.submit(pkt);
		av_packet_free(pkt);
	}

	private static void drainAndFree(EndpointMuxerPacingEngine engine) {
		AVPacket pkt;
		while ((pkt = engine.drainNext()) != null) {
			av_packet_free(pkt);
		}
	}

	private static long nextDts(EndpointMuxerPacingEngine engine) {
		AVPacket pkt = engine.drainNext();
		assertNotNull(pkt);
		long dts = pkt.dts();
		av_packet_free(pkt);
		return dts;
	}

	/** @return output dts of the one packet the engine is expected to be holding. */
	private static long drainOne(EndpointMuxerPacingEngine engine) {
		long dts = nextDts(engine);
		assertNull(engine.drainNext());
		return dts;
	}

	/** Pushes a nanosecond stamp into the past so a wall clock threshold fires without waiting for it. */
	private static void rewind(Object target, String field, long millis) {
		long stamp = (Long) ReflectionTestUtils.getField(target, field);
		ReflectionTestUtils.setField(target, field, stamp - millis * 1_000_000L);
	}

	/** Opens grace on a keyframe at dts 0 and resumes there, so live edge starts at offsetMs 0. */
	private static void reachSteadyState(EndpointMuxerPacingPolicy policy, EndpointMuxerPacingEngine engine) {
		submitAndFree(engine, packet(VIDEO, 0, true));
		rewind(policy, "graceStartNanos", GRACE_MS + 100);
		submitAndFree(engine, packet(VIDEO, 0, true));
		drainAndFree(engine);
	}

	private static Object policyOf(EndpointMuxer muxer) {
		EndpointMuxerPacingEngine engine = (EndpointMuxerPacingEngine) ReflectionTestUtils.getField(muxer, "engine");
		assertNotNull(engine);
		return ReflectionTestUtils.getField(engine, "policy");
	}

	/** Grace already elapsed with no cycle open, so packets pass without needing a keyframe. */
	private static void skipStartupGrace(EndpointMuxer muxer) {
		Object policy = policyOf(muxer);
		ReflectionTestUtils.setField(policy, "graceStarted", true);
		ReflectionTestUtils.setField(policy, "graceStartNanos", System.nanoTime() - 2 * GRACE_MS * 1_000_000L);
	}

	private static Object invokeEndpointMethod(EndpointMuxer muxer, String name, Class<?>[] types, Object... args)
			throws Exception {
		Method method = EndpointMuxer.class.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method.invoke(muxer, args);
	}

	/** All endpoint classes log under the EndpointMuxer category on purpose. */
	private ListAppender<ILoggingEvent> captureEndpointLogs() {
		logAppender = new ListAppender<>();
		logAppender.start();
		((Logger) LoggerFactory.getLogger(EndpointMuxer.class)).addAppender(logAppender);
		return logAppender;
	}

	private static boolean logged(ListAppender<ILoggingEvent> logs, String fragment) {
		return logs.list.stream().anyMatch(event -> event.getFormattedMessage().contains(fragment));
	}

	private static AVCodecParameters h264Parameters() {
		SPSParser spsParser = new SPSParser(EXTRADATA_ORIGINAL, 5);
		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.width(spsParser.getWidth());
		codecParameters.height(spsParser.getHeight());
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		codecParameters.extradata_size(SPS_PPS_AVC.length);
		codecParameters.extradata(SPS_PPS_POINTER);
		codecParameters.format(AV_PIX_FMT_YUV420P);
		codecParameters.codec_tag(0);
		return codecParameters;
	}

	/** Engine tests set the decision directly instead of reasoning through a real policy. */
	private static final class StubPolicy implements EndpointMuxerPacingPolicy {

		private final int capacity;
		private PacingDecision next = PacingDecision.pass(0);

		StubPolicy(int capacity) {
			this.capacity = capacity;
		}

		@Override
		public int queueCapacity() {
			return capacity;
		}

		@Override
		public PacingDecision onPacket(AVPacket pkt, EndpointMuxerPacingEngine engine) {
			return next;
		}
	}

	@Test
	public void testPacingDecision() {
		assertEquals(Action.DISCARD, PacingDecision.discard().action());
		assertEquals(0, PacingDecision.discard().shiftMs());
		assertSame(PacingDecision.discard(), PacingDecision.discard());

		PacingDecision unshifted = PacingDecision.pass(0);
		assertEquals(Action.PASS, unshifted.action());
		assertSame(unshifted, PacingDecision.pass(0), "a policy that never shifts must not allocate per packet");

		PacingDecision shifted = PacingDecision.pass(120);
		assertEquals(Action.PASS, shifted.action());
		assertEquals(120, shifted.shiftMs());
	}

	@Test
	public void testEngineQueuesCloneAndDrainsInOrder() {
		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(4), VIDEO);

		AVPacket src = packet(VIDEO, 500, true);
		engine.submit(src);
		// The producer reuses tmpPacket, so anything it does after submit must not reach the queue.
		src.dts(9999);
		av_packet_free(src);

		submitAndFree(engine, packet(AUDIO, 520, true));
		assertEquals(2, engine.size());

		assertEquals(500, nextDts(engine));
		assertEquals(520, nextDts(engine));
		assertEquals(0, engine.size());
		assertNull(engine.drainNext());
	}

	@Test
	public void testEngineRejectsOutOfRangeStreamIndex() {
		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(4), VIDEO);
		AVPacket pkt = packet(7, 100, false);

		engine.submit(pkt);
		assertEquals(0, engine.size(), "an unplaceable packet must never reach the queue");
		assertEquals(AV_NOPTS_VALUE, engine.toMs(pkt));

		av_packet_free(pkt);
	}

	@Test
	public void testEngineAppliesThePolicyDecision() {
		StubPolicy policy = new StubPolicy(4);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);

		policy.next = PacingDecision.discard();
		submitAndFree(engine, packet(VIDEO, 100, true));
		assertEquals(0, engine.size());

		policy.next = PacingDecision.pass(200);
		submitAndFree(engine, packet(VIDEO, 1000, true));
		assertEquals(800, drainOne(engine));

		// T9: AV_NOPTS_VALUE is Long.MIN_VALUE, so subtracting would overflow it into a timestamp.
		AVPacket noDts = av_packet_alloc();
		noDts.stream_index(VIDEO);
		submitAndFree(engine, noDts);

		AVPacket queued = engine.drainNext();
		assertNotNull(queued);
		assertEquals(AV_NOPTS_VALUE, queued.dts());
		assertEquals(AV_NOPTS_VALUE, queued.pts());
		av_packet_free(queued);
	}

	@Test
	public void testEngineCapsTheQueueAtCapacity() {
		// This policy passes even once full, so the engine has to drop the clone itself.
		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(1), VIDEO);

		submitAndFree(engine, packet(VIDEO, 0, false));
		submitAndFree(engine, packet(VIDEO, FRAME_MS, false));

		assertTrue(engine.isFull());
		assertEquals(1, engine.size());
		assertEquals(0, drainOne(engine), "the packet the queue refused never displaced the first");
	}

	@Test
	public void testEngineToMsRescalesFromTheStreamTimeBase() {
		TimeBase[] timeBases = {new TimeBase(1, 90000), new TimeBase(1, 1000)};
		EndpointMuxerPacingEngine engine = new EndpointMuxerPacingEngine(new StubPolicy(4),
				new EndpointMuxerAnalytics(URL, 4), timeBases, VIDEO);

		assertEquals(2, engine.streamCount());
		assertEquals(VIDEO, engine.videoStreamIndex());

		AVPacket video = packet(VIDEO, 90000, false);
		AVPacket audio = packet(AUDIO, 1000, false);
		AVPacket noDts = av_packet_alloc();
		noDts.stream_index(VIDEO);

		assertEquals(1000, engine.toMs(video));
		assertEquals(1000, engine.toMs(audio));
		assertEquals(AV_NOPTS_VALUE, engine.toMs(noDts));

		av_packet_free(video);
		av_packet_free(audio);
		av_packet_free(noDts);
	}

	@Test
	public void testEngineFlushAnchorsOnTheOldestQueuedVideoPacket() {
		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(8), VIDEO);

		submitAndFree(engine, packet(AUDIO, 100, true));
		submitAndFree(engine, packet(VIDEO, 140, true));
		submitAndFree(engine, packet(VIDEO, 180, false));

		assertEquals(140, engine.flush(), "video anchors the resume even when audio is older");
		assertEquals(0, engine.size());
	}

	@Test
	public void testEngineFlushFallsBackWhenThereIsNoVideoAnchor() {
		EndpointMuxerPacingEngine audioOnly = newEngine(new StubPolicy(8), NO_VIDEO);

		assertEquals(AV_NOPTS_VALUE, audioOnly.flush(), "nothing queued means nothing to charge");

		submitAndFree(audioOnly, packet(AUDIO, 250, true));
		submitAndFree(audioOnly, packet(AUDIO, 290, true));
		assertEquals(250, audioOnly.flush());

		// A queue the drain could not have placed anyway gives no anchor either.
		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(4), VIDEO);
		AVPacket noDts = av_packet_alloc();
		noDts.stream_index(VIDEO);
		submitAndFree(engine, noDts);
		assertEquals(1, engine.size());

		assertEquals(AV_NOPTS_VALUE, engine.flush());
		assertEquals(0, engine.size());
	}

	@Test
	public void testBacklogResumesOnlyOnAVideoKeyframe() {
		EndpointMuxerBacklogPacing policy = new EndpointMuxerBacklogPacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);

		submitAndFree(engine, packet(VIDEO, 0, true));
		assertEquals(0, engine.size(), "grace swallows the opening packet");

		rewind(policy, "graceStartNanos", GRACE_MS + 100);

		submitAndFree(engine, packet(VIDEO, 40, false));
		assertEquals(0, engine.size(), "grace releases on a keyframe, not on the deadline");

		// T22: writeAudioBuffer stamps KEY on every audio packet, so the flag alone is not enough.
		submitAndFree(engine, packet(AUDIO, 40, true));
		assertEquals(0, engine.size());

		submitAndFree(engine, packet(VIDEO, 80, true));
		assertEquals(80, drainOne(engine), "backlog pacing never rewrites timestamps");
	}

	@Test
	public void testBacklogGraceWindowRunsOnWallClock() throws InterruptedException {
		// The one test that spends real time, so the 1000ms window is proved rather than assumed.
		EndpointMuxerBacklogPacing policy = new EndpointMuxerBacklogPacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);

		submitAndFree(engine, packet(VIDEO, 0, true));
		Thread.sleep(200);
		submitAndFree(engine, packet(VIDEO, 200, true));
		assertEquals(0, engine.size(), "still inside the grace window");

		Thread.sleep(GRACE_MS);
		submitAndFree(engine, packet(VIDEO, 1200, true));
		assertEquals(1, engine.size(), "grace released once the window elapsed");

		engine.close();
	}

	@Test
	public void testBacklogFlushesAFullQueueAndRestartsOnAKeyframe() {
		EndpointMuxerBacklogPacing policy = new EndpointMuxerBacklogPacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		long dts = FRAME_MS;
		for (int i = 0; i < policy.queueCapacity(); i++, dts += FRAME_MS) {
			submitAndFree(engine, packet(VIDEO, dts, false));
		}
		assertEquals(policy.queueCapacity(), engine.size());
		assertTrue(engine.isFull());

		submitAndFree(engine, packet(VIDEO, dts, false));
		assertEquals(0, engine.size(), "a full queue is flushed whole, not trimmed");

		dts += FRAME_MS;
		submitAndFree(engine, packet(VIDEO, dts, false));
		assertEquals(0, engine.size(), "the cycle holds until a keyframe");

		dts += FRAME_MS;
		submitAndFree(engine, packet(VIDEO, dts, true));
		assertEquals(dts, drainOne(engine), "the hole stays visible to the endpoint");
	}

	/** Both policies, and live edge lands at 0 only because it charges the grace window away. */
	@Test
	public void testAudioOnlyEndpointResumesOnAnyPacket() {
		assertAudioOnlyResumes(new EndpointMuxerBacklogPacing(URL), 20);
		assertAudioOnlyResumes(new EndpointMuxerLiveEdgePacing(URL), 0);
	}

	private static void assertAudioOnlyResumes(EndpointMuxerPacingPolicy policy, long expectedDts) {
		EndpointMuxerPacingEngine engine = newEngine(policy, NO_VIDEO);

		submitAndFree(engine, packet(AUDIO, 0, true));
		rewind(policy, "graceStartNanos", GRACE_MS + 100);

		submitAndFree(engine, packet(AUDIO, 20, false));
		assertEquals(expectedDts, drainOne(engine), policy.getClass().getSimpleName()
				+ " would wait forever for a keyframe that cannot come");
	}

	/** Both policies, and live edge lands at 0 only because it charges the grace window away. */
	@Test
	public void testResumeWaitLimitReleasesWithoutAKeyframe() {
		assertWaitLimitReleases(new EndpointMuxerBacklogPacing(URL), 40);
		assertWaitLimitReleases(new EndpointMuxerLiveEdgePacing(URL), 0);
	}

	private static void assertWaitLimitReleases(EndpointMuxerPacingPolicy policy, long expectedDts) {
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);

		submitAndFree(engine, packet(VIDEO, 0, true));
		rewind(policy, "graceStartNanos", GRACE_MS + 100);
		rewind(policy, "cycleStartNanos", RESUME_WAIT_LIMIT_MS + 100);

		submitAndFree(engine, packet(VIDEO, 40, false));
		assertEquals(expectedDts, drainOne(engine), policy.getClass().getSimpleName()
				+ " must resume even if a keyframe never arrives");
	}

	@Test
	public void testLiveEdgeSteadyStreamNeverShifts() {
		EndpointMuxerLiveEdgePacing policy = new EndpointMuxerLiveEdgePacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		for (long dts = FRAME_MS; dts <= 20 * FRAME_MS; dts += FRAME_MS) {
			submitAndFree(engine, packet(VIDEO, dts, false));
			assertEquals(dts, drainOne(engine));
		}
	}

	@Test
	public void testLiveEdgeChargesAFullQueueFlushToTheOffset() {
		EndpointMuxerLiveEdgePacing policy = new EndpointMuxerLiveEdgePacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		long dts = FRAME_MS;
		for (int i = 0; i < policy.queueCapacity(); i++, dts += FRAME_MS) {
			submitAndFree(engine, packet(VIDEO, dts, false));
		}
		assertTrue(engine.isFull());

		// The drain never ran, so the oldest queued packet is where it would have carried on.
		submitAndFree(engine, packet(VIDEO, dts, false));
		assertEquals(0, engine.size());

		dts += FRAME_MS;
		submitAndFree(engine, packet(VIDEO, dts, true));
		assertEquals(FRAME_MS, drainOne(engine), "output resumes at the flushed anchor, not at the source dts");
	}

	@Test
	public void testLiveEdgeGapWithQueuedPacketsStillNeedsAKeyframe() {
		EndpointMuxerLiveEdgePacing policy = new EndpointMuxerLiveEdgePacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		// Left on the queue, so the flush throws away frames the decoder needs.
		submitAndFree(engine, packet(VIDEO, FRAME_MS, false));

		submitAndFree(engine, packet(VIDEO, 6000 + FRAME_MS, false));
		assertEquals(0, engine.size(), "the undelivered backlog is flushed");

		submitAndFree(engine, packet(VIDEO, 6000 + 2 * FRAME_MS, false));
		assertEquals(0, engine.size());

		// T22: audio carries the KEY flag too, so it must not end a video cycle.
		submitAndFree(engine, packet(AUDIO, 6000 + 2 * FRAME_MS, true));
		assertEquals(0, engine.size());

		submitAndFree(engine, packet(VIDEO, 6000 + 3 * FRAME_MS, true));
		assertEquals(FRAME_MS, drainOne(engine));
	}

	@Test
	public void testLiveEdgeChargesEachStallByAssignment() {
		// T1 and T10: the anchor comes off the queue already carrying the offset, so a second
		// stall must not charge the first one twice. Each 6s hole collapses to one frame.
		EndpointMuxerLiveEdgePacing policy = new EndpointMuxerLiveEdgePacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		long inputDts = FRAME_MS;
		long expectedOutput = FRAME_MS;
		submitAndFree(engine, packet(VIDEO, inputDts, false));
		assertEquals(expectedOutput, drainOne(engine));

		for (int stall = 0; stall < 3; stall++) {
			inputDts += 6000;
			expectedOutput += FRAME_MS;
			submitAndFree(engine, packet(VIDEO, inputDts, false));
			assertEquals(expectedOutput, drainOne(engine), "output advances one frame per stall, whatever the gap");

			inputDts += FRAME_MS;
			expectedOutput += FRAME_MS;
			submitAndFree(engine, packet(VIDEO, inputDts, false));
			assertEquals(expectedOutput, drainOne(engine));
		}
	}

	@Test
	public void testLiveEdgeSkipsForwardWhenArrivalsLagLive() {
		EndpointMuxerLiveEdgePacing policy = new EndpointMuxerLiveEdgePacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		submitAndFree(engine, packet(VIDEO, FRAME_MS, false));
		assertEquals(FRAME_MS, drainOne(engine));

		// Timestamps stay continuous, so only wall clock exposes this one.
		rewind(policy, "wallStartNanos", LAG_THRESHOLD_MS + 1000);

		submitAndFree(engine, packet(VIDEO, 2 * FRAME_MS, false));
		assertEquals(0, engine.size(), "the skip target is 3s ahead of what just arrived");

		submitAndFree(engine, packet(VIDEO, 2000, true));
		assertEquals(0, engine.size(), "a keyframe short of the target does not resume");

		submitAndFree(engine, packet(VIDEO, 4000, true));
		assertEquals(3 * FRAME_MS, drainOne(engine));
	}

	@Test
	public void testLiveEdgeBanksNoMarginFromAFastSource() {
		EndpointMuxerLiveEdgePacing policy = new EndpointMuxerLiveEdgePacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		// 45s of media in no wall clock time at all. Every step re-anchors, so none of it banks.
		for (long dts = 900; dts <= 45_000; dts += 900) {
			submitAndFree(engine, packet(VIDEO, dts, false));
			assertEquals(dts, drainOne(engine));
		}

		rewind(policy, "wallStartNanos", LAG_THRESHOLD_MS + 1000);
		submitAndFree(engine, packet(VIDEO, 45_900, false));

		assertEquals(0, engine.size(), "45s of banked margin would swallow this 4s stall");
	}

	@Test
	public void testLiveEdgeIgnoresPacketsWithoutDts() {
		EndpointMuxerLiveEdgePacing policy = new EndpointMuxerLiveEdgePacing(URL);
		EndpointMuxerPacingEngine engine = newEngine(policy, VIDEO);
		reachSteadyState(policy, engine);

		submitAndFree(engine, packet(VIDEO, FRAME_MS, false));

		AVPacket noDts = av_packet_alloc();
		noDts.stream_index(VIDEO);
		submitAndFree(engine, noDts);
		assertEquals(2, engine.size(), "a missing dts is not a reason to drop the packet");

		// It must not have overwritten lastInputDtsMs either, or this gap goes unnoticed.
		submitAndFree(engine, packet(VIDEO, 6000, false));
		assertEquals(0, engine.size(), "gap flushed the backlog, so the cycle owes a keyframe");

		// A keyframe on purpose: without the dts guard this would resume and charge garbage.
		AVPacket duringCycle = av_packet_alloc();
		duringCycle.stream_index(VIDEO);
		duringCycle.flags(duringCycle.flags() | AV_PKT_FLAG_KEY);
		submitAndFree(engine, duringCycle);
		assertEquals(0, engine.size(), "a packet with no dts cannot be a resume point");
	}

	/** Backstops that a real engine cannot reach, since it rejects the same indexes first. */
	@Test
	public void testFailsafeGuardsRejectUnplaceableStreamIndexes() throws Exception {
		EndpointMuxerPacingEngine stalled = mock(EndpointMuxerPacingEngine.class);
		when(stalled.toMs(any())).thenReturn(100L);
		when(stalled.streamCount()).thenReturn(1);

		AVPacket pkt = packet(5, 100, false);
		assertEquals(Action.DISCARD, new EndpointMuxerLiveEdgePacing(URL).onPacket(pkt, stalled).action());

		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(4), VIDEO);
		Method shift = EndpointMuxerPacingEngine.class.getDeclaredMethod("shift", AVPacket.class, long.class);
		shift.setAccessible(true);
		shift.invoke(engine, pkt, 200L);
		assertEquals(100, pkt.dts());

		av_packet_free(pkt);
	}

	/** Analytics only ever emits logs, so the logs are the behaviour worth asserting. */
	@Test
	public void testAnalyticsWarnsOnDropBurstAndWriteSpike() {
		ListAppender<ILoggingEvent> logs = captureEndpointLogs();
		EndpointMuxerAnalytics analytics = new EndpointMuxerAnalytics(URL, 250);

		analytics.recordDrop(50);
		assertTrue(logged(logs, "dropped 50 packets"));

		// Burst opens before any packet carried a dts. The span baseline has to latch on the
		// first real one, or every later span is measured from Long.MIN_VALUE and never warns.
		analytics.recordWrite(WRITE_NANOS, AV_NOPTS_VALUE, 10);
		analytics.recordWrite(WRITE_NANOS, AV_NOPTS_VALUE, 10);
		analytics.recordWrite(WRITE_NANOS, 0L, 10);
		analytics.recordWrite(WRITE_NANOS, 400L, 10);
		assertFalse(logged(logs, "burst-flush"), "400ms of dts is ordinary interleave, not a drain");

		analytics.recordWrite(WRITE_NANOS, 800L, 10);
		assertTrue(logged(logs, "burst-flush"));

		// 5x the 50us baseline but under the 100ms floor, so still not worth waking anyone.
		analytics.recordWrite(1_000_000L, 1200L, 10);
		assertFalse(logged(logs, "Write latency spike"));

		analytics.recordWrite(500_000_000L, 1600L, 10);
		assertTrue(logged(logs, "Write latency spike"));
	}

	@Test
	public void testParseEndpointURL() {
		// flv is the constructor default, so the options are what actually prove the rtmp branch ran.
		EndpointMuxer rtmp = new EndpointMuxer("rtmp://host/app/stream", vertx);
		assertEquals("rtmp", rtmp.getMuxerType());
		assertEquals("flv", rtmp.getFormat());
		assertEquals("5000000", rtmp.options.get("rw_timeout"), "bounds a dead remote");
		assertEquals("1048576", rtmp.options.get("send_buffer_size"), "keeps the kernel from parking media");
		assertEquals("1", rtmp.options.get("tcp_nodelay"));

		EndpointMuxer srt = new EndpointMuxer("srt://host:1234", vertx);
		assertEquals("srt", srt.getMuxerType());
		assertEquals("mpegts", srt.getFormat());
		assertNull(srt.options.get("rw_timeout"), "srt takes none of the rtmp tuning");

		EndpointMuxer unknown = new EndpointMuxer(null, vertx);
		assertNull(unknown.getMuxerType());
		assertNull(unknown.getOutputURL());
	}

	@Test
	public void testAddStreamAcceptsOnlySupportedCodecs() {
		EndpointMuxer muxer = new EndpointMuxer(null, vertx);

		assertTrue(muxer.isCodecSupported(AV_CODEC_ID_H264));
		assertTrue(muxer.isCodecSupported(AV_CODEC_ID_AAC));
		assertFalse(muxer.isCodecSupported(AV_CODEC_ID_AC3));

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);
		assertFalse(muxer.addStream(null, codecContext, 0), "no codec id yet");
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED, muxer.getStatus());

		codecContext.codec_id(AV_CODEC_ID_H264);
		assertTrue(muxer.addStream(null, codecContext, 10240));
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING, muxer.getStatus());

		assertTrue(muxer.addVideoStream(480, 360, Muxer.avRationalTimeBase, AV_CODEC_ID_H264, 0, true, null));
		assertEquals("extract_extradata", muxer.getBitStreamFilter(), "flv needs extradata pulled out");
	}

	@Test
	public void testWriteVideoBufferHoldsUntilTheFirstKeyframe() {
		EndpointMuxer muxer = spy(new EndpointMuxer(null, vertx));
		muxer.init(appScope, "", 0, "", 0);

		// A keyframe on purpose: a P frame would stop at the keyframe gate anyway, proving nothing.
		muxer.writeVideoBuffer(null, 10, 1, 1, true, 10, 10);

		muxer.setIsRunning(new AtomicBoolean(true));
		muxer.getRegisteredStreamIndexList().add(1);
		muxer.writeVideoBuffer(null, 10, 1, 1, false, 10, 10);
		verify((Muxer) muxer, times(0)).writeVideoBuffer(any());

		doNothing().when(muxer).writeVideoBuffer(any());
		muxer.writeVideoBuffer(null, 10, 1, 1, true, 10, 10);
		verify((Muxer) muxer, times(1)).writeVideoBuffer(any());
	}

	@Test
	public void testGetOutputFormatContext() {
		EndpointMuxer muxer = new EndpointMuxer("rtmp://test.antmedia.io/LiveApp/test", vertx);

		muxer.setFormat("testing");
		assertNull(muxer.getOutputFormatContext());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED, muxer.getStatus());

		muxer.setFormat("mpegts");
		AVFormatContext context = muxer.getOutputFormatContext();
		assertNotNull(context);
		assertNotEquals(0L, context.address(), "a failed alloc must not leave an unallocated context behind");
	}

	@Test
	public void testOpenIO() {
		// rtsp carries AVFMT_NOFILE, so openIO returns before touching avio_open2.
		EndpointMuxer noFile = new EndpointMuxer("rtsp://127.0.0.1:5544/dummy", vertx);
		noFile.setFormat("rtsp");
		assertTrue((noFile.getOutputFormatContext().oformat().flags() & AVFMT_NOFILE) != 0);
		assertTrue(noFile.openIO());

		// avio_open2 hands back what it did not take. Those get named, never fail the endpoint.
		ListAppender<ILoggingEvent> logs = captureEndpointLogs();
		EndpointMuxer file = newFileEndpoint("ignored-options.flv");
		file.options.put("not_a_real_option", "1");

		assertTrue(file.openIO());
		assertTrue(logged(logs, "not consumed by avio_open2"));
		assertTrue(logged(logs, "not_a_real_option"));
		file.clearResource();

		EndpointMuxer noContext = new EndpointMuxer("rtmp://test.antmedia.io/LiveApp/test", vertx);
		noContext.setFormat("testing");
		assertFalse(noContext.openIO());
	}

	@Test
	public void testStatusListenerNotifiesOnChangeOnly() {
		IEndpointStatusListener listener = mock(IEndpointStatusListener.class);
		EndpointMuxer muxer = new EndpointMuxer(URL, vertx);
		muxer.setStatusListener(listener);

		muxer.setStatus("foo");
		muxer.setStatus("foo");
		muxer.setStatus("bar");

		verify(listener, times(1)).endpointStatusUpdated(URL, "foo");
		verify(listener, times(1)).endpointStatusUpdated(URL, "bar");
		assertEquals("bar", muxer.getStatus());
	}

	@Test
	public void testRtmpUrlWithoutAppName() {
		assertRtmpAppOverridden("rtmp://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0");
		assertRtmpAppOverridden("rtmps://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0");

		assertRtmpAppUntouched("rtmp://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0/test");
		assertRtmpAppUntouched("rtmps://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0/test");
		assertRtmpAppUntouched("rtmps://live-api-s.facebook.com:443/rtmp/y8qd-42g5-1b53-fh15-2v0");
	}

	private void assertRtmpAppOverridden(String url) {
		AVDictionary opt = new EndpointMuxer(url, vertx).getOptionDictionary();
		AVDictionaryEntry entry = av_dict_get(opt, "rtmp_app", null, 0);
		assertNotNull(entry, url);
		assertEquals("", entry.value().getString());
		av_dict_free(opt);
	}

	private void assertRtmpAppUntouched(String url) {
		AVDictionary opt = new EndpointMuxer(url, vertx).getOptionDictionary();
		assertNull(av_dict_get(opt, "rtmp_app", null, 0), url);
		av_dict_free(opt);
	}

	@Test
	public void testPrepareIO() {
		// No streams, so the else branch fails it synchronously on this thread.
		EndpointMuxer noStreams = new EndpointMuxer("rtmp://no_server", vertx);
		assertFalse(noStreams.prepareIO(), "nothing to send");
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED, noStreams.getStatus());

		EndpointMuxer reachable = new EndpointMuxer("udp://127.0.0.1:12345?localaddr=127.0.0.1", vertx);
		reachable.init(appScope, "test", 0, null, 0);
		reachable.addStream(h264Parameters(), msTimeBase(), 50);
		reachable.prepareIO();
		Awaitility.await().atMost(25, TimeUnit.SECONDS)
				.until(() -> IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(reachable.getStatus()));
		assertTrue(reachable.getIsRunning().get());
		reachable.writeTrailer();

		// Stubbed rather than dialled: a resolver with a wildcard record turns a real bad host
		// into a TCP connect that rw_timeout does not bound.
		EndpointMuxer unreachable = spy(new EndpointMuxer("rtmp://fakeurl", vertx));
		unreachable.init(appScope, "test", 0, null, 0);
		unreachable.addStream(h264Parameters(), msTimeBase(), 50);
		doReturn(false).when(unreachable).openIO();

		unreachable.prepareIO();
		Awaitility.await().atMost(10, TimeUnit.SECONDS)
				.until(() -> IAntMediaStreamHandler.BROADCAST_STATUS_FAILED.equals(unreachable.getStatus()));
		verify(unreachable).clearResource();
	}

	@Test
	public void testPrepareIOIsIdempotentUntilResourcesAreCleared() throws Exception {
		EndpointMuxer cleared = preparedEndpoint();
		assertFalse(cleared.prepareIO(), "already prepared");

		// Neither teardown wrote a header, so both have to hand preparedIO back.
		cleared.clearResource();
		assertFalse(preparedIO(cleared), "clearResource must allow a restart");

		EndpointMuxer trailered = preparedEndpoint();
		trailered.writeTrailer();
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, trailered.getStatus());
		assertFalse(preparedIO(trailered), "writeTrailer must allow a restart");
	}

	/**
	 * Opens successfully and parks in the filter branch, so preparedIO stays true until teardown.
	 * A failing open would reset it on the worker and hide what these tests are checking.
	 */
	@SuppressWarnings("unchecked")
	private EndpointMuxer preparedEndpoint() throws Exception {
		EndpointMuxer muxer = spy(new EndpointMuxer("rtmp://dummy", vertx));
		muxer.init(appScope, "test", 0, null, 0);
		doReturn(true).when(muxer).openIO();

		java.lang.reflect.Field bsfField = Muxer.class.getDeclaredField("bsfFilterContextList");
		bsfField.setAccessible(true);
		((List<AVBSFContext>) bsfField.get(muxer)).add(new AVBSFContext(null));
		assertTrue(muxer.addStream(h264Parameters(), msTimeBase(), 50));

		assertTrue(muxer.prepareIO());
		Awaitility.await().atMost(5, TimeUnit.SECONDS)
				.until(() -> IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(muxer.getStatus()));
		assertTrue(preparedIO(muxer));
		return muxer;
	}

	private static boolean preparedIO(EndpointMuxer muxer) {
		return ((AtomicBoolean) ReflectionTestUtils.getField(muxer, "preparedIO")).get();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testPrepareIOWithBitstreamFilterCancelledAndNotCancelled() throws Exception {
		AVFormatContext outputFormatContext = new AVFormatContext(null);
		avformat_alloc_output_context2(outputFormatContext, null, "flv", null);
		avformat_new_stream(outputFormatContext, null);

		java.lang.reflect.Field bsfField = Muxer.class.getDeclaredField("bsfFilterContextList");
		bsfField.setAccessible(true);

		EndpointMuxer cancelled = spy(new EndpointMuxer("rtmp://dummy", vertx));
		doReturn(outputFormatContext).when(cancelled).getOutputFormatContext();
		((List<AVBSFContext>) bsfField.get(cancelled)).add(new AVBSFContext(null));
		// prepareIO clears the flag on entry, so teardown has to land while openIO is in flight.
		doAnswer(invocation -> {
			((AtomicBoolean) ReflectionTestUtils.getField(cancelled, "cancelOpenIO")).set(true);
			return true;
		}).when(cancelled).openIO();

		cancelled.prepareIO();
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() ->
				!((AtomicBoolean) ReflectionTestUtils.getField(cancelled, "preparedIO")).get()
						&& !cancelled.getIsRunning().get());
		verify(cancelled).clearResource();

		EndpointMuxer normal = spy(new EndpointMuxer("rtmp://dummy2", vertx));
		doReturn(outputFormatContext).when(normal).getOutputFormatContext();
		doReturn(true).when(normal).openIO();
		((List<AVBSFContext>) bsfField.get(normal)).add(new AVBSFContext(null));

		normal.prepareIO();
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> normal.getIsRunning().get()
				&& IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(normal.getStatus()));

		avformat_free_context(outputFormatContext);
	}

	@Test
	public void testWriteHeaderAfterTrailerIsRefused() {
		EndpointMuxer muxer = newFileEndpoint("write-after-trailer.flv");
		muxer.addStream(h264Parameters(), msTimeBase(), 50);
		assertTrue(muxer.openIO());

		assertTrue(muxer.writeHeader());
		muxer.writeTrailer();

		assertFalse(muxer.writeHeader(), "the trailer is written, the context is done");
		// Refused up front, so the endpoint is never marked broken on the way out.
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, muxer.getStatus());
	}

	@Test
	public void testWriteTrailerWithoutAHeaderDoesNotCrash() {
		// Used to crash: the context is up and isRunning is true, but no header exists yet.
		EndpointMuxer muxer = newFileEndpoint("trailer-first.flv");
		muxer.addStream(h264Parameters(), msTimeBase(), 50);
		assertTrue(muxer.openIO());
		muxer.setIsRunning(new AtomicBoolean(true));

		muxer.writeTrailer();

		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, muxer.getStatus());
		assertFalse(muxer.writeHeader(), "the streams went with the context, nothing left to mux");
	}

	@Test
	public void testWriteHeaderBuildsTheEngine() {
		EndpointMuxer muxer = headerWrittenEndpoint("engine-default.flv", false);
		EndpointMuxerPacingEngine engine = (EndpointMuxerPacingEngine) ReflectionTestUtils.getField(muxer, "engine");

		assertNotNull(engine);
		assertTrue(policyOf(muxer) instanceof EndpointMuxerBacklogPacing, "live edge is opt in");
		assertEquals(1, engine.streamCount());
		assertEquals(VIDEO, engine.videoStreamIndex());

		AVPacket pkt = packet(VIDEO, 1000, false);
		assertEquals(1000, engine.toMs(pkt));
		av_packet_free(pkt);

		// T18: a copy, not a pointer into the context, or a late toMs reads freed memory.
		TimeBase[] timeBases = (TimeBase[]) ReflectionTestUtils.getField(engine, "timeBases");
		AVRational streamTimeBase = muxer.getOutputFormatContext().streams(0).time_base();
		assertEquals(new TimeBase(streamTimeBase.num(), streamTimeBase.den()), timeBases[0]);
		muxer.writeTrailer();

		EndpointMuxer liveEdge = headerWrittenEndpoint("engine-live-edge.flv", true);
		assertTrue(policyOf(liveEdge) instanceof EndpointMuxerLiveEdgePacing);
		liveEdge.writeTrailer();
	}

	@Test
	public void testIsWritableRejectsBackwardAndNegativeDts() throws Exception {
		EndpointMuxer muxer = new EndpointMuxer(URL, vertx);
		ReflectionTestUtils.setField(muxer, "lastWrittenDts", new long[]{AV_NOPTS_VALUE, AV_NOPTS_VALUE});

		assertTrue(isWritable(muxer, av_packet_alloc()), "no dts is not our problem");
		assertTrue(isWritable(muxer, packet(9, 100, false)), "an out of range index is caught upstream");
		assertFalse(isWritable(muxer, packet(VIDEO, -1, false)));

		assertTrue(isWritable(muxer, packet(VIDEO, 100, false)));
		assertFalse(isWritable(muxer, packet(VIDEO, 90, false)), "FFmpeg errors on a backward dts");
		assertTrue(isWritable(muxer, packet(VIDEO, 100, false)), "an equal dts is still placeable");
		assertTrue(isWritable(muxer, packet(AUDIO, 40, false)), "streams advance independently");
	}

	private static boolean isWritable(EndpointMuxer muxer, AVPacket pkt) throws Exception {
		boolean writable = (Boolean) invokeEndpointMethod(muxer, "isWritable", new Class<?>[]{AVPacket.class}, pkt);
		av_packet_free(pkt);
		return writable;
	}

	@Test
	public void testCaptureFirstDtsRebasesBothStreamsFromOneOrigin() throws Exception {
		EndpointMuxer muxer = new EndpointMuxer(URL, vertx);
		assertEquals(0L, captureFirstDts(muxer, packet(VIDEO, 5000, true), AVMEDIA_TYPE_DATA),
				"only audio and video are rebased");

		assertEquals(5000L, captureFirstDts(muxer, packet(VIDEO, 5000, true), AVMEDIA_TYPE_VIDEO));
		assertEquals(5000L, captureFirstDts(muxer, packet(VIDEO, 5040, false), AVMEDIA_TYPE_VIDEO),
				"the origin is captured once");
		assertEquals(5000L, captureFirstDts(muxer, packet(AUDIO, 5020, true), AVMEDIA_TYPE_AUDIO),
				"one shared origin keeps A/V skew");

		// A stream starting behind the origin clamps to itself rather than going negative.
		EndpointMuxer lateVideo = new EndpointMuxer(URL, vertx);
		captureFirstDts(lateVideo, packet(AUDIO, 5000, true), AVMEDIA_TYPE_AUDIO);
		assertEquals(4000L, captureFirstDts(lateVideo, packet(VIDEO, 4000, true), AVMEDIA_TYPE_VIDEO));
	}

	private static long captureFirstDts(EndpointMuxer muxer, AVPacket pkt, int codecType) throws Exception {
		Class<?>[] types = {AVPacket.class, AVRational.class, int.class};
		long firstDts = (Long) invokeEndpointMethod(muxer, "captureFirstDts", types, pkt, msTimeBase(), codecType);
		av_packet_free(pkt);
		return firstDts;
	}

	@Test
	public void testTeardownFreesQueuedPackets() {
		EndpointMuxer muxer = new EndpointMuxer(URL, vertx);
		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(8), VIDEO);
		ReflectionTestUtils.setField(muxer, "engine", engine);
		ReflectionTestUtils.setField(muxer, "running", true);

		for (int i = 0; i < 5; i++) {
			submitAndFree(engine, packet(VIDEO, i * FRAME_MS, false));
		}
		assertEquals(5, engine.size());

		muxer.clearResource();

		assertEquals(0, engine.size());
		assertFalse((Boolean) ReflectionTestUtils.getField(muxer, "running"));
	}

	@Test
	public void testWritePacketRoutesByCodecTypeAndHeaderState() {
		EndpointMuxer muxer = spy(new EndpointMuxer("udp://127.0.0.1:12345?localaddr=127.0.0.1", vertx));
		AVCodecParameters audioParameters = new AVCodecParameters();
		audioParameters.codec_id(AV_CODEC_ID_AAC);
		audioParameters.codec_type(AVMEDIA_TYPE_AUDIO);
		audioParameters.codec_tag(1);

		// No prepareIO: the header must still be unwritten when the packet arrives, and the
		// open runs on a worker that would race us to it.
		muxer.init(appScope, "test", 0, null, 0);
		muxer.addStream(audioParameters, msTimeBase(), 50);

		AVPacket pkt = packet(VIDEO, 0, true);
		muxer.writePacket(pkt, msTimeBase(), msTimeBase(), AVMEDIA_TYPE_AUDIO);
		verify(muxer, times(0)).writeFrameInternal(any(), any(), any(), any(), anyInt());
		av_packet_free(pkt);

		EndpointMuxer video = spy(new EndpointMuxer("udp://127.0.0.1:12346?localaddr=127.0.0.1", vertx));
		video.init(appScope, "test", 0, null, 0);
		video.addStream(h264Parameters(), msTimeBase(), 50);
		video.prepareIO();
		Awaitility.await().atMost(25, TimeUnit.SECONDS)
				.until(() -> IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(video.getStatus()));

		// T23: still inside grace, so the policy throws this one away. The muxer routes it anyway,
		// the drop belongs to the policy and nothing upstream of the queue.
		AVPacket videoPkt = packet(VIDEO, 0, true);
		video.writePacket(videoPkt, msTimeBase(), msTimeBase(), AVMEDIA_TYPE_VIDEO);
		verify(video, times(1)).writeFrameInternal(any(), any(), any(), any(), anyInt());

		skipStartupGrace(video);
		video.setStatus("test");
		video.writePacket(videoPkt, msTimeBase(), msTimeBase(), AVMEDIA_TYPE_VIDEO);
		verify(video, times(2)).writeFrameInternal(any(), any(), any(), any(), anyInt());
		// Only the drain can put the status back, so this proves producer to queue to worker.
		Awaitility.await().atMost(5, TimeUnit.SECONDS)
				.until(() -> IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(video.getStatus()));

		// Same packet routed as audio: the only pin on the audio enqueue path.
		video.setStatus("test");
		video.writePacket(videoPkt, msTimeBase(), msTimeBase(), AVMEDIA_TYPE_AUDIO);
		Awaitility.await().atMost(5, TimeUnit.SECONDS)
				.until(() -> IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(video.getStatus()));
		av_packet_free(videoPkt);

		video.writeTrailer();
	}

	@Test
	public void testWritePacketDropsWhatItCannotPlace() {
		EndpointMuxer noContext = spy(new EndpointMuxer(URL, vertx));
		doReturn(null).when(noContext).getOutputFormatContext();

		AVPacket pkt = packet(VIDEO, 0, true);
		noContext.writePacket(pkt, msTimeBase(), msTimeBase(), AVMEDIA_TYPE_VIDEO);
		verify(noContext, times(0)).writeFrameInternal(any(), any(), any(), any(), anyInt());
		av_packet_free(pkt);

		EndpointMuxer muxer = spy(newFileEndpoint("out-of-range.flv"));
		muxer.addStream(h264Parameters(), msTimeBase(), 50);
		assertTrue(muxer.openIO());
		assertTrue(muxer.writeHeader());

		AVPacket outOfRange = packet(9, 0, true);
		muxer.writePacket(outOfRange, msTimeBase(), msTimeBase(), AVMEDIA_TYPE_VIDEO);
		verify(muxer, times(0)).writeFrameInternal(any(), any(), any(), any(), anyInt());

		av_packet_free(outOfRange);
		muxer.writeTrailer();
	}

	@Test
	public void testDrainRaisesErrorWhenNoPacketCanBePlaced() throws Exception {
		EndpointMuxer muxer = newFileEndpoint("unwritable.flv");
		muxer.addStream(h264Parameters(), msTimeBase(), 50);
		assertTrue(muxer.openIO());

		// Not through writeHeader, so the periodic drain never starts and this test owns the queue.
		EndpointMuxerPacingEngine engine = newEngine(new StubPolicy(400), VIDEO);
		ReflectionTestUtils.setField(muxer, "engine", engine);
		ReflectionTestUtils.setField(muxer, "lastWrittenDts", new long[]{1_000_000, AV_NOPTS_VALUE});
		ReflectionTestUtils.setField(muxer, "running", true);
		muxer.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);

		// A source dts reset makes every packet backward. Nothing reaches the writer, so FFmpeg
		// never errors and the endpoint would go quiet for good without this escalation.
		int limit = (int) ReflectionTestUtils.getField(EndpointMuxer.class, "UNWRITABLE_LIMIT");
		for (int i = 0; i < limit; i++) {
			submitAndFree(engine, packet(VIDEO, i, false));
		}
		invokeEndpointMethod(muxer, "drain", new Class<?>[0]);
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, muxer.getStatus(),
				"a run this long is still just a backward dts, not a broken endpoint");

		submitAndFree(engine, packet(VIDEO, limit, false));
		invokeEndpointMethod(muxer, "drain", new Class<?>[0]);

		assertEquals(0, engine.size());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR, muxer.getStatus());
	}

	/** Writes to a local file, which exercises the full open, header and trailer path with no network. */
	private EndpointMuxer newFileEndpoint(String name) {
		File file = new File("target", name);
		file.delete();
		EndpointMuxer muxer = new EndpointMuxer(file.getAbsolutePath(), vertx);
		muxer.init(appScope, "test", 0, null, 0);
		return muxer;
	}

	private EndpointMuxer headerWrittenEndpoint(String name, boolean liveEdge) {
		EndpointMuxer muxer = spy(newFileEndpoint(name));
		muxer.addStream(h264Parameters(), msTimeBase(), 50);
		assertTrue(muxer.openIO());

		AppSettings settings = new AppSettings();
		settings.setEndpointLiveEdgeEnabled(liveEdge);
		doReturn(settings).when(muxer).getAppSettings();

		assertTrue(muxer.writeHeader());
		return muxer;
	}
}
