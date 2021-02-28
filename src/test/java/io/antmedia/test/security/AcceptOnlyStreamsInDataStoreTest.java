package io.antmedia.test.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.licence.ILicenceService;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;

public class AcceptOnlyStreamsInDataStoreTest {
	
	
	@Test
	public void testAcceptOnlyStreamInDataStore() 
	{
		AcceptOnlyStreamsInDataStore filter = new AcceptOnlyStreamsInDataStore();
		
		InMemoryDataStore dataStore = new InMemoryDataStore("db");
		DataStoreFactory factory = Mockito.mock(DataStoreFactory.class);
		filter.setDataStoreFactory(factory);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);
		
		
		assertEquals(dataStore, filter.getDatastore());
		assertEquals(factory, filter.getDataStoreFactory());
		
		filter.setDataStore(dataStore);
		filter.setEnabled(true);
		
		
		IScope scope = Mockito.mock(IScope.class);
		
		boolean publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null);
		assertFalse(publishAllowed);
		
		
		filter.setEnabled(false);
	
		IContext context = Mockito.mock(IContext.class);
		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		
		Mockito.when(context.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(licenseService);
		Mockito.when(scope.getContext()).thenReturn(context);		
		
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null);
		assertTrue(publishAllowed);
		
		
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(true);
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null);
		assertFalse(publishAllowed);
		
		filter.setEnabled(true);
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(false);
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null);
		assertFalse(publishAllowed);
		
		String streamId = dataStore.save(new Broadcast());
		publishAllowed = filter.isPublishAllowed(scope, streamId, "mode", null);
		assertTrue(publishAllowed);
		
		
		
		
		
	}

}
