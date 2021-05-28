package io.antmedia.console.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import io.antmedia.datastore.db.types.Broadcast;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.antmedia.cluster.ClusterNode;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.UserType;
import kotlin.jvm.functions.Function1;


public class MapDBStore implements IConsoleDataStore {

	private DB db;
	private HTreeMap<String, String> userMap;
	private Gson gson;

	protected volatile boolean available = false;

	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);

	public MapDBStore() {
		db = DBMaker.fileDB(SERVER_STORAGE_FILE).transactionEnable().make();
		userMap = db.hashMap(SERVER_STORAGE_MAP_NAME)
				.keySerializer(Serializer.STRING)
				.valueSerializer(Serializer.STRING)
				.counterEnable()
				.createOrOpen();
		gson = new Gson();
		available = true;
	}


	public boolean addUser(String username, String password, UserType userType) {
		synchronized (this) {
			boolean result = false;
			if (username != null && password != null && userType != null) {
				try {
					if (!userMap.containsKey(username)) 
					{
						User user = new User(username, password, userType);
						userMap.put(username, gson.toJson(user));
						db.commit();
						result = true;
					}
					else {
						logger.warn("user with " + username + " already exist");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					result = false;
				}
			}
			return result;
		}
	}

	public boolean editUser(String username, String password, UserType userType) {
		synchronized (this) {
			boolean result = false;
			if (username != null && password != null && userType != null)  {
				try {
					if (userMap.containsKey(username)) {
						User user = new User(username, password, userType);
						userMap.put(username, gson.toJson(user));
						db.commit();
						result = true;
					}
				}
				catch (Exception e) {
					result = false;
				}
			}
			return result;
		}
	}


	public boolean deleteUser(String username) {
		synchronized (this) {
			boolean result = false;
			if (username != null) {
				try {
					if (userMap.containsKey(username)) {
						userMap.remove(username);
						db.commit();
						result = true;
					}
				}
				catch (Exception e) {
					result = false;
				}
			}
			return result;
		}
	}

	public boolean doesUsernameExist(String username) {
		synchronized (this) {
			return userMap.containsKey(username);
		}
	}

	public boolean doesUserExist(String username, String password) {
		synchronized (this) {
			boolean result = false;
			if (username != null && password != null) {
				try {
					if (userMap.containsKey(username)) {
						String value = userMap.get(username);
						User user = gson.fromJson(value, User.class);
						if (user.getPassword().equals(password)) {
							result = true;
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
			return result;
		}
	}
	public List<User> getUserList(){
		ArrayList<User> list = new ArrayList<>();
		synchronized (this) {
			Collection<String> users = userMap.getValues();
			for (String userString : users) {
				User user = gson.fromJson(userString, User.class);
				list.add(user);
			}
		}
		return list;
	}

	public User getUser(String username) 
	{
		synchronized (this) {
			if (username != null)  {
				try {
					if (userMap.containsKey(username)) {
						String value = userMap.get(username);
						return gson.fromJson(value, User.class);
					}
				}
				catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
			return null;
		}
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
