package io.antmedia.logger;

import java.io.File;
import java.util.UUID;

import org.red5.server.Launcher;

import com.brsanthu.googleanalytics.GoogleAnalytics;
import com.google.common.annotations.VisibleForTesting;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import io.antmedia.statistic.StatsCollector;

public class GoogleAnalyticsLoggerImp implements GoogleAnalyticsLogger {

    @VisibleForTesting
    String instanceId;
    
    GoogleAnalytics googleAnalytics;

    public GoogleAnalyticsLoggerImp(String instanceId) {
       this.instanceId = instanceId;
    }

    @Override
    public void log(IThrowableProxy throwableProxy) {
        String throwableStr = ThrowableProxyUtil.asString(throwableProxy);
        GoogleAnalytics googleAnalytic = getGoogleAnalytic();
        googleAnalytic.exception().
                exceptionDescription(throwableStr).
                clientId(getInstanceId()).
                sendAsync();
    }

    @VisibleForTesting
    public GoogleAnalytics getGoogleAnalytic() 
    {
    	if (googleAnalytics == null) {
          googleAnalytics =  StatsCollector.getGoogleAnalyticInstance(Launcher.getVersion(), Launcher.getVersionType());
    	}
    		
    	return googleAnalytics;
    }

	public String getInstanceId() {
		return instanceId;
	}
}
