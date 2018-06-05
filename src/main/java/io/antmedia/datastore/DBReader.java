package io.antmedia.datastore;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections.map.HashedMap;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;

public class DBReader {

	public static DBReader instance = new DBReader();
	
	ConcurrentHashMap<String, IDataStore> dbMap = new ConcurrentHashMap<>();	
	public String getHost(String streamName, String appName) {
		Broadcast broadcast = dbMap.get(appName).get(streamName);
		String host = null;
		if(broadcast != null) {
			host = broadcast.getOriginAdress();
		}
		return host;
	}
	
	public void addDataStore(String appName, IDataStore store) {
		dbMap.put(appName, store);
	}
}
