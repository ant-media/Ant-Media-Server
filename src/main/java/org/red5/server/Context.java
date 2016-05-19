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

package org.red5.server;

import java.beans.ConstructorProperties;
import java.io.IOException;

import javax.management.openmbean.CompositeData;

import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IContext;
import org.red5.server.api.IMappingStrategy;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.scope.IScopeResolver;
import org.red5.server.api.service.IServiceInvoker;
import org.red5.server.exception.ScopeHandlerNotFoundException;
import org.red5.server.jmx.mxbeans.ContextMXBean;
import org.red5.server.service.ServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.core.io.Resource;

/**
 * <p>
 * This is basic context implementation used by Red5.
 * </p>
 */
public class Context implements IContext, ApplicationContextAware, ContextMXBean {

    public static final Logger logger = LoggerFactory.getLogger(Context.class);

    /**
     * Spring application context
     */
    private ApplicationContext applicationContext;

    /**
     * Core context
     */
    private BeanFactory coreContext;

    /**
     * Context path
     */
    private String contextPath = "";

    /**
     * Scope resolver collaborator
     */
    private IScopeResolver scopeResolver;

    /**
     * Client registry
     */
    private IClientRegistry clientRegistry;

    /**
     * Service invoker collaborator
     */
    private IServiceInvoker serviceInvoker;

    /**
     * Mapping strategy collaborator
     */
    private IMappingStrategy mappingStrategy;

    /**
     * Persistence store
     */
    private IPersistenceStore persistanceStore;

    /**
     * Initializes core context bean factory using red5.core bean factory from red5.xml context
     */
    @ConstructorProperties(value = { "" })
    public Context() {
    }

    /**
     * Initializes app context and context path from given parameters
     * 
     * @param context
     *            Application context
     * @param contextPath
     *            Context path
     */
    @ConstructorProperties({ "context", "contextPath" })
    public Context(ApplicationContext context, String contextPath) {
        setApplicationContext(context);
        this.contextPath = contextPath;
    }

    /**
     * Return global scope
     * 
     * @return Global scope
     */
    public IGlobalScope getGlobalScope() {
        IGlobalScope gs = scopeResolver.getGlobalScope();
        logger.trace("Global scope: {}", gs);
        return gs;
    }

    /**
     * Return scope resolver
     * 
     * @return scope resolver
     */
    public IScopeResolver getScopeResolver() {
        return scopeResolver;
    }

    /**
     * Resolves scope using scope resolver collaborator
     * 
     * @param path
     *            Path to resolve
     * @return Scope resolution result
     */
    public IScope resolveScope(String path) {
        return scopeResolver.resolveScope(path);
    }

    /**
     * Resolves scope from given root using scope resolver.
     * 
     * @param root
     *            Scope to start from.
     * @param path
     *            Path to resolve.
     * @return Scope resolution result.
     */
    public IScope resolveScope(IScope root, String path) {
        return scopeResolver.resolveScope(root, path);
    }

    /**
     * Setter for client registry
     * 
     * @param clientRegistry
     *            Client registry
     */
    public void setClientRegistry(IClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
    }

    /**
     * Setter for mapping strategy
     * 
     * @param mappingStrategy
     *            Mapping strategy
     */
    public void setMappingStrategy(IMappingStrategy mappingStrategy) {
        this.mappingStrategy = mappingStrategy;
    }

    /**
     * Setter for scope resolver
     * 
     * @param scopeResolver
     *            Scope resolver used to resolve scopes
     */
    public void setScopeResolver(IScopeResolver scopeResolver) {
        this.scopeResolver = scopeResolver;
    }

    /**
     * Setter for service invoker
     * 
     * @param serviceInvoker
     *            Service invoker object
     */
    public void setServiceInvoker(IServiceInvoker serviceInvoker) {
        this.serviceInvoker = serviceInvoker;
    }

    /**
     * Return persistence store
     * 
     * @return Persistence store
     */
    public IPersistenceStore getPersistanceStore() {
        return persistanceStore;
    }

    /**
     * Setter for persistence store
     * 
     * @param persistanceStore
     *            Persistence store
     */
    public void setPersistanceStore(IPersistenceStore persistanceStore) {
        this.persistanceStore = persistanceStore;
    }

    /**
     * Setter for application context
     * 
     * @param context
     *            App context
     */
    public void setApplicationContext(ApplicationContext context) {
        this.applicationContext = context;
        String deploymentType = System.getProperty("red5.deployment.type");
        logger.debug("Deployment type: " + deploymentType);
        if (deploymentType == null) {
            // standalone core context
            String config = System.getProperty("red5.conf_file");
            if (config == null) {
                config = "red5.xml";
            }
            coreContext = ContextSingletonBeanFactoryLocator.getInstance(config).useBeanFactory("red5.core").getFactory();
        } else {
            logger.info("Setting parent bean factory as core");
            coreContext = applicationContext.getParentBeanFactory();
        }
    }

    /**
     * Return application context
     * 
     * @return App context
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Setter for context path. Adds a slash at the end of path if there isn't one
     * 
     * @param contextPath
     *            Context path
     */
    public void setContextPath(String contextPath) {
        if (!contextPath.endsWith("/")) {
            contextPath += '/';
        }
        this.contextPath = contextPath;
    }

    /**
     * Return client registry
     * 
     * @return Client registry
     */
    public IClientRegistry getClientRegistry() {
        return clientRegistry;
    }

    /**
     * Return scope
     * 
     * @return null
     */
    public IScope getScope() {
        return null;
    }

    /**
     * Return service invoker
     * 
     * @return Service invoker
     */
    public IServiceInvoker getServiceInvoker() {
        return serviceInvoker;
    }

    /**
     * Look up service by name
     * 
     * @param serviceName
     *            Service name
     * @return Service object
     * @throws ServiceNotFoundException
     *             When service found but null
     * @throws NoSuchBeanDefinitionException
     *             When bean with given name doesn't exist
     */
    public Object lookupService(String serviceName) {
        serviceName = getMappingStrategy().mapServiceName(serviceName);
        try {
            Object bean = applicationContext.getBean(serviceName);
            if (bean != null) {
                return bean;
            } else {
                throw new ServiceNotFoundException(serviceName);
            }
        } catch (NoSuchBeanDefinitionException err) {
            throw new ServiceNotFoundException(serviceName);
        }
    }

    /**
     * Look up scope handler for context path
     * 
     * @param contextPath
     *            Context path
     * @return Scope handler
     * @throws ScopeHandlerNotFoundException
     *             If there's no handler for given context path
     */
    public IScopeHandler lookupScopeHandler(String contextPath) {
        // Get target scope handler name
        String scopeHandlerName = getMappingStrategy().mapScopeHandlerName(contextPath);
        // Get bean from bean factory
        Object bean = applicationContext.getBean(scopeHandlerName);
        if (bean != null && bean instanceof IScopeHandler) {
            return (IScopeHandler) bean;
        } else {
            throw new ScopeHandlerNotFoundException(scopeHandlerName);
        }
    }

    /**
     * Return mapping strategy used by this context. Mapping strategy define naming rules (prefixes, postfixes, default application name, etc) for all named objects in context.
     * 
     * @return Mapping strategy
     */
    public IMappingStrategy getMappingStrategy() {
        return mappingStrategy;
    }

    /**
     * Return array or resource that match given pattern
     * 
     * @param pattern
     *            Pattern to check against
     * @return Array of Resource objects
     * @throws IOException
     *             On I/O exception
     * 
     * @see org.springframework.core.io.Resource
     */
    public Resource[] getResources(String pattern) throws IOException {
        return applicationContext.getResources(contextPath + pattern);
    }

    /**
     * Return resource by path
     * 
     * @param path
     *            Resource path
     * @return Resource
     * 
     * @see org.springframework.core.io.Resource
     */
    public Resource getResource(String path) {
        return applicationContext.getResource(contextPath + path);
    }

    /**
     * Resolve scope from host and path
     * 
     * @param host
     *            Host
     * @param path
     *            Path
     * @return Scope
     * 
     * @see org.red5.server.api.scope.IScope
     * @see org.red5.server.scope.Scope
     */
    public IScope resolveScope(String host, String path) {
        return scopeResolver.resolveScope(path);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasBean(String beanId) {
        return applicationContext.containsBean(beanId);
    }

    /**
     * Return bean instantiated by bean factory
     * 
     * @param beanId
     *            Bean name
     * @return Instantiated bean
     * 
     * @see org.springframework.beans.factory.BeanFactory
     */
    public Object getBean(String beanId) {
        // for war applications the "application" beans are not stored in the
        // sub-contexts, so look in the application context first and the core
        // context second
        Object bean = null;
        try {
            bean = applicationContext.getBean(beanId);
        } catch (NoSuchBeanDefinitionException e) {
            logger.warn("Bean lookup failed for {} in the application context", beanId, e);
        }
        if (bean == null) {
            bean = getCoreService(beanId);
        }
        return bean;
    }

    /**
     * Return core Red5 service instantiated by core context bean factory
     * 
     * @param beanId
     *            Bean name
     * @return Core Red5 service instantiated
     * 
     * @see org.springframework.beans.factory.BeanFactory
     */
    public Object getCoreService(String beanId) {
        return coreContext.getBean(beanId);
    }

    public void setCoreBeanFactory(BeanFactory core) {
        coreContext = core;
    }

    /**
     * Return current thread's context classloader
     * 
     * @return Classloder context of current thread
     */
    public ClassLoader getClassLoader() {
        return applicationContext.getClassLoader();
    }

    /**
     * Allows for reconstruction via CompositeData.
     * 
     * @param cd
     *            composite data
     * @return Context class instance
     */
    public static Context from(CompositeData cd) {
        Context instance = new Context();
        if (cd.containsKey("context") && cd.containsKey("contextPath")) {
            Object context = cd.get("context");
            Object contextPath = cd.get("contextPath");
            if (context != null && contextPath != null) {
                instance = new Context((ApplicationContext) context, (String) contextPath);
            }
        }
        return instance;
    }

}
