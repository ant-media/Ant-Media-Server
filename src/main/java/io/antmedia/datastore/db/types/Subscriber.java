package io.antmedia.datastore.db.types;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import io.swagger.v3.oas.annotations.media.Schema;


@Entity("subscriber")
@Indexes({ @Index(fields = @Field("subscriberId")), @Index(fields = @Field("streamId")) })
@Schema(description = "The time based token subscriber class. This keeps which subscriber can access to which stream and which TOTP")
public class Subscriber {
    
    @JsonIgnore
    public static final String PLAY_TYPE = "play";
    
    @JsonIgnore
    public static final String PUBLISH_TYPE = "publish";
    
    @JsonIgnore
    public static final String PUBLISH_AND_PLAY_TYPE = "publish_play";

    @JsonIgnore
    @Schema(hidden = true)
    @Id
    private ObjectId dbId;

    /**
     * The subscriber id of the subscriber
     */
    @Schema(description = "The subscriber id of the subscriber")
    private String subscriberId;

    /**
     * The stream id of the token
     */
    @Schema(description = "The stream id of the token")
    private String streamId;  

    /**
     * Stats for this subscriber
     */
    @Schema(description = "Stats for this subscriber")
    private SubscriberStats stats = new SubscriberStats();

    /**
     * Secret code of the Subscriber
     */
	@JsonProperty(access = Access.WRITE_ONLY)
    @Schema(description = "Secret code of the subscriber")
    private String b32Secret;

    /**
     * Type of subscriber (play or publish). Pay attention that 'publish' type can also play the streams for making easy to join video conferencing
     */
    @Schema(description = "Type of subscriber (play or publish). Pay attention that 'publish' type can also play the streams for making easy to join video conferencing")
    private String type;

    /**
     * Is subscriber connected
     * TODO: Write what the recommend way is to get this information? Let's write some comments when we deprecate something 
     * @mekya
     */
    @Deprecated(since = "2.4.3", forRemoval = true)
    @Schema(description = "Is subscriber connected")
    private boolean connected;

    /**
     * Count of subscriber usage
     */
    @Schema(description = "Count of subscriber usage")
    private int currentConcurrentConnections = 0;

    /**
     * Count of subscriber limit
     */
    @Schema(description = "Count of subscriber usage")
    private int concurrentConnectionsLimit = 1;

    /**
     * Type of block. It can be publish, play or publish_play in static field: {@link Subscriber#PLAY_TYPE},
     * {@link Subscriber#PUBLISH_TYPE}, {@link Subscriber#PUBLISH_AND_PLAY_TYPE}
     */
    private String blockedType;

    /**
     * If this is set, it means user is blocked until this time
     * This is unix timestamp in milliseconds
     */
    private long blockedUntilUnitTimeStampMs = 0;

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


	public String getRegisteredNodeIp() {
		return registeredNodeIp;
	}

	public void setRegisteredNodeIp(String registeredNodeIp) {
		this.registeredNodeIp = registeredNodeIp;
	}

	public boolean isBlocked(String type) 
	{
		long currTime = System.currentTimeMillis();
		
		return (type.equals(blockedType) || PUBLISH_AND_PLAY_TYPE.equals(blockedType))
				&& getBlockedUntilUnitTimeStampMs() != 0 && getBlockedUntilUnitTimeStampMs() >= currTime;
	}

	public String getBlockedType() {
		return blockedType;
	}

	public void setBlockedType(String blockedType) {
		this.blockedType = blockedType;
	}

	public long getBlockedUntilUnitTimeStampMs() {
		return blockedUntilUnitTimeStampMs;
	}

	public void setBlockedUntilUnitTimeStampMs(long blockedUntilUnitTimeStampMs) {
		this.blockedUntilUnitTimeStampMs = blockedUntilUnitTimeStampMs;
	}
}
