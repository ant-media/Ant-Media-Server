package io.antmedia.test.security;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.security.ExpireStreamPublishSecurity;

public class ExpireStreamPublishSecurityTest {
	
	
	@Test
	public void testCheckStreamDate() {
		
		ExpireStreamPublishSecurity filter = Mockito.spy(new ExpireStreamPublishSecurity());
		
		DataStore dataStore = new InMemoryDataStore("db");
		
		filter.setDataStore(dataStore);
				
		Broadcast broadcast = new Broadcast();
		
		String streamId = "stream1";
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		dataStore.save(broadcast);
		
		// Scenario-1
		// Broadcasts getPlannedStartDate = null & getPlannedEndDate = null
		assertEquals(true, filter.isPublishAllowed(null, streamId, null, null, null));
		
		// Scenario-2
		// Broadcast getPlannedStartDate > now & getPlannedEndDate > now
		broadcast.setPlannedStartDate(9999999999l);
		broadcast.setPlannedEndDate(9999999999l);
		
		dataStore.save(broadcast);
		
		assertEquals(false, filter.isPublishAllowed(null, streamId, null, null, null));
		
		// Scenario-3
		// Broadcast getPlannedStartDate < now & getPlannedEndDate > now
		broadcast.setPlannedStartDate(99l);
		broadcast.setPlannedEndDate(9999999999l);
		
		dataStore.save(broadcast);
		
		assertEquals(true, filter.isPublishAllowed(null, streamId, null, null, null));
		
		// Scenario-4
		// Broadcast getPlannedStartDate > now & getPlannedEndDate < now
		broadcast.setPlannedStartDate(9999999999l);
		broadcast.setPlannedEndDate(99l);
		
		dataStore.save(broadcast);
		
		assertEquals(false, filter.isPublishAllowed(null, streamId, null, null, null));
		
		// Scenario-5
		// Broadcast getPlannedStartDate < now & getPlannedEndDate < now
		broadcast.setPlannedStartDate(99l);
		broadcast.setPlannedEndDate(99l);
		
		dataStore.save(broadcast);
		
		assertEquals(false, filter.isPublishAllowed(null, streamId, null, null, null));
		
		// Scenario-6
		// Broadcast getPlannedStartDate = null & getPlannedEndDate < now
		broadcast.setPlannedStartDate(0);
		broadcast.setPlannedEndDate(99l);
		
		dataStore.save(broadcast);
		
		assertEquals(true, filter.isPublishAllowed(null, streamId, null, null, null));
		
		// Scenario-7
		// Broadcast getPlannedStartDate > now & getPlannedEndDate = null
		broadcast.setPlannedStartDate(9999999999l);
		broadcast.setPlannedEndDate(0);
		
		dataStore.save(broadcast);
		
		assertEquals(true, filter.isPublishAllowed(null, streamId, null, null, null));
		
		broadcast.setPlannedStartDate(0);
		broadcast.setPlannedEndDate(0);
		
		dataStore.save(broadcast);
		
		assertEquals(true, filter.isPublishAllowed(null, streamId, null, null, null));
		
		assertEquals(true, filter.isPublishAllowed(null, null, null, null, null));
		
	}

}
