package io.antmedia.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.BroadcastRestService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Appender for logback in charge of sending the logged events to a Google Analytics server.
 */
public class AntmediaAppender extends AppenderBase<ILoggingEvent> {
    private String implementationVersion = AntMediaApplicationAdapter.class.getPackage().getImplementationVersion();
    private String type = BroadcastRestService.isEnterprise() ? "Enterprise" : "Community";
    private String instanceId;

    public AntmediaAppender() {
        String path = System.getProperty("red5.root");
        File idFile = new File(path + "/conf/instanceId");
        instanceId = null;
        if (idFile.exists()) {
            instanceId = getFileContent(idFile.getAbsolutePath());
        } else {
            instanceId = UUID.randomUUID().toString();
            writeToFile(idFile.getAbsolutePath(), instanceId);
        }
    }

    /**
     * If set, only events with level = minLevel and up will be recorded.
     *
     * @deprecated use logback filters.
     */
    @Deprecated
    protected Level minLevel;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        if (isNotLoggable(iLoggingEvent) || LoggerEnvironment.isManagingThread()) {
            return;
        }

        LoggerEnvironment.startManagingThread();
        try {
            if (minLevel != null && !iLoggingEvent.getLevel().isGreaterOrEqual(minLevel)) {
                return;
            }

            IThrowableProxy throwbleProxy = iLoggingEvent.getThrowableProxy();
            if (throwbleProxy != null) {
                String throwableStr = ThrowableProxyUtil.asString(throwbleProxy);
                getGoogleAnalytic(implementationVersion, type).
                        exception().
                        exceptionDescription(throwableStr).
                        clientId(instanceId).
                        send();
            }
        } catch (Exception e) {
            addError("An exception occurred", e);
        } finally {
            LoggerEnvironment.stopManagingThread();
        }
    }

    private GoogleAnalytics getGoogleAnalytic(String implementationVersion, String type) {
        return GoogleAnalytics.builder()
                .withAppVersion(implementationVersion)
                .withAppName(type)
                .withTrackingId("UA-93263926-3").build();

    }

    private boolean isNotLoggable(ILoggingEvent iLoggingEvent) {
        return minLevel != null && !iLoggingEvent.getLevel().isGreaterOrEqual(minLevel);
    }

    protected static String formatMessageParameters(Object[] parameters) {
        String str = "";
        for (Object argument : parameters) {
            str += (argument != null) ? argument.toString() : "";
        }
        return str;
    }

    protected static String formatCallerData(StackTraceElement[] parameters) {
        String str = "";
        for (Object argument : parameters) {
            str += (argument != null) ? argument.toString() : "";
        }
        return str;
    }

    /**
     * Set minimum level to log.
     *
     * @param minLevel minimum level to log.
     * @deprecated use logback filters.
     */
    @Deprecated
    public void setMinLevel(String minLevel) {
        this.minLevel = minLevel != null ? Level.toLevel(minLevel) : null;
    }

    @Override
    public void stop() {
        LoggerEnvironment.startManagingThread();
        try {
            if (!isStarted()) {
                return;
            }
            super.stop();
        } catch (Exception e) {
            addError("An exception occurred", e);
        } finally {
            LoggerEnvironment.stopManagingThread();
        }
    }

    public String getFileContent(String path) {
        try {
            byte[] data = Files.readAllBytes(new File(path).toPath());
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeToFile(String absolutePath, String content) {
        try {
            Files.write(new File(absolutePath).toPath(), content.getBytes(), StandardOpenOption.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}