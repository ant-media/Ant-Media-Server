package io.antmedia.datastore.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.DBReader;

public class DataStoreFactory {

	public static final String BEAN_NAME = "dataStoreFactory";
	private static Logger logger = LoggerFactory.getLogger(DataStoreFactory.class);

	
	private IDataStore dataStore;
	private String appName;
	private String dbName;
	private String dbType;
	private String dbHost;
	private String dbUser;
	private String dbPassword;
	
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
	
	public IDataStore getDataStore() {
		if (dataStore == null) {
			if(dbType.contentEquals("mongodb"))
			{
				// dataStore = new MongoStore(dbHost, dbUser, dbPassword, dbName)
				dataStore = new MongoStore(dbName, dbHost);
			}
			else if(dbType .contentEquals("mapdb"))
			{
				dataStore = new MapDBStore(dbName+".db");
			}
			else if(dbType .contentEquals("memorydb"))
			{
				dataStore = new InMemoryDataStore("dbName");
			}
			else {
				logger.error("Undefined Datastore:{} app:{} db name:{}", dbType, appName, dbName);
			}
			
			logger.info("Used Datastore:{} app:{} db name:{}", dbType, appName, dbName);
			
			DBReader.instance.addDataStore(appName, dataStore);
		}
		return dataStore;
	}
	
	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}

	public String getAppName()
	{
		return appName;
	}
	
	public void setAppName(String appName)
	{
		this.appName = appName;
	}

}
	
