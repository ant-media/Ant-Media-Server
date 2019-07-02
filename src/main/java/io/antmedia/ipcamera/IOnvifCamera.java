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
	boolean moveUp();

	/**
	 * Stop IP Camera any movement
	 * @return
	 */
	boolean moveStop();

	/**
	 * Move down IP Camera
	 * @return
	 */
	boolean moveDown();

	/**
	 * Move right IP Camera
	 * @return
	 */
	boolean moveRight();

	/**
	 * Move left IP Camera
	 * @return
	 */
	boolean moveLeft();
	
	/**
	 * Zoom-in IP Camera
	 * @return
	 */
	boolean zoomIn();
	
	/**
	 * Zoom-out IP Camera
	 * @return
	 */
	boolean zoomOut();

	String getTCPStreamURI();


	
}
