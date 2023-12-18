package io.antmedia.console.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.antmedia.datastore.db.types.User;

public abstract class MapBasedDataStore extends AbstractConsoleDataStore {

	protected Map<String, String> userMap;
	protected Gson gson;

	protected volatile boolean available = false;

	protected static Logger logger = LoggerFactory.getLogger(MapBasedDataStore.class);

	protected MapBasedDataStore() {
		gson = new Gson();
		available = true;
	}

	 @Override
	public boolean addUser(User user) {
			synchronized (this) {
				boolean result = false;
				try {
								
					if(!userMap.containsKey(user.getEmail())) { 
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

	@Override
	public boolean editUser(User user) {
		synchronized (this) {
			boolean result = false;
			try {
				String username = user.getEmail();
				
				if(userMap.containsKey(username)) { 
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

	@Override
	public boolean deleteUser(String username) {
		synchronized (this) {
			boolean result = false;
			if (username != null) {
				try 
				{
					if (userMap.containsKey(username))
					{
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

	@Override
	public boolean doesUsernameExist(String username) {
		synchronized (this) {
			return userMap.containsKey(username);
		}
	}

	@Override
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
	
	@Override
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

	@Override
	public User getUser(String username) {
		synchronized (this) {
			if (username != null)  {
				try {
					
					String user = userMap.get(username);
					
					return gson.fromJson(user, User.class);
				}
				catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
			return null;
		}
	}

	@Override
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
