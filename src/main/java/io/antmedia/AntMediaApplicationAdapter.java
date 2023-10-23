package io.antmedia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
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

import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.filter.StreamAcceptFilter;
import io.antmedia.ipcamera.OnvifCamera;
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
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.ViewerStats;
import io.antmedia.statistic.type.RTMPToWebRTCStats;
import io.antmedia.statistic.type.WebRTCAudioReceiveStats;
import io.antmedia.statistic.type.WebRTCAudioSendStats;
import io.antmedia.statistic.type.WebRTCVideoReceiveStats;
import io.antmedia.statistic.type.WebRTCVideoSendStats;
import io.antmedia.storage.StorageClient;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.streamsource.StreamFetcherManager;
import io.antmedia.track.ISubtrackPoller;
import io.antmedia.webrtc.api.IWebRTCAdaptor;
import io.antmedia.webrtc.api.IWebRTCClient;
import io.antmedia.websocket.WebSocketConstants;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;

public class AntMediaApplicationAdapter  extends MultiThreadedApplicationAdapter implements IAntMediaStreamHandler, IShutdownListener {

	public static final String BEAN_NAME = "web.handler";

	public static final int BROADCAST_STATS_RESET = 0;
	public static final String HOOK_ACTION_END_LIVE_STREAM = "liveStreamEnded";
	public static final String HOOK_ACTION_START_LIVE_STREAM = "liveStreamStarted";
	public static final String HOOK_ACTION_VOD_READY = "vodReady";

	public static final String HOOK_ACTION_PUBLISH_TIMEOUT_ERROR = "publishTimeoutError";
	public static final String HOOK_ACTION_ENCODER_NOT_OPENED_ERROR =  "encoderNotOpenedError";
	public static final String HOOK_ACTION_ENDPOINT_FAILED = "endpointFailed";
	public static final String HOOK_ACTION_START_PLAY = "playStart";
	public static final String HOOK_ACTION_STOP_PLAY = "playStop";
	public static final String HOOK_ACTION_START_RECORD = "recordStart";

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
	public static final String WEBAPPS_PATH = "webapps/";

	//Allow any sub directory under /
	private static final String VOD_IMPORT_ALLOWED_DIRECTORY = "/";


	private List<IStreamPublishSecurity> streamPublishSecurityList;
	private HashMap<String, OnvifCamera> onvifCameraList = new HashMap<>();
	protected StreamFetcherManager streamFetcherManager;
	protected List<MuxAdaptor> muxAdaptors;
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

	IClusterStreamFetcher clusterStreamFetcher;
	
	protected ISubtrackPoller subtrackPoller;


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
			clusterNotifier.registerSettingUpdateListener(getAppSettings().getAppName(), settings -> updateSettings(settings, false, true));
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

		vertx.setTimer(10, l -> {

			getStreamFetcherManager();
			if(appSettings.isStartStreamFetcherAutomatically()) {
				List<Broadcast> streams = getDataStore().getExternalStreamsList();
				logger.info("Stream source size: {}", streams.size());
				streamFetcherManager.startStreams(streams);
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

	public String getListenerHookURL(@NotNull Broadcast broadcast) 
	{
		String listenerHookURL = broadcast.getListenerHookURL();
		if (listenerHookURL == null || listenerHookURL.isEmpty()) 
		{
			listenerHookURL = getAppSettings().getListenerHookURL();
		}
		return listenerHookURL;

	}

	public void closeBroadcast(String streamId) {

		try {
			logger.info("Closing broadcast stream id: {}", streamId);
			getDataStore().updateStatus(streamId, BROADCAST_STATUS_FINISHED);
			Broadcast broadcast = getDataStore().get(streamId);

			if (broadcast != null) {
				final String listenerHookURL = getListenerHookURL(broadcast);
				if (listenerHookURL != null && !listenerHookURL.isEmpty()) {
					final String name = broadcast.getName();
					final String category = broadcast.getCategory();
					final String metaData = broadcast.getMetaData();
					logger.info("Setting timer to call live stream ended hook for stream:{}",streamId );
					vertx.runOnContext(e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_END_LIVE_STREAM, name, category, null, null, null, null, null, metaData));

				}

				if (broadcast.isZombi()) {
					if(broadcast.getMainTrackStreamId() != null && !broadcast.getMainTrackStreamId().isEmpty()) {
						updateMainBroadcast(broadcast);
					}
					logger.info("Deleting streamId:{} because it's a zombi stream", streamId);
					getDataStore().delete(streamId);
				}
				else {
					// This is resets Viewer map in HLS Viewer Stats
					resetHLSStats(streamId);

					// This is resets Viewer map in DASH Viewer Stats
					resetDASHStats(streamId);
				}

				for (IStreamListener listener : streamListeners) {
					listener.streamFinished(broadcast.getStreamId());
				}
				logger.info("Leaving closeBroadcast for streamId:{}", streamId);
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * If multiple threads enter the method at the same time, the following method does not work correctly. 
	 * So we have made it synchronized 
	 * 
	 * It fixes the bug that sometimes main track(room) is not deleted in the video conferences
	 * 
	 * mekya
	 * 
	 * @param broadcast
	 */
	public synchronized void updateMainBroadcast(Broadcast broadcast) {
		Broadcast mainBroadcast = getDataStore().get(broadcast.getMainTrackStreamId());
		mainBroadcast.getSubTrackStreamIds().remove(broadcast.getStreamId());
		if(mainBroadcast.getSubTrackStreamIds().isEmpty() && mainBroadcast.isZombi()) {
			getDataStore().delete(mainBroadcast.getStreamId());
		}
		else {
			getDataStore().updateBroadcastFields(mainBroadcast.getStreamId(), mainBroadcast);
		}
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

	public void sendStartPlayWebHook(final String streamId, final String viewerId, final String token, final String viewerType){
		final Broadcast broadcast = getDataStore().get(streamId);
		if(broadcast == null){
			return;
		}
		final String listenerHookURL = getListenerHookURL(broadcast);
		if (listenerHookURL == null || listenerHookURL.isEmpty()) {
			return;
		}
		sendWebHook(broadcast, listenerHookURL, viewerId, viewerType, token, HOOK_ACTION_START_PLAY);
	}

	public void sendStopPlayWebHook(final String streamId, final String viewerId, final String token, final String viewerType){
		final Broadcast broadcast = getDataStore().get(streamId);
		if(broadcast == null){
			return;
		}
		final String listenerHookURL = getListenerHookURL(broadcast);
		if (listenerHookURL == null || listenerHookURL.isEmpty()) {
			return;
		}
		sendWebHook(broadcast, listenerHookURL, viewerId, viewerType, token, HOOK_ACTION_STOP_PLAY);
	}

	public void sendStartRecordWebHook(final String streamId){
		final Broadcast broadcast = getDataStore().get(streamId);
		if(broadcast == null){
			return;
		}
		final String listenerHookURL = getListenerHookURL(broadcast);
		if (listenerHookURL == null || listenerHookURL.isEmpty()) {
			return;
		}
		sendWebHook(broadcast, listenerHookURL, null, null,null, HOOK_ACTION_START_RECORD);
	}

	private void sendWebHook(final Broadcast broadcast, final String listenerHookUrl, final String viewerId, final String viewerType, final String token, final String hookAction){
		final String streamId = broadcast.getStreamId();
		final String name = broadcast.getName();
		final String category = broadcast.getCategory();
		final String metaData = broadcast.getMetaData();
		logger.info("Setting timer to call {} hook for stream:{}", hookAction, streamId);
		vertx.setTimer(10, e -> notifyHook(listenerHookUrl, streamId, hookAction, name, category,
				null, null, viewerId, viewerType, token, metaData));
	}

	private String getRtmpViewerId(){
		return "rtmp_" + RandomStringUtils.randomNumeric(8);
	}

	@Override
	public void streamPlayItemPlay(ISubscriberStream stream, IPlayItem item, boolean isLive) {
		final String streamId = item.getName();
		sendStartPlayWebHook(streamId, getRtmpViewerId(), null, ViewerStats.RTMP_TYPE);
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(streamId, true));
	}
	@Override
	public void streamPlayItemStop(ISubscriberStream stream, IPlayItem item) {
		final String streamId = item.getName();
		sendStopPlayWebHook(streamId, getRtmpViewerId(), null, ViewerStats.RTMP_TYPE);
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(streamId, false));
	}

	@Override
	public void streamSubscriberClose(ISubscriberStream stream) {
		vertx.setTimer(1, l -> getDataStore().updateRtmpViewerCount(stream.getBroadcastStreamPublishName(), false));
	}

	@Override
	public void startPublish(String streamId, long absoluteStartTimeMs, String publishType) {
		vertx.executeBlocking( handler -> {
			try {
				Broadcast broadcast = updateBroadcastStatus(streamId, absoluteStartTimeMs, publishType, getDataStore().get(streamId));

				final String listenerHookURL = getListenerHookURL(broadcast);
				if (listenerHookURL != null && !listenerHookURL.isEmpty())
				{
					final String name = broadcast.getName();
					final String category = broadcast.getCategory();
					final String metaData = broadcast.getMetaData();
					logger.info("Setting timer to call live stream started hook for stream:{}",streamId );
					vertx.setTimer(10, e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_START_LIVE_STREAM, name, category,
							null, null, null, null, null, metaData));
				}

				if ((broadcast.getMp4Enabled() == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM || broadcast.getWebMEnabled() == MuxAdaptor.RECORDING_ENABLED_FOR_STREAM)
						|| (appSettings.isMp4MuxingEnabled() || appSettings.isWebMMuxingEnabled())
				) {
					sendStartRecordWebHook(streamId);
				}

				int ingestingStreamLimit = appSettings.getIngestingStreamLimit();

				long activeBroadcastNumber = dataStore.getActiveBroadcastCount();
				if (ingestingStreamLimit != -1 && activeBroadcastNumber > ingestingStreamLimit) 
				{
					logger.info("Active broadcast count({}) is more than ingesting stream limit:{} so stopping broadcast:{}", activeBroadcastNumber, ingestingStreamLimit, broadcast.getStreamId());
					stopStreaming(broadcast);
				}


				for (IStreamListener listener : streamListeners) {
					listener.streamStarted(broadcast.getStreamId());
				}
				
	
				handler.complete();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				handler.fail(ExceptionUtils.getStackTrace(e));
			}

		}, null);


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
		if (broadcast == null) 
		{

			broadcast = saveUndefinedBroadcast(streamId, null, this, IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, absoluteStartTimeMs, publishType, "", "");
		} 
		else {

			broadcast.setStatus(BROADCAST_STATUS_BROADCASTING);
			long now = System.currentTimeMillis();
			broadcast.setStartTime(now);
			broadcast.setUpdateTime(now);
			broadcast.setOriginAdress(getServerSettings().getHostAddress());
			broadcast.setWebRTCViewerCount(0);
			broadcast.setHlsViewerCount(0);
			broadcast.setPublishType(publishType);
			boolean result = getDataStore().updateBroadcastFields(broadcast.getStreamId(), broadcast);

			logger.info(" Status of stream {} is set to Broadcasting with result: {}", broadcast.getStreamId(), result);
		}
		return broadcast;
	}

	public ServerSettings getServerSettings() 
	{
		if (serverSettings == null) {
			serverSettings = (ServerSettings)scope.getContext().getApplicationContext().getBean(ServerSettings.BEAN_NAME);
		}
		return serverSettings;
	}


	public static Broadcast saveUndefinedBroadcast(String streamId, String streamName, AntMediaApplicationAdapter appAdapter, String streamStatus, long absoluteStartTimeMs, String publishType, String mainTrackStreamId,  String metaData) {		
		Broadcast newBroadcast = new Broadcast();
		long now = System.currentTimeMillis();
		newBroadcast.setDate(now);
		newBroadcast.setStartTime(now);
		newBroadcast.setUpdateTime(now);
		newBroadcast.setZombi(true);
		newBroadcast.setName(streamName);
		newBroadcast.setMainTrackStreamId(mainTrackStreamId);
		newBroadcast.setMetaData(metaData);
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
	public void muxingFinished(final String streamId, File file, long startTime, long duration, int resolution, String previewFilePath, String vodId) {
		String vodName = file.getName();
		String filePath = file.getPath();
		long fileSize = file.length();
		long systemTime = System.currentTimeMillis();

		String relativePath = getRelativePath(filePath);
		String listenerHookURL = null;
		String streamName = file.getName();

		Broadcast broadcast = getDataStore().get(streamId);

		if(broadcast != null){
			listenerHookURL = broadcast.getListenerHookURL();
			if(broadcast.getName() != null){
				streamName =  resolution != 0 ? broadcast.getName() + " (" + resolution + "p)" : broadcast.getName();
			}
		}

		//We need to get the webhook url explicitly because broadcast may be deleted here
		if (listenerHookURL == null || listenerHookURL.isEmpty()) {
			// if hook URL is not defined for stream specific, then try to get common one from app
			listenerHookURL = appSettings.getListenerHookURL();
		}

		String vodIdFinal;
		if (vodId != null) {
			vodIdFinal = vodId;
		}
		else {
			vodIdFinal = RandomStringUtils.randomAlphanumeric(24);
		}

		VoD newVod = new VoD(streamName, streamId, relativePath, vodName, systemTime, startTime, duration, fileSize, VoD.STREAM_VOD, vodIdFinal, previewFilePath);

		if (getDataStore().addVod(newVod) == null) {
			logger.warn("Stream vod with stream id {} cannot be added to data store", streamId);
		}

		int index;

		//HOOK_ACTION_VOD_READY is called only the listenerHookURL is defined either for stream or in AppSettings
		if (listenerHookURL != null && !listenerHookURL.isEmpty() && 
				((index = vodName.lastIndexOf(".mp4")) != -1) 
				|| ((index = vodName.lastIndexOf(".webm")) != -1) )
		{
			final String baseName = vodName.substring(0, index);
			final String metaData = (broadcast != null) ? broadcast.getMetaData() : null;
			String finalListenerHookURL = listenerHookURL;
			logger.info("Setting timer for calling vod ready hook for stream:{}", streamId);
			vertx.runOnContext(e ->	notifyHook(finalListenerHookURL, streamId, HOOK_ACTION_VOD_READY, null, null, baseName, vodIdFinal, null, null, null, metaData));

		}

		String muxerFinishScript = appSettings.getMuxerFinishScript();
		if (muxerFinishScript != null && !muxerFinishScript.isEmpty()) {	
			runScript(muxerFinishScript + "  " + file.getAbsolutePath());
		}


	}

	public void runScript(String scriptFile) {
		vertx.executeBlocking(future -> {
			try {
				logger.info("running muxer finish script: {}", scriptFile);
				Process exec = Runtime.getRuntime().exec(scriptFile);
				int result = exec.waitFor();

				logger.info("completing script: {} with return value {}", scriptFile, result);
			} catch (IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			} catch (InterruptedException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
				Thread.currentThread().interrupt();
			} 
			future.complete();

		}, null);
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

	/**
	 * Notify hook with parameters below
	 *
	 * @param streamName, name of the stream. It is not the name of the file. It is just
	 *                    a user friendly name
	 * @param category,   category of the stream
	 * @param url         is the url of the service to be called
	 * @param id          is the stream id that is unique for each stream
	 * @param action      is the name of the action to be notified, it has values such
	 *                    as {@link #HOOK_ACTION_END_LIVE_STREAM}
	 *                    {@link #HOOK_ACTION_START_LIVE_STREAM}
	 * @param vodName     name of the vod
	 * @param vodId       id of the vod in the datastore
	 * @param viewerId    session id or subscriber id
	 * @param viewerType
	 * @param token       play token
	 * @return
	 */
	public StringBuilder notifyHook(String url, String id, String action, String streamName, String category,
									String vodName, String vodId, String viewerId, String viewerType, String token, String metadata) {
		StringBuilder response = null;
		logger.info("Running notify hook url:{} stream id: {} action:{} vod name:{} vod id:{}", url, id, action, vodName, vodId);
		if (url != null && url.length() > 0) {
			Map<String, String> variables = new HashMap<>();

			variables.put("id", id);
			variables.put("action", action);
			if (streamName != null) {
				variables.put("streamName", streamName);
			}
			if (category != null) {
				variables.put("category", category);
			}

			if (vodName != null) {
				variables.put("vodName", vodName);
			}

			if (vodId != null) {
				variables.put("vodId", vodId);
			}

			if(viewerId != null){
				variables.put("viewerId", viewerId);
			}

			if(viewerType != null){
				variables.put("viewerType", viewerType);
			}

			if(token !=null){
				variables.put("token",token);
			}

			if (metadata != null && !metadata.isEmpty()) {
				variables.put("metadata", metadata);
			}

			try {
				response = sendPOST(url, variables);
			} catch (Exception e) {
				//Make Exception generi
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return response;
	}

	public StringBuilder sendPOST(String url, Map<String, String> variables) throws IOException {
		StringBuilder response = null;
		try (CloseableHttpClient httpClient = getHttpClient()) 
		{
			HttpPost httpPost = new HttpPost(url);
			RequestConfig requestConfig =RequestConfig.custom()
					.setConnectTimeout(2000)
					.setConnectionRequestTimeout(2000)
					.setSocketTimeout(2000).build();
			httpPost.setConfig(requestConfig);
			List<NameValuePair> urlParameters = new ArrayList<>();
			Set<Entry<String, String>> entrySet = variables.entrySet();
			for (Entry<String, String> entry : entrySet) {
				urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}

			HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
			httpPost.setEntity(postParams);

			try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
				logger.info("POST Response Status:: {}" , httpResponse.getStatusLine().getStatusCode());

				HttpEntity entity = httpResponse.getEntity();
				if (entity != null) 
				{ 
					//read entity if it's available
					BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));

					String inputLine;
					response = new StringBuilder();

					while ((inputLine = reader.readLine()) != null) {
						response.append(inputLine);
					}
					reader.close();
				}
			}

		}
		return response;
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
	
	
	public static final boolean isStreaming(Broadcast broadcast) {
		//if updatetime is older than 2 times update period time, regard that it's not streaming
		return System.currentTimeMillis() - broadcast.getUpdateTime() < (2 * MuxAdaptor.STAT_UPDATE_PERIOD_MS) &&
				(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING.equals(broadcast.getStatus()) 
					||	IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING.equals(broadcast.getStatus()));
	}

	public Result startStreaming(Broadcast broadcast) 
	{		
		Result result = new Result(false);
		if(broadcast.getType().equals(AntMediaApplicationAdapter.IP_CAMERA) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.STREAM_SOURCE) ||
				broadcast.getType().equals(AntMediaApplicationAdapter.VOD)
				)  {
			result = getStreamFetcherManager().startStreaming(broadcast);
		}
		else if (broadcast.getType().equals(AntMediaApplicationAdapter.PLAY_LIST)) {
			result = getStreamFetcherManager().startPlaylist(broadcast);

		}
		return result;
	}

	public Result stopStreaming(Broadcast broadcast) 
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
	public void setQualityParameters(String id, String quality, double speed, int pendingPacketSize, long updateTimeMs) {

		vertx.setTimer(500, h -> {
			
			Broadcast broadcastLocal = getDataStore().get(id);
			if (broadcastLocal != null) 
			{
				//round the number to three decimal places, 
				double roundedSpeed = Math.round(speed * 1000.0) / 1000.0;

				logger.debug("update source quality for stream: {} quality:{} speed:{}", id, quality, speed);
				
				broadcastLocal.setSpeed(roundedSpeed);
				broadcastLocal.setPendingPacketSize(pendingPacketSize);
				broadcastLocal.setUpdateTime(updateTimeMs);
				broadcastLocal.setQuality(quality);
				getDataStore().updateBroadcastFields(id, broadcastLocal);
			}
			
		});
	}

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
		List<MuxAdaptor> adaptors = getMuxAdaptors();
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
			Queue<StreamFetcher> fetchers = streamFetcherManager.getStreamFetcherList();
			for (StreamFetcher streamFetcher : fetchers) {
				streamFetcher.stopStream();
				fetchers.remove(streamFetcher);
			}
		}
	}

	public void waitUntilLiveStreamsStopped() {
		int i = 0;
		int waitPeriod = 1000;
		while(getDataStore().getLocalLiveBroadcastCount(getServerSettings().getHostAddress()) > 0) {
			try {
				if (i > 3) {
					logger.warn("Waiting for active broadcasts number decrease to zero for app: {}"
							+ "total wait time: {}ms", getScope().getName(), i*waitPeriod);
				}
				if (i>10) {
					logger.error("*********************************************************************************");
					logger.error("Not all live streams're stopped. It's even breaking the loop to finish the server");
					logger.error("*********************************************************************************");
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


	public void stopApplication(boolean deleteDB) {
		logger.info("{} is closing streams", getScope().getName());
		serverShuttingDown = true;
		closeStreamFetchers();
		closeRTMPStreams();

		waitUntilLiveStreamsStopped();
		waitUntilThreadsStop();

		createShutdownFile(getScope().getName());

		vertx.setTimer(ClusterNode.NODE_UPDATE_PERIOD, l-> getDataStore().close(deleteDB));

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
		getMuxAdaptors().add(muxAdaptor);
	}

	@Override
	public void muxAdaptorRemoved(MuxAdaptor muxAdaptor) {
		getMuxAdaptors().remove(muxAdaptor);
	}

	public List<MuxAdaptor> getMuxAdaptors() {
		if(muxAdaptors == null){
			muxAdaptors = Collections.synchronizedList(new ArrayList<MuxAdaptor>());
		}
		return muxAdaptors;
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
				logger.info("Setting timer to call encoder not opened error for stream:{}", streamId);
				vertx.runOnContext(e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_ENCODER_NOT_OPENED_ERROR, name, category, null, null, null, null, null, metaData));

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
				logger.info("Setting timer to call hook that means live stream is not started to the publish timeout for stream:{}", streamId);

				JSONObject jsonResponse = new JSONObject();
				jsonResponse.put(WebSocketConstants.SUBSCRIBER_ID, subscriberId);
				
				vertx.runOnContext(e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_PUBLISH_TIMEOUT_ERROR, name, category, null, null, null, null, null, jsonResponse.toJSONString()));
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
	 * @param newSettings
	 * @param notifyCluster
	 * @param checkUpdateTime, if it is false it checks the update time of the currents settings and incoming settings. 
	 *   If the incoming setting is older than current settings, it returns false.
	 *   If it is false, it just writes the settings without checking time
	 * @return
	 */
	public synchronized boolean updateSettings(AppSettings newSettings, boolean notifyCluster, boolean checkUpdateTime) {

		boolean result = false;

		if (checkUpdateTime && !isIncomingTimeValid(newSettings)) {
			//if current app settings update time is bigger than the newSettings, don't update the bean
			//it may happen in cluster mode, app settings may be updated locally then a new update just may come instantly from cluster settings.
			logger.debug("Not saving the settings because current appsettings update time({}) is later than incoming settings update time({}) ", appSettings.getUpdateTime(), newSettings.getUpdateTime() );
			return result;
		}


		//if there is any wrong encoder settings, return false
		List<EncoderSettings> encoderSettingsList = newSettings.getEncoderSettings();
		if (!isEncoderSettingsValid(encoderSettingsList)) {
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
		if (updateAppSettingsFile(getScope().getName(), newSettings))
		{
			AcceptOnlyStreamsInDataStore securityHandler = (AcceptOnlyStreamsInDataStore)  getScope().getContext().getBean(AcceptOnlyStreamsInDataStore.BEAN_NAME);
			securityHandler.setEnabled(newSettings.isAcceptOnlyStreamsInDataStore());

			updateAppSettingsBean(appSettings, newSettings);

			if (notifyCluster && clusterNotifier != null) {
				//we should set to be deleted because app deletion fully depends on the cluster synch
				appSettings.setToBeDeleted(newSettings.isToBeDeleted());
				boolean saveSettings = clusterNotifier.getClusterStore().saveSettings(appSettings);
				logger.info("Saving settings to cluster db -> {} for app: {}", saveSettings, getScope().getName());
			}

			result = true;
		}
		else {
			logger.warn("Settings cannot be saved for {}", getScope().getName());
		}

		return result;
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

	/**
	 * 
	 * @param newSettings
	 * @return true if timing is valid, false if it is invalid
	 */
	public boolean isIncomingTimeValid(AppSettings newSettings) 
	{
		return appSettings.getUpdateTime() != 0 && newSettings.getUpdateTime() != 0 
				&& appSettings.getUpdateTime() < newSettings.getUpdateTime();
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
						else {
							store.put(field.getName(), value != null ? String.valueOf(value) : "");
						}
					} catch (IllegalArgumentException | IllegalAccessException e) {
						logger.error(ExceptionUtils.getStackTrace(e));
					}
					field.setAccessible(false);
				}
			}
			
		}

		return store.save();
	}


	public void updateAppSettingsBean(AppSettings appSettings, AppSettings newSettings) 
	{		
		Field[] declaredFields = appSettings.getClass().getDeclaredFields();

		for (Field field : declaredFields) 
		{     
			setAppSettingsFieldValue(appSettings, newSettings, field); 
		}

		appSettings.setUpdateTime(System.currentTimeMillis());

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
		List<MuxAdaptor> muxAdaptorsLocal = getMuxAdaptors();
		synchronized (muxAdaptorsLocal) 
		{
			for (MuxAdaptor muxAdaptor : muxAdaptorsLocal) 
			{
				if (streamId.equals(muxAdaptor.getStreamId())) 
				{
					muxAdaptor.addPacketListener(listener);
					logger.info("Packet listener({}) is added to streamId:{}", listener.getClass().getSimpleName(), streamId);
					isAdded = true;
					break;
				}
			}
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
				logger.info("Setting timer to call rtmp endpoint failed hook for stream:{}", streamId);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("rtmp-url", url);
				vertx.runOnContext(e -> notifyHook(listenerHookURL, streamId, HOOK_ACTION_ENDPOINT_FAILED, name, category, null, null, null, null, null, jsonObject.toJSONString()));
			}
		}
	}


	public boolean removePacketListener(String streamId, IPacketListener listener) {
		boolean isRemoved = false;
		
		List<MuxAdaptor> muxAdaptorsLocal = getMuxAdaptors();
		synchronized (muxAdaptorsLocal) 
		{
			for (MuxAdaptor muxAdaptor : muxAdaptorsLocal) 
			{
				if (streamId.equals(muxAdaptor.getStreamId())) 
				{
					isRemoved = muxAdaptor.removePacketListener(listener);
					break;

				}
			}
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


	public boolean stopPlayingBySubscriberId(String subscriberId){
		return false;
	}

	public boolean stopPublishingBySubscriberId(String subscriberId){
		return false;
	}

	public void stopPublish(String streamId) {
		vertx.executeBlocking(handler-> closeBroadcast(streamId) , null);
	}

	public void joinedTheRoom(String roomId, String streamId) {
		//No need to implement here. 
	}

	public void leftTheRoom(String roomId, String streamId) {
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



}
