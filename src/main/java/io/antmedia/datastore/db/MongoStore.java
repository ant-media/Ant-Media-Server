package io.antmedia.datastore.db;

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.mongodb.MongoClient;
import com.mongodb.WriteResult;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.Vod;

public class MongoStore implements IDataStore {

	private Morphia morphia;
	private Datastore datastore;

	public MongoStore(String dbName) {
		morphia = new Morphia();
		morphia.mapPackage("io.antmedia.datastore.db.types");
		datastore = morphia.createDatastore(new MongoClient(), dbName);
		datastore.ensureIndexes();
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
			Key<Broadcast> key = datastore.save(broadcast);
			return key.getId().toString();
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
			return datastore.find(Broadcast.class).field("dbId").equal(new ObjectId(id)).get();
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
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("dbId").equal(new ObjectId(id));
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
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("dbId").equal(new ObjectId(id));

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
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("dbId").equal(new ObjectId(id));

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
				Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("dbId").equal(new ObjectId(id));

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
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("dbId").equal(new ObjectId(id));
			UpdateOperations<Broadcast> ops = datastore.createUpdateOperations(Broadcast.class)
					.removeAll("endPointList", endpoint);
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
			Query<Broadcast> query = datastore.createQuery(Broadcast.class).field("dbId").equal(new ObjectId(id));
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
	public boolean addCamera(Broadcast camera) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean editCameraInfo(Broadcast camera) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCamera(String ipAddr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Broadcast getCamera(String ip) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Broadcast> getCameraList() {

		return null;

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Broadcast> filterBroadcastList(int offset, int size, String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addVod(String id, Vod vod) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Vod> getVodList(int offset, int size) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Vod> filterVoDList(int offset, int size, String keyword, long startdate, long endDate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteVod(String id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean resetBroadcastStatus() {
		// TODO Auto-generated method stub
		return false;
	}

}
