package io.antmedia.webrtc;

import java.util.List;

import io.antmedia.datastore.db.types.Broadcast;

public class PlayParameters {

	public final String streamId;
	public final Broadcast broadcast;
	public final String tokenId;
	public final boolean playTokenControlEnabled;
	public final String roomId;
	public final List<String> enabledTracks;
	public final boolean subscriberOnly;
	public final String subscriberId;
	public final String subscriberName;
	public final String subscriberCodeText;
	public final String viewerInfo;
	public final String linkedSessionForSignaling;
	public final String role;
	public final boolean isMainTrack;
	public final String userPublishId;
	public final boolean disableTracksByDefault;
	public final boolean isWhepClient;


	public PlayParameters(String streamId, Broadcast broadcast, String tokenId, boolean playTokenControlEnabled, String roomId,
						  List<String> enabledTracks, boolean subscriberOnly, String subscriberId, String subscriberName, String subscriberCodeText,
						  String viewerInfo, String linkedSessionForSignaling, String role, boolean isMainTrack, String userPublishId, boolean disableTracksByDefault, boolean isWhepClient) {
		this.streamId = streamId;
		this.broadcast = broadcast;
		this.tokenId = tokenId;
		this.playTokenControlEnabled = playTokenControlEnabled;
		this.roomId = roomId;
		this.enabledTracks = enabledTracks;
		this.subscriberOnly = subscriberOnly;
		this.subscriberId = subscriberId;
		this.subscriberName = subscriberName;
		this.subscriberCodeText = subscriberCodeText;
		this.viewerInfo = viewerInfo;
		this.linkedSessionForSignaling = linkedSessionForSignaling;
		this.role = role;
		this.isMainTrack = isMainTrack;
		this.userPublishId = userPublishId;
		this.disableTracksByDefault = disableTracksByDefault;
		this.isWhepClient = isWhepClient;
	}
}