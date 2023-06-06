package io.antmedia.servlet;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.storage.StorageClient;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.server.api.scope.IScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Nullable;
import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import java.io.*;

@MultipartConfig
public class UploadHLSChunk extends HttpServlet {

	public static final String STREAMS = "/streams";
	public static final String WEBAPPS = "webapps";
	protected static Logger logger = LoggerFactory.getLogger(UploadHLSChunk.class);
	protected static IScope scope;
	protected static ApplicationContext appCtx;
	@Context
	protected static ServletContext servletContext;
	protected static AntMediaApplicationAdapter appInstance;

	private static StorageClient storageClient = null;

	@Nullable
	public ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
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

	public void initStorageClient() {
		AntMediaApplicationAdapter app = getApplication();
		if (scope == null && app != null) {
			scope = app.getScope();
		}
		if (scope != null && scope.getContext().getApplicationContext().containsBean(StorageClient.BEAN_NAME)) {
			storageClient = (StorageClient) scope.getContext().getApplicationContext().getBean(StorageClient.BEAN_NAME);
		}
	}
	
	public static class StatusListener implements AsyncListener {
		
		String filepath;
		
		boolean timeoutOrErrorExist = false;
		
		public StatusListener (String filepath) {
			this.filepath = filepath;
		}

		@Override
		public void onTimeout(AsyncEvent event) throws IOException {
			logger.warn("handle incoming stream context Timeout: {}", filepath);
			timeoutOrErrorExist = true;
			
		}

		@Override
		public void onStartAsync(AsyncEvent event) throws IOException {
			logger.debug("handle incoming stream context onStartAsync: {}", filepath);
		}

		@Override
		public void onError(AsyncEvent event) throws IOException {
			logger.warn("handle incoming stream context onError: {}", filepath);
			timeoutOrErrorExist = true;
		}

		@Override
		public void onComplete(AsyncEvent event) throws IOException {
			logger.debug("handle incoming stream context onComplete: {}", filepath);
		}
		
		public boolean isTimeoutOrErrorExist() {
			return timeoutOrErrorExist;
		}
	}


	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
		servletContext = req.getServletContext();
		if (storageClient != null && storageClient.isEnabled()) {
			uploadHLSChunk(req, resp);
		}
		else {
			logger.error("Storage client is null, try to init it");
			initStorageClient();
		}
	}

	public void uploadHLSChunk(HttpServletRequest req, HttpServletResponse resp) {
		try {
			ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

			if (appContext != null && appContext.isRunning())
			{
				String applicationName = appContext.getApplicationName();

				String filepath = WEBAPPS + applicationName + STREAMS + req.getPathInfo();

				InputStream fileContent = req.getInputStream();
				File targetFile = new File(filepath + ".tmp");

				FileUtils.copyInputStreamToFile(fileContent, targetFile);

				if (targetFile.exists()) {
					storageClient.save(filepath, targetFile, false);
				} else {
					logger.error("File does not exist: {}", filepath);
				}
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

	}

	public StorageClient getStorageClient() {
		return storageClient;
	}

	public void setStorageClient(StorageClient storageClient) {
		this.storageClient = storageClient;
	}

	public void setAppInstance(AntMediaApplicationAdapter appInstance) {
		this.appInstance = appInstance;
	}

	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

}
