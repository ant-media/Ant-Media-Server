package io.antmedia.muxer;

import java.io.File;
import java.util.Map;

import org.onvif.ver10.device.wsdl.GetScopes;
import org.red5.server.api.scope.IScope;

import io.antmedia.AppSettings;
import io.antmedia.IAppSettingsUpdateListener;
import io.antmedia.analytic.model.PublishStatsEvent;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.plugin.api.IFrameListener;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.webrtc.datachannel.IDataChannelRouter;

public interface IAntMediaStreamHandler {
	
	public static final String VERTX_BEAN_NAME = "vertxCore";
	
	public static final String BROADCAST_STATUS_CREATED = "created";
	public static final String BROADCAST_STATUS_BROADCASTING = "broadcasting";
	public static final String BROADCAST_STATUS_FINISHED = "finished";
	public static final String BROADCAST_STATUS_PREPARING = "preparing";
	public static final String BROADCAST_STATUS_ERROR = "error";
	public static final String BROADCAST_STATUS_FAILED = "failed";
	public static final String BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY = "terminated_unexpectedly";
	
	public static final String PUBLISH_TYPE_PULL = "Pull";
	public static final String PUBLISH_TYPE_RTMP = "RTMP";
	public static final String PUBLISH_TYPE_WEBRTC = "WebRTC";
	public static final String PUBLISH_TYPE_SRT = "SRT";
	
	public static final String DEFAULT_USER_ROLE = "default";
	
	
	public static final String WEBAPPS_PATH = "webapps/";

	
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
	 * 
	 * @Deprecated use {@link #muxingFinished(Broadcast, File, long, long, int, String, String)} because Broadcast object may be deleted when this method is called
	 */
	@Deprecated
	public void muxingFinished(String id, File file, long startTime, long duration , int resolution, String path, String vodId);
	
	
	/**
	 * Called by some muxer like MP4Muxer
	 * 
	 * id actually is the name of the file however in some cases file name and the id may be different
	 * in some cases like there is already a file with that name
	 * 
	 * @param broadcast object that muxed is finished
	 * @param streamId is the id of the stream
	 * @param file video file that muxed is finished
	 * @param duration of the video in milliseconds
	 * @param resolution height of the video 
	 * @param previewFilePath path of the preview file
	 * 
	 */
	public void muxingFinished(Broadcast broadcast, String streamId, File file, long startTime, long duration , int resolution, String previewFilePath, String vodId);
	
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
	 * 
	 * @param totalByteReceived
	 * @param byteTransferred 
	 * @param currentTimeMillis
	 * 
	 * @Deprecated {@link #setQualityParameters(String, PublishStatsEvent, long)} should be used instead of this method
	 */
	@Deprecated
	public void setQualityParameters(String streamId, String quality, double speed, int inputQueueSize, long currentTimeMillis);
	
	
	/**
	 * Sets quality parameters for the given stream ID.

	 * This method is called to update quality-related parameters 
	 * for the specified stream during the publishing process. 

	 * @param streamId The unique identifier of the stream.
	 * @param stats The current publishing statistics for the stream.
	 * @param currentTimeMillis The current timestamp in milliseconds.
	 */
	public default void setQualityParameters(String streamId, PublishStatsEvent stats, long currentTimeMillis) {
		
	}

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
	 * @deprecated use {@link #startPublish(String, long, String, String)} instead of this method
	 * 
	 * @param streamName
	 * @param absoluteStartTimeMs
	 * @param publishType
	 */
	@Deprecated
	public void startPublish(String streamName, long absoluteStartTimeMs, String publishType);
	
	
	/**
	 * Notify the handler that stream is started to publish
	 * 
	 * @param streamId
	 * @param absoluteStartTimeMs
	 * @param publishType
	 * @param subscriberId: It's the id of the subscriber. It can be null if it's not available.
	 */
	public void startPublish(String streamId, long absoluteStartTimeMs, String publishType, String subscriberId, Map<String, String> publishParameters);

	
	/**
	 * Notify the handler that is stream is stopped
	 * @deprecated use {@link #stopPublish(String, String)} instead of this method
	 * @param streamId
	 */
	@Deprecated
	public void stopPublish(String streamId);
	
	/**
	 * Notify the handler that is stream is stopped
	 * 
	 * @param streamId
	 * @param subscriberId: It's the id of the subscriber. It can be null if it's not available.
	 */
	public void stopPublish(String streamId, String subscriberId);
	
	
	/**
	 * Notify the handler that is stream is stopped
	 * 
	 * @param streamId
	 * @param subscriberId: It's the id of the subscriber. It can be null if it's not available.
	 * @param publishParameters: It's the parameters of the publish. It can be null if it's not available.
	 */
	public void stopPublish(String streamId, String subscriberId, Map<String, String> publishParameters);	
	/**
	 * Update broadcast status to BROADCASTING
	 * 
	 * @param streamId is the id of the stream.
	 * @param absoluteStartTimeMs: @deprecated It's not used anymore. It's the absolute start time if available 
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
	 * Create another broadcast. It's useful to create another manipulated version of the original broadcast
	 * in the plugins. The returning frame listener should be feed with raw audio and video frames
	 * 
	 * @param streamId
	 * @param height
	 * @param bitrate
	 * @return
	 */
	public IFrameListener createCustomBroadcast(String streamId, int height, int bitrate);
	
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
	
	/**	
	 * Get the AppSettings of the application
	 * @return AppSettings
	 */
	public AppSettings getAppSettings();


	/**
	 * Get the DataStore of the application
	 * 
	 * @return DataStore
	 */
	public DataStore getDataStore();

	/**
	 * Get the scope
	 * @return
	 */
	public IScope getScope();


	/**
	 * Notify the webhook about the stream status
	 * 
	 * @param streamName
	 * @param absoluteStartTimeMs
	 */
	public void notifyWebhookForStreamStatus(Broadcast broadcast, int width, int height, long totalByteReceived,
			int inputQueueSize, int encodingQueueSize, int dropFrameCountInEncoding, int dropPacketCountInIngestion, double speed);
	
	/**
	 * Add listener that is notified when the settings are updated
	 * @param listener
	 */
	public void addSettingsUpdateListener(IAppSettingsUpdateListener listener);
	
	/**
	 * Get data channel router for data channel delivery
	 * @return
	 */
	public IDataChannelRouter getDataChannelRouter();
}
