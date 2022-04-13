package io.antmedia.test.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.webrtc.Logging;

import io.antmedia.settings.ServerSettings;

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
}
