package io.antmedia.datastore.db.types;

import dev.morphia.annotations.Entity;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="Endpoint", description="The endpoint class, such as Facebook, Twitter or custom RTMP endpoints")
@Entity
public class Endpoint 
{

	/**
	 * Keeps track of the RTMP endpoint status if it is writing or not
	 * {@link IAntMediaStreamHandler#BROADCAST_STATUS_*}
	 */
	@ApiModelProperty(value = "Status of the RTMP muxer, possible values are started, finished, failed, broadcasting, {@link IAntMediaStreamHandler#BROADCAST_STATUS_*}")
	private String status;

	/**
	 * Service name like facebook, periscope, youtube or generic
	 * it should match the VideoServiceEndpoint names or it can be generic
	 */
	@ApiModelProperty(value = "the service name like facebook, periscope, youtube or generic")
	private String type;

	
	/**
	 * RTMP URL of the endpoint
	 */
	@ApiModelProperty(value = "the RTMP URL of the endpoint")
	private  String rtmpUrl;
	
	
	/**
	 * Endpoint service id, this field holds the id of the endpoint
	 */
	@ApiModelProperty(value = "the endpoint service id, this field holds the id of the endpoint")
	private String endpointServiceId;
	



	/**
	 * Default constructor used in BroadcastRestService.addEndpoint
	 */
	public Endpoint() {
		this.status = IAntMediaStreamHandler.BROADCAST_STATUS_CREATED;

	}
		
	public Endpoint(String rtmpUrl, String type, String endpointServiceId, String status) {
		this();
		this.status = status;
		this.rtmpUrl = rtmpUrl;
		this.type = type;
		this.endpointServiceId = endpointServiceId;
	}
	

	public String getRtmpUrl() {
		return rtmpUrl;
	}

	public void setRtmpUrl(String rtmpUrl) {
		this.rtmpUrl = rtmpUrl;
	}

	public String getEndpointServiceId() {
		return endpointServiceId;
	}

	public void setEndpointServiceId(String endpointServiceId) {
		this.endpointServiceId = endpointServiceId;
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