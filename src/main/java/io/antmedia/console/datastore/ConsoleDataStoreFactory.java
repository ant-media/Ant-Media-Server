package io.antmedia.console.datastore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.vertx.core.Vertx;

public class ConsoleDataStoreFactory implements ApplicationContextAware {

	private AbstractConsoleDataStore dataStore;
	
	@Value( "${"+AppSettings.SETTINGS_DB_APP_NAME+":#{null}}" )
	private String appName;
	
	@Value( "${"+io.antmedia.datastore.db.DataStoreFactory.SETTINGS_DB_NAME+":#{null}}" )
	private String dbName;
	
	@Value( "${"+io.antmedia.datastore.db.DataStoreFactory.SETTINGS_DB_TYPE+":#{null}}" )
	private String dbType;
	
	@Value( "${"+io.antmedia.datastore.db.DataStoreFactory.SETTINGS_DB_HOST+":#{null}}" )
	private String dbHost;
	
	/**
	 * @deprecated
	 * Use dbHost with full connection url including username and password
	 */
	@Deprecated(since = "2.7.0", forRemoval = true)
	@Value( "${"+io.antmedia.datastore.db.DataStoreFactory.SETTINGS_DB_USER+":#{null}}" )
	private String dbUser;
	
	/**
	 * @deprecated
	 * Use dbHost with full connection url including username and password
	 */
	@Deprecated(since = "2.7.0", forRemoval = true)
	@Value( "${"+io.antmedia.datastore.db.DataStoreFactory.SETTINGS_DB_PASS+":#{null}}" )
	private String dbPassword;

	private Vertx vertx;
	
	private static Logger logger = LoggerFactory.getLogger(ConsoleDataStoreFactory.class);
	
	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getDbType() {
		return dbType;
	}

	public void setDbType(String dbType) {
		this.dbType = dbType;
	}

	public String getDbHost() {
		return dbHost;
	}

	public void setDbHost(String dbHost) {
		this.dbHost = dbHost;
	}

	public String getDbUser() {
		return dbUser;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		vertx = (Vertx)applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);
	}

	public AbstractConsoleDataStore getDataStore() {
		if (dataStore == null) {
			if(DataStoreFactory.DB_TYPE_MONGODB.contentEquals(dbType))
			{
				dataStore = new MongoStore(dbHost, dbUser, dbPassword);
			}
			else if(DataStoreFactory.DB_TYPE_MAPDB.contentEquals(dbType))
			{
				dataStore = new MapDBStore(vertx);
			}
			else if(DataStoreFactory.DB_TYPE_REDISDB.contentEquals(dbType))
			{
				dataStore = new RedisStore(dbHost);
			}
			else {
				logger.error("Undefined Console Datastore:{}  db name:{}", dbType, dbName);
			}
		}
		return dataStore;
	}

}
	
