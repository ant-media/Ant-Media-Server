package io.antmedia.datastore.db;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;

public class RedisStore extends MapBasedDataStore {

	protected static Logger logger = LoggerFactory.getLogger(RedisStore.class);
    	
	RedissonClient redisson;
    
    public RedisStore(String redisConnectionUrl, String dbName) {
    	super(dbName);
    	try {
	    	File file = new File(redisConnectionUrl);
	
			Config config;
			if (file.exists()) {
	
				config = Config.fromYAML(file);
	
			}
			else {
				config  = new Config();
				config.useSingleServer()
					.setAddress(redisConnectionUrl);
			}
			
	
	    	redisson = Redisson.create(config);
	    	
	    	map = redisson.getMap(dbName+"Broadcasts");
	    	vodMap = redisson.getMap(dbName+"Vods");
	    	conferenceRoomMap = redisson.getMap(dbName+"Conferences");
	    	detectionMap = redisson.getMap(dbName+"Detections");
	    	tokenMap = redisson.getMap(dbName+"Tokens");
	    	subscriberMap = redisson.getMap(dbName+"Subscribers");	
	    	webRTCViewerMap = redisson.getMap(dbName+"WebRTCViewers");
			
			available = true;
    	}
    	 catch (IOException e) {
 			logger.error(ExceptionUtils.getStackTrace(e));
 		} 
	}




	@Override
	public void close(boolean deleteDB) {
	
		synchronized(this) {
			available = false;
			if (deleteDB) {
		    	redisson.getMap(dbName+"Broadcasts").delete();
		    	redisson.getMap(dbName+"Vods").delete();
		    	redisson.getMap(dbName+"Conferences").delete();
		    	redisson.getMap(dbName+"Detections").delete();
		    	redisson.getMap(dbName+"tokens").delete();
		    	redisson.getMap(dbName+"Subscribers").delete();	
		    	redisson.getMap(dbName+"webRTCViewers").delete();
			}
			redisson.shutdown();
		}
	}



	public List<StreamInfo> getStreamInfoList(String streamId) {
		//TODO: Implement
		return null;
	}
	
	public void clearStreamInfoList(String streamId) {
		//TODO: Implement
	}
	
	@Override
	public void addStreamInfoList(List<StreamInfo> streamInfoList) {
		//TODO: Implement
	}
	
	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		//TODO: Implement
	}

	@Override
	public boolean createP2PConnection(P2PConnection conn) {
		//TODO: Implement
		return false;
	}

	@Override
	public boolean deleteP2PConnection(String streamId) {
		//TODO: Implement
		return false;
	}

	@Override
	public P2PConnection getP2PConnection(String streamId) {
		//TODO: Implement
		return null;
	}



}
