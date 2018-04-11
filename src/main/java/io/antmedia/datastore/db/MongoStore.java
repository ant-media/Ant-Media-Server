package io.antmedia.datastore.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.Vod;

public class MongoStore implements IDataStore {

	private Morphia morphia;
	private Datastore datastore;
	private Datastore vodDatastore;
	private Datastore endpointCredentialsDS;
	
	protected static Logger logger = LoggerFactory.getLogger(MongoStore.class);

	public MongoStore(String dbName) {
		morphia = new Morphia();
		morphia.mapPackage("io.antmedia.datastore.db.types");
		datastore = morphia.createDatastore(new MongoClient(), dbName);
		vodDatastore = morphia.createDatastore(new MongoClient(), dbName+"Vod");
		endpointCredentialsDS = morphia.createDatastore(new MongoClient(), dbName+"_endpointCredentials");
		datastore.ensureIndexes();
		vodDatastore.ensureIndexes();
		endpointCredentialsDS.ensureIndexes();
	}

	public MongoStore(String host, String username, String password, String dbName) {
		morphia = new Morphia();
		morphia.mapPackage("io.antmedia.datastore.db.types");
		List<MongoCredential> credentialList = new ArrayList<MongoCredential>();
		credentialList.add(MongoCredential.createCredential(username, dbName, password.toCharArray()));
		datastore = morphia.createDatastore(new MongoClient(new ServerAddress(host), credentialList), dbName);
		vodDatastore=morphia.createDatastore(new MongoClient(new ServerAddress(host), credentialList), dbName+"Vod");
		endpointCredentialsDS = morphia.createDatastore(new MongoClient(new ServerAddress(host), credentialList), dbName+"_endpointCredentials");
		
		datastore.ensureIndexes();
		vodDatastore.ensureIndexes();
		endpointCredentialsDS.ensureIndexes();
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
			Key<Broadcast> key = datastore.save(broadcast);

			return streamId;
		} catch (Exception e) {

			e.printStackTrace();
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

			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see io.antmedia.datastore.db.IDataStore#updatePublish(java.lang.String,
	 * boolean)
	 */
	@Override
	public boolean updatePublish(String id, boolean publish) {
		try {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("dbId").equal(new ObjectId(id));

			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("publish", publish);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			e.printStackTrace();
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
				e.printStackTrace();
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
			e.printStackTrace();
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
	public boolean editCameraInfo(Broadcast camera) {
		boolean result = false;

		try {
			logger.warn("result inside edit camera: " + result);
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(camera.getStreamId());
			

			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("name", camera.getName())
					.set("username", camera.getUsername()).set("password", camera.getPassword()).set("ipAddr", camera.getIpAddr());

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
			
	
	}

	@Override
	public boolean deleteStream(String id) {
	
		try {
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			WriteResult delete = datastore.delete(query);
			return delete.getN() == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;

	}

	@Override
	public List<Broadcast> getExternalStreamsList() {
		try {
			List<Broadcast> ipCameraList=datastore.find(Broadcast.class).field("type").equal("ipCamera").asList();
			List<Broadcast> streamSourceList=datastore.find(Broadcast.class).field("type").equal("streamSource").asList();
			
			List<Broadcast> newList = new ArrayList<Broadcast>(ipCameraList);
			
			newList.addAll(streamSourceList);
			
			return newList;
					
		} catch (Exception e) {

			e.printStackTrace();
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
			e.printStackTrace();
		}
		return null;	
	}

	@Override
	public boolean addVod(String id, Vod vod) {
		String vodId = null;
		boolean result = false;
		try {	
			if (vod.getStreamId() == null) {
				vodId = RandomStringUtils.randomAlphanumeric(12) + System.currentTimeMillis();
				vod.setStreamId(vodId);
			}
			vodId = vod.getStreamId();

			Key<Vod> key = vodDatastore.save(vod);
			result = true;
			return result;
		} catch (Exception e) {

			e.printStackTrace();
		}
		return result;
		
	}

	@Override
	public List<Vod> getVodList(int offset, int size) {	
		return vodDatastore.find(Vod.class).asList(new FindOptions().skip(offset).limit(size));
	}


	@Override
	public boolean deleteVod(String id) {
		try {
			Query<Broadcast> query = vodDatastore.createQuery(Broadcast.class).field("vodId").equal(id);
			WriteResult delete = vodDatastore.delete(query);
			return delete.getN() == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}



	@Override
	public long getTotalVodNumber() {
		return vodDatastore.getCount(Broadcast.class);
	
	}

	@Override
	public boolean fetchUserVodList(File userfile) {
		
		boolean result=false;
		try {
			Query<Vod> query = vodDatastore.createQuery(Vod.class).field("type").equal("userVod");
			WriteResult delete = vodDatastore.delete(query);
			result=true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		File[] listOfFiles = userfile.listFiles();

		for (File file : listOfFiles) {
			
			String fileExtension = FilenameUtils.getExtension(file.getName());
			
		    if (file.isFile()&&fileExtension.equals("mp4")) {
		
				long fileSize = file.length();
				long unixTime = System.currentTimeMillis();

				Vod newVod = new Vod("vodFile", "vodFile", file.getPath(), file.getName(), unixTime, 0, fileSize,
						"userVod");
		    	addUserVod("vodFile", newVod);
		    }
		}
		
		
		return result;

	}

	@Override
	public boolean addUserVod(String id, Vod vod) {
		try {
			String vodId = null;
			if (vod.getVodId() == null) {
				vodId = RandomStringUtils.randomAlphanumeric(12) + System.currentTimeMillis();
				vod.setVodId(vodId);
			}
			vodId = vod.getStreamId();
		

			Key<Vod> key = vodDatastore.save(vod);

			return true;
		} catch (Exception e) {

			e.printStackTrace();
		}
		return false;
	}



	@Override
	public boolean updateSourceQuality(String id, String quality) {
		try {

			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("quality", quality);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override

	public boolean updateSourceSpeed(String id, double speed) {
		try {

			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("streamId").equal(id);
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class).set("speed", speed);

			UpdateResults update = datastore.update(query, ops);
			return update.getUpdatedCount() == 1;
		} catch (Exception e) {
			e.printStackTrace();
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
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public SocialEndpointCredentials getSocialEndpointCredentials(String id) {
		try {
			return endpointCredentialsDS.get(SocialEndpointCredentials.class, new ObjectId(id));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Datastore getEndpointCredentialsDS() {
		return endpointCredentialsDS;
	}

	public void setEndpointCredentialsDS(Datastore endpointCredentialsDS) {
		this.endpointCredentialsDS = endpointCredentialsDS;
	}


}
