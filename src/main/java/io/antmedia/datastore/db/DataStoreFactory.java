package io.antmedia.datastore.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.settings.ServerSettings;

public class DataStoreFactory implements IDataStoreFactory, ApplicationContextAware{

	//TODO: I think all settings should be get from AppSettings bean
	
	public static final String DB_TYPE_MEMORYDB = "memorydb";
	public static final String DB_TYPE_MAPDB = "mapdb";
	public static final String DB_TYPE_MONGODB = "mongodb";
	
	public static final String SETTINGS_DB_NAME = "db.name";
	public static final String SETTINGS_DB_TYPE = "db.type";
	public static final String SETTINGS_DB_HOST = "db.host";
	public static final String SETTINGS_DB_USER = "db.user";
	public static final String SETTINGS_DB_PASS = "db.password";


	private static Logger logger = LoggerFactory.getLogger(DataStoreFactory.class);
	
	public static final String BEAN_NAME = "dataStoreFactory";

	
	private DataStore dataStore;
	 
	@Value( "${" + AppSettings.SETTINGS_WRITE_STATS_TO_DATASTORE +":true}")
	private boolean writeStatsToDatastore;
	
	
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
	
	public void init()  
	{
		if(dbType.contentEquals(DB_TYPE_MONGODB))
		{
			dataStore = new MongoStore(dbHost, dbUser, dbPassword, dbName);
		}
		else if(dbType .contentEquals(DB_TYPE_MAPDB))
		{
			dataStore = new MapDBStore(dbName+".db");
		}
		else if(dbType .contentEquals(DB_TYPE_MEMORYDB))
		{
			dataStore = new InMemoryDataStore(dbName);
		}
		else {
			logger.error("Undefined Datastore:{}  db name:{}", dbType, dbName);
		}
		
		logger.info("Used Datastore:{}  db name:{}", getDbType(), getDbName());
		
		if(dataStore != null) {
			dataStore.setWriteStatsToDatastore(writeStatsToDatastore);
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
		ServerSettings serverSettings = (ServerSettings) applicationContext.getBean(ServerSettings.BEAN_NAME);
		hostAddress = serverSettings.getHostAddress();
		init();
	}

}
	
