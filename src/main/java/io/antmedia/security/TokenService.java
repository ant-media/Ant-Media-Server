package io.antmedia.security;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
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
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		dataStore = (IDataStore) applicationContext.getBean(IDataStore.BEAN_NAME);

		if (applicationContext.containsBean(AppSettings.BEAN_NAME)) {
			settings = (AppSettings)applicationContext.getBean(AppSettings.BEAN_NAME);

		}

	}

	public boolean checkToken(String tokenId, String streamId, String sessionId) {
		boolean result = false;

		if(streamId != null && sessionId != null ) {
			Token token = new Token();
			token.setTokenId(tokenId);
			token.setStreamId(streamId);

			if(dataStore.validateToken(token)!= null) {
				result = true;	
				authenticatedMap.put(sessionId, streamId);
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
	public boolean isPublishAllowed(IScope scope, String name, String mode) {

		
		
		
		
		return false;
	}


}
