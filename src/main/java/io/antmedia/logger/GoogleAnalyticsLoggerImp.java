package io.antmedia.logger;

import java.io.File;
import java.util.UUID;

import org.red5.server.Launcher;

import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.google.common.annotations.VisibleForTesting;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import io.antmedia.statistic.StatsCollector;

class GoogleAnalyticsLoggerImp implements GoogleAnalyticsLogger {

    @VisibleForTesting
    String instanceId;
    
    GoogleAnalytics googleAnalytics;

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
    GoogleAnalytics getGoogleAnalytic() 
    {
    	if (googleAnalytics == null) {
          googleAnalytics =  StatsCollector.getGoogleAnalyticInstance(Launcher.getVersion(), Launcher.getVersionType());
    	}
    		
    	return googleAnalytics;
    }
}
