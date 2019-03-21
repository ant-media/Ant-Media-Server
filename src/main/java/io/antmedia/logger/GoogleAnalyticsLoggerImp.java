package io.antmedia.logger;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.google.common.annotations.VisibleForTesting;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.BroadcastRestService;
import org.red5.server.Launcher;

import java.io.File;
import java.util.UUID;

class GoogleAnalyticsLoggerImp implements GoogleAnalyticsLogger {

    private final String implementationVersion = AntMediaApplicationAdapter.class.getPackage().getImplementationVersion();
    private final String type = BroadcastRestService.isEnterprise() ? "Enterprise" : "Community";

    @VisibleForTesting
    String instanceId;

    public GoogleAnalyticsLoggerImp(String path) {
        File idFile = new File(path);
        instanceId = null;
        if (idFile.exists()) {
            instanceId = LoggerUtils.getFileContent(idFile.getAbsolutePath());
        } else {
            instanceId = UUID.randomUUID().toString();
            LoggerUtils.writeToFile(idFile.getAbsolutePath(), instanceId);
        }
    }

    @Override
    public void log(IThrowableProxy throwableProxy) {
        String throwableStr = ThrowableProxyUtil.asString(throwableProxy);
        GoogleAnalytics googleAnalytic = getGoogleAnalytic();
        googleAnalytic.exception().
                exceptionDescription(throwableStr).
                clientId(instanceId).
                sendAsync();
    }

    @VisibleForTesting
    GoogleAnalytics getGoogleAnalytic() {
        return GoogleAnalytics.builder()
                .withAppVersion(implementationVersion)
                .withAppName(type)
                .withTrackingId(Launcher.GA_TRACKING_ID).build();
    }
}
