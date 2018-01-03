package io.antmedia.rest;

import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.ipcamera.Application;
import io.antmedia.ipcamera.ArchivedVideo;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvifdiscovery.OnvifDiscovery;
import io.antmedia.ipcamera.utils.Camera;
import io.antmedia.ipcamera.utils.CameraStore;

@Component
@Path("/camera")
public class IPCameraRestService {

	public static class OperationResult {
		public boolean success = false;

		public OperationResult(boolean success) {
			this.success = success;
		}

		// use if required
		public int id = -1;

		public int errorId = -1;

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getErrorId() {
			return errorId;
		}

		public void setErrorId(int errorId) {
			this.errorId = errorId;
		}

	}

	@Context
	private ServletContext servletContext;

	private CameraStore cameraStore;

	private Application appInstance;
	protected static Logger logger = LoggerFactory.getLogger(Application.class);

	@GET
	@Path("/getList")
	@Produces(MediaType.APPLICATION_JSON)
	public Camera[] getCameraList() {
		return getCameraStore().getCameraList();
	}

	@GET
	@Path("/add")
	@Produces(MediaType.APPLICATION_JSON)
	public OperationResult addCamera(@QueryParam("name") String name, @QueryParam("ipAddr") String ipAddr,
			@QueryParam("username") String username, @QueryParam("password") String password) {
		boolean result = false;
		if (name != null && name.length() > 0) {

			if (ipAddr != null && ipAddr.length() > 0) {
				OnvifCamera onvif = new OnvifCamera();
				onvif.connect(ipAddr, username, password);
				String rtspURL = onvif.getRTSPStreamURI();

				if (rtspURL != "no") {

					String authparam = username + ":" + password + "@";
					String rtspURLWithAuth = "rtsp://" + authparam + rtspURL.substring("rtsp://".length());
					System.out.println("rtsp url with auth:" + rtspURLWithAuth);
					result = getCameraStore().addCamera(name, ipAddr, username, password, rtspURLWithAuth);
					if (result) {
						Camera cam = getCameraStore().getCamera(ipAddr);
						getApplicationInstance().startCameraStreaming(cam);
					}
					onvif.disconnect();
				}
			}
		}

		return new OperationResult(result);
	}

	@GET
	@Path("/deleteCamera")
	@Produces(MediaType.APPLICATION_JSON)
	public OperationResult deleteCamera(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		logger.warn("inside of rest service");
		Camera cam = getCameraStore().getCamera(ipAddr);
		getApplicationInstance().stopCameraStreaming(cam);
		result = getCameraStore().deleteCamera(ipAddr);

		return new OperationResult(result);
	}

	@GET
	@Path("/updateCamInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public OperationResult updateCamInfo(@QueryParam("name") String name, @QueryParam("ipAddr") String ipAddr,
			@QueryParam("username") String username, @QueryParam("password") String password,
			@QueryParam("rtspUrl") String rtspUrl) {
		boolean result = false;
		logger.warn("inside of rest service");
		Camera cam = getCameraStore().getCamera(ipAddr);
		getApplicationInstance().stopCameraStreaming(cam);

		result = getCameraStore().editCameraInfo(name, ipAddr, username, password, rtspUrl);

		if (result = true) {

			Camera newCam = getCameraStore().getCamera(ipAddr);
			getApplicationInstance().startCameraStreaming(newCam);

		}

		return new OperationResult(result);
	}

	@GET
	@Path("/searchOnvifDevices")
	@Produces(MediaType.APPLICATION_JSON)
	public String[] searchOnvifDevices() {

		String localIP = null;
		String[] list = null;
		Enumeration<NetworkInterface> interfaces = null;
		try {
			interfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			// handle error
		}

		if (interfaces != null) {
			while (interfaces.hasMoreElements()) {
				NetworkInterface i = interfaces.nextElement();
				Enumeration<InetAddress> addresses = i.getInetAddresses();
				while (addresses.hasMoreElements() && (localIP == null || localIP.isEmpty())) {
					InetAddress address = addresses.nextElement();
					if (!address.isLoopbackAddress() && address.isSiteLocalAddress()) {
						localIP = address.getHostAddress();
					}
				}
			}
			logger.warn("IP Address:  " + localIP);

		}

		if (localIP != null) {

			String[] ipAddrParts = localIP.split("\\.");

			String ipAd = ipAddrParts[0] + "." + ipAddrParts[1] + "." + ipAddrParts[2] + ".";

			System.out.println(ipAd);

			logger.warn("inside of auto discovery");

			ArrayList<String> addressList = new ArrayList<>();

			for (int i = 2; i < 255; i++) {
				addressList.add(ipAd + i);

			}

			List<URL> onvifDevices = OnvifDiscovery.discoverOnvifDevices(true, addressList);

			list = new String[onvifDevices.size()];

			if (onvifDevices.size() > 0) {

				for (int i = 0; i < onvifDevices.size(); i++) {

					logger.warn("inside of for loop" + i);
					logger.warn("inside of for loop" + onvifDevices.get(i).toString());

					list[i] = StringUtils.substringBetween(onvifDevices.get(i).toString(), "http://", "/");
				}
			}

		}

		else {

			logger.warn("IP Address is not found");
		}
		return list;
	}

	@GET
	@Path("/get")
	@Produces(MediaType.APPLICATION_JSON)
	public Camera getCamera(@QueryParam("ipAddr") String ipAddr) {
		Camera camera = getCameraStore().getCamera(ipAddr);
		if (camera == null) {
			camera = new Camera("null", "null", "null", "null", "null");
		}
		return camera;
	}

	@GET
	@Path("/moveUp")
	@Produces(MediaType.APPLICATION_JSON)
	public OperationResult moveUp(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveUp();
			result = true;
		}
		return new OperationResult(result);
	}

	@GET
	@Path("/moveDown")
	@Produces(MediaType.APPLICATION_JSON)
	public OperationResult moveDown(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveDown();
			result = true;
		}
		return new OperationResult(result);
	}

	@GET
	@Path("/moveLeft")
	@Produces(MediaType.APPLICATION_JSON)
	public OperationResult moveLeft(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveLeft();
			result = true;
		}
		return new OperationResult(result);
	}

	@GET
	@Path("/moveRight")
	@Produces(MediaType.APPLICATION_JSON)
	public OperationResult moveRight(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveRight();
			result = true;
		}
		return new OperationResult(result);
	}

	/*
	 * 
	 * 
	 * @GET
	 * 
	 * @Path("/getArchive")
	 * 
	 * @Produces(MediaType.APPLICATION_JSON) public ArchiveItem[] getArchive() {
	 * File folder = new File("webapps/IPCamera/streams"); ArchiveItem[]
	 * archiveItems = null;
	 * 
	 * if (folder.exists()) { String[] listOfFiles = folder.list(new
	 * FilenameFilter() { public boolean accept(File directory, String fileName)
	 * { return fileName.endsWith(".flv") || fileName.endsWith(".mp4");
	 * 
	 * } });
	 * 
	 * archiveItems = new ArchiveItem[listOfFiles.length];
	 * 
	 * for (int i = 0; i < listOfFiles.length; i++) { archiveItems[i] = new
	 * ArchiveItem(listOfFiles[i]); } }
	 * 
	 * return archiveItems;
	 * 
	 * }
	 * 
	 * 
	 * 
	 */

	@GET
	@Path("/getArchiveDate")
	@Produces(MediaType.APPLICATION_JSON)
	public ArchivedVideo[] getArchiveDate(@QueryParam("camName") String camName, @QueryParam("date") String date) {
		File folder = new File("webapps/IPCamera/streams");
		ArchivedVideo[] archiveItems = null;
		ArchivedVideo[] archiveItemsResult = null;
		int j = 0;

		if (folder.exists()) {
			File[] files = folder.listFiles((FileFilter) FileFileFilter.FILE);

			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			SimpleDateFormat sdfSelected = new SimpleDateFormat("dd.MM.yyyy");

			archiveItems = new ArchivedVideo[files.length];

			Arrays.sort(files, Comparator.comparingLong(File::lastModified));

			for (int i = 0; i < files.length; i++) {

				String dateModified = sdfSelected.format(files[i].lastModified());
				String cname = StringUtils.substringBefore(files[i].getName().toString(), "-");

				if (camName.equals(cname) && date.equals(dateModified)
						&& files[i].getName().toString().endsWith(".mp4")) {

					archiveItems[j] = new ArchivedVideo(cname, sdf.format(files[i].lastModified()), files[i].getName());
					j++;

				}
			}
			archiveItemsResult = new ArchivedVideo[j];

			for (int i = 0; i < archiveItemsResult.length; i++) {

				archiveItemsResult[i] = archiveItems[i];
			}

		}

		return archiveItemsResult;

	}

	public Application getApplicationInstance() {
		if (appInstance == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			appInstance = (Application) ctxt.getBean("web.handler1");
		}
		return appInstance;

	}

	public CameraStore getCameraStore() {
		if (cameraStore == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			cameraStore = (CameraStore) ctxt.getBean("cameraStore");
		}
		return cameraStore;
	}

	public void setCameraStore(CameraStore cameraStore) {
		this.cameraStore = cameraStore;
	}

}
