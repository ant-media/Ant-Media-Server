package io.antmedia.test;
import static io.antmedia.muxer.IAntMediaStreamHandler.*;
import static io.antmedia.muxer.MuxAdaptor.getExtendedSubfolder;
import static io.antmedia.muxer.MuxAdaptor.getSubfolder;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AC3;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H265;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_HCA;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_HEVC;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_MP3;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_NONE;
import static org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_VP8;
import static org.bytedeco.ffmpeg.global.avcodec.AV_PKT_FLAG_KEY;
import static org.bytedeco.ffmpeg.global.avcodec.av_init_packet;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_alloc;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_free;
import static org.bytedeco.ffmpeg.global.avcodec.av_packet_unref;
import static org.bytedeco.ffmpeg.global.avformat.AVFMT_NOFILE;
import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_stream_get_side_data;
import static org.bytedeco.ffmpeg.global.avformat.avformat_alloc_output_context2;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_free_context;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_ATTACHMENT;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_DATA;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_SUBTITLE;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P;
import static org.bytedeco.ffmpeg.global.avutil.AV_SAMPLE_FMT_FLTP;
import static org.bytedeco.ffmpeg.global.avutil.av_channel_layout_default;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_get;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import io.antmedia.*;
import java.lang.reflect.Field;
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
import org.bytedeco.ffmpeg.avcodec.*;
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
import java.util.*;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.Mp4Muxer;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.muxer.RecordMuxer;
import io.antmedia.muxer.EndpointMuxer;
import io.antmedia.muxer.WebMMuxer;
import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avformat.*;
import static org.bytedeco.ffmpeg.global.avutil.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ContextConfiguration(locations = {"test.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class MuxerTestBase extends AbstractJUnit4SpringContextTests {


	protected static Logger logger = LoggerFactory.getLogger(MuxerTestBase.class);
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
	protected AppSettings appSettings;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}

	protected DataStore datastore;

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
			// Force cleanup of native FFmpeg/JavaCPP resources to prevent native memory exhaustion
			System.gc();
			org.bytedeco.javacpp.Pointer.deallocateReferences();
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
	protected Vertx vertx;

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

	public File testMp4Muxing(String name) {
		return testMp4Muxing(name, true, true);
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

	protected IStreamPacket createPacket(byte type, int timeStamp) {
		CachedEvent ce = new CachedEvent();
		ce.setDataType(type);
		ce.setTimestamp(timeStamp);
		IoBuffer data = IoBuffer.allocate(1);
		ce.setData(data);
		return ce;
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
}
