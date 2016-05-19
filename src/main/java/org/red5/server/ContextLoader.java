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

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.jmx.mxbeans.ContextLoaderMXBean;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Red5 applications loader
 * 
 * @author The Red5 Project
 * @author Tiago Jacobs (tiago@imdt.com.br)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ManagedResource(objectName = "org.red5.server:name=contextLoader,type=ContextLoader", description = "ContextLoader")
public class ContextLoader implements ApplicationContextAware, InitializingBean, DisposableBean, ContextLoaderMXBean {

    protected static Logger log = Red5LoggerFactory.getLogger(ContextLoader.class);

    /**
     * Spring Application context
     */
    protected ApplicationContext applicationContext;

    /**
     * Spring parent app context
     */
    protected ApplicationContext parentContext;

    /**
     * Context location files
     */
    protected String contextsConfig;

    /**
     * MBean object name used for de/registration purposes.
     */
    private ObjectName oName;

    /**
     * Context map
     */
    protected ConcurrentMap<String, ApplicationContext> contextMap;

    /**
     * Registers with JMX and registers a shutdown hook.
     * 
     * @throws Exception
     *             I/O exception, casting exception and others
     */
    public void afterPropertiesSet() throws Exception {
        log.info("ContextLoader init");
        // register in jmx
        registerJMX();
        // initialize
        init();
    }

    /**
     * Un-loads or un-initializes the contexts; this is a shutdown method for this loader.
     */
    public void destroy() throws Exception {
        log.info("ContextLoader un-init");
        shutdown();
    }

    /**
     * Loads context settings from ResourceBundle (.properties file)
     */
    public void init() throws IOException {
        // Load properties bundle
        Properties props = new Properties();
        Resource res = applicationContext.getResource(contextsConfig);
        if (res.exists()) {
            // Load properties file
            props.load(res.getInputStream());
            // Pattern for arbitrary property substitution
            Pattern patt = Pattern.compile("\\$\\{([^\\}]+)\\}");
            Matcher matcher = null;
            // Iterate thru properties keys and replace config attributes with
            // system attributes
            for (Object key : props.keySet()) {
                String name = (String) key;
                String config = props.getProperty(name);
                String configReplaced = config + "";
                //
                matcher = patt.matcher(config);
                //execute the regex
                while (matcher.find()) {
                    String sysProp = matcher.group(1);
                    String systemPropValue = System.getProperty(sysProp);
                    if (systemPropValue == null) {
                        systemPropValue = "null";
                    }
                    configReplaced = configReplaced.replace(String.format("${%s}", sysProp), systemPropValue);
                }
                log.info("Loading: {} = {} => {}", new Object[] { name, config, configReplaced });
                matcher.reset();
                // Load context
                loadContext(name, configReplaced);
            }
            patt = null;
            matcher = null;
        } else {
            log.error("Contexts config must be set");
        }
    }

    /**
     * Loads a context (Red5 application) and stores it in a context map, then adds it's beans to parent (that is, Red5)
     * 
     * @param name
     *            Context name
     * @param config
     *            Filename
     */
    public void loadContext(String name, String config) {
        log.debug("Load context - name: {} config: {}", name, config);
        // check the existence of the config file
        try {
            File configFile = new File(config);
            if (!configFile.exists()) {
                log.warn("Config file was not found at: {}", configFile.getCanonicalPath());
                configFile = new File("file://" + config);
                if (!configFile.exists()) {
                    log.warn("Config file was not found at either: {}", configFile.getCanonicalPath());
                } else {
                    config = "file://" + config;
                }
            }
        } catch (IOException e) {
            log.error("Error looking for config file", e);
        }
        // add the context to the parent, this will be red5.xml
        ConfigurableBeanFactory factory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        if (factory.containsSingleton(name)) {
            log.warn("Singleton {} already exists, try unload first", name);
            return;
        }
        // if parent context was not set then lookup red5.common
        if (parentContext == null) {
            log.debug("Lookup common - bean:{} local:{} singleton:{}", new Object[] { factory.containsBean("red5.common"), factory.containsLocalBean("red5.common"), factory.containsSingleton("red5.common"), });
            parentContext = (ApplicationContext) factory.getBean("red5.common");
        }
        if (config.startsWith("/")) {
            // Spring always interprets files as relative, so will strip a leading slash unless we tell
            // it otherwise. It also appears to not need this for Windows
            // absolute paths (e.g. C:\Foo\Bar) so we don't catch that either
            String newConfig = "file://" + config;
            log.debug("Resetting {} to {}", config, newConfig);
            config = newConfig;
        }
        ApplicationContext context = new FileSystemXmlApplicationContext(new String[] { config }, parentContext);
        log.debug("Adding to context map - name: {} context: {}", name, context);
        if (contextMap == null) {
            contextMap = new ConcurrentHashMap<>(3, 0.9f, 1);
        }
        contextMap.put(name, context);
        // Register context in parent bean factory
        log.debug("Registering - name: {}", name);
        factory.registerSingleton(name, context);
    }

    /**
     * Unloads a context (Red5 application) and removes it from the context map, then removes it's beans from the parent (that is, Red5)
     * 
     * @param name
     *            Context name
     */
    public void unloadContext(String name) {
        log.debug("Un-load context - name: {}", name);
        ApplicationContext context = contextMap.remove(name);
        log.debug("Context from map: {}", context);
        String[] bnames = BeanFactoryUtils.beanNamesIncludingAncestors(context);
        for (String bname : bnames) {
            log.debug("Bean: {}", bname);
        }
        ConfigurableBeanFactory factory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        if (factory.containsSingleton(name)) {
            log.debug("Context found in parent, destroying: {}", name);
            FileSystemXmlApplicationContext ctx = (FileSystemXmlApplicationContext) factory.getSingleton(name);
            if (ctx.isRunning()) {
                log.debug("Context was running, attempting to stop");
                ctx.stop();
            }
            if (ctx.isActive()) {
                log.debug("Context is active, attempting to close");
                ctx.close();
            } else {
                try {
                    factory.destroyBean(name, ctx);
                } catch (Exception e) {
                    log.warn("Context destroy failed for: {}", name, e);
                    ctx.destroy();
                } finally {
                    if (factory.containsSingleton(name)) {
                        log.debug("Singleton still exists, trying another destroy method");
                        ((DefaultListableBeanFactory) factory).destroySingleton(name);
                    }
                }
            }
        } else {
            log.debug("Context does not contain singleton: {}", name);
        }
        context = null;
    }

    /**
     * Shut server down.
     */
    public void shutdown() {
        log.info("Shutting down");
        if (contextMap != null) {
            log.debug("Context map: {}", contextMap);
            try {
                // unload all the contexts in the map
                for (Map.Entry<String, ApplicationContext> entry : contextMap.entrySet()) {
                    String contextName = entry.getKey();
                    log.info("Unloading context {} on shutdown", contextName);
                    unloadContext(contextName);
                }
                contextMap.clear();
            } catch (Exception e) {
                log.warn("Exception shutting down contexts", e);
            } finally {
                contextMap = null;
            }
        }
        unregisterJMX();
        log.info("Shutdown complete");
    }

    /**
     * Return context by name
     * 
     * @param name
     *            Context name
     * @return Application context for given name
     */
    public ApplicationContext getContext(String name) {
        if (contextMap != null) {
            return contextMap.get(name);
        } else {
            return null;
        }
    }

    /**
     * Sets a parent context for child context based on a given key.
     * 
     * @param parentContextKey
     *            key for the parent context
     * @param appContextId
     *            id of the child context
     */
    public void setParentContext(String parentContextKey, String appContextId) {
        log.debug("Set parent context {} on {}", parentContextKey, appContextId);
        ApplicationContext parentContext = getContext(parentContextKey);
        if (parentContext != null) {
            XmlWebApplicationContext childContext = (XmlWebApplicationContext) getContext(appContextId);
            if (childContext != null) {
                childContext.setParent(parentContext);
            } else {
                log.debug("Child context not found");
            }
        } else {
            log.debug("Parent context not found");
        }
    }

    protected void registerJMX() {
        // register with jmx
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            oName = new ObjectName("org.red5.server:name=contextLoader,type=ContextLoader");
            // check for existing registration before registering
            if (!mbs.isRegistered(oName)) {
                mbs.registerMBean(new StandardMBean(this, ContextLoaderMXBean.class, true), oName);
            } else {
                log.debug("ContextLoader is already registered in JMX");
            }
        } catch (Exception e) {
            log.warn("Error on jmx registration", e);
        }
    }

    protected void unregisterJMX() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.unregisterMBean(oName);
        } catch (Exception e) {
            log.warn("Exception unregistering: {}", oName, e);
        }
        oName = null;
    }

    /**
     * @param applicationContext
     *            Spring application context
     * @throws BeansException
     *             Top level exception for app context (that is, in fact, beans factory)
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Setter for parent application context
     * 
     * @param parentContext
     *            Parent Spring application context
     */
    public void setParentContext(ApplicationContext parentContext) {
        this.parentContext = parentContext;
    }

    /**
     * Return parent context
     * 
     * @return parent application context
     */
    public ApplicationContext getParentContext() {
        return parentContext;
    }

    /**
     * Setter for context config name
     * 
     * @param contextsConfig
     *            Context config name
     */
    public void setContextsConfig(String contextsConfig) {
        this.contextsConfig = contextsConfig;
    }

    public String getContextsConfig() {
        return contextsConfig;
    }

}
