package io.antmedia.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.bytedeco.ffmpeg.avutil.AVDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.amazonaws.event.ProgressEventType;

import io.antmedia.AppSettings;
import io.antmedia.muxer.Muxer;
import io.antmedia.storage.StorageClient;

import static io.antmedia.muxer.Muxer.DATE_TIME_PATTERN;

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
		super.doPut(req, resp);
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


	public void uploadHLSChunk(StorageClient storageClient, ConfigurableWebApplicationContext appContext, 
			InputStream inputStream, HttpServletRequest req, HttpServletResponse resp) 
	{

		AppSettings appSettings = (AppSettings) appContext.getBean(AppSettings.BEAN_NAME);

		String s3FileKey = getS3Key(req, appSettings);

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

		String mainTrackId = req.getHeader("mainTrackId");
		String streamId = req.getHeader("streamId");

		return Muxer.replaceDoubleSlashesWithSingleSlash(getExtendedS3StreamsFolderPath(mainTrackId, streamId, appSettings.getS3StreamsFolderPath()) + File.separator + req.getPathInfo());
	}

	public static String getExtendedS3StreamsFolderPath(String mainTrackId, String streamId, String s3StreamsFolder) {
		if (s3StreamsFolder == null) {
			return "";
		}

		String result = s3StreamsFolder;

		if (mainTrackId == null) {
			result = result.replace("%m/", "")
					.replace("/%m", "")
					.replace("%m", "");
		} else {
			result = result.replace("%m", mainTrackId);
		}

		if (streamId == null) {
			result = result.replace("%s/", "")
					.replace("/%s", "")
					.replace("%s", "");
		} else {
			result = result.replace("%s", streamId);
		}

		result = result.replaceAll("//+", "/");

		result = result.trim().replaceAll("^/+|/+$", "");

		return result;
	}

}
