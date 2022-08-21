package io.antmedia.console.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.antmedia.datastore.db.types.User;

public class RedisStore extends AbstractConsoleDataStore {

	private RMap<String, String> userMap;
	private Gson gson;
	RedissonClient redisson;

	protected volatile boolean available = false;

	protected static Logger logger = LoggerFactory.getLogger(RedisStore.class);

	public RedisStore(String redisHost, String dbUser, String dbPassword, String redisPort) {

    	Config config  = new Config();
    	
    	SingleServerConfig singleServerConfig = config.useSingleServer();
    	singleServerConfig.setAddress("redis://"+redisHost+":"+redisPort);
		
		if(dbPassword != null && !dbPassword.isEmpty()) {
			singleServerConfig
		  	  .setPassword(dbPassword);
		}
		if(dbUser != null && !dbUser.isEmpty()) {
			singleServerConfig
		  	  .setUsername(dbUser);
		}
		
    	redisson = Redisson.create(config);
		
    	userMap = redisson.getMap("users");
    	
		gson = new Gson();
		available = true;
	}


	public boolean addUser(User user) {
		synchronized (this) {
			boolean result = false;
			try {
				if (!userMap.containsKey(user.getEmail()))
				{
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
		synchronized (this) {
			boolean result = false;
			try {
				String username = user.getEmail();
				if (userMap.containsKey(username)) {
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
		synchronized (this) {
			boolean result = false;
			if (username != null) {
				try {
					if (userMap.containsKey(username)) {
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
					logger.error("Does not exist user error:{} trace:{}", username, ExceptionUtils.getStackTrace(e));
				}
			}
			return result;
		}
	}
	public List<User> getUserList(){
		ArrayList<User> list = new ArrayList<>();
		synchronized (this) {
			Collection<String> users = userMap.values();
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
		}
	}

	public void close() {
		synchronized (this) {
			available = false;
			redisson.shutdown();
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
