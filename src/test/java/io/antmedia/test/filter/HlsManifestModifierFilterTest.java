package io.antmedia.test.filter;

import io.antmedia.filter.HlsManifestModifierFilter;
import io.antmedia.websocket.WebSocketConstants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

	String testAdaptiveM3u8 = "#EXTM3U\n" +
			"#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=749592,RESOLUTION=480x360,CODECS=\"avc1.42e00a,mp4a.40.2\"\n" +
			"teststream_360p800kbps.m3u8\n" +
			"#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=953304,RESOLUTION=640x480,CODECS=\"avc1.42e00a,mp4a.40.2\"\n" +
			"teststream_480p1000kbps.m3u8\n" +
			"#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1837224,RESOLUTION=960x720,CODECS=\"avc1.42e00a,mp4a.40.2\"\n" +
			"teststream_720p2000kbps.m3u8\n";


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

	@Test
	public void testFilterAdaptive() {

		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ServletOutputStream outputStream = mock(ServletOutputStream.class);
			when(mockResponse.getOutputStream()).thenReturn(outputStream);
			when(mockResponse.getStatus()).thenReturn(200);
			when(mockResponse.getWriter()).thenReturn(mock(java.io.PrintWriter.class));
			FilterChain myChain = new FilterChain() {
				@Override
				public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
						if(servletResponse instanceof  ContentCachingResponseWrapper){
							((ContentCachingResponseWrapper) servletResponse).setStatus(200);
							((ContentCachingResponseWrapper) servletResponse).getOutputStream().write(testAdaptiveM3u8.getBytes());
						}
				}
			};
			String subscriberId = "testSubscriber";
			String subscriberCode = "883068";
			String token = "testToken";

			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/test_adaptive.m3u8");
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_ID)).thenReturn(subscriberId);
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_CODE)).thenReturn(subscriberCode);
			when(mockRequest.getParameter(WebSocketConstants.TOKEN)).thenReturn(token);

			hlsManifestModifierFilter.doFilter(mockRequest, mockResponse, myChain);

			ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
			verify(outputStream,atLeastOnce()).write(captor.capture());

			String modifiedM3u8 = new String(captor.getValue());

			Pattern pattern = Pattern.compile("subscriberCode=(\\w+)&subscriberId=(\\w+)&token=(\\w+)");

			Matcher matcher = pattern.matcher(modifiedM3u8);
			boolean found = false;

			while (matcher.find()) {
				String subscriberCodeFound = matcher.group(1);
				String subscriberIdFound = matcher.group(2);
				String tokenFound = matcher.group(3);
				assertEquals(subscriberCode, subscriberCodeFound);
				assertEquals(subscriberId, subscriberIdFound);
				assertEquals(token, tokenFound);
				found=true;
				break;
			}

			assertTrue(found);

			HttpServletResponse mockResponse2 = mock(HttpServletResponse.class);
			ServletOutputStream outputStream2 = mock(ServletOutputStream.class);
			when(mockResponse2.getOutputStream()).thenReturn(outputStream2);
			when(mockResponse2.getStatus()).thenReturn(200);
			when(mockResponse2.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

			HttpServletRequest mockRequest2 = mock(HttpServletRequest.class);
			when(mockRequest2.getMethod()).thenReturn("GET");
			when(mockRequest2.getRequestURI()).thenReturn("/LiveApp/streams/test_adaptive.m3u8");

			when(mockRequest2.getParameter(WebSocketConstants.SUBSCRIBER_ID)).thenReturn("");
			when(mockRequest2.getParameter(WebSocketConstants.SUBSCRIBER_CODE)).thenReturn(subscriberCode);
			when(mockRequest2.getParameter(WebSocketConstants.TOKEN)).thenReturn(token);


			hlsManifestModifierFilter.doFilter(mockRequest2, mockResponse2, myChain);

			ArgumentCaptor<byte[]> captor2 = ArgumentCaptor.forClass(byte[].class);
			verify(outputStream2,atLeastOnce()).write(captor2.capture());

			Pattern pattern2 = Pattern.compile("subscriberCode=(\\w+)&token=(\\w+)");

			String modifiedm3u82 = new String(captor2.getValue());

			Matcher matcher2 = pattern2.matcher(modifiedm3u82);
			found = false;
			while (matcher2.find()) {
				String subscriberCodeFound = matcher2.group(1);
				String tokenFound = matcher2.group(2);
				assertEquals(subscriberCode, subscriberCodeFound);
				assertEquals(token, tokenFound);
				found = true;
				break;
			}
			assertTrue(found);

			HttpServletResponse mockResponse3 = mock(HttpServletResponse.class);
			ServletOutputStream outputStream3 = mock(ServletOutputStream.class);
			when(mockResponse3.getOutputStream()).thenReturn(outputStream3);
			when(mockResponse3.getStatus()).thenReturn(200);
			when(mockResponse3.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

			HttpServletRequest mockRequest3 = mock(HttpServletRequest.class);
			when(mockRequest3.getMethod()).thenReturn("GET");
			when(mockRequest3.getRequestURI()).thenReturn("/LiveApp/streams/test_adaptive.m3u8");

			when(mockRequest3.getParameter(WebSocketConstants.SUBSCRIBER_ID)).thenReturn("");
			when(mockRequest3.getParameter(WebSocketConstants.SUBSCRIBER_CODE)).thenReturn(null);
			when(mockRequest3.getParameter(WebSocketConstants.TOKEN)).thenReturn(token);
			
			hlsManifestModifierFilter.doFilter(mockRequest3, mockResponse3, myChain);

			ArgumentCaptor<byte[]> captor3 = ArgumentCaptor.forClass(byte[].class);
			verify(outputStream3,atLeastOnce()).write(captor3.capture());

			Pattern pattern3 = Pattern.compile("token=(\\w+)");

			String modifiedm3u83 = new String(captor3.getValue());

			Matcher matcher3 = pattern3.matcher(modifiedm3u83);
			found = false;
			while (matcher3.find()) {
				String tokenFound = matcher3.group(1);
				assertEquals(token, tokenFound);
				found = true;
				break;
			}

			assertTrue(found);

			HttpServletResponse mockResponse4 = mock(HttpServletResponse.class);
			ServletOutputStream outputStream4 = mock(ServletOutputStream.class);
			when(mockResponse4.getOutputStream()).thenReturn(outputStream4);
			when(mockResponse4.getStatus()).thenReturn(200);
			when(mockResponse4.getWriter()).thenReturn(mock(java.io.PrintWriter.class));

			HttpServletRequest mockRequest4 = mock(HttpServletRequest.class);
			when(mockRequest4.getMethod()).thenReturn("GET");
			when(mockRequest4.getRequestURI()).thenReturn("/LiveApp/streams/test_adaptive.m3u8");

			when(mockRequest4.getParameter(WebSocketConstants.SUBSCRIBER_ID)).thenReturn(subscriberId);
			when(mockRequest4.getParameter(WebSocketConstants.SUBSCRIBER_CODE)).thenReturn(subscriberCode);
			when(mockRequest4.getParameter(WebSocketConstants.TOKEN)).thenReturn(null);


			hlsManifestModifierFilter.doFilter(mockRequest4, mockResponse4, myChain);

			ArgumentCaptor<byte[]> captor4 = ArgumentCaptor.forClass(byte[].class);
			verify(outputStream4,atLeastOnce()).write(captor4.capture());

			Pattern pattern4 = Pattern.compile("subscriberCode=(\\w+)&subscriberId=(\\w+)");

			String modifiedm3u84 = new String(captor4.getValue());

			Matcher matcher4 = pattern4.matcher(modifiedm3u84);

			found = false;
			while (matcher4.find()) {
				String subscriberCodeFound = matcher4.group(1);
				String subscriberIdFound = matcher4.group(2);

				assertEquals(subscriberCode, subscriberCodeFound);
				assertEquals(subscriberId, subscriberIdFound);
				found = true;
				break;
			}

			assertTrue(found);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
