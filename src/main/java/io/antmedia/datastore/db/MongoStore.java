package io.antmedia.datastore.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;

public class MongoStore implements IDataStore {

	private Morphia morphia;
	private Datastore datastore;
	private Datastore vodDatastore;
	private Datastore endpointCredentialsDS;
	private Datastore tokenDatastore;
	private Datastore detectionMap;

	protected static Logger logger = LoggerFactory.getLogger(MongoStore.class);

	public static final String IMAGE_ID = "imageId"; 

	public MongoStore(String dbName) {
		morphia = new Morphia();
		morphia.mapPackage("io.antmedia.datastore.db.types");
		datastore = morphia.createDatastore(new MongoClient(), dbName);
		vodDatastore = morphia.createDatastore(new MongoClient(), dbName+"_VoD");
		endpointCredentialsDS = morphia.createDatastore(new MongoClient(), dbName+"_endpointCredentials");
		tokenDatastore = morphia.createDatastore(new MongoClient(), dbName + "_token");
		detectionMap = morphia.createDatastore(new MongoClient(), dbName + "detection");


		datastore.ensureIndexes();
		vodDatastore.ensureIndexes();
		endpointCredentialsDS.ensureIndexes();
		tokenDatastore.ensureIndexes();
		detectionMap.ensureIndexes();
	}

	public MongoStore(String host, String username, String password, String dbName) {
		morphia = new Morphia();
		morphia.mapPackage("io.antmedia.datastore.db.types");
		List<MongoCredential> credentialList = new ArrayList<>();
		credentialList.add(MongoCredential.createCredential(username, dbName, password.toCharArray()));
		datastore = morphia.createDatastore(new MongoClient(new ServerAddress(host), credentialList), dbName);
		vodDatastore=morphia.createDatastore(new MongoClient(new ServerAddress(host), credentialList), dbName+"VoD");
		endpointCredentialsDS = morphia.createDatastore(new MongoClient(new ServerAddress(host), credentialList), dbName+"_endpointCredentials");
		tokenDatastore = morphia.createDatastore(new MongoClient(), dbName + "_token");
		detectionMap = morphia.createDatastore(new MongoClient(), dbName + "detection");


		tokenDatastore.ensureIndexes();
		datastore.ensureIndexes();
		vodDatastore.ensureIndexes();
		endpointCredentialsDS.ensureIndexes();
		detectionMap.ensureIndexes();

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
			Key<Broadcast> key = datastore.save(broadcast);

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
		try {
			return datastore.find(Broadcast.class).field("streamId").equal(id).get();
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	@Override
	public VoD getVoD(String id) {
		try {
			return vodDatastore.find(VoD.class).field("vodId").equal(id).get();
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#updateName(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public boolean updateName(String id, String name, String description) {
		try {

			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("name", name)
					.set("description", description);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#updateStatus(java.lang.String,
	 * java.lang.String)
	 */
	@Override
	public boolean updateStatus(String id, String status) {
		try {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);

			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("status", status);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
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
		try {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);

			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("duration",
					duration);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
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
		if (id != null && endpoint != null) {
			try {
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);

				UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).push("endPointList",
						endpoint);

				UpdateResults update = datastore.update(query, ops);
				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return false;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint) {
		boolean result = false;

		if (id != null && endpoint != null) {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class)

					.removeAll("endPointList", endpoint);
			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		}
		return result;
	}

	@Override
	public boolean removeAllEndpoints(String id) {
		boolean result = false;

		if (id != null) {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).unset("endPointList");
			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
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
		return datastore.getCount(Broadcast.class);
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#delete(java.lang.String)
	 */
	@Override
	public boolean delete(String id) {
		try {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			WriteResult delete = datastore.delete(query);
			return delete.getN() == 1;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size) {
		return datastore.find(Broadcast.class).asList(new FindOptions().skip(offset).limit(size));
	}

	public Datastore getDataStore() {
		return datastore;
	}




	@Override
	public List<Broadcast> getExternalStreamsList() {
		try {
			List<Broadcast> ipCameraList=datastore.find(Broadcast.class).field("type").equal(AntMediaApplicationAdapter.IP_CAMERA).asList();
			List<Broadcast> streamSourceList=datastore.find(Broadcast.class).field("type").equal(AntMediaApplicationAdapter.STREAM_SOURCE).asList();

			List<Broadcast> newList = new ArrayList<Broadcast>(ipCameraList);

			newList.addAll(streamSourceList);

			return newList;

		} catch (Exception e) {

			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	@Override
	public void close() {
		datastore.getMongo().close();
	}

	@Override
	public List<Broadcast> filterBroadcastList(int offset, int size, String type) {
		try {
			return datastore.find(Broadcast.class).field("type").equal(type).asList(new FindOptions().skip(offset).limit(size));
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return null;	
	}

	@Override
	public String addVod(VoD vod) {

		String id = null;
		boolean result = false;
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
		return id;

	}

	@Override
	public List<VoD> getVodList(int offset, int size) {	
		return vodDatastore.find(VoD.class).asList(new FindOptions().skip(offset).limit(size));
	}


	@Override
	public boolean deleteVod(String id) {
		try {
			Query<VoD> query = vodDatastore.createQuery(VoD.class).field("vodId").equal(id);
			WriteResult delete = vodDatastore.delete(query);
			return delete.getN() == 1;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}



	@Override
	public long getTotalVodNumber() {
		return vodDatastore.getCount(VoD.class);

	}

	@Override
	public int fetchUserVodList(File userfile) {

		if(userfile==null) {
			return 0;
		}

		int numberOfSavedFiles = 0;
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
						(fileExtension.equals("mp4") || fileExtension.equals("flv") || fileExtension.equals("mkv"))) {

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
		return numberOfSavedFiles;

	}




	@Override
	public boolean updateSourceQualityParameters(String id, String quality, double speed, int pendingPacketQueue) {
		try {

			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class)
					.set("quality", quality).set("speed", speed).set("pendingPacketSize", pendingPacketQueue);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}


	public SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials) {
		SocialEndpointCredentials addedCredential = null;
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
		return addedCredential;
	}

	@Override
	public List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size) {
		return endpointCredentialsDS.find(SocialEndpointCredentials.class).asList(new FindOptions().skip(offset).limit(size));
	}

	@Override
	public boolean removeSocialEndpointCredentials(String id) {
		try {
			Query<SocialEndpointCredentials> query = endpointCredentialsDS
					.createQuery(SocialEndpointCredentials.class)
					.field("id").equal(new ObjectId(id));
			WriteResult delete = endpointCredentialsDS.delete(query);
			return delete.getN() == 1;
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}

	@Override
	public SocialEndpointCredentials getSocialEndpointCredentials(String id) {
		try {
			return endpointCredentialsDS.get(SocialEndpointCredentials.class, new ObjectId(id));
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
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
		return datastore.getCount(Broadcast.class);
	}

	public Datastore getVodDatastore() {
		return vodDatastore;
	}

	public void setVodDatastore(Datastore vodDatastore) {
		this.vodDatastore = vodDatastore;
	}

	@Override
	public long getActiveBroadcastCount() {
		return datastore.find(Broadcast.class).filter("status", AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING).count();
	}

	public void saveDetection(String id, long timeElapsed, List<TensorFlowObject> detectedObjects) {
		if (detectedObjects != null) {
			for (TensorFlowObject tensorFlowObject : detectedObjects) {
				tensorFlowObject.setDetectionTime(timeElapsed);
				tensorFlowObject.setImageId(id);
				detectionMap.save(tensorFlowObject);
			}
		}

	}

	@Override
	public List<TensorFlowObject> getDetectionList(String idFilter, int offsetSize, int batchSize) {
		try {
			return detectionMap.find(TensorFlowObject.class).field(IMAGE_ID).startsWith(idFilter).asList(new FindOptions().skip(offsetSize).limit(batchSize));
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;	
	}

	@Override
	public List<TensorFlowObject> getDetection(String id) {
		try {
			return detectionMap.find(TensorFlowObject.class).field(IMAGE_ID).equal(id).asList();
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return null;	
	}

	@Override
	public long getObjectDetectedTotal(String id) {
		return detectionMap.find(TensorFlowObject.class).field(IMAGE_ID).equal(id).asList().size();
	}



	@Override
	public boolean editStreamSourceInfo(Broadcast broadcast) {
		boolean result = false;

		try {
			logger.warn("result inside edit camera: {}" , result);
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(broadcast.getStreamId());

			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("name", broadcast.getName())
					.set("username", broadcast.getUsername()).set("password", broadcast.getPassword()).set("ipAddr", broadcast.getIpAddr())
					.set("streamUrl", broadcast.getStreamUrl());

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateHLSViewerCount(String streamId, int diffCount) {
		try {

			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(streamId);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).inc("hlsViewerCount", diffCount);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean updateWebRTCViewerCount(String streamId, boolean increment) {
		try {

			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(streamId);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class);
			String field = "webRTCViewerCount";
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
		return false;
	}

	@Override
	public boolean updateRtmpViewerCount(String streamId, boolean increment) {
		try {

			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(streamId);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class);
			String field = "rtmpViewerCount";
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
		return false;
	}

	@Override
	public Token createToken(String streamId, long expireDate, String type) {
		Token token = null;

		if(streamId != null) {
			token = new Token();
			token.setStreamId(streamId);
			token.setExpireDate(expireDate);
			token.setType(type);

			try {
				String tokenId = RandomStringUtils.randomNumeric(24);
				token.setTokenId(tokenId);
				tokenDatastore.save(token);

			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}

		return token;
	}

	@Override
	public Token validateToken(Token token) {
		Token fetchedToken = null;
		if (token.getTokenId() != null) {
			fetchedToken = tokenDatastore.find(Token.class).field("tokenId").equal(token.getTokenId()).get();
			if (fetchedToken != null && fetchedToken.getStreamId().equals(token.getStreamId()) && fetchedToken.getType().equals(token.getType())) {

				Query<Token> query = tokenDatastore.createQuery(Token.class).field("tokenId").equal(token.getTokenId());
				WriteResult delete = tokenDatastore.delete(query);
				if(delete.getN() == 1) {
					return fetchedToken;
				}
			}else {
				fetchedToken = null;
			}
		}
		return fetchedToken;
	}

	@Override
	public boolean revokeTokens(String streamId) {

		Query<Token> query = tokenDatastore.createQuery(Token.class).field("streamId").equal(streamId);
		WriteResult delete = tokenDatastore.delete(query);

		return delete.getN() >= 1;

	}

	@Override
	public List<Token> listAllTokens(String streamId, int offset, int size) {
		return 	tokenDatastore.find(Token.class).field("streamId").equal(streamId).asList(new FindOptions() .skip(offset).limit(size));

	}






}
