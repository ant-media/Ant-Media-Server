package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @deprecated Use subscriber class and rest methods,  it will be deleted in coming versions
 * @author mekya
 *
 */
@Deprecated(since = "2.7.0", forRemoval = true)
@Schema(description = "Stores the information for a WebRTC viewer")
@Entity(value = "WebRTCViewerInfo")
@Indexes({ @Index(fields = @Field("viewerId")) })
public class WebRTCViewerInfo {

	@JsonIgnore
	@Id
	private ObjectId dbId;

	@Schema(description = "The id of the viewer")
	private String viewerId;

	@Schema(description = "The stream id that viewer views")
	private String streamId;

	@Schema(description = "The IP address of the edge to which viewer is connected")
	private String edgeAddress;


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

}
