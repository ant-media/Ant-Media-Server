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

package org.red5.server;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.red5.server.jmx.mxbeans.ShutdownMXBean;

/**
 * Provides a means to cleanly shutdown an instance from the command line.
 *
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class Shutdown {

	/**
	 * Connects to the given RMI port (default: 9999) and invokes shutdown on
	 * the loader.
	 *
	 * @param args The first parameter should be a port number
	 */
	@SuppressWarnings("cast")
	public static void main(String[] args) {
		try {
			String policyFile = System.getProperty("java.security.policy");
			if (policyFile == null) {
				System.setProperty("java.security.debug", "failure");
				System.setProperty("java.security.policy", "conf/red5.policy");
			}
			/*
			try {
			    // Enable the security manager
			    SecurityManager sm = new SecurityManager();
			    System.setSecurityManager(sm);
			} catch (SecurityException se) {
				System.err.println("Security manager already set");
			}
			*/
			// check for the host name as a system prop
			String rmiAdapterHost = System.getProperty("java.rmi.server.hostname");
			if (rmiAdapterHost == null) {
				rmiAdapterHost = "localhost";
			}
			JMXServiceURL url = null;
			JMXConnector jmxc = null;
			HashMap<String, Object> env = null;
			if (null == args || args.length < 1) {
				System.out.printf("Attempting to connect to RMI %s:9999\n", rmiAdapterHost);
				url = new JMXServiceURL("service:jmx:rmi://" + rmiAdapterHost + ":9999/jndi/rmi://" + rmiAdapterHost + ":9999/red5");
			} else {
				System.out.printf("Attempting to connect to RMI %s:%s\n", rmiAdapterHost, args[0]);
				url = new JMXServiceURL("service:jmx:rmi://" + rmiAdapterHost + ":" + args[0] + "/jndi/rmi://" + rmiAdapterHost + ":" + args[0] + "/red5");
				if (args.length > 1) {
					env = new HashMap<String, Object>(1);
					String[] credentials = new String[] { args[1], args[2] };
					env.put("jmx.remote.credentials", credentials);
				}
			}
			jmxc = JMXConnectorFactory.connect(url, env);
			MBeanServerConnection mbs = jmxc.getMBeanServerConnection();
			// class supporting shutdown
			ShutdownMXBean proxy = null;
			// check for loader registration
			ObjectName tomcatObjectName = new ObjectName("org.red5.server:type=TomcatLoader");
			ObjectName jettyObjectName = new ObjectName("org.red5.server:type=JettyLoader");
			ObjectName winstoneObjectName = new ObjectName("org.red5.server:type=WinstoneLoader");
			ObjectName contextLoaderObjectName = new ObjectName("org.red5.server:type=ContextLoader");
			if (mbs.isRegistered(jettyObjectName)) {
				System.out.println("Red5 Jetty loader was found");
				proxy = JMX.newMXBeanProxy(mbs, jettyObjectName, ShutdownMXBean.class, true);
			} else if (mbs.isRegistered(tomcatObjectName)) {
				System.out.println("Red5 Tomcat loader was found");
				proxy = JMX.newMXBeanProxy(mbs, tomcatObjectName, ShutdownMXBean.class, true);
			} else if (mbs.isRegistered(winstoneObjectName)) {
				System.out.println("Red5 Winstone loader was found");
				proxy = JMX.newMXBeanProxy(mbs, winstoneObjectName, ShutdownMXBean.class, true);
			} else if (mbs.isRegistered(contextLoaderObjectName)) {
				System.out.println("Red5 Context loader was found");
				proxy = JMX.newMXBeanProxy(mbs, contextLoaderObjectName, ShutdownMXBean.class, true);
			} else {
				System.out.println("Red5 Loader was not found, is the server running?");
			}
			if (proxy != null) {
				System.out.println("Calling shutdown");
				proxy.destroy();
			}
			jmxc.close();
		} catch (UndeclaredThrowableException e) {
			//ignore
		} catch (NullPointerException e) {
			//ignore
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
