package io.antmedia.test.filter;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.HlsManifestModifierFilter;
import io.antmedia.filter.HlsStatisticsFilter;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.lang.model.util.Types;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HlsManifestModifierFilterTest {
	
	private HlsManifestModifierFilter hlsManifestModifierFilter;

	protected static Logger logger = LoggerFactory.getLogger(HlsManifestModifierFilterTest.class);


	@Before
	public void before() {
		hlsManifestModifierFilter = new HlsManifestModifierFilter();
	}
	
	@After
	public void after() {
		hlsManifestModifierFilter = null;
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

	String testM3u8 = "#EXTM3U\n" +
			"#EXT-X-VERSION:3\n" +
			"#EXT-X-TARGETDURATION:2\n" +
			"#EXT-X-MEDIA-SEQUENCE:0\n" +
			"#EXT-X-PLAYLIST-TYPE:EVENT\n" +
			"#EXTINF:2.200000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:27:58.590+0300\n" +
			"test000000000.ts\n" +
			"#EXTINF:1.980000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:00.790+0300\n" +
			"test000000001.ts\n" +
			"#EXTINF:2.020000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:02.770+0300\n" +
			"test000000002.ts\n" +
			"#EXTINF:2.000000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:04.790+0300\n" +
			"test000000003.ts\n" +
			"#EXTINF:2.100000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:06.790+0300\n" +
			"test000000004.ts\n" +
			"#EXTINF:1.980000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:08.890+0300\n" +
			"test000000005.ts\n" +
			"#EXTINF:2.020000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:10.870+0300\n" +
			"test000000006.ts\n" +
			"#EXTINF:2.000000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:12.890+0300\n" +
			"test000000007.ts\n" +
			"#EXTINF:2.020000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:14.890+0300\n" +
			"test000000008.ts\n" +
			"#EXTINF:2.000000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:16.910+0300\n" +
			"test000000009.ts\n" +
			"#EXTINF:2.100000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:18.910+0300\n" +
			"test000000010.ts\n" +
			"#EXT-X-ENDLIST\n";

	
	@Test
	public void testNonFilterCases() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			FilterChain mockChain = mock(FilterChain.class);

			//not get method
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			when(mockRequest.getMethod()).thenReturn("POST");

			hlsManifestModifierFilter.doFilter(mockRequest, mockResponse, mockChain);
			verify(mockChain).doFilter(mockRequest, mockResponse);

			//not m3u8
			HttpServletRequest mockRequest2 = mock(HttpServletRequest.class);
			when(mockRequest2.getMethod()).thenReturn("GET");
			when(mockRequest2.getRequestURI()).thenReturn("/LiveApp/streams/test.xyz");

			hlsManifestModifierFilter.doFilter(mockRequest2, mockResponse, mockChain);
			verify(mockChain).doFilter(mockRequest2, mockResponse);


			//no start end params
			HttpServletRequest mockRequest3 = mock(HttpServletRequest.class);
			when(mockRequest3.getMethod()).thenReturn("GET");
			when(mockRequest3.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");

			hlsManifestModifierFilter.doFilter(mockRequest3, mockResponse, mockChain);
			verify(mockChain).doFilter(mockRequest3, mockResponse);


			//blank start end params
			HttpServletRequest mockRequest4 = mock(HttpServletRequest.class);
			when(mockRequest4.getMethod()).thenReturn("GET");
			when(mockRequest4.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest4.getParameter(HlsManifestModifierFilter.START)).thenReturn("");
			when(mockRequest4.getParameter(HlsManifestModifierFilter.END)).thenReturn("");

			hlsManifestModifierFilter.doFilter(mockRequest4, mockResponse, mockChain);
			verify(mockChain).doFilter(mockRequest4, mockResponse);


		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	public void testFilter() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ServletOutputStream outputStream = mock(ServletOutputStream.class);
			when(mockResponse.getOutputStream()).thenReturn(outputStream);
			when(mockResponse.getStatus()).thenReturn(200);
			when(mockResponse.getWriter()).thenReturn(mock(java.io.PrintWriter.class));
			FilterChain myChain = new FilterChain() {
				@Override
				public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
					((ContentCachingResponseWrapper)servletResponse).setStatus(200);
					((ContentCachingResponseWrapper)servletResponse).getOutputStream().write(testM3u8.getBytes());
				}
			};

			//blank start end params
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest.getParameter(HlsManifestModifierFilter.START)).thenReturn("1709926082");
			when(mockRequest.getParameter(HlsManifestModifierFilter.END)).thenReturn("1709926087");

			hlsManifestModifierFilter.doFilter(mockRequest, mockResponse, myChain);

			//verify that the response is written with captor
			ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
			verify(outputStream).write(captor.capture());

			String modifiedM3u8 = new String(captor.getValue());
			//System.out.println(modifiedM3u8);

			assertFalse(modifiedM3u8.contains("test000000001.ts"));
			assertTrue(modifiedM3u8.contains("test000000002.ts"));
			assertTrue(modifiedM3u8.contains("test000000003.ts"));
			assertTrue(modifiedM3u8.contains("test000000004.ts"));
			assertFalse(modifiedM3u8.contains("test000000005.ts"));

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}



}
