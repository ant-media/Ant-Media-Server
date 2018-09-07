package io.antmedia;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardHost;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import io.antmedia.cluster.IClusterNotifier;

public class ClusterProvider{
	static IClusterNotifier cluster;
	
	public static IClusterNotifier getCluster(ApplicationContext context) {
		if(cluster == null) {
			/*
			try {
				Class tcpClusterClass = Class.forName("io.antmedia.enterprise.cluster.TcpCluster");
				Method getInstanceMethod = tcpClusterClass.getMethod("getInstance");
				cluster = (IClusterNotifier) getInstanceMethod.invoke(null);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			
			*/
			System.out.println("\n\n\n "+context.getDisplayName()+"bean:"+context.containsBean("tomcat.cluster"));
			cluster = (IClusterNotifier) context.getBean("tomcat.cluster");
		}
		
		return cluster;
	}

}
