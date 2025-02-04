package io.antmedia;

import static io.antmedia.rest.RestServiceBase.FETCH_REQUEST_REDIRECTED_TO_ORIGIN;
import static io.antmedia.muxer.IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING;
import static org.bytedeco.ffmpeg.global.avcodec.avcodec_get_name;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.Nonnull;

import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.statistic.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.bytedeco.ffmpeg.avcodec.AVCodecParameters;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.stream.ClientBroadcastStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.analytic.model.PublishEndedEvent;
import io.antmedia.analytic.model.PublishStartedEvent;
import io.antmedia.analytic.model.PublishStatsEvent;
import io.antmedia.analytic.model.ViewerCountEvent;
import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastUpdate;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.filter.StreamAcceptFilter;
import io.antmedia.ipcamera.OnvifCamera;
import io.antmedia.logger.LoggerUtils;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.muxer.Muxer;
import io.antmedia.plugin.api.IClusterStreamFetcher;
import io.antmedia.plugin.api.IFrameListener;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.plugin.api.IStreamListener;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.rest.model.Result;
import io.antmedia.security.AcceptOnlyStreamsInDataStore;
import io.antmedia.settings.ServerSettings;
import io.antmedia.shutdown.AMSShutdownManager;
import io.antmedia.shutdown.IShutdownListener;
import io.antmedia.statistic.type.RTMPToWebRTCStats;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.storage.StorageClient;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.track.ISubtrackPoller;
import io.antmedia.webrtc.PublishParameters;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;
import io.antmedia.websocket.WebSocketConstants;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;
import jakarta.validation.constraints.NotNull;

public class AntMediaApplicationAdapter  extends MultiThreadedApplicationAdapter implements IAntMediaStreamHandler, IShutdownListener {

	/**
	 * Timeout value that stream is considered as finished or stuck
	 */
	public static final int STREAM_TIMEOUT_MS = 2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS;

	public static final String BEAN_NAME = "web.handler";

	public static final int BROADCAST_STATS_RESET = 0;
	public static final String HOOK_ACTION_END_LIVE_STREAM = "liveStreamEnded";
	public static final String HOOK_ACTION_START_LIVE_STREAM = "liveStreamStarted";
	public static final String HOOK_ACTION_STREAM_STATUS = "liveStreamStatus";

	public static final String HOOK_ACTION_VOD_READY = "vodReady";

	public static final String HOOK_ACTION_PUBLISH_TIMEOUT_ERROR = "publishTimeoutError";
	public static final String HOOK_ACTION_ENCODER_NOT_OPENED_ERROR =  "encoderNotOpenedError";
	public static final String HOOK_ACTION_ENDPOINT_FAILED = "endpointFailed";


	/**
	 * This is used to notify that the play is stopped
	 */
	public static final String HOOK_ACTION_PLAY_STOPPED = "playStopped";


	/**
	 * This is used to notify that the play is started
	 */
	public static final String HOOK_ACTION_PLAY_STARTED = "playStarted";


	/**
	 * This is used to notify that the subtrack is created in the main track
	 * In video conferencing, it means a stream is started in the room
	 */
	public static final String HOOK_ACTION_SUBTRACK_ADDED_IN_THE_MAINTRACK = "subtrackAddedInTheMainTrack";


	/**
	 * This is used to notify that the subtrack left the main track
	 * In video conferencing, it means a stream left the room
	 */
	public static final String HOOK_ACTION_SUBTRACK_LEFT_FROM_THE_MAINTRACK = "subtrackLeftTheMainTrack";


	/**
	 * This is used to notify that the first active subtrack is created in the main track
	 * In video conferencing, it means the first stream is started in the room
	 */
	public static final String HOOK_ACTION_FIRST_ACTIVE_SUBTRACK_ADDED_IN_THE_MAINTRACK = "firstActiveTrackAddedInMainTrack";

	/**
	 * This is used to notify that there is no active subtracks left in the main track.
	 * In video conferencing, it means there is no active stream left in the room
	 */
	public static final String HOOK_ACTION_NO_ACTIVE_SUBTRACKS_LEFT_IN_THE_MAINTRACK = "noActiveSubtracksLeftInMainTrack";

	public static final String STREAMS = "streams";

	public static final String DEFAULT_LOCALHOST = "127.0.0.1";

	protected static Logger logger = LoggerFactory.getLogger(AntMediaApplicationAdapter.class);
	private ServerSettings serverSettings;
	public static final String VOD = "VoD";
	public static final String LIVE_STREAM = "liveStream";
	public static final String IP_CAMERA = "ipCamera";
	public static final String STREAM_SOURCE = "streamSource";
	public static final String PLAY_LIST = "playlist";
	protected static final int END_POINT_LIMIT = 20;
	public static final int CLUSTER_POST_RETRY_ATTEMPT_COUNT = 3;
	public static final int CLUSTER_POST_TIMEOUT_MS = 1000;

	//Allow any sub directory under /
	private static final String VOD_IMPORT_ALLOWED_DIRECTORY = "/";


	private List<IStreamPublishSecurity> streamPublishSecurityList;
	private Map<String, OnvifCamera> onvifCameraList = new ConcurrentHashMap<>();
	protected StreamFetcherManager streamFetcherManager;
	protected Map<String, MuxAdaptor> muxAdaptors = new ConcurrentHashMap<>();
	private DataStore dataStore;
	private DataStoreFactory dataStoreFactory;

	private StreamAcceptFilter streamAcceptFilter;
	private AppSettings appSettings;
	private Vertx vertx;

	protected List<String> encoderBlockedStreams = new ArrayList<>();
	private int numberOfEncoderNotOpenedErrors = 0;
	protected int publishTimeoutStreams = 0;
	private List<String> publishTimeoutStreamsList = new ArrayList<>();
	private boolean shutdownProperly = true;

	protected WebRTCVideoReceiveStats webRTCVideoReceiveStats = new WebRTCVideoReceiveStats();

	protected WebRTCAudioReceiveStats webRTCAudioReceiveStats = new WebRTCAudioReceiveStats();


	protected WebRTCVideoSendStats webRTCVideoSendStats = new WebRTCVideoSendStats();

	protected WebRTCAudioSendStats webRTCAudioSendStats = new WebRTCAudioSendStats();

	private IClusterNotifier clusterNotifier;

	protected boolean serverShuttingDown = false;

	protected StorageClient storageClient;

	protected Queue<IStreamListener> streamListeners = new ConcurrentLinkedQueue<>();

	protected Map<String, Long> playListSchedulerTimer = new ConcurrentHashMap<>();

	IClusterStreamFetcher clusterStreamFetcher;

	protected ISubtrackPoller subtrackPoller;

	private Random random = new Random();

	private IStatsCollector statsCollector;
	
	private Set<IAppSettingsUpdateListener> settingsUpdateListenerSet = new ConcurrentHashSet<IAppSettingsUpdateListener>();

	@Override
	public boolean appStart(IScope app) {
		setScope(app);
		for (IStreamPublishSecurity streamPublishSecurity : getStreamPublishSecurityList()) {
			registerStreamPublishSecurity(streamPublishSecurity);
		}
		//init vertx
		getVertx();

		//initalize to access the data store directly in the code
		getDataStore();

		// Create initialized file in application
		Result result = createInitializationProcess(app.getName());

		//initialize storage client
		storageClient = (StorageClient) app.getContext().getBean(StorageClient.BEAN_NAME);

		if (!result.isSuccess()) {
			//Save App Setting
			this.shutdownProperly = false;

			// Reset Broadcast Stats
			resetBroadcasts();
		}

		if (app.getContext().hasBean(IClusterNotifier.BEAN_NAME)) {
			//which means it's in cluster mode
			clusterNotifier = (IClusterNotifier) app.getContext().getBean(IClusterNotifier.BEAN_NAME);
			logger.info("Registering settings listener to the cluster notifier for app: {}", app.getName());			

			clusterNotifier.registerSettingUpdateListener(getAppSettings().getAppName(), new IAppSettingsUpdateListener() {

				@Override
				public boolean settingsUpdated(AppSettings settings) {
					return updateSettings(settings, false, true);
				}

				@Override
				public AppSettings getCurrentSettings() {

					return getAppSettings();
				}
			});
			AppSettings storedSettings = clusterNotifier.getClusterStore().getSettings(app.getName());

			boolean updateClusterSettings = false;
			if(storedSettings == null)
			{
				logger.warn("There is not a stored settings for the app:{}. It will update the database for app settings", app.getName());
				storedSettings = appSettings;
				updateClusterSettings = true;
			}
			else if (getServerSettings().getHostAddress().equals(storedSettings.getWarFileOriginServerAddress())
					&& storedSettings.isPullWarFile())
			{
				logger.info("This instance is the host of the app:{} to be deployed to the cluster", app.getName());
				//get the current value of isPullWarFile here otherwise it will be set to false below
				boolean isPullWarFile = storedSettings.isPullWarFile();
				storedSettings = appSettings;
				updateClusterSettings = true;

				//keep the settings to let the app distributed to all nodes
				storedSettings.setPullWarFile(isPullWarFile);
				storedSettings.setWarFileOriginServerAddress(getServerSettings().getHostAddress());
			}


			logger.info("Updating settings while app({}) is being started. AppSettings will be saved to Cluster db? Answer -> {}", app.getName(), updateClusterSettings ? "yes" : "no");
			updateSettings(storedSettings, updateClusterSettings, false);

		}

		vertx.setTimer(1000, l -> {

			getStreamFetcherManager();
			if(appSettings.isStartStreamFetcherAutomatically()) {
				List<Broadcast> streams = getDataStore().getExternalStreamsList();
				logger.info("Stream source size: {}", streams.size());
				for (Broadcast broadcast : streams)
				{
					if (!broadcast.isAutoStartStopEnabled()) {
						//start streaming is auto/stop is not enabled
						streamFetcherManager.startStreaming(broadcast);
					}
				}
			}

			//Schedule Playlist items 
			int offset = 0;
			int batch = 50;
			List<Broadcast> playlist;
			long now = System.currentTimeMillis();
			while ((playlist = getDataStore().getBroadcastList(offset, batch, AntMediaApplicationAdapter.PLAY_LIST, null, null, null)) != null ) {

				if (playlist.isEmpty()) {
					break;
				}


				for (Broadcast broadcast : playlist) 
				{
					schedulePlayList(now, broadcast);
				}

				offset += batch;

			} 


			synchUserVoDFolder(null, appSettings.getVodFolder());
		});


		AMSShutdownManager.getInstance().subscribe(this);

		//With the common app structure, we won't need to null check for WebRTCAdaptor
		if (app.getContext().hasBean(IWebRTCAdaptor.BEAN_NAME))
		{
			IWebRTCAdaptor webRTCAdaptor = (IWebRTCAdaptor) app.getContext().getBean(IWebRTCAdaptor.BEAN_NAME);

			webRTCAdaptor.setExcessiveBandwidthValue(appSettings.getExcessiveBandwidthValue());
			webRTCAdaptor.setExcessiveBandwidthCallThreshold(appSettings.getExcessiveBandwidthCallThreshold());
			webRTCAdaptor.setTryCountBeforeSwitchback(appSettings.getExcessiveBandwithTryCountBeforeSwitchback());
			webRTCAdaptor.setExcessiveBandwidthAlgorithmEnabled(appSettings.isExcessiveBandwidthAlgorithmEnabled());
			webRTCAdaptor.setPacketLossDiffThresholdForSwitchback(appSettings.getPacketLossDiffThresholdForSwitchback());
			webRTCAdaptor.setRttMeasurementDiffThresholdForSwitchback(appSettings.getRttMeasurementDiffThresholdForSwitchback());
		}

		setStorageclientSettings(appSettings);


		logger.info("{} started", app.getName());

		return true;
	}

	public void schedulePlayList(long now, Broadcast broadcast) 
	{
		if (broadcast.getPlannedStartDate() != 0) 
		{	
			long startTimeDelay = (broadcast.getPlannedStartDate()*1000) - now;

			if (startTimeDelay > 0) 
			{
				//Create some random value to not let any other node pull the stream at the same time.
				//I also improve the StreamFetcher to not get started in the WorkerThread if another node is pulling.
				//TBH, It's not a good solution and I could not find something better for now
				//@mekya

				long randomDelay = random.nextInt(5000);
				logger.info("Scheduling playlist to play after {}ms with random delay:{}ms, total delay:{}ms for id:{}", startTimeDelay, randomDelay, (startTimeDelay + randomDelay), broadcast.getStreamId());
				startTimeDelay += randomDelay;
				long timerId = vertx.setTimer(startTimeDelay, 
						(timer) -> 
				{

					Broadcast freshBroadcast = getDataStore().get(broadcast.getStreamId());
					if (freshBroadcast != null && 
							AntMediaApplicationAdapter.PLAY_LIST.equals(freshBroadcast.getType())) 
					{
						logger.info("Starting scheduled playlist for id:{} ", freshBroadcast.getStreamId());
						streamFetcherManager.startPlaylist(freshBroadcast);
					}
					else 
					{
						if (freshBroadcast == null) {
							logger.warn("Not starting playlist because it's null for stream id:{}. It must have been deleted", broadcast.getStreamId());
						}
						else {
							logger.error("Not starting playlist because wrong configuration for streamId:{}. It should be a bug in somewhere", broadcast.getStreamId());
						}
					}
					playListSchedulerTimer.remove(freshBroadcast.getStreamId());

				});

				playListSchedulerTimer.put(broadcast.getStreamId(), timerId);
			}		
		}
	}

	public void cancelPlaylistSchedule(String streamId) {
		Long timerId = playListSchedulerTimer.remove(streamId);
		if (timerId != null) {
			vertx.cancelTimer(timerId);
		}

	}

	/**
	 * This method is called after ungraceful shutdown
	 * @return
	 */
	public Result resetBroadcasts(){

		logger.info("Resetting streams viewer numbers because there is an unexpected stop happened in app: {}", getScope() != null? getScope().getName() : "[scope is null]");

		int operationCount = getDataStore().resetBroadcasts(getServerSettings().getHostAddress());

		logger.info("Resetting subscriber connection status" );
		getDataStore().resetSubscribersConnectedStatus();

		Result result = new Result(true);
		result.setMessage("Successfull operations: "+ operationCount);

		return result;
	}

	/**
	 * @Deprecated
	 * This method is deprecated. Use {@link #importVoDFolder(String)} {@link #unlinksVoD(String)}
	 * @param oldFolderPath
	 * @param vodFolderPath
	 * @return
	 */
	public boolean synchUserVoDFolder(String oldFolderPath, String vodFolderPath)
	{
		boolean result = false;
		File streamsFolder = new File(WEBAPPS_PATH + getScope().getName() + "/streams");

		if(oldFolderPath != null && !oldFolderPath.equals("")){
			deleteSymbolicLink(new File(oldFolderPath), streamsFolder);
		}


		if(vodFolderPath != null && !vodFolderPath.equals(""))
		{
			File f = new File(vodFolderPath);
			createSymbolicLink(streamsFolder, f);
			//if file does not exists, it means reset the vod
			getDataStore().fetchUserVodList(f);
			result = true;
		}

		return result;
	}

	public Result createSymbolicLink(File streamsFolder, File vodFolder) {
		Result result = null;
		try {
			if (!streamsFolder.exists()) {
				streamsFolder.mkdirs();
			}
			if (vodFolder.exists() && vodFolder.isDirectory())
			{
				File newLinkFile = new File(streamsFolder, vodFolder.getName());
				if (!Files.isSymbolicLink(newLinkFile.toPath()))
				{
					Path target = vodFolder.toPath();
					Files.createSymbolicLink(newLinkFile.toPath(), target);
					result = new Result(true);
				}
				else {
					result = new Result(false, "There is already a file with the name "+ vodFolder.getName()+" in the streams directory");
				}
			}
			else {
				result = new Result(false, vodFolder.getAbsolutePath() + " does not exist or is not a directory");
			}

		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			result = new Result(false, "Exception in creating symbolic link");
		}
		return result;
	}

	/**
	 * Import vod files recursively in the directory. It also created symbolic link to make the files streamable
	 * @param vodFolderPath absolute path of the vod folder to be imported
	 * @return
	 */
	public Result importVoDFolder(String vodFolderPath) {
		File streamsFolder = new File(WEBAPPS_PATH + getScope().getName() + "/streams");
		File directory = new File(vodFolderPath == null ? "" : vodFolderPath);

		File allowedDirectory = new File(VOD_IMPORT_ALLOWED_DIRECTORY);
		Result result = null;
		try {
			if (FileUtils.directoryContains(allowedDirectory, directory))
			{

				result = createSymbolicLink(streamsFolder, directory);
				if (result.isSuccess()) {
					int numberOfFilesImported = importToDB(directory, directory);
					result.setMessage(numberOfFilesImported + " files are imported");
				}
			}
			else {
				result = new Result(false, "VoD import directory is allowed under " + VOD_IMPORT_ALLOWED_DIRECTORY );
			}
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			result = new Result(false, "VoD import directory is allowed under " + VOD_IMPORT_ALLOWED_DIRECTORY );
		}

		return result;
	}

	public Result unlinksVoD(String directory)
	{
		//check the directory exist
		File folder = new File(directory == null ? "" : directory);
		Result result = null;
		if (folder.exists() && folder.isDirectory()) {

			File streamsFolder = new File(WEBAPPS_PATH + getScope().getName() + "/streams");
			//check the symbolic links exists and delete it

			deleteSymbolicLink(folder, streamsFolder);

			int deletedRecords = deleteUserVoDByStreamId(folder.getName());
			result = new Result(true, deletedRecords + " of records are deleted");
		}
		else {
			result = new Result(false, directory + " does not exist or it's not a directory");
		}
		return result;
	}

	private int deleteUserVoDByStreamId(String streamId)
	{
		int numberOfDeletedRecords = 0;
		List<VoD> vodList;
		do {
			vodList = getDataStore().getVodList(0, 50, null, null, streamId, null);

			if (vodList != null && !vodList.isEmpty())
			{
				for (VoD voD : vodList) {
					if (VoD.USER_VOD.equals(voD.getType()))
					{
						if (getDataStore().deleteVod(voD.getVodId())) {
							numberOfDeletedRecords++;
						}
					}

				}
			}
		} while(vodList != null && !vodList.isEmpty());

		return numberOfDeletedRecords;
	}

	public int importToDB(File subDirectory, File baseDirectory)
	{
		File[] listOfFiles = subDirectory.listFiles();
		int numberOfFilesImported = 0;
		if (listOfFiles != null)
		{
			for (File file : listOfFiles) {

				String fileExtension = FilenameUtils.getExtension(file.getName());

				if (file.isFile() && ("mp4".equals(fileExtension) || "flv".equals(fileExtension)
						|| "mkv".equals(fileExtension) || "m3u8".equals(fileExtension)))
				{

					long fileSize = file.length();
					long unixTime = System.currentTimeMillis();

					String relativePath = "streams" + File.separator +
							subDirectory.getAbsolutePath().substring(baseDirectory.getAbsolutePath().length() - baseDirectory.getName().length())
							+  File.separator + file.getName();

					String vodId = RandomStringUtils.randomNumeric(24);

					//add base directory folder name as streamId in order to find it easily
					VoD newVod = new VoD(baseDirectory.getName(), baseDirectory.getName(), relativePath, file.getName(), unixTime, 0, Muxer.getDurationInMs(file, null),
							fileSize, VoD.USER_VOD, vodId, null);
					if (getDataStore().addVod(newVod) != null)
					{
						numberOfFilesImported++;
					}

				}
				else if (file.isDirectory())
				{
					numberOfFilesImported += importToDB(file, baseDirectory);
				}
			}
		}
		return numberOfFilesImported;
	}

	/**
	 * Deletes the symbolic link under the streams directory
	 * @param vodDirectory
	 * @param streamsFolder
	 * @return
	 * @throws IOException
	 */
	public boolean deleteSymbolicLink(File vodDirectory, File streamsFolder){
		boolean result = false;
		try {
			if (vodDirectory != null && streamsFolder != null)
			{
				File linkFile = new File(streamsFolder.getAbsolutePath(), vodDirectory.getName());

				if (!streamsFolder.getAbsolutePath().equals(linkFile.getAbsolutePath())
						&&
						Files.isSymbolicLink(linkFile.toPath()))
				{
					Files.delete(linkFile.toPath());
					result = true;
				}
			}
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}

	public String getListenerHookURL(Broadcast broadcast)
	{
		String listenerHookURL = broadcast != null ? broadcast.getListenerHookURL() : null;
		if (StringUtils.isBlank(listenerHookURL))
		{
			listenerHookURL = getAppSettings().getListenerHookURL();
		}
		return listenerHookURL;

	}

	public void closeBroadcast(String streamId) {

		try {
			logger.info("Closing broadcast stream id: {}", streamId);
			Broadcast broadcast = getDataStore().get(streamId);
			if (broadcast != null) {

				if (broadcast.isZombi()) {

					logger.info("Deleting streamId:{} because it's a zombi stream", streamId);
					getDataStore().delete(streamId);
				}
				else {

					BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
					broadcastUpdate.setUpdateTime(System.currentTimeMillis());
					broadcastUpdate.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
					getDataStore().updateBroadcastFields(streamId, broadcastUpdate);
					// This is resets Viewer map in HLS Viewer Stats
					resetHLSStats(streamId);

					// This is resets Viewer map in DASH Viewer Stats
					resetDASHStats(streamId);
				}

				final String listenerHookURL = getListenerHookURL(broadcast);
				if (listenerHookURL != null && !listenerHookURL.isEmpty()) {
					final String name = broadcast.getName();
					final String category = broadcast.getCategory();
					final String metaData = broadcast.getMetaData();
					final String mainTrackId = broadcast.getMainTrackStreamId();
					logger.info("call live stream ended hook for stream:{}",streamId );
					notifyHook(listenerHookURL, streamId, mainTrackId, HOOK_ACTION_END_LIVE_STREAM, name, category, 
							null, null, metaData, null);
				}
				
				PublishEndedEvent publishEndedEvent = new PublishEndedEvent();
				publishEndedEvent.setStreamId(streamId);
				publishEndedEvent.setDurationMs(System.currentTimeMillis() - broadcast.getStartTime());
				publishEndedEvent.setApp(scope.getName());

				LoggerUtils.logAnalyticsFromServer(publishEndedEvent);

				if(StringUtils.isNotBlank(broadcast.getMainTrackStreamId())) {
					updateMainTrackWithRecentlyFinishedBroadcast(broadcast);
				}	

				for (IStreamListener listener : streamListeners) {
					//keep backward compatibility
					listener.streamFinished(broadcast.getStreamId());
					listener.streamFinished(broadcast);
				}
				logger.info("Leaving closeBroadcast for streamId:{}", streamId);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	public static Broadcast saveMainBroadcast(String streamId, String mainTrackId, DataStore dataStore) {
		Broadcast mainBroadcast = new Broadcast();
		try {
			mainBroadcast.setStreamId(mainTrackId);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		mainBroadcast.setZombi(true);
		mainBroadcast.setStatus(BROADCAST_STATUS_BROADCASTING);
		mainBroadcast.getSubTrackStreamIds().add(streamId);
		mainBroadcast.setVirtual(true);
		// don't set  setOriginAdress because it's not a real stream and it causes extra delay  -> mainBroadcast.setOriginAdress(serverSettings.getHostAddress()) 
		mainBroadcast.setStartTime(System.currentTimeMillis());

		return StringUtils.isNotBlank(dataStore.save(mainBroadcast)) ? mainBroadcast : null;
	}

	public static boolean isInstanceAlive(String originAdress, String hostAddress, int httpPort, String appName) {
		if (StringUtils.isBlank(originAdress) || StringUtils.equals(originAdress, hostAddress)) {
			return true;
		}

		String url = "http://" + originAdress + ":" + httpPort + "/" + appName;

		boolean result = isEndpointReachable(url);
		if (!result) {
			logger.warn("Instance with origin address {} is not reachable through its app:{}", originAdress, appName);
		}
		return result;
	}

	public static boolean isEndpointReachable(String endpoint) {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(endpoint))
				.method("HEAD", HttpRequest.BodyPublishers.noBody()) // HEAD request
				.timeout(java.time.Duration.ofSeconds(1))
				.build();


		try {
			HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
		} catch (InterruptedException e) {
			logger.error("InterruptedException Enpoint is not reachable: {}, {}", endpoint, ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
			return false;
		} catch (Exception e) {
			logger.error("Enpoint is not reachable: {}, {}", endpoint, ExceptionUtils.getStackTrace(e));
			return false;
		}
		return true;


	}

	/**
	 * If multiple threads enter the method at the same time, the following method does not work correctly.
	 * So we have made it synchronized
	 *
	 * It fixes the bug that sometimes main track(room) is not deleted in the video conferences
	 *
	 * mekya
	 *
	 * @param finishedBroadcast
	 */
	public synchronized void updateMainTrackWithRecentlyFinishedBroadcast(Broadcast finishedBroadcast) 
	{
		Broadcast mainBroadcast = getDataStore().get(finishedBroadcast.getMainTrackStreamId());
		logger.info("updating main track:{} status with recently finished broadcast:{}", finishedBroadcast.getMainTrackStreamId(), finishedBroadcast.getStreamId());

		if (mainBroadcast != null) {

			mainBroadcast.getSubTrackStreamIds().remove(finishedBroadcast.getStreamId());

			long activeSubtracksCount = getDataStore().getActiveSubtracksCount(mainBroadcast.getStreamId(), null);

			if (activeSubtracksCount == 0) {

				if (mainBroadcast.isZombi()) {
					logger.info("Deleting main track streamId:{} because it's a zombi stream and there is no activeSubtrack", mainBroadcast.getStreamId());
					getDataStore().delete(mainBroadcast.getStreamId());
				}
				else {
					logger.info("Update main track:{} status to finished ", finishedBroadcast.getMainTrackStreamId());
					BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
					broadcastUpdate.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED);

					getDataStore().updateBroadcastFields(mainBroadcast.getStreamId(), broadcastUpdate);
				}
				notifyNoActiveSubtracksLeftInMainTrack(mainBroadcast);
			}
			else {
				logger.info("There are {} active subtracks in the main track:{} status to finished. Just removing the subtrack:{}", activeSubtracksCount, finishedBroadcast.getMainTrackStreamId(), finishedBroadcast.getStreamId());
				BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
				broadcastUpdate.setSubTrackStreamIds(mainBroadcast.getSubTrackStreamIds());

				getDataStore().updateBroadcastFields(mainBroadcast.getStreamId(), broadcastUpdate);
			}
		}
		else {
			logger.warn("Maintrack is null while removing subtrack from maintrack for streamId:{} maintrackId:{}", finishedBroadcast.getStreamId(), finishedBroadcast.getMainTrackStreamId());
		}



		leftTheRoom(finishedBroadcast.getMainTrackStreamId(), finishedBroadcast.getStreamId());

	}

	public void resetHLSStats(String streamId) {
		if (scope.getContext().getApplicationContext().containsBean(HlsViewerStats.BEAN_NAME)) {
			HlsViewerStats hlsViewerStats = (HlsViewerStats) scope.getContext().getApplicationContext().getBean(HlsViewerStats.BEAN_NAME);
			hlsViewerStats.resetViewerMap(streamId, ViewerStats.HLS_TYPE);
		}
	}

	public void resetDASHStats(String streamId) {
		if (scope.getContext().getApplicationContext().containsBean(DashViewerStats.BEAN_NAME)) {
			DashViewerStats dashViewerStats = (DashViewerStats) scope.getContext().getApplicationContext().getBean(DashViewerStats.BEAN_NAME);
			dashViewerStats.resetViewerMap(streamId, ViewerStats.DASH_TYPE);
		}
	}

	@Override
	public void streamPlayItemPlay(ISubscriberStream stream, IPlayItem item, boolean isLive) {
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(item.getName(), true));
	}
	@Override
	public void streamPlayItemStop(ISubscriberStream stream, IPlayItem item) {
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(item.getName(), false));
	}

	@Override
	public void streamSubscriberClose(ISubscriberStream stream) {
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(stream.getBroadcastStreamPublishName(), false));
	}

	@Override
	public void startPublish(String streamId, long absoluteStartTimeMs, String publishType) {
		vertx.executeBlocking( () -> {
			try {

				Broadcast broadcast = updateBroadcastStatus(streamId, absoluteStartTimeMs, publishType, getDataStore().get(streamId));

				final String listenerHookURL = getListenerHookURL(broadcast);
				if (listenerHookURL != null && !listenerHookURL.isEmpty())
				{
					final String name = broadcast.getName();
					final String category = broadcast.getCategory();
					final String metaData = broadcast.getMetaData();
					final String mainTrackId = broadcast.getMainTrackStreamId();

					logger.info("Call live stream started hook for stream:{}",streamId );
					notifyHook(listenerHookURL, streamId, mainTrackId, HOOK_ACTION_START_LIVE_STREAM, name, category,
							null, null, metaData, null);
				}

				int ingestingStreamLimit = appSettings.getIngestingStreamLimit();

				long activeBroadcastNumber = dataStore.getActiveBroadcastCount();
				if (ingestingStreamLimit != -1 && activeBroadcastNumber > ingestingStreamLimit)
				{
					logger.info("Active broadcast count({}) is more than ingesting stream limit:{} so stopping broadcast:{}", activeBroadcastNumber, ingestingStreamLimit, broadcast.getStreamId());
					stopStreaming(broadcast, true);
				}

				for (IStreamListener listener : streamListeners) {
					listener.streamStarted(broadcast.getStreamId());
					listener.streamStarted(broadcast);
				}

				long videoHeight = 0;
				long videoWidth = 0;
				String videoCodecName=null;
				String audioCodecName=null;
				MuxAdaptor adaptor = getMuxAdaptor(streamId);
				if(adaptor!=null) {
					if(adaptor.isEnableVideo()) {
						AVCodecParameters videoCodecPar = adaptor.getVideoCodecParameters();
						videoWidth = videoCodecPar.width();
						videoHeight = videoCodecPar.height();
						videoCodecName = avcodec_get_name(videoCodecPar.codec_id()).getString();
					}
					if(adaptor.isEnableAudio()) {
						audioCodecName = avcodec_get_name(adaptor.getAudioCodecParameters().codec_id()).getString();
					}
				}

				PublishStartedEvent event = new PublishStartedEvent();
				event.setStreamId(streamId);
				event.setProtocol(publishType);
				event.setHeight((int) videoHeight);
				event.setWidth((int) videoWidth);
				event.setVideoCodec(videoCodecName);
				event.setAudioCodec(audioCodecName);
				event.setApp(scope.getName());

				LoggerUtils.logAnalyticsFromServer(event);
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			return null;

		}, false);


		if (absoluteStartTimeMs == 0)
		{
			vertx.setTimer(2000, h ->
			{
				IBroadcastStream broadcastStream = getBroadcastStream(getScope(), streamId);
				if (broadcastStream instanceof ClientBroadcastStream)
				{
					long absoluteStarTime = ((ClientBroadcastStream)broadcastStream).getAbsoluteStartTimeMs();
					if (absoluteStarTime != 0)
					{
						Broadcast broadcast = getDataStore().get(streamId);
						if (broadcast != null)
						{
							broadcast.setAbsoluteStartTimeMs(absoluteStarTime);

							getDataStore().save(broadcast);
							logger.info("Updating broadcast absolute time {} ms for stream:{}", absoluteStarTime, streamId);
						}
						else {
							logger.info("Broadcast is not available in the database to update the absolute start time for stream:{}", streamId);
						}

					}
					else {
						logger.info("Broadcast absolute time is not available for stream:{}", streamId);
					}

				}
			});
		}

		logger.info("start publish leaved for stream:{}", streamId);
	}



	public Broadcast updateBroadcastStatus(String streamId, long absoluteStartTimeMs, String publishType, Broadcast broadcast) {
		return updateBroadcastStatus(streamId, absoluteStartTimeMs, publishType, broadcast, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
	}


	public Broadcast updateBroadcastStatus(String streamId, long absoluteStartTimeMs, String publishType, Broadcast broadcast, String status) {
		if (broadcast == null)
		{

			logger.info("Saving zombi broadast to data store with streamId:{}", streamId);
			broadcast = saveUndefinedBroadcast(streamId, null, this, status, absoluteStartTimeMs, publishType, "", "", "");
		}
		else {

			BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
			broadcastUpdate.setStatus(status);
			long now = System.currentTimeMillis();
			broadcastUpdate.setStartTime(now);
			broadcastUpdate.setUpdateTime(now);
			broadcastUpdate.setOriginAdress(getServerSettings().getHostAddress());
			broadcastUpdate.setWebRTCViewerCount(0);
			broadcastUpdate.setHlsViewerCount(0);
			broadcastUpdate.setDashViewerCount(0);
			broadcastUpdate.setPublishType(publishType);
			//updateBroadcastFields just updates broadcast with the updated fields. No need to give real object
			boolean result = getDataStore().updateBroadcastFields(broadcast.getStreamId(), broadcastUpdate);

			logger.info(" Status of stream {} is set to {} with result: {}", broadcast.getStreamId(), status, result);
		}
		return broadcast;
	}

	public ServerSettings getServerSettings()
	{
		if (serverSettings == null) {
			serverSettings = (ServerSettings)getScope().getContext().getApplicationContext().getBean(ServerSettings.BEAN_NAME);
		}
		return serverSettings;
	}


	public static Broadcast saveUndefinedBroadcast(String streamId, String streamName, AntMediaApplicationAdapter appAdapter, String streamStatus, long absoluteStartTimeMs, String publishType, String mainTrackStreamId, String metaData, String role) {
		Broadcast newBroadcast = new Broadcast();
		long now = System.currentTimeMillis();
		newBroadcast.setDate(now);
		newBroadcast.setStartTime(now);
		newBroadcast.setUpdateTime(now);
		newBroadcast.setZombi(true);
		newBroadcast.setName(streamName);
		newBroadcast.setMainTrackStreamId(mainTrackStreamId);
		newBroadcast.setMetaData(metaData);
		newBroadcast.setRole(role);
		try {
			newBroadcast.setStreamId(streamId);
			newBroadcast.setPublishType(publishType);

			return RestServiceBase.saveBroadcast(newBroadcast,
					streamStatus, appAdapter.getScope().getName(), appAdapter.getDataStore(),
					appAdapter.getAppSettings().getListenerHookURL(), appAdapter.getServerSettings(), absoluteStartTimeMs);
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return null;
	}

	@Override
	@Deprecated
	public void muxingFinished(String streamId, File File, long startTime, long duration, int resolution,
			String previewFilePath, String vodId) 
	{
		muxingFinished(getDataStore().get(streamId), streamId, File, startTime, duration, resolution, previewFilePath, vodId);
	}

	@Override
	public void muxingFinished(Broadcast broadcast, String streamId, File file, long startTime, long duration, int resolution, String previewFilePath, String vodId) {
		
		String listenerHookURL = null;
		String streamName = file.getName();
		String description = null;
		String metadata = null;
		String longitude = null;
		String latitude = null;
		String altitude = null;

		if (broadcast != null) {
			listenerHookURL = broadcast.getListenerHookURL();
			if(StringUtils.isNotBlank(broadcast.getName())){
				streamName =  resolution != 0 ? broadcast.getName() + " (" + resolution + "p)" : broadcast.getName();
			}
			description = broadcast.getDescription();
			metadata = broadcast.getMetaData();
			longitude = broadcast.getLongitude();
			latitude = broadcast.getLatitude();
			altitude = broadcast.getAltitude();
		}
		else {
			logger.error("Broadcast is null for muxingFinished for stream: {} it's not supposed to happen", streamId);
		}
		
		String vodName = file.getName();
		String filePath = file.getPath();
		long fileSize = file.length();
		long systemTime = System.currentTimeMillis();

		String relativePath = getRelativePath(filePath);

		logger.info("muxing finished for stream: {} with file: {} and duration:{}", streamId, file, duration);

		//We need to get the webhook url explicitly because broadcast may be deleted here
		if (StringUtils.isBlank(listenerHookURL)) {
			// if hook URL is not defined for stream specific, then try to get common one from app
			listenerHookURL = appSettings.getListenerHookURL();
		}

		if (StringUtils.isBlank(vodId)) {
			vodId = RandomStringUtils.randomAlphanumeric(24);
		}

		VoD newVod = new VoD(streamName, streamId, relativePath, vodName, systemTime, startTime, duration, fileSize, VoD.STREAM_VOD, vodId, previewFilePath);
		newVod.setDescription(description);
		newVod.setMetadata(metadata);
		newVod.setLongitude(longitude);
		newVod.setLatitude(latitude);
		newVod.setAltitude(altitude);
		


		if (getDataStore().addVod(newVod) == null) {
			logger.warn("Stream vod with stream id {} cannot be added to data store", streamId);
		}

		int index;

		//HOOK_ACTION_VOD_READY is called only the listenerHookURL is defined either for stream or in AppSettings
		if (StringUtils.isNotBlank(listenerHookURL) &&
				((index = vodName.lastIndexOf(".mp4")) != -1)
				|| ((index = vodName.lastIndexOf(".webm")) != -1) )
		{
			final String baseName = vodName.substring(0, index);
			logger.info("Setting timer for calling vod ready hook for stream:{}", streamId);
			notifyHook(listenerHookURL, streamId, null, HOOK_ACTION_VOD_READY, null, null, baseName, vodId, metadata, null);
		}

		String muxerFinishScript = appSettings.getMuxerFinishScript();
		if (muxerFinishScript != null && !muxerFinishScript.isEmpty()) {
			runScript(muxerFinishScript + "  " + file.getAbsolutePath());
		}


	}

	public void notifyFirstActiveSubtrackInMainTrack(Broadcast mainTrack, String subtrackId) 
	{
		final String listenerHookURL = getListenerHookURL(mainTrack);
		if(listenerHookURL == null || listenerHookURL.isEmpty()){
			return;
		}
		final String name = mainTrack.getName();
		final String category = mainTrack.getCategory();
		notifyHook(listenerHookURL, subtrackId, mainTrack.getStreamId(), HOOK_ACTION_FIRST_ACTIVE_SUBTRACK_ADDED_IN_THE_MAINTRACK, name, category, null, null, null, null);
	}

	public void notifyNoActiveSubtracksLeftInMainTrack(Broadcast mainTrack) 
	{
		final String listenerHookURL = getListenerHookURL(mainTrack);

		if (StringUtils.isNotBlank(listenerHookURL)) {
			final String name = mainTrack.getName();
			final String category = mainTrack.getCategory();
			notifyHook(listenerHookURL, mainTrack.getStreamId(), null, HOOK_ACTION_NO_ACTIVE_SUBTRACKS_LEFT_IN_THE_MAINTRACK, name, category, null, null, null, null);
		}
	}
	public void runScript(String scriptFile) {
		vertx.executeBlocking(() -> {
			try {
				logger.info("running script: {}", scriptFile);
				Process exec = Runtime.getRuntime().exec(scriptFile);
				int result = exec.waitFor();

				logger.info("completing script: {} with return value {}", scriptFile, result);
			} catch (IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			} catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
			}

			return null;

		}, false);
	}

	public static String getRelativePath(String filePath){
		StringBuilder relativePath= new StringBuilder();
		String[] subDirs = filePath.split(STREAMS);
		if(subDirs.length == 2)
			relativePath = new StringBuilder(STREAMS + subDirs[1]);
		else{
			for(int i=1;i<subDirs.length;i++){
				relativePath.append(STREAMS).append(subDirs[i]);
			}
		}
		return relativePath.toString();
	}


	public void sendWebHook(String id, String mainTrackId, String action, String streamName, String category,
			String vodName, String vodId, String metadata, String subscriberId)  
	{
		String listenerHookURL = getListenerHookURL(getDataStore().get(id));	
		if (StringUtils.isNotBlank(listenerHookURL)) 
		{
			notifyHook(listenerHookURL, id, mainTrackId, action, streamName, category, vodName, vodId, metadata, subscriberId);
		} 
	}

	/**
	 * Notify hook with parameters below
	 *
	 * @param streamName, name of the stream. It is not the name of the file. It is just
	 *                    a user friendly name
	 * @param category,   category of the stream
	 * @param url         is the url of the service to be called
	 * @param id          is the stream id that is unique for each stream
	 * @param mainTrackId mainTrackId(roomId) of the stream
	 * @param action      is the name of the action to be notified, it has values such
	 *                    as {@link #HOOK_ACTION_END_LIVE_STREAM}
	 *                    {@link #HOOK_ACTION_START_LIVE_STREAM}
	 * @param vodName     name of the vod
	 * @param vodId       id of the vod in the datastore
	 * @return
	 */
	public void notifyHook(@NotNull String url, String id, String mainTrackId, String action, String streamName, String category,
			String vodName, String vodId, String metadata, String subscriberId) {

		//Previously, we're using runOnContext and switched to executeBlocking without ordered because this operation may take some time
		//and we don't want to block the event loop, the disadvantage of this approach is that the order of the operations may not be guaranteed 
		//then it's meaningful to check the timestamp of the event in the webhook - mekya


		vertx.executeBlocking(() -> {
			logger.info("Running notify hook url:{} stream id: {} mainTrackId:{} action:{} vod name:{} vod id:{}", url, id, mainTrackId, action, vodName, vodId);

			Map<String, Object> variables = new HashMap<>();

			variables.put("id", id);
			variables.put("action", action);


			putToMap("streamName", streamName, variables);
			putToMap("category", category, variables);
			putToMap("vodName", vodName, variables);
			putToMap("vodId", vodId, variables);
			putToMap("mainTrackId", mainTrackId, variables);
			putToMap("roomId", mainTrackId, variables);
			putToMap("subscriberId", subscriberId, variables);

			if (StringUtils.isNotBlank(metadata)) {
				Object metaDataJsonObj = null;
				try {
					JSONParser jsonParser = new JSONParser();
					metaDataJsonObj = (JSONObject) jsonParser.parse(metadata);
				} catch (ParseException parseException) {
					metaDataJsonObj = metadata;
				}
				putToMap("metadata", metaDataJsonObj, variables);

			}
			putToMap("timestamp", String.valueOf(System.currentTimeMillis()), variables);


			try {
				sendPOST(url, variables, appSettings.getWebhookRetryCount(), appSettings.getWebhookContentType());
			} catch (Exception exception) {
				logger.error(ExceptionUtils.getStackTrace(exception));
			}

			return null;

		}, false);
	}

	@Override
	public void notifyWebhookForStreamStatus(Broadcast broadcast, int width, int height, long totalByteReceived,
			int inputQueueSize,  int encodingQueueSize, int dropFrameCountInEncoding, int dropPacketCountInIngestion, double speed) {
		String listenerHookURL = getListenerHookURL(broadcast);

		if (StringUtils.isNotBlank(listenerHookURL)) {

			vertx.executeBlocking(() -> 
			{
				Map<String, Object> variables = new HashMap<>();

				variables.put("id", broadcast.getStreamId());
				variables.put("action", HOOK_ACTION_STREAM_STATUS);
				variables.put("width", width);
				variables.put("height", height);
				variables.put("totalByteReceived", totalByteReceived);
				variables.put("inputQueueSize", inputQueueSize);
				variables.put("speed", speed);
				variables.put("timestamp", System.currentTimeMillis());
				variables.put("streamName",broadcast.getName());
				variables.put("encodingQueueSize", encodingQueueSize);
				variables.put("dropFrameCountInEncoding", dropFrameCountInEncoding);
				variables.put("dropPacketCountInIngestion", dropPacketCountInIngestion);

				try {
					sendPOST(listenerHookURL, variables, appSettings.getWebhookRetryCount(), appSettings.getWebhookContentType());
				} catch (Exception exception) {
					logger.error(ExceptionUtils.getStackTrace(exception));
				}

				return null;
			}, false);

		}
	}

	private void putToMap(String keyName, Object keyValue, Map<String, Object> map) {
		if (keyValue != null && StringUtils.isNotBlank(keyValue.toString())) {
			map.put(keyName, keyValue);
		}
	}

	public boolean sendClusterPost(String url, String clusterCommunicationToken) 
	{
		
		return callClusterRestMethod(url, clusterCommunicationToken);
	}
	
	public boolean callClusterRestMethod(String url, String clusterCommunicationToken) 
	{

		boolean result = false;
		try (CloseableHttpClient httpClient = getHttpClient()) 
		{
			HttpPost request = new HttpPost(url);

			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectTimeout(CLUSTER_POST_TIMEOUT_MS)
					.setConnectionRequestTimeout(CLUSTER_POST_TIMEOUT_MS)
					.setSocketTimeout(CLUSTER_POST_TIMEOUT_MS)
					.build();
			request.setConfig(requestConfig);

			request.setHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION, clusterCommunicationToken);

			try (CloseableHttpResponse httpResponse = httpClient.execute(request)) 
			{
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				logger.info("Cluster POST Response Status: {} for url:{}", statusCode, request.getURI());
				if (statusCode == HttpStatus.SC_OK) {
					result = true;
				} 
			}
		} 
		catch (Exception e) 
		{
			//Cover all exceptions
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}

	public void trySendClusterPostWithDelay(String url, String clusterCommunicationToken, int retryAttempts, CompletableFuture<Boolean> future) {
		vertx.setTimer(appSettings.getWebhookRetryDelay(), timerId -> {

			vertx.executeBlocking(() -> {

				boolean result = sendClusterPost(url, clusterCommunicationToken);

				if (!result && retryAttempts >= 1) {
					trySendClusterPostWithDelay(url, clusterCommunicationToken, retryAttempts - 1, future);
				}
				else {

					future.complete(result);
					if (result) {
						logger.info("Cluster POST is successful another node for url:{}", url);
					}
					else {
						logger.info("Cluster POST is not successful to another node for url:{} and no more retry attempts left",
								url);
					}
				}
				return null;

			}, false);


		});
	}

	/**
	 *
	 * @param url
	 * @param variables
	 * @param retryAttempts
	 * @param sendType the type of the entity to be sent. It can be either "application/x-www-form-urlencoded" or "application/json"
	 */
	public void sendPOST(String url, Map<String, Object> variables, int retryAttempts, String contentType) {
		logger.info("Sending POST request to {}", url);
		try (CloseableHttpClient httpClient = getHttpClient()) {
			HttpPost httpPost = new HttpPost(url);
			RequestConfig requestConfig = RequestConfig.custom()
					.setConnectTimeout(2000)
					.setConnectionRequestTimeout(2000)
					.setSocketTimeout(2000)
					.build();
			httpPost.setConfig(requestConfig);


			if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType))
			{
				List<NameValuePair> urlParameters = new ArrayList<>();
				Set<Entry<String, Object>> entrySet = variables.entrySet();
				for (Entry<String, Object> entry : entrySet)
				{
					urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
				}

				HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
				httpPost.setEntity(postParams);
			}
			else
			{
				JSONObject hookPayload = new JSONObject(variables);
				httpPost.setEntity(new StringEntity(hookPayload.toString(), ContentType.APPLICATION_JSON));
			}

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
				int statusCode = httpResponse.getStatusLine().getStatusCode();
				logger.info("POST Response Status: {}", statusCode);

				if (statusCode != HttpStatus.SC_OK)
				{
					if (retryAttempts >= 1)
					{
						logger.info("Retry attempt for POST in {} milliseconds due to non-200 response: {}", appSettings.getWebhookRetryDelay(), statusCode);
						retrySendPostWithDelay(url, variables, retryAttempts - 1, contentType);
					} else if (appSettings.getWebhookRetryCount() != 0)
					{
						logger.info("Stopping sending POST because no more retry attempts left. Giving up.");
					}
				}
			}
		} catch (IOException e) {
			if (retryAttempts >= 1)
			{
				logger.info("Retry attempt for POST in {} milliseconds due to IO exception: {}", appSettings.getWebhookRetryDelay(), ExceptionUtils.getStackTrace(e));
				retrySendPostWithDelay(url, variables, retryAttempts - 1, contentType);
			}
			else if (appSettings.getWebhookRetryCount() != 0)
			{
				logger.info("Stopping sending POST because no more retry attempts left. Giving up.");
			}
		}
	}

	public void retrySendPostWithDelay(String url, Map<String, Object> variables, int retryAttempts, String contentType) {
		vertx.setTimer(appSettings.getWebhookRetryDelay(), timerId -> {
			sendPOST(url, variables, retryAttempts, contentType);
		});
	}

	public CloseableHttpClient getHttpClient() {
		return HttpClients.createDefault();
	}

	public List<IStreamPublishSecurity> getStreamPublishSecurityList() {
		return streamPublishSecurityList;
	}

	public void setStreamPublishSecurityList(List<IStreamPublishSecurity> streamPublishSecurityList) {
		this.streamPublishSecurityList = streamPublishSecurityList;
	}

	@Override
	public AppSettings getAppSettings() {
		return appSettings;
	}


	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}

	public StreamAcceptFilter getStreamAcceptFilter() {
		return streamAcceptFilter;
	}


	public void setStreamAcceptFilter(StreamAcceptFilter streamAcceptFilter) {
		this.streamAcceptFilter = streamAcceptFilter;
	}

	@Override
	public boolean isValidStreamParameters(int width, int height, int fps, int bitrate, String streamId) {
		return streamAcceptFilter.isValidStreamParameters(width, height, fps, bitrate, streamId);
	}
	
	/**
	 * Important information: Status field of Broadcast class checks the update time to report the status is broadcasting or not.
	 * {@link Broadcast#getStatus()}
	*/
	public static final boolean isStreaming(String status) {
		
		return (IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(status)
						||	IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING.equals(status));
	}

	public Result startStreaming(Broadcast broadcast) {
		Result result = new Result(false);

		// Check resource availability first
		if (!getStatsCollector().enoughResource()) {
			result.setMessage("Not enough resource on server to start streaming.");
			return result;
		}

		// Handle streaming for IP camera, stream source, and VOD
		if (broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.VOD)) {

			if (isClusterMode()) {
				String broadcastOriginAddress = broadcast.getOriginAdress();

				// Handle null or empty origin address
				if (StringUtils.isBlank(broadcastOriginAddress)) {
					result = getStreamFetcherManager().startStreaming(broadcast);
					result.setMessage("Broadcasts origin address is not set. " +
							getServerSettings().getHostAddress() + " will fetch the stream.");
					return result;
				}

				// Handle matching origin address
				if (broadcastOriginAddress.equals(getServerSettings().getHostAddress())) {
					result = getStreamFetcherManager().startStreaming(broadcast);
					return result;
				}

				// Forward request to origin server
				forwardStartStreaming(broadcast);
				result.setSuccess(true);
				result.setErrorId(FETCH_REQUEST_REDIRECTED_TO_ORIGIN);
				result.setMessage("Request forwarded to origin server for fetching. " +
						"Check broadcast status for final confirmation.");
				return result;
			} 
			else {
				result = getStreamFetcherManager().startStreaming(broadcast);
			}
		}
		// Handle playlist type
		else if (broadcast.getType().equals(AntMediaApplicationAdapter.PLAY_LIST)) {
			result = getStreamFetcherManager().startPlaylist(broadcast);
		}
		// Handle unsupported broadcast types
		else {
			logger.info("Broadcast type is not supported for startStreaming:{} streamId:{}",
					broadcast.getType(), broadcast.getStreamId());
			result.setMessage("Broadcast type is not supported. It can be StreamSource, IP Camera, VOD, Playlist");
		}

		return result;
	}

	public void forwardStartStreaming(Broadcast broadcast) {
		String jwtToken = JWTFilter.generateJwtToken(
				getAppSettings().getClusterCommunicationKey(),
				System.currentTimeMillis() + 5000
				);

		String restRouteOfNode = "http://" + broadcast.getOriginAdress() + ":" +
				getServerSettings().getDefaultHttpPort() +
				File.separator + getAppSettings().getAppName() +
				File.separator + "rest" +
				File.separator + "v2" +
				File.separator + "broadcasts" +
				File.separator + broadcast.getStreamId() +
				File.separator + "start";


		CompletableFuture<Boolean> future = new CompletableFuture<>();

		trySendClusterPostWithDelay(restRouteOfNode, jwtToken, CLUSTER_POST_RETRY_ATTEMPT_COUNT, future);


		future.thenAccept(success -> {
			if (success) {
				logger.info("Cluster POST redirection to {} succeeded", restRouteOfNode);
			} else {
				logger.info("Cluster POST redirection to {} failed. Local node {} will fetch the stream. ", restRouteOfNode, getServerSettings().getHostAddress());
				getStreamFetcherManager().startStreaming(broadcast);
			}
		})
		.exceptionally(ex -> {
			logger.error("Cluster POST encountered an exception: {}", ExceptionUtils.getStackTrace(ex));
			getStreamFetcherManager().startStreaming(broadcast);
			return null;
		});
	}

	public Result stopStreaming(Broadcast broadcast, boolean stopSubtracks)
	{
		Result result = new Result(false);
		logger.info("stopStreaming is called for stream:{}", broadcast.getStreamId());
		if (broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.VOD))
		{
			result = getStreamFetcherManager().stopStreaming(broadcast.getStreamId());
		}
		else if (broadcast.getType().equals(AntMediaApplicationAdapter.PLAY_LIST))
		{
			result = getStreamFetcherManager().stopPlayList(broadcast.getStreamId());
		}
		else if (broadcast.getType().equals(AntMediaApplicationAdapter.LIVE_STREAM))
		{

			IBroadcastStream broadcastStream = getBroadcastStream(getScope(), broadcast.getStreamId());
			if (broadcastStream != null)
			{

				IStreamCapableConnection connection = ((IClientBroadcastStream) broadcastStream).getConnection();
				if (connection != null) {
					connection.close();
				}
				else {
					logger.warn("Connection is null. It should not happen for stream: {}. Analyze the logs", broadcast.getStreamId());
				}
				result.setSuccess(true);
			}
		}
		return result;
	}
	
	public OnvifCamera getOnvifCamera(String id) {
		OnvifCamera onvifCamera = onvifCameraList.get(id);
		if (onvifCamera == null) {

			Broadcast camera = getDataStore().get(id);
			if (camera != null) {
				onvifCamera = new OnvifCamera();
				onvifCamera.connect(camera.getIpAddr(), camera.getUsername(), camera.getPassword());

				onvifCameraList.put(id, onvifCamera);
			}
		}
		return onvifCamera;
	}

	public StreamFetcherManager getStreamFetcherManager() {
		if(streamFetcherManager == null) {
			streamFetcherManager = new StreamFetcherManager(vertx, getDataStore(), getScope());
		}
		return streamFetcherManager;
	}

	public void setStreamFetcherManager(StreamFetcherManager streamFetcherManager) {
		this.streamFetcherManager = streamFetcherManager;
	}


	@Override
	public void setQualityParameters(String streamId, PublishStatsEvent stats, long currentTimeMillis) {
		vertx.runOnContext(h -> {

			Broadcast broadcastLocal = getDataStore().get(streamId);
			if (broadcastLocal != null)
			{
				

				BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
				broadcastUpdate.setSpeed(stats.getSpeed());	
				broadcastUpdate.setPendingPacketSize(stats.getInputQueueSize());
				broadcastUpdate.setUpdateTime(currentTimeMillis);
				long elapsedTimeMs = System.currentTimeMillis() - broadcastLocal.getStartTime();
				broadcastUpdate.setDuration(elapsedTimeMs);
				long elapsedSeconds = elapsedTimeMs / 1000;
				if (elapsedSeconds > 0 ) { //protect by zero division
					long bitrate = (stats.getTotalByteReceived()/elapsedSeconds)*8;
					broadcastUpdate.setBitrate(bitrate);
				}

				

				broadcastUpdate.setWidth(stats.getWidth());
				broadcastUpdate.setHeight(stats.getHeight());
				
				broadcastUpdate.setEncoderQueueSize(stats.getEncodingQueueSize());		
				broadcastUpdate.setDropPacketCountInIngestion(stats.getDroppedPacketCountInIngestion());
				broadcastUpdate.setDropFrameCountInEncoding(stats.getDroppedFrameCountInEncoding());
				broadcastUpdate.setPacketLostRatio(stats.getPacketLostRatio());
				broadcastUpdate.setPacketsLost(stats.getPacketsLost());
                broadcastUpdate.setJitterMs(stats.getJitterMs());
                broadcastUpdate.setRttMs(stats.getRoundTripTimeMs());	
                
                broadcastUpdate.setRemoteIp(stats.getRemoteIp());
                broadcastUpdate.setUserAgent(stats.getUserAgent()); 
                broadcastUpdate.setReceivedBytes(stats.getTotalByteReceived());

				getDataStore().updateBroadcastFields(streamId, broadcastUpdate);

				ViewerCountEvent viewerCountEvent = new ViewerCountEvent();
				viewerCountEvent.setApp(getScope().getName());
				viewerCountEvent.setStreamId(streamId);
				viewerCountEvent.setDashViewerCount(broadcastLocal.getDashViewerCount());
				viewerCountEvent.setHlsViewerCount(broadcastLocal.getHlsViewerCount());
				viewerCountEvent.setWebRTCViewerCount(broadcastLocal.getWebRTCViewerCount());

				LoggerUtils.logAnalyticsFromServer(viewerCountEvent);
				
				logger.debug("update source quality for stream:{} width:{} height:{} bitrate:{} input queue size:{} encoding queue size:{} packetsLost:{} packetLostRatio:{} jitter:{} rtt:{}",
						
						streamId, stats.getWidth(), stats.getHeight(), broadcastUpdate.getBitrate(), stats.getInputQueueSize(), stats.getEncodingQueueSize(),
						stats.getPacketsLost(), stats.getPacketLostRatio(), stats.getJitterMs(), stats.getRoundTripTimeMs());
			}

		});
		
		
	}

	@Override
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize, long updateTimeMs) {
		PublishStatsEvent stats = new PublishStatsEvent();
		stats.setSpeed(speed);
		stats.setInputQueueSize(pendingPacketSize);
		
		setQualityParameters(id, stats, updateTimeMs);
	}

	@Override
	public DataStore getDataStore() {
		//vertx should be initialized before calling this method
		if(dataStore == null)
		{
			dataStore = dataStoreFactory.getDataStore();
		}
		return dataStore;
	}

	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	/**
	 * This setter for test cases
	 * @param vertx
	 */
	public void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	public void closeRTMPStreams()
	{
		Collection<MuxAdaptor> adaptors = getMuxAdaptors();
		synchronized (adaptors)
		{
			for (MuxAdaptor adaptor : adaptors) {
				if(adaptor.getBroadcast().getType().equals(AntMediaApplicationAdapter.LIVE_STREAM)) {

					ClientBroadcastStream broadcastStream = adaptor.getBroadcastStream();
					if (broadcastStream != null) {
						broadcastStream.stop();
					}
					adaptor.stop(true);
				}
			}
		}
	}

	public void closeStreamFetchers() {
		if (streamFetcherManager != null) {
			Map<String, StreamFetcher> fetchers = streamFetcherManager.getStreamFetcherList();
			for (StreamFetcher streamFetcher : fetchers.values()) {
				streamFetcher.stopStream();
				//it may be also play list so stop it if it's 
				getStreamFetcherManager().stopPlayList(streamFetcher.getStreamId());
			}
			fetchers.clear();

		}
	}

	public void waitUntilLiveStreamsStopped() {
		int i = 0;
		int waitPeriod = 500;
		boolean everythingHasStopped = true;
		while(getDataStore().getLocalLiveBroadcastCount(getServerSettings().getHostAddress()) > 0) {
			try {
				if (i > 3) {
					logger.warn("Waiting for active broadcasts number decrease to zero for app: {}"
							+ " total wait time: {}ms", getScope().getName(), i*waitPeriod);
				}
				if (i>10) {
					logger.error("Not all live streams're stopped gracefully. It will update the streams' status to finished_unexpectedly");
					everythingHasStopped = false;
					break;
				}
				i++;
				Thread.sleep(waitPeriod);

			} catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
			}
		}

		if (!everythingHasStopped)
		{
			List<Broadcast> localLiveBroadcasts = getDataStore().getLocalLiveBroadcasts(getServerSettings().getHostAddress());
			List<String> streamIdList = new ArrayList<>();
			for (Broadcast broadcast : localLiveBroadcasts) {
				//if it's not closed properly, let's set the state to failed
				BroadcastUpdate broadcastUpdate = new BroadcastUpdate();

				broadcastUpdate.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);
				broadcastUpdate.setPlayListStatus(IAntMediaStreamHandler.BROADCAST_STATUS_TERMINATED_UNEXPECTEDLY);
				broadcastUpdate.setWebRTCViewerCount(0);
				broadcastUpdate.setHlsViewerCount(0);
				broadcastUpdate.setDashViewerCount(0);

				getDataStore().updateBroadcastFields(broadcast.getStreamId(), broadcastUpdate);
				streamIdList.add(broadcast.getStreamId());
			}

			if (logger.isWarnEnabled()) {
				logger.warn("Following streams status set to finished explicitly because they're not stopped properly: {}", String.join(",", streamIdList));
			}
		}


	}


	public void waitUntilThreadsStop() {
		int i = 0;
		int waitPeriod = 1000;
		int activeVertxThreadCount = 0;
		while((activeVertxThreadCount = getActiveVertxThreadCount()) > 0) {
			try {
				if (i > 3) {
					logger.warn("Waiting for active vertx threads count({}) decrease to zero for app: {}"
							+ " total wait time: {}ms", activeVertxThreadCount, getScope().getName(), i*waitPeriod);
				}
				if (i>10) {
					logger.error("*********************************************************************");
					logger.error("Not all active vertx threads are stopped. It's even breaking the loop");
					logger.error("*********************************************************************");
					break;
				}
				i++;
				Thread.sleep(waitPeriod);

			} catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
			}
		}
	}

	private int getActiveVertxThreadCount() {
		int activeVertexThreadCount = 0;
		try {
			MetricsService metricsService = MetricsService.create(vertx);
			String activeThreadKey = "vertx.pools.worker.vert.x-worker-thread.in-use";
			JsonObject metrics = metricsService.getMetricsSnapshot(activeThreadKey);
			if (metrics != null) {
				activeVertexThreadCount = metrics.getJsonObject(activeThreadKey).getInteger("count");
			}
		}
		catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return activeVertexThreadCount;
	}

	@Override
	public void serverShuttingdown() {
		stopApplication(false);

	}


	@Override
	public void appStop(IScope app) {
		super.appStop(app);
		//we may use this method for stopApplication
		logger.info("appStop is being called for {}", app.getName());
	}


	public void stopApplication(boolean deleteDB) {
		logger.info("{} is closing streams", getScope().getName());
		serverShuttingDown = true;
		closeStreamFetchers();
		closeRTMPStreams();

		waitUntilLiveStreamsStopped();
		waitUntilThreadsStop();

		createShutdownFile(getScope().getName());


		closeDB(deleteDB);

	}

	public void closeDB(boolean deleteDB) {
		boolean clusterMode = isClusterMode();
		if (deleteDB && clusterMode)
		{
			//let the other nodes have enough time to synch
			getVertx().setTimer(ClusterNode.NODE_UPDATE_PERIOD + 1000, l->
			getDataStore().close(deleteDB)
					);
		}
		else {
			getDataStore().close(deleteDB);
		}
	}



	public Result createInitializationProcess(String appName){

		Result result = new Result(false);

		String initializedFilePath =WEBAPPS_PATH + appName + "/.initialized";
		File initializedFile = new File(initializedFilePath);

		String closedFilePath =WEBAPPS_PATH + appName + "/.closed";
		File closedFile = new File(closedFilePath);

		try {
			// Check first start
			if(!initializedFile.exists() && !closedFile.exists()) {
				createInitializationFile(appName, result, initializedFile);
			}
			// Check repeated starting - It's normal start
			else if(initializedFile.exists() && closedFile.exists()) {
				// Delete old .closed file in application
				Files.delete(closedFile.toPath());

				if(!closedFile.exists()) {
					result.setMessage("System works, deleted closed file in " + appName);
					result.setSuccess(true);
					logger.info("Delete the \".closed\" file in {}",appName);
				}
				else {
					result.setMessage("Delete couldn't closed file in " + appName);
					result.setSuccess(false);
					logger.info("Not deleted the \".closed\" file in {}",appName);
				}
			}
			// It means didn't close normal (unexpected stop)
			else if(initializedFile.exists() && !closedFile.exists()) {
				// It's problem here
				// Notify user to send logs
				result.setMessage("Something wrong in " + appName);
				result.setSuccess(false);
				logger.error("Something wrong in {}",appName);
			}
			else {
				// Other odd is initialization file doesn't exist and closed file exist.
				// This case happens when app is created but not deployed for some reason
				createInitializationFile(appName, result, initializedFile);
				Files.deleteIfExists(closedFile.toPath());

			}



		} catch (IOException e) {
			logger.error(e.getMessage());
		}

		return result;
	}

	public void createInitializationFile(String appName, Result result, File initializedFile) throws IOException {
		if(initializedFile.createNewFile()) {
			result.setMessage("Initialized file created in " + appName);
			result.setSuccess(true);
			logger.info("Initialized file is created in {}",appName);
		}
		else {
			result.setMessage("Initialized file couldn't create in " + appName);
			result.setSuccess(false);
			logger.info("Initialized file couldn't be created in {}",appName);
		}
	}


	public void createShutdownFile(String appName){

		String closedFilePath =WEBAPPS_PATH + appName + "/.closed";
		File closedFile = new File(closedFilePath);

		try {
			if(!closedFile.exists()) {
				if(closedFile.createNewFile()) {
					logger.info("Closed file created in {}",appName);
				}
				else {
					logger.error("Closed file couldn't create in {}",appName);
				}
			}
			else {
				logger.warn("Closed file already exists for app: {}", appName);
			}

		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	public boolean isShutdownProperly() {
		return shutdownProperly;
	}

	public void setShutdownProperly(boolean shutdownProperly) {
		this.shutdownProperly = shutdownProperly;
	}

	@Override
	public void muxAdaptorAdded(MuxAdaptor muxAdaptor){
		muxAdaptors.put(muxAdaptor.getStreamId(), muxAdaptor);
	}

	@Override
	public void muxAdaptorRemoved(MuxAdaptor muxAdaptor) {
		muxAdaptors.remove(muxAdaptor.getStreamId());
	}

	public MuxAdaptor getMuxAdaptor(String streamId) {
		return muxAdaptors.get(streamId);
	}

	public Collection<MuxAdaptor> getMuxAdaptors() {
		return muxAdaptors.values();
	}


	/**
	 * Number of encoders blocked.
	 * @return
	 */
	public int getNumberOfEncodersBlocked() {
		return encoderBlockedStreams.size();
	}

	public synchronized void encoderBlocked(String streamId, boolean blocked) {
		if (blocked) {
			encoderBlockedStreams.add(streamId);
		}
		else {
			encoderBlockedStreams.remove(streamId);
		}
	}


	public synchronized void incrementEncoderNotOpenedError(String streamId) {
		numberOfEncoderNotOpenedErrors ++;

		Broadcast broadcast = getDataStore().get(streamId);

		if (broadcast != null) {
			final String listenerHookURL = getListenerHookURL(broadcast);
			if (listenerHookURL != null && listenerHookURL.length() > 0) {
				final String name = broadcast.getName();
				final String category = broadcast.getCategory();
				final String metaData = broadcast.getMetaData();
				String mainTrackId = broadcast.getMainTrackStreamId();
				logger.info("Setting timer to call encoder not opened error for stream:{}", streamId);
				notifyHook(listenerHookURL, streamId, mainTrackId, HOOK_ACTION_ENCODER_NOT_OPENED_ERROR, name, category, null, null, metaData, null);
			}
		}
	}

	public int getNumberOfEncoderNotOpenedErrors() {
		return numberOfEncoderNotOpenedErrors;
	}

	public int getNumberOfPublishTimeoutError() {
		return publishTimeoutStreams;
	}

	public synchronized void publishTimeoutError(String streamId, String subscriberId) {
		publishTimeoutStreams++;
		publishTimeoutStreamsList.add(streamId);
		Broadcast broadcast = getDataStore().get(streamId);

		if (broadcast != null)
		{
			final String listenerHookURL = getListenerHookURL(broadcast);
			if (listenerHookURL != null && listenerHookURL.length() > 0) {
				final String name = broadcast.getName();
				final String category = broadcast.getCategory();
				String mainTrackId = broadcast.getMainTrackStreamId();
				logger.info("Setting timer to call hook that means live stream is not started to the publish timeout for stream:{}", streamId);

				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);



				notifyHook(listenerHookURL, streamId, mainTrackId, HOOK_ACTION_PUBLISH_TIMEOUT_ERROR, name, category, null, null, jsonResponse.toJSONString(), null);
			}
		}
	}

	public WebRTCAudioReceiveStats getWebRTCAudioReceiveStats() {
		return webRTCAudioReceiveStats;
	}

	public WebRTCVideoReceiveStats getWebRTCVideoReceiveStats() {
		return webRTCVideoReceiveStats;
	}

	public WebRTCAudioSendStats getWebRTCAudioSendStats() {
		return webRTCAudioSendStats;
	}

	public WebRTCVideoSendStats getWebRTCVideoSendStats() {
		return webRTCVideoSendStats;
	}

	public Vertx getVertx() {
		if (vertx == null) {
			vertx = (Vertx) getScope().getContext().getBean(VERTX_BEAN_NAME);
		}
		return vertx;
	}

	/*
	 * This method can be called by multiple threads especially in cluster mode
	 * and this cause some issues for settings synchronization. So that it's synchronized
	 * 
	 * @param newSettings
	 * @param notifyCluster
	 * @param checkUpdateTime, if it is false it checks the update time of the currents settings and incoming settings.
	 *   If the incoming setting is older than current settings, it returns false.
	 *   If it is false, it just writes the settings without checking time
	 * @return
	 */
	public synchronized boolean updateSettings(AppSettings newSettings, boolean notifyCluster, boolean checkUpdateTime) {

		boolean result = false;

		if (checkUpdateTime && !isIncomingSettingsDifferent(newSettings)) {
			//if current app settings update time is bigger than the newSettings, don't update the bean
			//it may happen in cluster mode, app settings may be updated locally then a new update just may come instantly from cluster settings.
			logger.debug("Not saving the settings because current appsettings update time({}) incoming settings update time({}) are same", appSettings.getUpdateTime(), newSettings.getUpdateTime() );
			return result;
		}


		//if there is any wrong encoder settings, return false
		List<EncoderSettings> encoderSettingsList = newSettings.getEncoderSettings();
		if (!isEncoderSettingsValid(encoderSettingsList)) {
			return result;
		}


		//if there is any wrong publish/play token settings, return false.
		//A single token security setting can be activated for both publishing and playing, with a maximum limit of two settings—one for each.
		if(!isTokenSecuritySettingsValid(newSettings)){
			logger.info("Could not save app settings. Only one type of token control should be enabled for publish or play.");
			return result;
		}

		//synch again because of string to list mapping- TODO: There is a better way for string to list mapping
		//in properties files
		newSettings.setEncoderSettings(encoderSettingsList);

		if (newSettings.getHlsListSize() == null || Integer.valueOf(newSettings.getHlsListSize()) < 5) {
			newSettings.setHlsListSize("5");
		}

		if (newSettings.getHlsTime() == null || Integer.valueOf(newSettings.getHlsTime()) < 1) {
			newSettings.setHlsTime("1");
		}

		/**
		 * ATTENTION: When a new settings added both
		 *   {@link #updateAppSettingsFile} && {@link #updateAppSettingsBean} should be updated
		 */
		updateAppSettingsBean(appSettings, newSettings, notifyCluster);
		AcceptOnlyStreamsInDataStore securityHandler = (AcceptOnlyStreamsInDataStore)  getScope().getContext().getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME);
		securityHandler.setEnabled(newSettings.isAcceptOnlyStreamsInDataStore());

		if (notifyCluster && clusterNotifier != null) {
			//we should set to be deleted because app deletion fully depends on the cluster synch TODO remove the following line because toBeDeleted is deprecated
			appSettings.setToBeDeleted(newSettings.isToBeDeleted());

			appSettings.setAppStatus(newSettings.getAppStatus());


			boolean saveSettings = clusterNotifier.getClusterStore().saveSettings(appSettings);
			logger.info("Saving settings to cluster db -> {} for app: {} and updateTime:{}", saveSettings, getScope().getName(), appSettings.getUpdateTime());
		}

		if (updateAppSettingsFile(getScope().getName(), newSettings))
		{
			result = true;
		}
		else {
			logger.warn("Settings cannot be saved for {}", getScope().getName());
		}
		
		notifySettingsUpdateListeners(appSettings);

		return result;
	}
	
	public void notifySettingsUpdateListeners(AppSettings appSettings) {
		for (IAppSettingsUpdateListener listener : settingsUpdateListenerSet) {
			listener.settingsUpdated(appSettings);
		}
	}
	
	@Override
	public void addSettingsUpdateListener(IAppSettingsUpdateListener listener) {
		settingsUpdateListenerSet.add(listener);
	}

	private boolean isEncoderSettingsValid(List<EncoderSettings> encoderSettingsList) {
		if (encoderSettingsList != null) {
			for (Iterator<EncoderSettings> iterator = encoderSettingsList.iterator(); iterator.hasNext();) {
				EncoderSettings encoderSettings = iterator.next();
				if (encoderSettings.getHeight() <= 0)
				{
					logger.error("Unexpected encoder parameter. None of the parameters(height:{}, video bitrate:{}, audio bitrate:{}) can be zero or less", encoderSettings.getHeight(), encoderSettings.getVideoBitrate(), encoderSettings.getAudioBitrate());
					return false;
				}
			}
		}
		return true;
	}

	private boolean isTokenSecuritySettingsValid(AppSettings newSettings) {
		int publishTokenSecurityEnabledCount = 0;
		int playTokenSecurityEnabledCount = 0;

		if (newSettings.isPublishTokenControlEnabled()) {
			publishTokenSecurityEnabledCount++;
		}
		if (newSettings.isPublishJwtControlEnabled()) {
			publishTokenSecurityEnabledCount++;
		}
		if (newSettings.isEnableTimeTokenForPublish()) {
			publishTokenSecurityEnabledCount++;
		}
		if (newSettings.isEnableTimeTokenForPlay()) {
			playTokenSecurityEnabledCount++;
		}

		if (newSettings.isPlayTokenControlEnabled()) {
			playTokenSecurityEnabledCount++;
		}
		if (newSettings.isPlayJwtControlEnabled()) {
			playTokenSecurityEnabledCount++;
		}


		// Only one type of token control should be enabled for publish and play
		return publishTokenSecurityEnabledCount <= 1 && playTokenSecurityEnabledCount <= 1;
	}

	/**
	 *
	 * @param newSettings
	 * @param checkUpdateTime
	 * @return true if time are not equal, it means new settings is different than the current settings
	 */
	public boolean isIncomingSettingsDifferent(AppSettings newSettings)
	{
		return appSettings.getUpdateTime() != newSettings.getUpdateTime();
	}

	public void setClusterNotifier(IClusterNotifier clusterNotifier) {
		this.clusterNotifier = clusterNotifier;
	}


	public static boolean updateAppSettingsFile(String appName, AppSettings newAppsettings)
	{
		/*
		 * Remember remember the 23th of November
		 *
		 * String.valueof(null) returns "null" string.
		 *
		 * If we know the case above, we will write better codes.
		 *
		 */


		PreferenceStore store = new PreferenceStore(WEBAPPS_PATH + appName + "/WEB-INF/red5-web.properties");

		Field[] declaredFields = newAppsettings.getClass().getDeclaredFields();

		for (Field field : declaredFields)
		{
			if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
			{
				if (field.trySetAccessible())
				{
					try {
						Object value = field.get(newAppsettings);
						if (value instanceof List) {
							store.put(field.getName(), AppSettings.encodersList2Str(newAppsettings.getEncoderSettings()));
						}
						else if (value instanceof Map) {
							store.put(field.getName(), new JSONObject((Map) value).toJSONString());
						}
						else {
							store.put(field.getName(), value != null ? String.valueOf(value) : "");
						}
					} 
					catch (IllegalArgumentException | IllegalAccessException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
					field.setAccessible(false);
				}
			}
		}

		return store.save();
	}


	public void updateAppSettingsBean(AppSettings appSettings, AppSettings newSettings, boolean updateTime)
	{
		Field[] declaredFields = appSettings.getClass().getDeclaredFields();

		for (Field field : declaredFields)
		{
			setAppSettingsFieldValue(appSettings, newSettings, field);
		}

		if (updateTime) {
			//updateTime is true when the app settings is updated from the REST API or it's first updated when the app starts first in the cluster
			appSettings.setUpdateTime(System.currentTimeMillis());
		}
		String oldVodFolder = appSettings.getVodFolder();
		synchUserVoDFolder(oldVodFolder, newSettings.getVodFolder());


		setStorageclientSettings(newSettings);

		logger.warn("app settings bean updated for {}", getScope().getName());

	}

	public void setStorageclientSettings(AppSettings settings) {
		storageClient.setEndpoint(settings.getS3Endpoint());
		storageClient.setStorageName(settings.getS3BucketName());
		storageClient.setAccessKey(settings.getS3AccessKey());
		storageClient.setSecretKey(settings.getS3SecretKey());
		storageClient.setRegion(settings.getS3RegionName());
		storageClient.setEnabled(settings.isS3RecordingEnabled());
		storageClient.setPermission(settings.getS3Permission());
		storageClient.setStorageClass(settings.getS3StorageClass());
		storageClient.setCacheControl(settings.getS3CacheControl());
		storageClient.setPathStyleAccessEnabled(settings.isS3PathStyleAccessEnabled());
		storageClient.setTransferBufferSize(settings.getS3TransferBufferSizeInBytes());
		storageClient.reset();
	}

	public static boolean setAppSettingsFieldValue(AppSettings appSettings, AppSettings newSettings, Field field) {
		boolean result = false;
		try {

			if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {

				if (field.trySetAccessible())
				{
					field.set(appSettings, field.get(newSettings));
					field.setAccessible(false);
					result = true;
				}
				else
				{
					logger.warn("Cannot set the value this field: {}", field.getName());
				}
			}
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return result;
	}

	public void setServerSettings(ServerSettings serverSettings) {
		this.serverSettings = serverSettings;
	}

	/*
	 * This method is overridden in enterprise edition since RTMP to WebRTC streaming is an enterprise feature.
	 */
	public RTMPToWebRTCStats getRTMPToWebRTCStats(String streamId) {
		return new RTMPToWebRTCStats(streamId);
	}

	public boolean isDataChannelEnabled() {
		return false;
	}

	public boolean isDataChannelMessagingSupported() {

		return false;
	}

	public boolean sendDataChannelMessage(String streamId, String message) {

		return false;
	}

	public boolean doesWebRTCStreamExist(String streamId) {
		return false;
	}

	public boolean addPacketListener(String streamId, IPacketListener listener) {
		boolean isAdded = false;
		MuxAdaptor muxAdaptorsLocal = getMuxAdaptor(streamId);

		if (muxAdaptorsLocal != null) {
			muxAdaptorsLocal.addPacketListener(listener);
			logger.info("Packet listener({}) is added to streamId:{}", listener.getClass().getSimpleName(), streamId);
			isAdded = true;
		}

		if(!isAdded) {
			logger.info("Stream:{} is not in this server. It's creating cluster stream fetcher to get the stream", streamId);
			if(clusterStreamFetcher == null) {
				clusterStreamFetcher = createClusterStreamFetcher();
			}

			isAdded = clusterStreamFetcher.register(streamId, listener);
		}

		return isAdded;

	}

	public void endpointFailedUpdate(String streamId, String url) {
		Broadcast broadcast = getDataStore().get(streamId);

		if (broadcast != null)
		{
			final String listenerHookURL = getListenerHookURL(broadcast);
			if (listenerHookURL != null && listenerHookURL.length() > 0)
			{
				final String name = broadcast.getName();
				final String category = broadcast.getCategory();
				String mainTrackId = broadcast.getMainTrackStreamId();
				logger.info("Setting timer to call rtmp endpoint failed hook for stream:{}", streamId);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("rtmp-url", url);
				notifyHook(listenerHookURL, streamId, mainTrackId, HOOK_ACTION_ENDPOINT_FAILED, name, category, null, null, jsonObject.toJSONString(), null);
			}
		}
	}


	public boolean removePacketListener(String streamId, IPacketListener listener) {
		boolean isRemoved = false;

		MuxAdaptor muxAdaptorsLocal = getMuxAdaptor(streamId);

		if (muxAdaptorsLocal != null) {
			isRemoved = muxAdaptorsLocal.removePacketListener(listener);
		}


		if (!isRemoved)
		{
			if (clusterStreamFetcher != null)
			{
				isRemoved = clusterStreamFetcher.remove(streamId, listener);
			}
			else {
				logger.warn("Cluster stream fetcher is null so that packet listener cannot be removed for streamId:{}", streamId);
			}
		}

		if (isRemoved) {
			logger.info("Packet listener is removed succesfully from adaptor for streamId:{}", streamId);
		}
		else {
			logger.warn("Packet listener cannot be removed from adaptor for streamId:{}", streamId);
		}

		return isRemoved;
	}





	public void addFrameListener(String streamId, IFrameListener listener) {
		//for enterprise
	}
	
	public IFrameListener createCustomBroadcast(String streamId) {
		throw new IllegalStateException("This method is not implemented in Community Edition");
	}

	public IFrameListener createCustomBroadcast(String streamId, int height, int bitrate) {
		throw new IllegalStateException("This method is not implemented in Community Edition");
	}

	public void stopCustomBroadcast(String streamId) {
	}

	public void removeFrameListener(String streamId, IFrameListener listener) {
	}

	@Override
	public boolean isServerShuttingDown() {
		return serverShuttingDown;
	}

	public void setStorageClient(StorageClient storageClient) {
		this.storageClient = storageClient;
	}

	public StorageClient getStorageClient() {
		return storageClient;
	}

	public void addStreamListener(IStreamListener listener) {
		streamListeners.add(listener);
	}

	public void removeStreamListener(IStreamListener listener) {
		streamListeners.remove(listener);
	}

	public boolean stopPlaying(String viewerId) {
		return false;
	}

	public CompletableFuture<Result> startHttpSignaling(PublishParameters publishParameters, String sdp, String sessionId){
		//for enterprise
		return null;
	}

	public Result stopWhipBroadcast(String streamId, String sessionId){
		return new Result(false);
	}

	public boolean stopPlayingBySubscriberId(String subscriberId){
		return false;
	}

	public boolean stopPublishingBySubscriberId(String subscriberId){
		return false;
	}

	public void stopPublish(String streamId) {

		vertx.executeBlocking(() -> {
			closeBroadcast(streamId);
			return null;
		}, false);
	}

	public boolean isClusterMode() {
		return getScope().getContext().hasBean(IClusterNotifier.BEAN_NAME);
	}

	public void joinedTheRoom(String roomId, String streamId) {
		//No need to implement here.
	}

	public void leftTheRoom(String mainTrackId, String subtrackId) {
		//No need to implement here.
	}

	public IClusterStreamFetcher createClusterStreamFetcher() {
		return null;
	}

	public Map<String, Queue<IWebRTCClient>> getWebRTCClientsMap() {
		return Collections.emptyMap();
	}

	public ISubtrackPoller getSubtrackPoller() {
		return subtrackPoller;
	}

	public void setSubtrackPoller(ISubtrackPoller subtrackPoller) {
		this.subtrackPoller = subtrackPoller;
	}

	public Map<String, Long> getPlayListSchedulerTimer() {
		return playListSchedulerTimer;
	}

	public IStatsCollector getStatsCollector() {
		if(statsCollector == null)
		{
			statsCollector = (IStatsCollector)getScope().getContext().getApplicationContext().getBean(StatsCollector.BEAN_NAME);
		}
		return statsCollector;
	}

	public void setStatsCollector(IStatsCollector statsCollector) {
		this.statsCollector = statsCollector;
	}

}
