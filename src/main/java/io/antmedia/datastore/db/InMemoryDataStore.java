package io.antmedia.datastore.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.Vod;

public class InMemoryDataStore implements IDataStore {

	public LinkedHashMap<String, Broadcast> broadcastMap = new LinkedHashMap();

	public InMemoryDataStore(String dbName) {
	}

	@Override
	public String save(Broadcast broadcast) {

		String streamId = null;
		if (broadcast != null) {
			streamId = RandomStringUtils.randomNumeric(24);
			broadcast.setStreamId(streamId);
			broadcastMap.put(streamId, broadcast);

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
				for (Iterator iterator = endPointList.iterator(); iterator.hasNext();) {
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
		List<Broadcast> list = new ArrayList();
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
	public boolean addCamera(Broadcast camera) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean editCameraInfo(String name, String ipAddr, String username, String password, String rtspUrl) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean deleteCamera(String ipAddr) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Broadcast getCamera(String ip) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Broadcast> getCameraList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Broadcast> filterBroadcastList(int offset, int size, String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addVod(String id, Vod vod) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Vod> getVodList(int offset, int size) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Vod> filterVoDList(int offset, int size, String keyword, long startdate, long endDate) {
		// TODO Auto-generated method stub
		return null;
	}

}
