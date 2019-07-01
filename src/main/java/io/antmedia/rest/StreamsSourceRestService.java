package io.antmedia.rest;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.IResourceMonitor;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.ipcamera.onvifdiscovery.OnvifDiscovery;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(value = "StreamsSourceRestService")
@Component
@Path("/streamSource")
public class StreamsSourceRestService extends RestServiceBase{

	private static final String HIGH_RESOURCE_USAGE = "current system resources not enough";

	private static final String HTTP = "http://";
	private static final String RTSP = "rtsp://";
	public static final int HIGH_RESOURCE_USAGE_ERROR = -3;
	public static final int FETCHER_NOT_STARTED_ERROR = -4;



	protected static Logger logger = LoggerFactory.getLogger(StreamsSourceRestService.class);


	/**
	 * Add IP Camera and Stream Sources to the system as broadcast
	 * 
	 * @param stream - broadcast object of IP Camera or Stream Source
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Add IP Camera and Stream Sources to the system as broadcast", notes = "Notes here", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/addStreamSource")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addStreamSource(@ApiParam(value = "stream", required = true) Broadcast stream, @QueryParam("socialNetworks") String socialEndpointIds) {

		Result result = new Result(false);

		IResourceMonitor monitor = (IResourceMonitor) getAppContext().getBean(IResourceMonitor.BEAN_NAME);

		boolean systemResult = monitor.enoughResource();

		if(!systemResult) {
			logger.error("Stream Fetcher can not be created due to not enough system resource for stream {} CPU load:{}"
					+ " CPU Limit:{} free RAM Limit:{}, free RAM available:{}", stream.getName(), monitor.getCpuLoad(), monitor.getCpuLimit(), monitor.getMinFreeRamSize(), monitor.getFreeRam());
			result.setMessage(HIGH_RESOURCE_USAGE);
			result.setErrorId(HIGH_RESOURCE_USAGE_ERROR);
		}
		else {

			if (stream.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)) {
				result = addIPCamera(stream, socialEndpointIds);
			}
			else if (stream.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) ) {
				result = addSource(stream, socialEndpointIds);
			}
		} 
		
		return result;
	}

	/**
	 * Get IP Camera Error after connection failure
	 * @param id - the id of the stream
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Get IP Camera Error after connection failure", notes = "Notes here", response = Result.class)
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/getCameraError")
	@Produces(MediaType.APPLICATION_JSON)
	public Result getCameraError(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) {
		Result result = new Result(true);

		for (StreamFetcher camScheduler : getApplication().getStreamFetcherManager().getStreamFetcherList()) {
			if (camScheduler.getStream().getIpAddr().equals(id)) {
				result = camScheduler.getCameraError();
			}
		}

		return result;
	}

	/**
	 * Start external sources (IP Cameras and Stream Sources) again if it is added and stopped before
	 * @param id - the id of the stream
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Start external sources (IP Cameras and Stream Sources) again if it is added and stopped before", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/startStreamSource")
	@Produces(MediaType.APPLICATION_JSON)
	public Result startStreamSource(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) 
	{
		Result result = new Result(false);	
		Broadcast broadcast = getDataStore().get(id);

		if (broadcast != null) 
		{
			if(broadcast.getStreamUrl() == null && broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)) 
			{
				//if streamURL is not defined before for IP Camera, connect to it again and define streamURL
				Result connResult = connectToCamera(broadcast);

				if (connResult.isSuccess()) 
				{
					String authparam = broadcast.getUsername() + ":" + broadcast.getPassword() + "@";
					String rtspURLWithAuth = RTSP + authparam + connResult.getMessage().substring(RTSP.length());
					logger.info("rtsp url with auth: {}", rtspURLWithAuth);
					broadcast.setStreamUrl(rtspURLWithAuth);
				}
			}

			if(getApplication().startStreaming(broadcast) != null) {

				result.setSuccess(true);
			}
		}
		return result;
	}

	/**
	 * Stop external sources (IP Cameras and Stream Sources)
	 * @param id - the id of the stream
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Stop external sources (IP Cameras and Stream Sources)", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/stopStreamSource")
	@Produces(MediaType.APPLICATION_JSON)
	public Result stopStreamSource(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) 
	{
		Result result = new Result(false);
		Broadcast broadcast = getDataStore().get(id);
		if(broadcast != null) {
			result = getApplication().stopStreaming(broadcast);
		}
		return result;
	}

	
	/**
	 * Synchronize User VoD Folder and add them to VoD database if any file exist and create symbolic links to that folder
	 * 
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Synchronize VoD Folder and add them to VoD database if any file exist and create symbolic links to that folder", notes = "Notes here", response = Result.class)
	@POST
	@Path("/synchUserVoDList")
	@Produces(MediaType.APPLICATION_JSON)
	public Result synchUserVodList() {
		boolean result = false;
		int errorId = -1;
		String message = "";

		String vodFolder = getApplication().getAppSettings().getVodFolder();

		logger.info("synch user vod list vod folder is {}", vodFolder);

		if (vodFolder != null && vodFolder.length() > 0) {

			result = getApplication().synchUserVoDFolder(null, vodFolder);
		}
		else {
			errorId = 404;
			message = "no VodD folder defined";
		}

		return new Result(result, message, errorId);
	}

	/**
	 * Update IP Camera and Stream Sources' Parameters
	 * 
	 * @param broadcast- object of IP Camera or Stream Source
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@ApiOperation(value = "Update IP Camera and Stream Sources' Parameters", notes = "Notes here", response = Result.class)
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/updateCamInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateCamInfo(@ApiParam(value = "object of IP Camera or Stream Source", required = true) Broadcast broadcast, @QueryParam("socialNetworks") String socialNetworksToPublish) {

		boolean result = false;
		logger.debug("update cam info for stream {}", broadcast.getStreamId());

		if( checkStreamUrl(broadcast.getStreamUrl()) && broadcast.getStatus()!=null){
			getApplication().stopStreaming(broadcast);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				Thread.currentThread().interrupt();
			}
			if(broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA)) {
				String rtspURL = connectToCamera(broadcast).getMessage();

				if (rtspURL != null) {

					String authparam = broadcast.getUsername() + ":" + broadcast.getPassword() + "@";
					String rtspURLWithAuth = RTSP + authparam + rtspURL.substring(RTSP.length());
					logger.info("new RTSP URL: {}" , rtspURLWithAuth);
					broadcast.setStreamUrl(rtspURLWithAuth);
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				logger.error(e.getMessage());
				Thread.currentThread().interrupt();
			}

			result = getDataStore().updateBroadcastFields(broadcast.getStreamId(), broadcast);
			Broadcast fetchedBroadcast = getDataStore().get(broadcast.getStreamId());
			getDataStore().removeAllEndpoints(fetchedBroadcast.getStreamId());

			if (socialNetworksToPublish != null && socialNetworksToPublish.length() > 0) {
				addSocialEndpoints(fetchedBroadcast, socialNetworksToPublish);
			}

			getApplication().startStreaming(broadcast);
		}
		return new Result(result);
	}


	/**
	 * Get Discovered ONVIF IP Cameras, this service perform a discovery inside of internal network and 
	 * get automatically  ONVIF enabled camera information.
	 * 
	 * @return - list of discovered ONVIF URLs
	 */
	@ApiOperation(value = "Get Discovered ONVIF IP Cameras, this service perform a discovery inside of internal network and get automatically  ONVIF enabled camera information", notes = "Notes here", response = Result.class)
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
			logger.info("IP Address: {} " , localIP);
		}

		if (localIP != null) {

			String[] ipAddrParts = localIP.split("\\.");

			String ipAd = ipAddrParts[0] + "." + ipAddrParts[1] + "." + ipAddrParts[2] + ".";
			ArrayList<String> addressList = new ArrayList<>();

			for (int i = 2; i < 255; i++) {
				addressList.add(ipAd + i);

			}

			List<URL> onvifDevices = OnvifDiscovery.discoverOnvifDevices(true, addressList);

			list = new String[onvifDevices.size()];

			if (!onvifDevices.isEmpty()) {

				for (int i = 0; i < onvifDevices.size(); i++) {

					list[i] = StringUtils.substringBetween(onvifDevices.get(i).toString(), HTTP, "/");
				}
			}

		}

		return list;
	}

	/**
	 * Move IP Camera Up
	 * @param id - the id of the IP Camera
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */

	@ApiOperation(value = "Move IP Camera Up", notes = "Notes here", response = Result.class)
	@GET
	@Path("/moveUp")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveUp(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		if (camera != null) {
			camera.moveUp();
			result = true;
		}
		return new Result(result);
	}

	/**
	 * Move IP Camera Down
	 * @param id - the id of the IP Camera
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Move IP Camera Down", notes = "Notes here", response = Result.class)
	@GET
	@Path("/moveDown")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveDown(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		if (camera != null) {
			camera.moveDown();
			result = true;
		}
		return new Result(result);
	}

	/**
	 * Move IP Camera Left
	 * @param id - the id of the IP Camera
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Move IP Camera Left", notes = "Notes here", response = Result.class)
	@GET
	@Path("/moveLeft")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveLeft(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		if (camera != null) {
			camera.moveLeft();
			result = true;
		}
		return new Result(result);
	}

	/**
	 * Move IP Camera Right
	 * @param id - the id of the IP Camera
	 * @return {@link io.antmedia.rest.BroadcastRestService.Result}
	 */
	@ApiOperation(value = "Move IP Camera Right", notes = "Notes here", response = Result.class)
	@GET
	@Path("/moveRight")
	@Produces(MediaType.APPLICATION_JSON)
	public Result moveRight(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		boolean result = false;
		OnvifCamera camera = getApplication().getOnvifCamera(id);
		if (camera != null) {
			camera.moveRight();
			result = true;
		}
		return new Result(result);
	}

}
