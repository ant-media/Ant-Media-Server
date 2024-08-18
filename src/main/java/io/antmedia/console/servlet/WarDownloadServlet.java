package io.antmedia.console.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.google.api.client.http.HttpRequest;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.AdminApplication;
import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Context;

/**
 * The servlet to download the war file of the application
 * 
 * When an app is created, the war file is created in the tmp directory
 * This servlet just checks if the war file exists on the tmp directory and if it exists, it sends the war file to the client(another node in the cluster)
 * 
 * It's used in app distribution in the cluster
 */
public class WarDownloadServlet extends HttpServlet{


	private static final long serialVersionUID = -5206403831475536456L;

	private static final Logger logger = LoggerFactory.getLogger(WarDownloadServlet.class);

	public WebApplicationContext getContext(HttpServletRequest req) {
		return (ConfigurableWebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

	}

	public AdminApplication getApplication(HttpServletRequest req) {
		WebApplicationContext ctxt = getContext(req);
		if (ctxt != null) {
			return (AdminApplication)ctxt.getBean("web.handler");
		}
		return null;
	}

	public AntMediaApplicationAdapter getAppAdaptor(String appName, HttpServletRequest req) {

		AntMediaApplicationAdapter appAdaptor = null;
		AdminApplication application = getApplication(req);
		if (application != null) 
		{
			ApplicationContext context = application.getApplicationContext(appName);
			if (context != null) 
			{
				appAdaptor = (AntMediaApplicationAdapter) context.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			}
		}
		return appAdaptor;
	}

	/**
	 * TCPCluster makes the head requests  if the file is in this instance {@link TCPCluster#checkIfResourceExist}
	 */
	@Override
	public void doHead(HttpServletRequest request, HttpServletResponse response)  throws ServletException, IOException  {

		String requestURI = request.getRequestURI().replaceAll(TokenFilterManager.REPLACE_CHARS_REGEX,"_") ; // The path including the context path
		String appName = requestURI.substring(requestURI.lastIndexOf("/")+1, requestURI.lastIndexOf("."));

		File warFileInTmpDirectory = AdminApplication.getWarFileInTmpDirectory (AdminApplication.getWarName(appName));
		if (warFileInTmpDirectory == null) {
			logger.info("No such war file in the tmp directory for app: {} ", appName);
			sendError(response, HttpServletResponse.SC_NOT_FOUND, "No such war file in the tmp directory");
			return;
		}

		AntMediaApplicationAdapter appAdaptor = getAppAdaptor(appName, request);
		if (appAdaptor == null) {
			logger.info("No such application: {} installed on this instance while trying to download ", appName, requestURI);
			sendError(response, HttpServletResponse.SC_BAD_REQUEST, "No such application");
			return;
		}

		response.setContentLength((int)warFileInTmpDirectory.length());
		response.setStatus(HttpServletResponse.SC_OK);


	}
	
	
	public void sendError(HttpServletResponse response, int status, String message) {
		try {
			response.sendError(status, message);
		} catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}

	}

	/**
	 * AdminApplication makes the requests in {@link AdminApplication#downloadWarFile}
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			 {

		String requestURI = request.getRequestURI().replaceAll(TokenFilterManager.REPLACE_CHARS_REGEX,"_") ; // The path including the context path

		String token = request.getHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION);

		if (StringUtils.isEmpty(token)) {
			logger .info("Token parameter is missing while accessing to the resource {}", requestURI);
			sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Token parameter is missing");
			return;
		}

		//get the application name from the requestURI, the file has .war extension, remove the war extension
		String appName = requestURI.substring(requestURI.lastIndexOf("/")+1, requestURI.lastIndexOf("."));

		//check if the appAdaptor is there
		AntMediaApplicationAdapter appAdaptor = getAppAdaptor(appName, request);
		if (appAdaptor == null) {
			logger.info("No such application: {} installed on this instance while trying to download ", appName, requestURI);
			sendError(response, HttpServletResponse.SC_BAD_REQUEST, "No such application");
			return;
		}

		//check if the token is correct
		String jwtSecretKey = appAdaptor.getAppSettings().getClusterCommunicationKey();

		boolean jwtTokenValid = JWTFilter.isJWTTokenValid(jwtSecretKey, token);
		if (!jwtTokenValid) {
			logger.info("Token is not valid while trying to download {}", requestURI);
			sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token is not valid");
			return;
		}

		//get the war file from the tmp directory


		File warFileInTmpDirectory = AdminApplication.getWarFileInTmpDirectory (AdminApplication.getWarName(appName));
		if (warFileInTmpDirectory == null) {
			logger.info("No such war file in the tmp directory for app: {} ", appName);
			sendError(response, HttpServletResponse.SC_BAD_REQUEST, "No such war file in the tmp directory");
			return;
		}

		//set the content type
		response.setContentType("application/octet-stream");
		response.setContentLength((int)warFileInTmpDirectory.length());

		//set the header
		response.setHeader("Content-Disposition", "attachment; filename=" + warFileInTmpDirectory.getName());
		response.setStatus(HttpServletResponse.SC_OK);


		try (FileInputStream fileInputStream = new FileInputStream(warFileInTmpDirectory)) {
			byte[] buffer = new byte[1024];
			int bytesRead = -1;
			while ((bytesRead = fileInputStream.read(buffer)) != -1) {
				
				response.getOutputStream().write(buffer, 0, bytesRead);
			}

			response.getOutputStream().flush();
		}
		catch (IOException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error while reading the file");
		}

	}

}
