package io.antmedia.ipcamera.onvifdiscovery;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author th
 * @date 2015-06-18
 */
public class OnvifDiscovery {
	
	private OnvifDiscovery() {
	}

	public static List<URL> discoverOnvifDevices(boolean useIpv4, List<String> addressList) {
		final ArrayList<URL> onvifPointers = new ArrayList<>();
		final Collection<URL> urls = DeviceDiscovery.discoverWsDevicesAsUrls("^http$", ".*onvif.*", useIpv4,
				addressList);
		for (URL url : urls) {
			try {
				onvifPointers.add(url);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return onvifPointers;
	}
}
