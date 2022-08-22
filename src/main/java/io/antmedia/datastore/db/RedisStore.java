package io.antmedia.datastore.db;

import java.io.File;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;

public class RedisStore extends DataStore {

	protected static Logger logger = LoggerFactory.getLogger(RedisStore.class);
	
	private RMap<String, String> broadcastMap;
	private RMap<String, String> vodMap;
	private RMap<String, String> conferenceRoomMap;
	private RMap<String, String> detectionMap;
	private RMap<String, String> tokenMap;
	private RMap<String, String> subscriberMap;
	private RMap<String, String> webRTCViewerMap;	
    
	private Gson gson;
	
	private String dbName;
	RedissonClient redisson;
    
    public RedisStore(String redisHost, String dbName, String dbUser, String dbPassword, String redisPort) {
    	
		this.dbName = dbName;
		
		Config config  = new Config();
    	
    	SingleServerConfig singleServerConfig = config.useSingleServer();
    	singleServerConfig.setAddress("redis://"+redisHost+":"+redisPort);
		
		if(dbPassword != null && !dbPassword.isEmpty()) {
			singleServerConfig
		  	  .setPassword(dbPassword);
		}
		if(dbUser != null && !dbUser.isEmpty()) {
			singleServerConfig
		  	  .setUsername(dbUser);
		}

    	redisson = Redisson.create(config);
    	
    	broadcastMap = redisson.getMap(dbName+"Broadcasts");
    	vodMap = redisson.getMap(dbName+"Vods");
    	conferenceRoomMap = redisson.getMap(dbName+"Conferences");
    	detectionMap = redisson.getMap(dbName+"Detections");
    	tokenMap = redisson.getMap(dbName+"Tokens");
    	subscriberMap = redisson.getMap(dbName+"Subscribers");	
    	webRTCViewerMap = redisson.getMap(dbName+"WebRTCViewers");
    	
		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();
		
		available = true;
	}
	
    @Override
	public String save(Broadcast broadcast) {
		return super.save(broadcastMap, null, null, null, broadcast, gson);
	}

	@Override
	public Broadcast get(String id) {
		return super.get(broadcastMap, null, null, null, id, gson);
	}

	@Override
	public VoD getVoD(String id) {
		return super.getVoD(vodMap, null, null, null, id, gson);
	}

	@Override
	public boolean updateStatus(String id, String status) {
		return super.updateStatus(broadcastMap, null, null, id, status, gson); 
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		return super.updateDuration(broadcastMap, null, null, id, duration, gson); 
	}

	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		return super.addEndpoint(broadcastMap, null, null, id, endpoint, gson);
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		return super.removeEndpoint(broadcastMap, null, null, id, endpoint, checkRTMPUrl, gson);
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		return super.removeAllEndpoints(broadcastMap, null, null, id, gson);
	}


	/**
	 * Use getTotalBroadcastNumber
	 * @deprecated
	 */
	@Deprecated
	@Override
	public long getBroadcastCount() {
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	@Override
	public long getActiveBroadcastCount() {
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

	@Override
	public boolean delete(String id) {
		boolean result = false;
		synchronized (this) {
			result = broadcastMap.remove(id) != null;
		}
		return result;
	}
	@Override
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search){
		return super.getConferenceRoomList(conferenceRoomMap, null, offset, size, sortBy, orderBy, search, gson);
	}

	//GetBroadcastList method may be called without offset and size to get the full list without offset or size
	//sortAndCrop method returns maximum 50 (hardcoded) of the broadcasts for an offset.
	public List<Broadcast> getBroadcastListV2(String type, String search) {
		return super.getBroadcastListV2(broadcastMap, null, type, search, gson);
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search) {
		List<Broadcast> list = null;
		list = getBroadcastListV2(type ,search);
		return sortAndCropBroadcastList(list, offset, size, sortBy, orderBy);
	}

	public List<VoD> getVodListV2(String streamId, String search) {
		return super.getVodListV2(vodMap, null, streamId, search, gson, null);
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
		return super.addVod(vodMap, null, null, vod, gson);
	}


	@Override
	public List<Broadcast> getExternalStreamsList() {

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

	
	@Override
	public void close(boolean deleteDB) {
	
		synchronized(this) {
			available = false;
			if (deleteDB) {
				redisson.getMap(dbName+"Broadcasts").delete();
		    	redisson.getMap(dbName+"Broadcasts");
		    	redisson.getMap(dbName+"Vods");
		    	redisson.getMap(dbName+"Conferences");
		    	redisson.getMap(dbName+"Detections");
		    	redisson.getMap(dbName+"tokens");
		    	redisson.getMap(dbName+"Subscribers");	
		    	redisson.getMap(dbName+"webRTCViewers");
			}
			redisson.shutdown();
		}

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
		return super.getTotalVodNumber(vodMap, null);
	}

	@Override
	public long getPartialVodNumber(String search){
		List<VoD> vods = getVodListV2(null, search);
		return vods.size();
	}

	@Override
	public int fetchUserVodList(File userfile) {
		return super.fetchUserVodList(vodMap,null, userfile, gson, dbName);
	}


	@Override
	protected boolean updateSourceQualityParametersLocal(String id, String quality, double speed, int pendingPacketQueue) {
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

	@Override
	public long getTotalBroadcastNumber() {
		synchronized (this) {
			return broadcastMap.size();
		}
	}

	@Override
	public long getPartialBroadcastNumber(String search) {
		return getBroadcastListV2(null ,search).size();
	}

	@Override
	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		super.saveDetection(detectionMap, null, id, timeElapsed, detectedObjects, gson);
	}

	@Override
	public List<TensorFlowObject> getDetection(String id) {
		List<TensorFlowObject> tensorflowObject = new ArrayList<> ();
		synchronized (this) {
			if (id != null) {
				String jsonString = detectionMap.get(id);
				if (jsonString != null) {
					Type listType = new TypeToken<ArrayList<TensorFlowObject>>(){}.getType();
					return gson.fromJson(jsonString, listType);
				}
			}
		}
		return tensorflowObject;
	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		return super.getDetectionList(detectionMap, null, idFilter, offsetSize, batchSize, gson);
	}

	@Override
	public long getObjectDetectedTotal(String id) {
		return super.getObjectDetectedTotal(detectionMap, null, id, gson);
	}


	/**
	 * Updates the stream's name, description, userName, password, IP address, stream URL if these values is not null
	 * @param streamId
	 * @param broadcast
	 * @return
	 */
	@Override
	public boolean updateBroadcastFields(String streamId, Broadcast broadcast) {
		return super.updateBroadcastFields(broadcastMap, null, streamId, broadcast, gson);
	}

	@Override
	protected synchronized boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		return super.updateHLSViewerCountLocal(broadcastMap, null, streamId, diffCount, gson);
	}
	
	@Override
	protected synchronized boolean updateDASHViewerCountLocal(String streamId, int diffCount) {
		return super.updateDASHViewerCountLocal(broadcastMap, null, streamId, diffCount, gson);
	}

	@Override
	protected synchronized boolean updateWebRTCViewerCountLocal(String streamId, boolean increment) {
		return super.updateWebRTCViewerCountLocal(broadcastMap, null, streamId, increment, gson);
	}

	@Override
	protected synchronized boolean updateRtmpViewerCountLocal(String streamId, boolean increment) {
		return super.updateRtmpViewerCountLocal(broadcastMap, null, streamId, increment, gson);
	}

	public List<StreamInfo> getStreamInfoList(String streamId) {
		return new ArrayList<>();
	}
	
	public void clearStreamInfoList(String streamId) {
		//used in redis for cluster mode. useless here.
	}
	
	@Override
	public void addStreamInfoList(List<StreamInfo> streamInfoList) {
		//used in redis for cluster mode. useless here.
	}

	@Override
	public boolean saveToken(Token token) {
		return super.saveToken(tokenMap, null, token, gson);
	}

	@Override
	public Token validateToken(Token token) {
		return super.validateToken(tokenMap, null, token, gson);
	}

	@Override
	public boolean revokeTokens(String streamId) {
		return super.revokeTokens(tokenMap, null, streamId, gson);
	}

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {
		return super.listAllTokens(tokenMap, null, streamId, offset, size, gson);
	}

	@Override
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
		return super.listAllSubscribers(subscriberMap, null, streamId, offset, size, gson);
	}

	@Override
	public boolean addSubscriber(String streamId, Subscriber subscriber) {
		return super.addSubscriber(subscriberMap, null, streamId, subscriber, gson);
	}



	@Override
	public boolean deleteSubscriber(String streamId, String subscriberId) {
		return super.deleteSubscriber(subscriberMap, null, streamId, subscriberId);
	}


	@Override
	public boolean revokeSubscribers(String streamId) {
		return super.revokeSubscribers(subscriberMap, null, streamId, gson);
	}

	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		return super.getSubscriber(subscriberMap, null, streamId, subscriberId, gson);
	}		

	@Override
	public boolean resetSubscribersConnectedStatus() {
		return super.resetSubscribersConnectedStatus(subscriberMap, null, gson);
	}

	@Override
	public boolean setMp4Muxing(String streamId, int enabled) {
		return super.setMp4Muxing(broadcastMap, null, streamId, enabled, gson);
	}

	@Override
	public boolean setWebMMuxing(String streamId, int enabled) {
		return super.setWebMMuxing(broadcastMap, null, streamId, enabled, gson);
	}

	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		//no need to implement this method, it is used in cluster mode
	}



	@Override
	public boolean createConferenceRoom(ConferenceRoom room) {
		return super.createConferenceRoom(conferenceRoomMap, null, room, gson);
	}

	@Override
	public boolean editConferenceRoom(String roomId, ConferenceRoom room) {
		return super.editConferenceRoom(conferenceRoomMap, null, roomId, room, gson);
	}

	@Override
	public boolean deleteConferenceRoom(String roomId) {
		return super.deleteConferenceRoom(conferenceRoomMap, null, roomId);
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomId) {
		return super.getConferenceRoom(conferenceRoomMap, null, roomId, gson);
	}

	@Override
	public boolean deleteToken(String tokenId) {
		return super.deleteToken(tokenMap, null, tokenId);
	}

	@Override
	public Token getToken(String tokenId) {
		return super.getToken(tokenMap, null, tokenId, gson);
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

	@Override
	public int resetBroadcasts(String hostAddress) 
	{
		synchronized (this) {

			Collection<String> broadcastsRawJSON = broadcastMap.values();
			int size = broadcastsRawJSON.size();
			int updateOperations = 0;
			int zombieStreamCount = 0;
			int i = 0;
			for (String broadcastRaw : broadcastsRawJSON) {
				i++;
				if (broadcastRaw != null) {
					Broadcast broadcast = gson.fromJson(broadcastRaw, Broadcast.class);
					if (broadcast.isZombi()) {
						zombieStreamCount++;
						broadcastMap.remove(broadcast.getStreamId());
					}
					else {
						updateOperations++;
						broadcast.setHlsViewerCount(0);
						broadcast.setWebRTCViewerCount(0);
						broadcast.setRtmpViewerCount(0);
						broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);
						broadcastMap.put(broadcast.getStreamId(), gson.toJson(broadcast));
					}
				}

				if (i > size) {
					logger.error("Inconsistency in DB found in resetting broadcasts. It's likely db file({}) is damaged", dbName);
					break;
				}
			}
			logger.info("Reset broadcasts result in deleting {} zombi streams and {} update operations", zombieStreamCount, updateOperations );

			return updateOperations + zombieStreamCount;
		}
	}

	@Override
	public int getTotalWebRTCViewersCount() {
		long now = System.currentTimeMillis();
		if(now - totalWebRTCViewerCountLastUpdateTime > TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME) {
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

		ArrayList<WebRTCViewerInfo> list = new ArrayList<>();
		synchronized (this) {
			Collection<String> webRTCViewers = webRTCViewerMap.values();
			for (String infoString : webRTCViewers)
			{
				WebRTCViewerInfo info = gson.fromJson(infoString, WebRTCViewerInfo.class);
				list.add(info);
			}
		}
		if(search != null && !search.isEmpty()){
			logger.info("server side search called for Conference Room = {}", search);
			list = searchOnWebRTCViewerInfo(list, search);
		}
		return sortAndCropWebRTCViewerInfoList(list, offset, size, sortBy, orderBy);
	}



	@Override
	public boolean deleteWebRTCViewerInfo(String viewerId) {
		synchronized (this) 
		{		
			return webRTCViewerMap.remove(viewerId) != null;
		}
	}
	
	@Override
	public boolean updateStreamMetaData(String streamId, String metaData) {
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
					logger.debug("updateStatus replacing id {} having value {} to {}", streamId, previousValue, jsonVal);
				}
			}
		}
		return result;
	}

}
