package io.antmedia.webrtc;

/**
 * Class that holds parameters for WebRTC stream playback
 */
public class PlayParameters {
    
    private final String streamId;
    private String token;
    private String subscriberId;
    private String viewerInfo;
    private String mainTrack;
    private String linkedSessionForSignaling;
    private String role;
    
    /**
     * Constructor with required stream ID
     * @param streamId ID of the stream to play
     */
    public PlayParameters(String streamId) {
        this.streamId = streamId;
    }
    
    /**
     * @return the streamId
     */
    public String getStreamId() {
        return streamId;
    }
    
    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }
    
    /**
     * @param token the token to set
     */
    public void setToken(String token) {
        this.token = token;
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
     * @return the viewerInfo
     */
    public String getViewerInfo() {
        return viewerInfo;
    }
    
    /**
     * @param viewerInfo the viewerInfo to set
     */
    public void setViewerInfo(String viewerInfo) {
        this.viewerInfo = viewerInfo;
    }
    
    /**
     * @return the mainTrack
     */
    public String getMainTrack() {
        return mainTrack;
    }
    
    /**
     * @param mainTrack the mainTrack to set
     */
    public void setMainTrack(String mainTrack) {
        this.mainTrack = mainTrack;
    }
    
    /**
     * @return the linkedSessionForSignaling
     */
    public String getLinkedSessionForSignaling() {
        return linkedSessionForSignaling;
    }
    
    /**
     * @param linkedSessionForSignaling the linkedSessionForSignaling to set
     */
    public void setLinkedSessionForSignaling(String linkedSessionForSignaling) {
        this.linkedSessionForSignaling = linkedSessionForSignaling;
    }
    
    /**
     * @return the role
     */
    public String getRole() {
        return role;
    }
    
    /**
     * @param role the role to set
     */
    public void setRole(String role) {
        this.role = role;
    }
} 