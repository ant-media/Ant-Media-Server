package io.antmedia.integration;

import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NONE;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_rescale_q;
import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import io.antmedia.AppSettings;
import io.antmedia.settings.ServerSettings;
import io.antmedia.datastore.db.types.Broadcast;
import com.amazonaws.util.Base32;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.security.ITokenService;
import io.antmedia.security.TOTPGenerator;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.ffmpeg.avformat.AVInputFormat;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avformat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.EndpointMuxer;
import io.antmedia.rest.model.Result;

public class MuxingTest {

	private static Process red5Process;
	private static Process tmpExec;

	private static final String SERVER_ADDR = "127.0.0.1"; 

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	private static int OS_TYPE;
	private static String ffmpegPath = "ffmpeg";
	public static long audioStartTimeMs;
	public static long videoStartTimeMs;
	public static boolean audioExists;
	public static boolean videoExists;
	
	protected static Logger logger = LoggerFactory.getLogger(MuxingTest.class);
	public static long videoDuration;
	public static long audioDuration;


	static {
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.startsWith("mac os x") || osName.startsWith("darwin")) {
			OS_TYPE = MAC_OS_X;
		} else if (osName.startsWith("windows")) {
			OS_TYPE = WINDOWS;
		} else if (osName.startsWith("linux")) {
			OS_TYPE = LINUX;
		}
	}

	@Rule
	public TestRule watcher = new TestWatcher() {
		protected void starting(Description description) {
			System.out.println("Starting test: " + description.getMethodName());
		}

		protected void failed(Throwable e, Description description) {
			System.out.println("Failed test: " + description.getMethodName() );
			e.printStackTrace();

		};
		protected void finished(Description description) {
			System.out.println("Finishing test: " + description.getMethodName());
		};
	};

	@BeforeClass
	public static void beforeClass() {
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
	//	av_register_all();
		avformat_network_init();

	}

	@Before
	public void before() {
		// runs before every test code
		/*
		 * try { delete(new File(FULL_RED5_PATH + "/webapps/vod/streams")); }
		 * catch (IOException e) { e.printStackTrace(); }
		 */

	}

	@After
	public void after() {
		// runs after every test code
	}

	@AfterClass
	public static void afterClass() {
		// stop red5 server
	}



	@Test
	public void testRtmpAndVODStreaming() {
		assertTrue("duplicate test AppFunctionalV2Test#testSendRTMPStream", true);
	}

	

	@Test
	public void testSupportVideoCodecUnSupportedAudioCodec() {
		String streamName = "bug_test2"  + (int)(Math.random()*9999);

		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(
				ffmpegPath + " -re -i src/test/resources/test.flv -acodec pcm_alaw -vcodec copy -f flv rtmp://"
						+ SERVER_ADDR + "/LiveApp/" + streamName);

		try {
			Thread.sleep(5000);

			assertFalse(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8"));
			
			Awaitility.await().atMost(5, TimeUnit.SECONDS)
				.until(() -> {
					return !rtmpSendingProcess.isAlive();
				});

			assertFalse(rtmpSendingProcess.isAlive());

			// stop rtmp streaming
			rtmpSendingProcess.destroy();

		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}

		Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> {
			RestServiceV2Test restService = new RestServiceV2Test();

			return 0 == restService.callGetLiveStatistics();
		});

	}

	@Test
	public void testConcurrentStreaming() {
		try {
			String streamName1 = "conccurent" + (int) (Math.random() * 1000);
			// make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec pcm_alaw -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName1);

			String streamName2 = "conccurent" + (int) (Math.random() * 1000);
			Process rtmpSendingProcess2 = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec pcm_alaw -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName2);

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
			!rtmpSendingProcess.isAlive()
					);

			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
			!rtmpSendingProcess2.isAlive()
					);

			assertFalse(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName1 + ".mp4"));
			assertFalse(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName2 + ".mp4"));

			assertFalse(rtmpSendingProcess.isAlive());
			assertFalse(rtmpSendingProcess2.isAlive());

			rtmpSendingProcess.destroy();


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> {
			RestServiceV2Test restService = new RestServiceV2Test();

			if (0 == restService.callGetLiveStatistics()) {
				return true;
			}
			else {
				List<Broadcast> broadcastList = restService.callGetBroadcastList();
				if (broadcastList != null) {
					for (Broadcast broadcast : broadcastList) {
						logger.info("stream on the server side:{} status:{}", broadcast.getStreamId(), broadcast.getStatus());
					}
				}
				
			}
			return false;
		});

	}

	@Test
	public void testUnsupportedCodecForMp4() {

		// send rtmp stream with ffmpeg to red5
		String streamName = "bug_test" + (int)(Math.random() * 99877);

		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(ffmpegPath + " -re -i src/test/resources/test.flv  -f flv rtmp://"
				+ SERVER_ADDR + "/LiveApp/" + streamName);
		try {
			Thread.sleep(5000);

			assertFalse(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8"));

			assertFalse(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4"));

			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(()-> 
				!rtmpSendingProcess.isAlive()
			);
			// stop rtmp streaming
			rtmpSendingProcess.destroy();


		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		//wait a little more to let server update statistics

		Awaitility.await().atMost(90, TimeUnit.SECONDS)
		.pollInterval(1, TimeUnit.SECONDS)
		.until(() -> {
			RestServiceV2Test restService = new RestServiceV2Test();

			return 0 == restService.callGetLiveStatistics();
		});

	}

	@Test
	public void testHLSAESEncryption() throws Exception {

		ConsoleAppRestServiceTest.resetCookieStore();
		Result result = ConsoleAppRestServiceTest.callisFirstLogin();
		if (result.isSuccess()) {
			Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
			assertTrue(createInitialUser.isSuccess());
		}

		result = ConsoleAppRestServiceTest.authenticateDefaultUser();
		assertTrue(result.isSuccess());
		
		AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

		// send rtmp stream with ffmpeg
		String streamName = "aes_hls_test" + (int)(Math.random() * 93377);
		
		String hlsEncryptionSetting = appSettings.getHlsEncryptionKeyInfoFile();
		assertEquals("",hlsEncryptionSetting);
		
		appSettings.setHlsEncryptionKeyInfoFile("https://gist.githubusercontent.com/SelimEmre/0256120ad418e9f3184160da63977f99/raw/37f4ea5f161d89b6d05555b0421945e3237499a0/hls_aes.keyinfo");
		ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);

		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(ffmpegPath + " -re -i  src/test/resources/test.flv   -codec copy  -f flv rtmp://"
				+ SERVER_ADDR + "/LiveApp/" + streamName);
		
		try {
			Thread.sleep(5000);
			assertTrue(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8"));
			
			// stop rtmp streaming
			rtmpSendingProcess.destroy();

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		//Restore HLS AES Encryption Setting
		appSettings.setHlsEncryptionKeyInfoFile(null);
		ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
	}

	//	@Test
	public void testAzureRTMPSending() {
		//read bigbunny ts file and send it to the azure
		AVInputFormat findInputFormat = avformat.av_find_input_format("ts");
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();

		//TODO put a ts file having video only below
		int ret = avformat_open_input(inputFormatContext, "", findInputFormat, null);

		System.out.println("open input -> " + ret);
		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
		System.out.println("find stream info -> " + ret);

		AVPacket pkt = avcodec.av_packet_alloc();

		EndpointMuxer endpointMuxer = new EndpointMuxer("rtmp://test-rtmptest-usea.channel.media.azure.net:1935/live/e0c44eb42c2747869c67227f183fad59/test", null);

		//rtmpMuxer.prepare(inputFormatContext);
		endpointMuxer.addVideoStream(1280, 720, null, avcodec.AV_CODEC_ID_H264, 0, false, null);

		assertTrue(endpointMuxer.prepareIO());

		while((ret = av_read_frame(inputFormatContext, pkt)) >= 0) 
		{
			AVStream stream = inputFormatContext.streams(pkt.stream_index());
			if (stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) 
			{
				endpointMuxer.writePacket(pkt, stream);
			}
		}
		System.out.println("leaving from loop");

		pkt.close();

		avformat_close_input(inputFormatContext);

	}


	@Test
	public void testDynamicAddRemoveRTMPV2() 
	{
		assertTrue("This test is merged with RestServiceV2Test#testAddEndpointCrossCheckV2", true);
	}

	
	@Test
	public void testHEVCWithRTMP() throws Exception {
		
		ConsoleAppRestServiceTest.resetCookieStore();
		Result result = ConsoleAppRestServiceTest.callisFirstLogin();
		if (result.isSuccess()) {
			Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
			assertTrue(createInitialUser.isSuccess());
		}

		result = ConsoleAppRestServiceTest.authenticateDefaultUser();
		assertTrue(result.isSuccess());
		AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("live");
		boolean mp4Enabled = appSettings.isMp4MuxingEnabled();
		appSettings.setMp4MuxingEnabled(true);
		
		boolean hlsEnabled = appSettings.isHlsMuxingEnabled();
		appSettings.setHlsMuxingEnabled(true);
		ConsoleAppRestServiceTest.callSetAppSettings("live", appSettings);

		
		String streamId = "hevc"  + (int)(Math.random() * 999999);

		Process rtmpSendingProcess = execute(
				ffmpegPath + " -re -i src/test/resources/test_hevc.flv -codec copy -f flv rtmp://"
						+ SERVER_ADDR + "/live/" + streamId);
		
		
		Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/live/streams/" + streamId+ ".m3u8");
		});
		
		assertTrue(MuxingTest.videoExists);
		assertTrue(MuxingTest.audioExists);
		
		
		rtmpSendingProcess.destroy();
		
		
		Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/live/streams/" + streamId+ ".mp4");
		});
		
		
		appSettings.setMp4MuxingEnabled(mp4Enabled);
		appSettings.setHlsMuxingEnabled(hlsEnabled);
		ConsoleAppRestServiceTest.callSetAppSettings("live", appSettings);
		
		
	}

	@Test
	public void testMp4Muxing() {

		try {
			ConsoleAppRestServiceTest.resetCookieStore();
			Result result = ConsoleAppRestServiceTest.callisFirstLogin();
			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());
			AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
			boolean mp4Enabled = appSettings.isMp4MuxingEnabled();
			appSettings.setMp4MuxingEnabled(false);
			boolean hlsEnabled = appSettings.isHlsMuxingEnabled();
			appSettings.setHlsMuxingEnabled(true);
			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

			// send rtmp stream with ffmpeg to red5
			String streamName = "live_test"  + (int)(Math.random() * 999999);

			// make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);
			
			try {
				Process finalProcess = rtmpSendingProcess;
				Awaitility.await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(()-> {
					return finalProcess.isAlive();
				});
			}
			catch (Exception e) {
				//try one more time because it may give high resource usage
				 rtmpSendingProcess = execute(
							ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
									+ SERVER_ADDR + "/LiveApp/" + streamName);
            }
			
			

			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName+ ".m3u8");
			});

			result = RestServiceV2Test.callEnableMp4Muxing(streamName, 1);
			assertTrue(result.isSuccess());
			assertNotNull(result.getMessage());
			Thread.sleep(5000);

			result = RestServiceV2Test.callEnableRecording(streamName, 1, "webm");
			//it should return false because WebM cannot be recorded with incoming RTMP stream.
			//RTMP stream(H264, AAC)  codecs are not compatible with webm
			assertFalse(result.isSuccess());

			result = RestServiceV2Test.callEnableMp4Muxing(streamName, 0);
			assertTrue(result.isSuccess());

			//it should be true this time, because stream mp4 setting is 1 although general setting is disabled

			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName+ ".mp4", 5000);
			});

			rtmpSendingProcess.destroyForcibly();
			
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return null == RestServiceV2Test.getBroadcast(streamName);
			});
			

			appSettings.setMp4MuxingEnabled(mp4Enabled);
			appSettings.setHlsMuxingEnabled(hlsEnabled);
			ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public static boolean testFile(String absolutePath) {
		return testFile(absolutePath, 0, false);
	}

	public static boolean testFile(String absolutePath, boolean fullRead) {
		return testFile(absolutePath, 0, fullRead);
	}

	public static boolean testFile(String absolutePath, int expectedDurationInMS) {
		return testFile(absolutePath, expectedDurationInMS, false);
	}

	public static boolean testFile(String absolutePath, int expectedDurationInMS, boolean fullRead) {
		int ret;
		audioExists = false;
		videoExists = false;
		logger.info("Tested File: {}", absolutePath);

		//AVDictionary dic = null;

		/*
		if(absolutePath.contains("mpd")) {
			findInputFormat = avformat.av_find_input_format("dash");
			av_dict_set(dic, "protocol_whitelist","mpd,mpeg,dash,m4s", 0);
		}
		 */
		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();
		if (inputFormatContext == null) {
			System.out.println("cannot allocate input context");
			return false;
		}
		
		//allowed_extensions
		
		AVDictionary optionsDictionary = new AVDictionary();
			
		av_dict_set(optionsDictionary, "allowed_extensions", "ALL", 0);

		if ((ret = avformat_open_input(inputFormatContext, absolutePath, null, (AVDictionary) optionsDictionary)) < 0) {
			System.out.println("cannot open input context: " + absolutePath);
			return false;
		}

		/*
			byte[] data = new byte[2048];
			av_strerror(ret, data, data.length);
			throw new IllegalStateException("cannot open input context. Error is " + new String(data, 0, data.length));
		 */

		//av_dump_format(inputFormatContext,0,"test",0);

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary) null);
		if (ret < 0) {
			System.out.println("Could not find stream information\n");
			return false;
		}

		int streamCount = inputFormatContext.nb_streams();
		if (streamCount == 0) {
			return false;
		}

		boolean streamExists = false;
		for (int i = 0; i < streamCount; i++) {
			AVCodecParameters codecpar = inputFormatContext.streams(i).codecpar();

			if (codecpar.codec_type() == AVMEDIA_TYPE_VIDEO) 
			{
				assertTrue(codecpar.width() != 0);
				assertTrue(codecpar.height() != 0);
				assertTrue(codecpar.format() != AV_PIX_FMT_NONE);
				videoStartTimeMs = av_rescale_q(inputFormatContext.streams(i).start_time(), inputFormatContext.streams(i).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
				
				videoDuration = av_rescale_q(inputFormatContext.streams(i).duration(),  inputFormatContext.streams(i).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);


				videoExists = true;
				streamExists = true;
			} else if (codecpar.codec_type() == AVMEDIA_TYPE_AUDIO) 
			{
				assertTrue(codecpar.sample_rate() != 0);
				audioStartTimeMs = av_rescale_q(inputFormatContext.streams(i).start_time(), inputFormatContext.streams(i).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
				
				audioDuration = av_rescale_q(inputFormatContext.streams(i).duration(),  inputFormatContext.streams(i).time_base(), MuxAdaptor.TIME_BASE_FOR_MS);
				audioExists = true;
				streamExists = true;
			}
		}
		if (!streamExists) {
			return streamExists;
		}

		int i = 0;
		while (fullRead || i < 3) {
			AVPacket pkt = new AVPacket();
			ret = av_read_frame(inputFormatContext, pkt);

			if (ret < 0) {
				break;

			}
			i++;
			avcodec.av_packet_unref(pkt);
			pkt.close();
			pkt = null;
		}

		if (inputFormatContext.duration() != AV_NOPTS_VALUE) {
			long durationInMS = inputFormatContext.duration() / 1000;

			if (expectedDurationInMS != 0) {
				if ((durationInMS < (expectedDurationInMS - 2000)) || (durationInMS > (expectedDurationInMS + 2000))) {
					System.out.println("Failed: duration of the stream: " + durationInMS + " expected duration is: " + expectedDurationInMS);
					return false;
				}
			}
		}

		avformat_close_input(inputFormatContext);
		return true;

	}

	public static Process execute(final String command) {
		tmpExec = null;
		new Thread() {
			public void run() {
				try {

					tmpExec = Runtime.getRuntime().exec(command);
					InputStream errorStream = tmpExec.getErrorStream();
					byte[] data = new byte[1024];
					int length = 0;

					while ((length = errorStream.read(data, 0, data.length)) > 0) {
						System.out.println(new String(data, 0, length));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}.start();

		while (tmpExec == null) {
			try {
				System.out.println("Waiting for exec get initialized...");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return tmpExec;
	}



	private static void readErrorStream(final Process proc) {
		new Thread() {
			public void run() {
				try {
					InputStream errorStream = proc.getErrorStream();
					byte[] data = new byte[1024];
					int length = 0;

					while ((length = errorStream.read(data, 0, data.length)) > 0) {
						System.out.println(new String(data, 0, length));
					}
				} catch (IOException e) {
					fail(e.getMessage());
					e.printStackTrace();
				}
			};
		}.start();
	}

	private static void readInputStream(final Process proc) {
		new Thread() {
			public void run() {
				try {
					InputStream inputStream = proc.getInputStream();
					byte[] data = new byte[1024];
					int length = 0;

					while ((length = inputStream.read(data, 0, data.length)) > 0) {
						System.out.println(new String(data, 0, length));
					}
				} catch (IOException e) {
					fail(e.getMessage());
					e.printStackTrace();
				}
			};
		}.start();
	}

	public static void closeRed5() {
		// String path = "./red5-shutdown.sh" ;
		String path = "src/test/resources/kill_red5.sh";
		try {
			Process exec = Runtime.getRuntime().exec(path, null, null); // new
			// String[]
			// {"cd
			// red5;
			// "+
			// path});
			readErrorStream(exec);
			readInputStream(exec);
			exec.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public static void delete(File file) throws IOException {

		if (!file.exists()) {
			return;
		}

		if (file.isDirectory()) {

			// directory is empty, then delete it
			if (file.list().length == 0) {

				file.delete();

			} else {

				// list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					// construct the file structure
					File fileDelete = new File(file, temp);

					// recursive delete
					delete(fileDelete);
				}

				// check the directory again, if empty then delete it
				if (file.list().length == 0) {
					file.delete();
				}
			}

		} else {
			// if file, then delete it
			file.delete();
		}
	}

	public static boolean isURLAvailable(String address) {
		try {

			URL url = new URL(address);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(45000);
			urlConnection.setRequestMethod("HEAD");
			urlConnection.setDoInput(true);

			HttpURLConnection.setFollowRedirects(true);
			urlConnection.connect();

			InputStream in = urlConnection.getInputStream(); // getAssets().open("kralfmtop10.htm");

			byte[] byteArray = org.apache.commons.io.IOUtils.toByteArray(in);

			in.close();

			return true;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return false;
	}

	public static byte[] getByteArray(String address) {
		try {

			URL url = new URL(address);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(45000);
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);

			HttpURLConnection.setFollowRedirects(true);
			urlConnection.connect();

			InputStream in = urlConnection.getInputStream(); // getAssets().open("kralfmtop10.htm");

			byte[] byteArray = org.apache.commons.io.IOUtils.toByteArray(in);

			in.close();

			return byteArray;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}
	
	@Test
	public void testHLSSegmentFileName() {

		try {
			ConsoleAppRestServiceTest.resetCookieStore();
			Result result = ConsoleAppRestServiceTest.callisFirstLogin();
			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());
			AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");
			boolean hlsEnabled = appSettings.isHlsMuxingEnabled();
			appSettings.setHlsMuxingEnabled(true);
			String hlsSegmentFileNameFormat = appSettings.getHlsSegmentFileSuffixFormat();
			appSettings.setHlsSegmentFileSuffixFormat("-%Y%m%d-%s");
			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
			assertTrue(result.isSuccess());

			// send rtmp stream with ffmpeg to red5
			String streamName = "live_test"  + (int)(Math.random() * 999999);

			// make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);
			
			try {
				Process finalProcess = rtmpSendingProcess;
				Awaitility.await().pollDelay(5, TimeUnit.SECONDS).atMost(10, TimeUnit.SECONDS).until(()-> {
					return finalProcess.isAlive();
				});
			}
			catch (Exception e) {
				//try one more time because it may give high resource usage
				 rtmpSendingProcess = execute(
							ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
									+ SERVER_ADDR + "/LiveApp/" + streamName);
            }
			
			

			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName+ ".m3u8");
			});

			
			String content = getM3U8Content("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName+ ".m3u8");
			
			
			long now = System.currentTimeMillis();
	        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	        String formattedTime = formatter.format(new Date(now));

	        //(now/10000) we can not guarantee we will have a ts created just now so use regex like live_test873835-20241218-1734XXXX.ts
	        String regex = streamName+"-"+formattedTime+"-"+(now/10000000) + "\\d{4}\\.ts";
	        System.out.println("regex for ts name:"+regex);

			Pattern pattern = Pattern.compile(regex);
	        Matcher matcher = pattern.matcher(content);
	        assertTrue (matcher.find());
			
			rtmpSendingProcess.destroyForcibly();
			
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return null == RestServiceV2Test.getBroadcast(streamName);
			});
			
			appSettings.setHlsMuxingEnabled(hlsEnabled);
			appSettings.setHlsSegmentFileSuffixFormat(hlsSegmentFileNameFormat);
			ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private String getM3U8Content(String urlString) throws Exception {
		URL url = new URL(urlString);

        // Open a connection and create a BufferedReader
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        // Read the URL content into a StringBuilder
        StringBuilder content = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }

        // Close the reader
        reader.close();

        // Print the content
        System.out.println("URL Content:");
        System.out.println(content.toString());
        
        return content.toString();
	}
	@Test
	public void testRTMPURlFormat() throws Exception {
		// rtmp://localhost/LiveApp/stream/token/subid/subcode
		ConsoleAppRestServiceTest.resetCookieStore();
		Result result = ConsoleAppRestServiceTest.callisFirstLogin();
		if (result.isSuccess()) {
			Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
			assertTrue(createInitialUser.isSuccess());
		}

		result = ConsoleAppRestServiceTest.authenticateDefaultUser();
		assertTrue(result.isSuccess());

		String appName = "live";
		String restUrl = "http://" + ServerSettings.getLocalHostAddress() +":5080/" + appName +"/rest";
		AppSettings appSettings = ConsoleAppRestServiceTest.callGetAppSettings(appName);

		appSettings.setEnableTimeTokenForPublish(true);
		appSettings.setTimeTokenSecretForPublish("random_thing");
		appSettings.setIpFilterEnabled(false);
		ConsoleAppRestServiceTest.callSetAppSettings(appName,appSettings);

		String streamId = "stream_" + (int) (Math.random()*10000);
		String subscriberId = "sub12345";
		String secret = "abcdabcd";
		String type = "publish";

		result = ConsoleAppRestServiceTest.callCreateTOTPSubscriber(restUrl ,subscriberId, secret, streamId, type);
		assertTrue(result.isSuccess());

		byte[] secretBytes = Base32.decode(secret);
		String code = TOTPGenerator.generateTOTP(secretBytes, appSettings.getTimeTokenPeriod(), 6, ITokenService.HMAC_SHA1);

		String rtmpURL = "rtmp://localhost/" + appName + "/" + streamId + "/token/" + subscriberId +"/" + code;
		Process process = execute(ffmpegPath + " -re -i src/test/resources/test.flv "
				+ "-c copy -f flv " + rtmpURL);


		Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcast	= ConsoleAppRestServiceTest.callGetBroadcast(restUrl,streamId);
			return broadcast != null && AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus());
		});

		appSettings.setEnableTimeTokenForPublish(false);
		appSettings.setIpFilterEnabled(true);
		ConsoleAppRestServiceTest.callSetAppSettings(appName,appSettings);

		process.destroy();
		
		appSettings.setEnableTimeTokenForPublish(false);
		appSettings.setTimeTokenSecretForPublish("");
		ConsoleAppRestServiceTest.callSetAppSettings(appName,appSettings);
		
	}
}
