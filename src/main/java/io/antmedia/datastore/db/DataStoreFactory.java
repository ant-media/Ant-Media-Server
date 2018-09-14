package io.antmedia.datastore.db;

import io.antmedia.datastore.DBReader;

public class DataStoreFactory {

	public static String BEAN_NAME = "dataStoreFactory";
	
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
		
		System.out.println("\n\n\n getDataStore:"+getDbType()+" "+getDbName());
		
		if (dataStore == null) {
			if(dbType.contentEquals("mongodb"))
			{
				//dataStore = new MongoStore(dbHost, dbUser, dbPassword, dbName);
				dataStore = new MongoStore(dbName);
			}
			else if(dbType .contentEquals("mapdb"))
			{
				System.out.println("\n\n\n before map db init");
				
				try {
					dataStore = new MapDBStore(dbName+".db");
					System.out.println("datastore created:"+dataStore);
				} catch (Exception e) {
					System.out.println("\n\n eeeeeee"+e+"\n");
				}
				System.out.println("\n\n\n after map db init\n\n");
			}
			else if(dbType .contentEquals("memorydb"))
			{
				dataStore = new InMemoryDataStore("dbName");
			}
			
			DBReader.instance.addDataStore(appName, dataStore);
		}
		return dataStore;
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
	
