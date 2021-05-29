package io.antmedia.console.datastore;


import dev.morphia.query.FindOptions;
import io.antmedia.datastore.db.types.Broadcast;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteResult;

import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import dev.morphia.query.UpdateResults;
import io.antmedia.rest.model.User;
import io.antmedia.rest.model.UserType;

import java.util.List;
import java.util.ArrayList;

public class MongoStore implements IConsoleDataStore {

	private Morphia morphia;
	private Datastore datastore;

	protected static Logger logger = LoggerFactory.getLogger(MongoStore.class);

	protected volatile boolean available = false;

	public MongoStore(String dbHost, String dbUser, String dbPassword) {
		String dbName = SERVER_STORAGE_MAP_NAME;

		String uri =  io.antmedia.datastore.db.MongoStore.getMongoConnectionUri(dbHost, dbUser, dbPassword);

		MongoClientURI mongoUri = new MongoClientURI(uri);
		MongoClient client = new MongoClient(mongoUri);

		morphia = new Morphia();

		datastore = morphia.createDatastore(client, dbName);
		datastore.ensureIndexes();

		available = true;
	}

	@Override
	public List<User> getUserList(){
		synchronized(this) {
			List<User> users = new ArrayList<User>();
			try {
				Query<User> query = datastore.find(User.class);
				users = query.find(new FindOptions()).toList();
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			return users;
		}
	}

	@Override
	public boolean addUser(String username, String password, UserType userType) {
		synchronized(this) {
			boolean result = false;

			if (username != null && password != null && userType != null) {
				User existingUser = datastore.find(User.class).field("email").equal(username).get();
				if (existingUser == null) 
				{
					User user = new User(username, password, userType);
					datastore.save(user);
					result = true;
				}
				else {
					logger.warn("user with {} already exist", username);
				}
			}
			return result;
		}
	}

	@Override
	public boolean editUser(String username, String password, UserType userType) {
		synchronized(this) {
			try {
				Query<User> query = datastore.createQuery(User.class).field("email").equal(username);
				UpdateOperations<User> ops = datastore.createUpdateOperations(User.class).set("email", username)
						.set("password", password).set("userType", userType);

				UpdateResults update = datastore.update(query, ops);

				return update.getUpdatedCount() == 1;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	@Override
	public boolean deleteUser(String username) {
		synchronized(this) {
			try {
				Query<User> query = datastore.createQuery(User.class).field("email").equal(username);
				WriteResult delete = datastore.delete(query);
				return delete.getN() == 1;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		}
	}

	@Override
	public boolean doesUsernameExist(String username) {
		synchronized(this) {
			User existingUser = datastore.find(User.class).field("email").equal(username).get();

			return existingUser != null;
		}
	}

	@Override
	public boolean doesUserExist(String username, String password) {
		synchronized(this) {
			User existingUser = datastore.find(User.class).field("email").equal(username).get();
			if(existingUser != null)
			{
				return existingUser.getPassword().contentEquals(password);
			}
			return false;
		}
	}

	@Override
	public User getUser(String username) {
		synchronized(this) {
			return datastore.find(User.class).field("email").equal(username).get();
		}
	}

	@Override
	public void clear() {
		synchronized(this) {
			datastore.delete(datastore.createQuery(User.class));
		}
	}

	@Override
	public void close() {
		synchronized(this) {
			available = false;
			datastore.getMongo().close();
		}
	}

	@Override
	public int getNumberOfUserRecords() {
		synchronized(this) {
			return (int) datastore.getCount(User.class);
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
