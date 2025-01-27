package io.antmedia.datastore.db.types;


import java.util.List;

import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.types.Broadcast.HLSParameters;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * This is like a clone of Broadcast object. Just some differences, all default values are null in this object
 * and it is used to update the broadcast object in the datastore by only changing fields. 
 * 
 * If it a field is not null, it means that it should be updated in the datastore
 * 
 * It's not a good solution. It's a workaround so far that keeps the backward compatibility
 */
@Schema(description="This is the BroadcastUpdate and it's almost same with BroadcastUpdate with all default values are null. We update the fields in update method if it's not null."
		+ "It makes this data up to date in race conditions.")
public class BroadcastUpdate {

    private String streamId = null;

    private String status = null;

    private String playListStatus = null;

    private String type = null;

    private String publishType = null;

    private String name = null;

    private String description = null;

    private Boolean publish = null;

    private Long date = null;

    private Long plannedStartDate = null;

    private Long plannedEndDate = null;

    private Long duration = null;

    private List<Endpoint> endPointList = null;

    private List<PlayListItem> playListItemList = null;

    private Boolean publicStream = null;

    private Boolean is360 = null;

    private String listenerHookURL = null;

    private String category = null;

    private String ipAddr = null;

    private String username = null;

    private String password = null;

    private String quality = null;

    private Double speed = null;

    private String streamUrl = null;

    private String originAdress = null;

    private Integer mp4Enabled = null;

    private Integer webMEnabled = null;

    private Integer seekTimeInMs = null;

    @Deprecated(forRemoval = true, since = "2.9.1")
    private String conferenceMode = null;

    private Integer subtracksLimit = null;

    private Integer expireDurationMS = null;

    private String rtmpURL = null;

    private Boolean zombi = null;

    private Integer pendingPacketSize = null;

    private Integer hlsViewerCount = null;

    private Integer dashViewerCount = null;

    private Integer webRTCViewerCount = null;

    private Integer rtmpViewerCount = null;

    private Long startTime = null;

    private Long receivedBytes = null;
    
	private Integer width = null;
	
	private Integer height = null;
	
	private Integer encoderQueueSize = null;
	
	private Integer dropPacketCountInIngestion = null;

	private Integer dropFrameCountInEncoding = null;
	
	private Double packetLostRatio = null;
	
	private Integer packetsLost = null;
	
	private String remoteIp = null;
	
	private Integer jitterMs = null;
	
	private Integer rttMs = null;
    
    private Long bitrate = null;

    private String userAgent = null;

    private String latitude = null;

    private String longitude = null;

    private String altitude = null;

    private String mainTrackStreamId = null;

    @Deprecated(forRemoval = true, since = "2.10.1")
    private List<String> subTrackStreamIds = null;

    private Long absoluteStartTimeMs = null;

    private Integer webRTCViewerLimit = null;

    private Integer hlsViewerLimit = null;

    private Integer dashViewerLimit = null;

    private String subFolder = null;

    private Integer currentPlayIndex = null;

    private String metaData = null;

    private Boolean playlistLoopEnabled = null;

    private Long updateTime = null;

    private String role = null;

    private HLSParameters hlsParameters = null;

    private Boolean autoStartStopEnabled = null;

    private List<EncoderSettings> encoderSettingsList = null;

	public String getStreamId() {
		return streamId;
	}

	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}

	public String getStatus() {
		return status;
	}

	/**
	 * Pay attention to the status field. Even if you set to BROADCASTING, it will check last update time to understand if it's really broadcasting
	 * Check the {@link Broadcast#getStatus()}
	 * @param status
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	public String getPlayListStatus() {
		return playListStatus;
	}

	public void setPlayListStatus(String playListStatus) {
		this.playListStatus = playListStatus;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPublishType() {
		return publishType;
	}

	public void setPublishType(String publishType) {
		this.publishType = publishType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Boolean getPublish() {
		return publish;
	}

	public void setPublish(Boolean publish) {
		this.publish = publish;
	}

	public Long getDate() {
		return date;
	}

	public void setDate(Long date) {
		this.date = date;
	}

	public Long getPlannedStartDate() {
		return plannedStartDate;
	}

	public void setPlannedStartDate(Long plannedStartDate) {
		this.plannedStartDate = plannedStartDate;
	}

	public Long getPlannedEndDate() {
		return plannedEndDate;
	}

	public void setPlannedEndDate(Long plannedEndDate) {
		this.plannedEndDate = plannedEndDate;
	}

	public Long getDuration() {
		return duration;
	}

	public void setDuration(Long duration) {
		this.duration = duration;
	}

	public List<Endpoint> getEndPointList() {
		return endPointList;
	}

	public void setEndPointList(List<Endpoint> endPointList) {
		this.endPointList = endPointList;
	}

	public List<PlayListItem> getPlayListItemList() {
		return playListItemList;
	}

	public void setPlayListItemList(List<PlayListItem> playListItemList) {
		this.playListItemList = playListItemList;
	}

	public Boolean getPublicStream() {
		return publicStream;
	}

	public void setPublicStream(Boolean publicStream) {
		this.publicStream = publicStream;
	}

	public Boolean getIs360() {
		return is360;
	}

	public void setIs360(Boolean is360) {
		this.is360 = is360;
	}

	public String getListenerHookURL() {
		return listenerHookURL;
	}

	public void setListenerHookURL(String listenerHookURL) {
		this.listenerHookURL = listenerHookURL;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getIpAddr() {
		return ipAddr;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setIpAddr(String ipAddr) {
		this.ipAddr = ipAddr;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public Double getSpeed() {
		return speed;
	}

	public void setSpeed(Double speed) {
		this.speed = speed;
	}

	public String getStreamUrl() {
		return streamUrl;
	}

	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}

	public String getOriginAdress() {
		return originAdress;
	}

	public void setOriginAdress(String originAdress) {
		this.originAdress = originAdress;
	}

	public Integer getMp4Enabled() {
		return mp4Enabled;
	}

	public void setMp4Enabled(Integer mp4Enabled) {
		this.mp4Enabled = mp4Enabled;
	}

	public Integer getWebMEnabled() {
		return webMEnabled;
	}

	public void setWebMEnabled(Integer webMEnabled) {
		this.webMEnabled = webMEnabled;
	}

	public Integer getSeekTimeInMs() {
		return seekTimeInMs;
	}

	public void setSeekTimeInMs(Integer seekTimeInMs) {
		this.seekTimeInMs = seekTimeInMs;
	}

	public String getConferenceMode() {
		return conferenceMode;
	}

	public void setConferenceMode(String conferenceMode) {
		this.conferenceMode = conferenceMode;
	}

	public Integer getSubtracksLimit() {
		return subtracksLimit;
	}

	public void setSubtracksLimit(Integer subtracksLimit) {
		this.subtracksLimit = subtracksLimit;
	}

	public Integer getExpireDurationMS() {
		return expireDurationMS;
	}

	public void setExpireDurationMS(Integer expireDurationMS) {
		this.expireDurationMS = expireDurationMS;
	}

	public String getRtmpURL() {
		return rtmpURL;
	}

	public void setRtmpURL(String rtmpURL) {
		this.rtmpURL = rtmpURL;
	}

	public Boolean getZombi() {
		return zombi;
	}

	public void setZombi(Boolean zombi) {
		this.zombi = zombi;
	}

	public Integer getPendingPacketSize() {
		return pendingPacketSize;
	}

	public void setPendingPacketSize(Integer pendingPacketSize) {
		this.pendingPacketSize = pendingPacketSize;
	}

	public Integer getHlsViewerCount() {
		return hlsViewerCount;
	}

	public void setHlsViewerCount(Integer hlsViewerCount) {
		this.hlsViewerCount = hlsViewerCount;
	}

	public Integer getDashViewerCount() {
		return dashViewerCount;
	}

	public void setDashViewerCount(Integer dashViewerCount) {
		this.dashViewerCount = dashViewerCount;
	}

	public Integer getWebRTCViewerCount() {
		return webRTCViewerCount;
	}

	public void setWebRTCViewerCount(Integer webRTCViewerCount) {
		this.webRTCViewerCount = webRTCViewerCount;
	}

	public Integer getRtmpViewerCount() {
		return rtmpViewerCount;
	}

	public void setRtmpViewerCount(Integer rtmpViewerCount) {
		this.rtmpViewerCount = rtmpViewerCount;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getReceivedBytes() {
		return receivedBytes;
	}

	public void setReceivedBytes(Long receivedBytes) {
		this.receivedBytes = receivedBytes;
	}

	public Long getBitrate() {
		return bitrate;
	}

	public void setBitrate(Long bitrate) {
		this.bitrate = bitrate;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	
	public String getUserAgent() {
		return userAgent;
	}
	
	public String getAltitude() {
		return altitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public String getLongitude() {
		return longitude;
	}
	
	public String getMainTrackStreamId() {
		return mainTrackStreamId;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	
	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}

	public void setMainTrackStreamId(String mainTrackStreamId) {
		this.mainTrackStreamId = mainTrackStreamId;
	}

	public List<String> getSubTrackStreamIds() {
		return subTrackStreamIds;
	}

	public void setSubTrackStreamIds(List<String> subTrackStreamIds) {
		this.subTrackStreamIds = subTrackStreamIds;
	}

	public Long getAbsoluteStartTimeMs() {
		return absoluteStartTimeMs;
	}

	public void setAbsoluteStartTimeMs(Long absoluteStartTimeMs) {
		this.absoluteStartTimeMs = absoluteStartTimeMs;
	}

	public Integer getWebRTCViewerLimit() {
		return webRTCViewerLimit;
	}

	public void setWebRTCViewerLimit(Integer webRTCViewerLimit) {
		this.webRTCViewerLimit = webRTCViewerLimit;
	}

	public Integer getHlsViewerLimit() {
		return hlsViewerLimit;
	}

	public void setHlsViewerLimit(Integer hlsViewerLimit) {
		this.hlsViewerLimit = hlsViewerLimit;
	}

	public Integer getDashViewerLimit() {
		return dashViewerLimit;
	}

	public void setDashViewerLimit(Integer dashViewerLimit) {
		this.dashViewerLimit = dashViewerLimit;
	}

	public String getSubFolder() {
		return subFolder;
	}

	public void setSubFolder(String subFolder) {
		this.subFolder = subFolder;
	}

	public Integer getCurrentPlayIndex() {
		return currentPlayIndex;
	}

	public void setCurrentPlayIndex(Integer currentPlayIndex) {
		this.currentPlayIndex = currentPlayIndex;
	}

	public String getMetaData() {
		return metaData;
	}

	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}

	public Boolean getPlaylistLoopEnabled() {
		return playlistLoopEnabled;
	}

	public void setPlaylistLoopEnabled(Boolean playlistLoopEnabled) {
		this.playlistLoopEnabled = playlistLoopEnabled;
	}

	public Long getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Long updateTime) {
		this.updateTime = updateTime;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public HLSParameters getHlsParameters() {
		return hlsParameters;
	}

	public void setHlsParameters(HLSParameters hlsParameters) {
		this.hlsParameters = hlsParameters;
	}

	public Boolean getAutoStartStopEnabled() {
		return autoStartStopEnabled;
	}

	public void setAutoStartStopEnabled(Boolean autoStartStopEnabled) {
		this.autoStartStopEnabled = autoStartStopEnabled;
	}

	public List<EncoderSettings> getEncoderSettingsList() {
		return encoderSettingsList;
	}

	public void setEncoderSettingsList(List<EncoderSettings> encoderSettingsList) {
		this.encoderSettingsList = encoderSettingsList;
	}

	/**
	 * @return the width
	 */
	public Integer getWidth() {
		return width;
	}

	/**
	 * @param width the width to set
	 */
	public void setWidth(Integer width) {
		this.width = width;
	}

	/**
	 * @return the height
	 */
	public Integer getHeight() {
		return height;
	}

	/**
	 * @param height the height to set
	 */
	public void setHeight(Integer height) {
		this.height = height;
	}

	/**
	 * @return the encoderQueueSize
	 */
	public Integer getEncoderQueueSize() {
		return encoderQueueSize;
	}

	/**
	 * @param encoderQueueSize the encoderQueueSize to set
	 */
	public void setEncoderQueueSize(Integer encoderQueueSize) {
		this.encoderQueueSize = encoderQueueSize;
	}

	/**
	 * @return the dropPacketCountInIngestion
	 */
	public Integer getDropPacketCountInIngestion() {
		return dropPacketCountInIngestion;
	}

	/**
	 * @param dropPacketCountInIngestion the dropPacketCountInIngestion to set
	 */
	public void setDropPacketCountInIngestion(Integer dropPacketCountInIngestion) {
		this.dropPacketCountInIngestion = dropPacketCountInIngestion;
	}

	/**
	 * @return the dropFrameCountInEncoding
	 */
	public Integer getDropFrameCountInEncoding() {
		return dropFrameCountInEncoding;
	}

	/**
	 * @param dropFrameCountInEncoding the dropFrameCountInEncoding to set
	 */
	public void setDropFrameCountInEncoding(Integer dropFrameCountInEncoding) {
		this.dropFrameCountInEncoding = dropFrameCountInEncoding;
	}

	/**
	 * @return the packetLostRatio
	 */
	public Double getPacketLostRatio() {
		return packetLostRatio;
	}

	/**
	 * @param packetLostRatio the packetLostRatio to set
	 */
	public void setPacketLostRatio(Double packetLostRatio) {
		this.packetLostRatio = packetLostRatio;
	}

	/**
	 * @return the jitterMs
	 */
	public Integer getJitterMs() {
		return jitterMs;
	}

	/**
	 * @param jitterMs the jitterMs to set
	 */
	public void setJitterMs(Integer jitterMs) {
		this.jitterMs = jitterMs;
	}

	/**
	 * @return the rttMs
	 */
	public Integer getRttMs() {
		return rttMs;
	}

	/**
	 * @param rttMs the rttMs to set
	 */
	public void setRttMs(Integer rttMs) {
		this.rttMs = rttMs;
	}

	/**
	 * @return the packetsLost
	 */
	public Integer getPacketsLost() {
		return packetsLost;
	}

	/**
	 * @param packetsLost the packetsLost to set
	 */
	public void setPacketsLost(Integer packetsLost) {
		this.packetsLost = packetsLost;
	}

	/**
	 * @return the remoteIp
	 */
	public String getRemoteIp() {
		return remoteIp;
	}

	/**
	 * @param remoteIp the remoteIp to set
	 */
	public void setRemoteIp(String remoteIp) {
		this.remoteIp = remoteIp;
	}
    
    
    
}


