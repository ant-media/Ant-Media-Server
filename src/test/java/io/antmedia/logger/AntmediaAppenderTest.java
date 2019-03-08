package io.antmedia.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class AntmediaAppenderTest {

    AntmediaAppender antmediaAppender;

    @Before
    public void before(){
        antmediaAppender = Mockito.spy(new AntmediaAppender());
    }

    @Test
    public void appendNotCalledWhenThrowbleProxyIsNull(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(null);
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(antmediaAppender,Mockito.never()).getGoogleAnalytic(Mockito.anyString(),Mockito.anyString());
    }

    @Test
    public void appendCalledWhenThrowbleProxyIsNotNull(){
        ILoggingEvent iLoggingEvent = Mockito.mock(ILoggingEvent.class);
        Mockito.when(iLoggingEvent.getThrowableProxy()).thenReturn(Mockito.mock(IThrowableProxy.class));
        antmediaAppender.append(iLoggingEvent);
        Mockito.verify(antmediaAppender).getGoogleAnalytic(Mockito.anyString(),Mockito.anyString());
    }

    @Test
    public void instanceIdIsSet(){
        AntmediaAppender appender = new AntmediaAppender();
        Assert.assertNotNull(appender.instanceId);
    }
}
