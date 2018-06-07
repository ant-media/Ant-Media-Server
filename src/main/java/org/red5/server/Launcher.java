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
import io.antmedia.rest.BroadcastRestService;

/**
 * Launches Red5.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Launcher {


	private String instanceId;
	
	

	/**
	 * Launch Red5 under it's own classloader
	 * 
	 * @throws Exception
	 *             on error
	 */
	public void launch() throws Exception {



		av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_ERROR);
		System.out.printf("Root: %s%nDeploy type: %s%n", System.getProperty("red5.root"), System.getProperty("red5.deployment.type"));
		// check for the logback disable flag
		boolean useLogback = Boolean.valueOf(System.getProperty("useLogback", "true"));
		if (useLogback) {
			// check for context selector in system properties
			if (System.getProperty("logback.ContextSelector") == null) {
				// set our selector
				System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
			}
		}
		Red5LoggerFactory.setUseLogback(useLogback);
		// install the slf4j bridge (mostly for JUL logging)
		SLF4JBridgeHandler.install();
		// log stdout and stderr to slf4j
		//SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();
		// get the first logger
		Logger log = Red5LoggerFactory.getLogger(Launcher.class);
		// version info banner
		String implementationVersion = AntMediaApplicationAdapter.class.getPackage().getImplementationVersion();
		String type = BroadcastRestService.isEnterprise() ? "Enterprise" : "Community";
		log.info("Ant Media Server {} {}", type, implementationVersion);
		if (log.isDebugEnabled()) {
			log.debug("fmsVer: {}", Red5.getFMSVersion());
		}
		// create red5 app context
		@SuppressWarnings("resource")
		FileSystemXmlApplicationContext root = new FileSystemXmlApplicationContext(new String[] { "classpath:/red5.xml" }, false);
		// set the current threads classloader as the loader for the factory/appctx
		root.setClassLoader(Thread.currentThread().getContextClassLoader());
		root.setId("red5.root");
		root.setBeanName("red5.root");
		String path = System.getProperty("red5.root");
		File idFile = new File(path + "/conf/instanceId");
		instanceId = null;
		if (idFile.exists()) {
			instanceId = getFileContent(idFile.getAbsolutePath());
		}
		else {
			instanceId =  UUID.randomUUID().toString();
			writeToFile(idFile.getAbsolutePath(), instanceId);
		}


		Timer heartbeat = new Timer("heartbeat", true);
		heartbeat.schedule(new TimerTask() {
			
			@Override
			public void run() {
				getGoogleAnalytic(implementationVersion, type).screenView()
			    		.sessionControl("start")
			    		.clientId(instanceId)
			    		.send();
			}
		}, 0);
		
		heartbeat.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				
				System.out.println("-Heartbeat-");
				getGoogleAnalytic(implementationVersion, type).event()
					.eventCategory("server_status")
					.eventAction("heartbeat")
					.eventLabel("")
					.clientId(instanceId)
					.send();
				
			}
		}, 300000, 300000);
		

		
		
		// refresh must be called before accessing the bean factory
		log.trace("Refreshing root server context");
		root.refresh();
		log.trace("Root server context refreshed");
		log.debug("Launcher exit");

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				System.out.println("Shutting down just a sec");
				getGoogleAnalytic(implementationVersion, type).screenView()
					.clientId(instanceId)
					.sessionControl("end")
					.send();

			}
		});
	}

	private GoogleAnalytics getGoogleAnalytic(String implementationVersion, String type) {
		return GoogleAnalytics.builder()
		.withAppVersion(implementationVersion)
		.withAppName(type)
		.withTrackingId("UA-93263926-3").build();
		
	}

	public void writeToFile(String absolutePath, String content) {
		try {
			Files.write(new File(absolutePath).toPath(), content.getBytes(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public String getFileContent(String path) {
		try {
			byte[] data = Files.readAllBytes(new File(path).toPath());
			return new String(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
