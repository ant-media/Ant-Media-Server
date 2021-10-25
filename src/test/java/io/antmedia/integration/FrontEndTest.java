package io.antmedia.integration;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LoggingPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
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

        protected static Logger logger = LoggerFactory.getLogger(FrontEndTest.class);
        protected WebDriver driver;
        protected final String url = "http://localhost:5080/LiveApp/";

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

        @After
        public void stop(){
            delay(3);
            logger.info("Closing the driver");
            this.driver.quit();
        }

        @Test
        public void testPublishPageStartStopPublish(){

            logger.info("Starting to initialize headless chrome");
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
            this.driver.get(this.url+"index.html");

            //Check we landed on the page
            String title = this.driver.getTitle();
            assertEquals("Ant Media Server WebRTC Publish", title);
            System.out.println(this.url + " " + this.driver + " " + title);

            delay(3);
            assertTrue(checkAlert());

            this.driver.findElement(By.xpath("//*[@id='start_publish_button']")).click();
            delay(5);
            assertTrue(checkAlert());

            //Check logs if publish started
            LogEntries entry = driver.manage().logs().get(LogType.BROWSER);
            assertTrue(checkLogsForKeyword("publish started", entry));

            this.driver.findElement(By.xpath("//*[@id='stop_publish_button']")).click();
            delay(3);
            assertTrue(checkAlert());

            entry = driver.manage().logs().get(LogType.BROWSER);
            assertTrue(checkLogsForKeyword("publish finished", entry));

        }

        @Test
        public void testPlayerPageStartStopPlay(){

            Process rtmpSendingProcess = execute(ffmpegPath
                    + " -re -i src/test/resources/test.flv  -codec copy -f flv rtmp://127.0.0.1/LiveApp/"
                    + "stream1");

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
            this.driver.get(this.url+"player.html");

            //Check we landed on the page
            String title = this.driver.getTitle();
            assertEquals("Ant Media Server WebRTC Player", title);
            System.out.println(this.url + " " + this.driver + " " + title);

            delay(3);
            assertTrue(checkAlert());

            this.driver.findElement(By.xpath("//*[@id='start_play_button']")).click();

            delay(3);
            assertTrue(checkAlert());
            LogEntries entry = driver.manage().logs().get(LogType.BROWSER);
            assertTrue(checkLogsForKeyword("play started", entry));

            delay(3);
            assertTrue(checkAlert());
            this.driver.findElement(By.xpath("//*[@id='stop_play_button']")).click();

            delay(3);
            assertTrue(checkAlert());
            entry = driver.manage().logs().get(LogType.BROWSER);
            assertTrue(checkLogsForKeyword("play finished", entry));

            delay(3);

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

        public void delay(int seconds){
            try {
                TimeUnit.SECONDS.sleep(seconds);
            }catch(Exception e) {
                logger.warn("Delay is interrupted, destroying process");
                stop();
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


        /*public void run(){
            this.driver.findElement(By.xpath("//*[@id='join_publish_button']")).click();
            if(checkAlert() == false){
                stop();
            }
            delay(3);
            this.driver.findElement(By.xpath("//*[@id='start_publish_button']")).click();
            if(checkAlert() == false){
                stop();
            }
        }*/
}
