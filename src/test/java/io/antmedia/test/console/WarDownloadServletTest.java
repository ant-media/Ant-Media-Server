package io.antmedia.test.console;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.servlet.WarDownloadServlet;
import io.antmedia.filter.JWTFilter;
import io.antmedia.filter.TokenFilterManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

public class WarDownloadServletTest {
	
	@Test
	public void testDoHead() throws ServletException, IOException {
		
		WarDownloadServlet warDownloadServlet = new WarDownloadServlet();
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = Mockito.spy(new MockHttpServletResponse());
		
		request.setRequestURI("/test123.war");
		warDownloadServlet.doHead(request, response);
		verify(response).sendError(HttpServletResponse.SC_NOT_FOUND, "No such war file in the tmp directory");
		
		
		
		File f = new File(AdminApplication.getJavaTmpDirectory(), "test.war");
		assertTrue(f.createNewFile());
		f.deleteOnExit();
		
		response = Mockito.spy(new MockHttpServletResponse());
		warDownloadServlet.doHead(request, response);
		
		verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "No such application");
		
		
		warDownloadServlet = Mockito.spy(new WarDownloadServlet());
		Mockito.doReturn(mock(AntMediaApplicationAdapter.class)).when(warDownloadServlet).getAppAdaptor("test", request);
		response = Mockito.spy(new MockHttpServletResponse());
		verify(response, never()).sendError(anyInt(), anyString());
		
		warDownloadServlet.doHead(request, response);

		
		verify(response).setStatus(HttpServletResponse.SC_OK);		
		
	}
	
	@Test
	public void testSendError() throws IOException {
		WarDownloadServlet warDownloadServlet = spy(new WarDownloadServlet());

		MockHttpServletResponse response = mock(MockHttpServletResponse.class);
		
		//throw exception when response.sendError is called
		Mockito.doThrow(new IOException()).when(response).sendError(anyInt(), anyString());
		warDownloadServlet.sendError(response, HttpServletResponse.SC_BAD_REQUEST, "Token parameter is missing");
		
		//no exception when response.sendError is called
	}
	
	@Test
	public void testGetAppAdaptor() {
		
		WarDownloadServlet warDownloadServlet = spy(new WarDownloadServlet());
		MockHttpServletRequest request = spy(new MockHttpServletRequest());
		assertNull(warDownloadServlet.getAppAdaptor("test", request));
		
		//return context when called  req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		ServletContext servletContext = mock(ServletContext.class);
		when(request.getServletContext()).thenReturn(servletContext);
		
		WebApplicationContext webApplicationContext = mock(WebApplicationContext.class);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(webApplicationContext);
		//still null because there is no bean named "web.handler"
		assertNull(warDownloadServlet.getAppAdaptor("test", request));
		
		
		AdminApplication adminApplication = mock(AdminApplication.class);
		when(webApplicationContext.getBean("web.handler")).thenReturn(adminApplication);
		//still null because there is no such application
		assertNull(warDownloadServlet.getAppAdaptor("test", request));
		
		//return appAdaptor when called context.getBean(AntMediaApplicationAdapter.BEAN_NAME);
		AntMediaApplicationAdapter appAdaptor = mock(AntMediaApplicationAdapter.class);
		
		WebApplicationContext webApplicationContext2 = mock(WebApplicationContext.class);
		when(webApplicationContext2.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdaptor);
		
		when(adminApplication.getApplicationContext("test")).thenReturn(webApplicationContext2);
		when(webApplicationContext2.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdaptor);
		assertTrue(appAdaptor.equals(warDownloadServlet.getAppAdaptor("test", request)));
		
	}
	
	@Test
	public void testDoGet() throws IOException {
		WarDownloadServlet warDownloadServlet = spy(new WarDownloadServlet());
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = spy(new MockHttpServletResponse());
		
		request.setRequestURI("/test123.war");
		warDownloadServlet.doGet(request, response);
		verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "Token parameter is missing");
		
		request.addHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION, "token");
		response = spy(new MockHttpServletResponse());
		warDownloadServlet.doGet(request, response);
		verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "No such application");
		
		
		AntMediaApplicationAdapter appAdaptor = mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(appAdaptor).when(warDownloadServlet).getAppAdaptor("test123", request);
		AppSettings appSettings = new AppSettings();
		when(appAdaptor.getAppSettings()).thenReturn(appSettings);
		response = spy(new MockHttpServletResponse());
		warDownloadServlet.doGet(request, response);
		verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is not valid");
		
		
		String jwtToken = JWTFilter.generateJwtToken(appSettings.getClusterCommunicationKey(), System.currentTimeMillis() + 60000, "appName", "test123");
		request.removeHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION);
		request.addHeader(TokenFilterManager.TOKEN_HEADER_FOR_NODE_COMMUNICATION, jwtToken);
		response = spy(new MockHttpServletResponse());
		warDownloadServlet.doGet(request, response);
		verify(response).sendError(HttpServletResponse.SC_BAD_REQUEST, "No such war file in the tmp directory");
		
		
		File f = new File(AdminApplication.getJavaTmpDirectory(), "test2.war");
		Mockito.doReturn(appAdaptor).when(warDownloadServlet).getAppAdaptor("test2", request);
		request.setRequestURI("/test2.war");
		FileOutputStream fos = new FileOutputStream(f);
		
		fos.write("test".getBytes());
		
		fos.close();
		
		
		
		f.deleteOnExit();
		response = spy(new MockHttpServletResponse());
		warDownloadServlet.doGet(request, response);
		verify(response).setStatus(HttpServletResponse.SC_OK);
		
	}

}
