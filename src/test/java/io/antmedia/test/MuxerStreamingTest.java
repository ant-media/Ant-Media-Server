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

@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class MuxerStreamingTest extends MuxerTestBase {

	// ==================== HLS tests ====================

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
	public void testHLSNormal() {
		testHLSMuxing("hlsmuxing_test");
	}

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

	// ==================== RTMP/Endpoint tests ====================

	@Test
	public void testStopRtmpStreamingWhenEndpointMuxerNull() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		logger.info("Application / web scope: {}", appScope);
		assertTrue(appScope.getDepth() == 1);

		MuxAdaptor muxAdaptor = Mockito.spy(MuxAdaptor.initializeMuxAdaptor(null, null, false, appScope));
		String rtmpUrl = "rtmp://test.com/live/stream";
		Integer resolution = 0;

		ConcurrentHashMap<String, String> statusMap = Mockito.mock(ConcurrentHashMap.class);
		ReflectionTestUtils.setField(muxAdaptor, "statusMap", statusMap);
		Mockito.doReturn(null).when(muxAdaptor).getEndpointMuxer(rtmpUrl);

		Mockito.doReturn(null).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopEndpointStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(BROADCAST_STATUS_ERROR).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopEndpointStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(BROADCAST_STATUS_FAILED).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopEndpointStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED).when(statusMap).getOrDefault(rtmpUrl, null);
		assertTrue(muxAdaptor.stopEndpointStreaming(rtmpUrl, resolution).isSuccess());

		Mockito.doReturn(BROADCAST_STATUS_BROADCASTING).when(statusMap).getOrDefault(rtmpUrl, null);
		assertFalse(muxAdaptor.stopEndpointStreaming(rtmpUrl, resolution).isSuccess());
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
		Result result = muxAdaptor.startEndpointStreaming(rtmpUrl, resolutionHeight);
		assertFalse(result.isSuccess());

		muxAdaptor.setHeight(480);
		result = muxAdaptor.startEndpointStreaming(rtmpUrl, resolutionHeight);
		assertTrue(result.isSuccess());


		result = muxAdaptor.startEndpointStreaming(rtmpUrl, 0);
		assertTrue(result.isSuccess());


		EndpointMuxer endpointMuxer = Mockito.mock(EndpointMuxer.class);
		Mockito.doReturn(endpointMuxer).when(muxAdaptor).getEndpointMuxer(Mockito.any());
		muxAdaptor.stopEndpointStreaming(rtmpUrl, resolutionHeight);
		Mockito.verify(endpointMuxer).writeTrailer();


		muxAdaptor.stopEndpointStreaming(rtmpUrl, 0);
		Mockito.verify(endpointMuxer, Mockito.times(2)).writeTrailer();


		muxAdaptor.stopEndpointStreaming(rtmpUrl, 360);
		//it should be 2 times again because 360 and 480 don't match
		Mockito.verify(endpointMuxer, Mockito.times(2)).writeTrailer();

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
		rtmpEndpoint.setEndpointUrl(rtmpUrl);
		List<Endpoint> endpointList = new ArrayList<>();
		endpointList.add(rtmpEndpoint);

		broadcast.setEndPointList(endpointList);
		boolean result = muxAdaptor.init(appScope, "test", false);
		muxAdaptor.getDataStore().save(broadcast);

		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_BROADCASTING);


		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return BROADCAST_STATUS_BROADCASTING.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});


		muxAdaptor.getDataStore().delete(broadcast.getStreamId());

		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_BROADCASTING);
		assertEquals(1, muxAdaptor.getEndpointStatusUpdateMap().size());

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return 0 == muxAdaptor.getEndpointStatusUpdateMap().size();
		});

	}

	@Test
	public void testRTMPCodecSupport() {
		EndpointMuxer endpointMuxer = new EndpointMuxer(null, vertx);

		assertTrue(endpointMuxer.isCodecSupported(AV_CODEC_ID_H264));
		assertTrue(endpointMuxer.isCodecSupported(AV_CODEC_ID_AAC));

		assertFalse(endpointMuxer.isCodecSupported(AV_CODEC_ID_AC3));

	}

	@Test
	public void testAVWriteFrame() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer(null, vertx));

		AVFormatContext context = new AVFormatContext();
		int ret = avformat_alloc_output_context2(context, null, "flv", "test.flv");

		//rtmpMuxer.set
		AVPacket pkt = av_packet_alloc();

		appScope = (WebScope) applicationContext.getBean("web.scope");

		endpointMuxer.init(appScope, "", 0, "", 0);

		endpointMuxer.avWriteFrame(pkt, context);

		Mockito.verify(endpointMuxer).addExtradataIfRequired(pkt, false);
		endpointMuxer.avWriteFrame(pkt, context);

		pkt.flags(AV_PKT_FLAG_KEY);
		endpointMuxer.addExtradataIfRequired(pkt,true);

		av_packet_free(pkt);
		avformat_free_context(context);
	}
	@Test
	public void testWriteVideoBuffer(){
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer(null, vertx));
		appScope = (WebScope) applicationContext.getBean("web.scope");

		endpointMuxer.init(appScope, "", 0, "", 0);
		endpointMuxer.writeVideoBuffer(null,10,1,1,false,10,10);

		endpointMuxer.setIsRunning(new AtomicBoolean(true));
		endpointMuxer.getRegisteredStreamIndexList().add(1);
		endpointMuxer.writeVideoBuffer(null,10,1,1,false,10,10);
		verify((Muxer)endpointMuxer,times(0)).writeVideoBuffer(any());

		doNothing().when(endpointMuxer).writeVideoBuffer(any());
		endpointMuxer.writeVideoBuffer(null,10,1,1,true,10,10);
		verify((Muxer)endpointMuxer,times(1)).writeVideoBuffer(any());

	}

	@Test
	public void testRTMPAddStream() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		EndpointMuxer endpointMuxer = new EndpointMuxer(null, vertx);

		AVCodecContext codecContext = new AVCodecContext();
		codecContext.width(640);
		codecContext.height(480);


		boolean addStream = endpointMuxer.addStream(null, codecContext, 0);
		assertFalse(addStream);


		codecContext.codec_id(AV_CODEC_ID_H264);
		addStream = endpointMuxer.addStream(null, codecContext, BUFFER_SIZE);
		assertTrue(addStream);


		addStream = endpointMuxer.addVideoStream(480, 360, Muxer.avRationalTimeBase, AV_CODEC_ID_H264, 0, true, null);
		assertTrue(addStream);

	}

	@Test
	public void testRTMPPrepareIO() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		final EndpointMuxer endpointMuxer = new EndpointMuxer("rtmp://no_server", vertx);

		//it should return false because there is nothing to send.
		assertFalse(endpointMuxer.prepareIO());

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return endpointMuxer.getStatus().equals(BROADCAST_STATUS_FAILED);
		});

		assertFalse(endpointMuxer.prepareIO());

		final EndpointMuxer endpointMuxer1 = new EndpointMuxer("udp://127.0.0.1:12345?localaddr=127.0.0.1", vertx);
		AVCodecParameters codecParameters = new AVCodecParameters();
		SPSParser spsParser = new SPSParser(extradata_original, 5);
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

		endpointMuxer1.init(appScope, "test", 0, null, 0);
		endpointMuxer1.addStream(codecParameters, rat, 50);
		endpointMuxer1.prepareIO();

		Awaitility.await().atMost(25, TimeUnit.SECONDS).until(() -> {
			return endpointMuxer1.getStatus().equals(BROADCAST_STATUS_BROADCASTING);
		});

		assert(endpointMuxer1.getIsRunning().get());

		final EndpointMuxer endpointMuxer2 = spy(new EndpointMuxer("rtmp://fakeurl", vertx));
		endpointMuxer2.init(appScope, "test", 0, null, 0);
		endpointMuxer2.addStream(codecParameters, rat, 50);
		endpointMuxer2.prepareIO();

		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return endpointMuxer2.getStatus().equals(BROADCAST_STATUS_FAILED);
		});
		verify(endpointMuxer2).clearResource();
	}

	@Test
	public void testRTMPMuxerRaceCondition() {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		EndpointMuxer rtmpMuxer = new EndpointMuxer("rtmp://no_server", vertx);
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
		EndpointMuxer rtmpMuxer2 = new EndpointMuxer("rtmp://no_server", vertx);
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

		EndpointMuxer rtmpMuxer = new EndpointMuxer("rtmp://no_server", vertx);
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
		rtmpEndpoint.setEndpointUrl(rtmpUrl);
		List<Endpoint> endpointList = new ArrayList<>();
		endpointList.add(rtmpEndpoint);

		broadcast.setEndPointList(endpointList);
		boolean result = muxAdaptor.init(appScope, "test", false);
		muxAdaptor.getDataStore().save(broadcast);

		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_ERROR);

		assertTrue(muxAdaptor.getIsHealthCheckStartedMap().get(rtmpUrl));

		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_BROADCASTING);
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return muxAdaptor.getIsHealthCheckStartedMap().getOrDefault(rtmpUrl, false) == false;
		});

		//ERROR SCENARIO
		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_ERROR);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return BROADCAST_STATUS_ERROR.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});

		assertTrue(muxAdaptor.getIsHealthCheckStartedMap().get(rtmpUrl));
		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			return muxAdaptor.getIsHealthCheckStartedMap().getOrDefault(rtmpUrl, false) == false;
		});

		//SET BROADCASTING AGAIN
		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_BROADCASTING);


		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return BROADCAST_STATUS_BROADCASTING.equals(broadcastLocal.getEndPointList().get(0).getStatus());
		});

		//FAILED SCENARIO
		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_FAILED);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return BROADCAST_STATUS_FAILED.equals(broadcastLocal.getEndPointList().get(0).getStatus());
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

		muxAdaptor.endpointStatusUpdated(rtmpUrl, BROADCAST_STATUS_ERROR);

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcastLocal = muxAdaptor.getDataStore().get(broadcast.getStreamId());
			return BROADCAST_STATUS_ERROR.equals(broadcastLocal.getEndPointList().get(0).getStatus());
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

		EndpointMuxer endpointMuxer = new EndpointMuxer("any_url", vertx);

		endpointMuxer.init(appScope, "test", 0, null, 0);
		endpointMuxer.addStream(codecParameters, rat, 50);
		assertTrue(endpointMuxer.openIO());

		endpointMuxer.setIsRunning(new AtomicBoolean(true));

		//This was a crash if we don't check headerWritten after we initialize the context and get isRunning true
		//To test the scenarios of that crash;
		endpointMuxer.writeTrailer();

		//This should work since the trailer is not written yet
		endpointMuxer.writeHeader();

		//This should work since header is written
		endpointMuxer.writeTrailer();

		//This is for testing writeHeader after writeTrailer.
		endpointMuxer.writeHeader();
	}

	@Test
	public void testRtmpUrlWithoutAppName() {
		{
			EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer("rtmp://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0", vertx)); //RTMP URl without Appname
			AVDictionary opt = endpointMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertEquals("rtmp_app", optEntry.key().getString());
			assertEquals("", optEntry.value().getString());
		}


		{
			EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer("rtmp://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0/test", vertx)); //RTMP URl without Appname
			AVDictionary opt = endpointMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertNull(optEntry);

			//if it's different from zero, it means no file is need to be open.
			//If it's zero, Not "no file" and it means that file is need to be open .
			assertEquals(0, endpointMuxer.getOutputFormatContext().oformat().flags() & AVFMT_NOFILE);


			endpointMuxer.clearResource();
		}

		{
			EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer("rtmps://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0", vertx)); //RTMP URl without Appname
			AVDictionary opt = endpointMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertEquals("rtmp_app", optEntry.key().getString());
			assertEquals("", optEntry.value().getString());

		}

		{
			EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer("rtmps://a.rtmp.youtube.com/y8qd-42g5-1b53-fh15-2v0/test", vertx)); //RTMP URl without Appname
			AVDictionary opt = endpointMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertNull(optEntry);

			//if it's different from zero, it means no file is need to be open.
			//If it's zero, Not "no file" and it means that file is need to be open .
			assertEquals(0, endpointMuxer.getOutputFormatContext().oformat().flags() & AVFMT_NOFILE);


			endpointMuxer.clearResource();

		}

		{
			EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer("rtmps://live-api-s.facebook.com:443/rtmp/y8qd-42g5-1b53-fh15-2v0", vertx)); //RTMP URl without Appname
			AVDictionary opt = endpointMuxer.getOptionDictionary();
			AVDictionaryEntry optEntry = av_dict_get(opt, "rtmp_app", null, 0);
			assertNull(optEntry);

			//if it's different from zero, it means no file is need to be open.
			//If it's zero, Not "no file" and it means that file is need to be open .
			assertEquals(0, endpointMuxer.getOutputFormatContext().oformat().flags() & AVFMT_NOFILE);


			endpointMuxer.clearResource();
		}

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
	public void testParseEndpointURl(){
		EndpointMuxer endpointMuxer = new EndpointMuxer("rtmp://",vertx);
		Assert.assertEquals("rtmp", endpointMuxer.muxerType);
		Assert.assertEquals("flv", endpointMuxer.getFormat());
		Assert.assertEquals("rtmp",endpointMuxer.getMuxerType());

		endpointMuxer = new EndpointMuxer("srt://",vertx);
		Assert.assertEquals("srt", endpointMuxer.muxerType);
		Assert.assertEquals("mpegts", endpointMuxer.getFormat());
		Assert.assertEquals("srt",endpointMuxer.getMuxerType());
	}
	@Test
	public void testGetSetEndpointURl(){
		String url = "rtmp://test.antmedia.io/LiveApp/test";
		Endpoint endpoint = new Endpoint();
		endpoint.setRtmpUrl(url);
		assertEquals(url,endpoint.getEndpointUrl());
	}
	@Test
	public void testWritePacket() throws  InterruptedException {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		AVPacket pkt = new AVPacket();
		final AVRational inputTimebase = new AVRational().num(1).den(1000);
		final AVRational outputTimebase = new AVRational().num(1).den(1000);

		EndpointMuxer endpointMuxer = spy(new EndpointMuxer("udp://127.0.0.1:12345?localaddr=127.0.0.1", vertx));

		AVCodecParameters codecParameters = new AVCodecParameters();
		codecParameters.codec_id(AV_CODEC_ID_AAC);
		codecParameters.codec_type(AVMEDIA_TYPE_AUDIO);
		codecParameters.codec_tag(1);
		AVRational rat = new AVRational().num(1).den(1000);

		endpointMuxer.init(appScope, "test", 0, null, 0);
		endpointMuxer.addStream(codecParameters, rat, 50);
		endpointMuxer.prepareIO();
		pkt.stream_index(0);

		// writing audio packet when Header not written
		endpointMuxer.writePacket(pkt,inputTimebase,outputTimebase,1);
		verify(endpointMuxer,times(0)).writeFrameInternal(any(),any(),any(),any(),anyInt());

		endpointMuxer = spy(new EndpointMuxer("udp://127.0.0.1:12345?localaddr=127.0.0.1", vertx));

		codecParameters = new AVCodecParameters();
		SPSParser spsParser = new SPSParser(extradata_original, 5);
		codecParameters.width(spsParser.getWidth());
		codecParameters.height(spsParser.getHeight());
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		codecParameters.extradata_size(sps_pps_avc.length);
		BytePointer extraDataPointer = new BytePointer(sps_pps_avc);
		codecParameters.extradata(extraDataPointer);
		codecParameters.format(AV_PIX_FMT_YUV420P);
		codecParameters.codec_tag(0);
		rat = new AVRational().num(1).den(1000);


		endpointMuxer.init(appScope, "test", 0, null, 0);
		endpointMuxer.addStream(codecParameters, rat, 50);
		endpointMuxer.prepareIO();

		EndpointMuxer finalEndpointMuxer = endpointMuxer;
		Awaitility.await().atMost(25, TimeUnit.SECONDS).until(() -> {
			return finalEndpointMuxer.getStatus().equals(BROADCAST_STATUS_BROADCASTING);
		});

		pkt = new AVPacket();
		pkt.stream_index(0);

		//writing video packet when header written
		endpointMuxer.writePacket(pkt,inputTimebase,outputTimebase,0);
		verify(endpointMuxer,times(1)).writeFrameInternal(any(),any(),any(),any(),anyInt());
		verify(endpointMuxer,times(1)).avWriteFrame(any(),any());
		verify(endpointMuxer,times(1)).addExtradataIfRequired(any(),anyBoolean());
		assert(endpointMuxer.getStatus().equals(BROADCAST_STATUS_BROADCASTING));

		endpointMuxer.setStatus("test");//reset state

		//writing audio packet when header written
		endpointMuxer.writePacket(pkt,inputTimebase,outputTimebase,1);
		assert(endpointMuxer.getStatus().equals(BROADCAST_STATUS_BROADCASTING));

		endpointMuxer.setStatus("test");//reset state

		pkt = new AVPacket();
		pkt.stream_index(0);
		pkt.size(-1);
		pkt.data(null);

		//writing video packet when header written invalid packet
		endpointMuxer.writePacket(pkt,inputTimebase,outputTimebase,0);
		assert(endpointMuxer.getStatus().equals(BROADCAST_STATUS_ERROR));

		//writing audio packet when header written invalid packet
		endpointMuxer.getOutputFormatContext().streams(0).codecpar().codec_type(1);
		endpointMuxer.writePacket(pkt,inputTimebase,outputTimebase,1);
		assert(endpointMuxer.getStatus().equals(BROADCAST_STATUS_ERROR));

		//bitstream filter
		endpointMuxer = spy(new EndpointMuxer("udp://127.0.0.1:12345?localaddr=127.0.0.1", vertx));

		codecParameters = new AVCodecParameters();
		spsParser = new SPSParser(extradata_original, 5);
		codecParameters.width(spsParser.getWidth());
		codecParameters.height(spsParser.getHeight());
		codecParameters.codec_id(AV_CODEC_ID_H264);
		codecParameters.codec_type(AVMEDIA_TYPE_VIDEO);
		codecParameters.extradata_size(sps_pps_avc.length);
		extraDataPointer = new BytePointer(sps_pps_avc);
		codecParameters.extradata(extraDataPointer);
		codecParameters.format(AV_PIX_FMT_YUV420P);
		codecParameters.codec_tag(0);
		rat = new AVRational().num(1).den(1000);

		pkt = new AVPacket();
		pkt.stream_index(0);

		endpointMuxer.init(appScope, "test", 0, null, 0);
		endpointMuxer.addStream(codecParameters, rat, 50);

		AVBSFContext avbsfContext = endpointMuxer.initVideoBitstreamFilter("h264_mp4toannexb", codecParameters, Muxer.avRationalTimeBase);
		endpointMuxer.prepareIO();

         EndpointMuxer finalEndpointMuxer1 = endpointMuxer;
         Awaitility.await().atMost(25, TimeUnit.SECONDS).until(() -> {
	            return finalEndpointMuxer1.getStatus().equals(BROADCAST_STATUS_BROADCASTING);
          });

	 	endpointMuxer.setStatus("test");

	    endpointMuxer.writePacket(pkt,inputTimebase,outputTimebase,0);
		assert (!endpointMuxer.getStatus().equals(BROADCAST_STATUS_BROADCASTING));


	}
	@Test
	public void testGetOutputFormatCtx(){
		EndpointMuxer endpointMuxer = spy(new EndpointMuxer("rtmp://test.antmedia.io/LiveApp/prepareIOTest2", vertx));
		endpointMuxer.setFormat("testing");
		AVFormatContext ctx = endpointMuxer.getOutputFormatContext();
		assert (endpointMuxer.getStatus().equals(BROADCAST_STATUS_FAILED));
		assert (ctx == null);

		endpointMuxer.setFormat("mpegts");
		ctx = endpointMuxer.getOutputFormatContext();
		assert (ctx != null);

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
	public void testEndpointMuxerPrepareIOCancelledAndNotCancelled() throws Exception {
		appScope = (WebScope) applicationContext.getBean("web.scope");
		vertx = (Vertx) appScope.getContext().getApplicationContext().getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		// Scenario 1: Test Cancellation (Lines 246-253 and 151-164 cancellation path)
		EndpointMuxer endpointMuxer = Mockito.spy(new EndpointMuxer("rtmp://dummy", vertx));

		// Mock getOutputFormatContext to return a context with streams
		AVFormatContext outputFormatContext = new AVFormatContext(null);
		avformat_alloc_output_context2(outputFormatContext, null, "flv", null);
		avformat_new_stream(outputFormatContext, null);
		Mockito.doReturn(outputFormatContext).when(endpointMuxer).getOutputFormatContext();

		// Mock openIO to set cancelOpenIO = true and return true
		Mockito.doAnswer(invocation -> {
			Field cancelField = EndpointMuxer.class.getDeclaredField("cancelOpenIO");
			cancelField.setAccessible(true);
			AtomicBoolean cancel = (AtomicBoolean) cancelField.get(endpointMuxer);
			cancel.set(true);
			return true;
		}).when(endpointMuxer).openIO();

		// Add element to bsfFilterContextList so it enters the check block
		Field bsfField = Muxer.class.getDeclaredField("bsfFilterContextList");
		bsfField.setAccessible(true);
		List<AVBSFContext> bsfList = (List<AVBSFContext>) bsfField.get(endpointMuxer);
		bsfList.add(new AVBSFContext(null));

		endpointMuxer.prepareIO();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			// preparedIO should be false because clearResource is called
			Field preparedField = EndpointMuxer.class.getDeclaredField("preparedIO");
			preparedField.setAccessible(true);
			AtomicBoolean prepared = (AtomicBoolean) preparedField.get(endpointMuxer);
			return !prepared.get() && !endpointMuxer.getIsRunning().get();
		});

		Mockito.verify(endpointMuxer, Mockito.atLeastOnce()).clearResource();


		// Scenario 2: Test Normal Flow (Lines 151-164 normal path)
		EndpointMuxer rtmpMuxer2 = Mockito.spy(new EndpointMuxer("rtmp://dummy2", vertx));
		Mockito.doReturn(outputFormatContext).when(rtmpMuxer2).getOutputFormatContext();
		Mockito.doReturn(true).when(rtmpMuxer2).openIO();

		bsfList = (List<AVBSFContext>) bsfField.get(rtmpMuxer2);
		bsfList.add(new AVBSFContext(null));

		rtmpMuxer2.prepareIO();

		Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> {
			return rtmpMuxer2.getIsRunning().get()
					&& rtmpMuxer2.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		});

		assertTrue(rtmpMuxer2.getIsRunning().get());
		assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, rtmpMuxer2.getStatus());

		// Clean up
		avformat_free_context(outputFormatContext);
	}

	// ==================== ID3/Metadata tests ====================

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

}
