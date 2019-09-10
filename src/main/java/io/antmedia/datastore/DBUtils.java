package io.antmedia.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.rest.RestServiceBase;


public class DBUtils {

	private static final String USE_GLOBAL_IP = "useGlobalIp";
	private static Logger logger = LoggerFactory.getLogger(DBUtils.class);
	public static final DBUtils instance = new DBUtils();
	private String ip;

	private DBUtils() {
		//TODO: Don't access the properties file directly. Get parameters from bean
		PreferenceStore store = new PreferenceStore("red5.properties");
		store.setFullPath("conf/red5.properties");

		boolean global = Boolean.parseBoolean(store.get(USE_GLOBAL_IP));

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
		return RestServiceBase.getHostAddress();
	}

	public static String getGlobalHostAddress(){
		String globalIp = "";
		InputStream in = null;
		try {
			in = new URL("http://checkip.amazonaws.com").openStream();
			globalIp = IOUtils.toString(in, Charset.defaultCharset()).trim();
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		finally {
			IOUtils.closeQuietly(in);
		}

		return globalIp;
	}

	public static String getMongoConnectionUri(String host, String username, String password) {
		String credential = "";
		if(username != null && !username.isEmpty()) {
			credential = username+":"+password+"@";
		}

		String uri = "mongodb://"+credential+host;

		logger.info("uri:{}",uri);

		return uri;
	}
}
