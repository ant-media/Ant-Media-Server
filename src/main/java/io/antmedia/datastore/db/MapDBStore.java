package io.antmedia.datastore.db;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
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
	private static final String SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME = "SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME";
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



	public BTreeMap<String, String> getVodMap() {
		return vodMap;
	}

	public void setVodMap(BTreeMap<String, String> vodMap) {
		this.vodMap = vodMap;
	}

	public BTreeMap<String, String> getMap() {
		return map;
	}

	public void setMap(BTreeMap<String, String> map) {
		this.map = map;
	}

	public BTreeMap<String, String> getDetectionMap() {
		return detectionMap;
	}

	public void setDetectionMap(BTreeMap<String, String> detectionMap) {
		this.detectionMap = detectionMap;
	}

	@Override
	public String save(Broadcast broadcast) {

		String streamId = null;
		synchronized (this) {
			if (broadcast != null) {
				try {
					if (broadcast.getStreamId() == null || broadcast.getStreamId().isEmpty()) {
						streamId = RandomStringUtils.randomNumeric(24);
						broadcast.setStreamId(streamId);
					}
					streamId = broadcast.getStreamId();

					String rtmpURL = broadcast.getRtmpURL();
					if (rtmpURL != null) {
						rtmpURL += streamId;
					}
					broadcast.setRtmpURL(rtmpURL);
					if(broadcast.getStatus()==null) {
						broadcast.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_CREATED);
					}
					map.put(streamId, gson.toJson(broadcast));
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
					streamId = null;
				}
			}
		}

		return streamId;
	}

	@Override
	public Broadcast get(String id) {
		synchronized (this) {
			if (id != null) {
				String jsonString = map.get(id);
				if (jsonString != null) {
					return gson.fromJson(jsonString, Broadcast.class);
				}
			}
		}
		return null;
	}

	@Override
	public VoD getVoD(String id) {
		synchronized (this) {
			if (id != null) {
				String jsonString = vodMap.get(id);
				if (jsonString != null) {
					return gson.fromJson(jsonString, VoD.class);
				}
			}
		}
		return null;
	}

	@Override
	public boolean updateStatus(String id, String status) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setStatus(status);
					if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) {
						broadcast.setStartTime(System.currentTimeMillis());
					}
					else if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED)) {
						broadcast.setRtmpViewerCount(0);
						broadcast.setWebRTCViewerCount(0);
						broadcast.setHlsViewerCount(0);
						broadcast.setDashViewerCount(0);
					}

					String jsonVal = gson.toJson(broadcast);
					String previousValue = map.replace(id, jsonVal);
					logger.debug("updateStatus replacing id {} having value {} to {}", id, previousValue, jsonVal);
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
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setDuration(duration);
					String jsonVal = gson.toJson(broadcast);
					String previousValue = map.replace(id, jsonVal);
					result = true;
					logger.debug("updateStatus replacing id {} having value {} to {}", id, previousValue, jsonVal);
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
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList == null) {
						endPointList = new ArrayList<>();
					}
					endPointList.add(endpoint);
					broadcast.setEndPointList(endPointList);
					map.replace(id, gson.toJson(broadcast));
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
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					List<Endpoint> endPointList = broadcast.getEndPointList();
					if (endPointList != null) {
						for (Iterator<Endpoint> iterator = endPointList.iterator(); iterator.hasNext();) {
							Endpoint endpointItem = iterator.next();
							if(checkRTMPUrl) {
								if (endpointItem.getRtmpUrl().equals(endpoint.getRtmpUrl())) {
									iterator.remove();
									result = true;
									break;
								}
							}
							else if (endpointItem.getEndpointServiceId().equals(endpoint.getEndpointServiceId())) {
								iterator.remove();
								result = true;
								break;
							}
						}

						if (result) {
							broadcast.setEndPointList(endPointList);
							map.replace(id, gson.toJson(broadcast));
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
			if (id != null) {
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setEndPointList(null);
					map.replace(id, gson.toJson(broadcast));
					result = true;
				}
			}
		}
		return result;
	}


	/**
	 * Use getTotalBroadcastNumber
	 * @deprecated
	 */
	@Override
	public long getBroadcastCount() {
		synchronized (this) {
			return map.getSize();
		}
	}

	@Override
	public long getActiveBroadcastCount() {
		int activeBroadcastCount = 0;
		synchronized (this) {
			Collection<String> values = map.values();
			for (String broadcastString : values) {
				Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);
				String status = broadcast.getStatus();
				if (status != null && status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
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
			result = map.remove(id) != null;
			if (result) {
			}
		}
		return result;
	}
	@Override
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search){
		ArrayList<ConferenceRoom> list = new ArrayList<>();
		synchronized (this) {
			Collection<String> conferenceRooms = conferenceRoomMap.getValues();
			for (String roomString : conferenceRooms)
			{
				ConferenceRoom room = gson.fromJson(roomString, ConferenceRoom.class);
				list.add(room);
			}
		}
		if(search != null && !search.isEmpty()){
			logger.info("server side search called for Conference Room = {}", search);
			list = searchOnServerConferenceRoom(list, search);
		}
		return sortAndCropConferenceRoomList(list, offset, size, sortBy, orderBy);
	}

	//GetBroadcastList method may be called without offset and size to get the full list without offset or size
	//sortAndCrop method returns maximum 50 (hardcoded) of the broadcasts for an offset.
	public List<Broadcast> getBroadcastListV2(String type, String search) {
		ArrayList<Broadcast> list = new ArrayList<>();
		synchronized (this) {
			Collection<String> broadcasts = map.getValues();
			if(type != null && !type.isEmpty()) {
				for (String broadcastString : broadcasts)
				{
					Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);

					if (broadcast.getType().equals(type)) {
						list.add(broadcast);
					}
				}
			}
			else {
				for (String broadcastString : broadcasts)
				{
					Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);
					list.add(broadcast);
				}
			}
		}
		if(search != null && !search.isEmpty()){
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
		ArrayList<VoD> vods = new ArrayList<>();
		synchronized (this) {
			Collection<String> values = vodMap.values();
			int length = values.size();
			int i = 0;
			for (String vodString : values)
			{
				VoD vod = gson.fromJson(vodString, VoD.class);
				if (streamId != null && !streamId.isEmpty())
				{
					if (vod.getStreamId().equals(streamId)) {
						vods.add(vod);
					}
				}
				else {
					vods.add(vod);
				}

				i++;
				if (i > length) {
					logger.error("Inconsistency in DB. It's likely db file({}) is damaged", dbName);
					break;
				}
			}
			if(search != null && !search.isEmpty()){
				logger.info("server side search called for VoD searchString = {}", search);
				vods = searchOnServerVod(vods, search);
			}
			return vods;
		}
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

			Object[] objectArray = map.getValues().toArray();
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
					map.replace(broadcastArray[i].getStreamId(), gson.toJson(broadcastArray[i]));
				}
			}
		}
		return streamsList;
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

		boolean result = false;

		synchronized (this) {
			result = vodMap.remove(id) != null;

		}
		return result;
	}

	@Override
	public long getTotalVodNumber() {
		synchronized (this) {
			return vodMap.size();
		}
	}

	@Override
	public long getPartialVodNumber(String search){
		List<VoD> vods = getVodListV2(null, search);
		return vods.size();
	}

	@Override
	public int fetchUserVodList(File userfile) {

		if(userfile==null) {
			return 0;
		}

		int numberOfSavedFiles = 0;

		synchronized (this) {
			int i = 0;

			Collection<String> vodFiles = vodMap.values();

			int size = vodFiles.size();

			List<VoD> vodList = new ArrayList<>();

			for (String vodString : vodFiles)  {
				i++;
				vodList.add(gson.fromJson((String) vodString, VoD.class));
				if (i > size) {
					logger.error("Inconsistency in DB. It's likely db file({}) is damaged", dbName);
					break;
				}
			}

			boolean result = false;
			for (VoD vod : vodList) 
			{	
				if (vod.getType().equals(VoD.USER_VOD)) {
					result = vodMap.remove(vod.getVodId()) != null;
					if (!result) {
						logger.error("MapDB VoD is not synchronized. It's likely db files({}) is damaged", dbName);
					}
				}
			}

			File[] listOfFiles = userfile.listFiles();

			if (listOfFiles != null) 
			{
				for (File file : listOfFiles) {

					String fileExtension = FilenameUtils.getExtension(file.getName());

					if (file.isFile() && 
							("mp4".equals(fileExtension) || "flv".equals(fileExtension) || "mkv".equals(fileExtension))) {

						long fileSize = file.length();
						long unixTime = System.currentTimeMillis();

						String path=file.getPath();

						String[] subDirs = path.split(Pattern.quote(File.separator));
						Integer pathLength=Integer.valueOf(subDirs.length);
						String relativePath = "streams/" +subDirs[pathLength-2]+'/'+subDirs[pathLength-1];

						String vodId = RandomStringUtils.randomNumeric(24);

						VoD newVod = new VoD("vodFile", "vodFile", relativePath, file.getName(), unixTime, 0, 0, fileSize,
								VoD.USER_VOD, vodId, null);
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
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setSpeed(speed);
					if (quality != null) {
						broadcast.setQuality(quality);
					}
					broadcast.setPendingPacketSize(pendingPacketQueue);
					map.replace(id, gson.toJson(broadcast));

					result = true;

				}
			}
		}
		return result;
	}

	@Override
	public long getTotalBroadcastNumber() {
		synchronized (this) {
			return map.size();
		}
	}

	@Override
	public long getPartialBroadcastNumber(String search) {
		List<Broadcast> broadcasts = getBroadcastListV2(null ,search);
		return broadcasts.size();
	}

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

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {

		List<TensorFlowObject> list = new ArrayList<>();

		synchronized (this) {
			Type listType = new TypeToken<ArrayList<TensorFlowObject>>(){}.getType();
			int offsetCount = 0;
			int batchCount = 0;

			if (batchSize > MAX_ITEM_IN_ONE_LIST) {
				batchSize = MAX_ITEM_IN_ONE_LIST;
			}

			for (Iterator<String> keyIterator =  detectionMap.keyIterator(); keyIterator.hasNext();) {
				String keyValue = keyIterator.next();
				if (keyValue.startsWith(idFilter)) 
				{
					if (offsetCount < offsetSize) {
						offsetCount++;
						continue;
					}
					if (batchCount >= batchSize) {
						break;
					}
					List<TensorFlowObject> detectedList = gson.fromJson(detectionMap.get(keyValue), listType);
					list.addAll(detectedList);
					batchCount=list.size();
				}
			}
		}
		return list;
	}

	@Override
	public long getObjectDetectedTotal(String id) {

		List<TensorFlowObject> list = new ArrayList<>();

		Type listType = new TypeToken<ArrayList<TensorFlowObject>>(){}.getType();

		synchronized (this) {

			for (Iterator<String> keyIterator =  detectionMap.keyIterator(); keyIterator.hasNext();) {
				String keyValue = keyIterator.next();
				if (keyValue.startsWith(id)) 
				{
					List<TensorFlowObject> detectedList = gson.fromJson(detectionMap.get(keyValue), listType);
					list.addAll(detectedList);
				}
			}
		}
		return list.size();
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
				if (oldBroadcast != null) 
				{

					updateStreamInfo(oldBroadcast, broadcast);
					map.replace(streamId, gson.toJson(oldBroadcast));

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
					map.replace(streamId, gson.toJson(broadcast));
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
					map.replace(streamId, gson.toJson(broadcast));
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
					}
					else {
						webRTCViewerCount--;
					}
					if(webRTCViewerCount >= 0) {
						broadcast.setWebRTCViewerCount(webRTCViewerCount);
						map.replace(streamId, gson.toJson(broadcast));
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
					}
					else { 
						rtmpViewerCount--;
					}
					if(rtmpViewerCount >= 0) {
						broadcast.setRtmpViewerCount(rtmpViewerCount);
						map.replace(streamId, gson.toJson(broadcast));
						result = true;
					}
				}
			}
		}
		return result;
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
	public boolean saveToken(Token token) {
		boolean result = false;

		synchronized (this) {

			if(token.getStreamId() != null && token.getTokenId() != null) {


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
					fetchedToken = gson.fromJson((String) jsonToken, Token.class);

					if( fetchedToken.getType().equals(token.getType())
							&& Instant.now().getEpochSecond() < fetchedToken.getExpireDate()) {

						if(token.getRoomId() == null || token.getRoomId().isEmpty() ) {
							if(fetchedToken.getStreamId().equals(token.getStreamId())) {

								tokenMap.remove(token.getTokenId());

							}
							else{
								fetchedToken = null;
							}
						}
						return fetchedToken;
					}
					else {
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
			Object[] objectArray = tokenMap.getValues().toArray();
			Token[] tokenArray = new Token[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				tokenArray[i] = gson.fromJson((String) objectArray[i], Token.class);
			}

			for (int i = 0; i < tokenArray.length; i++) {
				if (tokenArray[i].getStreamId().equals(streamId)) {
					result = tokenMap.remove(tokenArray[i].getTokenId()) != null;
					if(!result) {
						break;
					}
				}

			}
		}
		return result;
	}

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {

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

			while(iterator.hasNext()) {
				Token token = gson.fromJson(iterator.next(), Token.class);

				if(token.getStreamId().equals(streamId)) {
					list.add(token);
				}
			}

			Iterator<Token> listIterator = list.iterator();

			while(itemCount < size && listIterator.hasNext()) {
				if (t < offset) {
					t++;
					listIterator.next();
				}
				else {

					listToken.add(listIterator.next());
					itemCount++;

				}
			}

		}
		return listToken;
	}

	@Override
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
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

			while(iterator.hasNext()) {
				Subscriber subscriber = gson.fromJson(iterator.next(), Subscriber.class);

				if(subscriber.getStreamId().equals(streamId)) {
					list.add(subscriber);
				}
			}

			Iterator<Subscriber> listIterator = list.iterator();

			while(itemCount < size && listIterator.hasNext()) {
				if (t < offset) {
					t++;
					listIterator.next();
				}
				else {

					listSubscriber.add(listIterator.next());
					itemCount++;

				}
			}

		}
		return listSubscriber;
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
			Object[] objectArray = subscriberMap.getValues().toArray();
			Subscriber[] subscriberArray = new Subscriber[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				subscriberArray[i] = gson.fromJson((String) objectArray[i], Subscriber.class);
			}

			for (int i = 0; i < subscriberArray.length; i++) {
				String subscriberStreamId = subscriberArray[i].getStreamId();
				if (subscriberStreamId != null && subscriberStreamId.equals(streamId)) {
					result = subscriberMap.remove(subscriberArray[i].getSubscriberKey()) != null;
					if(!result) {
						break;
					}
				}

			}
		}

		return result;
	}

	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
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
	public boolean setMp4Muxing(String streamId, int enabled) {
		boolean result = false;
		synchronized (this) {
			if (streamId != null) {
				String jsonString = map.get(streamId);
				if (jsonString != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM || enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM || enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {			

					Broadcast broadcast =  gson.fromJson(jsonString, Broadcast.class);	
					broadcast.setMp4Enabled(enabled);
					map.replace(streamId, gson.toJson(broadcast));


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
				String jsonString = map.get(streamId);
				if (jsonString != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM || enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM || enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {			

					Broadcast broadcast =  gson.fromJson(jsonString, Broadcast.class);	
					broadcast.setWebMEnabled(enabled);
					map.replace(streamId, gson.toJson(broadcast));


					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		//no need to implement this method, it is used in cluster mode
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
		synchronized (this) 
		{		
			boolean result = false;

			if (roomId != null && !roomId.isEmpty()) {
				result = conferenceRoomMap.remove(roomId) != null;
			}
			return result;
		}
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomId) {
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
			String json = map.get(mainTrackId);
			Broadcast mainTrack = gson.fromJson(json, Broadcast.class);
			List<String> subTracks = mainTrack.getSubTrackStreamIds();
			if (subTracks == null) {
				subTracks = new ArrayList<>();
			}
			subTracks.add(subTrackId);
			mainTrack.setSubTrackStreamIds(subTracks);
			map.replace(mainTrackId, gson.toJson(mainTrack));
			result = true;
		}

		return result;
	}

	@Override
	public int resetBroadcasts(String hostAddress) 
	{
		synchronized (this) {

			Collection<String> broadcastsRawJSON = map.values();
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
						map.remove(broadcast.getStreamId());
					}
					else {
						updateOperations++;
						broadcast.setHlsViewerCount(0);
						broadcast.setWebRTCViewerCount(0);
						broadcast.setRtmpViewerCount(0);
						broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
						map.put(broadcast.getStreamId(), gson.toJson(broadcast));
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
				for (String json : map.getValues()) {
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
			Collection<String> webRTCViewers = webRTCViewerMap.getValues();
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
				String jsonString = map.get(streamId);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setMetaData(metaData);
					String jsonVal = gson.toJson(broadcast);
					String previousValue = map.replace(streamId, jsonVal);
					result = true;
					logger.debug("updateStatus replacing id {} having value {} to {}", streamId, previousValue, jsonVal);
				}
			}
		}
		return result;
	}
}
