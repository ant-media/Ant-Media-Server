package io.antmedia.test.logger;


import org.junit.jupiter.api.Tag;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.antmedia.logger.LoggerEnvironment;

@Tag("fast")
public class LoggerEnvironmentTest {

    @AfterEach
    public void after(){
        LoggerEnvironment.stopManagingThread();
    }
    @Test
    public void startManagingThreadTest(){
        LoggerEnvironment.startManagingThread();
        Assertions.assertTrue(LoggerEnvironment.isManagingThread());
    }

    @Test
    public void stopManagingThreadTest(){
        LoggerEnvironment.startManagingThread();
        LoggerEnvironment.stopManagingThread();
        Awaitility.await().atMost(20, TimeUnit.SECONDS)
                .until(() -> !LoggerEnvironment.isManagingThread());
        Assertions.assertFalse(LoggerEnvironment.isManagingThread());
    }
}
