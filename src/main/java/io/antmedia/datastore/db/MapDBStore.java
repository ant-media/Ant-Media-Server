package io.antmedia.datastore.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DB.TreeMapMaker;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.Vod;
import io.antmedia.ipcamera.OnvifCamera;

public class MapDBStore implements IDataStore {

	private DB db;
	private HTreeMap<String, String> map;
	private HTreeMap<String, String> vodMap;
	private BTreeMap<String, String> detectionMap;
	private HTreeMap<String, String> userVodMap;


	private Gson gson;
	private BTreeMap<String, String> socialEndpointsCredentialsMap;
	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);
	private static final String MAP_NAME = "broadcast";
	private static final String VOD_MAP_NAME = "vod";
	private static final String DETECTION_MAP_NAME = "detection";
	private static final String USER_MAP_NAME = "userVod";
	private static final String SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME = "SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME";


	public MapDBStore(String dbName) {

		db = DBMaker.fileDB(dbName)
				.fileMmapEnable()
				.make();

		map = db.hashMap(MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).counterEnable()
				.createOrOpen();
		vodMap = db.hashMap(VOD_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		detectionMap = db.treeMap(DETECTION_MAP_NAME).keySerializer(Serializer.STRING)
				.valueSerializer(Serializer.STRING).counterEnable().createOrOpen();

		userVodMap = db.hashMap(USER_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		socialEndpointsCredentialsMap = db.treeMap(SOCIAL_ENDPONT_CREDENTIALS_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();


	}


	public HTreeMap<String, String> getUserVodMap() {

		return userVodMap;
	}
	public void setUserVodMap(HTreeMap<String, String> userVodMap) {
		this.userVodMap = userVodMap;
	}

	public HTreeMap<String, String> getVodMap() {
		return vodMap;
	}

	public void setVodMap(HTreeMap<String, String> vodMap) {
		this.vodMap = vodMap;
	}

	public HTreeMap<String, String> getMap() {
		return map;
	}

	public void setMap(HTreeMap<String, String> map) {
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
					e.printStackTrace();
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
						endPointList = new ArrayList<Endpoint>();
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
							if (endpointItem.rtmpUrl.equals(endpoint.rtmpUrl)) {
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
		List<Broadcast> list = new ArrayList<Broadcast>();
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

			for (String broadcastString : values) {
				if (t < offset) {
					t++;
					continue;
				}
				list.add(gson.fromJson(broadcastString, Broadcast.class));
				itemCount++;

				if (itemCount >= size) {
					break;
				}

			}
		}
		return list;
	}

	@Override
	public List<Vod> getVodList(int offset, int size) {

		List<Vod> list = new ArrayList<Vod>();
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
				list.add(gson.fromJson(vodString, Vod.class));
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

			List<Broadcast> filterList = new ArrayList<Broadcast>();
			for (int i = 0; i < broadcastArray.length; i++) {

				if (broadcastArray[i].getType().equals(type)) {
					filterList.add(gson.fromJson((String) objectArray[i], Broadcast.class));
				}
			}


			for (Broadcast broadcast : filterList) {
				if (t < offset) {
					t++;
					continue;
				}
				list.add(broadcast);
				itemCount++;

				if (itemCount >= size) {
					break;
				}

			}
		}
		return list;

	}

	@Override
	public boolean addVod(Vod vod) {
		String vodId = null;
		boolean result = false;
		synchronized (this) {
			if (vod != null) {
				try {
					vodId = RandomStringUtils.randomNumeric(24);
					vod.setVodId(vodId);

					vodMap.put(vodId, gson.toJson(vod));
					db.commit();

					result = true;
					logger.warn(Long.toString(vod.getCreationDate()));

				} catch (Exception e) {
					e.printStackTrace();

				}
			}
		}
		return result;
	}
	@Override
	public boolean addUserVod(Vod vod) {
		String vodId = null;
		boolean result = false;
		synchronized (this) {
			if (vod != null) {
				try {
					vodId = RandomStringUtils.randomNumeric(24);
					vod.setVodId(vodId);

					vodMap.put(vodId, gson.toJson(vod));
					db.commit();

					result = true;


				} catch (Exception e) {
					e.printStackTrace();

				}
			}
		}
		return result;
	}






	@Override
	public List<Broadcast> getExternalStreamsList() {

		List<Broadcast> streamsList = new ArrayList<Broadcast>();

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
			Vod[] vodtArray = new Vod[objectArray.length];

			for (int i = 0; i < objectArray.length; i++) {
				vodtArray[i] = gson.fromJson((String) objectArray[i], Vod.class);
			}

			for (int i = 0; i < vodtArray.length; i++) {
				if (vodtArray[i].getType().equals(Vod.USER_VOD)) {
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

						String relativePath=subDirs[pathLength-3]+'/'+subDirs[pathLength-2]+'/'+subDirs[pathLength-1];

						Vod newVod = new Vod("vodFile", "vodFile", relativePath, file.getName(), unixTime, 0, fileSize,
								Vod.USER_VOD);
						addUserVod(newVod);
						numberOfSavedFiles++;
					}
				}
			}
		}

		return numberOfSavedFiles;
	}

	@Override
	public boolean updateSourceQuality(String id, String quality) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setQuality(quality);
					String jsonVal = gson.toJson(broadcast);
					String previousValue = map.replace(id, jsonVal);
					db.commit();
					logger.debug("updateSourceQuality replacing id {} having value {} to {} and the fetched value {}", 
							id, previousValue, jsonVal, jsonString);
					result = true;
				}
			}
		}
		return result;
	}

	@Override

	public boolean updateSourceSpeed(String id, double speed) {
		boolean result = false;
		synchronized (this) {
			if (id != null) {
				String jsonString = map.get(id);
				if (jsonString != null) {
					Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
					broadcast.setSpeed(speed);
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

		List<SocialEndpointCredentials> list = new ArrayList<SocialEndpointCredentials>();

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
				e.printStackTrace();
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
			int offsetCount=0, batchCount=0;

			for (Iterator<String> keyIterator = detectionMap.keyIterator(); keyIterator.hasNext();) {
				String keyValue = keyIterator.next();
				if (keyValue.startsWith(idFilter)) 
				{
					if (offsetCount < offsetSize) {
						offsetCount++;
						continue;
					}
					if (batchCount > batchSize) {
						break;
					}
					batchCount++;
					List<TensorFlowObject> detectedList = gson.fromJson(detectionMap.get(keyValue), listType);
					list.addAll(detectedList);
				}
			}
		}
		return list;
	}

	@Override
	public boolean editStreamSourceInfo(Broadcast broadcast) {
		boolean result = false;
		synchronized (this) {
			try {
				logger.debug("inside of editStreamSourceInfo");
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

		logger.debug("result inside edit camera: " + result);
		return result;
	}

}
