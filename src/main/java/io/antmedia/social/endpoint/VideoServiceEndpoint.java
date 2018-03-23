package io.antmedia.social.endpoint;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.api.periscope.type.Broadcast;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.BroadcastStatus;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.preference.PreferenceStore;

/**
 * This is inteface that is used to integrate
 * video services like youtube, facebook, periscope etc. 
 * @author mekya
 *
 */
public abstract class VideoServiceEndpoint {

	public static final Long THREE_DAYS_IN_MS = 1000 * 60 * 60 * 24 * 3L; 


	protected static Logger logger = LoggerFactory.getLogger(VideoServiceEndpoint.class);

	public static class DeviceAuthParameters {
		/**
		 * device code 
		 */
		public String device_code;

		/**
		 * user code
		 */
		public String user_code;

		/**
		 * verificatin url to send the device code
		 */
		public String verification_url;

		/**
		 * The time in milliseconds, that the device_code and user_code are valid.
		 */
		public int expires_in;

		/**
		 * The length of time, in seconds, that your device should wait between polling requests
		 */
		public int interval;
	}

	protected String clientId;

	protected String clientSecret;

	protected IDataStore dataStore;

	/**
	 * id that is saved in db
	 */
	private String id;
	
	private SocialEndpointCredentials credentials;

	public VideoServiceEndpoint(String clientId, String clientSecret, IDataStore dataStore, String id) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.dataStore = dataStore;
		this.id = id;
		credentials = dataStore.getSocialEndpointCredentials(this.id);

	}

	public void start() {
		credentials = dataStore.getSocialEndpointCredentials(this.id);
		if (credentials != null) {
			//throw new NullPointerException("There is not a credential in datastore having id: " + this.id);

			String expireTimeSeconds = credentials.getExpireTimeInSeconds();
			String authtimeMilliSeconds = credentials.getAuthTimeInMillisecoonds();
			long expireTime = 0;
			long authTime = 0;
			if (expireTimeSeconds != null) 
			{
				expireTime = Long.valueOf(expireTimeSeconds);
			}
			if (authtimeMilliSeconds != null) {
				authTime = Long.valueOf(authtimeMilliSeconds);
			}
			try {
				init(credentials.getAccountName(), credentials.getAccessToken(), credentials.getRefreshToken(), 
						expireTime, credentials.getTokenType(),
						authTime);

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			init(null, null, null, 0, null, 0);
		}
	}

	/**
	 * 
	 * @param accessToken
	 * @param refreshToken
	 * @param expireTime in seconds
	 * @param tokenType
	 * @param authtimeInMilliSeconds System.currenTimeMillis value of the authentication time
	 */
	public abstract void init(String accountName, String accessToken, String refreshToken, long expireTime, String tokenType, long authtimeInMilliSeconds);

	public void saveCredentials(String accountName, String accessToken, String refreshToken, String expireTimeInSeconds, String token_type, String accountId) throws Exception {

		SocialEndpointCredentials credentials = new SocialEndpointCredentials(accountName, getName(), String.valueOf(System.currentTimeMillis()), expireTimeInSeconds, token_type, accessToken, refreshToken);
		credentials.setId(this.id);
		credentials.setAccountId(accountId);
		SocialEndpointCredentials addedCredential = dataStore.addSocialEndpointCredentials(credentials);
		if (addedCredential == null) {
			throw new Exception("Social endpoint credential cannot be added for account name: " + accountName + " and service name " + getName() );
		}

	}

	public void resetCredentials() {
		boolean removeSocialEndpointCredentials = dataStore.removeSocialEndpointCredentials(this.id);
		if (!removeSocialEndpointCredentials) {
			logger.warn("Social endpoint is not deleted having id: " + this.id + " and service name: " + getName());
		}
		this.credentials = null;
	}

	/**
	 * Name of the service such as youtube, facebook, periscope
	 * @return
	 */
	public abstract String getName();


	/**
	 * 
	 * @return the device authentication parameters for this service
	 */
	public abstract DeviceAuthParameters askDeviceAuthParameters() throws Exception; 


	/**
	 * Checks if user authenticates the server,
	 * 
	 * @return DeviceTokenParameters if user authenticate the server, 
	 * 	null if it is not yet authenticated
	 */
	public abstract boolean askIfDeviceAuthenticated() throws Exception;


	/**
	 * If the app authenticate the server to publish live streams
	 * @return
	 */
	public abstract boolean isAuthenticated();


	/**
	 * Creates broadcast in the video service
	 * 
	 * @param name give a name do not make null or zero length
	 * @param description description of the broadcast
	 * @param is_360 if this video is 360 degree
	 * @param isPublic if this video will be public or not
	 * @param videoHeight height of the video
	 * 
	 * @return the Endpoint which includes rtmp url
	 */
	public abstract Endpoint createBroadcast(String name, String description, boolean is360, boolean isPublic, int videoHeight, boolean is_low_latency) throws Exception;


	/**
	 * Publishes broadcast in the service
	 * 
	 * @param title of the video
	 * @param description of the video
	 * @param locale locale 
	 * @throws Exception if it is not 
	 */
	public abstract void publishBroadcast(Endpoint endpoint) throws Exception;


	/**
	 * Stop the broadcast
	 * @throws Exception if it is not successful
	 */
	public abstract void stopBroadcast(Endpoint endpoint) throws Exception;


	public abstract String getBroadcast(Endpoint endpoint) throws Exception;

	protected String getClientId() {
		return clientId;
	}

	protected void setClientId(String clientId) {
		this.clientId = clientId;
	}

	protected String getClientSecret() {
		return clientSecret;
	}

	protected void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public boolean isInitialized() {
		return (clientId != null) && (clientSecret != null) && (clientId.length() > 0) && (clientSecret.length() > 0);
	}

	/**
	 * Implement this function, it social media supports different channels
	 * @return
	 */
	public SocialEndpointChannel getChannel() {
		return null;
	}

	/**
	 * Get channel list from social media
	 * @param type of the channel if exists like page, event, group
	 * @return
	 */
	public List<SocialEndpointChannel> getChannelList(String type) {
		return null;
	}

	/**
	 * Set the active channel
	 * @param type
	 * @param id
	 */
	public boolean setActiveChannel(String type, String id) {
		return false;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public SocialEndpointCredentials getCredentials() {
		return credentials;
	}

	public void setCredentials(SocialEndpointCredentials credentials) {
		this.credentials = credentials;
	}




}
