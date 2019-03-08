package io.antmedia.logger;

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
    String instanceId;

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

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        if (LoggerEnvironment.isManagingThread()) {
            return;
        }

        LoggerEnvironment.startManagingThread();
        try {

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

    GoogleAnalytics getGoogleAnalytic(String implementationVersion, String type) {
        return GoogleAnalytics.builder()
                .withAppVersion(implementationVersion)
                .withAppName(type)
                .withTrackingId("UA-93263926-3").build();

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