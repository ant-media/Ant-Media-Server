package io.antmedia.statistic;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.vertx.core.Vertx;

public class DashViewerStats extends ViewerStats implements IStreamStats, ApplicationContextAware {

	protected static Logger logger = LoggerFactory.getLogger(DashViewerStats.class);
	
	public static final String BEAN_NAME = "dash.viewerstats";
	
	private Object lock = new Object();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)  {
		dataStoreFactory = (DataStoreFactory) applicationContext.getBean(IDataStoreFactory.BEAN_NAME);
		
		vertx = (Vertx) applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

		AppSettings settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
		timeoutMS = getTimeoutMSFromSettings(settings, timeoutMS, DASH_TYPE);
		
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
						}
						
						if(broadcast.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) {
							isBroadcasting = true;
						}
					
						numberOfDecrement = -1 * numberOfDecrement;
	
						int numberOfIncrement = getIncreaseCounterMap(streamId);
						if((numberOfIncrement != 0 || numberOfDecrement != 0) && isBroadcasting) {
							
							int dashDiffCount = numberOfIncrement + numberOfDecrement;
	
							logger.info("Update DASH viewer in stream ID:{} increment count:{} decrement count:{} diff:{}", streamId, numberOfIncrement, numberOfDecrement, dashDiffCount);
	
							getDataStore().updateDASHViewerCount(streamViewerEntry.getKey(), dashDiffCount);
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
		});	
	}

}
