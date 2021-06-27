package io.antmedia.datastore.db.types;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Embedded;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@Entity("subscriber")
@Indexes({ @Index(fields = @Field("subscriberId")), @Index(fields = @Field("streamId")) })
@ApiModel(value="Subscriber", description="The time based token subscriber class")
public class Subscriber {
	@JsonIgnore
	public static final String PLAY_TYPE = "play";
	@JsonIgnore
	public static final String PUBLISH_TYPE = "publish";

	@JsonIgnore
	@Id
	@ApiModelProperty(value = "the db id of the subscriber")
	private ObjectId dbId;
	
	/**
	 * random subscriber id
	 */
	@ApiModelProperty(value = "the subscriber id of the subscriber")
	private String subscriberId;
	
	@JsonIgnore
	/**
	 * related streamId with subscriber
	 */
	@ApiModelProperty(value = "the stream id of the token")
	private String streamId;	
	
	/**
	 * statistics for this subscriber
	 */
	@ApiModelProperty(value = "stats for this subscriber")
	@Embedded
	private SubscriberStats stats = new SubscriberStats();
	
	@JsonProperty(access = Access.WRITE_ONLY)
	/**
	 * secret code of the Subscriber
	 */
	@ApiModelProperty(value = "secret code of the subscriber")
	private String b32Secret;
	
	/**
	 * is subscriber connected
	 */
	@ApiModelProperty(value = "is subscriber connected")
	private boolean connected;
	
	/**
	 * type of the subscriber (default is play)
	 */
	@ApiModelProperty(value = " type of subscriber (play or publish)")
	private String type = PLAY_TYPE;
	
	public String getSubscriberId() {
		return subscriberId;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}
	
	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}	

	public String getB32Secret() {
		return b32Secret;
	}

	public void setB32Secret(String b32Secret) {
		this.b32Secret = b32Secret;
	}

	public SubscriberStats getStats() {
		return stats;
	}

	@JsonIgnore
	public void setStats(SubscriberStats stats) {
		if(stats != null)  {
			stats.setStreamId(streamId);
			stats.setSubscriberId(subscriberId);
		}
		this.stats = stats;
	}
	
	// database key of a subscriber consists both the stream id and subscriber id
	@JsonIgnore
	public String getSubscriberKey() {
		return getDBKey(streamId, subscriberId);
		
	}
	
	public static String getDBKey(String streamId, String subscriberId) {
		return streamId + "-" +subscriberId;
	}

	public boolean isConnected() {
		return connected;
	}

	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		// the type can be only play or publish
		if(PLAY_TYPE.equals(type) || PUBLISH_TYPE.equals(type)) {
			this.type = type;
		}
	}
}
