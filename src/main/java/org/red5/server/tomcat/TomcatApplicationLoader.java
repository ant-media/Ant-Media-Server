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

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.startup.Tomcat;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.LoaderBase;
import org.red5.server.api.IApplicationLoader;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

/**
 * Class that can load new applications in Tomcat.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class TomcatApplicationLoader implements IApplicationLoader {

    // Initialize Logging
    protected static Logger log = Red5LoggerFactory.getLogger(TomcatApplicationLoader.class);

    /** Store reference to embedded Tomcat engine. */
    private Tomcat embedded;

    /** Store reference to host Tomcat is running on. */
    private Host host;

    /** Stores reference to the root ApplicationContext. */
    private ApplicationContext rootCtx;

    /**
     * Wrap Tomcat engine and host.
     * 
     * @param embedded
     * @param host
     */
    protected TomcatApplicationLoader(Tomcat embedded, Host host, ApplicationContext rootCtx) {
        this.embedded = embedded;
        this.host = host;
        this.rootCtx = rootCtx;
    }

    /** {@inheritDoc} */
    public ApplicationContext getRootContext() {
        log.debug("getRootContext");
        return rootCtx;
    }

    /** {@inheritDoc} */
    public void loadApplication(String contextPath, String virtualHosts, String directory) throws Exception {
        log.debug("Load application - context path: {} directory: {} virtual hosts: {}", new Object[] { contextPath, directory, virtualHosts });
        if (directory.startsWith("file:")) {
            directory = directory.substring(5);
        }
        if (host.findChild(contextPath) == null) {
            Context c = embedded.addWebapp(contextPath, directory);
            LoaderBase.setRed5ApplicationContext(contextPath, new TomcatApplicationContext(c));
            host.addChild(c);
            //add virtual hosts / aliases
            String[] vhosts = virtualHosts.split(",");
            for (String s : vhosts) {
                if (!"*".equals(s)) {
                    //if theres a port, strip it
                    if (s.indexOf(':') == -1) {
                        host.addAlias(s);
                    } else {
                        host.addAlias(s.split(":")[0]);
                    }
                } else {
                    log.warn("\"*\" based virtual hosts not supported");
                }
            }
        } else {
            log.warn("Context path already exists with host");
        }
    }

}
