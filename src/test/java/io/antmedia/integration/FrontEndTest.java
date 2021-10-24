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
        protected static Logger logger = LoggerFactory.getLogger(FrontEndTest.class);
        protected WebDriver driver;
        public boolean driverRunning = false;
        protected final String url = "http://localhost:5080/LiveApp/";

        @BeforeClass
        public static void beforeClass(){
            WebDriverManager.chromedriver().setup();
        }

        @After
        public void stop(){
            delay(3);
            logger.info("Closing the driver");
            this.driver.quit();
            this.driverRunning = false;
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
                else if(alert.equalsIgnoreCase("There is no stream available in the room")){
                    logger.info("No stream available to rebroadcast");
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
