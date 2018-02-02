package io.antmedia.rest;

import java.io.File;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import com.google.gson.Gson;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.db.types.Vod;
import io.antmedia.ipcamera.IPCameraApplicationAdapter;
import io.antmedia.muxer.Muxer;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint.DeviceAuthParameters;
import io.antmedia.storage.StorageClient;
import io.antmedia.storage.StorageClient.FileType;

@Component
@Path("/")
public class BroadcastRestService {

	public static class Result {
		public boolean success = false;
		public String message = null;

		public Result(boolean success, String message) {
			this.success = success;
			this.message = message;
		}

		public Result(boolean success) {
			this.success = success;
		}
	}

	public static class SearchParam {
		public String keyword = null;

		public String getKeyword() {
			return keyword;
		}

		public void setKeyword(String keyword) {
			this.keyword = keyword;
		}

		public long getStartDate() {
			return startDate;
		}

		public void setStartDate(long startDate) {
			this.startDate = startDate;
		}

		public long getEndDate() {
			return endDate;
		}

		public void setEndDate(long endDate) {
			this.endDate = endDate;
		}

		public long startDate = 0;
		public long endDate = 0;

		public SearchParam(String keyword, long startDate, long endDate) {
			this.keyword = keyword;
			this.startDate = startDate;
			this.endDate = endDate;
		}

		public SearchParam() {
			super();
		}

	}

	@Context
	private ServletContext servletContext;

	private IScope scope;
	private ApplicationContext appCtx;

	private static Gson gson = new Gson();

	private AntMediaApplicationAdapter app;

	private IDataStore dataStore;

	private AppSettings appSettings;

	protected static Logger logger = LoggerFactory.getLogger(BroadcastRestService.class);

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/create")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createBroadcast(Broadcast broadcast) {
		if (broadcast == null) {
			broadcast = new Broadcast("null", "");
		}
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
		broadcast.setDate(System.currentTimeMillis());
		logger.info(broadcast.getType());
		String listenerHookURL = broadcast.getListenerHookURL();
		if (listenerHookURL == null || listenerHookURL.length() == 0) {
			AppSettings settings = getAppSettings();
			if (settings != null) {
				String settingsListenerHookURL = settings.getListenerHookURL();
				if (settingsListenerHookURL != null && settingsListenerHookURL.length() > 0) {
					broadcast.setListenerHookURL(settingsListenerHookURL);
				}
			}
		}

		getDataStore().save(broadcast);
		return broadcast;
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/createWithSocial")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast createWithSocial(Broadcast broadcast,
			@QueryParam("socialNetworks") String socialNetworksToPublish) {
		broadcast = createBroadcast(broadcast);
		if (broadcast.getStreamId() != null && socialNetworksToPublish != null) {
			String[] socialNetworks = socialNetworksToPublish.split(",");
			for (String networkName : socialNetworks) {
				addSocialEndpoint(broadcast.getStreamId(), networkName);
			}
		}

		return getBroadcast(broadcast.getStreamId());
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/addVODtoBroadcast")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast addVODtoBroadcast(Broadcast broadcast, Vod vod) {

		if (broadcast.getStreamId() != null && vod != null) {

			getDataStore().addVod(broadcast.getStreamId(), vod);

		}

		return getBroadcast(broadcast.getStreamId());
	}

	/**
	 * Updates broadcast name or status
	 * 
	 * @param broadcast
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/updateInfo")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updateInfo(@FormParam("id") String id, @FormParam("name") String name,
			@FormParam("description") String description) {

		boolean success = false;
		String message = null;
		if (getDataStore().updateName(id, name, description)) {
			success = true;
			message = "Modified count is not equal to 1";
		}

		return new Result(success, message);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/updatePublishStatus")
	@Produces(MediaType.APPLICATION_JSON)
	public Result updatePublishInfo(@FormParam("id") String id, @FormParam("publish") boolean publish) {

		boolean success = false;
		String message = null;
		if (getDataStore().updatePublish(id, publish)) {
			success = true;
			message = "Modified count is not equal to 1";
		}

		return new Result(success, message);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/revokeSocialNetwork/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result revokeSocialNetwork(@PathParam("serviceName") String serviceName) {
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		String message = null;
		boolean serviceFound = false;
		boolean result = false;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				if (videoServiceEndpoint.getName().equals(serviceName)) {
					serviceFound = true;
					try {
						videoServiceEndpoint.resetCredentials();
						result = true;
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (!serviceFound) {
				message = "Service with the name specified is not found in this app";
			}
		} else {
			message = "No endpoint is defined for this app";
		}
		return new Result(result, message);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addSocialEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addSocialEndpoint(@FormParam("id") String id, @FormParam("serviceName") String serviceName) {

		boolean success = false;
		String message = null;
		Broadcast broadcast = lookupBroadcast(id);
		if (broadcast != null) {
			List<VideoServiceEndpoint> endPointServiceList = getEndpointList();

			if (endPointServiceList != null) {
				boolean serviceFound = false;
				for (VideoServiceEndpoint videoServiceEndpoint : endPointServiceList) {
					if (videoServiceEndpoint.getName().equals(serviceName)) {
						serviceFound = true;
						boolean authenticated = videoServiceEndpoint.isInitialized()
								&& videoServiceEndpoint.isAuthenticated();
						if (authenticated) {
							Endpoint endpoint;
							try {
								endpoint = videoServiceEndpoint.createBroadcast(broadcast.getName(),
										broadcast.getDescription(), broadcast.isIs360(), broadcast.isPublicStream(),
										720);
								success = getDataStore().addEndpoint(id, endpoint);

							} catch (Exception e) {
								e.printStackTrace();
								message = e.getMessage();
							}
						} else {
							message = serviceName + " is not authenticated. Authenticate first";
						}
					}
				}
				if (!serviceFound) {
					message = serviceName + " endpoint does not exist in this app.";
				}
			} else {
				message = "No social endpoint is defined for this app. Consult your app developer";
			}
		} else {
			message = "No broadcast exist with the id specified";
		}

		return new Result(success, message);
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Path("/broadcast/addEndpoint")
	@Produces(MediaType.APPLICATION_JSON)
	public Result addEndpoint(@FormParam("id") String id, @FormParam("rtmpUrl") String rtmpUrl) {
		boolean success = false;
		String message = null;
		try {
			Broadcast broadcast = lookupBroadcast(id);

			Endpoint endpoint = new Endpoint();
			endpoint.rtmpUrl = rtmpUrl;
			endpoint.type = "generic";

			success = getDataStore().addEndpoint(id, endpoint);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Result(success, message);
	}

	protected Broadcast lookupBroadcast(String id) {
		Broadcast broadcast = null;
		try {
			broadcast = getDataStore().get(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return broadcast;
	}

	/**
	 * 
	 * @param id
	 * @return nothing if broadcast is not found
	 */
	@GET
	@Path("/broadcast/get")
	@Produces(MediaType.APPLICATION_JSON)
	public Broadcast getBroadcast(@QueryParam("id") String id) {
		Broadcast broadcast = null;
		if (id != null) {
			broadcast = lookupBroadcast(id);
		}
		if (broadcast == null) {
			broadcast = new Broadcast(null, null);
		}
		return broadcast;
	}

	@GET
	@Path("/broadcast/getList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> getBroadcastList(@PathParam("offset") int offset, @PathParam("size") int size) {
		return getDataStore().getBroadcastList(offset, size);
	}

	@GET
	@Path("/broadcast/getVodList/{offset}/{size}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Vod> getVodList(@PathParam("offset") int offset, @PathParam("size") int size) {
		return getDataStore().getVodList(offset, size);
	}

	@GET
	@Path("/broadcast/filterList/{offset}/{size}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Broadcast> filterBroadcastList(@PathParam("offset") int offset, @PathParam("size") int size,
			@PathParam("type") String type) {
		return getDataStore().filterBroadcastList(offset, size, type);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/filterVoD")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Vod> filterVoDList(SearchParam searchparam, @QueryParam("offset") int offset,
			@QueryParam("size") int size) {

		return getDataStore().filterVoDList(offset, size, searchparam.getKeyword(), searchparam.getStartDate(),
				searchparam.getEndDate());
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/deleteVoDFile/{name}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteVoDFile(@PathParam("name") String fileName, @PathParam("id") String id) {
		boolean success = false;
		String message = "";
		if (getAppContext() != null) {
			// TODO: write test code for this function
			File recordFile = Muxer.getRecordFile(getScope(), fileName, ".mp4");
			System.out.println("recordfile : " + recordFile.getAbsolutePath());
			if (recordFile.exists()) {
				success = true;
				recordFile.delete();
				getDataStore().deleteVod(id);
			} else {
				message = "No file to delete";
			}
			File previewFile = Muxer.getPreviewFile(getScope(), fileName, ".png");
			if (previewFile.exists()) {
				previewFile.delete();
			}

			if (getAppContext().containsBean("app.storageClient")) {
				StorageClient storageClient = (StorageClient) getAppContext().getBean("app.storageClient");

				storageClient.delete(fileName + ".mp4", FileType.TYPE_STREAM);
				storageClient.delete(fileName + ".png", FileType.TYPE_PREVIEW);
			}

		}
		return new Result(success, message);
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/delete/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result deleteBroadcast(@PathParam("id") String id) {
		boolean success = false;
		String message = null;

		Broadcast cam = getDataStore().getCamera(id);

		if (cam.getType().equals("ipCamera")) {

			AntMediaApplicationAdapter application = getApplication();

			if (application instanceof IPCameraApplicationAdapter) {
				((IPCameraApplicationAdapter) application).stopCameraStreaming(cam);
				success = getDataStore().deleteCamera(id);
				message = "ip camera is deleted";
				logger.info("ipcam is deleted");

			} else {

				logger.info("broadcast is not an IP Camera");
			}

		}

		else if (getDataStore().delete(id)) {
			success = true;
			message = "broadcast is deleted";

			logger.info("broadcast is deleted");

			// if (getAppContext() != null)
			// {
			// File recordFile = Muxer.getRecordFile(getScope(), id, "mp4");
			// if (recordFile.exists()) {
			// recordFile.delete();
			// }
			// File previewFile = Muxer.getPreviewFile(getScope(), id, "png");
			// if (previewFile.exists()) {
			// previewFile.delete();
			// }
			//
			// if (getAppContext().containsBean("app.storageClient"))
			// {
			// StorageClient storageClient = (StorageClient)
			// getAppContext().getBean("app.storageClient");
			//
			// storageClient.delete(id + ".mp4", FileType.TYPE_STREAM);
			// storageClient.delete(id + ".png", FileType.TYPE_PREVIEW);
			//
			// ApplicationContext appContext = getAppContext();
			// if (appContext.containsBean("app.settings")) {
			// AppSettings bean = (AppSettings)
			// appContext.getBean("app.settings");
			// List<EncoderSettings> encoderSettingsList =
			// bean.getAdaptiveResolutionList();
			// if (encoderSettingsList != null) {
			// for (EncoderSettings settings : encoderSettingsList) {
			// storageClient.delete(id + "_" +settings.getHeight() +"p.mp4",
			// FileType.TYPE_STREAM);
			// }
			// }
			// }
			// }
			// }
		}

		return new Result(success, message);
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/broadcast/getDeviceAuthParameters/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Object getDeviceAuthParameters(@PathParam("serviceName") String serviceName) {
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		String message = null;
		boolean serviceFound = false;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				if (videoServiceEndpoint.getName().equals(serviceName)) {
					serviceFound = true;
					try {
						if (videoServiceEndpoint.isInitialized()) {
							DeviceAuthParameters askDeviceAuthParameters = videoServiceEndpoint
									.askDeviceAuthParameters();
							getApplication().startDeviceAuthStatusPolling(videoServiceEndpoint,
									askDeviceAuthParameters);
							return askDeviceAuthParameters;
						} else {
							message = "Please enter service client id and client secret in app configuration";
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			if (!serviceFound) {
				message = "Service with the name specified is not found in this app";
			}
		} else {
			message = "No endpoint is defined for this app";
		}
		return new Result(false, message);
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/checkDeviceAuthStatus/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result checkDeviceAuthStatus(@PathParam("serviceName") String serviceName) {
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		boolean authenticated = false;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				if (videoServiceEndpoint.getName().equals(serviceName)) {
					authenticated = videoServiceEndpoint.isInitialized() && videoServiceEndpoint.isAuthenticated();
				}
			}
		}
		return new Result(authenticated, null);
	}

	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialNetworkChannel/{serviceName}")
	@Produces(MediaType.APPLICATION_JSON)
	public SocialEndpointChannel getSocialNetworkChannel(@PathParam("serviceName") String serviceName) {
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		SocialEndpointChannel channel = null;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				if (videoServiceEndpoint.getName().equals(serviceName)) {
					channel = videoServiceEndpoint.getChannel();
					break;
				}

			}
		}
		return channel;
	}

	@GET
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/getSocialNetworkChannelList/{serviceName}/{type}")
	@Produces(MediaType.APPLICATION_JSON)
	public List<SocialEndpointChannel> getSocialNetworkChannelList(@PathParam("serviceName") String serviceName,
			@PathParam("type") String type) {
		List<VideoServiceEndpoint> endPoint = getEndpointList();
		List<SocialEndpointChannel> channelList = null;
		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				if (videoServiceEndpoint.getName().equals(serviceName)) {
					channelList = videoServiceEndpoint.getChannelList(type);
					break;
				}

			}
		}
		return channelList;
	}

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/broadcast/setSocialNetworkChannel/{serviceName}/{type}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Result setSocialNetworkChannelList(@PathParam("serviceName") String serviceName,
			@PathParam("type") String type, @PathParam("id") String id) {
		boolean result = false;
		List<VideoServiceEndpoint> endPoint = getEndpointList();

		if (endPoint != null) {
			for (VideoServiceEndpoint videoServiceEndpoint : endPoint) {
				if (videoServiceEndpoint.getName().equals(serviceName)) {
					result = videoServiceEndpoint.setActiveChannel(type, id);
					break;
				}

			}
		}
		return new Result(result, null);
	}

	public long getRecordCount() {
		return getDataStore().getBroadcastCount();
	}

	protected List<VideoServiceEndpoint> getEndpointList() {
		return ((AntMediaApplicationAdapter) getApplication()).getVideoServiceEndpoints();
	}

	private ApplicationContext getAppContext() {
		if (appCtx == null && servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}

	public IDataStore getDataStore() {
		if (dataStore == null) {
			dataStore = (IDataStore) getAppContext().getBean("db.datastore");
		}
		return dataStore;
	}

	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}

	protected AntMediaApplicationAdapter getApplication() {
		if (app == null) {
			app = (AntMediaApplicationAdapter) getAppContext().getBean("web.handler");
		}
		return app;
	}

	private AppSettings getAppSettings() {
		if (appSettings == null) {
			appSettings = (AppSettings) getAppContext().getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	private IScope getScope() {
		if (scope == null) {
			scope = getApplication().getScope();
		}
		return scope;
	}

	public void setAppCtx(ApplicationContext appCtx) {
		this.appCtx = appCtx;
	}

	public void setAppSettings(AppSettings appSettings) {
		this.appSettings = appSettings;
	}

}
