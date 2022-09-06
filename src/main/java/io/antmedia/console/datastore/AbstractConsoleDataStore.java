package io.antmedia.console.datastore;

import io.antmedia.datastore.db.types.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
		return addUser(null, user, null);
	}
	
	public boolean addUser(Map<String, String> userMap, User user, Gson gson) {
		synchronized (this) {
			boolean result = false;
			try {
							
				if(userMap != null && userMap.get(user.getEmail()) == null) { 
					userMap.put(user.getEmail(), gson.toJson(user));
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
		return editUser(null, user, null);
	}
	
	public boolean editUser(Map<String, String> userMap, User user, Gson gson) {
		synchronized (this) {
			boolean result = false;
			try {
				String username = user.getEmail();
				
				if(userMap != null && !userMap.get(username).isEmpty()) { 
					userMap.put(username, gson.toJson(user));
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
		return deleteUser(null, username);
	}
	
	public boolean deleteUser(Map<String, String> userMap, String username) {
		
		synchronized (this) {
			boolean result = false;
			if (username != null) {
				try {
					
					if(userMap != null && userMap.containsKey(username)) {
						userMap.remove(username);
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
		return doesUsernameExist(null, username);
	}
	
	public boolean doesUsernameExist(Map<String, String> userMap, String username) {
		synchronized (this) {
			return userMap.containsKey(username);
		}
	}

	public boolean doesUserExist(String username, String password) {
		return doesUserExist(null, username, password, null);
	}
	
	public boolean doesUserExist(Map<String, String> userMap, String username, String password, Gson gson) {
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
					logger.error("Does not exist user error:{} trace:{}", username, ExceptionUtils.getStackTrace(e));
				}
			}
			return result;
		}
	}

	public List<User> getUserList(){
		return getUserList(null, null);
	}
	
	public List<User> getUserList(Map<String, String> userMap, Gson gson){
		ArrayList<User> list = new ArrayList<>();
		synchronized (this) {
			
			Collection<String> users = null;
			
			if(userMap != null) { 
				users = userMap.values();
			}
			
			for (String userString : users) {
				User user = gson.fromJson(userString, User.class);
				list.add(user);
			}
		}
		return list;
	}

	public User getUser(String username) {
		return getUser(null, username, null);
	}
	
	public User getUser(Map<String, String> userMap, String username, Gson gson) {
		synchronized (this) {
			if (username != null)  {
				try {
					
					String user = null;
					
					if(userMap != null) { 
						user = userMap.get(username);
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
	
	public int getNumberOfUserRecords() {
		return getNumberOfUserRecords(null);
	}
	
	public int getNumberOfUserRecords(Map<String,String> userMap) {
		synchronized (this) {
			return userMap.size();
		}
	}
	
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