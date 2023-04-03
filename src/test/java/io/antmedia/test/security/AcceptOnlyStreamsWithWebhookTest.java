package io.antmedia.test.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.security.AcceptOnlyStreamsWithWebhook;

public class AcceptOnlyStreamsWithWebhookTest {
	protected static Logger logger = LoggerFactory.getLogger(AcceptOnlyStreamsWithWebhookTest.class);
	
	@Test
	public void testAcceptOnlyStreamsWithWebhook()
	{
		AcceptOnlyStreamsWithWebhook filter = new AcceptOnlyStreamsWithWebhook();

		InMemoryDataStore dataStore = new InMemoryDataStore("db");
		DataStoreFactory factory = Mockito.mock(DataStoreFactory.class);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);
		IScope scope = Mockito.mock(IScope.class);
		IContext context = Mockito.mock(IContext.class);
		
		Mockito.when(scope.getContext()).thenReturn(context);
		Mockito.when(context.getBean(IDataStoreFactory.BEAN_NAME)).thenReturn(factory);

		AppSettings appSettings = new AppSettings();

		appSettings.setWebhookAuthenticateURL("sampleurl");
		filter.setAppSettings(appSettings);
		boolean publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null, null);
		assertFalse(publishAllowed);
				
	}

	@Test
	public void testIsPublishAllowedWithWebhook() throws IOException {
		AcceptOnlyStreamsWithWebhook filter = Mockito.spy(new AcceptOnlyStreamsWithWebhook());
		IScope scope = Mockito.mock(IScope.class);
		AppSettings appSettings = new AppSettings();
		appSettings.setWebhookAuthenticateURL("asd");

		InMemoryDataStore dataStore = new InMemoryDataStore("db");
		DataStoreFactory factory = Mockito.mock(DataStoreFactory.class);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);
		IContext context = Mockito.mock(IContext.class);
		
		Mockito.when(scope.getContext()).thenReturn(context);
		Mockito.when(context.getBean(IDataStoreFactory.BEAN_NAME)).thenReturn(factory);
		Mockito.when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
		String metaData = "test_metaData";
		assertFalse(filter.isPublishAllowed(scope, "any()", "any()", null, metaData));
		filter.setAppSettings(appSettings);

		assertFalse(filter.isPublishAllowed(scope, "any()", "any()", null, metaData));

		IConnection connectionMock = Mockito.mock(IConnection.class);
		Mockito.doNothing().when(connectionMock).close();
		Mockito.when(filter.getConnectionLocal()).thenReturn(connectionMock);


		assertFalse(filter.isPublishAllowed(scope, "any()", "any()", null, metaData));

		appSettings.setWebhookAuthenticateURL("");
		boolean publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null, null);
		assertTrue(publishAllowed);

		appSettings.setWebhookAuthenticateURL(null);
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("q1","p1");
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", queryParams, null);
		assertTrue(publishAllowed);


		CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
		Mockito.doReturn(client).when(filter).getHttpClient();
		CloseableHttpResponse httpResponse = Mockito.mock(CloseableHttpResponse.class);
		Mockito.when(client.execute(Mockito.any())).thenReturn(httpResponse);
		appSettings.setWebhookAuthenticateURL("url");
		
		StatusLine statusLine = Mockito.mock(StatusLine.class);
		Mockito.when(httpResponse.getStatusLine()).thenReturn(statusLine);
		Mockito.when(httpResponse.getStatusLine().getStatusCode()).thenReturn(404);

		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", queryParams, null);
		assertFalse(publishAllowed);

	}

}
