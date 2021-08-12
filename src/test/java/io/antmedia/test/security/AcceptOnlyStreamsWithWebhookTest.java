package io.antmedia.test.security;

import io.antmedia.AppSettings;
import io.antmedia.RecordType;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.licence.ILicenceService;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.security.AcceptOnlyStreamsWithWebhook;
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
		filter.setDataStoreFactory(factory);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);
		assertEquals(dataStore, filter.getDatastore());
		assertEquals(factory, filter.getDataStoreFactory());

		filter.setDataStore(dataStore);
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
	public void testIsPublishAllowedWithWebhook()
	{
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
		filter.setDataStoreFactory(factory);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);
		assertEquals(dataStore, filter.getDatastore());
		assertEquals(factory, filter.getDataStoreFactory());

		filter.setDataStore(dataStore);
		filter.setEnabled(true);
		boolean result = false;

		doReturn(result).when(mfilter).isPublishAllowed(any(),any(),any(),any());

		filter.getAppSettings();
		assertTrue(filter.isEnabled());

		Map<String, String> queryParams = new HashMap<>();

		queryParams.put("q1","p1");

		filter.isPublishAllowed(scope, "any()", "any()", null);

	}

}
