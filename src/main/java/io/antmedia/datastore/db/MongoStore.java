package io.antmedia.datastore.db;

import static dev.morphia.aggregation.expressions.AccumulatorExpressions.sum;
import static dev.morphia.aggregation.expressions.Expressions.field;
import static dev.morphia.query.updates.UpdateOperators.inc;
import static dev.morphia.query.updates.UpdateOperators.set;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
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
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.filters.LogicalFilter;
import dev.morphia.query.updates.UpdateOperator;
import dev.morphia.query.updates.UpdateOperators;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
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
	private static final String MAIN_TRACK_STREAM_ID = "mainTrackStreamId";
	private static final String ROLE = "role";
	private Datastore datastore;
	private Datastore vodDatastore;
	private Datastore tokenDatastore;
	private Datastore subscriberDatastore;
	private Datastore detectionMap;
	private Datastore conferenceRoomDatastore;
	private MongoClient mongoClient;
	public CaffeineCacheManager cacheManager;
	public CaffeineCacheManager broadcastCacheManager;
	public CaffeineCache subscriberCache;
	public CaffeineCache broadcastCache;


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
	private static final String UPDATE_TIME_FIELD = "updateTime";

	public static final String OLD_STREAM_ID_INDEX_NAME = "streamId_1";
	public static final String SUBSCRIBER_CACHE = "subscriberCache";
	public static final String BROADCAST_CACHE = "broadcastCache";
	public static final int SUBSCRIBER_CACHE_SIZE = 1000;
	public static final int SUBSCRIBER_CACHE_EXPIRE_SECONDS = 10;
	public static final int BROADCAST_CACHE_SIZE = 1000;
	public static final int BROADCAST_CACHE_EXPIRE_SECONDS = 3;
	private static final String CONNECTED = "connected";
	
	private Object broadcastLock = new Object();
	private Object vodLock = new Object();
	private Object tokenLock = new Object();
	private Object subscriberLock = new Object();
	private Object detectionLock = new Object();
	private Object roomLock = new Object();

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


		deleteOldStreamIdIndex();
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

		cacheManager = new CaffeineCacheManager(SUBSCRIBER_CACHE);

		cacheManager.setCaffeine(Caffeine.newBuilder()
				.maximumSize(SUBSCRIBER_CACHE_SIZE)
				.expireAfterWrite(SUBSCRIBER_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
				);
		
		broadcastCacheManager = new CaffeineCacheManager(BROADCAST_CACHE);

		broadcastCacheManager.setCaffeine(Caffeine.newBuilder()
				.maximumSize(BROADCAST_CACHE_SIZE)
				.expireAfterWrite(BROADCAST_CACHE_EXPIRE_SECONDS, TimeUnit.SECONDS)
				);

		//migrate from conference room to broadcast
		// May 11, 2024
		// we may remove this code after some time and ConferenceRoom class
		// mekya
		migrateConferenceRoomsToBroadcasts();

	}	

	@Deprecated(since = "2.12.0", forRemoval = true)
	public void deleteOldStreamIdIndex() 
	{
		long startTime = System.nanoTime();
		MongoCollection<Broadcast> collection = datastore.getCollection(Broadcast.class);
		if (collection != null) 
		{
			MongoIterable<Document> indexes = collection.listIndexes();

			try (MongoCursor<Document> cursor = indexes.iterator()) {
				while (cursor.hasNext()) {
					Document index = cursor.next();

					if (OLD_STREAM_ID_INDEX_NAME.equals(index.getString("name"))) 
					{
						logger.info("Found old index name: {} and deleting because it'll create a new one", index.getString("name"));
						collection.dropIndex(OLD_STREAM_ID_INDEX_NAME);

						logger.info("Checking data integrity");

						deleteDuplicateStreamIds(collection);

						break;
					}
				}
			}

		}

		recordQueryDuration(startTime, "deleteOldStreamIdIndex");

	}

	public void deleteDuplicateStreamIds(MongoCollection<Broadcast> collection) {

		long startTime = System.nanoTime();
		// Use a set to track unique appNames
		Set<String> uniqueStreamIds = new HashSet<>();


		try (MongoCursor<Broadcast> cursor = collection.find(Broadcast.class).iterator()) {
			while (cursor.hasNext()) {
				Broadcast doc = cursor.next();
				String streamId = doc.getStreamId();

				// Check if appName is already encountered
				if (uniqueStreamIds.contains(streamId)) {
					// If duplicate, delete the document
					collection.deleteOne(com.mongodb.client.model.Filters.eq("_id", doc.getDbId()));
				} else {
					// Add to the set of unique appNames
					uniqueStreamIds.add(streamId);
				}
			}
		}
		
		recordQueryDuration(startTime, "deleteDuplicateStreamIds");
	}

	@Override
	public void migrateConferenceRoomsToBroadcasts() {
		long startTime = System.nanoTime();
		while (true) {
			Query<ConferenceRoom> query = conferenceRoomDatastore.find(ConferenceRoom.class);
			ConferenceRoom conferenceRoom = query.first();
			if (conferenceRoom != null) {
				try {
					Broadcast broadcast = conferenceToBroadcast(conferenceRoom);
					save(broadcast);
					conferenceRoomDatastore.delete(conferenceRoom);
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			} else {
				break;
			}
		}
		
		recordQueryDuration(startTime, "migrateConferenceRoomsToBroadcasts");

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

		long startTime = System.nanoTime();
		String streamId = null;
		if (broadcast != null) 
		{		
			try {
				Broadcast updatedBroadcast = super.saveBroadcast(broadcast);
				synchronized(broadcastLock) {
					datastore.save(broadcast);
					String cacheKey = getBroadcastCacheKey(broadcast.getStreamId());
					getBroadcastCache().put(cacheKey, broadcast);
				}
				streamId = updatedBroadcast.getStreamId();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "save");
		return streamId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#get(java.lang.String)
	 */
	@Override
	public Broadcast get(String id) {
		long startTime = System.nanoTime();
		
		String cacheKey = getBroadcastCacheKey(id);
		Broadcast broadcast = getBroadcastCache().get(cacheKey, Broadcast.class);

		if(broadcast == null){
			synchronized(broadcastLock) {
				try {
					broadcast = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id)).first();
					getBroadcastCache().put(cacheKey, broadcast);

				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}

		recordQueryDuration(startTime, "get");
		return broadcast;
	}

	@Override
	public VoD getVoD(String id) {
		long startTime = System.nanoTime();
		VoD vod = null;
		synchronized(vodLock) {
			try {

				vod = vodDatastore.find(VoD.class).filter(Filters.eq(VOD_ID,id)).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "getVoD");
		return vod;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#updateStatus(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean updateStatus(String id, String status) {
		long startTime = System.nanoTime();
		boolean result = false;

		synchronized(broadcastLock) {
			try {
				
				String cacheKey = getBroadcastCacheKey(id);
				Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
				getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back

				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));

				Update<Broadcast> ops = query.update(set(STATUS, status));
				
				if(cachedBroadcast != null) {
					cachedBroadcast.setStatus(status);
					cachedBroadcast.setUpdateTime(System.currentTimeMillis());
				}

				if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)) 
				{
					long now = System.currentTimeMillis();
					ops.add(set(START_TIME, now));
					
					if(cachedBroadcast != null) {
						cachedBroadcast.setStartTime(now);
					}
				}
				else if(status.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED)) 
				{
					ops.add(set(WEBRTC_VIEWER_COUNT, 0));
					ops.add(set(HLS_VIEWER_COUNT, 0));
					ops.add(set(RTMP_VIEWER_COUNT, 0));
					ops.add(set(DASH_VIEWER_COUNT, 0));
					
					if(cachedBroadcast != null) {
						cachedBroadcast.setWebRTCViewerCount(0);
						cachedBroadcast.setHlsViewerCount(0);
						cachedBroadcast.setRtmpViewerCount(0);
						cachedBroadcast.setDashViewerCount(0);
					}
				}

				UpdateResult update = ops.execute();
				result = update.getMatchedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
				
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "updateStatus");
		return result;

	}

	@Override
	public boolean updateVoDProcessStatus(String id, String status) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized (vodLock) {
			try {

				Query<VoD> query = vodDatastore.find(VoD.class).filter(Filters.eq(VOD_ID, id));
				Update<VoD> ops = query.update(set("processStatus", status));
				if (VoD.PROCESS_STATUS_PROCESSING.equals(status)) 
				{
					ops.add(set("processStartTime", System.currentTimeMillis()));
				}
				else if (VoD.PROCESS_STATUS_FAILED.equals(status) || VoD.PROCESS_STATUS_FINISHED.equals(status)) 
				{
					ops.add(set("processEndTime", System.currentTimeMillis()));
				}

				UpdateResult update = ops.execute();
				result = update.getMatchedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "updateVoDProcessStatus");
		return result;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#addEndpoint(java.lang.String,
	 * io.antmedia.datastore.db.types.Endpoint)
	 */
	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			String cacheKey = getBroadcastCacheKey(id);
			Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
			getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back

			
			if (id != null && endpoint != null) {
				try {
					
					if(cachedBroadcast != null) {
						List<Endpoint> endPointList = cachedBroadcast.getEndPointList();
						if (endPointList == null) {
							endPointList = new ArrayList<>();
						}
						endPointList.add(endpoint);
						cachedBroadcast.setEndPointList(endPointList);
					}
					Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));

					result = query.update(UpdateOperators.push("endPointList", endpoint)).execute().getMatchedCount() == 1;
				
					if(result && cachedBroadcast != null) {
						getBroadcastCache().put(cacheKey, cachedBroadcast);
					}
				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		recordQueryDuration(startTime, "addEndpoint");
		return result;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint, boolean checkRTMPUrl) {
		long startTime = System.nanoTime();

		boolean result = false;
		synchronized(broadcastLock) {
			String cacheKey = getBroadcastCacheKey(id);
			Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
			getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back

			
			if (id != null && endpoint != null) 
			{
				if(cachedBroadcast != null) {
					List<Endpoint> endPointList = cachedBroadcast.getEndPointList();
					if (endPointList != null) {
						for (Iterator<Endpoint> iterator = endPointList.iterator(); iterator.hasNext();) {
							Endpoint endpointItem = iterator.next();
							if(checkRTMPUrl) {
								if (endpointItem.getEndpointUrl().equals(endpoint.getEndpointUrl())) {
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
				
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));

				Update<Broadcast> update = query.update(UpdateOperators.pullAll("endPointList", Arrays.asList(endpoint)));

				result = update.execute().getMatchedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
			}
		}
		recordQueryDuration(startTime, "removeEndpoint");
		return result;
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		long startTime = System.nanoTime();

		boolean result = false;
		synchronized(broadcastLock) {
			String cacheKey = getBroadcastCacheKey(id);
			Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
			getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back

			if (id != null) {
				
				if(cachedBroadcast != null) {
					cachedBroadcast.setEndPointList(null);
				}
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));
				result = query.update(UpdateOperators.unset("endPointList")).execute().getMatchedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
			}
		}
		recordQueryDuration(startTime, "removeAllEndpoints");
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#getBroadcastCount()
	 */
	@Override
	public long getBroadcastCount() {
		return this.getTotalBroadcastNumber();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#delete(java.lang.String)
	 */
	@Override
	public boolean delete(String id) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try {
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, id));
				result = query.delete().getDeletedCount() == 1;
				
				if(result){
					String cacheKey = getBroadcastCacheKey(id);
					getBroadcastCache().evictIfPresent(cacheKey);
				}
				
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "delete");
		return result;
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
	public List<ConnectionEvent> getConnectionEvents(String streamId, String subscriberId, int offset, int size) {
		long startTime = System.nanoTime();

		List<ConnectionEvent> connectionEvents = new ArrayList<>();
		synchronized(subscriberLock) {
			try {

				Query<ConnectionEvent> query = subscriberDatastore.find(ConnectionEvent.class)
						.filter(Filters.eq(STREAM_ID, streamId));


				if (StringUtils.isNotBlank(subscriberId)) {
					query.filter(Filters.eq(SUBSCRIBER_ID, subscriberId));
				}

				FindOptions findingOptions = new FindOptions().skip(offset).limit(size);

				connectionEvents = query.iterator(findingOptions).toList();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "getConnectionEvents");
		return connectionEvents;
	}


	@Override
	public List<Broadcast> getBroadcastList(int offset, int size, String type, String sortBy, String orderBy, String search) {
		long startTime = System.nanoTime();
		List<Broadcast> broadcastList = Arrays.asList();
		synchronized(broadcastLock) {
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

				broadcastList = query.iterator(findingOptions).toList();

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "getBroadcastList");
		return broadcastList;
	}

	public Datastore getDataStore() {
		return datastore;
	}


	@Override
	public List<Broadcast> getExternalStreamsList() {
		long startTime = System.nanoTime();
		List<Broadcast> streamList = Arrays.asList();
		long now = System.currentTimeMillis();
		synchronized(broadcastLock) {
			try {

				Query<Broadcast> query = datastore.find(Broadcast.class);

				query.filter(
						Filters.and(
								Filters.or(Filters.eq("type", AntMediaApplicationAdapter.IP_CAMERA), Filters.eq("type", AntMediaApplicationAdapter.STREAM_SOURCE)),
								Filters.and(Filters.ne(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING), Filters.ne(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING)))
						);

				streamList = query.iterator().toList();
				final UpdateResult results = query.update(new UpdateOptions().multi(true), 
						set(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING),
						set(UPDATE_TIME_FIELD, now));
				long updatedCount = results.getModifiedCount();
				if(updatedCount != streamList.size()) {
					logger.error("Only {} stream status updated out of {}", updatedCount, streamList.size());
				}
			} catch (Exception e) {

				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "getExternalStreamsList");
		return streamList;
	}

	@Override
	public void close(boolean deleteDB) {
		long startTime = System.nanoTime();

		synchronized(broadcastLock) {
			synchronized(vodLock) {
				synchronized(tokenLock) {
					synchronized(subscriberLock) {
						synchronized(detectionLock) {
							synchronized(roomLock) {
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
					}
				}
			}
		}
		recordQueryDuration(startTime, "close");
	}

	@Override
	public String addVod(VoD vod) {
		long startTime = System.nanoTime();


		String id = null;
		boolean result = false;
		synchronized(vodLock) {
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
		recordQueryDuration(startTime, "addVod");
		return id;

	}

	@Override
	public List<VoD> getVodList(int offset, int size, String sortBy, String orderBy, String filterStreamId, String search) {
		long startTime = System.nanoTime();

		List<VoD>  vodList = Arrays.asList();
		synchronized(vodLock) {

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
						Filters.regex("vodName").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("description").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("metadata").caseInsensitive().pattern(".*" + search + ".*")
						)
						);

			}
			vodList = query.iterator(findOptions).toList();
		}
		
		recordQueryDuration(startTime, "getVodList");

		return vodList;
	}


	@Override
	public boolean deleteVod(String id) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(vodLock) {
			try {
				Query<VoD> query = vodDatastore.find(VoD.class).filter(Filters.eq(VOD_ID, id));
				result = query.delete().getDeletedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "deleteVod");
		return result;
	}



	@Override
	public long getTotalVodNumber() {
		long startTime = System.nanoTime();
		long totalVodNumber = 0;
		synchronized(vodLock) {

			totalVodNumber = vodDatastore.find(VoD.class).count();
		}
		recordQueryDuration(startTime, "getTotalVodNumber");
		return totalVodNumber;
	}

	@Override
	public int fetchUserVodList(File userfile) {

		long startTime = System.nanoTime();


		if(userfile==null) {
			return 0;
		}

		int numberOfSavedFiles = 0;
		synchronized(vodLock) {
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
		recordQueryDuration(startTime, "fetchUserVodList");
		return numberOfSavedFiles;

	}


	@Override
	public long getTotalBroadcastNumber() {
		long startTime = System.nanoTime();
		long totalBroadcastNumber = 0;
		synchronized(broadcastLock) {

			totalBroadcastNumber = datastore.find(Broadcast.class).count();
		}

		recordQueryDuration(startTime, "getTotalBroadcastNumber");
		return totalBroadcastNumber;
	}

	@Override
	public long getPartialBroadcastNumber(String search){
		long startTime = System.nanoTime();
		long partialBroadcastNumber = 0;

		synchronized(broadcastLock) {

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

			partialBroadcastNumber = query.count();
		}
		
		recordQueryDuration(startTime, "getPartialBroadcastNumber");

		return partialBroadcastNumber;
	}

	@Override
	public long getPartialVodNumber(String search)
	{
		long startTime = System.nanoTime();
		long partialVodNumber = 0;
		synchronized(vodLock) {

			Query<VoD> query = vodDatastore.find(VoD.class);
			if (search != null && !search.isEmpty())
			{
				logger.info("Server side search is called for {}", search);
				query.filter(Filters.or(
						Filters.regex("streamId").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("streamName").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex(VOD_ID).caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("vodName").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("description").caseInsensitive().pattern(".*" + search + ".*"),
						Filters.regex("metadata").caseInsensitive().pattern(".*" + search + ".*")
						));
			}
			partialVodNumber = query.count();
		}

		recordQueryDuration(startTime, "getPartialVodNumber");
		return partialVodNumber;
	}

	public Datastore getVodDatastore() {
		return vodDatastore;
	}

	public void setVodDatastore(Datastore vodDatastore) {
		this.vodDatastore = vodDatastore;
	}

	@Override
	public long getActiveBroadcastCount() 
	{
		long startTime = System.nanoTime();

		long activeBroadcastCount = 0;
		synchronized(broadcastLock) {			
			LogicalFilter andFilter = Filters.and(Filters.eq(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING));

			//check their updateTime field
			long activeIntervalValue = System.currentTimeMillis() - (2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS);
			andFilter.add(Filters.gte(UPDATE_TIME_FIELD, activeIntervalValue));



			activeBroadcastCount = datastore.find(Broadcast.class).filter(andFilter).count();
		}
		
		recordQueryDuration(startTime, "getActiveBroadcastCount");

		return activeBroadcastCount;
	}

	@Override
	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		long startTime = System.nanoTime();


		synchronized(detectionLock) {
			if (detectedObjects != null) {
				for (TensorFlowObject tensorFlowObject : detectedObjects) {
					tensorFlowObject.setDetectionTime(timeElapsed);
					tensorFlowObject.setImageId(id);

					detectionMap.save(tensorFlowObject);
				}
			}
		}
		
		recordQueryDuration(startTime, "saveDetection");
	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		long startTime = System.nanoTime();
		List<TensorFlowObject> detectionList = new ArrayList<>();
		synchronized(detectionLock) {
			try {
				if (batchSize > MAX_ITEM_IN_ONE_LIST) {
					batchSize = MAX_ITEM_IN_ONE_LIST;
				}

				detectionList = detectionMap.find(TensorFlowObject.class).iterator(new FindOptions().skip(offsetSize).limit(batchSize)).toList();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		
		recordQueryDuration(startTime, "getDetectionList");
		return detectionList;	
	}

	@Override
	public List<TensorFlowObject> getDetection(String id) {
		long startTime = System.nanoTime();
		List<TensorFlowObject> detectionList = new ArrayList<>();
		synchronized(detectionLock) 
		{
			try {
				detectionList = detectionMap.find(TensorFlowObject.class).filter(Filters.eq(IMAGE_ID, id)).iterator().toList();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "getDetection");
		return detectionList;	
	}

	@Override
	public long getObjectDetectedTotal(String id) {
		long startTime = System.nanoTime();
		long totalObjectDetected = 0;
		synchronized(detectionLock) {

			totalObjectDetected = detectionMap.find(TensorFlowObject.class).filter(Filters.eq(IMAGE_ID, id)).count();
		}
		recordQueryDuration(startTime, "getObjectDetectedTotal");
		return totalObjectDetected;
	}



	@Override
	public boolean updateBroadcastFields(String streamId, BroadcastUpdate broadcast) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try {
				String cacheKey = getBroadcastCacheKey(streamId);
				Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
				getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
				
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));

				if(cachedBroadcast != null) {
					updateStreamInfo(cachedBroadcast, broadcast);
				}
				
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

				if (broadcast.getStreamUrl() != null) {
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

				if (broadcast.getSpeed() != null) {
					updates.add(set("speed", broadcast.getSpeed()));
				}

				if(broadcast.getEncoderSettingsList() != null){
					updates.add(set("encoderSettingsList",broadcast.getEncoderSettingsList()));
				}

				if (broadcast.getConferenceMode() != null) {
					updates.add(set("conferenceMode", broadcast.getConferenceMode()));
				}

				if (broadcast.getPlannedStartDate() != null) {
					updates.add(set("plannedStartDate", broadcast.getPlannedStartDate()));
				}

				if (broadcast.getSeekTimeInMs() != null) {
					updates.add(set("seekTimeInMs", broadcast.getSeekTimeInMs()));
				}

				if (broadcast.getReceivedBytes() != null) {
					updates.add(set("receivedBytes", broadcast.getReceivedBytes()));
				}

				if (broadcast.getBitrate() != null) {
					updates.add(set("bitrate", broadcast.getBitrate()));
				}

				if (broadcast.getUserAgent() != null) {
					updates.add(set("userAgent", broadcast.getUserAgent()));
				}

				if (broadcast.getWebRTCViewerLimit() != null) {
					updates.add(set("webRTCViewerLimit", broadcast.getWebRTCViewerLimit()));
				}
				
				if (broadcast.getWebRTCViewerCount() != null) {
					updates.add(set(WEBRTC_VIEWER_COUNT, broadcast.getWebRTCViewerCount()));
				}

				if (broadcast.getHlsViewerLimit() != null) {
					updates.add(set("hlsViewerLimit", broadcast.getHlsViewerLimit()));
				}

				if (broadcast.getDashViewerLimit() != null) {
					updates.add(set("dashViewerLimit", broadcast.getDashViewerLimit()));
				}

				if (broadcast.getSubTrackStreamIds() != null) {
					updates.add(set("subTrackStreamIds", broadcast.getSubTrackStreamIds()));
				}

				if (broadcast.getMetaData() != null) {
					updates.add(set(META_DATA, broadcast.getMetaData()));
				}

				if (broadcast.getUpdateTime() != null && broadcast.getUpdateTime() > 0) {
					updates.add(set(UPDATE_TIME_FIELD, broadcast.getUpdateTime()));
				}

				if (broadcast.getSubtracksLimit() != null) {
					updates.add(set("subtracksLimit", broadcast.getSubtracksLimit()));
				}

				if (broadcast.getCurrentPlayIndex() != null) {
					updates.add(set("currentPlayIndex", broadcast.getCurrentPlayIndex()));
				}

				if (broadcast.getPlaylistLoopEnabled() != null) {
					updates.add(set("playlistLoopEnabled", broadcast.getPlaylistLoopEnabled()));
				}

				if (broadcast.getAutoStartStopEnabled() != null) {
					updates.add(set("autoStartStopEnabled", broadcast.getAutoStartStopEnabled()));
				}

				if (broadcast.getPendingPacketSize() != null) {
					updates.add(set("pendingPacketSize", broadcast.getPendingPacketSize()));
				}

				if (broadcast.getPlannedEndDate() != null) {
					updates.add(set("plannedEndDate", broadcast.getPlannedEndDate()));
				}

				if (broadcast.getRole() != null) {
					updates.add(set(ROLE, broadcast.getRole()));
				}

				if (broadcast.getQuality() != null) {
					updates.add(set("quality", broadcast.getQuality()));
				}


				prepareFields(broadcast, updates, cachedBroadcast);

				UpdateResult updateResult = query.update(updates).execute();
				
				result = updateResult.getModifiedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
				
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "updateBroadcastFields");
		return result;
	}

	private void prepareFields(BroadcastUpdate broadcast, List<UpdateOperator> updates, Broadcast cachedBroadcast) {

		if ( broadcast.getDuration() != null) {
			updates.add(set(DURATION, broadcast.getDuration()));
		}

		if (broadcast.getStartTime() != null) {
			updates.add(set(START_TIME, broadcast.getStartTime()));
		}

		if (broadcast.getOriginAdress() != null) {
			updates.add(set(ORIGIN_ADDRESS, broadcast.getOriginAdress()));
		}

		if (broadcast.getStatus() != null) {
			updates.add(set(STATUS, broadcast.getStatus()));
		}

		if (broadcast.getAbsoluteStartTimeMs() != null) {
			updates.add(set("absoluteStartTimeMs", broadcast.getAbsoluteStartTimeMs()));
		}
		
		if (broadcast.getWidth() != null) {
			updates.add(set("width", broadcast.getWidth()));
		}
		
		if (broadcast.getHeight() != null) {
			updates.add(set("height", broadcast.getHeight()));
		}
		
		if (broadcast.getEncoderQueueSize() != null) {
			updates.add(set("encoderQueueSize", broadcast.getEncoderQueueSize()));
		}
		
		if (broadcast.getDropPacketCountInIngestion() != null) {
			updates.add(set("dropPacketCountInIngestion", broadcast.getDropPacketCountInIngestion()));
		}
		
		if (broadcast.getDropFrameCountInEncoding() != null) {
			updates.add(set("dropFrameCountInEncoding", broadcast.getDropFrameCountInEncoding()));
		}
		
		if (broadcast.getPacketLostRatio() != null) {
			updates.add(set("packetLostRatio", broadcast.getPacketLostRatio()));
		}
		
		if (broadcast.getJitterMs() != null) {
			updates.add(set("jitterMs", broadcast.getJitterMs()));
		}
		
		if (broadcast.getRttMs() != null) {
			updates.add(set("rttMs", broadcast.getRttMs()));
		}
		
		if (broadcast.getPacketsLost() != null) {
			updates.add(set("packetsLost", broadcast.getPacketsLost()));
		}
		
		if (broadcast.getRemoteIp() != null) {
			updates.add(set("remoteIp", broadcast.getRemoteIp()));
		}
		
		if (broadcast.getVirtual() != null) {
			updates.add(set("virtual", broadcast.getVirtual()));
		}
		
		if (broadcast.getMaxIdleTime() != null) {
			updates.add(set("maxIdleTime", broadcast.getMaxIdleTime()));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateHLSViewerCountLocal(String streamId, int diffCount) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try {
				String cacheKey = getBroadcastCacheKey(streamId);
				Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
				getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
				
				if(cachedBroadcast != null) {
					cachedBroadcast.setHlsViewerCount(cachedBroadcast.getHlsViewerCount() + diffCount);
				}
				
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));
				UpdateResult queryResult = query.update(inc(HLS_VIEWER_COUNT, diffCount)).execute();

				result = queryResult.getMatchedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "updateHLSViewerCountLocal");
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateDASHViewerCountLocal(String streamId, int diffCount) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try {
				String cacheKey = getBroadcastCacheKey(streamId);
				Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
				getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
				
				if(cachedBroadcast != null) {
					cachedBroadcast.setDashViewerCount(cachedBroadcast.getDashViewerCount() + diffCount);
				}
				
				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));
				UpdateResult queryResult = query.update(inc(DASH_VIEWER_COUNT, diffCount)).execute();
				result = queryResult.getMatchedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "updateDASHViewerCountLocal");
		return result;
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
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try {
				String cacheKey = getBroadcastCacheKey(streamId);
				Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
				getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
				
				if(cachedBroadcast != null) {
					if(fieldName.equals(WEBRTC_VIEWER_COUNT)) {
						cachedBroadcast.setWebRTCViewerCount(cachedBroadcast.getWebRTCViewerCount() + (increment ? 1 : -1));
					}
					else if(fieldName.equals(RTMP_VIEWER_COUNT)) {
						cachedBroadcast.setRtmpViewerCount(cachedBroadcast.getRtmpViewerCount() + (increment ? 1 : -1));
					}
				}
				
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

				result = updateResult.getModifiedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "updateViewerField");
		return result;
	}


	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		long startTime = System.nanoTime();

		synchronized(broadcastLock) {
			//TODO: Why do we run find(StreamInfo.class)
			datastore.find(StreamInfo.class);
			datastore.save(streamInfo);
		}
		recordQueryDuration(startTime, "saveStreamInfo");
	}

	public List<StreamInfo> getStreamInfoList(String streamId) {
		long startTime = System.nanoTime();
		List<StreamInfo> streamInfoList = new ArrayList<>();
		synchronized(broadcastLock) {
			streamInfoList = datastore.find(StreamInfo.class).filter(Filters.eq(STREAM_ID, streamId)).iterator().toList();
		}
		recordQueryDuration(startTime, "getStreamInfoList");
		return streamInfoList;
	}

	public void clearStreamInfoList(String streamId) {
		long startTime = System.nanoTime();

		synchronized(broadcastLock) {

			Query<StreamInfo> query = datastore.find(StreamInfo.class).filter(Filters.eq(STREAM_ID, streamId));
			long count = query.count();
			DeleteResult res = query.delete(new DeleteOptions().multi(true));

			if(res.getDeletedCount() != count) {
				logger.error("{} StreamInfo were deleted out of {} for stream {}",res.getDeletedCount(), count, streamId);
			}
		}
		recordQueryDuration(startTime, "clearStreamInfoList");
	}

	@Override
	public boolean saveToken(Token token) {
		long startTime = System.nanoTime();

		boolean result = false;
		synchronized(tokenLock) {
			if(token.getStreamId() != null && token.getTokenId() != null) {

				try {


					tokenDatastore.save(token);
					result = true;

				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}
		recordQueryDuration(startTime, "saveToken");

		return result;
	}

	@Override
	public Token validateToken(Token token) {
		long startTime = System.nanoTime();

		Token fetchedToken = null;
		synchronized(tokenLock) {
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

				}else {
					fetchedToken = null;
				}
			}
		}
		recordQueryDuration(startTime, "validateToken");
		return fetchedToken;
	}

	@Override
	public boolean revokeTokens(String streamId) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(tokenLock) {
			Query<Token> query = tokenDatastore.find(Token.class).filter(Filters.eq(STREAM_ID, streamId));
			DeleteResult delete = query.delete(new DeleteOptions().multi(true));

			result = delete.getDeletedCount() >= 1;
		}
		recordQueryDuration(startTime, "revokeTokens");
		return result;
	}

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {
		long startTime = System.nanoTime();
		List<Token> tokenList = new ArrayList<>();
		synchronized(tokenLock) {
			tokenList = tokenDatastore.find(Token.class).filter(Filters.eq(STREAM_ID, streamId)).iterator(new FindOptions() .skip(offset).limit(size)).toList();
		}
		recordQueryDuration(startTime, "listAllTokens");
		return tokenList;
	}
	
	@Override
	public long getConnectedSubscriberCount(String streamId) {
		long startTime = System.nanoTime();
		
		long subscriberCount = 0;
		synchronized(subscriberLock) {
			subscriberCount = subscriberDatastore.find(Subscriber.class).filter(
					 Filters.and(Filters.eq(STREAM_ID, streamId), Filters.eq(CONNECTED, true))).count();
		}
		recordQueryDuration(startTime, "getSubscriberCount");
		return subscriberCount;
	}
	
	@Override
	public List<Subscriber> getConnectedSubscribers(String streamId, int offset, int size) {
		long startTime = System.nanoTime();
		List<Subscriber> subscriberList = new ArrayList<>();
		synchronized(subscriberLock) {
			subscriberList = subscriberDatastore.find(Subscriber.class).filter(Filters.and(Filters.eq(STREAM_ID, streamId), Filters.eq(CONNECTED, true))).iterator(new FindOptions().skip(offset).limit(size)).toList();
		}
		recordQueryDuration(startTime, "getConnectedSubscribers");
		return subscriberList;	}

	@Override
	public List<Subscriber> listAllSubscribers(String streamId, int offset, int size) {
		long startTime = System.nanoTime();
		List<Subscriber> subscriberList = new ArrayList<>();
		synchronized(subscriberLock) {
			subscriberList = subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId)).iterator(new FindOptions().skip(offset).limit(size)).toList();
		}
		recordQueryDuration(startTime, "listAllSubscribers");
		return subscriberList;
	}


	@Override
	public boolean addSubscriber(String streamId, Subscriber subscriber) {
		long startTime = System.nanoTime();

		boolean result = false;
		if (subscriber != null) {

			synchronized (subscriberLock) {
				if (subscriber.getStreamId() != null && subscriber.getSubscriberId() != null) {
					try {


						subscriberDatastore.save(subscriber);

						getSubscriberCache().put(getSubscriberCacheKey(streamId, subscriber.getSubscriberId()), subscriber);
						result = true;
					} catch (Exception e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}
		}
		
		recordQueryDuration(startTime, "addSubscriber");

		return result;
	}

	@Override
	protected boolean addConnectionEvent(ConnectionEvent event) {
		long startTime = System.nanoTime();

		boolean result = false;
		if (event != null && StringUtils.isNoneBlank(event.getStreamId(), event.getSubscriberId())) {

			try {

				subscriberDatastore.save(event);
				result = true;
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		
		recordQueryDuration(startTime, "addConnectionEvent");
		return result;
	}

	@Override
	public boolean deleteSubscriber(String streamId, String subscriberId) {
		long startTime = System.nanoTime();

		boolean result = false;
		synchronized(subscriberLock) {
			try {


				Query<Subscriber> query = subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId), Filters.eq(SUBSCRIBER_ID, subscriberId));
				result = query.delete().getDeletedCount() == 1;
				if(result){
					getSubscriberCache().evictIfPresent(getSubscriberCacheKey(streamId, subscriberId));
				}

				Query<ConnectionEvent> queryConnectionEvent = subscriberDatastore.find(ConnectionEvent.class).filter(Filters.eq(STREAM_ID, streamId), Filters.eq(SUBSCRIBER_ID, subscriberId));
				queryConnectionEvent.delete(new DeleteOptions().multi(true));


			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		
		recordQueryDuration(startTime, "deleteSubscriber");
		return result;
	}

	@Override
	public boolean blockSubscriber(String streamId, String subscriberId,
			String blockedType, int seconds) {
		long startTime = System.nanoTime();

		boolean result = false;
		synchronized (subscriberLock) {
			if (streamId == null || subscriberId == null) {
				return false;
			}

			try {
				long blockedUntilTimestampMs = System.currentTimeMillis() + (seconds * 1000L);


				UpdateResult updateResult = subscriberDatastore.find(Subscriber.class)
						.filter(Filters.eq(STREAM_ID, streamId), Filters.eq("subscriberId", subscriberId))
						.update(set("blockedType", blockedType),
								set("blockedUntilUnitTimeStampMs", blockedUntilTimestampMs))
						.execute();

				long matchedCount = updateResult.getMatchedCount();
				if(matchedCount == 1){
					String cacheKey = getSubscriberCacheKey(streamId, subscriberId);
					Subscriber subscriber = getSubscriberCache().get(cacheKey, Subscriber.class);
					if(subscriber != null && subscriber.getSubscriberId() != null){
						subscriber.setBlockedType(blockedType);
						subscriber.setBlockedUntilUnitTimeStampMs(blockedUntilTimestampMs);
						getSubscriberCache().put(cacheKey, subscriber);
					}

				}
				if (matchedCount == 0) 
				{
					Subscriber subscriber = new Subscriber();
					subscriber.setStreamId(streamId);
					subscriber.setSubscriberId(subscriberId);
					subscriber.setBlockedType(blockedType);
					subscriber.setBlockedUntilUnitTimeStampMs(System.currentTimeMillis() + (seconds * 1000));
					subscriberDatastore.save(subscriber);
					getSubscriberCache().put(getSubscriberCacheKey(streamId, subscriber.getSubscriberId()), subscriber);


					result = true;
				}
				else {
					result = matchedCount == 1;
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "blockSubscriber");
		return result;
	}


	@Override
	public boolean revokeSubscribers(String streamId) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(subscriberLock) {


			Query<Subscriber> query = subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId));
			DeleteResult delete = query.delete(new DeleteOptions().multi(true));
			getSubscriberCache().clear();

			Query<ConnectionEvent> queryConnectionEvents = subscriberDatastore.find(ConnectionEvent.class).filter(Filters.eq(STREAM_ID, streamId));
			DeleteResult deleteConnectionEvents = queryConnectionEvents.delete(new DeleteOptions().multi(true));

			result = delete.getDeletedCount() >= 1 || deleteConnectionEvents.getDeletedCount() >= 1;
		}
		recordQueryDuration(startTime, "revokeSubscribers");
		return result;
	}

	public String getSubscriberCacheKey(String streamId, String subscriberId){
		return streamId + "_" + subscriberId;
	}
	
	public String getBroadcastCacheKey(String streamId){
		return "key_" + streamId;
	}

	@Override
	public Subscriber getSubscriber(String streamId, String subscriberId) {
		long startTime = System.nanoTime();

		Subscriber subscriber = null;
		if (subscriberId != null && streamId != null) {

			String cacheKey = getSubscriberCacheKey(streamId, subscriberId);
			Subscriber cachedSubscriber = getSubscriberCache().get(cacheKey, Subscriber.class);

			if (cachedSubscriber != null) 
			{ 
				if (StringUtils.isNotBlank(cachedSubscriber.getSubscriberId())) {
					// Subscriber exists in cache, return directly.
					return cachedSubscriber;
				}
				else {
					//Empty subscriber
					return null;
				}
			}

			synchronized (subscriberLock) {
				try {

					subscriber = subscriberDatastore.find(Subscriber.class).filter(Filters.eq(STREAM_ID, streamId), Filters.eq("subscriberId", subscriberId)).first();
					if(subscriber == null){
						getSubscriberCache().put(cacheKey, new Subscriber()); //Empty subscriber means that non-existence result is cached.
					}else{
						getSubscriberCache().put(cacheKey, subscriber); //Subscriber exists in DB. Cache him.
					}

				} catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}

		}
		recordQueryDuration(startTime, "getSubscriber");
		return subscriber;
	}

	@Override
	public boolean resetSubscribersConnectedStatus() {
		long startTime = System.nanoTime();

		boolean result = false;
		synchronized (subscriberLock) {
			try {


				UpdateResult execute = subscriberDatastore.find(Subscriber.class).update(new UpdateOptions().multi(true), set("connected", false));
				result = execute.getMatchedCount() >= 1;
				if(result){

					getSubscriberCache().getNativeCache().asMap().forEach((key, value) -> {
						if (value instanceof Subscriber) {
							Subscriber subscriber = (Subscriber) value;
							if(subscriber.getSubscriberId() != null){
								subscriber.setConnected(false);
								getSubscriberCache().put(key, subscriber);
							}

						}
					});

				}

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "resetSubscribersConnectedStatus");
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
		long startTime = System.nanoTime();
		boolean methodResult = false;
		synchronized(broadcastLock) {
			String cacheKey = getBroadcastCacheKey(streamId);
			Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
			getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
			
			try {
				if (streamId != null && (enabled == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM || enabled == MuxAdaptor.RECORDING_NO_SET_FOR_STREAM || enabled == MuxAdaptor.RECORDING_DISABLED_FOR_STREAM)) {

					if(cachedBroadcast != null) {
						if(field.equals("mp4Enabled")) {
							cachedBroadcast.setMp4Enabled(enabled);
						}
						else if(field.equals("webMEnabled")) {
							cachedBroadcast.setWebMEnabled(enabled);
						}
					}

					UpdateResult result = datastore.find(Broadcast.class)
							.filter(Filters.eq(STREAM_ID, streamId))
							.update(set(field, enabled))
							.execute();
					methodResult = result.getMatchedCount() == 1;
					
					if(methodResult && cachedBroadcast != null) {
						getBroadcastCache().put(cacheKey, cachedBroadcast);
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "setRecordMuxing");
		return methodResult;

	}

	@Override
	public boolean deleteToken(String tokenId) {
		long startTime = System.nanoTime();

		boolean result = false;
		synchronized(tokenLock) {
			try 
			{
				result = tokenDatastore.find(Token.class)
						.filter(Filters.eq(TOKEN_ID, tokenId))
						.delete()
						.getDeletedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "deleteToken");
		return result;
	}

	@Override
	public Token getToken(String tokenId) {
		long startTime = System.nanoTime();

		Token token = null;

		synchronized(tokenLock) {
			try {

				token =  tokenDatastore.find(Token.class).filter(Filters.eq(TOKEN_ID,tokenId)).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "getToken");
		return token;
	}

	@Override
	public long getLocalLiveBroadcastCount(String hostAddress) {
		long startTime = System.nanoTime();

		long liveBroadcastCount = 0;
		synchronized(broadcastLock) {


			//get the number of live broadcasts that are coming from hostAddress
			LogicalFilter logicalFilter = Filters.and(
					Filters.or(
							Filters.eq(ORIGIN_ADDRESS, hostAddress)),
					Filters.eq(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING));

			//check their updateTime field
			long activeIntervalValue = System.currentTimeMillis() - (2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS);
			logicalFilter.add(Filters.gte(UPDATE_TIME_FIELD, activeIntervalValue));


			liveBroadcastCount = datastore.find(Broadcast.class)
					.filter(logicalFilter).count();
		}
		
		recordQueryDuration(startTime, "getLocalLiveBroadcastCount");

		return liveBroadcastCount;
	}

	@Override
	public List<Broadcast> getLocalLiveBroadcasts(String hostAddress) 
	{
		long startTime = System.nanoTime();
		List<Broadcast> broadcastList = new ArrayList<>();
		synchronized(broadcastLock) {



			//get the number of live broadcasts that are coming from hostAddress
			LogicalFilter logicalFilter = Filters.and(
					Filters.or(
							Filters.eq(ORIGIN_ADDRESS, hostAddress)),
					Filters.eq(STATUS, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING));

			//check their updateTime field
			long activeIntervalValue = System.currentTimeMillis() - (2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS);
			logicalFilter.add(Filters.gte(UPDATE_TIME_FIELD, activeIntervalValue));

			broadcastList = datastore.find(Broadcast.class)
					.filter(logicalFilter).iterator().toList();
		}
		
		recordQueryDuration(startTime, "getLocalLiveBroadcasts");

		return broadcastList;
	}

	@Override
	public boolean createP2PConnection(P2PConnection conn) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try {
				if (conn != null) {


					datastore.save(conn);
					result = true;
				}
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		
		recordQueryDuration(startTime, "createP2PConnection");
		return result;
	}

	@Override
	public boolean deleteP2PConnection(String streamId) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try 
			{
				result = datastore.find(P2PConnection.class)
						.filter(Filters.eq(STREAM_ID, streamId))
						.delete().getDeletedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		
		recordQueryDuration(startTime, "deleteP2PConnection");
		return result;
	}

	@Override
	public P2PConnection getP2PConnection(String streamId) {
		long startTime = System.nanoTime();
		P2PConnection p2pConnection = null;
		synchronized(broadcastLock) {
			try {


				p2pConnection = datastore.find(P2PConnection.class).filter(Filters.eq(STREAM_ID, streamId)).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		
		recordQueryDuration(startTime, "getP2PConnection");
		return p2pConnection;
	}

	@Override
	public boolean addSubTrack(String mainTrackId, String subTrackId) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			String cacheKey = getBroadcastCacheKey(mainTrackId);
			Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
			getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
		
			try {
				if (subTrackId != null) {

					if(cachedBroadcast != null) {
						List<String> subTracks = cachedBroadcast.getSubTrackStreamIds();

						if (subTracks == null) {
							subTracks = new ArrayList<>();
						}

						if (!subTracks.contains(subTrackId)) 
						{
							subTracks.add(subTrackId);
						}
					}

					result = datastore.find(Broadcast.class)
							.filter(Filters.eq(STREAM_ID, mainTrackId))
							.update(UpdateOperators.push("subTrackStreamIds", subTrackId))
							.execute()
							.getMatchedCount() == 1;
					
					if(result && cachedBroadcast != null) {
						getBroadcastCache().put(cacheKey, cachedBroadcast);
					}
				}

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "addSubTrack");
		return result;
	}

	@Override
	public boolean removeSubTrack(String mainTrackId, String subTrackId) {
		long startTime = System.nanoTime();
		boolean result = false;

		synchronized(broadcastLock) {
			try {
				String cacheKey = getBroadcastCacheKey(mainTrackId);
				Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
				getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
			
				if (subTrackId != null) 
				{	
					if(cachedBroadcast != null) {
						List<String> subTracks = cachedBroadcast.getSubTrackStreamIds();
						if(subTracks.remove(subTrackId)) {
							cachedBroadcast.setSubTrackStreamIds(subTracks);
						}
					}
					
					result = datastore.find(Broadcast.class)
							.filter(Filters.eq(STREAM_ID, mainTrackId))
							.update(UpdateOperators.pullAll("subTrackStreamIds", Arrays.asList(subTrackId)))
							.execute()
							.getMatchedCount() == 1;
					
					if(result && cachedBroadcast != null) {
						getBroadcastCache().put(cacheKey, cachedBroadcast);
					}
				}

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "removeSubTrack");
		return result;
	}

	@Override
	public int resetBroadcasts(String hostAddress) 
	{
		long startTime = System.nanoTime();

		int totalOperationCount = 0;
		synchronized(broadcastLock) {

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
		
		recordQueryDuration(startTime, "resetBroadcasts");

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
		long startTime = System.nanoTime();

		long now = System.currentTimeMillis();
		if(now - totalWebRTCViewerCountLastUpdateTime > TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME) {
			synchronized(broadcastLock) {

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
		
		recordQueryDuration(startTime, "getTotalWebRTCViewersCount");
		return totalWebRTCViewerCount;
	}

	@Override
	public void saveViewerInfo(WebRTCViewerInfo info) {
		long startTime = System.nanoTime();
		if (info == null) {
			return;
		}
		synchronized(broadcastLock) {
			datastore.save(info);
		}
		
		recordQueryDuration(startTime, "saveViewerInfo");
	}

	@Override
	public List<WebRTCViewerInfo> getWebRTCViewerList(int offset, int size, String sortBy, String orderBy,
			String search) {
		long startTime = System.nanoTime();
		List<WebRTCViewerInfo> viewerList = new ArrayList<>();
		synchronized(broadcastLock) {


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
			viewerList = query.iterator(findOptions).toList();
		}
		
		recordQueryDuration(startTime, "getWebRTCViewerList");
		return viewerList;
	}

	@Override
	public boolean deleteWebRTCViewerInfo(String viewerId) {
		long startTime = System.nanoTime();
		boolean	result = false;
		synchronized(broadcastLock) {

			result = datastore.find(WebRTCViewerInfo.class)
					.filter(Filters.eq(VIEWER_ID, viewerId))
					.delete()
					.getDeletedCount() == 1;
		}

		recordQueryDuration(startTime, "deleteWebRTCViewerInfo");
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateStreamMetaData(String streamId, String metaData) {
		long startTime = System.nanoTime();
		boolean result = false;
		synchronized(broadcastLock) {
			try {
				String cacheKey = getBroadcastCacheKey(streamId);
				Broadcast cachedBroadcast = getBroadcastCache().get(cacheKey, Broadcast.class);	
				getBroadcastCache().evictIfPresent(cacheKey); //if it can be updated in mongo successfully, will put it back
				
				if(cachedBroadcast != null) {
					cachedBroadcast.setMetaData(metaData);
				}

				Query<Broadcast> query = datastore.find(Broadcast.class).filter(Filters.eq(STREAM_ID, streamId));
				result = query.update(set(META_DATA, metaData)).execute().getMatchedCount() == 1;
				
				if(result && cachedBroadcast != null) {
					getBroadcastCache().put(cacheKey, cachedBroadcast);
				}
			} catch (Exception e) {
				logger.error(e.getMessage());
			}
		}
		recordQueryDuration(startTime, "updateStreamMetaData");
		return result;
	}

	public Datastore getSubscriberDatastore() {
		return subscriberDatastore;
	}

	@Override
	public SubscriberMetadata getSubscriberMetaData(String subscriberId) {
		long startTime = System.nanoTime();
		SubscriberMetadata metadata = null;
		synchronized(subscriberLock) {
			try {

				metadata = datastore.find(SubscriberMetadata.class).filter(Filters.eq(SUBSCRIBER_ID, subscriberId)).first();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		recordQueryDuration(startTime, "getSubscriberMetaData");
		return metadata;
	}

	@Override
	public void putSubscriberMetaData(String subscriberId, SubscriberMetadata metadata) {
		long startTime = System.nanoTime();


		try {
			//delete the subscriberId if exists to make it compatible with all datastores
			Query<SubscriberMetadata> query = datastore.find(SubscriberMetadata.class).filter(Filters.eq(SUBSCRIBER_ID, subscriberId));
			long deletedCount = query.delete().getDeletedCount();
			if (deletedCount > 0) {
				logger.info("There is a SubsriberMetadata exists in database. It's deleted(deletedCount:{}) and it'll put to make it easy and compatible.", deletedCount);
			}

			metadata.setSubscriberId(subscriberId);
			synchronized(subscriberLock) {

				datastore.save(metadata);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		
		recordQueryDuration(startTime, "putSubscriberMetaData");
	}

	public Datastore getConferenceRoomDatastore() {
		return conferenceRoomDatastore;
	}

	@Override
	public List<Broadcast> getSubtracks(String mainTrackId, int offset, int size, String role) {
		return getSubtracks(mainTrackId, offset, size, role, null, null, null, null);
	}

	@Override
	public List<Broadcast> getSubtracks(String mainTrackId, int offset, int size, String role, String status, String sortBy, String orderBy, String search) {
		long startTime = System.nanoTime();
		List<Broadcast> subtracks = new ArrayList<>();
		synchronized(broadcastLock) {

			Filter roleFilter = getFilterForSubtracks(mainTrackId, role, status);
			Query<Broadcast> query = datastore.find(Broadcast.class);
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
			
			subtracks = query.filter(roleFilter).iterator(findingOptions).toList();
		}
		
		recordQueryDuration(startTime, "getSubtracks");

		return subtracks;
	}

	private LogicalFilter getFilterForSubtracks(String mainTrackId, String role, String status) {

		LogicalFilter filter = Filters.and(Filters.eq(MAIN_TRACK_STREAM_ID, mainTrackId));

		if(StringUtils.isNotBlank(role)) {
			filter.add(Filters.eq(ROLE, role));
		}

		if (StringUtils.isNotBlank(status)) {
			filter.add(Filters.eq(STATUS, status));
		}

		return filter;
	}

	@Override
	public long getSubtrackCount(String mainTrackId, String role, String status) {
		long startTime = System.nanoTime();
		long subtrackCount = 0;
		synchronized(broadcastLock) {
			subtrackCount = datastore.find(Broadcast.class).filter(getFilterForSubtracks(mainTrackId, role, status)).count();
		}
		recordQueryDuration(startTime, "getSubtrackCount");
		return subtrackCount;
	}


	@Override
	public List<Broadcast> getActiveSubtracks(String mainTrackId, String role) {

		long startTime = System.nanoTime();

		LogicalFilter filterForSubtracks = getFilterForSubtracks(mainTrackId, role, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		long activeIntervalValue = System.currentTimeMillis() - (2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS);
		filterForSubtracks.add(Filters.gte(UPDATE_TIME_FIELD, activeIntervalValue));
		List<Broadcast> subtracks = new ArrayList<>();

		synchronized(broadcastLock) {

			subtracks = 	datastore.find(Broadcast.class)
					.filter(filterForSubtracks)
					.iterator().toList();
		}
		
		recordQueryDuration(startTime, "getActiveSubtracks");

		return subtracks;
	}

	@Override
	public long getActiveSubtracksCount(String mainTrackId, String role) {
		long startTime = System.nanoTime();

		LogicalFilter filterForSubtracks = getFilterForSubtracks(mainTrackId, role, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
		long activeIntervalValue = System.currentTimeMillis() - (2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS);
		filterForSubtracks.add(Filters.gte(UPDATE_TIME_FIELD, activeIntervalValue));
		long subtrackCount = 0;
		synchronized(broadcastLock) {

			subtrackCount = datastore.find(Broadcast.class)
					.filter(filterForSubtracks).count();
		}
		
		recordQueryDuration(startTime, "getActiveSubtracksCount");
		return subtrackCount;
	}

	public boolean hasSubtracks(String streamId) {
		long startTime = System.nanoTime();
		boolean result = false;
		LogicalFilter filterForSubtracks = getFilterForSubtracks(streamId, null, null);
		synchronized(broadcastLock) {

			result = datastore.find(Broadcast.class)
					.filter(filterForSubtracks).first() != null;
		}
		
		recordQueryDuration(startTime, "hasSubtracks");

		return result;
	}

	public CaffeineCacheManager getCacheManager(){
		return cacheManager;
	}

	public CaffeineCache getSubscriberCache() {
		if(subscriberCache == null){
			subscriberCache = (CaffeineCache) cacheManager.getCache(SUBSCRIBER_CACHE);
		}

		return subscriberCache;
	}
	
	public CaffeineCache getBroadcastCache() {
		if(broadcastCache == null){
			broadcastCache = (CaffeineCache) broadcastCacheManager.getCache(BROADCAST_CACHE);
		}

		return broadcastCache;
	}



	private void recordQueryDuration(long startTime, String operationName) {
		long elapsedNanos = System.nanoTime() - startTime;
		addQueryTime(elapsedNanos);
		showWarningIfElapsedTimeIsMoreThanThreshold(elapsedNanos, operationName);
	}
}
