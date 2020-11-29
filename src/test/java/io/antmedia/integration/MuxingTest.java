package io.antmedia.integration;

import static org.bytedeco.ffmpeg.global.avformat.av_read_frame;
import static org.bytedeco.ffmpeg.global.avformat.av_register_all;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_network_init;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avutil.av_dict_set;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_AUDIO;
import static org.bytedeco.ffmpeg.global.avutil.AVMEDIA_TYPE_VIDEO;
import static org.bytedeco.ffmpeg.global.avutil.AV_NOPTS_VALUE;
import static org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_NONE;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessHandle.Info;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
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

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.muxer.RtmpMuxer;
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
	}



	@Test
	public void testVODStreaming() {

		// send rtmp stream with ffmpeg to red5
		String streamName = "vod_test" + (int)(Math.random()*10000);

		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(
				ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
						+ SERVER_ADDR + "/LiveApp/" + streamName);

		try {
			Thread.sleep(5000);
			Info processInfo = rtmpSendingProcess.info();

			// stop rtmp streaming
			rtmpSendingProcess.destroy();
			int duration = (int)(System.currentTimeMillis() - processInfo.startInstant().get().toEpochMilli());
			
			// check that stream can be watchable by hls
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> 
				testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8", duration)
			);
			
			// check that mp4 is created successfully and can be playable
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() ->
				testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4", duration)
			);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		Awaitility.await().atMost(55, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> { 
			RestServiceV2Test restService = new RestServiceV2Test();

			return 0 == restService.callGetLiveStatistics();
		});
		
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

			Thread.sleep(5000);
			
			assertFalse(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName1 + ".mp4"));
			assertFalse(testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName2 + ".mp4"));
			
			assertFalse(rtmpSendingProcess.isAlive());
			assertFalse(rtmpSendingProcess2.isAlive());

			rtmpSendingProcess.destroy();
		

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		Awaitility.await().atMost(140, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(()-> {
			RestServiceV2Test restService = new RestServiceV2Test();

			return 0 == restService.callGetLiveStatistics();
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

			assertFalse(rtmpSendingProcess.isAlive());
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
	public void testRTMPSending() {

		try {
			// send rtmp stream with ffmpeg to red5
			String streamName = "live_test"  + (int)(Math.random() * 999999);

			// make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);

			// check that stream can be watchable by rtsp
			// use ipv4 address to play rtsp stream

			//no support rtmp playback anymore
			//Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			//.until(() -> testFile("rtmp://" + SERVER_ADDR + "/LiveApp/" + streamName));		
			

			// check that stream can be watchable by hls
			Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".m3u8"));

			// stop rtmp streaming
			rtmpSendingProcess.destroy();

			// check that mp4 is created successfully and can be playable
			Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(4, TimeUnit.SECONDS).until(()->{
				return testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName + ".mp4");
			});
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
		 Awaitility.await().atMost(80, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
		 .until(() -> {
				RestServiceV2Test restService = new RestServiceV2Test();
				
				return 0 == restService.callGetLiveStatistics();
			});
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
		
		RtmpMuxer rtmpMuxer = new RtmpMuxer("rtmp://test-rtmptest-usea.channel.media.azure.net:1935/live/e0c44eb42c2747869c67227f183fad59/test");
		
		//rtmpMuxer.prepare(inputFormatContext);
		rtmpMuxer.addVideoStream(1280, 720, null, avcodec.AV_CODEC_ID_H264, 0, false, null);
		
		rtmpMuxer.prepareIO();
		
		while((ret = av_read_frame(inputFormatContext, pkt)) >= 0) 
		{
			AVStream stream = inputFormatContext.streams(pkt.stream_index());
			if (stream.codecpar().codec_type() == AVMEDIA_TYPE_VIDEO) 
			{
				rtmpMuxer.writePacket(pkt, stream);
			}
		}
		System.out.println("leaving from loop");
		
		pkt.close();
		
		avformat_close_input(inputFormatContext);
		
	}
	
	@Test
	public void testDynamicAddRemoveRTMP() 
	{
		String streamId = "live_test"  + (int)(Math.random() * 999999);
		
		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(
				ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
						+ SERVER_ADDR + "/LiveApp/" + streamId);
		
		 Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
		 .until(() -> RestServiceV2Test.callGetBroadcast(streamId).getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));
		
		 String streamIdDynamic = "dynamic_stream" + (int)(Math.random() * 999999);
		 String dynamicRtmpURL = "rtmp://localhost/LiveApp/" + streamIdDynamic;
		 try {
			 Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				 //if stream is being prepared, it may return false, so try again 
				 Result result = RestServiceV2Test.addEndpoint(streamId, dynamicRtmpURL);
				 return result.isSuccess();
			 });
			
			 Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			 .until(() -> { 
					 Broadcast broadcast = RestServiceV2Test.callGetBroadcast(streamIdDynamic);
					 if (broadcast != null) {
						 return broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING); 
					 }
					 return false;
			 	});
			 
			 Result result = RestServiceV2Test.removeEndpoint(streamId, dynamicRtmpURL);
			 assertTrue(result.isSuccess());
			 
			 Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			 .until(() -> RestServiceV2Test.callGetBroadcast(streamIdDynamic) == null );
			 
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		 
		 rtmpSendingProcess.destroy();
	}
	
	@Test
	public void testDynamicAddRemoveRTMPV2() 
	{
		String streamId = "live_test"  + (int)(Math.random() * 999999);
		
		// make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(
				ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
						+ SERVER_ADDR + "/LiveApp/" + streamId);
		
		 Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
		 .until(() -> RestServiceV2Test.callGetBroadcast(streamId).getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));
		
		 String streamIdDynamic = "dynamic_stream" + (int)(Math.random() * 999999);
		 String dynamicRtmpURL = "rtmp://localhost/LiveApp/" + streamIdDynamic;
		 
		 Endpoint endpoint = new Endpoint();
		 endpoint.setRtmpUrl(dynamicRtmpURL);
		 
		 try {
			 Awaitility.await().atMost(25, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> {
				 //if stream is being prepared, it may return false, so try again 
				 Result result = RestServiceV2Test.addEndpointV2(streamId, endpoint);
				 return result.isSuccess();
			 });
			
			
			 Awaitility.await().atMost(25, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			 .until(() -> { 
					 Broadcast broadcast = RestServiceV2Test.callGetBroadcast(streamIdDynamic);
					 if (broadcast != null) {
						 return broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING); 
					 }
					 return false;
			 	});
			 
			 Broadcast broadcast = RestServiceV2Test.callGetBroadcast(streamId);
			 
			 Result result = RestServiceV2Test.removeEndpointV2(streamId, broadcast.getEndPointList().get(0).getEndpointServiceId());
			 assertTrue(result.isSuccess());
			 
			 Awaitility.await().atMost(25, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			 .until(() -> RestServiceV2Test.callGetBroadcast(streamIdDynamic) == null );
			 
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		 
		 rtmpSendingProcess.destroy();
	}
	
	@Test
	public void testVP8Muxing() {
		try {
			ConsoleAppRestServiceTest.resetCookieStore();
			Result result = ConsoleAppRestServiceTest.callisFirstLogin();
			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());
			
			// send rtmp stream with ffmpeg to red5
			String streamName = "live_test"  + (int)(Math.random() * 999999);

			// make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);

            Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName+ ".m3u8");
            });

            result = RestServiceV2Test.callEnableRecording(streamName, 1, "webm");
            //it should return false because WebM cannot be recorded with incoming RTMP stream.
            //RTMP stream(H264, AAC)  codecs are not compatible with webm
            
            assertFalse(result.isSuccess());

            rtmpSendingProcess.destroy();
            
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
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
			ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettings);
	            
			
			// send rtmp stream with ffmpeg to red5
			String streamName = "live_test"  + (int)(Math.random() * 999999);

			// make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);

            Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName+ ".m3u8");
            });

            result = RestServiceV2Test.callEnableMp4Muxing(streamName, 1);
            assertTrue(result.isSuccess());
			assertNotNull(result.getMessage());
            Thread.sleep(5000);

			result = RestServiceV2Test.callEnableMp4Muxing(streamName, 0);
			assertTrue(result.isSuccess());

            //it should be true this time, because stream mp4 setting is 1 although general setting is disabled

            Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            	return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/" + streamName+ ".mp4", 5000);
            });

            rtmpSendingProcess.destroy();
            
			appSettings.setMp4MuxingEnabled(mp4Enabled);
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
		System.out.println("Tested File:"+absolutePath);

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

		if ((ret = avformat_open_input(inputFormatContext, absolutePath, null, (AVDictionary) null)) < 0) {
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
			avcodec.av_packet_unref(pkt);
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
				Thread.sleep(3000);
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
	public void testVideoOnlyStreaming() {
		Process rtmpSendingProcess = null;
		String streamName = "live_test"  + (int)(Math.random() * 999999);

		try {
			rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -c copy -an -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		Awaitility.await().atMost(25, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcast = RestServiceV2Test.callGetBroadcast(streamName);
			return broadcast.getSpeed() != 0;
		});

		rtmpSendingProcess.destroy();
	}
	
	@Test
	public void testAudioOnlyStreaming() {
		Process rtmpSendingProcess = null;
		String streamName = "live_test"  + (int)(Math.random() * 999999);

		try {
			rtmpSendingProcess = execute(
					ffmpegPath + " -re -i src/test/resources/test.flv -c copy -vn -f flv rtmp://"
							+ SERVER_ADDR + "/LiveApp/" + streamName);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			Broadcast broadcast = RestServiceV2Test.callGetBroadcast(streamName);
			return broadcast.getSpeed() != 0;
		});

		rtmpSendingProcess.destroy();
	}

}
