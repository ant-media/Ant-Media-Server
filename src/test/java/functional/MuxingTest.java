package functional;

import static org.junit.Assert.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Locale;

import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.red5.server.scope.WebScope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.antstreaming.rtsp.PacketSenderRunnable;

import org.apache.commons.io.FileUtils;
import org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avcodec.*;

public class MuxingTest {


	private static final String FULL_RED5_PATH = "../../softwares/ant-media-server";
	private static final String FULL_FFMPEG_BIN_PATH = "/usr/local/bin";

	private static Process red5Process;
	private Process tmpExec;




	@Test
	public void testVODStreaming() {

		//send rtmp stream with ffmpeg to red5
		String streamName = "vod_test";


		//make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/vod/" + streamName);

		try {
			Thread.sleep(10000);

			//stop rtmp streaming 
			rtmpSendingProcess.destroy();

			Thread.sleep(5000);

			assertTrue(testFile("rtmp://localhost/vod/" + streamName + ".mp4", 10000));

			//check that stream can be watchable by hls
			assertTrue(testFile("http://localhost:5080/vod/streams/" + streamName + ".m3u8", 10000));

			//check that mp4 is created successfully and can be playable
			assertTrue(testFile("http://localhost:5080/vod/streams/" + streamName + ".mp4", 10000));

			//check that stream can be playable with rtsp
			assertTrue(testFile("rtsp://127.0.0.1:5554/vod/" + streamName + ".mp4", true));


			assertTrue(testFile("rtsp://127.0.0.1:5554/vod/" + streamName + ".mp4"));


			Thread.sleep(1000);


		}
		catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}

	//TODO: check that if there is memory leak, if muxing is stopped by somehow


	@Test
	public void testSupportVideoCodecUnSupportedAudioCodec() {
		String streamName = "bug_test2";


		//make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.flv -acodec pcm_alaw -vcodec copy -f flv rtmp://localhost/vod/" + streamName);

		try {
			Thread.sleep(10000);

			//TODO: check that when live stream is requested with rtsp, server should not be shutdown

			//stop rtmp streaming 
			rtmpSendingProcess.destroy();

			Thread.sleep(8000);

			//TODO: check that when stream is requested with rtsp, server should not be shutdown
			boolean testResult = testFile("rtsp://127.0.0.1:5554/vod/" + streamName + ".mp4");
			assertTrue(testResult);

			testResult = testFile("rtmp://localhost/vod/" + streamName + ".mp4");
			assertTrue(testResult);

			//check that mp4 is not created
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".mp4");
			assertTrue(testResult);

			//check that stream is not created by hls muxer
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".m3u8");
			assertTrue(testResult);



		}
		catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}

	}

	@Test
	public void testConcurrentStreaming() {
		try {
			String streamName1 = "conccurent" + (int)(Math.random() * 1000);
			//make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.flv -acodec pcm_alaw -vcodec copy -f flv rtmp://localhost/vod/" + streamName1);

			String streamName2 = "conccurent" + (int)(Math.random() * 1000);
			Process rtmpSendingProcess2 = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.flv -acodec pcm_alaw -vcodec copy -f flv rtmp://localhost/vod/" + streamName2);

			Thread.sleep(15000);

			rtmpSendingProcess.destroy();
			rtmpSendingProcess2.destroy();
			
			Thread.sleep(7000);
			
			assertTrue(testFile("http://localhost:5080/vod/streams/" + streamName1 + ".mp4", 15000));
			assertTrue(testFile("http://localhost:5080/vod/streams/" + streamName2 + ".mp4", 15000));
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}


	@Test
	public void testUnsupportedCodecForMp4() {

		//send rtmp stream with ffmpeg to red5
		String streamName = "bug_test";


		//make sure that ffmpeg is installed and in path
		Process rtmpSendingProcess = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.flv  -f flv rtmp://localhost/vod/" + streamName);

		try {
			Thread.sleep(10000);

			//TODO: check that when live stream is requested with rtsp, server should not be shutdown

			//stop rtmp streaming 
			rtmpSendingProcess.destroy();

			Thread.sleep(5000);

			boolean testResult = testFile("rtmp://localhost/vod/" + streamName + ".mp4");
			assertTrue(testResult);

			//check that stream is not created by hls muxer
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".m3u8");
			assertFalse(testResult);

			//check that mp4 is not created
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".mp4");
			assertTrue(testResult);

			//TODO: check that when stream is requested with rtsp, server should not be shutdown

			assertFalse(testFile("rtsp://127.0.0.1:5554/vod/" + streamName));
			//assert false because rtp does not support flv1 

		}
		catch (Exception e) {
			fail(e.getMessage());
			e.printStackTrace();
		}

	}


	//TODO: check if rtsp failed in some state, how it can be free resources

	@Test
	public void testRTSPSending(){
		try {

			//send rtmp stream with ffmpeg to red5
			String streamName = "live_rtsp_test";

			//make sure that ffmpeg is installed and in path
			Process rtspSendingProcess = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.mp4 -acodec copy -vcodec copy -f rtsp rtsp://127.0.0.1:5554/vod/" + streamName);

			Thread.sleep(10000);

			//check that stream can be watchable by rtsp
			//use ipv4 address to play rtsp stream
			assertTrue(testFile("rtsp://127.0.0.1:5554/vod/" + streamName));


			//check that stream can be watchable by hls
			assertTrue(testFile("http://localhost:5080/vod/streams/" + streamName + ".m3u8"));
			//
			//			//stop rtsp streaming 
			rtspSendingProcess.destroy();



			Thread.sleep(15000);

			assertTrue(testFile("rtmp://localhost/vod/" + streamName ));


			assertTrue(testFile("rtsp://127.0.0.1:5554/vod/" + streamName + ".mp4", true));

			//check that mp4 is created successfully and can be playable
			assertTrue(testFile("http://localhost:5080/vod/streams/" + streamName + ".mp4"));

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	/**
	 * @Bug
	 */
	@Test
	public void testSetFLVRecordingFalseAndMP4RecordingTrue() {
		try {

			//change settings from auto record true to false
			File  confFile = new File(FULL_RED5_PATH + "/conf/red5.properties");

			String fileContent = FileUtils.readFileToString(confFile, Charset.defaultCharset());

			String searchStr = "broadcaststream.auto.record=true";
			int indexOf = fileContent.indexOf(searchStr);
			boolean restart = false;
			if (indexOf != -1) {
				fileContent.replaceAll(searchStr, "broadcaststream.auto.record=false");
				restart = true;
			}
			else {
				searchStr = "broadcaststream.auto.record= true";
				indexOf = fileContent.indexOf(searchStr);
				if (indexOf != -1) {
					fileContent.replaceAll(searchStr, "broadcaststream.auto.record=false");
					restart = true;
				}
			}

			if (restart) {
				FileUtils.writeStringToFile(confFile, fileContent, Charset.defaultCharset());

				//restart the red5
				afterClass();

				beforeClass();

				before();
			}


			//send rtmp stream with ffmpeg to red5
			String streamName = "live_rtsp_test2";

			//make sure that ffmpeg is installed and in path
			Process rtspSendingProcess = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.mp4 -acodec copy -vcodec copy -f rtsp rtsp://127.0.0.1:5554/vod/" + streamName);

			Thread.sleep(10000);

			//check that stream can be watchable by rtsp
			//use ipv4 address to play rtsp stream
			boolean testResult = testFile("rtsp://127.0.0.1:5554/vod/" + streamName);
			assertTrue(testResult);


			//check that stream can be watchable by hls
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".m3u8");
			assertTrue(testResult);

			//stop rtsp streaming 
			rtspSendingProcess.destroy();

			Thread.sleep(15000);

			testResult = testFile("rtmp://localhost/vod/" + streamName + ".mp4");
			assertTrue(testResult);

			//
			//check that mp4 is created successfully and can be playable
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".mp4");
			assertTrue(testResult);


			System.out.println("getting rtmp://localhost/vod/" + streamName);
			//check that if flv does not exists, but mp4 exists, it should play
			testResult = testFile("rtmp://localhost/vod/" + streamName);
			assertTrue(testResult);

			System.out.println("getting rtsp://localhost:5554/vod/" + streamName);
			testResult = testFile("rtsp://127.0.0.1:5554/vod/" + streamName);
			assertTrue(testResult);

		} catch (IOException e) {
			fail(e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			fail(e.getMessage());
			e.printStackTrace();
		}
	}



	@Test
	public void testRTMPSending() {

		try {
			//send rtmp stream with ffmpeg to red5
			String streamName = "live_test";

			//make sure that ffmpeg is installed and in path
			Process rtmpSendingProcess = execute(FULL_FFMPEG_BIN_PATH + "/ffmpeg -re -i src/test/resources/test.flv -acodec copy -vcodec copy -f flv rtmp://localhost/vod/" + streamName);

			Thread.sleep(10000);

			//check that stream can be watchable by rtsp
			//use ipv4 address to play rtsp stream
			boolean testResult = testFile("rtsp://127.0.0.1:5554/vod/" + streamName);
			assertTrue(testResult);


			testResult = testFile("rtmp://localhost/vod/" + streamName);
			assertTrue(testResult);	

			//check that stream can be watchable by hls
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".m3u8");
			assertTrue(testResult);


			//stop rtmp streaming 
			rtmpSendingProcess.destroy();

			Thread.sleep(5000);

			//check that mp4 is created successfully and can be playable
			testResult = testFile("http://localhost:5080/vod/streams/" + streamName + ".mp4");
			assertTrue(testResult);

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

	public static boolean  testFile(String absolutePath, int expectedDurationInMS) {
		return testFile(absolutePath, expectedDurationInMS, false);
	}
	public static boolean testFile(String absolutePath, int expectedDurationInMS, boolean fullRead) {
		int ret;

		AVFormatContext inputFormatContext = avformat.avformat_alloc_context();

		if (inputFormatContext == null) {
			System.out.println("cannot allocate input context");
			return false;
		}

		if ((ret = avformat_open_input(inputFormatContext, absolutePath, null, (AVDictionary)null)) < 0) {
			System.out.println("cannot open input context");
			return false;
		}

		ret = avformat_find_stream_info(inputFormatContext, (AVDictionary)null);
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
			if (codecContext.codec_type() ==  AVMEDIA_TYPE_VIDEO) {
				assertTrue(codecContext.width() != 0);
				assertTrue(codecContext.height() != 0);
				assertTrue(codecContext.pix_fmt() != AV_PIX_FMT_NONE);
				streamExists = true;
			}
			else if (codecContext.codec_type() ==  AVMEDIA_TYPE_AUDIO) {
				//TODO:
				assertTrue(codecContext.sample_rate() != 0);
				streamExists = true;
			}
		}
		if (!streamExists) {
			return streamExists;
		}

		int i = 0;
		while (fullRead || i < 3){
			AVPacket pkt = new AVPacket();
			ret = av_read_frame(inputFormatContext, pkt);

			if (ret < 0) {
				break;

			}
			i++;
			av_packet_unref(pkt);
		}

		if (inputFormatContext.duration() != AV_NOPTS_VALUE) 
		{
			long durationInMS = inputFormatContext.duration() / 1000;

			if (expectedDurationInMS != 0) {
				if ((durationInMS < (expectedDurationInMS - 2000)) || 
						(durationInMS > (expectedDurationInMS + 2000))) {
					System.out.println("Failed: duration of the stream: " + durationInMS);
					return false;
				}
			}
		}

		avformat_close_input(inputFormatContext);
		return true;

	}


	private Process execute(final String command) {
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


	@BeforeClass
	public static void beforeClass() {
		//start red5 server
		av_register_all();
		avformat_network_init();

		String path = "./start.sh";
		String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

		try {
			closeRed5();
			red5Process = Runtime.getRuntime().exec(path, null, new File(FULL_RED5_PATH));
			readErrorStream(red5Process); // this may required to not fill the error buffer
			readInputStream(red5Process);

			System.out.println("Waiting for letting red5 start." );
			System.out.println("You can get exception if red5 is not started fully after this while. \n So arrange this time according to your red5 startup time in your machine");


			Thread.sleep(30000);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}	
	}

	@AfterClass
	public static void afterClass() {
		System.out.println("MuxingTest.afterClass()");
		//stop red5 server
		closeRed5();
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
		//String path = "./red5-shutdown.sh" ;
		String path = "src/test/resources/kill_red5.sh";
		try {
			Process exec = Runtime.getRuntime().exec(path, null, null); //new String[] {"cd red5; "+ path});
			readErrorStream(exec);
			readInputStream(exec);
			exec.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	@Before
	public void before(){
		//runs before every test code
		try {
			delete(new File(FULL_RED5_PATH + "/webapps/vod/streams"));
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	@After
	public void after() {
		//runs after every test code 
	}



	public static void delete(File file)
			throws IOException{

		if (!file.exists()) {
			return;
		}

		if(file.isDirectory()){

			//directory is empty, then delete it
			if(file.list().length==0){

				file.delete();
				//System.out.println("Directory is deleted : " 
				//	+ file.getAbsolutePath());

			}else{

				//list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					//construct the file structure
					File fileDelete = new File(file, temp);

					//recursive delete
					delete(fileDelete);
				}

				//check the directory again, if empty then delete it
				if(file.list().length==0){
					file.delete();
					//System.out.println("Directory is deleted : " 
					//		+ file.getAbsolutePath());
				}
			}

		}else{
			//if file, then delete it
			file.delete();
			//System.out.println("File is deleted : " + file.getAbsolutePath());
		}
	}


	public byte[] getByteArray(String address){
		try {

			URL url = new URL(address);
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(45000);
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);      

			urlConnection.setFollowRedirects(true);
			urlConnection.connect();

			InputStream in = urlConnection.getInputStream(); //getAssets().open("kralfmtop10.htm");

			byte[] byteArray = org.apache.commons.io.IOUtils.toByteArray(in);

			in.close();

			return byteArray;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}








}
