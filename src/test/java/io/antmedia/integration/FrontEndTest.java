package io.antmedia.integration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.SubscriberStats;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
import io.antmedia.test.StreamFetcherUnitTest;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.tika.utils.ExceptionUtils;
import org.awaitility.Awaitility;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.StaleElementReferenceException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.junit.Assert.*;

public class FrontEndTest {

	public static final int MAC_OS_X = 0;
	public static final int LINUX = 1;
	public static final int WINDOWS = 2;

	public static final String SERVER_ADDR = ServerSettings.getLocalHostAddress();

	protected static Logger logger = LoggerFactory.getLogger(FrontEndTest.class);
	protected WebDriver driver;
	protected final String url = "http://localhost:5080/LiveApp/";

	private RestServiceV2Test restServiceTest;

	private static int OS_TYPE;
	public static Process process;
	private static Process tmpExec;

	public static String ffmpegPath = "ffmpeg";
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
	public static void beforeClass(){
		WebDriverManager.chromedriver().setup();
		if (OS_TYPE == MAC_OS_X) {
			ffmpegPath = "/usr/local/bin/ffmpeg";
		}
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
		restServiceTest = new RestServiceV2Test();

		try {
			//we use this delete operation because sometimes there are too many vod files and
			//vod service returns 50 for max and this make some tests fail

			int currentVodNumber = restServiceTest.callTotalVoDNumber();
			logger.info("current vod number before test {}", String.valueOf(currentVodNumber));
			if (currentVodNumber > 10) {


				//delete vods
				List<VoD> voDList = restServiceTest.callGetVoDList();
				if (voDList != null) {
					for (VoD voD : voDList) {
						RestServiceV2Test.deleteVoD(voD.getVodId());
					}
				}

				currentVodNumber = restServiceTest.callTotalVoDNumber();
				logger.info("vod number after deletion {}", String.valueOf(currentVodNumber));
			}

			ConsoleAppRestServiceTest.resetCookieStore();

			Result result = ConsoleAppRestServiceTest.callisFirstLogin();

			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());



		}
		catch (Exception e) {
			e.printStackTrace();
			fail(ExceptionUtils.getStackTrace(e));
		}
	}

	@After
	public void after(){
		logger.info("Closing the driver");
		if(this.driver != null)
			this.driver.quit();
	}

	public static ChromeOptions getChromeOptions() {
		ChromeOptions chrome_options = new ChromeOptions();
		chrome_options.addArguments("--disable-extensions");
		chrome_options.addArguments("--disable-gpu");
		chrome_options.addArguments("--headless=new");
		chrome_options.addArguments("--use-fake-ui-for-media-stream",
				"--use-fake-device-for-media-stream");
		chrome_options.addArguments("--no-sandbox");
		chrome_options.addArguments("--log-level=1");
		chrome_options.addArguments("--remote-allow-origins=*");
		LoggingPreferences logPrefs = new LoggingPreferences();
		//To get console log
		logPrefs.enable(LogType.BROWSER, Level.ALL);
		chrome_options.setCapability( "goog:loggingPrefs", logPrefs );

		return chrome_options;
	}

	//This was a bug for community edition
	@Test
	public void testAudioOnlyPublish() {
		Result result;
		List<EncoderSettings> encoderSettings = null;
		try {

			Random r = new Random();
			String streamId = "streamId" + r.nextInt();

			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			encoderSettings = appSettingsModel.getEncoderSettings();

			appSettingsModel.setHlsMuxingEnabled(true);
			appSettingsModel.setEncoderSettings(null);

			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

			this.driver = new ChromeDriver(getChromeOptions());
			this.driver.manage().timeouts().pageLoadTimeout( Duration.ofSeconds(10));
			this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			this.driver.get(this.url+"audio_publish.html?id=stream1");
			WebDriverWait wait = new WebDriverWait(driver,Duration.ofSeconds(15));

			this.driver.switchTo().frame(0);
			String publishButtonText = "//*[@id='start_publish_button']";
			wait.until(ExpectedConditions.elementToBeClickable(By.xpath(publishButtonText)));

			this.driver.findElement(By.xpath(publishButtonText)).click();

			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"broadcastingInfo\"]")));

			//wait for creating  files
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/stream1.m3u8");
			});

			assertTrue(MuxingTest.audioExists);
			assertFalse(MuxingTest.videoExists);

			String stopPublishButtonText = "//*[@id='stop_publish_button']";
			wait.until(ExpectedConditions.elementToBeClickable(By.xpath(stopPublishButtonText)));

			this.driver.findElement(By.xpath(stopPublishButtonText)).click();

			wait.until(ExpectedConditions.elementToBeClickable(By.xpath(publishButtonText)));


			//restore settings
			appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			appSettingsModel.setEncoderSettings(encoderSettings);

			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

		}
		catch (Exception e){
			logger.error(ExceptionUtils.getStackTrace(e));
			fail(e.getMessage());

		}

	}
	
	@Test
	public void testAutoStartStop() {
		
		RestServiceV2Test restService = new RestServiceV2Test();
		StreamFetcherUnitTest.startCameraEmulator();

		Broadcast broadcast = new Broadcast("rtsp_source", null, null, null, "rtsp://127.0.0.1:6554/test.flv",
				AntMediaApplicationAdapter.STREAM_SOURCE);
		broadcast.setAutoStartStopEnabled(true);
		
		Broadcast streamSource = restService.createBroadcast(broadcast);

		assertNotNull(streamSource);
		assertEquals(broadcast.getStreamUrl(), streamSource.getStreamUrl());
		
		Awaitility.await().pollDelay(10, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			Broadcast localBroadcast = restService.getBroadcast(streamSource.getStreamId());
			
			return localBroadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
		});
		
		//make a request to play the stream
		
		this.driver = new ChromeDriver(getChromeOptions());
		driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
		driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		
		String url = "http://localhost:5080/LiveApp/";

		//get with default code & it should fallback to hls and play
		driver.get(url+"play.html?id="+streamSource.getStreamId() + "&playOrder=hls");
		
		
		//check that it's started to play

		Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {

			try {
				String readyState = driver.findElement(By.tagName("video")).getDomProperty("readyState");
				//this.driver.findElement(By.xpath("//*[@id='video-player']")).
				logger.info("player ready state -> {}", readyState);
	
				return readyState != null && readyState.equals("4");
			}
			catch (StaleElementReferenceException e) {
				logger.warn("Stale element exception");
				return false;
			}
		});
		
		
		this.driver.quit();
		this.driver = null;
		
		//check that it's stopped because it's hls it takes more time to understand there is no viewer
		Awaitility.await().atMost(45, TimeUnit.SECONDS).pollInterval(3, TimeUnit.SECONDS).until(() -> {
			Broadcast localBroadcast = restService.getBroadcast(streamSource.getStreamId());
			
			return localBroadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		});
		
		
		StreamFetcherUnitTest.stopCameraEmulator();
		
	}

	@Test
	public void testPublishPageStartStopPublish(){

		Result result;
		List<EncoderSettings> encoderSettings = null;
		try {
			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			encoderSettings = appSettingsModel.getEncoderSettings();

			appSettingsModel.setHlsMuxingEnabled(true);
			appSettingsModel.setEncoderSettings(null);

			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

		}
		catch (Exception e){
			fail();
			System.out.println(ExceptionUtils.getStackTrace(e));
		}

		this.driver = new ChromeDriver(getChromeOptions());
		this.driver.manage().timeouts().pageLoadTimeout( Duration.ofSeconds(10));
		this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
		String streamId = "stream1" + RandomStringUtils.randomAlphanumeric(15);
		this.driver.get(this.url+"index.html?id=" + streamId);
		WebDriverWait wait = new WebDriverWait(driver,Duration.ofSeconds(15));

		//Check we landed on the page
		String title = this.driver.getTitle();
		assertEquals("WebRTC Samples > Publish", title);
		System.out.println(this.url + " " + this.driver + " " + title);
		
		this.driver.switchTo().frame(0);

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='start_publish_button']")));
		assertTrue(checkAlert());

		this.driver.findElement(By.xpath("//*[@id='start_publish_button']")).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"broadcastingInfo\"]")));

		//Check logs if publish started
		LogEntries entry = driver.manage().logs().get(LogType.BROWSER);
		checkLogsForKeyword("publish started", entry);

		//wait for creating  files
		Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
			return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/"+ streamId + ".m3u8");
		});

		assertTrue(checkAlert());
		
		//check if there is any subscriber
		List<SubscriberStats> subscriberStats = restServiceTest.getSubscriberStats(streamId);
		assertEquals(0, subscriberStats.size());

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='stop_publish_button']")));

		this.driver.findElement(By.xpath("//*[@id='stop_publish_button']")).click();

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='start_publish_button']")));

		assertTrue(checkAlert());

		entry = driver.manage().logs().get(LogType.BROWSER);
		assertTrue(checkLogsForKeyword("publish finished", entry));

		try {
			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			appSettingsModel.setEncoderSettings(encoderSettings);

			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

		}
		catch (Exception e){
			fail();
			System.out.println(ExceptionUtils.getStackTrace(e));
		}


	}

	@Test
	public void testEmbeddedPlayPage(){
		RestServiceV2Test restService = new RestServiceV2Test();

		ConsoleAppRestServiceTest.resetCookieStore();
		Result result;
		List<EncoderSettings> encoderSettings = null;
		try {
			result = ConsoleAppRestServiceTest.callisFirstLogin();

			if (result.isSuccess()) {
				Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
				assertTrue(createInitialUser.isSuccess());
			}

			result = ConsoleAppRestServiceTest.authenticateDefaultUser();
			assertTrue(result.isSuccess());
			Random r = new Random();
			String streamId = "streamId" + r.nextInt();

			AppSettings appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			encoderSettings = appSettingsModel.getEncoderSettings();

			appSettingsModel.setHlsMuxingEnabled(true);
			appSettingsModel.setEncoderSettings(null);

			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());


			Broadcast broadcast=restService.createBroadcast(streamId);
			assertNotNull(broadcast);

			Process rtmpSendingProcess = execute(ffmpegPath
					+ " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://localhost/LiveApp/"
					+ broadcast.getStreamId());

			LoggingPreferences logPrefs = new LoggingPreferences();
			//To get console log

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				Broadcast tmpBroadcast = restService.getBroadcast(broadcast.getStreamId());

				return tmpBroadcast != null && IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(tmpBroadcast.getStatus());
			});

			this.driver = new ChromeDriver(getChromeOptions());
			this.driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
			this.driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
			
			//get with default code & it should fallback to hls and play
			this.driver.get(this.url+"play.html?id="+broadcast.getStreamId());
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {

				String readyState = this.driver.findElement(By.tagName("video")).getDomProperty("readyState");
				//this.driver.findElement(By.xpath("//*[@id='video-player']")).
				logger.info("player ready state -> {}", readyState);

				return readyState != null && readyState.equals("4");
			});
			

			//Test HLS with playOrder
			this.driver.get(this.url+"play.html?id="+broadcast.getStreamId()+"&playOrder=hls");
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {

				String readyState = this.driver.findElement(By.tagName("video")).getDomProperty("readyState");
				//this.driver.findElement(By.xpath("//*[@id='video-player']")).
				logger.info("player ready state -> {}", readyState);

				return readyState != null && readyState.equals("4");
			});

			//Check we landed on the page
			String title = this.driver.getTitle();

			System.out.println(this.url + " " + this.driver + " " + title);
			assertEquals("Ant Media Server WebRTC/HLS/DASH Player", title);

			assertTrue(checkAlert());

			Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
				return restService.callGetBroadcast(broadcast.getStreamId()).getHlsViewerCount() > 0 ;
			});

			rtmpSendingProcess.destroy();


			appSettingsModel = ConsoleAppRestServiceTest.callGetAppSettings("LiveApp");

			appSettingsModel.setEncoderSettings(encoderSettings);

			result = ConsoleAppRestServiceTest.callSetAppSettings("LiveApp", appSettingsModel);
			assertTrue(result.isSuccess());

		}
		catch (Exception e){
			System.out.println(ExceptionUtils.getStackTrace(e));
			LogEntries entry = driver.manage().logs().get(LogType.BROWSER);

			//print the logs 
			List<LogEntry> logs= entry.getAll();
			// Print logs to debug 
			for(LogEntry log: logs)
			{
				logger.info(log.toString());
			}

			fail(e.getMessage());
		}

	}

	public boolean checkLogsForKeyword(String keyword, LogEntries entry){
		// Retrieving all log
		List<LogEntry> logs= entry.getAll();
		// Print one by one
		for(LogEntry e: logs)
		{
			if(e.toString().contains(keyword)){
				logger.info("Found the keyword in = " + e);
				return true;
			}
		}
		logger.error("Can't find the keyword in console logs");
		return false;
	}
	public boolean checkAlert()
	{
		try
		{
			String alert = this.driver.switchTo().alert().getText();
			System.out.println(alert);
			if(alert.equalsIgnoreCase("HighResourceUsage")){
				logger.error("High resource usage blocks testing");
				this.driver.switchTo().alert().accept();
				return false;
			}
			else if(alert.equalsIgnoreCase("no_stream_exist")){
				logger.info("No stream available check the publishing");
				this.driver.switchTo().alert().accept();
				return false;
			}
			else{
				logger.error("Unexpected pop-up alert on browser = {}" , alert);
				this.driver.switchTo().alert().dismiss();
				return false;
			}
		}
		catch (NoAlertPresentException e)
		{
			return true;
		}
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
}