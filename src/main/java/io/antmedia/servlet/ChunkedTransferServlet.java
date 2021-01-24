package io.antmedia.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.connector.ClientAbortException;
import org.apache.catalina.connector.CoyoteOutputStream;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.servlet.cmafutils.AtomParser;
import io.antmedia.servlet.cmafutils.ICMAFChunkListener;
import io.antmedia.servlet.cmafutils.AtomParser.MockAtomParser;
import io.antmedia.servlet.cmafutils.IParser;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;


public class ChunkedTransferServlet extends HttpServlet {


	protected static Logger logger = LoggerFactory.getLogger(ChunkedTransferServlet.class);

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{		

		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		if (appContext != null && appContext.isRunning()) 
		{
			String applicationName = appContext.getApplicationName();

			String filepath = "webapps" + applicationName + "/streams" + req.getPathInfo();

			File finalFile = new File(filepath);

			String tmpFilepath = filepath + ".tmp"; 
			File tmpFile = new File(tmpFilepath);

			IChunkedCacheManager cacheManager = (IChunkedCacheManager) appContext.getBean(IChunkedCacheManager.BEAN_NAME);

			logger.trace("doPut key:{}", finalFile.getAbsolutePath());

			cacheManager.addCache(finalFile.getAbsolutePath());



			IParser atomparser;

			if (filepath.endsWith(".mpd")) 
			{
				//don't parse atom for mpd files because they are text files

				atomparser = new MockAtomParser();
			}
			else {
				atomparser = new AtomParser(completeChunk -> 
				cacheManager.append(finalFile.getAbsolutePath(), completeChunk)
						);
			}

			AsyncContext asyncContext = req.startAsync();

			asyncContext.start(() -> {
				try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
					byte[] data = new byte[2048];
					int length = 0;
					
					ServletInputStream inputStream  = asyncContext.getRequest().getInputStream();
					while ((length = inputStream.read(data, 0, data.length)) > 0) 
					{
						atomparser.parse(data, 0, length);
						fos.write(data, 0, length);
					}

					Files.move(tmpFile.toPath(), finalFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
					asyncContext.complete();
				}
				catch (IOException e) 
				{
					logger.error(ExceptionUtils.getStackTrace(e));
				}
				finally {
					cacheManager.removeCache(finalFile.getAbsolutePath());
				}
				logger.trace("doPut done key:{}", finalFile.getAbsolutePath());

			});

		}
		else 
		{
			logger.info("AppContext is not running for write request to {}", req.getRequestURI());
			writeInternalError(req, resp);
		}

	}

	@Override
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

		if (appContext != null && appContext.isRunning()) 
		{
			String applicationName = appContext.getApplicationName();
			String filepath = "webapps" + applicationName + "/streams" + req.getPathInfo();

			File file = new File(filepath);


			File streamsDir = new File("webapps" + applicationName + "/streams");

			if (file.exists()) {

				//make sure streamsDir is parent of file
				File firstParent = file.getParentFile();
				File grandParent = firstParent.getParentFile();

				if (firstParent.equals(streamsDir) || grandParent.equals(streamsDir)) {
					Files.delete(file.toPath());
				}
				else {
					logger.error("Parent or grant parent is not streams directory for DELETE operation {}", filepath);
					resp.sendError(HttpServletResponse.SC_CONFLICT);
				}
			}
		}
	}


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPut(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (appContext != null && appContext.isRunning()) 
		{
			String applicationName = appContext.getApplicationName();

			File file = new File("webapps/" + req.getRequestURI());

			if (file.exists()) 
			{
				logger.info("File exists: {}", file.getAbsolutePath());

				AsyncContext asyncContext = req.startAsync();

				asyncContext.start(() -> 
				{
					try (FileInputStream fis = new FileInputStream(file)) {

						int length = 0;
						byte[] data = new byte[2048];
						ServletOutputStream ostream = asyncContext.getResponse().getOutputStream();

						while ((length = fis.read(data, 0, data.length)) > 0) {
							ostream.write(data, 0, length);
							ostream.flush();
						}

						asyncContext.complete();

					} 
					catch (IOException e) 
					{
						logger.error(ExceptionUtils.getStackTrace(e));
					}
				});

			}
			else 
			{
				IChunkedCacheManager cacheManager = (IChunkedCacheManager) appContext.getBean(IChunkedCacheManager.BEAN_NAME);

				boolean cacheAvailable = cacheManager.hasCache(file.getAbsolutePath());

				if (cacheAvailable ) {

					AsyncContext asyncContext = req.startAsync();
					
					
					cacheManager.registerChunkListener(file.getAbsolutePath(), new ICMAFChunkListener() {

						@Override
						public void chunkCompleted(byte[] completeChunk) 
						{			
							
								if (completeChunk != null) 
								{
									try {
										ServletOutputStream oStream = asyncContext.getResponse().getOutputStream();
										oStream.write(completeChunk);
										oStream.flush();
									}
									catch (ClientAbortException e) {
										logger.warn("Client aborted - Removing chunklistener this client for file: {}", file.getAbsolutePath());
										cacheManager.removeChunkListener(file.getAbsolutePath(), this);									
									}
									catch (Exception e) {
										logger.error(ExceptionUtils.getStackTrace(e));
										
									}
								}
								else 
								{
									logger.info("context is completed for {}", file.getAbsolutePath());
									//if it's null, it means related cache is finished
									
									asyncContext.complete();
								}
							

						}
					});

				}
				else {
					logger.debug("Sending not found error(404) for {}", file.getAbsolutePath());
					resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				}	

			}
		}
		else 
		{
			logger.info("AppContext is not running for get request {}", req.getRequestURI());
			writeInternalError(req, resp);
		}
	}

	private void writeInternalError(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		PrintWriter writer = resp.getWriter();
		writer.print("Server is not ready. It's likely starting. Please try a few seconds later. ");
		writer.close();
	}








}
