package io.antmedia.ipcamera.onvifdiscovery;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author th
 * @date 2015-06-18
 */
public class OnvifDiscovery {

	public static List<URL> discoverOnvifDevices(boolean useIpv4, ArrayList<String> addressList) {
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

	public static void main(String[] args) throws IOException {
		// List<URL> onvifDevices = discoverOnvifDevices(true);

		ArrayList<String> addressList = new ArrayList<>();

		for (int i = 2; i < 255; i++) {
			addressList.add("192.168.1." + i);
		}

		List<URL> onvifDevices = discoverOnvifDevices(true, addressList);

		if (onvifDevices.size() == 0)
			System.out.println("No Onvif device found");
		for (URL url : onvifDevices) {
			System.out.println("Device discovered: " + url.toString());
		}

		// cxfDiscovery();

	}

	// @SuppressWarnings("unused")
	// private static void cxfDiscovery() throws IOException {
	// //Use WS-Discovery to find references to services that implement the
	// changeName portType
	// String targetAddress = "soap.udp://192.168.1.53:3702";
	// WSDiscoveryClient client = new WSDiscoveryClient();
	// // Setting timeout for WS-Discovery
	// client.setDefaultProbeTimeout(1000);
	// // Use WS-discovery 1.0
	// client.setVersion10();
	// client.setSoapVersion11();
	// System.out.println("Probe:" + client.getAddress());
	// List<EndpointReference> references =
	// client.probe(DiscoveryService.SERVICE);
	//// List<EndpointReference> references = client.probe();
	// System.out.println("The probe has been finished");
	// client.close();
	//
	// System.out.println("Found "+references.size()+" ONVIF devices");
	// //loop through all of them and have them greet me.
	//// DiscoveryService service = new DiscoveryService();
	// for (EndpointReference ref : references) {
	//// HelloType g = service.getPort(ref, HelloType.class);
	// System.out.println(ref);
	// }
	// }
}
