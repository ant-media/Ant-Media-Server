package io.antmedia.datastore.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.cluster.StreamInfo;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.muxer.MuxAdaptor;

public class InMemoryDataStore extends DataStore {

	protected static Logger logger = LoggerFactory.getLogger(InMemoryDataStore.class);
	private Map<String, Broadcast> broadcastMap = new LinkedHashMap<>();
	private Map<String, VoD> vodMap = new LinkedHashMap<>();
	private Map<String, List<TensorFlowObject>> detectionMap = new LinkedHashMap<>();
	private Map<String, SocialEndpointCredentials> socialEndpointCredentialsMap = new LinkedHashMap<>();
	private Map<String, Token> tokenMap = new LinkedHashMap<>();
	private Map<String, ConferenceRoom> roomMap = new LinkedHashMap<>();

	public InMemoryDataStore(String dbName) {
	}

	@Override
	public String save(Broadcast broadcast) {

		String streamId = null;
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
				broadcastMap.put(streamId, broadcast);
			} catch (Exception e) {
				logger.error(e.getMessage());
				streamId = null;
			}

		}
		return streamId;
	}

	@Override
	public Broadcast get(String id) {

		return broadcastMap.get(id);
	}

	@Override
	public VoD getVoD(String id) {
		return vodMap.get(id);
	}

	@Override
	public boolean updateName(String id, String name, String description) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setName(name);
			broadcast.setDescription(description);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean updateStatus(String id, String status) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setStatus(status);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setDuration(duration);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}


	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null && endpoint != null) {
			List<Endpoint> endPointList = broadcast.getEndPointList();
			if (endPointList == null) {
				endPointList = new ArrayList<>();
			}
			endPointList.add(endpoint);
			broadcast.setEndPointList(endPointList);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint) {
		boolean result = false;
		Broadcast broadcast = broadcastMap.get(id);
		if (broadcast != null && endpoint != null) {
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

			}
		}
		return result;
	}

	@Override
	public long getBroadcastCount() {
		return broadcastMap.size();
	}

	@Override
	public long getActiveBroadcastCount() {
		Collection<Broadcast> values = broadcastMap.values();
		long activeBroadcastCount = 0;
		for (Broadcast broadcast : values) {
			String status = broadcast.getStatus();
			if (status != null && status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
				activeBroadcastCount++;
			}
		}
		return activeBroadcastCount;
	}


	@Override
	public boolean delete(String id) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcastMap.remove(id);
			result = true;
		}
		return result;
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size) {
		Collection<Broadcast> values = broadcastMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<Broadcast> list = new ArrayList<>();
		for (Broadcast broadcast : values) {

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
		return list;
	}




	@Override
	public List<Broadcast> getExternalStreamsList() {
		Collection<Broadcast> values = broadcastMap.values();

		List<Broadcast> streamsList = new ArrayList<>();
		for (Broadcast broadcast : values) {
			String type = broadcast.getType();

			if (type.equals(AntMediaApplicationAdapter.IP_CAMERA) || type.equals(AntMediaApplicationAdapter.STREAM_SOURCE)) {
				streamsList.add(broadcast);
			}
		}
		return streamsList;
	}

	@Override
	public void close() {
		//no need to implement 
	}

	@Override
	public List<Broadcast> filterBroadcastList(int offset, int size, String type) {
		int t = 0;
		int itemCount = 0;
		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}

		Collection<Broadcast> values =broadcastMap.values();

		List<Broadcast> list = new ArrayList();

		for (Broadcast broadcast : values) 
		{
			if("ipCamera".equals(broadcast.getType())) 
			{
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
	public String addVod(VoD vod) {
		String id = null;
		boolean result = false;

		if (vod != null) {
			try {
				if (vod.getVodId() == null) {
					vod.setVodId(RandomStringUtils.randomNumeric(24));
				}
				vodMap.put(vod.getVodId(),vod);
				result = true;

			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}

		if(result) {
			id = vod.getVodId();
		}
		return id;
	}

	@Override
	public List<VoD> getVodList(int offset, int size) {
		Collection<VoD> values = vodMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<VoD> list = new ArrayList<>();

		for (VoD vodString : values) {
			if (t < offset) {
				t++;
			}
			else {
				list.add(vodString);
				itemCount++;

				if (itemCount >= size) {
					break;
				}
			}

		}
		return list;
	}



	@Override
	public boolean deleteVod(String id) {
		return vodMap.remove(id) != null;
	}


	public boolean removeAllEndpoints(String id) {
		boolean result = false;
		Broadcast broadcast = broadcastMap.get(id);
		if (broadcast != null) {
			broadcast.setEndPointList(null);
			broadcastMap.replace(id, broadcast);
			result = true;
		}
		return result;

	}

	@Override
	public long getTotalVodNumber() {
		return vodMap.size();
	}

	@Override
	public int fetchUserVodList(File userfile) {

		if (userfile == null) {
			return 0;
		}


		/*
		 * Delete all user vod in db
		 */
		int numberOfSavedFiles = 0;
		Collection<VoD> vodCollection = vodMap.values();

		for (Iterator iterator = vodCollection.iterator(); iterator.hasNext();) {
			VoD vod = (VoD) iterator.next();
			if (vod.getType().equals(VoD.USER_VOD)) {
				iterator.remove();
			}
		}

		File[] listOfFiles = userfile.listFiles();

		if (listOfFiles != null) {

			for (File file : listOfFiles) 
			{
				String fileExtension = FilenameUtils.getExtension(file.getName());

				if (file.isFile() && 
						("mp4".equals(fileExtension) || "flv".equals(fileExtension) || "mkv".equals(fileExtension))) 
				{
					long fileSize = file.length();
					long unixTime = System.currentTimeMillis();

					String filePath = file.getPath();

					String[] subDirs = filePath.split(Pattern.quote(File.separator));

					String relativePath= "streams/" + subDirs[subDirs.length-2] +'/' +subDirs[subDirs.length-1];

					String vodId = RandomStringUtils.randomNumeric(24);
					VoD newVod = new VoD("vodFile", "vodFile", relativePath, file.getName(), unixTime, 0, fileSize,
							VoD.USER_VOD, vodId);

					addVod(newVod);
					numberOfSavedFiles++;
				}
			}
		}

		return numberOfSavedFiles;
	}




	@Override
	public boolean updateSourceQualityParametersLocal(String id, String quality, double speed, int pendingPacketSize) {
		boolean result = false;
		if (id != null) {
			Broadcast broadcast = broadcastMap.get(id);
			if (broadcast != null) {
				broadcast.setQuality(quality);
				broadcast.setSpeed(speed);
				broadcast.setPendingPacketSize(pendingPacketSize);
				broadcastMap.replace(id, broadcast);
				result = true;
			}
		}
		return result;
	}


	public SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials) {
		SocialEndpointCredentials addedCredential = null;
		if (credentials != null && credentials.getAccountName() != null && credentials.getAccessToken() != null
				&& credentials.getServiceName() != null) 
		{
			if (credentials.getId() == null) {
				//create new id if id is not set
				String id = RandomStringUtils.randomAlphanumeric(6);
				credentials.setId(id);
				socialEndpointCredentialsMap.put(id, credentials);
				addedCredential = credentials;
			}
			else {

				if(socialEndpointCredentialsMap.get(credentials.getId()) != null) 
				{
					//replace the field if id exists
					socialEndpointCredentialsMap.put(credentials.getId(), credentials);
					addedCredential = credentials;
				}
				//if id is not matched with any value, do not record
			}
		}
		return addedCredential;
	}

	@Override
	public List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size) 
	{
		Collection<SocialEndpointCredentials> values = socialEndpointCredentialsMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<SocialEndpointCredentials> list = new ArrayList();
		for (SocialEndpointCredentials credential : values) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(credential);
			itemCount++;

			if (itemCount >= size) {
				break;
			}
		}
		return list;
	}

	@Override
	public boolean removeSocialEndpointCredentials(String id) {
		return socialEndpointCredentialsMap.remove(id) != null;
	}

	@Override
	public SocialEndpointCredentials getSocialEndpointCredentials(String id) {
		SocialEndpointCredentials credential = null;
		if (id != null) {
			credential = socialEndpointCredentialsMap.get(id);
		}
		return credential;
	}

	@Override
	public long getTotalBroadcastNumber() {
		return broadcastMap.size();
	}

	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		if (detectedObjects != null) {
			for (TensorFlowObject tensorFlowObject : detectedObjects) {
				tensorFlowObject.setDetectionTime(timeElapsed);
			}
			detectionMap.put(id, detectedObjects);
		}
	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		int offsetCount=0, batchCount=0;
		List<TensorFlowObject> list = new ArrayList<>();
		Set<String> keySet = detectionMap.keySet();
		for(String keyValue: keySet) {
			if (keyValue.startsWith(idFilter)) 
			{
				if (offsetCount < offsetSize) {
					offsetCount++;
					continue;
				}
				if (batchCount >= batchSize) {
					break;
				}
				List<TensorFlowObject> detectedList = detectionMap.get(keyValue);
				list.addAll(detectedList);
				batchCount=list.size();
			}
		}
		return list;
	}

	@Override

	public long getObjectDetectedTotal(String id) {

		List<TensorFlowObject> list = new ArrayList<>();
		Set<String> keySet = detectionMap.keySet();

		for(String keyValue: keySet) {
			if (keyValue.startsWith(id)) 
			{
				List<TensorFlowObject> detectedList = detectionMap.get(keyValue);
				list.addAll(detectedList);
			}
		}
		return list.size();
	}

	@Override
	public List<TensorFlowObject> getDetection(String id) {
		if (id != null) {
			List<TensorFlowObject> detectedObjects = detectionMap.get(id);
			return detectedObjects;
		}
		return null;
	}

	@Override
	public boolean editStreamSourceInfo(Broadcast broadcast) {		
		boolean result = false;
		try {
			logger.warn("inside of editCameraInfo");

			Broadcast oldBroadcast = get(broadcast.getStreamId());

			oldBroadcast.setName(broadcast.getName());
			oldBroadcast.setUsername(broadcast.getUsername());
			oldBroadcast.setPassword(broadcast.getPassword());
			oldBroadcast.setIpAddr(broadcast.getIpAddr());
			oldBroadcast.setStreamUrl(broadcast.getStreamUrl());

			broadcastMap.replace(oldBroadcast.getStreamId(), oldBroadcast);

			result = true;
		} catch (Exception e) {
			result = false;
		}

		return result;
	}

	@Override
	public synchronized boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		boolean result = false;
		if (streamId != null) {
			Broadcast broadcast = broadcastMap.get(streamId);
			if (broadcast != null) {
				int hlsViewerCount = broadcast.getHlsViewerCount();
				hlsViewerCount += diffCount;

				broadcast.setHlsViewerCount(hlsViewerCount);
				broadcastMap.replace(streamId, broadcast);
				result = true;
			}
		}
		return result;
	}

	@Override
	public synchronized boolean updateWebRTCViewerCountLocal(String streamId, boolean increment) {
		boolean result = false;
		if (streamId != null) {
			Broadcast broadcast = broadcastMap.get(streamId);
			if (broadcast != null) {
				int webRTCViewerCount = broadcast.getWebRTCViewerCount();
				if (increment) {
					webRTCViewerCount++;
				}
				else  {
					webRTCViewerCount--;
				}

				broadcast.setWebRTCViewerCount(webRTCViewerCount);
				broadcastMap.replace(streamId, broadcast);
				result = true;
			}
		}
		return result;
	}

	@Override
	public synchronized boolean updateRtmpViewerCountLocal(String streamId, boolean increment) {
		boolean result = false;
		if (streamId != null) {
			Broadcast broadcast = broadcastMap.get(streamId);
			if (broadcast != null) {
				int rtmpViewerCount = broadcast.getRtmpViewerCount();
				if (increment) {
					rtmpViewerCount++;
				}
				else  {
					rtmpViewerCount--;
				}

				broadcast.setRtmpViewerCount(rtmpViewerCount);
				broadcastMap.replace(streamId, broadcast);
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean saveToken(Token token) {
		boolean result = false;
		if(token.getStreamId() != null && token.getTokenId() != null) {

			try {

				tokenMap.put(token.getTokenId(), token);
				result = true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return result;
	}

	@Override
	public Token validateToken(Token token) {
		Token fetchedToken = null;
		if (token.getTokenId() != null) {
			fetchedToken = tokenMap.get(token.getTokenId());
			if (fetchedToken != null && fetchedToken.getStreamId().equals(token.getStreamId()) && fetchedToken.getType().equals(token.getType())) {
				tokenMap.remove(token.getTokenId());
				return fetchedToken;
			}else {
				fetchedToken = null;
			}
		}
		return fetchedToken;
	}

	@Override
	public boolean revokeTokens(String streamId) {
		boolean result = false;
		Collection<Token> tokenCollection = tokenMap.values();

		for (Iterator iterator = tokenCollection.iterator(); iterator.hasNext();) {
			Token token = (Token) iterator.next();
			if (token.getStreamId().equals(streamId)) {
				iterator.remove();
				tokenMap.remove(token.getTokenId());
			}
			result = true;

		}
		return result;
	}

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {

		List<Token> list = new ArrayList<>();
		List<Token> returnList = new ArrayList<>();

		Collection<Token> values = tokenMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}


		for(Token token: values) {
			if (token.getStreamId().equals(streamId)) {
				list.add(token);
			}
		}


		Iterator<Token> iterator = list.iterator();

		while(itemCount < size && iterator.hasNext()) {
			if (t < offset) {
				t++;
				iterator.next();
			}
			else {

				returnList.add(iterator.next());
				itemCount++;
			}
		}

		return returnList;
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
		boolean result = false;

		if (streamId != null) {
			Broadcast broadcast = broadcastMap.get(streamId);
			if (broadcast != null && (enabled == MuxAdaptor.MP4_ENABLED_FOR_STREAM || enabled == MuxAdaptor.MP4_NO_SET_FOR_STREAM || enabled == MuxAdaptor.MP4_DISABLED_FOR_STREAM)) {
				broadcast.setMp4Enabled(enabled);
				broadcastMap.replace(streamId, broadcast);
				result = true;
			}
		}

		return result;
	}

	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		//no need to implement this method, it is used in cluster mode
	}

	@Override
	public void clearStreamsOnThisServer() {
		//no need to implement for InMemoryDb
	}

	@Override
	public boolean createConferenceRoom(ConferenceRoom room) {
		
		boolean result = false;

		if (room != null && room.getRoomName() != null) {
			roomMap.put(room.getRoomName(), room);
			result = true;
		}

		return result;
	}

	@Override
	public boolean editConferenceRoom(ConferenceRoom room) {

		boolean result = false;

		if (room != null && room.getRoomName() != null) {
			roomMap.replace(room.getRoomName(), room);
			result = true;
		}
		return result;
	}

	@Override
	public boolean deleteConferenceRoom(String roomName) {

		boolean result = false;

		if (roomName != null && roomName.length() > 0 ) {
			roomMap.remove(roomName);
			result = true;
		}
		return result;
		
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomName) {
		return roomMap.get(roomName);
	}
}
