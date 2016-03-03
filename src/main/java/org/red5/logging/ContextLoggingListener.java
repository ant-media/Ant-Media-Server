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

package org.red5.logging;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;

/**
 * A servlet context listener that puts this contexts LoggerContext into a static map of logger contexts within an overall singleton log context selector.
 * 
 * To use it, add the following line to a web.xml file
 *
 * <pre>
 * 	&lt;listener&gt;
 * 		&lt;listener-class&gt;org.red5.logging.ContextLoggingListener&lt;/listener-class&gt;
 * 	&lt;/listener&gt;
 * </pre>
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ContextLoggingListener implements ServletContextListener {

    public void contextDestroyed(ServletContextEvent event) {
        System.out.println("Context destroying...");
        String contextName = pathToName(event);
        ContextSelector selector = Red5LoggerFactory.getContextSelector();
        LoggerContext context = selector.detachLoggerContext(contextName);
        if (context != null) {
            Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            logger.debug("Shutting down context {}", contextName);
            context.reset();
        } else {
            System.err.printf("No context named %s was found", contextName);
        }
    }

    public void contextInitialized(ServletContextEvent event) {
        System.out.println("Context init...");
        String contextName = pathToName(event);
        System.out.printf("Logger name for context: %s%n", contextName);
        LoggingContextSelector selector = null;
        try {
            selector = (LoggingContextSelector) Red5LoggerFactory.getContextSelector();
            //set this contexts name
            selector.setContextName(contextName);
            LoggerContext context = selector.getLoggerContext();
            if (context != null) {
                Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
                logger.debug("Starting up context {}", contextName);
            } else {
                System.err.printf("No context named %s was found", contextName);
            }
        } catch (Exception e) {
            System.err.printf("LoggingContextSelector is not the correct type: %s", e.getMessage());
        } finally {
            //reset the name
            if (selector != null) {
                selector.setContextName(null);
            }
        }
    }

    private String pathToName(ServletContextEvent event) {
        String contextName = event.getServletContext().getContextPath().replaceAll("/", "");
        if ("".equals(contextName)) {
            contextName = "root";
        }
        return contextName;
    }

}
