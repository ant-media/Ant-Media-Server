package io.antmedia.test.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.scope.WebScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.webrtc.Logging;

import io.antmedia.AppSettings;
import io.antmedia.licence.ILicenceService;
import io.antmedia.settings.ServerSettings;



@ContextConfiguration(locations = { "../test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ServerSettingsTest extends AbstractJUnit4SpringContextTests {
	
	@Test
	public void testNativeLogLevel() {

		ServerSettings settings = new ServerSettings();
		
		assertEquals(Logging.Severity.LS_WARNING, settings.getWebRTCLogLevel());
		assertEquals(ServerSettings.LOG_LEVEL_ERROR, settings.getNativeLogLevel());
		
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
		
		String nativeLogLevel = settings.getNativeLogLevel();
		Mockito.verify(settings).setNativeLogLevel(nativeLogLevel);
		
		
		Mockito.doReturn("").when(settings).getHostAddressFromEnvironment();
		settings.setUseGlobalIp(true);
		settings.setApplicationContext(applicationContext);
		//it should still return public host address
		assertEquals(ServerSettings.getGlobalHostAddress(), settings.getHostAddress());
		
		Mockito.doReturn("144.123.45.67").when(settings).getHostAddressFromEnvironment();

		settings.setApplicationContext(applicationContext);
		assertEquals("144.123.45.67", settings.getHostAddress());
		
		Mockito.when(applicationContext.containsBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(true);
		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		Mockito.when(applicationContext.getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString())).thenReturn(licenseService);
		Mockito.when(licenseService.getLicenseType()).thenReturn(ILicenceService.LICENCE_TYPE_MARKETPLACE);
		settings.setApplicationContext(applicationContext);
		
		assertTrue(settings.isBuildForMarket());
		
		
		Mockito.when(licenseService.getLicenseType()).thenReturn(ILicenceService.LICENCE_TYPE_OFFLINE);
		settings.setApplicationContext(applicationContext);
		
		assertTrue(settings.isOfflineLicense());
				
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
	
	/*
	 * This is bug test that confirm wrong that proxy address is not "null". It should be null.
	 * 
	 * It should be like this
	 * @Value( "${"+SETTINGS_PROXY_ADDRESS+":#{null}}" )
	 * 
	 * Not like this 
	 * @Value( "${"+SETTINGS_PROXY_ADDRESS+":null}" )
	 */
	@Test
	public void testDefaultBeanSettings() {
		
		
		ServerSettings serverSettings = (ServerSettings) applicationContext.getBean(ServerSettings.BEAN_NAME);
		
		assertNotNull(serverSettings);
		
		assertNull(serverSettings.getProxyAddress());
		
		assertFalse(serverSettings.isSslEnabled());
		serverSettings.setSslEnabled(true);
		assertTrue(serverSettings.isSslEnabled());
		
		assertFalse(serverSettings.isOfflineLicense());
		serverSettings.setOfflineLicense(true);
		assertTrue(serverSettings.isOfflineLicense());
	}
	
}
