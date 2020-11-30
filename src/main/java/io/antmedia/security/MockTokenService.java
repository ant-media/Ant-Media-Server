package io.antmedia.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;

import io.antmedia.datastore.db.types.Token;


public class MockTokenService implements  IStreamPublishSecurity, ITokenService{

	Map<String, String> authenticatedMap = new ConcurrentHashMap<>();
	Map<String, String> subscriberAuthenticatedMap = new ConcurrentHashMap<>();

	public boolean checkToken(String tokenId, String streamId, String sessionId, String type) {
		return true;
	}

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams) {
		return true;
	}

	@Override
	public Token createToken(String streamId, long exprireDate, String type, String roomId) {
		return null;
	}

	@Override
	public Map<String, String> getAuthenticatedMap() {
		return authenticatedMap;
	}
	
	@Override
	public Map<String, String> getSubscriberAuthenticatedMap() {
		return subscriberAuthenticatedMap;
	}

	@Override
	public boolean checkHash(String hash, String streamId, String sessionId, String type) {
		return true;
	}

	@Override
	public boolean checkTimeBasedSubscriber(String subscriberId, String streamId, String sessionId,
			String subscriberCode, boolean forPublish) {
		return true;
	}

}
