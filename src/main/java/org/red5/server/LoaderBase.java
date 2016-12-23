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
import java.util.HashMap;
import java.util.Map;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IApplicationContext;
import org.red5.server.api.IApplicationLoader;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Base class for all JEE application loaders.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public abstract class LoaderBase implements ApplicationContextAware {

    private static Logger log = Red5LoggerFactory.getLogger(LoaderBase.class);

    /**
     * We store the application context so we can access it later.
     */
    protected static ApplicationContext applicationContext;

    /**
     * Current Red5 application context, set by the different loaders.
     */
    public static final Map<String, IApplicationContext> red5AppCtx = new HashMap<>();

    /**
     * Loader for new applications.
     */
    protected static ThreadLocal<IApplicationLoader> loader = new ThreadLocal<>();

    /**
     * Folder containing the webapps.
     */
    protected String webappFolder = null;

    /**
     * Getter for the application loader.
     * 
     * @return Application loader
     */
    public static IApplicationLoader getApplicationLoader() {
        log.debug("Get application loader");
        return loader.get();
    }

    /**
     * Setter for the application loader.
     * 
     * @param loader
     *            Application loader
     */
    public static void setApplicationLoader(IApplicationLoader loader) {
        log.debug("Set application loader: {}", loader);
        LoaderBase.loader.set(loader);
    }

    /**
     * Returns the map containing all of the registered Red5 application contexts.
     * 
     * @return a map
     */
    public static Map<String, IApplicationContext> getRed5ApplicationContexts() {
        log.debug("Get all red5 application contexts");
        return red5AppCtx;
    }

    /**
     * Getter for a Red5 application context.
     * 
     * @param path
     *            path
     * 
     * @return Red5 application context
     */
    public static IApplicationContext getRed5ApplicationContext(String path) {
        log.debug("Get red5 application context - path: {}", path);
        //log.trace("Map at get: {}", red5AppCtx);
        return red5AppCtx.get(path);
    }

    /**
     * Setter for a Red5 application context.
     * 
     * @param path
     *            path
     * 
     * @param context
     *            Red5 application context
     */
    public static void setRed5ApplicationContext(String path, IApplicationContext context) {
        log.debug("Set red5 application context - path: {} context: {}", path, context);
        //log.trace("Map at set: {}", red5AppCtx);
        if (context != null) {
            red5AppCtx.put(path, context);
        } else {
            red5AppCtx.remove(path);
        }
        //log.trace("Map after set: {}", red5AppCtx);
    }

    /**
     * Remover for a Red5 application context.
     * 
     * @param path
     *            path
     * 
     * @return Red5 application context
     */
    public static IApplicationContext removeRed5ApplicationContext(String path) {
        log.debug("Remove red5 application context - path: {}", path);
        return red5AppCtx.remove(path);
    }

    /**
     * Getter for application context
     * 
     * @return Application context
     */
    public static ApplicationContext getApplicationContext() {
        log.debug("Get application context: {}", applicationContext);
        return applicationContext;
    }

    /**
     * Setter for application context.
     * 
     * @param context
     *            Application context
     * @throws BeansException
     *             Abstract superclass for all exceptions thrown in the beans package and subpackages
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        log.debug("Set application context: {}", context);
        applicationContext = context;
    }

    /**
     * Set the folder containing webapps.
     * 
     * @param webappFolder
     *            web app folder
     */
    public void setWebappFolder(String webappFolder) {
        File fp = new File(webappFolder);
        if (!fp.canRead()) {
            throw new RuntimeException(String.format("Webapp folder %s cannot be accessed.", webappFolder));
        }
        if (!fp.isDirectory()) {
            throw new RuntimeException(String.format("Webapp folder %s doesn't exist.", webappFolder));
        }
        fp = null;
        this.webappFolder = webappFolder;
    }

    /**
     * Remove context from the current host.
     * 
     * @param path
     *            Path
     */
    public void removeContext(String path) {
        throw new UnsupportedOperationException();
    }

}
