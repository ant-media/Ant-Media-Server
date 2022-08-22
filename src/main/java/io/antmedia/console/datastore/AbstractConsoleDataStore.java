package io.antmedia.console.datastore;

import io.antmedia.datastore.db.types.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mapdb.HTreeMap;
import org.redisson.api.RMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;


public abstract class AbstractConsoleDataStore {
	
	protected static Logger logger = LoggerFactory.getLogger(AbstractConsoleDataStore.class);
	
	public static final String SERVER_STORAGE_FILE = "server.db";
	public static final String SERVER_STORAGE_MAP_NAME = "serverdb";
	
	private Map<String, Integer> invalidLoginCountMap = new HashMap<>();

	private Map<String, Long> blockTimeMap = new HashMap<>();

	private Map<String, Boolean> isBlockedMap = new HashMap<>();

	public boolean addUser(User user) {
		return addUser(null, null, user, null);
	}
	
	public boolean addUser(RMap<String, String> redisUserMap, HTreeMap<String, String> mapdbUserMap, User user, Gson gson) {
		synchronized (this) {
			boolean result = false;
			try {
							
				if(mapdbUserMap != null && mapdbUserMap.get(user.getEmail()) == null) { 
					mapdbUserMap.put(user.getEmail(), gson.toJson(user));
					result = true;
				}
				else if(redisUserMap != null && redisUserMap.get(user.getEmail()) == null) { 
					redisUserMap.put(user.getEmail(), gson.toJson(user));
					result = true;
				}
				else {
					logger.warn("user with {} already exist", user.getEmail());
				}
			}
			catch (Exception e) {
				logger.error("Add user error:{} trace:{}", user.getEmail(), ExceptionUtils.getStackTrace(e));
				result = false;
			}

		return result;
		}
	}

	public boolean editUser(User user) {
		return editUser(null, null, user, null);
	}
	
	public boolean editUser(RMap<String, String> redisUserMap, HTreeMap<String, String> mapdbUserMap, User user, Gson gson) {
		synchronized (this) {
			boolean result = false;
			try {
				String username = user.getEmail();
				
				if(mapdbUserMap != null && !mapdbUserMap.get(username).isEmpty()) { 
					mapdbUserMap.put(username, gson.toJson(user));
					result = true;
				}
				else if(redisUserMap != null && !redisUserMap.get(username).isEmpty()) { 
					redisUserMap.put(username, gson.toJson(user));
					result = true;
				}
			}
			catch (Exception e) {
				logger.error("Edit user error:{} trace:{}", user.getEmail(), ExceptionUtils.getStackTrace(e));
				result = false;
			}
			return result;
		}
	}

	public boolean deleteUser(String username) {
		return deleteUser(null, null, username);
	}
	
	public boolean deleteUser(RMap<String, String> redisUserMap, HTreeMap<String, String> mapdbUserMap, String username) {
		
		synchronized (this) {
			boolean result = false;
			if (username != null) {
				try {
					
					if(mapdbUserMap != null && mapdbUserMap.containsKey(username)) {
						mapdbUserMap.remove(username);
						result = true;
					}
					else if (redisUserMap != null && redisUserMap.containsKey(username)) {
						redisUserMap.remove(username);
						result = true;
					}
				}
				catch (Exception e) {
					logger.error("Delete user error:{} trace:{}", username, ExceptionUtils.getStackTrace(e));
					result = false;
				}
			}
			return result;
		}
	}
	
	public boolean doesUsernameExist(String username) {
		return doesUsernameExist(null, null, username);
	}
	
	public boolean doesUsernameExist(RMap<String, String> redisUserMap, HTreeMap<String, String> mapdbUserMap, String username) {
		synchronized (this) {

			if(mapdbUserMap != null) {
				return mapdbUserMap.containsKey(username);
			}
			else {
				return redisUserMap.containsKey(username);
			}
		}
	}

	public boolean doesUserExist(String username, String password) {
		return doesUserExist(null, null, username, password, null);
	}
	
	public boolean doesUserExist(RMap<String, String> redisUserMap, HTreeMap<String, String> mapdbUserMap, String username, String password, Gson gson) {
		synchronized (this) {
			boolean result = false;
			if (username != null && password != null) {
				try {
					String userMap = null;
					
					if(mapdbUserMap != null) { 
						userMap = mapdbUserMap.get(username);
					}
					else {
						userMap = redisUserMap.get(username);
					}
					
					if (userMap.contains(username)) {
						
						String value = userMap;
						User user = gson.fromJson(value, User.class);
						if (user.getPassword().equals(password)) {
							result = true;
						}
					}
				}
				catch (Exception e) {
					logger.error("Does not exist user error:{} trace:{}", username, ExceptionUtils.getStackTrace(e));
				}
			}
			return result;
		}
	}

	public List<User> getUserList(){
		return getUserList(null, null, null);
	}
	
	public List<User> getUserList(RMap<String, String> redisUserMap, HTreeMap<String, String> mapdbUserMap, Gson gson){
		ArrayList<User> list = new ArrayList<>();
		synchronized (this) {
			
			Collection<String> users = null;
			
			if(mapdbUserMap != null) { 
				users = mapdbUserMap.getValues();
			}
			else {
				users = redisUserMap.values();
			}
			
			for (String userString : users) {
				User user = gson.fromJson(userString, User.class);
				list.add(user);
			}
		}
		return list;
	}

	public User getUser(String username) {
		return getUser(null, null, username, null);
	}
	
	public User getUser(RMap<String, String> redisUserMap, HTreeMap<String, String> mapdbUserMap, String username, Gson gson) {
		synchronized (this) {
			if (username != null)  {
				try {
					
					String user = null;
					
					if(mapdbUserMap != null) { 
						user = mapdbUserMap.get(username);
					}
					else {
						user = redisUserMap.get(username);
					}
					
					return gson.fromJson(user, User.class);
					
				}
				catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
			return null;
		}
	}

	public abstract void clear();

	public abstract void close();
	
	public abstract int getNumberOfUserRecords();
	
	/**
	 * Return if data store is available. DataStore is available if it's initialized and not closed. 
	 * It's not available if it's closed. 
	 * @return availability of the datastore
	 */
	public abstract boolean isAvailable();
	
	
	public long getBlockTime(String usermail) {
		return blockTimeMap.containsKey(usermail) ? blockTimeMap.get(usermail) : 0;
	}
	
	public int getInvalidLoginCount(String usermail){
		return  getInvalidLoginCountMap().containsKey(usermail) ? getInvalidLoginCountMap().get(usermail) : 0;
	}
	
	public boolean isUserBlocked(String usermail){
		return getIsBlockedMap().containsKey(usermail);
	}

	public void setBlockTime(String usermail, long blockTime){
		blockTimeMap.put(usermail,blockTime);
	}
	
	public void incrementInvalidLoginCount(String usermail){
		getInvalidLoginCountMap().put(usermail, getInvalidLoginCount(usermail)+1);
	}

	public void resetInvalidLoginCount(String usermail){
		getInvalidLoginCountMap().remove(usermail);
	}
	
	public void setBlocked(String usermail){
		getIsBlockedMap().put(usermail, true);
	}
	
	public void setUnBlocked(String usermail){
		getIsBlockedMap().remove(usermail);
	}

	public Map<String, Boolean> getIsBlockedMap() {
		return isBlockedMap;
	}

	public Map<String, Integer> getInvalidLoginCountMap() {
		return invalidLoginCountMap;
	}
}