package io.antmedia.datastore.db.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.morphia.annotations.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.bson.types.ObjectId;

import java.io.Serializable;


@Entity("voDIdStreamIdPair")
@Indexes({ @Index(fields = @Field("streamId")), @Index(fields = @Field("voDId")) })
@ApiModel(value="VoDIdStreamIdPair", description="Video-on-demand id - stream id pair object class")
public class VoDIdStreamIdPair implements Serializable {

	private static final long serialVersionUID = 1L;


	@JsonIgnore
	@Id
	private ObjectId dbId;

	@ApiModelProperty(value = "the stream id of the VoD")
	private String streamId;

	@ApiModelProperty(value = "the id of the VoD")
	private String voDId;

	public VoDIdStreamIdPair() {
		//default constructor is used to return not found vod in rest service
	}

	public VoDIdStreamIdPair(String streamId, String voDId) {

		this.streamId = streamId;
		this.voDId = voDId;

	}

	public String getVoDId() {
		return voDId;
	}

	public void setVoDId(String voDId) {
		this.voDId = voDId;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

}