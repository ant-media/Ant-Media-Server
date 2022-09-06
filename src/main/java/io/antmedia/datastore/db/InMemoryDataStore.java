package io.antmedia.datastore.db;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

public class InMemoryDataStore extends DataStore {

	protected static Logger logger = LoggerFactory.getLogger(InMemoryDataStore.class);
	private Map<String, String> broadcastMap =  new LinkedHashMap<>();
	private Map<String, String> vodMap = new LinkedHashMap<>();
	private Map<String, String> detectionMap = new LinkedHashMap<>();
	private Map<String, String> tokenMap = new LinkedHashMap<>();
	private Map<String, String> subscriberMap = new LinkedHashMap<>();
	private Map<String, String> roomMap = new LinkedHashMap<>();
	private Map<String, String> webRTCViewerMap = new LinkedHashMap<>();
	private Gson gson;
	
	private String dbName;

	public InMemoryDataStore(String dbName) {
		available = true;
		this.dbName = dbName;
		
		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();
		
	}

	@Override
	public String save(Broadcast broadcast) {
		return super.save(broadcastMap, broadcast, gson);
	}

	@Override
	public Broadcast get(String id) {
		return super.get(broadcastMap, id, gson);
	}

	@Override
	public VoD getVoD(String id) {
		return super.getVoD(vodMap, id, gson);
	}

	@Override
	public boolean updateStatus(String id, String status) {
		return super.updateStatus(broadcastMap, id, status, gson); 
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		return super.updateDuration(broadcastMap, id, duration, gson); 
	}

	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		return super.addEndpoint(broadcastMap, id, endpoint, gson); 
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		return super.removeEndpoint(broadcastMap, id, endpoint, checkRTMPUrl, gson);
	}

	@Override
	public long getBroadcastCount() {
		return super.getBroadcastCount(broadcastMap);
	}

	@Override
	public long getActiveBroadcastCount() {
		return super.getActiveBroadcastCount(broadcastMap, gson);
	}


	@Override
	public boolean delete(String id) {
		return super.delete(broadcastMap, id);
	}
	
	//GetBroadcastList method may be called without offset and size to get the full list without offset or size
	//sortAndCrop method returns maximum 50 (hardcoded) of the broadcasts for an offset.
	public List<Broadcast> getBroadcastListV2(String type, String search) {
		return super.getBroadcastListV2(broadcastMap, type, search, gson);
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search) {
		List<Broadcast> list = null;
		list = getBroadcastListV2(type ,search);
		return super.sortAndCropBroadcastList(list, offset, size, sortBy, orderBy);
	}

	@Override
	public List<Broadcast> getExternalStreamsList() {
		return super.getExternalStreamsList(broadcastMap, gson);
	}

	@Override
	public void close(boolean deleteDB) {
		//no need to implement 
		available = false;
	}

	@Override
	public String addVod(VoD vod) {
		return super.addVod(vodMap, vod, gson); 
	}
	
	public List<VoD> getVodListV2(String streamId, String search) {
		return super.getVodListV2(vodMap, streamId, search, gson, dbName);
	}

	@Override
	public List<VoD> getVodList(int offset, int size, String sortBy, String orderBy, String streamId, String search) {
		List<VoD> vods = null;
		vods = getVodListV2(streamId,search);
		return sortAndCropVodList(vods, offset, size, sortBy, orderBy);
	}

	@Override
	public boolean deleteVod(String id) {
		return super.deleteVod(vodMap, id);
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		return super.removeAllEndpoints(broadcastMap, id, gson);
	}

	@Override
	public long getTotalVodNumber() {
		return super.getTotalVodNumber(vodMap);
	}

	@Override
	public int fetchUserVodList(File userfile) {
		return super.fetchUserVodList(vodMap, userfile, gson, dbName);
	}

	@Override
	public boolean updateSourceQualityParametersLocal(String id, String quality, double speed, int pendingPacketSize) {
		return super.updateSourceQualityParametersLocal(broadcastMap, id, quality, speed, pendingPacketSize, gson);
	}

	@Override
	public long getTotalBroadcastNumber() {
		return super.getTotalBroadcastNumber(broadcastMap);
	}

	@Override
	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		super.saveDetection(detectionMap, id, timeElapsed, detectedObjects, gson);
	}

	@Override
	public long getPartialBroadcastNumber(String search){
		return getBroadcastListV2(null ,search).size();
	}

	@Override
	public long getPartialVodNumber(String search){
		List<VoD> vods = getVodListV2(null, search);
		return vods.size();
	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		return super.getDetectionList(detectionMap, idFilter, offsetSize, batchSize, gson);
	}

	@Override
	public long getObjectDetectedTotal(String id) {
		return super.getObjectDetectedTotal(detectionMap, id, gson);
	}

	@Override
	public List<TensorFlowObject> getDetection(String id) {
		return super.getDetection(detectionMap, id, gson);
	}

	@Override
	public boolean updateBroadcastFields(String streamId, Broadcast broadcast) {		
		return super.updateBroadcastFields(broadcastMap, streamId, broadcast, gson);
	}

	@Override
	public synchronized boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		return super.updateHLSViewerCountLocal(broadcastMap, streamId, diffCount, gson);
	}
	
	@Override
	public synchronized boolean updateDASHViewerCountLocal(String streamId, int diffCount) {
		return super.updateDASHViewerCountLocal(broadcastMap, streamId, diffCount, gson);
	}

	@Override
	public synchronized boolean updateWebRTCViewerCountLocal(String streamId, boolean increment) {
		return super.updateWebRTCViewerCountLocal(broadcastMap, streamId, increment, gson);
	}

	@Override
	public synchronized boolean updateRtmpViewerCountLocal(String streamId, boolean increment) {
		return super.updateRtmpViewerCountLocal(broadcastMap, streamId, increment, gson);
	}

	@Override
	public boolean saveToken(Token token) {
		return super.saveToken(tokenMap, token, gson);
	}

	@Override
	public Token validateToken(Token token) {
		return super.validateToken(tokenMap, token, gson);
	}

	@Override
	public boolean revokeTokens(String streamId) {
		return super.revokeTokens(tokenMap, streamId, gson);
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
		return super.addSubscriber(subscriberMap, streamId, subscriber, gson);
	}

	@Override
	public boolean deleteSubscriber(String streamId, String subscriberId) {
		return super.deleteSubscriber(subscriberMap, streamId, subscriberId);
	}

	@Override
	public boolean revokeSubscribers(String streamId) {
		return super.revokeSubscribers(subscriberMap, streamId, gson);
	}
	
	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		return super.getSubscriber(subscriberMap, streamId, subscriberId, gson);
	}
	
	@Override
	public boolean resetSubscribersConnectedStatus() {
		return super.resetSubscribersConnectedStatus(subscriberMap, gson);
	}	
	
	@Override
	public void addStreamInfoList(List<StreamInfo> streamInfoList) {
		//used in mongo for cluster mode. useless here.
	}

	public List<StreamInfo> getStreamInfoList(String streamId) {
		return new ArrayList<>();
	}

	public void clearStreamInfoList(String streamId) {
		//used in mongo for cluster mode. useless here.
	}

	@Override
	public boolean setMp4Muxing(String streamId, int enabled) {
		return super.setMp4Muxing(broadcastMap, streamId, enabled, gson);
	}
	
	@Override
	public boolean setWebMMuxing(String streamId, int enabled) {
		return super.setWebMMuxing(broadcastMap, streamId, enabled, gson);
	}

	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		//no need to implement this method, it is used in cluster mode
	}

	@Override
	public boolean createConferenceRoom(ConferenceRoom room) {
		return super.createConferenceRoom(roomMap, room, gson);
	}

	@Override
	public boolean editConferenceRoom(String roomId, ConferenceRoom room) {
		return super.editConferenceRoom(roomMap, roomId, room, gson);
	}

	@Override
	public boolean deleteConferenceRoom(String roomName) {
		return super.deleteConferenceRoom(roomMap, roomName);
	}
	@Override
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search) {
		return super.getConferenceRoomList(roomMap, offset, size, sortBy, orderBy, search, gson);
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomName) {
		return super.getConferenceRoom(roomMap, roomName, gson);
	}

	@Override
	public boolean deleteToken(String tokenId) {
		return super.deleteToken(tokenMap, tokenId);
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
		return super.addSubTrack(broadcastMap, mainTrackId, subTrackId, gson);
	}
  
	@Override
	public int resetBroadcasts(String hostAddress) {
		return super.resetBroadcasts(broadcastMap, hostAddress, gson, dbName);
	}

	@Override
	public int getTotalWebRTCViewersCount() {
		return super.getTotalWebRTCViewersCount(broadcastMap, gson);
	}

	@Override
	public void saveViewerInfo(WebRTCViewerInfo info) {
		super.saveViewerInfo(webRTCViewerMap, info, gson);
	}

	@Override
	public List<WebRTCViewerInfo> getWebRTCViewerList(int offset, int size, String sortBy, String orderBy, String search) {
		return super.getWebRTCViewerList(webRTCViewerMap, offset, size, sortBy, orderBy, search, gson);
	}

	@Override
	public boolean deleteWebRTCViewerInfo(String viewerId) {
		return super.deleteWebRTCViewerInfo(webRTCViewerMap, viewerId);
	}
	
	@Override
	public boolean updateStreamMetaData(String streamId, String metaData) {
		return super.updateStreamMetaData(broadcastMap, streamId, metaData, gson);
	}
}
