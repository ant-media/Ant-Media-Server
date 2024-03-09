package io.antmedia.datastore.db;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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
import com.google.gson.GsonBuilder;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberMetadata;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;


public abstract class MapBasedDataStore extends DataStore {

	protected Map<String, String> map;
	protected Map<String, String> vodMap;
	protected Map<String, String> detectionMap;
	protected Map<String, String> tokenMap;
	protected Map<String, String> subscriberMap;
	protected Map<String, String> conferenceRoomMap;
	protected Map<String, String> webRTCViewerMap;
	protected Map<String, String> subscriberMetadataMap;

	public static final String REPLACE_CHARS_REGEX = "[\n|\r|\t]";

	protected Gson gson;
	protected String dbName;
	
	protected static Logger logger = LoggerFactory.getLogger(MapBasedDataStore.class);


	public MapBasedDataStore(String dbName) {
		this.dbName = dbName;
		

		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();

		available = true;
	}

	@Override
	public String save(Broadcast broadcast) {
    	String streamId = null;
		synchronized (this) {
			if (broadcast != null) {
				Broadcast updatedBroadcast = super.saveBroadcast(broadcast);
				streamId = updatedBroadcast.getStreamId();
				map.put(updatedBroadcast.getStreamId(), gson.toJson(updatedBroadcast));
			}
	    	return streamId;
		}
	}

	@Override
	public Broadcast get(String id) {
		return super.get(map, id, gson);
	}

	@Override
	public VoD getVoD(String id) {
		return super.getVoD(vodMap, id, gson);
	}

	@Override
	public boolean updateStatus(String id, String status) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				Broadcast broadcast = getBroadcastFromMap(id);
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
					setBroadcastToMap(broadcast, id);
					
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				Broadcast broadcast = getBroadcastFromMap(id);
				if (broadcast != null) {
					broadcast.setDuration(duration);
					setBroadcastToMap(broadcast, id);
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		boolean result = false;
		synchronized (this) {
			if (id != null && endpoint != null) {
				Broadcast broadcast = getBroadcastFromMap(id);
				if (broadcast != null) {
					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList == null) {
						endPointList = new ArrayList<>();
					}
					endPointList.add(endpoint);
					broadcast.setEndPointList(endPointList);
					setBroadcastToMap(broadcast, id);
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		boolean result = false;
		synchronized (this) {
			if (id != null && endpoint != null) {
				
				Broadcast broadcast = getBroadcastFromMap(id);
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
							setBroadcastToMap(broadcast, id);
						}
					}
				}
			}
		}
		return result;
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		boolean result = false;
		synchronized (this) {
			Broadcast broadcast = getBroadcastFromMap(id);
			if (broadcast != null) {
				broadcast.setEndPointList(null);
				setBroadcastToMap(broadcast, id);
				result = true;
			}
		}
		return result;
	}

	/**
	 * Use getTotalBroadcastNumber
	 * @deprecated
	 */
	@Override
	@Deprecated
	public long getBroadcastCount() {
		return super.getBroadcastCount(map);
	}

	@Override
	public long getActiveBroadcastCount() {
		return super.getActiveBroadcastCount(map, gson, null);
	}
	 
	public List<Broadcast> getActiveBroadcastList(String hostAddress) {
		return super.getActiveBroadcastList(map, gson, hostAddress);
	}

	@Override
	public boolean delete(String id) {
		boolean result = false;
		synchronized (this) {			
			result = map.remove(id) != null;
		}
		return result;
	}
	
	
	@Override
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search){
		return super.getConferenceRoomList(conferenceRoomMap, offset, size, sortBy, orderBy, search, gson);
	}

	//GetBroadcastList method may be called without offset and size to get the full list without offset or size
	//sortAndCrop method returns maximum 50 (hardcoded) of the broadcasts for an offset.
	public List<Broadcast> getBroadcastListV2(String type, String search) {
		ArrayList<Broadcast> list = new ArrayList<>();
		synchronized (this) {

			if (type != null && !type.isEmpty()) {
				for (String broadcastString : map.values()) {
					Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);

					if (broadcast.getType().equals(type)) {
						list.add(broadcast);
					}
				}
			} else {
				for (String broadcastString : map.values()) {
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

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search) {
		List<Broadcast> list = null;
		list = getBroadcastListV2(type ,search);
		return sortAndCropBroadcastList(list, offset, size, sortBy, orderBy);
	}

	public List<VoD> getVodListV2(String streamId, String search) {
		return super.getVodListV2(vodMap, streamId, search, gson, dbName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<VoD> getVodList(int offset, int size, String sortBy, String orderBy, String streamId, String search) {
		List<VoD> vods = null;
		vods = getVodListV2(streamId,search);
		return sortAndCropVodList(vods, offset, size, sortBy, orderBy);
	}

	@Override
	public String addVod(VoD vod) {
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


	@Override
	public List<Broadcast> getExternalStreamsList() {
		List<Broadcast> streamsList = new ArrayList<>();
		synchronized (this) {
			Object[] objectArray = map.values().toArray();
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
					setBroadcastToMap(broadcastArray[i], broadcastArray[i].getStreamId());
				}
			}
		}
		return streamsList;
	}

	@Override
	public boolean deleteVod(String id) {
		boolean result = false;

		synchronized (this) {
			result = vodMap.remove(id) != null;
		}
		return result;
	}

	@Override
	public long getTotalVodNumber() {
		return super.getTotalVodNumber(vodMap);
	}

	@Override
	public long getPartialVodNumber(String search){
		List<VoD> vods = getVodListV2(null, search);
		return vods.size();
	}

	@Override
	public int fetchUserVodList(File filedir) {
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

	@Override
	protected boolean updateSourceQualityParametersLocal(String id, String quality, double speed, int pendingPacketQueue) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				Broadcast broadcast = getBroadcastFromMap(id);
				if(broadcast != null) {
					broadcast.setSpeed(speed);
					if (quality != null) {
						broadcast.setQuality(quality);
					}
					broadcast.setPendingPacketSize(pendingPacketQueue);
					setBroadcastToMap(broadcast, id);
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public long getTotalBroadcastNumber() {
		return super.getTotalBroadcastNumber(map);
	}

	@Override
	public long getPartialBroadcastNumber(String search) {
		return getBroadcastListV2(null ,search).size();
	}
	
	@Override
	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
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

	@Override
	public List<TensorFlowObject> getDetection(String id) {
		return super.getDetection(detectionMap, id, gson);
	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		return super.getDetectionList(detectionMap, idFilter, offsetSize, batchSize, gson);
	}

	@Override
	public long getObjectDetectedTotal(String id) {
		return super.getObjectDetectedTotal(detectionMap, id, gson);
	}


	/**
	 * Updates the stream's name, description, userName, password, IP address, stream URL if these values is not null
	 * @param streamId
	 * @param broadcast
	 * @return
	 */
	@Override
	public boolean updateBroadcastFields(String streamId, Broadcast broadcast) {
		boolean result = false;
		synchronized (this) {
			try {
				logger.debug("inside of updateBroadcastFields {}", broadcast.getStreamId());
				Broadcast oldBroadcast = get(streamId);
				if (oldBroadcast != null) {
					updateStreamInfo(oldBroadcast, broadcast);
					setBroadcastToMap(oldBroadcast, streamId);

					result = true;
				}
			} catch (Exception e) {
				result = false;
			}
		}

		logger.debug("result inside updateBroadcastFields:{} ", result);
		return result;
	}

	@Override
	protected synchronized boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		boolean result = false;
		synchronized (this) {

			if (streamId != null) {
				Broadcast broadcast = get(streamId);
				if (broadcast != null) {
					int hlsViewerCount = broadcast.getHlsViewerCount();
					hlsViewerCount += diffCount;
					broadcast.setHlsViewerCount(hlsViewerCount);
					
					setBroadcastToMap(broadcast, streamId);
					result = true;
				}
			}
		}
		return result;
	}
	
	@Override
	protected synchronized boolean updateDASHViewerCountLocal(String streamId, int diffCount) {
		boolean result = false;
		synchronized (this) {

			if (streamId != null) {
				Broadcast broadcast = get(streamId);
				if (broadcast != null) {
					
					int dashViewerCount = broadcast.getDashViewerCount();
					dashViewerCount += diffCount;
					broadcast.setDashViewerCount(dashViewerCount);
					setBroadcastToMap(broadcast, streamId);
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	protected synchronized boolean updateWebRTCViewerCountLocal(String streamId, boolean increment) {
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
						setBroadcastToMap(broadcast, streamId);
						result = true;
					}
				}
			}
		}
		return result;
	}

	@Override
	protected synchronized boolean updateRtmpViewerCountLocal(String streamId, boolean increment) {
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
						map.replace(streamId, gson.toJson(broadcast));
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	public void clearStreamInfoList(String streamId) {
		//used in mongo for cluster mode. useless here.
	}
	
	
	public List<StreamInfo> getStreamInfoList(String streamId) {
		return new ArrayList<>();
	}

	@Override
	public boolean saveToken(Token token) {
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

	@Override
	public Token validateToken(Token token) {
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

	@Override
	public boolean revokeTokens(String streamId) {
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

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {
		return super.listAllTokens(tokenMap, streamId, offset, size, gson);
	}

	@Override
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
		return super.listAllSubscribers(subscriberMap, streamId, offset, size, gson);
	}

	@Override
	public boolean addSubscriber(String streamId, Subscriber subscriber) {
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

	public boolean blockSubscriber(String streamId, String subscriberId, String blockedType, int seconds) {
		boolean result = false;
			synchronized (this) {

				if (streamId != null && subscriberId != null) {
					try {
						Subscriber subscriber = gson.fromJson(subscriberMap.get(Subscriber.getDBKey(streamId, subscriberId)), Subscriber.class);
						if (subscriber == null) {
							subscriber = new Subscriber();
							subscriber.setStreamId(streamId);
							subscriber.setSubscriberId(subscriberId);
						}
						subscriber.setBlockedType(blockedType);
						subscriber.setBlockedUntilUnitTimeStampMs(System.currentTimeMillis() + (seconds * 1000));


						subscriberMap.put(subscriber.getSubscriberKey(), gson.toJson(subscriber));

						result = true;
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}


		return result;
	}

	@Override
	public boolean deleteSubscriber(String streamId, String subscriberId) {
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

	@Override
	public boolean revokeSubscribers(String streamId) {
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

	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		return super.getSubscriber(subscriberMap, streamId, subscriberId, gson);
	}		

	@Override
	public boolean resetSubscribersConnectedStatus() {
		synchronized (this) {
			try {
				Collection<String> subcribersRaw = subscriberMap.values();

				for (String subscriberRaw : subcribersRaw) {
					if (subscriberRaw != null) {
						Subscriber subscriber = gson.fromJson(subscriberRaw, Subscriber.class);
						if (subscriber != null) {
							subscriber.setConnected(false);
							subscriber.setCurrentConcurrentConnections(0);
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
	
	@Override
	public int resetBroadcasts(String hostAddress) {
		synchronized (this) {
			
			int size = map.size();
			int updateOperations = 0;
			int zombieStreamCount = 0;
			
			Set<Entry<String,String>> entrySet = map.entrySet();
			
			Iterator<Entry<String, String>> iterator = entrySet.iterator();
			int i = 0;
			while (iterator.hasNext()) {
				Entry<String, String> next = iterator.next();
				
				if (next != null) {
					Broadcast broadcast = gson.fromJson(next.getValue(), Broadcast.class);
					i++;
					
					if (broadcast.getOriginAdress() == null || broadcast.getOriginAdress().isEmpty() ||
							hostAddress.equals(broadcast.getOriginAdress())) 
					{
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
							map.put(broadcast.getStreamId(), gson.toJson(broadcast));
							updateOperations++;
						}
					}
				}
				
				if (i > size) {
					logger.error(
							"Inconsistency in DB found in resetting broadcasts for dbName:{}",
							dbName);
					break;
				}
			}
			
			logger.info("Reset broadcasts result in deleting {} zombi streams and {} update operations",
					zombieStreamCount, updateOperations);
			
			return updateOperations + zombieStreamCount;
		}
	}
	
	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		//no need to implement this method, it is used in cluster mode
	}

	@Override
	public boolean setMp4Muxing(String streamId, int enabled) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {
				Broadcast broadcast = getBroadcastFromMap(streamId);
				if (broadcast != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {
					broadcast.setMp4Enabled(enabled);
					setBroadcastToMap(broadcast, streamId);

					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public boolean setWebMMuxing(String streamId, int enabled) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {
				Broadcast broadcast = getBroadcastFromMap(streamId);
				if (broadcast != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM
						|| enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {
					broadcast.setWebMEnabled(enabled);
					setBroadcastToMap(broadcast, streamId);

					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public boolean createConferenceRoom(ConferenceRoom room) {
		synchronized (this) {
			boolean result = false;

			if (room != null && room.getRoomId() != null) {
				conferenceRoomMap.put(room.getRoomId(), gson.toJson(room));
				result = true;
			}

			return result;
		}
	}

	@Override
	public boolean editConferenceRoom(String roomId, ConferenceRoom room) {
		synchronized (this) {
			boolean result = false;

			if (roomId != null && room != null && room.getRoomId() != null) {
				result = conferenceRoomMap.replace(roomId, gson.toJson(room)) != null;
			}
			return result;
		}
	}

	@Override
	public boolean deleteConferenceRoom(String roomId) {
		synchronized (this) {
			boolean result = false;

			if (roomId != null && !roomId.isEmpty()) {

				result = conferenceRoomMap.remove(roomId) != null;
			}
			return result;
		}
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomId) {
		return super.getConferenceRoom(conferenceRoomMap, roomId, gson);
	}

	@Override
	public boolean deleteToken(String tokenId) {
		boolean result = false;

		synchronized (this) {
			result = tokenMap.remove(tokenId) != null;
		}
		return result;
	}

	@Override
	public Token getToken(String tokenId) {
		return super.getToken(tokenMap, tokenId, gson);
	}	

	@Override
	public boolean createP2PConnection(P2PConnection conn) {
		// No need to implement. It used in cluster mode
		return false;
	}

	@Override
	public boolean deleteP2PConnection(String streamId) {
		// No need to implement. It used in cluster mode
		return false;
	}

	@Override
	public P2PConnection getP2PConnection(String streamId) {
		// No need to implement. It used in cluster mode
		return null;
	}

	@Override
	public boolean addSubTrack(String mainTrackId, String subTrackId) {
		boolean result = false;
		synchronized (this) {
			Broadcast broadcast = getBroadcastFromMap(mainTrackId);
			if (broadcast != null && subTrackId != null) {
				List<String> subTracks = broadcast.getSubTrackStreamIds();
				if (subTracks == null) {
					subTracks = new ArrayList<>();
				}
				subTracks.add(subTrackId);
				broadcast.setSubTrackStreamIds(subTracks);
				setBroadcastToMap(broadcast, mainTrackId);
				result = true;
			}
		}

		return result;
	}

	@Override
	public boolean removeSubTrack(String mainTrackId, String subTrackId) {
		boolean result = false;
		synchronized (this) {
			Broadcast mainTrack = getBroadcastFromMap(mainTrackId);
			if (mainTrack != null && subTrackId != null) {
				List<String> subTracks = mainTrack.getSubTrackStreamIds();
				if(subTracks.remove(subTrackId)) {
					mainTrack.setSubTrackStreamIds(subTracks);
					setBroadcastToMap(mainTrack, mainTrackId);
					result = true;
				}
			}
		}
		return result;
	}



	@Override
	public int getTotalWebRTCViewersCount() {
		return super.getTotalWebRTCViewersCount(map, gson);
	}

	@Override
	public void saveViewerInfo(WebRTCViewerInfo info) {
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

	@Override
	public List<WebRTCViewerInfo> getWebRTCViewerList(int offset, int size, String sortBy, String orderBy,
			String search) {
		return super.getWebRTCViewerList(webRTCViewerMap, offset, size, sortBy, orderBy, search, gson);
	}

	@Override
	public boolean deleteWebRTCViewerInfo(String viewerId) {
		synchronized (this) {
			return webRTCViewerMap.remove(viewerId) != null;
		}
	}
	
	@Override
	public boolean updateStreamMetaData(String streamId, String metaData) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {
				Broadcast broadcast = getBroadcastFromMap(streamId);
				if(broadcast != null) {
					broadcast.setMetaData(metaData);
					setBroadcastToMap(broadcast, streamId);
					result = true;
				}
			}
		}
		return result;
	}
	
	
	public void setBroadcastToMap(Broadcast broadcast, String streamId){
		
		String jsonVal = gson.toJson(broadcast);
		String previousValue = null;

		previousValue = map.replace(streamId, jsonVal);
		
		streamId = streamId.replaceAll(REPLACE_CHARS_REGEX, "_");
		logger.debug("replacing id {} having value {} to {}", streamId,
				previousValue, jsonVal);
	}
	
	public Broadcast getBroadcastFromMap(String streamId) 
	{
		String jsonString = map.get(streamId);
		if(jsonString != null) {
			return gson.fromJson(jsonString, Broadcast.class);
		}
		return null;
	}
	
	@Override
	public void putSubscriberMetaData(String subscriberId, SubscriberMetadata metadata) {
		metadata.setSubscriberId(subscriberId);
		subscriberMetadataMap.put(subscriberId, gson.toJson(metadata));
	}
	
	@Override
	public SubscriberMetadata getSubscriberMetaData(String subscriberId) {
		String jsonString = subscriberMetadataMap.get(subscriberId);
		if(jsonString != null) {
			return gson.fromJson(jsonString, SubscriberMetadata.class);
		}
		return null;
	}

}
