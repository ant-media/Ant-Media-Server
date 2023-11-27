package io.antmedia.test.filter;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;

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
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.filter.SubscriberBlockFilter;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.ws.rs.HttpMethod;

public class SubscriberBlockFilterTest {

    private SubscriberBlockFilter subscriberBlockFilter;

    protected static Logger logger = LoggerFactory.getLogger(SubscriberBlockFilterTest.class);


    @Before
    public void before() {
        subscriberBlockFilter = new SubscriberBlockFilter();
    }

    @After
    public void after() {
        subscriberBlockFilter = null;
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
    public void testDoFilter() {
        FilterConfig filterconfig = mock(FilterConfig.class);
        ServletContext servletContext = mock(ServletContext.class);
        ConfigurableWebApplicationContext context = mock(ConfigurableWebApplicationContext.class);

        when(context.isRunning()).thenReturn(true);
        IStreamStats streamStats = mock(IStreamStats.class);

        when(context.getBean(HlsViewerStats.BEAN_NAME)).thenReturn(streamStats);

        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenReturn(context);

        when(filterconfig.getServletContext()).thenReturn(servletContext);


        try {
            subscriberBlockFilter.init(filterconfig);
            String subscriberId = "subscriber1Id";
            MockHttpServletRequest httpServletRequest1 = new MockHttpServletRequest();
            httpServletRequest1.setMethod(HttpMethod.GET);
            String streamId = "testStream";
            httpServletRequest1.setRequestURI("/LiveApp/streams/"+streamId+".m3u8");

            MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
            MockFilterChain filterChain1 = new MockFilterChain();
            DataStoreFactory dsf = mock(DataStoreFactory.class);
            when(context.getBean(DataStoreFactory.BEAN_NAME)).thenReturn(dsf);
            DataStore dataStore = mock(DataStore.class);
            when(dataStore.isAvailable()).thenReturn(true);
            when(dsf.getDataStore()).thenReturn(dataStore);

            subscriberBlockFilter.doFilter(httpServletRequest1, httpServletResponse, filterChain1);

            Broadcast broadcast = mock(Broadcast.class);
            broadcast.setStreamId(streamId);
            when(dataStore.get(streamId)).thenReturn(broadcast);

            MockFilterChain filterChain2 = new MockFilterChain();

            subscriberBlockFilter.doFilter(httpServletRequest1, httpServletResponse, filterChain2);


            Subscriber subscriber = new Subscriber();
            subscriber.setSubscriberId(subscriberId);
            subscriber.setBlockedType(Subscriber.PLAY_TYPE);
            long currTime = System.currentTimeMillis();
            subscriber.setBlockedUntilUnitTimeStampMs(currTime + 10000);
            when(dataStore.getSubscriber(streamId,subscriberId)).thenReturn(subscriber);

            MockFilterChain filterChain3 = new MockFilterChain();
            httpServletRequest1.setParameter("subscriberId",subscriberId);
            subscriberBlockFilter.doFilter(httpServletRequest1, httpServletResponse, filterChain3);


            MockFilterChain filterChain4 = new MockFilterChain();
            subscriber.setBlockedUntilUnitTimeStampMs(currTime - 15000);
            subscriberBlockFilter.doFilter(httpServletRequest1, httpServletResponse, filterChain4);






        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e));
            fail(ExceptionUtils.getStackTrace(e));
        }


    }



}
