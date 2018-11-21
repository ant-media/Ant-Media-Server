package io.antmedia.integration;

import static org.bytedeco.javacpp.avcodec.av_packet_unref;
import static org.bytedeco.javacpp.avformat.av_read_frame;
import static org.bytedeco.javacpp.avformat.av_register_all;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_find_stream_info;
import static org.bytedeco.javacpp.avformat.avformat_network_init;
import static org.bytedeco.javacpp.avformat.avformat_open_input;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.javacpp.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.javacpp.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.javacpp.avutil.AV_PIX_FMT_NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import io.antmedia.rest.BroadcastRestService.LiveStatistics;


public class MuxingTest {

	private static Process red5Process;
	private static Process tmpExec;

	private static final String SERVER_ADDR = "127.0.0.1"; // "34.206.64.213";

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;
	private static int OS_TYPE;
	private static String ffmpegPath = "ffmpeg";

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
		av_register_all();
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
		// closeRed5();

	}



	@Test
	public void testVODStreaming() {

		// send rtmp stream with ffmpeg to red5
		String streamName = "vod_test";

		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(
				ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
						+ SERVER_ADDR + "/LiveApp/" + streamName);

		try {
			Thread.sleep(10000);

			// stop rtmp streaming
			rtmpSendingProcess.destroy();

			Thread.sleep(5000);

			assertTrue(testFile("rtmp://" + SERVER_ADDR + "/LiveApp/" + streamName + ".mp4", 10000));

			// check that stream can be watchable by hls
			assertTrue(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8", 10000));

			// check that mp4 is created successfully and can be playable
			assertTrue(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4", 10000));

			// check that stream can be playable with rtsp
			assertTrue(testFile("rtsp://" + SERVER_ADDR + ":5554/LiveApp/" + streamName + ".mp4", true));

			assertTrue(testFile("rtsp://" + SERVER_ADDR + ":5554/LiveApp/" + streamName + ".mp4"));

			Thread.sleep(1000);

		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
		RestServiceTest restService = new RestServiceTest();

		LiveStatistics liveStatistics = restService.callGetLiveStatistics();
		assertEquals(0, liveStatistics.totalLiveStreamCount);
	}

	// TODO: check that if there is memory leak, if muxing is stopped by somehow

	@Test
	public void testSupportVideoCodecUnSupportedAudioCodec() {
		String streamName = "bug_test2";

		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(
				ffmpegPath + " -re -i src/test/resources/test.flv -acodec pcm_alaw -vcodec copy -f flv rtmp://"
						+ SERVER_ADDR + "/LiveApp/" + streamName);

		try {
			Thread.sleep(10000);

			// TODO: check that when live stream is requested with rtsp, server
			// should not be shutdown

			// stop rtmp streaming
			rtmpSendingProcess.destroy();

			Thread.sleep(8000);

			boolean testResult = testFile("rtmp://" + SERVER_ADDR + "/LiveApp/" + streamName + ".mp4");
			assertTrue(testResult);

			// check that mp4 is not created
			testResult = testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4");
			assertTrue(testResult);

			// check that stream is not created by hls muxer
			// testResult =
			// testFile("http://"+SERVER_ADDR+":5080/LiveApp/streams/" +
			// streamName + ".m3u8");
			// assertTrue(testResult);

			// TODO: check that when stream is requested with rtsp, server
			// should not be shutdown
			testResult = testFile("rtsp://" + SERVER_ADDR + ":5554/LiveApp/" + streamName + ".mp4");
			assertTrue(testResult);
			
			//let the server update stats
			//wait a little to let the server finish state after rtsp fetching
			Thread.sleep(2000);

		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
		
		RestServiceTest restService = new RestServiceTest();

		LiveStatistics liveStatistics = restService.callGetLiveStatistics();
		assertEquals(0, liveStatistics.totalLiveStreamCount);

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

			Thread.sleep(15000);

			rtmpSendingProcess.destroy();
			rtmpSendingProcess2.destroy();

			Thread.sleep(12000);

			assertTrue(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName1 + ".mp4", 16000));
			assertTrue(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName2 + ".mp4", 16000));

		} catch (Exception e) {
			e.printStackTrace();
		}
		
		RestServiceTest restService = new RestServiceTest();

		LiveStatistics liveStatistics = restService.callGetLiveStatistics();
		assertEquals(0, liveStatistics.totalLiveStreamCount);

	}

	@Test
	public void testUnsupportedCodecForMp4() {

		// send rtmp stream with ffmpeg to red5
		String streamName = "bug_test";

		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(ffmpegPath + " -re -i src/test/resources/test.flv  -f flv rtmp://"
				+ SERVER_ADDR + "/LiveApp/" + streamName);

		try {
			Thread.sleep(10000);

			// TODO: check that when live stream is requested with rtsp, server
			// should not be shutdown

			// stop rtmp streaming
			rtmpSendingProcess.destroy();

			Thread.sleep(5000);

			boolean testResult = testFile("rtmp://" + SERVER_ADDR + "/LiveApp/" + streamName + ".mp4");
			assertTrue(testResult);

			// check that stream is not created by hls muxer
			testResult = testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8");
			assertFalse(testResult);

			// check that mp4 is not created
			testResult = testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4");
			assertTrue(testResult);

			// TODO: check that when stream is requested with rtsp, server
			// should not be shutdown

			assertFalse(testFile("rtsp://" + SERVER_ADDR + ":5554/LiveApp/" + streamName));
			// assert false because rtp does not support flv1

		} catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
		
		//wait a little more to let server update statistics
		
		Awaitility.await().atMost(10, TimeUnit.SECONDS)
			.pollInterval(1, TimeUnit.SECONDS)
			.until(() -> {
				RestServiceTest restService = new RestServiceTest();
	
				LiveStatistics liveStatistics = restService.callGetLiveStatistics();
				return 0 == liveStatistics.totalLiveStreamCount;
			});
		
	}

	// TODO: check if rtsp failed in some state, how it can be free resources

	// TODO: make rtsp send with tcp and open this test
	// @Test
	public void testRTSPSending() {
		try {

			// send rtmp stream with ffmpeg to red5
			String streamName = "live_rtsp_test";

			// make sure that ffmpeg is installed and in path
			Process rtspSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -rtsp_transport udp -f rtsp rtsp://"
					+ SERVER_ADDR + ":5554/LiveApp/" + streamName);

			Thread.sleep(10000);

			// check that stream can be watchable by rtsp
			// use ipv4 address to play rtsp stream

			assertTrue(testFile("rtsp://" + SERVER_ADDR + ":5554/LiveApp/" + streamName));

			// check that stream can be watchable by hls
			assertTrue(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8"));
			//
			// //stop rtsp streaming
			rtspSendingProcess.destroy();

			Thread.sleep(15000);

			assertTrue(testFile("rtmp://" + SERVER_ADDR + "/LiveApp/" + streamName));

			assertTrue(testFile("rtsp://" + SERVER_ADDR + ":5554/LiveApp/" + streamName + ".mp4", true));

			assertTrue(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4"));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		RestServiceTest restService = new RestServiceTest();

		LiveStatistics liveStatistics = restService.callGetLiveStatistics();
		assertEquals(0, liveStatistics.totalLiveStreamCount);

	}

	@Test
	public void testRTMPSending() {

		try {
			// send rtmp stream with ffmpeg to red5
			String streamName = "live_test"  + (int)(Math.random() * 999999);

			// make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);

			Thread.sleep(10000);

			// check that stream can be watchable by rtsp
			// use ipv4 address to play rtsp stream

			// TODO: open this test when ant media server supports rtsp tcp
			// transport
			boolean testResult;
			// = testFile("rtsp://"+SERVER_ADDR+":5554/LiveApp/" + streamName);
			// assertTrue(testResult);

			testResult = testFile("rtmp://" + SERVER_ADDR + "/LiveApp/" + streamName);
			assertTrue(testResult);

			// check that stream can be watchable by hls
			testResult = testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8");
			assertTrue(testResult);

			// stop rtmp streaming
			rtmpSendingProcess.destroy();

			Thread.sleep(5000);

			// check that mp4 is created successfully and can be playable
			testResult = testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4");
			assertTrue(testResult);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		RestServiceTest restService = new RestServiceTest();

		LiveStatistics liveStatistics = restService.callGetLiveStatistics();
		assertEquals(0, liveStatistics.totalLiveStreamCount);

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

		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();

		if (inputFormatContext == null) {
			System.out.println("cannot allocate input context");
			return false;
		}

		if ((ret = avformat_open_input(inputFormatContext, absolutePath, null, (AVDictionary) null)) < 0) {
			System.out.println("cannot open input context: " + absolutePath);
			return false;
		}

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
			AVCodecContext codecContext = inputFormatContext.streams(i).codec();
			if (codecContext.codec_type() == AVMEDIA_TYPE_VIDEO) {
				assertTrue(codecContext.width() != 0);
				assertTrue(codecContext.height() != 0);
				assertTrue(codecContext.pix_fmt() != AV_PIX_FMT_NONE);
				streamExists = true;
			} else if (codecContext.codec_type() == AVMEDIA_TYPE_AUDIO) {
				assertTrue(codecContext.sample_rate() != 0);
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
			av_packet_unref(pkt);
		}

		if (inputFormatContext.duration() != AV_NOPTS_VALUE) {
			long durationInMS = inputFormatContext.duration() / 1000;

			if (expectedDurationInMS != 0) {
				if ((durationInMS < (expectedDurationInMS - 2000)) || (durationInMS > (expectedDurationInMS + 2000))) {
					System.out.println("Failed: duration of the stream: " + durationInMS);
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
				Thread.sleep(1000);
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
				// System.out.println("Directory is deleted : "
				// + file.getAbsolutePath());

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
					// System.out.println("Directory is deleted : "
					// + file.getAbsolutePath());
				}
			}

		} else {
			// if file, then delete it
			file.delete();
			// System.out.println("File is deleted : " +
			// file.getAbsolutePath());
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

}
