package io.antmedia.datastore.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;

public class MapDBStore implements IDataStore {

	private DB db;
	private HTreeMap<String, String> map;
	private Gson gson;
	protected static Logger logger = LoggerFactory.getLogger(MapDBStore.class);
	private static final String MAP_NAME = "broadcast";

	public MapDBStore(String dbName) {

		db = DBMaker.fileDB(dbName).transactionEnable().make();
		map = db.hashMap(MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).counterEnable()
				.createOrOpen();

		GsonBuilder builder = new GsonBuilder();
		gson = builder.create();

	}

	@Override
	public String save(Broadcast broadcast) {
		String streamId = null;
		if (broadcast != null) {
			try {
				streamId = RandomStringUtils.randomNumeric(24);
				broadcast.setStreamId(streamId);

				map.put(streamId, gson.toJson(broadcast));
				db.commit();
			} catch (Exception e) {
				e.printStackTrace();
				streamId = null;
			}
		}

		return streamId;
	}

	@Override
	public Broadcast get(String id) {
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				return gson.fromJson(jsonString, Broadcast.class);
			}
		}
		return null;
	}

	@Override
	public boolean updateName(String id, String name, String description) {
		boolean result = false;
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				broadcast.setName(name);
				broadcast.setDescription(description);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean updateStatus(String id, String status) {
		boolean result = false;
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				broadcast.setStatus(status);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean updateDuration(String id, long duration) {
		boolean result = false;
		if (id != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				broadcast.setDuration(duration);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean updatePublish(String id, boolean publish) {
		String jsonString = map.get(id);
		boolean result = false;
		if (jsonString != null) {
			Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
			broadcast.setPublish(publish);
			map.replace(id, gson.toJson(broadcast));
			db.commit();
			result = true;
		}
		return result;
	}

	@Override
	public boolean addEndpoint(String id, Endpoint endpoint) {
		boolean result = false;
		if (id != null && endpoint != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				List<Endpoint> endPointList = broadcast.getEndPointList();
				if (endPointList == null) {
					endPointList = new ArrayList();
				}
				endPointList.add(endpoint);
				broadcast.setEndPointList(endPointList);
				map.replace(id, gson.toJson(broadcast));
				db.commit();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean removeEndpoint(String id, Endpoint endpoint) {
		boolean result = false;

		if (id != null && endpoint != null) {
			String jsonString = map.get(id);
			if (jsonString != null) {
				Broadcast broadcast = gson.fromJson(jsonString, Broadcast.class);
				List<Endpoint> endPointList = broadcast.getEndPointList();
				if (endPointList != null) {
					for (Iterator iterator = endPointList.iterator(); iterator.hasNext();) {
						Endpoint endpointItem = (Endpoint) iterator.next();
						if (endpointItem.rtmpUrl.equals(endpoint.rtmpUrl)) {
							iterator.remove();
							result = true;
							break;
						}
					}

					if (result) {
						broadcast.setEndPointList(endPointList);
						map.replace(id, gson.toJson(broadcast));
						db.commit();
					}
				}
			}
		}
		return result;
	}

	@Override
	public long getBroadcastCount() {
		return map.getSize();
	}

	@Override
	public boolean delete(String id) {

		boolean result = map.remove(id) != null;
		if (result) {
			db.commit();
		}

		return result;
	}

	@Override
	public List<Broadcast> getBroadcastList(int offset, int size) {
		Collection<String> values = map.values();
		int t = 0;
		int itemCount = 0;
		if (size > 50) {
			size = 50;
		}
		if (offset < 0) {
			offset = 0;
		}
		List<Broadcast> list = new ArrayList();
		for (String broadcastString : values) {
			if (t < offset) {
				t++;
				continue;
			}
			list.add(gson.fromJson(broadcastString, Broadcast.class));
			itemCount++;

			if (itemCount >= size) {
				break;
			}

		}
		return list;
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

		Object[] objectArray = map.getValues().toArray();

		Broadcast[] broadcastArray = new Broadcast[objectArray.length];

		List<Broadcast> filterList = new ArrayList<Broadcast>();

		for (int i = 0; i < objectArray.length; i++) {

			broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);

		}

		for (int i = 0; i < broadcastArray.length; i++) {

			if (broadcastArray[i].getType().equals(type)) {

				filterList.add(gson.fromJson((String) objectArray[i], Broadcast.class));

			}

		}

		List<Broadcast> list = new ArrayList();
		for (Broadcast broadcast : filterList) {
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

	/*
	 * IP Camera Operations
	 */

	@Override
	public boolean addCamera(Broadcast camera) {
		boolean result = false;
		String streamId = null;

		// StreamID Address is primary key

		if (camera != null) {
			try {
				streamId = RandomStringUtils.randomNumeric(24);
				camera.setStreamId(streamId);

				map.put(streamId, gson.toJson(camera));
				db.commit();
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
				streamId = null;
			}
		}

		return result;
	}

	@Override
	public boolean editCameraInfo(String name, String ipAddr, String username, String password, String rtspUrl) {
		boolean result = false;
		try {

			logger.warn("inside of editCameraInfo");
			Broadcast camera = new Broadcast(name, ipAddr, username, password, rtspUrl, "ipCamera");
			map.replace(ipAddr, gson.toJson(camera));
			db.commit();
			result = true;
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	/**
	 * Delete camera from camera store
	 * 
	 * @returns true if camera exists, otherwise return false
	 */
	@Override
	public boolean deleteCamera(String id) {
		boolean result = false;
		try {

			if (map.containsKey(id)) {
				logger.warn("inside of deleteCamera");
				map.remove(id);
				db.commit();
				result = true;
			}

		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	@Override
	public Broadcast getCamera(String streamId) {
		if (map.containsKey(streamId)) {
			return gson.fromJson(map.get(streamId), Broadcast.class);
		}
		return null;
	}

	@Override
	public List<Broadcast> getCameraList() {

		Object[] objectArray = map.getValues().toArray();

		Broadcast[] broadcastArray = new Broadcast[objectArray.length];

		List<Broadcast> cameraList = new ArrayList<Broadcast>();

		for (int i = 0; i < objectArray.length; i++) {

			broadcastArray[i] = gson.fromJson((String) objectArray[i], Broadcast.class);

		}

		for (int i = 0; i < broadcastArray.length; i++) {

			if (broadcastArray[i].getType().equals("ipCamera")) {

				cameraList.add(gson.fromJson((String) objectArray[i], Broadcast.class));

			}

		}

		return cameraList;
	}

	@Override
	public void close() {
		db.close();
	}

}
