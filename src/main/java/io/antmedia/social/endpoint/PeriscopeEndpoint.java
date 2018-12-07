package io.antmedia.social.endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.api.periscope.AuthorizationEndpoints;
import io.antmedia.api.periscope.BroadcastEndpoints;
import io.antmedia.api.periscope.ChatEndpoints;
import io.antmedia.api.periscope.PeriscopeEndpointFactory;
import io.antmedia.api.periscope.RegionEndpoints;
import io.antmedia.api.periscope.UserEndpoints;
import io.antmedia.api.periscope.response.AuthorizationResponse;
import io.antmedia.api.periscope.response.CheckDeviceCodeResponse;
import io.antmedia.api.periscope.response.CreateBroadcastResponse;
import io.antmedia.api.periscope.response.CreateDeviceCodeResponse;
import io.antmedia.api.periscope.type.Broadcast;
import io.antmedia.api.periscope.type.IChatListener;
import io.antmedia.api.periscope.type.User;
import io.antmedia.api.periscope.type.chatEndpointTypes.ChatMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.ErrorMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.HeartMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.JoinMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.ScreenshotMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.ShareMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.SuperHeartMessage;
import io.antmedia.api.periscope.type.chatEndpointTypes.ViewerCountMessage;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.BroadcastStatus;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.rest.model.Interaction;
import io.antmedia.social.LiveComment;
import io.antmedia.social.ResourceOrigin;
import io.vertx.core.Vertx;

public class PeriscopeEndpoint extends VideoServiceEndpoint {

	private static final String FIRST_AUTHENTICATED_THE_SERVER = "First authenticated the server";
	private AuthorizationEndpoints authorizationEndpoint;
	private String device_code;
	private PeriscopeEndpointFactory periscopeEndpointFactory;
	private BroadcastEndpoints broadcastEndpoint;
	private RegionEndpoints regionEndpoint;
	private String accessToken;
	private String region;
	private long expireTimeMS;
	private UserEndpoints userEndpoint;
	private String refresh_token;
	private ChatEndpoints chatEndpoint;
	protected Map<String, Integer> viewerCountMap = new HashMap<>();

	private Map<String, List<LiveComment>> commentMapList = new HashMap<>();

	private Map<String, Interaction> interactionMap = new HashMap<>();
		
	private static Logger logger = LoggerFactory.getLogger(PeriscopeEndpoint.class);
	public class ChatListener implements IChatListener {

		private Endpoint endpoint;

		public ChatListener(Endpoint endpoint) {
			this.endpoint = endpoint;
		}

		@Override
		public void viewerCountMessageReceived(ViewerCountMessage viewerCountMessage) {
			logger.debug("viewerCountMessageReceived live view {}", viewerCountMessage.live);
			if (viewerCountMessage.total < 0) {
				viewerCountMessage.total = 0;
			}
			viewerCountMap.put(endpoint.getServerStreamId(), viewerCountMessage.total);
		}

		@Override
		public void screenshotMessageReceived(ScreenshotMessage screenshotMessage) {
			//No need to implement
		}

		@Override
		public void errorMessageReceived(ErrorMessage errorMessage) {
			logger.error("errorMessageReceived  {} for stream {} ", errorMessage.description, endpoint.getStreamId());
		}

		@Override
		public void chatMessageReceived(ChatMessage chatMessage) {
			logger.debug("chatMessageReceived {} from {} for stream {} ", chatMessage.text,chatMessage.user.display_name, endpoint.getStreamId());

			List<LiveComment> list = commentMapList.get(endpoint.getServerStreamId());
			if (list == null) {
				list = new ArrayList<>();
			}
			io.antmedia.rest.model.User from = new io.antmedia.rest.model.User();
			from.setId(chatMessage.user.id);
			from.setFullName(chatMessage.user.display_name);
			from.setPicture(chatMessage.user.profile_image_urls.get(0).url);
			list.add(new LiveComment(chatMessage.id, chatMessage.text, from, ResourceOrigin.PERISCOPE, System.currentTimeMillis()));
			commentMapList.put(endpoint.getServerStreamId(), list);
		}

		@Override
		public void heartMessageReceived(HeartMessage heartMessage) {
			logger.debug("heart MessageReceived for stream {}", endpoint.getServerStreamId());
			Interaction interaction = interactionMap.get(endpoint.getServerStreamId());
			if (interaction == null) {
				interaction = new Interaction();
			}
			interaction.setLoveCount(interaction.getLoveCount()+1);
			interaction.setOrigin(ResourceOrigin.PERISCOPE);
			interactionMap.put(endpoint.getServerStreamId(), interaction);
		}

		@Override
		public void superheartMessageReceived(SuperHeartMessage heartMessage) {
			//No need to implement
		}

		@Override
		public void joinMessageReceived(JoinMessage joinMessage) {
			//No need to implement
		}

		@Override
		public void shareMessageReceived(ShareMessage shareMessage) {
			//No need to implement
		}
	}


	public PeriscopeEndpoint(String clientId, String clientSecret, IDataStore dataStore, SocialEndpointCredentials endpointCredentials, Vertx vertx) {
		super(clientId, clientSecret, dataStore, endpointCredentials, vertx);
	}


	@Override
	public String getName() {
		return AntMediaApplicationAdapter.PERISCOPE;
	}

	@Override
	public DeviceAuthParameters askDeviceAuthParameters() throws Exception {
		AuthorizationEndpoints authorizationEndpointTmp = getAuthorizationEndpoint();
		CreateDeviceCodeResponse response = authorizationEndpointTmp.createDeviceCode(getClientId(), ChatEndpoints.CHAT_SCOPE);
		if (response != null) {
			authParameters = new DeviceAuthParameters();
			authParameters.device_code = response.device_code;
			this.device_code = response.device_code;
			authParameters.expires_in = response.expires_in;
			authParameters.interval = response.interval;
			authParameters.user_code = response.user_code;
			authParameters.verification_url = response.associate_url;
		}

		return getAuthParameters();
	}

	private AuthorizationEndpoints getAuthorizationEndpoint() {
		if (authorizationEndpoint == null) {
			authorizationEndpoint = new AuthorizationEndpoints();
		}
		return authorizationEndpoint;
	}

	@Override
	public boolean askIfDeviceAuthenticated() throws Exception {
		AuthorizationEndpoints authorizationEndpointTmp = getAuthorizationEndpoint();

		CheckDeviceCodeResponse checkDeviceCode = authorizationEndpointTmp.checkDeviceCode(device_code, getClientId());

		logger.warn("State: {}" , checkDeviceCode.state);

		boolean result = false;
		if ( checkDeviceCode.state.equals("associated")) {
			init("", checkDeviceCode.access_token, checkDeviceCode.refresh_token, (long)checkDeviceCode.expires_in, checkDeviceCode.token_type, System.currentTimeMillis());
			String accountName = "";
			String accountId = "";
			try {
				User userResponse = userEndpoint.get();
				accountName = userResponse.username;
				accountId = userResponse.id;
				logger.info("authenticated account name is {}", accountName);
			}
			catch (Exception e) {
				//even if throw exception, catch here and save the record below lines
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			saveCredentials(accountName, checkDeviceCode.access_token, checkDeviceCode.refresh_token, String.valueOf(checkDeviceCode.expires_in), checkDeviceCode.token_type, accountId);
			result = true;
		}
		return result;
	}

	@Override
	public boolean isAuthenticated() {
		return periscopeEndpointFactory != null && accessToken != null && (accessToken.length() > 0);
	}

	@Override
	public void resetCredentials() {
		super.resetCredentials();
		accessToken = null;

	}

	@Override
	public Endpoint createBroadcast(String name, String description, String serverStreamId, boolean is360, boolean isPublic,
			int videoHeight, boolean isLowLatency) throws IOException{
		if (broadcastEndpoint == null) {
			throw new NullPointerException(FIRST_AUTHENTICATED_THE_SERVER);
		}

		
		try {
			updateTokenIfRequired();
			CreateBroadcastResponse createBroadcastResponse = broadcastEndpoint.createBroadcast(getRegion(), is360, isLowLatency);

			String rtmpUrl = createBroadcastResponse.encoder.rtmp_url + "/" + createBroadcastResponse.encoder.stream_key;
			return new Endpoint(createBroadcastResponse.broadcast.id, null, name, rtmpUrl, getName(), getCredentials().getId(), serverStreamId);

		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			throw new IOException(e.getMessage());
		}
	}

	private String getRegion() {
		if (region == null) {
			try {
				region = regionEndpoint.get();
			} catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
		}
		return region;
	}


	@Override
	public void publishBroadcast(Endpoint endpoint) throws Exception {
		if (broadcastEndpoint == null) {
			throw new NullPointerException(FIRST_AUTHENTICATED_THE_SERVER);
		}
		if (endpoint.getBroadcastId() == null) {
			throw new NullPointerException("No broadcast is available, call createBroadcast function before calling publish broadcast");
		}
		updateTokenIfRequired();
		broadcastEndpoint.publishBroadcast(endpoint.getBroadcastId(), endpoint.getName(), false, "en_US", true);

		if (collectInteractivity) {
			connectToChatEndpoint(endpoint);
		}
	}

	@Override
	public void stopBroadcast(Endpoint endpoint) throws Exception {
		if (broadcastEndpoint == null) {
			throw new NullPointerException(FIRST_AUTHENTICATED_THE_SERVER);
		}
		if (endpoint.getBroadcastId() == null) {
			throw new NullPointerException("No broadcast is available");
		}
		updateTokenIfRequired();
		broadcastEndpoint.stopBroadcast(endpoint.getBroadcastId());
		viewerCountMap.remove(endpoint.getServerStreamId());
		commentMapList.remove(endpoint.getServerStreamId());
		interactionMap.remove(endpoint.getServerStreamId());
	}

	private void updateTokenIfRequired() throws Exception {
		if (expireTimeMS < (System.currentTimeMillis() + THREE_DAYS_IN_MS)) {
			updateToken();
		}
	}

	public void updateToken() throws Exception 
	{
		AuthorizationResponse token = periscopeEndpointFactory.refreshToken(clientId, clientSecret);
		if (token.refresh_token == null || token.refresh_token.length() == 0) {
			token.refresh_token = this.refresh_token;
		}
		saveCredentials(getCredentials().getAccountName(), token.access_token, token.refresh_token, String.valueOf(token.expires_in), token.token_type, getCredentials().getAccountId());
		init(getCredentials().getAccountName(), token.access_token, token.refresh_token, Long.valueOf(token.expires_in), token.token_type, System.currentTimeMillis());
	}

	@Override
	public void init(String accountName, String accessToken, String refreshToken, long expireTime, String tokenType, long authTimeInMS) {
		this.accessToken = accessToken;
		this.refresh_token = refreshToken;
		periscopeEndpointFactory = new PeriscopeEndpointFactory(tokenType, accessToken, refreshToken);
		expireTimeMS = authTimeInMS + expireTime * 1000;
		broadcastEndpoint = periscopeEndpointFactory.getBroadcastEndpoints();
		regionEndpoint = periscopeEndpointFactory.getRegionEndpoints();
		userEndpoint = periscopeEndpointFactory.getUserEndpoints();
		chatEndpoint = periscopeEndpointFactory.getChatEndpoints();
	}


	@Override
	public String getBroadcast(Endpoint endpoint) throws Exception {
		Broadcast broadcast = broadcastEndpoint.getBroadcast(endpoint.getBroadcastId());

		return broadcast.state.equals("running") ? BroadcastStatus.LIVE_NOW : BroadcastStatus.UNPUBLISHED;
	}


	public void connectToChatEndpoint(final Endpoint endpoint) {
		chatEndpoint.connect(endpoint.getBroadcastId(), getNewChatListener(endpoint));
	}


	public IChatListener getNewChatListener(Endpoint endpoint) {
		return new ChatListener(endpoint);
	}


	public String getAccountName() {
		return getCredentials().getAccountName();
	}


	public void setAccountName(String accountName) {
		getCredentials().setAccountName(accountName);
	}

	@Override
	public List<LiveComment> getComments(String streamId, int offset, int batch) {
		List<LiveComment> comments = commentMapList.get(streamId);
		List<LiveComment> resultList = null;
		if (comments != null) {
			int size = comments.size();
			if (offset < size) {
				int toIndex = offset + batch;
				if (toIndex > size) {
					toIndex = size;
				}
				resultList = comments.subList(offset, toIndex);
			}
		}
		return resultList;
	}

	@Override
	public Interaction getInteraction(String streamId) {
		return interactionMap.getOrDefault(streamId, null);
	}

	@Override
	public  int getTotalCommentsCount(String streamId) {
		List<LiveComment> comments = commentMapList.get(streamId);
		if (comments != null)  {
			return comments.size();
		}
		return 0;
	}

	@Override
	public long getLiveViews(String streamId) {
		return viewerCountMap.getOrDefault(streamId, 0);
	}

}
