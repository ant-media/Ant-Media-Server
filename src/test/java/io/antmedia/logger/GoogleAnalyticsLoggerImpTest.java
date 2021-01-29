package io.antmedia.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.brsanthu.googleanalytics.GoogleAnalytics;

import ch.qos.logback.classic.spi.IThrowableProxy;

public class GoogleAnalyticsLoggerImpTest {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAnalyticsLoggerImpTest.class);

    private GoogleAnalyticsLoggerImp googleAnalyticsLogger;

    private IThrowableProxy throwableProxy;

    @Before
    public void before(){
        googleAnalyticsLogger = Mockito.mock(GoogleAnalyticsLoggerImp.class);
        throwableProxy = Mockito.mock(IThrowableProxy.class);
    }

    @After
    public void after(){
    }

    private void deleteFile(String testPath) {
        File file = new File(testPath);
        if (file.exists()) {
            boolean deleteResult = file.delete();
            if(!deleteResult){
                logger.info("GoogleAnalyticsLoggerImpTest file deletion failed");
            }
        }
    }


    @Test
    public void logTest(){
        googleAnalyticsLogger.log(throwableProxy);
        Assert.assertNotNull(throwableProxy);
    }

    @Test
    public void getGoogleAnalyticsTest(){
        Mockito.when(googleAnalyticsLogger.getGoogleAnalytic()).thenReturn(Mockito.mock(GoogleAnalytics.class));
        GoogleAnalytics googleAnalytic = googleAnalyticsLogger.getGoogleAnalytic();
        Assert.assertNotNull(googleAnalytic);
    }

    @Test
    public void constructorFileNotExistTest(){
        GoogleAnalyticsLoggerImp googleAnalyticsLogger = new GoogleAnalyticsLoggerImp(Launcher.getInstanceId());
        Assert.assertNotNull(googleAnalyticsLogger.instanceId);
    }

}
