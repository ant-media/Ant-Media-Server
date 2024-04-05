package io.antmedia.datastore.db;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.set;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.Morphia;
import dev.morphia.UpdateOptions;
import dev.morphia.aggregation.stages.Group;
import dev.morphia.annotations.Entity;
import dev.morphia.query.FindOptions;
import dev.morphia.query.MorphiaCursor;
import dev.morphia.query.Query;
import dev.morphia.query.Sort;
import dev.morphia.query.Update;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.PushNotificationToken;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberMetadata;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;

public class MongoStore extends DataStore {

	public static final String VOD_ID = "vodId";
	private static final String VIEWER_ID = "viewerId";
	private static final String TOKEN_ID = "tokenId";
	public static final String STREAM_ID = "streamId";
	public static final String SUBSCRIBER_ID = "subscriberId"; 
	private Datastore datastore;
	private Datastore vodDatastore;
	private Datastore tokenDatastore;
	private Datastore subscriberDatastore;
	private Datastore detectionMap;
	private Datastore conferenceRoomDatastore;
	private MongoClient mongoClient;

	protected static Logger logger = LoggerFactory.getLogger(MongoStore.class);

	public static final String IMAGE_ID = "imageId"; 
	public static final String STATUS = "status";
	private static final String ORIGIN_ADDRESS = "originAdress";
	private static final String START_TIME = "startTime"; 
	private static final String DURATION = "duration"; 
	private static final String CREATION_DATE = "creationDate";
	private static final String RTMP_VIEWER_COUNT = "rtmpViewerCount";
	private static final String HLS_VIEWER_COUNT = "hlsViewerCount";
	private static final String DASH_VIEWER_COUNT = "dashViewerCount";
	private static final String WEBRTC_VIEWER_COUNT = "webRTCViewerCount";
	private static final String META_DATA = "metaData";

	public MongoStore(String host, String username, String password, String dbName) {

		String uri = getMongoConnectionUri(host, username, password);


		mongoClient = MongoClients.create(uri);


		//TODO: Refactor these stores so that we don't have separate datastore for each class
		datastore = Morphia.createDatastore(mongoClient, dbName);
		vodDatastore = Morphia.createDatastore(mongoClient, dbName+"VoD");
		tokenDatastore = Morphia.createDatastore(mongoClient, dbName + "_token");
		subscriberDatastore = Morphia.createDatastore(mongoClient, dbName + "_subscriber");
		detectionMap = Morphia.createDatastore(mongoClient, dbName + "detection");
		conferenceRoomDatastore = Morphia.createDatastore(mongoClient, dbName + "room");

		//*************************************************
		//do not create data store for each type as we do above
		//*************************************************
		datastore.getMapper().mapPackage("io.antmedia.datastore.db.types");
		
		
		//TODO: only map related class not all of them
		tokenDatastore.getMapper().mapPackage("io.antmedia.datastore.db.types");
		subscriberDatastore.getMapper().mapPackage("io.antmedia.datastore.db.types");
		vodDatastore.getMapper().mapPackage("io.antmedia.datastore.db.types");
		detectionMap.getMapper().mapPackage("io.antmedia.datastore.db.types");
		conferenceRoomDatastore.getMapper().mapPackage("io.antmedia.datastore.db.types");
		
		tokenDatastore.ensureIndexes();
		subscriberDatastore.ensureIndexes();
		datastore.ensureIndexes();
		vodDatastore.ensureIndexes();
		detectionMap.ensureIndexes();
		conferenceRoomDatastore.ensureIndexes();

		available = true;
	}

	public static String getMongoConnectionUri(String host, String username, String password) {
		//If it is DNS seed name, no need to check for username and password since it needs to be integrated to the given uri.
		//Mongodb Atlas users will have such syntax and won't need to enter seperate username and password to the script since it is already in the uri.
		
		//if host includes starts with mongodb:// or mongodb+srv://, let's use the connection string and don't build new one
		if(host.indexOf("mongodb://") == 0 || host.indexOf("mongodb+srv://") == 0)
			return host;
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
			Broadcast updatedBroadcast = super.saveBroadcast(broadcast);
			String streamId = updatedBroadcast.getStreamId();

			synchronized(this) {
				datastore.save(broadcast);
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
				return datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id)).first();
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
				return vodDatastore.find(VoD.class).filter(Filters.eq(VOD_ID,id)).first();
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
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));

				Update<Broadcast> ops = query.update(set(STATUS, status));

				if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) 
				{
					ops.add(set(START_TIME, System.currentTimeMillis()));
				}
				else if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED)) 
				{
					ops.add(set(WEBRTC_VIEWER_COUNT, 0));
					ops.add(set(HLS_VIEWER_COUNT, 0));
					ops.add(set(RTMP_VIEWER_COUNT, 0));
					ops.add(set(DASH_VIEWER_COUNT, 0));
				}

				UpdateResult update = ops.execute();
				return update.getMatchedCount() == 1;
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
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));
				return query.update(set(DURATION, duration)).execute().getMatchedCount() == 1;
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
					Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));

					return query.update(UpdateOperators.push("endPointList", endpoint)).execute().getMatchedCount() == 1;
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
			if (id != null && endpoint != null) 
			{
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));

				Update<Broadcast> update = query.update(UpdateOperators.pullAll("endPointList", Arrays.asList(endpoint)));

				return update.execute().getMatchedCount() == 1;
			}
		}
		return result;
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		boolean result = false;
		synchronized(this) {
			if (id != null) {
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));
				return query.update(UpdateOperators.unset("endPointList")).execute().getMatchedCount() == 1;
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
			return datastore.find(Broadcast.class).count();
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
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));
				return query.delete().getDeletedCount() == 1;
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
				Query<ConferenceRoom> query = conferenceRoomDatastore.find(ConferenceRoom.class);

				if (size > MAX_ITEM_IN_ONE_LIST) {
					size = MAX_ITEM_IN_ONE_LIST;
				}

				FindOptions findingOptions = new FindOptions().skip(offset).limit(size);
				if (sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) 
				{
					findingOptions.sort(orderBy.equals("desc") ? Sort.descending(sortBy) : Sort.ascending(sortBy));
				}
				if (search != null && !search.isEmpty()) 
				{
					logger.info("Server side search is called for Conference Rooom = {}", search);
					query.filter(
							Filters.regex("roomId").caseInsensitive().pattern(".*" + search + ".*")
							
				    );
				}
				return query.iterator(findingOptions).toList();

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}

	private boolean checkIfRegexValid(String regex) {
		try {
			Pattern.compile(regex);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}


	@Override
	public List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.find(Broadcast.class);
				datastore.ensureIndexes();

				if (size > MAX_ITEM_IN_ONE_LIST) {
					size = MAX_ITEM_IN_ONE_LIST;
				}
				FindOptions findingOptions = new FindOptions().skip(offset).limit(size);

				if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
					findingOptions.sort(orderBy.equals("desc") ? Sort.descending(sortBy) : Sort.ascending(sortBy));

				}
				if(search != null && !search.isEmpty())
				{
					logger.info("Server side search in broadcast for the text -> {}", search);

					// if search is not a valid regex, then search as a text in name
					if (!checkIfRegexValid(search)) {
						query.filter(Filters.text(search));
					} else {
						query.filter(Filters.or(
								Filters.regex(STREAM_ID).caseInsensitive().pattern(".*" + search + ".*"),
								Filters.regex("name").caseInsensitive().pattern(".*" + search + ".*")
								)
						);
					}
					
					
					
				}

				if(type != null && !type.isEmpty()) {
					query.filter(Filters.eq("type", type));
				}

				return query.iterator(findingOptions).toList();

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
				Query<Broadcast> query = datastore.find(Broadcast.class);

				query.filter(
						Filters.and(
								Filters.or(Filters.eq("type", AntMediaApplicationAdapter.IP_CAMERA), Filters.eq("type", AntMediaApplicationAdapter.STREAM_SOURCE)),
						Filters.and(Filters.ne(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING), Filters.ne(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)))
						);

				List<Broadcast> streamList = query.iterator().toList();
				final UpdateResult results = query.update(new UpdateOptions().multi(true), set(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING));
				long updatedCount = results.getModifiedCount();
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
	public void close(boolean deleteDB) {
		synchronized(this) {
			available = false;
			if (deleteDB) {
				mongoClient.getDatabase(tokenDatastore.getDatabase().getName()).drop();
				mongoClient.getDatabase(subscriberDatastore.getDatabase().getName()).drop();
				mongoClient.getDatabase(datastore.getDatabase().getName()).drop();
				mongoClient.getDatabase(vodDatastore.getDatabase().getName()).drop();
				mongoClient.getDatabase(detectionMap.getDatabase().getName()).drop();
				mongoClient.getDatabase(conferenceRoomDatastore.getDatabase().getName()).drop();
			}
			mongoClient.close();
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
				query.filter(Filters.eq(STREAM_ID, filterStreamId));
			}

			FindOptions findOptions = new FindOptions().skip(offset).limit(size);
			if(sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
				String field;
				if(sortBy.contentEquals("name")) {
					field = "vodName";
				}
				else { // sortBy can be "date" . Let's make it default
					field = CREATION_DATE;
				}
				findOptions.sort(orderBy.contentEquals("desc") ? Sort.descending(field) : Sort.ascending(field));
			}
			if(search != null && !search.isEmpty())
			{
				logger.info("Server side search is called for VoD, searchString =  {}", search);
				
				query.filter(Filters.or(
						Filters.regex(STREAM_ID).caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("streamName").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex(VOD_ID).caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("vodName").caseInsensitive().pattern(".*" + search + ".*")
						)
			    );

			}
			return query.iterator(findOptions).toList();
		}
	}


	@Override
	public boolean deleteVod(String id) {
		synchronized(this) {
			try {
				Query<VoD> query = vodDatastore.find(VoD.class).filter(Filters.eq(VOD_ID, id));
				return query.delete().getDeletedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}



	@Override
	public long getTotalVodNumber() {
		synchronized(this) {
			return vodDatastore.find(VoD.class).count();
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
				vodDatastore.find(VoD.class).filter(Filters.eq("type", "userVod")).delete(new DeleteOptions().multi(true));
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
						VoD newVod = new VoD("vodFile", "vodFile", relativePath, file.getName(), unixTime, 0, 0, fileSize,
								VoD.USER_VOD,vodId, null);

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
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));
				List<UpdateOperator> updateOperators = new ArrayList<>();
				updateOperators.add(set("speed", speed));
				updateOperators.add(set("pendingPacketSize", pendingPacketQueue));

				if (quality != null) {
					updateOperators.add(set("quality", quality));
				}
				return query.update(updateOperators).execute().getModifiedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public long getTotalBroadcastNumber() {
		synchronized(this) {
			return datastore.find(Broadcast.class).count();
		}
	}

	@Override
	public long getPartialBroadcastNumber(String search){
		synchronized(this) {
			Query<Broadcast> query = datastore.find(Broadcast.class);
			if (search != null && !search.isEmpty()) 
			{
				logger.info("Server side search is called for {}", search);
				query.filter(Filters.or(
						Filters.regex("streamId").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("name").caseInsensitive().pattern(".*" + search + ".*")
						)
			    );
			}

			return query.count();
		}
	}

	@Override
	public long getPartialVodNumber(String search)
	{
		synchronized(this) {
			Query<VoD> query = vodDatastore.find(VoD.class);
			if (search != null && !search.isEmpty()) 
			{
				logger.info("Server side search is called for {}", search);
				query.filter(Filters.or(
						Filters.regex("streamId").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("streamName").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex(VOD_ID).caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("vodName").caseInsensitive().pattern(".*" + search + ".*")
						));
			}
			return query.count();
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
			return datastore.find(Broadcast.class).filter(Filters.eq(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)).count();
		}
	}

	@Override
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
				return detectionMap.find(TensorFlowObject.class).iterator(new FindOptions().skip(offsetSize).limit(batchSize)).toList();
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
				return detectionMap.find(TensorFlowObject.class).filter(Filters.eq(IMAGE_ID, id)).iterator().toList();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return null;	
	}

	@Override
	public long getObjectDetectedTotal(String id) {
		synchronized(this) {
			return detectionMap.find(TensorFlowObject.class).filter(Filters.eq(IMAGE_ID, id)).count();
		}
	}



	@Override
	public boolean updateBroadcastFields(String streamId, Broadcast broadcast) {
		boolean result = false;
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));

				List<UpdateOperator> updates = new ArrayList<>();

				if (broadcast.getName() != null) {
					updates.add(set("name",  broadcast.getName()));
				}

				if (broadcast.getDescription() != null) {
					updates.add(set("description", broadcast.getDescription()));
				}

				if (broadcast.getUsername() != null) {
					updates.add(set("username", broadcast.getUsername()));
				}

				if (broadcast.getPassword() != null) {
					updates.add(set("password", broadcast.getPassword()));
				}

				if (broadcast.getIpAddr() != null) {
					updates.add(set("ipAddr", broadcast.getIpAddr()));
				}

				if ( broadcast.getStreamUrl() != null) {
					updates.add(set("streamUrl", broadcast.getStreamUrl()));
				}

				if (broadcast.getLatitude() != null) {
					updates.add(set("latitude", broadcast.getLatitude()));
				}

				if (broadcast.getLongitude() != null) {
					updates.add(set("longitude", broadcast.getLongitude()));
				}

				if (broadcast.getAltitude() != null) {
					updates.add(set("altitude", broadcast.getAltitude()));
				}

				if (broadcast.getMainTrackStreamId() != null) {
					updates.add(set("mainTrackStreamId", broadcast.getMainTrackStreamId()));
				}

				if (broadcast.getPlayListItemList() != null) {
					updates.add(set("playListItemList", broadcast.getPlayListItemList()));
				}

				if (broadcast.getPlayListStatus() != null) {
					updates.add(set("playListStatus", broadcast.getPlayListStatus()));
				}

				if (broadcast.getEndPointList() != null) {
					updates.add(set("endPointList", broadcast.getEndPointList()));
				}

				if (broadcast.getSubFolder() != null) {
					updates.add(set("subFolder", broadcast.getSubFolder()));
				}
				
				if (broadcast.getListenerHookURL() != null && !broadcast.getListenerHookURL().isEmpty()) {
					updates.add(set("listenerHookURL", broadcast.getListenerHookURL()));
				}
				if (broadcast.getSpeed() != 0) {
					updates.add(set("speed", broadcast.getSpeed()));
				}
				

				prepareFields(broadcast, updates);

				updates.add(set("plannedStartDate", broadcast.getPlannedStartDate()));
				updates.add(set("seekTimeInMs", broadcast.getSeekTimeInMs()));
				updates.add(set("currentPlayIndex", broadcast.getCurrentPlayIndex()));
				updates.add(set("receivedBytes", broadcast.getReceivedBytes()));
				updates.add(set("bitrate", broadcast.getBitrate()));
				updates.add(set("userAgent", broadcast.getUserAgent()));
				updates.add(set("webRTCViewerLimit", broadcast.getWebRTCViewerLimit()));
				updates.add(set("hlsViewerLimit", broadcast.getHlsViewerLimit()));
				updates.add(set("dashViewerLimit", broadcast.getDashViewerLimit()));
				updates.add(set("subTrackStreamIds", broadcast.getSubTrackStreamIds()));
				updates.add(set(META_DATA, broadcast.getMetaData()));
				updates.add(set("playlistLoopEnabled", broadcast.isPlaylistLoopEnabled()));
				updates.add(set("updateTime", broadcast.getUpdateTime()));
				updates.add(set("autoStartStopEnabled",broadcast.isAutoStartStopEnabled()));

				UpdateResult updateResult = query.update(updates).execute();
				return updateResult.getModifiedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return false;
	}

	private void prepareFields(Broadcast broadcast, List<UpdateOperator> updates) {

		if ( broadcast.getDuration() != 0) {
			updates.add(set(DURATION, broadcast.getDuration()));
		}

		if (broadcast.getStartTime() != 0) {
			updates.add(set(START_TIME, broadcast.getStartTime()));
		}

		if (broadcast.getOriginAdress() != null) {
			updates.add(set(ORIGIN_ADDRESS, broadcast.getOriginAdress()));
		}

		if (broadcast.getStatus() != null) {
			updates.add(set(STATUS, broadcast.getStatus()));
		}

		if (broadcast.getAbsoluteStartTimeMs() != 0) {
			updates.add(set("absoluteStartTimeMs", broadcast.getAbsoluteStartTimeMs()));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));
				UpdateResult result = query.update(inc(HLS_VIEWER_COUNT, diffCount)).execute();

				return result.getMatchedCount() == 1;
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
	public boolean updateDASHViewerCountLocal(String streamId, int diffCount) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));
				UpdateResult result = query.update(inc(DASH_VIEWER_COUNT, diffCount)).execute();

				return result.getMatchedCount() == 1;
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
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));

				if(!increment) {
					query.filter(Filters.gt(fieldName, 0));
				}

				UpdateResult updateResult = null;
				if (increment) {
					updateResult = query.update(inc(fieldName)).execute();
				}
				else {
					updateResult = query.update(UpdateOperators.dec(fieldName)).execute();
				}

				return updateResult.getModifiedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return false;
	}


	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		synchronized(this) {
			//TODO: Why do we run find(StreamInfo.class)
			datastore.find(StreamInfo.class);
			datastore.save(streamInfo);
		}
	}

	public List<StreamInfo> getStreamInfoList(String streamId) {
		synchronized(this) {
			return datastore.find(StreamInfo.class).filter(Filters.eq(STREAM_ID, streamId)).iterator().toList();
		}
	}

	public void clearStreamInfoList(String streamId) {
		synchronized(this) {
			Query<StreamInfo> query = datastore.find(StreamInfo.class).filter(Filters.eq(STREAM_ID, streamId));
			long count = query.count();
			DeleteResult res = query.delete(new DeleteOptions().multi(true));

			if(res.getDeletedCount() != count) {
				logger.error("{} StreamInfo were deleted out of {} for stream {}",res.getDeletedCount(), count, streamId);
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
			if (token.getTokenId() != null) 
			{
				Query<Token> query = tokenDatastore.find(Token.class).filter(Filters.eq(TOKEN_ID, token.getTokenId()));
				fetchedToken = query.first();
				if (fetchedToken != null 
						&& fetchedToken.getType().equals(token.getType())
						&& Instant.now().getEpochSecond() < fetchedToken.getExpireDate()) 
				{
					if(token.getRoomId() == null || token.getRoomId().isEmpty()) 
					{

						if(fetchedToken.getStreamId().equals(token.getStreamId())) 
						{	
							query.delete(new DeleteOptions().multi(true));
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
			Query<Token> query = tokenDatastore.find(Token.class).filter(Filters.eq(STREAM_ID, streamId));
			DeleteResult delete = query.delete(new DeleteOptions().multi(true));

			return delete.getDeletedCount() >= 1;
		}
	}

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {
		synchronized(this) {
			return 	tokenDatastore.find(Token.class).filter(Filters.eq(STREAM_ID, streamId)).iterator(new FindOptions() .skip(offset).limit(size)).toList();
		}
	}

	@Override
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
		synchronized(this) {
			return 	subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId)).iterator(new FindOptions().skip(offset).limit(size)).toList();
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
				Query<Subscriber> query = subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId), Filters.eq("subscriberId", subscriberId));
				result = query.delete().getDeletedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return result;
	}

	@Override
	public boolean blockSubscriber(String streamId, String subscriberId,
								   String blockedType, int seconds) {
		synchronized (this) {
			if (streamId == null || subscriberId == null) {
				return false;
			}

			try {
				UpdateResult updateResult = subscriberDatastore.find(Subscriber.class)
						.filter(Filters.eq(STREAM_ID, streamId), Filters.eq("subscriberId", subscriberId))
						.update(set("blockedType", blockedType),
								set("blockedUntilUnitTimeStampMs", System.currentTimeMillis() + (seconds * 1000)))
						.execute();

				long matchedCount = updateResult.getMatchedCount();
				if (matchedCount == 0) 
				{
					Subscriber subscriber = new Subscriber();
					subscriber.setStreamId(streamId);
					subscriber.setSubscriberId(subscriberId);
					subscriber.setBlockedType(blockedType);
					subscriber.setBlockedUntilUnitTimeStampMs(System.currentTimeMillis() + (seconds * 1000));
					subscriberDatastore.save(subscriber);
					return true;
				}
				else {
					return matchedCount == 1;
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
				return false;
			}
		}
	}


	@Override
	public boolean revokeSubscribers(String streamId) {
		synchronized(this) {
			Query<Subscriber> query = subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId));
			DeleteResult delete = query.delete(new DeleteOptions().multi(true));

			return delete.getDeletedCount() >= 1;
		}
	}

	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		Subscriber subscriber = null;
		if (subscriberId != null && streamId != null) {
			synchronized (this) {
				try {
					subscriber = subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId), Filters.eq("subscriberId", subscriberId)).first();
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
				UpdateResult execute = subscriberDatastore.find(Subscriber.class).update(new UpdateOptions().multi(true), set("connected", false));

				result = execute.getMatchedCount() > 1;
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
					UpdateResult result = datastore.find(Broadcast.class)
							.filter(Filters.eq(STREAM_ID, streamId))
							.update(set(field, enabled))
							.execute();
					return result.getMatchedCount() == 1;
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
				UpdateResult updateResult = conferenceRoomDatastore.find(ConferenceRoom.class)
						.filter(Filters.eq("roomId", roomId))
						.update(set("roomId", room.getRoomId()),
								set("startDate", room.getStartDate()),
								set("endDate", room.getEndDate()),
								set("roomStreamList", room.getRoomStreamList())
								).execute();



				return updateResult.getMatchedCount() == 1;
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
				DeleteResult deleteResult = conferenceRoomDatastore.find(ConferenceRoom.class)
						.filter(Filters.eq("roomId",roomId))
						.delete();
				return deleteResult.getDeletedCount() == 1;
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
				return conferenceRoomDatastore.find(ConferenceRoom.class).filter(Filters.eq("roomId", roomId)).first();
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
				return tokenDatastore.find(Token.class)
						.filter(Filters.eq(TOKEN_ID, tokenId))
						.delete()
						.getDeletedCount() == 1;
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
				token =  tokenDatastore.find(Token.class).filter(Filters.eq(TOKEN_ID,tokenId)).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return token;
	}

	@Override
	public long getLocalLiveBroadcastCount(String hostAddress) {
		synchronized(this) {
			return datastore.find(Broadcast.class)
					.filter(Filters.and(
							Filters.or(
									Filters.eq(ORIGIN_ADDRESS, hostAddress),
									Filters.exists(ORIGIN_ADDRESS).not()
									),
							Filters.eq(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)
							)).count();
		}
	}
	
	@Override
	public List<Broadcast> getLocalLiveBroadcasts(String hostAddress) 
	{
		synchronized(this) {
			return datastore.find(Broadcast.class)
					.filter(Filters.and(
							Filters.or(
									Filters.eq(ORIGIN_ADDRESS, hostAddress),
									Filters.exists(ORIGIN_ADDRESS).not()
									),
							Filters.eq(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)
							)).iterator().toList();
		}
	}

	@Override
	public boolean createP2PConnection(P2PConnection conn) {
		synchronized(this) {
			try {
				if (conn != null) {
					datastore.save(conn);
					return true;
				}
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
				return datastore.find(P2PConnection.class)
						.filter(Filters.eq(STREAM_ID, streamId))
						.delete().getDeletedCount() == 1;
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
				return datastore.find(P2PConnection.class).filter(Filters.eq(STREAM_ID, streamId)).first();
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
				if (subTrackId != null) {
					return datastore.find(Broadcast.class)
							.filter(Filters.eq(STREAM_ID, mainTrackId))
							.update(UpdateOperators.push("subTrackStreamIds", subTrackId))
							.execute()
							.getMatchedCount() == 1;
				}

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public boolean removeSubTrack(String mainTrackId, String subTrackId) {
		synchronized(this) {
			try {
				if (subTrackId != null) {
					return datastore.find(Broadcast.class)
							.filter(Filters.eq(STREAM_ID, mainTrackId))
							.update(UpdateOperators.pullAll("subTrackStreamIds", Arrays.asList(subTrackId)))
							.execute()
							.getMatchedCount() == 1;
				}

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public int resetBroadcasts(String hostAddress) 
	{
		int totalOperationCount = 0;
		synchronized(this) {

			//delete zombi streams that are belong to origin address
			totalOperationCount += datastore.find(Broadcast.class)
					.filter(Filters.and(
								Filters.or(
									Filters.eq(ORIGIN_ADDRESS, hostAddress),
									Filters.exists(ORIGIN_ADDRESS).not()
									),
								Filters.eq("zombi", true)
							))
					.delete(new DeleteOptions().multi(true))
					.getDeletedCount();



			//reset the broadcasts viewer numbers
			totalOperationCount += datastore.find(Broadcast.class)
					.filter(Filters.or(
								Filters.eq(ORIGIN_ADDRESS, hostAddress),
								Filters.exists(ORIGIN_ADDRESS).not()
								)
							)
					.update(
							set(WEBRTC_VIEWER_COUNT, 0),
							set(HLS_VIEWER_COUNT, 0),
							set(RTMP_VIEWER_COUNT, 0),
							set(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED)
							)
					.execute(new UpdateOptions().multi(true))
					.getModifiedCount();


			//delete streaminfo 
			totalOperationCount +=  datastore.find(StreamInfo.class)
					.filter(Filters.eq("host", hostAddress))
					.delete(new DeleteOptions().multi(true))
					.getDeletedCount();

		}

		return totalOperationCount;
	}

	@Entity
	public static class Summation {
		private int total;
		public int getTotal() {
			return total;
		}
		public void setTotal(int total) {
			this.total = total;
		}
	}

	@Override
	public int getTotalWebRTCViewersCount() 
	{
		long now = System.currentTimeMillis();
		if(now - totalWebRTCViewerCountLastUpdateTime > TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME) {
			synchronized(this) {
				
				int total = 0;
				
				MorphiaCursor<Summation> cursor = datastore.aggregate(Broadcast.class)
					.match(Filters.eq(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING))
					.group(Group.group().field("total", sum(field(WEBRTC_VIEWER_COUNT))))
					.execute(Summation.class);
				

				if(cursor.hasNext()) {
					total = (cursor.next()).getTotal();
				}
				

				totalWebRTCViewerCount = total;
				totalWebRTCViewerCountLastUpdateTime = now;
			}
		}
		return totalWebRTCViewerCount;
	}

	@Override
	public void saveViewerInfo(WebRTCViewerInfo info) {
		synchronized(this) {
			if (info == null) {
				return;
			}
			datastore.save(info);
		}
	}

	@Override
	public List<WebRTCViewerInfo> getWebRTCViewerList(int offset, int size, String sortBy, String orderBy,
			String search) {
		synchronized(this) {
			Query<WebRTCViewerInfo> query = datastore.find(WebRTCViewerInfo.class);

			if (size > MAX_ITEM_IN_ONE_LIST) {
				size = MAX_ITEM_IN_ONE_LIST;
			}

			FindOptions findOptions = new FindOptions().skip(offset).limit(size);

			if (sortBy != null && orderBy != null && !sortBy.isEmpty() && !orderBy.isEmpty()) {
				findOptions.sort(orderBy.equals("desc") ? Sort.descending(sortBy) : Sort.ascending(sortBy));

			}
			if (search != null && !search.isEmpty()) {
				logger.info("Server side search is called for WebRTCViewerInfo = {}", search);
				
				query.filter(
						Filters.regex(VIEWER_ID).caseInsensitive().pattern(".*" + search + ".*")
			    );

			}
			return query.iterator(findOptions).toList();
		}
	}

	@Override
	public boolean deleteWebRTCViewerInfo(String viewerId) {
		synchronized(this) {
			return datastore.find(WebRTCViewerInfo.class)
					.filter(Filters.eq(VIEWER_ID, viewerId))
					.delete()
					.getDeletedCount() == 1;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateStreamMetaData(String streamId, String metaData) {
		synchronized(this) {
			try {
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));
				return query.update(set(META_DATA, metaData)).execute().getMatchedCount() == 1;
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		return false;
	}
	
	public Datastore getSubscriberDatastore() {
		return subscriberDatastore;
	}
	
	@Override
	public SubscriberMetadata getSubscriberMetaData(String subscriberId) {
		synchronized(this) {
			try {
				return datastore.find(SubscriberMetadata.class).filter(Filters.eq(SUBSCRIBER_ID, subscriberId)).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return null;
	}
	
	@Override
	public void putSubscriberMetaData(String subscriberId, SubscriberMetadata metadata) {
		
		try {
			//delete the subscriberId if exists to make it compatible with all datastores
			Query<SubscriberMetadata> query = datastore.find(SubscriberMetadata.class).filter(Filters.eq(SUBSCRIBER_ID, subscriberId));
			long deletedCount = query.delete().getDeletedCount();
			if (deletedCount > 0) {
				logger.info("There is a SubsriberMetadata exists in database. It's deleted(deletedCount:{}) and it'll put to make it easy and compatible.", deletedCount);
			}
				
			metadata.setSubscriberId(subscriberId);
			synchronized(this) {
				datastore.save(metadata);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}
}
