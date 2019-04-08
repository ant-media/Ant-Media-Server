package io.antmedia.datastore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.settings.LogSettings;


public class DBUtils {

	private static Logger logger = LoggerFactory.getLogger(DBUtils.class);
	public static final DBUtils instance = new DBUtils();
	private String ip;

	private DBUtils() {
		PreferenceStore store = new PreferenceStore("red5.properties");
		store.setFullPath("conf/red5.properties");

		boolean global = Boolean.parseBoolean(store.get("useGlobalIp"));

		if(global) {
			ip = getGlobalHostAddress();
		}
		else {
			ip = getLocalHostAddress(); 
		}


	}
	
	public static String getHostAddress() {
		return instance.ip;
	}
	
	public static String getLocalHostAddress() {

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
		return hostAddresses.get(0);
	}

	public static String getGlobalHostAddress() {
		String ip = "-.-.-.-";
		URL whatismyip;
		try {
			whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			ip = in.readLine(); //you get the IP as a String
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ip;
	}

	public static String getUri(String host, String username, String password) {
		String credential = "";
		if(username != null && !username.isEmpty()) {
			credential = username+":"+password+"@";
		}

		String uri = "mongodb://"+credential+host;

		logger.info("uri:{}",uri);

		return uri;
	}
}
