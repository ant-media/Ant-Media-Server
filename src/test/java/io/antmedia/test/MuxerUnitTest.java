package io.antmedia.test;

import com.google.gson.JsonObject;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.eRTMP.HEVCDecoderConfigurationParser.HEVCSPSParser;
import io.antmedia.eRTMP.HEVCVideoEnhancedRTMP;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.integration.MuxingTest;
import io.antmedia.muxer.*;
import io.antmedia.muxer.parser.AACConfigParser;
import io.antmedia.muxer.parser.AACConfigParser.AudioObjectTypes;
import io.antmedia.muxer.parser.SPSParser;
import io.antmedia.muxer.parser.codec.AACAudio;
import io.antmedia.plugin.PacketFeeder;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.StreamParametersInfo;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.storage.AmazonS3StorageClient;
import io.antmedia.storage.StorageClient;
import io.antmedia.test.eRTMP.HEVCDecoderConfigurationParserTest;
import io.antmedia.test.utils.VideoInfo;
import io.antmedia.test.utils.VideoProber;
import io.antmedia.websocket.WebSocketConstants;
import io.vertx.core.Vertx;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.buffer.SimpleBufferAllocator;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVBSFContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVChannelLayout;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.avutil.AVDictionaryEntry;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.SizeTPointer;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.red5.codec.AbstractVideo;
import org.red5.codec.IAudioStreamCodec;
import org.red5.codec.IVideoStreamCodec;
import org.red5.codec.StreamCodecInfo;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.io.amf3.Input;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.flv.impl.Tag;
import org.red5.io.object.DataTypes;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.event.CachedEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.event.VideoData.ExVideoPacketType;
import org.red5.server.net.rtmp.event.VideoData.VideoFourCC;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.scope.WebScope;
import org.red5.server.stream.AudioCodecFactory;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.VideoCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.antmedia.muxer.MuxAdaptor.getExtendedSubfolder;
import static io.antmedia.muxer.MuxAdaptor.getSubfolder;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ContextConfiguration(locations = {"test.xml"})
//@ContextConfiguration(classes = {AppConfig.class})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class MuxerUnitTest extends AbstractJUnit4SpringContextTests {


	protected static Logger logger = LoggerFactory.getLogger(MuxerUnitTest.class);
	protected static final int BUFFER_SIZE = 10240;


	byte[] extradata_original = new byte[]{0x00, 0x00, 0x00, 0x01, 0x67, 0x64, 0x00, 0x15, (byte) 0xAC, (byte) 0xB2, 0x03, (byte) 0xC1, 0x7F, (byte) 0xCB, (byte) 0x80,
			(byte) 0x88, 0x00, 0x00, 0x03, 0x00, 0x08, 0x00, 0x00, 0x03, 0x01, (byte) 0x94, 0x78, (byte) 0xB1, 0x72, 0x40, 0x00, 0x00, 0x00, 0x01, 0x68,
			(byte) 0xEB, (byte) 0xC3, (byte) 0xCB, (byte) 0x22, (byte) 0xC0};

	byte[] sps_pps_avc = new byte[]{0x01, 0x64, 0x00, 0x15, (byte) 0xFF,
			(byte) 0xE1, 0x00, 0x1A, 0x67, 0x64, 0x00, 0x15, (byte) 0xAC, (byte) 0xB2, 0x03,
			(byte) 0xC1, 0x7F, (byte) 0xCB, (byte) 0x80, (byte) 0x88, 0x00, 0x00, 0x03, 0x00,
			0x08, 0x00, 0x00, 0x03, 0x01, (byte) 0x94, (byte) 0x78, (byte) 0xB1, 0x72, 0x40, 0x01, 0x00, 0x06, 0x68,
			(byte) 0xEB, (byte) 0xC3, (byte) 0xCB, 0x22, (byte) 0xC0};

	byte[] aacConfig = new byte[]{0x12, 0x10, 0x56, (byte) 0xE5, 0x00};

	protected WebScope appScope;
	private AppSettings appSettings;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	private DataStore datastore;

	@BeforeClass
	public static void beforeClass() {
		//avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
	}

	@Before
	public void before() {
		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}


		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}

	@After
	public void after() {


		try {
			AppFunctionalV2Test.delete(new File("webapps"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		//reset values in the bean
		getAppSettings().resetDefaults();
		getAppSettings().setAddDateTimeToMp4FileName(false);
	}

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName());
		}

		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		}

	};
	private Vertx vertx;

	public class StreamPacket implements IStreamPacket {

		private ITag readTag;
		private IoBuffer data;

		public StreamPacket(ITag tag) {
			readTag = tag;
			data = readTag.getBody();
		}

		@Override
		public int getTimestamp() {
			return readTag.getTimestamp();
		}

		@Override
		public byte getDataType() {
			return readTag.getDataType();
		}

		@Override
		public IoBuffer getData() {
			return data;
		}

		public void setData(IoBuffer data) {
			this.data = data;
		}
	}


	@Test
	public void testConvertAvcExtraDataToAnnexB() {
		byte[] sps_pps_avc = new byte[]{0x01, 0x64, 0x00, 0x15, (byte) 0xFF,
				(byte) 0xE1, 0x00, 0x1A, 0x67, 0x64, 0x00, 0x15, (byte) 0xAC, (byte) 0xB2, 0x03,
				(byte) 0xC1, 0x7F, (byte) 0xCB, (byte) 0x80, (byte) 0x88, 0x00, 0x00, 0x03, 0x00,
				0x08, 0x00, 0x00, 0x03, 0x01, (byte) 0x94, (byte) 0x78, (byte) 0xB1, 0x72, 0x40, 0x01, 0x00, 0x06, 0x68,
				(byte) 0xEB, (byte) 0xC3, (byte) 0xCB, 0x22, (byte) 0xC0};

		assertEquals(43, sps_pps_avc.length);

		byte[] extradata_annexb = MuxAdaptor.getAnnexbExtradata(sps_pps_avc);


		assertEquals(extradata_annexb.length, extradata_original.length);

		for (int i = 0; i < extradata_original.length; i++) {
			assertEquals(extradata_annexb[i], extradata_original[i]);
		}

		SPSParser spsParser = new SPSParser(extradata_annexb, 5);

		assertEquals(480, spsParser.getWidth());
		assertEquals(360, spsParser.getHeight());

	}

	@Test
	public void testHEVCSPSParser() {
		byte[] sps = new byte[] {66, 1, 1, 1, 96, 0, 0, 3, 0, -112, 0, 0, 3, 0, 0, 3, 0, 123, -96, 3, -64, -128, 16,
				-27, -106, 74, -110, 76, -82, 106, 2, 2, 3, -62, 0, 0, 3, 0, 2, 0, 0, 3, 0, 120, 16};

		//2 bytes because first 2 bytes are NAL HEADER
		HEVCSPSParser parser = new HEVCSPSParser(sps, 2);

		assertEquals(1920, parser.getWidth());
		assertEquals(1080, parser.getHeight());

	}

	@Test
	public void testAddAudioStream() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(Mockito.mock(StorageClient.class), vertx, "");
		appScope = (WebScope) applicationContext.getBean("web.scope");

		mp4Muxer.init(appScope, "test", 0, "", 0);
		assertEquals(0, mp4Muxer.getOutputFormatContext().nb_streams());
		AVChannelLayout layout = new AVChannelLayout();
		av_channel_layout_default(layout, 1);
		assertTrue(mp4Muxer.addAudioStream(44100, layout, AV_CODEC_ID_AAC, 0));

		assertEquals(1, mp4Muxer.getOutputFormatContext().nb_streams());

	}


	@Test
	public void testAddExtradata() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(Mockito.mock(StorageClient.class), vertx, "");
		appScope = (WebScope) applicationContext.getBean("web.scope");
		mp4Muxer.init(appScope, "test", 0, "", 0);

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);

		mp4Muxer.contextChanged(codecContext, 0);

		codecContext.extradata_size(10);
		codecContext.extradata(new BytePointer(10));

		mp4Muxer.contextChanged(codecContext, 0);

		AVPacket pkt = new AVPacket();

		mp4Muxer.addExtradataIfRequired(pkt, true);

		assertEquals(10, mp4Muxer.getTmpPacket().size());

		pkt.data(new BytePointer(15)).size(15);
		mp4Muxer.addExtradataIfRequired(pkt, false);
		assertEquals(10, mp4Muxer.getTmpPacket().size());

		mp4Muxer.addExtradataIfRequired(pkt, true);
		assertEquals(25, mp4Muxer.getTmpPacket().size());
	}

	@Test
	public void testInitVideoBitstreamFilter() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(Mockito.mock(StorageClient.class), vertx, "");
		appScope = (WebScope) applicationContext.getBean("web.scope");
		mp4Muxer.init(appScope, "test", 0, "", 0);
		mp4Muxer.getOutputFormatContext();

		mp4Muxer.setBitstreamFilter("h264_mp4toannexb");
		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		AVBSFContext avbsfContext = mp4Muxer.initVideoBitstreamFilter("h264_mp4toannexb", codecParameters, Muxer.avRationalTimeBase);
		assertNotNull(avbsfContext);


	}

	@Test
	public void testAddStream() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(Mockito.mock(StorageClient.class), vertx, "");
		appScope = (WebScope) applicationContext.getBean("web.scope");

		mp4Muxer.clearResource();

		mp4Muxer.init(appScope, "test", 0, "", 0);
		//increase coverage
		mp4Muxer.init(appScope, "test", 0, "", 0);

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);

		assertEquals(0, mp4Muxer.getOutputFormatContext().nb_streams());

		boolean addStream = mp4Muxer.addStream(null, codecContext, 0);
		assertTrue(addStream);

		assertEquals(1, mp4Muxer.getOutputFormatContext().nb_streams());

		mp4Muxer.getIsRunning().set(true);
		addStream = mp4Muxer.addStream(null, codecContext, 0);
		assertFalse(addStream);

		//increase coverage
		mp4Muxer.getIsRunning().set(false);
		mp4Muxer.writePacket(new AVPacket(), codecContext);

		//increase coverage
		mp4Muxer.writeVideoBuffer(null, 0, 0, 0, false, 0, 0);
		mp4Muxer.writeAudioBuffer(null, 0, 0);

	}

	@Test
	public void testContextChanged() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(Mockito.mock(StorageClient.class), vertx, "");
		appScope = (WebScope) applicationContext.getBean("web.scope");

		mp4Muxer.init(appScope, "test", 0, "", 0);
		assertEquals(0, mp4Muxer.getOutputFormatContext().nb_streams());

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);
		assertEquals(0, mp4Muxer.getInputTimeBaseMap().size());
		mp4Muxer.contextWillChange(new AVCodecContext(), 0);
		mp4Muxer.contextChanged(codecContext, 0);
		assertEquals(1, mp4Muxer.getInputTimeBaseMap().size());

		assertEquals(640, mp4Muxer.getVideoWidth());
		assertEquals(480, mp4Muxer.getVideoHeight());

		codecContext.extradata_size(10);
		codecContext.extradata(new BytePointer(10));
		mp4Muxer.contextWillChange(new AVCodecContext(), 0);
		mp4Muxer.contextChanged(codecContext, 1);

		assertEquals(2, mp4Muxer.getInputTimeBaseMap().size());

		codecContext = new AVCodecContext();
		codecContext.codec_type(AVMEDIA_TYPE_AUDIO);
		mp4Muxer.contextWillChange(new AVCodecContext(), 0);
		mp4Muxer.contextChanged(codecContext, 3);

		assertEquals(3, mp4Muxer.getInputTimeBaseMap().size());

	}

	@Test
	public void testErrorDefinition() {
		String errorDefinition = Muxer.getErrorDefinition(-1);
		assertNotNull(errorDefinition);
	}

	@Test
	public void testParseAACConfig() {
		AACConfigParser aacParser = new AACConfigParser(aacConfig, 0);
		assertEquals(44100, aacParser.getSampleRate());
		assertEquals(2, aacParser.getChannelCount());
		assertEquals(AACConfigParser.AudioObjectTypes.AAC_LC, aacParser.getObjectType());
		assertEquals(1024, aacParser.getFrameSize());
		;
		assertFalse(aacParser.isErrorOccured());

		aacParser = new AACConfigParser(new byte[]{0, 0}, 0);
		assertTrue(aacParser.isErrorOccured());


		aacParser = new AACConfigParser(new byte[]{(byte) 0x80, 0}, 0);
		assertTrue(aacParser.isErrorOccured());

		aacParser = new AACConfigParser(new byte[]{(byte) 0x17, 0}, 0);
		assertTrue(aacParser.isErrorOccured());

		aacParser = new AACConfigParser(new byte[]{(byte) 0x12, (byte) 0x77}, 0);
		assertTrue(aacParser.isErrorOccured());

		aacParser = new AACConfigParser(new byte[]{(byte) 0x12, (byte) 0x17}, 0);
		assertFalse(aacParser.isErrorOccured());

		aacParser = new AACConfigParser(new byte[]{(byte) 0x12, (byte) 0x38}, 0);
		assertFalse(aacParser.isErrorOccured());

	}

	@Test
	public void testAACAudio() {
		AACAudio aacAudio = new AACAudio();

		assertEquals("AAC", aacAudio.getName());

		IoBuffer result = IoBuffer.allocate(4);
		result.setAutoExpand(true);
		result.put(aacConfig);
		result.rewind();

		assertFalse(aacAudio.canHandleData(result));
		result.limit(0);

		assertFalse(aacAudio.canHandleData(result));
		assertTrue(aacAudio.addData(result));
		assertNull(aacAudio.getDecoderConfiguration());
	}


	@Test
	public void testInitBitstreamFilter() {
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		//AVInputFormat findInputFormat = avformat.av_find_input_format("mp4");
		if (avformat_open_input(inputFormatContext, (String) "src/test/resources/test_hevc.ts", null,
				(AVDictionary) null) < 0) {

			logger.error("cannot open input format");
			fail("cannot open input format");
		}

		int ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			fail("cannot find stream info");
		}




		HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 7, null, false);
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}


		String streamId = "stream_name_" + (int) (Math.random() * 10000);
		hlsMuxer.setHlsParameters("5", "2", "event", null, null, "fmp4");

		//init
		hlsMuxer.init(appScope, streamId, 0, null, 0);


		AVBSFContext initVideoBitstreamFilter = hlsMuxer.initVideoBitstreamFilter("h264_mp4toannexb",  inputFormatContext.streams(0).codecpar(),  inputFormatContext.streams(0).time_base());
		assertNull(initVideoBitstreamFilter);


		initVideoBitstreamFilter = hlsMuxer.initVideoBitstreamFilter("not exists",  inputFormatContext.streams(0).codecpar(),  inputFormatContext.streams(0).time_base());
		assertNull(initVideoBitstreamFilter);

		avformat_close_input(inputFormatContext);
	}

	@Test
	public void testHEVCHLSMuxingInFMP4() {

		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		//AVInputFormat findInputFormat = avformat.av_find_input_format("mp4");
		if (avformat_open_input(inputFormatContext, (String) "src/test/resources/test_hevc.ts", null,
				(AVDictionary) null) < 0) {

			logger.error("cannot open input format");
			fail("cannot open input format");
		}

		int ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			fail("cannot find stream info");
		}

		vertx = Vertx.vertx();
		HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 7, null, false);

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}


		String streamId = "stream_name_" + (int) (Math.random() * 10000);
		hlsMuxer.setHlsParameters("5", "2", "event", null, null, "fmp4");

		//init
		hlsMuxer.init(appScope, streamId, 0, null, 0);

		//add video stream
		assertEquals(AVMEDIA_TYPE_VIDEO, inputFormatContext.streams(0).codecpar().codec_type());
		assertEquals(AV_CODEC_ID_HEVC, inputFormatContext.streams(0).codecpar().codec_id());
		boolean addStreamResult = hlsMuxer.addStream(inputFormatContext.streams(0).codecpar(), inputFormatContext.streams(0).time_base(), 0);
		assertTrue(addStreamResult);

		assertEquals("hevc_mp4toannexb", hlsMuxer.getBitStreamFilter());


		assertEquals(AVMEDIA_TYPE_AUDIO, inputFormatContext.streams(1).codecpar().codec_type());
		assertEquals(AV_CODEC_ID_AAC, inputFormatContext.streams(1).codecpar().codec_id());
		addStreamResult = hlsMuxer.addStream(inputFormatContext.streams(1).codecpar(), inputFormatContext.streams(1).time_base(), 1);
		assertTrue(addStreamResult);

		assertTrue(hlsMuxer.getBsfAudioNames().contains("aac_adtstoasc"));
		assertEquals(1, hlsMuxer.getBsfAudioNames().size());

		//prepare io
		boolean prepareIOresult = hlsMuxer.prepareIO();
		assertTrue(prepareIOresult);


		AVPacket pkt = new AVPacket();

		while (av_read_frame(inputFormatContext, pkt) >= 0) {
			hlsMuxer.writePacket(pkt, inputFormatContext.streams(pkt.stream_index()));
			av_packet_unref(pkt);
		}

		hlsMuxer.writeTrailer();

		//check the init file and m4s files there
		assertTrue(hlsMuxer.getFile().exists());
		String[] filesInStreams = hlsMuxer.getFile().getParentFile().list();
		boolean initFileFound = false;
		String regex = streamId + "_" + System.currentTimeMillis()/1000000 + "\\d{6}_init.mp4";
		System.out.println("regex:"+regex);

		for (int i = 0; i < filesInStreams.length; i++) {
			System.out.println("files:"+filesInStreams[i]);
			initFileFound |= filesInStreams[i].matches(regex);
		}
		assertTrue(initFileFound);
		assertTrue(MuxingTest.testFile(hlsMuxer.getFile().getAbsolutePath(), 107000));

		assertEquals(0, hlsMuxer.getAudioNotWrittenCount());
		assertEquals(0, hlsMuxer.getVideoNotWrittenCount());

		avformat_close_input(inputFormatContext);

		//wait and check the files are deleted

		Awaitility.await().atMost(5 * 2 * 1000 + 3000, TimeUnit.MILLISECONDS).pollInterval(1, TimeUnit.SECONDS)
		.until(() -> {
			File[] filesTmp = hlsMuxer.getFile().getParentFile().listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".fmp4") || name.endsWith(".m3u8");
				}
			});
			return 0 == filesTmp.length;
		});

	}


	@Test
	public void testFFmpegReadPacket() {
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		AVInputFormat findInputFormat = avformat.av_find_input_format("flv");
		if (avformat_open_input(inputFormatContext, (String) "src/test/resources/test.flv", findInputFormat,
				(AVDictionary) null) < 0) {
			//	return false;
		}

		long startFindStreamInfoTime = System.currentTimeMillis();

		int ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
		}

		AVCodecParameters codecpar = inputFormatContext.streams(1).codecpar();

		byte[] data2 = new byte[codecpar.extradata_size()];


		codecpar.extradata().position(0).get(data2);

		AVPacket pkt = new AVPacket();


		logger.info("codecpar.bit_rate(): {}\n" +
				"		codecpar.bits_per_coded_sample(): {} \n" +
				"		codecpar.bits_per_raw_sample(): {} \n" +
				"		codecpar.block_align(): {}\n" +
				"		codecpar.channel_layout(): {}\n" +
				"		codecpar.channels(): {}\n" +
				"		codecpar.codec_id(): {}\n" +
				"		codecpar.codec_tag(): {}\n" +
				"		codecpar.codec_type(): {} \n" +
				"		codecpar.format(): {}\n" +
				"		codecpar.frame_size():{} \n" +
				"		codecpar.level():{} \n" +
				"		codecpar.profile():{} \n" +
				"		codecpar.sample_rate(): {}",

				codecpar.bit_rate(),
				codecpar.bits_per_coded_sample(),
				codecpar.bits_per_raw_sample(),
				codecpar.block_align(),
				codecpar.ch_layout(),
				codecpar.ch_layout().nb_channels(),
				codecpar.codec_id(),
				codecpar.codec_tag(),
				codecpar.codec_type(),
				codecpar.format(),
				codecpar.frame_size(),
				codecpar.level(),
				codecpar.profile(),
				codecpar.sample_rate());


		int i = 0;
		try {

			while ((ret = av_read_frame(inputFormatContext, pkt)) >= 0) {

				if (inputFormatContext.streams(pkt.stream_index()).codecpar().codec_type() == AVMEDIA_TYPE_AUDIO) {
					pkt.data().position(0).limit(pkt.size());


					logger.info("		pkt.duration():{} \n" +
							"					pkt.flags(): {} \n" +
							"					pkt.pos(): {}\n" +
							"					pkt.size(): {}\n" +
							"					pkt.stream_index():{} ",
							pkt.duration(),
							pkt.flags(),
							pkt.pos(),
							pkt.size(),
							pkt.stream_index());


					byte[] data = new byte[(int) pkt.size()];

					pkt.data().get(data);

					FileOutputStream fos = new FileOutputStream("audio_ffmpeg" + i);

					fos.write(data);

					fos.close();

					i++;
					if (i == 5) {

						break;
					}

				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		avformat_close_input(inputFormatContext);

	}


	@Test
	public void testAudioTag() {

		try {
			File file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			int i = 0;
			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);


				if (streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) {
					int bodySize = streamPacket.getData().limit();

					byte[] bodyBuf = new byte[bodySize - 2];
					// put the bytes into the array
					//streamPacket.getData().position(5);
					streamPacket.getData().position(2);
					streamPacket.getData().get(bodyBuf);
					// get the audio or video codec identifier
					streamPacket.getData().position(0);

					FileOutputStream fos = new FileOutputStream("audio_tag" + i);
					fos.write(bodyBuf);


					fos.close();
					i++;
					if (i == 5) {
						break;
					}


				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	@Test
	public void testIsCodecSupported() {
		appScope = (WebScope) applicationContext.getBean("web.scope");

		Mp4Muxer mp4Muxer = new Mp4Muxer(null, null, "streams");
		mp4Muxer.init(appScope, "test", 0, null, 0);


		WebMMuxer webMMuxer = new WebMMuxer(null, null, "streams");
		webMMuxer.init(appScope, "test", 0, null, 0);


		assertFalse(webMMuxer.isCodecSupported(AV_CODEC_ID_H264));
		assertTrue(mp4Muxer.isCodecSupported(AV_CODEC_ID_H264));

		assertFalse(mp4Muxer.isCodecSupported(AV_CODEC_ID_VP8));
		assertTrue(webMMuxer.isCodecSupported(AV_CODEC_ID_VP8));


	}

	@Test
	public void testStreamIndex() {
		Mp4Muxer mp4Muxer = new Mp4Muxer(null, vertx, "streams");

		appScope = (WebScope) applicationContext.getBean("web.scope");
		mp4Muxer.init(appScope, "test", 0, null, 0);

		SPSParser spsParser = new SPSParser(extradata_original, 5);

		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.width(spsParser.getWidth());
		codecParameters.height(spsParser.getHeight());
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		codecParameters.extradata_size(sps_pps_avc.length);
		BytePointer extraDataPointer = new BytePointer(sps_pps_avc);
		codecParameters.extradata(extraDataPointer);
		codecParameters.format(AV_PIX_FMT_YUV420P);
		codecParameters.codec_tag(0);

		AVRational rat = new AVRational().num(1).den(1000);
		//mp4Muxer.addVideoStream(spsParser.getWidth(), spsParser.getHeight(), rat, AV_CODEC_ID_H264, 0, true, codecParameters);

		mp4Muxer.addStream(codecParameters, rat, 5);
		mp4Muxer.setPreviewPath("/path");

		assertTrue(mp4Muxer.getRegisteredStreamIndexList().contains(5));


		HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 0, "http://example.com", false);
		hlsMuxer.setHlsParameters(null, null, null, null, null, null);
		hlsMuxer.init(appScope, "test", 0, null, 0);
		hlsMuxer.addStream(codecParameters, rat, 50);
		assertTrue(hlsMuxer.getRegisteredStreamIndexList().contains(50));
		hlsMuxer.writeTrailer();


		RtmpMuxer rtmpMuxer = new RtmpMuxer("any_url", vertx);
		rtmpMuxer.init(appScope, "test", 0, null, 0);
		rtmpMuxer.addStream(codecParameters, rat, 50);

	}

	@Test
	public void testRecordMuxerS3Prefix() {
		String s3Prefix = RecordMuxer.getS3Prefix("s3", null);
		assertEquals("s3/", s3Prefix);

		s3Prefix = RecordMuxer.getS3Prefix("s3", "test");
		assertEquals("s3/test/", s3Prefix);

		s3Prefix = RecordMuxer.getS3Prefix("s3/", "test/");
		assertEquals("s3/test/", s3Prefix);

		s3Prefix = RecordMuxer.getS3Prefix("s3", "");
		assertEquals("s3/", s3Prefix);
	}

	@Test
	public void testHLSMuxerGetOutputURLAndSegmentFilename() throws IOException {

		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);


		{
			HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 0, "http://example.com", false);
			hlsMuxer.setIsRunning(new AtomicBoolean(true));
			String streamId = "streamId";
			String subFolder = "subfolder";

			hlsMuxer.init(appScope, streamId, 0, subFolder, 0);


			assertEquals("http://example.com/" + subFolder + "/" + streamId + ".m3u8", hlsMuxer.getOutputURL());
			assertEquals("http://example.com/" + subFolder + "/" + streamId + "%09d.ts", hlsMuxer.getSegmentFilename());

		}

		{
			//add trailer slash
			HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 0, "http://example.com/", false);
			hlsMuxer.setIsRunning(new AtomicBoolean(true));
			String streamId = "streamId";
			String subFolder = "subfolder/";

			hlsMuxer.init(appScope, streamId, 0, subFolder, 0);


			assertEquals("http://example.com/" + subFolder + streamId + ".m3u8", hlsMuxer.getOutputURL());
			assertEquals("http://example.com/" + subFolder + streamId + "%09d.ts", hlsMuxer.getSegmentFilename());

		}

		{
			StorageClient storageClient = Mockito.mock(StorageClient.class);
			Mockito.when(storageClient.isEnabled()).thenReturn(true);


			HLSMuxer hlsMuxer = Mockito.spy(new HLSMuxer(vertx, storageClient, "streams/", 0b010, null, false));
			hlsMuxer.setIsRunning(new AtomicBoolean(true));
			String streamId = "streamId";
			String subFolder = "subfolder/";

			hlsMuxer.setHlsParameters("1", "1", null, null, null, null);

			File[] file = new File[1];
			file[0] = new File("./webapps/junit/streams/subfolder/streamId.m3u8");

			file[0].getParentFile().mkdirs(); // Ensure the directory exists
			file[0].createNewFile(); // Create the file
			file[0].deleteOnExit();
	    

			 // Code under test that uses the stubbed method
			   
			
			hlsMuxer.init(appScope, streamId, 0, subFolder, 0);

			hlsMuxer.writeTrailer();

			Mockito.verify(storageClient, Mockito.timeout(2000)).save("streams/subfolder/" + streamId + ".m3u8", file[0], true);

			

		}

		{
			StorageClient storageClient = Mockito.mock(StorageClient.class);
			Mockito.when(storageClient.isEnabled()).thenReturn(true);


			HLSMuxer hlsMuxer = Mockito.spy(new HLSMuxer(vertx, storageClient, "streams", 0b010, null, false));
			hlsMuxer.setIsRunning(new AtomicBoolean(true));
			String streamId = "streamId";
			String subFolder = "subfolder";

			hlsMuxer.setHlsParameters("1", "1", null, null, null, null);
			File[] file = new File[1];
			file[0] = new File("./webapps/junit/streams/subfolder/streamId.m3u8");

			file[0].getParentFile().mkdirs(); // Ensure the directory exists
			file[0].createNewFile(); // Create the file
			file[0].deleteOnExit();
	    
			


			hlsMuxer.init(appScope, streamId, 0, subFolder, 0);


			hlsMuxer.writeTrailer();

			Mockito.verify(storageClient, Mockito.timeout(2000)).save("streams/subfolder/" + streamId + ".m3u8", file[0], true);

		}

		{
			StorageClient storageClient = Mockito.mock(StorageClient.class);
			Mockito.when(storageClient.isEnabled()).thenReturn(true);


			HLSMuxer hlsMuxer = Mockito.spy(new HLSMuxer(vertx, storageClient, "streams", 0b010, null, false));
			hlsMuxer.setIsRunning(new AtomicBoolean(true));
			String streamId = "streamId";
			hlsMuxer.setHlsParameters("1", "1", null, null, null, null);

			File[] file = new File[1];
			file[0] = new File("./webapps/junit/streams/streamId.m3u8");

			file[0].getParentFile().mkdirs(); // Ensure the directory exists
			file[0].createNewFile(); // Create the file
			file[0].deleteOnExit();
	    
			hlsMuxer.init(appScope, streamId, 0, null, 0);

			hlsMuxer.writeTrailer();

			Mockito.verify(storageClient, Mockito.timeout(2000)).save("streams/" + streamId + ".m3u8", file[0], true);

		}

	}

	@Test
	public void testGetAudioCodecParameters() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertEquals(1, appScope.getDepth());

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(Mockito.mock(ClientBroadcastStream.class), null, false, appScope));
		muxAdaptor.setAudioDataConf(new byte[]{0, 0});

		assertNull(muxAdaptor.getAudioCodecParameters());

		try {
			muxAdaptor.setEnableAudio(true);
			assertTrue(muxAdaptor.isEnableAudio());

			muxAdaptor.prepare();

			assertFalse(muxAdaptor.isEnableAudio());
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testStopRtmpStreamingWhenRtmpMuxerNull() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertTrue(appScope.getDepth() == 1);

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));
		String rtmpUrl = "rtmp://test.com/live/stream";
		Integer resolution = 0;

		ConcurrentHashMap<String, String> statusMap = Mockito.mock(ConcurrentHashMap.class);
		ReflectionTestUtils.setField(muxAdaptor, "statusMap", statusMap);
		Mockito.doReturn(null).when(muxAdaptor).getRtmpMuxer(rtmpUrl);

		Mockito.doReturn(null).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopRtmpStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(IAntMediaStreamHandler.BROADCAST_STATUS_ERROR).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopRtmpStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopRtmpStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopRtmpStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING).when(statusMap).getOrDefault(rtmpUrl, null);
		assertFalse(muxAdaptor.stopRtmpStreaming(rtmpUrl, resolution).isSuccess());
	}

	@Test
	public void testMuxerStartStopRTMPStreaming() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertTrue(appScope.getDepth() == 1);

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));

		muxAdaptor.setIsRecording(true);
		Mockito.doReturn(true).when(muxAdaptor).prepareMuxer(Mockito.any(), anyInt());

		String rtmpUrl = "rtmp://localhost";
		int resolutionHeight = 480;
		Result result = muxAdaptor.startRtmpStreaming(rtmpUrl, resolutionHeight);
		assertFalse(result.isSuccess());

		muxAdaptor.setHeight(480);
		result = muxAdaptor.startRtmpStreaming(rtmpUrl, resolutionHeight);
		assertTrue(result.isSuccess());


		result = muxAdaptor.startRtmpStreaming(rtmpUrl, 0);
		assertTrue(result.isSuccess());


		RtmpMuxer rtmpMuxer = Mockito.mock(RtmpMuxer.class);
		Mockito.doReturn(rtmpMuxer).when(muxAdaptor).getRtmpMuxer(Mockito.any());
		muxAdaptor.stopRtmpStreaming(rtmpUrl, resolutionHeight);
		Mockito.verify(rtmpMuxer).writeTrailer();


		muxAdaptor.stopRtmpStreaming(rtmpUrl, 0);
		Mockito.verify(rtmpMuxer, Mockito.times(2)).writeTrailer();


		muxAdaptor.stopRtmpStreaming(rtmpUrl, 360);
		//it should be 2 times again because 360 and 480 don't match
		Mockito.verify(rtmpMuxer, Mockito.times(2)).writeTrailer();

	}

	@Test
	public void testMuxerEndpointStatusUpdate() {

		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertTrue(appScope.getDepth() == 1);

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId("test");
		} catch (Exception e) {
			e.printStackTrace();
		}

		muxAdaptor.setBroadcast(broadcast);
		Endpoint rtmpEndpoint = new Endpoint();
		String rtmpUrl = "rtmp://localhost/LiveApp/test12";
		rtmpEndpoint.setRtmpUrl(rtmpUrl);
		List<Endpoint> endpointList = new ArrayList<>();
		endpointList.add(rtmpEndpoint);

		broadcast.setEndPointList(endpointList);
		boolean result = muxAdaptor.init(appScope, "test", false);
		muxAdaptor.getDataStore().save(broadcast);

		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);


		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});


		muxAdaptor.getDataStore().delete(broadcast.getStreamId());

		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		assertEquals(1, muxAdaptor.getEndpointStatusUpdateMap().size());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return 0 == muxAdaptor.getEndpointStatusUpdateMap().size();
		});

	}

	@Test
	public void testBroadcastHasBeenDeleted() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertTrue(appScope.getDepth() == 1);

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));

		String rtmpUrl = "rtmp://localhost/LiveApp/test12";
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId("test");
		} catch (Exception e) {
			e.printStackTrace();
		}

		getAppSettings().setEndpointRepublishLimit(1);
		getAppSettings().setEndpointHealthCheckPeriodMs(2000);
		muxAdaptor.setBroadcast(broadcast);

		boolean result = muxAdaptor.init(appScope, "test", false);

		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);

		muxAdaptor.setBroadcast(null);
		Mockito.verify(muxAdaptor, timeout(3000)).clearCounterMapsAndCancelTimer(Mockito.anyString(), Mockito.anyLong());


	}

	@Test
	public void testRTMPCodecSupport() {
		RtmpMuxer rtmpMuxer = new RtmpMuxer(null, vertx);

		assertTrue(rtmpMuxer.isCodecSupported(AV_CODEC_ID_H264));
		assertTrue(rtmpMuxer.isCodecSupported(AV_CODEC_ID_AAC));

		assertFalse(rtmpMuxer.isCodecSupported(AV_CODEC_ID_AC3));

	}


	@Test
	public void testHLSAddStream() {
		HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "", 7, null, false);
		appScope = (WebScope) applicationContext.getBean("web.scope");
		hlsMuxer.init(appScope, "test", 0, "", 100);

		assertFalse(hlsMuxer.writeHeader());

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);
		codecContext.codec_id(AV_CODEC_ID_H264);

		boolean addStream = hlsMuxer.addStream(null, codecContext, 0);
		assertTrue(addStream);

		assertNull(hlsMuxer.getBitStreamFilter());
	}



	@Test
	public void testHLSID3TagEnabled() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);


		HLSMuxer hlsMuxer = spy(new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "", 7, null, false));
		hlsMuxer.init(appScope, "test", 0, "", 100);

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);
		codecContext.codec_id(AV_CODEC_ID_H264);

		boolean addStream = hlsMuxer.addStream(null, codecContext, 0);

		hlsMuxer.setId3Enabled(false);

		hlsMuxer.writeMetaData("hello", 0);

		//it should not write id3 tag
		Mockito.verify(hlsMuxer, never()).writeDataFrame(any(), any());

		hlsMuxer.writeTrailer();

	}

	@Test
	public void testAVWriteFrame() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		RtmpMuxer rtmpMuxer = Mockito.spy(new RtmpMuxer(null, vertx));

		AVFormatContext context = new AVFormatContext();
		int ret = avformat_alloc_output_context2(context, null, "flv", "test.flv");

		//rtmpMuxer.set
		AVPacket pkt = av_packet_alloc();

		appScope = (WebScope) applicationContext.getBean("web.scope");

		rtmpMuxer.init(appScope, "", 0, "", 0);

		rtmpMuxer.avWriteFrame(pkt, context);

		Mockito.verify(rtmpMuxer).addExtradataIfRequired(pkt, false);

		av_packet_free(pkt);
		avformat_free_context(context);
	}

	@Test
	public void testRTMPAddStream() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		RtmpMuxer rtmpMuxer = new RtmpMuxer(null, vertx);

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);


		boolean addStream = rtmpMuxer.addStream(null, codecContext, 0);
		assertFalse(addStream);


		codecContext.codec_id(AV_CODEC_ID_H264);
		addStream = rtmpMuxer.addStream(null, codecContext, BUFFER_SIZE);
		assertTrue(addStream);


		addStream = rtmpMuxer.addVideoStream(480, 360, Muxer.avRationalTimeBase, AV_CODEC_ID_H264, 0, true, null);
		assertTrue(addStream);

	}

	@Test
	public void testRTMPPrepareIO() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		RtmpMuxer rtmpMuxer = new RtmpMuxer("rtmp://no_server", vertx);

		//it should return false because there is no thing to send.
		assertFalse(rtmpMuxer.prepareIO());

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return rtmpMuxer.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
		});

		assertFalse(rtmpMuxer.prepareIO());

	}

	@Test
	public void testRTMPMuxerRaceCondition() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		RtmpMuxer rtmpMuxer = new RtmpMuxer("rtmp://no_server", vertx);
		rtmpMuxer.init(appScope, "test", 0, null, 0);

		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		AVRational rat = new AVRational().num(1).den(1000);
		assertTrue(rtmpMuxer.addStream(codecParameters, rat, 50));

		// 1. Test preparedIO reset in clearResource
		assertTrue(rtmpMuxer.prepareIO());
		assertFalse(rtmpMuxer.prepareIO()); // already prepared

		rtmpMuxer.clearResource(); // headerWritten is false, resets preparedIO
		
		// Re-add stream because clearResource cleared outputFormatContext
		assertTrue(rtmpMuxer.addStream(codecParameters, rat, 50));
		assertTrue(rtmpMuxer.prepareIO()); // can prepare again

		// 2. Test cancelOpenIO race protection
		RtmpMuxer rtmpMuxer2 = new RtmpMuxer("rtmp://no_server", vertx);
		rtmpMuxer2.init(appScope, "test", 0, null, 0);
		assertTrue(rtmpMuxer2.addStream(codecParameters, rat, 50));

		// Start prepareIO which will call openIO in blocking thread
		rtmpMuxer2.prepareIO();
		// Immediately cancel it
		rtmpMuxer2.writeTrailer();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			String status = rtmpMuxer2.getStatus();
			return status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED) || status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
		});

		// isRunning should remain false because it was cancelled
		assertFalse(rtmpMuxer2.getIsRunning().get());
	}

	@Test
	public void testWriteTrailerBeforeHeader() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		RtmpMuxer rtmpMuxer = new RtmpMuxer("rtmp://no_server", vertx);
		rtmpMuxer.init(appScope, "test", 0, null, 0);

		// No header written yet
		rtmpMuxer.writeTrailer();

		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED, rtmpMuxer.getStatus());

		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		AVRational rat = new AVRational().num(1).den(1000);
		
		// It should be able to add stream and prepareIO again because clearResource was called in writeTrailer
		assertTrue(rtmpMuxer.addStream(codecParameters, rat, 50));
		assertTrue(rtmpMuxer.prepareIO());
	}


	@Test
	public void testRTMPHealthCheckProcess() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertTrue(appScope.getDepth() == 1);

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId("test");
		} catch (Exception e) {
			e.printStackTrace();
		}

		getAppSettings().setEndpointRepublishLimit(1);
		getAppSettings().setEndpointHealthCheckPeriodMs(2000);

		muxAdaptor.setBroadcast(broadcast);
		Endpoint rtmpEndpoint = new Endpoint();
		String rtmpUrl = "rtmp://localhost/LiveApp/test12";
		rtmpEndpoint.setRtmpUrl(rtmpUrl);
		List<Endpoint> endpointList = new ArrayList<>();
		endpointList.add(rtmpEndpoint);

		broadcast.setEndPointList(endpointList);
		boolean result = muxAdaptor.init(appScope, "test", false);
		muxAdaptor.getDataStore().save(broadcast);

		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);

		assertTrue(muxAdaptor.getIsHealthCheckStartedMap().get(rtmpUrl));

		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return muxAdaptor.getIsHealthCheckStartedMap().getOrDefault(rtmpUrl, false) == false;
		});

		//ERROR SCENARIO
		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return IAntMediaStreamHandler.BROADCAST_STATUS_ERROR.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});

		assertTrue(muxAdaptor.getIsHealthCheckStartedMap().get(rtmpUrl));
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return muxAdaptor.getIsHealthCheckStartedMap().getOrDefault(rtmpUrl, false) == false;
		});

		//SET BROADCASTING AGAIN
		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);


		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});

		//FAILED SCENARIO
		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return IAntMediaStreamHandler.BROADCAST_STATUS_FAILED.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});

		assertTrue(muxAdaptor.getIsHealthCheckStartedMap().get(rtmpUrl));
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return muxAdaptor.getIsHealthCheckStartedMap().getOrDefault(rtmpUrl, false) == false;
		});

		//FINISHED SCENARIO
		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return muxAdaptor.getIsHealthCheckStartedMap().getOrDefault(rtmpUrl, false) == false;
		});


		//RETRY LIMIT EXCEEDED SCENARIO
		getAppSettings().setEndpointRepublishLimit(0);

		muxAdaptor.endpointStatusUpdated(rtmpUrl, IAntMediaStreamHandler.BROADCAST_STATUS_ERROR);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return IAntMediaStreamHandler.BROADCAST_STATUS_ERROR.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});

		assertTrue(muxAdaptor.getIsHealthCheckStartedMap().get(rtmpUrl));
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return muxAdaptor.getIsHealthCheckStartedMap().getOrDefault(rtmpUrl, false) == false;
		});

		verify(muxAdaptor, Mockito.timeout(5000)).sendEndpointErrorNotifyHook(rtmpUrl);

	}

	@Test
	public void testRTMPWriteCrash() {

		appScope = (WebScope) applicationContext.getBean("web.scope");

		SPSParser spsParser = new SPSParser(extradata_original, 5);

		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.width(spsParser.getWidth());
		codecParameters.height(spsParser.getHeight());
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		codecParameters.extradata_size(sps_pps_avc.length);
		BytePointer extraDataPointer = new BytePointer(sps_pps_avc);
		codecParameters.extradata(extraDataPointer);
		codecParameters.format(AV_PIX_FMT_YUV420P);
		codecParameters.codec_tag(0);
		AVRational rat = new AVRational().num(1).den(1000);

		RtmpMuxer rtmpMuxer = new RtmpMuxer("any_url", vertx);

		rtmpMuxer.init(appScope, "test", 0, null, 0);
		rtmpMuxer.addStream(codecParameters, rat, 50);
		assertTrue(rtmpMuxer.openIO());

		rtmpMuxer.setIsRunning(new AtomicBoolean(true));

		//This was a crash if we don't check headerWritten after we initialize the context and get isRunning true
		//To test the scenarios of that crash;
		rtmpMuxer.writeTrailer();

		//This should work since the trailer is not written yet
		rtmpMuxer.writeHeader();

		//This should work since header is written
		rtmpMuxer.writeTrailer();

		//This is for testing writeHeader after writeTrailer.
		rtmpMuxer.writeHeader();
	}

	@Test
	public void testRtmpUrlWithoutAppName() {
		{
			RtmpMuxer rtmpMuxer = Mockito.spy(new RtmpMuxer("rtmp://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0", vertx)); //RTMP URl without Appname
			AVDictionary opt = rtmpMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertEquals("rtmp_app", optEntry.key().getString());
			assertEquals("", optEntry.value().getString());
		}


		{
			RtmpMuxer rtmpMuxer = Mockito.spy(new RtmpMuxer("rtmp://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0/test", vertx)); //RTMP URl without Appname
			AVDictionary opt = rtmpMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertNull(optEntry);

			//if it's different from zero, it means no file is need to be open.
			//If it's zero, Not "no file" and it means that file is need to be open .
			assertEquals(0, rtmpMuxer.getOutputFormatContext().oformat().flags() & AVFMT_NOFILE);


			rtmpMuxer.clearResource();
		}

		{
			RtmpMuxer rtmpMuxer = Mockito.spy(new RtmpMuxer("rtmps://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0", vertx)); //RTMP URl without Appname
			AVDictionary opt = rtmpMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertEquals("rtmp_app", optEntry.key().getString());
			assertEquals("", optEntry.value().getString());

		}

		{
			RtmpMuxer rtmpMuxer = Mockito.spy(new RtmpMuxer("rtmps://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0/test", vertx)); //RTMP URl without Appname
			AVDictionary opt = rtmpMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertNull(optEntry);

			//if it's different from zero, it means no file is need to be open.
			//If it's zero, Not "no file" and it means that file is need to be open .
			assertEquals(0, rtmpMuxer.getOutputFormatContext().oformat().flags() & AVFMT_NOFILE);


			rtmpMuxer.clearResource();

		}

		{
			RtmpMuxer rtmpMuxer = Mockito.spy(new RtmpMuxer("rtmps://live-api-s.facebook.com:443/rtmp/y8qd-42g5-1b53-fh15-2v0", vertx)); //RTMP URl without Appname
			AVDictionary opt = rtmpMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertNull(optEntry);

			//if it's different from zero, it means no file is need to be open.
			//If it's zero, Not "no file" and it means that file is need to be open .
			assertEquals(0, rtmpMuxer.getOutputFormatContext().oformat().flags() & AVFMT_NOFILE);


			rtmpMuxer.clearResource();
		}

	}

	@Test
	public void testMp4MuxerDirectStreaming() {

		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		Mp4Muxer mp4Muxer = new Mp4Muxer(null, vertx, "streams");

		mp4Muxer.init(appScope, "test", 0, null, 0);


		SPSParser spsParser = new SPSParser(extradata_original, 5);

		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.width(spsParser.getWidth());
		codecParameters.height(spsParser.getHeight());
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		codecParameters.extradata_size(sps_pps_avc.length);
		BytePointer extraDataPointer = new BytePointer(sps_pps_avc);
		codecParameters.extradata(extraDataPointer);
		codecParameters.format(AV_PIX_FMT_YUV420P);
		codecParameters.codec_tag(0);

		AVRational rat = new AVRational().num(1).den(1000);
		//mp4Muxer.addVideoStream(spsParser.getWidth(), spsParser.getHeight(), rat, AV_CODEC_ID_H264, 0, true, codecParameters);

		mp4Muxer.addStream(codecParameters, rat, 0);


		AACConfigParser aacConfigParser = new AACConfigParser(aacConfig, 0);
		AVCodecParameters audioCodecParameters = new AVCodecParameters();
		audioCodecParameters.sample_rate(aacConfigParser.getSampleRate());

		AVChannelLayout chLayout = new AVChannelLayout();
		avutil.av_channel_layout_default(chLayout, aacConfigParser.getChannelCount());
		audioCodecParameters.ch_layout(chLayout);

		audioCodecParameters.codec_id(AV_CODEC_ID_AAC);
		audioCodecParameters.codec_type(AVMEDIA_TYPE_AUDIO);

		if (aacConfigParser.getObjectType() == AudioObjectTypes.AAC_LC) {

			audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_LOW);
		} else if (aacConfigParser.getObjectType() == AudioObjectTypes.AAC_LTP) {

			audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_LTP);
		} else if (aacConfigParser.getObjectType() == AudioObjectTypes.AAC_MAIN) {

			audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_MAIN);
		} else if (aacConfigParser.getObjectType() == AudioObjectTypes.AAC_SSR) {

			audioCodecParameters.profile(AVCodecContext.FF_PROFILE_AAC_SSR);
		}

		audioCodecParameters.frame_size(aacConfigParser.getFrameSize());
		audioCodecParameters.format(AV_SAMPLE_FMT_FLTP);
		BytePointer extraDataPointer2 = new BytePointer(aacConfig);
		audioCodecParameters.extradata(extraDataPointer2);
		audioCodecParameters.extradata_size(aacConfig.length);
		audioCodecParameters.codec_tag(0);


		mp4Muxer.addStream(audioCodecParameters, rat, 1);


		mp4Muxer.prepareIO();

		int i = 0;
		try {
			File file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);
			while (flvReader.hasMoreTags()) {
				ITag readTag = flvReader.readTag();
				StreamPacket streamPacket = new StreamPacket(readTag);


				if (streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
					int bodySize = streamPacket.getData().limit();

					byte frameType = streamPacket.getData().position(0).get();

					// get the audio or video codec identifier

					ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bodySize - 5);
					byteBuffer.put(streamPacket.getData().buf().position(5));

					mp4Muxer.writeVideoBuffer(byteBuffer, streamPacket.getTimestamp(), 0, 0, (frameType & 0xF0) == IVideoStreamCodec.FLV_FRAME_KEY, 0, streamPacket.getTimestamp());

				} else if (streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) {
					i++;
					if (i == 1) {
						continue;
					}
					int bodySize = streamPacket.getData().limit();

					ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bodySize - 2);
					byteBuffer.put(streamPacket.getData().buf().position(2));

					mp4Muxer.writeAudioBuffer(byteBuffer, 1, streamPacket.getTimestamp());

				}

			}

			mp4Muxer.writeTrailer();


			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> MuxingTest.testFile(mp4Muxer.getFile().getAbsolutePath(), 697000));


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}

	@Test
	public void testMuxAdaptorEnableSettingsPreviewCreatePeriod() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope);
		int createPreviewPeriod = (int) (Math.random() * 10000);
		assertNotEquals(0, createPreviewPeriod);
		getAppSettings().setCreatePreviewPeriod(createPreviewPeriod);
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId("test");
		} catch (Exception e) {
			e.printStackTrace();
		}

		muxAdaptor.setBroadcast(broadcast);
		boolean result = muxAdaptor.init(appScope, "test", false);
		assertTrue(result);

		assertEquals(createPreviewPeriod, muxAdaptor.getPreviewCreatePeriod());
	}


	@Test
	public void testClientBroadcastStreamStartPublish() {
		ClientBroadcastStream clientBroadcastStreamReal = new ClientBroadcastStream();

		ClientBroadcastStream clientBroadcastStream = Mockito.spy(clientBroadcastStreamReal);
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		IStreamCapableConnection conn = Mockito.mock(IStreamCapableConnection.class);
		Mockito.doReturn(conn).when(clientBroadcastStream).getConnection();

		//	IContext context = conn.getScope().getContext(); 
		//	ApplicationContext appCtx = context.getApplicationContext(); 
		appScope = (WebScope) applicationContext.getBean("web.scope");

		Mockito.when(conn.getScope()).thenReturn(appScope);

		assertNull(clientBroadcastStream.getMuxAdaptor());


		clientBroadcastStream.startPublishing();

		//because no streamId
		assertNull(clientBroadcastStream.getMuxAdaptor());


		clientBroadcastStream.setPublishedName("streamId");
		clientBroadcastStream.startPublishing();
		assertNotNull(clientBroadcastStream.getMuxAdaptor());
	}

	@Test
	public void testMuxingSimultaneously() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(false);

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		Broadcast broadcast = new Broadcast();
		List<EncoderSettings> adaptiveResolutionList = Arrays.asList(new EncoderSettings(144, 150000, 32000,true));
		broadcast.setEncoderSettingsList(adaptiveResolutionList);


		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, broadcast, false, appScope);

		//this value should be -1. It means it is uninitialized
		assertEquals(0, muxAdaptor.getPacketTimeList().size());
		File file = null;

		try {
			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());
			String streamId = "test" + (int) (Math.random() * 991000);
			broadcast = new Broadcast();
			broadcast.setStreamId(streamId);
			getDataStore().save(broadcast);
			boolean result = muxAdaptor.init(appScope, streamId, false);
			assertTrue(result);

			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop(true);

			flvReader.close();


			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			assertFalse(muxAdaptor.isRecording());

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				File f1 = new File(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath());
				File f2 = new File(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath());
				return f1.exists() && f2.exists();
			});


			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath()));
			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath()));

		} catch (Exception e) {
			e.printStackTrace();
			fail("excsereption:" + e);
		}

	}

	@Test
	public void testIsEncoderAdaptorShouldBeTried() {

		AppSettings appSettingsLocal = new AppSettings();
		appSettingsLocal.setWebRTCEnabled(false);
		appSettingsLocal.setForceDecoding(false);

		assertFalse(MuxAdaptor.isEncoderAdaptorShouldBeTried(null, appSettingsLocal));

		appSettingsLocal.setWebRTCEnabled(true);
		assertTrue(MuxAdaptor.isEncoderAdaptorShouldBeTried(null, appSettingsLocal));

		appSettingsLocal.setWebRTCEnabled(false);
		appSettingsLocal.setForceDecoding(true);
		assertTrue(MuxAdaptor.isEncoderAdaptorShouldBeTried(null, appSettingsLocal));

		appSettingsLocal.setWebRTCEnabled(true);
		appSettingsLocal.setForceDecoding(true);
		assertTrue(MuxAdaptor.isEncoderAdaptorShouldBeTried(null, appSettingsLocal));

		appSettingsLocal.setWebRTCEnabled(false);
		appSettingsLocal.setForceDecoding(false);
		appSettingsLocal.setEncoderSettings(null);
		assertFalse(MuxAdaptor.isEncoderAdaptorShouldBeTried(null, appSettingsLocal));


		appSettingsLocal.setEncoderSettings(Arrays.asList());
		assertFalse(MuxAdaptor.isEncoderAdaptorShouldBeTried(null, appSettingsLocal));

		appSettingsLocal.setEncoderSettings(Arrays.asList(new EncoderSettings(144, 150000, 32000, true)));
		assertTrue(MuxAdaptor.isEncoderAdaptorShouldBeTried(null, appSettingsLocal));

		appSettingsLocal.setEncoderSettings(null);
		Broadcast broadcast = new Broadcast();
		assertFalse(MuxAdaptor.isEncoderAdaptorShouldBeTried(broadcast, appSettingsLocal));

		broadcast.setEncoderSettingsList(Arrays.asList());
		assertFalse(MuxAdaptor.isEncoderAdaptorShouldBeTried(broadcast, appSettingsLocal));


		broadcast.setEncoderSettingsList(Arrays.asList(new EncoderSettings(144, 150000, 32000, true)));
		assertTrue(MuxAdaptor.isEncoderAdaptorShouldBeTried(broadcast, appSettingsLocal));

	}


	@Test
	public void testStressMp4Muxing() {

		long startTime = System.nanoTime();
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		try {

			ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
			StreamCodecInfo info = new StreamCodecInfo();
			clientBroadcastStream.setCodecInfo(info);

			getAppSettings().setMaxAnalyzeDurationMS(50000);
			getAppSettings().setHlsMuxingEnabled(false);
			getAppSettings().setMp4MuxingEnabled(true);
			getAppSettings().setAddDateTimeToMp4FileName(false);

			List<MuxAdaptor> muxAdaptorList = new ArrayList<>();
			for (int j = 0; j < 5; j++) {
				MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, new Broadcast(), false, appScope);
				muxAdaptorList.add(muxAdaptor);
			}
			{

				File file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
				final FLVReader flvReader = new FLVReader(file);

				logger.debug("f path: " + file.getAbsolutePath());
				assertTrue(file.exists());

				for (Iterator<MuxAdaptor> iterator = muxAdaptorList.iterator(); iterator.hasNext(); ) {
					MuxAdaptor muxAdaptor = (MuxAdaptor) iterator.next();
					String streamId = "test" + (int) (Math.random() * 991000);
					Broadcast broadcast = new Broadcast();
					broadcast.setStreamId(streamId);
					getDataStore().save(broadcast);

					boolean result = muxAdaptor.init(appScope, streamId, false);
					assertTrue(result);
					muxAdaptor.start();
					logger.info("Mux adaptor instance initialized for {}", streamId);
				}

				feedMuxAdaptor(flvReader, muxAdaptorList, info);

				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					logger.info("Check if is recording: {}", muxAdaptor.getStreamId());
					Awaitility.await().atMost(50, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
						return muxAdaptor.isRecording();
					});
				}

				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					muxAdaptor.stop(true);
				}


				flvReader.close();

				for (MuxAdaptor muxAdaptor : muxAdaptorList) {
					Awaitility.await().atMost(50, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
						logger.info("Check if it's not recording: {}", muxAdaptor.getStreamId());
						return !muxAdaptor.isRecording();
					});
				}


			}

			int count = 0;
			for (MuxAdaptor muxAdaptor : muxAdaptorList) {
				List<Muxer> muxerList = muxAdaptor.getMuxerList();
				for (Muxer abstractMuxer : muxerList) {
					if (abstractMuxer instanceof Mp4Muxer) {
						assertTrue(MuxingTest.testFile(abstractMuxer.getFile().getAbsolutePath(), 697000));
						count++;
					}
				}
			}

			assertEquals(muxAdaptorList.size(), count);

			long diff = (System.nanoTime() - startTime) / 1000000;

			System.out.println(" time diff: " + diff + " ms");
		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setHlsMuxingEnabled(true);

	}

	@Test
	public void testMuxerStreamType() {
		assertEquals("video", MuxAdaptor.getStreamType(AVMEDIA_TYPE_VIDEO));
		assertEquals("audio", MuxAdaptor.getStreamType(AVMEDIA_TYPE_AUDIO));
		assertEquals("data", MuxAdaptor.getStreamType(AVMEDIA_TYPE_DATA));
		assertEquals("subtitle", MuxAdaptor.getStreamType(AVMEDIA_TYPE_SUBTITLE));
		assertEquals("attachment", MuxAdaptor.getStreamType(AVMEDIA_TYPE_ATTACHMENT));
		assertEquals("not_known", MuxAdaptor.getStreamType(55));
	}


	@Test
	public void testMp4MuxingWithWithMultipleDepth() {
		File file = testMp4Muxing("test_test/test");
		assertEquals("test.mp4", file.getName());

		file = testMp4Muxing("dir1/dir2/file");
		assertTrue(file.exists());

		file = testMp4Muxing("dir1/dir2/dir3/file");
		assertTrue(file.exists());

		file = testMp4Muxing("dir1/dir2/dir3/dir4/file");
		assertTrue(file.exists());
	}

	@Test
	public void testMp4MuxingWithSameName() {
		logger.info("running testMp4MuxingWithSameName");

		Application.resetFields();

		assertTrue(Application.id.isEmpty());
		assertTrue(Application.file.isEmpty());
		assertTrue(Application.duration.isEmpty());

		File file = testMp4Muxing("test_test");
		assertEquals("test_test.mp4", file.getName());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test".equals(Application.id.get(0));
		});

		assertEquals("test_test", Application.id.get(0));

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test.mp4".equals(Application.file.get(0).getName());
		});

		assertEquals("test_test.mp4", Application.file.get(0).getName());
		assertNotEquals(0L, (long) Application.duration.get(0));

		Application.resetFields();

		assertTrue(Application.id.isEmpty());
		assertTrue(Application.file.isEmpty());
		assertTrue(Application.duration.isEmpty());

		file = testMp4Muxing("test_test");
		assertEquals("test_test_1.mp4", file.getName());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test".equals(Application.id.get(0));
		});

		assertEquals("test_test", Application.id.get(0));

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test_1.mp4".equals(Application.file.get(0).getName());
		});

		assertEquals("test_test_1.mp4", Application.file.get(0).getName());
		assertNotEquals(0L, Application.duration);

		Application.resetFields();

		assertTrue(Application.id.isEmpty());
		assertTrue(Application.file.isEmpty());
		assertTrue(Application.duration.isEmpty());

		file = testMp4Muxing("test_test");
		assertEquals("test_test_2.mp4", file.getName());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test".equals(Application.id.get(0));
		});

		assertEquals("test_test", Application.id.get(0));

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return "test_test_2.mp4".equals(Application.file.get(0).getName());
		});

		assertEquals("test_test_2.mp4", Application.file.get(0).getName());
		assertNotEquals(0L, (long) Application.duration.get(0));

		logger.info("leaving testMp4MuxingWithSameName");
	}


	@Test
	public void testBaseStreamFileServiceBug() {
		//I've removed this test because we don't maintain the RTMP and removing some redundant dependencies
		/*
		MP4Service mp4Service = new MP4Service();

		String fileName = mp4Service.prepareFilename("mp4:1");
		assertEquals("1.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:12");
		assertEquals("12.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:123");
		assertEquals("123.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:1234");
		assertEquals("1234.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:12345");
		assertEquals("12345.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:123456");
		assertEquals("123456.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:1.mp4");
		assertEquals("1.mp4", fileName);

		fileName = mp4Service.prepareFilename("mp4:123456789.mp4");
		assertEquals("123456789.mp4", fileName);
		 */

	}


	@Test
	public void testApplicationStreamLimit() {
		AntMediaApplicationAdapter appAdaptor = Mockito.spy((AntMediaApplicationAdapter) applicationContext.getBean("web.handler"));
		assertNotNull(appAdaptor);

		String streamId = "stream " + (int) (Math.random() * 10000);

		appAdaptor.setDataStore(new InMemoryDataStore("dbtest"));
		long activeBroadcastCount = appAdaptor.getDataStore().getActiveBroadcastCount();

		logger.info("Active broadcast count: {}", activeBroadcastCount);
		long broadcastCount = appAdaptor.getDataStore().getBroadcastCount();
		logger.info("Total broadcast count: {}", broadcastCount);
		if (activeBroadcastCount > 0) {
			long pageSize = broadcastCount / 50 + 1;

			for (int i = 0; i < pageSize; i++) {
				List<Broadcast> broadcastList = appAdaptor.getDataStore().getBroadcastList(i * 50, 50, "", "status", "", "");

				for (Broadcast broadcast : broadcastList) {
					logger.info("Broadcast id: {} status:{}", broadcast.getStreamId(), broadcast.getStatus());
				}
			}
		}

		activeBroadcastCount = appAdaptor.getDataStore().getActiveBroadcastCount();

		appSettings.setIngestingStreamLimit(2);


		appAdaptor.startPublish(streamId, 0, null, null, null);


		streamId = "stream " + (int) (Math.random() * 10000);

		appAdaptor.startPublish(streamId, 0, null, null, null);

		long activeBroadcastCountFinal = activeBroadcastCount;
		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
		.until(() -> {
			return activeBroadcastCountFinal + 2 == appAdaptor.getDataStore().getActiveBroadcastCount();
		});

		if (activeBroadcastCount == 1) {
			Mockito.verify(appAdaptor, timeout(1000)).stopStreaming(Mockito.any(), Mockito.any(), Mockito.any());
		}

		streamId = "stream " + (int) (Math.random() * 10000);

		appAdaptor.startPublish(streamId, 0, null, null, null);

		Mockito.verify(appAdaptor, timeout(1000).times((int) activeBroadcastCount + 1)).stopStreaming(Mockito.any(), Mockito.anyBoolean(), Mockito.any());

	}

	@Test
	public void testAbsoluteStartTimeMs() {
		AntMediaApplicationAdapter appAdaptor = ((AntMediaApplicationAdapter) applicationContext.getBean("web.handler"));
		assertNotNull(appAdaptor);

		AntMediaApplicationAdapter spyAdaptor = Mockito.spy(appAdaptor);

		ClientBroadcastStream stream = Mockito.mock(ClientBroadcastStream.class);

		String streamId = "stream" + (int) (Math.random() * 10000000);
		Mockito.when(stream.getPublishedName()).thenReturn(streamId);

		doReturn(stream).when(spyAdaptor).getBroadcastStream(Mockito.any(), Mockito.any());



		long absoluteTimeMS = System.currentTimeMillis();
		
		spyAdaptor.startPublish(streamId, absoluteTimeMS, null, null, null);

		when(stream.getAbsoluteStartTimeMs()).thenReturn(absoluteTimeMS);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
		.until(() ->
		appAdaptor.getDataStore().get(streamId).getAbsoluteStartTimeMs() == absoluteTimeMS);

		spyAdaptor.stopPublish(stream.getPublishedName());


		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
		.until(() ->
		appAdaptor.getDataStore().get(streamId) == null);


	}


	@Test
	public void testMp4MuxingAndNotifyCallback() {

		Application app = (Application) applicationContext.getBean("web.handler");
		AntMediaApplicationAdapter appAdaptor = Mockito.spy(app);

		Mockito.doNothing().when(appAdaptor).notifyHook(anyString(), anyString(), any(), anyString(), anyString(), anyString(), anyString(), 
					anyString(), anyString(), anyString(), anyMap());
		assertNotNull(appAdaptor);

		//just check below value that it is not null, this is not related to this case but it should be tested
		String hookUrl = "http://google.com";
		String name = "namer123";
		Broadcast broadcast = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, name);
		broadcast.setListenerHookURL(hookUrl);
		String streamId = appAdaptor.getDataStore().save(broadcast);

		Application.resetFields();
		testMp4Muxing(streamId, false, true);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return streamId.equals(Application.id.get(0));
		});

		assertTrue(Application.id.contains(streamId));
		assertEquals(Application.file.get(0).getName(), streamId + ".mp4");
		assertTrue(Math.abs(697202l - Application.duration.get(0)) < 250);

		broadcast = appAdaptor.getDataStore().get(streamId);
		//we do not save duration of the finished live streams
		//assertEquals((long)broadcast.getDuration(), 697132L);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			return Application.notifyHookAction.contains(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY);
		});

		assertTrue(Application.notifyId.contains(streamId));
		assertTrue(Application.notitfyURL.contains(hookUrl));
		assertTrue(Application.notifyHookAction.contains(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY));
		assertTrue(Application.notifyVodName.contains(streamId));

		Application.resetFields();
		assertTrue(Application.notifyId.isEmpty());
		assertTrue(Application.notitfyURL.isEmpty());
		assertTrue(Application.notifyHookAction.isEmpty());

		//test with same id again
		testMp4Muxing(streamId, true, true);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			return Application.notifyHookAction.contains(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY);
		});

		assertTrue(Application.notifyId.contains(streamId));
		assertTrue(Application.notitfyURL.contains(hookUrl));
		assertTrue(Application.notifyHookAction.contains(AntMediaApplicationAdapter.HOOK_ACTION_VOD_READY));
		assertTrue(Application.notifyVodName.contains(streamId + "_1"));


		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return Application.id.contains(streamId);
		});
		assertEquals(Application.id.get(0), streamId);
		assertEquals(Application.file.get(0).getName(), streamId + "_1.mp4");
		assertEquals(10062L, (long) Application.duration.get(0));

		broadcast = appAdaptor.getDataStore().get(streamId);
		//we do not save duration of the finished live streams
		//assertEquals((long)broadcast.getDuration(), 10080L);

	}

	@Test
	public void testMp4MuxingHighProfileDelayedVideo() {

		String name = "high_profile_delayed_video_" + (int) (Math.random() * 10000);
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();

		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		if (getDataStore().get(name) == null) {
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId(name);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			getDataStore().save(broadcast);
		}
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setHlsMuxingEnabled(false);

		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			file = new File("src/test/resources/high_profile_delayed_video.flv");

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, name, false);

			assertTrue(result);

			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop(true);

			flvReader.close();


			Awaitility.await().atMost(40, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());
			assertFalse(muxAdaptor.isRecording());

			int finalDuration = 20000;
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() ->
			MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), finalDuration));

			assertEquals(1640, MuxingTest.videoStartTimeMs);
			assertEquals(0, MuxingTest.audioStartTimeMs);

		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
		logger.info("leaving testMp4Muxing");
	}


	public File testMp4Muxing(String name) {
		return testMp4Muxing(name, true, true);
	}

	@Test
	public void testMuxAdaptorClose() {

		appScope = (WebScope) applicationContext.getBean("web.scope");

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope);
		String streamId = "stream_id" + (int) (Math.random() * 10000);

		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		muxAdaptor.setBroadcast(broadcast);
		boolean result = muxAdaptor.init(appScope, streamId, false);

		assertTrue(result);

		muxAdaptor.closeResources();
	}

	@Test
	public void testOrderedBufferedQueue() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));
		ConcurrentSkipListSet<IStreamPacket> bufferQueue = muxAdaptor.getBufferQueue();


		ITag tag = mock(ITag.class);
		IStreamPacket pkt;
		{
			when(tag.getTimestamp()).thenReturn(1000);
			pkt = new StreamPacket(tag);
			bufferQueue.add(pkt);


			tag = mock(ITag.class);
			when(tag.getTimestamp()).thenReturn(2000);
			pkt = new StreamPacket(tag);
			bufferQueue.add(pkt);

			tag = mock(ITag.class);
			when(tag.getTimestamp()).thenReturn(3000);
			pkt = new StreamPacket(tag);
			bufferQueue.add(pkt);

			assertEquals(1000, bufferQueue.pollFirst().getTimestamp());
			assertEquals(2000, bufferQueue.pollFirst().getTimestamp());
			assertEquals(3000, bufferQueue.pollFirst().getTimestamp());
		}

		{
			tag = mock(ITag.class);
			when(tag.getTimestamp()).thenReturn(1000);
			pkt = new StreamPacket(tag);
			bufferQueue.add(pkt);

			tag = mock(ITag.class);
			when(tag.getTimestamp()).thenReturn(3000);
			pkt = new StreamPacket(tag);
			bufferQueue.add(pkt);

			assertEquals(1000, bufferQueue.first().getTimestamp());
			assertEquals(3000, bufferQueue.last().getTimestamp());

			tag = mock(ITag.class);
			when(tag.getTimestamp()).thenReturn(2000);
			pkt = new StreamPacket(tag);
			bufferQueue.add(pkt);

			assertEquals(1000, bufferQueue.pollFirst().getTimestamp());
			assertEquals(2000, bufferQueue.pollFirst().getTimestamp());
			assertEquals(3000, bufferQueue.pollFirst().getTimestamp());
		}


	}


	@Test
	public void testAddBufferQueue() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));

		muxAdaptor.setBufferTimeMs(1000);


		assertFalse(muxAdaptor.isBuffering());

		muxAdaptor.setBuffering(true);

		for (int i = 0; i <= 11; i++) {
			ITag tag = mock(ITag.class);
			when(tag.getTimestamp()).thenReturn(i * 100);
			IStreamPacket pkt = new StreamPacket(tag);
			muxAdaptor.addBufferQueue(pkt);
			muxAdaptor.calculateBufferStatus();
			if (i < 11) {
				assertTrue(muxAdaptor.isBuffering());
			} else if (i == 11) {
				assertFalse(muxAdaptor.isBuffering());
			}
		}


		for (int i = 12; i <= 51; i++) {
			ITag tag = mock(ITag.class);
			when(tag.getTimestamp()).thenReturn(i * 100);
			IStreamPacket pkt = new StreamPacket(tag);
			muxAdaptor.addBufferQueue(pkt);
			muxAdaptor.calculateBufferStatus();
			long bufferedDuration = muxAdaptor.getBufferQueue().last().getTimestamp() - muxAdaptor.getBufferQueue().first().getTimestamp();
			if (i < 51) {
				assertEquals(i * 100, bufferedDuration);
			}
		}

		//it exceeds the 5 times of the buffer time so it should truncate less than the 2 times of buffer time -> 2*1000 = 2000. 
		//for our sample, It turns out that -> 1900 because there is 100ms difference in the packets
		long bufferedDuration = muxAdaptor.getBufferQueue().last().getTimestamp() - muxAdaptor.getBufferQueue().first().getTimestamp();
		assertEquals(1900, bufferedDuration);

	}

	@Test
	public void testWriteBufferedPacket() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));

		muxAdaptor.setBuffering(true);
		muxAdaptor.writeBufferedPacket();
		assertTrue(muxAdaptor.isBuffering());

		muxAdaptor.setBuffering(false);
		muxAdaptor.writeBufferedPacket();
		//it should false because there is no packet in the queue
		assertTrue(muxAdaptor.isBuffering());

		ConcurrentSkipListSet<IStreamPacket> bufferQueue = muxAdaptor.getBufferQueue();
		muxAdaptor.setBuffering(false);


		ITag tag = mock(ITag.class);
		when(tag.getTimestamp()).thenReturn(1000);

		IStreamPacket pkt = new StreamPacket(tag);

		//pkt.stream_index(0);

		bufferQueue.add(pkt);

		doNothing().when(muxAdaptor).writeStreamPacket(any());
		muxAdaptor.writeBufferedPacket();
		verify(muxAdaptor).writeStreamPacket(any());
		assertTrue(muxAdaptor.isBuffering());
		assertTrue(bufferQueue.isEmpty());

		muxAdaptor.setBuffering(false);
		muxAdaptor.setBufferingFinishTimeMs(System.currentTimeMillis());

		ITag tag2 = mock(ITag.class);
		int timeStamp = 10000;
		when(tag2.getTimestamp()).thenReturn(timeStamp);
		pkt = new StreamPacket(tag2);
		bufferQueue.add(pkt);
		muxAdaptor.writeBufferedPacket();
		assertFalse(muxAdaptor.isBuffering());

	}

	@Test
	public void testDropPacketIfStopped() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = Mockito.spy(new ClientBroadcastStream());
		clientBroadcastStream.setConnection(Mockito.mock(IStreamCapableConnection.class));
		StreamCodecInfo info = new StreamCodecInfo();

		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope));

		ITag tag = new Tag((byte) Constants.TYPE_VIDEO_DATA, 0, 10, IoBuffer.allocate(10), BUFFER_SIZE);
		StreamPacket streamPacket = new StreamPacket(tag);


		assertEquals(0, muxAdaptor.getInputQueueSize());

		muxAdaptor.packetReceived(clientBroadcastStream, streamPacket);
		assertEquals(1, muxAdaptor.getInputQueueSize());


		muxAdaptor.stop(true);
		muxAdaptor.packetReceived(clientBroadcastStream, streamPacket);
		assertEquals(1, muxAdaptor.getInputQueueSize());
		Mockito.verify(muxAdaptor).closeRtmpConnection();

	}




	@Test
	public void testRtmpIngestBufferTime() throws IOException {


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();

		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		//increase max analyze duration to some higher value because it's also to close connections if packet is not received
		getAppSettings().setMaxAnalyzeDurationMS(5000);
		getAppSettings().setRtmpIngestBufferTimeMs(1000);
		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setHlsMuxingEnabled(false);

		File file = new File("target/test-classes/test.flv");

		String streamId = "streamId" + (int) (Math.random() * 10000);
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		muxAdaptor.setBroadcast(broadcast);
		boolean result = muxAdaptor.init(appScope, streamId, false);
		assertTrue(result);

		muxAdaptor.start();


		final FLVReader flvReader = new FLVReader(file);
		boolean firstAudioPacketReceived = false;
		boolean firstVideoPacketReceived = false;
		long lastTimeStamp = 0;
		while (flvReader.hasMoreTags()) {
			ITag readTag = flvReader.readTag();
			StreamPacket streamPacket = new StreamPacket(readTag);
			lastTimeStamp = readTag.getTimestamp();
			if (!firstAudioPacketReceived && streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) {
				IAudioStreamCodec audioStreamCodec = AudioCodecFactory.getAudioCodec(streamPacket.getData().position(0));
				info.setAudioCodec(audioStreamCodec);
				audioStreamCodec.addData(streamPacket.getData().position(0));
				info.setHasAudio(true);
				firstAudioPacketReceived = true;
			} else if (!firstVideoPacketReceived && streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
				IVideoStreamCodec videoStreamCodec = VideoCodecFactory.getVideoCodec(streamPacket.getData().position(0));
				videoStreamCodec.addData(streamPacket.getData().position(0));
				info.setVideoCodec(videoStreamCodec);
				info.setHasVideo(true);
				firstVideoPacketReceived = true;
			}


			if (lastTimeStamp < 6000) {

				if (streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
					VideoData videoData = new VideoData(streamPacket.getData().duplicate().position(0));
					videoData.setTimestamp(streamPacket.getTimestamp());
					videoData.setReceivedTime(System.currentTimeMillis());

					muxAdaptor.packetReceived(null, videoData);

				}

				else {
					CachedEvent event = new CachedEvent();
					event.setData(streamPacket.getData().duplicate());
					event.setDataType(streamPacket.getDataType());
					event.setReceivedTime(System.currentTimeMillis());
					event.setTimestamp(streamPacket.getTimestamp());

					muxAdaptor.packetReceived(null, event);

				}


			} else {
				break;
			}
		}
		System.gc();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(muxAdaptor::isRecording);
		//let the buffered time finish and buffering state is true
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(muxAdaptor::isBuffering);

		//load again for 6 more seconds
		while (flvReader.hasMoreTags()) {
			ITag readTag = flvReader.readTag();

			if (readTag.getTimestamp() - lastTimeStamp < 6000) {
				StreamPacket streamPacket = new StreamPacket(readTag);
				muxAdaptor.packetReceived(null, streamPacket);
			} else {
				break;
			}
		}
		System.out.println("finish feeding");
		//buffering should be false after a while because it's loaded with 5 seconds
		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !muxAdaptor.isBuffering());

		//after 6 seconds buffering should be also true again because it's finished
		Awaitility.await().atMost(6, TimeUnit.SECONDS).until(muxAdaptor::isBuffering);

		muxAdaptor.stop(true);

		Awaitility.await().atMost(4, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());



		getAppSettings().setRtmpIngestBufferTimeMs(0);

	}

	@Test
	public void testMp4Muxing() {
		File mp4File = testMp4Muxing("lkdlfkdlfkdlfk");

		VideoInfo fileInfo = VideoProber.getFileInfo(mp4File.getAbsolutePath());
		assertTrue(252 - fileInfo.videoPacketsCount < 5);
		assertTrue(431 - fileInfo.audioPacketsCount < 5);
	}

	@Test
	public void testPlusplus() 
	{
		long t = Byte.toUnsignedLong((byte)0xff);
		assertEquals(255, t);
		long i  = (t << 48);

		assertEquals(0xFF000000000000l, i);


		//long data = Byte.toUnsignedLong((byte)0xFF);
		long data = ((long)0xff << 32);
		assertEquals(0Xff00000000l, data);

		//int unsigned = Byte.toUnsignedInt(t);

		//assertEquals(0xFF00, unsigned << 8);

	}

	public File testMp4Muxing(String streamId, boolean shortVersion, boolean checkDuration) {

		logger.info("running testMp4Muxing");

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();

		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		String streamName = "broadcastName";
		String description = "broadadcastDescription";
		String metadata = "metadata";
		String lat = "1L";
		String longitude = "2L";
		String altitude = "3L";

		boolean checkValuesVoDFields = false;

		if (getDataStore().get(streamId) == null) {

			checkValuesVoDFields = true;
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId(streamId);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			broadcast.setName(streamName);
			broadcast.setDescription(description);
			broadcast.setLatitude(lat);
			broadcast.setLongitude(longitude);
			broadcast.setAltitude(altitude);
			broadcast.setMetaData(metadata);
			//set this zombi to trigger delete operation at the end
			broadcast.setZombi(true);
			getDataStore().save(broadcast);

		}
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setHlsMuxingEnabled(false);

		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			if (shortVersion) {
				file = new File("target/test-classes/test_short.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			} else {
				file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			}

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			boolean result = muxAdaptor.init(appScope, streamId, false);

			assertTrue(result);

			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());
			assertTrue(muxAdaptor.isRecording());

			muxAdaptor.stop(true);

			flvReader.close();


			Awaitility.await().atMost(40, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());
			assertFalse(muxAdaptor.isRecording());

			int duration = 697000;
			if (shortVersion) {
				duration = 10080;
			}

			if (checkDuration) {
				int finalDuration = duration;
				Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() ->
				MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), finalDuration));
			}

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
				List<VoD> tmpList = getDataStore().getVodList(0, 10, "date", "desc", streamId, null);

				return tmpList.size() > 0;
			});

			List<VoD> vodList = getDataStore().getVodList(0, 10, "date", "desc", streamId, null);

			assertTrue(1 <= vodList.size());

			if (checkValuesVoDFields) {
				assertEquals(streamName, vodList.get(0).getStreamName());
				assertEquals(description, vodList.get(0).getDescription());

				assertEquals(lat, vodList.get(0).getLatitude());

				assertEquals(longitude, vodList.get(0).getLongitude());

				assertEquals(altitude, vodList.get(0).getAltitude());
				assertEquals(metadata, vodList.get(0).getMetadata());
			}



			return muxAdaptor.getMuxerList().get(0).getFile();
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
		logger.info("leaving testMp4Muxing");
		return null;
	}

	@Test
	public void testHLSMuxerCodecSupported() 
	{
		HLSMuxer hlsMuxerTester = new HLSMuxer(vertx, null, "streams", 1, null, false);

		assertFalse(hlsMuxerTester.isCodecSupported(AV_CODEC_ID_VP8));
		assertTrue(hlsMuxerTester.isCodecSupported(AV_CODEC_ID_AC3));
		assertTrue(hlsMuxerTester.isCodecSupported(AV_CODEC_ID_AAC));
		assertTrue(hlsMuxerTester.isCodecSupported(AV_CODEC_ID_H264));
		assertTrue(hlsMuxerTester.isCodecSupported(AV_CODEC_ID_H265));
		assertTrue(hlsMuxerTester.isCodecSupported(AV_CODEC_ID_MP3));
		assertFalse(hlsMuxerTester.isCodecSupported(AV_CODEC_ID_NONE));

	}

	@Test
	public void updateStreamQualityParameters() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope);


		String streamId = "streamId";
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Application.enableSourceHealthUpdate = true;


		boolean result = muxAdaptor.init(appScope, streamId, false);

		muxAdaptor.getDataStore().save(broadcast);

		muxAdaptor.setInputQueueSize(10);

		muxAdaptor.updateStreamQualityParameters(streamId, 0.99612);

		Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcast2 = muxAdaptor.getDataStore().get(streamId);
			logger.info("speed: {}", broadcast2.getSpeed());
			return "0.996".equals(Double.toString(broadcast2.getSpeed()));
		});

		Broadcast broadcast2 = muxAdaptor.getDataStore().get(streamId);
		assertEquals("0.996", Double.toString(broadcast2.getSpeed()));

		assertEquals(10, broadcast2.getPendingPacketSize());
		long lastUpdateTime = broadcast2.getUpdateTime();
		assertTrue((System.currentTimeMillis() - lastUpdateTime) < 1000);

		//todo: this is a hack to increase the coverage for webhook things, add better tests to confirm
		muxAdaptor.getAppSettings().setWebhookStreamStatusUpdatePeriodMs(1000);


		for (int i = 0; i < 100; i++) {
			//it should not update because it updates for every 5 seconds
			muxAdaptor.updateStreamQualityParameters(streamId, 0.99612 + Math.random());
		}

		broadcast2 = muxAdaptor.getDataStore().get(streamId);
		assertEquals("0.996", Double.toString(broadcast2.getSpeed()));

		assertEquals(10, broadcast2.getPendingPacketSize());
		assertEquals(lastUpdateTime, broadcast2.getUpdateTime());

		//todo: this is a hack to increase the coverage for webhook things, add better tests to confirm
		muxAdaptor.getAppSettings().setListenerHookURL("http://127.0.0.1/webhook");

		muxAdaptor.setInputQueueSize(12120);


		Awaitility.await().pollDelay(MuxAdaptor.STAT_UPDATE_PERIOD_MS + 1000, TimeUnit.MILLISECONDS)
		.atMost(MuxAdaptor.STAT_UPDATE_PERIOD_MS * 2, TimeUnit.MILLISECONDS).until(() -> {
			muxAdaptor.updateStreamQualityParameters(streamId, 1.0123);
			return true;
		});

		Awaitility.await().atMost(MuxAdaptor.STAT_UPDATE_PERIOD_MS + 1000, TimeUnit.MILLISECONDS).until(() -> {
			Broadcast broadcastTmp = muxAdaptor.getDataStore().get(streamId);
			logger.info("speed: {}", broadcastTmp.getSpeed());
			return "1.012".equals(Double.toString(broadcastTmp.getSpeed()));
		});

		broadcast2 = muxAdaptor.getDataStore().get(streamId);
		assertEquals("1.012", Double.toString(broadcast2.getSpeed()));

		assertEquals(12120, broadcast2.getPendingPacketSize());
		assertNotEquals(lastUpdateTime, broadcast2.getUpdateTime());

		assertTrue((System.currentTimeMillis() - broadcast2.getUpdateTime()) < 1000);

		Application.enableSourceHealthUpdate = false;

	}


	@Test
	public void testMp4MuxingSubtitledVideo() {
		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setAddDateTimeToMp4FileName(true);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(false);
		getAppSettings().setUploadExtensionsToS3(2);


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			file = new File("target/test-classes/test_video_360p_subtitle.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));


			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId("video_with_subtitle_stream");
			} catch (Exception e) {
				e.printStackTrace();
			}

			muxAdaptor.setBroadcast(broadcast);

			boolean result = muxAdaptor.init(appScope, "video_with_subtitle_stream", false);
			assertTrue(result);


			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());
			muxAdaptor.stop(true);

			flvReader.close();

			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			// if there is listenerHookURL, a task will be scheduled, so wait a little to make the call happen
			Thread.sleep(200);

			List<Muxer> muxerList = muxAdaptor.getMuxerList();
			Mp4Muxer mp4Muxer = null;
			for (Muxer muxer : muxerList) {
				if (muxer instanceof Mp4Muxer) {
					mp4Muxer = (Mp4Muxer) muxer;
					break;
				}
			}
			assertFalse(mp4Muxer.isUploadingToS3());

			int duration = 146401;

			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath(), duration));
			assertTrue(MuxingTest.testFile(muxAdaptor.getMuxerList().get(1).getFile().getAbsolutePath()));

		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
	}

	@Test
	public void testHLSNormal() {
		testHLSMuxing("hlsmuxing_test");
	}

	@Test
	public void testUploadExtensions() {
		//av_log_set_level (40);
		int hlsListSize = 3;
		int hlsTime = 2;
		String name = "streamtestExtensions";

		getAppSettings().setMp4MuxingEnabled(true);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(false);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));
		getAppSettings().setUploadExtensionsToS3(3);
		getAppSettings().setS3RecordingEnabled(true);


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		Vertx vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
		assertNotNull(vertx);

		StorageClient client = Mockito.mock(AmazonS3StorageClient.class);
		HLSMuxer hlsMuxerTester = new HLSMuxer(vertx, client, "streams", 1, null, false);
		hlsMuxerTester.setHlsParameters(null, null, null, null, null, null);
		assertFalse(hlsMuxerTester.isUploadingToS3());


		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);
		muxAdaptor.setStorageClient(client);

		File file = null;
		try {

			file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.info("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId(name);
			} catch (Exception e) {
				e.printStackTrace();
			}

			muxAdaptor.setBroadcast(broadcast);
			boolean result = muxAdaptor.init(appScope, name, false);
			assert (result);

			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

			muxAdaptor.stop(true);

			flvReader.close();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			Thread.sleep(10000);

			List<Muxer> muxerList = muxAdaptor.getMuxerList();
			HLSMuxer hlsMuxer = null;
			Mp4Muxer mp4Muxer = null;
			for (Muxer muxer : muxerList) {
				if (muxer instanceof HLSMuxer) {
					hlsMuxer = (HLSMuxer) muxer;
					break;
				}
			}
			for (Muxer muxer : muxerList) {
				if (muxer instanceof Mp4Muxer) {
					mp4Muxer = (Mp4Muxer) muxer;
					break;
				}
			}
			assertNotNull(hlsMuxer);
			assertNotNull(mp4Muxer);
			File hlsFile = hlsMuxer.getFile();

			String hlsFilePath = hlsFile.getAbsolutePath();
			int lastIndex = hlsFilePath.lastIndexOf(".m3u8");

			String mp4Filename = hlsFilePath.substring(0, lastIndex) + ".mp4";

			//just check mp4 file is not created
			File mp4File = new File(mp4Filename);
			assertTrue(mp4File.exists());

			assertTrue(MuxingTest.testFile(hlsFile.getAbsolutePath()));

			//The setting is given 3 (011) so both enabled true
			assertTrue(hlsMuxer.isUploadingToS3());
			assertTrue(mp4Muxer.isUploadingToS3());

			File dir = new File(hlsFile.getAbsolutePath()).getParentFile();
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts");
				}
			});

			System.out.println("ts file count:" + files.length);

			assertTrue(files.length > 0);
			assertTrue(files.length < (int) Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMp4MuxingWithDirectParams() {
		Vertx vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
		assertNotNull(vertx);

		Mp4Muxer mp4Muxer = new Mp4Muxer(null, vertx, "streams");

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		String streamName = "stream_name_" + (int) (Math.random() * 10000);
		//init
		mp4Muxer.init(appScope, streamName, 0, null, 0);

		//add stream
		int width = 640;
		int height = 480;
		boolean addStreamResult = mp4Muxer.addVideoStream(width, height, null, AV_CODEC_ID_H264, 0, false, null);
		assertTrue(addStreamResult);

		//prepare io
		boolean prepareIOresult = mp4Muxer.prepareIO();
		assertTrue(prepareIOresult);

		try {
			FileInputStream fis = new FileInputStream("src/test/resources/frame0");

			byte[] byteArray = fis.readAllBytes();

			fis.close();

			long now = System.currentTimeMillis();
			ByteBuffer encodedVideoFrame = ByteBuffer.wrap(byteArray);

			for (int i = 0; i < 100; i++) {
				//add packet
				mp4Muxer.writeVideoBuffer(encodedVideoFrame, now + i * 100, 0, 0, true, 0, now + i * 100);
			}

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		//write trailer
		mp4Muxer.writeTrailer();

		Awaitility.await().atMost(20, TimeUnit.SECONDS)
		.pollInterval(1, TimeUnit.SECONDS)
		.until(() -> {
			return MuxingTest.testFile("webapps/junit/streams/" + streamName + ".mp4", 10000);
		});


	}

	/**
	 * Real functional test is under enterprise test repo
	 * It is called testReinitializeEncoderContext
	 */
	@Test
	public void testHLSMuxingWithDirectParams() {
		Vertx vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
		assertNotNull(vertx);

		HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 7, null, false);

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		String streamName = "stream_name_" + (int) (Math.random() * 10000);
		//init
		hlsMuxer.init(appScope, streamName, 0, null, 0);

		hlsMuxer.setId3Enabled(true);


		//add stream
		int width = 640;
		int height = 480;
		boolean addStreamResult = hlsMuxer.addVideoStream(width, height, null, AV_CODEC_ID_H264, 0, false, null);
		assertTrue(addStreamResult);

		//prepare io
		boolean prepareIOresult = hlsMuxer.prepareIO();
		assertTrue(prepareIOresult);

		addStreamResult = hlsMuxer.addVideoStream(width, height, null, AV_CODEC_ID_H264, 0, false, null);
		assertFalse(addStreamResult);

		addStreamResult = hlsMuxer.addVideoStream(width, height, null, AV_CODEC_ID_HCA, 0, false, null);
		assertFalse(addStreamResult);

		try {
			FileInputStream fis = new FileInputStream("src/test/resources/frame0");
			byte[] byteArray = fis.readAllBytes();

			fis.close();

			long now = System.currentTimeMillis();
			ByteBuffer encodedVideoFrame = ByteBuffer.wrap(byteArray);

			AVPacket videoPkt = avcodec.av_packet_alloc();
			av_init_packet(videoPkt);

			for (int i = 0; i < 100; i++) {

				/*
				 * Rotation field is used add metadata to the mp4.
				 * this method is called in directly creating mp4 from coming encoded WebRTC H264 stream
				 */
				videoPkt.stream_index(0);
				videoPkt.pts(now + i * 100);
				videoPkt.dts(now + i * 100);

				encodedVideoFrame.rewind();

				if (i == 0) {
					videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
					String seiData = "test_data";
					hlsMuxer.setSeiData(seiData);
				}
				videoPkt.data(new BytePointer(encodedVideoFrame));
				videoPkt.size(encodedVideoFrame.limit());
				videoPkt.position(0);
				videoPkt.duration(5);
				hlsMuxer.writePacket(videoPkt, new AVCodecContext());

				av_packet_unref(videoPkt);
			}

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		//write trailer
		hlsMuxer.writeTrailer();

	}

	/**
	 * Real functional test is under enterprise test repo
	 * It is called testReinitializeEncoderContext
	 */
	@Test
	public void testRecordMuxingWithDirectParams() {
		Vertx vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
		assertNotNull(vertx);

		Mp4Muxer mp4Muxer = new Mp4Muxer(null, vertx, "streams");

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		String streamName = "stream_name_" + (int) (Math.random() * 10000);
		//init
		mp4Muxer.init(appScope, streamName, 0, null, 0);

		//add stream
		int width = 640;
		int height = 480;
		boolean addStreamResult = mp4Muxer.addVideoStream(width, height, null, AV_CODEC_ID_H264, 0, false, null);
		assertTrue(addStreamResult);

		//prepare io
		boolean prepareIOresult = mp4Muxer.prepareIO();
		assertTrue(prepareIOresult);

		try {
			FileInputStream fis = new FileInputStream("src/test/resources/frame0");
			byte[] byteArray = fis.readAllBytes();

			fis.close();

			long now = System.currentTimeMillis();
			ByteBuffer encodedVideoFrame = ByteBuffer.wrap(byteArray);

			AVPacket videoPkt = avcodec.av_packet_alloc();
			av_init_packet(videoPkt);

			for (int i = 0; i < 100; i++) {

				/*
				 * Rotation field is used add metadata to the mp4.
				 * this method is called in directly creating mp4 from coming encoded WebRTC H264 stream
				 */
				videoPkt.stream_index(0);
				videoPkt.pts(now + i * 100);
				videoPkt.dts(now + i * 100);

				encodedVideoFrame.rewind();

				if (i == 0) {
					videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
				}
				videoPkt.data(new BytePointer(encodedVideoFrame));
				videoPkt.size(encodedVideoFrame.limit());
				videoPkt.position(0);
				videoPkt.duration(5);
				mp4Muxer.writePacket(videoPkt, new AVCodecContext());

				av_packet_unref(videoPkt);
			}

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		//write trailer
		mp4Muxer.writeTrailer();

	}

	@Test
	public void testMp4FinalName() {
		{
			//Scenario 1
			//1. The file does not exist on local disk -> stream1.mp4
			//2. The same file exists on storage -> stream1.mp4
			//3. The uploaded file should be to the storage should be -> stream1_1.mp4

			Vertx vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
			assertNotNull(vertx);
			String streamName = "stream_name_s" + (int) (Math.random() * 10000);
			getAppSettings().setMp4MuxingEnabled(true);
			getAppSettings().setUploadExtensionsToS3(7);
			getAppSettings().setS3RecordingEnabled(true);

			StorageClient client = Mockito.mock(StorageClient.class);
			doReturn(false).when(client).fileExist(Mockito.any());
			doReturn(true).when(client).fileExist("streams/" + streamName + ".mp4");

			if (appScope == null) {
				appScope = (WebScope) applicationContext.getBean("web.scope");
				logger.debug("Application / web scope: {}", appScope);
				assertTrue(appScope.getDepth() == 1);
			}

			//scenario 1
			//1. The file does not exist on local disk -> stream1.mp4
			//2. The same file exists on storage -> stream1.mp4
			//3. The uploaded file should be to the storage should be -> stream1_1.mp4
			{
				Mp4Muxer mp4Muxer = new Mp4Muxer(client, vertx, "streams");
				//init
				mp4Muxer.init(appScope, streamName, 0, null, 0);

				//initialize tmp file
				mp4Muxer.getOutputFormatContext();

				File finalFileName = mp4Muxer.getFinalFileName(true);
				assertTrue(finalFileName.getAbsolutePath().endsWith(streamName + "_1.mp4"));
			}

			//Scenario 2
			//1. The file exists on local disk -> stream1.mp4
			//2. The file does not exist on storage -> stream1.mp4
			//3. The uploaded file should be  -> stream1_1.mp4
			{

				try {
					File file1 = new File("webapps/junit/streams/" + streamName + ".mp4");
					file1.createNewFile();

					doReturn(false).when(client).fileExist("streams/" + streamName + ".mp4");


					Mp4Muxer mp4Muxer = new Mp4Muxer(client, vertx, "streams");
					//init
					mp4Muxer.init(appScope, streamName, 0, null, 0);

					//initialize tmp file
					mp4Muxer.getOutputFormatContext();

					File finalFileName = mp4Muxer.getFinalFileName(true);
					assertTrue(finalFileName.getAbsolutePath().endsWith(streamName + "_1.mp4"));

					finalFileName = mp4Muxer.getFinalFileName(false);
					assertTrue(finalFileName.getAbsolutePath().endsWith(streamName + "_1.mp4"));

					file1.delete();


				} catch (IOException e) {
					e.printStackTrace();
					fail(e.getMessage());
				}


			}

			//Scenario 3
			//1. The file does not exists on local disk -> stream1.mp4
			//2. The file does exist on storage -> stream1.mp4, stream1_1.mp4, stream1_2.mp4
			//3. The uploaded file should be  -> stream1_3.mp4
			{
				doReturn(true).when(client).fileExist("streams/" + streamName + ".mp4");
				doReturn(true).when(client).fileExist("streams/" + streamName + "_1.mp4");
				doReturn(true).when(client).fileExist("streams/" + streamName + "_2.mp4");


				Mp4Muxer mp4Muxer = new Mp4Muxer(client, vertx, "streams");
				//init
				mp4Muxer.init(appScope, streamName, 0, null, 0);

				//initialize tmp file
				mp4Muxer.getOutputFormatContext();

				File finalFileName = mp4Muxer.getFinalFileName(true);
				assertTrue(finalFileName.getAbsolutePath().endsWith(streamName + "_3.mp4"));

				finalFileName = mp4Muxer.getFinalFileName(false);
				assertTrue(finalFileName.getAbsolutePath().endsWith(streamName + ".mp4"));
			}

			//Scenario 4
			//1. The file does not exists on local disk -> stream1.webm
			//2. The file does exist on storage -> stream1.webm, stream1_1.webm, stream1_2.webm
			//3. The uploaded file should be  -> stream1_3.mp4
			{
				doReturn(true).when(client).fileExist("streams/" + streamName + ".webm");
				doReturn(true).when(client).fileExist("streams/" + streamName + "_1.webm");
				doReturn(true).when(client).fileExist("streams/" + streamName + "_2.webm");


				WebMMuxer webMMuxer = new WebMMuxer(client, vertx, "streams");
				//init
				webMMuxer.init(appScope, streamName, 0, null, 0);

				//initialize tmp file
				webMMuxer.getOutputFormatContext();

				File finalFileName = webMMuxer.getFinalFileName(true);
				assertTrue(finalFileName.getAbsolutePath().endsWith(streamName + "_3.webm"));

				finalFileName = webMMuxer.getFinalFileName(false);
				assertTrue(finalFileName.getAbsolutePath().endsWith(streamName + ".webm"));
			}

		}

	}


	@Test
	public void testHLSMuxingWithinChildScope() {

		int hlsTime = 2;
		int hlsListSize = 3;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		appScope.createChildScope("child");


		IScope childScope = appScope.getScope("child");

		childScope.createChildScope("child2");
		IScope childScope2 = childScope.getScope("child2");

		childScope2.createChildScope("child3");
		IScope childScope3 = childScope2.getScope("child3");

		File file = null;
		try {


			file = new File("target/test-classes/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId("test_within_childscope");
			} catch (Exception e) {
				e.printStackTrace();
			}

			muxAdaptor.setBroadcast(broadcast);


			boolean result = muxAdaptor.init(childScope3, "test_within_childscope", false);
			assert (result);

			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);
			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

			muxAdaptor.stop(true);

			flvReader.close();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());
			List<Muxer> muxerList = muxAdaptor.getMuxerList();
			HLSMuxer hlsMuxer = null;
			for (Muxer muxer : muxerList) {
				if (muxer instanceof HLSMuxer) {
					hlsMuxer = (HLSMuxer) muxer;
					break;
				}
			}
			assertNotNull(hlsMuxer);
			File hlsFile = hlsMuxer.getFile();

			String hlsFilePath = hlsFile.getAbsolutePath();
			int lastIndex = hlsFilePath.lastIndexOf(".m3u8");

			String mp4Filename = hlsFilePath.substring(0, lastIndex) + ".mp4";

			//just check mp4 file is not created
			File mp4File = new File(mp4Filename);
			assertFalse(mp4File.exists());

			assertTrue(MuxingTest.testFile(hlsFile.getAbsolutePath()));

			File dir = new File(hlsFile.getAbsolutePath()).getParentFile();
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts");
				}
			});

			System.out.println("ts file count:" + files.length);

			assertTrue(files.length > 0);
			assertTrue(files.length < (int) Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

			//wait to let hls muxer delete ts and m3u8 file

			Awaitility.await().atMost(hlsListSize * hlsTime * 1000 + 3000, TimeUnit.MILLISECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> {
				File[] filesTmp = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".ts") || name.endsWith(".m3u8");
					}
				});
				return 0 == filesTmp.length;
			});


			assertFalse(hlsFile.exists());

			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}


	public void feedMuxAdaptor(FLVReader flvReader, List<MuxAdaptor> muxAdaptorList, StreamCodecInfo info) {
		boolean firstAudioPacketReceived = false;
		boolean firstVideoPacketReceived = false;
		while (flvReader.hasMoreTags()) 
		{
			ITag readTag = flvReader.readTag();
			StreamPacket streamPacket = new StreamPacket(readTag);

			if (!firstAudioPacketReceived && streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) 
			{
				IAudioStreamCodec audioStreamCodec = AudioCodecFactory.getAudioCodec(streamPacket.getData().position(0));
				info.setAudioCodec(audioStreamCodec);
				audioStreamCodec.addData(streamPacket.getData().position(0));
				info.setHasAudio(true);
				firstAudioPacketReceived = true;
			} 
			else if (!firstVideoPacketReceived && streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) 
			{
				IVideoStreamCodec videoStreamCodec = VideoCodecFactory.getVideoCodec(streamPacket.getData().position(0));
				videoStreamCodec.addData(streamPacket.getData().position(0));
				info.setVideoCodec(videoStreamCodec);
				info.setHasVideo(true);
				firstVideoPacketReceived = true;

			}
			for (MuxAdaptor muxAdaptor : muxAdaptorList) {


				if (streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
					VideoData videoData = new VideoData(streamPacket.getData().duplicate().position(0));
					videoData.setTimestamp(streamPacket.getTimestamp());
					videoData.setReceivedTime(System.currentTimeMillis());

					muxAdaptor.packetReceived(null, videoData);

				}
				else {
					CachedEvent event = new CachedEvent();
					event.setData(streamPacket.getData().duplicate());
					event.setDataType(streamPacket.getDataType());
					event.setReceivedTime(System.currentTimeMillis());
					event.setTimestamp(streamPacket.getTimestamp());

					muxAdaptor.packetReceived(null, event);

				}

			}
		}
	}



	@Test
	public void testWriteStreamPacketHEVC() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		Muxer muxer = Mockito.spy(new HLSMuxer(vertx, null, null, 0, null, false));
		muxAdaptor.setEnableVideo(true);

		muxAdaptor.addMuxer(muxer, 0);

		muxAdaptor.setPacketFeeder(new PacketFeeder("streamId"));

		CachedEvent event = new CachedEvent();
		event.setDataType(IoConstants.TYPE_VIDEO);
		event.setExVideoHeader(true);
		event.setReceivedTime(1000);
		long timestamp = 1000;
		event.setTimestamp((int)timestamp);
		event.setData(IoBuffer.allocate(1000));
		event.setExVideoPacketType(ExVideoPacketType.CODED_FRAMES);

		//assume that this decoder configuration file
		muxAdaptor.writeStreamPacket(event);

		muxAdaptor.writeStreamPacket(event);


		//5 + 3 bytes for extended timestamp
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1000-8);
		byteBuffer.position(992);

		Mockito.verify(muxer, Mockito.times(1)).writeVideoBuffer(byteBuffer, timestamp, 0, 
				0, false, 0, timestamp);



		event = new CachedEvent();
		event.setDataType(IoConstants.TYPE_VIDEO);
		event.setExVideoHeader(true);
		event.setReceivedTime(System.currentTimeMillis());
		event.setData(IoBuffer.allocate(500));
		timestamp += 50;
		event.setTimestamp((int)timestamp);
		event.setExVideoPacketType(ExVideoPacketType.CODED_FRAMESX);

		muxAdaptor.writeStreamPacket(event);

		//no extended timestamp
		byteBuffer = ByteBuffer.allocateDirect(1000-5);
		byteBuffer.position(995);

		Mockito.verify(muxer, Mockito.times(1)).writeVideoBuffer(byteBuffer, timestamp, 0, 
				0, false, 0, timestamp);


		//regular file
		event = new CachedEvent();
		event.setDataType(IoConstants.TYPE_VIDEO);
		event.setExVideoHeader(false);
		event.setReceivedTime(System.currentTimeMillis());
		event.setData(IoBuffer.allocate(1000));
		timestamp += 50;
		event.setTimestamp((int)timestamp);

		muxAdaptor.writeStreamPacket(event);

		Mockito.verify(muxer, Mockito.times(1)).writeVideoBuffer(byteBuffer, timestamp, 0, 
				0, false, 0, timestamp);

	}

	@Test
	public void testMuxAdaptorGetVideConf() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);


		IVideoStreamCodec codec = new HEVCVideoEnhancedRTMP();


		byte[] header = new byte[5];
		header[0] = ((byte) 0x80); 
		header[1] = ((byte) (byte)(VideoFourCC.HEVC_FOURCC.value)); 
		header[2] = ((byte) (VideoFourCC.HEVC_FOURCC.value >> 8)); 
		header[3] = ((byte) (VideoFourCC.HEVC_FOURCC.value >> 16)); 
		header[4] = ((byte) (VideoFourCC.HEVC_FOURCC.value >> 24)); 


		IoBuffer buffer = IoBuffer.wrap(header);
		buffer.setAutoExpand(true);
		buffer.position(5);
		buffer.put(HEVCDecoderConfigurationParserTest.HEVC_DECODER_CONFIGURATION);
		buffer.rewind();

		codec.addData(buffer);

		info.setVideoCodec(codec);

		muxAdaptor.setEnableVideo(true);
		muxAdaptor.getVideoDataConf(info);
		assertEquals(AV_CODEC_ID_H265, muxAdaptor.getVideoCodecId());

		AVCodecParameters videoCodecParameters = muxAdaptor.getVideoCodecParameters();
		assertEquals(1920, videoCodecParameters.width());
		assertEquals(1080, videoCodecParameters.height());


		//test with null codec
		muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);
		muxAdaptor.setEnableVideo(true);

		info.setVideoCodec(Mockito.mock(AbstractVideo.class));
		muxAdaptor.getVideoDataConf(info);

		assertEquals(-1, muxAdaptor.getVideoCodecId());


		muxAdaptor.setVideoDataConf(HEVCDecoderConfigurationParserTest.HEVC_DECODER_CONFIGURATION);

		try {
			videoCodecParameters = muxAdaptor.getVideoCodecParameters();
			fail("It should throw exception");
		}
		catch (Exception e) {

		}
	}

	@Test
	public void testSimpleGetterSetters() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		assertEquals(0, muxAdaptor.getDurationMs());
		assertEquals(0, muxAdaptor.getTotalByteReceived());

		muxAdaptor.setDurationMs(1000);
		muxAdaptor.setTotalByteReceived(1000);
		assertEquals(1000, muxAdaptor.getDurationMs());
		assertEquals(1000, muxAdaptor.getTotalByteReceived());

	}

	@Test
	public void testStreamSpeed() throws IOException {

		int hlsListSize = 3;
		int hlsTime = 2;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));
		getAppSettings().setUploadExtensionsToS3(2);


		String name = "stream_id_speed";

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		File file = null;


		file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
		final FLVReader flvReader = new FLVReader(file);

		logger.info("f path:" + file.getAbsolutePath());
		assertTrue(file.exists());
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(name);
		} catch (Exception e) {
			e.printStackTrace();
		}

		muxAdaptor.setBroadcast(broadcast);
		boolean result = muxAdaptor.init(appScope, name, false);
		assert (result);

		muxAdaptor.start();		


		long packetSize = 0;

		boolean firstAudioPacketReceived = false;
		boolean firstVideoPacketReceived = false;
		while (flvReader.hasMoreTags()) {
			ITag readTag = flvReader.readTag();
			StreamPacket streamPacket = new StreamPacket(readTag);
			if (!firstAudioPacketReceived && streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) {
				IAudioStreamCodec audioStreamCodec = AudioCodecFactory.getAudioCodec(streamPacket.getData().position(0));
				info.setAudioCodec(audioStreamCodec);
				audioStreamCodec.addData(streamPacket.getData().position(0));
				info.setHasAudio(true);
				firstAudioPacketReceived = true;
			} else if (!firstVideoPacketReceived && streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
				IVideoStreamCodec videoStreamCodec = VideoCodecFactory.getVideoCodec(streamPacket.getData().position(0));
				videoStreamCodec.addData(streamPacket.getData().position(0));
				info.setVideoCodec(videoStreamCodec);
				info.setHasVideo(true);
				firstVideoPacketReceived = true;

			}

			if (streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
				VideoData videoData = new VideoData(streamPacket.getData().duplicate().position(0));
				videoData.setTimestamp(streamPacket.getTimestamp());
				videoData.setReceivedTime(System.currentTimeMillis());

				muxAdaptor.packetReceived(null, videoData);

			}

			else {
				CachedEvent event = new CachedEvent();
				event.setData(streamPacket.getData().duplicate());
				event.setDataType(streamPacket.getDataType());
				event.setReceivedTime(System.currentTimeMillis());
				event.setTimestamp(streamPacket.getTimestamp());

				muxAdaptor.packetReceived(null, event);

			}



			packetSize++;
			if (packetSize > 10000)	
			{
				break;
			}
		}



		Awaitility.await().atMost(200, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());
		logger.info("----- 1. speed:{}", muxAdaptor.getLatestSpeed());

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> 50  < muxAdaptor.getLatestSpeed());


		packetSize = 0;
		while (flvReader.hasMoreTags()) {
			ITag readTag = flvReader.readTag();
			StreamPacket streamPacket = new StreamPacket(readTag);
			if (!firstAudioPacketReceived && streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) {
				IAudioStreamCodec audioStreamCodec = AudioCodecFactory.getAudioCodec(streamPacket.getData().position(0));
				info.setAudioCodec(audioStreamCodec);
				audioStreamCodec.addData(streamPacket.getData().position(0));
				info.setHasAudio(true);
				firstAudioPacketReceived = true;
			} else if (!firstVideoPacketReceived && streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
				IVideoStreamCodec videoStreamCodec = VideoCodecFactory.getVideoCodec(streamPacket.getData().position(0));
				videoStreamCodec.addData(streamPacket.getData().position(0));
				info.setVideoCodec(videoStreamCodec);
				info.setHasVideo(true);
				firstVideoPacketReceived = true;

			}

			if (streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
				VideoData videoData = new VideoData(streamPacket.getData().duplicate().position(0));
				videoData.setTimestamp(streamPacket.getTimestamp());
				videoData.setReceivedTime(System.currentTimeMillis());

				muxAdaptor.packetReceived(null, videoData);

			}

			else {
				CachedEvent event = new CachedEvent();
				event.setData(streamPacket.getData().duplicate());
				event.setDataType(streamPacket.getDataType());
				event.setReceivedTime(System.currentTimeMillis());
				event.setTimestamp(streamPacket.getTimestamp());

				muxAdaptor.packetReceived(null, event);

			}


			packetSize++;
			if (packetSize > 300)	
			{
				break;
			}

			try {
				//slow down the process to check the speed
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (muxAdaptor.getLatestSpeed() < 0.7) {
				//break the loop if the speed is less than 0.7
				break;
			}
		}


		logger.info("----- 2. speed:{}", muxAdaptor.getLatestSpeed());

		Awaitility.await().atMost(200, TimeUnit.SECONDS).until(() -> 0.7 > muxAdaptor.getLatestSpeed());


		muxAdaptor.stop(true);

		flvReader.close();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());


		getAppSettings().setDeleteHLSFilesOnEnded(false);

	}


	@Test
	public void testHLSNaming() {
		HLSMuxer hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "", 7, null, false);
		appScope = (WebScope) applicationContext.getBean("web.scope");
		hlsMuxer.init(appScope, "test", 0, "", 100);
		assertEquals("./webapps/junit/streams/test%09d.ts", hlsMuxer.getSegmentFilename());

		hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "", 7, null, false);
		hlsMuxer.init(appScope, "test", 0, "", 0);
		assertEquals("./webapps/junit/streams/test%09d.ts", hlsMuxer.getSegmentFilename());

		hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "", 7, null, false);
		hlsMuxer.init(appScope, "test", 300, "", 0);
		assertEquals("./webapps/junit/streams/test_300p%09d.ts", hlsMuxer.getSegmentFilename());


		hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "", 7, null, false);
		hlsMuxer.init(appScope, "test", 300, "", 400000);
		assertEquals("./webapps/junit/streams/test_300p400kbps%09d.ts", hlsMuxer.getSegmentFilename());

		getAppSettings().setHlsSegmentFileSuffixFormat("-%Y%m%d-%s");
		hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "", 7, null, false);
		hlsMuxer.init(appScope, "test", 0, "", 0);
		assertEquals("./webapps/junit/streams/test-%Y%m%d-%s.ts", hlsMuxer.getSegmentFilename());


	}

	public void testHLSMuxing(String name) {

		//av_log_set_level (40);
		int hlsListSize = 3;
		int hlsTime = 2;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));
		getAppSettings().setUploadExtensionsToS3(2);


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		File file = null;
		try {

			file = new File("src/test/resources/test.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.info("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId(name);
			} catch (Exception e) {
				e.printStackTrace();
			}

			muxAdaptor.setBroadcast(broadcast);
			boolean result = muxAdaptor.init(appScope, name, false);
			assert (result);

			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

			Awaitility.await().atMost(200, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());


			HLSMuxer hlsMuxer = null;
			{
				List<Muxer> muxerList = muxAdaptor.getMuxerList();

				for (Muxer muxer : muxerList) {
					if (muxer instanceof HLSMuxer) {
						hlsMuxer = (HLSMuxer) muxer;
						break;
					}
				}
				assertNotNull(hlsMuxer);
				//Call it separately for an unexpected case. It increases coverage and it checks not to crash
				hlsMuxer.prepareIO();
			}

			muxAdaptor.stop(true);

			flvReader.close();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());


			File hlsFile = hlsMuxer.getFile();

			String hlsFilePath = hlsFile.getAbsolutePath();
			int lastIndex = hlsFilePath.lastIndexOf(".m3u8");

			String mp4Filename = hlsFilePath.substring(0, lastIndex) + ".mp4";

			//just check mp4 file is not created
			File mp4File = new File(mp4Filename);
			assertFalse(mp4File.exists());

			assertTrue(MuxingTest.testFile(hlsFile.getAbsolutePath()));

			File dir = new File(hlsFile.getAbsolutePath()).getParentFile();
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts");
				}
			});

			System.out.println("ts file count:" + files.length);

			assertTrue(files.length > 0);
			assertTrue(files.length < (int) Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

			//wait to let hls muxer delete ts and m3u8 file

			Awaitility.await().atMost(hlsListSize * hlsTime * 1000 + 3000, TimeUnit.MILLISECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> {
				File[] filesTmp = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".ts") || name.endsWith(".m3u8");
					}
				});
				return 0 == filesTmp.length;
			});


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		getAppSettings().setDeleteHLSFilesOnEnded(false);

	}

	@Test
	public void testDontSendPacketsForTimeoutPeriod() throws IOException {
		//av_log_set_level (40);
		int hlsListSize = 3;
		int hlsTime = 2;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		File file = null;


		file = new File("target/test-classes/test_video_360p_subtitle.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
		final FLVReader flvReader = new FLVReader(file);

		logger.info("f path: {}", file.getAbsolutePath());
		assertTrue(file.exists());
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId("hls_video_subtitle");
		} catch (Exception e) {
			e.printStackTrace();
		}

		broadcast.setListenerHookURL("any_url");

		muxAdaptor.setBroadcast(broadcast);


		boolean result = muxAdaptor.init(appScope, "hls_video_subtitle", false);
		assert (result);


		muxAdaptor.getDataStore().save(broadcast);

		muxAdaptor.start();

		Application.resetFields();

		feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

		Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

		Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> {
			return IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(muxAdaptor.getDataStore().get(broadcast.getStreamId()).getStatus());
		});

		//wait about time period 
		Awaitility.await().atMost(AntMediaApplicationAdapter.STREAM_TIMEOUT_MS+5000, TimeUnit.MILLISECONDS).until(() -> {
			return IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED.equals(muxAdaptor.getDataStore().get(broadcast.getStreamId()).getStatus());
		});


		assertFalse(muxAdaptor.isRecording());

		flvReader.close();


		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

		assertTrue(Application.notifyHookAction.contains(AntMediaApplicationAdapter.HOOK_ACTION_END_LIVE_STREAM));

	}


	@Test
	public void testHLSMuxingWithSubtitle() {

		//av_log_set_level (40);
		int hlsListSize = 3;
		int hlsTime = 2;

		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setAddDateTimeToMp4FileName(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setDeleteHLSFilesOnEnded(true);
		getAppSettings().setHlsTime(String.valueOf(hlsTime));
		getAppSettings().setHlsListSize(String.valueOf(hlsListSize));


		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		File file = null;
		try {

			file = new File("target/test-classes/test_video_360p_subtitle.flv"); //ResourceUtils.getFile(this.getClass().getResource("test.flv"));
			final FLVReader flvReader = new FLVReader(file);

			logger.info("f path: {}", file.getAbsolutePath());
			assertTrue(file.exists());
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId("hls_video_subtitle");
			} catch (Exception e) {
				e.printStackTrace();
			}

			muxAdaptor.setBroadcast(broadcast);

			boolean result = muxAdaptor.init(appScope, "hls_video_subtitle", false);
			assert (result);

			muxAdaptor.start();

			feedMuxAdaptor(flvReader, Arrays.asList(muxAdaptor), info);

			Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> muxAdaptor.isRecording());

			muxAdaptor.stop(true);

			flvReader.close();

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			// delete job in the list
			List<Muxer> muxerList = muxAdaptor.getMuxerList();
			HLSMuxer hlsMuxer = null;
			for (Muxer muxer : muxerList) {
				if (muxer instanceof HLSMuxer) {
					hlsMuxer = (HLSMuxer) muxer;
					break;
				}
			}
			assertNotNull(hlsMuxer);
			File hlsFile = hlsMuxer.getFile();

			String hlsFilePath = hlsFile.getAbsolutePath();
			int lastIndex = hlsFilePath.lastIndexOf(".m3u8");

			String mp4Filename = hlsFilePath.substring(0, lastIndex) + ".mp4";

			//just check mp4 file is not created
			File mp4File = new File(mp4Filename);
			assertFalse(mp4File.exists());

			assertTrue(MuxingTest.testFile(hlsFile.getAbsolutePath()));

			File dir = new File(hlsFile.getAbsolutePath()).getParentFile();
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts");
				}
			});

			System.out.println("ts file count:" + files.length);

			assertTrue(files.length > 0);

			logger.info("hls list:{}", (int) Integer.valueOf(hlsMuxer.getHlsListSize()));

			logger.info("hls time:{}", (int) Integer.valueOf(hlsMuxer.getHlsTime()));

			assertTrue(files.length < (int) Integer.valueOf(hlsMuxer.getHlsListSize()) * (Integer.valueOf(hlsMuxer.getHlsTime()) + 1));

			//wait to let hls muxer delete ts and m3u8 file
			Awaitility.await().atMost(hlsListSize * hlsTime * 1000 + 3000, TimeUnit.MILLISECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> {
				File[] filesTmp = dir.listFiles(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".ts") || name.endsWith(".m3u8");
					}
				});
				return 0 == filesTmp.length;
			});


			assertFalse(hlsFile.exists());

			files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".ts") || name.endsWith(".m3u8");
				}
			});

			assertEquals(0, files.length);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	public AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	public DataStore getDataStore() {
		if (datastore == null) {
			datastore = ((DataStoreFactory) applicationContext.getBean(DataStoreFactory.BEAN_NAME)).getDataStore();
		}
		return datastore;
	}

	@Test
	public void testNotifyMetadata() 
	{
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		appScope = (WebScope) applicationContext.getBean("web.scope");

		MuxAdaptor muxAdaptor = spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope));


		Notify notify = mock(Notify.class);
		when(notify.getData()).thenReturn(IoBuffer.allocate(100));
		when(notify.getAction()).thenReturn("NOT_onMetaData");

		Muxer muxer = mock(Muxer.class);
		muxAdaptor.addMuxer(muxer, 0);
		Input input = mock(Input.class);

		muxAdaptor.notifyMetaDataReceived(notify, 0);

		//verify that it's not called because action is not onMetaData
		verify(muxer, never()).writeMetaData(anyString(), anyLong());

		when(input.readDataType()).thenReturn(DataTypes.CORE_NUMBER);
		when(input.readMap()).thenReturn(new HashMap<>());

		when(notify.getAction()).thenReturn("onMetaData");
		when(notify.getData()).thenReturn(IoBuffer.allocate(100));

		muxAdaptor.notifyMetaDataReceived(notify, 0);
		//verify that it's not called because data type is not as expected
		verify(muxer, never()).writeMetaData(anyString(), anyLong());


		when(input.readDataType()).thenReturn(DataTypes.CORE_MAP);
		doReturn(input).when(muxAdaptor).getInput(any());
		muxAdaptor.notifyMetaDataReceived(notify, 0);
		verify(muxer, never()).writeMetaData(anyString(), anyLong());


		when(input.readMap()).thenReturn(new HashMap<>());
		muxAdaptor.notifyMetaDataReceived(notify, 0);
		verify(muxer, never()).writeMetaData(anyString(), anyLong());


		when(notify.getData()).thenReturn(IoBuffer.allocate(100));
		Map<String, String> map = new HashMap<>();
		map.put("streamId", "streamId");
		map.put("name", "streamId");
		when(input.readMap()).thenReturn(map);

		muxAdaptor.notifyMetaDataReceived(notify, 0);
		verify(muxer, times(1)).writeMetaData(anyString(), anyLong());

	}

	@Test
	public void testRelayRTMPMetadata() {

		String streamId = "testRelayRTMPMetadata";
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope));
		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setHlsMuxingEnabled(true);
		getAppSettings().setId3TagEnabled(true);

		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			file = new File("target/test-classes/test.flv");

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId(streamId);
			} catch (Exception e) {
				e.printStackTrace();
			}

			muxAdaptor.setBroadcast(broadcast);
			boolean result = muxAdaptor.init(appScope, streamId, false);

			assertTrue(result);

			HLSMuxer hlsMuxer = mock(HLSMuxer.class);
			when(hlsMuxer.prepareIO()).thenReturn(true);
			muxAdaptor.addMuxer(hlsMuxer, 0);

			//
			Input input = mock(Input.class);
			when(input.readDataType()).thenReturn(DataTypes.CORE_MAP);

			Map<String, String> map = new HashMap<>();
			map.put("streamId", streamId);
			map.put("name", streamId);
			when(input.readMap()).thenReturn(map);



			doReturn(input).when(muxAdaptor).getInput(any());


			muxAdaptor.start();
			logger.info("2");
			int lastTimeStamp = 0;
			boolean firstAudioPacketReceived = false;
			boolean firstVideoPacketReceived = false;
			ArrayList<Integer> timeStamps = new ArrayList<>();


			while (flvReader.hasMoreTags()) {

				ITag readTag = flvReader.readTag();

				StreamPacket streamPacket = new StreamPacket(readTag);
				lastTimeStamp = streamPacket.getTimestamp();


				timeStamps.add(lastTimeStamp);
				if (!firstAudioPacketReceived && streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) {
					System.out.println("audio configuration received");
					IAudioStreamCodec audioStreamCodec = AudioCodecFactory.getAudioCodec(streamPacket.getData().position(0));
					info.setAudioCodec(audioStreamCodec);
					audioStreamCodec.addData(streamPacket.getData().position(0));
					info.setHasAudio(true);

					firstAudioPacketReceived = true;
				} else if (!firstVideoPacketReceived && streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
					System.out.println("video configuration received");
					IVideoStreamCodec videoStreamCodec = VideoCodecFactory.getVideoCodec(streamPacket.getData().position(0));
					videoStreamCodec.addData(streamPacket.getData().position(0));
					info.setVideoCodec(videoStreamCodec);
					IoBuffer decoderConfiguration = info.getVideoCodec().getDecoderConfiguration();
					logger.info("decoder configuration:" + decoderConfiguration);
					info.setHasVideo(true);

					firstVideoPacketReceived = true;

				}

				if (streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
					VideoData videoData = new VideoData(streamPacket.getData().duplicate().position(0));
					videoData.setTimestamp(streamPacket.getTimestamp());
					videoData.setReceivedTime(System.currentTimeMillis());

					muxAdaptor.packetReceived(null, videoData);

				}

				else if (streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA){
					CachedEvent event = new CachedEvent();
					event.setData(streamPacket.getData().duplicate());
					event.setDataType(streamPacket.getDataType());
					event.setReceivedTime(System.currentTimeMillis());
					event.setTimestamp(streamPacket.getTimestamp());

					muxAdaptor.packetReceived(null, event);

				}
				else if (streamPacket.getDataType() == Constants.TYPE_STREAM_METADATA)
				{
					logger.info("stream metadata received");
					Notify notify = new Notify(streamPacket.getData().duplicate());
					notify.setAction("onMetaData");
					notify.setTimestamp(streamPacket.getTimestamp());

					muxAdaptor.packetReceived(null, notify);
				}
			}

			verify(hlsMuxer, atLeast(1)).writeMetaData(eq(new JSONObject(map).toJSONString()), anyLong());

			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(muxAdaptor::isRecording);

			assertTrue(muxAdaptor.isRecording());

			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.getInputQueueSize() == 0);

			muxAdaptor.stop(true);

			flvReader.close();

			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			assertFalse(muxAdaptor.isRecording());




		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
		logger.info("leaving testRecording");

	}

	@Test
	public void testRecording() {
		testRecording("dasss", true);
	}

	public void testRecording(String name, boolean checkDuration) {
		logger.info("running testMp4Muxing");

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);
		getAppSettings().setMp4MuxingEnabled(false);
		getAppSettings().setHlsMuxingEnabled(false);

		logger.info("HLS muxing enabled {}", appSettings.isHlsMuxingEnabled());

		//File file = new File(getResource("test.mp4").getFile());
		File file = null;

		try {

			file = new File("target/test-classes/test.flv");

			final FLVReader flvReader = new FLVReader(file);

			logger.debug("f path:" + file.getAbsolutePath());
			assertTrue(file.exists());

			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId(name);
			} catch (Exception e) {
				e.printStackTrace();
			}

			muxAdaptor.setBroadcast(broadcast);
			boolean result = muxAdaptor.init(appScope, name, false);

			assertTrue(result);


			muxAdaptor.start();
			logger.info("2");
			int packetNumber = 0;
			int lastTimeStamp = 0;
			int startOfRecordingTimeStamp = 0;
			boolean firstAudioPacketReceived = false;
			boolean firstVideoPacketReceived = false;
			ArrayList<Integer> timeStamps = new ArrayList<>();
			HLSMuxer hlsMuxer = null;
			while (flvReader.hasMoreTags()) {

				ITag readTag = flvReader.readTag();

				StreamPacket streamPacket = new StreamPacket(readTag);
				lastTimeStamp = streamPacket.getTimestamp();

				if (packetNumber == 0) {
					logger.info("timeStamp 1 " + streamPacket.getTimestamp());
				}
				timeStamps.add(lastTimeStamp);
				if (!firstAudioPacketReceived && streamPacket.getDataType() == Constants.TYPE_AUDIO_DATA) {
					System.out.println("audio configuration received");
					IAudioStreamCodec audioStreamCodec = AudioCodecFactory.getAudioCodec(streamPacket.getData().position(0));
					info.setAudioCodec(audioStreamCodec);
					audioStreamCodec.addData(streamPacket.getData().position(0));
					info.setHasAudio(true);

					firstAudioPacketReceived = true;
				} else if (!firstVideoPacketReceived && streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
					System.out.println("video configuration received");
					IVideoStreamCodec videoStreamCodec = VideoCodecFactory.getVideoCodec(streamPacket.getData().position(0));
					videoStreamCodec.addData(streamPacket.getData().position(0));
					info.setVideoCodec(videoStreamCodec);
					IoBuffer decoderConfiguration = info.getVideoCodec().getDecoderConfiguration();
					logger.info("decoder configuration:" + decoderConfiguration);
					info.setHasVideo(true);

					firstVideoPacketReceived = true;

				}

				if (streamPacket.getDataType() == Constants.TYPE_VIDEO_DATA) {
					VideoData videoData = new VideoData(streamPacket.getData().duplicate().position(0));
					videoData.setTimestamp(streamPacket.getTimestamp());
					videoData.setReceivedTime(System.currentTimeMillis());

					muxAdaptor.packetReceived(null, videoData);

				}

				else {
					CachedEvent event = new CachedEvent();
					event.setData(streamPacket.getData().duplicate());
					event.setDataType(streamPacket.getDataType());
					event.setReceivedTime(System.currentTimeMillis());
					event.setTimestamp(streamPacket.getTimestamp());

					muxAdaptor.packetReceived(null, event);

				}


				if (packetNumber == 40000) {
					logger.info("----input queue size: {}", muxAdaptor.getInputQueueSize());
					Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> 
					{ 
						logger.info("----> input queue size: {}", muxAdaptor.getInputQueueSize());
						return muxAdaptor.getInputQueueSize() == 0 ;
					});
					logger.info("----input queue size: {}", muxAdaptor.getInputQueueSize());
					startOfRecordingTimeStamp = streamPacket.getTimestamp();
					assertTrue(muxAdaptor.startRecording(RecordType.MP4, 0) != null);
					hlsMuxer = new HLSMuxer(vertx, null, null, 0, null, false);

					assertTrue(muxAdaptor.addMuxer(hlsMuxer));
					assertFalse(muxAdaptor.addMuxer(hlsMuxer));

				}
				packetNumber++;

				if (packetNumber % 3000 == 0) {
					logger.info("packetNumber " + packetNumber);
				}
			}

			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(muxAdaptor::isRecording);

			assertTrue(muxAdaptor.isRecording());
			final String finalFilePath = muxAdaptor.getMuxerList().get(0).getFile().getAbsolutePath();

			int inputQueueSize = muxAdaptor.getInputQueueSize();
			logger.info("----input queue size before stop recording: {}", inputQueueSize);
			Awaitility.await().atMost(90, TimeUnit.SECONDS).until(() -> muxAdaptor.getInputQueueSize() == 0);

			inputQueueSize = muxAdaptor.getInputQueueSize();
			int estimatedLastTimeStamp = lastTimeStamp;
			if (inputQueueSize > 0) {
				estimatedLastTimeStamp = timeStamps.get((timeStamps.size() - inputQueueSize));
			}
			assertTrue(muxAdaptor.stopRecording(RecordType.MP4, 0) != null);

			assertTrue(muxAdaptor.removeMuxer(hlsMuxer));
			assertFalse(muxAdaptor.removeMuxer(hlsMuxer));

			muxAdaptor.stop(true);

			flvReader.close();

			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(() -> !muxAdaptor.isRecording());

			assertFalse(muxAdaptor.isRecording());

			assertTrue(MuxingTest.testFile(finalFilePath, estimatedLastTimeStamp - startOfRecordingTimeStamp));

			assertTrue(MuxingTest.testFile(hlsMuxer.getFile().getAbsolutePath()));


		} catch (Exception e) {
			e.printStackTrace();
			fail("exception:" + e);
		}
		logger.info("leaving testRecording");
	}


	@Test
	public void testRemux() {

		String input = "src/test/resources/test_video_360p.flv";
		String rotated = "rotated.mp4";

		Mp4Muxer.remux(input, rotated, 90);
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();

		int ret;
		if (inputFormatContext == null) {
			System.out.println("cannot allocate input context");
		}

		if ((ret = avformat_open_input(inputFormatContext, rotated, null, (AVDictionary) null)) < 0) {
			System.out.println("cannot open input context: " + rotated);
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			System.out.println("Could not find stream information\n");
		}

		int streamCount = inputFormatContext.nb_streams();

		for (int i = 0; i < streamCount; i++) {
			AVCodecParameters codecpar = inputFormatContext.streams(i).codecpar();
			if (codecpar.codec_type() == AVMEDIA_TYPE_VIDEO) {
				AVStream videoStream = inputFormatContext.streams(i);

				SizeTPointer size = new SizeTPointer(1);
				BytePointer displayMatrixBytePointer = av_stream_get_side_data(videoStream, avcodec.AV_PKT_DATA_DISPLAYMATRIX, size);
				//it should be 36 because it's a 3x3 integer(size=4). 
				assertEquals(36, size.get());

				IntPointer displayPointerIntPointer = new IntPointer(displayMatrixBytePointer);
				//it gets counter clockwise
				int rotation = (int) -(avutil.av_display_rotation_get(displayPointerIntPointer));

				assertEquals(90, rotation);
			}
		}

		avformat_close_input(inputFormatContext);

		new File(rotated).delete();

	}

	@Test
	public void testMp4MuxingWithSameNameWhileRecording() {

		/*
		 * In this test we create such a case with spy Files
		 * In record directory
		 * test.mp4						existing
		 * test_1.mp4 					non-existing
		 * test_2.mp4 					non-existing
		 * test.mp4.tmp_extension		non-existing
		 * test_1.mp4.tmp_extension		existing
		 * test_2.mp4.tmp_extension		non-existing
		 *
		 * So we check new record file must be temp_2.mp4
		 */

		String streamId = "test";
		Muxer mp4Muxer = spy(new Mp4Muxer(null, null, "streams"));
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		File parent = mock(File.class);
		when(parent.exists()).thenReturn(true);

		File existingFile = spy(new File(streamId + ".mp4"));
		doReturn(true).when(existingFile).exists();
		doReturn(parent).when(existingFile).getParentFile();

		File nonExistingFile_1 = spy(new File(streamId + "_1.mp4"));
		doReturn(false).when(nonExistingFile_1).exists();

		File nonExistingFile_2 = spy(new File(streamId + "_2.mp4"));
		doReturn(false).when(nonExistingFile_2).exists();


		File nonExistingTempFile = spy(new File(streamId + ".mp4" + Muxer.TEMP_EXTENSION));
		doReturn(true).when(nonExistingTempFile).exists();

		File existingTempFile_1 = spy(new File(streamId + "_1.mp4" + Muxer.TEMP_EXTENSION));
		doReturn(true).when(existingTempFile_1).exists();

		File nonExistingTempFile_2 = spy(new File(streamId + "_2.mp4" + Muxer.TEMP_EXTENSION));
		doReturn(false).when(nonExistingTempFile_2).exists();

		doReturn(existingFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4"), eq(null));
		doReturn(nonExistingFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId + "_1"), eq(".mp4"), eq(null));
		doReturn(nonExistingFile_2).when(mp4Muxer).getResourceFile(any(), eq(streamId + "_2"), eq(".mp4"), eq(null));

		doReturn(nonExistingTempFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4" + Muxer.TEMP_EXTENSION), eq(null));
		doReturn(existingTempFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId + "_1"), eq(".mp4" + Muxer.TEMP_EXTENSION), eq(null));
		doReturn(nonExistingTempFile_2).when(mp4Muxer).getResourceFile(any(), eq(streamId + "_2"), eq(".mp4" + Muxer.TEMP_EXTENSION), eq(null));

		mp4Muxer.init(appScope, streamId, 0, false, null, 0);

		assertEquals(nonExistingFile_2, mp4Muxer.getFile());

	}

	@Test
	public void testGetExtendedName() {
		Muxer mp4Muxer = spy(new Mp4Muxer(null, null, "streams"));

		assertEquals("test_400p", mp4Muxer.getExtendedName("test", 400, 1000000, ""));
		
		//this is the assertion for this fix https://github.com/ant-media/Ant-Media-Server/issues/7079
		assertNotEquals(0, mp4Muxer.getCurrentVoDTimeStamp());
		
		assertEquals("test_400p1000kbps", mp4Muxer.getExtendedName("test", 400, 1000000, "%r%b"));
		assertEquals("test_1000kbps", mp4Muxer.getExtendedName("test", 400, 1000000, "%b"));
		assertEquals("test_400p", mp4Muxer.getExtendedName("test", 400, 1000000, "%r"));
		assertEquals("test", mp4Muxer.getExtendedName("test", 0, 1000000, "%r"));
		assertEquals("test_1000kbps", mp4Muxer.getExtendedName("test", 0, 1000000, "%b"));
		assertEquals("test_1000kbps", mp4Muxer.getExtendedName("test", 0, 1000000, "%r%b"));
		assertEquals("test", mp4Muxer.getExtendedName("test", 400, 10, "%b"));
		assertEquals("test_400p", mp4Muxer.getExtendedName("test", 400, 10, "%r"));
		assertEquals("test_400p", mp4Muxer.getExtendedName("test", 400, 10, "%r%b"));
		assertEquals("test_1000kbps400p", mp4Muxer.getExtendedName("test", 400, 1000000, "%b%r"));
		assertEquals("test_1000kbps", mp4Muxer.getExtendedName("test", 0, 1000000, "%b%r"));
		assertEquals("test_400p", mp4Muxer.getExtendedName("test", 400, 0, "%b%r"));

		String customText = "{customText}";

		assertEquals("test_400p1000kbpscustomText", mp4Muxer.getExtendedName("test", 400, 1000000, "%r%b" + customText));
		assertEquals("test_customText400p1000kbps", mp4Muxer.getExtendedName("test", 400, 1000000, customText + "%r%b"));
		assertEquals("test_400pcustomText1000kbps", mp4Muxer.getExtendedName("test", 400, 1000000, "%r" + customText + "%b"));

		assertEquals("test_1000kbps400pcustomText", mp4Muxer.getExtendedName("test", 400, 1000000, "%b%r" + customText));
		assertEquals("test_customText1000kbps400p", mp4Muxer.getExtendedName("test", 400, 1000000, customText + "%b%r"));
		assertEquals("test_1000kbpscustomText400p", mp4Muxer.getExtendedName("test", 400, 1000000, "%b" + customText + "%r"));

		assertEquals("test_400pcustomText", mp4Muxer.getExtendedName("test", 400, 10, "%r%b" + customText));
		assertEquals("test_customText400p", mp4Muxer.getExtendedName("test", 400, 10, customText + "%r%b"));
		assertEquals("test_400pcustomText", mp4Muxer.getExtendedName("test", 400, 10, "%r" + customText + "%b"));

		assertEquals("test_400pcustomText", mp4Muxer.getExtendedName("test", 400, 10, "%b%r" + customText));
		assertEquals("test_customText400p", mp4Muxer.getExtendedName("test", 400, 10, customText + "%b%r"));
		assertEquals("test_customText400p", mp4Muxer.getExtendedName("test", 400, 10, "%b" + customText + "%r"));

		assertEquals("test_1000kbpscustomText", mp4Muxer.getExtendedName("test", 0, 1000000, "%r%b" + customText));
		assertEquals("test_customText1000kbps", mp4Muxer.getExtendedName("test", 0, 1000000, customText + "%r%b"));
		assertEquals("test_customText1000kbps", mp4Muxer.getExtendedName("test", 0, 1000000, "%r" + customText + "%b"));

		assertEquals("test_1000kbpscustomText", mp4Muxer.getExtendedName("test", 0, 1000000, "%b%r" + customText));
		assertEquals("test_customText1000kbps", mp4Muxer.getExtendedName("test", 0, 1000000, customText + "%b%r"));
		assertEquals("test_1000kbpscustomText", mp4Muxer.getExtendedName("test", 0, 1000000, "%b" + customText + "%r"));

		assertEquals("test_400pcustomText", mp4Muxer.getExtendedName("test", 400, 1000000, "%r"+customText));
		assertEquals("test_customText400p", mp4Muxer.getExtendedName("test", 400, 1000000, customText+"%r"));

		assertEquals("test_400pcustomText", mp4Muxer.getExtendedName("test", 400, 10, "%r"+customText));
		assertEquals("test_customText400p", mp4Muxer.getExtendedName("test", 400, 10, customText+"%r"));

		assertEquals("test_1000kbpscustomText", mp4Muxer.getExtendedName("test", 400, 1000000, "%b"+customText));
		assertEquals("test_customText1000kbps", mp4Muxer.getExtendedName("test", 400, 1000000, customText+"%b"));

		assertEquals("test_customText", mp4Muxer.getExtendedName("test", 400, 10, "%b"+customText));
		assertEquals("test_customText", mp4Muxer.getExtendedName("test", 400, 10, customText+"%b"));
		assertEquals("test_customText", mp4Muxer.getExtendedName("test", 400, 10, customText));

	}

	@Test
	public void testMp4MuxingWhileTempFileExist() {

		/*
		 * In this test we create such a case with spy Files
		 * In record directory
		 * test.mp4						non-existing
		 * test_1.mp4 					non-existing
		 * test.mp4.tmp_extension		existing
		 * test_1.mp4.tmp_extension		non-existing
		 *
		 * So we check new record file must be temp_1.mp4
		 */

		String streamId = "test";
		Muxer mp4Muxer = spy(new Mp4Muxer(null, null, "streams"));
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		File parent = mock(File.class);
		when(parent.exists()).thenReturn(true);

		File nonExistingFile = spy(new File(streamId + ".mp4"));
		doReturn(false).when(nonExistingFile).exists();
		doReturn(parent).when(nonExistingFile).getParentFile();

		File nonExistingFile_1 = spy(new File(streamId + "_1.mp4"));
		doReturn(false).when(nonExistingFile_1).exists();

		File existingTempFile = spy(new File(streamId + ".mp4" + Muxer.TEMP_EXTENSION));
		doReturn(true).when(existingTempFile).exists();

		File nonExistingTempFile_1 = spy(new File(streamId + "_1.mp4" + Muxer.TEMP_EXTENSION));
		doReturn(false).when(nonExistingTempFile_1).exists();

		doReturn(nonExistingFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4"), eq(null));
		doReturn(nonExistingFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId + "_1"), eq(".mp4"), eq(null));

		doReturn(existingTempFile).when(mp4Muxer).getResourceFile(any(), eq(streamId), eq(".mp4" + Muxer.TEMP_EXTENSION), eq(null));
		doReturn(nonExistingTempFile_1).when(mp4Muxer).getResourceFile(any(), eq(streamId + "_1"), eq(".mp4" + Muxer.TEMP_EXTENSION), eq(null));

		mp4Muxer.init(appScope, streamId, 0, false, null, 0);

		assertEquals(nonExistingFile_1, mp4Muxer.getFile());

	}

	@Test
	public void testAnalyzeTime() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		getAppSettings().setDeleteHLSFilesOnEnded(false);

		ClientBroadcastStream clientBroadcastStream = Mockito.spy(new ClientBroadcastStream());
		Mockito.doReturn(Mockito.mock(IStreamCapableConnection.class)).when(clientBroadcastStream).getConnection();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream.setCodecInfo(info);

		assertFalse(clientBroadcastStream.getCodecInfo().hasVideo());
		assertFalse(clientBroadcastStream.getCodecInfo().hasAudio());

		getAppSettings().setMaxAnalyzeDurationMS(3000);
		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope));
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId("name");
		} catch (Exception e) {
			e.printStackTrace();
		}

		muxAdaptor.setBroadcast(broadcast);
		muxAdaptor.init(appScope, "name", false);

		clientBroadcastStream.setMuxAdaptor(new WeakReference<MuxAdaptor>(muxAdaptor));

		assertFalse(muxAdaptor.isRecording());

		muxAdaptor.start();

		Awaitility.await().atLeast(getAppSettings().getMaxAnalyzeDurationMS() * 2, TimeUnit.MILLISECONDS)
		.atMost(getAppSettings().getMaxAnalyzeDurationMS() * 2 + 1000, TimeUnit.MILLISECONDS)
		.until(() -> {
			return muxAdaptor.isStopRequestExist();
		});

		Mockito.verify(muxAdaptor, Mockito.timeout(500)).closeRtmpConnection();

		//it should be false because there is no video and audio in the stream.
		assertFalse(muxAdaptor.isRecording());

	}

	@Test
	public void testMuxAdaptorPacketListener() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.info("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		String streamId = "stream" + RandomUtils.nextInt(1, 1000);
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));
		muxAdaptor.setBroadcast(broadcast);
		muxAdaptor.init(appScope, streamId, false);
		doNothing().when(muxAdaptor).updateQualityParameters(Mockito.anyLong(), any(), Mockito.eq(10), Mockito.eq(true));


		IPacketListener listener = mock(IPacketListener.class);
		muxAdaptor.addPacketListener(listener);

		verify(listener, Mockito.times(1)).setVideoStreamInfo(eq(muxAdaptor.getStreamId()), any());
		verify(listener, Mockito.times(1)).setAudioStreamInfo(eq(muxAdaptor.getStreamId()), any());

		AVStream stream = new AVStream();
		AVCodecParameters codecParameters = new AVCodecParameters();
		stream.codecpar(codecParameters);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);

		AVPacket pkt = new AVPacket();
		pkt.flags(AV_PKT_FLAG_KEY);

		//inject time base to not encounter nullpointer
		for (Muxer muxer : muxAdaptor.getMuxerList()) {
			muxer.getInputTimeBaseMap().put(pkt.stream_index(), MuxAdaptor.TIME_BASE_FOR_MS);
		}


		muxAdaptor.writePacket(stream, pkt);
		verify(listener, Mockito.times(1)).onVideoPacket(streamId, pkt);

		codecParameters.codec_type(AVMEDIA_TYPE_AUDIO);
		muxAdaptor.writePacket(stream, pkt);
		verify(listener, Mockito.times(1)).onAudioPacket(streamId, pkt);

		muxAdaptor.removePacketListener(listener);

	}

	@Test
	public void testPacketFeeder() {
		String streamId = "stream" + RandomUtils.nextInt(1, 1000);
		PacketFeeder packetFeeder = new PacketFeeder(streamId);
		IPacketListener listener = mock(IPacketListener.class);
		packetFeeder.addListener(listener);

		ByteBuffer encodedVideoFrame = ByteBuffer.allocate(100);
		packetFeeder.writeVideoBuffer(encodedVideoFrame, 50, 0, 0, false, 0, 50);
		verify(listener, Mockito.times(1)).onVideoPacket(eq(streamId), any());

		ByteBuffer audioFrame = ByteBuffer.allocate(100);
		packetFeeder.writeAudioBuffer(audioFrame, 1, 50);
		verify(listener, Mockito.times(1)).onAudioPacket(eq(streamId), any());

		packetFeeder.writePacket(new AVPacket(), AVMEDIA_TYPE_DATA);
		verify(listener, Mockito.times(1)).onDataPacket(eq(streamId), any());

		packetFeeder.writeTrailer();
		verify(listener, Mockito.times(1)).writeTrailer(eq(streamId));
	}

	@Test
	public void testStreamParametersInfo() {
		StreamParametersInfo spi = new StreamParametersInfo();
		AVCodecParameters codecParameters = mock(AVCodecParameters.class);
		AVRational timebase = mock(AVRational.class);
		boolean enable = RandomUtils.nextBoolean();
		boolean hostedInOtherNode = RandomUtils.nextBoolean();

		spi.setCodecParameters(codecParameters);
		spi.setTimeBase(timebase);
		spi.setEnabled(enable);
		spi.setHostedInOtherNode(hostedInOtherNode);

		assertEquals(codecParameters, spi.getCodecParameters());
		assertEquals(timebase, spi.getTimeBase());
		assertEquals(enable, spi.isEnabled());
		assertEquals(hostedInOtherNode, spi.isHostedInOtherNode());


	}

	@Test
	public void testRegisterRTMPStreamToMainTrack() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream1 = new ClientBroadcastStream();
		StreamCodecInfo info = new StreamCodecInfo();
		clientBroadcastStream1.setCodecInfo(info);
		Map<String, String> params1 = new HashMap<String, String>();
		String mainTrackId = "mainTrack" + RandomUtils.nextInt(0, 10000);
		params1.put("mainTrack", mainTrackId);
		clientBroadcastStream1.setParameters(params1);
		MuxAdaptor muxAdaptor1 = spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream1, null, false, appScope));

		String sub1 = "subtrack1" + RandomUtils.nextInt(0, 10000);
		;
		muxAdaptor1.setStreamId(sub1);
		DataStore ds1 = spy(new InMemoryDataStore("testdb"));
		doReturn(ds1).when(muxAdaptor1).getDataStore();
		doReturn(new Broadcast()).when(muxAdaptor1).getBroadcast();
		muxAdaptor1.registerToMainTrackIfExists(mainTrackId);
		verify(ds1, times(1)).updateBroadcastFields(anyString(), any());

		ArgumentCaptor<Broadcast> argument = ArgumentCaptor.forClass(Broadcast.class);
		verify(ds1, times(1)).save(argument.capture());
		assertEquals(mainTrackId, argument.getValue().getStreamId());
		assertTrue(argument.getValue().getSubTrackStreamIds().contains(sub1));

		String sub2 = "subtrack2" + RandomUtils.nextInt(0, 10000);
		;
		ClientBroadcastStream clientBroadcastStream2 = new ClientBroadcastStream();
		clientBroadcastStream2.setCodecInfo(info);
		clientBroadcastStream2.setParameters(params1);
		MuxAdaptor muxAdaptor2 = spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream2, null, false, appScope));
		muxAdaptor2.setStreamId(sub2);
		doReturn(new Broadcast()).when(muxAdaptor2).getBroadcast();
		doReturn(ds1).when(muxAdaptor2).getDataStore();
		muxAdaptor2.registerToMainTrackIfExists(mainTrackId);

		ArgumentCaptor<Broadcast> argument2 = ArgumentCaptor.forClass(Broadcast.class);
		verify(ds1, times(1)).save(argument2.capture());
		assertEquals(mainTrackId, argument2.getValue().getStreamId());
		assertTrue(argument2.getValue().getSubTrackStreamIds().contains(sub1));

		Broadcast mainBroadcast = ds1.get(mainTrackId);
		assertEquals(2, mainBroadcast.getSubTrackStreamIds().size());

		ClientBroadcastStream clientBroadcastStream3 = new ClientBroadcastStream();
		clientBroadcastStream3.setCodecInfo(info);
		MuxAdaptor muxAdaptor3 = spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream3, null, false, appScope));
		muxAdaptor3.setStreamId("stream3");
		DataStore ds2 = mock(DataStore.class);
		doReturn(ds2).when(muxAdaptor3).getDataStore();
		muxAdaptor3.registerToMainTrackIfExists(null);
		verify(ds2, never()).updateBroadcastFields(anyString(), any());

	}

	@Test
	public void testOrderAudioPacket() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);

		muxAdaptor.getBufferQueue().add(createPacket(Constants.TYPE_AUDIO_DATA, 10));
		muxAdaptor.getBufferQueue().add(createPacket(Constants.TYPE_VIDEO_DATA, 15));
		muxAdaptor.getBufferQueue().add(createPacket(Constants.TYPE_AUDIO_DATA, 20));
		muxAdaptor.getBufferQueue().add(createPacket(Constants.TYPE_VIDEO_DATA, 25));
		muxAdaptor.getBufferQueue().add(createPacket(Constants.TYPE_AUDIO_DATA, 14));
		muxAdaptor.getBufferQueue().add(createPacket(Constants.TYPE_AUDIO_DATA, 30));

		IStreamPacket p = muxAdaptor.getBufferQueue().pollFirst();
		assertTrue(p.getDataType() == Constants.TYPE_AUDIO_DATA && p.getTimestamp() == 10);

		p = muxAdaptor.getBufferQueue().pollFirst();
		assertTrue(p.getDataType() == Constants.TYPE_AUDIO_DATA && p.getTimestamp() == 14);

		p = muxAdaptor.getBufferQueue().pollFirst();
		assertTrue(p.getDataType() == Constants.TYPE_VIDEO_DATA && p.getTimestamp() == 15);

		p = muxAdaptor.getBufferQueue().pollFirst();
		assertTrue(p.getDataType() == Constants.TYPE_AUDIO_DATA && p.getTimestamp() == 20);

		p = muxAdaptor.getBufferQueue().pollFirst();
		assertTrue(p.getDataType() == Constants.TYPE_VIDEO_DATA && p.getTimestamp() == 25);

		p = muxAdaptor.getBufferQueue().pollFirst();
		assertTrue(p.getDataType() == Constants.TYPE_AUDIO_DATA && p.getTimestamp() == 30);
	}

	private IStreamPacket createPacket(byte type, int timeStamp) {
		CachedEvent ce = new CachedEvent();
		ce.setDataType(type);
		ce.setTimestamp(timeStamp);
		IoBuffer data = IoBuffer.allocate(1);
		ce.setData(data);
		return ce;
	}

	/*
	 * This test is throwing exception and failing before the fix
	 */
	@Test
	public void testBufferUnderflowException() {

		byte[] _resultPacket;
		try {
			//get the packet in the correct location
			//timestamp is bigger than 0xffffff and it is not enough to get the last header
			_resultPacket = Hex.decodeHex("04ffffff0072fb0901000000010774382701000000000072f261e2205f574237ebeaa0eedfaf450700a1edf0fea4fb55291efeb102c8847a7bd3f20a7887d3d3adcdbfbc965045d3d95e3b7fadc95d272924ec824b97fb56411ecec56fdfeebba6ad392b17cff7fbd7a4acdbfde5ebabff045d777d3f5ac842efdf93676ff15bf7fb77b6de5b726fd253f7eb11a65f4ec401077438fc27d7d6bdb6d69aed93deaf2e5a23edea5275cd455e466e9b73e2e25e5fcbab6fb49f75df966cde6aa75630264104d7abcce69071b61dfbaf18ebe9d985b793a9f19ea10332e4f6bff0838879440ab1af821b8095a40e2c4ec9e1157a6fb8e5ed74b58befbfd584b6d3ebf489969d376d0437e953bf4d72c5376f4d3de69584c4010774381ee9f2c3d5212904dee6618f59d97bb36f88757821d2db952a45c5fdb7fa8805699269e9afb6e29ca7b04bab2f5b9a691d44fae9b992f2109bfac4910aaa710be7c992bcc9eed21d0ecf1214737e6907bd194c57de9f0d378b753fe6cd24d34fe2fd75eeecab6e219cf556f7fb6eab148ed0b5fb7ab4821beef7933be436fedbc4010774384ca436ffe087afdd26af75beba27da511b93b6da847f04dafaee6da94debfdea8f44f757b6fec9082563b7dfab24da7d27da26ad9ffbcacf94dabd3275ed3559ef7d717e5fa9b6fac9d5ba54fda7dd7a89fe57c51a23af274f567457aabe4bcced958d3abd746ee9f562d3b386eaa930eff1c8f46f4b4f0cae7feb5c431d1a1bc4010774385defbfc739fa87a5ec56b3f4b78257dac68e2b646ddf88d952774c4043c611a7dbae09197f1df85108dda3241e66312f74d050c882ae0a732291532580447e3e7062427fe317977e92b79b7eae326e411cd0a6ff7fd3dffc25d7be939ac53d24fd6115e6fc9ade316cfa27bf965c6f5fef0825deed1d9dd6fa6c27a3d75b7562c401077438c72822dfbf451c11ddf75a67a3fd1102edfbfaebd3dd8d5d721ef0cbfcf9818bdb9ff4e49495e40f6ed5affe203476eab5fe4f7e4e5672b696ba054c7718dbbfb9597357db6e9f763fa2a75f4ff2a3f8fc9fa75f97b7d3f2937ff15bb921a6de9df28e72b3cbbaebccddbd26e7057f89cb4f4f5e12efd75513de09344da79a5bc4010774389327fe3fa64ddb5fc433c7a77de4d7d65c69b659feca52fbfd13beb687b7cccdedbe7f2f5d351129410efebd5f5d1b693095ba6facade43c9bf22d13aa5049c9f7fdf37dd242b612113c33c18a7de27b7936cf27b565dac831a1f7f4f7f690add3d77d6097d7a76eaa9b968f5a9757c95625aedf56c48af7ebf608f7f2fcbafbc4010774385866827dfd6eb11f4cfbf56f593d7ff189bb7551bb086e880ed21d5e78a1b6b41b89f9bf8da1f728f9661457fc620dc9e24f743e50c158347f80366f2be6c04ba4fc6b9d57091d363880d18988e36b6acfd873a3cddd25e59ebed7f8d784ddea998f4dae94ca290fe4389295fe9eb1dbf979861c0bfe1d1ca70d8279e24febfcc4010774386231194b86bb9f9ef0cbb91c32ff4b58f6bd9bb0e03b7109ed6f1dc2deccafbd5b0e76cf0f597d755dfe3dda4fbafa5d2da794baf90ab687a79e621b4eb4aed184a5fcdff516d3ac8969ed41468b66fdfc8411dfd7e51eedbab797c8c69ae715b9e9db4f6ef977f75b8f64deafe394f30e79986fe9ef694f3022f44dcbd8909ec401077438a48e5514f7a58cfa7b39d43bf7fa5d50c44b1fc7713ff7edfe8dbfa767c15bdb9fbc7bfb74c8443bcde8a8faefd147afa61853ddcd9d27f8c4f83da763cafb7257abca92300adac06e85b4908d8983b9aa4a754ed37ce23bc4796b2d29d7825e99a91bf4db27b56dc448885dffa2ecd7e8c5e9f765178eeb79e16956f27bd659c40107743897595937fd2fafc126ba7df9b7ebc16ecf6dbdb6f3989baf27dbead0bfe4dfdb8a0c93d59b9e29d7b557aa4f79fd5efbd74e31ebcf95ebd3d91177fd94bbff9b5fbcdb7e4fb721532da043d7ef51fcf467bbf5f5d93ffe0a337aabdc25b77dfa77166aecaeb7b5d959b7e9f966df97cc95b5f2915d778b61136b1bd91efb097ac401077438dfebef7bfd221776592b2b333abb18cac77e5fdf5c451ad6a0433d29ebe1a26e8d3ab9413afdfbf411bf3ffc9dff08fe295094634d04e5cbdbc78fff18e11878d37fad5fabd7e6c49959f86a9ba365caa77884893ef3a95571476af127bd7d613d7838b0200d0380dffc28d2688b3d78dc3218d33f78ccbb235a7d2e4f4d7eb0c4010774389b844bff091fdb57584101affff7364a55409a18db4b1ddcf1a66ede65627b499b1eef950baf19aae136c3e14507f48a71f07c6ec71fddedd109ba20a2a27a757f699baad5e39d17332a1fd7bff8adfbfd7d9458c499a63579acee9de9fbce36c12aedde9d27d29645f9727fbcce54ba5b687724e5aeddb7ff84356faa7b7d64c4010774386d0256dd6d85ce9aaf6f7e482a6dfa49e567db6a430c04e8f1c7f7fba28913dbe6e651f6872c7e7f2e3fd118c57d3a699a05625fef6eb41152e89e927e4f497a2cc709b2ff5d76c86cf44d9a0edc878a0569617dedafdf5d42209b7ef477d555851546e63e12ab9d1fa9bc19546ed483a395380ffd37e621fa46df8e9a56aca1c401077438dfcd756dbf8aab2fd21ddefae7efedfbfabeffcdbb76ccaf13fde9d536fc3326abda4246d5784f7efd5b95efc89fae973a13bf74f33711eaf2a3357fe137267bfcbbf7b24b5f7d58c72e7d54c5dfabcbaf5e6eef5691b7e9af35f2d6fea6ebd276d17ae4f7d36db129fa08e44d33d77f346c575d8c549e13f7bfdc76e0e1bb72c40107743812458fc66e6f09bbfc7f2f909f7ff895df839381fc98d463bd708b38a3c78ec43a8787fe39c7eefb1e8cebdea8ea8b977ea35ef7ea6b7794808f444a5b7de73e4ef9135a555289f4faf527e5deda76ae42578bd7bff413cf4efaf57b7e4faf1659336f4f27f981409ee13703fccb2ffa2909bfaca8444efefd56ad9e1f28404ac401077438e318ca8aeaaa8b9ebe4f4d2ff0a2bc4c7ba6351e8e7623b34f07c8ecebf3cdb639aa12b5cef7b7d91f797dfa2b1cc91fbc57e9317bf345d3a6d2264dd3a7f2f7b7a8856fdf4f4eb285aa91899fae66de264dfeb36bb7b5ba3b36bcf793668f7e66b49ed6f7264acdaf5e235adf89aa8bb844baae6b726feec9abbc9a7f58be8ac4010774386f5ef8dfe8dbbebcdeba75736ffe2981eea5d27ff4312769e57e3c8c61eeb3076665c3ae575e147859b9e363483b7ad71c9dd16f73b6dcfe6bf0a3b1d0d8e3481f22f383d97a04edbd59ad7fada7d0973a49db423bff7e77d10fdebd5e08f6f6dbf09edb7ebd2209f6efd6244d7822d7f768c6f6ebcbcb5b76f984f713d44845c4010774386fd3cbebbb22537abf4fd9f27f903724c4841cdefe7fe9237676fa382953bc2b38ffb77eca4ea2bb8b1cdfbfded6694dbf93ebfe813e993b2fab96db1d4319d208c5f8f3449e4c7d3d17a36b5f6cdbdb4f8b6bda4c8c7107374edeef7dbcdbdfdfc73db4ffbda44bf291b71b55742b9a8bd75e4effc10f35d395d99da53eadd7c401077438eee4c9dbe9f5f2411efdeae57dbb64a249eb54662ba36f34354462bdf5dd1cbf2824d74fbf17e9ef6fb408fa7bd2c2bf797dde235efe9fcbbf278977bf77f537aefda15a67ad9bf5e2551bc7dee9bc9f59394657937e553cdfe6ddbe6c7827ee8dfd6e2df4e6efa7d0242f823eadca84a9ff4f697a9b97f6fff8add97b74be85c401077438bbe076c421e207caf4ff7d8ae68b76e4f40a4ff3fdd59d608b54d137315e31ef8ffdd1e48f1ec21779f8e54a673076f9ce62d8f9c6b7c2f864fa6beec15361479ca3dca92b7b3f09a74fd7d631eb6cb9d3fd3dccfd3dbbe6dd7d10a107a69b1f6e9aa72b923ae108647afedcfef4dda4fa7e9c208e9cead7b6fcd26d59d0aebbc401077438abe4fd3f4949e9fe39b66634df57d5244ea9f64102fd36dfd55dbd7eb0837f1ec6661d3fc12e9e9bb7bf4e3d2c3f7b5f31c46bdfdba8b8a7a576f360cc477469dbe3cb5edef4ee1871cf7897fde4f5e798c0cf1ef4e55fa263097e6bd0d673b6c1347b9e71231606f3bcc39c6cef203ca1671cabc728b6ff578c6b5eff69f8c5c401077438d78f46fba77f57562156bfe9dda6c1e096f75fc620965b44cf5f4a1179375ef5697d97b7a4a74ce12ddfedaa4cbaf93debf976f77d7b5e5f212aae89f5f8ceaf7e65b27fe4db4a9aac25b8ea2a3f5a546793bfd49d73ba29393687efdfb9feab96d4dbfdbe6bce8277efd57d91c493da4fb97049a5b6fbbff42eda7892495935c401077438af7fe12f4eff7284d6f6e9cb4dc47b264fab717510c4b26f5d24cab36bf6266dfd2c202a57b689ef77f42b67f1296a10d203eaeefc62561ae2afe76666cafeb08ba61bf29548f79408a6431e715f6e1cc55a58b71e1dac3d787242e4f4fff16994bd021f84269907fdbfe29f248e70375a54875755e2749f4bbe97c633bfd8fbc40107743851c054095c96bc73b1c9fb79174272630464e9f4ed75f7af2fa7fbdfe8a81636f9a1e6675dbf6a4dfa49afb721ffc53c6e9dbd6d92de0f5cc59999ebc9fd2a1a742451764ef93fef8e1641eec88863f8f3e68d9995df93f57d24e8eaca4c9b4bf726ff76314fbce93958bf93ef54932fc78a2ef6444ba20d37ae6a05ab35ddbfc4010774385cd57fb8efc1d5fbd6e646ee7b7aa18f733086f725fc8c532762fec6accca6da13b44eb646f8392e9b8e8cbfc6a4519b23fadffb5a6c61bfb71ec6dfdeb7660a31943edf9fa6dcffeee71c972d3eb8bdf2415f925e450dc75c41458c7bcec4383d3c344dfeb36cadfe31dbbe5c7f7e4f7b39110494c3c12ee687dcfdd93fb8a9c401077438c2a1d45dada431cac537cbc9bbfa776cdbbe935512b2ee7d348514c14699e167fe5fe651eb6a5782d73e77cbd2086f27bee892b0fa23ddfe882b4bbfac7ccc268b7bae9bac16e7abf6e9b7e669b7f623673f4dd3d56fafbeb471cf79f297527b754e94ad214d7ef584ac684da6fd6aa426edaeefd7b27db6ce694bbdff452e5ac4010774387dfda2ad5d3bdfd5e4ab1e4ed4d75efb3397e4f5556e6d5e959c9425deff4a4efefeb7bfbcf2155e89f4abfea8a45083974ac65fb1caaaaf55e938813f59e8cb93d6bdb285357fb52908ed26b08af6f47218bbfbb993093bf5928a61cb28c98f37c7f6a6dbfe9a5cd3e53e9f8408e9fe3defe1b584a672a8ef18c4e36077fa0cc401077438b133a9a4f3010eac0b7d696cd05f7e8afb514073a3063575267f27dffe3f5e322c487efb080a80561f6235186b8c8a63b6bc3c91b3eb3267eb6b019bc8fd5d4d83bd26fafb3ff1aa574ee36fb8cb7896dd846982ed2ab31f8325b7febf73fd6fac1572fa4456cd026761514dae772972f3c7c9faff8c5f99508b280cfb245b4ec401077438aadc86dd3871c935626821756f485e9c95470ace3afeead2961018f06bdddfeb05feff8f6f2a975b1d32ddeafc529d39fa2ee9bac9e969866e8382f7eff6917a6bd90a13defcd5eec27db6efead26510a52fdfe51639ce839981bc5d2eca2828fabb77999efe6dfb28edf468adb6fd65408f648d1fb748b976dbf4a104bb7e86c401077438593e27b6a8c824c5ea4dfb53f18b7d2f7cbbf78f79b2d4cd9e57e438c72ef6fbeff90a252edb2ed5d6f90e862f195fa70bd177a1f2045e9c9d2cf0fb72a3101167c4b7d3624c665d33d2dc9f4b3cc8e420294ec8eff8e1c71a7df6dcad02978d9963357a7e7fd93dd33cca6317789d8cefad22467eafb6969ad3632b0543f851c401077438b3c6c5ac63336a0f51ab8cc542fd61bed61b378d58e0d7efb74890887d65067a411ce3b3e31939077f5d54599059ccc5bc7efa7fd0225ccc597773041958e663a4d8eaea609f37f4fb4c12f774deb6a5d30d8b53f6e4ce3982d736e5d9e15f8c4657df8ce3baff290576f9bfaa05b9fe9eddde913bf4e91cabdaa20867573ca5c40107743836ff7851b7ef4c9e95def9ada31917e4e3bdfd75295adf62d7539348f4bada379bd762f6cfde0fa6b21446fcbfdbf974d3e4fdfaa3310de5674dfc4937fcc55ea6aa27bc447de7c5ac9f4f6d2a5213e9b67ad4debab2c240b5eff8865cb72d7e810b3febf08b2fc90df2c3a9821a6fdf7fa45931310a8d2fbdb276299bcdf958c401077438f3096f7233ff8ad37cfff97b4810efdadbf84b746f6e4a6ad91c534921bb9dcfdf6318e57f2fe4e463e5629bc904deddfebc5b093bb6663ea436fbdd948311b6ff20fdbe309e6161366f2653adad1d045ac98f644634dbf4277f74d74ec491beecd37a309774ded30fb38275dfc9dbedb6420f40932fb3b7d84f6af4d3f69029c4010774386ef6ff76e91423a2e9b69e9b361c9f697d3851a764b44421e8cc6831ac9b0d6df41a19c6b8d93e4f7aeeb18ee28869bffdbd121434da8bc748bb619b41571de727e9d498d60814f78de020627d4f1fedb607f56d5a82dcd65e9feb0fbccc2e44bfc16b99195a911ba174fff050f826d6fcf0253ebdd717e317d4369797d87ec6c40107743821c947c6ebd409293e924bdc208b7bc26c55cf1cf52b086a200df4fa4d1beb78c4e82ff7299d39d789217e4039aa496b154b1bb4d4f094dd355144d3e59d878b52d557821df85aef1aa2f2a09b86d591cbe5fb5045f6dbe205e5d5dbf56ce848c6fb7c72bbf7eba108f5f6fa6aa714bf7990865c7f3fd5a10cc134bd75de95a9c40107743889eece4f69bb31220c71397f9f28fa8913da2437f27f4b3a8f317edea826cf0c8c939ffa9027ad20fbee5a33fdbdedf9440c52b9d927db1f57fb11045ba7bee88b0a34f0e562e30d63b6bd3b6da69e2013add56f571db7d9f747881209be5c7af67458ad72f15edfb823ebfff3668a36f27b698e3a21063160b74f6d2d3edacec401077438a145bae48bbd9726de3926c5ccba631acbccb68e4dcf80e7673dd608594aad727d7fa8c48bb67b9b9b9eadbcf6b74c76adbd5df35bf076f5a9fadc0ef1a37fe7661b2f367c16497ec9d88e55fd27733dfdd69f75a6a739f4fa826e6869aa3b7907049e5f8cd5cda262a6febbcddb27ba715dabead1427f6d7ecef27dba2d3440c401077438f6ecc4aef096feade4f7bf4b0976f74f59a5dfa5ca732f76cdd2757fe13d1aefd2bd49d5be9b2effb14f7ef8826bce4f69f7f7dbcf572350a49569eccfb25fadd23ebca42eedeac5a13bf7fc56b2e22fb4e8bbebba6bdf56b408b7af7ddba8c5cc7996cd557775feedfa77718e9fed6accb21c74bedff82a78be90c7d32954f2c401077438916b3b7e23257ef33d842d48389583350f6ccbaf7abdef8e8af5de21628737b4a9fb953e44672fabae95b1acf9166312453fca3f767497e08797fdd66ebb4a7546dfdb9058f45e68d1dbb21e4dfde4395137a3f318bbfe8dab7cbd494f6cf9ac1c4d79f44e8bdd9b7d8aec916f7487abfe8629e3177877ac7c7bd887a89d093ec40107743897fc6b32fb58d9b321b1d08974637a36b9e3355946d90227d93677d5e74d9f9adbff58c4ecff3fedf247b8a0937b1f6fd176ebbb9326fd53977fa2c4374f79ffa7ab7edf726ffa824efbfb3137fcebe8477dfefed7a70977dff13f7275d77ba4dc9d538ff7bf2d397a7ed336ff5b4acaeb165eebb94aa7b97cdbf0dda66df5b7c401077438a19af054a7be7f43233b94d3fa77cb6da9aeab6cffdf3ed17adbfbdebe3459b7fa11fa1fdf4abafbcdbff3befda775de5cfeb7d84f74cd5dfeac529972d6fd65d37d5ce6dfad09f3936908435377d3d228277b6379118cff97d8e55a2efaf725c7927238d697f082dc78713192654eeec7b77d937e4bad2e462db23c4cfaf5c6c4010774386d1b7edd9fdaaf36fd55d5755bd5f74f2d954d909157c9a6bd5652ab6ad52fc4793b6bc5d88dabc842fdbd90b5e117be9f8dd15f4966cbbeddd7845d50f6ad84cced032246bff4a4fcaed9ba89ffb759855735ee4def3d609fba7cf44db744d937fe811a99924ecf57209ebebf845e3749776dac7a929b78b352a27bb8449e9ac401077438756230d7e4dfdd9784b7b71bc7a74be337e3479ec71a1e168b0477b63c60af22266041ab47648d7bd5274d2bcc08d4a38c7cd835dd5c71e326d5ba6533c9ed3c7e8318e78e5ced3f7d69fe2b7f74fb4557dadd0964f9d8dbd582574e19d4e2f55e1372797fbad1efcdedf918972cab3ffcdbfdefa7e99021df7effb413df97fdc40107743810b57eabf4ffaaefad5ae96a8f58b4ba24bef7afb1f568fde4dff847c49e549623f33dfe9eed2047eb7aada5a7b6bcdd7def5efa7b8d2f26df8b88dfdf5c4bf4e275a789dfd04b7db4dd56db166c142f4f2b1ba9d25fe2943d799eab14f75cfedfc28a9854670ae63855d18dc4f771784ccdfd356f3c5baf08abe11f438f6cedc401077438c071b7eefc6285d2632b6ec21e1c1e164cdca426fd25f9b8c035ec5642bd6768dffde91afed52eb0825cf010faea97ef11b416d79df7684485e9fdc27b757b97facbdbd79577dba98696edb11cb56e37f27e975dd93eed5531a050d2f186ebfa3766db7b9427b5649f6eadca4377e92e2c259ff7f7423639959d77bff0876fedc401077438bff8b53e999fae67baaed4c43a2333fe6284faf4f93dea53bd107b2c34ff7d5977f77e1461c4a37f828e214e44b2fb988e19d697a18904fef8e97f1994e7e3f7d8211a6ffa7f2f8697d36bc8315a6cf958bd1694b84dfb1213e5abf7e4f4dfc98810bb7db6fd0815efd7a39bd08273ff782972e2e5ea7fee6efb08f75bd33fbfc4010774386afbedb7d75e239ff7dde08fafdde5dff887eadde5924cffea411bf7fe9f6f7e4eadfe107965343eda75b9beda59d225643f64d2b4d62d2f977ed26bfcf77822dfbcf7f5b71ba7f2bad93e9f9ba2fbf4cef691f6b915d51b72d344f7bcb62b239a5d5f720add9a7ebb427b901277eb7e225f5f66ae376ee5274462bb68d3b76fc4010774385f6bd63f3aaa1316dd133b76fe2b128c4e2039c68e3a3a79eabc6694798d2152e3a0a67ab1ea50f2732a92581a8055728736d7c14fd896c45a43fe6b0b4885c5fb4beb5b55c11219fc8d7f76abc46ff3541dbcaa0a3b9274adeed26d4fb4a82efb62f762ca31f4545ecedc9ee9273248a0b7adbb3477f39456efbfe98437ef6fc4010774386edaac27ede9eb102369b11937eae4d5f5e91749db47db14f7745d2cf2049df99bbc68c23f65096ff6f27bfb9810793da4e7ce0fa6052d7d7d37af375fa1a292ffd0e4f69779f26fe9bd3377fd8516340b83e6922f231f2826e18ddc55ccf1292b073c8855a5f0a2ac5c14d7201aea2706beb3dab110031b2af6cfb9b3251a99c401077438d9923b49fa93391bf6675bd8d441964554510efa262a57adb4c674afc9f59a94c2611751b57fd37be2d8a049b77b7712274d3f77f12146d9f1bf4cf00f46dffd2ff7302c5e78397efd37f6c162e9fb7bcfdb6ac45827e78db5e9d565d8adfa6ece95c8e31d5d9377d4f0c9ba69d4da92e7fa718ef5cffbe7ceda5c9badfb3051c401077438b73f2b09f9bfbe5de4df6fa1212dfefb69b39c1775c672a71dc6ac5e2be68ed6f2e7a273bcb514f76d3efb88374f44fa4a27d22f75d79ca273e77fec4f4f6eba4ea732f998d582e75963fbbde5ff653fe4dfeb7c9f93d6be71a50927cddfbb17a590c465d4d3f6a2bb7bf55fa3b6e5fae20dafa4d8202092efd5ca5efdd150a9c4010774383dfb369df93b64f91a89f790a5f6fafbff276e9bda7d6de96926204b455d3ef30dd6bf1cdd3e7669f5d0aeb6eabdcfbb691014aeff5d6d44f6b69939defb6b667d793ed633592a89049bd3d64f74e44f4b54d1d82ef54ebb64feeaef08e1a52d6841289680046542463c33b061afd631d3c3ca61cc403e5d98036b71bb59c9a2c4010774389796339ff721ddb5e146f6cff1d36b26204e252886d3d530d7a84c7084cfcb1868347a4b8df33c2ec96dfc6a3834b901ae3f35a4fc1c333071be16b1adf33d9141feeb15d95979d1a2dc4fc8151f0a2762e9ffe5d848b034fb909bfdb2edc84c7ae5caa22a9f5025c475be1070fc8f2e5110c9a7654c9c371bbed5575662f8ebc4010774380d1f2acef3b69daacc5fb776638b28bd1b7bbf5648a1c39efcbe7c6fbb5cdbfa567b043c6f4efac449086fdd3f2d5be5826de9e689abb56ce3deef6cfdfa7d2052f71838fd7f7451a4f4f794f2193f2c5eb93ef59c81fb12e58e9355a4d0fa08f46fb7ecf748ac67ca10dfa4e486cd3eaf8814e16dce71ea7949dd5b44fed599c40107743811c11eda044af2f41d26da25f840467dc95dbfca0a366cb2d1b97b64fd32041a290b36e78a2af904026dd3c946f77178f64612dcd0d59dbb491cf18d3df18ad3c65558831e5f8c41bd068a795638095eddfeaac6c18109d477f2c16b3792cc7f1ff17d6861b5f8d52b410094ef2a8b84a4527f52d5d0d28f2bcb59cf93e11f27c4010774388b7d7f93daf0511516846febdde4824f5b1d3627edb74fd49edd78c57eebbe97b485fafa7bb2f7d272273699a8bea244fbf6fabe237c333eeae65eef7afde5eba27a7ce1065917e3bbedfd7a27b48a9b393288f477bdfccc4aa6f6f7a6e437f824eab789a949bfd492ee4dbeb275b0a57876fedaaff7affbea9c9689559bd3aec40107743884659d1b6fe9df10f7eeafeb7d74f632fa69bc7e979366f5a7ee29bf638cddb593ba636952e104be5cb54f2eff10e9dbe4f08b09582374eef5be09b5bf7ecc09989fb69f8c7e43db2634f9511d688225adefe4f97b989aac36b7e483f25aa8edf4e4fa5ff1be834b0db8c4d21c2a6ed024a7b2963bd8ec5703b7e234823fdfabc401077438f681ace2d192c482ff793da5ff0a3b2eda6635f42b30e6378934894ee995b40978422c4e7ff66afc63abcd99987e56685841c9c7d184d9c9eb08b2e69ba06d3dbf8c37e21db4bb22eedf18e689697f979ee7c3cf9df1857483f7923a2c3cb9fa3e54fa196874b0d26c21099424eb7d2d84dab79997eeece6f8b09a9e6fd0f6c4c4010774380adfbfda2b5efe4145debed17aaf57a5dfa4c25bfd727ee823c7397dbfa1dbfbf6ff30491a31de7fdf658acbbeaff164dbd349dc4949dd3f1f16e6e5632ff102c7b3e150f9ff958a78e61236bef33989a77ddcb8c4c98e59a727658ea6001c7eebe256df65d6b93b934069bda1bd938d158223e31d191e492bc7006ee248975ac401077438a50c57d7a3ff7c06e39f2b5d5457c85d61a6abdd06f4ac6a10262e015ddd58f2b1b974d1c9bfec939675e2b77dfeeafcdcffa6f8404eebd5d393d6abfaf08b579bf3fe456e8a087372d7cafcbbf1552137e44e9e46fe8f13d3eb9b48af3f138f2f63bba0492fdea895693c5f7f588aa15beed3df9c50953ecaf69d2fb6c9bf11c401077438e474dd3ec4d7afc434fdb0a7bc631a6d9787ade7f8e26135c0b0ffac9a7c2acde5cbc0e93f1ee72206185c8b24aa60963a540d142eb3fd5786f671c1cb4d7d3ff26fff054e077a8a8378eae5518f9c77700ed4673575897849dd7c3f197ba6e51937f298aa65dbd3528a4f7ef4349bf3e904bbeda7ee11cda4e09fbd7fcde4edc401077438ee7c9fbdc56e4dfac304ec97bf5e462b7fdfda2eff78f76fcbe103e7efe31bc22fbb59aa0cd5f121ebf176b7fd6e01ff49d5ef08e0d7c59bf25c6ff46538abe8f363d121f236c1d578c5e49dac20dd13c707597e30fd52f5d5bebd6984baeffe6df5a578f57ed0b36ebb5e3c27137bdf90dd4e1f93d3f8bde3866277efcfe6dfc401077438dda242244ffaac25ebb7df95dd7e7d79b5e22c409ecf376fe4445f09f66f6f75f494ba937e4be5b2269a3cd9b5e23aefd93dbd570cfbb2ef4ff1887f57894a56f77edfec31f94bcffe31b61062f1a862e452c613fae1237f91c145ec181db5e11d5b6240e3433dbdbb4db5fe105be3670719dd3998fc636370edacc19db9a115c4010774384d13c984961e8c1bfef1ec908cf77e97c2083342207700ca4123779075beb0ab3943dedc90d226c88c248526af6358e6309aa91dc1bbb9c7cc87bef507ffdded598ea6edfbf7f317bfb9c5afb79fd253b1d36bd2b20a90dbfe5317bedbedf49937e6ec4eedf5f5431bfdbacbf5d294e58415fdb7fa21bdcbbf55824dff6aa270c4010774388bc38f537fa74bc4051a2acfa1f3c6f31cc1ad48b81f428b27ef11128e530c4fefefb7d3e4fdd36ea861f7b4ddf9f2c11342f8fcd08e751c9f69efd3f3f4e393f5fec9abfb9cabe6f35d211736a24757aeff0c64f74d9d98a0951d594e9a437a8e2ecfc9f5e5a17d594e1255f3febc9cff5b3937ed27efaf787035c56d4ba6dcc401077438f0a27aefe0a323f4369a7f64f5ef4ae64aa4297afcafe22bbf4fbe4bc25debdbb6f255a4a4717dbdf9713d3e3086df4d57293fac4ecb09bcacdf77884dafbcc97f5829535b7b867d12d85457af7d2e46d27e31c78703af82e0fff4daa77e449c9fa7fe398c09d3b3120a1a556fcad7eefc62357503bd5372fccee8f26adac6a4c401077438c7d194c7f00ab7c3957c2093a5a15c7b7bda41eb3e0ebfc9e9293cb8c7b6ed7afa1badbb7dfd254abaaf1eeda77ed7cb096fd7ec8511df9bff045d3bb9b5ded25c84efaec537fa6ae301275dfd187b3f7c6aecfd3aca4057adf5ebb993f721c0dcfc27bfedf9c27dde7dfbc11f7fbca726ffd057e7a9c45ca28bef45ff0a30c6c401077438ec48c98e460a3933f6c47e9ddca97edd3d07bfff5f965b495c6ae40169a6686efeb0cd7e4757bc5c3d7e99bef7d3d53ff27db5b69631a57b6dcaceefbfe10688ec6553efd35d3de9fc502e4fefa6972ae710ce95fd75da2192c49868fa66043af7f4c4f7efaafb2257a27a74bca23237f16a7af4fbc5f12115f79f3d3aa5ee52c401077438efdd6135d37cffd08fc4779bdbdd4ebf975f4eeaf5e4bcad3c95b7a92b7efe43ab5f845be66c7ccbae4ec26dfb74d24a9c25df7eb20d57bad5a627b579f59774fd66ebef7ef6df88eb2f7eafc25bdbd9a3d3f6b16d44e88c7be665bb27ab5a6cb8c69741b961d4ffb76a4f6de8844dc9bbf27ebeb88474dc69826507a2535579c4010774383d2557da189b58816cbef8edf5efb7dafbf8e91a7fc436e5637efc9eb7fe57b7d0917eb77fc9abe67b2dad11b74df59b5f27b57a66ff897a6e497ce4231ac744f5ae27526ff784b2c5f2e7577a6e2593b4413dd77d5dad64fae250bd13dc72bedee66344fe80f93b14ce3d5bc5defd26e98b2ef3fe922eff78d6d65e63f09c1fc401077438e5f57c418c9961c5fbff0a7922cae7575d878618387d23dc70e621a6186b2dd38511b490ddede52725337256bc7675c70b3072f1aa84e011b52dfbf2f33cbbc2961d4c9cab950ad344a703dd183767144f96876a3f76bdf0c6d43327fc8524a541470c7fcecf3731aefd5ff2e5aa3dfd8864d7f5b734af8577c5cb0670d2d8bdc4010774385ff05a83b7f9fa5da79fd24c839ebda56c9efc72f544fbcf9eda13bf4747dcc13d367f7edd6662dbe95edf8c429c91ee33196a7259346da3ace8a8fafcb5bd26f37667a7ab2f6f9b3118677d55e44df3895ece13ebb7fcd6d720d26ff7125dffded7aa7f49da4275b7d6f3afbfcbcbff93669fa3c269bf958cef2958c17d7a6dc401077438fe2f4a9928cd1fdd0e6e6491ddd3c8cfca2042e3377fa2d66dbd35bd3a64212bdebe4f5d6272236feacae51c999e4fe6faf05bcfe9dbd335e5da8e750fa43ddcfef933c9f540c5655234f0fb2b16ef7d753045be9bfbfc40bf7edae84319973e8e9f22b4a54ed5889426eafbad7228411fdfa2e5e0eff5f45918d18ff18e5606c4010774385337c6613e8670c07721d5e560957aeceef3771dc536f76dff26750328adf7dcc2de3877bffcc45ebe710f7fa6c7abb6dfadf760ab3feb6ef5f76e13f4edb7d515fb515711afba3f711e8d3933bdf08b2e1e3d65fbdd34eef4b918c52bf7f1bc73dc8239d1a7b13245bb8d460ce6f6ebfdbaac8508bcf097fbaf9965a542f327c401077438ebfa11bf6b4ba1a135d29fcecfd31cd97336c6afdaa20b631333d3a7e5634cac74e4248316c92ba7f3fbd3f620734fcacefd5d04c58bf4d5ffc4f491a33f5090dfbb1da4fb7f7d373480815c6d6f16e24e56b9360beffdf046ed9bb5bfff1aaca97c6ad4cad04d858b4208377e9bb4ab8491f7e31a4dd77d4f3da2ceee7cc178c401077438e7f6bfe31c9d8b5e820e98442164e5f8dce6ec88e96cebfd8c719c2a2ff9499ddce538a375b56b184bfb80b74ba3d892bc1acd1b61e6a0ac685efd82b70e34bc7f043909746fdf49d991bd36d72dd50e65fbfb6f4ec4091fecc39edbfa6c35bf5fbeaefd26f7577f0d137ff275d3b2caeeecdeb115d53dbb9e18f67f91fd335fc401077438dbdd9af7f579d022f5b9546cf59374f25ef77e95b8d909fb748bb36dd93f52af5ecdd26e251bb7ee637ad13f7f6bef5af48bbfd966f5ed32eff48ef7ff2ef46df95f4f93fd0966b93b71cf76f97fd2c82227af2ff6a1053eef3a25c7f8c7959ef0b271fe4414e961bc74d2f52e4b01afe9524ad218f4fa83e8e1f7bb5c9fd253c401077438be314bf6184f9cfede5a4e794d4741377eea6af16ac83f342a9113aab95c9ea9be463af7fa6736ff799efd14dd7ec80877edde5e9a7bb909ef36cacd1efd3f5e2447512bdda36aa7646afa77a043c93e2fc9bfd46bebd2a3452b694a6ad37ce09fb69edeff3f448bee9edafb0b08ebd12fe2b7effd8416eefd38deffc6a18474c4010774387de8f74e0e5fb8ce9d6bfd2f8d56a0798b4ca6b96146ac1b0f6bdf473a468e6ad9c6fe5f1d5fd1f27d5711c48cde21688ecde2034d8dce9f4d020dacd16fc32ee1871fa1855f96875d645f73df8dce8f27b994a3efe08fcd07d69b3a60bff797fb4d7db84c21593ddad6fdbad3dd37c9f7b8ac6e5c6513fa6be436fd54e5dfa7c40107743889fdd1bbfae21a785a5ebab9a6c8a2ed6ad7bc9eae10bc5bb756f77df8ac92a7afa2fd167b5fc46fdfa4fe13efbf44fb7b5f14ee3e776a9ed54f09aca446bb96ddeb4a6e3bd7d5fd3be0892fc7bf5b75c62ddb219a5b26d935e01927f17db5ef618b873c6cd3acddd774f8c7233af772a6ac40925fbc63af353b209ead3023a6c4010774382e8251e461fcc3b9cab3edea15de955f845d74055c67b4e0bd0f947176f5f1577277bcb4a7dfe43f5827ddfb6f6aefbb10f4d16e9d2294b44f5f344d7763697698aebd7a4f28bfcdb5bda7b20ae58ea78271ac861cba79527a7b4f58f69e4fefd55c83b6993e932d7fc621b53b5a3b437deeb92104eb44e7adf59b78ff6ab0d5c401077438f28d9e4f7ae5e41895905ba631a6dcbacc18e2273995772b1641a5d917c2863b328112ca056f008156cdc79a65cda4394dfd1ceb4dfac50e507ba6fb16ca37e10c650da306871cad2544ac3e97dc1169266af5accd94adbf75f857d5b9feb5fd248a72ada46209be9ed27652977ea9c4f6f56fdf559b7eef2af37fc9e9cda34fc4010774387dd2255393eeb7049ebe92f12d76ffc9d5f6a7eaf3e23746cb7d13ef58b8d84a971ad11eff7d57f10d0cf2835a7d91e4f7f79911f6efe4fcd8cf70fe2c52da1fcac7de78a65fdf8814fee4e4043d76c9eefb4dabdb57aec63d0817eb1ea0ea26060b62153a57606bee39acb8f6f042be8eff6f7eb98530e3a71fb29f65bc20f1c401077438e97afcf7afd3fc5ef4ecd3eefc15a87e39973afffc625a1ebc78ff06bb463d1a6a0b64ac10357704eeed445c5fb8c135305f819f6a8be31e95e16433c7ebbb96a075984838f719d47bf2cbaa6b58842f4d76fed8bfcbdbd2de13ea27bc137bf5b0da38876cb4cccd71c5dfc9fbc4b926935fd4176cd3dfbf66befea456f12185c401077438bfe1173318f656434dbf47056acc8b9bf79586ea70965ffab3ae788f341365cecd3e544f5b88542c4a56377538fdfdbdfdbc98c5b0eabef55cec8f5d849ca3e4c115d9e7f5a727dffe35a2ce4aa3979aace3f26eaa2ad6b52cb235e953c730c26ee3731c0d8978a8fffc635de5500a3776d122eeb30030f6abd73ec68fa97d36c4010774384b5a995177e33da1d3916cca5b3575c648319be7cb93d35b9e2660a31c375f1d17df6f97683023bedfebf163bb56d7bfd080a6f5c7b2c3d6db6dfa821d5bfbda0c6ebeceafffd937fe2447befd56518bd3d4b93f7fc18cbeb575e8cade7d56215f37d5165d6bdcddf4efe9df3022d3fa92fddf75bd79b5bc23e12ebbf5e0a9bfc40107743863a66a7f53a2feb04adfcd1d55a99deaa22ac855655d3dfde1915bdffc20b1bd3bee3676feb05af5ac8ceff895fe9ee84c46bb7bff42f257eb5e31b7e33673b7ccf43fbe6cf27b778435895bdc9f43f86959f3849afcbfe39db4a73b0c88bf3d410bf9ef578f69434eba3e6198107fe3c5dffe31a0d7b0b93d42a597dd7413e2c401077438c3cba37e5e1e5c7706759fe0b32c59b06c3be8d1278327afc4d3431bfac314dd7faa5854b824dfb798c12f6c94654d393ffe2a56b7e89f6dc2b14ff5431d3d97cbd37eaa604fefbefe8cbff6873d373b3ebd440c4ff9229cbddf4bdb4a3d94bd35f237d36dbd38a7a6ff7439c6b1c8c6d7ff84b9aafdff12fa6fbf90dd7e58bfc4010774385bfeca67962bc6ad0b309e6d8751dae9f42d5fcaffe9af0a756c53d1cb8fd34995b85b0cb40fd4fe0a0102076a444367d94ae54fc6221cb194fb48a6fd0ea3bd237e8c632549bac32e4febfc6604ededb6fbc3fe1bff60ed405bb98a5008e7fda6be89238d417327d2d4f5614687910c126d2df8714c7caf21ae8146e040b690c401077438f695d77106dfddb58215f4d9dde4f6df9ea43d7affa045d77f8c7ebbe13310cddff13cf5f7af09f7d3676d3784311eb7f54765edb3c2027f11cb4dfeb7bf4ddf2f5d9a412820d73d66ebfdbaf4484f7eff7f902823afaf92469db7e5df9ab274db896fcb5e5dbb391afeae3684eff7d28a6c53dff9c9bffbdb6f4d627fb267f4c401077438fc485c9afd23abefe822b7edde18acfab13ebeb64fa7ccf47ea23f09a4fe9fbfbc9d7de08b67a7d5ef1ecffbb3d57fea2c9bf5e1164674f3a4db1b36577896cb52f9df63ff14cc67c71a947ba557fa7c8aaa7f6856df937e9af5279d14574dbbdff2efe9db6712bf331d3dcc504ae30bbb6feeb7a3a2ab14aaf18db45dd972fdc4010774387f17d367bf27dd655c85ee4afa8a7dfd468adfbfe95f8c47a8bb3e36250f203171c32a18e8d649bad8b9ff1907293d7ff1ad774ed99b594b6f6bbe9d99175510babfd4496c675b7fdbf4146ea17e83916ee6c6c3020f63d73d12ac097edf8c34fdcc43cba50267e3ba3c72aa3527604c993035ffb4e4d7f0f1c9fd5f54145ec5c401077438e48b98b56545de4fce641daf48514f2b696d0230e6eda3afe9f5425a7dd7ac21b225bc6efd377d58c7aab6fa58fd3f55cc4dc90e9a4694725e3665cb6a7cdab27f3051e872c16bb78c5cf773ac57676d1b127d64cbfd27b6145edf568ffe5ccb6fb826d3f775f1d62fe988f6f6e9af2ecdd6aca1513df7fc413aa051e996efbbc401077438f2e5cfb4bba6f5aa74c229bd3d7beb3b054af58f2ee827d694cc610d9ffdd2bd5fb793dd10d89dfcbf56976e4d7aa556f97a3a08b75a79fe9bec8c163da1baf663c993fc7226a27aebe557ad55a4ca5d2d555e3db797fe5ff2f7f78f69f9d21a19b3f26bfe237f66ce89fde6d19049bdd24dbdbecc105727e4ea4fee9b9025dfc401077438bfcccdbfba09e414ca84669c66ff29fa4c7aedf333536e9fe5e5ff9421cf44e9b3d9bc95f2101765ceedaeb27ad1d7210cac6b6b09e538f38546b1a567598fa84f41b2bbf492e89d3e4f6af40cb3c20f23ee5dbeed2c4589dd08f4f3746956ca6dd3b0e9aa4414de3dfe9e97c668889d3d915091238408d152a064dca1c6699dc4010774384de0064f4dea9bc1631aa7ba79e3d7c2e21e368397fa0d8fdb9f8c6dd79bfb733d7f8512780cd5e3ebe16a1824769ac489805dabd76a70bd15daff8c7209924558e6264368bee3eff6586dd6727f6eba8c7b0d28ee570ab136fe3c8aa57e1075e55932f4393ed37c629e5f6a1a4a546e2dbc8ed37fa7a2f1fbb4be104e180b46c40107743807259f1af919edded65a1897f6eeb4feddbb2c6eaa34867ebecc104d1a77fafb140a375b376fd6d254413cf7b55acb2efe6c257c4fba6eb75ab23f7d3a6c5422e96d26762f6ba1610e69aefb7ae4fd6bdc7bae7e495cf23feab31c2ce64b6febfd6ef1e714ee9cf9cd7707b931b75357cf0e379bbfb5049bf964f7a6ff37ab7bc401077438c172fb64edfdf29b1be7ffc6778443943dd96d61be4fcd07236a4cb75626d371475076edfb1a57b44052bbcbc56ffa6e64ffae5c166610a39d404aafc596081e0360908d69e2c278544695b2bdeae3e3534c4d361ecda067b73c0dc72cbcd3f759ae3bc4dcd9edbc9735ddcda5af96f50dd7bc0cc7d7138f5b818adba7fac6a2c401077438354f5bf2defd7d705da41763c9f43d7d378af4ceb2f5eebdfe97c710266ec4b517c4754dae4166c2e40d664ff18ec6ec9a8d8c3fca8adf9c729fe7c9e0dfa5043f3f6eba49ffe271dd2feb58b393bf7618185af7a4de5fbb277d7befaf2ebe96c845692cb2b7f684f8a9bbfa2c146e9fbebbb26fed44923eabfcdbafcf12f7bdc401077438cce83c3d67e249bfaca3623b28a90d820b0f6bf123db7c8d60e75a8e4b325db5f8ae99271a661c581dd78c44b50bb5bb5aa9f1562756f698aa076bcac13ffd5371b19d756b9e4fe9ff18f8d99b1f1139c24e4a440170f7f07faeba2cea49cab55f8c61c75c30621172f77f0752b6f90aa53af54d8ca6c992d9fad4daf7bc01adc401077438d8749faba6ae336269720cfd823d63c68a155cb139502ab4c4b9f27ebeea10ebb77eb2644a86d4f01d4f1b0637bc4eff6ce354f7d3bdbed0926fd782a650c0c7eb72016e5008885fce0e4ae3a25ec6d3f083e383bcd055a7e033d78c2bd858a3821d76e69a0dd2fe6d1a13341f345eacc88dae6dfd57887698e3ad929493e4f7c401077438bff1096b049bef0f65e999a5e9eb0528cd5eeb4afc26b4ee3e0ad940b9afd57b7782a4f064307740f49eddff335934d421e4fd74d2623a72efc95845e77a7f87959f2a68a73ad5a2ba6aa94d9a8cf764608ba7b7677f95e37713729ec9bff89dfbedfa1a958cac53162b43444d249e7e7b08f793ba1cd779b28c45fe9d2f8515c401077438d24d56314cbf153a93b6e0e43e95f532f3403b76844e5f039edb6b067a5570dee29e6c63a32a9069a7f4ff5d892a562279eb18533a54dd6ea6a741ea25c3badd3aeb64f4fefa0932b13a03072ef0ef8e2bd9f14f9f22790706b49e97420de3c493b7af2bae9ecb239a1d90d35f134bd2c3b7c4de5dfdaedd25f08b0fdc0398f9c4010774386304df6b16dd9fc9f4bfe31f2323d45d9e1f5bddd6ee4994b068aa4c7c9e9bff8c55d5fba624b21efdfadbea2337436666611c784b1a040d08d7327ed67fbf4b89eadd4a75347bdf1836fafa51894d8583820c84d27f3841f6a4eb85fe13e08d9affcd02493d863ac20d7627e3df3b1dfb770e8725d1a26faaa7bf7e275567d6c4010774389d3649b7fb1bd35f75647add962e08bcfdd8a8494dfbe93ff721bb7a27ee86b53754c4137e8d0b04b69d3adeebb8ed13668e9fb18be4dbf4efb9527c28e3c70fe437875e43f5755ed7317a0ef0a6dc71aefc7a5fc28df6b72cba5b2269da7ce7fcddadbf6a219381220aff67563f61fe8f5e9fd5f8c74a081d2f0c136f37f08ec401077438da88f01f7e8d61a5c9ea9ff28e41dad63a0bc70ae186d9d256d8aad411fef4bfca5d31b5b2bba04897d3c511e13d7ba796f8b4a40efd965edfb296aff4788f7bf37977e9634d97d6297a1b542be524def77d2ef15af648aceeea8229d98fa638e9e3f4fbc7364b198df9503477d293daffc3c846c5e7bf40bf0a2d40ef365d2dc401077438d72582aa2e3a64bf6fa7f1fc7d37ebd13d6bd0ac6305597bfe9b63d0ca9cc780725daa3c6c435ebd7a202946470793f6f7abaaa9ebcb597ae5ae21f1bbf47569fe5da55bbfd44b4fede887275d57d881725c55733edfb10cdd7d5513d7ff1ccacc6b2e06cff5fc7ba9a74eac6b566d64277f1d4c74f587d0d396d0c7ccf2bf25c4010774388ec95ea9fff842f73ec0802aca7c465661962a4256526d7c6b9707d3fdd78e442a4d70cdb4ba75bfff04ce13e3cd8f8f0e2625eb6f09777992f7fe11edefdff427bc25bf7ede53af79b7ef8e377d51f262725d19998f597beadbd39d2195a38417fcca5fceef21af517cbfaeb8c7efd995d0493dfa585fdf133f8c6f7d977f6ec401077438dd5af5a6be5effc47b7ba7b4e4733b1baf05bc75023d341d1b6fc402f12a9078fe7523483b5826c7b79f0c1b5224932b5ce1bad0f9ce0fb76ae09d8dd5fa26562bdb23d7a27ef8dffa84b7d74fdc12a765baf275da4bc9bff9bd7b6f751069be9fcac926f5793aeaf043bfaea2447a36cf345a393fb8efc435dffdf37b6555a1c401077438629bfab7b2a12ebcac53fb26de7e9d225cb6d13b7de70c89275e4fd35f8c2f7fd0c7a703bd63ba7cac413bfaff8c6b018d4789bb83b5606ea75319b7310ff47b4f076526fbfe77c84dfd727db5fd0c71a102f3618a30c463716bd0cac4c071867299d52e2bbbad15e83eff7cb7f93effd46a97be74a8b12fa4bcdcce92b36ee9c4010774383f508717e91cd4c7256b617fc6b62afb15e6f6348cf10c3377cc3263e0ee27dbc2dfff8c7865d47dbc9a3879defa77b2ebc97adfa1cbddf7d62d513ee8b2473fbb096d6dbff8957d3f7fdf7d5949d55dabd799be993d7d425bf7e5bc9bff9754fbbdaf8dda92bd64c906ed9a96af912d3eeffd949bbfbfac575f6e4bbde19d6fc4010774384e1ad52190df741353f95248af24bbff8acfa8526906e026f4fe1f5d47305b95fc6ae41a7b985dbb2d7dfc9f49ff8454a48e588b86c6637694c66ca4ffa4fb75aec8ddf1dfb27f779c2b20c69e3f5fa0ef9d6b8c9cd2f9b7fc4fec7eebdf67ac9ee96fafe6e7874aae21e3776de77e9cbbb362af36ffe4cf6acf79bdac6967fcc4010774389d1dd784bbfacb5d2524a304e89edd7f84b1effc9f75cdd0b66df5d85af7e9b90267ede2e9e9edda71136fc47b4f56e5ee98d1033a2115f77d13d69ec763d963e8c335b94775ccd6c9fbf18b7c128d2c301b542dff6ff68c243455e383eb28ab094f99ac6b96f407aeef1655aa4d191b856f67750485e9678ce94cf318c5c0d1c4010774389b8e1ee3c5aff773434314ee8da9e741dce839fe4f5f572107198cc27bd3a562a72718fc81a47e5bcbcb353fc2ba7c6b646975619a4043f7677790d5492f74f73ef15f83b179153fff1cab31a02bda8cfbf443cbd5e42bd7ecb05ce9d2f37422fd3e84fb98bb93f4a5a122f7ebd93dec43c8d4436f998bab39016eef8aff6d79c40107743844bbf64eedc4b7b7ccc33764bcff1715e4a365faafbc46f93be7f2749bea27a88051d937ba76c9e9bb36d93fbf54d37542ccf1831a4de9ebb5054ea9bf6fb693391f599082bd53fe2fbdb556e9a564608f5ef7e5dfab975642949ae1b325ed3e58ed19a08ab9b37d349c47af18eeefdbfcfd538adfebfbe6a7e889ff4d347c22c401077438f076c9d82fe39ba8caefa12f6c7633460ab77afd28b77aa76d3e41713972daa7db54a2073a65876e78257a3104e7591edbfb5652a367cefa18db9b6d24b48e5c842c1eff7f7778cc7ffbab30f233f808958f301bb34512ea1f7fa5bc136738fa1146949e63a7d55439d37ad3e48d3f6319549183057fd4dd42e26397b2d085b9c401077438df35ca227a43362f7111f5e4b3a3abf1a93e03aeb52a45b26227e1570013757b2ce780c7efdcf8f4cddac527231e696d01be22977fd2f8dc87bf49811b408b57884c45a6446aa7fef218e781833d9c6474b11a5a3154ffb27eff58ccfabe063a7db48deb81a5d087f9826b65cd7ee4aac20e6ee888f8f889e10952d35e2f2a82c40107743847dd09efc918e5d7b2e0d3e88e6bf8c72f29a2b38261db6804ec4eed9cedb5ffc22a7c22fe6d5da9a4da335f49bf8a0df569e117a74b7e3f196fb4add1e0b197fb1cffb1a4c43c126fdf747e4dfe8a5fb08bc91e9db93a54ae5dff88188f67cdbe4efbefbcbdbe4fb4c848b18d8af9814bd57b19fffeac632b37b7a7f7fcdb68c401077438dbda642b62346dadedf68ef47fe3b2cef1af74e7cfc8512e5f4fde58c897bf2b1b1bab7092587bad6913db4f5bf7db4531083199fbf7a7e7fa12cdebba0a144fca55ec9ebb43038a9ab349902ac280934fb1f82c610b43e1906eda345accae4b5f8c64b77eef57596e83fd0fe216e7f8d2ff0589d699006d26d0982be5eece98c401077438d57bc2466975b1f821f4f8465318f08f7906e13227fd74a9c3de25198611fe28a08774537efc7baf7d7a6a9ef34bdf3e93edfe2656f4d77fe4f595594b11e432b77b564c9f7277dd613efbb7ecb7d75e091081f63eff997e5bc5fbf4fbcdbb79d7a84bbf5f7b73435d8bfbc8d6ebe89bf213deefe3024e9e95e9e7853f4154a6c401077438287f8c1c7fadbfeb5c73a774b17437da68156b4fb7f1c83fe1eba03ce85967ff51bb0ba37a3cbbfc9eefeee314bd42cba04dade7d5d7a2f22d3857e34263aaacde36f27aa5fe145b0e20c46119b9daa7042990d2430301183673c839af7ffcf0729e42c569762cdbd7f159c63022a8547451015a905528d3b1f8d479559c7c16c401077438bd6eb5e854fc83bd12a59ad922f0a2dce0133ebe5a83fa4a05a5ef75c629b17b5ff18bd72621acc80fa598b9ea1d86d83cca02992f9b3eb6386949a7e3483d25a6dd2d0ee1e4fa4bf718fe081ea707dc685b84358176ffeb3f355912ccd36cead61fe653d4bfc28d715a7faade145087b4ad76ecc5533cd8ae93628d557bf913c4010774386a5ea7c18232c08bba718f6a3569bbaea937dbe52841305bba55a65d442b17d17a3ef65d94bd084f3d28fa13ed7b5fc163d537d3a7ede67824fb7d4bf0837b4d7d74f132757f608ba6b7de34bbdfed4b977daf393bf6bd8948cb76d153c9eb9a27b13ed1b7facdbfba20917bcb1edfb97f6fb7339bb436ad5fbf45fa37ce577ac40107743856fab1b7879905fbb6ffe3daf4eb4a9e84c534feba51d4fdfe9b7a1517d374e8c9ff8febdf9ffb75795267465493cb36fb6e8845e89b67275cd688d5497cae9d65d5fbb11beb56abd5aaa42f6f2f34d9e4df9eaed5f64efeb2effe5dfb49d366ebddf39b6a653d5e9edb2efdba97ea57bf4fe3811a34ac87697c7bccc84adaf7c40107743819df79d921f2a8589ff19f00d59b509458ca7adfbe8c64fac8edd60efdf2b34fd1194f505fbf4f5aa7fd37f18fc3b5cd51e7c6718ccbd0e8527dd1dde4215636c4e4b2149eb44fae054f4636fdf43deddf231bab90deb26a0994ef7f5bebbbbda9f937dbdb555bd3fb8c9315ef047db6efd9483d2ff7dbe449ac1c3dffb277f7c401077438dbf0994bafe51c2d6aba7bf1ddbabf5eddc30a5d6f3742fbaeba57f262f58b1513b40b776f7ed7e27ab75ef214bbf264ac1569cf7d78a76faf5937e6bb36efaf10dbfa89bb37d3d1d4cddac536a0494080eb90524fefef18840fff718a4f6896e1bb58e8cea3953e081ffefeffc89110c6fa55e3f5eff468d5f10d37dbb65fecc401077438aae9e8824126ebbd2713b2f5d657b515bd2f0e0a4bfe6f9b3e1ebafc8856feda6fac21eadcca4d5dfdd597d1d6f28417c88dbaa7ea666fda92508937fa2cdbeb76159188dfbbff269d7ecbd7aac819bd1c4b6fdbed6ac9775fe3b5efec89fe23d3d3a38eebcdc7b189de5f57d9cafae9348c593be436c74fbe6fbd6fcfdaf74fc40107743878c1513782574e69773ac27cf56fde6bb2fa39b515bfbfacdd710f4c690aaf579376eaa919effe13eef5ff049c74f90b7c9ebfa65e9de57d1a3c9f5fccf5a42daf7d24c4eeb68ccacff09f5dfa769d0458f4bd0ee9606b557f58c65203eb3dab8f9e044328ab4fb28f03d6338c79a75cdf0aaef18af13ff7d685cae71ccbfc12c4010774387667f1ebf7b094e960a4f4e9f49452308fac897a2bbd44bb7085938572a0599a8661ce2ad2d26937790a13ebb2a25a4a46cdbff89d7eb4ec78990dbff93ae93487ff2abe6cb9f6db54df3fd3ed3a63966a41a129ab5cd0fe12dfbfab948d5a964102517691b7f162777efeec16b7f1d366e772fe273febf7edbca37b1028727cc401077438f1cc73bfdf439137fb16102e5c67fcdbfde08f56f7327a7558742b7a4b49f3d1bf62c4cd870ada6bdb44df9bc27d7b795e715f45d784fb7bf5e2fd6fe4fdffc53d3e5e4b3afb08bb7bedfe25790a4f5efac9ef91df8236fc52adbd7b5c50ccb74bdf82dd7aa3dfa26bc7bdbdf19b1dde4d3ea91c46fef97deedf5884f44fefd3c4010774387bedbb72f824ebb5710453facab6fdd33e574ff18ed8d1a2067b0f490354898c5a0e6487514be19ca26051e8164899ea83ad7f93ddaf570a29c7d3fe2c6be3bee0ce70ed93eefcdc625630709fd60cbadae9204849f8fde316e918d22c58cfa79d607effedb948fdd50b302a65feee7feddf49611c9d79bd2f77ba237c90b36dc4010774385fed137e93c4cbe8502e7b6ed6ddb1613dfba3f5977f27eeafe6d9d37de4cb2fa6626ff65ebcb277eddda11bbf76fa6d5a9b88e0b5a59bbfddc508f6efe9f103df0dbeb8d6633df5937e938ce6f93f15ea27a30a04bbf7ee78d639ee9bc6e8f6f69171825a5bf6f514212c3a7ebc432efd5e6dd3d5cafdf5e8daa893935ac84dc401077438ffc5faefabfb52fbd5d886bedf899b7fb344ebefa6e0a7f631e6896d75bd6ebaf7cdc640968edebe22f447fb4d64f79777d13d5cfc47bbeea95e8d8490caf7ebc8f2fa6d6be49baecd8deb4edaaf66cebdfa5a7737f8224dfb7729b5fa50459e88f676f93fa2e3b0feabe9a93cbd7b6beaf7d5bde9d022dd0be93c5f4d9e9eafc4010774387a36dbfa047bf7ab355fb12436ff642d3b7c16bcdfab1d9480916fb7cc5d3f4e52a44fbc9eb5fe14611beb081fd94fb53f1bc2c89ba225c11ae295687627c9faf5b8411e3c6c44390bf9719e47e567c9fed91ad84f7f4f93dbaf6b2697f9b5ff19b675361a912a0501bf499469d7f18925f6f049f07fb7fc5eb5126375f4644dc401077438dfdf4f27fdf5d26a31236ff68c2137a76dfd793d6ff13d7c4cb69176edfc82f3edf4fdc8277fd3d14e6dff29cdbff8437eff5da7e6f4f6d0d2924cf067f124fb2774eb908c5be9e2c6821043a3db7f187042c6d4bfff62552b20cb16e9d7ab8cf2bf45c9fbfc29827dfdd3b3a1126fc9d82e779f3b55d04f92acfbfa2b33cfd5c401077438612dcd5d7af27bd2536c76fdfbfe7e6d38ba6deebafbf3c27bbfd3e2359d04baf6f7e2f5ebebcccbaaff2efc46dfe275efcdefafac9eb2bf844116fda5afa7047bf6dbf54a9709e45244d9b556b5e5d7c9fb6bf412799eddf57c95e11fa6fd159c965937fe18375be996ef083d13fa610870f41056f4a66b5aaaefc63161c770c4010774381261d1b4b84fda44d95ad2332ed7c29ef5f5e100c44f8c5fd253c51f48e928a7b889af7afb48492847bf7f09ed51f7fc172146372b8e35f4373f0548cd11f4b4fbc38ef8329908e9d4ebb62ebcbf18efd1c840d0cc426d4d4d31c7547cd6b2cbadd9af7a839fdaec28099a32a0afd72eca20dbfd4aafe78a7a51d56abe91b7afc4010774384d97edf8a57bf7f4fe4631eb3fc7bb7e5fb75f13a722929de2757f6560a32f37dfb74d6959fd552edd2fbc25dfd69d94a718b2c9fafe12056bdf896374f3fd3a1493cd69bc56fbf57e4fd4950c90a436fb735ced557a36d1ebefebc6c9de9e92a6d8f666adf293d396dcd9f7ab2164c6fa709f4dbd7dfab2e26f8caec10f9bdbc401077438d88ae22d6c592f5fba11adbbff89dbe9b7ea6096be93cbbd5612efdff82556dfa7aaa94dbf7bd5eaf2de6f4cf893bfda824dfea9771608bdf5a4c38720af2c9dbb6bc475eff89dd1c464f5eacdddba27ad7eeaf93ddeeea28da47528f7649748bfd4a4f7edc116fdba5f128575ebaec57c4fa7ddd50225c9bb2d5b2e4dfeb054c401077438d55be5dd3fbf1df63281d414ceb48f7c8cd47b20a4c9b3edb751ee059bff89e69e0d3b9f87cd4fc9f6dff845db076ebe1d9f6691c768603eabc27c68da9c1dd64d237ad13db69bc9c25eddfe9aee5aacaa2b7de09f7efebb3758838965fa76f119bd7e436bd6984f4dbe9a7d39b676f27aa6e992af7fbcdbfe41afa6df7313b7c401077438f44049ebee8a515d793f379b1334cf5b695bbaf27f40be313aabd1bad1aeda6dafd6eda7c72c593e9bc218f5dd04c632acbd0e04dbfb6f3dec5f6640a1cdf999edd480b97e1773f77df02b0b27ca22dd7447a7c5218e3061fe9f3d2efe4096fd9b76ed59931397d7d3ee4cfd7564887bbedf5043aedd64feb47703f55fca4045c401077438b67a33f2f2901466b76efdbf3797eea1766e957bc4de4a7e2359485dbb6b27ba5afaf489a8a7da48a317e239fbf4fde097aebfafc23bfb6dbcd5fd04fe9d953edf59bbf27da508ecebd4e6d6bd3317d6ab053baa77efd8f452693efc4bdb8dd3ba13ee32bc1176edc55d93abef2b1baf047bffc9d3eaff63dafcf86faf90a297c401077438be9d794808f9fa7ffd1826a5f9feb11a767dded2291941237a6df84db7cbd7d44821d2bfbf08a5c575e5fc6aedab34a0bb68f5deddfddef6fcd7ddbf651fc8a29dfbfd989dbe9c714a20126abb1d8d209dbf4db5d9480b93ff2fa6f44fd3ca2f905bd38e9c3b789764dbce924f5a92d710c7ee192fc3ed7be88418d6fa1f6fb9c4010774381272b04397d75d245f46de884047b387dc5dfca42effef56d1edc4d208eca8fac913c7e56237e9abe935208dd10697a51337d44eff6de4fdf2257327fef4f797f49d33b10e685f5d443f56ea99998623f6fd3ef346bbeca4d2ca9f738bebbfe73697112f7fa8c7b2dfc32ac9d8d6f6128826c88ae8e926cc813a4c2fc731f634c40107743899882569a4ca236a423288d99e99e84afa8a13ddf5fb8496ef7dbd517d3ea4a6d34493b7cda488a4e5f1f510f592f1034c9d066a368faf99a521f05abb97eaf1fcdf690921049bd7699460b4f0d294a7ad35881a0a5bf3f7d7563d1bbcac9e96c6b2f6ff608b7f5ed8206bbe7cd75fd3f314468d4d5d332cdf36aec854f0efbdc4010774381cd5d38a7375c728ffb68bcb8fe91422e5fe584bcd0f9a0a97fb6f5f2ab489ebd14db7e23cdaa77dbbf6244f9bf5b6e132149ea09b4de9afff78e5f52f6fd2ebd1c5eeba69db55284b7d527d51e13d1a23f8d37f9c8319197d35e7c4fdfa42df5d359a57edfa5e7209e9f67ecc6369aaf4e29ddddd7f12bd39bfeebd74a234fdc401077438df27ada3e50a6674f4b128c08f7efe6185dff8d5ade6119b7d9f9fb6820f7def7a7a1343de5eb9b75fa36be4f4d24d5a2a05bdf0f26a70fdf8f74facd4fb697b6b9117dfd90dbb7ae31efde416294f6d6fdd02dedb6ecdf6a5cecc273e6ad5bef199aaea1b32ce1a68eda366ff74144ec72b0ff7b73334fb54d32eb2dc4fad3fc401077438b726a3da7dca3defebc652ba2b572b16b8b82a73e5fa75b7882befabc153a1eeaf69efde2fb6dda4e9d652990f67fa5bb7fc50a63a2278ce7fefb6ca53b04bd1cd547f8bbadbc4339b777c9fe46a8c7937b7f2ab52a5a7b5e4f5521970cf7c6986025dfa69db32bb9476c4c9db6cb3dfde353d13e97345daf2b26cd51f90c236c4010774386befe24af4bee7045a3a34fba28927446a7a214bcffbd0b120807b7bccdf7ecd047bfcbca35fded78c189e1e6632c3ae9daf0982ce9e7aa0fef7ed2db9c4cc1fdfbb2a3a67ac6bfbf5123d27dbbe9b7fb139ffbf4d9a70d8265bddff6f3d45b4fdcf1554afece5ec9b2df48bd36fa21c1774d092454dbb7ca114ac27e8af8cfac4010774384fef2ea704bb2e9f6eb340582e37a7b62d9f0b3d5922bb8c2bf54da60ad7f2e54b9fee95cd94da8873177737252515dc57ac62ad5c8ebf21c56fefea8126bdc6cf5afc56fd7a6a1f2c79b7fe1efc10efef27bdf9df854e472fe825ade7dc9d75e27dbd3b636b377cfe21220e5f6aeb594ebc4777ef7f5461765ebdbab5a7b5efc401077438a7a8e5bb12b04ef1ba7ef16ed14de8b0932ff4e5be95d9bee5dd3de8f6fcdd56351baebcbbbfa2cbbf5a7ed5b5bdfadd9f5a11ef57f7ccec8529369908bab34489ddbeace999ebfcc4f5db9081c041b5620acba537dd883360b77efabfb46dff6107afe43b65fe9ff2eff6e2b7f375d5a213bfa13ada36e9e6aedbfe9b71ded5c401077438fbc955f9b7e954544f591ee476bc635d0dfc4efdfabfbf75beaf5bcb374fd65c592042cd8bcaf5fafa13abfbe46bdf593b69f4e277effafa9b7e9a642fe30b759776fd18d4b338ff29f6e4a5ac682f62e93678884deec7b91add57826e7823dfbd278972fad699b74fbb2a4cacf12b1ed3bf337ede595eff68464fbdd72cdbf6c401077438d7727bdda11ece5dfa7472d77821dff6a975637aa1013d7bbf3696413a6deebe98de850a5676728492b1dbf4e42468e676e7cf5ef29504b78c8f2846f56dfddfe1d33d7742d9bd4bffca0bb7f5dfe20adfb4d54be6edddc20279a9304086e6fea6a4b932a7030eb69e8f4daf9a85b92efb4bb7277d6e4dffcdd7d65dfeb26fd3c4010774385e34476fae9fa8e73b37e76e764befadfe6f3fbca57bfdf7e47baacbee9e8babcbbf5627dfde5efa54449be9f6fc49ba7ebaf2ef4fb9420db9e034bfefa93a6fc7985f7f5dea27696a52eff68e11dfbfd6f645655c86d1b2ef112a2e7a3fb67d64c1af94aad7e27d76d1b7d49bf49c8ff647bfb74f14c669aa9d13bc7bdf6f4ec401077438ab290bafe23568469e2d8910deeff59366be9a277eaf4f6f11282d6df6fff4511ea4d9a67aead9010fd37f44045df6de7844a4797db6ca357620809f5274fdefad05219054edf3f41e5e1a75ce69b151a8269a5dcd0a4decfddc8c169b7fc405011efce9d7ab61e290133a7b7b7446f363bdbe730adfbafcaea90d26ff611d5ec4010774385cd9c96b56429b775fda5ce4d12fcbbebb5bf043a2499ebe2354c7cf7e4ee9e96e207fb33e3fd5beb91e52623be5fe4ff9d7ebde30bf44f7bd90e7c9bf59b2af70877df9a07f91dc6c9d0afc86d7af7bff977eb517abf7f4e27b67afd7e8da4ff3c9a36f4dc471fb68fd3e9e4f5ae3bc63bad3d32bdf4fe99565dc9549c8f94bc40107743826ffd6d6a1c929c432f9184ff9ff84def4f55219cd01d382da14f089b35aff841ef2a2920d7a761e5d7971fe9e97ce6dfab37b3fd49b3656fa412d7f7bb4589047b3a76a6adaf678adf6edb7f5d190a76fbfb38f6f7e4c7aff9b7e9aab6294dfbf5901f911b7d3a53da14bfddbf824e9f5f2f964dd3a76a5149efc884f6f7a7ac401077438c824a2825bfbfd84846bc4b4f364d216b6ebb9b0d5df27f4527920aacf2f7baaaffdd3653023cb0a7477b1295fa6718f2ee2e334ebbbef1cb5ebac9fd9609b7ba72c3d6949717d37d7b53105a058e9f23e8aeaec9ed5f8a98fe61ef6ebe4115aad7946024dbf555593deb7117045bf83a6827c9f6e9d93ebdce8a3cbb7f93545c40107743888edd6dd3fbe6a9ebedcdae7eaf1edeeef3453e9f8b139e88fd48a2b4d78ca6d64204f75eddbe997d5bee564ddf14117b69179eedbf6a66528956fbff0965a773fc84ff56ad8aa7cc6427d5b2e6e9a564948b2f4873a9727fdff8418da02a2bb741f5f823ebd7944ad55e6dadf9060f74f4e5f7bf1251ced79faf5a9bc946f59c401077438b7f27bd7e29f7f9793eb4788d8c0832fd4edf7da95d792237efd78f74cccd76dfd4f4da05827f42fddf2c6a9dca9df8872626fdf608b7edf8edbb3d137b719e5c788f47bf4b0a18713493ed3289cbb4c48ff278718fc9f5d262526236fd9bccaf65208efdb47bb216d6ba4c4b232bcecf58279015f78c4ac1f1fd5f7ddff1cddc401077438d8ebd93bd7d041c6632bf71db9defbc6371ea361be549da5ada70b4b582e6bcf9b1a73d3087ae1b2ddfb7215422f79fddb9795893d6ac4a359336c03176fbaadbaf94c2f4f56ff516958be9fb37d2f2cbd7da2ad6dc8a4083b8dafbf4f8841046e9fbde4cede54c1637f4df76ade88116b345375eebbc672c34d3ddbc6ffc3d3c401077438b64208f975feddd0b18eb374e2b25ef999c9eea643dc361c0882974cb9cac457bf6682eebd36d3af9028be6d952c98b9921e036916ed7c25a53660589d2f4fe58c5df27df09b21d84d02171ccdfdf344d9aa7c5579d5ae21fe3b94ec6dd62b34cff8b6e9f30455bcc18577b09977b7bb425c25d1bd37fa413df9e8feceded9ffc401077438a0ca57327f8814994882a292b3ea0df8ff17f6fbfb5aa38fa045bfadd560a9e1fabbaeb7da9dd90a236b9abea922101332fdfbf62609b9e28f735befbe528922dbfaff78ff7eb8643822b8fc9ebc9f0a04b7ae937f105efa6d3a7ee0a97ef265bfdbb65a09bbf93db776392436e7f7b5f6b2e09b676e7feba7093973d7b45ddfc401077438f623969ba7bb57bbfa2a0969a7bb3b3ddbeeef6a6597a7aac77adbeefaac63be4cc3bc3e5404491e3bbbfefef56e9a56f3b2efd5623a11abd793f48e04e16fc11b3fbbf5204fedbf5abebcdb0e223ab97bb9be5b2f2d3aae3aab2f5de93cfd7f611efcf34eab7ab39453b8cd372c7ef72521f4b31bfbe4ade4f5a37c5defeb32c401077438c27d3d2669fe08f7adff33b9fd51596105ff5cbfcec9a2d3f82669beef6da8f2438117578f7bf7e9947a3d5e5f3b37ee1fabd64122460b66f57cf9d226fe93e4c9fb68bbd1b6edf49175fce4274fecad961f145fa36ff642022e3a523577db944e13e88ab375beb4a58940a3a3dff7a04831bbcffd17df48c09f72532c3ebe73c401077438755fc1174f6dd91e115d327efb19bfa1209f9eabb26dd3ada653341476f59beeabfdab94918a9e5cac63949bc8deadaa6863c74dbba355667fcbb6948362d4d7aebc8708efd9beffde68dc6e785166f6c3e2385b6fe944ca34994c5d87f40a94ef797f342fe41213ebb6c93fc163debbf8bc78917bcf1bbd1f104047a7ca2d26c401077438761523d247d2946049823ea7fa6527a6a3137133af78e823edf5f895b27a9b97aad3458f204b452793d5bda4391bc7efc9bd6914682871eb1e9dcc9f7ee990a0b77e9ae570f8499ee778ce39df8e28296e1e547b2795ae5720e661b6aa62c923a7cd5f1367dfcfd65666bdb688e47dfe2208fd752de085dfd5e0a1eb2d36486dc4010774385720bb6536fc89e11a56b348049852efaf779bbf756f79eafdf2d344dfabe7f14dfd7f7ba796b56aae5a392ad04bcfab4aa887abfe579ffd9d564217dff36fd5ae4ffc88abb69fc9441ff8ed5beddbdf69f74ec4f92c3834dd57d887bbfb183f362f89bf47dfad09a6e2060989f797fba683e1e2eff666fa6ba7c225047977a4c401077438f1a856bd5793db535aeb74d8504fcc6cb5b3e883f27bd5941288051f86d975f785064c5df54ba848daff12f7f6acec85dfabfa26477712d93e4b16ee8565a232eff45e9bfee8b9f3efb790464f6fa7ed65f26fed32faaf76bf44554b12d5c85bafb184f1f9abebba294cedf64d7977eb36efef56adcdbaf4a2f97be950e61e76c40107743896b6ededa4d95f6fb4a41156f4ada56886dfdb999085dfefae1513d9a69bbea8a85efdfd5b4225df92a436fea8450a7bfd62fe9975ffa57c9ed2f99fd7baa16f781c069e4faaa3b09bbefeb7e9bfc1175e1d2b4390789936b93ebc291bc10a6fb4fd14ab1741b081bb6df8620abbe6fe9fde596edd3d4f49cc322d5b6e24e8e4c401077438dfece3c136febb52b45937963d13e9c9eebc3ccb05bf823dfe5d42008f7ed6eb757e97a44aa09c10ebe2ed64e20ced9b6de4297c9e945eefbfe6821dfb6d5c67d5f74f59d0540a1dfbf2116aa825aabaf15ed96b7e937a12bb2fa1ee8cbbe84faf5e9c11f6dbbd55135fa142de5fba27d25cd62cb4b182b36edf6e85fd964dfac401077438b793debedca6defe972fad72ec87e9ccc64e7bbd5b4d8afea0875efeb5713d1cbbaec8ddf27df850b8bf4aad4d5f047eb6aac9bfb7a25b7a754e235b4093675bf4e3bd75dfbaadbbd041dec6f7a77de476f4f47593f7425d43272f6ff43ddbf2fa7d6cb822d3d366ed28af92ad2043bb34f164faf094d3cc9d9fa1abf13eaddfc401077438a686b526f4fb18b56f30bdfeff8265edcafa56eca57bfe7382a77e9d32fbb79c4513fa3ef0a7a51ef7efae9f108133247eee6f02336df3fe9321cc301569a26fadfd76c1193d36eef3c23df7faf8e4afa7cec116bd8de452822c95edd6115fcac7bbda627bdff2413eecc90f7baeaba2956bce3defee8c1014a89eb4e9893bedc40107743842475d381b65cb4518aff9bde950e8d821e9f52de10efbe9d7d2317dfa4305eed97f6edd274df455da46d76d5627bfad5e12f5bfd2299db937ea89dfd5021ddf7789b49feba27ff1327f10277efeb162e2a9e785cbf95727f595edad9c56e9f7f5beff505bd7af575bdfa49ac9e422b793f2fce106fbfeff26fcf5219954a77ac40107743839b6eb6ad495fe9a11bfd76fa75e6e7523ba6d5d9cc4d7bca8aedf765efb6baabb5a42102deafbf5d64dfa6b3e091defa5cb573b7043bf7d35297e7455fdacf15b56860b7a4be822c4fa7d3fc4ba6cb7c9f4e889cf3eeca1a28be98a13bb7dfb6b18456f27ca2025a41e5173ff4e2260b89636fbfc40bd5045d049c7291bf562c4010774384fdeae78b14af4aceb040f7db9bbe1afd3eae4c637fabe7f24491ae4fd3729cb16c738e98f3e8f64fff7898a47aedffb8af6e6cff09755aea89ed2d1612eb1765f62359937bf39012a877c42cedb6e8cfb125a54c112977f27f5cc37f72197fb1c2c9eb5708551323cd67ed8a5aea26ab369b7aac52f2fe7f55d74faefdefeaec4010774389850aaa7fb15c94cbf9bc63a7e6615fe997c9afe4cc93fbb94bdfb7cb26d27c9ef72efeae60426bbefa09f2d35fb8f04ef2fbf5e19465bc9b561c282460b7b746f8f5d2204dc8c755e91b49ebbc5f7dfda6b82475b6efc7395292193fb7bc6287e378bea6eca18e8427c5049f74fe317674f9c523434683326135d1775e1072ac401077438909f97f1b8cf9b94302034cf095e23c7a6cdb535777112fe3f691b76d3ee9d79bbd79e5efbc4ff045d77af2faf95abf991b7f4bb74fb82e092be7c2bfba83d8eec957f4814775bf7fc1339a0b1ec77ba0a5fa758393bdfdb71d026e685cdbc4b45cfd73f17ecd9692384880b74e2421124edff13abf5e4fb7d9c209422d26f5ac40107743879f49f99047b37bfebab9cec5fadfe863369dbd3e2c83bdb4c6f48f46bf59b7d6aaac629fbdf7345f579231bfb13ab7c7b567eb0968435aebb6af27affa8c676f7d3f887fc22e3eb8e766a31dc8bbccddf1eac27c2d9f79c60d349bfd3b7323defedc74efac11f5eb4e1d96ddf547351916fec9f3211afa3b74e289413cf0675c401077438cb7f56f2e7a52c60a0937d7ee47baeb16a5a9a6b10c9dbfe13edfbf489d1513bfbeeb096fedeaf22dbeaac11efdbf7bf252823e4a6d75937fc4a045dfafdfbdf93ae4bff05daf9fb7b2ef337e16c3f2e37ffaff68a13dfbfe72025656376f7f4c26d6edf57abfe3919b9377fb749fc25eaaff9d09dbf6edf52ebf44647a6bb36c401077438fbece89a7f540bb52ce9bf7f6defe9f2422ee122b738b70f75a563f2316be106e0c5421f73364ef6f4be311f14b5926f95f77bcc846c4e369e1370a401baaed77598b31a2481d27e217a945dffcb3ebcb48df1ff497c67727de92f841e3e390dc749dd06a21f7c8d5b5379b75dad854abe9be3b57f6e4efe9204bcbf71e45ed6c40107743894305245bafbbb14345b76f3b1f9016faddbdfe16c9eda7132d6d3fdbb52b163e46d5054a6e6853de08b7f9fc619f7fc6bdfd61a3a5e1f23eff10cbb4bf1a536ffe2f545b7f9595b4fe7824d6cf2ec8c15efdcf32fd24686d45d0b42b746db63ba6bbdfb08ecf46d83facce19f37628769a950515c7d63c38a3c30bda6fff7b8c40107743893f56ad5420f91e5929a8cc7e1b7fbaac625f19efbcd0b51842a72856205f27dbbeea31e1ee2973951183b8aecc36ed9b5a73b50832b16c6f1664e51e8b3d6b02fa261d7b51dc7b3e5627603171ddffc7b7a56e3f954e3db562631f53ffa43669e687dc7ed73218add2e6846f2058d8dac7c872d37df5edcc4349b7b7b4c4efec401077438e4f3e7276fd597ab7ec6977eb89aa219eff88eda159aef56cd774c292ed66f7a6d3f793d166ebec833f047bf6e88f6d6ff821dededdcbd73e8d7ce45f24365f88fca4df88f11b37b56f4ad7db9d7d6117ba5e78646a7423d9f4f89704fb6bd77db7b937493f508eff4fa75655d8ed68af64c99d93d70d0ea6d749f63d18111f8c4010774386ea6b8c561a973977871c693e97fc28d95845a94c1b627f3128bc92279319d4785f86fda36fd56316318f1117beadb1c71470f6f1c322c171ba4f4d7af18ac38ec0c1d3dd9da23369df6e89ead96c336525736b634d13b4ab6eff8c6dd93dd2cf97fb7aa91ebed4616504fbfdd263f17a2449a6ffb30a6d7abda76357c2bd8a6c4010774380b16f5e7612f7f2c46fdfddb60570b6e808d7fc4fb77b68fe2f7eedfb21c16ebdeddad35885be319b58cb6fafce6058f4d579a17764f6e88c7a2b330a3beafc6697c3840fb2d9db82b4fb54d5d9e5d6cd7266c9ed2a466a5841ea1bcb28ab7617a143c5bf6c7a661e196aaddafdcb0e4fb75fc4764f6e37c9fd8d4aa582c4b9cc40107743846298e645452b30f662f2dd3e31e326e4358c72c80edddcc225436fd94d3ff1faf8e0fbc56fd9f9e8adfb8e7576e9d4ffa77595824deedbb4f91029f8925e1976553a4bb76a3b1a88f43683ad589d0a37d5d3c9e9a6a7ab8c7b70d4bf1dc3c64dac2ed6e5f27ed244ab631971fcdadaba9c96d98dda4de0abd52e76114932b8dc401077438bf48d4ae58727d6ae7e3f5e8296544c256166167f93d3b2c9c5a0a2661eb0d8accabdba7ebce3f6c9edfdee3388777e0fb6648994a994d6a0ca4fed6b516a6031e9a4a150dee4b887f76e7c9fa679b7fa4107a6efdcac7caebb26ccbff935e958b84115eeac11cdb69fb33aaa2f5d79b5f584653cb7455edde531375f593bf27c401077438df847e0b9dbf57dabe4565dfa049adbb3c6115e477544df971ccddfd4b358b955eb3fc5208ba76fefbb705d237f9a237faebed6ba46376e9cd774c14ecf7adfc610116fdfc4f590a6fccf7fbf27bd7aeb14b90474f5ef5f27d5ff84744bf462aed991e6b78e95650c7e0a59f74fa0dabeef08bc3349122bd665e4fd513f1c8c6c4010774387ff5f9fc9eb5f4e395b7f437af584538a47a35759b4fd409dfbc63c0b27d1444d05dc3b2134ec0be95b6435855d4ab8fdfdbeccb587c444bad2fadf6fe72822edbfa457622bdbc521a457edc13767a49a78b5457ed712c16b233dbdce8a5dd12e1376e7cf4e3cee08f7d37d7225d1e217263e9f93decd0842f6fbcda6fe9a04fc401077438bfa7c1d64edd3d5841b7f3fd11d9ee4ddbeb673423ef23ccb0f8c454d91ecfb7a2610db6ee79d2d22327dcd927da4bea09fbbcf54d9ebf18f272c37bc5b9d0069edeecff6db468c53da2f632f972ff793fc546a75189f57fbf6cd19c72136eb63dd69f74807732b4a5a6e353893ddf272c6359378077f9328d9fa2771bef74f2c40107743855dc9f693a9a3d82aece91621eda976d8845b6575d765423a657d27e5bfd7aad9c47af6dbacb15376cd5d25d45557aff2ed4d4f6853dd7eb08a386dcff8bcdb456f27bee4bb88f3ab2e3a8aa10bd3b9223898e030618de58f8deb27f7fa8f5b9c40f48cc0754d7f784f4deab42a6348bef093aa4f9ab93fa3ae9b1dbae14606cc401077438c2d2739a5e4f5dd4b2c42c66a324af2f932f3dc525a542de1e39d649f5367e4b44d6d2ae2f1f81c22d676477967a1d55f8e57a7f1f50335dcfb09ee9efeafcdeaba10c5e9b7a74faeef093d7a6eabab13bfb75f8b73493d537646f76746925a8276be5eedb5d1c5affbe8d37a5d65d14f5ed76d07e99b6d576ae68454dbbe537c401077438cf31c3bf8c5bfd7841a6e9b237cef460d771f16e53f785f97dc62b2375f3d7c3834ddc5ba45a8247b58729ead27ab5fe0a1d42f8d53adbae2dca25c4a94d293dd79f1c616e33e4fb4bc7731f222e2b4c99fa36ecff8bd7d99545489fd667fbc6f32946c52f27f9aee4845592fc6d0bdfe2f49b7dfd28bc730738f61933d91565c40107743886dc5e4525ef7487d4866b1234c6d72b050e1f2b9dd8f17b62dc4bc6b19bd63128d3e2dab9224196a6d48d33a3c5ec5ae21d1a4810b9cd93d36874a5906c5bc077f577b4b4722ae92bba69904c20bf6e13d97c869e3ed29dc215c9a5a716e89caade3df0256bedfdab0836ff8c7aa569cc221af69a53422f5b626ae099b93467c40107743850a01e064f74eb19b08bbf660205ddf4fa541869fd32ec6928372f41ac20efce9030e815a63db1fc22efc0bfffb48b5da21e2447049f75fd978f145a060ddb796107bd4bdff6401735fa57bb61048cfd3b3db0c04f50dda8018fdd316e2ca6907cec18cd2aa9b7bc5b979a14c32f727d5aa866a107ac438ebfd4dd4fe6f0d51ac4010774389deb8be7db6cda448727af5dd845a175dcac36efdc6e9fc638633c6835618f4367c8ea048ea3d5fef59240d7aa4726f26bf79f18f6beefb14f1b36db04b55885b0f351f39b8ebfedd6e4bc5f082b8cb21b4a6fa27218b7d98b25c85bad821f5e4fba72c9c5ed11433dc0ff4812ce8e4fb4973cb0a2e5cbc62efb4f71e315f1bdc401077438de2f6c93db25b37a5e4fe9ef717bf698870c941bad4d7366325906f4cbf7a519db4f353cbf4f4d3d3845e865a7ca67e5841ee1c6d8fdb3feec5c5dec89145ad4b8dfd93fdc73ca4b5c0e9fede9c20961a66674fb757ed251508b44102d455dd6321cbbfd03a3c9efb4ea78ad37a0dfc9f6929e326ef233c7b9783bf4a1d9af1dc401077438f4f8a886186a60eaebb04cc8fcff1e830640b4e0a472352111fdd7f937fbc9ee966ae0a50cb9fd345fbeb79e226d1266ad6a9c28de9a74ee3346f8fe593d5c9d5463cb09350e26812345f1ed1feef58f46e9e6e21a13373dfd79e212838d7b5b24a4ffd1a238da2275dfe85c176bdfd74a5efee2056fdb6fe42021e9d3adaccec40107743821c68e19d9de4facbcf2bd3559724bd550bf5eba68ae369d74feabc43d6b1ccbe2393c7bf1e6caa7c42d87b2bfa88428c05c8aef5db59212e7823f4f759231bd2e6ac7e37ed7b4ddfcb2efe4ff74ff6af88cf074d3b7f51db3656ef6eddd7932c635ddd6a4cb417d5288dd933a346888dd3895c656f7bfe298588a4f4ff828f7c401077438dbfabc7a665ecebebf5a2f998e553aeed6869dbfe4f7afcd10c116c17f27c68af53ea923c42f14e6449f27aa6a5a5884b82f72be1cea5f766898fc6305aa67eb523437b593d2cb24ba6ad31e949e96a591e210daa2e8ef247e0e6a57fbaa71ea31cd02dae9f6dff83587172ebf76a58e4347fc78cd8fd59b599dbb7a6b2472c8c40107743883dc342e967cbe97b2442854fa1e9e6fe88a3c21fcbed54b1c9a74f506df4b53db7f27a4b9e6e4df74e4f7fcc4df1d65ad74dbb8856dc4171917e4c2feefc72a33fb2a3bede4f495c9c8bcb13b2c9df7dfad93d35552dfd423e898b5a9a1d3f8f63d661a79ac7d2e9c22862b69d1fe8afe4f7af5504abb5bf075e2fcbbedacd1c401".toCharArray());


			RTMPProtocolDecoder decoder = new RTMPProtocolDecoder();

			RTMPMinaConnection conn = new RTMPMinaConnection();
			conn.setStateCode(RTMP.STATE_CONNECTED);

			//lastHeader: Header [streamId=1, channelId=4, dataType=9, timerBase=17265720, timerDelta=0, size=29435, extendedTimestamp=17265720]

			Header header = new Header();
			header.setExtendedTimestamp(17265642);
			header.setTimerBase(17265642);
			header.setTimerDelta(0);
			header.setSize(365);
			header.setChannelId(4);
			header.setStreamId(1);

			conn.getState().setLastReadHeader(4, header);


			ByteBuffer byteBuffer = ByteBuffer.wrap(_resultPacket);
			IoBuffer ioBuffer = new SimpleBufferAllocator().wrap(byteBuffer);


			int limit = ioBuffer.limit();


			decoder.decodeBuffer(conn, ioBuffer);

		} catch (org.apache.commons.codec.DecoderException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testAddMuxer() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		MuxAdaptor muxAdaptorReal = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);
		MuxAdaptor muxAdaptor = spy(muxAdaptorReal);
		muxAdaptor.setIsRecording(true);
		muxAdaptor.setHeight(480);
		Muxer muxer = mock(Muxer.class);

		doReturn(true).when(muxAdaptor).prepareMuxer(eq(muxer), anyInt());
		assertTrue(muxAdaptor.addMuxer(muxer, 0));

		doReturn(true).when(muxAdaptor).prepareMuxer(eq(muxer), anyInt());
		assertTrue(muxAdaptor.addMuxer(muxer, 480));

		doReturn(true).when(muxAdaptor).prepareMuxer(eq(muxer), anyInt());
		assertFalse(muxAdaptor.addMuxer(muxer, 240));

	}

	@Test
	public void testIsAlreadyRecording() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		MuxAdaptor muxAdaptorReal = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);
		MuxAdaptor muxAdaptor = spy(muxAdaptorReal);
		muxAdaptor.setIsRecording(true);
		muxAdaptor.setHeight(480);
		Muxer mp4Muxer = mock(Mp4Muxer.class);
		Muxer webmMuxer = mock(WebMMuxer.class);
		muxAdaptor.getMuxerList().add(mp4Muxer);
		muxAdaptor.getMuxerList().add(webmMuxer);

		assertTrue(muxAdaptor.isAlreadyRecording(RecordType.MP4, 0));
		assertTrue(muxAdaptor.isAlreadyRecording(RecordType.MP4, 480));
		assertFalse(muxAdaptor.isAlreadyRecording(RecordType.MP4, 240));

		assertTrue(muxAdaptor.isAlreadyRecording(RecordType.WEBM, 0));
		assertTrue(muxAdaptor.isAlreadyRecording(RecordType.WEBM, 480));
		assertFalse(muxAdaptor.isAlreadyRecording(RecordType.WEBM, 240));
	}

	@Test
	public void testAddID3TagToHLSMuxer() {
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		MuxAdaptor muxAdaptorReal = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);
		MuxAdaptor muxAdaptor = spy(muxAdaptorReal);

		String data = "test data";
		HLSMuxer hlsMuxer = mock(HLSMuxer.class);
		Muxer someOtherMuxer = mock(Mp4Muxer.class);
		muxAdaptor.getMuxerList().add(someOtherMuxer);
		assertFalse(muxAdaptor.addID3Data(data));
		muxAdaptor.getMuxerList().add(hlsMuxer);
		assertTrue(muxAdaptor.addID3Data(data));
		verify(hlsMuxer, times(1)).addID3Data(data);
	}

	@Test
	public void testWriteID3TagToHLSStream() {
		HLSMuxer hlsMuxer = spy(new HLSMuxer(vertx, Mockito.mock(StorageClient.class),
				"streams", 0, "http://example.com", false));
		hlsMuxer.setId3Enabled(true);
		hlsMuxer.createID3StreamIfRequired();
		hlsMuxer.getIsRunning().set(true);

		doNothing().when(hlsMuxer).writeDataFrame(any(), any());
		String data = "test data";
		hlsMuxer.addID3Data(data);
		verify(hlsMuxer, times(1)).writeDataFrame(any(), any());
	}

	@Test
	public void testID3Timing() {
		HLSMuxer hlsMuxer = spy(new HLSMuxer(vertx, Mockito.mock(StorageClient.class),
				"streams", 0, "http://example.com", false));
		hlsMuxer.setId3Enabled(true);
		hlsMuxer.createID3StreamIfRequired();
		long lastPts = RandomUtils.nextLong();
		doReturn(lastPts).when(hlsMuxer).getLastPts();

		doNothing().when(hlsMuxer).writeDataFrame(any(), any());
		String data = "test data";
		hlsMuxer.addID3Data(data);
		ArgumentCaptor<AVPacket> argument = ArgumentCaptor.forClass(AVPacket.class);

		verify(hlsMuxer, times(1)).writeDataFrame(argument.capture(), any());

		AVPacket pkt = argument.getValue();

		BytePointer ptrData = pkt.data();
		byte[] id3Data = new byte[pkt.size()];
		ptrData.get(id3Data);

		assertEquals("ID3", new String(id3Data, 0, 3));
		assertEquals("TXXX", new String(id3Data, 10, 4));

		assertEquals(lastPts, pkt.pts());
		assertEquals(lastPts, pkt.dts());


		HLSMuxer.logError(-1, "test error message", "stream1");
		HLSMuxer.logError(0, "test error message", "stream1");
		HLSMuxer.logError(1, "test error message", "stream1");

	}

	@Test
	public void testMuxAdaptorPipeReader() {


		appScope = (WebScope) applicationContext.getBean("web.scope");
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		MuxAdaptor muxAdaptorReal = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope);
		MuxAdaptor muxAdaptor = spy(muxAdaptorReal);

		muxAdaptor.execute();

		assertFalse(muxAdaptor.getIsPipeReaderJobRunning().get());

		muxAdaptor.setIsRecording(true);
		muxAdaptor.debugSetStopRequestExist(true);
		muxAdaptor.execute();

		//execute methods throws an exception because it's not initalized probperly
		
		//recording should be in same state
		
		assertTrue(muxAdaptor.isRecording());
		//check that pipe reader is set to false again
		assertFalse(muxAdaptor.getIsPipeReaderJobRunning().get());


	}

	@Test
	public void testBroadcastHLSParameters() {
		AppSettings appSettings = new AppSettings();
		appSettings.setHlsListSize("5");
		appSettings.setHlsTime("2");
		appSettings.setHlsPlayListType("event");

		//If hls parameters for a broadcast is null, it should use app settings
		{
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId("stream1");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			assertNull(broadcast.getHlsParameters());

			appScope = (WebScope) applicationContext.getBean("web.scope");
			ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
			MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, broadcast, false, appScope);
			muxAdaptor.setAppSettings(appSettings);
			muxAdaptor.setBroadcast(broadcast);

			muxAdaptor.enableSettings();
			HLSMuxer hlsMuxer = muxAdaptor.addHLSMuxer();
			assertEquals(appSettings.getHlsListSize(), hlsMuxer.getHlsListSize());
			assertEquals(appSettings.getHlsTime(), hlsMuxer.getHlsTime());
			assertEquals(appSettings.getHlsPlayListType(), hlsMuxer.getHlsPlayListType());
		}


		//If hls parameters for a broadcast is not null, it should use broadcast hls settings
		{
			Broadcast broadcast = new Broadcast();
			try {
				broadcast.setStreamId("stream2");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			Broadcast.HLSParameters hlsParameters = new Broadcast.HLSParameters();
			hlsParameters.setHlsListSize("10");
			hlsParameters.setHlsTime("4");
			hlsParameters.setHlsPlayListType("vod");
			broadcast.setHlsParameters(hlsParameters);
			assertEquals(hlsParameters, broadcast.getHlsParameters());

			appScope = (WebScope) applicationContext.getBean("web.scope");
			ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
			MuxAdaptor muxAdaptor = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, broadcast, false, appScope);
			muxAdaptor.setAppSettings(appSettings);
			muxAdaptor.setBroadcast(broadcast);

			muxAdaptor.enableSettings();
			HLSMuxer hlsMuxer = muxAdaptor.addHLSMuxer();
			assertEquals(hlsParameters.getHlsListSize(), hlsMuxer.getHlsListSize());
			assertEquals(hlsParameters.getHlsTime(), hlsMuxer.getHlsTime());
			assertEquals(hlsParameters.getHlsPlayListType(), hlsMuxer.getHlsPlayListType());
		}

	}

	@Test
	public void testSetSEIData() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		ClientBroadcastStream clientBroadcastStream = new ClientBroadcastStream();
		MuxAdaptor muxAdaptorReal = MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null,false, appScope);
		HLSMuxer hlsMuxer = mock(HLSMuxer.class);
		muxAdaptorReal.getMuxerList().add(hlsMuxer);
		String data = "some data to put frame";
		muxAdaptorReal.addSEIData(data);
		verify(hlsMuxer, times(1)).setSeiData(data);


		{
			hlsMuxer = new HLSMuxer(Mockito.mock(Vertx.class), Mockito.mock(StorageClient.class), "streams", 7, null, false);


			String streamId = "stream_name_" + (int) (Math.random() * 10000);
			hlsMuxer.setHlsParameters("5", "2", "event", null, null, "mpegts");

			//init
			hlsMuxer.init(appScope, streamId, 0, null, 0);

			int width = 640;
			int height = 480;
			boolean addStreamResult = hlsMuxer.addVideoStream(width, height, null, AV_CODEC_ID_H264, 0, false, null);
			assertTrue(addStreamResult);

			//prepare io
			boolean prepareIOresult = hlsMuxer.prepareIO();
			assertTrue(prepareIOresult);

			String seiData = "test_data";
			hlsMuxer.setSeiData(seiData);

			//it's annexb format, it means there is not mp4toannexb format
			//it should be 4 bytes for start code, 1 byte for nal type, 1 byte for sei type, 1 byte for payload size, 16 byste for UUID, data length, 1 byte for alignment 
			assertEquals(4 + 1 + 1 + 1 + 16 + seiData.length() + 1, hlsMuxer.getPendingSEIData().limit());

			assertEquals(0, hlsMuxer.getPendingSEIData().get(0));
			assertEquals(0, hlsMuxer.getPendingSEIData().get(1));
			assertEquals(0, hlsMuxer.getPendingSEIData().get(2));
			assertEquals(1, hlsMuxer.getPendingSEIData().get(3));


			try {
				FileInputStream fis = new FileInputStream("src/test/resources/frame0");
				byte[] byteArray = fis.readAllBytes();

				fis.close();

				long now = System.currentTimeMillis();
				ByteBuffer encodedVideoFrame = ByteBuffer.wrap(byteArray);

				AVPacket videoPkt = avcodec.av_packet_alloc();
				av_init_packet(videoPkt);


				videoPkt.stream_index(0);
				videoPkt.pts(now);
				videoPkt.dts(now);

				encodedVideoFrame.rewind();

				videoPkt.flags(videoPkt.flags() | AV_PKT_FLAG_KEY);
				videoPkt.data(new BytePointer(encodedVideoFrame));
				videoPkt.size(encodedVideoFrame.limit());
				videoPkt.position(0);
				videoPkt.duration(5);
				hlsMuxer.writePacket(videoPkt, new AVCodecContext());

				assertNull(hlsMuxer.getPendingSEIData());

				av_packet_unref(videoPkt);



			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}

			//write trailer
			hlsMuxer.writeTrailer();



		}

		{
			hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 7, null, false);

			String streamId = "stream_name_" + (int) (Math.random() * 10000);
			hlsMuxer.setHlsParameters("5", "2", "event", null, null, "fmp4");

			//init
			hlsMuxer.init(appScope, streamId, 0, null, 0);

			int width = 640;
			int height = 480;
			boolean addStreamResult = hlsMuxer.addVideoStream(width, height, null, AV_CODEC_ID_H264, 0, false, null);
			assertTrue(addStreamResult);

			//prepare io
			boolean prepareIOresult = hlsMuxer.prepareIO();
			assertTrue(prepareIOresult);

			String seiData = "";

			for (int i = 0; i < 300; i++) {
				seiData += "i";
			}

			//size is more than 255, it means data length is 2 bytes
			hlsMuxer.setSeiData(seiData);

			//it's annexb format, it means there is not mp4toannexb format
			//it should be 4 bytes for start code, 1 byte for nal type, 1 byte for sei type, 2 byte for payload size, 16 byste for UUID, data length, 1 byte for alignment 
			int totalLength = 4 + 1 + 1 + 2 + 16 + seiData.length() + 1;
			assertEquals(totalLength, hlsMuxer.getPendingSEIData().limit());

			//it should totalLength-4 because 4 bytes are length code
			assertEquals(totalLength-4, hlsMuxer.getPendingSEIData().getInt());

		}

		{
			hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 7, null, false);

			String streamId = "stream_name_" + (int) (Math.random() * 10000);
			hlsMuxer.setHlsParameters("5", "2", "event", null, null, "mpegts");

			//init
			hlsMuxer.init(appScope, streamId, 0, null, 0);

			int width = 640;
			int height = 480;
			boolean addStreamResult = hlsMuxer.addVideoStream(width, height, null, AV_CODEC_ID_H265, 0, false, null);
			assertTrue(addStreamResult);

			//prepare io
			boolean prepareIOresult = hlsMuxer.prepareIO();
			assertTrue(prepareIOresult);

			String seiData = "";

			for (int i = 0; i < 300; i++) {
				seiData += "i";
			}

			//size is more than 255, it means data length is 2 bytes
			hlsMuxer.setSeiData(seiData);

			//it's annexb format, it means there is not mp4toannexb format
			//it should be 4 bytes for start code, 2 byte for nal type(Because HEVC), 1 byte for sei type, 2 byte for payload size, 16 byste for UUID, data length, 1 byte for alignment 
			assertEquals(4 + 2 + 1 + 2 + 16 + seiData.length() + 1, hlsMuxer.getPendingSEIData().limit());

			assertEquals(0, hlsMuxer.getPendingSEIData().get(0));
			assertEquals(0, hlsMuxer.getPendingSEIData().get(1));
			assertEquals(0, hlsMuxer.getPendingSEIData().get(2));
			assertEquals(1, hlsMuxer.getPendingSEIData().get(3));


		}

		{
			hlsMuxer = new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 7, null, false);

			String streamId = "stream_name_" + (int) (Math.random() * 10000);
			hlsMuxer.setHlsParameters("5", "2", "event", null, null, "mpegts");

			//init
			hlsMuxer.init(appScope, streamId, 0, null, 0);


			AVChannelLayout channelLayout = new AVChannelLayout();
			av_channel_layout_default(channelLayout, 2);
			boolean addStreamResult = hlsMuxer.addAudioStream(44100, channelLayout, AV_CODEC_ID_AAC, 0);
			assertTrue(addStreamResult);

			//prepare io
			boolean prepareIOresult = hlsMuxer.prepareIO();
			assertTrue(prepareIOresult);

			String seiData = "";

			for (int i = 0; i < 300; i++) {
				seiData += "i";
			}

			//size is more than 255, it means data length is 2 bytes
			hlsMuxer.setSeiData(seiData);

			//it's annexb format, it means there is not mp4toannexb format
			//it should be 4 bytes for start code, 2 byte for nal type(Because HEVC), 1 byte for sei type, 2 byte for payload size, 16 byste for UUID, data length, 1 byte for alignment 
			assertNull(hlsMuxer.getPendingSEIData());

		}


		{
			muxAdaptorReal.getMuxerList().clear();
			boolean result = muxAdaptorReal.addSEIData(data);
			assertFalse(result);
		}

	}

	@Test
	public void testRecordingWithRecordingSubfolder() {
		appSettings.setRecordingSubfolder("records");
		testMp4Muxing("record" + RandomUtils.nextInt(0, 10000));
	}

	@Test
	public void testRtmpDtsOverflow() {

		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		ClientBroadcastStream clientBroadcastStream = mock(ClientBroadcastStream.class);
		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(clientBroadcastStream, null, false, appScope));
		PacketFeeder packetFeeder = new PacketFeeder("test");
		muxAdaptor.setPacketFeeder(packetFeeder);

		muxAdaptor.setVideoStreamIndex(0);
		muxAdaptor.setAudioStreamIndex(1);

		HLSMuxer hlsMuxer = mock(HLSMuxer.class);
		muxAdaptor.addMuxer(hlsMuxer);

		muxAdaptor.setEnableAudio(true);
		muxAdaptor.setEnableVideo(true);

		ByteBuffer byteBuffer = mock(ByteBuffer.class);
		IoBuffer ioBuffer = mock(IoBuffer.class);
		when(ioBuffer.limit()).thenReturn(1024);
		when(ioBuffer.buf()).thenReturn(byteBuffer);
		when(byteBuffer.position(2)).thenReturn(ByteBuffer.allocateDirect(3));
		when(byteBuffer.position(5)).thenReturn(ByteBuffer.allocateDirect(3));


		when(ioBuffer.position(0)).thenReturn(ioBuffer);
		when(ioBuffer.position(2)).thenReturn(ioBuffer);
		when(ioBuffer.position(3)).thenReturn(ioBuffer);

		ByteBuffer directByteBuffer = ByteBuffer.allocateDirect(1024-2);
		directByteBuffer.put(ioBuffer.buf().position(2));
		directByteBuffer.position(0);

		ByteBuffer directByteBufferVideo = ByteBuffer.allocateDirect(1024-5);
		directByteBufferVideo.put(ioBuffer.buf().position(2));
		directByteBufferVideo.position(3);

		//audio packets
		IStreamPacket audioPacket1 = mock(IStreamPacket.class);
		when(audioPacket1.getDataType()).thenReturn(Constants.TYPE_AUDIO_DATA);
		when(audioPacket1.getTimestamp()).thenReturn(2147483584);
		when(audioPacket1.getData()).thenReturn(ioBuffer);

		IStreamPacket audioPacket2 =  mock(IStreamPacket.class);
		when(audioPacket2.getDataType()).thenReturn(Constants.TYPE_AUDIO_DATA);
		when(audioPacket2.getTimestamp()).thenReturn(2147483604);
		when(audioPacket2.getData()).thenReturn(ioBuffer);

		IStreamPacket audioPacket3 =  mock(IStreamPacket.class);
		when(audioPacket3.getDataType()).thenReturn(Constants.TYPE_AUDIO_DATA);
		when(audioPacket3.getTimestamp()).thenReturn(2147483627);
		when(audioPacket3.getData()).thenReturn(ioBuffer);

		IStreamPacket audioPacket4 = mock(IStreamPacket.class);
		when(audioPacket4.getDataType()).thenReturn(Constants.TYPE_AUDIO_DATA);
		when(audioPacket4.getTimestamp()).thenReturn(2147483628);
		when(audioPacket4.getData()).thenReturn(ioBuffer);

		IStreamPacket audioPacketOverflowed =  mock(IStreamPacket.class);
		when(audioPacketOverflowed.getDataType()).thenReturn(Constants.TYPE_AUDIO_DATA);
		when(audioPacketOverflowed.getTimestamp()).thenReturn(24);
		when(audioPacketOverflowed.getData()).thenReturn(ioBuffer);

		//video packets
		IStreamPacket videoPacket1 = mock(CachedEvent.class);
		when(videoPacket1.getDataType()).thenReturn(Constants.TYPE_VIDEO_DATA);
		when(videoPacket1.getTimestamp()).thenReturn(2147483579);
		when(videoPacket1.getData()).thenReturn(ioBuffer);

		IStreamPacket videoPacket2 = mock(CachedEvent.class);
		when(videoPacket2.getDataType()).thenReturn(Constants.TYPE_VIDEO_DATA);
		when(videoPacket2.getTimestamp()).thenReturn(2147483613);
		when(videoPacket2.getData()).thenReturn(ioBuffer);

		IStreamPacket videoPacket3 = mock(CachedEvent.class);
		when(videoPacket3.getDataType()).thenReturn(Constants.TYPE_VIDEO_DATA);
		when(videoPacket3.getTimestamp()).thenReturn(2147483646);
		when(videoPacket3.getData()).thenReturn(ioBuffer);

		IStreamPacket videoPacket4 =  mock(CachedEvent.class);
		when(videoPacket4.getDataType()).thenReturn(Constants.TYPE_VIDEO_DATA);
		when(videoPacket4.getTimestamp()).thenReturn(2147483647);
		when(videoPacket4.getData()).thenReturn(ioBuffer);

		IStreamPacket videoPacketOverflowed = mock(CachedEvent.class);

		when(videoPacketOverflowed.getDataType()).thenReturn(Constants.TYPE_VIDEO_DATA);
		when(videoPacketOverflowed.getTimestamp()).thenReturn(65);
		when(videoPacketOverflowed.getData()).thenReturn(ioBuffer);


		muxAdaptor.writeStreamPacket(audioPacket1);
		int overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		long lastAudioDts = muxAdaptor.getLastDTS();
		assertEquals(lastAudioDts, audioPacket1.getTimestamp());

		muxAdaptor.writeStreamPacket(videoPacket1);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		long lastVideoDts = muxAdaptor.getLastDTS();
		assertEquals(lastVideoDts, videoPacket1.getTimestamp() + (long) overFlowCount * Integer.MAX_VALUE);

		muxAdaptor.writeStreamPacket(audioPacket2);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		lastAudioDts = muxAdaptor.getLastDTS();
		assertEquals(lastAudioDts, audioPacket2.getTimestamp() + (long) overFlowCount * Integer.MAX_VALUE);

		muxAdaptor.writeStreamPacket(videoPacket2);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		lastVideoDts = muxAdaptor.getLastDTS();
		assertEquals(lastVideoDts, videoPacket2.getTimestamp() + (long) overFlowCount * Integer.MAX_VALUE);


		verify(hlsMuxer,times(1)).writeAudioBuffer(directByteBuffer,1, audioPacket2.getTimestamp() );
		verify(hlsMuxer,times(1)).writeVideoBuffer(directByteBufferVideo, videoPacket2.getTimestamp(), 0, 0, false, 0, videoPacket2.getTimestamp() );

		muxAdaptor.writeStreamPacket(audioPacket3);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		lastAudioDts = muxAdaptor.getLastDTS();
		assertEquals(lastAudioDts, audioPacket3.getTimestamp() + (long) overFlowCount * Integer.MAX_VALUE);

		muxAdaptor.writeStreamPacket(videoPacket3);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		lastVideoDts = muxAdaptor.getLastDTS();
		assertEquals(lastVideoDts, videoPacket3.getTimestamp() + (long) overFlowCount * Integer.MAX_VALUE);

		verify(hlsMuxer,times(1)).writeAudioBuffer(directByteBuffer,1, audioPacket3.getTimestamp());

		directByteBufferVideo.position(0);
		verify(hlsMuxer,times(1)).writeVideoBuffer(directByteBufferVideo, videoPacket3.getTimestamp(), 0, 0, false, 0, 
				videoPacket3.getTimestamp());

		muxAdaptor.writeStreamPacket(audioPacket4);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		lastAudioDts = muxAdaptor.getLastDTS();
		assertEquals(lastAudioDts, audioPacket4.getTimestamp());

		muxAdaptor.writeStreamPacket(videoPacket4);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(0, overFlowCount);
		lastVideoDts = muxAdaptor.getLastDTS();
		assertEquals(lastVideoDts, videoPacket4.getTimestamp());

		verify(hlsMuxer,times(1)).writeAudioBuffer(directByteBuffer,1, audioPacket4.getTimestamp());
		verify(hlsMuxer,times(1)).writeVideoBuffer(directByteBufferVideo, videoPacket4.getTimestamp(), 0, 0, false, 0,
				videoPacket4.getTimestamp() );

		muxAdaptor.writeStreamPacket(audioPacketOverflowed);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(1, overFlowCount);
		lastAudioDts = muxAdaptor.getLastDTS();
		assertEquals(lastAudioDts, audioPacketOverflowed.getTimestamp());

		muxAdaptor.writeStreamPacket(videoPacketOverflowed);
		overFlowCount = muxAdaptor.getOverflowCount();
		assertEquals(1, overFlowCount);
		lastVideoDts = muxAdaptor.getLastDTS();
		assertEquals(lastVideoDts, videoPacketOverflowed.getTimestamp());

		verify(hlsMuxer,times(1)).writeAudioBuffer(directByteBuffer,1, lastAudioDts + (long) overFlowCount * Integer.MAX_VALUE);
		verify(hlsMuxer,times(1)).writeVideoBuffer(directByteBufferVideo, lastVideoDts + (long) overFlowCount * Integer.MAX_VALUE,
				0, 0, false, 0, lastVideoDts + (long) overFlowCount * Integer.MAX_VALUE);
	}


	@Test
	public void testID3HeaderTagSize() {
		int size = 257;
		byte[] tagSizeBytes = HLSMuxer.convertIntToID3v2TagSize(size);
		for (byte b : tagSizeBytes) {
			System.out.printf("%02X ", b); // Print bytes in hexadecimal format
		}

		assertEquals(0x00, tagSizeBytes[0]);
		assertEquals(0x00, tagSizeBytes[1]);
		assertEquals(0x02, tagSizeBytes[2]);
		assertEquals(0x01, tagSizeBytes[3]);


		tagSizeBytes = HLSMuxer.convertIntToID3v2TagSize((int) Math.pow(2, 7));

		assertEquals(0x00, tagSizeBytes[0]);
		assertEquals(0x00, tagSizeBytes[1]);
		assertEquals(0x01, tagSizeBytes[2]);
		assertEquals(0x00, tagSizeBytes[3]);

		tagSizeBytes = HLSMuxer.convertIntToID3v2TagSize((int) Math.pow(2, 14));

		assertEquals(0x00, tagSizeBytes[0]);
		assertEquals(0x01, tagSizeBytes[1]);
		assertEquals(0x00, tagSizeBytes[2]);
		assertEquals(0x00, tagSizeBytes[3]);

		tagSizeBytes = HLSMuxer.convertIntToID3v2TagSize((int) Math.pow(2, 21));

		assertEquals(0x01, tagSizeBytes[0]);
		assertEquals(0x00, tagSizeBytes[1]);
		assertEquals(0x00, tagSizeBytes[2]);
		assertEquals(0x00, tagSizeBytes[3]);
	}

	@Test
	public void testAddID3Data() {
		HLSMuxer hlsMuxer = spy(new HLSMuxer(vertx, Mockito.mock(StorageClient.class),
				"streams", 0, "http://example.com", false));
		hlsMuxer.setId3Enabled(true);
		hlsMuxer.createID3StreamIfRequired();
		long lastPts = RandomUtils.nextLong();
		doReturn(lastPts).when(hlsMuxer).getLastPts();
		doNothing().when(hlsMuxer).writeDataFrame(any(), any());

		int dataSize = 257 - 10 - 3;
		String data = "a".repeat(dataSize); // Create a string with 247 'a' characters

		hlsMuxer.addID3Data(data);

		// Capture the parameter passed to writeID3Packet
		ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(hlsMuxer).writeID3Packet(captor.capture());

		ByteBuffer capturedBuffer = captor.getValue();

		// Extract values from the captured buffer
		byte[] id3Header = new byte[3];
		capturedBuffer.get(id3Header);
		assertArrayEquals("ID3".getBytes(), id3Header);

		byte[] version = new byte[2];
		capturedBuffer.get(version);
		assertArrayEquals(new byte[]{0x03, 0x00}, version);

		byte flags = capturedBuffer.get();
		assertEquals(0x00, flags);

		byte[] size = new byte[4];
		capturedBuffer.get(size);
		assertEquals(0x00, size[0]);
		assertEquals(0x00, size[1]);
		assertEquals(0x02, size[2]);
		assertEquals(0x01, size[3]);

		byte[] frameId = new byte[4];
		capturedBuffer.get(frameId);
		assertArrayEquals("TXXX".getBytes(), frameId);

		int frameSize = capturedBuffer.getInt();
		assertEquals(dataSize + 3, frameSize);

		byte[] frameFlags = new byte[2];
		capturedBuffer.get(frameFlags);
		assertArrayEquals(new byte[]{0x00, 0x00}, frameFlags);

		byte encoding = capturedBuffer.get();
		assertEquals(0x03, encoding);

		byte descriptionTerminator = capturedBuffer.get();
		assertEquals(0x00, descriptionTerminator);

		byte[] description = new byte[dataSize];
		capturedBuffer.get(description);
		assertArrayEquals(data.getBytes(), description);

		byte endOfString = capturedBuffer.get();
		assertEquals(0x00, endOfString);
	}

	@Test
	public void testGetSubfolder() throws Exception {
		String mainTrackId = "mainTrackId";
		String streamId = "stream456";

		AppSettings appSettings = new AppSettings();

		assertEquals("", getExtendedSubfolder(mainTrackId, streamId, null));
		assertEquals("simplepath", getExtendedSubfolder(mainTrackId, streamId, "simplepath"));
		assertEquals("mainTrackId", getExtendedSubfolder(mainTrackId, streamId, "%m"));
		assertEquals("stream456", getExtendedSubfolder(mainTrackId, streamId, "%s"));

		assertEquals("mainTrackId/stream456", getExtendedSubfolder(mainTrackId, streamId, "%m/%s"));
		assertEquals("stream456/mainTrackId", getExtendedSubfolder(mainTrackId, streamId, "%s/%m"));
		assertEquals(appSettings.getSubFolder(), getExtendedSubfolder(mainTrackId, streamId, appSettings.getSubFolder()));

		assertEquals("folder", getExtendedSubfolder(null, null, "folder/%m/%s"));
		assertEquals("folder", getExtendedSubfolder(null, null, "/folder/%m/%s/"));
		assertEquals("folder", getExtendedSubfolder(null, null, "folder/%m/%s/"));
		assertEquals("folder", getExtendedSubfolder(null, null, "/folder/%m/%s"));

		assertEquals("folder",
				getExtendedSubfolder(null, null, "folder/%m/%s"));

		assertEquals("folder/stream1",
				getExtendedSubfolder(null, "stream1", "folder/%m/%s"));

		assertEquals("folder/track1",
				getExtendedSubfolder("track1", null, "folder/%m/%s"));

		assertEquals("folder/track1/stream1",
				getExtendedSubfolder("track1", "stream1", "folder/%m/%s"));

		assertEquals("folder/stream1",
				getExtendedSubfolder(null, "stream1", "/folder/%m/%s/"));

		assertEquals("lastpeony/mainTrackId/stream456",
				getExtendedSubfolder(mainTrackId, streamId, "lastpeony/%m/%s"));

		assertEquals("folder/mainTrackId",
				getExtendedSubfolder(mainTrackId, streamId, "folder/%m"));

		assertEquals("folder/mainTrackId",
				getExtendedSubfolder(mainTrackId, streamId, "folder/%m/"));

		appSettings.setSubFolder("defaultFolder");

		assertEquals("defaultFolder", getSubfolder(new Broadcast(), appSettings));

		Broadcast broadcastWithSubfolder = new Broadcast();
		broadcastWithSubfolder.setSubFolder("customSubfolder");

		Broadcast broadcastWithIds = new Broadcast();
		broadcastWithIds.setMainTrackStreamId(mainTrackId);
		broadcastWithIds.setStreamId(streamId);

		assertEquals("customSubfolder", getSubfolder(broadcastWithSubfolder, appSettings));

		appSettings.setSubFolder("recordings/%m/%s");
		assertEquals("recordings/mainTrackId/stream456", getSubfolder(broadcastWithIds, appSettings));

		appSettings.setSubFolder("recordings/%m/%s");
		assertEquals("recordings", getSubfolder(new Broadcast(), appSettings));

		Broadcast broadcastWithEmptyIds = new Broadcast();
		broadcastWithEmptyIds.setMainTrackStreamId("");
		broadcastWithEmptyIds.setStreamId("");
		assertEquals("recordings", getSubfolder(broadcastWithEmptyIds, appSettings));

		appSettings.setSubFolder("recordings/%m");
		Broadcast broadcastWithOnlyMainTrack = new Broadcast();
		broadcastWithOnlyMainTrack.setMainTrackStreamId(mainTrackId);
		assertEquals("recordings/mainTrackId", getSubfolder(broadcastWithOnlyMainTrack, appSettings));

		appSettings.setSubFolder("recordings/%s");
		Broadcast broadcastWithOnlyStreamId = new Broadcast();
		broadcastWithOnlyStreamId.setStreamId(streamId);
		assertEquals("recordings/stream456", getSubfolder(broadcastWithOnlyStreamId, appSettings));

		appSettings.setSubFolder("fixedFolder");
		assertEquals("fixedFolder", getSubfolder(broadcastWithIds, appSettings));

		//broadcast subfolder overwrites app settings sub folder.
		assertEquals("customSubfolder", getSubfolder(broadcastWithSubfolder, appSettings));


	}

	public static class RecordMuxerMock extends RecordMuxer {
		protected RecordMuxerMock(StorageClient storageClient, Vertx vertx, String s3FolderPath) {
			super(storageClient, vertx, s3FolderPath);
		}
		@Override
		protected void finalizeRecordFile(File file) throws IOException {
		}

	}
	public static class  StorageClientMock extends StorageClient{
		static  Boolean saveCalledWithCorrectParams = false;

    @Override

    public void deleteMultipleFiles(String key, String regex){
		logger.info("test delete method");
    }
		@Override
		public void delete(String key) {

		}

		@Override
		public void save(String key, InputStream inputStream, boolean waitForCompletion) {

		}

		@Override
		public void save(String key, File file, boolean deleteLocalFile) {
			logger.info(key);
			String result = RecordMuxer.replaceDoubleSlashesWithSingleSlash(key);
			if(key.equals(result)) {
				saveCalledWithCorrectParams = true;
			}
		}

		@Override
		public boolean fileExist(String key) {
			return false;
		}

		@Override
		public void reset() {

		}
	}
	
	@Test
	public void testReplaceMultipleSlashes() {
		String replaceDoubleSlashesWithSingleSlash = RecordMuxer.replaceDoubleSlashesWithSingleSlash("WebRTCAppEE/streams///stream1.mp4");
		assertEquals("WebRTCAppEE/streams/stream1.mp4", replaceDoubleSlashesWithSingleSlash);
	}

	@Test
	public void testWriteTrailer() throws IOException, InterruptedException {

		Vertx vertx = Vertx.vertx();
		File file = Mockito.spy(new File("test"));
		StorageClientMock storageClient = Mockito.spy(new StorageClientMock());
		Mockito.when(storageClient.isEnabled()).thenReturn(true);
		RecordMuxerMock recordMuxerMock = Mockito.spy(new RecordMuxerMock(storageClient, vertx, "streams"));
		appScope = (WebScope) applicationContext.getBean("web.scope");
		recordMuxerMock.init(appScope, "", 0, "", 0);
		recordMuxerMock.setIsRunning(new AtomicBoolean(true));
		doReturn(true).when(recordMuxerMock).isUploadingToS3();

		recordMuxerMock.setFileTmp(file);
		Mockito.when(file.exists()).thenReturn(true);


		doReturn(new File("test.mp4")).when(recordMuxerMock).getFinalFileName(anyBoolean());
		doNothing().when(recordMuxerMock).finalizeRecordFile(any());

		AppSettings settings = Mockito.mock(AppSettings.class);
		doReturn(true).when(settings).isS3RecordingEnabled();
		doReturn(7).when(settings).getUploadExtensionsToS3();
		doReturn(settings).when(recordMuxerMock).getAppSettings();

		AntMediaApplicationAdapter adapter = Mockito.mock(AntMediaApplicationAdapter.class);
		doReturn(adapter).when(recordMuxerMock).getAppAdaptor();
		DataStore dataStore = Mockito.mock(DataStore.class);
		doReturn(dataStore).when(adapter).getDataStore();
		doReturn(null).when(dataStore).get(anyString());
		doNothing().when(adapter).muxingFinished(any(),anyString(),any(),anyLong(),anyLong(),anyInt(),anyString(),anyString());

		recordMuxerMock.writeTrailer();

		Thread.sleep(500);

		verify(recordMuxerMock, times(1)).finalizeRecordFile(any());
		verify(recordMuxerMock, times(1)).getFinalFileName(anyBoolean());
		assert (StorageClientMock.saveCalledWithCorrectParams);

		recordMuxerMock.writeTrailer();


		//trailer already written should not invoke again
		verify(recordMuxerMock, times(1)).finalizeRecordFile(any());

		verify(recordMuxerMock, times(1)).getFinalFileName(anyBoolean());

	}


	@Test
	public void testNotifyStreamFinish() throws IOException {

		HLSMuxer muxer = spy(new HLSMuxer(vertx, Mockito.mock(StorageClient.class), "streams", 7, "test", false));

		AntMediaApplicationAdapter appAdaptor = mock(AntMediaApplicationAdapter.class);
		doReturn(appAdaptor).when(muxer).getAppAdaptor();

		//if client is null
		CloseableHttpClient client = mock(CloseableHttpClient.class);
		doReturn(null).when(appAdaptor).getHttpClient();
		verify(client,times(0)).execute(any());
		doReturn(client).when(appAdaptor).getHttpClient();

		ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
		String streamId = "test";
		String path = "stream/test";
		muxer.notifyStreamFinish(streamId,path);

		verify(client).execute(captor.capture());
		HttpPost capturedValue = captor.getValue();

		HttpEntity entity = capturedValue.getEntity();
		String content = EntityUtils.toString(entity, "UTF-8");

		JsonObject streamFinished = new JsonObject();

		streamFinished.addProperty("streamId", streamId);
		streamFinished.addProperty("filePath", path);
		streamFinished.addProperty("command", WebSocketConstants.PUBLISH_FINISHED);

		assert(content.equals(streamFinished.toString()));

		//test write trailer

		muxer.setIsRunning(new AtomicBoolean(true));
		AppSettings settings = new AppSettings();
		settings.setS3StreamsFolderPath("streams");
		doReturn(appSettings).when(muxer).getAppSettings();

		muxer.writeTrailer();
		verify(muxer).notifyStreamFinish(streamId,path);
	}
	
	
	@Test
	public void testRTMPStreamMedaData(){
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertEquals(1, appScope.getDepth());
		
		

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(Mockito.mock(ClientBroadcastStream.class), null, false, appScope));
		String streamId = "stream " + (int) (Math.random() * 10000);
		muxAdaptor.setStreamId(streamId);
		
		DataStore dataStore = Mockito.mock(DataStore.class);
		doReturn(dataStore).when(muxAdaptor).getDataStore();		
		
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("key1", "val1");
		parameters.put("key2", "val2");
		
		muxAdaptor.setBroadcastMetaData(parameters);
		
		ArgumentCaptor<BroadcastUpdate> argument = ArgumentCaptor.forClass(BroadcastUpdate.class);
		verify(dataStore, times(1)).updateBroadcastFields(eq(streamId), argument.capture());
		assertEquals("{\"key1\":\"val1\",\"key2\":\"val2\"}", argument.getValue().getMetaData());
		
	}

	@Test
	public void testMp4MuxerInitUsesOverrideFileName() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		Mp4Muxer mp4Muxer = Mockito.spy(new Mp4Muxer(Mockito.mock(StorageClient.class), Vertx.vertx(), "streams"));
		AppSettings appSettingsLocal = new AppSettings();
		appSettingsLocal.setFileNameFormat("");
		Mockito.doReturn(appSettingsLocal).when(mp4Muxer).getAppSettings();
		mp4Muxer.setInitialResourceNameOverride("custom_file_name");
		mp4Muxer.init(appScope, "ignoredStreamId", 0, null, 0);
		assertEquals("custom_file_name.mp4", mp4Muxer.getFileName());
	}

	@Test
	public void testWebMMuxerInitUsesOverrideFileName() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		WebMMuxer webmMuxer = Mockito.spy(new WebMMuxer(Mockito.mock(StorageClient.class), Vertx.vertx(), "streams"));
		AppSettings appSettingsLocal = new AppSettings();
		appSettingsLocal.setFileNameFormat("");
		Mockito.doReturn(appSettingsLocal).when(webmMuxer).getAppSettings();
		webmMuxer.setInitialResourceNameOverride("custom_file_name_webm");
		webmMuxer.init(appScope, "ignoredStreamId", 0, null, 0);
		assertEquals("custom_file_name_webm.webm", webmMuxer.getFileName());
	}

	@Test
	public void testSanitizeAndStripExtension_LengthAndChars() throws Exception {
		RestServiceBase rest = new RestServiceBase() {};
		java.lang.reflect.Method m = RestServiceBase.class.getDeclaredMethod("sanitizeAndStripExtension", String.class, RecordType.class);
		m.setAccessible(true);
		String veryLong = new String(new char[150]).replace('\0', 'a') + ".mp4";
		String res = (String)m.invoke(rest, veryLong, RecordType.MP4);
		assertTrue(res.length() <= 120);
		String withPath = "..//..\\bad/name\\file.mp4";
		String res2 = (String)m.invoke(rest, withPath, RecordType.MP4);
		assertFalse(res2.contains("/"));
		assertFalse(res2.contains("\\"));
		// explicitly cover WEBM extension strip path
		String webm = (String)m.invoke(rest, "clip.webm", RecordType.WEBM);
		assertEquals("clip", webm);
	}

	@Test
	public void testMuxAdaptorStartRecordingOverloadPassesBaseName() {
		class TestMuxAdaptor extends MuxAdaptor {
			public TestMuxAdaptor() { super(Mockito.mock(ClientBroadcastStream.class)); }
			@Override public boolean addMuxer(Muxer muxer, int resolutionHeight) { return true; }
			@Override public Mp4Muxer createMp4Muxer() { return Mockito.spy(new Mp4Muxer(Mockito.mock(StorageClient.class), Vertx.vertx(), "streams")); }
			@Override public boolean isAlreadyRecording(RecordType recordType, int resolutionHeight) { return false; }
		}

		TestMuxAdaptor adaptor = new TestMuxAdaptor();
		adaptor.setIsRecording(true);
		RecordMuxer result = adaptor.startRecording(RecordType.MP4, 0, "base_name");
		// verify override applied on created muxer
		Mp4Muxer created = (Mp4Muxer) result;
		Mockito.verify(created, Mockito.times(1)).setInitialResourceNameOverride("base_name");
	}

	@Test
	public void testMuxAdaptorDirectMuxingSupported() {
		class TestMuxAdaptor extends MuxAdaptor {
			public TestMuxAdaptor() { super(Mockito.mock(ClientBroadcastStream.class)); }
			@Override public boolean addMuxer(Muxer muxer, int resolutionHeight) { return true; }
			@Override public Mp4Muxer createMp4Muxer() { return Mockito.spy(new Mp4Muxer(Mockito.mock(StorageClient.class), Vertx.vertx(), "streams")); }
			@Override public boolean isAlreadyRecording(RecordType recordType, int resolutionHeight) { return false; }
		}

		TestMuxAdaptor adaptor = new TestMuxAdaptor();
		adaptor.setDirectMuxingSupported(false);
		assertFalse(adaptor.directMuxingSupported());

		adaptor.setDirectMuxingSupported(true);
		assertTrue(adaptor.directMuxingSupported());
	}

	@Test
	public void testEnableRecordMuxingWithFileNameUsesOverride() throws Exception {
		final String streamId = "s1";
		// mock datastore and broadcast in broadcasting state
		DataStore store = Mockito.mock(DataStore.class);
		Broadcast b = new Broadcast();
		b.setStreamId(streamId);
		b.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		b.setOriginAdress("127.0.0.1");
		Mockito.when(store.get(streamId)).thenReturn(b);
		Mockito.when(store.setMp4Muxing(Mockito.eq(streamId), Mockito.anyInt())).thenReturn(true);

		RecordMuxer rm = Mockito.mock(RecordMuxer.class);
		Mockito.when(rm.getCurrentVoDTimeStamp()).thenReturn(0L);
		RestServiceBase rest = new RestServiceBase() {
			@Override
			public DataStore getDataStore() { return store; }
			@Override
			public boolean isInSameNodeInCluster(String originAddress) { return true; }
			@Override
			protected RecordMuxer startRecord(String sid, RecordType rt, int res, String baseFileName) { return rm; }
			@Override
			protected RecordMuxer startRecord(String sid, RecordType rt, int res) { return null; }
		};

		Result res = rest.enableRecordMuxing(streamId, true, "mp4", 0, "name.mp4");
		assertTrue(res.isSuccess());
	}

	@Test
	public void testSanitizeAndStripExtension() throws Exception {
		RestServiceBase rest = new RestServiceBase() {};
		// access protected method via reflection
		java.lang.reflect.Method m = RestServiceBase.class.getDeclaredMethod("sanitizeAndStripExtension", String.class, RecordType.class);
		m.setAccessible(true);
		assertEquals("my_vod", (String)m.invoke(rest, "my_vod.mp4", RecordType.MP4));
		assertEquals("cl_ea_n_na_me", (String)m.invoke(rest, "cl/ea\\n_na\tme.webm", RecordType.WEBM));
		assertEquals("noext", (String)m.invoke(rest, "noext", RecordType.MP4));
	}
	
	@Test
	public void testPrepareFromInputFormatContextForData() throws Exception {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertEquals(1, appScope.getDepth());
		
		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(Mockito.mock(ClientBroadcastStream.class), null, false, appScope));
		String streamId = "stream " + (int) (Math.random() * 10000);
		muxAdaptor.setStreamId(streamId);
		
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();

		int ret;
		if (inputFormatContext == null) {
			System.out.println("cannot allocate input context");
		}

		if ((ret = avformat_open_input(inputFormatContext, "src/test/resources/test_with_scte35.ts", null, (AVDictionary) null)) < 0) {
			System.out.println("cannot open input context: test_with_scte35.ts");
		}
		muxAdaptor.prepareFromInputFormatContext(inputFormatContext);
		
		assertEquals(0, muxAdaptor.getDataStreamIndex());
		assertEquals(1, muxAdaptor.getVideoStreamIndex());
		assertEquals(2, muxAdaptor.getAudioStreamIndex());
	}
	
	
	

}
