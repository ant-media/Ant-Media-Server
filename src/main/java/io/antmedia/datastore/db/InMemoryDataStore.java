package io.antmedia.datastore.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.Vod;

public class InMemoryDataStore implements IDataStore {


	protected static Logger logger = LoggerFactory.getLogger(InMemoryDataStore.class);

	private Gson gson;

	public LinkedHashMap<String, Broadcast> broadcastMap = new LinkedHashMap<String, Broadcast>();

	public LinkedHashMap<String, Vod> vodMap = new LinkedHashMap<String, Vod>();
	
	public LinkedHashMap<String, SocialEndpointCredentials> socialEndpointCredentialsMap = new LinkedHashMap<String, SocialEndpointCredentials>();


	public InMemoryDataStore(String dbName) {
	}

	@Override
	public String save(Broadcast broadcast) {

		String streamId = null;
		if (broadcast != null) {

			try {
				if (broadcast.getStreamId() == null) {
					streamId = RandomStringUtils.randomNumeric(24);
					broadcast.setStreamId(streamId);
				}
				streamId = broadcast.getStreamId();
				String rtmpURL = broadcast.getRtmpURL();
				if (rtmpURL != null) {
					rtmpURL += streamId;
				}
				broadcast.setRtmpURL(rtmpURL);
				broadcastMap.put(streamId, broadcast);
			} catch (Exception e) {
				e.printStackTrace();
				streamId = null;
			}

		}
		return streamId;
	}

	@Override
	public Broadcast get(String id) {

		return broadcastMap.get(id);
	}

	@Override
	public boolean updateName(String id, String name, String description) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setName(name);
			broadcast.setDescription(description);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean updateStatus(String id, String status) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setStatus(status);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setDuration(duration);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean updatePublish(String id, boolean publish) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcast.setPublish(publish);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null && endpoint != null) {
			List<Endpoint> endPointList = broadcast.getEndPointList();
			if (endPointList == null) {
				endPointList = new ArrayList<>();
			}
			endPointList.add(endpoint);
			broadcast.setEndPointList(endPointList);
			broadcastMap.put(id, broadcast);
			result = true;
		}
		return result;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint) {
		boolean result = false;
		Broadcast broadcast = broadcastMap.get(id);
		if (broadcast != null && endpoint != null) {
			List<Endpoint> endPointList = broadcast.getEndPointList();
			if (endPointList != null) {
				for (Iterator<Endpoint> iterator = endPointList.iterator(); iterator.hasNext();) {
					Endpoint endpointItem = (Endpoint) iterator.next();
					if (endpointItem.rtmpUrl.equals(endpoint.rtmpUrl)) {
						iterator.remove();
						result = true;
						break;
					}
				}

			}
		}
		return result;
	}

	@Override
	public long getBroadcastCount() {
		return broadcastMap.size();
	}

	@Override
	public boolean delete(String id) {
		Broadcast broadcast = broadcastMap.get(id);
		boolean result = false;
		if (broadcast != null) {
			broadcastMap.remove(id);
			result = true;
		}
		return result;
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size) {
		Collection<Broadcast> values = broadcastMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<Broadcast> list = new ArrayList<Broadcast>();
		for (Broadcast broadcast : values) {

			if (t < offset) {
				t++;
				continue;
			}
			list.add(broadcast);
			itemCount++;

			if (itemCount >= size) {
				break;
			}

		}
		return list;
	}



	@Override
	public boolean editCameraInfo(Broadcast camera) {
		boolean result = false;
		try {
			logger.warn("inside of editCameraInfo");

			Broadcast oldCam = get(camera.getStreamId());

			oldCam.setName(camera.getName());
			oldCam.setUsername(camera.getUsername());
			oldCam.setPassword(camera.getPassword());
			oldCam.setIpAddr(camera.getIpAddr());

			broadcastMap.replace(oldCam.getStreamId(), oldCam);


			result = true;
		} catch (Exception e) {
			result = false;
		}

		return result;
	}

	@Override
	public boolean deleteStream(String id) {
		boolean result = false;
		try {

			if (broadcastMap.containsKey(id)) {
				logger.warn("inside of deleteStream");
				broadcastMap.remove(id);
				result = true;
			}

		} catch (Exception e) {
			result = false;
		}
		return result;
	}


	@Override
	public List<Broadcast> getExternalStreamsList() {
		Object[] objectArray = broadcastMap.values().toArray();

		Broadcast[] broadcastArray = new Broadcast[objectArray.length];

		List<Broadcast> streamsList = new ArrayList<Broadcast>();

		for (int i = 0; i < objectArray.length; i++) {

			broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);

		}

		for (int i = 0; i < broadcastArray.length; i++) {

			if (broadcastArray[i].getType().equals("ipCamera") || broadcastArray[i].getType().equals("streamSource")) {

				streamsList.add(gson.fromJson((String) objectArray[i], Broadcast.class));
			}
		}

		return streamsList;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Broadcast> filterBroadcastList(int offset, int size, String type) {
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}

		Collection<Broadcast> values =broadcastMap.values();

		List<Broadcast> list = new ArrayList();

		for (Broadcast broadcast : values) 
		{
			if(broadcast.getType().equals("ipCamera")) 
			{
				if (t < offset) {
					t++;
					continue;
				}
				list.add(broadcast);

				itemCount++;

				if (itemCount >= size) {
					break;
				}
			}
		}

		return list;
	}

	@Override
	public boolean addVod(String id, Vod vod) {
		String vodId = null;
		boolean result = false;

		if (vod != null) {
			try {
				vodId = RandomStringUtils.randomNumeric(24);
				vod.setVodId(vodId);

				vodMap.put(vodId,vod);
				result = true;

			} catch (Exception e) {
				e.printStackTrace();
				
			}
		}
		return result;
	}

	@Override
	public List<Vod> getVodList(int offset, int size) {
		Collection<Vod> values = vodMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<Vod> list = new ArrayList();
		for (Vod vodString : values) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(vodString);
			itemCount++;

			if (itemCount >= size) {
				break;
			}

		}
		return list;
	}



	@Override
	public boolean deleteVod(String id) {
		boolean result = vodMap.remove(id) != null;

		return result;
	}



	public boolean removeAllEndpoints(String id) {
		boolean result = false;
		Broadcast broadcast = broadcastMap.get(id);
		if (broadcast != null) {
			broadcast.setEndPointList(null);
			broadcastMap.replace(id, broadcast);
			result = true;
		}
		return result;

	}

	@Override
	public long getTotalVodNumber() {

		return vodMap.size();

	}

	@Override
	public boolean fetchUserVodList(File userfile) {

		Object[] objectArray = vodMap.values().toArray();

		Vod[] vodtArray = new Vod[objectArray.length];
		for (int i = 0; i < objectArray.length; i++) {
			vodtArray[i] = gson.fromJson((String) objectArray[i], Vod.class);
		}

		for (int i = 0; i < vodtArray.length; i++) {
			if (vodtArray[i].getType().equals("userVod")) {
				vodMap.remove(vodtArray[i].getVodId());
			}
		}

		File[] listOfFiles = userfile.listFiles();

		for (File file : listOfFiles) {

			String fileExtension = FilenameUtils.getExtension(file.getName());

			if (file.isFile() && fileExtension.equals("mp4")) {
				long fileSize = file.length();
				long unixTime = System.currentTimeMillis();

				Vod newVod = new Vod("vodFile", "vodFile", file.getPath(), file.getName(), unixTime, 0, fileSize,
						"userVod");

				addUserVod("vodFile", newVod);
			}
		}


		return true;



	}

	@Override
	public boolean addUserVod(String id, Vod vod) {
		String vodId = null;
		boolean result = false;

		if (vod != null) {
			try {
				vodId = RandomStringUtils.randomNumeric(24);
				vod.setVodId(vodId);
				vodMap.put(vodId, vod);
				result = true;

			} catch (Exception e) {
				e.printStackTrace();
				
			}
		}
		return result;
	}



	@Override
	public boolean updateSourceQuality(String id, String quality) {
		boolean result = false;
		if (id != null) {
			Broadcast broadcast = broadcastMap.get(id);
			if (broadcast != null) {
				broadcast.setQuality(quality);
				broadcastMap.replace(id, broadcast);
				result = true;
			}
		}
		return result;
	}

	@Override

	public boolean updateSourceSpeed(String id, double speed) {
		boolean result = false;
		if (id != null) {
			Broadcast broadcast = broadcastMap.get(id);
			if (broadcast != null) {
				broadcast.setSpeed(speed);
				broadcastMap.replace(id, broadcast);
				result = true;
			}
		}
		return result;
	}
	public SocialEndpointCredentials addSocialEndpointCredentials(SocialEndpointCredentials credentials) {
		SocialEndpointCredentials addedCredential = null;
		if (credentials != null && credentials.getAccountName() != null && credentials.getAccessToken() != null
				 && credentials.getServiceName() != null) 
		{
			if (credentials.getId() == null) {
				//create new id if id is not set
				String id = RandomStringUtils.randomAlphanumeric(6);
				credentials.setId(id);
				socialEndpointCredentialsMap.put(id, credentials);
				addedCredential = credentials;
			}
			else {
				
				 if(socialEndpointCredentialsMap.get(credentials.getId()) != null) 
				 {
					 //replace the field if id exists
					socialEndpointCredentialsMap.put(credentials.getId(), credentials);
					addedCredential = credentials;
				 }
				 //if id is not matched with any value, do not record
			}
		}
		return addedCredential;
	}

	@Override
	public List<SocialEndpointCredentials> getSocialEndpoints(int offset, int size) 
	{
		Collection<SocialEndpointCredentials> values = socialEndpointCredentialsMap.values();
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<SocialEndpointCredentials> list = new ArrayList();
		for (SocialEndpointCredentials credential : values) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(credential);
			itemCount++;

			if (itemCount >= size) {
				break;
			}
		}
		return list;
	}

	@Override
	public boolean removeSocialEndpointCredentials(String id) {
		return socialEndpointCredentialsMap.remove(id) != null;
	}

	@Override
	public SocialEndpointCredentials getSocialEndpointCredentials(String id) {
		SocialEndpointCredentials credential = null;
		if (id != null) {
			credential = socialEndpointCredentialsMap.get(id);
		}
		return credential;
	}

}
