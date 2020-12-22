package io.antmedia.datastore.db;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
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
	private Map<String, Subscriber> subscriberMap = new LinkedHashMap<>();
	private Map<String, ConferenceRoom> roomMap = new LinkedHashMap<>();
	private Map<String, Playlist> playlistMap = new LinkedHashMap<>();

	public InMemoryDataStore(String dbName) {
		
		available = true;
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
	public boolean updateStatus(String id, String status) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setStatus(status);
			if(status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
				broadcast.setStartTime(System.currentTimeMillis());
			}
			else if(status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED)) {
				broadcast.setRtmpViewerCount(0);
				broadcast.setWebRTCViewerCount(0);
				broadcast.setHlsViewerCount(0);
			}
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
	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		boolean result = false;
		Broadcast broadcast = broadcastMap.get(id);
		if (broadcast != null && endpoint != null) {
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
			result = broadcastMap.remove(id) != null ? true : false;
		}
		return result;
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search) {
		
		Collection<Broadcast> values = broadcastMap.values();

		ArrayList<Broadcast> list = new ArrayList<>();
		
		if(type != null && !type.isEmpty()) {
			for (Broadcast broadcast : values) 
			{
				if(type.equals(broadcast.getType())) 
				{
					list.add(broadcast);
				}
			}
		}
		else {
			for (Broadcast broadcast : values) 
			{
				list.add(broadcast);
			}
		}
		if(search != null && !search.isEmpty()){
			logger.info("server side search called for String = {}", search);
			list = searchOnServer(list, search);
		}
		return sortAndCropBroadcastList(list, offset, size, sortBy, orderBy);
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
		available = false;
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
	public List<VoD> getVodList(int offset, int size, String sortBy, String orderBy, String filterStreamId, String search)
	{
		ArrayList<VoD> vods = null;
		
		if (filterStreamId != null && !filterStreamId.isEmpty()) 
		{
			vods = new ArrayList<>();
			
			for (VoD vod : vodMap.values()) 
			{
				if(vod.getStreamId().equals(filterStreamId)) {
					vods.add(vod);
				}
			}
			
		}
		else {
			vods = new ArrayList<>(vodMap.values());
		}
		if(search != null && !search.isEmpty()){
			logger.info("server side search called for VoD searchString = {}", search);
			vods = searchOnServerVod(vods, search);
		}
		return sortAndCropVodList(vods, offset, size, sortBy, orderBy);
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

		for (Iterator<VoD> iterator = vodCollection.iterator(); iterator.hasNext();) {
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
				if (quality != null) {
					broadcast.setQuality(quality);
				}
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
		List<SocialEndpointCredentials> list = new ArrayList<>();
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
	public long getPartialBroadcastNumber(String search){
		ArrayList<Broadcast> broadcasts = new ArrayList<>(broadcastMap.values());
		if(search != null && !search.isEmpty()) {
			broadcasts = searchOnServer(broadcasts, search);
		}
		return broadcasts.size();
	}

	@Override
	public long getPartialVodNumber(String search){
		ArrayList<VoD> vods = new ArrayList<>(vodMap.values());
		if(search != null && !search.isEmpty()) {
			vods = searchOnServerVod(vods, search);
		}
		return vods.size();
	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		int offsetCount=0; 
		int batchCount=0;
		List<TensorFlowObject> list = new ArrayList<>();
		Set<String> keySet = detectionMap.keySet();
		if (batchSize > MAX_ITEM_IN_ONE_LIST) {
			batchSize = MAX_ITEM_IN_ONE_LIST;
		}
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
	public boolean updateBroadcastFields(String streamId, Broadcast broadcast) {		
		boolean result = false;
		try {
			Broadcast oldBroadcast = get(streamId);

			if (oldBroadcast != null) {
				updateStreamInfo(oldBroadcast, broadcast);
				broadcastMap.replace(oldBroadcast.getStreamId(), oldBroadcast);

				result = true;
			}
		} catch (Exception e) {
			logger.error("error in editStreamSourceInfo: {}",  ExceptionUtils.getStackTrace(e));
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
				if(webRTCViewerCount >= 0) {
					broadcast.setWebRTCViewerCount(webRTCViewerCount);
					broadcastMap.replace(streamId, broadcast);
					result = true;
				}
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
				if(rtmpViewerCount >= 0) {
					broadcast.setRtmpViewerCount(rtmpViewerCount);
					broadcastMap.replace(streamId, broadcast);
					result = true;
				}
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
			if (fetchedToken != null 
					&& fetchedToken.getType().equals(token.getType()) 
					&& Instant.now().getEpochSecond() < fetchedToken.getExpireDate()) {

				if(token.getRoomId() == null || token.getRoomId().isEmpty()) {
					if(fetchedToken.getStreamId().equals(token.getStreamId())) {
						tokenMap.remove(token.getTokenId());
					}
					else {
						fetchedToken = null;
					}
				}
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

		for (Iterator<Token> iterator = tokenCollection.iterator(); iterator.hasNext();) {
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
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
		List<Subscriber> list = new ArrayList<>();
		List<Subscriber> returnList = new ArrayList<>();

		Collection<Subscriber> values = subscriberMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > MAX_ITEM_IN_ONE_LIST) {
			size = MAX_ITEM_IN_ONE_LIST;
		}
		if (offset < 0) {
			offset = 0;
		}


		for(Subscriber subscriber: values) {
			if (subscriber.getStreamId().equals(streamId)) {
				list.add(subscriber);
			}
		}


		Iterator<Subscriber> iterator = list.iterator();

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
	public boolean addSubscriber(String streamId, Subscriber subscriber) {
		boolean result = false;

		if (subscriber != null && subscriber.getStreamId() != null && subscriber.getSubscriberId() != null) {
			try {
				subscriberMap.put(subscriber.getSubscriberKey(), subscriber);
				result = true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return result;
	}

	@Override
	public boolean deleteSubscriber(String streamId, String subscriberId) {
		
		boolean result = false;
		if(streamId != null && subscriberId != null) {
			try {
				 Subscriber sub = subscriberMap.remove(Subscriber.getDBKey(streamId, subscriberId));
				result = sub != null;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return result;
	}

	@Override
	public boolean revokeSubscribers(String streamId) {
		boolean result = false;
		Collection<Subscriber> subscriberCollection = subscriberMap.values();

		for (Iterator<Subscriber> iterator = subscriberCollection.iterator(); iterator.hasNext();) {
			Subscriber subscriber =  iterator.next();
			String subscriberStreamId = subscriber.getStreamId();
			if (subscriberStreamId != null && subscriberStreamId.equals(streamId)) {
				iterator.remove();
				subscriberMap.remove(subscriber.getSubscriberKey());
			}
			result = true;

		}
		return result;
	}
	
	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		return subscriberMap.get(Subscriber.getDBKey(streamId, subscriberId));
	}
	
	@Override
	public boolean resetSubscribersConnectedStatus() {
		for(Subscriber subscriber: subscriberMap.values()) {
			if (subscriber != null) {
				subscriber.setConnected(false);
			}
		}
		return true;
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
			if (broadcast != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM || enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM || enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {
				broadcast.setMp4Enabled(enabled);
				broadcastMap.replace(streamId, broadcast);
				result = true;
			}
		}

		return result;
	}
	
	@Override
	public boolean setWebMMuxing(String streamId, int enabled) {
		boolean result = false;

		if (streamId != null) {
			Broadcast broadcast = broadcastMap.get(streamId);
			if (broadcast != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM || enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM || enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {
				broadcast.setWebMEnabled(enabled);
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
	public boolean createConferenceRoom(ConferenceRoom room) {

		boolean result = false;

		if (room != null && room.getRoomId() != null) {
			roomMap.put(room.getRoomId(), room);
			result = true;
		}

		return result;
	}

	@Override
	public boolean editConferenceRoom(String roomId, ConferenceRoom room) {

		boolean result = false;

		if (room != null && room.getRoomId() != null) {
			return roomMap.replace(roomId, room) != null;
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
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search) {
		Collection<ConferenceRoom> values = roomMap.values();

		ArrayList<ConferenceRoom> list = new ArrayList<>();


		for (ConferenceRoom room : values)
		{
			list.add(room);
		}

		if(search != null && !search.isEmpty()){
			logger.info("server side search called for Conference Room = {}", search);
			list = searchOnServerConferenceRoom(list, search);
		}
		return sortAndCropConferenceRoomList(list, offset, size, sortBy, orderBy);
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomName) {
		return roomMap.get(roomName);
	}

	@Override
	public boolean deleteToken(String tokenId) {

		return tokenMap.remove(tokenId) != null;

	}

	@Override
	public Token getToken(String tokenId) {

		return tokenMap.get(tokenId);

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
		boolean result = true;
		Broadcast mainTrack = broadcastMap.get(mainTrackId);
		List<String> subTracks = mainTrack.getSubTrackStreamIds();
		if (subTracks == null) {
			subTracks = new ArrayList<>();
		}
		subTracks.add(subTrackId);
		mainTrack.setSubTrackStreamIds(subTracks);
		broadcastMap.put(mainTrackId, mainTrack);
		return result;
	}
	
	@Override
	public boolean createPlaylist(Playlist playlist) {

		boolean result = false;

		if (playlist != null && playlist.getPlaylistId() != null) {
			playlistMap.put(playlist.getPlaylistId(), playlist);
			result = true;
		}

		return result;
	}
	
	@Override
	public Playlist getPlaylist(String playlistId) {

		return playlistMap.get(playlistId);
	}
	
	@Override
	public boolean deletePlaylist(String playlistId) {
		return playlistMap.remove(playlistId) != null;
	}
	
	@Override
	public boolean editPlaylist(String playlistId,Playlist playlist) {

		boolean result = false;

		if (playlist != null && playlist.getPlaylistId() != null) {
			playlistMap.replace(playlistId, playlist);
			result = true;
		}
		return result;
	}
  
	@Override
	public int resetBroadcasts(String hostAddress) {
		Set<Entry<String,Broadcast>> entrySet = broadcastMap.entrySet();
		
		Iterator<Entry<String, Broadcast>> iterator = entrySet.iterator();
		int i = 0;
		while (iterator.hasNext()) {
			Entry<String, Broadcast> next = iterator.next();
			if (next.getValue().isZombi()) {
				iterator.remove();
				i++;
			}
			if (next.getValue().getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING) ||
					next.getValue().getStatus().equals(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING))
			{
				next.getValue().setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
				next.getValue().setWebRTCViewerCount(0);
				next.getValue().setHlsViewerCount(0);
				next.getValue().setRtmpViewerCount(0);
				i++;
			}
		}
		
		
		return i;
	}

	@Override
	public int getTotalWebRTCViewersCount() {
		long now = System.currentTimeMillis();
		if(now - totalWebRTCViewerCountLastUpdateTime > TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME) {
			int total = 0;
			for (Broadcast broadcast : broadcastMap.values()) {
				total += broadcast.getWebRTCViewerCount();
			}
			totalWebRTCViewerCount = total;
			totalWebRTCViewerCountLastUpdateTime = now;
		}  
		return totalWebRTCViewerCount;
	}
}
