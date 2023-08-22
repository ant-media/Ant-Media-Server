package io.antmedia.datastore.db.types;
import io.antmedia.datastore.db.DataStore;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
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
	private SubscriberStats stats = new SubscriberStats();
	
	@JsonProperty(access = Access.WRITE_ONLY)
	/**
	 * secret code of the Subscriber
	 */
	@ApiModelProperty(value = "secret code of the subscriber")
	private String b32Secret;
	
	/**
	 * type of the subscriber (default is play)
	 */
	@ApiModelProperty(value = " type of subscriber (play or publish)")
	private String type = PLAY_TYPE;
	
	/**
	 * is subscriber connected
	 * TODO: Write what the recommend way is to get this information? Let's write some comments when we deprecate something 
	 * @mekya
	 */
	@Deprecated(since="2.4.3", forRemoval=true)
	@ApiModelProperty(value = "is subscriber connected")
	private boolean connected;
	
	/**
	 * count of subscriber usage
	 */
	@ApiModelProperty(value = " count of subscriber usage")
	private int currentConcurrentConnections = 0;

	/**
	 * count of subscriber limit
	 */
	@ApiModelProperty(value = " count of subscriber usage")
	private int concurrentConnectionsLimit = 1;

	private boolean playBlocked = false;

	private boolean publishBlocked = false;

	private long playBlockTime = 0;

	private long publishBlockTime = 0;

	private long playBlockedUntilTime = 0;

	private long publishBlockedUntilTime = 0;

	private String registeredNodeIp;

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

	public String getType() {
		return type;
	}

	public void setType(String type) {
		// the type can be only play or publish
		if(PLAY_TYPE.equals(type) || PUBLISH_TYPE.equals(type)) {
			this.type = type;
		}
	}
	
	@Deprecated(since="2.4.3", forRemoval=true)
	public boolean isConnected() {
		return connected;
	}
	
	@Deprecated(since="2.4.3", forRemoval=true)
	public void setConnected(boolean connected) {
		this.connected = connected;
	}
	
	public int getCurrentConcurrentConnections() {
		return currentConcurrentConnections;
	}

	public void setCurrentConcurrentConnections(int currentConcurrentConnections) {
		this.currentConcurrentConnections = currentConcurrentConnections;
	}
	
	public int getConcurrentConnectionsLimit() {
		return concurrentConnectionsLimit;
	}

	public void setConcurrentConnectionsLimit(int concurrentConnectionsLimit) {
		this.concurrentConnectionsLimit = concurrentConnectionsLimit;
	}

	public boolean isPlayBlocked() {
		return playBlocked;
	}

	public void setPlayBlocked(boolean playBlocked) {
		this.playBlocked = playBlocked;
	}

	public boolean isPublishBlocked() {
		return publishBlocked;
	}

	public void setPublishBlocked(boolean publishBlocked) {
		this.publishBlocked = publishBlocked;
	}

	public long getPlayBlockTime() {
		return playBlockTime;
	}

	public void setPlayBlockTime(long playBlockTime) {
		this.playBlockTime = playBlockTime;
	}

	public long getPublishBlockTime() {
		return publishBlockTime;
	}

	public void setPublishBlockTime(long publishBlockTime) {
		this.publishBlockTime = publishBlockTime;
	}

	public long getPlayBlockedUntilTime() {
		return playBlockedUntilTime;
	}

	public void setPlayBlockedUntilTime(long playBlockedUntilTime) {
		this.playBlockedUntilTime = playBlockedUntilTime;
	}

	public long getPublishBlockedUntilTime() {
		return publishBlockedUntilTime;
	}

	public void setPublishBlockedUntilTime(long publishBlockedUntil) {
		this.publishBlockedUntilTime = publishBlockedUntil;
	}

	public String getRegisteredNodeIp() {
		return registeredNodeIp;
	}

	public void setRegisteredNodeIp(String registeredNodeIp) {
		this.registeredNodeIp = registeredNodeIp;
	}

	public boolean isBlocked(String type) {
			long currTime = System.currentTimeMillis();
			if (type.equals(PLAY_TYPE) && isPlayBlocked()) {
				long subscriberPlayBlockTime = getPlayBlockTime();
				long subscriberPlayBlockedUntilTime = getPlayBlockedUntilTime();
				return (currTime > subscriberPlayBlockTime && currTime < subscriberPlayBlockedUntilTime);
			} else if (type.equals(PUBLISH_TYPE) && isPublishBlocked()) {
				long subscriberPublishBlockTime = getPublishBlockTime();
				long subscriberPublishBlockedUntilTime = getPublishBlockedUntilTime();
				return (currTime > subscriberPublishBlockTime && currTime < subscriberPublishBlockedUntilTime);
			}

		return false;
	}
}
