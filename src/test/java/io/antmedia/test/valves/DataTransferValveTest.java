package io.antmedia.test.valves;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.Valve;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.red5.server.api.scope.IScope;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.analytic.model.PlayerStatsEvent;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.logger.LoggerUtils;
import io.antmedia.valves.DataTransferValve;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public class DataTransferValveTest {

	@InjectMocks
    private DataTransferValve dataTransferValve;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private Valve nextValve;

    @Mock
    private ConfigurableWebApplicationContext context;

    @Mock
    private AntMediaApplicationAdapter appAdapter;
    
    @Mock
    private ServletContext servletContext;
    
    @Captor
    private ArgumentCaptor<PlayerStatsEvent> eventCaptor;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        when(request.getServletContext()).thenReturn(servletContext);
        when(request.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE)).thenReturn(context);
        when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(appAdapter);
        when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(new AppSettings());

        
        when(appAdapter.getScope()).thenReturn(Mockito.mock(IScope.class));
        when(appAdapter.getScope().getName()).thenReturn("appScope");
    }

    @Test
    public void testInvoke_withValidRequest() throws IOException, ServletException {
    	 when(request.getRequestURI()).thenReturn("/stream/123.m3u8");
         when(request.getMethod()).thenReturn("GET");
         when(request.getRemoteAddr()).thenReturn("192.168.0.1");
         when(((HttpServletRequest)request).getParameter("subscriberId")).thenReturn("user123");
         when(response.getBytesWritten(false)).thenReturn(2048L);

         // Execute the valve's invoke method
         DataTransferValve dataTransferValveSpy = Mockito.spy(dataTransferValve);
         dataTransferValveSpy.invoke(request, response);

         // Verify interactions
         verify(nextValve).invoke(request, response);
         verify(context, times(0)).getBean(AntMediaApplicationAdapter.BEAN_NAME);
         
         
         when(context.isRunning()).thenReturn(true);
         dataTransferValveSpy.invoke(request, response);
         verify(context, times(1)).getBean(AntMediaApplicationAdapter.BEAN_NAME);

         // Verify static method interaction
         Mockito.verify(dataTransferValveSpy).log(eventCaptor.capture());

         // Asserts
         PlayerStatsEvent loggedEvent = eventCaptor.getValue();
         assertEquals("123", loggedEvent.getStreamId());
         assertEquals("/stream/123.m3u8", loggedEvent.getUri());
         assertEquals("user123", loggedEvent.getSubscriberId());
         assertEquals("appScope", loggedEvent.getApp());
         assertEquals(2048L, loggedEvent.getByteTransferred());
         assertEquals("192.168.0.1", loggedEvent.getClientIP());  
  
    }

    @Test
    public void testInvoke_withInvalidMethod() throws IOException, ServletException {
        // Set up the HTTP request for an unsupported method
        when(request.getRequestURI()).thenReturn("/stream/123");
        when(request.getMethod()).thenReturn("POST");

        // Execute the valve's invoke method
        DataTransferValve dataTransferValveSpy = Mockito.spy(dataTransferValve);
        dataTransferValveSpy.invoke(request, response);
        
        
        Mockito.verify(dataTransferValveSpy, Mockito.never()).log(eventCaptor.capture());


    }
}

