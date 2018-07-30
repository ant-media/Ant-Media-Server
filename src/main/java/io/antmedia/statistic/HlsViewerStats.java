package io.antmedia.statistic;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.scheduling.ISchedulingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;

public class HlsViewerStats implements IStreamStats, ApplicationContextAware{

	protected static Logger logger = LoggerFactory.getLogger(HlsViewerStats.class);
	
	public static final String BEAN_NAME = "hls.viewerstats";

	private IDataStore dataStore;

	public static final int DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT = 10000;
	/**
	 * Time period in milliseconds to check if viewer is dropped
	 */
	private int timePeriodMS = DEFAULT_TIME_PERIOD_FOR_VIEWER_COUNT;

	Map<String, Map<String, Long>> streamsViewerMap = new ConcurrentHashMap<>();

	/**
	 * Time out value in milliseconds, it is regarded as user is not watching stream 
	 * if last request time is older than timeout value
	 */
	private int timeoutMS = 20000;

	@Override
	public void registerNewViewer(String streamId, String sessionId) 
	{
		Map<String, Long> viewerMap = streamsViewerMap.get(streamId);
		if (viewerMap == null) {
			viewerMap = new ConcurrentHashMap<>();
		}
		if (!viewerMap.containsKey(sessionId)) {
			//if sessionId is not in the map, this is the first time for getting stream,
			//increment viewer count
			dataStore.updateHLSViewerCount(streamId, 1);
		}
		viewerMap.put(sessionId, System.currentTimeMillis());

		streamsViewerMap.put(streamId, viewerMap);
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

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)  {
		dataStore = (IDataStore) applicationContext.getBean(IDataStore.BEAN_NAME);

		ISchedulingService scheduler = (ISchedulingService) applicationContext.getBean(ISchedulingService.BEAN_NAME);
		
		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			AppSettings settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);
			timeoutMS = getTimeoutMSFromSettings(settings, timeoutMS);
		}

		scheduler.addScheduledJobAfterDelay(timePeriodMS, 
				(ISchedulingService service) -> {
					
					Iterator<Entry<String, Map<String, Long>>> streamIterator = streamsViewerMap.entrySet().iterator();
					Iterator<Entry<String, Long>> viewerIterator;
					Entry<String, Map<String, Long>> streamViewerEntry;
					Map<String, Long> viewerMapEnty;
					
					long now = System.currentTimeMillis();
					while (streamIterator.hasNext()) 
					{
						streamViewerEntry = streamIterator.next();
						viewerMapEnty = streamViewerEntry.getValue();
						viewerIterator = viewerMapEnty.entrySet().iterator();
						int numberOfDecrement = 0;
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
						
						dataStore.updateHLSViewerCount(streamViewerEntry.getKey(), numberOfDecrement);
						
					}
					
				}, timePeriodMS);
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

	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}

}
