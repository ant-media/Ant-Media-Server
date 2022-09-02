package io.antmedia.datastore.db.types;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.morphia.annotations.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.bson.types.ObjectId;

import java.io.Serializable;


@Entity("vodId")
@Indexes({ @Index(fields = @Field("streamId")), @Index(fields = @Field("vodId")) })
@ApiModel(value="VodId", description="Video-on-demand id - stream id object class")
public class VodId implements Serializable {

	private static final long serialVersionUID = 1L;


	@JsonIgnore
	@Id
	private ObjectId dbId;
	@ApiModelProperty(value = "the id of the VoD")
	private String vodId;

	@ApiModelProperty(value = "the stream id of the VoD")
	private String streamId;

	public VodId() {
		//default constructor is used to return not found vod in rest service
	}

	public VodId(String vodId, String streamId) {

		this.vodId = vodId;
		this.streamId = streamId;

	}

	public String getVodId() {
		return vodId;
	}

	public void setVodId(String vodId) {
		this.vodId = vodId;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

}