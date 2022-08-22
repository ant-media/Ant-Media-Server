package io.antmedia.console.datastore;

import java.util.List;

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

	@Override
	public boolean addUser(User user) {
		return super.addUser(userMap, null, user, gson);
	}

	@Override
	public boolean editUser(User user) {
		return super.editUser(userMap, null, user, gson);
	}

	@Override
	public boolean deleteUser(String username) {
		return super.deleteUser(userMap, null, username);
	}

	@Override
	public boolean doesUsernameExist(String username) {
		return super.doesUsernameExist(userMap, null, username);
	}

	@Override
	public boolean doesUserExist(String username, String password) {
		return super.doesUserExist(userMap, null, username, password, gson);
	}
	
	@Override
	public List<User> getUserList(){
		return super.getUserList(userMap, null, gson);
	}

	@Override
	public User getUser(String username) {
		return super.getUser(userMap, null, username, gson);
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
