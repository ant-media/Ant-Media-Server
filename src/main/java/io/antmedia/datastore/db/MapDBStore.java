package io.antmedia.datastore.db;

import java.io.File;
import java.lang.reflect.Type;
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
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;


public class MapDBStore implements IDataStore {

	private DB db;
	private BTreeMap<String, String> map;
	private BTreeMap<String, String> vodMap;
	private BTreeMap<String, String> detectionMap;
	private BTreeMap<String, String> userVodMap;
	private BTreeMap<String, String> socialEndpointsCredentialsMap;
	private BTreeMap<String, String> tokenMap;

	private Gson gson;
	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);
	private static final String MAP_NAME = "BROADCAST";
	private static final String VOD_MAP_NAME = "VOD";
	private static final String DETECTION_MAP_NAME = "DETECTION";
	private static final String USER_MAP_NAME = "USER_VOD";
	private static final String TOKEN = "TOKEN";
	private static final String SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME = "SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME";


	public MapDBStore(String dbName) {

		db = DBMaker
				.fileDB(dbName)
				.fileMmapEnableIfSupported()
				.closeOnJvmShutdown()
				.make();

		map = db.treeMap(MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).counterEnable()
				.createOrOpen();
		vodMap = db.treeMap(VOD_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		detectionMap = db.treeMap(DETECTION_MAP_NAME).keySerializer(Serializer.STRING)
				.valueSerializer(Serializer.STRING).counterEnable().createOrOpen();

		userVodMap = db.treeMap(USER_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		socialEndpointsCredentialsMap = db.treeMap(SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		tokenMap = db.treeMap(TOKEN).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();

	}

	public BTreeMap<String, String> getUserVodMap() {

		return userVodMap;
	}
	public void setUserVodMap(BTreeMap<String, String> userVodMap) {
		this.userVodMap = userVodMap;
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
					if (broadcast.getStreamId() == null) {
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
						broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
					}
					map.put(streamId, gson.toJson(broadcast));
					db.commit();
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
	public boolean updateName(String id, String name, String description) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setName(name);
					broadcast.setDescription(description);
					map.replace(id, gson.toJson(broadcast));
					db.commit();
					result = true;
				}
			}
		}
		return result;
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
					String jsonVal = gson.toJson(broadcast);
					String previousValue = map.replace(id, jsonVal);
					db.commit();
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
					db.commit();
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
					db.commit();
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint) {
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
							if (endpointItem.getRtmpUrl().equals(endpoint.getRtmpUrl())) {
								iterator.remove();
								result = true;
								break;
							}
						}

						if (result) {
							broadcast.setEndPointList(endPointList);
							map.replace(id, gson.toJson(broadcast));
							db.commit();
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
					db.commit();
					result = true;
				}
			}
		}
		return result;
	}

	@Override
	public long getBroadcastCount() {
		synchronized (this) {
			return map.getSize();
		}
	}

	@Override
	public long getActiveBroadcastCount() {
		Collection<String> values = map.values();
		int activeBroadcastCount = 0;
		for (String broadcastString : values) {
			Broadcast broadcast = gson.fromJson(broadcastString, Broadcast.class);
			String status = broadcast.getStatus();
			if (status != null && status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
				activeBroadcastCount++;
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
				db.commit();
			}
		}
		return result;
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size) {
		List<Broadcast> list = new ArrayList<>();
		synchronized (this) {
			Collection<String> values = map.values();
			int t = 0;
			int itemCount = 0;
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			if (offset < 0) {
				offset = 0;
			}
			Iterator<String> iterator = values.iterator();

			while(itemCount < size && iterator.hasNext()) {
				if (t < offset) {
					t++;
					iterator.next();
				}
				else {
					list.add(gson.fromJson(iterator.next(), Broadcast.class));

					itemCount++;	
				}
			}

		}
		return list;
	}

	@Override
	public List<VoD> getVodList(int offset, int size) {

		List<VoD> list = new ArrayList<>();
		synchronized (this) {

			Collection<String> values = vodMap.values();
			int t = 0;
			int itemCount = 0;
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			if (offset < 0) {
				offset = 0;
			}

			for (String vodString : values) {
				if (t < offset) {
					t++;
					continue;
				}
				list.add(gson.fromJson(vodString, VoD.class));
				itemCount++;

				if (itemCount >= size) {
					break;
				}

			}
		}
		return list;
	}


	@Override
	public List<Broadcast> filterBroadcastList(int offset, int size, String type) {

		List<Broadcast> list = new ArrayList<Broadcast>();
		synchronized (this) {
			int t = 0;
			int itemCount = 0;
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			if (offset < 0) {
				offset = 0;
			}

			Object[] objectArray = map.getValues().toArray();

			Broadcast[] broadcastArray = new Broadcast[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);
			}

			List<Broadcast> filterList = new ArrayList<>();
			for (int i = 0; i < broadcastArray.length; i++) {

				if (broadcastArray[i].getType().equals(type)) {
					filterList.add(gson.fromJson((String) objectArray[i], Broadcast.class));
				}
			}
			Iterator<Broadcast> iterator = filterList.iterator();

			while(itemCount < size && iterator.hasNext()) {
				if (t < offset) {
					t++;
					iterator.next();
				}
				else {

					list.add(iterator.next());
					itemCount++;
				}
			}

		}
		return list;

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
				db.commit();
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

				if (broadcastArray[i].getType().equals(AntMediaApplicationAdapter.IP_CAMERA) || broadcastArray[i].getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE)) {

					streamsList.add(gson.fromJson((String) objectArray[i], Broadcast.class));
				}
			}
		}
		return streamsList;
	}

	@Override
	public void close() {
		db.close();
	}

	@Override
	public boolean deleteVod(String id) {

		boolean result = false;

		synchronized (this) {
			result = vodMap.remove(id) != null;
			if (result) {
				db.commit();
			}
		}
		return result;
	}

	@Override
	public long getTotalVodNumber() {
		synchronized (this) {
			return getVodMap().size();
		}
	}

	@Override
	public int fetchUserVodList(File userfile) {

		if(userfile==null) {
			return 0;
		}

		int numberOfSavedFiles = 0;

		synchronized (this) {
			Object[] objectArray = vodMap.getValues().toArray();
			VoD[] vodtArray = new VoD[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				vodtArray[i] = gson.fromJson((String) objectArray[i], VoD.class);
			}

			for (int i = 0; i < vodtArray.length; i++) {
				if (vodtArray[i].getType().equals(VoD.USER_VOD)) {
					vodMap.remove(vodtArray[i].getVodId());
					db.commit();
				}
			}


			File[] listOfFiles = userfile.listFiles();

			if (listOfFiles != null) 
			{
				for (File file : listOfFiles) {

					String fileExtension = FilenameUtils.getExtension(file.getName());

					if (file.isFile() && 
							(fileExtension.equals("mp4") || fileExtension.equals("flv") || fileExtension.equals("mkv"))) {

						long fileSize = file.length();
						long unixTime = System.currentTimeMillis();

						String path=file.getPath();

						String[] subDirs = path.split(Pattern.quote(File.separator));

						Integer pathLength=Integer.valueOf(subDirs.length);

						String relativePath = "streams/" +subDirs[pathLength-2]+'/'+subDirs[pathLength-1];

						String vodId = RandomStringUtils.randomNumeric(24);

						VoD newVod = new VoD("vodFile", "vodFile", relativePath, file.getName(), unixTime, 0, fileSize,
								VoD.USER_VOD, vodId);
						addVod(newVod);
						numberOfSavedFiles++;
					}
				}
			}
		}

		return numberOfSavedFiles;
	}


	@Override
	public boolean updateSourceQualityParameters(String id, String quality, double speed, int pendingPacketQueue) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setSpeed(speed);
					broadcast.setQuality(quality);
					broadcast.setPendingPacketSize(pendingPacketQueue);
					map.replace(id, gson.toJson(broadcast));
					db.commit();
					result = true;

				}
			}
		}
		return result;
	}

	public SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials) {
		SocialEndpointCredentials addedCredential = null;
		synchronized (this) {

			if (credentials != null && credentials.getAccountName() != null && credentials.getAccessToken() != null
					&& credentials.getServiceName() != null) 
			{
				if (credentials.getId() == null) {
					//create new id if id is not set
					String id = RandomStringUtils.randomAlphanumeric(6);
					credentials.setId(id);
					socialEndpointsCredentialsMap.put(id, gson.toJson(credentials));
					db.commit();
					addedCredential = credentials;
				}	
				else {

					if(socialEndpointsCredentialsMap.get(credentials.getId()) != null) 
					{
						//replace the field if id exists
						socialEndpointsCredentialsMap.put(credentials.getId(), gson.toJson(credentials));
						db.commit();
						addedCredential = credentials;
					}
					//if id is not matched with any value, do not record
				}
			}
		}
		return addedCredential;
	}

	@Override
	public List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size) {

		List<SocialEndpointCredentials> list = new ArrayList<>();

		synchronized (this) {
			Collection<String> values = socialEndpointsCredentialsMap.values();
			int t = 0;
			int itemCount = 0;
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			if (offset < 0) {
				offset = 0;
			}

			for (String credentialString : values) {
				if (t < offset) {
					t++;
					continue;
				}
				list.add(gson.fromJson(credentialString, SocialEndpointCredentials.class));
				itemCount++;

				if (itemCount >= size) {
					break;
				}

			}
		}
		return list;
	}

	@Override
	public boolean removeSocialEndpointCredentials(String id) {
		boolean result = false;
		synchronized (this) {
			result = socialEndpointsCredentialsMap.remove(id) != null;
			if (result) {
				db.commit();
			}
		}
		return result;
	}

	@Override
	public SocialEndpointCredentials getSocialEndpointCredentials(String id) {
		SocialEndpointCredentials credential = null;
		synchronized (this) {
			if (id != null) {
				String jsonString = socialEndpointsCredentialsMap.get(id);
				if (jsonString != null) {
					credential = gson.fromJson(jsonString, SocialEndpointCredentials.class);
				}
			}
		}
		return credential;

	}

	@Override

	public long getTotalBroadcastNumber() {
		synchronized (this) {
			return getMap().size();
		}
	}


	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		synchronized (this) {
			try {
				if (detectedObjects != null) {
					for (TensorFlowObject tensorFlowObject : detectedObjects) {
						tensorFlowObject.setDetectionTime(timeElapsed);
					}
					detectionMap.put(id, gson.toJson(detectedObjects));
					db.commit();
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

	@Override
	public boolean editStreamSourceInfo(Broadcast broadcast) {
		boolean result = false;
		synchronized (this) {
			try {
				logger.debug("inside of editStreamSourceInfo {}", broadcast.getStreamId());
				Broadcast oldBroadcast = get(broadcast.getStreamId());

				oldBroadcast.setName(broadcast.getName());
				oldBroadcast.setUsername(broadcast.getUsername());
				oldBroadcast.setPassword(broadcast.getPassword());
				oldBroadcast.setIpAddr(broadcast.getIpAddr());
				oldBroadcast.setStreamUrl(broadcast.getStreamUrl());
				oldBroadcast.setStreamUrl(broadcast.getStreamUrl());

				getMap().replace(oldBroadcast.getStreamId(), gson.toJson(oldBroadcast));

				db.commit();
				result = true;
			} catch (Exception e) {
				result = false;
			}
		}

		logger.debug("result inside edit camera:{} ", result);
		return result;
	}

	@Override
	public synchronized boolean updateHLSViewerCount(String streamId, int diffCount) {
		boolean result = false;

		if (streamId != null) {
			Broadcast broadcast = get(streamId);
			if (broadcast != null) {
				int hlsViewerCount = broadcast.getHlsViewerCount();
				hlsViewerCount += diffCount;
				broadcast.setHlsViewerCount(hlsViewerCount);
				map.replace(streamId, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}

		return result;
	}

	@Override
	public synchronized boolean updateWebRTCViewerCount(String streamId, boolean increment) {
		boolean result = false;
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
				broadcast.setWebRTCViewerCount(webRTCViewerCount);
				map.replace(streamId, gson.toJson(broadcast));
				result = true;
			}
		}
		return result;
	}

	@Override
	public synchronized boolean updateRtmpViewerCount(String streamId, boolean increment) {
		boolean result = false;
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
				broadcast.setRtmpViewerCount(rtmpViewerCount);
				map.replace(streamId, gson.toJson(broadcast));
				result = true;
			}
		}
		return result;
	}

	@Override
	public Token createToken(String streamId, long expireDate, String type) {
		Token token = null;
		synchronized (this) {

			if(streamId != null) {
				token = new Token();
				token.setStreamId(streamId);
				token.setExpireDate(expireDate);
				token.setType(type);

				try {
					String tokenId = RandomStringUtils.randomNumeric(24);
					token.setTokenId(tokenId);
					tokenMap.put(tokenId, gson.toJson(token));
					db.commit();
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}

		return token;
	}

	@Override
	public Token validateToken(Token token) {
		Token fetchedToken = null;

		synchronized (this) {
			if (token.getTokenId() != null) {
				String jsonToken = tokenMap.get(token.getTokenId());
				if (jsonToken != null) {
					fetchedToken = gson.fromJson((String) jsonToken, Token.class);
					if(fetchedToken.getStreamId().equals(token.getStreamId()) && fetchedToken.getType().equals(token.getType())) {
						boolean result = tokenMap.remove(token.getTokenId()) != null;
						if (result) {
							db.commit();
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
				db.commit();
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
}