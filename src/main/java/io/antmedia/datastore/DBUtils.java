package io.antmedia.datastore;

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DBUtils {
	
	private static Logger logger = LoggerFactory.getLogger(DBUtils.class);
	
	private DBUtils() {
	}
	public static String getHostAddress() {
		String ip;  
		ArrayList<String> hostAddresses = new ArrayList<>();
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
		} catch (SocketException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		ip = hostAddresses.get(0);
		return ip;
	}
	
	public static String getHostAddress2() {
		String ip = "-.-.-.-";
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return ip;
	}
}
