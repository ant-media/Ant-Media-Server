package io.antmedia.statistic;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;

public class ViewerStats {
	
	protected static Logger logger = LoggerFactory.getLogger(ViewerStats.class);
	
	protected Vertx vertx;
	
	public static final String HLS_TYPE = "hls";
	public static final String DASH_TYPE = "dash";
	
	//hls or dash
	private String type;
	
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
	
	protected ServerSettings serverSettings;

	
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
					
					Subscriber subscriber = getDataStore().getSubscriber(streamId, subscriberId);
					if (subscriber == null) {
						subscriber = new Subscriber();
						subscriber.setStreamId(streamId);
						subscriber.setSubscriberId(subscriberId);
					}
					subscriber.setRegisteredNodeIp(serverSettings.getHostAddress());
					
					//if subscriber is coming from the DB following command just updates the one in the db
					getDataStore().addSubscriber(streamId, subscriber);
					
					
					// map sessionId to subscriberId
					sessionId2subscriberId.put(sessionId, subscriberId);
					// add a connected event to the subscriber
					ConnectionEvent event = new ConnectionEvent();
					event.setEventType(ConnectionEvent.CONNECTED_EVENT);
					Date curDate = new Date();
					event.setTimestamp(curDate.getTime());
					event.setEventProtocol(getType());
					
					//TODO: There is a bug here. It adds +1 for each ts request 
					if (getDataStore().addSubscriberConnectionEvent(streamId, subscriberId, event)) {
						logger.info("CONNECTED_EVENT for subscriberId:{} streamId:{}", subscriberId, streamId);
					}
				}
			}
			
		});
		
	}
	
	public void resetViewerMap(String streamID, String type) {
		
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
			logger.info("Reset {} Stream ID: {} removed successfully", type, streamID);			
		}
		else {
			logger.info("Reset {} Stream ID: {} remove failed or null", type, streamID);
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
	
	public void updateViewerCountProcess(String type) {
		
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
						
						String sessionId = viewer.getKey();
						String subscriberId = sessionId2subscriberId.get(sessionId);
						// set subscriber status to not connected
						if(subscriberId != null) {
							// add a disconnected event to the subscriber
							ConnectionEvent event = new ConnectionEvent();
							event.setEventType(ConnectionEvent.DISCONNECTED_EVENT);
							Date curDate = new Date();
							event.setTimestamp(curDate.getTime());
							event.setEventProtocol(getType());
							if (getDataStore().addSubscriberConnectionEvent(streamId, subscriberId, event)) {
								logger.info("DISCONNECTED_EVENT for subscriberId:{} and streamId:{}", subscriberId, streamId);
							}
						}
					}
				}
				
				isBroadcasting = isStreaming(broadcast);
			
				numberOfDecrement = -1 * numberOfDecrement;

				int numberOfIncrement = getIncreaseCounterMap(streamId);
				if((numberOfIncrement != 0 || numberOfDecrement != 0) && isBroadcasting) {
					
					int diffCount = numberOfIncrement + numberOfDecrement;

					logger.info("Update {} viewer in stream ID:{} increment count:{} decrement count:{} diff:{}", type, streamId, numberOfIncrement, numberOfDecrement, diffCount);
					
					if(type.equals(ViewerStats.HLS_TYPE)) {
						getDataStore().updateHLSViewerCount(streamViewerEntry.getKey(), diffCount);
					}
					else {
						getDataStore().updateDASHViewerCount(streamViewerEntry.getKey(), diffCount);
					}

					increaseCounterMap.put(streamId, 0);
				}
			}

			if (!isBroadcasting) {
				// set all connection status information about the subscribers of the stream to false
				viewerMapEntry = streamViewerEntry.getValue();
				viewerIterator = viewerMapEntry.entrySet().iterator();
				while (viewerIterator.hasNext()) {
					Entry<String, Long> viewer = viewerIterator.next();
					
					String sessionId = viewer.getKey();
					String subscriberId = sessionId2subscriberId.get(sessionId);
					// set subscriber status to not connected
					if(subscriberId != null) {
						// add a disconnected event to the subscriber
						ConnectionEvent event = new ConnectionEvent();
						event.setEventType(ConnectionEvent.DISCONNECTED_EVENT);
						Date curDate = new Date();
						event.setTimestamp(curDate.getTime());
						getDataStore().addSubscriberConnectionEvent(streamId, subscriberId, event);
					}
				}
				
				streamIterator.remove();
				increaseCounterMap.remove(streamId);
			}
		}
		
	}
	
	public boolean isStreaming(Broadcast broadcast) {
		return AntMediaApplicationAdapter.isStreaming(broadcast);
	}
	
	public void setServerSettings(ServerSettings serverSettings) {
		this.serverSettings = serverSettings;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
