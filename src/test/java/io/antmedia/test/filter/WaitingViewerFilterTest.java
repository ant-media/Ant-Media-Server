package io.antmedia.test.filter;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.filter.WaitingViewerFilter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;

public class WaitingViewerFilterTest {

    private WaitingViewerFilter waitingViewerFilter;

    protected static Logger logger = LoggerFactory.getLogger(WaitingViewerFilterTest.class);


    @Before
    public void before() {
        waitingViewerFilter = new WaitingViewerFilter();
    }

    @After
    public void after() {
        waitingViewerFilter = null;
    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Starting test: " + description.getMethodName());
        }

        protected void failed(Throwable e, Description description) {
            System.out.println("Failed test: " + description.getMethodName());
        };
        protected void finished(Description description) {
            System.out.println("Finishing test: " + description.getMethodName());
        };
    };

    @Test
    public void testUninitialized() {
        FilterConfig filterconfig = mock(FilterConfig.class);

        ServletContext servletContext = mock(ServletContext.class);
        ApplicationContext context = mock(ConfigurableWebApplicationContext.class);
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(context);

        when(filterconfig.getServletContext()).thenReturn(servletContext);

        waitingViewerFilter.setConfig(filterconfig);


        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        HttpServletResponse mockResponse = mock(HttpServletResponse.class);
        FilterChain mockChain = mock(FilterChain.class);

        HttpSession session = mock(HttpSession.class);
        String sessionId = RandomStringUtils.randomAlphanumeric(16);
        when(session.getId()).thenReturn(sessionId);
        when(mockRequest.getSession()).thenReturn(session);
        when(mockRequest.getMethod()).thenReturn("HEAD");

        String streamId = RandomStringUtils.randomAlphanumeric(8);
        when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");

        when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_OK);

        try {
            waitingViewerFilter.doFilter(mockRequest, mockResponse, mockChain);
        } catch (IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            fail(ExceptionUtils.getStackTrace(e));
        } catch (ServletException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @Test
    public void testDoFilter() throws Exception {
        FilterConfig filterconfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);
        AntMediaApplicationAdapter antMediaApplicationAdapter = mock(AntMediaApplicationAdapter.class);

        when(context.isRunning()).thenReturn(true);
        IStreamStats streamStats = mock(IStreamStats.class);

        when(context.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(streamStats);
        when(context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).thenReturn(antMediaApplicationAdapter);

        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(context);

        when(filterconfig.getServletContext()).thenReturn(servletContext);

        DataStoreFactory dsf = mock(DataStoreFactory.class);
        when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);

        DataStore dataStore = mock(DataStore.class);
        when(dataStore.isAvailable()).thenReturn(true);
        when(dsf.getDataStore()).thenReturn(dataStore);
        antMediaApplicationAdapter.setDataStore(dataStore);
        try {
            waitingViewerFilter.init(filterconfig);

            HttpServletRequest mockRequest = mock(HttpServletRequest.class);
            HttpServletResponse mockResponse = mock(HttpServletResponse.class);
            FilterChain mockChain = mock(FilterChain.class);

            HttpSession session = mock(HttpSession.class);
            String sessionId = RandomStringUtils.randomAlphanumeric(16);
            when(session.getId()).thenReturn(sessionId);
            when(mockRequest.getSession()).thenReturn(session);
            when(mockRequest.getMethod()).thenReturn("HEAD");

            String streamId = RandomStringUtils.randomAlphanumeric(8);
            when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+streamId+".m3u8");

            when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_NOT_FOUND);
            Broadcast broadcast = new Broadcast();
            broadcast.setStreamId(streamId);
            broadcast.setAutoStartStopEnabled(true);
            broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_STOPPED);
            dataStore.save(broadcast);

            when(waitingViewerFilter.getBroadcast(mockRequest, streamId)).thenReturn(broadcast);
            when(waitingViewerFilter.getAntMediaApplicationAdapter().getDataStore()).thenReturn(dataStore);
            logger.info("session id {}, stream id {}", sessionId, streamId);
            waitingViewerFilter.doFilter(mockRequest, mockResponse, mockChain);

            verify(antMediaApplicationAdapter, times(1)).autoStartBroadcast(broadcast);


            when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/"+broadcast.getStreamId()+"/.m4s");
            broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
            when(mockResponse.getStatus()).thenReturn(HttpServletResponse.SC_BAD_GATEWAY);
            waitingViewerFilter.doFilter(mockRequest, mockResponse, mockChain);





        } catch (ServletException|IOException e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            fail(ExceptionUtils.getStackTrace(e));
        }
        catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            fail(ExceptionUtils.getStackTrace(e));
        }


    }

}
