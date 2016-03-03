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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * A class that allows the LoggerFactory to access an web context based LoggerContext.
 * 
 * Add this java option -Dlogback.ContextSelector=org.red5.logging.LoggingContextSelector
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class LoggingContextSelector implements ContextSelector {

    private static boolean debug = false;

    private static final ConcurrentMap<String, LoggerContext> contextMap = new ConcurrentHashMap<>(6, 0.9f, 1);

    private static final Semaphore lock = new Semaphore(1, true);

    private final ThreadLocal<LoggerContext> threadLocal = new ThreadLocal<>();

    private final LoggerContext defaultContext;

    private volatile String contextName;

    private volatile String contextConfigFile;

    static {
        debug = System.getProperty("logback.debug") == null ? false : Boolean.valueOf(System.getProperty("logback.debug"));
    }

    public LoggingContextSelector(LoggerContext context) {
        if (debug) {
            System.out.printf("Setting default logging context: %s\n", context.getName());
        }
        defaultContext = context;
    }

    public LoggerContext getLoggerContext() {
        if (debug) {
            System.out.println("getLoggerContext request");
        }
        // First check if ThreadLocal has been set already
        LoggerContext lc = threadLocal.get();
        if (lc != null) {
            if (debug) {
                System.out.printf("Thread local found: %s\n", lc.getName());
            }
            return lc;
        }
        if (contextName == null) {
            if (debug) {
                System.out.println("Context name was null, returning default");
            }
            // We return the default context
            return defaultContext;
        } else {
            LoggerContext loggerContext = null;
            try {
                // use a semaphore to protect against the .001% times when this gets hit too quickly
                lock.acquire();
                // Let's see if we already know such a context
                loggerContext = contextMap.get(contextName);
                if (loggerContext == null) {
                    // We have to create a new LoggerContext
                    loggerContext = new LoggerContext();
                    loggerContext.setName(contextName);
                    // allow override using logbacks system prop
                    String overrideProperty = System.getProperty("logback.configurationFile");
                    if (overrideProperty == null) {
                        contextConfigFile = String.format("logback-%s.xml", contextName);
                    } else {
                        contextConfigFile = String.format(overrideProperty, contextName);
                    }
                    if (debug) {
                        System.out.printf("Context logger config file: %s%n", contextConfigFile);
                    }
                    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                    URL url = Loader.getResource(contextConfigFile, classloader);
                    if (url != null) {
                        try {
                            JoranConfigurator configurator = new JoranConfigurator();
                            loggerContext.reset();
                            configurator.setContext(loggerContext);
                            configurator.doConfigure(url);
                        } catch (JoranException e) {
                            StatusPrinter.print(loggerContext);
                        }
                    } else {
                        try {
                            ContextInitializer ctxInit = new ContextInitializer(loggerContext);
                            ctxInit.autoConfig();
                        } catch (JoranException je) {
                            StatusPrinter.print(loggerContext);
                        }
                    }
                    if (debug) {
                        System.out.printf("Adding logger context: %s to map for context: %s%n", loggerContext.getName(), contextName);
                    }
                    contextMap.put(contextName, loggerContext);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.release();
            }
            return loggerContext;
        }
    }

    public LoggerContext getLoggerContext(String name) {
        if (debug) {
            System.out.printf("getLoggerContext request for %s in context map %s%n", name, contextMap.containsKey(name));
        }
        return contextMap.get(name);
    }

    public LoggerContext getDefaultLoggerContext() {
        return defaultContext;
    }

    public void attachLoggerContext(String contextName, LoggerContext loggerContext) {
        contextMap.put(contextName, loggerContext);
    }

    public LoggerContext detachLoggerContext(String loggerContextName) {
        return contextMap.remove(loggerContextName);
    }

    public List<String> getContextNames() {
        List<String> list = new ArrayList<>();
        list.addAll(contextMap.keySet());
        return list;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public void setContextConfigFile(String contextConfigFile) {
        this.contextConfigFile = contextConfigFile;
    }

    /**
     * Returns the number of managed contexts Used for testing purposes
     * 
     * @return the number of managed contexts
     */
    public int getCount() {
        return contextMap.size();
    }

    /**
     * These methods are used by the LoggerContextFilter.
     * 
     * They provide a way to tell the selector which context to use, thus saving the cost of a JNDI call at each new request.
     * 
     * @param context
     *            logging context
     */
    public void setLocalContext(LoggerContext context) {
        threadLocal.set(context);
    }

    public void removeLocalContext() {
        threadLocal.remove();
    }

}
