package io.antmedia.test.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import io.antmedia.cluster.ClusterNode;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.settings.ServerSettings;
import org.apache.catalina.ha.tcp.SimpleTcpCluster;
import org.awaitility.Awaitility;
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


import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.filter.IPFilter;
import io.antmedia.filter.RestProxyFilter;

import static org.junit.Assert.*;

public class IPFilterTest {
	
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
	public void testBugNullContext() {
		 IPFilter ipFilter = Mockito.spy(new IPFilter());
		 
		 Mockito.doReturn(null).when(ipFilter).getAppContext();
		 assertFalse(ipFilter.isAllowed("127.0.0.1"));
		 

		 Mockito.doReturn(null).when(ipFilter).getAppSettings();
		 assertFalse(ipFilter.isAllowed("127.0.0.1"));
		
	}
	
	@Test
	public void testDataStoreClosed() 
	{
		 IPFilter ipFilter = Mockito.spy(new IPFilter());
		 ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);
		
		 
		 DataStoreFactory dtFactory = Mockito.mock(DataStoreFactory.class);
		 DataStore dtStore = Mockito.mock(DataStore.class);
		 
		 Mockito.when(dtFactory.getDataStore()).thenReturn(dtStore);
		 Mockito.when(dtStore.isAvailable()).thenReturn(true);
		 
		 //make webApplicationContext null
		 Mockito.doReturn(null).when(ipFilter).getWebApplicationContext();
		 //it should be null because no context
		 assertNull(ipFilter.getAppContext());
		 
		 Mockito.doReturn(webAppContext).when(ipFilter).getWebApplicationContext();
		 //make context running false
		 Mockito.when(webAppContext.isRunning()).thenReturn(false);
		 //it should be false because context is not running
		 assertNull(ipFilter.getAppContext());
		 
		 //make context running true
		 Mockito.when(webAppContext.isRunning()).thenReturn(true);
		 //it should not return  null because there is no datastorefactory is null which means it's not instance of IDataStoreFactory
		 assertNotNull(ipFilter.getAppContext());
		 

		 //Make datastorefactory available
		 Mockito.doReturn(dtFactory).when(webAppContext).getBean(DataStoreFactory.BEAN_NAME);
		 //it should return notnull because everything is ok
		 assertNotNull(ipFilter.getAppContext());
		 
		 //make data store not available
		 Mockito.when(dtStore.isAvailable()).thenReturn(false);
		 //it should be null because datastore is not available.
		 assertNull(ipFilter.getAppContext());
		 
	}

    @Test
    public void testIsCluster(){
        IPFilter ipFilter = Mockito.spy(new IPFilter());

       

        ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);
        Mockito.doReturn(webAppContext).when(ipFilter).getAppContext();
        Mockito.doReturn(false).when(webAppContext).containsBean(IClusterNotifier.BEAN_NAME);
    }
	
    @Test
    public void testDoFilterPass() throws IOException, ServletException {
        IPFilter ipFilter = Mockito.spy(new IPFilter());

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("127.0.0.1");
        
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        
        AppSettings appSettings = new AppSettings();
        appSettings.setRemoteAllowedCIDR("127.0.0.1/8");
        
        appSettings.setIpFilterEnabled(true);
        Mockito.doReturn(appSettings).when(ipFilter).getAppSettings();
        
        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
        assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
        
        // Add App Settings tests 
        MockHttpServletRequest httpServletRequest2 = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("22.22.11.11");
        
        MockHttpServletResponse httpServletResponse2 = new MockHttpServletResponse();
        MockFilterChain filterChain2 = new MockFilterChain();
        
        appSettings.setRemoteAllowedCIDR("null");
        appSettings.setIpFilterEnabled(false);
        Mockito.doReturn(appSettings).when(ipFilter).getAppSettings();
        
        ipFilter.doFilter(httpServletRequest2, httpServletResponse2, filterChain2);
        assertEquals(HttpStatus.OK.value(),httpServletResponse.getStatus());
        
    }

    @Test
    public void testDoFilterFail() throws IOException, ServletException {
        IPFilter ipFilter = Mockito.spy(new IPFilter());
        ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);
        Mockito.doReturn(webAppContext).when(ipFilter).getAppContext();

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRemoteAddr("192.168.0.1");
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        AppSettings appSettings = new AppSettings();
        appSettings.setIpFilterEnabled(true);
        appSettings.setRemoteAllowedCIDR("127.0.0.1/8");
        Mockito.doReturn(appSettings).when(ipFilter).getAppSettings();
        
        httpServletRequest.setPathInfo("");
        
        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(HttpStatus.FORBIDDEN.value(),httpServletResponse.getStatus());
    }
    
    
    boolean comodification = false;
    int numberOfInvoke = 0;
    /**
     * Below test is written to confirm a bug. 
     * ArrayList was throwing concurrent modification exception while accessing the same list by different threads. 
     * We make the method thread safe by filling 
     */
    @Test
    public void testComodification() {
    	   ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);
    	   
    	   AppSettings appSettings = new AppSettings();
       appSettings.setRemoteAllowedCIDR("127.0.0.1/8,192.168.5.10/8,10.10.5.2/16");
           
       comodification = false;
       numberOfInvoke = 0;
       for (int i = 0; i < 5; i++) 
       {
	    	   executorService.scheduleAtFixedRate(new Runnable() {
				
				@Override
				public void run() {
					try {
						IPFilter ipFilter = Mockito.spy(new IPFilter());
						MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
						httpServletRequest.setRemoteAddr("192.168.0.1");
						MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
						MockFilterChain filterChain = new MockFilterChain();
						Mockito.doReturn(appSettings).when(ipFilter).getAppSettings();
						ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
						
						appSettings.setRemoteAllowedCIDR("127.0.0.1/8,192.168.5.10/8,10.10.5.2/16");
						numberOfInvoke++;
					}
					catch (Exception e) {
						comodification = true;
						e.printStackTrace();
					}
					
				}
			}, 0, 1, TimeUnit.NANOSECONDS);
       }
    	   
    	   Awaitility.await().pollInterval(5,TimeUnit.SECONDS).atMost(6, TimeUnit.SECONDS).until(()-> !comodification);
    	   
    	   executorService.shutdownNow();
    	   logger.info("Number of invoke: " + numberOfInvoke);
    	
    }

    @Test
    public void testACMRest() throws IOException, ServletException {
        IPFilter ipFilter = Mockito.spy(new IPFilter());
        Mockito.doReturn(false).when(ipFilter).isAllowed(Mockito.anyString());
        Mockito.doReturn(new AppSettings()).when(ipFilter).getAppSettings();
        ConfigurableWebApplicationContext webAppContext = Mockito.mock(ConfigurableWebApplicationContext.class);
        Mockito.doReturn(webAppContext).when(ipFilter).getAppContext();

        MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setRequestURI("http://127.0.0.1:5080/WebRTCAppEE/rest/v2/acm/msg");
        MockHttpServletResponse httpServletResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        ipFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

        assertEquals(HttpStatus.OK.value(), httpServletResponse.getStatus());
    }
    
}
