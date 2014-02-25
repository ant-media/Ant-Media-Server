/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

package org.red5.server.war;

import java.beans.Introspector;
import java.io.File;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.red5.io.amf.Output;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.ClientRegistry;
import org.red5.server.Context;
import org.red5.server.MappingStrategy;
import org.red5.server.Server;
import org.red5.server.api.Red5;
import org.red5.server.scope.GlobalScope;
import org.red5.server.scope.ScopeResolver;
import org.red5.server.scope.WebScope;
import org.red5.server.service.ServiceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import ch.qos.logback.classic.LoggerContext;

/**
 * Entry point from which the server config file is loaded while running within
 * a J2EE application container.
 * 
 * This listener should be registered after Log4jConfigListener in web.xml, if
 * the latter is used.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class WarLoaderServlet extends ContextLoaderListener {

	// Initialize Logging
	public static Logger logger = Red5LoggerFactory.getLogger(WarLoaderServlet.class);

	private static ArrayList<ServletContext> registeredContexts = new ArrayList<ServletContext>(3);

	private ConfigurableWebApplicationContext applicationContext;

	private DefaultListableBeanFactory parentFactory;

	private static ServletContext servletContext;

	private ClientRegistry clientRegistry;

	private ServiceInvoker globalInvoker;

	private MappingStrategy globalStrategy;

	private ScopeResolver globalResolver;

	private GlobalScope global;

	private Server server;

	/**
	 * Main entry point for the Red5 Server as a war
	 */
	// Notification that the web application is ready to process requests
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		if (null != servletContext) {
			return;
		}
		System.setProperty("red5.deployment.type", "war");

		servletContext = sce.getServletContext();
		String prefix = servletContext.getRealPath("/");

		if (System.getProperty("red5.webapp.root") == null) {
			File webapps = new File(prefix);
			System.setProperty("red5.webapp.root", webapps.getParent());
			webapps = null;
		}

		long time = System.currentTimeMillis();

		logger.info("{} WAR loader", Red5.VERSION);
		logger.debug("Path: {}", prefix);

		try {
			//use super to initialize
			super.contextInitialized(sce);
			//get the web context
			applicationContext = (ConfigurableWebApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			logger.debug("Root context path: {}", applicationContext.getServletContext().getContextPath());

			ConfigurableBeanFactory factory = applicationContext.getBeanFactory();

			// register default
			factory.registerSingleton("default.context", applicationContext);

			// get the main factory
			parentFactory = (DefaultListableBeanFactory) factory.getParentBeanFactory();

		} catch (Throwable t) {
			logger.error("", t);
		}

		long startupIn = System.currentTimeMillis() - time;
		logger.info("Startup done in: {} ms", startupIn);

	}

	/*
	 * Registers a subcontext with red5
	 */
	public void registerSubContext(String webAppKey) {
		// get the sub contexts - servlet context
		ServletContext ctx = servletContext.getContext(webAppKey);
		if (ctx == null) {
			ctx = servletContext;
		}
		ContextLoader loader = new ContextLoader();
		ConfigurableWebApplicationContext appCtx = (ConfigurableWebApplicationContext) loader.initWebApplicationContext(ctx);
		appCtx.setParent(applicationContext);
		appCtx.refresh();

		ctx.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx);

		ConfigurableBeanFactory appFactory = appCtx.getBeanFactory();

		logger.debug("About to grab Webcontext bean for {}", webAppKey);
		Context webContext = (Context) appCtx.getBean("web.context");
		webContext.setCoreBeanFactory(parentFactory);
		webContext.setClientRegistry(clientRegistry);
		webContext.setServiceInvoker(globalInvoker);
		webContext.setScopeResolver(globalResolver);
		webContext.setMappingStrategy(globalStrategy);

		WebScope scope = (WebScope) appFactory.getBean("web.scope");
		scope.setServer(server);
		scope.setParent(global);
		scope.register();
		scope.start();

		// register the context so we dont try to reinitialize it
		registeredContexts.add(ctx);

	}

	@Deprecated
	public ContextLoader getContextLoader() {
		return super.getContextLoader();
	}

	/**
	 * Clearing the in-memory configuration parameters, we will receive
	 * notification that the servlet context is about to be shut down
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		synchronized (servletContext) {
			logger.info("Webapp shutdown");
			// XXX Paul: grabbed this from
			// http://opensource.atlassian.com/confluence/spring/display/DISC/Memory+leak+-+classloader+won%27t+let+go
			// in hopes that we can clear all the issues with J2EE containers
			// during shutdown
			ServletContext ctx = null;
			try {
				ctx = sce.getServletContext();
				// prepare spring for shutdown
				Introspector.flushCaches();
				// dereg any drivers
				for (Enumeration<?> e = DriverManager.getDrivers(); e.hasMoreElements();) {
					Driver driver = (Driver) e.nextElement();
					if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {
						DriverManager.deregisterDriver(driver);
					}
				}
				// clear the AMF output cache
				Output.destroyCache();
				// stop the logger
				try {
					((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
				} catch (Exception e) {
				}
				// shutdown spring
				Object attr = ctx.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
				if (attr != null) {
					// get web application context from the servlet context
					ConfigurableWebApplicationContext applicationContext = (ConfigurableWebApplicationContext) attr;
					ConfigurableBeanFactory factory = applicationContext.getBeanFactory();
					// for (String scope : factory.getRegisteredScopeNames()) {
					// logger.debug("Registered scope: " + scope);
					// }
					try {
						for (String singleton : factory.getSingletonNames()) {
							logger.debug("Registered singleton: {}", singleton);
							factory.destroyScopedBean(singleton);
						}
					} catch (RuntimeException e) {
					}
					factory.destroySingletons();
					applicationContext.close();
				}
			} catch (Throwable e) {
				logger.warn("Exception {}", e);
			} finally {
				super.contextDestroyed(sce);
				if (ctx != null) {
					ctx.removeAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
				}
			}
		}
	}

}
