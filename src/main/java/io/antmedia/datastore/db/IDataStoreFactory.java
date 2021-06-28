package io.antmedia.datastore.db;

public interface IDataStoreFactory {
	public static final String BEAN_NAME = "dataStoreFactory";

	public DataStore getDataStore();
}
