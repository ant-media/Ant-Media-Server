/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 *
 * Copyright (c) 2006-2011 by respective authors (see below). All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.red5.server.scheduling;

import java.lang.management.ManagementFactory;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.servlet.ServletContext;

import org.quartz.impl.StdSchedulerFactory;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.jmx.mxbeans.QuartzSchedulingServiceMXBean;
import org.slf4j.Logger;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * This class can be used to initialize Quartz for a Red5 application.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ManagedResource(objectName = "org.red5.server:type=ApplicationSchedulingService,name=default")
public class ApplicationSchedulingService extends QuartzSchedulingService {

    private static Logger log = Red5LoggerFactory.getLogger(ApplicationSchedulingService.class);

    public static final String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";

    private String applicationName;

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Sets the scheduler factory in the servlet context.
     * 
     * @param servletContext
     *            sevlet context
     */
    public void setServletAttribute(ServletContext servletContext) {
        log.debug("Storing the scheduler factory in the servlet context");
        servletContext.setAttribute(QUARTZ_FACTORY_KEY, factory);
    }

    /**
     * Getter for job name.
     *
     * @return Job name
     */
    @Override
    public String getJobName() {
        return String.format("%s_ScheduledJob_%d", applicationName, jobDetailCounter.getAndIncrement());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Application scheduler initializing...");
        try {
            //set properties
            if (configFile != null) {
                factory = new StdSchedulerFactory(configFile);
            } else {
                Properties props = new Properties();
                props.put("org.quartz.scheduler.instanceName", applicationName + "_Scheduler");
                props.put("org.quartz.scheduler.instanceId", "AUTO");
                props.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
                props.put("org.quartz.threadPool.threadCount", "2");
                props.put("org.quartz.threadPool.threadPriority", "5");
                props.put("org.quartz.jobStore.misfireThreshold", "60000");
                props.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
                factory = new StdSchedulerFactory(props);
            }
            super.afterPropertiesSet();
        } catch (Exception e) {
            log.error("Quartz Scheduler failed to initialize", e);
        }
    }

    protected void registerJMX() {
        //register with jmx server
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName oName = null;
            if (instanceId == null) {
                oName = new ObjectName("org.red5.server:type=ApplicationSchedulingService,applicationName=" + applicationName);
            } else {
                oName = new ObjectName("org.red5.server:type=ApplicationSchedulingService,applicationName=" + applicationName + ",instanceId=" + instanceId);
            }
            mbeanServer.registerMBean(new StandardMBean(this, QuartzSchedulingServiceMXBean.class, true), oName);
        } catch (Exception e) {
            log.warn("Error on jmx registration", e);
        }
    }

    protected void unregisterJMX() {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName oName = null;
            if (instanceId == null) {
                oName = new ObjectName("org.red5.server:type=ApplicationSchedulingService,applicationName=" + applicationName);
            } else {
                oName = new ObjectName("org.red5.server:type=ApplicationSchedulingService,applicationName=" + applicationName + ",instanceId=" + instanceId);
            }
            mbs.unregisterMBean(oName);
        } catch (Exception e) {
            log.warn("Exception unregistering", e);
        }
    }

}
