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

package org.red5.logging;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

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

    public void contextInitialized(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        String contextName = servletContext.getContextPath().replaceAll("/", "");
        if ("".equals(contextName)) {
            contextName = "root";
        }
        System.out.printf("Context init: %s%n", contextName);
        ConfigurableWebApplicationContext appctx = (ConfigurableWebApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (appctx != null) {
            System.out.printf("ConfigurableWebApplicationContext is not null in ContextLoggingListener for: %s, this indicates a misconfiguration or load order problem%n", contextName);
        }
        try {
            // get the selector
            ContextSelector selector = Red5LoggerFactory.getContextSelector();
            // get the logger context for this servlet / app context by name
            URL url = servletContext.getResource(String.format("/WEB-INF/classes/logback-%s.xml", contextName));
            if (url != null && Files.exists(Paths.get(url.toURI()))) {
                System.out.printf("Context logger config found: %s%n", url.toURI());
            } else {
                url = servletContext.getResource("/WEB-INF/classes/logback.xml");
                if (url != null && Files.exists(Paths.get(url.toURI()))) {
                    System.out.printf("Context logger config found: %s%n", url.toURI());
                }
            }
            // get the logger context for the servlet context
            LoggerContext loggerContext = url != null ? ((LoggingContextSelector) selector).getLoggerContext(contextName, url) : selector.getLoggerContext(contextName);
            // set the logger context for use elsewhere in the servlet context
            servletContext.setAttribute(Red5LoggerFactory.LOGGER_CONTEXT_ATTRIBUTE, loggerContext);
            // get the root logger for this context
            Logger logger = Red5LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME, contextName);
            logger.info("Starting up context: {}", contextName);
        } catch (Exception e) {
            System.err.printf("LoggingContextSelector is not the correct type: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    public void contextDestroyed(ServletContextEvent event) {
        ServletContext servletContext = event.getServletContext();
        LoggerContext context = (LoggerContext) servletContext.getAttribute(Red5LoggerFactory.LOGGER_CONTEXT_ATTRIBUTE);
        if (context != null) {
            Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            logger.debug("Shutting down context {}", context.getName());
            context.reset();
            context.stop();
        } else {
            System.err.printf("No logger context found for %s%n", event.getServletContext().getContextPath());
        }
    }

}
