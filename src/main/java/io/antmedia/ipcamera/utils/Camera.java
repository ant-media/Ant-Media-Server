package io.antmedia.ipcamera.utils;

public class Camera {

	private String ipAddr;
	private String username;
	private String password;
	private String rtspUrl;

	public Camera(String name, String ipAddr, String username, String password, String rtspUrl) {
		// this.setName(name);
		this.ipAddr = ipAddr;
		this.username = username;
		this.password = password;
		this.rtspUrl = rtspUrl;
	}

	public String getIpAddr() {
		return ipAddr;
	}

	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRtspUrl() {
		return rtspUrl;
	}

	public void setRtspUrl(String rtspUrl) {
		this.rtspUrl = rtspUrl;
	}

}