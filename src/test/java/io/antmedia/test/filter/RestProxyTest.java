package io.antmedia.test.filter;

import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.RestProxyFilter;
import io.antmedia.settings.ServerSettings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RestProxyTest {
    protected static Logger logger = LoggerFactory.getLogger(IPFilterTest.class);


    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Starting test: " + description.getMethodName());
        }

        protected void failed(Throwable e, Description description) {
            System.out.println("Failed test: " + description.getMethodName() );
            e.printStackTrace();
        }
        protected void finished(Description description) {
            System.out.println("Finishing test: " + description.getMethodName());
        }
    };

    @Test
    public void testDoFilterPass() throws IOException, ServletException {
        RestProxyFilter restFilter = Mockito.spy(new RestProxyFilter());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);
        Mockito.doReturn(webAppContext).when(restFilter).getAppContext();

        DataStoreFactory dtFactory = Mockito.mock(DataStoreFactory.class);
        DataStore dtStore = Mockito.mock(DataStore.class);

        Mockito.when(dtFactory.getDataStore()).thenReturn(dtStore);
        Mockito.when(dtStore.isAvailable()).thenReturn(true);

        Mockito.doReturn(dtFactory).when(webAppContext).getBean(DataStoreFactory.BEAN_NAME);

        AppSettings appSettings = new AppSettings();
        appSettings.setRemoteAllowedCIDR("127.0.0.1/8");

        appSettings.setIpFilterEnabled(true);
        Mockito.doReturn(appSettings).when(restFilter).getAppSettings();

        restFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());

        // Add App Settings tests
        MockHttpServletRequest httpServletRequest2 = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("22.22.11.11");

        MockHttpServletResponse httpServletResponse2 = new MockHttpServletResponse();
        MockFilterChain filterChain2 = new MockFilterChain();

        appSettings.setRemoteAllowedCIDR("null");
        appSettings.setIpFilterEnabled(false);
        Mockito.doReturn(appSettings).when(restFilter).getAppSettings();

        restFilter.doFilter(httpServletRequest2, httpServletResponse2, filterChain2);
        assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());

    }

    @Test
    public void testDoFilterPassCluster() throws IOException, ServletException {
        RestProxyFilter restFilter = Mockito.spy(new RestProxyFilter());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("10.0.0.0");

        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);
        Mockito.doReturn(webAppContext).when(restFilter).getAppContext();

        DataStoreFactory dtFactory = Mockito.mock(DataStoreFactory.class);
        DataStore dtStore = Mockito.mock(DataStore.class);

        Broadcast broadcast = new Broadcast();
        broadcast.setOriginAdress("127.0.0.1");
        try{
            broadcast.setStreamId("stream1");
        }
        catch(Exception e ){
            logger.error("StreamId can't set");
            fail();
        }

        Mockito.doReturn(broadcast).when(dtStore).get(Mockito.anyString());

        Mockito.when(dtFactory.getDataStore()).thenReturn(dtStore);
        Mockito.when(dtStore.isAvailable()).thenReturn(true);

        Mockito.doReturn(dtFactory).when(webAppContext).getBean(DataStoreFactory.BEAN_NAME);
        Mockito.doReturn(true).when(webAppContext).containsBean(IClusterNotifier.BEAN_NAME);

        AppSettings appSettings = new AppSettings();
        appSettings.setRemoteAllowedCIDR("127.0.0.1/8");

        ServerSettings serverSettings = Mockito.spy(new ServerSettings());
        Mockito.doReturn("172.0.0.0").when(serverSettings).getHostAddress();

        appSettings.setIpFilterEnabled(true);
        Mockito.doReturn(appSettings).when(restFilter).getAppSettings();
        Mockito.doReturn(serverSettings).when(restFilter).getServerSetting();

        restFilter.doFilter(httpServletRequest,httpServletResponse,filterChain);
        assertEquals(501, httpServletResponse.getStatus());
    }
}
