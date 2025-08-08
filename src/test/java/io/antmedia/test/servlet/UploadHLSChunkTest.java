package io.antmedia.test.servlet;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import com.google.gson.JsonObject;
import io.antmedia.websocket.WebSocketConstants;
import io.vertx.core.Vertx;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.servlet.UploadHLSChunk;
import io.antmedia.storage.StorageClient;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
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
	
	private static Vertx vertx = Vertx.vertx();

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
	public void testDoGet() throws ServletException, IOException {
		when(mockRequest.getServletContext()).thenReturn(mockServletContext);
		when(mockStorageClient.isEnabled()).thenReturn(false);
		when(mockServletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(mockAppContext);
		when(mockAppContext.isRunning()).thenReturn(true);
		when(mockRequest.getServletContext()).thenReturn(mockServletContext);

		// test with storage client
		when(mockAppContext.getBean(StorageClient.BEAN_NAME)).thenReturn(mockStorageClient);

		AppSettings appSettings = new AppSettings();
		appSettings.setS3CacheControl(null);
		assertNull(appSettings.getS3CacheControl());
		when(mockAppContext.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
		
		servlet.doGetForUnitTests(mockRequest, mockResponse);
		
		
		when(mockStorageClient.isEnabled()).thenReturn(true);
		servlet.doGetForUnitTests(mockRequest, mockResponse);
		verify(mockResponse, times(1)).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());

		
		when(mockRequest.getPathInfo()).thenReturn("test.m3u8");
		servlet.doGetForUnitTests(mockRequest, mockResponse);
		verify(mockResponse, times(2)).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
		
		
		when(mockStorageClient.get(anyString())).thenReturn(new ByteArrayInputStream(new String("test content").getBytes()));
		
		ServletOutputStream outputStream = Mockito.mock(ServletOutputStream.class);
		when(mockResponse.getOutputStream()).thenReturn(outputStream);
		servlet.doGetForUnitTests(mockRequest, mockResponse);
		verify(mockResponse, times(2)).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
		verify(outputStream).flush();
		
		
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
	@Test
	public void testDeleteS3afterUpload() throws InterruptedException, IOException {

		StorageClient client = Mockito.mock(StorageClient.class);
		UploadHLSChunk uploadHlsChunk = spy(UploadHLSChunk.class);

		JsonObject message = new JsonObject();
		message.addProperty("streamId","test");
		message.addProperty("filePath","test");
		message.addProperty("command", WebSocketConstants.PUBLISH_FINISHED);

		AppSettings appSettings = new AppSettings();
		appSettings.setHlsTime("1");
		appSettings.setHlsListSize("1");
		appSettings.setDeleteHLSFilesOnEnded(false);

		ConfigurableWebApplicationContext ctx = mock(ConfigurableWebApplicationContext.class);

		doReturn(appSettings).when(ctx).getBean(AppSettings.BEAN_NAME);
		doReturn(vertx).when(ctx).getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME);


		try (MockedStatic<UploadHLSChunk> mockedStatic = mockStatic(UploadHLSChunk.class)) {
			mockedStatic.when(() -> UploadHLSChunk.getJsonFromPostRequest(any()))
					.thenReturn(message);

			uploadHlsChunk.handlePostRequest(client, ctx, mock(HttpServletRequest.class));
			Thread.sleep(3000);
			verify(client, times(0)).deleteMultipleFiles(anyString(), anyString());

			appSettings.setDeleteHLSFilesOnEnded(true);

			uploadHlsChunk.handlePostRequest(client, ctx, mock(HttpServletRequest.class));
			verify(client, timeout(5000).times(1)).deleteMultipleFiles(anyString(), anyString());
		}
	}

	@Test
	public void testGetJsonFromPostRequest() throws IOException {

		UploadHLSChunk uploadHlsChunk = spy(UploadHLSChunk.class);
		HttpServletRequest request = mock(HttpServletRequest.class);

		JsonObject object = new JsonObject();
		object.addProperty("test","test");
		object.addProperty("abc","abc");

		BufferedReader bufferedReader = new BufferedReader(new StringReader(object.toString()));
		doReturn(bufferedReader).when(request).getReader();

		JsonObject object1 = uploadHlsChunk.getJsonFromPostRequest(request);

        assertEquals(object1, object);
	}
	@Test
	public void testDoPost() throws ServletException, IOException {
		UploadHLSChunk uploadHlsChunk = spy(UploadHLSChunk.class);
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		StorageClient storageClient = mock(StorageClient.class);

		ServletContext servletContext = mock(ServletContext.class);
		ConfigurableWebApplicationContext appContext = mock(ConfigurableWebApplicationContext.class);

		doReturn(servletContext).when(request).getServletContext();
		doReturn(appContext).when(servletContext).getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		
		AppSettings appSettings = new AppSettings();
		doReturn(appSettings).when(appContext).getBean(AppSettings.BEAN_NAME);

		doReturn(storageClient).when(uploadHlsChunk).getStorageClient(any());
		doReturn(true).when(uploadHlsChunk).handlePostRequest(any(),any(),any());

		uploadHlsChunk.doPostForUnitTests(request,response);

		verify(uploadHlsChunk).handlePostRequest(storageClient,appContext,request);
	}
}
