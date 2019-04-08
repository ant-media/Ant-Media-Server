package io.antmedia.logger;

import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class LoggerEnvironmentTest {

    @After
    public void after(){
        LoggerEnvironment.stopManagingThread();
    }
    @Test
    public void startManagingThreadTest(){
        LoggerEnvironment.startManagingThread();
        Assert.assertTrue(LoggerEnvironment.isManagingThread());
    }

    @Test
    public void stopManagingThreadTest(){
        LoggerEnvironment.startManagingThread();
        LoggerEnvironment.stopManagingThread();
        Awaitility.await().atMost(20, TimeUnit.SECONDS)
                .until(() -> !LoggerEnvironment.isManagingThread());
        Assert.assertFalse(LoggerEnvironment.isManagingThread());
    }
}
