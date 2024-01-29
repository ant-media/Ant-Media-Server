package io.antmedia.datastore.db;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.redisson.Redisson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.PushNotificationToken;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.SubscriberMetadata;
import io.antmedia.muxer.IAntMediaStreamHandler;

public class RedisStore extends MapBasedDataStore {

	protected static Logger logger = LoggerFactory.getLogger(RedisStore.class);
    	
	RedissonClient redisson;

	private RMap<Object, Object> streamInfoMap;

	private RMap<Object, Object> p2pMap;
    
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
	    	streamInfoMap = redisson.getMap(dbName+"StreamInfo");
	    	p2pMap = redisson.getMap(dbName+"P2P");
	    	subscriberMetadataMap = redisson.getMap(dbName+"SubscriberMetaData");
			
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
		    	redisson.getMap(dbName+"StreamInfo").delete();
		    	redisson.getMap(dbName+"P2P").delete();
			}
			redisson.shutdown();
		}
	}

	
	@Override
	public int resetBroadcasts(String hostAddress) {
		synchronized (this) {
			int resetBroadcasts = super.resetBroadcasts(hostAddress);
		
			Collection<Object> streamInfoValues = streamInfoMap.values();
			if (streamInfoValues != null) {
				for (Iterator<Object> streamInfoListIterator = streamInfoValues.iterator(); streamInfoListIterator.hasNext();) 
				{
					List<StreamInfo> streamInfoList = (List<StreamInfo>) streamInfoListIterator.next();
					
					for (Iterator<StreamInfo> streamInfoIterator = streamInfoList.iterator(); streamInfoIterator.hasNext();) 
					{
						StreamInfo streamInfo = streamInfoIterator.next();
						if (hostAddress.equals(streamInfo.getHost())) {
							streamInfoIterator.remove();
						}
					}
					
					if (streamInfoList.isEmpty()) {
						streamInfoListIterator.remove();
					}
					
					
				} 
			}
			return resetBroadcasts;
		}
	}


	@Override
	public List<StreamInfo> getStreamInfoList(String streamId) {
		synchronized (this) {
			List<StreamInfo> object = (List<StreamInfo>) streamInfoMap.get(streamId);
			return object != null ? object : new ArrayList<>();
		}
	}
	
	@Override
	public void clearStreamInfoList(String streamId) {
		synchronized (this) {
			streamInfoMap.clear();
		}
	}
	
	
	@Override
	public void saveStreamInfo(StreamInfo streamInfo) {
		synchronized (this) {
			List streamInfoList = (List) streamInfoMap.get(streamInfo.getStreamId());
			if (streamInfoList == null) {
				streamInfoList = new ArrayList<>();
			}
			streamInfoList.add(streamInfo);
			streamInfoMap.put(streamInfo.getStreamId(), streamInfoList);
		}
	}

	@Override
	public boolean createP2PConnection(P2PConnection conn) {
		if (conn != null) {
			p2pMap.put(conn.getStreamId(), conn);
			return true;
		}
		return false;
	}

	@Override
	public boolean deleteP2PConnection(String streamId) {
		return p2pMap.remove(streamId) != null ? true : false;
	}

	@Override
	public P2PConnection getP2PConnection(String streamId) {
		return (P2PConnection) p2pMap.get(streamId);
	}
	
	public long getLocalLiveBroadcastCount(String hostAddress) {
		return getActiveBroadcastCount(map, gson, hostAddress);
	}
	
	@Override
	public List<Broadcast> getLocalLiveBroadcasts(String hostAddress) 
	{
		return getActiveBroadcastList(hostAddress);
	}

}
