package io.antmedia.statistic;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.vertx.core.Vertx;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;

public class HlsViewerStats implements IStreamStats, ApplicationContextAware{

	protected static Logger logger = LoggerFactory.getLogger(HlsViewerStats.class);
	
	public static final String BEAN_NAME = "hls.viewerstats";

	private DataStore dataStore;
	
	@Autowired
	public Vertx vertx;
	
	private long hlsCountPeriodicTask;
	
	private DataStoreFactory dataStoreFactory;

	public static final int DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT = 10000;
	/**
	 * Time period in milliseconds to check if viewer is dropped
	 */
	private int timePeriodMS = DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT;

	Map<String, Map<String, Long>> streamsViewerMap = new ConcurrentHashMap<>();
	
	Map<String, Integer> increaseCounterMap = new ConcurrentHashMap<>();
	
	/**
	 * Time out value in milliseconds, it is regarded as user is not watching stream 
	 * if last request time is older than timeout value
	 */
	private int timeoutMS = 20000;
	
	@Override
	public void registerNewViewer(String streamId, String sessionId) 
	{
		
		int streamIncrementCounter = 0;
		Map<String, Long> viewerMap = streamsViewerMap.get(streamId);

		if (viewerMap == null) {
			viewerMap = new ConcurrentHashMap<>();
		}
		if (!viewerMap.containsKey(sessionId)) {
				
			if( increaseCounterMap.get(streamId) != null) {
				streamIncrementCounter = increaseCounterMap.get(streamId);
			}
				
			streamIncrementCounter++;
			increaseCounterMap.put(streamId, streamIncrementCounter);

		}
			
		viewerMap.put(sessionId, System.currentTimeMillis());
		streamsViewerMap.put(streamId, viewerMap);
			
			
	}
	
	public int getIncreaseCounterMap(String streamId) {
		
		int increaseCounter = 0;
		
		if( increaseCounterMap.get(streamId) != null) {
			increaseCounter = increaseCounterMap.get(streamId);
		}
		
		return increaseCounter;
	}
	
	@Override
	public int getViewerCount(String streamId) {
		Map<String, Long> viewerMap = streamsViewerMap.get(streamId);
		int viewerCount = 0;
		if (viewerMap != null) 
		{
			viewerCount = viewerMap.size();
		}
		return viewerCount;
	}
	
	public int getTotalViewerCount() {
		int viewerCount = 0;
		for (Map<String, Long> map : streamsViewerMap.values()) {
			viewerCount += map.size();
		}
		return viewerCount;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)  {
		dataStoreFactory = (DataStoreFactory) applicationContext.getBean(IDataStoreFactory.BEAN_NAME);

		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			AppSettings settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
			timeoutMS = getTimeoutMSFromSettings(settings, timeoutMS);
		}
		
		hlsCountPeriodicTask = vertx.setPeriodic(timeoutMS, yt-> 
		{
			
			Iterator<Entry<String, Map<String, Long>>> streamIterator = streamsViewerMap.entrySet().iterator();
			
			Iterator<Entry<String, Long>> viewerIterator;
			Entry<String, Map<String, Long>> streamViewerEntry;
			Map<String, Long> viewerMapEntry;
			
			long now = System.currentTimeMillis();
			
			while (streamIterator.hasNext()) 
			{
				streamViewerEntry = streamIterator.next();
				viewerMapEntry = streamViewerEntry.getValue();
				
				int numberOfDecrement = 0;
				viewerIterator = viewerMapEntry.entrySet().iterator();
				
				while (viewerIterator.hasNext()) 
				{

					Entry<String, Long> viewer = viewerIterator.next();

					if (viewer.getValue() < (now - getTimeoutMS())) 
					{
						// regard it as not a viewer
						viewerIterator.remove();
						numberOfDecrement++;
					}
				}

				
				numberOfDecrement = -1 * numberOfDecrement;
				
				String streamId = streamViewerEntry.getKey();
				Broadcast broadcast = getDataStore().get(streamId);
				
				if((increaseCounterMap.get(streamId) != 0 || numberOfDecrement != 0) && broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
				
					logger.error("Update HLS viewer in stream ID: {}", streamId);
					
					int numberOfIncrement = increaseCounterMap.get(streamId);
					
					int hlsDiffCount = numberOfIncrement + numberOfDecrement;
					
					getDataStore().updateHLSViewerCount(streamViewerEntry.getKey(), hlsDiffCount);
					increaseCounterMap.put(streamId, 0);
				}				
				if(!broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
					streamIterator.remove();
					increaseCounterMap.put(streamId, 0);
				}
			}

		});	
	}
	
	public void resetHLSViewerMap(String streamID) {	
		
		if(streamsViewerMap.get(streamID) != null) {
			streamsViewerMap.get(streamID).clear();
			streamsViewerMap.remove(streamID);
			logger.error("Reset HLS Stream ID: {} removed successfully", streamID);			
		}
		else {
			logger.error("Reset HLS Stream ID: {} remove failed or null", streamID);
		}
	}
	
	public static int getTimeoutMSFromSettings(AppSettings settings, int defaultValue) {
		int newTimePeriodMS = defaultValue;
		String hlsTime = settings.getHlsTime();
		if (hlsTime != null && !hlsTime.isEmpty()) {
			try {
				newTimePeriodMS = Integer.valueOf(hlsTime) * 10 * 1000;
			}
			catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return newTimePeriodMS;
	}

	public void setTimePeriodMS(int timePeriodMS) {
		this.timePeriodMS = timePeriodMS;
	}
	public int getTimePeriodMS() {
		return timePeriodMS;
	}

	public int getTimeoutMS() {
		return timeoutMS;
	}

	public DataStore getDataStore() {
		if (dataStore == null) {
			dataStore = getDataStoreFactory().getDataStore();
		}
		return dataStore;
	}
	
	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	
	
	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}
	
	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

}
