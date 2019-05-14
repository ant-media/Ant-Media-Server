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

import static org.bytedeco.javacpp.avformat.av_register_all;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.brsanthu.googleanalytics.GoogleAnalytics;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AsciiArt;
import io.antmedia.rest.BroadcastRestService;
import io.antmedia.shutdown.AMSShutdownManager;

/**
 * Launches Red5.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Launcher {

	public static final String GA_TRACKING_ID = "UA-93263926-3";
	public static final String RED5_ROOT = "red5.root";
	private static Logger logger;
	private static String instanceId = null;
	private static final String INSTANCE_ID_DEFAULT_PATH = "conf/instanceId";
	private static String instanceIdFilePath = INSTANCE_ID_DEFAULT_PATH; 


	Timer heartbeat = new Timer("heartbeat", true);


	/**
	 * Launch Red5 under it's own classloader
	 * 
	 * @throws Exception
	 *             on error
	 */
	public void launch()  {

		av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		// check for the logback disable flag
		boolean useLogback = Boolean.parseBoolean(System.getProperty("useLogback", "true"));
		if (useLogback && System.getProperty("logback.ContextSelector") == null) {
			// set our selector
			System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
		}
		Red5LoggerFactory.setUseLogback(useLogback);
		// install the slf4j bridge (mostly for JUL logging)
		SLF4JBridgeHandler.install();

		// get the first logger
		final Logger log = Red5LoggerFactory.getLogger(Launcher.class);
		setLog(log);

		// version info banner
		String implementationVersion = AntMediaApplicationAdapter.class.getPackage().getImplementationVersion();
		String type = BroadcastRestService.isEnterprise() ? "Enterprise" : "Community";
		log.info("Ant Media Server {} {}", type, implementationVersion);
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

		startAnalytic(implementationVersion, type);

		startHeartBeats(implementationVersion, type, 300000);

		// refresh must be called before accessing the bean factory
		log.trace("Refreshing root server context");
		root.refresh();
		log.trace("Root server context refreshed");
		log.debug("Launcher exit");

		notifyShutDown(implementationVersion, type);
	}


	public void printLogo() {
		logger.info("\n {}", AsciiArt.LOGO);
	}


	public boolean notifyShutDown(String implementationVersion, String type) {
		boolean result = false;

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				AMSShutdownManager.getInstance().notifyShutdown();
				if(logger != null) {
					logger.info("Shutting down just a sec");
				}
				getGoogleAnalytic(implementationVersion, type).screenView()
				.clientId(getInstanceId())
				.sessionControl("end")
				.send();
			}
		});
		result = true;
		return result;
	}

	public boolean startHeartBeats(String implementationVersion, String type, int periodMS) {
		boolean result = false;

		heartbeat.scheduleAtFixedRate(new TimerTask() {

			@Override
			public void run() {
				if(logger != null) {
					logger.info("-Heartbeat-");
				}
				getGoogleAnalytic(implementationVersion, type).event()
				.eventCategory("server_status")
				.eventAction("heartbeat")
				.eventLabel("")
				.clientId(getInstanceId())
				.send();

			}
		}, periodMS, periodMS);
		result = true;
		return result;
	}

	public boolean startAnalytic(String implementationVersion, String type) {
		boolean  result = false;
		heartbeat.schedule(new TimerTask() {

			@Override
			public void run() {
				getGoogleAnalytic(implementationVersion, type).screenView()
				.sessionControl("start")
				.clientId(getInstanceId())
				.send();
			}
		}, 0);
		result = true;
		return result;
	}

	public GoogleAnalytics getGoogleAnalytic(String implementationVersion, String type) {
		return GoogleAnalytics.builder()
				.withAppVersion(implementationVersion)
				.withAppName(type)
				.withTrackingId(GA_TRACKING_ID).build();

	}

	public static void writeToFile(String absolutePath, String content) {
		try {
			Files.write(new File(absolutePath).toPath(), content.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			logger.error(e.toString());	
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

	public static String getInstanceId() {
		if (instanceId == null) {
			File idFile = new File(instanceIdFilePath);
			if (idFile.exists()) {
				instanceId = getFileContent(idFile.getAbsolutePath());
			}
			else {
				instanceId =  UUID.randomUUID().toString();
				writeToFile(idFile.getAbsolutePath(), instanceId);
			}
		}
		return instanceId;
	}

	public static void setLog(Logger log) {
		Launcher.logger = log;
	}

	/**
	 * Written for tests. Do not use in code
	 * @param instanceIdFilePath
	 */
	public static void setInstanceIdFilePath(String instanceIdFilePath) {
		Launcher.instanceIdFilePath = instanceIdFilePath;
	}

}
