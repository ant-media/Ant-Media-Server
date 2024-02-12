package io.antmedia.filter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Queue;


import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.security.ITokenService;
import org.apache.catalina.util.NetMask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.settings.ServerSettings;
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public abstract class AbstractFilter implements Filter{
	
	public static final String BROADCAST_OBJECT = "broadcast";

	protected static Logger logger = LoggerFactory.getLogger(AbstractFilter.class);
	protected FilterConfig config;
	
	IStreamStats streamStats;
	private ITokenService tokenService;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.config = filterConfig;
	}

	public AppSettings getAppSettings() 
	{
		AppSettings appSettings = null;
		ConfigurableWebApplicationContext context = getAppContext();
		if (context != null) {
			appSettings = (AppSettings)context.getBean(AppSettings.BEAN_NAME);
		}
		return appSettings;
	}

	public ServerSettings getServerSettings()
	{
		ServerSettings serverSettings = null;
		ConfigurableWebApplicationContext context = getAppContext();
		if (context != null) {
			serverSettings = (ServerSettings)context.getBean(ServerSettings.BEAN_NAME);
		}
		return serverSettings;
	}

	public boolean checkCIDRList(Queue<NetMask> allowedCIDRList, final String remoteIPAdrress) {
		try {
			InetAddress addr = InetAddress.getByName(remoteIPAdrress);
			for (final NetMask nm : allowedCIDRList) {
				if (nm.matches(addr)) {
					return true;
				}
			}
		} catch (UnknownHostException e) {
			// This should be in the 'could never happen' category but handle it
			// to be safe.
			logger.error("error", e);
		}
		return false;
	}


	public ConfigurableWebApplicationContext getAppContext() 
	{
		ConfigurableWebApplicationContext appContext = getWebApplicationContext();
		if (appContext != null && appContext.isRunning()) 
		{
			Object dataStoreFactory = appContext.getBean(IDataStoreFactory.BEAN_NAME);
			
			if (dataStoreFactory instanceof IDataStoreFactory) 
			{
				DataStore dataStore = ((IDataStoreFactory)dataStoreFactory).getDataStore();
				if (dataStore.isAvailable()) 
				{
					return appContext;
				}
				else {
					logger.warn("DataStore is not available. It may be closed or not initialized");
				}
			}
			else {
				//return app context if it's not app's IDataStoreFactory
				return appContext;
			}
		}
		else 
		{
			if (appContext == null) {
				logger.warn("App context not initialized ");
			}
			else {
				logger.warn("App context not running yet." );
			}
		}

		return null;
	}
	
	public ConfigurableWebApplicationContext getWebApplicationContext() 
	{
		return (ConfigurableWebApplicationContext) getConfig().getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	}

	public FilterConfig getConfig() {
		return config;
	}

	public void setConfig(FilterConfig config) {
		this.config = config;
	}

	@Override
	public void destroy() {
		//nothing to destroy
	}
	
	public IStreamStats getStreamStats(String type) {
		if (streamStats == null) {
			ApplicationContext context = getAppContext();
			if (context != null) 
			{
				if(type.equals(HlsViewerStats.BEAN_NAME)) {
					streamStats = (IStreamStats)context.getBean(HlsViewerStats.BEAN_NAME);
				}
				else {
					streamStats = (IStreamStats)context.getBean(DashViewerStats.BEAN_NAME);
				}
			}
		}
		return streamStats;
	}
	
	public Broadcast getBroadcast(HttpServletRequest request, String streamId) {
		Broadcast broadcast = (Broadcast) request.getAttribute(BROADCAST_OBJECT);
		if (broadcast == null) 
		{
			
			DataStore dataStore = getDataStore();
			if (dataStore != null) 
			{
				broadcast = dataStore.get(streamId);
				if (broadcast != null) {
					request.setAttribute(BROADCAST_OBJECT, broadcast);
				}
			}	
		}
		return broadcast;
	}

	public AntMediaApplicationAdapter getAntMediaApplicationAdapter(){
		AntMediaApplicationAdapter antMediaApplicationAdapter = null;
		ApplicationContext context = getAppContext();
		if (context != null)
		{
			antMediaApplicationAdapter= (AntMediaApplicationAdapter)context.getBean(AntMediaApplicationAdapter.BEAN_NAME);
		}
		return antMediaApplicationAdapter;
	}

	public DataStore getDataStore(){
		ConfigurableWebApplicationContext appContext = getWebApplicationContext();
		if (appContext != null && appContext.isRunning())
		{
			Object dataStoreFactory = appContext.getBean(IDataStoreFactory.BEAN_NAME);

			if (dataStoreFactory instanceof IDataStoreFactory)
			{
				DataStore dataStore = ((IDataStoreFactory)dataStoreFactory).getDataStore();
				if (dataStore.isAvailable())
				{
					return dataStore;
				}
				else {
					logger.info("DataStore is not available. It may be closed or not initialized");
				}
			}
			else {
				//return app context if it's not app's IDataStoreFactory
				return null;
			}
		}
		else
		{
			if (appContext == null) {
				logger.warn("App context not initialized ");
			}
			else {
				logger.warn("App context not running yet." );
			}
		}

		return null;

	}

	public ITokenService getTokenService() {
		if (tokenService == null) {
			ApplicationContext context = getAppContext();
			if (context != null) {
				tokenService = (ITokenService)context.getBean(ITokenService.BeanName.TOKEN_SERVICE.toString());
			}
		}
		return tokenService;
	}

	public void setTokenService(ITokenService tokenService) {
		this.tokenService = tokenService;
	}
	
}
