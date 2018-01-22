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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.ipcamera.ArchivedVideo;
import io.antmedia.ipcamera.IPCameraApplicationAdapter;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvifdiscovery.OnvifDiscovery;
import io.antmedia.rest.BroadcastRestService.Result;

@Component
@Path("/camera")
public class IPCameraRestService {

	@Context
	private ServletContext servletContext;

	private MapDBStore cameraStore;
	private ApplicationContext appCtx;

	private IPCameraApplicationAdapter app;

	protected static Logger logger = LoggerFactory.getLogger(IPCameraApplicationAdapter.class);

	@GET
	@Path("/getList")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getCameraList() {
		return getCameraStore().getCameraList();
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/add")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addCamera(Broadcast camera) {
		boolean result = false;
		if (camera.getName() != null && camera.getName().length() > 0) {

			if (camera.getIpAddr() != null && camera.getIpAddr().length() > 0) {

				if (camera.getStreamId() == null) {

					OnvifCamera onvif = new OnvifCamera();
					onvif.connect(camera.getIpAddr(), camera.getUsername(), camera.getPassword());
					String rtspURL = onvif.getRTSPStreamURI();

					if (rtspURL != "no") {

						String authparam = camera.getUsername() + ":" + camera.getPassword() + "@";
						String rtspURLWithAuth = "rtsp://" + authparam + rtspURL.substring("rtsp://".length());
						System.out.println("rtsp url with auth:" + rtspURLWithAuth);
						camera.setRtspUrl(rtspURLWithAuth);
						result = getCameraStore().addCamera(camera);

						if (result) {
							Broadcast newCam = getCameraStore().getCamera(camera.getStreamId());
							getApplicationInstance().startCameraStreaming(newCam);
						}
						onvif.disconnect();
					}
				}
			}
		}

		return new Result(result);
	}

	/*
	 * This function is not used anymore, use BroadcastRestservice/delete
	 */
	@GET
	@Path("/deleteCamera")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteCamera(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		logger.warn("inside of rest service" + ipAddr);
		Broadcast cam = getCameraStore().getCamera(ipAddr);

		if (cam != null) {
			getApplicationInstance().stopCameraStreaming(cam);
			result = getCameraStore().deleteCamera(ipAddr);
		}
		return new Result(result);
	}

	@GET
	@Path("/updateCamInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateCamInfo(@QueryParam("name") String name, @QueryParam("ipAddr") String ipAddr,
			@QueryParam("username") String username, @QueryParam("password") String password,
			@QueryParam("rtspUrl") String rtspUrl) {
		boolean result = false;
		logger.warn("inside of rest service");
		Broadcast cam = getCameraStore().getCamera(ipAddr);
		getApplicationInstance().stopCameraStreaming(cam);

		result = getCameraStore().editCameraInfo(name, ipAddr, username, password, rtspUrl);

		if (result = true) {

			Broadcast newCam = getCameraStore().getCamera(ipAddr);
			getApplicationInstance().startCameraStreaming(newCam);

		}

		return new Result(result);
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
	public Broadcast getCamera(@QueryParam("ipAddr") String ipAddr) {
		Broadcast camera = getCameraStore().getCamera(ipAddr);
		if (camera == null) {
			camera = new Broadcast("null", "null", "null", "null", "null", "null");
		}
		return camera;
	}

	@GET
	@Path("/moveUp")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveUp(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveUp();
			result = true;
		}
		return new Result(result);
	}

	@GET
	@Path("/moveDown")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveDown(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveDown();
			result = true;
		}
		return new Result(result);
	}

	@GET
	@Path("/moveLeft")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveLeft(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveLeft();
			result = true;
		}
		return new Result(result);
	}

	@GET
	@Path("/moveRight")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveRight(@QueryParam("ipAddr") String ipAddr) {
		boolean result = false;
		OnvifCamera camera = getApplicationInstance().getOnvifCamera(ipAddr);
		if (camera != null) {
			camera.MoveRight();
			result = true;
		}
		return new Result(result);
	}

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

	private ApplicationContext getAppContext() {
		if (appCtx == null && servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}

	public IPCameraApplicationAdapter getApplicationInstance() {
		if (app == null) {
			app = (IPCameraApplicationAdapter) getAppContext().getBean("web.handler");
		}
		return app;
	}

	public MapDBStore getCameraStore() {
		if (cameraStore == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			cameraStore = (MapDBStore) ctxt.getBean("db.datastore");
		}
		return cameraStore;
	}

	public void setCameraStore(MapDBStore cameraStore) {
		this.cameraStore = cameraStore;
	}

}
