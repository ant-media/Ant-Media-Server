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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.CoreConstants;
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

    private static final Semaphore lock = new Semaphore(1, true);

    private static final ConcurrentMap<String, LoggerContext> contextMap = new ConcurrentHashMap<>(6, 0.9f, 1);

    private static LoggerContext DEFAULT_CONTEXT;

    private final ThreadLocal<LoggerContext> threadLocal = new ThreadLocal<>();

    private volatile String contextConfigFile;

    public final static String KEY_APP_NAME = "application-name";

    public LoggingContextSelector(LoggerContext context) {
        if (DEFAULT_CONTEXT == null) {
            if (Red5LoggerFactory.DEBUG) {
                System.out.printf("Setting default logging context: %s%n", context.getName());
            }
            DEFAULT_CONTEXT = context;
            // add listener
            context.addListener(new Red5LoggerContextListener());
            String defaultContextName = DEFAULT_CONTEXT.getName();
            if (Red5LoggerFactory.DEBUG) {
                System.out.printf("defaultContextName: %s%n", defaultContextName);
            }
            if (defaultContextName == null) {
                DEFAULT_CONTEXT.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
            }
            // inject the name of the current application as "application-name" property of the LoggerContext
            DEFAULT_CONTEXT.putProperty(KEY_APP_NAME, "red5");
            // attach to the map
            attachLoggerContext(defaultContextName, DEFAULT_CONTEXT);
            // set on thread local
            threadLocal.set(DEFAULT_CONTEXT);
        } else {
            threadLocal.set(context);
        }
    }

    public LoggerContext getLoggerContext() {
        if (Red5LoggerFactory.DEBUG) {
            System.out.println("getLoggerContext request");
        }
        // check if ThreadLocal has been set already
        LoggerContext context = threadLocal.get();
        if (context != null) {
            if (Red5LoggerFactory.DEBUG) {
                System.out.printf("Thread local found: %s%n", context.getName());
            }
        } else {
            if (Red5LoggerFactory.DEBUG) {
                //System.out.println("Context name not specified, returning default");
            }
            context = DEFAULT_CONTEXT;
        }
        return context;
    }

    public LoggerContext getLoggerContext(String contextName) {
        if (Red5LoggerFactory.DEBUG) {
            System.out.printf("getLoggerContext request for %s in context map %s%n", contextName, contextMap.containsKey(contextName));
        }
        LoggerContext context = contextMap.get(contextName);
        if (context == null) {
            try {
                // use a semaphore to protect against the .001% times when this gets hit too quickly
                lock.acquire();
                // allow override using logbacks system prop
                String overrideProperty = System.getProperty("logback.configurationFile");
                if (overrideProperty == null) {
                    contextConfigFile = String.format("logback-%s.xml", contextName);
                } else {
                    contextConfigFile = String.format(overrideProperty, contextName);
                }
                if (Red5LoggerFactory.DEBUG) {
                    System.out.printf("Context logger config file: %s%n", contextConfigFile);
                }
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                URL url = Loader.getResource(contextConfigFile, classloader);
                if (url != null) {
                    try {
                        JoranConfigurator configurator = new JoranConfigurator();
                        // create a new LoggerContext
                        context = new LoggerContext();
                        context.setName(contextName);
                        // add listener
                        context.addListener(new Red5LoggerContextListener());
                        // reset
                        context.reset();
                        configurator.setContext(context);
                        configurator.doConfigure(url);
                        context.start();
                    } catch (JoranException e) {
                        StatusPrinter.print(context);
                    }
                } else {
                    if (Red5LoggerFactory.DEBUG) {
                        System.out.printf("Skipping logger context configure for: %s%n", contextName);
                    }
                    context = DEFAULT_CONTEXT;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.release();
            }
        }
        return context;
    }

    public LoggerContext getLoggerContext(String contextName, URL url) {
        if (Red5LoggerFactory.DEBUG) {
            System.out.printf("getLoggerContext request for %s in context map %s url: %s%n", contextName, contextMap.containsKey(contextName), url);
        }
        LoggerContext context = contextMap.get(contextName);
        if (context == null) {
            try {
                // use a semaphore to protect against the .001% times when this gets hit too quickly
                lock.acquire();
                // create a new LoggerContext
                context = new LoggerContext();
                context.setName(contextName);
                // add listener
                context.addListener(new Red5LoggerContextListener());
                if (url != null) {
                    try {
                        JoranConfigurator configurator = new JoranConfigurator();
                        context.reset();
                        configurator.setContext(context);
                        configurator.doConfigure(url);
                        context.start();
                    } catch (JoranException e) {
                        StatusPrinter.print(context);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.release();
            }
        }
        return context;
    }

    public LoggerContext getDefaultLoggerContext() {
        return DEFAULT_CONTEXT;
    }

    public void attachLoggerContext(String contextName, LoggerContext loggerContext) {
        if (Red5LoggerFactory.DEBUG) {
            System.out.printf("Adding logger context: %s to map for context: %s%n", loggerContext.getName(), contextName);
        }
        contextMap.put(contextName, loggerContext);
    }

    public LoggerContext detachLoggerContext(String contextName) {
        return contextMap.remove(contextName);
    }

    public List<String> getContextNames() {
        List<String> list = new ArrayList<>();
        list.addAll(contextMap.keySet());
        return list;
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

    class Red5LoggerContextListener implements LoggerContextListener {

        AtomicBoolean started = new AtomicBoolean(false);

        @Override
        public boolean isResetResistant() {
            return true;
        }

        @Override
        public void onStart(LoggerContext context) {
            String contextName = context.getName();
            if (Red5LoggerFactory.DEBUG) {
                System.out.printf("onStart: %s%n", contextName);
            }
            if (started.compareAndSet(false, true)) {
                attachLoggerContext(contextName, context);
            }
        }

        @Override
        public void onReset(LoggerContext context) {
            String contextName = context.getName();
            if (Red5LoggerFactory.DEBUG) {
                System.out.printf("onReset: %s%n", contextName);
            }
        }

        @Override
        public void onStop(LoggerContext context) {
            String contextName = context.getName();
            if (Red5LoggerFactory.DEBUG) {
                System.out.printf("onStop: %s%n", contextName);
            }
            if (started.compareAndSet(true, false)) {
                detachLoggerContext(contextName);
            }
        }

        @Override
        public void onLevelChange(Logger logger, Level level) {
            if (Red5LoggerFactory.DEBUG) {
                System.out.printf("onLevelChange: %s level: %s%n", logger, level);
            }
        }

    }
}
