package io.antmedia.ipcamera.utils;

public interface ICameraStore {
	
	
	boolean addCamera(String name, String ipAddr, String username, String password, String rtspUrl);
	boolean editCameraInfo(String name, String ipAddr, String username, String password, String rtspUrl);
	boolean deleteCamera(String ipAddr); 
	
	Camera getCamera(String ip);
	
	
	Camera[] getCameraList();
	
	void close();

}
