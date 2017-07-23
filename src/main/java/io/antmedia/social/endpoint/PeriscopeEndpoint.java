package io.antmedia.social.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.periscope.AuthorizationEndpoints;
import io.antmedia.periscope.BroadcastEndpoints;
import io.antmedia.periscope.PeriscopeEndpointFactory;
import io.antmedia.periscope.RegionEndpoints;
import io.antmedia.periscope.response.CheckDeviceCodeResponse;
import io.antmedia.periscope.response.CreateBroadcastResponse;
import io.antmedia.periscope.response.CreateDeviceCodeResponse;

public class PeriscopeEndpoint extends VideoServiceEndpoint {

	private static final String serviceName = "periscope";
	private AuthorizationEndpoints authorizationEndpoint;
	private String device_code;
	private PeriscopeEndpointFactory periscopeEndpointFactory;
	private BroadcastEndpoints broadcastEndpoint;
	private RegionEndpoints regionEndpoint;
	private String accessToken;
	private String region;

	protected static Logger logger = LoggerFactory.getLogger(PeriscopeEndpoint.class);


	public PeriscopeEndpoint(String clientId, String clientSecret, PreferenceStore dataStore) {
		super(clientId, clientSecret, dataStore);
	}


	@Override
	public String getName() {
		return serviceName;
	}

	@Override
	public DeviceAuthParameters askDeviceAuthParameters() throws Exception {
		AuthorizationEndpoints authorizationEndpoint = getAuthorizationEndpoint();
		CreateDeviceCodeResponse response = authorizationEndpoint.createDeviceCode(getClientId());
		DeviceAuthParameters authParameters = null;
		if (response != null) {
			authParameters = new DeviceAuthParameters();
			authParameters.device_code = response.device_code;
			this.device_code = response.device_code;
			authParameters.expires_in = response.expires_in;
			authParameters.interval = response.interval;
			authParameters.user_code = response.user_code;
			authParameters.verification_url = response.associate_url;
		}

		return authParameters;
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
			saveCredentials(checkDeviceCode.access_token, checkDeviceCode.refresh_token, String.valueOf(checkDeviceCode.expires_in), checkDeviceCode.token_type);
			init(checkDeviceCode.access_token, checkDeviceCode.refresh_token, (long)checkDeviceCode.expires_in, checkDeviceCode.token_type);
			result = true;
		}
		return result;
	}

	@Override
	public boolean isAuthenticated() {
		return periscopeEndpointFactory != null && accessToken != null && (accessToken.length() > 0);
	}

	@Override
	public Endpoint createBroadcast(String name, String description, boolean is360, boolean isPublic,
			int videoHeight) throws Exception {
		if (broadcastEndpoint == null) {
			throw new Exception("First authenticated the server");
		}
		
		CreateBroadcastResponse createBroadcastResponse = broadcastEndpoint.createBroadcast(getRegion(), is360);
		
		String rtmpUrl = createBroadcastResponse.encoder.rtmp_url + "/" + createBroadcastResponse.encoder.stream_key;
		return new Endpoint(createBroadcastResponse.broadcast.id, null, name, rtmpUrl, getName());
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
		broadcastEndpoint.publishBroadcast(endpoint.broadcastId, endpoint.name, true, "en_US");
	}

	@Override
	public void stopBroadcast(Endpoint endpoint) throws Exception {
		if (broadcastEndpoint == null) {
			throw new Exception("First authenticated the server");
		}
		if (endpoint.broadcastId == null) {
			throw new Exception("No broadcast is available");
		}
		broadcastEndpoint.stopBroadcast(endpoint.broadcastId);
	}

	@Override
	public void init(String accessToken, String refreshToken, Long expireTime, String tokenType) {
		this.accessToken = accessToken;
		periscopeEndpointFactory = new PeriscopeEndpointFactory(tokenType, accessToken, refreshToken);
		broadcastEndpoint = periscopeEndpointFactory.getBroadcastEndpoints();
	    regionEndpoint = periscopeEndpointFactory.getRegionEndpoints();

	}

}
