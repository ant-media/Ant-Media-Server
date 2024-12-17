package io.antmedia.datastore.db.types;


import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexes;
import dev.morphia.utils.IndexType;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Connection Event for the subscriber")
@Entity
@Indexes({ 
	@Index(fields = @Field("streamId")), 
	@Index(fields = @Field("subscriberId")) })
public class ConnectionEvent {
    @JsonIgnore
    public static final String CONNECTED_EVENT = "connected";
    @JsonIgnore
    public static final String DISCONNECTED_EVENT = "disconnected";

    /**
     * Timestamp of this event. Unix timestamp in milliseconds
     */
    @Schema(description = "the unix timestamp of the event in milliseconds")
    private long timestamp;
    /**
     * type of the event Connection/Disconnection
     */
    @Schema(description = "The type of the event. It can have connected or disconnected values")
    private String eventType;

    @Schema(description = "IP address of the instance that this event happened")
    private String instanceIP;

    @Schema(description = "Connection type. It can be publish or play")
    private String type;

    @Schema(description = "Event protocol. It can be webrtc, hls, dash")
    private String eventProtocol;
    
    @Schema(description = "Stream id of the event")
    private String streamId;
    
    @Schema(description = "Subscriber id of the event")
    private String subscriberId;
    
    @JsonIgnore
    @Schema(hidden = true)
    @Id
    private ObjectId dbId;

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getInstanceIP() {
        return instanceIP;
    }

    public void setInstanceIP(String instanceIP) {
        this.instanceIP = instanceIP;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEventProtocol() {
        return eventProtocol;
    }

    public void setEventProtocol(String eventProtocol) {
        this.eventProtocol = eventProtocol;
    }

	/**
	 * @return the subscriberId
	 */
	public String getSubscriberId() {
		return subscriberId;
	}

	/**
	 * @param subscriberId the subscriberId to set
	 */
	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	/**
	 * @return the streamId
	 */
	public String getStreamId() {
		return streamId;
	}

	/**
	 * @param streamId the streamId to set
	 */
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
}
