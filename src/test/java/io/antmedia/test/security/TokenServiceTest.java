package io.antmedia.test.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
	public void testRemoveSession() {
		
		//check that listeners exist in the applications' web.xml
		assertTrue(getXMLValue("LiveApp"));
		assertTrue(getXMLValue("WebRTCAppEE"));

		//create token with session id "sessionId", then it saves session to map because it is play token
		testCheckToken();

		//check that session is saved to map
		assertEquals(1, tokenService.getAuthenticatedMap().size());

		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionId");

		HttpSessionEvent event = mock (HttpSessionEvent.class);
		when(event.getSession()).thenReturn(session);

		//close session
		tokenService.sessionDestroyed(event);

		//check that map size is decreased
		assertEquals(0, tokenService.getAuthenticatedMap().size());
		

		
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
	
	public boolean getXMLValue(String appName) {
		Boolean flag = false;
		
		try {

			File xmlFile = new File("/usr/local/antmedia/webapps/"+appName+"/WEB-INF/web.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(xmlFile);
			doc.getDocumentElement().normalize();
			
			NodeList nList = doc.getElementsByTagName("listener");
			
		
			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);			
					
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					if(eElement.getElementsByTagName("listener-class").item(0).getTextContent().equals("io.antmedia.filter.TokenFilter")){

						flag = true;
						break;
					}			
				}
			}			
			assertTrue(flag);
		
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		
		return flag;
		
	}

}
