package io.antmedia.ipcamera;



import java.sql.Date;
import java.sql.Time;



public interface IOnvifCamera {
	/**
	 * 
	 * @param address
	 * @param username
	 * @param password
	 * @return true if connected
	 * 		   false if not connected
	 */
	int connect(String address, String username, String password);
	
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
	 * Move up IP Camera
	 * @return
	 */
	boolean relativeMoveUp();

	/**
	 * Stop IP Camera any movement
	 * @return
	 */
	boolean moveStop();

	/**
	 * Move down IP Camera
	 * @return
	 */
	boolean relativeMoveDown();

	/**
	 * Move right IP Camera
	 * @return
	 */
	boolean relativeMoveRight();

	/**
	 * Move left IP Camera
	 * @return
	 */
	boolean relativeMoveLeft();
	
	/**
	 * Zoom-in IP Camera
	 * @return
	 */
	boolean relativeZoomIn();
	
	/**
	 * Zoom-out IP Camera
	 * @return
	 */
	boolean relativeZoomOut();

	String getTCPStreamURI();
	
	/**
	 * Move in X direction in positive or negative
	 * @param value
	 * @return true if successful, false if faied
	 */
	boolean moveX(float value);
	
	/**
	 * Move in Y direction in positive or negative
	 * @param value 
	 * @return true if successful, false if failed
	 */
	boolean moveY(float value);
	
	/**
	 * Zoom in and out according to the value
	 * @param value, if 0-1 zoom in, if -1-0 zoom out
	 * @return true if successful, false if failed
	 */
	boolean zoom(float value);


	
}
