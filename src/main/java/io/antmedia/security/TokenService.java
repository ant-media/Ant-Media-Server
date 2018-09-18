package io.antmedia.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Token;


public class TokenService implements ApplicationContextAware, IStreamPublishSecurity{

	public static final String BEAN_NAME = "token.service";
	protected static Logger logger = LoggerFactory.getLogger(TokenService.class);
	private AppSettings settings;
	private IDataStore dataStore;


	Map<String, String> authenticatedMap = new ConcurrentHashMap<>();


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {

		dataStore = (IDataStore) applicationContext.getBean(IDataStore.BEAN_NAME);

		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);

		}

	}

	public boolean checkToken(String tokenId, String streamId, String sessionId, String type) {
		boolean result = false;

		if(streamId != null && sessionId != null ) {
			Token token = new Token();
			token.setTokenId(tokenId);
			token.setStreamId(streamId);
			token.setType(type);

			if(dataStore.validateToken(token)!= null) {
				result = true;	
				if(type.equals(Token.PLAY_TOKEN)) {
					authenticatedMap.put(sessionId, streamId);
				}
			}
			else {

				if (authenticatedMap.containsKey(sessionId) && authenticatedMap.get(sessionId).equals(streamId) ) {
					result = true;
				}

			}

		}
		return result;
	}

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams) {
		boolean result = true;

		if(settings.isTokenControlEnabled()) {
			result = false;
			if(queryParams == null || !queryParams.containsKey("token")) {
				Red5.getConnectionLocal().close();
				return false;
			}

			String token = queryParams.get("token");

			if(checkToken(token, name, "sessionId", Token.PUBLISH_TOKEN)) {

				result = true;
			}
			else {

				logger.info("Token {} is not valid for publishing ", token);
				Red5.getConnectionLocal().close();
			}
		}
		return result;
	}
	public AppSettings getSettings() {
		return settings;
	}

	public void setSettings(AppSettings settings) {
		this.settings = settings;
	}

	public IDataStore getDataStore() {
		return dataStore;
	}

	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}


}
