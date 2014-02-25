/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

import java.lang.reflect.Method;

import org.red5.server.adapter.StatefulScopeWrappingAdapter;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.ContextSelector;

/**
 * LoggerFactory to simplify requests for Logger instances within
 * Red5 applications. This class is expected to be run only once per
 * logger request and is optimized as such.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Red5LoggerFactory {

	private static boolean useLogback;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Logger getLogger(Class<?> clazz) {
		if (useLogback) {
			//determine the red5 app name or servlet context name
			String contextName = null;
			//if the incoming class extends StatefulScopeWrappingAdapter we lookup the context
			//by scope name
			boolean scopeAware = StatefulScopeWrappingAdapter.class.isAssignableFrom(clazz);
			//System.out.printf("Wrapper - %s\n", StatefulScopeWrappingAdapter.class.isAssignableFrom(clazz));
			if (scopeAware) {
				try {
					Class wrapper = null;
					if ((wrapper = clazz.asSubclass(StatefulScopeWrappingAdapter.class)) != null) {
						Method getScope = wrapper.getMethod("getScope", new Class[0]);
						//NPE will occur here if the scope is not yet set on the application adapter
						IScope scope = (IScope) getScope.invoke(null, new Object[0]);
						contextName = scope.getName();
					}
				} catch (Exception cce) {
					//cclog.warn("Exception {}", e);
				}
			} else {
				//route the Launcher entries to the correct context
				String[] parts = Thread.currentThread().getName().split("Launcher:/");
				if (parts.length > 1) {
					contextName = parts[1];
				}

			}

			/* TODO: For a future day, the context or application will be determined
			//get a reference to our caller
			Class caller = Reflection.getCallerClass(2);
			//System.err.printf("Caller class: %s classloader: %s\n", caller, caller.getClassLoader());

			try {
				//check to see if we've been called by a servlet
				Class sub = caller.asSubclass(Servlet.class);
				//System.err.println("Caller is a Servlet");
			
				//Method[] methods = caller.getMethods();
				//for (Method meth : methods) {
				//	System.err.printf("Method: %s\n", meth.getName());
				//}
				
				Method getContext = caller.getMethod("getServletContext", new Class[0]);
				//System.err.printf("got context method - %s\n", getContext);
				ServletContext context = (ServletContext) getContext.invoke(caller, null);
				System.err.printf("invoked context\n");
			
				contextName = context.getServletContextName();
				//System.err.printf("Servlet context name: %s\n", contextName);

				Method getContextName = context.getClass().getMethod("getServletContextName", new Class[0]);
				System.err.printf("got context name\n");
				Object ctxName = getContextName.invoke(null, new Object[0]);				
				
				System.err.printf("Servlet context result: %s\n", ctxName);
				if (ctxName != null && ctxName instanceof String) {
					contextName = ctxName.toString();
				}	
			} catch (Exception ex) {
				//ex.printStackTrace();
			}
			*/
			return getLogger(clazz, contextName);
		} else {
			return LoggerFactory.getLogger(clazz);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static Logger getLogger(Class clazz, String contextName) {
		if (useLogback) {
			Logger logger = null;
			try {
				//check for logback
				Class cs = Class.forName("ch.qos.logback.classic.selector.ContextSelector");
				//trigger an exception if the class doesn't actually exist
				cs.getDeclaredMethods();
				// get the class for static binding
				cs = Class.forName("org.slf4j.impl.StaticLoggerBinder");
				// get its declared methods
				Method[] methods = cs.getDeclaredMethods();
				for (Method method : methods) {
					//ensure method exists
					if (method.getName().equals("getContextSelector")) {
						//System.out.println("Logger context selector method found");
						//get the context selector
						StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
						Method m1 = binder.getClass().getMethod("getContextSelector", (Class[]) null);
						ContextSelector selector = (ContextSelector) m1.invoke(binder, (Object[]) null);
						//get the context for the given context name or default if null
						LoggerContext ctx = null;
						if (contextName != null && contextName.length() > 0) {
							ctx = selector.getLoggerContext(contextName);
						}
						// and if we get here, fall back to the default context
						if (ctx == null) {
							ctx = selector.getLoggerContext();
						}
						//debug
						//StatusPrinter.print(ctx);
						//get the logger from the context or default context
						logger = ((ctx != null) ? ctx.getLogger(clazz) : selector.getDefaultLoggerContext().getLogger(clazz));
						break;
					}
				}
			} catch (Exception e) {
				//no logback, use whatever logger is in-place
				System.err.printf("Exception %s", e.getMessage());
			}
			if (logger == null) {
				//no logback, use whatever logger is in-place
				logger = LoggerFactory.getLogger(clazz);
			}
			return logger;
		} else {
			return LoggerFactory.getLogger(clazz);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public static Logger getLogger(String name, String contextName) {
		Logger logger = null;
		try {
			//check for logback
			Class cs = Class.forName("ch.qos.logback.classic.selector.ContextSelector");
			//trigger an exception if the class doesn't actually exist
			cs.getDeclaredMethods();
			// get the class for static binding
			cs = Class.forName("org.slf4j.impl.StaticLoggerBinder");
			// get its declared methods
			Method[] methods = cs.getDeclaredMethods();
			for (Method method : methods) {
				//ensure method exists
				if (method.getName().equals("getContextSelector")) {
					//System.out.println("Logger context selector method found");
					//get the context selector
					StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
					Method m1 = binder.getClass().getMethod("getContextSelector", (Class[]) null);
					ContextSelector selector = (ContextSelector) m1.invoke(binder, (Object[]) null);
					//get the context for the given context name or default if null
					LoggerContext ctx = null;
					if (contextName != null && contextName.length() > 0) {
						ctx = selector.getLoggerContext(contextName);
					}
					// and if we get here, fall back to the default context
					if (ctx == null) {
						ctx = selector.getLoggerContext();
					}
					//debug
					//StatusPrinter.print(ctx);
					//get the logger from the context or default context
					logger = ((ctx != null) ? ctx.getLogger(name) : selector.getDefaultLoggerContext().getLogger(name));
					break;
				}
			}
		} catch (Exception e) {
			//no logback, use whatever logger is in-place
			System.err.printf("Exception %s", e.getMessage());
		}
		if (logger == null) {
			//no logback, use whatever logger is in-place
			logger = LoggerFactory.getLogger(name);
		}
		return logger;
	}

	public static ContextSelector getContextSelector() {
		ContextSelector selector = null;
		StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
		try {
			Method m1 = binder.getClass().getMethod("getContextSelector", (Class[]) null);
			selector = (ContextSelector) m1.invoke(binder, (Object[]) null);
		} catch (Exception e) {
			System.err.printf("Exception %s", e.getMessage());
		}
		return selector;
	}

	public static void setUseLogback(boolean useLogback) {
		Red5LoggerFactory.useLogback = useLogback;
	}

}