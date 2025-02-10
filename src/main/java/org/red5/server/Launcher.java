/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Enumeration;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.webrtc.PeerConnectionFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AsciiArt;
import io.antmedia.console.rest.CommonRestService;
import io.antmedia.rest.RestServiceBase;

/**
 * Launches Red5.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Launcher {

	public static final String RED5_ROOT = "red5.root";
	private static Logger logger;
	private static String instanceId = null;
	private static String implementationVersion;
	private static String versionType = null;  //community or enterprise

	/**
	 * Launch Red5 under it's own classloader
	 * 
	 * @throws Exception
	 *             on error
	 */
	public void launch()  {

		// check for the logback disable flag
		boolean useLogback = Boolean.parseBoolean(System.getProperty("useLogback", "true"));
		if (useLogback && System.getProperty("logback.ContextSelector") == null) {
			// set our selector
			System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
		}

		Red5LoggerFactory.setUseLogback(useLogback);

		// get the first logger
		final Logger log = Red5LoggerFactory.getLogger(Launcher.class);
		setLog(log);

		// version info banner
		log.info("Ant Media Server {} {}", getVersionType(), getVersion());
		printLogo();

		if (log.isDebugEnabled()) {
			log.debug("fmsVer: {}", Red5.getFMSVersion());
		}
		// create red5 app context
		@SuppressWarnings("resource")
		FileSystemXmlApplicationContext root = new FileSystemXmlApplicationContext(new String[] { "classpath:/red5.xml" }, false);
		// set the current threads classloader as the loader for the factory/appctx
		root.setClassLoader(Thread.currentThread().getContextClassLoader());
		root.setId(RED5_ROOT);
		root.setBeanName(RED5_ROOT);

		// refresh must be called before accessing the bean factory
		log.trace("Refreshing root server context");
		root.refresh();
		log.trace("Root server context refreshed");
		log.debug("Launcher exit");
		PeerConnectionFactory.initialize(
				PeerConnectionFactory.InitializationOptions.builder()
				.setFieldTrials(null)
				.createInitializationOptions());

	}


	public void printLogo() {
		logger.info("\n {}", AsciiArt.LOGO);
	}


	public static void writeToFile(String absolutePath, String content) {
		try {
			Files.write(new File(absolutePath).toPath(), content.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			if (logger != null) {
				logger.error(e.toString());
			}
		}

	}

	public static String getFileContent(String path) {
		try {
			byte[] data = Files.readAllBytes(new File(path).toPath());
			return new String(data);
		} catch (IOException e) {
			logger.error(e.toString());	
		}
		return null;
	}

	private static byte[] getMacAddress(NetworkInterface networkInterface) {
		byte[] macAddressBytes = null;
		try {
			if (!networkInterface.isVirtual() && !networkInterface.isLoopback()) {
				macAddressBytes = networkInterface.getHardwareAddress();
			}

		} catch (SocketException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return macAddressBytes;
	}

	private static String getHashInstanceId() {
		StringBuilder instanceId = new StringBuilder();
		try {

			Enumeration<NetworkInterface> networks = NetworkInterface.getNetworkInterfaces();
			while (networks.hasMoreElements()) {
				NetworkInterface network = networks.nextElement();
				byte[] mac = getMacAddress(network);
				if (mac != null) {

					for (byte b : mac) {
						instanceId.append(String.format("%02X:", b));
					}
					if (instanceId.length() > 0) {
						instanceId.deleteCharAt(instanceId.length() - 1); // Remove trailing colon
					}
					break;
				}
			}
		} catch (Exception e) {
			logger.error(e.toString());
		}

		if (instanceId.length() == 0) {
			instanceId.append(UUID.randomUUID().toString());
		}
		
		return CommonRestService.getMD5Hash(instanceId.toString());
	}

	public static String getInstanceId() {
		if (instanceId == null) {			
			instanceId = getHashInstanceId();
		}
		return instanceId;
	}

	public static void setLog(Logger log) {
		Launcher.logger = log;
	}

	public static String getVersion() {
		if (implementationVersion == null) {
			implementationVersion = AntMediaApplicationAdapter.class.getPackage().getImplementationVersion();
		}
		return implementationVersion;
	}

	public static String getVersionType() {
		if (versionType == null) {
			versionType = RestServiceBase.isEnterprise() ? "Enterprise" : "Community";
		}
		return versionType;
	}

}
