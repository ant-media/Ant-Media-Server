/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

package org.red5.server.tomcat;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.management.ManagementFactory;
import java.net.BindException;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.catalina.Cluster;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.apache.catalina.Realm;
import org.apache.catalina.Valve;
import org.apache.catalina.authenticator.jaspic.AuthConfigFactoryImpl;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.realm.NullRealm;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.ContextLoader;
import org.red5.server.LoaderBase;
import org.red5.server.api.IApplicationContext;
import org.red5.server.jmx.mxbeans.ContextLoaderMXBean;
import org.red5.server.jmx.mxbeans.LoaderMXBean;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Red5 loader for Tomcat.
 * 
 * http://tomcat.apache.org/tomcat-8.5-doc/api/index.html
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ManagedResource(objectName = "org.red5.server:type=TomcatLoader", description = "TomcatLoader")
public class TomcatLoader extends LoaderBase implements InitializingBean, DisposableBean, LoaderMXBean {

	static {
		// set jaspic AuthConfigFactory to prevent NPEs like this:
		// java.lang.NullPointerException
		//     at org.apache.catalina.authenticator.AuthenticatorBase.getJaspicProvider(AuthenticatorBase.java:1140)
		//     at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:431)
		//     at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:140)
		Security.setProperty(AuthConfigFactory.DEFAULT_FACTORY_SECURITY_PROPERTY, AuthConfigFactoryImpl.class.getName());
	}

	/**
	 * Filters directory content
	 */
	protected final static class DirectoryFilter implements FilenameFilter {
		/**
		 * Check whether file matches filter rules
		 * 
		 * @param dir
		 *            Directory
		 * @param name
		 *            File name
		 * @return true If file does match filter rules, false otherwise
		 */
		public boolean accept(File dir, String name) {
			File f = new File(dir, name);
			if (log.isTraceEnabled()) {
				log.trace("Filtering: {} name: {} dir: {}", dir.getName(), name, f.getAbsolutePath());
			}
			// filter out all non-directories that are hidden and/or not readable
			boolean result = f.isDirectory() && f.canRead() && !f.isHidden();
			// nullify
			f = null;
			return result;
		}
	}

	// Initialize Logging
	private static Logger log = LoggerFactory.getLogger(TomcatLoader.class);

	public static final String defaultSpringConfigLocation = "/WEB-INF/red5-*.xml";

	public static final String defaultParentContextKey = "default.context";

	/**
	 * Common name for the Service and Engine components.
	 */
	public String serviceEngineName = "red5Engine";

	/**
	 * Base container host.
	 */
	protected Host host;

	/**
	 * Embedded Tomcat service (like Catalina).
	 */
	protected static EmbeddedTomcat embedded;

	/**
	 * Tomcat engine.
	 */
	protected static Engine engine;

	/**
	 * Tomcat realm.
	 */
	protected Realm realm;

	/**
	 * Hosts
	 */
	protected List<Host> hosts;

	/**
	 * Connectors
	 */
	protected List<TomcatConnector> connectors;


	/**
	 * Cluster
	 */
	@Autowired(required=false)
	private Cluster cluster;

	/**
	 * Valves
	 */
	protected List<Valve> valves = new ArrayList<>();

	@Override
	public void afterPropertiesSet() throws Exception {
		start();
	}

	/**
	 * Add context for path and docbase to current host.
	 * 
	 * @param contextPath
	 *            Path
	 * @param docBase
	 *            Document base
	 * @return Catalina context (that is, web application)
	 * @throws ServletException
	 */
	public Context addContext(String path, String docBase) throws ServletException {
		return addContext(path, docBase, host);
	}

	/**
	 * Add context for path and docbase to a host.
	 * 
	 * @param contextPath
	 *            Path
	 * @param docBase
	 *            Document base
	 * @param host
	 *            Host to add context to
	 * @return Catalina context (that is, web application)
	 * @throws ServletException
	 */
	public Context addContext(String contextPath, String docBase, Host host) throws ServletException {
		log.debug("Add context - path: {} docbase: {}", contextPath, docBase);
		// instance a context
		org.apache.catalina.Context ctx = embedded.addWebapp(host, contextPath, docBase);
		if (ctx != null) {
			// grab the current classloader
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			ctx.setParentClassLoader(classLoader);
			// get the associated loader for the context
			Object ldr = ctx.getLoader();
			log.trace("Context loader (null if the context has not been started): {}", ldr);
			if (ldr == null) {
				WebappLoader wldr = new WebappLoader();
				//wldr.setLoaderInstance(classLoader);
				// add the Loader to the context
				ctx.setLoader(wldr);
			}
			log.trace("Context loader (check): {} Context classloader: {}", ctx.getLoader(), ctx.getLoader().getClassLoader());
			LoaderBase.setRed5ApplicationContext(getHostId() + contextPath, new TomcatApplicationContext(ctx));
		} else {
			log.trace("Context is null");
		}
		return ctx;
	}

	/**
	 * Remove context from the current host.
	 * 
	 * @param path
	 *            Path
	 */
	@Override
	public void removeContext(String path) {
	
		Container[] children = host.findChildren();
		for (Container c : children) {
			if (c instanceof StandardContext && c.getName().equals(path)) {
				try {
					log.info("Stopping standard context for {}", path);
					((StandardContext) c).stop();
					host.removeChild(c);
					break;
				} catch (Exception e) {
					log.error("Could not remove context: {}", c.getName(), e);
				}
			}
		}
		
		IApplicationContext ctx = LoaderBase.removeRed5ApplicationContext(path);
		if (ctx != null) {
			ctx.stop();
		} else {
			//try with host Id
			ctx = LoaderBase.removeRed5ApplicationContext(getHostId() + path);
			if (ctx != null) {
				ctx.stop();
			}
			else {
				log.warn("Context could not be stopped, it was null for path: {}", path);
			}
			
		}
	}

	/**
	 * Initialization.
	 */
	public void start() throws ServletException {
		log.info("Loading Tomcat");
		//get a reference to the current threads classloader
		final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		// root location for servlet container
		String serverRoot = System.getProperty("red5.root");
		log.info("Server root: {}", serverRoot);
		String confRoot = System.getProperty("red5.config_root");
		log.info("Config root: {}", confRoot);
		// check naming flag
		Boolean useNaming = Boolean.valueOf(System.getProperty("catalina.useNaming"));
		// create one embedded (server) and use it everywhere
		if (embedded == null) {
			embedded = new EmbeddedTomcat();
		}
		File serverRootF = new File(serverRoot);
		embedded.getServer().setCatalinaBase(serverRootF);
		embedded.getServer().setCatalinaHome(serverRootF);
		embedded.setHost(host);
		// provide default configuration for a context. This is the programmatic equivalent of the default web.xml
		// default-web.xml
		//embedded.initWebappDefaults(confRoot);
		// controls if the loggers will be silenced or not
		embedded.setSilent(false);
		// get the engine
		engine = embedded.getEngine();
		// give the engine a name
		engine.setName(serviceEngineName);
		// set the default host for our engine
		engine.setDefaultHost(host.getName());

		if (cluster != null) {
			engine.setCluster(cluster);
		}
		// set the webapp folder if not already specified
		if (webappFolder == null) {
			// Use default webapps directory
			webappFolder = FileUtil.formatPath(System.getProperty("red5.root"), "/webapps");
		}
		System.setProperty("red5.webapp.root", webappFolder);
		log.info("Application root: {}", webappFolder);
		// Root applications directory
		File appDirBase = new File(webappFolder);
		// Subdirs of root apps dir
		File[] dirs = appDirBase.listFiles(new DirectoryFilter());
		// Search for additional context files
		for (File dir : dirs) {
			String dirName = '/' + dir.getName();
			// check to see if the directory is already mapped
			if (null == host.findChild(dirName)) {
				String webappContextDir = FileUtil.formatPath(appDirBase.getAbsolutePath(), dirName);
				log.debug("Webapp context directory (full path): {}", webappContextDir);
				Context ctx = null;
				if ("/root".equals(dirName) || "/root".equalsIgnoreCase(dirName)) {
					log.trace("Adding ROOT context");
					ctx = addContext("", webappContextDir);
				} else {
					log.trace("Adding context from directory scan: {}", dirName);
					ctx = addContext(dirName, webappContextDir);
				}
				log.trace("Context: {}", ctx);
				//see if the application requests php support
				String enablePhp = ctx.findParameter("enable-php");
				//if its null try to read directly
				if (enablePhp == null) {
					File webxml = new File(webappContextDir + "/WEB-INF/", "web.xml");
					if (webxml.exists() && webxml.canRead()) {
						try {
							DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
							docBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); // Compliant
							docBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, ""); // compliant
							DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
							Document doc = docBuilder.parse(webxml);
							// normalize text representation
							doc.getDocumentElement().normalize();
							log.trace("Root element of the doc is {}", doc.getDocumentElement().getNodeName());
							NodeList listOfElements = doc.getElementsByTagName("context-param");
							int totalElements = listOfElements.getLength();
							log.trace("Total no of elements: {}", totalElements);
							for (int s = 0; s < totalElements; s++) {
								Node fstNode = listOfElements.item(s);
								if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
									Element fstElmnt = (Element) fstNode;
									NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("param-name");
									Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
									NodeList fstNm = fstNmElmnt.getChildNodes();
									String pName = (fstNm.item(0)).getNodeValue();
									log.trace("Param name: {}", pName);
									if ("enable-php".equals(pName)) {
										NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("param-value");
										Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
										NodeList lstNm = lstNmElmnt.getChildNodes();
										String pValue = (lstNm.item(0)).getNodeValue();
										log.trace("Param value: {}", pValue);
										enablePhp = pValue;
										//
										break;
									}
								}
							}
						} catch (Exception e) {
							log.warn("Error reading web.xml", e);
						}
					}
					webxml = null;
				}
				log.debug("Enable php: {}", enablePhp);
				if ("true".equals(enablePhp)) {
					log.info("Adding PHP (Quercus) servlet for context: {}", ctx.getName());
					// add servlet wrapper
					StandardWrapper wrapper = (StandardWrapper) ctx.createWrapper();
					wrapper.setServletName("QuercusServlet");
					wrapper.setServletClass("com.caucho.quercus.servlet.QuercusServlet");
					log.debug("Wrapper: {}", wrapper);
					ctx.addChild(wrapper);
					// add servlet mappings
					ctx.addServletMappingDecoded("*.php", "QuercusServlet");
				}
				webappContextDir = null;
			}
		}
		appDirBase = null;
		dirs = null;
		// Dump context list
		if (log.isDebugEnabled()) {
			for (Container cont : host.findChildren()) {
				log.debug("Context child name: {}", cont.getName());
			}
		}
		// set a realm on the "server" if specified
		if (realm != null) {
			embedded.getEngine().setRealm(realm);
		} else {
			realm = new NullRealm();
			embedded.getEngine().setRealm(realm);
		}
		// use Tomcat jndi or not
		if (Boolean.TRUE.equals(useNaming)) {
			embedded.enableNaming();
		}
		// add the valves to the host
		for (Valve valve : valves) {
			log.debug("Adding host valve: {}", valve);
			((StandardHost) host).addValve(valve);
		}
		// add any additional hosts
		if (hosts != null && !hosts.isEmpty()) {
			// grab current contexts from base host
			Container[] currentContexts = host.findChildren();
			log.info("Adding {} additional hosts", hosts.size());
			for (Host h : hosts) {
				log.debug("Host - name: {} appBase: {} info: {}", new Object[] { h.getName(), h.getAppBase(), h });
				//add the contexts to each host
				for (Container cont : currentContexts) {
					Context c = (Context) cont;
					addContext(c.getPath(), c.getDocBase(), h);
				}
				//add the host to the engine
				engine.addChild(h);
			}
		}
		try {
			// loop through connectors and apply methods / props
			boolean added = false;
			for (TomcatConnector tomcatConnector : connectors) {
				// get the connector
				Connector connector = tomcatConnector.getConnector();
				// add new Connector to set of Connectors for embedded server, associated with Engine
				if (!added) {
					embedded.setConnector(connector);
					added = true;
				} else {
					embedded.getService().addConnector(connector);
				}
				log.trace("Connector oName: {}", connector.getObjectName());
			}
		} catch (Exception ex) {
			log.warn("An exception occurred during network configuration", ex);
		}
		// create an executor for "ordered" start-up of the webapps
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			log.info("Starting Tomcat servlet engine");
			embedded.start();
			// create references for later lookup
			LoaderBase.setApplicationLoader(new TomcatApplicationLoader(embedded, host, applicationContext));
			for (final Container cont : host.findChildren()) {
				if (cont instanceof StandardContext) {
					if (log.isDebugEnabled()) {
						ContainerBase cb = (ContainerBase) cont;
						log.debug("Oname - domain: {}", cb.getDomain());
					}
					final StandardContext ctx = (StandardContext) cont;
					final ServletContext servletContext = ctx.getServletContext();
					// set the hosts id
					servletContext.setAttribute("red5.host.id", getHostId());
					final String prefix = servletContext.getRealPath("/");
					log.info("Context initialized: {} path: {}", servletContext.getContextPath(), prefix);
					try {
						ctx.resourcesStart();
						log.debug("Context - privileged: {}, start time: {}, reloadable: {}", new Object[] { ctx.getPrivileged(), ctx.getStartTime(), ctx.getReloadable() });
						Loader cldr = ctx.getLoader();
						log.debug("Loader delegate: {} type: {}", cldr.getDelegate(), cldr.getClass().getName());
						if (log.isTraceEnabled()) {
							if (cldr instanceof WebappLoader) {
								log.trace("WebappLoader class path: {}", ((WebappLoader) cldr).getClasspath());
							}
						}
						final ClassLoader webClassLoader = cldr.getClassLoader();
						log.debug("Webapp classloader: {}", webClassLoader);
						// get the (spring) config file path
						final String contextConfigLocation = servletContext.getInitParameter(org.springframework.web.context.ContextLoader.CONFIG_LOCATION_PARAM) == null ? defaultSpringConfigLocation : servletContext.getInitParameter(org.springframework.web.context.ContextLoader.CONFIG_LOCATION_PARAM);
						log.debug("Spring context config location: {}", contextConfigLocation);
						// get the (spring) parent context key
						final String parentContextKey = servletContext.getInitParameter("parentContextKey") == null ? defaultParentContextKey : servletContext.getInitParameter("parentContextKey");
						log.info("Spring parent context key: {}", parentContextKey);
						// set current threads classloader to the webapp classloader
						Thread.currentThread().setContextClassLoader(webClassLoader);
						// create a thread to speed-up application loading
						Future<?> appStartTask = executor.submit(new Runnable() {
							public void run() {
								//set thread context classloader to web classloader
								Thread.currentThread().setContextClassLoader(webClassLoader);
								Thread.currentThread().setName("Loader:" + servletContext.getContextPath());
								//get the web app's parent context
								ApplicationContext parentContext = null;
								if (applicationContext.containsBean(parentContextKey)) {
									parentContext = (ApplicationContext) applicationContext.getBean(parentContextKey);
								} else {
									log.warn("Parent context was not found: {}", parentContextKey);
								}
								// create a spring web application context
								final String contextClass = servletContext.getInitParameter(org.springframework.web.context.ContextLoader.CONTEXT_CLASS_PARAM) == null ? XmlWebApplicationContext.class.getName() : servletContext.getInitParameter(org.springframework.web.context.ContextLoader.CONTEXT_CLASS_PARAM);
								// web app context (spring)
								ConfigurableWebApplicationContext appctx = null;
								try {
									Class<?> clazz = Class.forName(contextClass, true, webClassLoader);
									appctx = (ConfigurableWebApplicationContext) clazz.newInstance();
									// set the root webapp ctx attr on the each servlet context so spring can find it later
									servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appctx);
									appctx.setConfigLocations(new String[] { contextConfigLocation });
									appctx.setServletContext(servletContext);
									// set parent context or use current app context
									if (parentContext != null) {
										appctx.setParent(parentContext);
									} else {
										appctx.setParent(applicationContext);
									}
									// refresh the factory
									log.trace("Classloader prior to refresh: {}", appctx.getClassLoader());
									appctx.refresh();
									if (log.isDebugEnabled()) {
										log.debug("Red5 app is active: {} running: {}", appctx.isActive(), appctx.isRunning());
									}

									appctx.start();
								} catch (Throwable e) {
									log.error(ExceptionUtils.getStackTrace(e));
									throw new RuntimeException("Failed to load webapplication context class", e);
								}
							}
						});
						// see if everything completed
						log.debug("Context: {} done: {}", servletContext.getContextPath(), appStartTask.isDone());
					} catch (Throwable t) {
						log.error("Error setting up context: {} due to: {}", servletContext.getContextPath(), t.getMessage());
						log.error(ExceptionUtils.getStackTrace(t));
					} finally {
						//reset the classloader
						Thread.currentThread().setContextClassLoader(originalClassLoader);
					}
				}
			}

		} catch (Exception e) {
			if (e instanceof BindException || e.getMessage().indexOf("BindException") != -1) {
				log.error("Error loading tomcat, unable to bind connector. You may not have permission to use the selected port", e);
			} else {
				log.error("Error loading tomcat", e);
			}
		} finally {
			// finish-up with the executor
			executor.shutdown();
			// do our jmx stuff
			registerJMX();
		}
		log.debug("Tomcat load completed");
	}

	/**
	 * Starts a web application and its red5 (spring) component. This is basically a stripped down version of start().
	 * 
	 * @return true on success
	 * @throws ServletException
	 */
	public boolean startWebApplication(String applicationName) throws ServletException {
		log.info("Starting Tomcat - Web application");
		boolean result = false;
		//get a reference to the current threads classloader
		final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

		try {
			log.debug("Webapp root: {}", webappFolder);
			if (webappFolder == null) {
				// Use default webapps directory
				webappFolder = System.getProperty("red5.root") + "/webapps";
			}
			System.setProperty("red5.webapp.root", webappFolder);
			log.info("Application root: {}", webappFolder);
			// application directory
			String contextName = '/' + applicationName;
			Container ctx = null;
			// Root applications directory
			File appDirBase = new File(webappFolder);
			// check if the context already exists for the host
			if ((ctx = host.findChild(contextName)) == null) {
				log.debug("Context did not exist in host");
				String webappContextDir = FileUtil.formatPath(appDirBase.getAbsolutePath(), applicationName);
				log.debug("Webapp context directory (full path): {}", webappContextDir);
				// set the newly created context as the current container
				ctx = addContext(contextName, webappContextDir);
			} else {
				log.debug("Context already exists in host");
			}
			final ServletContext servletContext = ((Context) ctx).getServletContext();
			log.debug("Context initialized: {}", servletContext.getContextPath());
			String prefix = servletContext.getRealPath("/");

			log.debug("Path: {}", prefix);

			Loader cldr = ((Context) ctx).getLoader();
			log.debug("Loader delegate: {} type: {}", cldr.getDelegate(), cldr.getClass().getName());
			if (cldr instanceof WebappLoader) {
				log.debug("WebappLoader class path: {}", ((WebappLoader) cldr).getClasspath());
			}
			final ClassLoader webClassLoader = cldr.getClassLoader();
			log.debug("Webapp classloader: {}", webClassLoader);
			// get the (spring) config file path
			final String contextConfigLocation = servletContext.getInitParameter("contextConfigLocation") == null ? defaultSpringConfigLocation : servletContext.getInitParameter("contextConfigLocation");
			log.debug("Spring context config location: {}", contextConfigLocation);
			// get the (spring) parent context key
			final String parentContextKey = servletContext.getInitParameter("parentContextKey") == null ? defaultParentContextKey : servletContext.getInitParameter("parentContextKey");
			log.debug("Spring parent context key: {}", parentContextKey);
			//set current threads classloader to the webapp classloader
			Thread.currentThread().setContextClassLoader(webClassLoader);
			//create a thread to speed-up application loading
			Thread thread = new Thread("Launcher:" + servletContext.getContextPath()) {
				@SuppressWarnings("cast")
				public void run() {

					//set current threads classloader to the webapp classloader
					Thread.currentThread().setContextClassLoader(webClassLoader);
					// create a spring web application context
					XmlWebApplicationContext appctx = new XmlWebApplicationContext();
					appctx.setClassLoader(webClassLoader);
					appctx.setConfigLocations(new String[] { contextConfigLocation });
					// check for red5 context bean
					ApplicationContext parentAppCtx = null;
					if (applicationContext.containsBean(defaultParentContextKey)) {
						parentAppCtx = (ApplicationContext) applicationContext.getBean(defaultParentContextKey);
						appctx.setParent(parentAppCtx);
					} else {
						log.warn("{} bean was not found in context: {}", defaultParentContextKey, applicationContext.getDisplayName());
						// lookup context loader and attempt to get what we need from it
						if (applicationContext.containsBean("context.loader")) {
							ContextLoader contextLoader = (ContextLoader) applicationContext.getBean("context.loader");
							parentAppCtx = contextLoader.getContext(defaultParentContextKey);
							appctx.setParent(parentAppCtx);
						} else {
							log.debug("Context loader was not found, trying JMX");
							MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
							// get the ContextLoader from jmx
							ContextLoaderMXBean proxy = null;
							ObjectName oName = null;
							try {
								oName = new ObjectName("org.red5.server:name=contextLoader,type=ContextLoader");
								if (mbs.isRegistered(oName)) {
									proxy = JMX.newMXBeanProxy(mbs, oName, ContextLoaderMXBean.class, true);
									log.debug("Context loader was found");
									proxy.setParentContext(defaultParentContextKey, appctx.getId());
								} else {
									log.warn("Context loader was not found");
								}
							} catch (Exception e) {
								log.warn("Exception looking up ContextLoader", e);
							}
						}
					}

					// add the servlet context
					appctx.setServletContext(servletContext);
					// set the root webapp ctx attr on the each servlet context so spring can find it later
					servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appctx);
					log.info("Setting root web app context attribute for {}", applicationName);
					appctx.refresh();

				}
			};
			thread.setDaemon(true);
			thread.start();
			result = true;
		} catch (Throwable t) {
			log.error("Error setting up context: {} due to: {}", applicationName, t.getMessage());
			log.error(ExceptionUtils.getStackTrace(t));
		} finally {
			//reset the classloader
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
		return result;
	}

	/**
	 * Set base host.
	 * 
	 * @param baseHost
	 *            Base host
	 */
	public void setBaseHost(Host baseHost) {
		log.debug("setBaseHost: {}", baseHost);
		this.host = baseHost;
	}

	/**
	 * Get base host.
	 * 
	 * @return Base host
	 */
	public Host getBaseHost() {
		return host;
	}

	/**
	 * Return Tomcat engine.
	 * 
	 * @return Tomcat engine
	 */
	public Engine getEngine() {
		return engine;
	}

	/**
	 * Set connectors.
	 * 
	 * @param connectors
	 */
	public void setConnectors(List<TomcatConnector> connectors) {
		log.debug("setConnectors: {}", connectors.size());
		this.connectors = connectors;
	}

	/**
	 * Set additional contexts.
	 * 
	 * @param contexts
	 *            Map of contexts
	 * @throws ServletException
	 */
	public void setContexts(Map<String, String> contexts) throws ServletException {
		log.debug("setContexts: {}", contexts.size());
		for (Map.Entry<String, String> entry : contexts.entrySet()) {
			host.addChild(embedded.addWebapp(entry.getKey(), webappFolder + entry.getValue()));
		}
	}

	/**
	 * Setter for embedded object.
	 * 
	 * @param embedded
	 *            Embedded object
	 */
	public void setEmbedded(EmbeddedTomcat embedded) {
		log.info("Setting embedded: {}", embedded.getClass().getName());
		TomcatLoader.embedded = embedded;
	}

	/**
	 * Getter for embedded object.
	 * 
	 * @return Embedded object
	 */
	public EmbeddedTomcat getEmbedded() {
		return embedded;
	}

	/**
	 * Get the host.
	 * 
	 * @return host
	 */
	public Host getHost() {
		return host;
	}

	/**
	 * Set the host.
	 * 
	 * @param host
	 *            host
	 */
	public void setHost(Host host) {
		log.debug("setHost");
		this.host = host;
	}

	/**
	 * Set additional hosts.
	 * 
	 * @param hosts
	 *            List of hosts added to engine
	 */
	public void setHosts(List<Host> hosts) {
		log.debug("setHosts: {}", hosts.size());
		this.hosts = hosts;
	}

	/**
	 * Setter for realm.
	 * 
	 * @param realm
	 *            Realm
	 */
	public void setRealm(Realm realm) {
		log.info("Setting realm: {}", realm.getClass().getName());
		this.realm = realm;
	}

	/**
	 * Getter for realm.
	 * 
	 * @return Realm
	 */
	public Realm getRealm() {
		return realm;
	}

	/**
	 * Set additional valves.
	 * 
	 * @param valves
	 *            List of valves
	 */
	public void setValves(List<Valve> valves) {
		log.debug("setValves: {}", valves.size());
		this.valves.addAll(valves);
	}

	/**
	 * Returns a semi-unique id for this host based on its host values
	 * 
	 * @return host id
	 */
	public String getHostId() {
		String hostId = host.getName();
		log.debug("Host id: {}", hostId);
		return hostId;
	}

	protected void registerJMX() {
		// register with jmx
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName oName = new ObjectName("org.red5.server:type=TomcatLoader");
			// check for existing registration before registering
			if (!mbs.isRegistered(oName)) {
				mbs.registerMBean(this, oName);
			} else {
				log.debug("ContextLoader is already registered in JMX");
			}
		} catch (Exception e) {
			log.warn("Error on jmx registration", e);
		}
	}

	protected void unregisterJMX() {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName oName = new ObjectName("org.red5.server:type=TomcatLoader");
			mbs.unregisterMBean(oName);
		} catch (Exception e) {
			log.warn("Exception unregistering", e);
		}
	}

	/**
	 * Shut server down.
	 */
	@Override
	public void destroy() throws Exception {
		log.info("Shutting down Tomcat context");
		// run through the applications and ensure that spring is told to commence shutdown / disposal
		AbstractApplicationContext absCtx = (AbstractApplicationContext) LoaderBase.getApplicationContext();
		if (absCtx != null) {
			log.debug("Using loader base application context for shutdown");
			// get all the app (web) contexts and shut them down first
			Map<String, IApplicationContext> contexts = LoaderBase.getRed5ApplicationContexts();
			if (contexts.isEmpty()) {
				log.info("No contexts were found to shutdown");
			}
			for (Map.Entry<String, IApplicationContext> entry : contexts.entrySet()) {
				// stop the context
				log.debug("Calling stop on context: {}", entry.getKey());
				entry.getValue().stop();
			}
			if (absCtx.isActive()) {
				log.debug("Closing application context");
				absCtx.close();
			}
		} else {
			log.error("Error getting Spring bean factory for shutdown");
		}
		try {
			// stop tomcat
			embedded.stop();
		} catch (Exception e) {
			log.warn("Tomcat could not be stopped", e);
			throw new RuntimeException("Tomcat could not be stopped");
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TomcatLoader [serviceEngineName=" + serviceEngineName + "]";
	}

	/**
	 * Get cluster
	 * @return cluster object
	 */
	public Cluster getCluster() {
		return cluster;
	}

	/**
	 * Set cluster
	 * @param cluster object
	 */
	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}
	
	public List<TomcatConnector> getConnectors() {
		return connectors;
	}

}
