package io.antmedia.datastore.db;

import java.io.File;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.antmedia.AntMediaApplicationAdapter;
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
import io.antmedia.muxer.MuxAdaptor;

public abstract class DataStore {

	// Do not forget to write function descriptions especially if you are adding new
	// functions

	public static final int MAX_ITEM_IN_ONE_LIST = 250;
	private static final String REPLACE_CHARS_REGEX = "[\n|\r|\t]";

	private boolean writeStatsToDatastore = true;

	protected volatile boolean available = false;

	protected static Logger logger = LoggerFactory.getLogger(DataStore.class);

	public String save(Broadcast broadcast) {
		return save(null, broadcast, null);
	}

	public String save(Map<String, String> broadcastMap, Broadcast broadcast, Gson gson) {
		if (broadcast == null) {
			return null;
		}
		try {
			String streamId = null;
			if (broadcast.getStreamId() == null || broadcast.getStreamId().isEmpty()) {
				streamId = RandomStringUtils.randomAlphanumeric(12) + System.currentTimeMillis();
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

			synchronized (this) {
				broadcastMap.put(streamId, gson.toJson(broadcast));
			}
			return streamId;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/**
	 * Return the broadcast in data store
	 * 
	 * @param id
	 * @return broadcast
	 */
	public Broadcast get(String id) {
		return get(null, id, null);
	}

	public Broadcast get(Map<String, String> broadcastMap, String streamId, Gson gson) {
		synchronized (this) {
			if (streamId != null) {
				String jsonString = null;
				jsonString = broadcastMap.get(streamId);
				if (jsonString != null) {
					return gson.fromJson(jsonString, Broadcast.class);
				}
			}
		}
		return null;
	}

	/**
	 * Return the vod by id
	 * 
	 * @param id
	 * @return Vod object
	 */
	public VoD getVoD(String id) {
		return getVoD(null, id, null);
	}

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

	public boolean updateStatus(String id, String status) {
		return updateStatus(null, id, status, null);
	}

	public boolean updateStatus(Map<String, String> broadcastMap, String streamId, String status, Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {

				Broadcast broadcast = getBroadcastFromMap(broadcastMap, streamId, gson);

				if (broadcast != null) {

					broadcast.setStatus(status);
					if (status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) {
						broadcast.setStartTime(System.currentTimeMillis());
					} else if (status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED)) {
						broadcast.setRtmpViewerCount(0);
						broadcast.setWebRTCViewerCount(0);
						broadcast.setHlsViewerCount(0);
						broadcast.setDashViewerCount(0);
					}

					setBroadcastToMap(broadcastMap, broadcast, streamId, gson);
					
					result = true;
				}
			}
		}
		return result;
	}

	public static final long TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME = 5000;
	protected int totalWebRTCViewerCount = 0;
	protected long totalWebRTCViewerCountLastUpdateTime = 0;

	public boolean updateSourceQualityParameters(String id, String quality, double speed, int pendingPacketQueue) {
		if (writeStatsToDatastore) {
			return updateSourceQualityParametersLocal(id, quality, speed, pendingPacketQueue);
		}
		return false;
	}

	protected boolean updateSourceQualityParametersLocal(String id, String quality, double speed, int pendingPacketQueue) {
		return updateSourceQualityParametersLocal(null, id, quality, speed, pendingPacketQueue, null);
	}
	
	protected boolean updateSourceQualityParametersLocal(Map<String, String> broadcastMap, String id, String quality, double speed, int pendingPacketQueue, Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				String jsonString = broadcastMap.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setSpeed(speed);
					if (quality != null) {
						broadcast.setQuality(quality);
					}
					broadcast.setPendingPacketSize(pendingPacketQueue);
					broadcastMap.replace(id, gson.toJson(broadcast));

					result = true;

				}
			}
		}
		return result;
	}

	public boolean updateDuration(String id, long duration) {
		return updateDuration(null, id, duration, null);
	}

	public Broadcast getBroadcastFromMap(Map<String, String> broadcastMap, String streamId, Gson gson) {
		Broadcast broadcast = null;
		String jsonString = null;

		jsonString = broadcastMap.get(streamId);
		
		broadcast = gson.fromJson(jsonString, Broadcast.class);

		return broadcast;
	}

	public boolean updateDuration(Map<String, String> broadcastMap, String streamId, long duration, Gson gson) {

		boolean result = false;
		synchronized (this) {
			if (streamId != null) {

				Broadcast broadcast = getBroadcastFromMap(broadcastMap, streamId, gson);
				
				if (broadcast != null) {
					broadcast.setDuration(duration);
					setBroadcastToMap(broadcastMap, broadcast, streamId, gson);
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * Returns the number of vods which contains searched string
	 * 
	 * @param search is used for searching in vodIds and names of the vods
	 * @return
	 */
	public abstract long getPartialVodNumber(String search);

	/**
	 * Returns the number of broadcasts which contains searched string
	 * 
	 * @param search is used for searching in streamIds and names of the stream
	 * @return
	 */
	public abstract long getPartialBroadcastNumber(String search);

	public boolean addEndpoint(String id, Endpoint endpoint) {
		return addEndpoint(null, id, endpoint, null);
	}

	public boolean addEndpoint(Map<String, String> broadcastMap, String streamId, Endpoint endpoint, Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null && endpoint != null) {

				Broadcast broadcast = getBroadcastFromMap(broadcastMap, streamId, gson);
				if (broadcast != null) {

					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList == null) {
						endPointList = new ArrayList<>();
					}

					endPointList.add(endpoint);
					broadcast.setEndPointList(endPointList);
					setBroadcastToMap(broadcastMap, broadcast, streamId, gson);
					result = true;
				}
			}
		}
		return result;
	}

	public long getBroadcastCount() {
		return getBroadcastCount(null);
	}
	
	public long getBroadcastCount(Map<String,String> broadcastMap) {
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	public boolean delete(String id) {
		return delete(null, id);
	}

	public boolean delete(Map<String, String> broadcastMap, String id) {
		boolean result = false;
		synchronized (this) {			
			result = broadcastMap.remove(id) != null;
		}
		return result;
	}

	public boolean deleteVod(String id) {
		return deleteVod(null, id);
	}

	public boolean deleteVod(Map<String, String> vodMap, String vodId) {
		boolean result = false;

		synchronized (this) {
			result = vodMap.remove(vodId) != null;
		}
		return result;
	}
	
	/**
	 * Returns the Broadcast List in order
	 *
	 * @param offset  the number of items to skip
	 * @param size    batch size
	 * @param type    can get "liveStream" or "ipCamera" or "streamSource" or "VoD"
	 *                values. Default is getting all broadcast types.
	 * @param sortBy  can get "name" or "date" or "status" values
	 * @param orderBy can get "desc" or "asc"
	 * @param search  is used for searching in streamIds and names of the stream
	 * @return
	 */
	public abstract List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy,
			String search);

	/**
	 * Returns the Conference Room List in order
	 *
	 * @param offset  the number of items to skip
	 * @param size    batch size
	 * @param sortBy  can get "name" or "startDate" or "endDate" values
	 * @param orderBy can get "desc" or "asc"
	 * @param search  is used for searching in RoomId
	 * @return
	 */
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy,
			String search) {
		return getConferenceRoomList(null, offset, size, sortBy, orderBy, search, null);
	}

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

	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		return removeEndpoint(null, id, endpoint, checkRTMPUrl, null);
	}

	public boolean removeEndpoint(Map<String, String> broadcastMap, String streamId, Endpoint endpoint, boolean checkRTMPUrl,
			Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null && endpoint != null) {
				
				Broadcast broadcast = getBroadcastFromMap(broadcastMap, streamId, gson);
				if (broadcast != null) {

					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList != null) {
						for (Iterator<Endpoint> iterator = endPointList.iterator(); iterator.hasNext();) {
							Endpoint endpointItem = iterator.next();
							if (checkRTMPUrl) {
								if (endpointItem.getRtmpUrl().equals(endpoint.getRtmpUrl())) {
									iterator.remove();
									result = true;
									break;
								}
							} else if (endpointItem.getEndpointServiceId().equals(endpoint.getEndpointServiceId())) {
								iterator.remove();
								result = true;
								break;
							}
						}

						if (result) {
							broadcast.setEndPointList(endPointList);
							setBroadcastToMap(broadcastMap, broadcast, streamId, gson);
						}
					}
				}
			}
		}
		return result;
	}
	
	public void setBroadcastToMap(Map<String, String> broadcastMap, Broadcast broadcast, String streamId, Gson gson){
		
		String jsonVal = gson.toJson(broadcast);
		String previousValue = null;

		previousValue = broadcastMap.replace(streamId, jsonVal);
		
		streamId = streamId.replaceAll(REPLACE_CHARS_REGEX, "_");
		logger.debug("replacing id {} having value {} to {}", streamId,
				previousValue, jsonVal);

	}

	public List<Broadcast> getBroadcastListV2(Map<String, String> broadcastMap, String type, String search, Gson gson) {
		ArrayList<Broadcast> list = new ArrayList<>();
		synchronized (this) {

			if (type != null && !type.isEmpty()) {
				for (String broadcastString : broadcastMap.values()) {
					Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);

					if (broadcast.getType().equals(type)) {
						list.add(broadcast);
					}
				}
			} else {
				for (String broadcastString : broadcastMap.values()) {
					Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);
					list.add(broadcast);
				}
			}
		}
		if (search != null && !search.isEmpty()) {
			search = search.replaceAll(REPLACE_CHARS_REGEX, "_");
			logger.info("server side search called for Broadcast searchString = {}", search);
			list = searchOnServer(list, search);
		}
		return list;
	}

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

	public String addVod(VoD vod) {
		return addVod(null, vod, null);
	}

	public String addVod(Map<String, String> vodMap, VoD vod, Gson gson) {

		String id = null;
		synchronized (this) {
			try {
				if (vod.getVodId() == null) {
					vod.setVodId(RandomStringUtils.randomNumeric(24));
				}
				id = vod.getVodId();
				vodMap.put(vod.getVodId(), gson.toJson(vod));

				logger.warn("VoD is saved to DB {} with voID {}", vod.getVodName(), id);

			} catch (Exception e) {
				logger.error(e.getMessage());
				id = null;
			}

		}
		return id;
	}

	public List<Broadcast> getExternalStreamsList(){
		return getExternalStreamsList(null, null);
	}
	/*
	public List<Broadcast> getExternalStreamsList(Map<String,String> broadcastMap, Gson gson){
		List<Broadcast> streamsList = new ArrayList<>();

		synchronized (this) {

			Object[] objectArray = broadcastMap.values().toArray();
			Broadcast[] broadcastArray = new Broadcast[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);
			}

			for (int i = 0; i < broadcastArray.length; i++) {
				String type = broadcastArray[i].getType();
				String status = broadcastArray[i].getStatus();

				if ((type.equals(AntMediaApplicationAdapter.IP_CAMERA) || type.equals(AntMediaApplicationAdapter.STREAM_SOURCE)) && (!status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING) && !status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING)) ) {
					streamsList.add(gson.fromJson((String) objectArray[i], Broadcast.class));
					broadcastArray[i].setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
					broadcastMap.replace(broadcastArray[i].getStreamId(), gson.toJson(broadcastArray[i]));
				}
			}
		}
		return streamsList;
	}
	*/
	
	public List<Broadcast> getExternalStreamsList(Map<String,String> broadcastMap, Gson gson){
		Collection<String> values = broadcastMap.values();

		List<Broadcast> streamsList = new ArrayList<>();
		for (String broadcastObject : values) {
			Broadcast broadcast = gson.fromJson(broadcastObject, Broadcast.class);
			String type = broadcast.getType();
			String status = broadcast.getStatus();

			if ((type.equals(AntMediaApplicationAdapter.IP_CAMERA) || type.equals(AntMediaApplicationAdapter.STREAM_SOURCE)) && (!status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING) && !status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING)) ) {
				streamsList.add(broadcast);
				broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING);
				broadcastMap.replace(broadcast.getStreamId(), gson.toJson(broadcast));
			}
		}
		return streamsList;
	}

	/**
	 * Closes the database
	 * 
	 * @param deleteDB if it's true, it also deletes the db and closes
	 */
	public abstract void close(boolean deleteDB);

	/**
	 * Returns the VoD List in order
	 *
	 * @param offset:        the number of items to skip
	 * @param size:          batch size
	 * @param sortBy         can get "name" or "date" values
	 * @param orderBy        can get "desc" or "asc"
	 * @param filterStreamId is used for filtering the vod by stream id. If it's
	 *                       null or empty, it's not used
	 * @param search         is used for searching in vodIds and names of the vods.
	 * @return
	 */
	public abstract List<VoD> getVodList(int offset, int size, String sortBy, String orderBy, String filterStreamId,
			String search);

	public boolean removeAllEndpoints(String id) {
		return removeAllEndpoints(null, id, null);
	}

	public boolean removeAllEndpoints(Map<String, String> broadcastMap, String streamId,
			Gson gson) {
		boolean result = false;
		synchronized (this) {
			Broadcast broadcast = getBroadcastFromMap(broadcastMap, streamId, gson);
			if (broadcast != null) {
				broadcast.setEndPointList(null);
				setBroadcastToMap(broadcastMap, broadcast, streamId, gson);
				result = true;
			}
		}
		return result;
	}

	public long getTotalVodNumber() {
		return getTotalVodNumber(null);
	}

	public long getTotalVodNumber(Map<String, String> broadcastMap) {
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	public long getTotalBroadcastNumber() {
		return getTotalBroadcastNumber(null);
	}
	
	public long getTotalBroadcastNumber(Map<String,String> broadcastMap) {
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		saveDetection(null, id, timeElapsed, detectedObjects, null);
	}

	public void saveDetection(Map<String, String> detectionMap, String id, long timeElapsed, List<TensorFlowObject> detectedObjects, Gson gson) {
		synchronized (this) {
			try {
				if (detectedObjects != null) {
					for (TensorFlowObject tensorFlowObject : detectedObjects) {
						tensorFlowObject.setDetectionTime(timeElapsed);
					}
					detectionMap.put(id, gson.toJson(detectedObjects));

				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
	}

	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		return getDetectionList(null, idFilter, offsetSize, batchSize, null);
	}

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

	public List<TensorFlowObject> getDetection(String id){
		return getDetection(null, id, null);
	}
	
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
	 * 
	 * @param token - created token
	 * @return true/false
	 */
	public boolean saveToken(Token token) {
		return saveToken(null, token, null);
	}

	public boolean saveToken(Map<String, String> tokenMap, Token token, Gson gson) {
		boolean result = false;

		synchronized (this) {

			if (token.getStreamId() != null && token.getTokenId() != null) {

				try {
					tokenMap.put(token.getTokenId(), gson.toJson(token));
					result = true;
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}

		return result;
	}

	/**
	 * Lists all tokens of requested stream
	 * 
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of tokens
	 */
	public List<Token> listAllTokens(String streamId, int offset, int size) {
		return listAllTokens(null, streamId, offset, size, null);
	}

	public List<Token> listAllTokens(Map<String, String> tokenMap, String streamId, int offset, int size, Gson gson) {

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
	 * 
	 * @param token
	 * @param streamId
	 * @return token if validated, null if not
	 */
	public Token validateToken(Token token) {
		return validateToken(null, token, null);
	}

	public Token validateToken(Map<String, String> tokenMap, Token token,
			Gson gson) {
		Token fetchedToken = null;

		synchronized (this) {
			if (token.getTokenId() != null) {
				String jsonToken = tokenMap.get(token.getTokenId());
				if (jsonToken != null) {
					fetchedToken = gson.fromJson(jsonToken, Token.class);

					if (fetchedToken.getType().equals(token.getType())
							&& Instant.now().getEpochSecond() < fetchedToken.getExpireDate()) {

						if (token.getRoomId() == null || token.getRoomId().isEmpty()) {
							if (fetchedToken.getStreamId().equals(token.getStreamId())) {

								tokenMap.remove(token.getTokenId());

							} else {
								fetchedToken = null;
							}
						}
						return fetchedToken;
					} else {
						fetchedToken = null;
					}
				}
			}
		}

		return fetchedToken;
	}

	/**
	 * Delete all tokens of the stream
	 * 
	 * @param streamId
	 */

	public boolean revokeTokens(String streamId) {
		return revokeTokens(null, streamId, null);
	}

	public boolean revokeTokens(Map<String, String> tokenMap, String streamId, Gson gson) {
		boolean result = false;

		synchronized (this) {
			Object[] objectArray = tokenMap.values().toArray();
			Token[] tokenArray = new Token[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				tokenArray[i] = gson.fromJson((String) objectArray[i], Token.class);
			}

			for (int i = 0; i < tokenArray.length; i++) {
				if (tokenArray[i].getStreamId().equals(streamId)) {
					result = tokenMap.remove(tokenArray[i].getTokenId()) != null;
					if (!result) {
						break;
					}
				}

			}
		}
		return result;
	}

	/**
	 * Delete specific token
	 * 
	 * @param tokenId id of the token
	 */

	public boolean deleteToken(String tokenId) {
		return deleteToken(null, tokenId);
	}

	public boolean deleteToken(Map<String, String> tokenMap, String tokenId) {
		boolean result = false;

		synchronized (this) {
			result = tokenMap.remove(tokenId) != null;
		}
		return result;
	}

	/**
	 * retrieve specific token
	 * 
	 * @param tokenId id of the token
	 */

	public Token getToken(String tokenId) {
		return getToken(null, tokenId, null);
	}

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
	 * 
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of subscribers
	 */
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
		return listAllSubscribers(null, streamId, offset, size, null);
	}

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
	 * 
	 * @param streamId
	 * @param offset
	 * @param size
	 * @return lists of subscriber statistics
	 */
	public List<SubscriberStats> listAllSubscriberStats(String streamId, int offset, int size) {
		List<Subscriber> subscribers = listAllSubscribers(streamId, offset, size);
		List<SubscriberStats> subscriberStats = new ArrayList<>();

		for (Subscriber subscriber : subscribers) {
			subscriberStats.add(subscriber.getStats());
		}

		return subscriberStats;
	}

	/**
	 * adds subscriber to the datastore for this stream
	 * 
	 * @param streamId
	 * @param subscriber - subscriber to be added
	 * @return- true if set, false if not
	 */
	public boolean addSubscriber(String streamId, Subscriber subscriber) {
		return addSubscriber(null, streamId, subscriber, null);
	}

	public boolean addSubscriber(Map<String, String> subscriberMap, String streamId, Subscriber subscriber, Gson gson) {
		boolean result = false;

		if (subscriber != null) {
			synchronized (this) {

				if (subscriber.getStreamId() != null && subscriber.getSubscriberId() != null) {
					try {
						subscriberMap.put(subscriber.getSubscriberKey(), gson.toJson(subscriber));

						result = true;
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		}

		return result;
	}

	/**
	 * deletes subscriber from the datastore for this stream
	 * 
	 * @param streamId
	 * @param subscriberId - id of the subsciber to be deleted
	 * @return- true if set, false if not
	 */
	public boolean deleteSubscriber(String streamId, String subscriberId) {
		return deleteSubscriber(null, streamId, subscriberId);
	}

	public boolean deleteSubscriber(Map<String, String> subscriberMap, String streamId, String subscriberId) {
		boolean result = false;

		synchronized (this) {
			try {
				result = subscriberMap.remove(Subscriber.getDBKey(streamId, subscriberId)) != null;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return result;
	}

	/**
	 * deletes all subscriber from the datastore for this stream
	 * 
	 * @param streamId
	 * @return- true if set, false if not
	 */
	public boolean revokeSubscribers(String streamId) {
		return revokeSubscribers(null, streamId, null);
	}

	public boolean revokeSubscribers(Map<String, String> subscriberMap, String streamId, Gson gson) {
		boolean result = false;

		synchronized (this) {
			Object[] objectArray = subscriberMap.values().toArray();
			Subscriber[] subscriberArray = new Subscriber[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				subscriberArray[i] = gson.fromJson((String) objectArray[i], Subscriber.class);
			}

			for (int i = 0; i < subscriberArray.length; i++) {
				String subscriberStreamId = subscriberArray[i].getStreamId();
				if (subscriberStreamId != null && subscriberStreamId.equals(streamId)) {
					result = subscriberMap.remove(subscriberArray[i].getSubscriberKey()) != null;
					if (!result) {
						break;
					}
				}

			}
		}

		return result;
	}

	/**
	 * gets subscriber from the datastore
	 * 
	 * @param streamId
	 * @param subscriberId - id of the subsciber to be deleted
	 * @return- Subscriber
	 */
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		return getSubscriber(null, streamId, subscriberId, null);
	}

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
	 * 
	 * @param streamId
	 * @param subscriberId - id of the subscriber
	 * @return- true if connected else false
	 */
	public boolean isSubscriberConnected(String streamId, String subscriberId) {
		Subscriber subscriber = getSubscriber(streamId, subscriberId);

		if (subscriber != null) {
			return subscriber.isConnected();
		}
		return false;
	}

	/**
	 * sets the connection status of the subscriber in the datastore
	 * 
	 * @param streamId
	 * @param subscriberId - id of the subscriber
	 * @param event        - connection event which occured for this subscriber
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
		if (ConnectionEvent.CONNECTED_EVENT.equals(event.getEventType())) {
			subscriber.setConnected(true);
		} else if (ConnectionEvent.DISCONNECTED_EVENT.equals(event.getEventType())) {
			subscriber.setConnected(false);
		}
		subscriber.getStats().addConnectionEvent(event);
	}

	/**
	 * sets the avarage bitrate of the subscriber in the datastore
	 * 
	 * @param streamId
	 * @param subscriberId - id of the subscriber
	 * @param event        - bitrate measurement event
	 * @return- true if successful else false
	 */
	public boolean updateSubscriberBitrateEvent(String streamId, String subscriberId, long avgVideoBitrate,
			long avgAudioBitrate) {
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
	 * 
	 * @return- true if successful else false
	 */
	public boolean resetSubscribersConnectedStatus() {
		return resetSubscribersConnectedStatus(null, null);
	}

	public boolean resetSubscribersConnectedStatus(Map<String, String> subscriberMap,  Gson gson) {
		synchronized (this) {
			try {
				Collection<String> subcribersRaw = subscriberMap.values();

				for (String subscriberRaw : subcribersRaw) {
					if (subscriberRaw != null) {
						Subscriber subscriber = gson.fromJson(subscriberRaw, Subscriber.class);
						if (subscriber != null) {
							subscriber.setConnected(false);
							subscriberMap.put(subscriber.getSubscriberKey(), gson.toJson(subscriber));
						}
					}
				}

				return true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				return false;
			}
		}
	}

	/**
	 * enables or disables mp4 muxing for the stream
	 * 
	 * @param streamId- id of the stream
	 * @param enabled   1 means enabled, -1 means disabled, 0 means no setting for
	 *                  the stream
	 * @return- true if set, false if not
	 */
	public boolean setMp4Muxing(String streamId, int enabled) {
		return setMp4Muxing(null, streamId, enabled, null);
	}

	public boolean setMp4Muxing(Map<String, String> broadcastMap, String streamId, int enabled, Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {

				String jsonString = broadcastMap.get(streamId);
				if (jsonString != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {

					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setMp4Enabled(enabled);
					broadcastMap.replace(streamId, gson.toJson(broadcast));

					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * enables or disables WebM muxing for the stream
	 * 
	 * @param streamId- id of the stream
	 * @param enabled   1 means enabled, -1 means disabled, 0 means no setting for
	 *                  the stream
	 * @return- true if set, false if not
	 */
	public boolean setWebMMuxing(String streamId, int enabled) {
		return setWebMMuxing(null, streamId, enabled, null);
	}

	public boolean setWebMMuxing(Map<String, String> broadcastMap, String streamId, int enabled, Gson gson) {

		boolean result = false;
		synchronized (this) {
			if (streamId != null) {
				String jsonString = broadcastMap.get(streamId);
				if (jsonString != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {

					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setWebMEnabled(enabled);
					broadcastMap.replace(streamId, gson.toJson(broadcast));

					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * Gets the video files under the {@code fileDir} directory parameter and saves
	 * them to the datastore as USER_VOD in {@code Vod} class
	 * 
	 * @param file
	 * @return number of files that are saved to datastore
	 */
	public int fetchUserVodList(File filedir) {
		return fetchUserVodList(null, filedir, null, null);
	}

	public int fetchUserVodList(Map<String, String> vodMap, File filedir,
			Gson gson, String dbName) {

		if (filedir == null) {
			return 0;
		}

		int numberOfSavedFiles = 0;

		synchronized (this) {
			int i = 0;

			Collection<String> vodFiles = new ArrayList<>();

			if (vodMap != null) {
				vodFiles = vodMap.values();
			}

			int size = vodFiles.size();

			List<VoD> vodList = new ArrayList<>();

			for (String vodString : vodFiles) {
				i++;
				vodList.add(gson.fromJson(vodString, VoD.class));
				if (i > size) {
					logger.error("Inconsistency in DB. It's likely db file({}) is damaged", dbName);
					break;
				}
			}

			boolean result = false;
			for (VoD vod : vodList) {
				if (vod.getType().equals(VoD.USER_VOD)) {
					if (vodMap != null) {
						result = vodMap.remove(vod.getVodId()) != null;
					}

					if (!result) {
						logger.error("MapDB VoD is not synchronized. It's likely db files({}) is damaged", dbName);
					}
				}
			}

			File[] listOfFiles = filedir.listFiles();

			if (listOfFiles != null) {
				for (File file : listOfFiles) {

					String fileExtension = FilenameUtils.getExtension(file.getName());

					if (file.isFile() && ("mp4".equals(fileExtension) || "flv".equals(fileExtension)
							|| "mkv".equals(fileExtension))) {

						long fileSize = file.length();
						long unixTime = System.currentTimeMillis();

						String path = file.getPath();

						String[] subDirs = path.split(Pattern.quote(File.separator));
						Integer pathLength = Integer.valueOf(subDirs.length);
						String relativePath = "streams/" + subDirs[pathLength - 2] + '/' + subDirs[pathLength - 1];

						String vodId = RandomStringUtils.randomNumeric(24);

						VoD newVod = new VoD("vodFile", "vodFile", relativePath, file.getName(), unixTime, 0, 0,
								fileSize, VoD.USER_VOD, vodId, null);
						addVod(newVod);
						numberOfSavedFiles++;
					}
				}
			}
		}

		return numberOfSavedFiles;
	}

	/**
	 * Return the number of active broadcasts in the server
	 * 
	 * @return
	 */
	public long getActiveBroadcastCount() {
		return getActiveBroadcastCount(null, null);
	}

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
	public boolean updateBroadcastFields(String streamId, Broadcast broadcast) {
		return updateBroadcastFields(null, streamId, broadcast, null);
	}

	public boolean updateBroadcastFields(Map<String, String> broadcastMap, String streamId, Broadcast broadcast, Gson gson) {
		boolean result = false;
		synchronized (this) {
			try {
				logger.debug("inside of updateBroadcastFields {}", broadcast.getStreamId());
				Broadcast oldBroadcast = get(streamId);
				if (oldBroadcast != null) {
					updateStreamInfo(oldBroadcast, broadcast);
					broadcastMap.replace(streamId, gson.toJson(oldBroadcast));

					result = true;
				}
			} catch (Exception e) {
				result = false;
			}
		}

		logger.debug("result inside updateBroadcastFields:{} ", result);
		return result;
	}

	/**
	 * Add or subtract the HLS viewer count from current value
	 * 
	 * @param streamId
	 * @param diffCount
	 */
	public boolean updateHLSViewerCount(String streamId, int diffCount) {
		if (writeStatsToDatastore) {
			return updateHLSViewerCountLocal(streamId, diffCount);
		}
		return false;
	}

	protected boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		return updateHLSViewerCountLocal(null, streamId, diffCount, null);
	}

	protected boolean updateHLSViewerCountLocal(Map<String, String> broadcastMap, String streamId, int diffCount, Gson gson) {
		boolean result = false;
		synchronized (this) {

			if (streamId != null) {
				Broadcast broadcast = get(streamId);
				if (broadcast != null) {
					int hlsViewerCount = broadcast.getHlsViewerCount();
					hlsViewerCount += diffCount;
					broadcast.setHlsViewerCount(hlsViewerCount);
					
					broadcastMap.replace(streamId, gson.toJson(broadcast));
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * Add or subtract the DASH viewer count from current value
	 * 
	 * @param streamId
	 * @param diffCount
	 */
	public boolean updateDASHViewerCount(String streamId, int diffCount) {
		if (writeStatsToDatastore) {
			return updateDASHViewerCountLocal(streamId, diffCount);
		}
		return false;
	}

	protected boolean updateDASHViewerCountLocal(String streamId, int diffCount) {
		return updateDASHViewerCountLocal(null, streamId, diffCount, null);
	}

	protected boolean updateDASHViewerCountLocal(Map<String, String> broadcastMap, String streamId, int diffCount, Gson gson) {
		boolean result = false;
		synchronized (this) {

			if (streamId != null) {
				Broadcast broadcast = get(streamId);
				if (broadcast != null) {
					
					int dashViewerCount = broadcast.getDashViewerCount();
					dashViewerCount += diffCount;
					broadcast.setDashViewerCount(dashViewerCount);
					broadcastMap.replace(streamId, gson.toJson(broadcast));
					result = true;
				}
			}
		}
		return result;
	}

	/**
	 * Returns the total number of detected objects in the stream
	 * 
	 * @param id is the stream id
	 * @return total number of detected objects
	 */
	public long getObjectDetectedTotal(String streamId) {
		return getObjectDetectedTotal(null, streamId, null);
	}

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
	 * 
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one if it is false,
	 *                  decrement viewer count by one
	 */
	public boolean updateWebRTCViewerCount(String streamId, boolean increment) {
		if (writeStatsToDatastore) {
			return updateWebRTCViewerCountLocal(streamId, increment);
		}
		return false;
	}

	protected boolean updateWebRTCViewerCountLocal(String streamId, boolean increment) {
		return updateWebRTCViewerCountLocal(null, streamId, increment, null);
	}

	protected boolean updateWebRTCViewerCountLocal(Map<String, String> broadcastMap, String streamId, boolean increment, Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {
				Broadcast broadcast = get(streamId);
				if (broadcast != null) {
					
					int webRTCViewerCount = broadcast.getWebRTCViewerCount();
					if (increment) {
						webRTCViewerCount++;
					} else {
						webRTCViewerCount--;
					}
					if (webRTCViewerCount >= 0) {
						broadcast.setWebRTCViewerCount(webRTCViewerCount);
						broadcastMap.replace(streamId, gson.toJson(broadcast));
						result = true;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Update the RTMP viewer count
	 * 
	 * @param streamId
	 * @param increment if it is true, increment viewer count by one if it is false,
	 *                  decrement viewer count by one
	 */
	public boolean updateRtmpViewerCount(String streamId, boolean increment) {
		if (writeStatsToDatastore) {
			return updateRtmpViewerCountLocal(streamId, increment);
		}
		return false;
	}

	protected boolean updateRtmpViewerCountLocal(String streamId, boolean increment) {
		return updateRtmpViewerCountLocal(null, streamId, increment, null);
	}

	protected boolean updateRtmpViewerCountLocal(Map<String, String> broadcastMap, String streamId, boolean increment, Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {
				Broadcast broadcast = get(streamId);
				if (broadcast != null) {
					int rtmpViewerCount = broadcast.getRtmpViewerCount();
					if (increment) {
						rtmpViewerCount++;
					} else {
						rtmpViewerCount--;
					}
					if (rtmpViewerCount >= 0) {
						broadcast.setRtmpViewerCount(rtmpViewerCount);
						broadcastMap.replace(streamId, gson.toJson(broadcast));
						result = true;
					}
				}
			}
		}
		return result;
	}

	/**
	 * Saves the stream info to the db
	 * 
	 * @param streamInfo
	 */
	public abstract void saveStreamInfo(StreamInfo streamInfo);

	/**
	 * Add stream info list to db
	 * 
	 * @param streamInfoList
	 */
	public abstract void addStreamInfoList(List<StreamInfo> streamInfoList);

	/**
	 * Returns stream info list added to db
	 * 
	 * @param streamId
	 * @return
	 */
	public abstract List<StreamInfo> getStreamInfoList(String streamId);

	/**
	 * Remove the stream info list from db
	 * 
	 * @param streamId
	 */
	public abstract void clearStreamInfoList(String streamId);

	public boolean isWriteStatsToDatastore() {
		return writeStatsToDatastore;
	}

	public void setWriteStatsToDatastore(boolean writeStatsToDatastore) {
		this.writeStatsToDatastore = writeStatsToDatastore;
	}

	/**
	 * Creates a conference room with the parameters. The room name is key so if
	 * this is called with the same room name then new room is overwritten to old
	 * one.
	 * 
	 * @param room - conference room
	 * @return true if successfully created, false if not
	 */
	public boolean createConferenceRoom(ConferenceRoom room) {
		return createConferenceRoom(null, room, null);
	}

	public boolean createConferenceRoom(Map<String, String> conferenceRoomMap, ConferenceRoom room, Gson gson) {
		synchronized (this) {
			boolean result = false;

			if (room != null && room.getRoomId() != null) {
				conferenceRoomMap.put(room.getRoomId(), gson.toJson(room));
				result = true;
			}

			return result;
		}
	}

	/**
	 * Edits previously saved conference room
	 * 
	 * @param room - conference room
	 * @return true if successfully edited, false if not
	 */
	public boolean editConferenceRoom(String roomId, ConferenceRoom room) {
		return editConferenceRoom(null, roomId, room, null);
	}

	public boolean editConferenceRoom(Map<String, String> conferenceRoomMap, String roomId, ConferenceRoom room, Gson gson) {
		synchronized (this) {
			boolean result = false;

			if (roomId != null && room != null && room.getRoomId() != null) {
				result = conferenceRoomMap.replace(roomId, gson.toJson(room)) != null;
			}
			return result;
		}
	}

	/**
	 * Deletes previously saved conference room
	 * 
	 * @param roomName- name of the conference room
	 * @return true if successfully deleted, false if not
	 */
	public boolean deleteConferenceRoom(String roomId) {
		return deleteConferenceRoom(null, roomId);
	}

	public boolean deleteConferenceRoom(Map<String, String> conferenceRoomMap, String roomId) {
		synchronized (this) {
			boolean result = false;

			if (roomId != null && !roomId.isEmpty()) {

				result = conferenceRoomMap.remove(roomId) != null;
			}
			return result;
		}
	}

	/**
	 * Retrieves previously saved conference room
	 * 
	 * @param roomName- name of the conference room
	 * @return room - conference room
	 */
	public ConferenceRoom getConferenceRoom(String roomId) {
		return getConferenceRoom(null, roomId, null);
	}

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
	 * 
	 * @param broadcast
	 * @param name
	 * @param description
	 * @param userName
	 * @param password
	 * @param ipAddr
	 * @param streamUrl
	 */
	protected void updateStreamInfo(Broadcast broadcast, Broadcast newBroadcast) {
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
	 * This method returns the local active broadcast count.ro Mongodb
	 * implementation is different because of cluster. Other implementations just
	 * return active broadcasts in db
	 * 
	 * @return
	 */
	public long getLocalLiveBroadcastCount(String hostAddress) {
		return getActiveBroadcastCount();
	}

	/**
	 * Below search methods and sortandcrop methods are used for getting the
	 * searched items and sorting and pagination. Sorting, search and cropping is
	 * available for Broadcasts, VoDs and Conference Rooms. They are used by
	 * InMemoryDataStore and MapDBStore, Mongodb implements the same functionality
	 * inside its own class.
	 */
	protected ArrayList<VoD> searchOnServerVod(ArrayList<VoD> broadcastList, String search) {
		if (search != null && !search.isEmpty()) {
			for (Iterator<VoD> i = broadcastList.iterator(); i.hasNext();) {
				VoD item = i.next();
				if (item.getVodName() != null && item.getStreamName() != null && item.getStreamId() != null
						&& item.getVodId() != null) {
					if (item.getVodName().toLowerCase().contains(search.toLowerCase())
							|| item.getStreamId().toLowerCase().contains(search.toLowerCase())
							|| item.getStreamName().toLowerCase().contains(search.toLowerCase())
							|| item.getVodId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else
						i.remove();
				} else if (item.getVodName() != null && item.getVodId() != null) {
					if (item.getVodName().toLowerCase().contains(search.toLowerCase())
							|| item.getVodId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else
						i.remove();
				} else {
					if (item.getVodId() != null) {
						if (item.getVodId().toLowerCase().contains(search.toLowerCase()))
							continue;
						else
							i.remove();
					}
				}
			}
		}
		return broadcastList;
	}

	protected List<VoD> sortAndCropVodList(List<VoD> vodList, int offset, int size, String sortBy, String orderBy) {
		if (("name".equals(sortBy) || "date".equals(sortBy)) && orderBy != null)

		{
			Collections.sort(vodList, (vod1, vod2) -> {
				Comparable c1 = null;
				Comparable c2 = null;
				if (sortBy.contentEquals("name")) {
					c1 = vod1.getVodName().toLowerCase();
					c2 = vod2.getVodName().toLowerCase();
				} else if (sortBy.contentEquals("date")) {
					c1 = Long.valueOf(vod1.getCreationDate());
					c2 = Long.valueOf(vod2.getCreationDate());
				}

				int result = 0;
				if (c1 != null && c2 != null) {
					if (orderBy.contentEquals("desc")) {
						result = c2.compareTo(c1);
					} else {
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

		int toIndex = Math.min(offset + size, vodList.size());
		if (offset >= toIndex) {
			return new ArrayList<>();
		} else {
			return vodList.subList(offset, Math.min(offset + size, vodList.size()));
		}

	}

	protected ArrayList<Broadcast> searchOnServer(ArrayList<Broadcast> broadcastList, String search) {
		if (search != null && !search.isEmpty()) {
			for (Iterator<Broadcast> i = broadcastList.iterator(); i.hasNext();) {
				Broadcast item = i.next();
				if (item.getName() != null && item.getStreamId() != null) {
					if (item.getName().toLowerCase().contains(search.toLowerCase())
							|| item.getStreamId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else
						i.remove();
				} else {
					if (item.getStreamId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else
						i.remove();
				}
			}
		}
		return broadcastList;
	}

	protected List<Broadcast> sortAndCropBroadcastList(List<Broadcast> broadcastList, int offset, int size,
			String sortBy, String orderBy) {

		if (("name".equals(sortBy) || "date".equals(sortBy) || "status".equals(sortBy)) && orderBy != null) {
			Collections.sort(broadcastList, new Comparator<Broadcast>() {
				@Override
				public int compare(Broadcast broadcast1, Broadcast broadcast2) {
					Comparable c1 = null;
					Comparable c2 = null;

					if (sortBy.equals("name")) {
						c1 = broadcast1.getName().toLowerCase();
						c2 = broadcast2.getName().toLowerCase();
					} else if (sortBy.equals("date")) {
						c1 = Long.valueOf(broadcast1.getDate());
						c2 = Long.valueOf(broadcast2.getDate());
					} else if (sortBy.equals("status")) {
						c1 = broadcast1.getStatus();
						c2 = broadcast2.getStatus();
					}

					int result = 0;
					if (c1 != null && c2 != null) {
						if (orderBy.equals("desc")) {
							result = c2.compareTo(c1);
						} else {
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
		if (offset < 0) {
			offset = 0;
		}

		int toIndex = Math.min(offset + size, broadcastList.size());
		if (offset >= toIndex) {
			return new ArrayList<>();
		} else {
			return broadcastList.subList(offset, toIndex);
		}
	}

	protected ArrayList<ConferenceRoom> searchOnServerConferenceRoom(ArrayList<ConferenceRoom> roomList,
			String search) {
		if (search != null && !search.isEmpty()) {
			for (Iterator<ConferenceRoom> i = roomList.iterator(); i.hasNext();) {
				ConferenceRoom item = i.next();
				if (item.getRoomId() != null) {
					if (item.getRoomId().toLowerCase().contains(search.toLowerCase()))
						continue;
					else
						i.remove();
				}
			}
		}
		return roomList;
	}

	protected List<ConferenceRoom> sortAndCropConferenceRoomList(List<ConferenceRoom> roomList, int offset, int size,
			String sortBy, String orderBy) {
		if ("roomId".equals(sortBy) || "startDate".equals(sortBy) || "endDate".equals(sortBy)) {
			Collections.sort(roomList, (room1, room2) -> {
				Comparable c1 = null;
				Comparable c2 = null;

				if (sortBy.equals("roomId")) {
					c1 = room1.getRoomId().toLowerCase();
					c2 = room2.getRoomId().toLowerCase();
				} else if (sortBy.equals("startDate")) {
					c1 = Long.valueOf(room1.getStartDate());
					c2 = Long.valueOf(room2.getStartDate());
				} else if (sortBy.equals("endDate")) {
					c1 = Long.valueOf(room1.getEndDate());
					c2 = Long.valueOf(room2.getEndDate());
				}

				int result = 0;
				if (c1 != null && c2 != null) {
					if ("desc".equals(orderBy)) {
						result = c2.compareTo(c1);
					} else {
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

		int toIndex = Math.min(offset + size, roomList.size());
		if (offset >= toIndex) {
			return new ArrayList<>();
		} else {
			return roomList.subList(offset, toIndex);
		}
	}

	/**
	 * Creates new P2PConnection
	 * 
	 * @param conn - P2PConnection object
	 * @return boolean - success
	 */
	public abstract boolean createP2PConnection(P2PConnection conn);

	/**
	 * Get the P2PConnection by streamId
	 * 
	 * @param streamId - stream id for P2PConnection
	 * @return P2PConnection - if exist else null
	 */
	public abstract P2PConnection getP2PConnection(String streamId);

	/**
	 * Deletes a P2PConnection
	 * 
	 * @param conn - P2PConnection object
	 * @return boolean - success
	 */
	public abstract boolean deleteP2PConnection(String streamId);

	/**
	 * Add a subtrack id to a main track (broadcast)
	 * 
	 * @param mainTrackId - main track id
	 * @param subTrackId  - main track id
	 * @return boolean - success
	 */
	public boolean addSubTrack(String mainTrackId, String subTrackId) {
		return addSubTrack(null, mainTrackId, subTrackId, null);
	}

	public boolean addSubTrack(Map<String, String> broadcastMap, String mainTrackId, String subTrackId, Gson gson) {
		boolean result = false;
		synchronized (this) {
			
			String json = broadcastMap.get(mainTrackId);
			Broadcast mainTrack = gson.fromJson(json, Broadcast.class);
			List<String> subTracks = mainTrack.getSubTrackStreamIds();
			if (subTracks == null) {
				subTracks = new ArrayList<>();
			}
			subTracks.add(subTrackId);
			mainTrack.setSubTrackStreamIds(subTracks);
			broadcastMap.replace(mainTrackId, gson.toJson(mainTrack));
			result = true;
		}

		return result;
	}

	/**
	 * Resets the broadcasts in the database. It sets number of viewers to zero. It
	 * also delete the stream if it's zombi stream
	 *
	 * @returns total number of operation in the db
	 */
	public int resetBroadcasts(String hostAddress) {
		return resetBroadcasts(null, hostAddress, null, null);
	}

	public int resetBroadcasts(Map<String, String> broadcastMap, String hostAddress, Gson gson, String dbName) {
		synchronized (this) {
			
			int size = broadcastMap.size();
			int updateOperations = 0;
			int zombieStreamCount = 0;
			
			Set<Entry<String,String>> entrySet = broadcastMap.entrySet();
			
			Iterator<Entry<String, String>> iterator = entrySet.iterator();
			int i = 0;
			while (iterator.hasNext()) {
				Entry<String, String> next = iterator.next();
				
				if (next != null) {
					Broadcast broadcast = gson.fromJson(next.getValue(), Broadcast.class);
					i++;
					
					if (broadcast.isZombi()) {
						iterator.remove();
						zombieStreamCount++;
					}
					else
					{
						broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
						broadcast.setWebRTCViewerCount(0);
						broadcast.setHlsViewerCount(0);
						broadcast.setRtmpViewerCount(0);
						broadcast.setDashViewerCount(0);
						broadcastMap.put(broadcast.getStreamId(), gson.toJson(broadcast));
						updateOperations++;
					}
				}
				
				if (i > size) {
					logger.error(
							"Inconsistency in DB found in resetting broadcasts. It's likely db file({}) is damaged",
							dbName);
					break;
				}
			}
			
			logger.info("Reset broadcasts result in deleting {} zombi streams and {} update operations",
					zombieStreamCount, updateOperations);
			
			return updateOperations + zombieStreamCount;
		}
	}

	/**
	 * Return if data store is available. DataStore is available if it's initialized
	 * and not closed. It's not available if it's closed.
	 * 
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
	public int getTotalWebRTCViewersCount() {
		return getTotalWebRTCViewersCount(null, null);
	}

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
		if (search != null && !search.isEmpty()) {
			for (Iterator<WebRTCViewerInfo> i = list.iterator(); i.hasNext();) {
				WebRTCViewerInfo item = i.next();
				if (item.getViewerId() != null && !item.getViewerId().toLowerCase().contains(search.toLowerCase())) {
					i.remove();
				}
			}
		}
		return list;
	}

	protected List<WebRTCViewerInfo> sortAndCropWebRTCViewerInfoList(List<WebRTCViewerInfo> list, int offset, int size,
			String sortBy, String orderBy) {
		if ("viewerId".equals(sortBy)) {
			Collections.sort(list, (viewer1, viewer2) -> {
				Comparable c1 = viewer1.getViewerId();
				Comparable c2 = viewer2.getViewerId();

				return "desc".equals(orderBy) ? c2.compareTo(c1) : c1.compareTo(c2);
			});
		}

		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}

		int toIndex = Math.min(offset + size, list.size());
		if (offset >= toIndex) {
			return new ArrayList<>();
		} else {
			return list.subList(offset, toIndex);
		}
	}

	/**
	 * This is used to save WebRTC Viewer Info to datastore
	 *
	 * @param info information for the WebRTC Viewer
	 */
	public void saveViewerInfo(WebRTCViewerInfo info) {
		saveViewerInfo(null, info, null);
	}

	public void saveViewerInfo(Map<String, String> webRTCViewerMap, WebRTCViewerInfo info, Gson gson) {
		synchronized (this) {
			if (info != null) {
				try {
					webRTCViewerMap.put(info.getViewerId(), gson.toJson(info));
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
	}

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
	public List<WebRTCViewerInfo> getWebRTCViewerList(int offset, int size, String sortBy, String orderBy,
			String search) {
		return getWebRTCViewerList(null, offset, size, sortBy, orderBy, search, null);
	}

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
	public boolean deleteWebRTCViewerInfo(String viewerId) {
		return deleteWebRTCViewerInfo(null, viewerId);
	}

	public boolean deleteWebRTCViewerInfo(Map<String, String> webRTCViewerMap, String viewerId) {
		synchronized (this) {
			return webRTCViewerMap.remove(viewerId) != null;
		}
	}

	/**
	 * This is used to update meta data for a bradcast
	 *
	 * @param streamId id for the broadcast
	 * @param metaData new meta data
	 */
	public boolean updateStreamMetaData(String streamId, String metaData) {
		return updateStreamMetaData(null, streamId, metaData, null);
	}

	public boolean updateStreamMetaData(Map<String, String> broadcastMap, String streamId, String metaData, Gson gson) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {

				String jsonString = broadcastMap.get(streamId);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setMetaData(metaData);
					String jsonVal = gson.toJson(broadcast);
					String previousValue = broadcastMap.replace(streamId, jsonVal);
					result = true;
					logger.debug("updateStatus replacing id {} having value {} to {}", streamId, previousValue,
							jsonVal);
				}
			}
		}
		return result;
	}

	// **************************************
	// ATTENTION: Write function descriptions while adding new functions
	// **************************************
}
