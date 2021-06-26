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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import ch.qos.logback.core.CoreConstants;

/**
 * LoggerFactory to simplify requests for Logger instances within Red5 applications. This class is expected to be run only once per logger
 * request and is optimized as such.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Red5LoggerFactory {

    public static final String LOGGER_CONTEXT_ATTRIBUTE = "logger.context";

    private static boolean useLogback = true;

    public static boolean DEBUG = true;

    static {
        DEBUG = Boolean.valueOf(System.getProperty("logback.debug", "false"));
        try {
            Logger logger = LoggerFactory.getILoggerFactory().getLogger(Logger.ROOT_LOGGER_NAME);
            logger.debug("Red5LoggerFactory instanced by Thread: {}", Thread.currentThread().getName());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        if (DEBUG) {
            System.out.printf("getLogger for: %s thread: %s%n", clazz.getName(), Thread.currentThread().getName());
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            System.out.printf("class loader: %s%n", cl);
            // if cl is WebappClassLoader type we can probably get the context from it
            //if (cl instanceof WebappClassLoader) {
            //    getContextName()
            //}
        }
        Logger logger = null;
        if (useLogback) {
            // determine the red5 app name or servlet context name
            String contextName = CoreConstants.DEFAULT_CONTEXT_NAME;
            // route the Launcher entries to the correct context
            String[] parts = Thread.currentThread().getName().split("Loader:/");
            if (parts.length > 1) {
                contextName = parts[1];
            }
            logger = Red5LoggerFactory.getLogger(clazz, contextName);
            /*
            // get a reference to our caller
            Class caller = Thread.currentThread().getStackTrace()[2].getClassName();
            if (DEBUG) {
                System.out.printf("Caller class: %s classloader: %s%n", caller, caller.getClassLoader());
            }
            // if the incoming class extends StatefulScopeWrappingAdapter we lookup the context by scope name
            boolean scopeAware = StatefulScopeWrappingAdapter.class.isAssignableFrom(caller);
            if (DEBUG) {
                System.out.printf("scopeAware: %s%n", scopeAware);
            }
            if (scopeAware) {
                try {
                    Class wrapper = null;
                    if ((wrapper = caller.asSubclass(StatefulScopeWrappingAdapter.class)) != null) {
                        Method getScope = wrapper.getMethod("getScope", new Class[0]);
                        // NPE will occur here if the scope is not yet set on the application adapter
                        IScope scope = (IScope) getScope.invoke(null, new Object[0]);
                        if (DEBUG) {
                            System.out.printf("scope: %s%n", scope);
                        }
                        contextName = scope.getName();
                    }
                } catch (Exception cce) {
                    //cclog.warn("Exception {}", e);
                }
            } else {
                // if the incoming class is a servlet we lookup the context name
                boolean servletScopeAware = Servlet.class.isAssignableFrom(caller);
                if (DEBUG) {
                    System.out.printf("servletScopeAware: %s%n", servletScopeAware);
                }
                if (servletScopeAware) {
                    try {
                        Class wrapper = null;
                        if ((wrapper = caller.asSubclass(Servlet.class)) != null) {
                            //ServletConfig getServletConfig
                            Method getServletConfig = wrapper.getMethod("getServletConfig", new Class[0]);
                            // NPE will occur here if the scope is not yet set on the application adapter
                            ServletConfig config = (ServletConfig) getServletConfig.invoke(null, new Object[0]);
                            if (DEBUG) {
                                System.out.printf("config: %s%n", config);
                            }
                            contextName = config.getServletContext().getContextPath().replaceAll("/", "");
                            if ("".equals(contextName)) {
                                contextName = "root";
                            }
                        }
                    } catch (Exception cce) {
                        //cclog.warn("Exception {}", e);
                    }
                } else {
                    // route the Launcher entries to the correct context
                    String[] parts = Thread.currentThread().getName().split("Loader:/");
                    if (parts.length > 1) {
                        contextName = parts[1];
                    } else {
                        contextName = CoreConstants.DEFAULT_CONTEXT_NAME;
                    }
                }
            }
            logger = Red5LoggerFactory.getLogger(clazz, contextName);
            */
        }
        if (logger == null) {
            logger = LoggerFactory.getLogger(clazz);
        }
        return logger;
    }

    @SuppressWarnings({ "rawtypes" })
    public static Logger getLogger(Class clazz, String contextName) {
        return getLogger(clazz.getName(), contextName);
    }

    public static Logger getLogger(String name, String contextName) {
        if (DEBUG) {
            System.out.printf("getLogger for: %s in context: %s thread: %s%n", name, contextName, Thread.currentThread().getName());
        }
        Logger logger = null;
        if (useLogback) {
            // disallow null context names
            if (contextName == null) {
                contextName = CoreConstants.DEFAULT_CONTEXT_NAME;
            }
            try {
                ContextSelector selector = Red5LoggerFactory.getContextSelector();
                // get the context for the given context name or default if null
                LoggerContext context = selector.getLoggerContext(contextName);
                // and if we get here, fall back to the default context
                if (context == null) {
                    System.err.printf("No context named %s was found!!%n", contextName);
                }
                // get the logger from the context or default context
                if (context != null) {
                    logger = context.getLogger(name);
                    //                    System.out.printf("Application name: %s in context: %s%n", context.getProperty(KEY_APP_NAME), contextName);
                }
            } catch (Exception e) {
                // no logback, use whatever logger is in-place
                System.err.printf("Exception %s%n", e.getMessage());
                e.printStackTrace();
            }
        }
        if (logger == null) {
            logger = LoggerFactory.getLogger(name);
        }
        return logger;
    }

    public static ContextSelector getContextSelector() {
        if (useLogback) {
            ContextSelectorStaticBinder contextSelectorBinder = ContextSelectorStaticBinder.getSingleton();
            ContextSelector selector = contextSelectorBinder.getContextSelector();
            if (selector == null) {
                if (DEBUG) {
                    System.err.println("Context selector was null, creating default context");
                }
                LoggerContext defaultLoggerContext = new LoggerContext();
                defaultLoggerContext.setName(CoreConstants.DEFAULT_CONTEXT_NAME);
                try {
                    contextSelectorBinder.init(defaultLoggerContext, null);
                    selector = contextSelectorBinder.getContextSelector();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            //System.out.printf("Context selector: %s%n", selector.getClass().getName());
            return selector;
        }
        return null;
    }

    public static void setUseLogback(boolean useLogback) {
        Red5LoggerFactory.useLogback = useLogback;
    }

}