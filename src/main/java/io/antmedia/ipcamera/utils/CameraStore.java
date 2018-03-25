package io.antmedia.ipcamera.utils;

import org.mapdb.DB;
import org.mapdb.HTreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

public class CameraStore implements ICameraStore {

	public final static String CAMERA_STORAGE_FILE = "camera.db";
	public final static String CAMERA_STORAGE_MAP_NAME = "camera_map";

	private DB db;
	private HTreeMap<String, String> map;
	private Gson gson;
	protected static Logger logger = LoggerFactory.getLogger(CameraStore.class);

	public CameraStore() {
		// db =
		// DBMaker.fileDB(CAMERA_STORAGE_FILE).fileMmapEnableIfSupported().closeOnJvmShutdown().make();
		// map =
		// db.hashMap(CAMERA_STORAGE_MAP_NAME).keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING)
		// .counterEnable().createOrOpen();
		//
		// GsonBuilder builder = new GsonBuilder();
		// gson = builder.create();

	}

	public boolean addCamera(String name, String ipAddr, String username, String password, String rtspUrl) {
		boolean result = false;
		try {

			// IP Address is primary key
			Camera camera = new Camera(name, ipAddr, username, password, rtspUrl);
			map.put(ipAddr, gson.toJson(camera));
			db.commit();
			result = true;
		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	public boolean editCameraInfo(String name, String ipAddr, String username, String password, String rtspUrl) {
		boolean result = false;
		try {

			logger.warn("inside of editCameraInfo");
			Camera camera = new Camera(name, ipAddr, username, password, rtspUrl);
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
	public boolean deleteCamera(String ipAddr) {
		boolean result = false;
		try {

			if (map.containsKey(ipAddr)) {
				logger.warn("inside of deleteCamera");
				map.remove(ipAddr);
				db.commit();
				result = true;
			}

		} catch (Exception e) {
			result = false;
		}
		return result;
	}

	public Camera getCamera(String ip) {
		if (map.containsKey(ip)) {
			return gson.fromJson(map.get(ip), Camera.class);
		}
		return null;
	}

	public Camera[] getCameraList() {

		Object[] objectArray = map.getValues().toArray();

		Camera[] cameraArray = new Camera[objectArray.length];

		for (int i = 0; i < cameraArray.length; i++) {
			cameraArray[i] = gson.fromJson((String) objectArray[i], Camera.class);
		}
		return cameraArray;
	}

	public void close() {
		db.close();
	}

}
