package io.antmedia.console.datastore;

import io.antmedia.rest.model.User;
import io.antmedia.rest.model.UserType;
import java.util.List;


public interface IConsoleDataStore {
	
	public static final String SERVER_STORAGE_FILE = "server.db";
	public static final String SERVER_STORAGE_MAP_NAME = "serverdb";

	public boolean addUser(String username, String password, UserType userType);

	public boolean editUser(String username, String password, UserType userType);

	public boolean deleteUser(String username);
	
	public boolean doesUsernameExist(String username);

	public boolean doesUserExist(String username, String password);

	public List<User> getUserList();

	public User getUser(String username);

	public void clear();

	public void close();
	
	public int getNumberOfUserRecords();
	
	/**
	 * Return if data store is available. DataStore is available if it's initialized and not closed. 
	 * It's not available if it's closed. 
	 * @return availability of the datastore
	 */
	public boolean isAvailable();
}