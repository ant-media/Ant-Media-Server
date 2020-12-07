package io.antmedia.datastore.db;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.AggregationOptions;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteResult;

import dev.morphia.utils.IndexType;
import dev.morphia.Datastore;
import dev.morphia.Key;
import dev.morphia.Morphia;
import dev.morphia.aggregation.Group;
import dev.morphia.query.CriteriaContainer;
import dev.morphia.query.Criteria;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;
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

public class MongoStore extends DataStore {

	private static final String TOKEN_ID = "tokenId";
	private static final String STREAM_ID = "streamId";
	private Morphia morphia;
	private Datastore datastore;
	private Datastore vodDatastore;
	private Datastore endpointCredentialsDS;
	private Datastore tokenDatastore;
	private Datastore subscriberDatastore;
	private Datastore detectionMap;
	private Datastore conferenceRoomDatastore;

	protected static Logger logger = LoggerFactory.getLogger(MongoStore.class);

	public static final String IMAGE_ID = "imageId"; 
	public static final String STATUS = "status";
	private static final String ORIGIN_ADDRESS = "originAdress";
	private static final String START_TIME = "startTime"; 
	private static final String DURATION = "duration"; 
	private static final String CREATION_DATE = "creationDate";
	private static final String PLAYLIST_ID = "playlistId";
	private static final String RTMP_VIEWER_COUNT = "rtmpViewerCount";
	private static final String HLS_VIEWER_COUNT = "hlsViewerCount";
	private static final String WEBRTC_VIEWER_COUNT = "webRTCViewerCount";
	
	public MongoStore(String host, String username, String password, String dbName) {
		morphia = new Morphia();
		morphia.mapPackage("io.antmedia.datastore.db.types");

		String uri = getMongoConnectionUri(host, username, password);

		MongoClientURI mongoUri = new MongoClientURI(uri);
		MongoClient client = new MongoClient(mongoUri);

		//TODO: Refactor these stores so that we don't have separate datastore for each class
		datastore = morphia.createDatastore(client, dbName);
		vodDatastore=morphia.createDatastore(client, dbName+"VoD");
		endpointCredentialsDS = morphia.createDatastore(client, dbName+"_endpointCredentials");
		tokenDatastore = morphia.createDatastore(client, dbName + "_token");
		subscriberDatastore = morphia.createDatastore(client, dbName + "_subscriber");
		detectionMap = morphia.createDatastore(client, dbName + "detection");
		conferenceRoomDatastore = morphia.createDatastore(client, dbName + "room");

		//*************************************************
		//do not create data store for each type as we do above
		//*************************************************

		tokenDatastore.ensureIndexes();
		subscriberDatastore.ensureIndexes();
		datastore.ensureIndexes();
		vodDatastore.ensureIndexes();
		endpointCredentialsDS.ensureIndexes();
		detectionMap.ensureIndexes();
		conferenceRoomDatastore.ensureIndexes();
		
		available = true;
	}
	
	public static String getMongoConnectionUri(String host, String username, String password) {
		String credential = "";
		if(username != null && !username.isEmpty()) {
			credential = username+":"+password+"@";
		}

		String uri = "mongodb://"+credential+host;

		logger.info("uri:{}",uri);

		return uri;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * io.antmedia.datastore.db.IDataStore#save(io.antmedia.datastore.db.types.
	 * Broadcast)
	 */
	@Override
	public String save(Broadcast broadcast) {
		if (broadcast == null) {
			return null;
		}
		try {
			String streamId = null;
			if (broadcast.getStreamId() == null) {
				streamId = RandomStringUtils.randomAlphanumeric(12) + System.currentTimeMillis();
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

			synchronized(this) {
				Key<Broadcast> key = datastore.save(broadcast);
			}
			return streamId;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#get(java.lang.String)
	 */
	@Override
	public Broadcast get(String id) {
		synchronized(this) {
			try {
				return datastore.find(Broadcast.class).field(STREAM_ID).equal(id).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	@Override
	public VoD getVoD(String id) {
		synchronized(this) {
			try {
				return vodDatastore.find(VoD.class).field("vodId").equal(id).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#updateStatus(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean updateStatus(String id, String status) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(id);

				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set(STATUS, status);

				if(status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)) {
					ops.set(START_TIME, System.currentTimeMillis());
				}
				else if(status.equals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED)) {
					ops.set(WEBRTC_VIEWER_COUNT, 0);
					ops.set(HLS_VIEWER_COUNT, 0);
					ops.set(RTMP_VIEWER_COUNT, 0);
				}
				
				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#updateDuration(java.lang.String,
	 * long)
	 */
	@Override
	public boolean updateDuration(String id, long duration) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(id);

				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set(DURATION,
						duration);

				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}



	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#addEndpoint(java.lang.String,
	 * io.antmedia.datastore.db.types.Endpoint)
	 */
	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		synchronized(this) {
			if (id != null && endpoint != null) {
				try {
					Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(id);

					UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).push("endPointList",
							endpoint);

					UpdateResults update = datastore.update(query, ops);
					return update.getUpdatedCount() == 1;
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return false;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		boolean result = false;
		synchronized(this) {
			if (id != null && endpoint != null) {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(id);
				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class)

						.removeAll("endPointList", endpoint);
				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			}
		}
		return result;
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		boolean result = false;
		synchronized(this) {
			if (id != null) {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(id);
				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).unset("endPointList");
				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			}
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#getBroadcastCount()
	 */
	@Override
	public long getBroadcastCount() {
		synchronized(this) {
			return datastore.createQuery(Broadcast.class).count();
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#delete(java.lang.String)
	 */
	@Override
	public boolean delete(String id) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(id);
				WriteResult delete = datastore.delete(query);
				return delete.getN() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}
	@Override
	public List<ConferenceRoom> getConferenceRoomList(int offset, int size, String sortBy, String orderBy, String search) {
		synchronized(this) {
			try {
				Query<ConferenceRoom> query = conferenceRoomDatastore.createQuery(ConferenceRoom.class);

				if (size > MAX_ITEM_IN_ONE_LIST) {
					size = MAX_ITEM_IN_ONE_LIST;
				}

				if (sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
					query = query.order(orderBy.equals("desc") ? Sort.descending(sortBy) : Sort.ascending(sortBy));
				}
				if (search != null && !search.isEmpty()) {
					logger.info("Server side search is called for Conference Rooom = {}", search);
					Pattern regexp = Pattern.compile(search, Pattern.CASE_INSENSITIVE);
					query.criteria("roomId").containsIgnoreCase(search);
					return query.find(new FindOptions().skip(offset).limit(size)).toList();
				}
				return query.find(new FindOptions().skip(offset).limit(size)).toList();

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}


	@Override
	public List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.find(Broadcast.class);
			
			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}
			
			if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
				query = query.order(orderBy.equals("desc") ? Sort.descending(sortBy) : Sort.ascending(sortBy));
			}
			if(search != null && !search.isEmpty()){
				logger.info("Server side search is called for {}", search);
				query.or(
						query.criteria("name").containsIgnoreCase(search),
						query.criteria("streamId").containsIgnoreCase(search)
				);
				return query.find(new FindOptions().skip(offset).limit(size)).toList();
			}

			if(type != null && !type.isEmpty()) {
				return query.field("type").equal(type).find(new FindOptions().skip(offset).limit(size)).toList();
			}
			else {
				return query.find(new FindOptions().skip(offset).limit(size)).toList();
			}
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	public Datastore getDataStore() {
		return datastore;
	}




	@Override
	public List<Broadcast> getExternalStreamsList() {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class);
				query.and(
						query.or(
								query.criteria("type").equal(AntMediaApplicationAdapter.IP_CAMERA),
								query.criteria("type").equal(AntMediaApplicationAdapter.STREAM_SOURCE)
								), 
						query.and(
								query.criteria(STATUS).notEqual(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING),
								query.criteria(STATUS).notEqual(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)
								)
						);
				
				List<Broadcast> streamList = query.find().toList();
				
				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set(STATUS, AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING);
				UpdateResults update = datastore.update(query, ops);
				int updatedCount = update.getUpdatedCount();
				if(updatedCount != streamList.size()) {
					logger.error("Only {} stream status updated out of {}", updatedCount, streamList.size());
				}
						
				return streamList;
			} catch (Exception e) {

				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	@Override
	public void close() {
		synchronized(this) {
			available = false;
			datastore.getMongo().close();
		}
	}

	@Override
	public String addVod(VoD vod) {

		String id = null;
		boolean result = false;
		synchronized(this) {
			try {	
				if (vod.getVodId() == null) {
					vod.setVodId(RandomStringUtils.randomAlphanumeric(12) + System.currentTimeMillis());
				}
				vodDatastore.save(vod);
				result = true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}

			if(result) {
				id = vod.getVodId();
			}
		}
		return id;

	}

	@Override
	public List<VoD> getVodList(int offset, int size, String sortBy, String orderBy, String filterStreamId, String search) {
		synchronized(this) {
			Query<VoD> query = vodDatastore.find(VoD.class);
			
			if (filterStreamId != null && !filterStreamId.isEmpty()) {
				query = query.field(STREAM_ID).equal(filterStreamId);
			}
			
			if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
				String sortString = orderBy.contentEquals("desc") ? "-" : "";
				if(sortBy.contentEquals("name")) {
					sortString += "vodName";
				}
				else if(sortBy.contentEquals("date")) {
					sortString += CREATION_DATE;
				}
				query = query.order(sortString);
			}
			if(search != null && !search.isEmpty()){
				logger.info("Server side search is called for VoD, searchString =  {}", search);
				query.or(
						query.criteria("vodName").containsIgnoreCase(search),
						query.criteria("vodId").containsIgnoreCase(search),
						query.criteria("streamName").containsIgnoreCase(search),
						query.criteria("streamId").containsIgnoreCase(search)
				);
				return query.find(new FindOptions().skip(offset).limit(size)).toList();
			}
			return query.find(new FindOptions().skip(offset).limit(size)).toList();
		}
	}


	@Override
	public boolean deleteVod(String id) {
		synchronized(this) {
			try {
				Query<VoD> query = vodDatastore.createQuery(VoD.class).field("vodId").equal(id);
				WriteResult delete = vodDatastore.delete(query);
				return delete.getN() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}



	@Override
	public long getTotalVodNumber() {
		synchronized(this) {
			return vodDatastore.createQuery(VoD.class).count();
		}
	}

	@Override
	public int fetchUserVodList(File userfile) {

		if(userfile==null) {
			return 0;
		}

		int numberOfSavedFiles = 0;
		synchronized(this) {
			try {
				Query<VoD> query = vodDatastore.createQuery(VoD.class).field("type").equal("userVod");
				WriteResult delete = vodDatastore.delete(query);
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}

			File[] listOfFiles = userfile.listFiles();

			if (listOfFiles != null) {

				for (File file : listOfFiles) {

					String fileExtension = FilenameUtils.getExtension(file.getName());

					if (file.isFile() &&
							("mp4".equals(fileExtension) || "flv".equals(fileExtension) || "mkv".equals(fileExtension))) {

						long fileSize = file.length();
						long unixTime = System.currentTimeMillis();


						String filePath=file.getPath();

						String[] subDirs = filePath.split(Pattern.quote(File.separator));

						Integer pathLength=Integer.valueOf(subDirs.length);

						String relativePath = "streams/"+subDirs[pathLength-2]+'/'+subDirs[pathLength-1];
						String vodId = RandomStringUtils.randomNumeric(24);
						VoD newVod = new VoD("vodFile", "vodFile", relativePath, file.getName(), unixTime, 0, fileSize,
								VoD.USER_VOD,vodId);

						addVod(newVod);
						numberOfSavedFiles++;
					}
				}
			}
		}
		return numberOfSavedFiles;

	}




	@Override
	public boolean updateSourceQualityParametersLocal(String id, String quality, double speed, int pendingPacketQueue) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(id);
				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class)
						.set("speed", speed).set("pendingPacketSize", pendingPacketQueue);

				if (quality != null) {
					ops.set("quality", quality);
				}
				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}


	public SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials) {
		SocialEndpointCredentials addedCredential = null;
		synchronized(this) {
			if (credentials != null && credentials.getAccountName() != null && credentials.getAccessToken() != null
					&& credentials.getServiceName() != null) 
			{
				if (credentials.getId() == null) {
					//create new id if id is not set
					//String id = RandomStringUtils.randomAlphanumeric(6);
					//credentials.setId(id);
					endpointCredentialsDS.save(credentials);
					addedCredential = credentials;
				}
				else {
					SocialEndpointCredentials endpointCredentials = getSocialEndpointCredentials(credentials.getId());
					if (endpointCredentials != null) {
						UpdateOperations<SocialEndpointCredentials> ops = endpointCredentialsDS
								.createUpdateOperations(SocialEndpointCredentials.class)
								.set("accessToken", credentials.getAccessToken());
						if (credentials.getAccountId() != null) {
							ops.set("accountId", credentials.getAccountId());
						}
						if (credentials.getAccountName() != null) {
							ops.set("accountName", credentials.getAccountName());
						}
						if (credentials.getAccountType() != null) {
							ops.set("accountType", credentials.getAccountType());
						}
						if (credentials.getAuthTimeInMilliseconds() != null) {
							ops.set("authTimeInMilliseconds", credentials.getAuthTimeInMilliseconds());
						}
						if (credentials.getExpireTimeInSeconds() != null) {
							ops.set("expireTimeInSeconds", credentials.getExpireTimeInSeconds());
						}
						if (credentials.getRefreshToken() != null) {
							ops.set("refreshToken", credentials.getRefreshToken());
						}
						if (credentials.getTokenType() != null) {
							ops.set("tokenType", credentials.getTokenType());
						}



						UpdateResults update = endpointCredentialsDS.update(endpointCredentials, ops);
						addedCredential = credentials;
					}
				}
			}
		}
		return addedCredential;
	}

	@Override
	public List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size) {
		synchronized(this) {
			return endpointCredentialsDS.find(SocialEndpointCredentials.class).find(new FindOptions().skip(offset).limit(size)).toList();
		}
	}

	@Override
	public boolean removeSocialEndpointCredentials(String id) {
		synchronized(this) {
			try {
				Query<SocialEndpointCredentials> query = endpointCredentialsDS
						.createQuery(SocialEndpointCredentials.class)
						.field("id").equal(new ObjectId(id));
				WriteResult delete = endpointCredentialsDS.delete(query);
				return delete.getN() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public SocialEndpointCredentials getSocialEndpointCredentials(String id) {
		synchronized(this) {
			try {
				return endpointCredentialsDS.createQuery(SocialEndpointCredentials.class).field("id").equal(new ObjectId(id)).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	public Datastore getEndpointCredentialsDS() {
		return endpointCredentialsDS;
	}

	public void setEndpointCredentialsDS(Datastore endpointCredentialsDS) {
		this.endpointCredentialsDS = endpointCredentialsDS;
	}

	@Override
	public long getTotalBroadcastNumber() {
		synchronized(this) {
			return datastore.createQuery(Broadcast.class).count();
		}
	}

	@Override
	public long getPartialBroadcastNumber(String search){
		synchronized(this) {
			Query<Broadcast> query = datastore.find(Broadcast.class);
			List<Broadcast> list = null;
			if (search != null && !search.isEmpty()) {
				logger.info("Server side search is called for {}", search);
				query.or(
						query.criteria("name").containsIgnoreCase(search),
						query.criteria("streamId").containsIgnoreCase(search)
				);
				list = query.find(new FindOptions()).toList();
			}
			else{
				return query.find(new FindOptions()).toList().size();
			}
			return list.size();
		}
	}

	@Override
	public long getPartialVodNumber(String search){
		synchronized(this) {
			Query<VoD> query = vodDatastore.find(VoD.class);
			List<VoD> list = null;
			if (search != null && !search.isEmpty()) {
				logger.info("Server side search is called for {}", search);
				query.or(
						query.criteria("vodName").containsIgnoreCase(search),
						query.criteria("vodId").containsIgnoreCase(search),
						query.criteria("streamName").containsIgnoreCase(search),
						query.criteria("streamId").containsIgnoreCase(search)
				);
				list = query.find(new FindOptions()).toList();
			}
			else{
				return query.find(new FindOptions()).toList().size();
			}
			return list.size();
		}
	}

	public Datastore getVodDatastore() {
		return vodDatastore;
	}

	public void setVodDatastore(Datastore vodDatastore) {
		this.vodDatastore = vodDatastore;
	}

	@Override
	public long getActiveBroadcastCount() {
		synchronized(this) {
			return datastore.find(Broadcast.class).filter(STATUS, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING).count();
		}
	}

	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		synchronized(this) {
			if (detectedObjects != null) {
				for (TensorFlowObject tensorFlowObject : detectedObjects) {
					tensorFlowObject.setDetectionTime(timeElapsed);
					tensorFlowObject.setImageId(id);
					detectionMap.save(tensorFlowObject);
				}
			}
		}
	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		synchronized(this) {
			try {
				if (batchSize > MAX_ITEM_IN_ONE_LIST) {
					batchSize = MAX_ITEM_IN_ONE_LIST;
				}
				return detectionMap.find(TensorFlowObject.class).field(IMAGE_ID).startsWith(idFilter).find(new FindOptions().skip(offsetSize).limit(batchSize)).toList();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return null;	
	}

	@Override
	public List<TensorFlowObject> getDetection(String id) {
		synchronized(this) {
			try {
				return detectionMap.find(TensorFlowObject.class).field(IMAGE_ID).equal(id).find().toList();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return null;	
	}

	@Override
	public long getObjectDetectedTotal(String id) {
		synchronized(this) {
			return detectionMap.find(TensorFlowObject.class).field(IMAGE_ID).equal(id).count();
		}
	}



	@Override
	public boolean updateBroadcastFields(String streamId, Broadcast broadcast) {
		boolean result = false;
		synchronized(this) {
			try {
				logger.warn("result inside edit camera: {}" , result);
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(streamId);

				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class);
				if (broadcast.getName() != null) {
					ops.set("name", broadcast.getName());
				}

				if (broadcast.getDescription() != null) {
					ops.set("description", broadcast.getDescription());
				}

				if (broadcast.getUsername() != null) {
					ops.set("username", broadcast.getUsername());
				}

				if (broadcast.getPassword() != null) {
					ops.set("password", broadcast.getPassword());
				}

				if (broadcast.getIpAddr() != null) {
					ops.set("ipAddr", broadcast.getIpAddr());
				}

				if ( broadcast.getStreamUrl() != null) {
					ops.set("streamUrl", broadcast.getStreamUrl());
				}
				
				if (broadcast.getLatitude() != null) {
					ops.set("latitude", broadcast.getLatitude());
				}
				
				if (broadcast.getLongitude() != null) {
					ops.set("longitude", broadcast.getLongitude());
				}
				
				if (broadcast.getAltitude() != null) {
					ops.set("altitude", broadcast.getAltitude());
				}
				
				if (broadcast.getMainTrackStreamId() != null) {
					ops.set("mainTrackStreamId", broadcast.getMainTrackStreamId());
				}
				
				prepareFields(broadcast, ops);
				
				
				ops.set("receivedBytes", broadcast.getReceivedBytes());
				ops.set("bitrate", broadcast.getBitrate());
				ops.set("userAgent", broadcast.getUserAgent());
				ops.set("webRTCViewerLimit", broadcast.getWebRTCViewerLimit());
				ops.set("hlsViewerLimit", broadcast.getHlsViewerLimit());
				
				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return false;
	}
	
	private void prepareFields(Broadcast broadcast, UpdateOperations<Broadcast> ops ) {
		
		if ( broadcast.getDuration() != 0) {
			ops.set(DURATION, broadcast.getDuration());
		}
		
		if (broadcast.getStartTime() != 0) {
			ops.set(START_TIME, broadcast.getStartTime());
		}
		
		if (broadcast.getOriginAdress() != null) {
			ops.set(ORIGIN_ADDRESS, broadcast.getOriginAdress());
		}
		
		if (broadcast.getStatus() != null) {
			ops.set(STATUS, broadcast.getStatus());
		}
		
		if (broadcast.getAbsoluteStartTimeMs() != 0) {
			ops.set("absoluteStartTimeMs", broadcast.getAbsoluteStartTimeMs());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(streamId);
				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).inc(HLS_VIEWER_COUNT, diffCount);

				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateWebRTCViewerCountLocal(String streamId, boolean increment) {
		return updateViewerField(streamId, increment, WEBRTC_VIEWER_COUNT);
	}

	@Override
	public boolean updateRtmpViewerCountLocal(String streamId, boolean increment) {
		return updateViewerField(streamId, increment, RTMP_VIEWER_COUNT);
	}

	private boolean updateViewerField(String streamId, boolean increment, String fieldName) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(streamId);
				
				if(!increment) {
					query = query.filter(fieldName+" >",0);
				}
				
				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class);
				String field = fieldName;
				if (increment) {
					ops.inc(field);
				}
				else {
					ops.dec(field);
				}

				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return false;
	}


	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		synchronized(this) {
			Query<StreamInfo> query = datastore.createQuery(StreamInfo.class);
			
			List<Criteria> criteriaList = new ArrayList<>();
			if (streamInfo.getVideoPort() != 0) {
				criteriaList.add(query.criteria("videoPort").equal(streamInfo.getVideoPort()));
				criteriaList.add(query.criteria("audioPort").equal(streamInfo.getVideoPort()));
				criteriaList.add(query.criteria("dataChannelPort").equal(streamInfo.getVideoPort()));
			}
			if (streamInfo.getAudioPort() != 0) {
				criteriaList.add(query.criteria("videoPort").equal(streamInfo.getAudioPort()));
				criteriaList.add(query.criteria("audioPort").equal(streamInfo.getAudioPort()));
				criteriaList.add(query.criteria("dataChannelPort").equal(streamInfo.getAudioPort()));
			}
			
			if (streamInfo.getDataChannelPort() != 0) {
				criteriaList.add(query.criteria("videoPort").equal(streamInfo.getDataChannelPort()));
				criteriaList.add(query.criteria("audioPort").equal(streamInfo.getDataChannelPort()));
				criteriaList.add(query.criteria("dataChannelPort").equal(streamInfo.getDataChannelPort()));
			}
			
			Criteria[] criteriaArray = new Criteria[criteriaList.size()];
			criteriaList.toArray(criteriaArray);
			if (criteriaArray.length > 0) {
				query.and(
						query.criteria("host").equal(streamInfo.getHost()),
						query.or(
								criteriaArray
								)
						);
			}
			else {
				query.and(
						query.criteria("host").equal(streamInfo.getHost())
						);
			}
			
			long count = query.count();
			if(count > 0) {
				logger.error("{} port duplications are detected for host: {}, video port: {}, audio port:{}",
						count, streamInfo.getHost(), streamInfo.getVideoPort(), streamInfo.getAudioPort());

				WriteResult res = datastore.delete(query);
				if(res.getN() != count) {
					logger.error("Only {} stream info were deleted out of {} having duplicated port.", res.getN(), count);
				}
			}
			datastore.save(streamInfo);
		}
	}

	@Override
	public void addStreamInfoList(List<StreamInfo> streamInfoList) {
		synchronized(this) {
			for (StreamInfo streamInfo : streamInfoList) {
				datastore.save(streamInfo);
			}
		}
	}

	public List<StreamInfo> getStreamInfoList(String streamId) {
		synchronized(this) {
			return datastore.find(StreamInfo.class).field(STREAM_ID).equal(streamId).find().toList();
		}
	}

	public void clearStreamInfoList(String streamId) {
		synchronized(this) {
			Query<StreamInfo> query = datastore.createQuery(StreamInfo.class).field(STREAM_ID).equal(streamId);
			long count = query.count();
			WriteResult res = datastore.delete(query);

			if(res.getN() != count) {
				logger.error("{} StreamInfo were deleted out of {} for stream {}",res.getN(), count, streamId);
			}
		}
	}

	@Override
	public boolean saveToken(Token token) {
		boolean result = false;
		synchronized(this) {
			if(token.getStreamId() != null && token.getTokenId() != null) {

				try {
					tokenDatastore.save(token);
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
		synchronized(this) {
			if (token.getTokenId() != null) {
				fetchedToken = tokenDatastore.find(Token.class).field(TOKEN_ID).equal(token.getTokenId()).first();
				if (fetchedToken != null 
						&& fetchedToken.getType().equals(token.getType())
						&& Instant.now().getEpochSecond() < fetchedToken.getExpireDate()) {
					if(token.getRoomId() == null || token.getRoomId().isEmpty()) {

						if(fetchedToken.getStreamId().equals(token.getStreamId())) {	
							Query<Token> query = tokenDatastore.createQuery(Token.class).field(TOKEN_ID).equal(token.getTokenId());
							tokenDatastore.delete(query);
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
		}
		return fetchedToken;
	}

	@Override
	public boolean revokeTokens(String streamId) {
		synchronized(this) {
			Query<Token> query = tokenDatastore.createQuery(Token.class).field(STREAM_ID).equal(streamId);
			WriteResult delete = tokenDatastore.delete(query);

			return delete.getN() >= 1;
		}
	}

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {
		synchronized(this) {
			return 	tokenDatastore.find(Token.class).field(STREAM_ID).equal(streamId).asList(new FindOptions() .skip(offset).limit(size));
		}
	}
	
	@Override
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
		synchronized(this) {
			return 	subscriberDatastore.find(Subscriber.class).field("streamId").equal(streamId).asList(new FindOptions() .skip(offset).limit(size));
		}
	}


	@Override
	public boolean addSubscriber(String streamId, Subscriber subscriber) {
		boolean result = false;
		if (subscriber != null) {
			synchronized (this) {
				if (subscriber.getStreamId() != null && subscriber.getSubscriberId() != null) {
					try {
						subscriberDatastore.save(subscriber);
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
		synchronized(this) {
			try {
				Query<Subscriber> query = subscriberDatastore.createQuery(Subscriber.class).field("streamId").equal(streamId).field("subscriberId").equal(subscriberId);
				WriteResult delete = subscriberDatastore.delete(query);
				result = delete.getN() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return result;
	}

	@Override
	public boolean revokeSubscribers(String streamId) {
		synchronized(this) {
			Query<Subscriber> query = subscriberDatastore.createQuery(Subscriber.class).field("streamId").equal(streamId);
			WriteResult delete = subscriberDatastore.delete(query);

			return delete.getN() >= 1;
		}
	}
	
	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		Subscriber subscriber = null;
		if (subscriberId != null && streamId != null) {
			synchronized (this) {
				try {
					subscriber = subscriberDatastore.find(Subscriber.class).field("subscriberId").equal(subscriberId)
							.field("streamId").equal(streamId).get();
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return subscriber;
	}
	
	@Override
	public boolean resetSubscribersConnectedStatus() {
		boolean result = false;
		synchronized (this) {
			try {
			Query<Subscriber> query = subscriberDatastore.createQuery(Subscriber.class);
			UpdateOperations<Subscriber> ops = subscriberDatastore.createUpdateOperations(Subscriber.class).set("connected", false);

			subscriberDatastore.update(query, ops);
			result = true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return result;
	}	

	@Override
	public boolean setMp4Muxing(String streamId, int enabled) {
		return setRecordMuxing(streamId, enabled, "mp4Enabled");
	}
	
	@Override
	public boolean setWebMMuxing(String streamId, int enabled) {
		return setRecordMuxing(streamId, enabled, "webMEnabled");
	}
	
	private boolean setRecordMuxing(String streamId, int enabled, String field) {
		synchronized(this) {
			try {
				if (streamId != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM || enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM || enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {
					Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(streamId);
					UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set(field, enabled);
					UpdateResults update = datastore.update(query, ops);
					return update.getUpdatedCount() == 1;
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return false;

	}

	@Override
	public boolean createConferenceRoom(ConferenceRoom room) {
		boolean result = false;
		synchronized(this) {
			if(room != null && room.getRoomId() != null) {

				try {
					conferenceRoomDatastore.save(room);
					result = true;

				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		return result;
	}

	@Override
	public boolean editConferenceRoom(String roomId, ConferenceRoom room) {
		boolean result = false;
		synchronized(this) {
			try {
				Query<ConferenceRoom> query = conferenceRoomDatastore.createQuery(ConferenceRoom.class).field("roomId").equal(roomId);

				UpdateOperations<ConferenceRoom> ops = conferenceRoomDatastore.createUpdateOperations(ConferenceRoom.class).set("roomId", room.getRoomId())
						.set("startDate", room.getStartDate()).set("endDate", room.getEndDate())
						.set("roomStreamList", room.getRoomStreamList());

				UpdateResults update = conferenceRoomDatastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return result;
	}

	@Override
	public boolean deleteConferenceRoom(String roomId) {
		synchronized(this) {
			try {
				Query<ConferenceRoom> query = conferenceRoomDatastore.createQuery(ConferenceRoom.class).field("roomId").equal(roomId);
				WriteResult delete = conferenceRoomDatastore.delete(query);
				return delete.getN() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public ConferenceRoom getConferenceRoom(String roomId) {
		synchronized(this) {
			try {
				return conferenceRoomDatastore.find(ConferenceRoom.class).field("roomId").equal(roomId).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	@Override
	public boolean deleteToken(String tokenId) {
		boolean result = false;
		synchronized(this) {
			try {
				Query<Token> query = tokenDatastore.createQuery(Token.class).field(TOKEN_ID).equal(tokenId);
				WriteResult delete = tokenDatastore.delete(query);
				result = delete.getN() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return result;
	}

	@Override
	public Token getToken(String tokenId) {
		Token token = null;

		synchronized(this) {
			try {
				token =  tokenDatastore.find(Token.class).field(TOKEN_ID).equal(tokenId).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return token;
	}
	
	@Override
	public long getLocalLiveBroadcastCount(String hostAddress) {
		synchronized(this) {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class);
			query.and(
					query.or(
							query.criteria(ORIGIN_ADDRESS).doesNotExist(), //check for non cluster mode
							query.criteria(ORIGIN_ADDRESS).equal(hostAddress)
							),
					query.criteria(STATUS).equal(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING)
					);
			return query.count();
		}
	}

	@Override
	public boolean createP2PConnection(P2PConnection conn) {
		synchronized(this) {
			try {
				datastore.save(conn);
				return true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public boolean deleteP2PConnection(String streamId) {
		synchronized(this) {
			try {
				Query<P2PConnection> query = datastore.createQuery(P2PConnection.class).field(STREAM_ID).equal(streamId);
				WriteResult delete = datastore.delete(query);
				return (delete.getN() == 1);
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}
	
	@Override
	public P2PConnection getP2PConnection(String streamId) {
		synchronized(this) {
			try {
				return datastore.find(P2PConnection.class).field(STREAM_ID).equal(streamId).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	@Override
	public boolean addSubTrack(String mainTrackId, String subTrackId) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field(STREAM_ID).equal(mainTrackId);

				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).push("subTrackStreamIds",
						subTrackId);

				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}
	
	
	@Override
	public boolean createPlaylist(Playlist playlist) {
		synchronized(this) {
			try {
				datastore.save(playlist);
				return true;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public Playlist getPlaylist(String playlistId) {
		synchronized(this) {
			try {
				return datastore.find(Playlist.class).field(PLAYLIST_ID).equal(playlistId).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	@Override
	public boolean deletePlaylist(String playlistId) {
		synchronized(this) {
			try {
				Query<Playlist> query = datastore.createQuery(Playlist.class).field(PLAYLIST_ID).equal(playlistId);
				WriteResult delete = datastore.delete(query);
				return (delete.getN() == 1);
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public boolean editPlaylist(String playlistId, Playlist playlist) {
		boolean result = false;
		synchronized(this) {
			try {
				Query<Playlist> query = datastore.createQuery(Playlist.class).field(PLAYLIST_ID).equal(playlist.getPlaylistId());

				UpdateOperations<Playlist> ops = datastore.createUpdateOperations(Playlist.class).set(PLAYLIST_ID, playlist.getPlaylistId())
						.set("playlistName", playlist.getPlaylistName()).set("playlistStatus", playlist.getPlaylistStatus())
						.set(CREATION_DATE, playlist.getCreationDate()).set(DURATION, playlist.getDuration())
						.set("broadcastItemList", playlist.getBroadcastItemList());

				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return result;
	}
	
	@Override
	public int resetBroadcasts(String hostAddress) 
	{
		int totalOperationCount = 0;
		synchronized(this) {
			{
				//delete zombi streams that are belong to origin address
				Query<Broadcast> query = datastore.createQuery(Broadcast.class);
				query.and(
						query.or(
								query.criteria(ORIGIN_ADDRESS).doesNotExist(), //check for non cluster mode
								query.criteria(ORIGIN_ADDRESS).equal(hostAddress)
								),
						query.criteria("zombi").equal(true)
						);
				long count = query.count();
				
				if(count > 0) 
				{
					logger.error("There are {} streams for {} at start. They are deleted now.", count, hostAddress);
	
					WriteResult res = datastore.delete(query);
					if(res.getN() != count) {
						logger.error("Only {} streams were deleted out of {} streams.", res.getN(), count);
					}
					totalOperationCount += res.getN();
				}
			}
			
			{
				//reset the broadcasts viewer numbers
				Query<Broadcast> queryUpdateStatus = datastore.createQuery(Broadcast.class);
				queryUpdateStatus.or(queryUpdateStatus.criteria(ORIGIN_ADDRESS).equal(hostAddress),
						queryUpdateStatus.criteria(ORIGIN_ADDRESS).doesNotExist());
				
				long broadcastCount = queryUpdateStatus.count();
	
				if (broadcastCount > 0) 
				{
					UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class);
					ops.set(WEBRTC_VIEWER_COUNT, 0);
					ops.set(HLS_VIEWER_COUNT, 0);
					ops.set(RTMP_VIEWER_COUNT, 0);
					ops.set(STATUS, AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
					
					UpdateResults update = datastore.update(queryUpdateStatus, ops);
					
					if (update.getUpdatedCount() == broadcastCount) 
					{
						logger.info("{} of Broadcasts are reset. ", broadcastCount);
					}
					else 
					{
						logger.error("Broadcast reset count is not correct. {} stream info were updated out of {} streams.", update.getUpdatedCount(), broadcastCount);
					}
					
					totalOperationCount += update.getUpdatedCount();
				}
				
			}
			
			{
				//delete streaminfo 
				Query<StreamInfo> querySI = datastore.createQuery(StreamInfo.class).field("host").equal(hostAddress);
				long count = querySI.count();
				if(count > 0) 
				{
					logger.error("There are {} stream info adressing {} at start. They are deleted now.", count, hostAddress);
					WriteResult res = datastore.delete(querySI);
					if(res.getN() != count) {
						logger.error("Only {} stream info were deleted out of {} streams.", res.getN(), count);
					}
					totalOperationCount += res.getN();
				}
			}
			
		}
		
		return totalOperationCount;
	}

	
	static class Summation {
		private int total;
		public int getTotal() {
			return total;
		}
		public void setTotal(int total) {
			this.total = total;
		}
	}

	@Override
	public int getTotalWebRTCViewersCount() {
		long now = System.currentTimeMillis();
		if(now - totalWebRTCViewerCountLastUpdateTime > TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME) {
			synchronized(this) {
				int total = 0;
				Query<Broadcast> query = datastore.createQuery(Broadcast.class);
				query.field(STATUS).equal(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

				Iterator<Summation> result = datastore.createAggregation(Broadcast.class)
						.match(query)
						.group("AllBroadcasts", Group.grouping("total", Group.sum(WEBRTC_VIEWER_COUNT)))
						.aggregate(Summation.class, AggregationOptions.builder().build());

				if(result.hasNext()) {
					total = ((Summation) result.next()).getTotal();
				}
				
				totalWebRTCViewerCount = total;
				totalWebRTCViewerCountLastUpdateTime = now;
			}
		}
		return totalWebRTCViewerCount;
	}
}
