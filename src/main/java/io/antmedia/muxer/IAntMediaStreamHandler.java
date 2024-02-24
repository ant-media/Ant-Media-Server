package io.antmedia.muxer;

import java.io.File;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.plugin.api.IFrameListener;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.IStreamListener;

public interface IAntMediaStreamHandler {
	
	public static final String VERTX_BEAN_NAME = "vertxCore";
	
	public static final String BROADCAST_STATUS_CREATED = "created";
	public static final String BROADCAST_STATUS_BROADCASTING = "broadcasting";
	public static final String BROADCAST_STATUS_FINISHED = "finished";
	public static final String BROADCAST_STATUS_PREPARING = "preparing";
	public static final String BROADCAST_STATUS_ERROR = "error";
	public static final String BROADCAST_STATUS_FAILED = "failed";
	
	public static final String PUBLISH_TYPE_PULL = "Pull";
	public static final String PUBLISH_TYPE_RTMP = "RTMP";
	public static final String PUBLISH_TYPE_WEBRTC = "WebRTC";
	public static final String PUBLISH_TYPE_SRT = "SRT";
	
	
	/**
	 * Called by some muxer like MP4Muxer
	 * 
	 * id actually is the name of the file however in some cases file name and the id may be different
	 * in some cases like there is already a file with that name
	 * 
	 * @param id is the name of the stream published 
	 * @param file video file that muxed is finished
	 * @param duration of the video in milliseconds
	 * @param resolution height of the video 
	 */
	public void muxingFinished(String id, File file, long startTime, long duration , int resolution, String path, String vodId);
	
	
	/**
	 * Update stream quality, speed and number of pending packet size and update time
	 * in datastore
	 * 
	 * @param id this is the id of the stream
	 * 
	 * @param quality, quality of the stream values can be 
	 * {@link MuxAdaptor#QUALITY_GOOD, MuxAdaptor#QUALITY_AVERAGE, MuxAdaptor#QUALITY_POOR, MuxAdaptor#QUALITY_NA}
	 * 
	 * @param speed
	 * Speed of the stream. It should 1x
	 * 
	 * @param pendingPacketSize
	 * Number of packets pending to be processed
	 */
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize, long updateTimeMs);

    /***
     * Adds a MuxAdaptor when a muxAdaptor is created
     *
     * @param muxAdaptor
     */
    public void muxAdaptorAdded(MuxAdaptor muxAdaptor);

    /***
     * Removes a MuxAdaptor when a muxAdaptor is closingResources
     *
     * @param muxAdaptor
     */
    public void muxAdaptorRemoved(MuxAdaptor muxAdaptor);

    /***
     * Checks a Stream parameters is valid.
     *
     * @param inputFormatContext, pkt, streamId
     */
	public boolean isValidStreamParameters(int width, int height, int fps, int bitrate, String streamId);
	
	/**
	 * 
	 * @return true if server is shutting down
	 */
	public boolean isServerShuttingDown();

	/**
	 * Notify the handler that stream is started to publish
	 * @param streamName
	 * @param absoluteStartTimeMs
	 * @param publishType
	 */
	public void startPublish(String streamName, long absoluteStartTimeMs, String publishType);
	
	/**
	 * Notify the handler that is stream is stopped
	 * @param streamId
	 */
	public void stopPublish(String streamId);
	
	/**
	 * Update broadcast status to BROADCASTING
	 * 
	 * @param streamId is the id of the stream.
	 * @param absoluteStartTimeMs: It's the absolute start time if available
	 * @param publishType: It's RTMP, WebRTC, StreamSource
	 * @param broadcast: It's the broadcast object. If it's null, a new record will be created
	 * 
	 * @return broadcast object from database
	 */
	public Broadcast updateBroadcastStatus(String streamId, long absoluteStartTimeMs, String publishType, Broadcast broadcast);
	
	/**
	 * Add a stream listener to get notified when a new stream is started or finished. 
	 * {@link IStreamListener} interface has callbacks for conference attendees.
	 * It's very useful to use in plugins. 
	 * 
	 * @param listener
	 */
	public void addStreamListener(IStreamListener listener);
	
	/**
	 * Remove a stream listener from the Stream Handler to stop getting notified
	 * 
	 * @param listener
	 */
	public void removeStreamListener(IStreamListener listener) ;
	
	/**
	 * Add listener to get raw audio or video frames from the internal of Ant Media Server. 
	 * The methods are called if the audio/video frames are decoded due to settings such adaptive bitrate etc.. 
	 * This method does not force the Ant Media Server to decode the streams.
	 * 'Frame' is the decoded data of audio/video 'Packet'
	 * 
	 * @param streamId
	 * @param listener
	 */
	public void addFrameListener(String streamId, IFrameListener listener);
	
	/**
	 * Remove frame listener from the Stream Handler  to stop getting notified
	 * 
	 * @param streamId
	 * @param listener
	 */
	public void removeFrameListener(String streamId, IFrameListener listener);
	
	/**
	 * Add listener to get audio and video packets from the internal of Ant Media Server. 
	 * 'Packet' is the encoded data of audio/video 'Frame'. 
	 * 
	 * @param streamId
	 * @param listener
	 * @return
	 */
	public boolean addPacketListener(String streamId, IPacketListener listener);
	
	/**
	 * Remove listener from the Stream Handler to stop getting notified
	 * 
	 * @param streamId
	 * @param listener
	 * @return
	 */
	public boolean removePacketListener(String streamId, IPacketListener listener);
	
	/**
	 * Create another broadcast. It's useful to create another manipulated version of the original broadcast
	 * in the plugins. The returning frame listener should be feed with raw audio and video frames
	 * 
	 * @param streamId
	 * @return
	 */
	public IFrameListener createCustomBroadcast(String streamId);
	
	/**
	 * Stop the custom broadcast that is being created. Stop encoders and make database operations.
	 * 
	 * @param streamId
	 */
	public void stopCustomBroadcast(String streamId);
	
	/**
	 * Get the MuxAdaptor of the stream. MuxAdaptor is the base class that is responsible for ingesting and distributing the stream
	 * 
	 * @param streamId
	 * @return
	 */
	public MuxAdaptor getMuxAdaptor(String streamId);
}
