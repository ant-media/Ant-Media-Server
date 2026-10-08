package io.antmedia.test.logger;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import io.antmedia.logger.AntmediaAppender;
import io.antmedia.logger.LoggerEnvironment;

public class AntmediaAppenderTest {

    private AntmediaAppender antmediaAppender;

    @BeforeEach
    public void before(){
        LoggerEnvironment.stopManagingThread();
        antmediaAppender = Mockito.spy(new AntmediaAppender());
    }

    @Test
    public void logNotCalledWhenThrowbleProxyIsNull(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(null);
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(antmediaAppender,Mockito.never()).sendErrorToAnalytic(Mockito.any(IThrowableProxy.class));
    }

    @Test
    public void logCalledWhenThrowbleProxyIsNotNull(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(new ThrowableProxy(new NullPointerException()));
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(antmediaAppender).sendErrorToAnalytic(Mockito.any(IThrowableProxy.class));
        
        Awaitility.await().atMost(100, TimeUnit.SECONDS).until(() -> {
        	return antmediaAppender.getNumberOfCalls() > 0;
        });
    }

    @Test
    public void stopShouldStartManagingThread(){
        antmediaAppender.stop();
        Assertions.assertNotNull(antmediaAppender);
    }

    @Test
    public void constructorTest(){
        AntmediaAppender appender = new AntmediaAppender();
        Assertions.assertNotNull(appender);
    }

    @Test
    public void appenderShouldReturnWhenManagingThread(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(Mockito.mock(IThrowableProxy.class));
        LoggerEnvironment.startManagingThread();
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(antmediaAppender,Mockito.never()).sendErrorToAnalytic(Mockito.any(IThrowableProxy.class));
        
    }

    @Test
    public void exceptionAddErrorTest(){
        antmediaAppender.append(null);
        Mockito.verify(antmediaAppender).addError(Mockito.anyString(),Mockito.any(Throwable.class));
    }
}