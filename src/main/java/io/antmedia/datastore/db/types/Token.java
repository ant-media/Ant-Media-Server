package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity("token")

@Indexes({ @Index(fields = @Field("tokenId")) })
@ApiModel(value="Token", description="The one-time token class")
public class Token {
	
	
	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the token")
	private ObjectId dbId;
	
	public static final String PUBLISH_TOKEN = "publish";
	public static final String PLAY_TOKEN = "play";
	
	/**
	 * random tokenID
	 */
	@ApiModelProperty(value = "the token id of the token")
	private String tokenId;
	
	/**
	 * related streamId with token
	 */
	@ApiModelProperty(value = "the stream id of the token")
	private String streamId;
	
	/**
	 * expiration date of the token
	 */
	@ApiModelProperty(value = "the expire date of the token")
	private long expireDate;
	
	/**
	 * type of the token, such as publish, play etc.
	 */
	@ApiModelProperty(value = "the type of the token")
	private String type;
	
	/**
	 * the id of the conference room which requested streams belongs to.
	 */
	@ApiModelProperty(value = "the id of the conference room which requested streams belongs to")
	private String roomId;
	
	
	public String getRoomId() {
		return roomId;
	}

	public void setRoomId(String roomName) {
		this.roomId = roomName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTokenId() {
		return tokenId;
	}

	public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public long getExpireDate() {
		return expireDate;
	}

	public void setExpireDate(long expireDate) {
		this.expireDate = expireDate;
	}
	

}
