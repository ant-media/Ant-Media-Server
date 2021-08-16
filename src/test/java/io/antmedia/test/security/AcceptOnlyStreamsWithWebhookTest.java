package io.antmedia.test.security;

import com.google.gson.JsonObject;
import io.antmedia.AppSettings;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.IPFilterDashboard;
import io.antmedia.licence.ILicenceService;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.security.AcceptOnlyStreamsWithWebhook;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.doReturn;

public class AcceptOnlyStreamsWithWebhookTest {
	protected static Logger logger = LoggerFactory.getLogger(AcceptOnlyStreamsWithWebhookTest.class);
	
	@Test
	public void testAcceptOnlyStreamsWithWebhook()
	{
		AcceptOnlyStreamsWithWebhook filter = new AcceptOnlyStreamsWithWebhook();
		AcceptOnlyStreamsWithWebhook mfilter = Mockito.mock(AcceptOnlyStreamsWithWebhook.class);

		InMemoryDataStore dataStore = new InMemoryDataStore("db");
		DataStoreFactory factory = Mockito.mock(DataStoreFactory.class);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);

		filter.setEnabled(true);
		boolean result = false;

		doReturn(result).when(mfilter).isPublishAllowed(any(),any(),any(),any());

		IScope scope = Mockito.mock(IScope.class);

		AppSettings appSettings = mock(AppSettings.class);

		appSettings.setWebhookAuthenticateURL("sampleurl");
		boolean publishAllowed = mfilter.isPublishAllowed(scope, "streamId", "mode", null);
		assertFalse(publishAllowed);
		

		filter.setEnabled(false);
	
		IContext context = Mockito.mock(IContext.class);
		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		
		Mockito.when(context.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(licenseService);
		Mockito.when(scope.getContext()).thenReturn(context);		
		
		publishAllowed = mfilter.isPublishAllowed(scope, "streamId", "mode", null);
		assertFalse(publishAllowed);
		
		
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(true);
		publishAllowed = mfilter.isPublishAllowed(scope, "streamId", "mode", null);
		assertFalse(publishAllowed);
		
		filter.setEnabled(true);
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(false);
		publishAllowed = mfilter.isPublishAllowed(scope, "streamId", "mode", null);
		assertFalse(publishAllowed);

	}

	@Test
	public void testIsPublishAllowedWithWebhook() throws IOException {
		AcceptOnlyStreamsWithWebhook filter = new AcceptOnlyStreamsWithWebhook();
		AcceptOnlyStreamsWithWebhook mfilter = Mockito.mock(AcceptOnlyStreamsWithWebhook.class);
		IScope scope = Mockito.mock(IScope.class);
		AppSettings appSettings = new AppSettings();
		appSettings.setWebhookAuthenticateURL("asd");
		filter.setAppSettings(appSettings);

		assertFalse(filter.isPublishAllowed(scope, "any()", "any()", null));

		appSettings.setWebhookAuthenticateURL("");
		filter.setAppSettings(appSettings);
		IContext context = Mockito.mock(IContext.class);
		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		Mockito.when(context.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(licenseService);
		Mockito.when(scope.getContext()).thenReturn(context);
		IConnection connectionlocal = Mockito.mock(IConnection.class);

		boolean publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null);
		assertTrue(publishAllowed);

		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null);
		assertTrue(publishAllowed);

		filter.isPublishAllowed(scope, "any()", "any()", null);

		appSettings.setWebhookAuthenticateURL(null);
		filter.setAppSettings(appSettings);

		filter.isPublishAllowed(scope, "any()", "any()", null);

		InMemoryDataStore dataStore = new InMemoryDataStore("db");
		DataStoreFactory factory = Mockito.mock(DataStoreFactory.class);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);

		filter.setEnabled(true);
		boolean result = false;

		doReturn(result).when(mfilter).isPublishAllowed(any(),any(),any(),any());

		filter.getAppSettings();
		assertTrue(filter.isEnabled());

		Map<String, String> queryParams = new HashMap<>();

		queryParams.put("q1","p1");

		filter.isPublishAllowed(scope, "any()", "any()", null);

		filter.isPublishAllowed(scope, "any()", "any()", null);


		filter.setEnabled(true);
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(false);
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", queryParams);
		assertTrue(publishAllowed);



		HttpResponse httpResponse = Mockito.mock(HttpResponse.class);

		mfilter.setEnabled(true);


		//Mockito.when(httpResponse.getStatusLine().getStatusCode()).thenReturn(404);
		//Mockito.when(filter.readHttpResponse(httpResponse)).thenReturn(404);
		Mockito.doReturn(404).when(mfilter).readHttpResponse(httpResponse);

		publishAllowed = mfilter.isPublishAllowed(scope, "streamId", "mode", queryParams);
		assertFalse(publishAllowed);

		Mockito.doReturn(200).when(mfilter).readHttpResponse(httpResponse);
		appSettings.setWebhookAuthenticateURL("asd");
		mfilter.setAppSettings(appSettings);

		mfilter.setEnabled(true);
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(true);
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", queryParams);
		assertFalse(publishAllowed);



		AcceptOnlyStreamsWithWebhook acceptOnlyStreamsWithWebhook = Mockito.spy(new AcceptOnlyStreamsWithWebhook());

		acceptOnlyStreamsWithWebhook.setAppSettings(appSettings);

		assertFalse(acceptOnlyStreamsWithWebhook.isPublishAllowed(scope, "any()", "any()", null));

		appSettings.setWebhookAuthenticateURL("");
		acceptOnlyStreamsWithWebhook.setAppSettings(appSettings);

		Mockito.doReturn(200).when(acceptOnlyStreamsWithWebhook).readHttpResponse(httpResponse);

		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(true);
		publishAllowed = acceptOnlyStreamsWithWebhook.isPublishAllowed(scope, "streamId", "mode", queryParams);
		assertTrue(publishAllowed);
		RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(2 * 1000).setSocketTimeout(5*1000).build();

		CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
		JsonObject instance = new JsonObject();
		HttpRequestBase post = (HttpRequestBase) RequestBuilder.post().setUri("webhookAuthURL")
				.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")
				.setEntity(new StringEntity(instance.toString())).build();
		post.setConfig(requestConfig);


		CloseableHttpResponse httpResponsse = Mockito.mock(CloseableHttpResponse.class);
		Mockito.when(client.execute(post)).thenReturn(httpResponsse);
		StatusLine statusLine = Mockito.mock(StatusLine.class);


		Mockito.when(httpResponsse.getStatusLine()).thenReturn(statusLine);
		Mockito.when(httpResponsse.getStatusLine().getStatusCode()).thenReturn(404);
		Mockito.when(acceptOnlyStreamsWithWebhook.readHttpResponse(httpResponsse)).thenReturn(404);

		publishAllowed = acceptOnlyStreamsWithWebhook.isPublishAllowed(scope, "streamId", "mode", queryParams);
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(false);
		assertTrue(publishAllowed);


		Mockito.when(httpResponsse.getStatusLine()).thenReturn(statusLine);
		Mockito.when(httpResponsse.getStatusLine().getStatusCode()).thenReturn(200);
		Mockito.when(acceptOnlyStreamsWithWebhook.readHttpResponse(httpResponsse)).thenReturn(200);
		publishAllowed = acceptOnlyStreamsWithWebhook.isPublishAllowed(scope, "streamId", "mode", queryParams);
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(false);
		assertTrue(publishAllowed);

	}

}
