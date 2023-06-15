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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MultipartConfig
public class UploadHLSChunk extends HttpServlet {

	public static final String STREAMS = "/streams";
	public static final String WEBAPPS = "webapps";
	public static final String PARSE_TIMESTAMP_M3U8 = "-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.\\d{3}\\.m3u8";
	public static final String PARSE_TIMESTAMP_ADAPTIVE_M3U8 = "-\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.\\d{3}\\_adaptive.m3u8";
	public static final String BLANK_STRING = "";
	public static final String HLS_MASTER_FILE_EXTENSION = ".m3u8";
	public static final String HLS_END_LIST_TAG = "#EXT-X-ENDLIST";
	protected static Logger logger = LoggerFactory.getLogger(UploadHLSChunk.class);
	protected static IScope scope;
	protected static ApplicationContext appCtx;
	@Context
	protected static ServletContext servletContext;
	protected static AntMediaApplicationAdapter appInstance;

	private static StorageClient storageClient = null;

	public UploadHLSChunk() {
		super();
	}

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

	public void doPutForUnitTests(HttpServletRequest req, HttpServletResponse resp) {
		doPut(req, resp);
	}

	public void uploadHLSChunk(HttpServletRequest req, HttpServletResponse resp) {
		try {
			ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

			if (appContext != null && appContext.isRunning())
			{
				String applicationName = appContext.getApplicationName();
				InputStream fileContent = req.getInputStream();

				String filepath = WEBAPPS + applicationName + STREAMS + req.getPathInfo();

				File targetFile = new File(filepath + ".tmp");

				FileUtils.copyInputStreamToFile(fileContent, targetFile); // TODO: FileUtils is not stable, replace it!

				String fileName = req.getPathInfo();

				// if stream is not finished, we remove ts file from disk
				// if stream is finished, we remove m3u8 file from disk
				boolean deleteLocalFileAfterItsUploaded = !(fileName.endsWith(HLS_MASTER_FILE_EXTENSION) && !checkIfStreamIsFinished(filepath + ".tmp"));

				// if stream is not finished, we remove timestamp from m3u8 file name
				// because some customers try to play the stream from the S3 bucket, and
				// they shouldn't deal with timestamp in the file name
				if (fileName.endsWith(HLS_MASTER_FILE_EXTENSION) && !checkIfStreamIsFinished(filepath + ".tmp")) {
					Pattern regexPattern = Pattern.compile(PARSE_TIMESTAMP_M3U8+"|"+PARSE_TIMESTAMP_ADAPTIVE_M3U8);
					Matcher matcher = regexPattern.matcher(fileName);
					fileName = matcher.replaceAll(BLANK_STRING);
					// add .m3u8 extension if it is not there
					if(!fileName.endsWith(HLS_MASTER_FILE_EXTENSION)) {
						fileName += HLS_MASTER_FILE_EXTENSION;
					}
				}

				String s3FileKey = WEBAPPS + applicationName + STREAMS + fileName;

				if (targetFile.exists()) {
					storageClient.save(s3FileKey, targetFile, deleteLocalFileAfterItsUploaded);
				} else {
					logger.error("File does not exist: {}", filepath);
				}
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

	}

	public boolean checkIfStreamIsFinished(String filePath) {
		boolean isFinished = false;
		try {
			File file = new File(filePath);
			if (file.exists()) {
				String content = FileUtils.readFileToString(file, "UTF-8");
				if (content.contains(HLS_END_LIST_TAG)) {
					isFinished = true;
				}
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
		return isFinished;
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
