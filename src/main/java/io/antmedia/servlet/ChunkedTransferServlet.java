package io.antmedia.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.servlets.DefaultServlet;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.google.common.io.Files;


public class ChunkedTransferServlet extends DefaultServlet {
	
	
	protected static Logger logger = LoggerFactory.getLogger(ChunkedTransferServlet.class);
	
	
	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{		
		ConfigurableWebApplicationContext appContext = (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		
		if (appContext != null && appContext.isRunning()) 
		{
			String applicationName = appContext.getApplicationName();
			
			String filepath = "webapps" + applicationName + "/streams" + req.getPathInfo();
			
			String tmpFilepath = filepath + ".tmp"; 
			
			File tmpFile = new File(tmpFilepath);
			
			IChunkedCacheManager cacheManager = (IChunkedCacheManager) appContext.getBean(IChunkedCacheManager.BEAN_NAME);
			
			String cacheKey = "webapps" + applicationName + "/streams/" + req.getPathInfo();
			
			cacheManager.addCache(cacheKey);
			
			FileOutputStream fos = new FileOutputStream(tmpFile);
			ServletInputStream inputStream = req.getInputStream();
			
			byte[] data = new byte[2048];
			int length = 0;
			while ((length = inputStream.read(data, 0, data.length)) > 0) 
			{
				logger.info("read length:{} path info:{}", length, req.getPathInfo());
				cacheManager.append(cacheKey, data);
				//writing to file system. If it takes longer to write to disk, latency/freeze may happen
				fos.write(data, 0, length);
			}
			fos.close();
			
			Files.move(tmpFile, new File(filepath));
			
			cacheManager.removeCache(cacheKey);
		}
		else 
		{
			logger.info("AppContext is not running for write request to {}", req.getRequestURI());
			writeInternalError(req, resp);
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
			
			String cacheKey = "streams" + req.getRequestURI();
			
			File file = new File("webapps/" + req.getRequestURI());
			
			if (file.exists()) {
				super.doGet(req, resp);
			}
			else {
				IChunkedCacheManager cacheManager = (IChunkedCacheManager) appContext.getBean(IChunkedCacheManager.BEAN_NAME);
				
				
				int size = 0; 
				int index = 0;
				
				while ((size = cacheManager.size(cacheKey)) > 0) 
				{
					if (size > index) 
					{
						byte[] cache = cacheManager.getCache(cacheKey, index);
						resp.getOutputStream().write(cache);
						resp.getOutputStream().flush();
						index++;
					}
					else 
					{	
						try 
						{
							Thread.sleep(15);
						} 
						catch (InterruptedException e) 
						{
							logger.error(ExceptionUtils.getStackTrace(e));
						}
					}
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
