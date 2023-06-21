package io.antmedia.datastore.db.types;

import java.io.Serializable;

import org.bson.types.ObjectId;
import dev.morphia.annotations.Embedded;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;

import io.antmedia.cluster.IStreamInfo;
import io.antmedia.webrtc.VideoCodec;

@Entity("StreamInfo")
@Indexes({@Index(fields = @Field("streamId"))})
public class StreamInfo implements IStreamInfo, Serializable {
	
	private static final long serialVersionUID = 1L;
	@Id
	private ObjectId dbId;
	private String streamId;
	private int height;
	private int width;
	private int videoBitrate;
	private int audioBitrate;
	private int videoRTimebase;
	private int audioRTimebase;
	private String host;
	private boolean videoEnabled;
	private boolean audioEnabled;
	private boolean dataChannelEnabled;
	private VideoCodec videoCodec;
	private String nodeGroup;
	private boolean isGlobalHost;
	private int originPort;


	
	public StreamInfo(boolean videoEnabled, int height, int width, int videobitrate, boolean audioEnabled, int audiobitrate, int videoRTimebase, int audioRTimebase, VideoCodec codec) {
		this.height = height;
		this.width = width;
		this.videoBitrate = videobitrate;
		this.audioBitrate = audiobitrate;
		this.videoRTimebase = videoRTimebase;
		this.audioRTimebase = audioRTimebase;
		this.videoEnabled = videoEnabled;
		this.audioEnabled = audioEnabled;
		this.videoCodec = codec;
	}
	
	public StreamInfo() {
		
	}
	
	@Override
	public int getVideoHeight() {
		return height;
	}

	@Override
	public int getVideoWidth() {
		return width;
	}

	@Override
	public int getVideoBitrate() {
		return videoBitrate;
	}

	@Override
	public int getAudioBitrate() {
		return audioBitrate;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setVideoBitrate(int videoBitrate) {
		this.videoBitrate = videoBitrate;
	}

	public void setAudioBitrate(int audioBitrate) {
		this.audioBitrate = audioBitrate;
	}

	public void setVideoRTimebase(int rtimebase) {
		this.videoRTimebase = rtimebase;
	}

	public void setAudioRTimebase(int rtimebase) {
		this.audioRTimebase = rtimebase;
	}
	
	public int getVideoRTimebase() {
		return videoRTimebase;
	}
	
	public int getAudioRTimebase() {
		return audioRTimebase;
	}
	
	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setVideoEnabled(boolean b) {
		this.videoEnabled = b;
	}

	public void setAudioEnabled(boolean b) {
		this.audioEnabled = b;
	}

	public boolean isVideoEnabled() {
		return this.videoEnabled;
	}

	public boolean isAudioEnabled() {
		return this.audioEnabled;
	}

	public void setDataChannelEnabled(boolean dataChannelEnabled) {
		this.dataChannelEnabled = dataChannelEnabled;
	}

	public boolean isDataChannelEnabled() {
		return dataChannelEnabled;
	}
	
	public void setVideoCodec(VideoCodec videoCodec) {
		this.videoCodec = videoCodec;
	}
	
	public VideoCodec getVideoCodec() {
		return videoCodec;
	}

	public String getNodeGroup() {
		return nodeGroup;
	}

	public void setNodeGroup(String nodeGroup) {
		this.nodeGroup = nodeGroup;
	}

	public boolean isGlobalHost() {
		return isGlobalHost;
	}

	public void setGlobalHost(boolean isGlobalHost) {
		this.isGlobalHost = isGlobalHost;
	}

	public int getOriginPort() {
		return originPort;
	}

	public void setOriginPort(int originPort) {
		this.originPort = originPort;
	}

}
