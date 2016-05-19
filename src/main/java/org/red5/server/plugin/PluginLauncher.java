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

package org.red5.server.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.red5.server.Server;
import org.red5.server.api.plugin.IRed5Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Creates the plug-in environment and cleans up on shutdown.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class PluginLauncher implements ApplicationContextAware, InitializingBean {

    protected static Logger log = LoggerFactory.getLogger(PluginLauncher.class);

    /**
     * Spring application context
     */
    private ApplicationContext applicationContext;

    public void afterPropertiesSet() throws Exception {
        // get common context
        ApplicationContext common = (ApplicationContext) applicationContext.getBean("red5.common");
        Server server = (Server) common.getBean("red5.server");
        //server should be up and running at this point so load any plug-ins now

        // get the plugins dir
        File pluginsDir = new File(System.getProperty("red5.root"), "plugins");
        // get installed plugins (the jars in the plugins directory)
        File[] plugins = pluginsDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                // lower the case
                String tmp = name.toLowerCase();
                // accept jars and zips
                return tmp.endsWith(".jar") || tmp.endsWith(".zip");
            }
        });
        if (plugins != null) {
            IRed5Plugin red5Plugin = null;
            log.debug("{} plugins to launch", plugins.length);
            for (File plugin : plugins) {
                JarFile jar = null;
                Manifest manifest = null;
                try {
                    jar = new JarFile(plugin, false);
                    manifest = jar.getManifest();
                } catch (Exception e1) {
                    log.warn("Error loading plugin manifest: {}", plugin);
                } finally {
                    if (jar != null) {
                        jar.close();
                    }
                }
                if (manifest == null) {
                    continue;
                }
                Attributes attributes = manifest.getMainAttributes();
                if (attributes == null) {
                    continue;
                }
                String pluginMainClass = attributes.getValue("Red5-Plugin-Main-Class");
                if (pluginMainClass == null || pluginMainClass.length() <= 0) {
                    continue;
                }
                // attempt to load the class; since it's in the plugins directory this should work
                ClassLoader loader = common.getClassLoader();
                Class<?> pluginClass;
                String pluginMainMethod = null;
                try {
                    pluginClass = Class.forName(pluginMainClass, true, loader);
                } catch (ClassNotFoundException e) {
                    continue;
                }
                try {
                    // handle plug-ins without "main" methods
                    pluginMainMethod = attributes.getValue("Red5-Plugin-Main-Method");
                    if (pluginMainMethod == null || pluginMainMethod.length() <= 0) {
                        // just get an instance of the class
                        red5Plugin = (IRed5Plugin) pluginClass.newInstance();
                    } else {
                        Method method = pluginClass.getMethod(pluginMainMethod, (Class<?>[]) null);
                        Object o = method.invoke(null, (Object[]) null);
                        if (o != null && o instanceof IRed5Plugin) {
                            red5Plugin = (IRed5Plugin) o;
                        }
                    }
                    // register and start
                    if (red5Plugin != null) {
                        // set top-level context
                        red5Plugin.setApplicationContext(applicationContext);
                        // set server reference
                        red5Plugin.setServer(server);
                        // register the plug-in to make it available for lookups
                        PluginRegistry.register(red5Plugin);
                        // start the plugin
                        red5Plugin.doStart();
                    }
                    log.info("Loaded plugin: {}", pluginMainClass);
                } catch (Throwable t) {
                    log.warn("Error loading plugin: {}; Method: {}", pluginMainClass, pluginMainMethod);
                    log.error("", t);
                }
            }
        } else {
            log.info("Plugins directory cannot be accessed or doesnt exist");
        }

    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        log.trace("Setting application context");
        this.applicationContext = applicationContext;
    }

}
