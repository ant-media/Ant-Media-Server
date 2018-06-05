package io.antmedia;

import org.apache.catalina.Valve;
import org.apache.catalina.core.StandardHost;
import org.red5.server.tomcat.TomcatLoader;

import io.antmedia.rest.BroadcastRestService;

public class ValveInjector {
	private TomcatLoader loader;
	public void init() {
		if(BroadcastRestService.isEnterprise())
		{
			try {
				Class valveClass;
				valveClass = Class.forName("io.antmedia.enterprise.cluster.HttpLiveStreamValve");
				((StandardHost) getLoader().getHost()).addValve((Valve) valveClass.newInstance());
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
	}
	public TomcatLoader getLoader() {
		return loader;
	}
	public void setLoader(TomcatLoader loader) {
		this.loader = loader;
	}
}
