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

package org.red5.server.api;

import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.service.IServiceInvoker;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * The current context, this object basically wraps the Spring context
 * or in the case of the .Net version, any similar system.
 * 
 */
public interface IContext extends ResourcePatternResolver {

	public static final String ID = "red5.context";

	/**
	 * Getter for application context
	 *
	 * @return  Application context
	 */
	ApplicationContext getApplicationContext();

	// public IScopeResolver getScopeResolver();
	/**
	 * Get client registry. Client registry is a place where all clients are
	 * registred.
	 * 
	 * @return	Client registry object
	 */
	IClientRegistry getClientRegistry();

	/**
	 * Returns service invoker object. Service invokers are objects that make
	 * service calls to client side NetConnection objects.
	 * 
	 * @return		Service invoker object
	 */
	IServiceInvoker getServiceInvoker();

	/**
	 * Returns persistence store object, a storage for persistent objects like
	 * persistent SharedObjects.
	 * 
	 * @return	Persistence store object
	 */
	IPersistenceStore getPersistanceStore();

	/**
	 * Returns scope handler (object that handle all actions related to the
	 * scope) by path. See {@link IScopeHandler} for details.
	 * 
	 * @param path
	 *            Path of scope handler
	 * @return		Scope handler
	 */
	IScopeHandler lookupScopeHandler(String path);

	/**
	 * Returns scope by path. You can think of IScope as of tree items, used to
	 * separate context and resources between users. See {@link IScope} for more
	 * details.
	 * 
	 * @param path
	 *            Path of scope
	 * @return		IScope object
	 */
	IScope resolveScope(String path);

	/**
	 * Returns scope by path from given root. You can think of IScope as of tree
	 * items, used to separate context and resources between users.
	 * See {@link IScope} for more details.
	 * 
	 * @param root
	 *            Root to start from
	 * @param path
	 *            Path of scope
	 * @return		IScope object
	 */
	IScope resolveScope(IScope root, String path);

	/**
	 * Returns global scope reference
	 * 
	 * @return	global scope reference
	 */
	IGlobalScope getGlobalScope();

	/**
	 * Returns service by name. 
	 * 
	 * @param serviceName
	 *            Name of service
	 * @return				Service object
	 */
	Object lookupService(String serviceName);

	/**
	 * Returns bean by ID
	 * 
	 * @param beanId
	 *            Bean ID
	 * @return			Given bean instance
	 */
	Object getBean(String beanId);

	/**
	 * Returns true if the context contains a certain bean,
	 * false otherwise.
	 * @param beanId	The name of the bean to find. 
	 * @return	True if the bean exists, false otherwise. 
	 */
	boolean hasBean(String beanId);

	/**
	 * Returns core service by bean id
	 * 
	 * @param beanId
	 *            Bean ID
	 * @return			Core service
	 */
	Object getCoreService(String beanId);

	/**
	 * Returns IMappingStrategy object
	 * 
	 * @return	IMappingStrategy object
	 */
	public IMappingStrategy getMappingStrategy();
}