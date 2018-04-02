package io.antmedia.ipcamera;

import java.net.ConnectException;
import java.util.List;

import javax.xml.soap.SOAPException;

import org.onvif.ver10.schema.AutoFocusMode;
import org.onvif.ver10.schema.Date;
import org.onvif.ver10.schema.DateTime;
import org.onvif.ver10.schema.FocusConfiguration20;
import org.onvif.ver10.schema.ImagingSettings20;
import org.onvif.ver10.schema.Profile;
import org.onvif.ver10.schema.Time;

import io.antmedia.ipcamera.onvif.soap.OnvifDevice;
import io.antmedia.ipcamera.onvif.soap.devices.PtzDevices;

public class OnvifCamera implements IOnvifCamera {

	OnvifDevice nvt;
	PtzDevices ptzDevices;
	List<Profile> profiles;

	String profileToken;

	@Override
	public boolean connect(String address, String username, String password) {
		boolean result = false;
		try {
			nvt = new OnvifDevice(address, username, password);
			nvt.getDevices().getCapabilities().getDevice();
			nvt.getDevices().getServices(false);
			ptzDevices = nvt.getPtz();
			profiles = nvt.getDevices().getProfiles();
			profileToken = profiles.get(0).getToken();
			result = true;
		} catch (ConnectException e) {
			e.printStackTrace();
			result = false;
		} catch (SOAPException e) {
			e.printStackTrace();
			result = false;
		}
		return result;
	}

	@Override
	public void disconnect() {
		// nvt.getDevices().;
		// TODO Auto-generated method stub

	}

	@Override
	public String getRTSPStreamURI() {
		String PTSPURL = null;

		try {
			PTSPURL = nvt.getMedia().getRTSPStreamUri(profileToken);

		} catch (NullPointerException e) {
			PTSPURL = "no";
			e.printStackTrace();

		} catch (SOAPException e) {
			PTSPURL = "no";
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return PTSPURL;
	}

	@Override
	public String getTCPStreamURI() {
		String PTSPURL = null;

		try {
			PTSPURL = nvt.getMedia().getTCPStreamUri(profileToken);

		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SOAPException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return PTSPURL;
	}

	@Override
	public boolean MoveUp() {

		ptzDevices.relativeMove(profileToken, 0f, 0.1f, 0f);

		try {
			Thread.sleep(500);

			ptzDevices.stopMove(profileToken);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		return true;
	}

	@Override
	public boolean MoveDown() {

		ptzDevices.relativeMove(profileToken, 0f, -0.1f, 0f);
		try {
			Thread.sleep(500);

			ptzDevices.stopMove(profileToken);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

		return true;
	}

	@Override
	public boolean MoveRight() {
		ptzDevices.relativeMove(profileToken, 1f, 0f, 0f);

		try {
			Thread.sleep(500);

			ptzDevices.stopMove(profileToken);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

		return true;
	}

	@Override
	public boolean MoveLeft() {
		ptzDevices.relativeMove(profileToken, -1f, 0f, 0f);
		try {
			Thread.sleep(500);

			ptzDevices.stopMove(profileToken);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}

		return true;
	}

	@Override
	public boolean MoveStop() {
		ptzDevices.stopMove(profileToken);
		return true;
	}

	@Override
	public String getAlarms() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean enableDhcp() {
		// TODO Auto-generated method stub
		// .setDHCP
		// IPv4NetworkInterfaceSetConfiguration.java
		return false;
	}

	@Override
	public boolean disableDhcp(String ipaddress, String netmask, String gateway) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public java.util.Date getTime() {
		return nvt.getDate();
	}

	public boolean setDateTime(Date date, Time time) {

		DateTime dt = new DateTime();

		/*
		 * Date date = new Date();
		 * 
		 * date.setDay(1);
		 * 
		 * date.setMonth(1);
		 * 
		 * date.setYear(2005);
		 * 
		 * 
		 * Time time = new Time();
		 * 
		 * time.setHour(10);
		 * 
		 * time.setMinute(10);
		 * 
		 * time.setSecond(10);
		 */

		dt.setDate(date);

		dt.setTime(time);

		return true;
	}

	@Override
	public boolean setBrightness(float brightness) {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		image_set.setBrightness(brightness);
		return true;
	}

	@Override
	public float getBrightness() {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		return image_set.getBrightness();
	}

	@Override
	public boolean setSaturation(float saturation) {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		image_set.setColorSaturation(saturation);
		return true;
	}

	@Override
	public float getSaturation() {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		return image_set.getColorSaturation();
	}

	@Override
	public boolean setContrast(float contrast) {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		image_set.setContrast(contrast);
		return true;
	}

	@Override
	public float getContrast() {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		return image_set.getContrast();
	}

	@Override
	public boolean setSharpness(float sharpness) {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		image_set.setSharpness(sharpness);
		return true;
	}

	@Override
	public float getSharpness() {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		return image_set.getSharpness();
	}

	@Override
	public boolean setFocusMode(boolean auto) {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		FocusConfiguration20 focus = image_set.getFocus();
		AutoFocusMode foc_mode;
		if (auto == true)
			foc_mode = AutoFocusMode.fromValue("AUTO");
		else
			foc_mode = AutoFocusMode.fromValue("MANUAL");

		focus.setAutoFocusMode(foc_mode);

		return true;
	}

	@Override
	public boolean isFocusModeAuto() {
		// TODO Auto-generated method stub
		ImagingSettings20 image_set = nvt.getImaging().getImagingSettings(profileToken);
		FocusConfiguration20 focus = image_set.getFocus();
		if (focus.getAutoFocusMode().value() == "AUTO")
			return true;
		else
			return false;
	}

	@Override
	public boolean setDateTime(java.sql.Date date, java.sql.Time time) {
		// TODO Auto-generated method stub
		return false;
	}

}
