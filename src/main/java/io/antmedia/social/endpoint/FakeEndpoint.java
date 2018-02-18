package io.antmedia.social.endpoint;

import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.preference.PreferenceStore;

public class FakeEndpoint extends VideoServiceEndpoint {

	public FakeEndpoint(String clientId, String clientSecret, PreferenceStore dataStore) {
		super(clientId, clientSecret, dataStore);
	}

	@Override
	public void init(String accessToken, String refreshToken, long expireTime, String tokenType,
			long authtimeInMilliSeconds) {
	}

	@Override
	public String getName() {
		return "fake";
	}

	@Override
	public DeviceAuthParameters askDeviceAuthParameters() throws Exception {
		return null;
	}

	@Override
	public boolean askIfDeviceAuthenticated() throws Exception {
		return false;
	}

	@Override
	public boolean isAuthenticated() {
		return false;
	}

	@Override
	public Endpoint createBroadcast(String name, String description, boolean is360, boolean isPublic, int videoHeight)
			throws Exception {
		return null;
	}

	@Override
	public void publishBroadcast(Endpoint endpoint) throws Exception {

	}

	@Override
	public void stopBroadcast(Endpoint endpoint) throws Exception {

	}

}
