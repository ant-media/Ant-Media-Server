package io.antmedia.logger;

import ch.qos.logback.classic.spi.IThrowableProxy;

public interface GoogleAnalyticsLogger {
    void log(IThrowableProxy throwableProxy);
}
