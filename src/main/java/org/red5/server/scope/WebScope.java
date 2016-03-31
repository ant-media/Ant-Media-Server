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

package org.red5.server.scope;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletContext;

import org.red5.server.LoaderBase;
import org.red5.server.api.IApplicationContext;
import org.red5.server.api.IApplicationLoader;
import org.red5.server.api.IConnection;
import org.red5.server.api.IServer;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.jmx.mxbeans.WebScopeMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.context.ServletContextAware;

/**
 * <p>
 * Web scope is special scope that is aware of servlet context and represents scope of a Red5 application within a servlet container (or application server) such as Tomcat, Jetty or JBoss.
 * </p>
 * <p>
 * Web scope is aware of virtual hosts configuration for Red5 application and is the first scope that instantiated after Red5 application gets started.
 * </p>
 * <p>
 * Then it loads virtual hosts configuration, adds mappings of paths to global scope that is injected thru Spring IoC context file and runs initialization process.
 * </p>
 * 
 * Red5 server implementation instance and ServletContext are injected as well.
 */
@ManagedResource
public class WebScope extends Scope implements ServletContextAware, WebScopeMXBean, InitializingBean, DisposableBean {

    /**
     * Logger
     */
    protected static Logger log = LoggerFactory.getLogger(WebScope.class);

    /**
     * Server instance
     */
    protected transient IServer server;

    /**
     * The application context this webscope is running in.
     */
    protected transient IApplicationContext appContext;

    /**
     * Loader for new applications.
     */
    protected transient IApplicationLoader appLoader;

    /**
     * Servlet context
     */
    protected transient ServletContext servletContext;

    /**
     * Context path
     */
    protected String contextPath;

    /**
     * Virtual hosts list as string
     */
    protected String virtualHosts;

    /**
     * Hostnames
     */
    protected String[] hostnames;

    /**
     * Has the web scope been registered?
     */
    protected AtomicBoolean registered = new AtomicBoolean(false);

    /**
     * Is the scope currently shutting down?
     */
    protected AtomicBoolean shuttingDown = new AtomicBoolean(false);

    {
        type = ScopeType.APPLICATION;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        register();
    }

    @Override
    public void destroy() throws Exception {
        unregister();
        super.destroy();
    }

    /**
     * Setter for global scope. Sets persistence class.
     * 
     * @param globalScope
     *            Red5 global scope
     */
    public void setGlobalScope(IGlobalScope globalScope) {
        log.trace("Set global scope: {}", globalScope);
        // XXX: this is called from nowhere, remove?
        super.setParent(globalScope);
        try {
            setPersistenceClass(globalScope.getStore().getClass().getName());
        } catch (Exception error) {
            log.error("Could not set persistence class.", error);
        }
    }

    /**
     * Web scope has no name
     */
    public void setName() {
        throw new RuntimeException("Cannot set name, you must set context path");
    }

    /**
     * Can't set parent to Web scope. Web scope is top level.
     */
    public void setParent() {
        throw new RuntimeException("Cannot set parent, you must set global scope");
    }

    /**
     * Setter for server
     * 
     * @param server
     *            Server instance
     */
    public void setServer(IServer server) {
        log.info("Set server {}", server);
        this.server = server;
    }

    /**
     * Servlet context
     * 
     * @param servletContext
     *            Servlet context
     */
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Setter for context path
     * 
     * @param contextPath
     *            Context path
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
        super.setName(contextPath.substring(1));
    }

    /**
     * Return scope context path
     * 
     * @return Scope context path
     */
    @Override
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Setter for virtual hosts. Creates array of hostnames.
     * 
     * @param virtualHosts
     *            Virtual hosts list as string
     */
    public void setVirtualHosts(String virtualHosts) {
        this.virtualHosts = virtualHosts;
        // Split string into array of vhosts
        hostnames = virtualHosts.split(",");
        for (int i = 0; i < hostnames.length; i++) {
            hostnames[i] = hostnames[i].trim();
            if (hostnames[i].equals("*")) {
                hostnames[i] = "";
            }
        }
    }

    /**
     * Map all vhosts to global scope then initialize
     */
    public void register() {
        if (registered.compareAndSet(false, true)) {
            log.debug("Webscope registering: {}", contextPath);
            getAppContext();
            appLoader = LoaderBase.getApplicationLoader();
            //get the parent name
            String parentName = getParent().getName();
            //add host name mappings
            if (hostnames != null && hostnames.length > 0) {
                for (String hostName : hostnames) {
                    server.addMapping(hostName, getName(), parentName);
                }
            }
            init();
            // don't free configured scopes when a client disconnects
            keepOnDisconnect = true;
        } else {
            log.info("Webscope already registered; remove the 'init-method' from your 'web.scope' bean to prevent this message in the future.");
        }
    }

    /**
     * Uninitialize and remove all vhosts from the global scope.
     */
    public void unregister() {
        if (!registered.compareAndSet(true, false)) {
            log.info("Webscope not registered");
            return;
        }
        log.debug("Webscope un-registering: {}", contextPath);
        if (shuttingDown.compareAndSet(false, true)) {
            keepOnDisconnect = false;
            uninit();
            // disconnect all clients before unregistering
            Set<IConnection> conns = getClientConnections();
            for (IConnection conn : conns) {
                conn.close();
            }
            conns.clear();
            //
            if (hostnames != null && hostnames.length > 0) {
                for (String element : hostnames) {
                    server.removeMapping(element, getName());
                }
            }
            //check for null
            if (appContext == null) {
                log.debug("Application context is null, trying retrieve from loader");
                getAppContext();
            }
            //try to stop the app context
            if (appContext != null) {
                log.debug("Stopping app context");
                appContext.stop();
            } else {
                log.debug("Application context is null, could not be stopped");
            }
            // Various cleanup tasks
            store = null;
            setServletContext(null);
            setServer(null);
            appContext = null;
            shuttingDown.set(false);
        } else {
            log.info("Webscope is currently shutting down");
        }
    }

    /** {@inheritDoc} */
    @Override
    public IServer getServer() {
        return server;
    }

    /**
     * Return object that can be used to load new applications.
     * 
     * @return the application loader
     */
    public IApplicationLoader getApplicationLoader() {
        return appLoader;
    }

    /**
     * Sets the local app context variable based on host id if available in the servlet context.
     */
    private final void getAppContext() {
        //get the host id
        String hostId = null;
        //get host from servlet context
        if (servletContext != null) {
            ServletContext sctx = servletContext.getContext(contextPath);
            if (sctx != null) {
                hostId = (String) sctx.getAttribute("red5.host.id");
                log.trace("Host id from init param: {}", hostId);
            }
        }
        if (hostId != null) {
            appContext = LoaderBase.getRed5ApplicationContext(hostId + contextPath);
        } else {
            appContext = LoaderBase.getRed5ApplicationContext(contextPath);
        }
    }

    /**
     * Is the scope currently shutting down?
     * 
     * @return is shutting down
     */
    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

}
