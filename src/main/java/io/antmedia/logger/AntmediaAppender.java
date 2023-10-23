package io.antmedia.logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
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

	private static ExecutorService executor = Executors.newSingleThreadExecutor();

	private int numberOfCalls = 0;

	private int numberOfException = 0;

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

		executor.submit(() -> 
		{
			try (CloseableHttpClient client = getHttpClient())
			{
				String errorDetail = ThrowableProxyUtil.asString(throwbleProxy);
				String instanceId = Launcher.getInstanceId();
				
				String version = Launcher.getVersion();
				String type = Launcher.getVersionType();
					

				JsonObject instance = new JsonObject();
				instance.addProperty(StatsCollector.INSTANCE_ID, instanceId);
				instance.addProperty(StatsCollector.INSTANCE_TYPE, type);
				instance.addProperty(StatsCollector.INSTANCE_VERSION, version);
				instance.addProperty("errorDetail", errorDetail);
				

				RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).setSocketTimeout(5*1000).build();

				HttpRequestBase post = (HttpRequestBase)RequestBuilder.post().setUri("https://log-api.eu.newrelic.com/log/v1?Api-Key=eu01xx03e8e936f6760014346295526cFFFFNRAL")
						.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.setEntity(new StringEntity(instance.toString())).build();

				post.setConfig(requestConfig);

				client.execute(post);

				numberOfCalls ++;
			} 
			catch (Exception e) {
				logger.error("Couldn't connect Ant Media Server Analytics: {} " , ExceptionUtils.getStackTrace(e));
				numberOfException ++;
			} 
		});
	}

	public static CloseableHttpClient getHttpClient() {
		return HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy()).build();
	}

	public int getNumberOfCalls() {
		return numberOfCalls;
	}

}
