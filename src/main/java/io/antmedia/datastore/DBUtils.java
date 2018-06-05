package io.antmedia.datastore;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Vector;

public class DBUtils {
	public static String getHostAddress() {
		String ip = "-.-.-.-";  
		Vector<String> hostAddresses = new Vector<>();
		try {
			for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
				if (!ni.isLoopback() && ni.isUp() && ni.getHardwareAddress() != null) {
					for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
						if (ia.getBroadcast() != null) {  //If limited to IPV4
							hostAddresses.add(ia.getAddress().getHostAddress());
						}
					}
				}
			}
		} catch (SocketException e) { }
		ip = hostAddresses.get(0);
		return ip;
	}
	
	public static String getHostAddress2() {
		String ip = "-.-.-.-";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return ip;
	}
}
