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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;

/**
 * A servlet filter that puts this contexts LoggerContext into a Threadlocal variable.
 * 
 * It removes it after the request is processed.
 *
 * To use it, add the following lines to a web.xml file
 *
 * <pre>
 * 	&lt;filter&gt;
 * 		&lt;filter-name&gt;LoggerContextFilter&lt;/filter-name&gt;
 * 		&lt;filter-class&gt;org.red5.logging.LoggerContextFilter&lt;/filter-class&gt;
 * 	&lt;/filter&gt;
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;LoggerContextFilter&lt;/filter-name&gt;
 * 		&lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * 	&lt;/filter-mapping&gt;
 * </pre>
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class LoggerContextFilter implements Filter {

    private String contextName;

    public void init(FilterConfig config) throws ServletException {
        ServletContext servletContext = config.getServletContext();
        contextName = servletContext.getContextPath().replaceAll("/", "");
        if ("".equals(contextName)) {
            contextName = "root";
        }
        System.out.printf("Filter init: %s%n", contextName);
        ConfigurableWebApplicationContext appctx = (ConfigurableWebApplicationContext) servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        if (appctx != null) {
            System.out.printf("ConfigurableWebApplicationContext is not null in LoggerContextFilter for: %s, this indicates a misconfiguration or load order problem%n", contextName);
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        LoggerContext context = (LoggerContext) request.getServletContext().getAttribute(Red5LoggerFactory.LOGGER_CONTEXT_ATTRIBUTE);
        // get the selector
        ContextSelector selector = Red5LoggerFactory.getContextSelector();
        if (context != null) {
            // set the thread local ref
            ((LoggingContextSelector) selector).setLocalContext(context);
        } else {
            System.err.printf("No context named %s was found%n", contextName);
        }
        chain.doFilter(request, response);
        // remove the thread local ref so that log contexts dont use the wrong contextName
        ((LoggingContextSelector) selector).removeLocalContext();
    }

    public void destroy() {
    }
}

