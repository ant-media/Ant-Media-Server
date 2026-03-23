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
public class MuxerMp4RecordingTest extends MuxerTestBase {

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

	@Test
	public void testMp4Muxing() {
		File mp4File = testMp4Muxing("lkdlfkdlfkdlfk");

		VideoInfo fileInfo = VideoProber.getFileInfo(mp4File.getAbsolutePath());
		assertTrue(252 - fileInfo.videoPacketsCount < 5);
		assertTrue(431 - fileInfo.audioPacketsCount < 5);
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
	public void testRecordingWithRecordingSubfolder() {
		appSettings.setRecordingSubfolder("records");
		testMp4Muxing("record" + RandomUtils.nextInt(0, 10000));
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

	@Test
	public void testReplaceMultipleSlashes() {
		String replaceDoubleSlashesWithSingleSlash = RecordMuxer.replaceDoubleSlashesWithSingleSlash("WebRTCAppEE/streams///stream1.mp4");
		assertEquals("WebRTCAppEE/streams/stream1.mp4", replaceDoubleSlashesWithSingleSlash);
	}

	@Test
	public void testWriteTrailer() throws IOException, InterruptedException {

		Vertx vertx = Vertx.vertx();
		File file = Mockito.spy(new File("test"));
		MuxerTestBase.StorageClientMock storageClient = Mockito.spy(new MuxerTestBase.StorageClientMock());
		Mockito.when(storageClient.isEnabled()).thenReturn(true);
		MuxerTestBase.RecordMuxerMock recordMuxerMock = Mockito.spy(new MuxerTestBase.RecordMuxerMock(storageClient, vertx, "streams"));
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
		assert (MuxerTestBase.StorageClientMock.saveCalledWithCorrectParams);

		recordMuxerMock.writeTrailer();


		//trailer already written should not invoke again
		verify(recordMuxerMock, times(1)).finalizeRecordFile(any());

		verify(recordMuxerMock, times(1)).getFinalFileName(anyBoolean());

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

}
