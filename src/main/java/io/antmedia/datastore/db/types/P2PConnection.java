package io.antmedia.datastore.db.types;

import java.io.Serializable;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity("P2PConnection")
@Indexes({ @Index(fields = @Field("streamId")) })
@Schema(description = "P2P Signalling Connection Info")
public class P2PConnection implements Serializable{
	private static final long serialVersionUID = 1L;

	@JsonIgnore
	@Id
    @Schema(description = "The db id of the p2p connection")
	private ObjectId dbId;
	
	/**
	 * streamId
	 */
    @Schema(description = "The streamId for the p2p connection")
	private String streamId;
	
	/**
	 * originNode
	 */
    @Schema(description = "The IP of the originNode to which caller is connected")
	private String originNode;
	
	public P2PConnection() {
		//for mongodb
	}
	
	public P2PConnection(String streamId, String originNode) {
		this.streamId = streamId;
		this.originNode = originNode;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public String getOriginNode() {
		return originNode;
	}

	public void setOriginNode(String originNode) {
		this.originNode = originNode;
	}
}
