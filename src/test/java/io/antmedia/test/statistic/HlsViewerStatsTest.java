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
import org.springframework.test.context.ContextConfiguration;import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;


public class HlsViewerStatsTest {


	@Test
	public void testHLSViewerCount() {
		HlsViewerStats viewerStats = new HlsViewerStats();

		DataStore dataStore = new InMemoryDataStore("datastore");
		viewerStats.setDataStore(dataStore);
		
		String streamId = String.valueOf((Math.random() * 999999));
		for (int i = 0; i < 100; i++) {
			String sessionId = String.valueOf((Math.random() * 999999));
			viewerStats.registerNewViewer(streamId, sessionId);
		}

		int viewerCount = viewerStats.getViewerCount(streamId);
		assertEquals(100, viewerCount);

		assertEquals(0, viewerStats.getViewerCount("no_streamid"));

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

		QuartzSchedulingService scheduler = new QuartzSchedulingService();
		scheduler.setConfigFile("src/main/server/conf/quartz.properties");
		try {
			scheduler.afterPropertiesSet();

			DataStoreFactory dsf = new DataStoreFactory();
			dsf.setAppName("liveapp");
			dsf.setDbType("memorydb");
			dsf.setDbName("datastore");
			when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);
			
			when(context.getBean(ISchedulingService.BEAN_NAME)).thenReturn(scheduler);
			
			when(context.containsBean(AppSettings.BEAN_NAME)).thenReturn(true);
			
			AppSettings settings = mock(AppSettings.class);
			
			//set hls time to 1
			when(settings.getHlsTime()).thenReturn("1");
			
			when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(settings);
			
			HlsViewerStats viewerStats = new HlsViewerStats();
			
			viewerStats.setTimePeriodMS(1000);
			
			viewerStats.setApplicationContext(context);
			
			Broadcast broadcast = new Broadcast();
			broadcast.setName("name");
			when(settings.isWriteStatsToDatastore()).thenReturn(true);
			dsf.setAppSettings(settings);
			String streamId = dsf.getDataStore().save(broadcast);
			
			String sessionId = "sessionId" + (int)(Math.random() * 10000);
			viewerStats.registerNewViewer(streamId, sessionId);
			viewerStats.registerNewViewer(streamId, sessionId);
			
			assertEquals(1, dsf.getDataStore().get(streamId).getHlsViewerCount());
			
			assertEquals(1000, viewerStats.getTimePeriodMS());
			
			assertEquals(10000, viewerStats.getTimeoutMS());
			
			assertEquals(1, viewerStats.getViewerCount(streamId));
			
			//we set hls time to 1 above, user should be dropped after 1*10 seconds. 
			Awaitility.await().atLeast(9, TimeUnit.SECONDS).atMost(12, TimeUnit.SECONDS).until(() -> {
				return viewerStats.getViewerCount(streamId) == 0;
			});
			
			
			assertEquals(0, dsf.getDataStore().get(streamId).getHlsViewerCount());
			
			
			scheduler.destroy();
			
		
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		
	}

}
