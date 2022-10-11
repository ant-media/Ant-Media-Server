package io.antmedia.console.datastore;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;

public class MapDBStore extends MapBasedDataStore {

	private DB db;

	private long timerId;

	private Vertx vertx;

	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);

	public MapDBStore(Vertx vertx) {
		db = DBMaker.fileDB(SERVER_STORAGE_FILE).fileMmapEnableIfSupported().checksumHeaderBypass().make();
		userMap = db.hashMap(SERVER_STORAGE_MAP_NAME)
				.keySerializer(Serializer.STRING)
				.valueSerializer(Serializer.STRING)
				.counterEnable()
				.createOrOpen();
		
		this.vertx = vertx;
		timerId = vertx.setPeriodic(5000, id -> 
		
						this.vertx.executeBlocking(b -> {
							synchronized (this) 
							{
								if (available) {
									db.commit();
								}
							}
				
						}, false, null)
		);
	}

	@Override
	public void clear() {
		synchronized (this) {
			userMap.clear();
			db.commit();
		}
	}

	@Override
	public void close() {
		synchronized (this) {
			vertx.cancelTimer(timerId);
			db.commit();
			available = false;
			db.close();
		}
	}



}
