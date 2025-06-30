package io.antmedia.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.antmedia.muxer.HLSMuxer;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.websocket.WebSocketConstants;
import io.vertx.core.Vertx;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.amazonaws.event.ProgressEventType;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.muxer.Muxer;
import io.antmedia.storage.StorageClient;

@MultipartConfig
public class UploadHLSChunk extends HttpServlet{

	private static final long serialVersionUID = 1L;

	protected static Logger logger = LoggerFactory.getLogger(UploadHLSChunk.class);

	public StorageClient getStorageClient(HttpServletRequest req) 
	{
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		if (appContext != null && appContext.isRunning()) 
		{
			//we assume that StorageClient bean exists in all apps even if it's not enabled
			StorageClient storageClient = (StorageClient)appContext.getBean(StorageClient.BEAN_NAME);
			if (storageClient.isEnabled()) {
				return storageClient;
			}
			else {
				logger.warn("Storage client is not enabled for request:{}", req.getRequestURI());
			}
		}
		else {
			logger.warn("AppContext is not running for write request to {}", req.getRequestURI());
		}

		return null;

	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		StorageClient storageClient = getStorageClient(req);
		if (storageClient != null) 
		{
			try {
				ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

				AppSettings appSettings = (AppSettings) appContext.getBean(AppSettings.BEAN_NAME);
				storageClient.delete(getS3Key(req, appSettings));

			}
			catch (IllegalStateException | BeansException e) {
				logger.error(ExceptionUtils.getStackTrace(e));

			}
		}
	}
	
	public void doDeleteForUnitTests(HttpServletRequest req, HttpServletResponse resp) {
		try {
			doDelete(req, resp);
		} catch (ServletException | IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		} 
	}
	public static JsonObject getJsonFromPostRequest(HttpServletRequest request) throws IOException {
		BufferedReader reader = request.getReader();
		StringBuilder sb = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		String jsonString = sb.toString();
        return new Gson().fromJson(jsonString, JsonObject.class);
	}

	public boolean handlePostRequest(StorageClient storageClient, ConfigurableWebApplicationContext ctx, HttpServletRequest request) throws IOException {

		boolean isHandled = false;
		JsonObject message = getJsonFromPostRequest(request);
		String streamId = message.get("streamId").getAsString();
		String command = message.get("command").getAsString();

		if(command.equals(WebSocketConstants.PUBLISH_FINISHED)){
			isHandled = true;
			
			AppSettings appSettings = (AppSettings) ctx.getBean(AppSettings.BEAN_NAME);

			
			logger.info("stream finished : {}",streamId);
			
			if(appSettings.isDeleteHLSFilesOnEnded()){
				
				Vertx vertx = (Vertx) ctx.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);

				String filePath = message.get("filePath").getAsString();
				vertx.setTimer(Integer.parseInt(appSettings.getHlsTime()) * Integer.parseInt(appSettings.getHlsListSize()) * 1000l, l ->{
					storageClient.deleteMultipleFiles(filePath,HLSMuxer.HLS_FILES_REGEX_MATCHER);
					logger.info("deleting files from S3 for streamId: {}",streamId);
					}
				);
			}
		}
		
		return isHandled;
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
		StorageClient storageClient = getStorageClient(req);

		if (storageClient != null)
		{
			try {
				ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
				boolean isHandled = handlePostRequest(storageClient,appContext,req);
				
				if (!isHandled) {
					resp.setStatus(HttpServletResponse.SC_OK);
				} 
				else {
					// If the request is not handled, we assume it's an upload request
					doPut(req, resp);
				}
				
			}
			catch (IllegalStateException | IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}

		}
		
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) 
	{

		StorageClient storageClient = getStorageClient(req);

		if (storageClient != null) 
		{
			try {
				ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

				uploadHLSChunk(storageClient, appContext, req.getInputStream(), req, resp);
			} 
			catch (IllegalStateException | IOException e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}

		}

	}

	public void doPutForUnitTests(HttpServletRequest req, HttpServletResponse resp) {
		doPut(req, resp);
	}
	public void doPostForUnitTests(HttpServletRequest req, HttpServletResponse resp) {
		doPost(req, resp);
	}

	public void uploadHLSChunk(StorageClient storageClient, ConfigurableWebApplicationContext appContext, 
			InputStream inputStream, HttpServletRequest req, HttpServletResponse resp) 
	{

		AppSettings appSettings = (AppSettings) appContext.getBean(AppSettings.BEAN_NAME);

		String s3FileKey = getS3Key(req, appSettings);

		//TODO: we overwrite progressListener for the ongoing upload here. This may make logs misleading.
		storageClient.setProgressListener(event -> {
			if (event.getEventType() == ProgressEventType.TRANSFER_FAILED_EVENT)
			{
				logger.error("S3 - Error: Upload failed for {} with key {}", req.getPathInfo(), s3FileKey);
			}
			else if (event.getEventType() == ProgressEventType.TRANSFER_COMPLETED_EVENT)
			{	
				logger.debug("File uploaded to S3 with key: {}", s3FileKey);
			}
		});

		storageClient.save(s3FileKey, inputStream, true);

	}

	public static String getS3Key(HttpServletRequest req, AppSettings appSettings) {
		//No need have File.separator between the below strings because req.getPathInfo() starts with "/"
		//req.getPathInfo(); includes only the part after /hls-upload/. In other words, just {SUB_FOLDER} + (M3U8 or TS files)
		return Muxer.replaceDoubleSlashesWithSingleSlash(appSettings.getS3StreamsFolderPath() + File.separator + req.getPathInfo());
	}

}
