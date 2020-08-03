package io.antmedia.logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

public class AntmediaAppenderTest {

    private AntmediaAppender antmediaAppender;

    private GoogleAnalyticsLogger googleAnalyticsLogger;

    @Before
    public void before(){
        LoggerEnvironment.stopManagingThread();
        googleAnalyticsLogger = Mockito.mock(GoogleAnalyticsLogger.class);
        antmediaAppender = Mockito.spy(new AntmediaAppender(googleAnalyticsLogger));
    }

    @Test
    public void logNotCalledWhenThrowbleProxyIsNull(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(null);
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(googleAnalyticsLogger,Mockito.never()).log(Mockito.any(IThrowableProxy.class));
    }

    @Test
    public void logCalledWhenThrowbleProxyIsNotNull(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(Mockito.mock(IThrowableProxy.class));
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(googleAnalyticsLogger).log(Mockito.any(IThrowableProxy.class));
    }

    @Test
    public void stopShouldStartManagingThread(){
        antmediaAppender.stop();
        Assert.assertNotNull(antmediaAppender);
    }

    @Test
    public void constructorTest(){
        AntmediaAppender appender = new AntmediaAppender();
        Assert.assertNotNull(appender);
    }

    @Test
    public void appenderShouldReturnWhenManagingThread(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(Mockito.mock(IThrowableProxy.class));
        LoggerEnvironment.startManagingThread();
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(googleAnalyticsLogger,Mockito.never()).log(Mockito.any(IThrowableProxy.class));
    }

    @Test
    public void exceptionAddErrorTest(){
        antmediaAppender.append(null);
        Mockito.verify(antmediaAppender).addError(Mockito.anyString(),Mockito.any(Throwable.class));
    }
}
