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


public class MockTokenService implements  IStreamPublishSecurity, ITokenService{

	Map<String, String> authenticatedMap = new ConcurrentHashMap<>();

	public boolean checkToken(String tokenId, String streamId, String sessionId, String type) {

		return true;
	}

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams) {

		return true;
	}

	@Override
	public Token createToken(String streamId, long exprireDate, String type) {

		return null;
	}

	@Override
	public Map<String, String> getAuthenticatedMap() {
		
		return authenticatedMap;
	}

}
