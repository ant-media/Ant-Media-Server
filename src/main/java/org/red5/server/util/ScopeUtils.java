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

package org.red5.server.util;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.red5.server.api.IContext;
import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.scope.IScopeService;
import org.red5.server.api.scope.ScopeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

/**
 * Collection of utilities for working with scopes
 */
public class ScopeUtils {

	private static final Logger log = LoggerFactory.getLogger(ScopeUtils.class);

	private static final String SERVICE_CACHE_PREFIX = "__service_cache:";

	/**
	 * Constant for slash symbol
	 */
	private static final String SLASH = "/";

	/**
	 * Resolves scope for specified scope and path.
	 *
	 * @param from Scope to use as context (to start from)
	 * @param path Path to resolve
	 * @return	Resolved scope
	 */
	public static IScope resolveScope(IScope from, String path) {
		log.debug("resolveScope from: {} path: {}", from.getName(), path);
		IScope current = from;
		if (path.startsWith(SLASH)) {
			current = ScopeUtils.findRoot(current);
			path = path.substring(1, path.length());
		}
		if (path.endsWith(SLASH)) {
			path = path.substring(0, path.length() - 1);
		}
		log.trace("Current: {}", current);
		String[] parts = path.split(SLASH);
		if (log.isTraceEnabled()) {
			log.trace("Parts: {}", Arrays.toString(parts));
		}
		for (String part : parts) {
			log.trace("Part: {}", part);
			if (part.equals(".")) {
				continue;
			}
			if (part.equals("..")) {
				if (!current.hasParent()) {
					return null;
				}
				current = current.getParent();
				continue;
			}
			if (!current.hasChildScope(part)) {
				return null;
			}
			current = current.getScope(part);
			log.trace("Current: {}", current);
		}
		return current;
	}

	/**
	 * Finds root scope for specified scope object. Root scope is the top level
	 * scope among scope's parents.
	 *
	 * @param from Scope to find root for
	 * @return	Root scope object
	 */
	public static IScope findRoot(IScope from) {
		IScope current = from;
		while (current.hasParent()) {
			current = current.getParent();
		}
		return current;
	}

	/**
	 * Returns the application scope for specified scope. Application scope has
	 * depth of 1 and has no parent.
	 *
	 * See <code>isApp</code> method for details.
	 *
	 * @param from Scope to find application for
	 * @return		Application scope.
	 */
	public static IScope findApplication(IScope from) {
		IScope current = from;
		while (current.hasParent() && !current.getType().equals(ScopeType.APPLICATION)) {
			current = current.getParent();
		}
		return current;
	}

	/**
	 * Check whether one scope is an ancestor of another
	 *
	 * @param from Scope
	 * @param ancestor Scope to check
	 * @return <code>true</code> if ancestor scope is really an ancestor of
	 *         scope passed as from parameter, <code>false</code> otherwise.
	 */
	public static boolean isAncestor(IBasicScope from, IBasicScope ancestor) {
		IBasicScope current = from;
		while (current.hasParent()) {
			current = current.getParent();
			if (current.equals(ancestor)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether scope is root or not
	 *
	 * @param scope Scope to check
	 * @return <code>true</code> if scope is root scope (top level scope),
	 *         <code>false</code> otherwise.
	 */
	public static boolean isRoot(IBasicScope scope) {
		return !scope.hasParent();
	}

	/**
	 * Check whether scope is the global scope (level 0 leaf in scope tree) or
	 * not
	 *
	 * When user connects the following URL: rtmp://localhost/myapp/foo/bar then /
	 * is the global level scope, myapp is app level, foo is room level and bar
	 * is room level as well (but with higher depth level)
	 *
	 * @param scope Scope to check
	 * @return <code>true</code> if scope is the global scope,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isGlobal(IBasicScope scope) {
		return scope.getType().equals(ScopeType.GLOBAL);
	}

	/**
	 * Check whether scope is an application scope (level 1 leaf in scope tree)
	 * or not
	 *
	 * @param scope Scope to check
	 * @return <code>true</code> if scope is an application scope,
	 *         <code>false</code> otherwise.
	 */
	public static boolean isApp(IBasicScope scope) {
		return scope.getType().equals(ScopeType.APPLICATION);
	}

	/**
	 * Check whether scope is a room scope (level 2 leaf in scope tree or lower,
	 * e.g. 3, 4, ...) or not
	 *
	 * @param scope Scope to check
	 * @return <code>true</code> if scope is a room scope, <code>false</code>
	 *         otherwise.
	 */
	public static boolean isRoom(IBasicScope scope) {
		return scope.getType().equals(ScopeType.ROOM);
	}

	/**
	 * Returns scope service by bean name. See overloaded method for details.
	 *
	 * @param scope scope
	 * @param name name
	 * @return object
	 */
	protected static Object getScopeService(IScope scope, String name) {
		return getScopeService(scope, name, null);
	}

	/**
	 * Returns scope services (e.g. SharedObject, etc) for the scope. Method
	 * uses either bean name passes as a string or class object.
	 *
	 * @param scope
	 *            The scope service belongs to
	 * @param name
	 *            Bean name
	 * @param defaultClass
	 *            Class of service
	 * @return				Service object
	 */
	protected static Object getScopeService(IScope scope, String name, Class<?> defaultClass) {
		if (scope != null) {
			final IContext context = scope.getContext();
			ApplicationContext appCtx = context.getApplicationContext();
			Object result;
			if (!appCtx.containsBean(name)) {
				if (defaultClass == null) {
					return null;
				}
				try {
					result = defaultClass.newInstance();
				} catch (Exception e) {
					log.error("{}", e);
					return null;
				}
			} else {
				result = appCtx.getBean(name);
			}
			return result;
		}
		return null;
	}

	/**
	 * Returns scope service that implements a given interface.
	 *
	 * @param scope The scope service belongs to
	 * @param intf The interface the service must implement
	 * @return Service object
	 */
	public static Object getScopeService(IScope scope, Class<?> intf) {
		return getScopeService(scope, intf, null);
	}

	public static Object getScopeService(IScope scope, Class<?> intf, boolean checkHandler) {
		return getScopeService(scope, intf, null, checkHandler);
	}

	/**
	 * Returns scope service that implements a given interface.
	 *
	 * @param scope The scope service belongs to
	 * @param intf The interface the service must implement
	 * @param defaultClass Class that should be used to create a new service if no service was found.
	 * @return Service object
	 */
	public static Object getScopeService(IScope scope, Class<?> intf, Class<?> defaultClass) {
		return getScopeService(scope, intf, defaultClass, true);
	}

	public static Object getScopeService(IScope scope, Class<?> intf, Class<?> defaultClass, boolean checkHandler) {
		if (scope == null || intf == null) {
			return null;
		}
		// We expect an interface
		assert intf.isInterface();

		String attr = IPersistable.TRANSIENT_PREFIX + SERVICE_CACHE_PREFIX + intf.getCanonicalName();
		if (scope.hasAttribute(attr)) {
			// return cached service
			return scope.getAttribute(attr);
		}

		Object handler = null;
		if (checkHandler) {
			IScope current = scope;
			while (current != null) {
				IScopeHandler scopeHandler = current.getHandler();
				if (intf.isInstance(scopeHandler)) {
					handler = scopeHandler;
					break;
				}
				if (!current.hasParent()) {
					break;
				}
				current = current.getParent();
			}
		}

		if (handler == null && IScopeService.class.isAssignableFrom(intf)) {
			// we've got an IScopeService, try to lookup bean
			Field key = null;
			Object serviceName = null;
			try {
				key = intf.getField("BEAN_NAME");
				serviceName = key.get(null);
				if (serviceName instanceof String) {
					handler = getScopeService(scope, (String) serviceName, defaultClass);
				}
			} catch (Exception e) {
				log.debug("No string field 'BEAN_NAME' in that interface");
			}
		}
		if (handler == null && defaultClass != null) {
			try {
				handler = defaultClass.newInstance();
			} catch (Exception e) {
				log.error("", e);
			}
		}
		// cache service
		scope.setAttribute(attr, handler);
		return handler;
	}

}
