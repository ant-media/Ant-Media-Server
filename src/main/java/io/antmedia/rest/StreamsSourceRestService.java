package io.antmedia.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.antmedia.StreamIdValidator;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.model.Result;
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
	@Override
	public Result addStreamSource(@ApiParam(value = "stream", required = true) Broadcast stream, @QueryParam("socialNetworks") String socialEndpointIds) {
		if (stream == null) {
			return new Result(false, "Stream  object is null.");
		}
		else if (stream.getStreamId() != null && !stream.getStreamId().isEmpty()) 
		{
			// make sure stream id is not set on rest service
			Broadcast broadcastTmp = getDataStore().get(stream.getStreamId());
			if (broadcastTmp != null) 
			{
				return new Result(false, "Stream id is already being used.");
			}
			else if (!StreamIdValidator.isStreamIdValid(stream.getStreamId())) 
			{
				return new Result(false, "Stream id is not valid.", INVALID_STREAM_NAME_ERROR);
			}
		}
		return super.addStreamSource(stream, socialEndpointIds);
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
	@Override
	public Result getCameraError(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) {
		return super.getCameraError(id);
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
	@Override
	public Result startStreamSource(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) 
	{
		return super.startStreamSource(id);
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
	@Override
	public Result stopStreamSource(@ApiParam(value = "the id of the stream", required = true) @QueryParam("id") String id) 
	{
		return super.stopStreamSource(id);
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
	@Override
	public Result synchUserVodList() {
		return super.synchUserVodList();
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
		return super.updateStreamSource(broadcast.getStreamId(), broadcast, socialNetworksToPublish);
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
	@Override
	public String[] searchOnvifDevices() {
		return super.searchOnvifDevices();
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
	@Override
	public Result moveUp(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		return super.moveUp(id);
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
	@Override
	public Result moveDown(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		return super.moveDown(id);
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
	@Override
	public Result moveLeft(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		return super.moveLeft(id);
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
	@Override
	public Result moveRight(@ApiParam(value = "the id of the IP Camera", required = true) @QueryParam("id") String id) {
		return super.moveRight(id);
	}

}
