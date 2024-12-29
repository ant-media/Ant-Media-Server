package io.antmedia.datastore.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;

public class DataStoreFactory implements IDataStoreFactory, ApplicationContextAware{

	//TODO: I think all settings should be get from AppSettings bean
	
	public static final String DB_TYPE_MEMORYDB = "memorydb";
	public static final String DB_TYPE_MAPDB = "mapdb";
	public static final String DB_TYPE_MONGODB = "mongodb";
	public static final String DB_TYPE_REDISDB = "redisdb";
	
	public static final String SETTINGS_DB_NAME = "db.name";
	public static final String SETTINGS_DB_TYPE = "db.type";
	public static final String SETTINGS_DB_HOST = "db.host";
	public static final String SETTINGS_DB_USER = "db.user";
	public static final String SETTINGS_DB_PASS = "db.password";


	private static Logger logger = LoggerFactory.getLogger(DataStoreFactory.class);
	
	
	private DataStore dataStore;
	 
	@Value( "${"+SETTINGS_DB_NAME+":#{null}}" )
	private String dbName;

	/**
	 * One of the DB_TYPE_*
	 */
	
	@Value( "${"+SETTINGS_DB_TYPE+":#{null}}" )
	private String dbType;
	
	@Value( "${"+SETTINGS_DB_HOST+":#{null}}" )
	private String dbHost;
	
	@Value( "${"+SETTINGS_DB_USER+":#{null}}" )
	private String dbUser;
	
	@Value( "${"+SETTINGS_DB_PASS+":#{null}}" )
	private String dbPassword;

	private String hostAddress;
	
	private Vertx vertx;
	private boolean writeStatsToDatastore;
	private boolean writeSubscriberEventsToDatastore;
	private AppSettings appSettings;
	
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
	
	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public void init()  
	{
		if(DB_TYPE_MONGODB.contentEquals(dbType))
		{
			dataStore = new MongoStore(dbHost, dbUser, dbPassword, dbName);
		}
		else if(DB_TYPE_MAPDB .contentEquals(dbType))
		{
			dataStore = new MapDBStore(dbName+".db", vertx);
		}
		else if(DB_TYPE_REDISDB .contentEquals(dbType))
		{
			dataStore = new RedisStore(dbHost, dbName);
		}
		else if(DB_TYPE_MEMORYDB .contentEquals(dbType))
		{
			dataStore = new InMemoryDataStore(dbName);
		}
		else {
			logger.error("Undefined Datastore:{}  db name:{}", dbType, dbName);
		}
		
		logger.info("Used Datastore:{}  db name:{}", getDbType(), getDbName());
		
		if(dataStore != null) {
			dataStore.setAppSettings(appSettings);
		}
	}	
	
	public DataStore getDataStore() {
		return dataStore;
	}
	
	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	public boolean isWriteStatsToDatastore() {
		return writeStatsToDatastore;
	}

	public void setWriteStatsToDatastore(boolean writeStatsToDatastore) {
		this.writeStatsToDatastore = writeStatsToDatastore;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		vertx = (Vertx)applicationContext.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);
		
		ServerSettings serverSettings = (ServerSettings) applicationContext.getBean(ServerSettings.BEAN_NAME);
		hostAddress = serverSettings.getHostAddress();
		appSettings = ((AppSettings) applicationContext.getBean(AppSettings.BEAN_NAME));
		
		init();
	}
	
	public void setWriteSubscriberEventsToDatastore(boolean writeSubscriberEventsToDatastore) {
		this.writeSubscriberEventsToDatastore = writeSubscriberEventsToDatastore;
	}
	
	public boolean isWriteSubscriberEventsToDatastore() {
		return writeSubscriberEventsToDatastore;
	}

}
	
