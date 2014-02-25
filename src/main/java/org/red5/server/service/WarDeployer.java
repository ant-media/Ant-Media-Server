/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

package org.red5.server.service;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.management.ManagementFactory;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.jmx.mxbeans.LoaderMXBean;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This service provides the means to auto-deploy a war. 
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class WarDeployer {

	private Logger log = LoggerFactory.getLogger(WarDeployer.class);

	private ISchedulingService scheduler;

	//how often to check for wars
	private int checkInterval = 600000; //ten minutes

	//where we deploy from - where the war files are located
	private String deploymentDirectory;

	private static String jobName;

	//that wars are currently being installed
	private static boolean deploying;

	{
		log.info("War deployer service created");
	}

	public void setCheckInterval(int checkInterval) {
		this.checkInterval = checkInterval;
	}

	public int getCheckInterval() {
		return checkInterval;
	}

	public ISchedulingService getScheduler() {
		return scheduler;
	}

	public void setScheduler(ISchedulingService scheduler) {
		this.scheduler = scheduler;
	}

	public String getDeploymentDirectory() {
		return deploymentDirectory;
	}

	public void setDeploymentDirectory(String deploymentDirectory) {
		this.deploymentDirectory = deploymentDirectory;
	}

	public void init() {
		// create the job and schedule it
		jobName = scheduler.addScheduledJobAfterDelay(checkInterval, new DeployJob(), 60000);
		// check the deploy from directory
		log.debug("Source directory: {}", deploymentDirectory);
		File dir = new File(deploymentDirectory);
		if (!dir.exists()) {
			log.warn("Source directory not found");
		} else {
			if (!dir.isDirectory()) {
				log.warn("Source directory is not a directory");
			}
		}
		dir = null;
	}

	public void shutdown() {
		scheduler.removeScheduledJob(jobName);
	}

	/**
	 * Returns the LoaderMBean.
	 * @return LoadeerMBean
	 */
	@SuppressWarnings("cast")
	public LoaderMXBean getLoader() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		// proxy class
		LoaderMXBean proxy = null;
		ObjectName oName;
		try {
			// TODO support all loaders
			oName = new ObjectName("org.red5.server:type=TomcatLoader");
			if (mbs.isRegistered(oName)) {
				proxy = JMX.newMXBeanProxy(mbs, oName, LoaderMXBean.class, true);
				log.debug("Loader was found");
			} else {
				log.warn("Loader not found");
			}
		} catch (Exception e) {
			log.error("Exception getting loader", e);
		}
		return proxy;		
	}

	/**
	 * Filters directory content
	 */
	protected class DirectoryFilter implements FilenameFilter {
		/**
		 * Check whether file matches filter rules
		 * 
		 * @param dir	Directory
		 * @param name	File name
		 * @return true If file does match filter rules, false otherwise
		 */
		public boolean accept(File dir, String name) {
			File f = new File(dir, name);
			log.trace("Filtering: {} name: {}", dir.getName(), name);
			// filter out all but war files
			boolean result = f.getName().endsWith("war");
			//nullify
			f = null;
			return result;
		}
	}

	private class DeployJob implements IScheduledJob {

		public void execute(ISchedulingService service) {
			log.trace("Executing job");
			if (deploying) {
				return;
			}
			deploying = true;
			log.debug("Starting scheduled deployment of wars");

			//short name
			String application = null;
			//file name
			String applicationWarName = null;

			//get webapp location
			String webappsDir = System.getProperty("red5.webapp.root");
			log.debug("Webapp folder: {}", webappsDir);

			//look for web application archives
			File dir = new File(deploymentDirectory);
			//get a list of wars
			File[] files = dir.listFiles(new DirectoryFilter());
			for (File f : files) {
				//get the war name
				applicationWarName = f.getName();

				int dashIndex = applicationWarName.indexOf('-');
				if (dashIndex != -1) {
					//strip everything except the applications name
					application = applicationWarName.substring(0, dashIndex);
				} else {
					//grab every char up to the last '.'
					application = applicationWarName.substring(0, applicationWarName.lastIndexOf('.'));
				}
				log.debug("Application name: {}", application);

				//setup context
				String contextPath = '/' + application;
				String contextDir = webappsDir + contextPath;

				log.debug("Web context: {} context directory: {}", contextPath, contextDir);

				//verify this is a unique app
				File appDir = new File(webappsDir, application);
				if (appDir.exists()) {
					if (appDir.isDirectory()) {
						log.debug("Application directory exists");
					} else {
						log.warn("Application destination is not a directory");
					}
					log.info("Application {} already installed, please un-install before attempting another install",
							application);
				} else {
					log.debug("Unwaring and starting...");
					//un-archive it to app dir
					FileUtil.unzip(deploymentDirectory + '/' + applicationWarName, contextDir);
					//get the webapp loader
					LoaderMXBean loader = getLoader();
					if (loader != null) {
						//load and start the context
						loader.startWebApplication(application);
						//remove the war file
						File warFile = new File(deploymentDirectory, applicationWarName);
						if (warFile.delete()) {
							log.debug("{} was deleted", warFile.getName());
						} else {
							log.debug("{} was not deleted", warFile.getName());
							warFile.deleteOnExit();
						}
						warFile = null;
					}
				}
				appDir = null;
			}
			dir = null;

			//reset sentinel
			deploying = false;
		}

	}

}
