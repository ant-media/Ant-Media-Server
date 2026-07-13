package io.antmedia.test.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.red5.server.scope.WebScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.webrtc.Logging;

import io.antmedia.AppSettings;
import io.antmedia.licence.ILicenceService;
import io.antmedia.settings.ServerSettings;


@ContextConfiguration(locations = { "../test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
public class ServerSettingsTest {

	@Autowired
	private ApplicationContext applicationContext;

	private static final String TEST_NONE_LOOPBACK_ADDRESS = "198.51.100.10";
	private static final String TEST_PRIVATE_ADDRESS = "10.0.0.10";
	
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
		
		Mockito.when(applicationContext.containsBean(ILicenceService.BEAN_NAME)).thenReturn(true);
		ILicenceService licenseService = Mockito.mock(ILicenceService.class);
		Mockito.when(applicationContext.getBean(ILicenceService.BEAN_NAME)).thenReturn(licenseService);
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
		
		assertFalse(ServerSettings.isRtmpsEnabled());
		
		serverSettings.setRtmpsEnabled(true);
		
		assertTrue(ServerSettings.isRtmpsEnabled());
	}
	
	@Test
	public void testGetLocalHostAddressReturnsNoneLoopbackAddress() throws Exception {
		Field localHostAddressField = ServerSettings.class.getDeclaredField("localHostAddress");
		localHostAddressField.setAccessible(true);
		Object originalLocalHostAddress = localHostAddressField.get(null);
		localHostAddressField.set(null, null);

		InetAddress noneLoopbackAddress = InetAddress.getByName(TEST_NONE_LOOPBACK_ADDRESS);

		try (MockedStatic<ServerSettings> serverSettings = Mockito.mockStatic(ServerSettings.class, Mockito.CALLS_REAL_METHODS)) {
			serverSettings.when(ServerSettings::getPrivateAddress).thenReturn(null);
			serverSettings.when(ServerSettings::getNoneLoopbackHostAddress).thenReturn(noneLoopbackAddress);
			serverSettings.clearInvocations();

			assertEquals(TEST_NONE_LOOPBACK_ADDRESS, ServerSettings.getLocalHostAddress());

			serverSettings.verify(ServerSettings::getPrivateAddress);
			serverSettings.verify(ServerSettings::getNoneLoopbackHostAddress);
		}
		finally {
			localHostAddressField.set(null, originalLocalHostAddress);
		}
	}

	@Test
	public void testGetLocalHostAddressReturnsPrivateAddress() throws Exception {
		Field localHostAddressField = ServerSettings.class.getDeclaredField("localHostAddress");
		localHostAddressField.setAccessible(true);
		Object originalLocalHostAddress = localHostAddressField.get(null);
		localHostAddressField.set(null, null);

		InetAddress privateAddress = InetAddress.getByName(TEST_PRIVATE_ADDRESS);

		try (MockedStatic<ServerSettings> serverSettings = Mockito.mockStatic(ServerSettings.class, Mockito.CALLS_REAL_METHODS)) {
			serverSettings.when(ServerSettings::getPrivateAddress).thenReturn(privateAddress);
			serverSettings.clearInvocations();

			assertEquals(TEST_PRIVATE_ADDRESS, ServerSettings.getLocalHostAddress());

			serverSettings.verify(ServerSettings::getPrivateAddress);
			serverSettings.verify(ServerSettings::getNoneLoopbackHostAddress, Mockito.never());
		}
		finally {
			localHostAddressField.set(null, originalLocalHostAddress);
		}
	}
	
	@Test
	public void testGetPrivateAddressReturnsSiteLocalIPv4Address() throws Exception {
		InetAddress loopbackAddress = InetAddress.getByName("127.0.0.1");
		InetAddress publicAddress = InetAddress.getByName(TEST_NONE_LOOPBACK_ADDRESS);
		InetAddress ipv6Address = InetAddress.getByName("fd00::10");
		InetAddress privateAddress = InetAddress.getByName(TEST_PRIVATE_ADDRESS);

		NetworkInterface downInterface = mockNetworkInterface(false, false, privateAddress);
		NetworkInterface loopbackInterface = mockNetworkInterface(true, true, privateAddress);
		NetworkInterface publicInterface = mockNetworkInterface(true, false, loopbackAddress, ipv6Address, publicAddress);
		NetworkInterface privateInterface = mockNetworkInterface(true, false, privateAddress);

		try (MockedStatic<ServerSettings> serverSettings = Mockito.mockStatic(ServerSettings.class, Mockito.CALLS_REAL_METHODS)) {
			serverSettings.when(ServerSettings::getNetworkInterfaces).thenReturn(Collections.enumeration(Arrays.asList(
					downInterface,
					loopbackInterface,
					publicInterface,
					privateInterface)));

			assertEquals(privateAddress, ServerSettings.getPrivateAddress());
		}
	}

	@Test
	public void testGetPrivateAddressReturnsNullIfPrivateIPv4AddressDoesNotExist() throws Exception {
		InetAddress loopbackAddress = InetAddress.getByName("127.0.0.1");
		InetAddress publicAddress = InetAddress.getByName(TEST_NONE_LOOPBACK_ADDRESS);
		InetAddress ipv6Address = InetAddress.getByName("fd00::10");

		NetworkInterface publicInterface = mockNetworkInterface(true, false, loopbackAddress, ipv6Address, publicAddress);

		try (MockedStatic<ServerSettings> serverSettings = Mockito.mockStatic(ServerSettings.class, Mockito.CALLS_REAL_METHODS)) {
			serverSettings.when(ServerSettings::getNetworkInterfaces).thenReturn(Collections.enumeration(Arrays.asList(publicInterface)));

			assertNull(ServerSettings.getPrivateAddress());
		}
	}

	private NetworkInterface mockNetworkInterface(boolean up, boolean loopback, InetAddress... addresses) throws Exception {
		NetworkInterface networkInterface = Mockito.mock(NetworkInterface.class);
		Mockito.when(networkInterface.isUp()).thenReturn(up);
		Mockito.when(networkInterface.isLoopback()).thenReturn(loopback);
		Mockito.when(networkInterface.getInetAddresses()).thenReturn(Collections.enumeration(Arrays.asList(addresses)));
		return networkInterface;
	}
	
	
}
