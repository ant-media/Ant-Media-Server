package io.antmedia.rest;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.red5.server.api.scope.IScope;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Vod;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvifdiscovery.OnvifDiscovery;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcherManager;

@Component
@Path("/streamSource")
public class StreamsSourceRestService {

	@Context
	private ServletContext servletContext;

	private MapDBStore dbStore;
	private ApplicationContext appCtx;

	private StreamFetcherManager app;
	private IScope scope;

	private AntMediaApplicationAdapter appInstance;

	protected static Logger logger = LoggerFactory.getLogger(StreamsSourceRestService.class);


	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/addStreamSource")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addStreamSource(Broadcast stream) {
		boolean result = false;
		String message = "";

		if (stream.getName() != null && stream.getName().length() > 0) {

			if (stream.getType().equals("ipCamera")) {

				OnvifCamera onvif = new OnvifCamera();
				onvif.connect(stream.getIpAddr(), stream.getUsername(), stream.getPassword());
				String rtspURL = onvif.getRTSPStreamURI();

				if (rtspURL != "no") {

					String authparam = stream.getUsername() + ":" + stream.getPassword() + "@";
					String rtspURLWithAuth = "rtsp://" + authparam + rtspURL.substring("rtsp://".length());
					System.out.println("rtsp url with auth:" + rtspURLWithAuth);
					stream.setStreamUrl(rtspURLWithAuth);
					Date currentDate = new Date();
					long unixTime = currentDate.getTime();

					stream.setDate(unixTime);
					stream.setStatus("created");

					String id = getStore().save(stream);

					if (id.length() > 0) {
						Broadcast newCam = getStore().get(stream.getStreamId());
						getInstance().startStreaming(newCam);
					}
					onvif.disconnect();
					result = true;
					message = "IP Camera successfully added";
				}

			}

			else if (stream.getType().equals("streamSource")) {

				Date currentDate = new Date();
				long unixTime = currentDate.getTime();

				stream.setDate(unixTime);
				stream.setStatus("created");

				String id = getStore().save(stream);

				if (id.length() > 0) {
					Broadcast newSource = getStore().get(stream.getStreamId());
					getInstance().startStreaming(newSource);
				}

				result = true;
				message = "StreamSource successfully added";

			}else {

				message = "No stream added";

			}
		}

		return new Result(result, message);
	}

	@GET
	@Path("/getUserVodList/{folderPath}")
	@Produces(MediaType.APPLICATION_JSON)
	public boolean  getUserVodList(@PathParam("folderPath") String folderPath) {
		boolean result = false;

		String appScopeName = ScopeUtils.findApplication(getScope()).getName();


		File directory = new File(
				String.format("%s/webapps/%s/%s", System.getProperty("red5.root"), appScopeName, folderPath));

		// if the directory does not exist, create it first
		if (!directory.exists()) {
			try {
				directory.mkdir();
			} catch (SecurityException se) {
				se.printStackTrace();
			}
		}

		result=getStore().fetchUserVodList(directory);

		return result;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/updateCamInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateCamInfo(Broadcast camera) {
		boolean result = false;
		OnvifCamera onvif = null;
		logger.warn("inside of rest service");

		logger.warn(camera.getStatus());

		if(camera.getStatus()!=null) {

			if (camera.getStatus().equals("broadcasting")) {
				getInstance().stopStreaming(camera);

				result = getStore().editCameraInfo(camera);

				logger.warn("after edit:" + result);

				onvif = new OnvifCamera();
				onvif.connect(camera.getIpAddr(), camera.getUsername(), camera.getPassword());
				String rtspURL = onvif.getRTSPStreamURI();

				logger.warn("camera starting point inside camera edit:  " + camera.getStreamId());

				if (result = true && rtspURL != "no") {

					String authparam = camera.getUsername() + ":" + camera.getPassword() + "@";
					String rtspURLWithAuth = "rtsp://" + authparam + rtspURL.substring("rtsp://".length());
					System.out.println("rtsp url with auth:" + rtspURLWithAuth);
					camera.setStreamUrl(rtspURLWithAuth);

					Broadcast newCam = getStore().get(camera.getStreamId());
					getInstance().startStreaming(newCam);

				}
			} else {

				logger.warn("camera couldnot started");
				camera.setStatus("finished");
				result = getStore().editCameraInfo(camera);
			}

		}else {result = getStore().editCameraInfo(camera);}
		

		logger.warn("final result:" + result);
		if (onvif != null) {
			onvif.disconnect();
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
	@Path("/moveUp")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveUp(@QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getInstance().getOnvifCamera(id);
		if (camera != null) {
			camera.MoveUp();
			result = true;
		}
		return new Result(result);
	}

	@GET
	@Path("/moveDown")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveDown(@QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getInstance().getOnvifCamera(id);
		if (camera != null) {
			camera.MoveDown();
			result = true;
		}
		return new Result(result);
	}

	@GET
	@Path("/moveLeft")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveLeft(@QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getInstance().getOnvifCamera(id);
		if (camera != null) {
			camera.MoveLeft();
			result = true;
		}
		return new Result(result);
	}

	@GET
	@Path("/moveRight")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveRight(@QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getInstance().getOnvifCamera(id);
		if (camera != null) {
			camera.MoveRight();
			result = true;
		}
		return new Result(result);
	}
	@Nullable
	private ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}

	public AntMediaApplicationAdapter getInstance() {
		if (appInstance == null) {
			appInstance = (AntMediaApplicationAdapter) getAppContext().getBean("web.handler");
		}
		return appInstance;
	}

	public IScope getScope() {
		if (scope == null) {
			scope = getInstance().getScope();
		}
		return scope;
	}

	public MapDBStore getStore() {
		if (dbStore == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			dbStore = (MapDBStore) ctxt.getBean("db.datastore");
		}
		return dbStore;
	}

	public void setCameraStore(MapDBStore cameraStore) {
		this.dbStore = cameraStore;
	}

}
