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
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.UUID;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.red5.compatibility.flex.messaging.messages.AcknowledgeMessage;
import org.red5.compatibility.flex.messaging.messages.AsyncMessage;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.ServiceUtils;
import org.red5.server.jmx.mxbeans.LoaderMXBean;
import org.red5.server.util.FileUtil;
import org.red5.server.util.HttpConnectionUtil;
import org.slf4j.Logger;

/**
 * This service provides the means to list, download, install, and un-install 
 * applications from a given url. 
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Dominick Accattato (daccattato@gmail.com)
 */
public final class Installer {

	private static Logger log = Red5LoggerFactory.getLogger(Installer.class);

	private String applicationRepositoryUrl;

	{
		log.info("Installer service created");
	}

	public String getApplicationRepositoryUrl() {
		return applicationRepositoryUrl;
	}

	public void setApplicationRepositoryUrl(String applicationRepositoryUrl) {
		this.applicationRepositoryUrl = applicationRepositoryUrl;
	}

	/**
	 * Returns the LoaderMBean.
	 * @return LoaderMBean
	 */
	@SuppressWarnings("cast")
	public LoaderMXBean getLoader() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		// proxy class
		LoaderMXBean proxy = null;
		ObjectName oName;
		try {
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
	 * Returns a Map containing all of the application wars in the snapshot repository.
	 * 
	 * @return async message
	 */
	public AsyncMessage getApplicationList() {
		AcknowledgeMessage result = new AcknowledgeMessage();
		// create a singular HttpClient object
		DefaultHttpClient client = HttpConnectionUtil.getClient();
		//setup GET
		HttpGet method = null;
		try {
			//get registry file
			method = new HttpGet(applicationRepositoryUrl + "registry.xml");
			// execute the method
			HttpResponse response = client.execute(method);
			int code = response.getStatusLine().getStatusCode();
			log.debug("HTTP response code: {}", code);
			if (code == 200) {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					String responseText = EntityUtils.toString(entity);
					log.debug("Response: {}", responseText);
					//prepare response for flex			
					result.body = responseText;
					IConnection conn = Red5.getConnectionLocal();
					result.clientId = conn.getClient().getId();
					result.messageId = UUID.randomUUID().toString();
					result.timestamp = System.currentTimeMillis();
					//send the servers java version so the correct apps are installed
					String javaVersion = System.getProperty("java.version").substring(0, 3);
					log.info("JRE version detected: {}", javaVersion);
					// allow any jre version greater than 1.5 to equal 1.6 for client compatibility
					// fix for issue #189
					if (Double.valueOf(javaVersion) > 1.5d) {
						javaVersion = "1.6";
					}
					if (!ServiceUtils.invokeOnConnection(conn, "onJavaVersion", new Object[] { javaVersion })) {
						log.warn("Client call to onJavaVersion failed");
					}
				}
			} else {
				log.warn("Service returned an error");
				if (log.isDebugEnabled()) {
					HttpConnectionUtil.handleError(response);
				}
			}
		} catch (HttpHostConnectException he) {
			log.error("Http error connecting to {}", applicationRepositoryUrl, he);
			method.abort();
		} catch (IOException ioe) {
			log.error("Unable to connect to {}", applicationRepositoryUrl, ioe);
			method.abort();
		}
		return result;
	}

	/**
	 * Installs a given application.
	 * 
	 * @param applicationWarName app war name
	 * @return true if installed; false otherwise
	 */
	public boolean install(String applicationWarName) {
		IConnection conn = Red5.getConnectionLocal();
		boolean result = false;
		//strip everything except the applications name
		String application = applicationWarName.substring(0, applicationWarName.indexOf('-'));
		log.debug("Application name: {}", application);
		//get webapp location
		String webappsDir = System.getProperty("red5.webapp.root");
		log.debug("Webapp folder: {}", webappsDir);
		//setup context
		String contextPath = '/' + application;
		String contextDir = webappsDir + contextPath;
		//verify this is a unique app
		File appDir = new File(webappsDir, application);
		if (appDir.exists()) {
			if (appDir.isDirectory()) {
				log.debug("Application directory exists");
			} else {
				log.warn("Application destination is not a directory");
			}
			ServiceUtils.invokeOnConnection(conn, "onAlert",
					new Object[] { String.format("Application %s already installed, please un-install before attempting another install", application) });
		} else {
			//use the system temp directory for moving files around
			String srcDir = System.getProperty("java.io.tmpdir");
			log.debug("Source directory: {}", srcDir);
			//look for archive containing application (war, zip, etc..)
			File dir = new File(srcDir);
			if (!dir.exists()) {
				log.warn("Source directory not found");
				//use another directory
				dir = new File(System.getProperty("red5.root"), "/webapps/installer/WEB-INF/cache");
				if (!dir.exists()) {
					if (dir.mkdirs()) {
						log.info("Installer cache directory created");
					}
				}
			} else {
				if (!dir.isDirectory()) {
					log.warn("Source directory is not a directory");
				}
			}
			//get a list of temp files
			File[] files = dir.listFiles();
			for (File f : files) {
				String fileName = f.getName();
				if (fileName.equals(applicationWarName)) {
					log.debug("File found matching application name");
					result = true;
					break;
				}
			}
			dir = null;
			//if the file was not found then download it
			if (!result) {
				// create a singular HttpClient object
				DefaultHttpClient client = HttpConnectionUtil.getClient();
				// set transfer encoding
				client.getParams().setBooleanParameter(CoreProtocolPNames.STRICT_TRANSFER_ENCODING, Boolean.TRUE);
				//setup GET
				HttpGet method = null;
				FileOutputStream fos = null;
				try {
					//try the war version first
					method = new HttpGet(applicationRepositoryUrl + applicationWarName);
					//we dont want any transformation - RFC2616
					method.addHeader("Accept-Encoding", "identity");
					// execute the method
					HttpResponse response = client.execute(method);
					int code = response.getStatusLine().getStatusCode();
					log.debug("HTTP response code: {}", code);
					if (code == 200) {
						HttpEntity entity = response.getEntity();
						if (entity != null) {
							//create output file
							fos = new FileOutputStream(srcDir + '/' + applicationWarName);
							log.debug("Writing response to {}/{}", srcDir, applicationWarName);
							// have to receive the response as a byte array.  This has the advantage of writing to the file system
							// faster and it also works on macs ;)
							byte[] buf = EntityUtils.toByteArray(entity);
							fos.write(buf);
							fos.flush();
							// we should be good to go
							result = true;
						}
					}
				} catch (HttpHostConnectException he) {
					log.error("Http error connecting to {}", applicationRepositoryUrl, he);
					method.abort();
				} catch (IOException ioe) {
					log.error("Unable to connect to {}", applicationRepositoryUrl, ioe);
					method.abort();
				} finally {
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
						}
					}
				}
			}
			//if we've found or downloaded the war
			if (result) {
				//get the webapp loader
				LoaderMXBean loader = getLoader();
				if (loader != null) {
					//un-archive it to app dir
					FileUtil.unzip(srcDir + '/' + applicationWarName, contextDir);
					//load and start the context
					loader.startWebApplication(application);
				} else {
					//just copy the war to the webapps dir
					try {
						FileUtil.moveFile(srcDir + '/' + applicationWarName, webappsDir + '/' + application + ".war");
						ServiceUtils.invokeOnConnection(conn, "onAlert",
								new Object[] { String.format("Application %s will not be available until container is restarted", application) });
					} catch (IOException e) {
					}
				}
			}
			ServiceUtils.invokeOnConnection(conn, "onAlert", new Object[] { String.format("Application %s was %s", application, (result ? "installed" : "not installed")) });
		}
		appDir = null;
		return result;
	}

	/**
	 * Un-installs a given application.
	 * 
	 * @param applicationName name to uninstall
	 * @return true if uninstalled; else false
	 */
	public boolean uninstall(String applicationName) {
		ServiceUtils.invokeOnConnection(Red5.getConnectionLocal(), "onAlert", new Object[] { "Uninstall function not available" });

		return false;
	}

}
