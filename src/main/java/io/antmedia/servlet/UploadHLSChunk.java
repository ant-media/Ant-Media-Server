package io.antmedia.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.amazonaws.event.ProgressEventType;

import io.antmedia.AppSettings;
import io.antmedia.muxer.Muxer;
import io.antmedia.storage.StorageClient;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@MultipartConfig
public class UploadHLSChunk extends HttpServlet {

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

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPut(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		logger.debug("Received GET request for HLS chunk upload: {}", req.getPathInfo());
		StorageClient storageClient = getStorageClient(req);
		if (storageClient != null) 
		{
			ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

			AppSettings appSettings = (AppSettings) appContext.getBean(AppSettings.BEAN_NAME);

			String s3FileKey = getS3Key(req, appSettings);
			try (InputStream inputStream = storageClient.get(s3FileKey);
					ServletOutputStream outputStream = resp.getOutputStream();) {

				if (inputStream == null) {
					logger.error("File path is null for S3 key: {}", s3FileKey);
					resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File not found: " + s3FileKey);
					return;
				}

				String pathInfo = req.getPathInfo(); // e.g., "/files/myvideo.m3u8"
				String fileName = null;
				if (pathInfo != null) {
					String[] parts = pathInfo.split("/");
					fileName = parts[parts.length - 1];
				}

				resp.setContentType("application/vnd.apple.mpegurl");
				resp.setCharacterEncoding("UTF-8");
				resp.setHeader("Cache-Control", "no-cache"); // optional: or "max-age=10"
				resp.setHeader("Content-Disposition", "inline; filename=\""+ fileName +"\"");

				logger.debug("Local file path for S3 key {}: {}", s3FileKey, fileName);


				byte[] buffer = new byte[8192]; // 8KB buffer

				
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					outputStream.write(buffer, 0, bytesRead);
				}
				outputStream.flush();

			}
			catch (IOException e) {
				logger.error("Error reading file from S3 with key {}: {}", s3FileKey, ExceptionUtils.getStackTrace(e));
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading file: " + s3FileKey);
			}

		} 
		else 
		{
			logger.warn("Storage client is not available for request: {}", req.getRequestURI());
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Storage client is not available");
			return;
		}


	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) 
	{

		logger.debug("Received PUT request for HLS chunk upload: {}", req.getPathInfo());

		StorageClient storageClient = getStorageClient(req);


		if (storageClient != null) 
		{

			ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

			AppSettings appSettings = (AppSettings) appContext.getBean(AppSettings.BEAN_NAME);

			InputStream inputStream = null;

			try 
			{
				inputStream = req.getInputStream();

				uploadHLSChunk(storageClient, appSettings, inputStream, req, resp);
			} 
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}

		}
	}


	public void doPutForUnitTests(HttpServletRequest req, HttpServletResponse resp) {
		doPut(req, resp);
	}


	public void uploadHLSChunk(StorageClient storageClient, AppSettings appSettings, 
			InputStream inputStream, HttpServletRequest req, HttpServletResponse resp) 
	{

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
