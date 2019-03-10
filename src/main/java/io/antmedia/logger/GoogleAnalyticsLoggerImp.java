package io.antmedia.logger;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.BroadcastRestService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

public class GoogleAnalyticsLoggerImp implements GoogleAnalyticsLogger {

    private final String implementationVersion = AntMediaApplicationAdapter.class.getPackage().getImplementationVersion();
    private final String type = BroadcastRestService.isEnterprise() ? "Enterprise" : "Community";

    private String instanceId;

    public GoogleAnalyticsLoggerImp() {
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
    public void log(IThrowableProxy throwableProxy) {
        String throwableStr = ThrowableProxyUtil.asString(throwableProxy);
        getGoogleAnalytic(implementationVersion, type).
                exception().
                exceptionDescription(throwableStr).
                clientId(instanceId).
                send();
    }

    private GoogleAnalytics getGoogleAnalytic(String implementationVersion, String type) {
        return GoogleAnalytics.builder()
                .withAppVersion(implementationVersion)
                .withAppName(type)
                .withTrackingId("UA-93263926-3").build();

    }

    private String getFileContent(String path) {
        try {
            byte[] data = Files.readAllBytes(new File(path).toPath());
            return new String(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeToFile(String absolutePath, String content) {
        try {
            File file = new File(absolutePath);
            if(file.exists()) {
                Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
