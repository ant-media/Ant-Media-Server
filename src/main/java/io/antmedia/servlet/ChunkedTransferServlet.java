package io.antmedia.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.servlet.cmafutils.AtomParser;
import io.antmedia.servlet.cmafutils.AtomParser.MockAtomParser;
import io.antmedia.servlet.cmafutils.ICMAFChunkListener;
import io.antmedia.servlet.cmafutils.IParser;


public class ChunkedTransferServlet extends HttpServlet {


	public static final String STREAMS = "/streams";
	public static final String WEBAPPS = "webapps";
	protected static Logger logger = LoggerFactory.getLogger(ChunkedTransferServlet.class);


	public static class ChunkListener implements ICMAFChunkListener {

		LinkedBlockingQueue<byte[]> chunksQueue = new LinkedBlockingQueue<>();

		@Override
		public void chunkCompleted(byte[] completeChunk) 
		{
			byte[] data;
			if (completeChunk != null) {
				data = new byte[completeChunk.length];
				
				System.arraycopy(completeChunk, 0, data, 0, data.length);
			}
			else {
				//EOF 
				data = new byte[0];
			}			
			chunksQueue.add(data);
		
		}

		public LinkedBlockingQueue<byte[]> getChunksQueue() {
			return chunksQueue;
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
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {		
		handleIncomingStream(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		handleIncomingStream(req, resp);
	}

	public void handleIncomingStream(HttpServletRequest req, HttpServletResponse resp) {
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		if (appContext != null && appContext.isRunning()) 
		{
			String applicationName = appContext.getApplicationName();

			String filepath = WEBAPPS + applicationName + STREAMS + req.getPathInfo();

			File finalFile = new File(filepath);

			
			
			String tmpFilepath = filepath + ".tmp"; 
			File tmpFile = new File(tmpFilepath);

			File streamsDir = new File(WEBAPPS + applicationName + STREAMS);
			File firstParent = finalFile.getParentFile();
			File grandParent = firstParent.getParentFile();

			if (firstParent.equals(streamsDir) || grandParent.equals(streamsDir)) 
			{
				mkdirIfRequired(req, applicationName);

				try {
					IChunkedCacheManager cacheManager = (IChunkedCacheManager) appContext.getBean(IChunkedCacheManager.BEAN_NAME);

					logger.debug("doPut key:{}", finalFile.getAbsolutePath());

					cacheManager.addCache(finalFile.getAbsolutePath());
					IParser atomparser;

					if (filepath.endsWith(".mpd") || filepath.endsWith(".m3u8")) 
					{
						//don't parse atom for mpd files because they are text files
						atomparser = new MockAtomParser();
					}
					else 
					{
						atomparser = new AtomParser(completeChunk -> 
							cacheManager.append(finalFile.getAbsolutePath(), completeChunk));
					}


					AsyncContext asyncContext = req.startAsync();
					StatusListener statusListener = new StatusListener(filepath);
					asyncContext.addListener(statusListener);


					InputStream inputStream = asyncContext.getRequest().getInputStream();
					asyncContext.start(() -> 
						readInputStream(finalFile, tmpFile, cacheManager, atomparser, asyncContext, inputStream, statusListener)
					);
				}
				catch (BeansException | IllegalStateException | IOException e) 
				{
					logger.error("Exception in handleIncomingStream for the chunk:{} ",finalFile.getAbsolutePath());
					logger.error(ExceptionUtils.getStackTrace(e));
					writeInternalError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
				} 

			}
			else {
				logger.warn("AppContext is not running for write request to {}", req.getRequestURI());
			}

		}
		else 
		{
			logger.warn("AppContext is not running for write request to {}", req.getRequestURI());
			writeInternalError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server is not ready. It's likely starting. Please try a few seconds later. ");

		}
	}

	private void mkdirIfRequired(HttpServletRequest req, String applicationName) 
	{
		int secondSlashIndex = req.getPathInfo().indexOf('/', 1);
		if (secondSlashIndex != -1) {
			String subDirName = req.getPathInfo().substring(0, secondSlashIndex);

			File subDir = new File( WEBAPPS + applicationName + STREAMS + subDirName);
			if (!subDir.exists()) {
				subDir.mkdir();
			}
		}
	}

	public void readInputStream(File finalFile, File tmpFile, IChunkedCacheManager cacheManager, IParser atomparser,
			AsyncContext asyncContext, InputStream inputStream, StatusListener statusListener) 
	{
		boolean exceptionOccured = false;
		try (FileOutputStream fos = new FileOutputStream(tmpFile)) 
		{
			byte[] data = new byte[2048];
			int length = 0;

			while ((length = inputStream.read(data, 0, data.length)) > 0) 
			{
				atomparser.parse(data, 0, length);
				fos.write(data, 0, length);
				
				if (statusListener.isTimeoutOrErrorExist()) {
					logger.warn("Timeout or error exists for file: {} breaking the loop", finalFile.getAbsolutePath());
					break;
				}
			}
			
			if (finalFile.getParentFile().exists()) {
				Files.move(tmpFile.toPath(), finalFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				logger.debug("File:{} was generated ", finalFile.getName());
			}
			else {
				logger.warn("Parent file of {} not exist", finalFile.getParent());
			}
		}
		catch (ClientAbortException e) {
			logger.warn("Client aborted - Reading input stream for file: {}", finalFile.getAbsolutePath());
			exceptionOccured = true;
		}
		catch (Exception e) 
		{
			logger.error(ExceptionUtils.getStackTrace(e));
			exceptionOccured = true;
		}
		
		if (!exceptionOccured) {
			asyncContext.complete();
		}
		
		cacheManager.removeCache(finalFile.getAbsolutePath());
		
		logger.debug("doPut done key:{}", finalFile.getAbsolutePath());
	}


	public void deleteRequest(HttpServletRequest req, HttpServletResponse resp) 
	{
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		if (appContext != null && appContext.isRunning()) 
		{
			String applicationName = appContext.getApplicationName();
			String filepath = WEBAPPS + applicationName + STREAMS + req.getPathInfo();

			File file = new File(filepath);


			File streamsDir = new File(WEBAPPS + applicationName + STREAMS);

			logger.debug("doDelete for file: {}", file.getAbsolutePath());
			try {
				if (file.exists()) 
				{
					//make sure streamsDir is parent of file
					File firstParent = file.getParentFile();
					File grandParent = firstParent.getParentFile();

					if (firstParent.equals(streamsDir) || grandParent.equals(streamsDir)) 
					{
						Files.deleteIfExists(file.toPath());
						deleteFreeDir(req, applicationName, streamsDir);

					}
					else 
					{
						logger.error("Parent or grant parent is not streams directory for DELETE operation {}", filepath);
						writeInternalError(resp, HttpServletResponse.SC_CONFLICT, null);
					}
				}
			}
			catch (Exception e) {
				writeInternalError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
			}
		}
		else {
			logger.error("Server is not ready for req: {}",req.getPathInfo());
			writeInternalError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		deleteRequest(req, resp);
	}



	private void deleteFreeDir(HttpServletRequest req, String applicationName, File streamsDir) throws IOException {
		//delete the subdirectory if there is no file inside
		int secondSlashIndex = req.getPathInfo().indexOf('/', 1);
		if (secondSlashIndex != -1) 
		{
			String subDirName = req.getPathInfo().substring(0, req.getPathInfo().indexOf('/', 1));
			File subDir = new File( WEBAPPS + applicationName + STREAMS + subDirName);
			if (subDir.exists() && subDir.isDirectory() && subDir.getParentFile().equals(streamsDir) && subDir.list().length == 0) 
			{
				Files.deleteIfExists(subDir.toPath());
			}
		}
	}


	public void writeOutputStream(File file, AsyncContext asyncContext, String mimeType) 
	{
		int total = 0;
		try (FileInputStream fis = new FileInputStream(file)) 
		{
			//it seems that headers should be set in the same thread.
			ServletResponse response = asyncContext.getResponse();
			response.setContentType(mimeType);
			
			OutputStream ostream = response.getOutputStream();
			int length = 0;
			byte[] data = new byte[2048];

			
			while ((length = fis.read(data, 0, data.length)) > 0) {
				ostream.write(data, 0, length);
				total += length;
			}
		
			ostream.flush(); 
			asyncContext.complete();

		} 
		catch (Exception e) 
		{
			logger.error("Exception in writing the following file:{} total written byte:{} stacktrace:{}", file.getName(), total, ExceptionUtils.getStackTrace(e));
		}
	}

	public static void logHeaders(HttpServletResponse resp) {
		Collection<String> headerNames = resp.getHeaderNames();
		for (String name : headerNames) {
			try {
				logger.info("Header name:{}", name);
				logger.info("Header value:{}", resp.getHeader(name));
			}
			catch (Exception te) {
				logger.error(ExceptionUtils.getStackTrace(te));
			}
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		handleGetRequest(req, resp);
	}


	public void handleGetRequest(HttpServletRequest req, HttpServletResponse resp) {
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (appContext != null && appContext.isRunning()) 
		{

			File file = new File(WEBAPPS + File.separator + req.getRequestURI());

			try 
			{    
				//set the mime type
				String mimeType = req.getServletContext().getMimeType(file.getName());
				
				if (Files.exists(file.toPath())) 
				{
					AsyncContext asyncContext = req.startAsync();
					asyncContext.start(() -> writeOutputStream(file, asyncContext, mimeType));
				}
				else 
				{
					IChunkedCacheManager cacheManager = (IChunkedCacheManager) appContext.getBean(IChunkedCacheManager.BEAN_NAME);

					boolean cacheAvailable = cacheManager.hasCache(file.getAbsolutePath());

					if (cacheAvailable) 
					{
						logger.info("File:{} is being generated on the fly so getting from cache", file.getAbsolutePath());
						AsyncContext asyncContext = req.startAsync();

						ChunkListener chunkListener = new ChunkListener();
						cacheManager.registerChunkListener(file.getAbsolutePath(), chunkListener);
						asyncContext.start(() ->  
							writeChunks(file, cacheManager, asyncContext, chunkListener, mimeType)
						);

					}
					else 
					{
						logger.info("Sending not found error(404) for {}", file.getAbsolutePath());
						writeInternalError(resp, HttpServletResponse.SC_NOT_FOUND, null);
					}

				}
			} 
			catch (BeansException | IllegalStateException e) 
			{
				logger.error(ExceptionUtils.getStackTrace(e));
				writeInternalError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
			}
		}
		else 
		{
			logger.warn("AppContext is not running for get request {}", req.getRequestURI());
			writeInternalError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server is not ready. It's likely starting. Please try a few seconds later. ");
		}
	}

	public void writeChunks(File file, IChunkedCacheManager cacheManager, AsyncContext asyncContext,
			ChunkListener chunkListener, String mimeType) 
	{
		String filePath = file.getAbsolutePath();
		boolean exceptionOccured = false;
		try {
			//it seems that headers should be set in the same thread.
			ServletResponse response = asyncContext.getResponse();
			response.setContentType(mimeType);
			
			ServletOutputStream oStream = response.getOutputStream();
			byte[] chunk;
			while ((chunk = chunkListener.getChunksQueue().take()).length > 0) {
				int offset = 0;
				int batchSize = 2048;
				int length = 0;
				logger.debug("start writing chunk leaving for file: {}", filePath);

				while ((length = chunk.length - offset) > 0) 
				{
					if (length > batchSize) {
						length = batchSize;
					}
					oStream.write(chunk, offset, length);
					offset += length;
				} 
				
				oStream.flush();
				
				logger.debug("writing chunk leaving for file: {}", filePath);

			}
			
		}
		catch (ClientAbortException e) {
			logger.warn("Client aborted - writing chunks for file: {}", filePath);
			exceptionOccured = true;
		}
		catch (InterruptedException e) {
			logger.error("InterruptedException - writing chunks for file: {} stacktrace:{}", filePath, ExceptionUtils.getStackTrace(e));
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			logger.error("Exception - writing chunks for file: {} stacktrace:{}", filePath, ExceptionUtils.getStackTrace(e));
			exceptionOccured = true;
		} 
		
		if (!exceptionOccured) {
			asyncContext.complete();
		}

		cacheManager.removeChunkListener(filePath, chunkListener);
		
	}

	private void writeInternalError(HttpServletResponse resp, int status, String message) {

		try {
			resp.setStatus(status);
			PrintWriter writer;
			writer = resp.getWriter();
			if (message != null) {
				writer.print(message);
			}
			writer.close();
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

	}








}
