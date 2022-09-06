package io.antmedia.datastore.db;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
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
import io.vertx.core.Vertx;


public class MapDBStore extends DataStore {

	private DB db;
	private BTreeMap<String, String> map;
	private BTreeMap<String, String> vodMap;
	private BTreeMap<String, String> detectionMap;
	private BTreeMap<String, String> tokenMap;
	private BTreeMap<String, String> subscriberMap;
	private BTreeMap<String, String> conferenceRoomMap;
	private BTreeMap<String, String> webRTCViewerMap;


	private Gson gson;
	private String dbName;
	private Iterable<String> dbFiles;
	private Vertx vertx;
	private long timerId;
	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);
	private static final String MAP_NAME = "BROADCAST";
	private static final String VOD_MAP_NAME = "VOD";
	private static final String DETECTION_MAP_NAME = "DETECTION";
	private static final String TOKEN = "TOKEN";
	private static final String SUBSCRIBER = "SUBSCRIBER";
	private static final String CONFERENCE_ROOM_MAP_NAME = "CONFERENCE_ROOM";
	private static final String WEBRTC_VIEWER = "WEBRTC_VIEWER";


	public MapDBStore(String dbName, Vertx vertx) {
		this.vertx = vertx;
		this.dbName = dbName;
		db = DBMaker
				.fileDB(dbName)
				.fileMmapEnableIfSupported()
				/*.transactionEnable() we disable this because under load, it causes exception.
					//In addition, we already commit and synch methods. So it seems that we don't need this one
				 */
				.checksumHeaderBypass()
				.make();


		map = db.treeMap(MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).counterEnable()
				.createOrOpen();

		vodMap = db.treeMap(VOD_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		detectionMap = db.treeMap(DETECTION_MAP_NAME).keySerializer(Serializer.STRING)
				.valueSerializer(Serializer.STRING).counterEnable().createOrOpen();

		tokenMap = db.treeMap(TOKEN).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		subscriberMap = db.treeMap(SUBSCRIBER).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		conferenceRoomMap = db.treeMap(CONFERENCE_ROOM_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		webRTCViewerMap = db.treeMap(WEBRTC_VIEWER).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();

		timerId = vertx.setPeriodic(5000, id -> 

		vertx.executeBlocking(b -> {

			synchronized (this) 
			{
				if (available) {
					db.commit();
				}
			}

		}, false, null)
				);

		available = true;
	}

	@Override
	public String save(Broadcast broadcast) {
		return super.save(map, broadcast, gson);
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
		return super.updateStatus(map, id, status, gson); 
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		return super.updateDuration(map, id, duration, gson); 
	}

	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		return super.addEndpoint(map, id, endpoint, gson); 
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		return super.removeEndpoint(map, id, endpoint, checkRTMPUrl, gson); 
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		return super.removeAllEndpoints(map, id, gson); 
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
		return super.getActiveBroadcastCount(map, gson);
	}

	@Override
	public boolean delete(String id) {
		return super.delete(map, id);
	}
	
	
	@Override
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search){
		return super.getConferenceRoomList(conferenceRoomMap, offset, size, sortBy, orderBy, search, gson);
	}

	//GetBroadcastList method may be called without offset and size to get the full list without offset or size
	//sortAndCrop method returns maximum 50 (hardcoded) of the broadcasts for an offset.
	public List<Broadcast> getBroadcastListV2(String type, String search) {
		return super.getBroadcastListV2(map, type, search, gson);
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
		return super.addVod(vodMap, vod, gson);
	}


	@Override
	public List<Broadcast> getExternalStreamsList() {
		return super.getExternalStreamsList(map, gson);
	}

	@Override
	public void close(boolean deleteDB) {
		//get db file before closing. They can be used in delete method
		dbFiles = db.getStore().getAllFiles();
		synchronized (this) {
			vertx.cancelTimer(timerId);
			db.commit();
			available = false;
			db.close();
		}

		if (deleteDB) 
		{
			for (String fileName : dbFiles) 
			{
				File file = new File(fileName);
				if (file.exists()) 
				{
					try {
						Files.delete(file.toPath());
					} catch (IOException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}

		}
	}

	@Override
	public boolean deleteVod(String id) {
		return super.deleteVod(vodMap, id);
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
	public int fetchUserVodList(File userfile) {
		return super.fetchUserVodList(vodMap, userfile, gson, dbName);
	}

	@Override
	protected boolean updateSourceQualityParametersLocal(String id, String quality, double speed, int pendingPacketQueue) {
		return super.updateSourceQualityParametersLocal(map, id, quality, speed, pendingPacketQueue, gson);
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
		super.saveDetection(detectionMap, id, timeElapsed, detectedObjects, gson);
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
		return super.updateBroadcastFields(map, streamId, broadcast, gson);
	}

	@Override
	protected synchronized boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		return super.updateHLSViewerCountLocal(map, streamId, diffCount, gson);
	}
	
	@Override
	protected synchronized boolean updateDASHViewerCountLocal(String streamId, int diffCount) {
		return super.updateDASHViewerCountLocal(map, streamId, diffCount, gson);
	}

	@Override
	protected synchronized boolean updateWebRTCViewerCountLocal(String streamId, boolean increment) {
		return super.updateWebRTCViewerCountLocal(map, streamId, increment, gson);
	}

	@Override
	protected synchronized boolean updateRtmpViewerCountLocal(String streamId, boolean increment) {
		return super.updateRtmpViewerCountLocal(map, streamId, increment, gson);
	}
	
	public void clearStreamInfoList(String streamId) {
		//used in mongo for cluster mode. useless here.
	}

	@Override
	public void addStreamInfoList(List<StreamInfo> streamInfoList) {
		//used in mongo for cluster mode. useless here.
	}
	
	public List<StreamInfo> getStreamInfoList(String streamId) {
		return new ArrayList<>();
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
	public void saveStreamInfo(StreamInfo streamInfo) {
		//no need to implement this method, it is used in cluster mode
	}

	@Override
	public boolean setMp4Muxing(String streamId, int enabled) {
		return super.setMp4Muxing(map, streamId, enabled, gson);
	}

	@Override
	public boolean setWebMMuxing(String streamId, int enabled) {
		return super.setWebMMuxing(map, streamId, enabled, gson);
	}

	@Override
	public boolean createConferenceRoom(ConferenceRoom room) {
		return super.createConferenceRoom(conferenceRoomMap, room, gson);
	}

	@Override
	public boolean editConferenceRoom(String roomId, ConferenceRoom room) {
		return super.editConferenceRoom(conferenceRoomMap, roomId, room, gson);
	}

	@Override
	public boolean deleteConferenceRoom(String roomId) {
		return super.deleteConferenceRoom(conferenceRoomMap, roomId);
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomId) {
		return super.getConferenceRoom(conferenceRoomMap, roomId, gson);
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
		return super.addSubTrack(map, mainTrackId, subTrackId, gson);
	}

	@Override
	public int resetBroadcasts(String hostAddress) {
		return super.resetBroadcasts(map, hostAddress, gson, dbName);
	}

	@Override
	public int getTotalWebRTCViewersCount() {
		return super.getTotalWebRTCViewersCount(map, gson);
	}

	@Override
	public void saveViewerInfo(WebRTCViewerInfo info) {
		super.saveViewerInfo(webRTCViewerMap, info, gson);
	}

	@Override
	public List<WebRTCViewerInfo> getWebRTCViewerList(int offset, int size, String sortBy, String orderBy,
			String search) {
		return super.getWebRTCViewerList(webRTCViewerMap, offset, size, sortBy, orderBy, search, gson);
	}

	@Override
	public boolean deleteWebRTCViewerInfo(String viewerId) {
		return super.deleteWebRTCViewerInfo(webRTCViewerMap, viewerId);
	}
	
	@Override
	public boolean updateStreamMetaData(String streamId, String metaData) {
		return super.updateStreamMetaData(map, streamId, metaData, gson);
	}

}
