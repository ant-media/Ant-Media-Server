package io.antmedia.datastore.db.types;

import java.util.ArrayList;
import java.util.List;

import dev.morphia.annotations.Entity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Use Connection Event and Subscriber class instead of this class. No need to use this class anymore
 */
@Schema(description="Statistics for each subsciber to the stream")
@Entity
@Deprecated
public class SubscriberStats {
	
	/**
     * The subscriber id to which this statistic belongs to.
     */
    @Schema(description = "The subscriber id of the subscriber")
    private String subscriberId;

    /**
     * The related streamId with the subscriber.
     */
    @Schema(description = "The stream id of the token")
    private String streamId;

    /**
     * The connection events happened for this subscriber. Use ConnectionEvent class instead of this field
     */
    @Schema(description = "List of connection events")
    @Deprecated(since = "2.12.0", forRemoval = true)
    private List<ConnectionEvent> connectionEvents = new ArrayList<>();

    /**
     * The average video bitrate for a subscriber.
     */
    @Schema(description = "Average video bitrate for a subscriber")
    private long avgVideoBitrate;

    /**
     * The average audio bitrate for a subscriber.
     */
    @Schema(description = "Average audio bitrate for a subscriber")
    private long avgAudioBitrate;


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

	public List<ConnectionEvent> getConnectionEvents() {
		return connectionEvents;
	}

	public void setConnectionEvents(List<ConnectionEvent> connectionEvents) {
		this.connectionEvents = connectionEvents;
	}
	
	public void addConnectionEvent(ConnectionEvent connectionEvent) {
		connectionEvents.add(connectionEvent);
	}

	public void setAvgVideoBitrate(long avgVideoBitrate) {
		this.avgVideoBitrate = avgVideoBitrate;
	}
	
	public long getAvgVideoBitrate() {
		return avgVideoBitrate;
	}

	public void setAvgAudioBitrate(long avgAudioBitrate) {
		this.avgAudioBitrate = avgAudioBitrate;
	}
	
	public long getAvgAudioBitrate() {
		return avgAudioBitrate;
	}
	
}
