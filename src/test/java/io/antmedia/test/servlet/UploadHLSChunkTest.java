package io.antmedia.test.servlet;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.servlet.UploadHLSChunk;
import io.antmedia.storage.AmazonS3StorageClient;
import io.antmedia.storage.StorageClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class UploadHLSChunkTest {

    @Mock
    private HttpServletRequest mockRequest;
    @Mock
    private HttpServletResponse mockResponse;
    @Mock
    private ServletContext mockServletContext;
    @Mock
    private StorageClient mockStorageClient;

    private UploadHLSChunk servlet;

    @Before
    public void setUp() throws ServletException {
        MockitoAnnotations.initMocks(this);
        servlet = new UploadHLSChunk();
        servlet.init();
    }

    @After
    public void after() {

    }

    @Test
    public void testDoPut() {
        when(mockRequest.getServletContext()).thenReturn(mockServletContext);
        when(mockServletContext.getAttribute("storageClient")).thenReturn(mockStorageClient);
        when(mockStorageClient.isEnabled()).thenReturn(true);

        //test with null storage client
        servlet.setServletContext(null);
        servlet.doPutForUnitTests(mockRequest, mockResponse);
        verify(mockStorageClient, times(0)).isEnabled();
        verify(mockRequest, times(1)).getServletContext();
        verify(mockResponse, never()).setStatus(anyInt());

        // test with storage client
        StorageClient storageClient = (StorageClient) mockServletContext.getAttribute("storageClient");
        servlet.setStorageClient(storageClient);

        servlet.doPutForUnitTests(mockRequest, mockResponse);

        verify(mockStorageClient, times(1)).isEnabled();
        verify(mockRequest, times(3)).getServletContext();
        verify(mockResponse, never()).setStatus(anyInt());
    }

    @Test
    public void testStatusListener() {

        try {
            UploadHLSChunk.StatusListener statusListener = new UploadHLSChunk.StatusListener("file");
            assertFalse(statusListener.isTimeoutOrErrorExist());

            statusListener.onComplete(null);
            assertFalse(statusListener.isTimeoutOrErrorExist());

            statusListener.onStartAsync(null);
            assertFalse(statusListener.isTimeoutOrErrorExist());

            statusListener.onTimeout(null);
            assertTrue(statusListener.isTimeoutOrErrorExist());

            statusListener = new UploadHLSChunk.StatusListener("file");
            assertFalse(statusListener.isTimeoutOrErrorExist());

            statusListener.onError(null);
            assertTrue(statusListener.isTimeoutOrErrorExist());

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    @Test
    public void testGetAppContext() {
        // Create a mock ServletContext object
        ServletContext servletContext = mock(ServletContext.class);
        ApplicationContext appCtx = mock(ApplicationContext.class);

        // Set up the mock ServletContext object to return the mock ApplicationContext object
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(appCtx);

        // Call the method to be tested
        UploadHLSChunk uploadHLSChunk = new UploadHLSChunk();
        uploadHLSChunk.setServletContext(servletContext);
        ApplicationContext result = uploadHLSChunk.getAppContext();

        // Verify that the method returned the expected ApplicationContext object
        assertEquals(appCtx, result);
    }

    @Test
    public void testGetApplication() {
        // Create a mock ServletContext object
        ServletContext servletContext = mock(ServletContext.class);
        ApplicationContext appContext = mock(ApplicationContext.class);
        AntMediaApplicationAdapter appInstance = mock(AntMediaApplicationAdapter.class);

        // Set up the mock objects to return the expected values
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(appContext);
        when(appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appInstance);

        // Call the method to be tested
        UploadHLSChunk uploadHLSChunk = new UploadHLSChunk();
        uploadHLSChunk.setServletContext(servletContext);
        AntMediaApplicationAdapter result = uploadHLSChunk.getApplication();

        // Verify that the method returned the expected AntMediaApplicationAdapter object
        assertEquals(appInstance, result);
    }

    @Test
    public void testInitStorageClient() {
        // Create a mock AntMediaApplicationAdapter object
        AntMediaApplicationAdapter app = mock(AntMediaApplicationAdapter.class);
        IScope scope = mock(IScope.class);
        IContext context = mock(IContext.class);
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        StorageClient storageClient = mock(StorageClient.class);

        // Set up the mock objects
        when(app.getScope()).thenReturn(scope);
        when(scope.getContext()).thenReturn(context);
        when(context.getApplicationContext()).thenReturn(applicationContext);
        when(applicationContext.containsBean(StorageClient.BEAN_NAME)).thenReturn(true);
        when(applicationContext.getBean(StorageClient.BEAN_NAME)).thenReturn(storageClient);

        // Call the method to be tested
        UploadHLSChunk uploadHLSChunk = new UploadHLSChunk();
        uploadHLSChunk.setAppInstance(app);
        uploadHLSChunk.initStorageClient();

        // Verify that the StorageClient object was set correctly
        assertEquals(storageClient, uploadHLSChunk.getStorageClient());
    }

    @Test
    public void testUploadHLSChunk() throws Exception {
        // Create a mock HttpServletRequest object
        HttpServletRequest req = mock(HttpServletRequest.class);
        ServletInputStream inputStream = mock(ServletInputStream.class);
        when(req.getInputStream()).thenReturn(inputStream);
        when(req.getPathInfo()).thenReturn("/hls-upload/test");

        // Create a mock HttpServletResponse object
        HttpServletResponse resp = mock(HttpServletResponse.class);

        // Create a mock ConfigurableWebApplicationContext object
        ConfigurableWebApplicationContext appContext = mock(ConfigurableWebApplicationContext.class);
        when(appContext.isRunning()).thenReturn(true);
        when(appContext.getApplicationName()).thenReturn("testApp");

        // Create a mock StorageClient object
        AmazonS3StorageClient storageClient = Mockito.spy(new AmazonS3StorageClient());

        // Call the method to be tested
        UploadHLSChunk uploadHLSChunk = new UploadHLSChunk();
        uploadHLSChunk.setStorageClient(storageClient);
        ServletContext servletContext = mock(ServletContext.class);
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(appContext);
        uploadHLSChunk.setServletContext(servletContext);
        uploadHLSChunk.uploadHLSChunk(req, resp);

        // Verify that the StorageClient object was called with the expected parameters
        String expectedFilepath = "webapps/testApp/streams/hls-upload/test.tmp";
        File expectedFile = new File(expectedFilepath);

        storageClient.setAccessKey("ACCESS_KEY");
        storageClient.setSecretKey("SECRET_KEY");
        storageClient.setRegion("eu-west-1");
        storageClient.setStorageName("BUCKET_NAME");
        storageClient.setStorageClass("STANDARD");
        storageClient.setEnabled(true);

        storageClient.save(expectedFilepath, expectedFile);

        Mockito.verify(storageClient, Mockito.times(1)).getTransferManager();
    }
}
