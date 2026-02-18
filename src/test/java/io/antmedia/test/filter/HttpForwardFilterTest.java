package io.antmedia.test.filter;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import io.antmedia.AppSettings;
import io.antmedia.filter.HttpForwardFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class HttpForwardFilterTest {

	 	@Test
	    public void testDoFilterPass() throws IOException, ServletException
	    {
	 		 HttpForwardFilter httpForwardFilter = Mockito.spy(new HttpForwardFilter());

	         MockHttpServletRequest httpServletRequest = new MockHttpServletRequest();
	         httpServletRequest.setRemoteAddr("192.168.0.1");
	         httpServletRequest.setRequestURI("/LiveApp/streams/test.m3u8");
	         HttpServletResponse httpServletResponse = Mockito.mock(HttpServletResponse.class);
	         FilterChain filterChain = Mockito.mock(FilterChain.class);
	         AppSettings appSettings = new AppSettings();
	         
	         Mockito.doReturn(null).when(httpForwardFilter).getAppSettings();
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(1)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         Mockito.doReturn(appSettings).when(httpForwardFilter).getAppSettings();
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(2)).doFilter(httpServletRequest, httpServletResponse);
	         
	         //set extension
	         appSettings.setHttpForwardingExtension("m3u8");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should be called because no base url
	         Mockito.verify(filterChain, Mockito.times(3)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         //set extension empty
	         appSettings.setHttpForwardingExtension("");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should be called because no base url
	         Mockito.verify(filterChain, Mockito.times(4)).doFilter(httpServletRequest, httpServletResponse);
	        
	         appSettings.setHttpForwardingExtension("m3u8");
	         appSettings.setHttpForwardingBaseURL("");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should  be called because   url is empty
	         Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("m3u8");
	         appSettings.setHttpForwardingBaseURL("http://url/");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //chain filter should not be called because  base url is et
	         Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
	         Mockito.verify(httpServletResponse).sendRedirect("http://url/streams/test.m3u8");
	         
	         
	         httpServletRequest.setRequestURI(null);
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(6)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         httpServletRequest.setRequestURI("");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(7)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         Mockito.verify(filterChain, Mockito.times(8)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("m3u8,mp4");
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast.mp4");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //it shoud not increase because comma separated mp4
	         Mockito.verify(filterChain, Mockito.times(8)).doFilter(httpServletRequest, httpServletResponse);
	         
	         
	         appSettings.setHttpForwardingExtension("mkv");
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast.mp4");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //it shoud  increase because comma separated mp4
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("mp4");
	         try {
	        	 	httpServletRequest.setRequestURI("/LiveApp/rest/../broadcast.mp4");
	        	 	httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         	fail("It should throw exception");
	         }
	         catch (IOException e) {
	        	 
	         }
	         //it should not  increase because it throws exception
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         try {
		         appSettings.setHttpForwardingExtension("mp4");
		         httpServletRequest.setRequestURI("/LiveApp/rest/../broadcast.mp4");
		         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         }
	         catch (IOException e) {
	        	 
	         }
	         //it should not increase because it throws exception
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         
	         appSettings.setHttpForwardingExtension("mp4");
	         httpServletRequest.setRequestURI("/LiveApp/rest/broadcast.mp4");
	         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
	         //it should not increase because it's  ok to forward
	         Mockito.verify(filterChain, Mockito.times(9)).doFilter(httpServletRequest, httpServletResponse);
	         
	         {
	        	 appSettings.setHttpForwardingExtension("m3u8");
	        	 appSettings.setHttpForwardingBaseURL("http://url");
		         httpServletRequest.setRequestURI("/LiveApp/streams/test.m3u8");

		         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
		         //chain filter should not be called because  base url is et
		       //  Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
		         Mockito.verify(httpServletResponse, Mockito.times(2)).sendRedirect("http://url/streams/test.m3u8");
	         }
	         
	         {
	        	 appSettings.setHttpForwardingExtension("m3u8");
	        	 appSettings.setHttpForwardingBaseURL("http://url");
		         httpServletRequest.setRequestURI("LiveApp/streams/test.m3u8");

		         httpForwardFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);
		         //chain filter should not be called because  base url is et
		       //  Mockito.verify(filterChain, Mockito.times(5)).doFilter(httpServletRequest, httpServletResponse);
		         Mockito.verify(httpServletResponse, Mockito.times(3)).sendRedirect("http://url/streams/test.m3u8");
	        	 
	         }
	          
	         
	    }
        @Test
        public void testCorsFilterOrder()  {
             //cors filter should be before http forwarding filter because correct headers should be set before forwarding
            try {
                File xmlFile = new File("/usr/local/antmedia/webapps/live/WEB-INF/web.xml");
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(xmlFile);
                doc.getDocumentElement().normalize();

                NodeList filterList = doc.getElementsByTagName("filter");

                int corsIndex = -1;
                int httpForwardIndex = -1;

                for (int i = 0; i < filterList.getLength(); i++) {
                    Node node = filterList.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element filterElement = (Element) node;
                        String filterName = filterElement.getElementsByTagName("filter-name")
                                .item(0).getTextContent().trim();

                        if ("CorsFilter".equals(filterName)) {
                            corsIndex = i;
                        } else if ("HttpForwardFilter".equals(filterName)) {
                            httpForwardIndex = i;
                        }
                    }
                }

                if (corsIndex == -1) {
                    System.out.println("❌ CorsFilter not found in web.xml");
                    assert(false);
                } else if (httpForwardIndex == -1) {
                    System.out.println("❌ HttpForwardFilter not found in web.xml");
                    assert(false);
                } else {
                    if (corsIndex < httpForwardIndex) {
                        System.out.println("✅ CorsFilter is declared BEFORE HttpForwardFilter");
                        assert(true);
                    } else {
                        System.out.println("❌ CorsFilter is declared AFTER HttpForwardFilter");
                        assert(false);
                    }
                    System.out.println("CorsFilter index: " + corsIndex + ", HttpForwardFilter index: " + httpForwardIndex);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }


}
