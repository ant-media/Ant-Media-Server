package io.antmedia.rest;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.social.endpoint.VideoServiceEndpoint;

public abstract class RestServiceBase {
	protected static Logger logger = LoggerFactory.getLogger(RestServiceBase.class);


	@Context
	private ServletContext servletContext;
	private DataStoreFactory dataStoreFactory;
	private IDataStore dbStore;
	private ApplicationContext appCtx;
	private IScope scope;
	private AntMediaApplicationAdapter appInstance;

	protected boolean addSocialEndpoints(Broadcast broadcast, String socialEndpointIds) {	
		boolean success = true;
		Map<String, VideoServiceEndpoint> endPointServiceList = getApplication().getVideoServiceEndpoints();

		String[] endpointIds = socialEndpointIds.split(",");

		if (endPointServiceList != null) {
			for (String endpointId : endpointIds) {
				VideoServiceEndpoint videoServiceEndpoint = endPointServiceList.get(endpointId);
				if (videoServiceEndpoint != null) {
					success = success && addSocialEndpoint(broadcast, videoServiceEndpoint);
				}
				else {
					success = false;
					String warning = endpointId + " endpoint does not exist in this app.";
					logger.warn(warning);
				}
			}
		}
		else {
			success = false;
			logger.warn("endPointServiceList is null");
		}
		return success;
	}

	protected boolean addSocialEndpoint(Broadcast broadcast, VideoServiceEndpoint socialEndpoint) {
		Endpoint endpoint;
		try {
			endpoint = socialEndpoint.createBroadcast(broadcast.getName(),
					broadcast.getDescription(), broadcast.getStreamId(), broadcast.isIs360(), broadcast.isPublicStream(),
					720, true);
			return getDataStore().addEndpoint(broadcast.getStreamId(), endpoint);

		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return false;
	}
	
	public void setAppCtx(ApplicationContext appCtx) {
		this.appCtx = appCtx;
	}

	@Nullable
	protected ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}
	
	/**
	 * this is for testing
	 * @param app
	 */
	public void setApplication(AntMediaApplicationAdapter app) {
		this.appInstance = app;
	}

	public AntMediaApplicationAdapter getApplication() {
		if (appInstance == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				appInstance = (AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			}
		}
		return appInstance;
	}


	public IScope getScope() {
		if (scope == null) {
			scope = getApplication().getScope();
		}
		return scope;
	}

	public void setScope(IScope scope) {
		this.scope = scope;
	}

	public IDataStore getDataStore() {
		if (dbStore == null) {
			dbStore = getDataStoreFactory().getDataStore();
		}
		return dbStore;
	}

	public void setDataStore(IDataStore dataStore) {
		this.dbStore = dataStore;
	}

	public DataStoreFactory getDataStoreFactory() {
		if(dataStoreFactory == null) {
			WebApplicationContext ctxt = WebApplicationContextUtils.getWebApplicationContext(servletContext); 
			dataStoreFactory = (DataStoreFactory) ctxt.getBean("dataStoreFactory");
		}
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	protected Map<String, VideoServiceEndpoint> getEndpointList() {
		return getApplication().getVideoServiceEndpoints();
	}

}
