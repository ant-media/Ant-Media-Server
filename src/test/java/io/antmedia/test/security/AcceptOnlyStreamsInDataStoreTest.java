package io.antmedia.test.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;


import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
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
		
		boolean publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null, null);
		assertFalse(publishAllowed);
		
		
		filter.setEnabled(false);
	
		IContext context = Mockito.mock(IContext.class);
		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		
		Mockito.when(context.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(licenseService);
		Mockito.when(scope.getContext()).thenReturn(context);		
		
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null, null);
		assertTrue(publishAllowed);
		
		
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(true);
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null, null);
		assertFalse(publishAllowed);
		
		filter.setEnabled(true);
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(false);
		publishAllowed = filter.isPublishAllowed(scope, "streamId", "mode", null, null);
		assertFalse(publishAllowed);
		
		String streamId = dataStore.save(new Broadcast());
		publishAllowed = filter.isPublishAllowed(scope, streamId, "mode", null, null);
		assertTrue(publishAllowed);
		
	}
	
	
	@Test
	public void testStreamIdInUseCase() 
	{
		AcceptOnlyStreamsInDataStore filter = spy(new AcceptOnlyStreamsInDataStore());
		
		InMemoryDataStore dataStore = new InMemoryDataStore("db");
		DataStoreFactory factory = mock(DataStoreFactory.class);
		filter.setDataStoreFactory(factory);
		Mockito.when(factory.getDataStore()).thenReturn(dataStore);

		ILicenceService licenseService = mock(ILicenceService.class);
		doReturn(licenseService).when(filter).getLicenceService(any());
		Mockito.when(licenseService.isLicenceSuspended()).thenReturn(false);

		
		
		assertEquals(dataStore, filter.getDatastore());
		assertEquals(factory, filter.getDataStoreFactory());
		
		filter.setDataStore(dataStore);
		filter.setEnabled(true);
		
		IScope scope = Mockito.mock(IScope.class);

		
		try {
			Broadcast preparingBroadcast = new Broadcast();
			preparingBroadcast.setStreamId("preparingStream");
			preparingBroadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
			preparingBroadcast.setUpdateTime(System.currentTimeMillis());
			dataStore.save(preparingBroadcast);
			
			Broadcast broadcastingBroadcast = new Broadcast();
			broadcastingBroadcast.setStreamId("broadcastingStream");
			broadcastingBroadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
			broadcastingBroadcast.setUpdateTime(System.currentTimeMillis());
			dataStore.save(broadcastingBroadcast);
			
			Broadcast stuckedBroadcast = new Broadcast();
			stuckedBroadcast.setStreamId("stuckedBroadcast");
			stuckedBroadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
			stuckedBroadcast.setUpdateTime(System.currentTimeMillis() - 2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS -1000);
			dataStore.save(stuckedBroadcast);
			
			Broadcast offlineBroadcast = new Broadcast();
			offlineBroadcast.setStreamId("offlineStream");
			offlineBroadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
			offlineBroadcast.setUpdateTime(System.currentTimeMillis());
			dataStore.save(offlineBroadcast);
			
			
			assertFalse(filter.isPublishAllowed(scope, preparingBroadcast.getStreamId(), "mode", null, null));
			assertFalse(filter.isPublishAllowed(scope, broadcastingBroadcast.getStreamId(), "mode", null, null));
			assertTrue(filter.isPublishAllowed(scope, offlineBroadcast.getStreamId(), "mode", null, null));
			assertTrue(filter.isPublishAllowed(scope, stuckedBroadcast.getStreamId(), "mode", null, null));
			
			
			filter.setEnabled(false);
			assertTrue(filter.isPublishAllowed(scope, "notExistent", "mode", null, null));

			assertFalse(filter.isPublishAllowed(scope, preparingBroadcast.getStreamId(), "mode", null, null));
			assertFalse(filter.isPublishAllowed(scope, broadcastingBroadcast.getStreamId(), "mode", null, null));
			assertTrue(filter.isPublishAllowed(scope, offlineBroadcast.getStreamId(), "mode", null, null));
			assertTrue(filter.isPublishAllowed(scope, stuckedBroadcast.getStreamId(), "mode", null, null));


		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
