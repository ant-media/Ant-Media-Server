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

package org.red5.server.tomcat;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import jakarta.servlet.ServletException;

import org.red5.server.jmx.mxbeans.LoaderMXBean;
import org.red5.server.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * This service provides the means to auto-deploy a war.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class WarDeployer implements InitializingBean, DisposableBean {

    private Logger log = LoggerFactory.getLogger(WarDeployer.class);

    //that wars are currently being installed
    private static AtomicBoolean deploying = new AtomicBoolean(false);

    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private ScheduledFuture<DeployJob> future;

    /**
     * How often to check for new war files
     */
    private int checkInterval = 600000; //ten minutes

    /**
     * Deployment directory
     */
    private String webappFolder;

    /**
     * Expand WAR files in the webapps directory prior to start up
     */
    private boolean expandWars;

    {
        log.info("War deployer service created");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Starting WarDeployer");
        // create the job and schedule it
        future = (ScheduledFuture<DeployJob>) scheduler.scheduleAtFixedRate(new DeployJob(), 60000L, checkInterval, TimeUnit.MILLISECONDS);
        // check the deploy from directory
        log.debug("Webapps directory: {}", webappFolder);
        File dir = new File(webappFolder);
        if (!dir.exists()) {
            log.warn("Source directory not found");
        } else {
            if (!dir.isDirectory()) {
                throw new Exception("Webapps directory is not a directory");
            }
        }
        dir = null;
        // expand wars if so requested
        if (expandWars) {
            log.debug("Deploying wars");
            deploy(false);
        }
    }

    public void deploy(boolean startApplication) {
        log.info("Deploy wars {} app start", (startApplication ? "with" : "without"));
        if (deploying.compareAndSet(false, true)) {
            // short name
            String application = null;
            // file name
            String applicationWarName = null;
            // look for web application archives
            File dir = new File(webappFolder);
            // get a list of wars
            File[] files = dir.listFiles(new DirectoryFilter());
            for (File f : files) {
                // get the war name
                applicationWarName = f.getName();
                int dashIndex = applicationWarName.indexOf('-');
                if (dashIndex != -1) {
                    // strip everything except the applications name
                    application = applicationWarName.substring(0, dashIndex);
                } else {
                    // grab every char up to the last '.'
                    application = applicationWarName.substring(0, applicationWarName.lastIndexOf('.'));
                }
                log.debug("Application name: {}", application);
                // setup context
                String contextPath = '/' + application;
                String contextDir = webappFolder + contextPath;
                log.debug("Web context: {} context directory: {}", contextPath, contextDir);
                // verify this is a unique app
                File appDir = new File(dir, application);
                if (appDir.exists()) {
                    if (appDir.isDirectory()) {
                        log.debug("Application directory exists");
                    } else {
                        log.warn("Application destination is not a directory");
                    }
                    log.info("Application {} already installed, please un-install before attempting another install", application);
                } else {
                    log.debug("Unwaring and starting...");
                    // un-archive it to app dir
                    FileUtil.unzip(webappFolder + '/' + applicationWarName, contextDir);
                    // load and start the context
                    if (startApplication) {
                        // get the webapp loader from jmx
                        LoaderMXBean loader = getLoader();
                        if (loader != null) {
                            try {
                                loader.startWebApplication(application);
                            } catch (ServletException e) {
                                log.error("Unexpected error while staring web application", e);
                            }
                        }
                    }
                    // remove the war file
                    File warFile = new File(dir, applicationWarName);
                    if (warFile.delete()) {
                        log.debug("{} was deleted", warFile.getName());
                    } else {
                        log.debug("{} was not deleted", warFile.getName());
                        warFile.deleteOnExit();
                    }
                    warFile = null;
                }
                appDir = null;
            }
            dir = null;
            // reset sentinel
            deploying.set(false);
        }
    }

    @Override
    public void destroy() throws Exception {
        if (future != null) {
            future.cancel(true);
        }
        scheduler.shutdownNow();
    }

    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public String getWebappFolder() {
        return webappFolder;
    }

    public void setWebappFolder(String webappFolder) {
        this.webappFolder = webappFolder;
    }

    /**
     * Whether or not to expand war files prior to start up.
     * 
     * @param expandWars
     *            to expand or not
     */
    public void setExpandWars(boolean expandWars) {
        this.expandWars = expandWars;
    }

    /**
     * Returns the LoaderMBean.
     * 
     * @return LoadeerMBean
     */
    @SuppressWarnings("cast")
    public LoaderMXBean getLoader() {
        LoaderMXBean loader = null;
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName oName;
        try {
            // TODO support all loaders
            oName = new ObjectName("org.red5.server:type=TomcatLoader");
            if (mbs.isRegistered(oName)) {
                loader = JMX.newMXBeanProxy(mbs, oName, LoaderMXBean.class, true);
                log.debug("Loader was found");
            } else {
                log.warn("Loader not found");
            }
        } catch (Exception e) {
            log.error("Exception getting loader", e);
        }
        return loader;
    }

    /**
     * Filters directory content
     */
    protected class DirectoryFilter implements FilenameFilter {
        /**
         * Check whether file matches filter rules
         * 
         * @param dir
         *            Directory
         * @param name
         *            File name
         * @return true If file does match filter rules, false otherwise
         */
        public boolean accept(File dir, String name) {
            File f = new File(dir, name);
            log.trace("Filtering: {} name: {}", dir.getName(), name);
            // filter out all but war files
            boolean result = f.getName().endsWith("war");
            // nullify
            f = null;
            return result;
        }
    }

    private class DeployJob implements Runnable {

        public void run() {
            log.debug("Starting scheduled deployment of wars");
            deploy(true);
        }

    }
    
    public void undeploy(String name) {
    	LoaderMXBean loader = getLoader();
        if (loader != null) {
            loader.removeContext("/"+name);
        }
    }

}
