package io.antmedia.test.settings;

import io.antmedia.rest.model.SslConfigurationType;
import io.antmedia.settings.SslSettings;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.webrtc.Logging;

import io.antmedia.settings.ServerSettings;

import javax.validation.constraints.AssertTrue;

import static org.junit.Assert.*;

public class ServerSettingsTest {
	
	@Test
	public void testNativeLogLevel() {
		ServerSettings settings = new ServerSettings();
		
		assertEquals(Logging.Severity.LS_WARNING, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_WARN, settings.getNativeLogLevel());
		
		settings.setNativeLogLevel(ServerSettings.LOG_LEVEL_ALL);
		assertEquals(Logging.Severity.LS_VERBOSE, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_ALL, settings.getNativeLogLevel());
		
		
		settings.setNativeLogLevel(ServerSettings.LOG_LEVEL_TRACE);
		assertEquals(Logging.Severity.LS_VERBOSE, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_TRACE, settings.getNativeLogLevel());
		
		
		settings.setNativeLogLevel(ServerSettings.LOG_LEVEL_DEBUG);
		assertEquals(Logging.Severity.LS_VERBOSE, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_DEBUG, settings.getNativeLogLevel());
		
		settings.setNativeLogLevel(ServerSettings.LOG_LEVEL_INFO);
		assertEquals(Logging.Severity.LS_INFO, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_INFO, settings.getNativeLogLevel());
		
		settings.setNativeLogLevel(ServerSettings.LOG_LEVEL_WARN);
		assertEquals(Logging.Severity.LS_WARNING, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_WARN, settings.getNativeLogLevel());
		
		settings.setNativeLogLevel(ServerSettings.LOG_LEVEL_ERROR);
		assertEquals(Logging.Severity.LS_ERROR, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_ERROR, settings.getNativeLogLevel());
		
		settings.setNativeLogLevel(ServerSettings.LOG_LEVEL_OFF);
		assertEquals(Logging.Severity.LS_NONE, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_OFF, settings.getNativeLogLevel());
		
		settings.setNativeLogLevel(".....");
		assertEquals(Logging.Severity.LS_WARNING, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_WARN, settings.getNativeLogLevel());
		
		settings.setMarketplace("aws");
		assertEquals("aws", settings.getMarketplace());
		
		
		
	}
	
	@Test
	public void testSetAppContext() 
	{
		ServerSettings settings = Mockito.spy(new ServerSettings());
		
		assertNull(settings.getHostAddressFromEnvironment());
		
		Mockito.doReturn(null).when(settings).getHostAddressFromEnvironment();
		
		ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
		settings.setApplicationContext(applicationContext);
		
		assertEquals(ServerSettings.getLocalHostAddress(), settings.getHostAddress());
		
		
		Mockito.doReturn("").when(settings).getHostAddressFromEnvironment();
		settings.setUseGlobalIp(true);
		settings.setApplicationContext(applicationContext);
		//it should still return public host address
		assertEquals(ServerSettings.getGlobalHostAddress(), settings.getHostAddress());
		
		Mockito.doReturn("144.123.45.67").when(settings).getHostAddressFromEnvironment();

		settings.setApplicationContext(applicationContext);
		assertEquals("144.123.45.67", settings.getHostAddress());
		
		
		
	}
	
	@Test
	public void testNodeGroup() {
		ServerSettings settings = new ServerSettings();
		assertEquals(ServerSettings.DEFAULT_NODE_GROUP, settings.getNodeGroup());
		settings.setNodeGroup("group1");
		assertEquals("group1", settings.getNodeGroup());
		
	}
	
	@Test
	public void testDefaultHttpPort() {
		ServerSettings settings = new ServerSettings();
		
		settings.setDefaultHttpPort(5090);
		assertEquals(5090,settings.getDefaultHttpPort());
		
	}

	@Test
	public void testOriginPort() {
		ServerSettings settings = new ServerSettings();
		
		settings.setOriginServerPort(5001);
		assertEquals(5001,settings.getOriginServerPort());
		
	}

	@Test
	public void testSslSettings() {
		ServerSettings settings = new ServerSettings();
		SslSettings currSslSettings = settings.getSslSettings();
		String sslConfigurationType = currSslSettings.getConfigurationType();
		assertNull(sslConfigurationType);
		SslSettings newSslSettings = new SslSettings();
		newSslSettings.setConfigurationType("NO_SSL");
		settings.setSslSettings(newSslSettings);
		assertTrue(settings.getSslSettings().getConfigurationType().equals("NO_SSL"));
	}

}
