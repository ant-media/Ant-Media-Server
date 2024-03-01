package io.antmedia.test.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.servlet.UploadHLSChunk;
import io.antmedia.storage.StorageClient;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UploadHLSChunkTest {

	@Mock
	private HttpServletRequest mockRequest;
	@Mock
	private HttpServletResponse mockResponse;
	@Mock
	private ServletContext mockServletContext;

	@Mock
	private ConfigurableWebApplicationContext mockAppContext;
	@Mock
	private StorageClient mockStorageClient;

	private UploadHLSChunk servlet;

	@Before
	public void setUp() throws ServletException {
		MockitoAnnotations.initMocks(this);
		servlet = Mockito.spy(new UploadHLSChunk());
		servlet.init();
	}

	@After
	public void after() {

	}

	@Test
	public void testDoPut() {
		when(mockRequest.getServletContext()).thenReturn(mockServletContext);
		when(mockStorageClient.isEnabled()).thenReturn(true);
		when(mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(mockAppContext);
		when(mockAppContext.isRunning()).thenReturn(true);
		when(mockRequest.getServletContext()).thenReturn(mockServletContext);

		// test with storage client
		when(mockAppContext.getBean(StorageClient.BEAN_NAME)).thenReturn(mockStorageClient);
		AppSettings appSettings = new AppSettings();
		appSettings.setS3CacheControl(null);
		assertNull(appSettings.getS3CacheControl());
		when(mockAppContext.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);

		//test with null storage client
		servlet.doPutForUnitTests(mockRequest, mockResponse);
		verify(mockStorageClient, times(1)).isEnabled();
		verify(mockRequest, times(2)).getServletContext();
		verify(mockResponse, never()).setStatus(anyInt());
		verify(servlet, times(1)).uploadHLSChunk(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());




		try {
			when(mockRequest.getInputStream()).thenReturn(Mockito.mock(ServletInputStream.class));


			servlet.doPutForUnitTests(mockRequest, mockResponse);

			verify(mockStorageClient, times(2)).isEnabled();
			verify(mockRequest, times(4)).getServletContext();
			verify(mockResponse, never()).setStatus(anyInt());
			verify(servlet, times(2)).uploadHLSChunk(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());


			when(mockStorageClient.isEnabled()).thenReturn(false);
			servlet.doPutForUnitTests(mockRequest, mockResponse);
			verify(mockStorageClient, times(3)).isEnabled();
			verify(mockRequest, times(5)).getServletContext();

			//uploadHLSChunk is not called again(still times(2)) because storageClient is not enabled
			verify(servlet, times(2)).uploadHLSChunk(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDoDelete() {
		when(mockRequest.getServletContext()).thenReturn(mockServletContext);
		when(mockStorageClient.isEnabled()).thenReturn(false);
		when(mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(mockAppContext);
		when(mockAppContext.isRunning()).thenReturn(true);
		when(mockRequest.getServletContext()).thenReturn(mockServletContext);

		// test with storage client
		when(mockAppContext.getBean(StorageClient.BEAN_NAME)).thenReturn(mockStorageClient);
		when(mockAppContext.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());


		servlet.doDeleteForUnitTests(mockRequest, mockResponse);
		Mockito.verify(mockStorageClient, never()).delete(Mockito.any());
		when(mockStorageClient.isEnabled()).thenReturn(true);

		servlet.doDeleteForUnitTests(mockRequest, mockResponse);
		Mockito.verify(mockStorageClient).delete(Mockito.any());


	}



	@Test
	public void testGetStorageClient() {

		// Call the method to be tested
		UploadHLSChunk uploadHLSChunk = new UploadHLSChunk();
		when(mockRequest.getServletContext()).thenReturn(mockServletContext);
		when(mockStorageClient.isEnabled()).thenReturn(false);
		when(mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(mockAppContext);
		when(mockAppContext.getBean(StorageClient.BEAN_NAME)).thenReturn(mockStorageClient);

		// Verify that the StorageClient object was set correctly
		assertNull(uploadHLSChunk.getStorageClient(mockRequest));

		// Create a mock AntMediaApplicationAdapter object
		when(mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(mockAppContext);
		when(mockAppContext.isRunning()).thenReturn(false);
		assertNull(uploadHLSChunk.getStorageClient(mockRequest));

		when(mockAppContext.isRunning()).thenReturn(true);
		assertNull(uploadHLSChunk.getStorageClient(mockRequest));

		when(mockAppContext.isRunning()).thenReturn(true);
		assertNull(uploadHLSChunk.getStorageClient(mockRequest));

		when(mockStorageClient.isEnabled()).thenReturn(true);
		assertNotNull(uploadHLSChunk.getStorageClient(mockRequest));


	}
	
	@Test
	public void testGetS3Key() {
		
		String pathInfo = "test.m3u8";

		when(mockRequest.getPathInfo()).thenReturn(pathInfo);
		AppSettings appSettings = new AppSettings();

		String s3Key = UploadHLSChunk.getS3Key(mockRequest, appSettings);
		assertEquals("streams/test.m3u8", s3Key);
		
		pathInfo = "/test.m3u8";
		s3Key = UploadHLSChunk.getS3Key(mockRequest, appSettings);
		assertEquals("streams/test.m3u8", s3Key);
		
		
		pathInfo = "/test.m3u8";
		appSettings.setS3StreamsFolderPath("streams/");
		s3Key = UploadHLSChunk.getS3Key(mockRequest, appSettings);
		assertEquals("streams/test.m3u8", s3Key);

	}


}
