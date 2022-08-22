package io.antmedia.console.datastore;

import java.util.List;

import io.antmedia.datastore.db.types.User;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class MapDBStore extends AbstractConsoleDataStore {

	private DB db;
	private HTreeMap<String, String> userMap;
	private Gson gson;

	protected volatile boolean available = false;

	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);

	public MapDBStore() {
		db = DBMaker.fileDB(SERVER_STORAGE_FILE).fileMmapEnableIfSupported().checksumHeaderBypass().make();
		userMap = db.hashMap(SERVER_STORAGE_MAP_NAME)
				.keySerializer(Serializer.STRING)
				.valueSerializer(Serializer.STRING)
				.counterEnable()
				.createOrOpen();
		gson = new Gson();
		available = true;
	}

	 @Override
	public boolean addUser(User user) {
		return super.addUser(null, userMap, user, gson);
	}

	@Override
	public boolean editUser(User user) {
		return super.editUser(null, userMap, user, gson);
	}

	@Override
	public boolean deleteUser(String username) {
		return super.deleteUser(null, userMap, username);
	}

	@Override
	public boolean doesUsernameExist(String username) {
		return super.doesUsernameExist(null, userMap, username);
	}

	@Override
	public boolean doesUserExist(String username, String password) {
		return super.doesUserExist(null, userMap, username, password, gson);
	}
	
	@Override
	public List<User> getUserList(){
		return super.getUserList(null, userMap, gson);
	}

	@Override
	public User getUser(String username) {
		return super.getUser(null, userMap, username, gson);
	}


	public void clear() {
		synchronized (this) {
			userMap.clear();
			db.commit();
		}
	}

	public void close() {
		synchronized (this) {
			available = false;
			db.close();
		}
	}

	public int getNumberOfUserRecords() {
		synchronized (this) {
			return userMap.size();
		}
	}

	/**
	 * Return if data store is available. DataStore is available if it's initialized and not closed. 
	 * It's not available if it's closed. 
	 * @return availability of the datastore
	 */
	public boolean isAvailable() {
		return available;
	}

}
