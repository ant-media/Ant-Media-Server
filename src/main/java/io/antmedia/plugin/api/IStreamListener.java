package io.antmedia.plugin.api;

import io.antmedia.datastore.db.types.Broadcast;

/*
 * Interface class to inform the plugins with stream start/start event.
 */
public interface IStreamListener {
	/**
	 * AMS inform the plugins when a stream is started with this method.
	 * @param streamId is the id of the stream
	 * 
	 * @Deprecated use {@link #streamStarted(Broadcast)} because Broadcast object may be deleted when this method is called
	 */
	@Deprecated (since="3.0", forRemoval = true)
	public default void streamStarted(String streamId) {
		//do nothing
	}
	
	/**
	 * AMS inform the plugins when a stream is started with this method.
	 * @param broadcast is the the broadcast object of the stream
	 * 
	 */
	public default void streamStarted(Broadcast broadcast) {
		//do nothing
	}
	
	/**
	 * AMS inform the plugins when a stream is finished with this method.
	 * @param streamId is the id of the stream
	 * 
	 * @Deprecated use {@link #streamFinished(Broadcast)} because Broadcast object may be deleted when this method is called
	 */
	@Deprecated (since="3.0", forRemoval = true)
	public default void streamFinished(String streamId) {
		//do nothing
	}

	/**
	 * AMS inform the plugins when a stream is finished with this method.
	 * @param broadcast is the broadcast object of the stream
	 * 
	 * The default implementation does nothing in order to avoid breaking the existing plugins.
	 */
	public default void streamFinished(Broadcast broadcast) {
		//do nothing
	}
	/**
	 * AMS inform the plugins when a new participant joins to the conference room
	 * @param roomId is the id of the conference room
	 * @param streamId is the id of new stream
	 */
	public void joinedTheRoom(String roomId, String streamId);

	/**
	 * AMS inform the plugins when a participant leaves from the conference room
	 * @param rroomId is the id of the conference room
	 * @param streamId is the id of new stream
	 */
	public void leftTheRoom(String roomId, String streamId);

}
