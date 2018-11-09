package io.antmedia.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Token;

@WebListener
public class TokenService implements ApplicationContextAware, IStreamPublishSecurity, HttpSessionListener{

	public static final String BEAN_NAME = "token.service";
	protected static Logger logger = LoggerFactory.getLogger(TokenService.class);
	private AppSettings settings;
	private IDataStore dataStore;


	Map<String, String> authenticatedMap = new ConcurrentHashMap<>();
	private ApplicationContext applicationContext;


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {

		this.applicationContext = applicationContext;
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

			if(getDataStore().validateToken(token)!= null) {
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
		if(dataStore == null) {
			dataStore = ((DataStoreFactory) applicationContext.getBean(IDataStoreFactory.BEAN_NAME)).getDataStore();
		}
		return dataStore;
	}

	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}
	

	public Map<String, String> getAuthenticatedMap() {
		return authenticatedMap;
	}

	public void setAuthenticatedMap(Map<String, String> authenticatedMap) {
		this.authenticatedMap = authenticatedMap;
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		logger.info("session created {}", se.getSession().getId());
		
	}


	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		logger.info("session closed {}", se.getSession().getId());
		
		
		if(!authenticatedMap.isEmpty() && authenticatedMap.containsKey(se.getSession().getId())) {
			
			authenticatedMap.remove(se.getSession().getId());
		}
		
	}


}
