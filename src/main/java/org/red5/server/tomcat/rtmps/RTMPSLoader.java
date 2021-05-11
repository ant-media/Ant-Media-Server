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

package org.red5.server.tomcat.rtmps;

import java.io.File;

import javax.servlet.ServletException;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Service;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.core.StandardService;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.loader.WebappLoader;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IServer;
import org.red5.server.tomcat.TomcatConnector;
import org.red5.server.tomcat.rtmpt.RTMPTLoader;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;

/**
 * Loader for the RTMPS server which uses Tomcat.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPSLoader extends RTMPTLoader {

    // Initialize Logging
    private Logger log = Red5LoggerFactory.getLogger(RTMPSLoader.class);

    /**
     * RTMPS Tomcat engine.
     */
    protected Engine rtmpsEngine;

    /**
     * Setter for server
     * 
     * @param server
     *            Value to set for property 'server'.
     */
    public void setServer(IServer server) {
        log.debug("RTMPS setServer");
        this.server = server;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ServletException
     */
    @SuppressWarnings("deprecation")
    @Override
    public void start() throws ServletException {
        log.info("Loading RTMPS context");
        rtmpsEngine = new StandardEngine();
        rtmpsEngine.setName("red5RTMPSEngine");
        rtmpsEngine.setDefaultHost(host.getName());
        rtmpsEngine.setRealm(embedded.getEngine().getRealm());

        Service service = new StandardService();
        service.setName("red5RTMPSEngine");
        service.setContainer(rtmpsEngine);

        // add the valves to the host
        for (Valve valve : valves) {
            log.debug("Adding host valve: {}", valve);
            ((StandardHost) host).addValve(valve);
        }

        // create and add root context
        File appDirBase = new File(webappFolder);
        String webappContextDir = FileUtil.formatPath(appDirBase.getAbsolutePath(), "/root");
        Context ctx = embedded.addWebapp("/", webappContextDir);
        //no reload for now
        ctx.setReloadable(false);
        log.debug("Context name: {}", ctx.getName());
        Object ldr = ctx.getLoader();
        log.trace("Context loader (null if the context has not been started): {}", ldr);
        if (ldr == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            //log.debug("Classloaders - Parent {}\nTCL {}\n\n", new Object[] {classLoader.getParent(), classLoader});
            ctx.setParentClassLoader(classLoader);

            WebappLoader wldr = new WebappLoader(classLoader);
            //add the Loader to the context
            ctx.setLoader(wldr);
        }
        appDirBase = null;
        webappContextDir = null;

        host.addChild(ctx);
        // add servlet wrapper
        StandardWrapper wrapper = new StandardWrapper();
        wrapper.setServletName("RTMPTServlet");
        wrapper.setServletClass("org.red5.server.net.rtmpt.RTMPTServlet");
        ctx.addChild(wrapper);

        // add servlet mappings
        ctx.addServletMapping("/open/*", "RTMPTServlet");
        ctx.addServletMapping("/close/*", "RTMPTServlet");
        ctx.addServletMapping("/send/*", "RTMPTServlet");
        ctx.addServletMapping("/idle/*", "RTMPTServlet");
        // add the host
        rtmpsEngine.addChild(host);
        // add new Engine to set of Engine for embedded server
        embedded.getServer().addService(service);
        try {
            // loop through connectors and apply methods / props
            for (TomcatConnector tomcatConnector : connectors) {
                // get the connector
                Connector connector = tomcatConnector.getConnector();
                // add new Connector to set of Connectors for embedded server, associated with Engine
                service.addConnector(connector);
                log.trace("Connector oName: {}", connector.getObjectName());
                log.info("Starting RTMPS engine");
                // start connector
                connector.start();
            }
        } catch (Exception e) {
            log.error("Error initializing RTMPS server instance", e);
        } finally {
            registerJMX();
        }

    }

}
