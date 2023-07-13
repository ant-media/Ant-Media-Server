package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import ch.qos.logback.classic.Logger;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.ViewerStats;
import io.vertx.core.Vertx;

public class DashViewerStatsTest {
	
	static Vertx vertx;	
	
	@BeforeClass
	public static void beforeClass() {
		vertx = io.vertx.core.Vertx.vertx();
	}
	
	@AfterClass
	public static void afterClass() {
		vertx.close();
	}

	@Test
	public void testDASHViewerCount() {
		DashViewerStats viewerStats = new DashViewerStats();
			
		viewerStats.setVertx(vertx);
		DataStore dataStore = new InMemoryDataStore("datastore");
		viewerStats.setDataStore(dataStore);
		
		String streamId = String.valueOf((Math.random() * 999999));

		Broadcast broadcast = new Broadcast();

		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < 100; i++) {
			String sessionId = String.valueOf((Math.random() * 999999));
			viewerStats.registerNewViewer(streamId, sessionId, null);
		}

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
				()->viewerStats.getViewerCount(streamId) == 100 );
		
		int viewerCount = viewerStats.getViewerCount(streamId);
		assertEquals(100, viewerCount);

		assertEquals(0, viewerStats.getViewerCount("no_streamid"));

		//Add same session ID
		for (int i = 0; i < 10; i++) {
			String sessionId = "sameSessionID";
			viewerStats.registerNewViewer(streamId, sessionId, null);
		}

		Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
				()->viewerStats.getViewerCount(streamId) == 101 );

		viewerCount = viewerStats.getViewerCount(streamId);
		assertEquals(101, viewerCount);

	}

	@Test
	public void testSubscriberEvents() {
		DashViewerStats viewerStats = new DashViewerStats();
		
		viewerStats.setVertx(vertx);

		DataStore dataStore = new InMemoryDataStore("datastore");
		viewerStats.setDataStore(dataStore);
		
		String streamId = "stream1";
		
		viewerStats.resetViewerMap(streamId,ViewerStats.DASH_TYPE);
		
		// create a subscriber play
		Subscriber subscriberPlay = new Subscriber();
		subscriberPlay.setStreamId(streamId);
		subscriberPlay.setSubscriberId("subscriber1");
		subscriberPlay.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
		subscriberPlay.setType(Subscriber.PLAY_TYPE);
		dataStore.addSubscriber(subscriberPlay.getStreamId(), subscriberPlay);
		
		String sessionId = String.valueOf((Math.random() * 999999));
		// check if viewer is added
		viewerStats.registerNewViewer(streamId, sessionId, subscriberPlay.getSubscriberId());
		Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
				()-> {
				boolean eventExist = false;
				Subscriber subData = dataStore.getSubscriber(streamId, subscriberPlay.getSubscriberId());
				
				List<ConnectionEvent> events = subData.getStats().getConnectionEvents();
				
				if(events.size() == 1) {
					ConnectionEvent event2 = events.get(0);
					eventExist = ConnectionEvent.CONNECTED_EVENT == event2.getEventType();
				}

				return (subData.isConnected()) && subData.getCurrentConcurrentConnections() == 1  && eventExist; }
		);
	
		viewerStats.resetViewerMap(streamId, ViewerStats.DASH_TYPE);
		Map<String, String> map = viewerStats.getSessionId2subscriberId();
		assertTrue(map.isEmpty());
		
	}
	
	@Test
	public void testGetTimeout() {
		AppSettings settings = new AppSettings();
		
		settings.setDashFragmentDuration("");
		
		int defaultValue = DashViewerStats.DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT;
		assertEquals(defaultValue, DashViewerStats.getTimeoutMSFromSettings(settings, defaultValue, DashViewerStats.DASH_TYPE));
		
		settings.setDashFragmentDuration("0.5");
		
		assertEquals(10000, DashViewerStats.getTimeoutMSFromSettings(settings, defaultValue, DashViewerStats.DASH_TYPE));
		
	}
	
	@Test
	public void testSetApplicationContextSubscribers() {
		ApplicationContext context = mock(ApplicationContext.class);

		try {

			DataStoreFactory dsf = new DataStoreFactory();
			dsf.setDbType("memorydb");
			dsf.setDbName("datastore");
			when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);

			when(context.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
			
			when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);

			AppSettings settings = new AppSettings();

			//set dash fragment duration to 0.5
			settings.setDashFragmentDuration("0.5");
			
			when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(settings);
			when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());
			
			DashViewerStats viewerStats = new DashViewerStats();
			
			viewerStats.setTimePeriodMS(1000);
			viewerStats.setApplicationContext(context);
			
			Broadcast broadcast = new Broadcast();
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setName("name");
			
			dsf.setWriteStatsToDatastore(true);
			dsf.setApplicationContext(context);
			String streamId = dsf.getDataStore().save(broadcast);

			assertEquals(1000, viewerStats.getTimePeriodMS());
			assertEquals(10000, viewerStats.getTimeoutMS());

			String sessionId = "sessionId" + (int)(Math.random() * 10000);
			String sessionId2 = "sessionId" + (int)(Math.random() * 10000);
			
			// create a subscriber play
			Subscriber subscriberPlay = new Subscriber();
			subscriberPlay.setStreamId(streamId);
			subscriberPlay.setSubscriberId("subscriber1");
			subscriberPlay.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
			subscriberPlay.setType(Subscriber.PLAY_TYPE);
			subscriberPlay.setConcurrentConnectionsLimit(2);
			dsf.getDataStore().addSubscriber(subscriberPlay.getStreamId(), subscriberPlay);
			
			Subscriber subscriberPlay2 = new Subscriber();
			subscriberPlay2.setStreamId(streamId);
			subscriberPlay2.setSubscriberId("subscriber2");
			subscriberPlay2.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
			subscriberPlay2.setType(Subscriber.PLAY_TYPE);
			dsf.getDataStore().addSubscriber(subscriberPlay2.getStreamId(), subscriberPlay2);			
			
			Subscriber subscriberPlay3 = new Subscriber();
			subscriberPlay3.setStreamId(streamId);
			subscriberPlay3.setSubscriberId("subscriber3");
			subscriberPlay3.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
			subscriberPlay3.setType(Subscriber.PLAY_TYPE);
			dsf.getDataStore().addSubscriber(subscriberPlay3.getStreamId(), subscriberPlay3);				
			
			viewerStats.registerNewViewer(streamId, sessionId, subscriberPlay.getSubscriberId());
			viewerStats.registerNewViewer(streamId, sessionId2, subscriberPlay.getSubscriberId());
			
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
					()->viewerStats.getViewerCount(streamId) == 2 );

			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
					()->viewerStats.getIncreaseCounterMap(streamId) == 2 );
			
			Awaitility.await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
					()->viewerStats.getTotalViewerCount() == 2 );
			
			//Viewer timeout increase
			viewerStats.registerNewViewer(streamId, sessionId, subscriberPlay2.getSubscriberId());
			viewerStats.registerNewViewer(streamId, sessionId2, subscriberPlay2.getSubscriberId());
			
			Awaitility.await().atMost(15, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
					()-> {
					boolean eventExist = false;
					Subscriber subData = dsf.getDataStore().getSubscriber(streamId, subscriberPlay.getSubscriberId());
					
					List<ConnectionEvent> events = subData.getStats().getConnectionEvents();
					
					if(events.size() == 2) {
						ConnectionEvent event = events.get(0);
						eventExist = ConnectionEvent.CONNECTED_EVENT == event.getEventType();
					}

					return subData.isConnected() && subData.getCurrentConcurrentConnections() == 2 && eventExist; 
			});
			
			// Check viewer is online
			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().get(streamId).getDashViewerCount() == 2);
			
			// Wait some time for detect disconnect
			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().get(streamId).getDashViewerCount() == 0);
			
			assertEquals(0, viewerStats.getViewerCount(streamId));
			assertEquals(0, viewerStats.getIncreaseCounterMap(streamId));
			assertEquals(0, viewerStats.getTotalViewerCount());
			
			// a disconnection event should be added 
			Subscriber subData = dsf.getDataStore().getSubscriber(streamId, subscriberPlay2.getSubscriberId());
			
			List<ConnectionEvent> events = subData.getStats().getConnectionEvents();
			
			assertEquals(4, events.size());
			ConnectionEvent eventDis = events.get(3);
			assertSame(ConnectionEvent.DISCONNECTED_EVENT, eventDis.getEventType());
			
			// Broadcast finished test
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			dsf.getDataStore().save(broadcast);
			
			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().save(broadcast).equals(streamId));
			
			
			viewerStats.registerNewViewer(streamId, sessionId, subscriberPlay3.getSubscriberId());
			
			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(
					()-> viewerStats.getViewerCount(streamId) == 1);
			

			assertEquals(1, viewerStats.getViewerCount(streamId));
			assertEquals(1, viewerStats.getIncreaseCounterMap(streamId));
			assertEquals(1, viewerStats.getTotalViewerCount());
			
			// Wait some time for detect disconnect
			Awaitility.await().atMost(20, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().get(streamId).getDashViewerCount() == 0);
			
			// Check Viewer 
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
					()-> viewerStats.getViewerCount(streamId) == 0);
			
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
					()-> viewerStats.getIncreaseCounterMap(streamId) == 0);
			
			Awaitility.await().atMost(10, TimeUnit.SECONDS).until(
					()-> viewerStats.getTotalViewerCount() == 0);
			

			Subscriber subData2 = dsf.getDataStore().getSubscriber(streamId, subscriberPlay3.getSubscriberId());
			
			List<ConnectionEvent> events2 = subData2.getStats().getConnectionEvents();
			
			assertEquals(2, events2.size());	
			ConnectionEvent eventDis2 = events2.get(1);
			assertSame(ConnectionEvent.DISCONNECTED_EVENT, eventDis2.getEventType());		
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}	

	@Test
	public void testSetApplicationContext() {
		ApplicationContext context = mock(ApplicationContext.class);

		try {

			DataStoreFactory dsf = new DataStoreFactory();
			dsf.setDbType("memorydb");
			dsf.setDbName("datastore");
			when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);

			when(context.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
			
			when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);

			AppSettings settings = new AppSettings();

			//set dash fragment duration time to 1
			settings.setDashFragmentDuration("0.5");
			when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(settings);
			when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());
			
			DashViewerStats viewerStats = new DashViewerStats();
			
			viewerStats.setTimePeriodMS(1000);
			viewerStats.setApplicationContext(context);
			
			Broadcast broadcast = new Broadcast();
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setName("name");
			
			dsf.setWriteStatsToDatastore(true);
			dsf.setApplicationContext(context);
			String streamId = dsf.getDataStore().save(broadcast);

			assertEquals(1000, viewerStats.getTimePeriodMS());
			assertEquals(10000, viewerStats.getTimeoutMS());

			String sessionId = "sessionId" + (int)(Math.random() * 10000);

			viewerStats.registerNewViewer(streamId, sessionId, null);
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
					()->viewerStats.getViewerCount(streamId) == 1 );

			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
					()->viewerStats.getIncreaseCounterMap(streamId) == 1 );
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(
					()->viewerStats.getTotalViewerCount() == 1 );
			
			//Viewer timeout increase
			viewerStats.registerNewViewer(streamId, sessionId, null);
			
			// Check viewer is online
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().get(streamId).getDashViewerCount() == 1);
			
			// Wait some time for detect disconnect
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().get(streamId).getDashViewerCount() == 0);
			
			assertEquals(0, viewerStats.getViewerCount(streamId));
			assertEquals(0, viewerStats.getIncreaseCounterMap(streamId));
			assertEquals(0, viewerStats.getTotalViewerCount());
			
			// Broadcast finished test
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			dsf.getDataStore().save(broadcast);
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().save(broadcast).equals(streamId));
			
			
			viewerStats.registerNewViewer(streamId, sessionId, null);
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> viewerStats.getViewerCount(streamId) == 1);
			
			assertEquals(1, viewerStats.getViewerCount(streamId));
			assertEquals(1, viewerStats.getIncreaseCounterMap(streamId));
			assertEquals(1, viewerStats.getTotalViewerCount());
			
			// Wait some time for detect disconnect
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().get(streamId).getDashViewerCount() == 0);
			
			// Check Viewer 
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> viewerStats.getViewerCount(streamId) == 0);
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> viewerStats.getIncreaseCounterMap(streamId) == 0);
			
			Awaitility.await().atMost(30, TimeUnit.SECONDS).until(
					()-> viewerStats.getTotalViewerCount() == 0);
			
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
	}

}
