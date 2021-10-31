package io.antmedia.integration;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.settings.ServerSettings;
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
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.openqa.selenium.NoAlertPresentException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FrontEndTest {

    public static final int MAC_OS_X = 0;
    public static final int LINUX = 1;
    public static final int WINDOWS = 2;

    public static final String SERVER_ADDR = ServerSettings.getLocalHostAddress();

    protected static Logger logger = LoggerFactory.getLogger(FrontEndTest.class);
    protected WebDriver driver;
    protected final String url = "http://localhost:5080/LiveApp/";

    private RestServiceV2Test restServiceTest;
    private BroadcastRestService restService = null;

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

    @BeforeClass
    public static void beforeClass(){
        WebDriverManager.chromedriver().setup();
        if (OS_TYPE == MAC_OS_X) {
            ffmpegPath = "/usr/local/bin/ffmpeg";
        }
    }

    @Before
    public void before() {
        restService = new BroadcastRestService();

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
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @After
    public void stop(){
        logger.info("Closing the driver");
        if(this.driver != null)
            this.driver.quit();
    }

    @Test
    public void testPublishPageStartStopPublish(){

        ChromeOptions chrome_options = new ChromeOptions();
        chrome_options.addArguments("--disable-extensions");
        chrome_options.addArguments("--disable-gpu");
        chrome_options.addArguments("--headless");
        chrome_options.addArguments("--use-fake-ui-for-media-stream",
                "--use-fake-device-for-media-stream",
                "--use-file-for-fake-video-capture=src/test/resources/bunny.mjpeg");
        chrome_options.addArguments("--no-sandbox");
        chrome_options.addArguments("--log-level=1");
        LoggingPreferences logPrefs = new LoggingPreferences();
        //To get console log
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        chrome_options.setCapability( "goog:loggingPrefs", logPrefs );

        this.driver = new ChromeDriver(chrome_options);
        this.driver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS);
        this.driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        this.driver.manage().timeouts().setScriptTimeout(10, TimeUnit.SECONDS);
        this.driver.get(this.url+"index.html");
        WebDriverWait wait = new WebDriverWait(driver,30);

        //Check we landed on the page
        String title = this.driver.getTitle();
        assertEquals("Ant Media Server WebRTC Publish", title);
        System.out.println(this.url + " " + this.driver + " " + title);

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='start_publish_button']")));
        assertTrue(checkAlert());

        this.driver.findElement(By.xpath("//*[@id='start_publish_button']")).click();

        assertTrue(checkAlert());

        //wait for creating  files
        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            return MuxingTest.testFile("http://" + SERVER_ADDR + ":5080/LiveApp/streams/stream1.m3u8");
        });

        //Check logs if publish started
        LogEntries entry = driver.manage().logs().get(LogType.BROWSER);
        checkLogsForKeyword("publish started", entry);

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='stop_publish_button']")));

        this.driver.findElement(By.xpath("//*[@id='stop_publish_button']")).click();

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='start_publish_button']")));

        assertTrue(checkAlert());

        entry = driver.manage().logs().get(LogType.BROWSER);
        assertTrue(checkLogsForKeyword("publish finished", entry));

    }

    @Test
    public void testPlayerPageStartStopPlay(){
        try{
            ConsoleAppRestServiceTest.resetCookieStore();
            Result result = ConsoleAppRestServiceTest.callisFirstLogin();
            if (result.isSuccess()) {
                Result createInitialUser = ConsoleAppRestServiceTest.createDefaultInitialUser();
                assertTrue(createInitialUser.isSuccess());
            }

            result = ConsoleAppRestServiceTest.authenticateDefaultUser();
            assertTrue(result.isSuccess());

            Result isEnterpriseEdition = ConsoleAppRestServiceTest.callIsEnterpriseEdition();
            if (!isEnterpriseEdition.isSuccess()) {
                //if it's not enterprise return
                return;
            }

            RestServiceV2Test restService = new RestServiceV2Test();

            Random r = new Random();
            String streamId = "streamId" + r.nextInt();

            Broadcast broadcast=restService.createBroadcast(streamId);
            assertNotNull(broadcast);

            Process rtmpSendingProcess = execute(ffmpegPath
                    + " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://localhost/LiveApp/"
                    + broadcast.getStreamId());

            ChromeOptions chrome_options = new ChromeOptions();
            chrome_options.addArguments("--disable-extensions");
            chrome_options.addArguments("--disable-gpu");
            chrome_options.addArguments("--headless");
            chrome_options.addArguments("--no-sandbox");
            chrome_options.addArguments("--log-level=1");
            LoggingPreferences logPrefs = new LoggingPreferences();
            //To get console log
            logPrefs.enable(LogType.BROWSER, Level.ALL);
            chrome_options.setCapability( "goog:loggingPrefs", logPrefs );

            this.driver = new ChromeDriver(chrome_options);
            this.driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
            this.driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
            this.driver.get(this.url+"player.html?id=" + broadcast.getStreamId());
            WebDriverWait wait = new WebDriverWait(driver,20);

            //Check we landed on the page
            String title = this.driver.getTitle();
            assertEquals("Ant Media Server WebRTC Player", title);
            System.out.println(this.url + " " + this.driver + " " + title);

            Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                return MuxingTest.testFile("http://localhost:5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8");
            });

            assertTrue(checkAlert());

            //Waiting until the element is clickable or 20 seconds as timeout.
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='start_play_button']")));

            this.driver.findElement(By.xpath("//*[@id='start_play_button']")).click();

            assertTrue(checkAlert());

            Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                return restService.callGetBroadcast(broadcast.getStreamId()).getWebRTCViewerCount() == 1 ;
            });

            LogEntries entry = driver.manage().logs().get(LogType.BROWSER);
            assertTrue(checkLogsForKeyword("play started", entry));

            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='stop_play_button']")));

            this.driver.findElement(By.xpath("//*[@id='stop_play_button']")).click();


            Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
                return restService.callGetBroadcast(broadcast.getStreamId()).getWebRTCViewerCount() == 0 ;
            });

            assertTrue(checkAlert());
            entry = driver.manage().logs().get(LogType.BROWSER);
            assertTrue(checkLogsForKeyword("play finished", entry));

            rtmpSendingProcess.destroy();

            restService.callDeleteBroadcast(broadcast.getStreamId());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testEmbeddedPlayPage(){
        RestServiceV2Test restService = new RestServiceV2Test();

        Random r = new Random();
        String streamId = "streamId" + r.nextInt();

        Broadcast broadcast=restService.createBroadcast(streamId);
        assertNotNull(broadcast);

        Process rtmpSendingProcess = execute(ffmpegPath
                + " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://localhost/LiveApp/"
                + broadcast.getStreamId());

        ChromeOptions chrome_options = new ChromeOptions();
        chrome_options.addArguments("--disable-extensions");
        chrome_options.addArguments("--disable-gpu");
        chrome_options.addArguments("--headless");
        chrome_options.addArguments("--no-sandbox");
        chrome_options.addArguments("--log-level=1");
        LoggingPreferences logPrefs = new LoggingPreferences();
        //To get console log
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        chrome_options.setCapability( "goog:loggingPrefs", logPrefs );

        Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            return MuxingTest.testFile("http://localhost:5080/LiveApp/streams/" + broadcast.getStreamId() + ".m3u8");
        });

        Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            return restService.callGetBroadcast(broadcast.getStreamId()).getHlsViewerCount() == 0 ;
        });

        this.driver = new ChromeDriver(chrome_options);
        this.driver.manage().timeouts().pageLoadTimeout(20, TimeUnit.SECONDS);
        this.driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        //Test HLS
        this.driver.get(this.url+"play.html?id="+broadcast.getStreamId()+"&playOrder=hls");

        //Check we landed on the page
        String title = this.driver.getTitle();

        System.out.println(this.url + " " + this.driver + " " + title);
        assertEquals("Ant Media Server WebRTC/HLS Player", title);


        assertTrue(checkAlert());

        Awaitility.await().atMost(20, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            return restService.callGetBroadcast(broadcast.getStreamId()).getHlsViewerCount() > 0 ;
        });

        rtmpSendingProcess.destroy();

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