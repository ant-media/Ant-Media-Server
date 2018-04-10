package io.antmedia.social.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.api.periscope.AuthorizationEndpoints;
import io.antmedia.api.periscope.BroadcastEndpoints;
import io.antmedia.api.periscope.PeriscopeEndpointFactory;
import io.antmedia.api.periscope.RegionEndpoints;
import io.antmedia.api.periscope.UserEndpoints;
import io.antmedia.api.periscope.response.AuthorizationResponse;
import io.antmedia.api.periscope.response.CheckDeviceCodeResponse;
import io.antmedia.api.periscope.response.CreateBroadcastResponse;
import io.antmedia.api.periscope.response.CreateDeviceCodeResponse;
import io.antmedia.api.periscope.response.UserResponse;
import io.antmedia.api.periscope.type.Broadcast;
import io.antmedia.api.periscope.type.User;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.BroadcastStatus;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.preference.PreferenceStore;

public class PeriscopeEndpoint extends VideoServiceEndpoint {

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
	

	protected static Logger logger = LoggerFactory.getLogger(PeriscopeEndpoint.class);

	public PeriscopeEndpoint(String clientId, String clientSecret, IDataStore dataStore, SocialEndpointCredentials endpointCredentials) {
		super(clientId, clientSecret, dataStore, endpointCredentials);
	}


	@Override
	public String getName() {
		return AntMediaApplicationAdapter.PERISCOPE;
	}

	@Override
	public DeviceAuthParameters askDeviceAuthParameters() throws Exception {
		AuthorizationEndpoints authorizationEndpoint = getAuthorizationEndpoint();
		CreateDeviceCodeResponse response = authorizationEndpoint.createDeviceCode(getClientId());
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
		AuthorizationEndpoints authorizationEndpoint = getAuthorizationEndpoint();

		CheckDeviceCodeResponse checkDeviceCode = authorizationEndpoint.checkDeviceCode(device_code, getClientId());

		logger.warn("State: " + checkDeviceCode.state);

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
				e.printStackTrace();
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
	public Endpoint createBroadcast(String name, String description, boolean is360, boolean isPublic,
			int videoHeight, boolean is_low_latency) throws Exception {
		if (broadcastEndpoint == null) {
			throw new Exception("First authenticated the server");
		}

		updateTokenIfRequired();
		CreateBroadcastResponse createBroadcastResponse = broadcastEndpoint.createBroadcast(getRegion(), is360, is_low_latency);

		String rtmpUrl = createBroadcastResponse.encoder.rtmp_url + "/" + createBroadcastResponse.encoder.stream_key;
		return new Endpoint(createBroadcastResponse.broadcast.id, null, name, rtmpUrl, getName(), getCredentials().getId());
	}

	private String getRegion() {
		if (region != null) {
			try {
				region = regionEndpoint.get();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return region;
	}


	@Override
	public void publishBroadcast(Endpoint endpoint) throws Exception {
		if (broadcastEndpoint == null) {
			throw new Exception("First authenticated the server");
		}
		if (endpoint.broadcastId == null) {
			throw new Exception("No broadcast is available, call createBroadcast function before calling publish broadcast");
		}
		updateTokenIfRequired();
		broadcastEndpoint.publishBroadcast(endpoint.broadcastId, endpoint.name, false, "en_US", true);
	}

	@Override
	public void stopBroadcast(Endpoint endpoint) throws Exception {
		if (broadcastEndpoint == null) {
			throw new Exception("First authenticated the server");
		}
		if (endpoint.broadcastId == null) {
			throw new Exception("No broadcast is available");
		}
		updateTokenIfRequired();
		broadcastEndpoint.stopBroadcast(endpoint.broadcastId);
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

	}


	@Override
	public String getBroadcast(Endpoint endpoint) throws Exception {
		Broadcast broadcast = broadcastEndpoint.getBroadcast(endpoint.broadcastId);
		
		return broadcast.state.equals("running") ? BroadcastStatus.LIVE_NOW : BroadcastStatus.UNPUBLISHED;
	}


	public String getAccountName() {
		return getCredentials().getAccountName();
	}


	public void setAccountName(String accountName) {
		getCredentials().setAccountName(accountName);
	}

}
