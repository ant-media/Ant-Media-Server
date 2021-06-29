package io.antmedia.datastore.db.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.IEndpointStatusListener;

@ApiModel(value="Endpoint", description="The endpoint class, such as Facebook, Twitter or custom RTMP endpoints")
public class Endpoint 
{

	/**
	 * Keeps track of the RTMP endpoint status if it is writing or not
	 */
	@ApiModelProperty(value = "Status of the RTMP muxer, possible values are, started, finished, failed")
	private String status;

	/**
	 * Service name like facebook, periscope, youtube or generic
	 * it should match the VideoServiceEndpoint names or it can be generic
	 */
	@ApiModelProperty(value = "the service name like facebook, periscope, youtube or generic")
	private String type;

	/**
	 * Broadcast id in the end point, Social endpoints has this field 
	 * but generic endpoint does not have
	 */
	@ApiModelProperty(value = "the id in the end point, Social endpoints has this field but generic endpoint does not have ")
	private  String broadcastId;
	
	/**
	 * Stream id in the endpoint if exists, it may be null
	 */
	@ApiModelProperty(value = "the id in the endpoint if exists, it may be null")
	private  String streamId;
	
	/**
	 * RTMP URL of the endpoint
	 */
	@ApiModelProperty(value = "the RTMP URL of the endpoint")
	private  String rtmpUrl;
	
	/**
	 * Name of the stream
	 */
	@ApiModelProperty(value = "the name of the stream")
	private  String name;
	
	/**
	 * Endpoint service id, this field holds the id of the endpoint
	 */
	@ApiModelProperty(value = "the endpoint service id, this field holds the id of the endpoint")
	private String endpointServiceId;
	
	/**
	 * Stream id in the server
	 */
	@ApiModelProperty(value = "the id of the stream in the server")
	private String serverStreamId;


	/**
	 * Default constructor used in BroadcastRestService.addEndpoint
	 */
	public Endpoint() {
		this.status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	}
		
	public Endpoint(String broadcastId, String streamId, String name, String rtmpUrl, String type, String endpointServiceId, String serverStreamId) {
		this();
		this.broadcastId = broadcastId;
		this.streamId = streamId;
		this.rtmpUrl = rtmpUrl;
		this.name = name;
		this.type = type;
		this.endpointServiceId = endpointServiceId;
		this.serverStreamId = serverStreamId;
	}
	
	public String getBroadcastId() {
		return broadcastId;
	}

	public void setBroadcastId(String broadcastId) {
		this.broadcastId = broadcastId;
	}
	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
	public String getRtmpUrl() {
		return rtmpUrl;
	}

	public void setRtmpUrl(String rtmpUrl) {
		this.rtmpUrl = rtmpUrl;
	}
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}
	public String getEndpointServiceId() {
		return endpointServiceId;
	}

	public void setEndpointServiceId(String endpointServiceId) {
		this.endpointServiceId = endpointServiceId;
	}

	public String getServerStreamId() {
		return serverStreamId;
	}

	public void setServerStreamId(String serverStreamId) {
		this.serverStreamId = serverStreamId;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}