package io.antmedia.test.filter;

import io.antmedia.AppSettings;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HlsManifestModifierFilterTest {

	private HlsManifestModifierFilter hlsManifestModifierFilter;

	protected static Logger logger = LoggerFactory.getLogger(HlsManifestModifierFilterTest.class);


	@Before
	public void before() {
		hlsManifestModifierFilter = spy(new HlsManifestModifierFilter());
		AppSettings appSettings = new AppSettings();
		doReturn(appSettings).when(hlsManifestModifierFilter).getAppSettings();
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

	String testM3u8Query = "#EXTM3U\n" +
			"#EXT-X-VERSION:3\n" +
			"#EXT-X-TARGETDURATION:2\n" +
			"#EXT-X-MEDIA-SEQUENCE:0\n" +
			"#EXT-X-PLAYLIST-TYPE:EVENT\n" +
			"#EXTINF:2.200000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:27:58.590+0300\n" +
			"test000000000.ts?test=123\n" +
			"#EXTINF:1.980000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:00.790+0300\n" +
			"test000000001.ts?test=123\n" +
			"#EXTINF:2.020000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:02.770+0300\n" +
			"test000000002.ts?test=123\n" +
			"#EXTINF:2.000000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:04.790+0300\n" +
			"test000000003.ts?test=123\n" +
			"#EXTINF:2.100000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:06.790+0300\n" +
			"test000000004.ts?test=123\n" +
			"#EXTINF:1.980000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:08.890+0300\n" +
			"test000000005.ts?test=123\n" +
			"#EXTINF:2.020000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:10.870+0300\n" +
			"test000000006.ts?test=123\n" +
			"#EXTINF:2.000000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:12.890+0300\n" +
			"test000000007.ts?test=123\n" +
			"#EXTINF:2.020000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:14.890+0300\n" +
			"test000000008.ts?test=123\n" +
			"#EXTINF:2.000000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:16.910+0300\n" +
			"test000000009.ts?test=123\n" +
			"#EXTINF:2.100000,\n" +
			"#EXT-X-PROGRAM-DATE-TIME:2024-03-08T22:28:18.910+0300\n" +
			"test000000010.ts?test=123\n" +
			"#EXT-X-ENDLIST\n";

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

	String testAdaptiveM3u8Query = "#EXTM3U\n" +
			"#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=749592,RESOLUTION=480x360,CODECS=\"avc1.42e00a,mp4a.40.2\"\n" +
			"teststream_360p800kbps.m3u8?segment=1234\n" +
			"#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=953304,RESOLUTION=640x480,CODECS=\"avc1.42e00a,mp4a.40.2\"\n" +
			"teststream_480p1000kbps.m3u8?segment=1234\n" +
			"#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1837224,RESOLUTION=960x720,CODECS=\"avc1.42e00a,mp4a.40.2\"\n" +
			"teststream_720p2000kbps.m3u8?segment=1234\n";


	@Test
	public void testNonFilterCases() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			FilterChain mockChain = mock(FilterChain.class);

			// Case 1: Request method is not GET
			HttpServletRequest mockRequest1 = mock(HttpServletRequest.class);
			when(mockRequest1.getMethod()).thenReturn("POST");

			hlsManifestModifierFilter.doFilter(mockRequest1, mockResponse, mockChain);
			verify(mockChain).doFilter(eq(mockRequest1), any(HttpServletResponse.class));

			// Case 2: URI does not end with .m3u8
			HttpServletRequest mockRequest2 = mock(HttpServletRequest.class);
			when(mockRequest2.getMethod()).thenReturn("GET");
			when(mockRequest2.getRequestURI()).thenReturn("/LiveApp/streams/test.xyz");

			hlsManifestModifierFilter.doFilter(mockRequest2, mockResponse, mockChain);
			verify(mockChain).doFilter(eq(mockRequest2), any(HttpServletResponse.class));

			// Case 3: No start or end parameters provided
			HttpServletRequest mockRequest3 = mock(HttpServletRequest.class);
			when(mockRequest3.getMethod()).thenReturn("GET");
			when(mockRequest3.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest3.getParameter(HlsManifestModifierFilter.START)).thenReturn(null);
			when(mockRequest3.getParameter(HlsManifestModifierFilter.END)).thenReturn(null);

			hlsManifestModifierFilter.doFilter(mockRequest3, mockResponse, mockChain);
			verify(mockChain).doFilter(eq(mockRequest3), any(HttpServletResponse.class));

			// Case 4: Empty start and end parameters
			HttpServletRequest mockRequest4 = mock(HttpServletRequest.class);
			when(mockRequest4.getMethod()).thenReturn("GET");
			when(mockRequest4.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest4.getParameter(HlsManifestModifierFilter.START)).thenReturn("");
			when(mockRequest4.getParameter(HlsManifestModifierFilter.END)).thenReturn("");

			hlsManifestModifierFilter.doFilter(mockRequest4, mockResponse, mockChain);
			verify(mockChain).doFilter(eq(mockRequest4), any(HttpServletResponse.class));

		} catch (IOException | ServletException e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	public void testFilterIfIncludesQuery() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			when(mockResponse.getStatus()).thenReturn(200);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);
			ServletOutputStream outputStream = mock(ServletOutputStream.class);

			// Set up response wrapper and mocks for output stream and response
			when(mockResponse.getOutputStream()).thenReturn(outputStream);

			// Define the mock filter chain to write the `.m3u8` content to the response wrapper
			FilterChain myChain = (servletRequest, servletResponse) -> {
				((ContentCachingResponseWrapper) servletResponse).setStatus(200);
				((ContentCachingResponseWrapper) servletResponse).getOutputStream().write(testM3u8Query.getBytes(StandardCharsets.UTF_8));
			};

			// Set up request with method, URI, and parameters for start/end times and query details
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest.getParameter(HlsManifestModifierFilter.START)).thenReturn("1709926082");
			when(mockRequest.getParameter(HlsManifestModifierFilter.END)).thenReturn("1709926087");

			// Add subscriber parameters to be used in query
			String subscriberId = "testSubscriber";
			String subscriberCode = "883068";
			String token = "testToken";
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_ID)).thenReturn(subscriberId);
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_CODE)).thenReturn(subscriberCode);
			when(mockRequest.getParameter(WebSocketConstants.TOKEN)).thenReturn(token);

			// Run the filter
			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);

			// Capture the modified `.m3u8` content after filter processing
			byte[] modifiedResponseContent = responseWrapper.getContentAsByteArray();
			String modifiedM3u8 = new String(modifiedResponseContent, StandardCharsets.UTF_8);

			// Logging for debugging (if needed)
			logger.info(modifiedM3u8);

			// Assertions to confirm expected modifications
			assertFalse(modifiedM3u8.contains("test000000001.ts"));  // Segment outside the range
			assertTrue(modifiedM3u8.contains("test000000002.ts?test=123&subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertTrue(modifiedM3u8.contains("test000000003.ts?test=123&subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertTrue(modifiedM3u8.contains("test000000004.ts?test=123&subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertFalse(modifiedM3u8.contains("test000000005.ts"));  // Segment outside the range

		} catch (IOException | ServletException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testFilter() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);

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


			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);

			byte[] modifiedResponseContent = responseWrapper.getContentAsByteArray();
			String modifiedM3u8 = new String(modifiedResponseContent, StandardCharsets.UTF_8);

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
	public void testFilterAdaptiveQuery() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);

			ServletOutputStream outputStream = mock(ServletOutputStream.class);
			when(mockResponse.getOutputStream()).thenReturn(outputStream);
			when(mockResponse.getStatus()).thenReturn(200);
			when(mockResponse.getWriter()).thenReturn(mock(java.io.PrintWriter.class));
			FilterChain myChain = new FilterChain() {
				@Override
				public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
					if(servletResponse instanceof  ContentCachingResponseWrapper){
						((ContentCachingResponseWrapper) servletResponse).setStatus(200);
						((ContentCachingResponseWrapper) servletResponse).getOutputStream().write(testAdaptiveM3u8Query.getBytes());
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

			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);

			byte[] modifiedResponseContent = responseWrapper.getContentAsByteArray();
			String modifiedM3u8 = new String(modifiedResponseContent, StandardCharsets.UTF_8);

			logger.info(modifiedM3u8);

			assertTrue(modifiedM3u8.contains("teststream_360p800kbps.m3u8?segment=1234&subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertTrue(modifiedM3u8.contains("teststream_480p1000kbps.m3u8?segment=1234&subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertTrue(modifiedM3u8.contains("teststream_720p2000kbps.m3u8?segment=1234&subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testFilterAdaptive() {

		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);

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

			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);

			byte[] modifiedResponseContent = responseWrapper.getContentAsByteArray();
			String modifiedM3u8 = new String(modifiedResponseContent, StandardCharsets.UTF_8);


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
			ContentCachingResponseWrapper responseWrapper2 = new ContentCachingResponseWrapper(mockResponse2);

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


			hlsManifestModifierFilter.doFilter(mockRequest2, responseWrapper2, myChain);


			byte[] modifiedResponseContent2 = responseWrapper2.getContentAsByteArray();
			String modifiedm3u82 = new String(modifiedResponseContent2, StandardCharsets.UTF_8);

			Pattern pattern2 = Pattern.compile("subscriberCode=(\\w+)&token=(\\w+)");


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
			ContentCachingResponseWrapper responseWrapper3 = new ContentCachingResponseWrapper(mockResponse3);

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

			hlsManifestModifierFilter.doFilter(mockRequest3, responseWrapper3, myChain);

			byte[] modifiedResponseContent3 = responseWrapper3.getContentAsByteArray();
			String modifiedm3u83 = new String(modifiedResponseContent3, StandardCharsets.UTF_8);

			Pattern pattern3 = Pattern.compile("token=(\\w+)");


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
			ContentCachingResponseWrapper responseWrapper4 = new ContentCachingResponseWrapper(mockResponse4);

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


			hlsManifestModifierFilter.doFilter(mockRequest4, responseWrapper4, myChain);

			byte[] modifiedResponseContent4 = responseWrapper4.getContentAsByteArray();
			String modifiedm3u84= new String(modifiedResponseContent4, StandardCharsets.UTF_8);

			Pattern pattern4 = Pattern.compile("subscriberCode=(\\w+)&subscriberId=(\\w+)");


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

	@Test
	public void testAddTokenToTSSegmentURL() {
		testAddTokenToSegmentURL(testM3u8);
	}

	@Test
	public void testAddTokenToM4SSegmentURL() {
		String mp4TypeM3U8 = testM3u8.replace(".ts", ".m4s");
		testAddTokenToSegmentURL(mp4TypeM3U8);
	}

	public void testAddTokenToSegmentURL(String testFileContent) {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);

			ServletOutputStream outputStream = mock(ServletOutputStream.class);
			when(mockResponse.getOutputStream()).thenReturn(outputStream);
			when(mockResponse.getStatus()).thenReturn(200);
			when(mockResponse.getWriter()).thenReturn(mock(java.io.PrintWriter.class));
			FilterChain myChain = (servletRequest, servletResponse) -> {
				((ContentCachingResponseWrapper)servletResponse).setStatus(200);
				((ContentCachingResponseWrapper)servletResponse).getOutputStream().write(testFileContent.getBytes());
			};

			String subscriberId = "testSubscriber";
			String subscriberCode = "883068";
			String token = "testToken";

			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_ID)).thenReturn(subscriberId);
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_CODE)).thenReturn(subscriberCode);
			when(mockRequest.getParameter(WebSocketConstants.TOKEN)).thenReturn(token);


			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);

			//verify that the response is written with captor
			byte[] modifiedResponseContent4 = responseWrapper.getContentAsByteArray();
			String modifiedM3u8= new String(modifiedResponseContent4, StandardCharsets.UTF_8);


			Pattern pattern = Pattern.compile("subscriberCode=(\\w+)&subscriberId=(\\w+)");
			Matcher matcher = pattern.matcher(modifiedM3u8);

			boolean found = false;
			while (matcher.find()) {
				String subscriberCodeFound = matcher.group(1);
				String subscriberIdFound = matcher.group(2);

				assertEquals(subscriberCode, subscriberCodeFound);
				assertEquals(subscriberId, subscriberIdFound);
				found = true;
				break;
			}

			assertTrue(found);



		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}


	@Test
	public void testAddToSegmentWithTimeInterval() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);

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


			String subscriberId = "testSubscriber";
			String subscriberCode = "883068";
			String token = "testToken";

			//blank start end params
			HttpServletRequest mockRequest = mock(HttpServletRequest.class);
			when(mockRequest.getMethod()).thenReturn("GET");
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest.getParameter(HlsManifestModifierFilter.START)).thenReturn("1709926082");
			when(mockRequest.getParameter(HlsManifestModifierFilter.END)).thenReturn("1709926087");
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_ID)).thenReturn(subscriberId);
			when(mockRequest.getParameter(WebSocketConstants.SUBSCRIBER_CODE)).thenReturn(subscriberCode);
			when(mockRequest.getParameter(WebSocketConstants.TOKEN)).thenReturn(token);

			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);

			//verify that the response is written with captor
			byte[] modifiedResponseContent = responseWrapper.getContentAsByteArray();
			String modifiedM3u8= new String(modifiedResponseContent, StandardCharsets.UTF_8);

			//System.out.println(modifiedM3u8);

			assertFalse(modifiedM3u8.contains("test000000001.ts"));
			assertTrue(modifiedM3u8.contains("test000000002.ts?subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertTrue(modifiedM3u8.contains("test000000003.ts?subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertTrue(modifiedM3u8.contains("test000000004.ts?subscriberCode=883068&subscriberId=testSubscriber&token=testToken"));
			assertFalse(modifiedM3u8.contains("test000000005.ts"));

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	@Test
	public void testFilterWithRedirection() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);
			
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

			AppSettings appSettings = new AppSettings();
			appSettings.setHttpForwardingBaseURL("forward_url");
			
			doReturn(appSettings).when(hlsManifestModifierFilter).getAppSettings();
			
			URL url = mock(URL.class);
			HttpURLConnection connection = mock(HttpURLConnection.class);
			
			doReturn(url).when(hlsManifestModifierFilter).createRedirectURL(anyString());

			when(url.openConnection()).thenReturn(connection);
			when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(testM3u8Query.getBytes()));
			
	
			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);

			byte[] modifiedResponseContent = responseWrapper.getContentAsByteArray();
			String modifiedM3u8 = new String(modifiedResponseContent, StandardCharsets.UTF_8);

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
	public void testFilterWithRedirectionWithHeadRequest() {
		try {
			HttpServletResponse mockResponse = mock(HttpServletResponse.class);
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(mockResponse);
			
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
			when(mockRequest.getMethod()).thenReturn("HEAD");
			when(mockRequest.getRequestURI()).thenReturn("/LiveApp/streams/test.m3u8");
			when(mockRequest.getParameter(HlsManifestModifierFilter.START)).thenReturn("1709926082");
			when(mockRequest.getParameter(HlsManifestModifierFilter.END)).thenReturn("1709926087");

			AppSettings appSettings = new AppSettings();
			appSettings.setHttpForwardingBaseURL("forward_url");
			
			doReturn(appSettings).when(hlsManifestModifierFilter).getAppSettings();
	
			hlsManifestModifierFilter.doFilter(mockRequest, responseWrapper, myChain);
			
			verify(mockResponse).sendRedirect("forward_url/streams/test.m3u8");

			

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ServletException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Test
	public void testNoDoubleAmps(){

		String test_token_add = "#EXTM3U\n"
				+ "#EXT-X-TARGETDURATION:4\n"
				+ "#EXT-X-VERSION:3\n"
				+ "#EXT-X-PART-INF:PART-TARGET=1.002000\n"
				+ "#EXT-X-MEDIA-SEQUENCE:565\n"
				+ "#EXT-X-PROGRAM-DATE-TIME:2025-04-11T12:25:45.757Z\n"
				+ "#EXTINF:3.98000,\n"
				+ "test__580.ts\n"
				+ "#EXTINF:4.04000,\n"
				+ "test__581.ts\n"
				+ "#EXT-X-PROGRAM-DATE-TIME:2025-04-11T12:25:53.777Z\n"
				+ "#EXTINF:3.98000,\n"
				+ "test__582.ts\n"
				+ "#EXT-X-PART:DURATION=0.98000,INDEPENDENT=YES,URI=\"test__lowlatency.m3u8?segment=test__583.1.ts\"\n"
				+ "#EXT-X-PART:DURATION=1.02000,URI=\"test__lowlatency.m3u8?segment=test__583.2.ts\"\n"
				+ "#EXT-X-PART:DURATION=1.00000,INDEPENDENT=YES,URI=\"test__lowlatency.m3u8?segment=test__583.3.ts\"\n"
				+ "#EXT-X-PART:DURATION=1.00000,URI=\"test__lowlatency.m3u8?segment=test__583.4.ts\"\n"
				+ "#EXTINF:4.00000,\n"
				+ "test__583.ts\n"
				+ "#EXT-X-PROGRAM-DATE-TIME:2025-04-11T12:26:01.757Z\n"
				+ "#EXT-X-PART:DURATION=1.00000,INDEPENDENT=YES,URI=\"test__lowlatency.m3u8?segment=test__584.1.ts\"\n"
				+ "#EXT-X-PART:DURATION=1.00000,URI=\"test__lowlatency.m3u8?segment=test__584.2.ts\"\n"
				+ "#EXT-X-PART:DURATION=1.00000,INDEPENDENT=YES,URI=\"test__lowlatency.m3u8?segment=test__584.3.ts\"\n"
				+ "#EXT-X-PART:DURATION=1.00000,URI=\"test__lowlatency.m3u8?segment=test__584.4.ts\"\n"
				+ "#EXTINF:4.00000,\n";


		String modified = hlsManifestModifierFilter.modifyManifestFileContent(test_token_add,"testtoken",null,null,hlsManifestModifierFilter.MANIFEST_FILE_REGEX);
		assertTrue(!modified.contains("&&"));
		assertTrue(modified.contains("segment=test__584.2.ts&token=testtoken"));
	}
	@Test
	public void testAddParamSeparator(){
		assertEquals(hlsManifestModifierFilter.addParamSeparator("test"),"?");
		assertEquals(hlsManifestModifierFilter.addParamSeparator("test?segment"),"&");

	}
	
	@Test
    public void testIsHLSIntervalQuery() {

        // ---- Case 1: Valid m3u8 + start + end → true ----
        {
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRequestURI()).thenReturn("/live/stream.m3u8");
            when(req.getParameter("start")).thenReturn("1");
            when(req.getParameter("end")).thenReturn("2");

            assertTrue(HlsManifestModifierFilter.isHLSIntervalQuery(req));
        }

        // ---- Case 2: URI does not end with .m3u8 → false ----
        {
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRequestURI()).thenReturn("/live/stream.txt");
            when(req.getParameter("start")).thenReturn("1");
            when(req.getParameter("end")).thenReturn("2");

            assertFalse(HlsManifestModifierFilter.isHLSIntervalQuery(req));
        }

        // ---- Case 3: Missing start param → false ----
        {
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRequestURI()).thenReturn("/live/stream.m3u8");
            when(req.getParameter("start")).thenReturn(null);
            when(req.getParameter("end")).thenReturn("2");

            assertFalse(HlsManifestModifierFilter.isHLSIntervalQuery(req));
        }

        // ---- Case 4: Missing end param → false ----
        {
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRequestURI()).thenReturn("/live/stream.m3u8");
            when(req.getParameter("start")).thenReturn("1");
            when(req.getParameter("end")).thenReturn(null);

            assertFalse(HlsManifestModifierFilter.isHLSIntervalQuery(req));
        }

        // ---- Case 5: Empty start → false ----
        {
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRequestURI()).thenReturn("/live/stream.m3u8");
            when(req.getParameter("start")).thenReturn("");
            when(req.getParameter("end")).thenReturn("2");

            assertFalse(HlsManifestModifierFilter.isHLSIntervalQuery(req));
        }

        // ---- Case 6: Empty end → false ----
        {
            HttpServletRequest req = mock(HttpServletRequest.class);
            when(req.getRequestURI()).thenReturn("/live/stream.m3u8");
            when(req.getParameter("start")).thenReturn("1");
            when(req.getParameter("end")).thenReturn("");

            assertFalse(HlsManifestModifierFilter.isHLSIntervalQuery(req));
        }
    }
}