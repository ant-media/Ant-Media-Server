package io.antmedia.logger;

import ch.qos.logback.classic.spi.IThrowableProxy;

interface GoogleAnalyticsLogger {
    void log(IThrowableProxy throwableProxy);
}
