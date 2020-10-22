package io.antmedia.ipcamera.onvifdiscovery;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceDiscovery {
	
	protected static Logger logger = LoggerFactory.getLogger(DeviceDiscovery.class);

	public static final String WS_DISCOVERY_SOAP_VERSION = "SOAP 1.2 Protocol";
	public static final String WS_DISCOVERY_CONTENT_TYPE = "application/soap+xml";
	public static final  int WS_DISCOVERY_TIMEOUT = 4000;
	public static final int WS_DISCOVERY_PORT = 3702;

	
	private DeviceDiscovery() {
		
	}
	/**
	 * Discover WS device on the local network and returns Urls
	 * 
	 * @return list of unique device urls
	 */
	public static Collection<URL> discoverWsDevicesAsUrls(boolean useIpv4) {
		return discoverWsDevicesAsUrls("", "", useIpv4);
	}

	public static Collection<URL> discoverWsDevicesAsUrls(String regexpProtocol, String regexpPath, boolean useIpv4,
			List<String> targetAddresses) {
		String probeMsgTemplate;
		try {
			File probeMsgFile = new File(
					Thread.currentThread().getContextClassLoader().getResource("probe-template.xml").toURI());
			probeMsgTemplate = FileUtils.readFileToString(probeMsgFile,"UTF-8");
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		final Collection<URL> urls = new TreeSet<>(new URLComparator());
		for (String key : discoverWsDevices(probeMsgTemplate, useIpv4, targetAddresses)) {
			try {
				final URL url = new URL(key);
				boolean ok = true;
				if (!regexpProtocol.isEmpty() && !url.getProtocol().matches(regexpProtocol))
					ok = false;
				if (!regexpPath.isEmpty() && !url.getPath().matches(regexpPath))
					ok = false;
				if (ok)
					urls.add(url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		return urls;
	}

	/**
	 * Discover WS device on the local network with specified filter
	 * 
	 * @param regexpProtocol
	 *            url protocol matching regexp like "^http$", might be empty ""
	 * @param regexpPath
	 *            url path matching regexp like "onvif", might be empty ""
	 * @return list of unique device urls filtered
	 */
	public static Collection<URL> discoverWsDevicesAsUrls(String regexpProtocol, String regexpPath, boolean useIpv4) {
		return discoverWsDevicesAsUrls(regexpProtocol, regexpPath, useIpv4);
	}

	/**
	 * Discover WS device on the local network
	 * 
	 * @param useIpv4
	 * @param targetAddresses
	 * 
	 * @return list of unique devices access strings which might be URLs in most
	 *         cases
	 */
	public static Collection<String> discoverWsDevices(String probeMsgTemplate, boolean useIpv4,
			List<String> targetAddresses) 
	{
		final Collection<String> addresses = new ConcurrentSkipListSet<>();
		final CountDownLatch serverStarted = new CountDownLatch(1);
		final CountDownLatch serverFinished = new CountDownLatch(1);
		final Collection<InetAddress> addressList = new ArrayList<>();
		if (targetAddresses != null && !targetAddresses.isEmpty()) {
			for (String addressStr : targetAddresses) {
				try {
					InetAddress addr = InetAddress.getByName(addressStr);
					addressList.add(addr);
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		} else {
			try {
				final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				if (interfaces != null) {
					while (interfaces.hasMoreElements()) {
						NetworkInterface anInterface = interfaces.nextElement();
						// if (!anInterface.isLoopback()) {
						final List<InterfaceAddress> interfaceAddresses = anInterface.getInterfaceAddresses();
						for (InterfaceAddress address : interfaceAddresses) {
							addressList.add(address.getAddress());
						}
						// }
					}
				}
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}

		ExecutorService executorService = Executors.newCachedThreadPool();
		int port = 50454;
		for (final InetAddress address : addressList) 
		{
			if (useIpv4 && address instanceof Inet6Address)
				continue;
			if (!useIpv4 && address instanceof Inet4Address)
				continue;

			int result = 0;
			do {
				port += 1;
				result = tryAddress(probeMsgTemplate, addresses, serverStarted, serverFinished, executorService, port,
					address);
			}
			while(result == -1);
			
		}
		try {
			executorService.shutdown();
			executorService.awaitTermination(WS_DISCOVERY_TIMEOUT + 2000, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ignored) {
			

			    Thread.currentThread().interrupt();
			
		}
		return addresses;
	}

	public static int tryAddress(String probeMsgTemplate, final Collection<String> addresses,
			final CountDownLatch serverStarted, final CountDownLatch serverFinished, ExecutorService executorService,
			int port, final InetAddress address) {
		try {
			DatagramSocket socket = new DatagramSocket(port);
			
			Thread probeReceiver = new ProbeReceiverThread(addresses, serverStarted, socket, serverFinished);
			Runnable probeSender = new ProbeSenderThread(address, socket, probeMsgTemplate, serverStarted,
					serverFinished, probeReceiver);
			executorService.submit(probeSender);
		}
		catch (BindException e) {
			logger.error("Port:{} is in use. Trying new one", port);
			return -1;
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
		return port;
	}
	


}