package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="ViewerInfo", description="Stores the info for the viewers")
@Entity(value = "ViewerInfo")
@Indexes({ @Index(fields = @Field("sessionId")) })
public class ViewerInfo {

	@JsonIgnore
	@Id
	private ObjectId dbId;

	/**
	 * Id of the viewer
	 */
	@ApiModelProperty(value = "The id of the viewer")
	private String viewerId;
	
	/**
	 * The session id of the viewer
	 */
	@ApiModelProperty(value = "The session id of the viewer")
	private String sessionId;

	/**
	 * Stream id that viewer views
	 */
	@ApiModelProperty(value = "Stream id that viewer views")
	private String streamId;
	
	/**
	 * IP address of the edge to which viewer is connected
	 */
	@ApiModelProperty(value = "IP address of the edge to which viewer is connected")
	private String edgeAddress;
	
	/**
	 * It shows viewer type
	 * It can be "webrtc", "hls" or "dash"
	 * Default value: "webrtc"
	 */
	@ApiModelProperty(value = "The type of viewer")
	private String viewerType="webrtc";
	
	/**
	 * the viewer start time of the stream
	 */
	@ApiModelProperty(value = "The viewer start time of the stream")
	private long startTime = 0;

	/**
	 * The viewer end time of the stream
	 */
	@ApiModelProperty(value = "The viewer end time of the stream")
	private long endTime = 0;

	public ObjectId getDbId() {
		return dbId;
	}

	public void setDbId(ObjectId dbId) {
		this.dbId = dbId;
	}

	public String getViewerId() {
		return viewerId;
	}

	public void setViewerId(String id) {
		this.viewerId = id;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public String getEdgeAddress() {
		return edgeAddress;
	}

	public void setEdgeAddress(String edgeAddress) {
		this.edgeAddress = edgeAddress;
	}
	
	public String getViewerType() {
		return viewerType;
	}

	public void setViewerType(String viewerType) {
		this.viewerType = viewerType;
	}
	
	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	
	public long getEndTime() {
		return endTime;
	}

	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
}
