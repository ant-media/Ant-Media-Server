package io.antmedia.webrtc;

public class PublishParameters {

    private final String streamId;
    private  String token;
    //default values are true for viode and audio
    private boolean enableVideo = true;
    private boolean enableAudio = true;
    
    private String subscriberId;
    private String subscriberCode;
    private String streamName;
    private String mainTrack;
    private String metaData;
    private String linkedSessionForSignaling;
    
    public PublishParameters(String streamId) {
    	this.streamId = streamId;
    } 
    
    public void setEnableVideo(boolean enableVideo) {
		this.enableVideo = enableVideo;
	}

	public void setEnableAudio(boolean enableAudio) {
		this.enableAudio = enableAudio;
	}

	public void setSubscriberId(String subscriberId) {
		this.subscriberId = subscriberId;
	}

	public void setSubscriberCode(String subscriberCode) {
		this.subscriberCode = subscriberCode;
	}

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}

	public void setMainTrack(String mainTrack) {
		this.mainTrack = mainTrack;
	}

	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}

	public void setLinkedSessionForSignaling(String linkedSessionForSignaling) {
		this.linkedSessionForSignaling = linkedSessionForSignaling;
	}

    public String getToken() {
        return token;
    }

    public boolean isEnableVideo() {
        return enableVideo;
    }

    public boolean isEnableAudio() {
        return enableAudio;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public String getSubscriberCode() {
        return subscriberCode;
    }

    public String getStreamName() {
        return streamName;
    }

    public String getMainTrack() {
        return mainTrack;
    }

    public String getMetaData() {
        return metaData;
    }

    public String getLinkedSessionForSignaling() {
        return linkedSessionForSignaling;
    }
    public String getStreamId() {
        return streamId;
    }

	public void setToken(String tokenId) {
		this.token = tokenId;
	}
}