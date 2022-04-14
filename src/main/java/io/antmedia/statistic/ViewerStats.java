package io.antmedia.statistic;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.vertx.core.Vertx;

public class ViewerStats {
	
	protected static Logger logger = LoggerFactory.getLogger(ViewerStats.class);
	
	protected Vertx vertx;
	
	public static final String HLS_TYPE = "hls";
	public static final String DASH_TYPE = "dash";
	
	private DataStore dataStore;
	
	protected DataStoreFactory dataStoreFactory;
	
	public static final int DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT = 10000;
	
	/**
	 * Time period in milliseconds to check if viewer is dropped
	 */
	private int timePeriodMS = DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT;
	
	Map<String, Map<String, Long>> streamsViewerMap = new ConcurrentHashMap<>();
	Map<String, String> sessionId2subscriberId = new ConcurrentHashMap<>();
	Map<String, Integer> increaseCounterMap = new ConcurrentHashMap<>();
	
	private Object lock = new Object();
	
	/**
	 * Time out value in milliseconds, it is regarded as user is not watching stream 
	 * if last request time is older than timeout value
	 */
	protected int timeoutMS = 20000;
	
	public void registerNewViewer(String streamId, String sessionId, String subscriberId) 
	{
		//do not block the thread, run in vertx event queue 
		vertx.runOnContext(h -> {
			
			synchronized (lock) {
				//synchronize with database update calculations, because some odd cases may happen
				
				Map<String, Long> viewerMap = streamsViewerMap.get(streamId);
				if (viewerMap == null) {
					viewerMap = new ConcurrentHashMap<>();
				}
				if (!viewerMap.containsKey(sessionId)) 
				{
					int streamIncrementCounter = getIncreaseCounterMap(streamId);
					streamIncrementCounter++;
					increaseCounterMap.put(streamId, streamIncrementCounter);
					
				}
				viewerMap.put(sessionId, System.currentTimeMillis());
				streamsViewerMap.put(streamId, viewerMap);
				if(subscriberId != null) {
					// map sessionId to subscriberId
					sessionId2subscriberId.put(sessionId, subscriberId);
					// add a connected event to the subscriber
					ConnectionEvent event = new ConnectionEvent();
					event.setEventType(ConnectionEvent.CONNECTED_EVENT);
					Date curDate = new Date();
					event.setTimestamp(curDate.getTime());
					getDataStore().addSubscriberConnectionEvent(streamId, subscriberId, event);
				}
			}
			
		});
		
	}
	
	public void resetDASHViewerMap(String streamID) {
		Iterator<Entry<String, Long>> viewerIterator;
		Map<String, Long> viewerMapEntry = streamsViewerMap.get(streamID);
		if(viewerMapEntry != null) {
			// remove all the subscribers associated with the sessions in the stream 
			viewerIterator = viewerMapEntry.entrySet().iterator();
			while (viewerIterator.hasNext()) {
				Entry<String, Long> viewer = viewerIterator.next();
				
				String sessionId = viewer.getKey();
				if(sessionId2subscriberId.containsKey(sessionId)) {
					sessionId2subscriberId.remove(sessionId);					
				}
			}
			
			streamsViewerMap.get(streamID).clear();
			streamsViewerMap.remove(streamID);
			logger.info("Reset DASH Stream ID: {} removed successfully", streamID);			
		}
		else {
			logger.info("Reset DASH Stream ID: {} remove failed or null", streamID);
		}
	}
	
	public void resetHLSViewerMap(String streamID) {
		Iterator<Entry<String, Long>> viewerIterator;
		Map<String, Long> viewerMapEntry = streamsViewerMap.get(streamID);
		if(viewerMapEntry != null) {
			// remove all the subscribers associated with the sessions in the stream 
			viewerIterator = viewerMapEntry.entrySet().iterator();
			while (viewerIterator.hasNext()) {
				Entry<String, Long> viewer = viewerIterator.next();
				
				String sessionId = viewer.getKey();
				if(sessionId2subscriberId.containsKey(sessionId)) {
					sessionId2subscriberId.remove(sessionId);					
				}
			}
			
			streamsViewerMap.get(streamID).clear();
			streamsViewerMap.remove(streamID);
			logger.info("Reset HLS Stream ID: {} removed successfully", streamID);			
		}
		else {
			logger.info("Reset HLS Stream ID: {} remove failed or null", streamID);
		}
	}
	
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
	
	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}
	
	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}
	
	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}
	
	public DataStore getDataStore() {
		if (dataStore == null) {
			dataStore = getDataStoreFactory().getDataStore();
		}
		return dataStore;
	}
	
	public int getIncreaseCounterMap(String streamId) 
	{
		Integer increaseCounter = increaseCounterMap.get(streamId);
		return increaseCounter != null ? increaseCounter : 0;
	}
	
	public static int getTimeoutMSFromSettings(AppSettings settings, int defaultValue, String type) {
		int newTimePeriodMS = defaultValue;
		
		if(type.equals(HLS_TYPE)) {
			String hlsTime = settings.getHlsTime(); 
			if (hlsTime != null && !hlsTime.isEmpty()) {
				try {
					newTimePeriodMS = (int) (Double.parseDouble(hlsTime) * 10 * 1000);
				}
				catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		}
		else {
			String dashTime = settings.getDashFragmentDuration(); 
			if (dashTime != null && !dashTime.isEmpty()) {
				try {
					newTimePeriodMS = (int) (Double.parseDouble(dashTime) * 20 * 1000);
				}
				catch (Exception e) {
					logger.error(e.getMessage());
				}
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
	
	public Map<String, String> getSessionId2subscriberId() {
		return sessionId2subscriberId;
	}

	public void setSessionId2subscriberId(Map<String, String> sessionId2subscriberId) {
		this.sessionId2subscriberId = sessionId2subscriberId;
	}
	
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

}
