package io.antmedia.datastore.db.types;

import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity("token")

@Indexes({ @Index(fields = @Field("tokenId")) })
@Schema(description="The one-time token class")
public class Token {
	
	
	public static final String PUBLISH_TOKEN = "publish";
	public static final String PLAY_TOKEN = "play";
	
	 /**
     * The db id of the token.
     */
    @Schema(description = "The db id of the token")
    @Id
    private ObjectId dbId;

    /**
     * The token id.
     */
    @Schema(description = "The token id")
    private String tokenId;

    /**
     * The stream id associated with the token.
     */
    @Schema(description = "The stream id associated with the token")
    private String streamId;

    /**
     * The expiration date of the token.
     */
    @Schema(description = "The expiration date of the token")
    private long expireDate;

    /**
     * The type of the token, such as publish, play, etc.
     */
    @Schema(description = "The type of the token")
    private String type;

    /**
     * The id of the conference room which requested streams belong to.
     */
    @Schema(description = "The id of the conference room which requested streams belong to")
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
