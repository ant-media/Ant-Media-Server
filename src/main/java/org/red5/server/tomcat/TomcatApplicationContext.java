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

import javax.servlet.ServletContext;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.core.StandardContext;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IApplicationContext;
import org.slf4j.Logger;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * Class that wraps a Tomcat webapp context.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire
 */
public class TomcatApplicationContext implements IApplicationContext {

    protected static Logger log = Red5LoggerFactory.getLogger(TomcatApplicationContext.class);

    /** Store a reference to the Tomcat webapp context. */
    private Context context;

    /**
     * Wrap the passed Tomcat webapp context.
     * 
     * @param context
     */
    protected TomcatApplicationContext(Context context) {
        log.debug("new context: {}", context);
        this.context = context;
    }

    /**
     * Stop the application and servlet contexts.
     */
    public void stop() {
        log.debug("stop");
        try {
            ServletContext servlet = context.getServletContext();
            Object o = servlet.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
            if (o != null) {
                log.debug("Spring context for {} was found", context.getName());
                ConfigurableWebApplicationContext appCtx = (ConfigurableWebApplicationContext) o;
                // close the red5 app
                if (appCtx.isRunning()) {
                    log.debug("Context was running, attempting to stop");
                    appCtx.stop();
                }
                if (appCtx.isActive()) {
                    log.debug("Context is active, attempting to close");
                    appCtx.close();
                }
            } else {
                log.warn("Spring context for {} was not found", context.getName());
            }
        } catch (Exception e) {
            log.error("Could not stop spring context", e);
        }
        context.getParent().removeChild(context);
        if (context instanceof StandardContext) {
            StandardContext ctx = (StandardContext) context;
            LifecycleState state = ctx.getState();
            if (state != LifecycleState.DESTROYED && state != LifecycleState.DESTROYING) {
                try {
                    if (state != LifecycleState.STOPPED && state != LifecycleState.STOPPING) {
                        // stop the tomcat context
                        ctx.stop();
                    }
                } catch (Exception e) {
                    log.error("Could not stop context", e);
                } finally {
                    try {
                        ctx.destroy();
                    } catch (Exception e) {
                        log.error("Could not destroy context", e);
                    }
                }
            }
        }
    }

}
