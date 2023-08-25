package io.antmedia.ipcamera;



import java.sql.Date;
import java.sql.Time;



public interface IOnvifCamera {
	/**
	 * 
	 * @param address it can be direct ip address with port or it can start with http:// or https://
	 * @param username
	 * @param password
	 * @return true if connected
	 * 		   false if not connected
	 */
	int connect(String address, String username, String password);
	
	/**
	 * 
	 * @return profile list
	 */
	String[] getProfiles();

	
	void disconnect();
	
	String getRTSPStreamURI();
	
	
	
	String getAlarms();
	
	boolean enableDhcp();
	
	boolean disableDhcp(String ipaddress, String netmask, String gateway);
	
	java.util.Date getTime();

	boolean setDateTime(Date date, Time time);
	
	boolean setBrightness(float brightness);

	float getBrightness();

	boolean setSaturation(float saturation);

	float getSaturation();

	boolean setContrast(float contrast);

	float getContrast();

	boolean setSharpness(float sharpness);

	float getSharpness();

	/**
	 * 
	 * @param focusmode
	 * AUTO
	 * MANUAL
	 * @return
	 */
	boolean setFocusMode(boolean focusmode);

	boolean isFocusModeAuto();

	

	/**
	 * Stop IP Camera any movement
	 * @return
	 */
	boolean moveStop();

	/**
	 * Move camera continously
	 * @param x speed in pan
	 * @param y speed in tilt
	 * @param zoom 
	 * @return true if successful, false if failed
	 */
	boolean moveContinous(float x, float y, float zoom);
	
	/**
	 * Move camera relatively
	 * @param x
	 * @param y
	 * @param zoom
	 * @return
	 */
	boolean moveRelative(float x, float y, float zoom);
	
	/**
	 * Move camera absolutely in the x,y and zoom positions
	 * @param x
	 * @param y
	 * @param zoom
	 * @return
	 */
	boolean moveAbsolute(float x, float y, float zoom);
	


	String getTCPStreamURI();
		
}
