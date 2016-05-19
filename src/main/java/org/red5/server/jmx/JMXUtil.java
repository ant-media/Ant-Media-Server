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

package org.red5.server.jmx;

import java.lang.management.ManagementFactory;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper methods for working with ObjectName or MBean instances.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JMXUtil {

    private static Logger log = LoggerFactory.getLogger(JMXUtil.class);

    public static void printMBeanInfo(ObjectName objectName, String className) {
        log.info("Retrieve the management information for the {}", className);
        log.info("MBean using the getMBeanInfo() method of the MBeanServer");
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        MBeanInfo info = null;
        try {
            info = mbs.getMBeanInfo(objectName);
        } catch (Exception e) {
            log.error("Could not get MBeanInfo object for {}", className, e);
            return;
        }
        log.info("CLASSNAME: \t" + info.getClassName());
        log.info("DESCRIPTION: \t" + info.getDescription());
        log.info("ATTRIBUTES");
        MBeanAttributeInfo[] attrInfo = info.getAttributes();
        if (attrInfo.length > 0) {
            for (int i = 0; i < attrInfo.length; i++) {
                log.info(" ** NAME: \t" + attrInfo[i].getName());
                log.info("    DESCR: \t" + attrInfo[i].getDescription());
                log.info("    TYPE: \t" + attrInfo[i].getType() + "\tREAD: " + attrInfo[i].isReadable() + "\tWRITE: " + attrInfo[i].isWritable());
            }
        } else
            log.info(" ** No attributes **");
        log.info("CONSTRUCTORS");
        MBeanConstructorInfo[] constrInfo = info.getConstructors();
        for (int i = 0; i < constrInfo.length; i++) {
            log.info(" ** NAME: \t" + constrInfo[i].getName());
            log.info("    DESCR: \t" + constrInfo[i].getDescription());
            log.info("    PARAM: \t" + constrInfo[i].getSignature().length + " parameter(s)");
        }
        log.info("OPERATIONS");
        MBeanOperationInfo[] opInfo = info.getOperations();
        if (opInfo.length > 0) {
            for (int i = 0; i < opInfo.length; i++) {
                log.info(" ** NAME: \t" + opInfo[i].getName());
                log.info("    DESCR: \t" + opInfo[i].getDescription());
                log.info("    PARAM: \t" + opInfo[i].getSignature().length + " parameter(s)");
            }
        } else
            log.info(" ** No operations ** ");
        log.info("NOTIFICATIONS");
        MBeanNotificationInfo[] notifInfo = info.getNotifications();
        if (notifInfo.length > 0) {
            for (int i = 0; i < notifInfo.length; i++) {
                log.info(" ** NAME: \t" + notifInfo[i].getName());
                log.info("    DESCR: \t" + notifInfo[i].getDescription());
                String notifTypes[] = notifInfo[i].getNotifTypes();
                for (int j = 0; j < notifTypes.length; j++) {
                    log.info("    TYPE: \t" + notifTypes[j]);
                }
            }
        } else {
            log.info(" ** No notifications **");
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean registerNewMBean(Class clazz, Class interfaceClass) {
        boolean status = false;
        try {
            String cName = clazz.getName();
            if (cName.indexOf('.') != -1) {
                cName = cName.substring(cName.lastIndexOf('.')).replaceFirst("[\\.]", "");
            }
            log.debug("Register name: {}", cName);
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(new StandardMBean(Class.forName(clazz.getName()).newInstance(), interfaceClass), new ObjectName("org.red5.server:type=" + cName));
            status = true;
        } catch (Exception e) {
            log.error("Could not register the {} MBean", clazz.getName(), e);
        }
        return status;
    }

}