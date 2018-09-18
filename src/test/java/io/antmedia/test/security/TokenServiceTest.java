package io.antmedia.test.security;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.security.TokenService;

public class TokenServiceTest {
	protected static Logger logger = LoggerFactory.getLogger(TokenServiceTest.class);

	IDataStore datastore;
	ApplicationContext applicationContext;

	private TokenService tokenService;
	@Before
	public void before() {
		tokenService = new TokenService();
	}

	@After
	public void after() {
		tokenService = null;
	}


	@Test
	public void testCheckToken() {

		datastore = new InMemoryDataStore("testDb");

		ApplicationContext context = mock(ApplicationContext.class);

		when(context.getBean(IDataStore.BEAN_NAME)).thenReturn(datastore);

		tokenService.setDataStore(datastore);

		//create token

		Token token = new Token();
		token.setStreamId("streamId");
		token.setExpireDate(1454354);
		token.setType(Token.PLAY_TOKEN);

		//save to datastore
		Token createdToken = datastore.createToken(token.getStreamId(), token.getExpireDate(), token.getType());

		logger.info("created token id: " + createdToken.getTokenId());
		logger.info("created token type: " + createdToken.getType());

		//check token
		boolean flag = tokenService.checkToken(createdToken.getTokenId(), createdToken.getStreamId(), "sessionId", createdToken.getType());

		assertTrue(flag);

	}

	@Test
	public void testIsPublishAllowed() {

		datastore = new InMemoryDataStore("testDb");
		ApplicationContext context = mock(ApplicationContext.class);
		AppSettings settings = new AppSettings();
		settings.setTokenControlEnabled(true);
		IScope scope = mock(IScope.class);

		tokenService.setDataStore(datastore);
		tokenService.setSettings(settings);
		when(context.getBean(IDataStore.BEAN_NAME)).thenReturn(datastore);

		Map<String, String> queryParams = new HashMap<>();

		//create token

		Token token = new Token();
		token.setStreamId("streamId");
		token.setExpireDate(1454354);
		token.setType(Token.PUBLISH_TOKEN);
		
		
		
		//save to datastore
		Token createdToken=	datastore.createToken(token.getStreamId(), token.getExpireDate(), token.getType());
		queryParams.put("token", createdToken.getTokenId());
		
		//check is publish allowed or not
		boolean flag = tokenService.isPublishAllowed(scope, createdToken.getStreamId(), "mode", queryParams);

		assertTrue(flag);

	}

}
