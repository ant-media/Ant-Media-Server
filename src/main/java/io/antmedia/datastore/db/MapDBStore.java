package io.antmedia.datastore.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.vertx.core.Vertx;


public class MapDBStore extends MapBasedDataStore {

	private DB db;

	private Vertx vertx;
	private long timerId;
	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);
	private static final String MAP_NAME = "BROADCAST";
	private static final String VOD_MAP_NAME = "VOD";
	private static final String DETECTION_MAP_NAME = "DETECTION";
	private static final String TOKEN = "TOKEN";
	private static final String SUBSCRIBER = "SUBSCRIBER";
	private static final String CONFERENCE_ROOM_MAP_NAME = "CONFERENCE_ROOM";
	private static final String WEBRTC_VIEWER = "WEBRTC_VIEWER";


	public MapDBStore(String dbName, Vertx vertx) {
		super(dbName);
		this.vertx = vertx;
		
		db = DBMaker
				.fileDB(dbName)
				.fileMmapEnableIfSupported()
				/*.transactionEnable() we disable this because under load, it causes exception.
					//In addition, we already commit and synch methods. So it seems that we don't need this one
				 */
				.checksumHeaderBypass()
				.make();


		map = db.treeMap(MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).counterEnable()
				.createOrOpen();

		vodMap = db.treeMap(VOD_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		detectionMap = db.treeMap(DETECTION_MAP_NAME).keySerializer(Serializer.STRING)
				.valueSerializer(Serializer.STRING).counterEnable().createOrOpen();

		tokenMap = db.treeMap(TOKEN).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		subscriberMap = db.treeMap(SUBSCRIBER).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		conferenceRoomMap = db.treeMap(CONFERENCE_ROOM_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		webRTCViewerMap = db.treeMap(WEBRTC_VIEWER).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
				.counterEnable().createOrOpen();

		timerId = vertx.setPeriodic(5000, id -> 

		vertx.executeBlocking(b -> {

			synchronized (this) 
			{
				if (available) {
					db.commit();
				}
			}

		}, false, null)
				);

		available = true;
	}

	@Override
	public void close(boolean deleteDB) {
		//get db file before closing. They can be used in delete method
		Iterable<String> dbFiles = db.getStore().getAllFiles();
		synchronized (this) {
			vertx.cancelTimer(timerId);
			db.commit();
			available = false;
			db.close();
		}

		if (deleteDB) 
		{
			for (String fileName : dbFiles) 
			{
				File file = new File(fileName);
				if (file.exists()) 
				{
					try {
						Files.delete(file.toPath());
					} catch (IOException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				}
			}

		}
	}


	@Override
	public void clearStreamInfoList(String streamId) {
		//used in mongo for cluster mode. useless here.
	}
	
	@Override
	public List<StreamInfo> getStreamInfoList(String streamId) {
		return new ArrayList<>();
	}


	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		//no need to implement this method, it is used in cluster mode
	}

}
