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

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.vertx.core.Vertx;

public class HlsViewerStats implements IStreamStats, ApplicationContextAware{

	protected static Logger logger = LoggerFactory.getLogger(HlsViewerStats.class);
	
	public static final String BEAN_NAME = "hls.viewerstats";

	private DataStore dataStore;
	
	private Vertx vertx;
	
	private DataStoreFactory dataStoreFactory;

	public static final int DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT = 10000;
	/**
	 * Time period in milliseconds to check if viewer is dropped
	 */
	private int timePeriodMS = DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT;

	Map<String, Map<String, Long>> streamsViewerMap = new ConcurrentHashMap<>();

	Map<String, Integer> increaseCounterMap = new ConcurrentHashMap<>();
	
	private Object lock = new Object();

	/**
	 * Time out value in milliseconds, it is regarded as user is not watching stream 
	 * if last request time is older than timeout value
	 */
	private int timeoutMS = 20000;

	@Override
	public void registerNewViewer(String streamId, String sessionId) 
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
			}
			
		});
		
	}
	
	public int getIncreaseCounterMap(String streamId) 
	{
		Integer increaseCounter = increaseCounterMap.get(streamId);
		return increaseCounter != null ? increaseCounter : 0;
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
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)  {
		dataStoreFactory = (DataStoreFactory) applicationContext.getBean(IDataStoreFactory.BEAN_NAME);
		
		vertx = (Vertx) applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		
		AppSettings settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
		timeoutMS = getTimeoutMSFromSettings(settings, timeoutMS);
		
		
		vertx.setPeriodic(DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT, yt-> 
		{
			synchronized (lock) {
				
				Iterator<Entry<String, Map<String, Long>>> streamIterator = streamsViewerMap.entrySet().iterator();
				
				Iterator<Entry<String, Long>> viewerIterator;
				Entry<String, Map<String, Long>> streamViewerEntry;
				Map<String, Long> viewerMapEntry;
				
				long now = System.currentTimeMillis();
				
				while (streamIterator.hasNext()) 
				{
					streamViewerEntry = streamIterator.next();
					
					String streamId = streamViewerEntry.getKey();
					Broadcast broadcast = getDataStore().get(streamId);
					
					boolean isBroadcasting = false;
					
					// Check if it's deleted.
					// This case for the deleted streams(zombi streams)
					if(broadcast != null) {
					
						int numberOfDecrement = 0;
						
						viewerMapEntry = streamViewerEntry.getValue();
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
						
						if(broadcast.getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
							isBroadcasting = true;
						}
					
						numberOfDecrement = -1 * numberOfDecrement;
	
						int numberOfIncrement = getIncreaseCounterMap(streamId);
						if((numberOfIncrement != 0 || numberOfDecrement != 0) && isBroadcasting) {
							
							int hlsDiffCount = numberOfIncrement + numberOfDecrement;
	
							logger.info("Update HLS viewer in stream ID:{} increment count:{} decrement count:{} diff:{}", streamId, numberOfIncrement, numberOfDecrement, hlsDiffCount);
	
							getDataStore().updateHLSViewerCount(streamViewerEntry.getKey(), hlsDiffCount);
							increaseCounterMap.put(streamId, 0);
						}
					}
					if (!isBroadcasting) {
						streamIterator.remove();
						increaseCounterMap.remove(streamId);
					}
				}
			}
		});	
	}
	
	public void resetHLSViewerMap(String streamID) {	
		
		if(streamsViewerMap.get(streamID) != null) {
			streamsViewerMap.get(streamID).clear();
			streamsViewerMap.remove(streamID);
			logger.info("Reset HLS Stream ID: {} removed successfully", streamID);			
		}
		else {
			logger.info("Reset HLS Stream ID: {} remove failed or null", streamID);
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
