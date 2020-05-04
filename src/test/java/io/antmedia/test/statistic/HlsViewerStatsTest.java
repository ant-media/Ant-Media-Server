package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.scheduling.QuartzSchedulingService;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import io.vertx.core.Vertx;


public class HlsViewerStatsTest {

	@Test
	public void testHLSViewerCount() {
		HlsViewerStats viewerStats = new HlsViewerStats();

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
			viewerStats.registerNewViewer(streamId, sessionId);
		}

		int viewerCount = viewerStats.getViewerCount(streamId);
		assertEquals(100, viewerCount);
		
		assertEquals(0, viewerStats.getViewerCount("no_streamid"));
		
		//Add same session ID
		for (int i = 0; i < 10; i++) {
			String sessionId = "sameSessionID";
			viewerStats.registerNewViewer(streamId, sessionId);
		}
		
		
		viewerCount = viewerStats.getViewerCount(streamId);
		assertEquals(101, viewerCount);

	}

	
	@Test
	public void testGetTimeout() {
		AppSettings settings = mock(AppSettings.class);
		when(settings.getHlsTime()).thenReturn("");
		
		int defaultValue = HlsViewerStats.DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT;
		assertEquals(defaultValue, HlsViewerStats.getTimeoutMSFromSettings(settings, defaultValue));
		
		when(settings.getHlsTime()).thenReturn("2");
		
		assertEquals(20000, HlsViewerStats.getTimeoutMSFromSettings(settings, defaultValue));
		
	}

	@Test
	public void testSetApplicationContext() {
		ApplicationContext context = mock(ApplicationContext.class);
		
		Vertx vertx = io.vertx.core.Vertx.vertx();		
		
		try {

			DataStoreFactory dsf = new DataStoreFactory();
			dsf.setDbType("memorydb");
			dsf.setDbName("datastore");
			when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);
			
			when(context.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
			
			AppSettings settings = mock(AppSettings.class);
			
			//set hls time to 1
			when(settings.getHlsTime()).thenReturn("4");
			
			when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(settings);
			when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());
			
			HlsViewerStats viewerStats = new HlsViewerStats();
			
			viewerStats.vertx = vertx;
			
			viewerStats.setTimePeriodMS(1000);
			
			viewerStats.setApplicationContext(context);
			
			Broadcast broadcast = new Broadcast();
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setName("name");
			
			dsf.setWriteStatsToDatastore(true);
			dsf.setApplicationContext(context);
			String streamId = dsf.getDataStore().save(broadcast);
			
			String sessionId = "sessionId" + (int)(Math.random() * 10000);
			viewerStats.registerNewViewer(streamId, sessionId);
			viewerStats.registerNewViewer(streamId, sessionId);
			
			/*Awaitility.await().atMost(20, TimeUnit.SECONDS).until(
					()-> dsf.getDataStore().get(streamId).getHlsViewerCount() == 1);
			*/
			/*
			Awaitility.await().atLeast(5, TimeUnit.SECONDS).atMost(12, TimeUnit.SECONDS).until(() -> {
				return dsf.getDataStore().get(streamId).getHlsViewerCount() == 1;
			});
			*/
			/*
			Awaitility.await().atLeast(5, TimeUnit.SECONDS).atMost(20, TimeUnit.SECONDS).until(() -> {
				return dsf.getDataStore().get(streamId).getHlsViewerCount() == 1;
			});
			*/
			/*
			Awaitility.await().atMost(30, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS)
			.until(() -> dsf.getDataStore().get(streamId).getHlsViewerCount() == 1);
			*/
			/*
			Awaitility.await().atLeast(7, TimeUnit.SECONDS).atMost(9, TimeUnit.SECONDS).until(() -> {
				return dsf.getDataStore().get(streamId).getHlsViewerCount() == 1;
			});
			
			assertEquals(1, dsf.getDataStore().get(streamId).getHlsViewerCount());
			
			//we set hls time to 1 above, user should be dropped after 1*10 seconds. 
			Awaitility.await().atLeast(9, TimeUnit.SECONDS).atMost(12, TimeUnit.SECONDS).until(() -> {
				return dsf.getDataStore().get(streamId).getHlsViewerCount() == 0;
			});
			*/
			
			assertEquals(1000, viewerStats.getTimePeriodMS());
			assertEquals(10000, viewerStats.getTimeoutMS());
			
			assertEquals(1, viewerStats.getViewerCount(streamId));
			
			/*
			//we set hls time to 1 above, user should be dropped after 1*10 seconds. 
			Awaitility.await().atLeast(9, TimeUnit.SECONDS).atMost(12, TimeUnit.SECONDS).until(() -> {
				return viewerStats.getViewerCount(streamId) == 0;
			});
			*/

			/*
			//we set hls time to 1 above, user should be dropped after 1*10 seconds. 
			Awaitility.await().atLeast(9, TimeUnit.SECONDS).atMost(12, TimeUnit.SECONDS).until(() -> {
				return viewerStats.getViewerCount(streamId) == 0;
			});
			
			*/
		//	assertEquals(0, dsf.getDataStore().get(streamId).getHlsViewerCount());
			
		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
	}

}
