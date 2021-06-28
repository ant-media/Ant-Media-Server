package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity("P2PConnection")
@Indexes({ @Index(fields = @Field("streamId")) })
@ApiModel(value="P2PConnection", description="P2P Signalling Connection Info")
public class P2PConnection {
	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the p2p connection")
	private ObjectId dbId;
	
	/**
	 * streamId
	 */
	@ApiModelProperty(value = "the streamId for the p2p connection")
	private String streamId;
	
	/**
	 * originNode
	 */
	@ApiModelProperty(value = "the IP of the originNode to which caller is connected")
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
