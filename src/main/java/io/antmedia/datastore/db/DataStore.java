package io.antmedia.datastore.db;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberMetadata;
import io.antmedia.datastore.db.types.SubscriberStats;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;

public abstract class DataStore {


	//Do not forget to write function descriptions especially if you are adding new functions

	private static final int QUERY_TIME_THRESHOLD_MS_SEC  = 100;
	
	private static final int QUERY_TIME_THRESHOLD_NANO_SEC = QUERY_TIME_THRESHOLD_MS_SEC * 1_000_000;
	private static final int QUERY_TIME_EXTRA_LOG_THRESHOLD_NANO_SEC = 5 * QUERY_TIME_THRESHOLD_NANO_SEC;
	public static final int MAX_ITEM_IN_ONE_LIST = 250;
	public static final String REPLACE_CHARS_REGEX = "[\n|\r|\t]";


	private long executedQueryCount = 0;
	
	private long totalQueryTimeNanoSec = 0;
	

	protected volatile boolean available = false;

	protected static Logger logger = LoggerFactory.getLogger(DataStore.class);
	
	/**
	 * We have appSettings fields because we need to refect the changes on the fly
	 */
	protected AppSettings appSettings;


	public abstract String save(Broadcast broadcast);

	//TODO: In rare scenarios, streamId can not be unique 
	protected Broadcast saveBroadcast(Broadcast broadcast) {
		String streamId = null;
		try {
			if (broadcast.getStreamId() == null || broadcast.getStreamId().isEmpty()) {
				streamId = RandomStringUtils.randomAlphanumeric(16) + System.nanoTime();
				broadcast.setStreamId(streamId);
			}
			streamId = broadcast.getStreamId();
			String rtmpURL = broadcast.getRtmpURL();
			if (rtmpURL != null) {
				rtmpURL += streamId;
			}
			broadcast.setRtmpURL(rtmpURL);
			if (broadcast.getStatus() == null) {
				broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_CREATED);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return broadcast;
	}

	/**
	 * Return the broadcast in data store
	 * @param id
	 * @return broadcast
	 */
	public abstract Broadcast get(String id);

	public Broadcast get(Map<String, String> broadcastMap, String streamId, Gson gson) {
		long startTime = System.nanoTime();

		Broadcast broadcast = null;
		synchronized (this) 
		{	
			String jsonString = null;
			if (streamId != null) 
			{
				jsonString = broadcastMap.get(streamId);
				if(jsonString != null) 
				{
					broadcast = gson.fromJson(jsonString, Broadcast.class);
				}
			}
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "get(Map<String, String> broadcastMap, String streamId, Gson gson)");
		return broadcast;
	}

	/**
	 * Return the vod by id
	 * @param id
	 * @return Vod object
	 */
	public abstract VoD getVoD(String id);

	public VoD getVoD(Map<String, String> vodMap, String vodId, Gson gson) {
		long startTime = System.nanoTime();
		VoD vod = null;
		synchronized (this) {
			if (vodId != null) {
				String jsonString = null;
				jsonString = vodMap.get(vodId);

				if (jsonString != null) {
					vod = gson.fromJson(jsonString, VoD.class);
				}
			}
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getVoD(Map<String, String> vodMap, String vodId, Gson gson)");
		return vod;
	}

	public abstract boolean updateStatus(String id, String status);

	public static final long TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME = 5000;
	protected int totalWebRTCViewerCount = 0;
	protected long totalWebRTCViewerCountLastUpdateTime = 0;


	/**
	 * Returns the number of vods which contains searched string
	 * @param search is used for searching in vodIds and names of the vods
	 * @return
	 */
	public abstract long getPartialVodNumber(String search);

	/**
	 * Returns the number of broadcasts which contains searched string
	 * @param search is used for searching in streamIds and names of the stream
	 * @return
	 */
	public abstract long getPartialBroadcastNumber(String search);

	public abstract boolean addEndpoint(String id, Endpoint endpoint);

	/**
	 * Add VoD record to the datastore
	 * @param vod
	 * @return the id of the VoD if it's successful or it returns null if it's failed
	 */
	public abstract String addVod(VoD vod);

	/**
	 * Use getTotalBroadcastNumber
	 * @deprecated
	 */
	@Deprecated
	public abstract long getBroadcastCount();

	public long getBroadcastCount(Map<String,String> broadcastMap) {
		long startTime = System.nanoTime();
		long count = 0;
		synchronized (this) {
			count = broadcastMap.size();
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getBroadcastCount(Map<String,String> broadcastMap");
		
		return count;
	}

	public abstract boolean delete(String id);

	public abstract boolean deleteVod(String id);

	public abstract boolean updateVoDProcessStatus(String id, String status);

	/**
	 * Returns the Broadcast List in order
	 *
	 * @param offset the number of items to skip
	 * @param size batch size
	 * @param type can get "liveStream" or "ipCamera" or "streamSource" or "VoD" values. Default is getting all broadcast types.
	 * @param sortBy can get "name" or "date" or "status" values
	 * @param orderBy can get "desc" or "asc"
	 * @param search is used for searching in streamIds and names of the stream
	 * @return
	 */
	public abstract List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search);


	public abstract boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl);

	public abstract List<Broadcast> getExternalStreamsList();

	/**
	 * Closes the database
	 * @param deleteDB if it's true, it also deletes the db and closes
	 */
	public abstract void close(boolean deleteDB);

	/**
	 * Returns the VoD List in order
	 *
	 * @param offset: the number of items to skip
	 * @param size: batch size
	 * @param sortBy can get "name" or "date" values
	 * @param orderBy can get "desc" or "asc"
	 * @param filterStreamId is used for filtering the vod by stream id. If it's null or empty, it's not used
	 * @param search is used for searching in vodIds and names of the vods.
	 * @return
	 */
	public abstract List<VoD> getVodList(int offset, int size, String sortBy, String orderBy, String filterStreamId, String search);

	public List<VoD> getVodListV2(Map<String, String> vodMap, String streamId, String search, Gson gson, String dbName) {
		long startTime = System.nanoTime();

		ArrayList<VoD> vods = new ArrayList<>();
		synchronized (this) {

			int length = vodMap.size();
			int i = 0;
			for (String vodString : vodMap.values()) {
				VoD vod = gson.fromJson(vodString, VoD.class);
				if (streamId != null && !streamId.isEmpty()) {
					if (vod.getStreamId().equals(streamId)) {
						vods.add(vod);
					}
				} else {
					vods.add(vod);
				}

				i++;
				if (i > length) {
					logger.error("Inconsistency in DB. It's likely db file({}) is damaged", dbName);
					break;
				}
			}
			if (search != null && !search.isEmpty()) {
				search = search.replaceAll(REPLACE_CHARS_REGEX, "_");
				logger.info("server side search called for VoD searchString = {}", search);
				vods = searchOnServerVod(vods, search);
			}
			
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getVodListV2(Map<String, String> vodMap, String streamId, String search, Gson gson, String dbName)");
		
		return vods;
	}


	public abstract boolean removeAllEndpoints(String id);

	public abstract long getTotalVodNumber();

	public long getTotalVodNumber(Map<String, String> broadcastMap) {
		long startTime = System.nanoTime();
		long count = 0;
		synchronized (this) {
			count = broadcastMap.size();
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getTotalVodNumber(Map<String, String> broadcastMap)");
		return count;
	}

	public abstract long getTotalBroadcastNumber();

	public long getTotalBroadcastNumber(Map<String,String> broadcastMap) {
		long startTime = System.nanoTime();
		long count = 0;
		synchronized (this) {
			count = broadcastMap.size();
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getTotalBroadcastNumber(Map<String,String> broadcastMap)");
		return count;
	}

	public abstract void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects);

	public abstract List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize);

	public List<TensorFlowObject> getDetectionList(Map<String, String> detectionMap, String idFilter, int offsetSize, int batchSize, Gson gson) {
		long startTime = System.nanoTime();


		List<TensorFlowObject> list = new ArrayList<>();

		synchronized (this) {
			Type listType = new TypeToken<ArrayList<TensorFlowObject>>() {
			}.getType();
			int offsetCount = 0;
			int batchCount = 0;

			if (batchSize > MAX_ITEM_IN_ONE_LIST) {
				batchSize = MAX_ITEM_IN_ONE_LIST;
			}

			for (Iterator<String> keyIterator = detectionMap.keySet().iterator(); keyIterator.hasNext();) {
				String keyValue = keyIterator.next();
				if (keyValue.startsWith(idFilter)) {
					if (offsetCount < offsetSize) {
						offsetCount++;
						continue;
					}
					if (batchCount >= batchSize) {
						break;
					}
					List<TensorFlowObject> detectedList = gson.fromJson(detectionMap.get(keyValue), listType);
					list.addAll(detectedList);
					batchCount = list.size();
				}
			}
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getDetectionList(Map<String, String> detectionMap, String idFilter, int offsetSize, int batchSize, Gson gson)");
		return list;
	}

	public abstract List<TensorFlowObject> getDetection(String id);

	public List<TensorFlowObject> getDetection(Map<String, String> detectionMap, String id, Gson gson){
		long startTime = System.nanoTime();
		List<TensorFlowObject> list = new ArrayList<>();
		synchronized (this) {
			if (id != null) {
				String jsonString = detectionMap.get(id);
				if (jsonString != null) {
					Type listType = new TypeToken<ArrayList<TensorFlowObject>>(){}.getType();
					list = gson.fromJson(jsonString, listType);
				}
			}
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getDetection(Map<String, String> detectionMap, String id, Gson gson)");
		return list;
	}

	/**
	 * saves token to store
	 * @param token - created token
	 * @return  true/false
	 */
	public abstract boolean saveToken(Token token);

	/**
	 * Lists all tokens of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of tokens
	 */
	public abstract List<Token> listAllTokens (String streamId, int offset, int size);

	public List<Token> listAllTokens(Map<String, String> tokenMap, String streamId, int offset, int size, Gson gson) {
		
		long startTime = System.nanoTime();


		List<Token> list = new ArrayList<>();
		List<Token> listToken = new ArrayList<>();

		synchronized (this) {
			Collection<String> values = tokenMap.values();
			int t = 0;
			int itemCount = 0;
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			if (offset < 0) {
				offset = 0;
			}

			Iterator<String> iterator = values.iterator();

			while (iterator.hasNext()) {
				Token token = gson.fromJson(iterator.next(), Token.class);

				if (token.getStreamId().equals(streamId)) {
					list.add(token);
				}
			}

			Iterator<Token> listIterator = list.iterator();

			while (itemCount < size && listIterator.hasNext()) {
				if (t < offset) {
					t++;
					listIterator.next();
				} else {

					listToken.add(listIterator.next());
					itemCount++;

				}
			}

		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "listAllTokens(Map<String, String> tokenMap, String streamId, int offset, int size, Gson gson)");
		return listToken;
	}

	/**
	 * Validates token
	 * @param token
	 * @param streamId
	 * @return token if validated, null if not
	 */
	public abstract Token validateToken (Token token);

	/**
	 * Delete all tokens of the stream
	 * @param streamId
	 */

	public abstract boolean revokeTokens (String streamId);

	/**
	 * Delete specific token
	 * @param tokenId id of the token
	 */

	public abstract boolean deleteToken (String tokenId);

	/**
	 * retrieve specific token
	 * @param tokenId id of the token
	 */

	public abstract Token getToken (String tokenId);

	public Token getToken(Map<String, String> tokenMap, String tokenId, Gson gson) {
		long startTime = System.nanoTime();
		Token token = null;
		synchronized (this) {
			if (tokenId != null) {
				String jsonString = tokenMap.get(tokenId);
				if (jsonString != null) {
					token = gson.fromJson(jsonString, Token.class);
				}
			}
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getToken(Map<String, String> tokenMap, String tokenId, Gson gson)");
		return token;
	}

	/**
	 * Lists all subscribers of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of subscribers
	 */	
	public abstract List<Subscriber> listAllSubscribers(String streamId, int offset, int size);
	
	/**
	 * Returns the number of the subscribers of requested stream
	 * @param streamId
	 * @return number of the subscribers of requested stream
	 */	
	public abstract long getConnectedSubscriberCount(String streamId);
	
	/**
	 * Lists connected subscribers of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of subscribers
	 */	
	public abstract List<Subscriber> getConnectedSubscribers(String streamId, int offset, int size);
	
	
	public List<Subscriber> listAllSubscribers(Map<String, String> subscriberMap, String streamId, int offset, int size, Gson gson, boolean connectedOnly) {
		long startTime = System.nanoTime();

		List<Subscriber> list = new ArrayList<>();
		List<Subscriber> listSubscriber = new ArrayList<>();

		synchronized (this) {
			Collection<String> values = subscriberMap.values();
			int t = 0;
			int itemCount = 0;
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			if (offset < 0) {
				offset = 0;
			}

			Iterator<String> iterator = values.iterator();

			while (iterator.hasNext()) {
				Subscriber subscriber = gson.fromJson(iterator.next(), Subscriber.class);

				if (subscriber.getStreamId().equals(streamId) &&
					    (!connectedOnly || subscriber.isConnected())) {
					list.add(subscriber);
				}
			}

			Iterator<Subscriber> listIterator = list.iterator();

			while (itemCount < size && listIterator.hasNext()) {
				if (t < offset) {
					t++;
					listIterator.next();
				} else {

					listSubscriber.add(listIterator.next());
					itemCount++;

				}
			}

		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "listAllSubscribers(Map<String, String> subscriberMap, String streamId, int offset, int size, Gson gson)");
		return listSubscriber;
	}

	/**
	 * Lists all subscriber statistics of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of subscriber statistics
	 */	
	public List<SubscriberStats> listAllSubscriberStats(String streamId, int offset, int size) {
		long startTime = System.nanoTime();
		List<Subscriber> subscribers= listAllSubscribers(streamId, offset, size);
		List<SubscriberStats> subscriberStats = new ArrayList<>();


		for(Subscriber subscriber : subscribers) {
			SubscriberStats stat = subscriber.getStats();
			stat.setStreamId(subscriber.getStreamId());
			stat.setSubscriberId(subscriber.getSubscriberId());
			subscriberStats.add(stat);
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "listAllSubscriberStats(String streamId, int offset, int size)");

		return subscriberStats;
	}

	/**
	 * adds subscriber to the datastore for this stream
	 * @param streamId
	 * @param subscriber - subscriber to be added
	 * @return- true if set, false if not
	 */	
	public abstract boolean addSubscriber(String streamId, Subscriber subscriber);

	/**
	 * deletes subscriber from the datastore for this stream
	 * @param streamId
	 * @param subscriberId - id of the subsciber to be deleted
	 * @return- true if set, false if not
	 */		
	public abstract boolean deleteSubscriber(String streamId, String subscriberId);

	/**
	 * blocks subscribe from playing or publishing
	 * @param streamId
	 * @param subscriberId - id of the subsciber to be blocked
	 * @param blockedType - it can be the value of the static field {@link Subscriber#PLAY_TYPE}, {@link Subscriber#PUBLISH_TYPE}, {@link Subscriber#PUBLISH_AND_PLAY_TYPE}
	 * 						publish, play, publish_play
	 * 
	 * @param seconds - duration of seconds to block the user
	 * @return- true if set, false if not
	 */
	public abstract boolean blockSubscriber(String streamId, String subscriberId, String blockedType, int seconds);

	/**
	 * deletes all subscriber from the datastore for this stream
	 * @param streamId
	 * @return- true if set, false if not
	 */		
	public abstract boolean revokeSubscribers(String streamId);

	/**
	 * gets subscriber from the datastore
	 * @param streamId
	 * @param subscriberId - id of the subsciber to be deleted
	 * @return- Subscriber
	 */	
	public abstract Subscriber getSubscriber (String streamId, String subscriberId);

	public Subscriber getSubscriber(Map<String, String> subscriberMap, String streamId, String subscriberId, Gson gson) {
		long startTime = System.nanoTime();

		Subscriber subscriber = null;
		synchronized (this) {
			if (subscriberId != null && streamId != null) {
				String jsonString = subscriberMap.get(Subscriber.getDBKey(streamId, subscriberId));
				if (jsonString != null) {
					subscriber = gson.fromJson(jsonString, Subscriber.class);
				}
			}
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getSubscriber(Map<String, String> subscriberMap, String streamId, String subscriberId, Gson gson) ");
		return subscriber;
	}

	/**
	 * gets the connection status of the subscriber from the datastore
	 * @param streamId
	 * @param subscriberId - id of the subscriber 
	 * @return- true if connected else false
	 */	
	public boolean isSubscriberConnected(String streamId, String subscriberId) {
		long startTime = System.nanoTime();
		boolean connected = false;
		Subscriber subscriber = getSubscriber(streamId, subscriberId);

		if(subscriber != null) {
			connected = subscriber.isConnected();
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "isSubscriberConnected(String streamId, String subscriberId)");
		return connected;
	}

	/**
	 * sets the connection status of the subscriber in the datastore
	 * @param streamId
	 * @param subscriberId - id of the subscriber 
	 * @param event - connection event which occured for this subscriber
	 * @return- true if successful else false
	 */	
	public boolean addSubscriberConnectionEvent(String streamId, String subscriberId, ConnectionEvent event) {
		long startTime = System.nanoTime();
		boolean result = false;
		if (appSettings.isWriteSubscriberEventsToDatastore() && event != null) 
		{
			
			Subscriber subscriber = getSubscriber(streamId, subscriberId);
			
			if (subscriber != null && !StringUtils.isBlank(subscriber.getSubscriberId())) 
			{
				if(ConnectionEvent.CONNECTED_EVENT.equals(event.getEventType())) {
					subscriber.setConnected(true);
					subscriber.setCurrentConcurrentConnections(subscriber.getCurrentConcurrentConnections()+1);
				} else if(ConnectionEvent.DISCONNECTED_EVENT.equals(event.getEventType())) {
					subscriber.setConnected(false);
					subscriber.setCurrentConcurrentConnections(subscriber.getCurrentConcurrentConnections()-1);
				}
				addSubscriber(streamId, subscriber);
			}
			
			result = handleConnectionEvent(streamId, subscriberId, event);
		}
		else {
			logger.debug("Not saving subscriber events to datastore because either writeSubscriberEventsToDatastore are false in the settings or event is null."
					+ "writeSubscriberEventsToDatastore:{} and event is {} null", appSettings.isWriteSubscriberEventsToDatastore(), event == null ? "" : "not");
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "addSubscriberConnectionEvent(String streamId, String subscriberId, ConnectionEvent event)");
		
		return result;


	}

	// helper method used by all datastores
	protected boolean handleConnectionEvent(String streamId, String subscriberId, ConnectionEvent event) 
	{
		long startTime = System.nanoTime();
		boolean result = false;
		
		if (StringUtils.isNoneBlank(subscriberId, streamId)) 
		{
			event.setStreamId(streamId);
			event.setSubscriberId(subscriberId);
			
			result = addConnectionEvent(event);
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "handleConnectionEvent(String streamId, String subscriberId, ConnectionEvent event)");
		return result;
	}
	
	protected abstract boolean addConnectionEvent(ConnectionEvent event);

	/**
	 * sets the avarage bitrate of the subscriber in the datastore
	 * @param streamId
	 * @param subscriberId - id of the subscriber 
	 * @param event - bitrate measurement event
	 * @return- true if successful else false
	 */	
	public boolean updateSubscriberBitrateEvent(String streamId, String subscriberId, long avgVideoBitrate, long avgAudioBitrate) {
		boolean result = false;
		long startTime = System.nanoTime();
		if (appSettings.isWriteSubscriberEventsToDatastore()) 
		{
			Subscriber subscriber = getSubscriber(streamId, subscriberId);
			if (subscriber != null) {	
				subscriber.setAvgVideoBitrate(avgVideoBitrate);
				subscriber.setAvgAudioBitrate(avgAudioBitrate);
				addSubscriber(streamId, subscriber);
				result = true;
			}
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "updateSubscriberBitrateEvent(String streamId, String subscriberId, long avgVideoBitrate, long avgAudioBitrate)");

		return result;
	}



	/**
	 * sets the connection status of all the subscribers false in the datastore
	 * called after an ungraceful shutdown
	 * @return- true if successful else false
	 */	
	public abstract boolean resetSubscribersConnectedStatus ();	

	/**
	 * enables or disables mp4 muxing for the stream
	 * @param streamId- id of the stream
	 * @param enabled 1 means enabled, -1 means disabled, 0 means no setting for the stream
	 * @return- true if set, false if not
	 */
	public abstract boolean setMp4Muxing(String streamId, int enabled);

	/**
	 * enables or disables WebM muxing for the stream
	 * @param streamId- id of the stream
	 * @param enabled 1 means enabled, -1 means disabled, 0 means no setting for the stream
	 * @return- true if set, false if not
	 */
	public abstract boolean setWebMMuxing(String streamId, int enabled);


	/**
	 * Gets the video files under the {@code fileDir} directory parameter
	 * and saves them to the datastore as USER_VOD in {@code Vod} class
	 * @param file
	 * @return number of files that are saved to datastore
	 */
	public abstract int fetchUserVodList(File filedir);

	/**
	 * Return the number of active broadcasts in the server
	 * @return
	 */
	public abstract long getActiveBroadcastCount();

	public long getActiveBroadcastCount(Map<String, String> broadcastMap, Gson gson, String hostAddress) {
		long startTime = System.nanoTime();

		int activeBroadcastCount = 0;
		synchronized (this) {

			Collection<String> values = broadcastMap.values();
			for (String broadcastString : values) 
			{
				Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);
				String status = broadcast.getStatus();
				if (IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(status) &&
						(StringUtils.isBlank(hostAddress) || hostAddress.equals(broadcast.getOriginAdress()))) 
				{
					activeBroadcastCount++;
				}
			}
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getActiveBroadcastCount(Map<String, String> broadcastMap, Gson gson, String hostAddress)");
		return activeBroadcastCount;
	}

	public List<Broadcast> getActiveBroadcastList(Map<String, String> broadcastMap, Gson gson, String hostAddress) {
		long startTime = System.nanoTime();

		List<Broadcast> broadcastList = new ArrayList<>();
		synchronized (this) {

			Collection<String> values = broadcastMap.values();
			for (String broadcastString : values) 
			{
				Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);

				String status = broadcast.getStatus();
				if (IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(status) &&
						(StringUtils.isBlank(hostAddress) || hostAddress.equals(broadcast.getOriginAdress())))
				{
					broadcastList.add(broadcast);
				}
			}
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getActiveBroadcastList(Map<String, String> broadcastMap, Gson gson, String hostAddress)");

		return broadcastList;
	}

	/**
	 * Updates the Broadcast objects fields if it's not null. The updated fields are
	 * as follows name, description, userName, password, IP address, streamUrl
	 * 
	 * @param broadcast
	 * @return
	 */
	public abstract boolean updateBroadcastFields(String streamId, BroadcastUpdate broadcast);

	/**
	 * Add or subtract the HLS viewer count from current value
	 * @param streamId
	 * @param diffCount
	 */
	public boolean updateHLSViewerCount(String streamId, int diffCount) {
		if (appSettings.isWriteStatsToDatastore()) {
			return updateHLSViewerCountLocal(streamId, diffCount);
		}
		return false;
	}

	protected abstract boolean updateHLSViewerCountLocal(String streamId, int diffCount);

	/**
	 * Add or subtract the DASH viewer count from current value
	 * @param streamId
	 * @param diffCount
	 */
	public boolean updateDASHViewerCount(String streamId, int diffCount) {
		if (appSettings.isWriteStatsToDatastore()) {
			return updateDASHViewerCountLocal(streamId, diffCount);
		}
		return false;
	}

	protected abstract boolean updateDASHViewerCountLocal(String streamId, int diffCount);	

	/**
	 * Returns the total number of detected objects in the stream
	 * @param id is the stream id
	 * @return total number of detected objects
	 */
	public abstract long getObjectDetectedTotal(String streamId);

	public long getObjectDetectedTotal(Map<String, String> detectionMap, String streamId, Gson gson) {
		long startTime = System.nanoTime();

		List<TensorFlowObject> list = new ArrayList<>();

		Type listType = new TypeToken<ArrayList<TensorFlowObject>>() {
		}.getType();

		synchronized (this) {

			for (Iterator<String> keyIterator = detectionMap.keySet().iterator(); keyIterator.hasNext();) {
				String keyValue = keyIterator.next();
				if (keyValue.startsWith(streamId)) {
					List<TensorFlowObject> detectedList = gson.fromJson(detectionMap.get(keyValue), listType);
					list.addAll(detectedList);
				}
			}
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getObjectDetectedTotal(Map<String, String> detectionMap, String streamId, Gson gson)");
		return list.size();
	}

	/**
	 * Update the WebRTC viewer count
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one
	 * if it is false, decrement viewer count by one
	 */
	public boolean updateWebRTCViewerCount(String streamId, boolean increment) {
		if (appSettings.isWriteStatsToDatastore()) {
			return updateWebRTCViewerCountLocal(streamId, increment);
		}
		return false;
	}

	protected abstract boolean updateWebRTCViewerCountLocal(String streamId, boolean increment);


	/**
	 * Update the RTMP viewer count
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one
	 * if it is false, decrement viewer count by one
	 */
	public boolean updateRtmpViewerCount(String streamId, boolean increment) {
		if (appSettings.isWriteStatsToDatastore()) {
			return updateRtmpViewerCountLocal(streamId, increment);
		}
		return false;
	}

	protected abstract boolean updateRtmpViewerCountLocal(String streamId, boolean increment);


	/**
	 * Saves the stream info to the db
	 * @param streamInfo
	 */
	public abstract void saveStreamInfo(StreamInfo streamInfo);

	/**
	 * Returns stream info list added to db
	 * @param streamId
	 * @return
	 */
	public abstract  List<StreamInfo> getStreamInfoList(String streamId);

	/**
	 * Remove the stream info list from db
	 * @param streamId
	 */
	public abstract  void clearStreamInfoList(String streamId);

	/**
	 * Updates the stream fields if it's not null
	 * @param broadcast
	 * @param name
	 * @param description
	 * @param userName
	 * @param password
	 * @param ipAddr
	 * @param streamUrl
	 */
	protected void updateStreamInfo(Broadcast broadcast, BroadcastUpdate newBroadcast)
	{
		if (newBroadcast.getName() != null) {
			broadcast.setName(newBroadcast.getName());
		}

		if (newBroadcast.getDescription() != null) {
			broadcast.setDescription(newBroadcast.getDescription());
		}

		if (newBroadcast.getUsername() != null) {
			broadcast.setUsername(newBroadcast.getUsername());
		}

		if (newBroadcast.getPassword() != null) {
			broadcast.setPassword(newBroadcast.getPassword());
		}

		if (newBroadcast.getIpAddr() != null) {
			broadcast.setIpAddr(newBroadcast.getIpAddr());
		}

		if (newBroadcast.getStreamUrl() != null) {
			broadcast.setStreamUrl(newBroadcast.getStreamUrl());
		}

		if (newBroadcast.getLatitude() != null) {
			broadcast.setLatitude(newBroadcast.getLatitude());
		}

		if (newBroadcast.getLongitude() != null) {
			broadcast.setLongitude(newBroadcast.getLongitude());
		}

		if (newBroadcast.getAltitude() != null) {
			broadcast.setAltitude(newBroadcast.getAltitude());
		}

		//mainTrackStreamId can be empty
		if (newBroadcast.getMainTrackStreamId() != null) {
			broadcast.setMainTrackStreamId(newBroadcast.getMainTrackStreamId());
		}

		if (newBroadcast.getStartTime() != null) {
			broadcast.setStartTime(newBroadcast.getStartTime());
		}

		if (newBroadcast.getOriginAdress() != null) {
			broadcast.setOriginAdress(newBroadcast.getOriginAdress());
		}

		if (newBroadcast.getStatus() != null) {
			broadcast.setStatus(newBroadcast.getStatus());
		}

		if (newBroadcast.getAbsoluteStartTimeMs() != null) {
			broadcast.setAbsoluteStartTimeMs(newBroadcast.getAbsoluteStartTimeMs());
		}		

		if (newBroadcast.getUpdateTime() != null && newBroadcast.getUpdateTime() > 0) {
			broadcast.setUpdateTime(newBroadcast.getUpdateTime());
		}

		if (newBroadcast.getPlayListItemList() != null) {
			broadcast.setPlayListItemList(newBroadcast.getPlayListItemList());
		}

		if (newBroadcast.getPlayListStatus() != null) {
			broadcast.setPlayListStatus(newBroadcast.getPlayListStatus());
		}

		if (newBroadcast.getEndPointList() != null) {
			broadcast.setEndPointList(newBroadcast.getEndPointList());
		}
		if (newBroadcast.getSubFolder() != null) {
			broadcast.setSubFolder(newBroadcast.getSubFolder());
		}
		if (newBroadcast.getListenerHookURL() != null && !newBroadcast.getListenerHookURL().isEmpty()) {
			broadcast.setListenerHookURL(newBroadcast.getListenerHookURL());
		}

		if (newBroadcast.getSpeed() != null) {
			broadcast.setSpeed(newBroadcast.getSpeed());
		}

		if (newBroadcast.getMetaData() != null) {
			broadcast.setMetaData(newBroadcast.getMetaData());
		}

		if (newBroadcast.getConferenceMode() != null) {
			broadcast.setConferenceMode(newBroadcast.getConferenceMode());
		}

		if (newBroadcast.getEncoderSettingsList() != null) {
			broadcast.setEncoderSettingsList(newBroadcast.getEncoderSettingsList());
		}

		if (newBroadcast.getPlannedStartDate() != null) {
			broadcast.setPlannedStartDate(newBroadcast.getPlannedStartDate());
		}

		if (newBroadcast.getPlannedEndDate() != null) {
			broadcast.setPlannedEndDate(newBroadcast.getPlannedEndDate());
		}

		if (newBroadcast.getSeekTimeInMs() != null) {
			broadcast.setSeekTimeInMs(newBroadcast.getSeekTimeInMs());
		}

		if (newBroadcast.getReceivedBytes() != null) {
			broadcast.setReceivedBytes(newBroadcast.getReceivedBytes());
		}

		if (newBroadcast.getDuration() != null) {
			broadcast.setDuration(newBroadcast.getDuration());
		}

		if (newBroadcast.getBitrate() != null) {
			broadcast.setBitrate(newBroadcast.getBitrate());
		}

		if (newBroadcast.getUserAgent() != null) {
			broadcast.setUserAgent(newBroadcast.getUserAgent());
		}

		if (newBroadcast.getWebRTCViewerLimit() != null) {
			broadcast.setWebRTCViewerLimit(newBroadcast.getWebRTCViewerLimit());
		}
		
		if (newBroadcast.getWebRTCViewerCount() != null) {
			broadcast.setWebRTCViewerCount(newBroadcast.getWebRTCViewerCount());
		}

		if (newBroadcast.getHlsViewerLimit() != null) {
			broadcast.setHlsViewerLimit(newBroadcast.getHlsViewerLimit());
		}
		
		if (newBroadcast.getHlsViewerCount() != null) {
			broadcast.setHlsViewerCount(newBroadcast.getHlsViewerCount());
		}

		if (newBroadcast.getDashViewerLimit() != null) {
			broadcast.setDashViewerLimit(newBroadcast.getDashViewerLimit());
		}
		
		if (newBroadcast.getDashViewerCount() != null) {
			broadcast.setDashViewerCount(newBroadcast.getDashViewerCount());
		}

		if (newBroadcast.getSubTrackStreamIds() != null) {
			broadcast.setSubTrackStreamIds(newBroadcast.getSubTrackStreamIds());
		}

		if (newBroadcast.getPlaylistLoopEnabled() != null) {
			broadcast.setPlaylistLoopEnabled(newBroadcast.getPlaylistLoopEnabled());
		}

		if (newBroadcast.getAutoStartStopEnabled() != null) {
			broadcast.setAutoStartStopEnabled(newBroadcast.getAutoStartStopEnabled());
		}		

		if (newBroadcast.getCurrentPlayIndex() != null) {
			broadcast.setCurrentPlayIndex(newBroadcast.getCurrentPlayIndex());
		}

		if (newBroadcast.getSubtracksLimit() != null) {
			broadcast.setSubtracksLimit(newBroadcast.getSubtracksLimit());
		}

		if (newBroadcast.getPendingPacketSize() != null) {
			broadcast.setPendingPacketSize(newBroadcast.getPendingPacketSize());
		}
		
		if (newBroadcast.getQuality() != null) {
			broadcast.setQuality(newBroadcast.getQuality());
		}

		if (newBroadcast.getRole() != null) {
			broadcast.setRole(newBroadcast.getRole());
		}
		
		if (newBroadcast.getWidth() != null) {
			broadcast.setWidth(newBroadcast.getWidth());
		}
		
		if (newBroadcast.getHeight() != null) {
			broadcast.setHeight(newBroadcast.getHeight());
		}
		
		if (newBroadcast.getEncoderQueueSize() != null) {
			broadcast.setEncoderQueueSize(newBroadcast.getEncoderQueueSize());
		}
		
		if (newBroadcast.getDropPacketCountInIngestion() != null) {
			broadcast.setDropPacketCountInIngestion(newBroadcast.getDropPacketCountInIngestion());
		}
		
		if (newBroadcast.getDropFrameCountInEncoding() != null) {
			broadcast.setDropFrameCountInEncoding(newBroadcast.getDropFrameCountInEncoding());
		}
		
		if (newBroadcast.getPacketLostRatio() != null) {
			broadcast.setPacketLostRatio(newBroadcast.getPacketLostRatio());
		}
		
		if (newBroadcast.getJitterMs() != null) {
			broadcast.setJitterMs(newBroadcast.getJitterMs());
		}
		
		if (newBroadcast.getRttMs() != null) {
			broadcast.setRttMs(newBroadcast.getRttMs());
		}
		
		if (newBroadcast.getPacketsLost() != null) {
			broadcast.setPacketsLost(newBroadcast.getPacketsLost());
		}
		
		if (newBroadcast.getRemoteIp() != null) {
			broadcast.setRemoteIp(newBroadcast.getRemoteIp());
		}
		
		if (newBroadcast.getVirtual() != null) {
			broadcast.setVirtual(newBroadcast.getVirtual());
		}
		
		if (newBroadcast.getMaxIdleTime() != null) {
			broadcast.setMaxIdleTime(newBroadcast.getMaxIdleTime());
		}
		
		
	}


	public abstract long getLocalLiveBroadcastCount(String hostAddress);

	public abstract List<Broadcast> getLocalLiveBroadcasts(String hostAddress);

	/**
	 * Below search methods and sortandcrop methods are used for getting the searched items and sorting and pagination.
	 * Sorting, search and cropping is available for Broadcasts, VoDs and Conference Rooms.
	 * They are used by InMemoryDataStore and MapDBStore, Mongodb implements the same functionality inside its own class.
	 */
	protected ArrayList<VoD> searchOnServerVod(ArrayList<VoD> broadcastList, String search){

		long startTime = System.nanoTime();
		if(search != null && !search.isEmpty()) {
			String searchLower = search.toLowerCase();
			for (Iterator<VoD> i = broadcastList.iterator(); i.hasNext(); ) {
				VoD item = i.next();
				if (matchesVodSearch(item, searchLower)) {
					continue;
				}
				i.remove();
			}
		}

		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "searchOnServerVod");
		return broadcastList;
	}

	private boolean matchesVodSearch(VoD item, String searchLower) {
		return containsIgnoreCase(item.getVodId(), searchLower) ||
				containsIgnoreCase(item.getVodName(), searchLower) ||
				containsIgnoreCase(item.getStreamId(), searchLower) ||
				containsIgnoreCase(item.getStreamName(), searchLower) ||
				containsIgnoreCase(item.getDescription(), searchLower) ||
				containsIgnoreCase(item.getMetadata(), searchLower);
	}

	private boolean containsIgnoreCase(String field, String searchLower) {
		return field != null && field.toLowerCase().contains(searchLower);
	}

	protected List<VoD> sortAndCropVodList(List<VoD> vodList, int offset, int size, String sortBy, String orderBy) 
	{
		long startTime = System.nanoTime();
		if (("name".equals(sortBy) || "date".equals(sortBy)) && orderBy != null ) 

		{
			Collections.sort(vodList, (vod1, vod2) -> 
			{
				Comparable c1 = null;
				Comparable c2 = null;
				if (sortBy.contentEquals("name")) 
				{
					c1 = vod1.getVodName().toLowerCase();
					c2 = vod2.getVodName().toLowerCase();
				} 
				else if (sortBy.contentEquals("date")) 
				{
					c1 = Long.valueOf(vod1.getCreationDate());
					c2 = Long.valueOf(vod2.getCreationDate());
				}

				int result = 0;
				if (c1 != null && c2 != null) 
				{
					if (orderBy.contentEquals("desc")) 
					{
						result = c2.compareTo(c1);
					} 
					else {
						result = c1.compareTo(c2);
					}

				}
				return result;
			});
		}

		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}

		int toIndex =  Math.min(offset+size, vodList.size());
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "sortAndCropVodList(List<VoD> vodList, int offset, int size, String sortBy, String orderBy)");
		
		if (offset >= toIndex)
		{
			return new ArrayList<>();
		}
		else {
			return vodList.subList(offset, Math.min(offset+size, vodList.size()));
		}

	}
	protected List<Broadcast> searchOnServer(List<Broadcast> broadcastList, String search){
		long startTime = System.nanoTime();
		if(search != null && !search.isEmpty()) {
			for (Iterator<Broadcast> i = broadcastList.iterator(); i.hasNext(); ) {
				Broadcast item = i.next();
				if(item.getName() != null && item.getStreamId() != null) {
					if (item.getName().toLowerCase().contains(search.toLowerCase()) || item.getStreamId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else i.remove();
				}
				else{
					if (item.getStreamId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else i.remove();
				}
			}
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "searchOnServer(ArrayList<Broadcast> broadcastList, String search){");
		return broadcastList;
	}

	protected List<Broadcast> sortAndCropBroadcastList(List<Broadcast> broadcastList, int offset, int size, String sortBy, String orderBy) {

		long startTime = System.nanoTime();
		if(("name".equals(sortBy) || "date".equals(sortBy) || "status".equals(sortBy)) && orderBy != null) 
		{
			Collections.sort(broadcastList, new Comparator<Broadcast>() {
				@Override
				public int compare(Broadcast broadcast1, Broadcast broadcast2) {
					Comparable c1 = null;
					Comparable c2 = null;

					if (sortBy.equals("name")) 
					{
						c1 = broadcast1.getName().toLowerCase();
						c2 = broadcast2.getName().toLowerCase();
					} 
					else if (sortBy.equals("date")) 
					{
						c1 = Long.valueOf(broadcast1.getDate());
						c2 = Long.valueOf(broadcast2.getDate());
					} 
					else if (sortBy.equals("status")) 
					{
						c1 = broadcast1.getStatus();
						c2 = broadcast2.getStatus();
					} 


					int result = 0;
					if (c1 != null && c2 != null) 
					{
						if (orderBy.equals("desc")) 
						{
							result = c2.compareTo(c1);
						} 
						else {
							result = c1.compareTo(c2);
						}
					}
					return result;
				}
			});
		}

		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0 ) {
			offset = 0;
		}

		int toIndex =  Math.min(offset+size, broadcastList.size());
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "sortAndCropBroadcastList");
		
		if (offset >= toIndex)
		{
			return new ArrayList<>();
		}
		else {
			return broadcastList.subList(offset,toIndex);
		}
	}

	/**
	 * Creates new P2PConnection
	 * @param conn - P2PConnection object
	 * @return boolean - success 
	 */
	public abstract boolean createP2PConnection(P2PConnection conn);

	/**
	 * Get the P2PConnection by streamId
	 * @param streamId - stream id for P2PConnection
	 * @return P2PConnection - if exist else null 
	 */
	public abstract P2PConnection getP2PConnection(String streamId);

	/**
	 * Deletes a P2PConnection
	 * @param conn - P2PConnection object
	 * @return boolean - success 
	 */
	public abstract boolean deleteP2PConnection(String streamId);

	/**
	 * @deprecated no need to use this method, logic has changed and we use directly getSubtracks, getActiveSubtracks.
	 * It's kept for backward compatibility
	 * 
	 * Add a subtrack id to a main track (broadcast)
	 * @param mainTrackId - main track id
	 * @param subTrackId - main track id
	 * @return boolean - success 
	 */
	@Deprecated(since = "2.9.1", forRemoval = true)
	public abstract boolean addSubTrack(String mainTrackId, String subTrackId);

	/**
	 * @deprecated no need to use this method, logic has changed and we use directly getSubtracks, getActiveSubtracks.
	 * It's kept for backward compatibility
	 * 
	 * Remove a subtrack id from a main track (broadcast)
	 * @param mainTrackId - main track id
	 * @param subTrackId - main track id
	 * @return boolean - success
	 */
	@Deprecated(since = "2.9.1", forRemoval = true)
	public abstract boolean removeSubTrack(String mainTrackId, String subTrackId);

	/**
	 * Resets the broadcasts in the database. 
	 * It sets number of viewers to zero. 
	 * It also delete the stream if it's zombi stream
	 *
	 * @returns total number of operation in the db
	 */
	public abstract int resetBroadcasts(String hostAddress);

	/**
	 * Return if data store is available. DataStore is available if it's initialized and not closed. 
	 * It's not available if it's closed. 
	 * @return availability of the datastore
	 */
	public boolean isAvailable() {
		return available;
	}


	/**
	 * This is used to get total number of WebRTC viewers 
	 *
	 * @returns total number of WebRTC viewers
	 */
	public abstract int getTotalWebRTCViewersCount();

	public int getTotalWebRTCViewersCount(Map<String, String> broadcastMap, Gson gson) {
		long startTime = System.nanoTime();

		long now = System.currentTimeMillis();
		if (now - totalWebRTCViewerCountLastUpdateTime > TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME) {
			int total = 0;
			synchronized (this) {
				for (String json : broadcastMap.values()) {
					Broadcast broadcast = gson.fromJson(json, Broadcast.class);
					total += broadcast.getWebRTCViewerCount();
				}
			}
			totalWebRTCViewerCount = total;
			totalWebRTCViewerCountLastUpdateTime = now;
		}
		
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getTotalWebRTCViewersCount(Map<String, String> broadcastMap, Gson gson)");
		
		return totalWebRTCViewerCount;
	}

	protected ArrayList<WebRTCViewerInfo> searchOnWebRTCViewerInfo(ArrayList<WebRTCViewerInfo> list, String search) {
		long startTime = System.nanoTime();
		if(search != null && !search.isEmpty()) {
			for (Iterator<WebRTCViewerInfo> i = list.iterator(); i.hasNext(); ) {
				WebRTCViewerInfo item = i.next();
				if(item.getViewerId() != null && !item.getViewerId().toLowerCase().contains(search.toLowerCase())) {
					i.remove();
				}
			}
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "searchOnWebRTCViewerInfo");
		return list;
	}

	protected List<WebRTCViewerInfo> sortAndCropWebRTCViewerInfoList(List<WebRTCViewerInfo> list, int offset, int size, String sortBy, String orderBy) {
		long startTime = System.nanoTime();
		if("viewerId".equals(sortBy)) 
		{
			Collections.sort(list, (viewer1, viewer2) -> {
				Comparable c1 = viewer1.getViewerId();
				Comparable c2 = viewer2.getViewerId();

				return "desc".equals(orderBy) ? c2.compareTo(c1) : c1.compareTo(c2);
			});
		}

		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0 ) {
			offset = 0;
		}

		int toIndex =  Math.min(offset+size, list.size());
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "sortAndCropWebRTCViewerInfoList");
		if (offset >= toIndex)
		{
			return new ArrayList<>();
		}
		else {
			return list.subList(offset,toIndex);
		}
	}

	/**
	 * This is used to save WebRTC Viewer Info to datastore 
	 *
	 * @param info information for the WebRTC Viewer
	 */
	public abstract void saveViewerInfo(WebRTCViewerInfo info);

	/**
	 * Get list of webrtc viewers
	 *
	 * @param offset
	 * @param size
	 * @param search 
	 * @param orderBy 
	 * @param sortBy 
	 *
	 * @return list of webrtc viewers
	 */
	public abstract List<WebRTCViewerInfo> getWebRTCViewerList(int offset, int size, String sortBy, String orderBy, String search);

	public List<WebRTCViewerInfo> getWebRTCViewerList(Map<String, String> webRTCViewerMap, int offset, int size, String sortBy, String orderBy,
			String search, Gson gson) {
		long startTime = System.nanoTime();

		ArrayList<WebRTCViewerInfo> list = new ArrayList<>();
		synchronized (this) {

			Collection<String> webRTCViewers = webRTCViewerMap.values();
			for (String infoString : webRTCViewers) {
				WebRTCViewerInfo info = gson.fromJson(infoString, WebRTCViewerInfo.class);
				list.add(info);
			}
		}
		if (search != null && !search.isEmpty()) {
			search = search.replaceAll(REPLACE_CHARS_REGEX, "_");
			logger.info("server side search called for Conference Room = {}", search);
			list = searchOnWebRTCViewerInfo(list, search);
		}
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, "getWebRTCViewerList");
		return sortAndCropWebRTCViewerInfoList(list, offset, size, sortBy, orderBy);
	}

	/**
	 * This is used to delete a WebRTC Viewer Info from datastore 
	 *
	 * @param viewerId WebRTC Viewer Id
	 */
	public abstract boolean deleteWebRTCViewerInfo(String viewerId);

	/**
	 * This is used to update meta data for a bradcast 
	 *
	 * @param streamId id for the broadcast
	 * @param metaData new meta data
	 */
	public abstract boolean updateStreamMetaData(String streamId, String metaData);

	/**
	 * Put subscriber metadata. It overwrites the metadata, if you need to update something, 
	 * first get the {@link #getSubscriberMetaData(String)} , update it and put it
	 * 
	 * @param subscriberId
	 * @param SubscriberMetadata
	 * @return 
	 */
	public abstract void putSubscriberMetaData(String subscriberId, SubscriberMetadata metadata);

	/**
	 * Get subscriber metadata
	 * @param subscriberId
	 * @return
	 */
	public abstract SubscriberMetadata getSubscriberMetaData(String subscriberId);

	/**
	 * This is a helper method to remove the ConferenceRoom in later versions
	 * 
	 * May 11, 2024 - mekya
	 * 
	 * @param broadcast
	 * @return
	 */
	public static ConferenceRoom broadcastToConference(Broadcast broadcast) {

		ConferenceRoom conferenceRoom = new ConferenceRoom();

		conferenceRoom.setRoomId(broadcast.getStreamId());
		conferenceRoom.setStartDate(broadcast.getPlannedStartDate());
		conferenceRoom.setEndDate(broadcast.getPlannedEndDate());
		conferenceRoom.setMode(broadcast.getConferenceMode());
		conferenceRoom.setZombi(broadcast.isZombi());
		conferenceRoom.setOriginAdress(broadcast.getOriginAdress());
		conferenceRoom.setRoomStreamList(broadcast.getSubTrackStreamIds());


		return conferenceRoom;
	}

	/**
	 * This is a helper method to remove the ConferenceRoom in later versions
	 * 
	 * May 11, 2024 - mekya
	 * 
	 * @param broadcast
	 * @return
	 */
	public static BroadcastUpdate conferenceUpdateToBroadcastUpdate(ConferenceRoom conferenceRoom) throws Exception {
		BroadcastUpdate broadcast = new BroadcastUpdate();
		broadcast.setStreamId(conferenceRoom.getRoomId());
		broadcast.setPlannedStartDate(conferenceRoom.getStartDate());
		broadcast.setPlannedEndDate(conferenceRoom.getEndDate());
		broadcast.setZombi(conferenceRoom.isZombi());
		broadcast.setOriginAdress(conferenceRoom.getOriginAdress());
		broadcast.setConferenceMode(conferenceRoom.getMode());
		broadcast.setSubTrackStreamIds(conferenceRoom.getRoomStreamList());

		return broadcast;

	}

	public static Broadcast conferenceToBroadcast(ConferenceRoom conferenceRoom) throws Exception {
		Broadcast broadcast = new Broadcast();
		broadcast.setStreamId(conferenceRoom.getRoomId());
		broadcast.setPlannedStartDate(conferenceRoom.getStartDate());
		broadcast.setPlannedEndDate(conferenceRoom.getEndDate());
		broadcast.setZombi(conferenceRoom.isZombi());
		broadcast.setOriginAdress(conferenceRoom.getOriginAdress());
		broadcast.setConferenceMode(conferenceRoom.getMode());
		broadcast.setSubTrackStreamIds(conferenceRoom.getRoomStreamList());

		return broadcast;

	}

	/**
	 * Move ConferenceRoom to Broadcast
	 */
	public abstract void migrateConferenceRoomsToBroadcasts();

	/**
	 * Get the subtracks of the main track
	 * @param mainTrackId the main track to get the subtracks
	 * @param offset the offset to get the subtracks
	 * @param size 	number of items to get
	 * @param role the role of the subtracks for role based streaming especially in conferences. It can be null
	 * @param status the status of the stream broadcasting, finished etc. It can be null
	 * @param sortBy can get "name" or "date" or "status" values
	 * @param orderBy can get "desc" or "asc"
	 * @param search is used for searching in streamIds and names of the stream
	 * @return
	 */
	public abstract List<Broadcast> getSubtracks(String mainTrackId, int offset, int size, String role, String status, String sortBy, String orderBy, String search);

	/**
	 * Get the subtracks of the main track
	 * @param mainTrackId the main track to get the subtracks
	 * @param offset the offset to get the subtracks
	 * @param size 	number of items to get
	 * @param role the role of the subtracks for role based streaming especially in conferences. It can be null
	 * @return
	 */
	public abstract List<Broadcast> getSubtracks(String mainTrackId, int offset, int size, String role);

	/**
	 * Get the count of subtracks
	 * @param mainTrackId the main track to get the subtracks
	 * @param role the role of the subtracks for role based streaming especially in conferences 
	 * @return number of subtracks
	 */
	public abstract long getSubtrackCount(String mainTrackId, String role, String status);

	/**
	 * Get the count of active subtracks. If subtrack is stucked in broadcasting or preparing, it will not count it. 
	 * @param mainTrackId
	 * @param role
	 * @return
	 */
	public abstract long getActiveSubtracksCount(String mainTrackId, String role);

	/**
	 * Get of active subtracks. If subtrack is stucked in broadcasting or preparing, it will not return it. 
	 * This method is generally not recommended to use because it can be very costly.
	 * It's implemented for the poll mechanism in Subtracks and poll mechanismi will be replaced with event mechanism
	 * @param mainTrackId
	 * @param role
	 * @return
	 */
	public abstract List<Broadcast> getActiveSubtracks(String mainTrackId, String role);


	/**
	 * 
	 * @param streamId
	 * @return If the stream has subtracks, it return true. If not, it returns false
	 */
	public abstract boolean hasSubtracks(String streamId);


	

	/**
	 *
	 * Get executed query count. For now only mongodb queries are counted.
	 * @return Executed query count.
	 */
	public long getExecutedQueryCount() {
		return executedQueryCount;
	}

	/**
	 * Get connection events for a specific streamId and subscriberId
	 * 
	 * ConnectionEvents are recorded if {@link AppSettings#isWriteSubscriberEventsToDatastore()} is true
	 * 
	 * @param streamId
	 * @param subscriberId 
	 * @param offset
	 * @param size
	 * @return
	 */
	public abstract List<ConnectionEvent> getConnectionEvents(String streamId, @Nullable String subscriberId, int offset, int size);
	
	/**
	 * Simple converter from Collection to List
	 * @param values
	 * @return
	 */
	protected static List<ConnectionEvent> getConnectionEventListFromCollection(Collection<ConnectionEvent> values, String streamId) {
		List<ConnectionEvent> list = new ArrayList<>();		
		
		for(ConnectionEvent event: values) {
			if (StringUtils.isBlank(streamId)) {
				list.add(event);
			}
			else if (streamId.equals(event.getStreamId())) {
				list.add(event);
			}
		}
		
		return list;
		
	}
	
	/**
	 * Setter for appSettings
	 * @param appSettings
	 */
	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}
	
	/**
	 * Calculate total query time in milliseconds
	 * @return
	 */
	public long getAverageQueryTimeMs() {
		return (totalQueryTimeNanoSec/1_000_000) / (executedQueryCount == 0 ? 1 : executedQueryCount);
	}
	
	/**
	 * Add query time in nanoseconds
	 * @param queryTimeNanoSec
	 */
	public void addQueryTime(long queryTimeNanoSec) {
		totalQueryTimeNanoSec += queryTimeNanoSec;
		executedQueryCount++;
	}
	
	public void showWarningIfElapsedTimeIsMoreThanThreshold(long elapsedNano, String methodName) {
		if (elapsedNano > QUERY_TIME_THRESHOLD_NANO_SEC) {
			logger.warn("Query execution time:{}ms is more than {} ms for method: {}", elapsedNano / 1_000_000,
					QUERY_TIME_THRESHOLD_MS_SEC, methodName);
		}
		
		if (elapsedNano > QUERY_TIME_EXTRA_LOG_THRESHOLD_NANO_SEC) {
			logger.warn(ExceptionUtils.getStackTrace(new Exception("Long Mongo Query:")));
		}
	}

	//**************************************
	//ATTENTION: Write function above with descriptions while adding new functions
	//**************************************	

	//**************************************
	//ATTENTION 2: What is the reason you don't add descriptions to the functions? 
	// Ignore this message if you have added descriptions to the new functions.
	// I'm writing to the one who is ignoring this first message - mekya
	//**************************************

}
