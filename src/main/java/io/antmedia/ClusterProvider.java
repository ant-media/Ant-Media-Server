package io.antmedia;

import org.springframework.context.ApplicationContext;

import io.antmedia.cluster.IClusterNotifier;

public class ClusterProvider{
	static IClusterNotifier cluster;
	
	private ClusterProvider() {
	}
	
	public static IClusterNotifier getCluster(ApplicationContext context) {
		if(cluster == null) {
			cluster = (IClusterNotifier) context.getBean("tomcat.cluster");
		}
		
		return cluster;
	}

}
