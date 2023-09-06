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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberStats;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;

public abstract class DataStore {


	//Do not forget to write function descriptions especially if you are adding new functions

	public static final int MAX_ITEM_IN_ONE_LIST = 250;
	private static final String REPLACE_CHARS_REGEX = "[\n|\r|\t]";

	private boolean writeStatsToDatastore = true;

	protected volatile boolean available = false;

	protected static Logger logger = LoggerFactory.getLogger(DataStore.class);
	
	
	public abstract String save(Broadcast broadcast);

	public Broadcast saveBroadcast (Broadcast broadcast) {
		String streamId = null;
		try {
		if (broadcast.getStreamId() == null || broadcast.getStreamId().isEmpty()) {
			streamId = RandomStringUtils.randomAlphanumeric(16) + System.currentTimeMillis();
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
		synchronized (this) {
			Broadcast broadcast = null;
			String jsonString = null;
			if (streamId != null) {
				jsonString = broadcastMap.get(streamId);
				if(jsonString != null) {
					return gson.fromJson(jsonString, Broadcast.class);
				}
				return broadcast;
			}
		}
		return null;
	}

	/**
	 * Return the vod by id
	 * @param id
	 * @return Vod object
	 */
	public abstract VoD getVoD(String id);
	
	public VoD getVoD(Map<String, String> vodMap, String vodId, Gson gson) {
		synchronized (this) {
			if (vodId != null) {
				String jsonString = null;
				jsonString = vodMap.get(vodId);

				if (jsonString != null) {
					return gson.fromJson(jsonString, VoD.class);
				}
			}
		}
		return null;
	}

	public abstract boolean updateStatus(String id, String status);

	public static final long TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME = 5000;
	protected int totalWebRTCViewerCount = 0;
	protected long totalWebRTCViewerCountLastUpdateTime = 0;

	public boolean updateSourceQualityParameters(String id, String quality, double speed,  int pendingPacketQueue) {
		if(writeStatsToDatastore) {
			return updateSourceQualityParametersLocal(id, quality, speed, pendingPacketQueue);
		}
		return false;
	}

	protected abstract boolean updateSourceQualityParametersLocal(String id, String quality, double speed,  int pendingPacketQueue);

	public abstract boolean updateDuration(String id, long duration);

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
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	public abstract boolean delete(String id);

	public abstract boolean deleteVod(String id);

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

	/**
	 * Returns the Conference Room List in order
	 *
	 * @param offset the number of items to skip
	 * @param size batch size
	 * @param sortBy can get "name" or "startDate" or "endDate" values
	 * @param orderBy can get "desc" or "asc"
	 * @param search is used for searching in RoomId
	 * @return
	 */
	public abstract List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search);
	
	public List<ConferenceRoom> getConferenceRoomList(Map<String, String> conferenceMap, int offset, int size, String sortBy, String orderBy,
			String search, Gson gson) {
		ArrayList<ConferenceRoom> list = new ArrayList<>();
		synchronized (this) {
			Collection<String> conferenceRooms = null;
			conferenceRooms = conferenceMap.values();

			for (String roomString : conferenceRooms) {
				ConferenceRoom room = gson.fromJson(roomString, ConferenceRoom.class);
				list.add(room);
			}
		}
		if (search != null && !search.isEmpty()) {
			search = search.replaceAll(REPLACE_CHARS_REGEX, "_");
			logger.info("server side search called for Conference Room = {}", search);
			list = searchOnServerConferenceRoom(list, search);
		}
		return sortAndCropConferenceRoomList(list, offset, size, sortBy, orderBy);
	}


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
			return vods;
		}
	}


	public abstract boolean removeAllEndpoints(String id);

	public abstract long getTotalVodNumber();

	public long getTotalVodNumber(Map<String, String> broadcastMap) {
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	public abstract long getTotalBroadcastNumber();
	
	public long getTotalBroadcastNumber(Map<String,String> broadcastMap) {
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	public abstract void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects);

	public abstract List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize);

	public List<TensorFlowObject> getDetectionList(Map<String, String> detectionMap, String idFilter, int offsetSize, int batchSize, Gson gson) {

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
		return list;
	}

	public abstract List<TensorFlowObject> getDetection(String id);
	
	public List<TensorFlowObject> getDetection(Map<String, String> detectionMap, String id, Gson gson){
		synchronized (this) {
			if (id != null) {
				String jsonString = detectionMap.get(id);
				if (jsonString != null) {
					Type listType = new TypeToken<ArrayList<TensorFlowObject>>(){}.getType();
					return gson.fromJson(jsonString, listType);
				}
			}
		}
		return null;
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

	public List<Token> listAllTokens (Map<String, String> tokenMap, String streamId, int offset, int size, Gson gson) {

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

	public Token getToken(Map<String, String> tokenMap, String tokenId,
			Gson gson) {
		Token token = null;
		synchronized (this) {
			if (tokenId != null) {
				String jsonString = tokenMap.get(tokenId);
				if (jsonString != null) {
					token = gson.fromJson(jsonString, Token.class);
				}
			}
		}
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

	public List<Subscriber> listAllSubscribers(Map<String, String> subscriberMap, String streamId, int offset, int size, Gson gson) {
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

				if (subscriber.getStreamId().equals(streamId)) {
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
		List<Subscriber> subscribers= listAllSubscribers(streamId, offset, size);
		List<SubscriberStats> subscriberStats = new ArrayList<>();

		for(Subscriber subscriber : subscribers) {
			subscriberStats.add(subscriber.getStats());
		}

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
		Subscriber subscriber = null;
		synchronized (this) {
			if (subscriberId != null && streamId != null) {
				String jsonString = subscriberMap.get(Subscriber.getDBKey(streamId, subscriberId));
				if (jsonString != null) {
					subscriber = gson.fromJson(jsonString, Subscriber.class);
				}
			}
		}
		return subscriber;
	}

	/**
	 * gets the connection status of the subscriber from the datastore
	 * @param streamId
	 * @param subscriberId - id of the subscriber 
	 * @return- true if connected else false
	 */	
	public boolean isSubscriberConnected(String streamId, String subscriberId) {
		Subscriber subscriber = getSubscriber(streamId, subscriberId);

		if(subscriber != null) {
			return subscriber.isConnected();
		}
		return false;
	}

	/**
	 * sets the connection status of the subscriber in the datastore
	 * @param streamId
	 * @param subscriberId - id of the subscriber 
	 * @param event - connection event which occured for this subscriber
	 * @return- true if successful else false
	 */	
	public boolean addSubscriberConnectionEvent(String streamId, String subscriberId, ConnectionEvent event) {
		boolean result = false;
		Subscriber subscriber = getSubscriber(streamId, subscriberId);
		if (subscriber != null) {
			handleConnectionEvent(subscriber, event);

			addSubscriber(streamId, subscriber);
			result = true;
		}

		return result;
	}

	// helper method used by all datastores
	protected void handleConnectionEvent(Subscriber subscriber, ConnectionEvent event) {
		if(ConnectionEvent.CONNECTED_EVENT.equals(event.getEventType())) {
			subscriber.setConnected(true);
			subscriber.setCurrentConcurrentConnections(subscriber.getCurrentConcurrentConnections()+1);
		} else if(ConnectionEvent.DISCONNECTED_EVENT.equals(event.getEventType())) {
			subscriber.setConnected(false);
			subscriber.setCurrentConcurrentConnections(subscriber.getCurrentConcurrentConnections()-1);
		}
		subscriber.getStats().addConnectionEvent(event);
	}	

	/**
	 * sets the avarage bitrate of the subscriber in the datastore
	 * @param streamId
	 * @param subscriberId - id of the subscriber 
	 * @param event - bitrate measurement event
	 * @return- true if successful else false
	 */	
	public boolean updateSubscriberBitrateEvent(String streamId, String subscriberId,
			long avgVideoBitrate, long avgAudioBitrate) {
		boolean result = false;
		Subscriber subscriber = getSubscriber(streamId, subscriberId);
		if (subscriber != null) {	
			subscriber.getStats().setAvgVideoBitrate(avgVideoBitrate);
			subscriber.getStats().setAvgAudioBitrate(avgAudioBitrate);
			addSubscriber(streamId, subscriber);
			result = true;
		}

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

	public long getActiveBroadcastCount(Map<String, String> broadcastMap, Gson gson) {
		int activeBroadcastCount = 0;
		synchronized (this) {
			
			Collection<String> values = broadcastMap.values();
			for (String broadcastString : values) {
				Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);
				String status = broadcast.getStatus();
				if (status != null && status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) {
					activeBroadcastCount++;
				}
			}
		}
		return activeBroadcastCount;
	}

	/**
	 * Updates the Broadcast objects fields if it's not null. The updated fields are
	 * as follows name, description, userName, password, IP address, streamUrl
	 * 
	 * @param broadcast
	 * @return
	 */
	public abstract boolean updateBroadcastFields(String streamId, Broadcast broadcast);

	/**
	 * Add or subtract the HLS viewer count from current value
	 * @param streamId
	 * @param diffCount
	 */
	public boolean updateHLSViewerCount(String streamId, int diffCount) {
		if (writeStatsToDatastore) {
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
		if (writeStatsToDatastore) {
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
		return list.size();
	}

	/**
	 * Update the WebRTC viewer count
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one
	 * if it is false, decrement viewer count by one
	 */
	public boolean updateWebRTCViewerCount(String streamId, boolean increment) {
		if (writeStatsToDatastore) {
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
		if (writeStatsToDatastore) {
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

	public boolean isWriteStatsToDatastore() {
		return writeStatsToDatastore;
	}

	public void setWriteStatsToDatastore(boolean writeStatsToDatastore) {
		this.writeStatsToDatastore = writeStatsToDatastore;
	}

	/**
	 * Creates a conference room with the parameters.
	 * The room name is key so if this is called with the same room name
	 * then new room is overwritten to old one.
	 * @param room - conference room
	 * @return true if successfully created, false if not
	 */
	public abstract boolean createConferenceRoom(ConferenceRoom room);

	/**
	 * Edits previously saved conference room
	 * @param room - conference room
	 * @return true if successfully edited, false if not
	 */
	public abstract boolean editConferenceRoom(String roomId, ConferenceRoom room);

	/**
	 * Deletes previously saved conference room
	 * @param roomName- name of the conference room
	 * @return true if successfully deleted, false if not
	 */
	public abstract boolean deleteConferenceRoom(String roomId);

	/**
	 * Retrieves previously saved conference room
	 * @param roomName- name of the conference room
	 * @return room - conference room
	 */
	public abstract ConferenceRoom getConferenceRoom(String roomId);

	public ConferenceRoom getConferenceRoom(Map<String, String> conferenceRoomMap, String roomId, Gson gson) {
		synchronized (this) {
			if (roomId != null) {

				String jsonString = conferenceRoomMap.get(roomId);
				if (jsonString != null) {
					return gson.fromJson(jsonString, ConferenceRoom.class);
				}
			}
		}
		return null;
	}

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
	protected void updateStreamInfo(Broadcast broadcast, Broadcast newBroadcast)
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

		if (newBroadcast.getMainTrackStreamId() != null) {
			broadcast.setMainTrackStreamId(newBroadcast.getMainTrackStreamId());
		}

		if (newBroadcast.getStartTime() != 0) {
			broadcast.setStartTime(newBroadcast.getStartTime());
		}

		if (newBroadcast.getOriginAdress() != null) {
			broadcast.setOriginAdress(newBroadcast.getOriginAdress());
		}

		if (newBroadcast.getStatus() != null) {
			broadcast.setStatus(newBroadcast.getStatus());
		}

		if (newBroadcast.getAbsoluteStartTimeMs() != 0) {
			broadcast.setAbsoluteStartTimeMs(newBroadcast.getAbsoluteStartTimeMs());
		}		
		
		if (newBroadcast.getUpdateTime() != 0) {
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
		
		if (newBroadcast.getSpeed() != 0) {
			broadcast.setSpeed(newBroadcast.getSpeed());
		}

		if (newBroadcast.getMetaData() != null) {
			broadcast.setMetaData(newBroadcast.getMetaData());
		}

		broadcast.setCurrentPlayIndex(newBroadcast.getCurrentPlayIndex());
		broadcast.setReceivedBytes(newBroadcast.getReceivedBytes());
		broadcast.setDuration(newBroadcast.getDuration());
		broadcast.setBitrate(newBroadcast.getBitrate());
		broadcast.setUserAgent(newBroadcast.getUserAgent());
		broadcast.setWebRTCViewerLimit(newBroadcast.getWebRTCViewerLimit());
		broadcast.setHlsViewerLimit(newBroadcast.getHlsViewerLimit());
		broadcast.setSubTrackStreamIds(newBroadcast.getSubTrackStreamIds());
		broadcast.setPlaylistLoopEnabled(newBroadcast.isPlaylistLoopEnabled());
	}

	/**
	 * This method returns the local active broadcast count.ro
	 * Mongodb implementation is different because of cluster.
	 * Other implementations just return active broadcasts in db
	 * @return
	 */
	public long getLocalLiveBroadcastCount(String hostAddress) {
		return getActiveBroadcastCount();
	}

	/**
	 * Below search methods and sortandcrop methods are used for getting the searched items and sorting and pagination.
	 * Sorting, search and cropping is available for Broadcasts, VoDs and Conference Rooms.
	 * They are used by InMemoryDataStore and MapDBStore, Mongodb implements the same functionality inside its own class.
	 */
	protected ArrayList<VoD> searchOnServerVod(ArrayList<VoD> broadcastList, String search){
		if(search != null && !search.isEmpty()) {
			for (Iterator<VoD> i = broadcastList.iterator(); i.hasNext(); ) {
				VoD item = i.next();
				if(item.getVodName() != null && item.getStreamName() != null && item.getStreamId() != null && item.getVodId() != null) {
					if (item.getVodName().toLowerCase().contains(search.toLowerCase()) || item.getStreamId().toLowerCase().contains(search.toLowerCase()) || item.getStreamName().toLowerCase().contains(search.toLowerCase()) || item.getVodId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else i.remove();
				}
				else if (item.getVodName()!= null && item.getVodId() != null){
					if (item.getVodName().toLowerCase().contains(search.toLowerCase()) || item.getVodId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else i.remove();
				}
				else{
					if (item.getVodId() != null){
						if (item.getVodId().toLowerCase().contains(search.toLowerCase()))
							continue;
						else i.remove();
					}
				}
			}
		}
		return broadcastList;
	}

	protected List<VoD> sortAndCropVodList(List<VoD> vodList, int offset, int size, String sortBy, String orderBy) 
	{
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
		if (offset >= toIndex)
		{
			return new ArrayList<>();
		}
		else {
			return vodList.subList(offset, Math.min(offset+size, vodList.size()));
		}

	}
	protected ArrayList<Broadcast> searchOnServer(ArrayList<Broadcast> broadcastList, String search){
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
		return broadcastList;
	}

	protected List<Broadcast> sortAndCropBroadcastList(List<Broadcast> broadcastList, int offset, int size, String sortBy, String orderBy) {

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
		if (offset >= toIndex)
		{
			return new ArrayList<>();
		}
		else {
			return broadcastList.subList(offset,toIndex);
		}
	}

	protected ArrayList<ConferenceRoom> searchOnServerConferenceRoom(ArrayList<ConferenceRoom> roomList, String search){
		if(search != null && !search.isEmpty()) {
			for (Iterator<ConferenceRoom> i = roomList.iterator(); i.hasNext(); ) {
				ConferenceRoom item = i.next();
				if(item.getRoomId() != null) {
					if (item.getRoomId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else i.remove();
				}
			}
		}
		return roomList;
	}

	protected List<ConferenceRoom> sortAndCropConferenceRoomList(List<ConferenceRoom> roomList, int offset, int size, String sortBy, String orderBy) {
		if("roomId".equals(sortBy) || "startDate".equals(sortBy) || "endDate".equals(sortBy)) 
		{
			Collections.sort(roomList, (room1, room2) -> {
				Comparable c1 = null;
				Comparable c2 = null;

				if (sortBy.equals("roomId")) 
				{
					c1 = room1.getRoomId().toLowerCase();
					c2 = room2.getRoomId().toLowerCase();
				} 
				else if (sortBy.equals("startDate")) {
					c1 = Long.valueOf(room1.getStartDate());
					c2 = Long.valueOf(room2.getStartDate());
				} 
				else if (sortBy.equals("endDate")) {
					c1 = Long.valueOf(room1.getEndDate());
					c2 = Long.valueOf(room2.getEndDate());
				} 

				int result = 0;
				if (c1 != null && c2 != null) 
				{
					if ("desc".equals(orderBy)) {
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
		if (offset < 0 ) {
			offset = 0;
		}

		int toIndex =  Math.min(offset+size, roomList.size());
		if (offset >= toIndex)
		{
			return new ArrayList<>();
		}
		else {
			return roomList.subList(offset,toIndex);
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
	 * Add a subtrack id to a main track (broadcast)
	 * @param mainTrackId - main track id
	 * @param subTrackId - main track id
	 * @return boolean - success 
	 */
	public abstract boolean addSubTrack(String mainTrackId, String subTrackId);

	/**
	 * Remove a subtrack id from a main track (broadcast)
	 * @param mainTrackId - main track id
	 * @param subTrackId - main track id
	 * @return boolean - success
	 */
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
		return totalWebRTCViewerCount;
	}

	protected ArrayList<WebRTCViewerInfo> searchOnWebRTCViewerInfo(ArrayList<WebRTCViewerInfo> list, String search) {
		if(search != null && !search.isEmpty()) {
			for (Iterator<WebRTCViewerInfo> i = list.iterator(); i.hasNext(); ) {
				WebRTCViewerInfo item = i.next();
				if(item.getViewerId() != null && !item.getViewerId().toLowerCase().contains(search.toLowerCase())) {
					i.remove();
				}
			}
		}
		return list;
	}

	protected List<WebRTCViewerInfo> sortAndCropWebRTCViewerInfoList(List<WebRTCViewerInfo> list, int offset, int size, String sortBy, String orderBy) {
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
	

	//**************************************
	//ATTENTION: Write function descriptions while adding new functions
	//**************************************	
}
