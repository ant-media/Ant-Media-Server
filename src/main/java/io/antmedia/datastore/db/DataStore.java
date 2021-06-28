package io.antmedia.datastore.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberStats;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;

public abstract class DataStore {


	//Do not forget to write function descriptions especially if you are adding new functions

	public static final int MAX_ITEM_IN_ONE_LIST = 50;

	private boolean writeStatsToDatastore = true;

	protected volatile boolean available = false;
	
	protected static Logger logger = LoggerFactory.getLogger(DataStore.class);


	public abstract String save(Broadcast broadcast);

	/**
	 * Return the broadcast in data store
	 * @param id
	 * @return broadcast
	 */
	public abstract Broadcast get(String id);

	/**
	 * Return the vod by id
	 * @param id
	 * @return Vod object
	 */
	public abstract VoD getVoD(String id);

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

	public abstract String addVod(VoD vod);

	public abstract long getBroadcastCount();

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

	public abstract boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl);

	public abstract List<Broadcast> getExternalStreamsList();

	public abstract void close();

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

	public abstract boolean removeAllEndpoints(String id);

	public abstract long getTotalVodNumber();

	public abstract long getTotalBroadcastNumber();

	public abstract void saveDetection(String id,long timeElapsed,List<TensorFlowObject> detectedObjects);

	public abstract List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize);

	public abstract List<TensorFlowObject> getDetection(String id);


	/**
	 * saves token to store
	 * @param token - created token
	 * @return  true/false
	 */
	public abstract boolean saveToken (Token token);


	/**
	 * Lists all tokens of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of tokens
	 */
	public abstract List<Token> listAllTokens (String streamId, int offset, int size);


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

	/**
	 * Lists all subscribers of requested stream
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of subscribers
	 */	
	public abstract List<Subscriber> listAllSubscribers(String streamId, int offset, int size);
	
	
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
		} else if(ConnectionEvent.DISCONNECTED_EVENT.equals(event.getEventType())) {
			subscriber.setConnected(false);
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
	 * Add social endpoint credentials to data store
	 * Do not add id to the credentials, it will be added by data store
	 * @param credentials
	 * The credentials that will be stored to datastore
	 *
	 * @return SocialEndpointCredentials by settings id of the credentials
	 * null if it is not saved to datastore
	 *
	 */
	public abstract SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials);

	/**
	 * Get list of social endpoints
	 *
	 * @param offset
	 * @param size
	 *
	 * @return list of social endpoints
	 */
	public abstract List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size);

	/**
	 * Remove social endpoint from data store
	 * @param id , this is the id of the credential
	 *
	 * @return true if it is removed from datastore
	 * false if it is not removed
	 */
	public abstract boolean removeSocialEndpointCredentials(String id);

	/**
	 * Return social endpoint credential that having the id
	 *
	 * @param id the id of the credential to be returns
	 * @return {@link SocialEndpointCredentials} if there is a matching credential with the id
	 * <code>null</code> if there is no matching id
	 */
	public abstract SocialEndpointCredentials getSocialEndpointCredentials(String id);

	/**
	 * Return the number of active broadcasts in the server
	 * @return
	 */
	public abstract long getActiveBroadcastCount();

	/**
	 * Updates the Broadcast objects fields if it's not null.
	 * The updated fields are as follows
	 * name, description, userName, password, IP address, streamUrl

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
	 * Returns the total number of detected objects in the stream
	 * @param id is the stream id
	 * @return total number of detected objects
	 */
	public abstract long getObjectDetectedTotal(String streamId);

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
	 * Add stream info list to db
	 * @param streamInfoList
	 */
	public abstract  void addStreamInfoList(List<StreamInfo> streamInfoList);

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
		
		broadcast.setCurrentPlayIndex(newBroadcast.getCurrentPlayIndex());
		broadcast.setReceivedBytes(newBroadcast.getReceivedBytes());
		broadcast.setDuration(newBroadcast.getDuration());
		broadcast.setBitrate(newBroadcast.getBitrate());
		broadcast.setUserAgent(newBroadcast.getUserAgent());
		broadcast.setWebRTCViewerLimit(newBroadcast.getWebRTCViewerLimit());
		broadcast.setHlsViewerLimit(newBroadcast.getHlsViewerLimit());
	}

	/**
	 * This method returns the local active broadcast count.
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

	protected List<VoD> sortAndCropVodList(List<VoD> vodList, int offset, int size, String sortBy, String orderBy) {
		if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
			if(sortBy.contentEquals("date") || sortBy.contentEquals("name")) {
				Collections.sort(vodList, new Comparator<VoD>() {
					@Override
					public int compare(VoD vod1, VoD vod2) {
						Comparable c1 = null;
						Comparable c2 = null;
						if (sortBy.contentEquals("name")) {
							c1 = vod1.getVodName().toLowerCase();
							c2 = vod2.getVodName().toLowerCase();
						} else if (sortBy.contentEquals("date")) {
							c1 = new Long(vod1.getCreationDate());
							c2 = new Long(vod2.getCreationDate());
						}
						if (orderBy.contentEquals("desc")) {
							return c2.compareTo(c1);
						} else if (orderBy != null && !(orderBy.isEmpty())) {
							//Wrong entry check to not get null pointer.
						}
						return c1.compareTo(c2);
					}
				});
			}
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
		if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty() )
		{
			if(sortBy.equals("name") || sortBy.equals("date") || sortBy.equals("status")) {
				Collections.sort(broadcastList, new Comparator<Broadcast>() {
					@Override
					public int compare(Broadcast broadcast1, Broadcast broadcast2) {
						Comparable c1 = null;
						Comparable c2 = null;

						if (sortBy.equals("name")) {
							c1 = broadcast1.getName().toLowerCase();
							c2 = broadcast2.getName().toLowerCase();
						} else if (sortBy.equals("date")) {
							c1 = new Long(broadcast1.getDate());
							c2 = new Long(broadcast2.getDate());
						} else if (sortBy.equals("status")) {
							c1 = broadcast1.getStatus();
							c2 = broadcast2.getStatus();
						} else if (sortBy != null && !(sortBy.isEmpty())) {
							//Wrong entry check to not get null pointer.
						}

						if (orderBy.equals("desc")) {
							return c2.compareTo(c1);
						} else if (orderBy != null && !(orderBy.isEmpty())) {
							//Wrong entry check to not get null pointer.
						}
						return c1.compareTo(c2);
					}
				});
			}
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
		if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty())
		{
			if(sortBy.equals("roomId") || sortBy.equals("startDate") || sortBy.equals("endDate") ) {
				Collections.sort(roomList, new Comparator<ConferenceRoom>() {
					@Override
					public int compare(ConferenceRoom room1, ConferenceRoom room2) {
						Comparable c1 = null;
						Comparable c2 = null;

						if (sortBy.equals("roomId")) {
							c1 = room1.getRoomId().toLowerCase();
							c2 = room2.getRoomId().toLowerCase();
						} else if (sortBy.equals("startDate")) {
							c1 = new Long(room1.getStartDate());
							c2 = new Long(room2.getStartDate());
						} else if (sortBy.equals("endDate")) {
							c1 = new Long(room1.getEndDate());
							c2 = new Long(room2.getEndDate());
						} else if (sortBy != null && !(sortBy.isEmpty())) {
							//Wrong entry check to not get null pointer.
						}

						if (orderBy.equals("desc")) {
							return c2.compareTo(c1);
						} else if (orderBy != null && !(orderBy.isEmpty())) {
							//Wrong entry check to not get null pointer.
						}
						return c1.compareTo(c2);
					}
				});
			}
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

//**************************************
//ATTENTION: Write function descriptions while adding new functions
//**************************************	
}
