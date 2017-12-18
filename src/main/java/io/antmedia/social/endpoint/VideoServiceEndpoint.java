package io.antmedia.social.endpoint;

import java.util.List;

import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointChannel;
import io.antmedia.datastore.preference.PreferenceStore;

/**
 * This is inteface that is used to integrate
 * video services like youtube, facebook, periscope etc. 
 * @author mekya
 *
 */
public abstract class VideoServiceEndpoint {
	
	protected static final String AUTH_TIME = ".authTime";

	protected static final String EXPIRE_TIME_SECONDS = ".expireTimeSeconds";

	protected static final String REFRESH_TOKEN = ".refreshToken";

	protected static final String ACCESS_TOKEN = ".accessToken";

	protected static final String TOKEN_TYPE = ".tokenType";
	
	public static final Long THREE_DAYS_IN_MS = 1000 * 60 * 60 * 24 * 3L; 

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

	protected PreferenceStore dataStore;

	public VideoServiceEndpoint(String clientId, String clientSecret, PreferenceStore dataStore) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.dataStore = dataStore;
		
	}
	
	public void start() {
		String accessToken = dataStore.get(getName() + ACCESS_TOKEN);
		String refreshToken = dataStore.get(getName() + REFRESH_TOKEN);
		String expireTimeSeconds = dataStore.get(getName() + EXPIRE_TIME_SECONDS);
		String tokenType = dataStore.get(getName() + TOKEN_TYPE);
		String authtimeMilliSeconds = dataStore.get(getName() + AUTH_TIME);
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
			init(accessToken, refreshToken, expireTime, tokenType, authTime);
		}
		catch (Exception e) {
			e.printStackTrace();
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
	public abstract void init(String accessToken, String refreshToken, long expireTime, String tokenType, long authtimeInMilliSeconds);

	public void saveCredentials(String accessToken, String refreshToken, String expireTimeInSeconds, String token_type) {
		if (refreshToken != null) {
			dataStore.put(getName() + REFRESH_TOKEN, refreshToken);
		}
		if (accessToken != null) {
			dataStore.put(getName() + ACCESS_TOKEN, accessToken);
		}
		if (expireTimeInSeconds != null) {
			dataStore.put(getName() + EXPIRE_TIME_SECONDS, expireTimeInSeconds);
		}
		if (token_type != null) {
			dataStore.put(getName() + TOKEN_TYPE, token_type);
		}
		dataStore.put(getName()+ AUTH_TIME, String.valueOf(System.currentTimeMillis()));
		dataStore.save();
	}
	
	public void resetCredentials() {
		dataStore.put(getName() + REFRESH_TOKEN, "");
		dataStore.put(getName() + ACCESS_TOKEN, "");
		dataStore.put(getName() + EXPIRE_TIME_SECONDS, "0");
		dataStore.put(getName() + TOKEN_TYPE, "");
		dataStore.save();
		
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
	public abstract Endpoint createBroadcast(String name, String description, boolean is360, boolean isPublic, int videoHeight) throws Exception;


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




}
