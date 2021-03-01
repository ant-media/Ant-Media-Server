package io.antmedia.logger;

import java.io.IOException;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.red5.server.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.AppenderBase;
import io.antmedia.statistic.StatsCollector;

/**
 * Appender for logback in charge of sending the logged events to a Firebase analytic server.
 */
public class AntmediaAppender extends AppenderBase<ILoggingEvent> {
	
	protected static final Logger logger = LoggerFactory.getLogger(AntmediaAppender.class);

    @Override
    public void append(ILoggingEvent iLoggingEvent) {
        if (LoggerEnvironment.isManagingThread()) {
            return;
        }
        LoggerEnvironment.startManagingThread();
        try {
            IThrowableProxy throwbleProxy = iLoggingEvent.getThrowableProxy();
            if (throwbleProxy != null) {
            	sendErrorToAnalytic(throwbleProxy);
            }
        } catch (Exception e) {
            addError("An exception occurred", e);
        } finally {
            LoggerEnvironment.stopManagingThread();
        }
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
    
	public void sendErrorToAnalytic(IThrowableProxy throwbleProxy) {
		
		String errorDetail = ThrowableProxyUtil.asString(throwbleProxy);
		String instanceId = Launcher.getInstanceId();
			
		JsonObject instance = new JsonObject();
		instance.addProperty(StatsCollector.INSTANCE_ID, instanceId);
		instance.addProperty("errorDetail", errorDetail);

		try (CloseableHttpClient client = getHttpClient()){
			 HttpUriRequest post = RequestBuilder.post().setUri("https://analytics.antmedia.io/send-error-detail.php").setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.setEntity(new StringEntity(instance.toString())).build();
			 
			 client.execute(post);
				
			}catch (IOException e) {
				logger.error("Couldn't connect Ant Media Server Analytics");
			} 
	}
	
	public static CloseableHttpClient getHttpClient() {
		return  HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
				.build();
	}
	
}