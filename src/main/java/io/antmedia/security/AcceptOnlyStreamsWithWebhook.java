package io.antmedia.security;

import com.google.gson.JsonObject;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.licence.ILicenceService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AcceptOnlyStreamsWithWebhook implements IStreamPublishSecurity  {

	@Autowired
	private DataStoreFactory dataStoreFactory;
	private DataStore dataStore;
	private AppSettings appSettings = null;

	@Value("${settings.acceptOnlyStreamsWithWebhook:true}")
	private boolean enabled = true;


	public static final String BEAN_NAME = "acceptOnlyStreamsWithWebhook";

	protected static Logger logger = LoggerFactory.getLogger(AcceptOnlyStreamsWithWebhook.class);

	@Override
	public synchronized boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams) {

		AtomicBoolean result = new AtomicBoolean(false);
		if (appSettings == null){
			appSettings = (AppSettings) scope.getContext().getBean(AppSettings.BEAN_NAME);
		}
		final String webhookAuthURL = appSettings.getWebhookAuthenticateURL();
		if (webhookAuthURL != null && !webhookAuthURL.isEmpty())
		{
			try (CloseableHttpClient client = getHttpClient())
			{
				//Broadcast broadcast = getDatastore().get(name);
				JsonObject instance = new JsonObject();

				instance.addProperty("name", name);
				instance.addProperty("mode", mode);
				instance.addProperty("queryParams", queryParams.toString());

				RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).setSocketTimeout(5*1000).build();

				HttpRequestBase post = (HttpRequestBase) RequestBuilder.post().setUri(webhookAuthURL)
						.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
						.setEntity(new StringEntity(instance.toString())).build();
				post.setConfig(requestConfig);

				HttpResponse response= client.execute(post);

				int statuscode = response.getStatusLine().getStatusCode();
				logger.info("Response from webhook is: {}", statuscode);

				result.set(statuscode==200);

			}
			catch (Exception e) {
				logger.error("Couldn't connect Webhook for Stream Authentication " , ExceptionUtils.getStackTrace(e));
			}

		}
		else
		{
			logger.info("AcceptOnlyStreamsWithWebhook is not activated. Accepting all streams {}", name);
			result.set(true);
		}


		if (!result.get()) {
			IConnection connectionLocal = Red5.getConnectionLocal();
			if (connectionLocal != null) {
				connectionLocal.close();
			}
			else {
				logger.warn("Connection object is null for {}", name);
			}

		}

		return result.get();
	}



	public boolean isEnabled() {
		return enabled;
	}


	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public AppSettings getAppSettings() {
		return appSettings;
	}

	public void setAppSettings(AppSettings appSettings){
		this.appSettings = appSettings;
	}

	public CloseableHttpClient getHttpClient() {
		return HttpClients.createDefault();
	}


	public int readHttpResponse(HttpResponse response){

		int statuscode = response.getStatusLine().getStatusCode();
		logger.info("Response from webhook is: {}", statuscode);
		return statuscode;
	}
}