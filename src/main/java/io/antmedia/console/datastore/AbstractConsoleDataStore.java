package io.antmedia.console.datastore;

import io.antmedia.datastore.db.types.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractConsoleDataStore {
	
	protected static Logger logger = LoggerFactory.getLogger(AbstractConsoleDataStore.class);
	
	public static final String SERVER_STORAGE_FILE = "server.db";
	public static final String SERVER_STORAGE_MAP_NAME = "serverdb";
	
	private Map<String, Integer> invalidLoginCountMap = new HashMap<>();

	private Map<String, Long> blockTimeMap = new HashMap<>();

	private Map<String, Boolean> isBlockedMap = new HashMap<>();

	public abstract boolean addUser(User user);

	public abstract boolean editUser(User user);
	
	public abstract boolean deleteUser(String username);
	
	public abstract boolean doesUsernameExist(String username);

	public abstract boolean doesUserExist(String username, String password);

	public abstract List<User> getUserList();

	public abstract User getUser(String username);
	
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