package io.antmedia.social.endpoint;

import java.util.List;

import io.antmedia.api.periscope.type.Broadcast;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.BroadcastStatus;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.rest.model.Interaction;
import io.antmedia.social.LiveComment;

public class FakeEndpoint extends VideoServiceEndpoint {

	public FakeEndpoint(String clientId, String clientSecret, IDataStore dataStore, SocialEndpointCredentials credentials) {
		super(clientId, clientSecret, dataStore, credentials);
	}

	@Override
	public void init(String accountName, String accessToken, String refreshToken, long expireTime, String tokenType,
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
	public Endpoint createBroadcast(String name, String description, boolean is360, boolean isPublic, int videoHeight, boolean is_low_latency)
			throws Exception {
		return null;
	}

	@Override
	public void publishBroadcast(Endpoint endpoint) throws Exception {

	}

	@Override
	public void stopBroadcast(Endpoint endpoint) throws Exception {

	}

	@Override
	public String getBroadcast(Endpoint endpoint) {
		return null;
	}

	@Override
	public List<LiveComment> getComments(String streamId, int offset, int batch) {
		//not yet implemented
		return null;
	}

	@Override
	public Interaction getInteraction(String streamId) {
		// not yet implemented
		return null;
	}
	
	public int getTotalCommentsCount(String streamId) {
		return 0;
	}
	
	@Override
	public long getLiveViews(Endpoint endpoint) {
		return 0;
	}

}
