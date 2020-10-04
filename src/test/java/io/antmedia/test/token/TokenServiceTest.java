package io.antmedia.test.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.filter.TokenSessionFilter;
import io.antmedia.security.ITokenService;
import io.antmedia.security.MockTokenService;

public class TokenServiceTest {
	protected static Logger logger = LoggerFactory.getLogger(TokenServiceTest.class);

	DataStore datastore;
	ApplicationContext applicationContext;

	private MockTokenService tokenService;
	private TokenFilterManager tokenFilter;
	private TokenSessionFilter tokenSessionFilter;
	@Before
	public void before() {
		tokenService = new MockTokenService();
		tokenFilter = new TokenFilterManager();
		tokenSessionFilter = new TokenSessionFilter();
	}

	@After
	public void after() {
		tokenService = null;
		tokenFilter = null;
		tokenSessionFilter = null;
	}


	@Test
	public void testCheckToken() {

		datastore = new InMemoryDataStore("testDb");

		//create token
		
		Token token = new Token();
		token.setStreamId("streamId");
		token.setTokenId("tokenId");
		token.setType(Token.PLAY_TOKEN);

		//check token
		boolean flag = tokenService.checkToken(token.getTokenId(), token.getStreamId(), "sessionId", token.getType());

		// it should be true because mock service always replies as true
		assertTrue(flag);

	}
	
	@Test
	public void testCheckTimeBasedSubscriber() {

		datastore = new InMemoryDataStore("testDb");

		//check subscriber
		boolean flag = tokenService.checkTimeBasedSubscriber("subscriber2", "stream1", "sdafsd",
				"fssdg", false);

		// it should be true because mock service always replies as true
		assertTrue(flag);
	}
	
	
	@Test
	public void testGetTokenService() {
		ITokenService iTokenService = tokenSessionFilter.getTokenService();
		assertNull(iTokenService);
		
		ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
		when(context.isRunning()).thenReturn(true);
		
		when(context.getBean(Mockito.anyString())).thenReturn(mock(ITokenService.class));
		
		tokenSessionFilter.setContext(context);
		
		iTokenService = tokenSessionFilter.getTokenService();
		assertNotNull(iTokenService);
		
		
	}

	@Test
	public void testRemoveSession() {

		//check that listeners exist in the applications' web.xml
		tokenFilter.setTokenService(tokenService);	

		//create token
		Token token = new Token();
		token.setStreamId("streamId");
		token.setTokenId("tokenId");
		token.setType(Token.PLAY_TOKEN);
		
		tokenService.getAuthenticatedMap().put("sessionId", "streamId");

		//check that map size
		assertEquals(1, tokenService.getAuthenticatedMap().size());

		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionId");

		HttpSessionEvent event = mock (HttpSessionEvent.class);
		when(event.getSession()).thenReturn(session);

		tokenSessionFilter.setTokenService(tokenService);

		//close session
		tokenSessionFilter.sessionDestroyed(event);

		//check map size
		assertEquals(0, tokenService.getAuthenticatedMap().size());


	}

	@Test
	public void testcreateToken() {

		//create token
		Token token = tokenService.createToken("streamId", 654345, Token.PUBLISH_TOKEN, "testRoomName");

		//check that token is null, because mock service can not create token
		assertNull(token);

	}

	@Test
	public void testIsPublishAllowed() {

		IScope scope = mock(IScope.class);

		Map<String, String> queryParams = new HashMap<>();

		//check is publish allowed or not
		boolean flag = tokenService.isPublishAllowed(scope, "streamId", "mode", queryParams);
		
		//mock service should turn true even is token is not created and saved
		assertTrue(flag);

	}

}
