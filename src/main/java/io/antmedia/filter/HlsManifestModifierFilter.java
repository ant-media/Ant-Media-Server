package io.antmedia.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.util.StringUtils;

import io.antmedia.AppSettings;
import io.antmedia.rest.RestServiceBase;
import io.antmedia.websocket.WebSocketConstants;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.HttpMethod;

import org.springframework.web.util.ContentCachingResponseWrapper;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;

import static io.antmedia.muxer.MuxAdaptor.ADAPTIVE_SUFFIX;

public class HlsManifestModifierFilter extends AbstractFilter {

	public static final String START = "start";
	public static final String END = "end";
	//matches any words ending with .ts or .m4s and any query parameters
	public static final String SEGMENT_FILE_REGEX = "\\b\\S+\\.(ts|m4s)(\\?.*)?\\b";
	//matches any words ending with .m3u8 and any query parameters
	public static final String MANIFEST_FILE_REGEX = "\\b\\S+\\.m3u8(\\?.*)?\\b";
	protected static Logger logger = LoggerFactory.getLogger(HlsManifestModifierFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		String method = httpRequest.getMethod();
		if ((HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method)) && httpRequest.getRequestURI().endsWith("m3u8")) {

			//start date is in seconds since epoch
			String startDate = request.getParameter(START);
			//end date is in seconds since epoch
			String endDate = request.getParameter(END);
			String token = request.getParameter("token");
			String subscriberId = request.getParameter("subscriberId");
			String subscriberCode = request.getParameter("subscriberCode");

			boolean parameterExists = !StringUtils.isNullOrEmpty(token) ||
					!StringUtils.isNullOrEmpty(subscriberId) ||
					!StringUtils.isNullOrEmpty(subscriberCode);



			// Use ContentCachingResponseWrapper for modifications
			ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

			try 
			{
				// Proceed with adaptive and regular m3u8 handling
				if (httpRequest.getRequestURI().contains(ADAPTIVE_SUFFIX) && parameterExists) 
				{
					addSecurityParametersToAdaptiveM3u8File(token, subscriberId, subscriberCode, request, responseWrapper, chain);
				} 
				else if (StringUtils.isNullOrEmpty(startDate) || StringUtils.isNullOrEmpty(endDate)) 
				{
					if (!httpRequest.getRequestURI().contains(ADAPTIVE_SUFFIX) && parameterExists) 
					{
						addSecurityParametersToSegmentUrls(token, subscriberId, subscriberCode, request, responseWrapper, chain);
					} 
					else 
					{
						chain.doFilter(request, responseWrapper);
					}
				} 
				else 
				{
					// Handling of custom start/end time range for playlist segments

					long start = Long.parseLong(startDate);
					long end = Long.parseLong(endDate);

					String original = null;
					String redirectLocation = null;
					
					AppSettings appSettings = getAppSettings();
			        
					String requestURI = ((HttpServletRequest) request).getRequestURI();
				    if (requestURI != null && !requestURI.isEmpty() && appSettings != null) {
				    	String httpForwardingBaseURL = appSettings.getHttpForwardingBaseURL();
					    String httpForwardingExtension = "m3u8";
			
				    	redirectLocation = HttpForwardFilter.getRedirectUrl(requestURI, httpForwardingBaseURL, httpForwardingExtension);
				    }

					// Important information: 
				    // if HLSForwardFilter is active, this filter checks it and download the m3u8 files 
				    // from redirect location, modify and return it
				    
					if (redirectLocation != null) {
						redirectLocation = redirectLocation.replaceAll(RestServiceBase.REPLACE_CHARS, "_");
					    logger.info("HLS manifest file will be downloaded from redirect location:{}", redirectLocation);
				    	
					    if (HttpMethod.HEAD.equals(method)) {
					    	httpResponse.sendRedirect(redirectLocation);
					    	return; // return immediately after redirect
					    }
					    
				        // Make a new HTTP request to the redirect URL
				        URL url = createRedirectURL(redirectLocation);
				        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				        connection.setRequestMethod("GET");

				        // Read the response from the redirect URL
				        try (InputStream inputStream = connection.getInputStream();
				                ByteArrayOutputStream result = new ByteArrayOutputStream()) {

				               byte[] buffer = new byte[1024];
				               int length;
				               while ((length = inputStream.read(buffer)) != -1) {
				                   result.write(buffer, 0, length);
				               }

				               // Convert the result to a string
				               original = result.toString(StandardCharsets.UTF_8.name());
				               
						       responseWrapper.setStatus(HttpServletResponse.SC_OK); // Set status to 200 OK

				        }
				        catch (Exception e) {
						    logger.info("HLS manifest file cannot be downloaded from redirect location:{}", redirectLocation);
				        }
					} 
					else {
						chain.doFilter(request, responseWrapper);

						int status = responseWrapper.getStatus();
						if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST) {
							final byte[] originalData = responseWrapper.getContentAsByteArray();
							original = new String(originalData, StandardCharsets.UTF_8);
						}
					} 
					
					if(original != null) {
						MediaPlaylistParser parser = new MediaPlaylistParser();
						MediaPlaylist playList = parser.readPlaylist(original);
	
						List<MediaSegment> segments = new ArrayList<>();
						for (MediaSegment segment : playList.mediaSegments()) 
						{
							segment.programDateTime().ifPresent(dateTime -> 
							{
								long time = dateTime.toEpochSecond();
								if (time >= start && time <= end) 
								{
									segments.add(MediaSegment.builder()
											.duration(segment.duration())
											.uri(segment.uri())
											.build());
								}
							});
						}
	
						MediaPlaylist newPlayList = MediaPlaylist.builder()
								.version(playList.version())
								.targetDuration(playList.targetDuration())
								.ongoing(false)
								.addAllMediaSegments(segments)
								.build();
	
						String newData = new MediaPlaylistParser().writePlaylistAsString(newPlayList);
						if (parameterExists) {
							newData = modifyManifestFileContent(newData, token, subscriberId, subscriberCode, SEGMENT_FILE_REGEX);
						}
	
						// Write final modified data to response
						responseWrapper.resetBuffer(); // Clears any previous response data
						responseWrapper.getOutputStream().write(newData.getBytes(StandardCharsets.UTF_8));
						//copyBodyToResponse is called in finally block
					}
				}
			}
			catch (Exception e) {
				logger.error(ExceptionUtils.getStackTrace(e));
			}
			finally 
			{
				// Ensure the response body is copied back after all modifications
				//IT IS CALLED FOR ALL CASES
				responseWrapper.copyBodyToResponse();	
			}
		} 
		else 
		{
			chain.doFilter(httpRequest, response);
		}
	}

	public URL createRedirectURL(String redirectLocation) throws MalformedURLException {
		URL url = new URL(redirectLocation);
		return url;
	}
	
	private void addSecurityParametersToSegmentUrls(String token, String subscriberId, String subscriberCode, ServletRequest request, ContentCachingResponseWrapper response, FilterChain chain) throws IOException, ServletException {

		addSecurityParametersToURLs(token, subscriberId, subscriberCode, request, response, chain, SEGMENT_FILE_REGEX);
	}

	public void addSecurityParametersToAdaptiveM3u8File(String token, String subscriberId, String subscriberCode, ServletRequest request, ContentCachingResponseWrapper response, FilterChain chain) throws IOException, ServletException {
		addSecurityParametersToURLs(token, subscriberId, subscriberCode, request, response, chain, MANIFEST_FILE_REGEX);
	}

	public void addSecurityParametersToURLs(String token, String subscriberId, String subscriberCode,
			ServletRequest request, ContentCachingResponseWrapper responseWrapper, FilterChain chain,
			String regex) throws IOException, ServletException 
	{

		chain.doFilter(request, responseWrapper);
		int status = responseWrapper.getStatus();

		if (status >= HttpServletResponse.SC_OK && status <= HttpServletResponse.SC_BAD_REQUEST) {
			byte[] originalData = responseWrapper.getContentAsByteArray();
			String original = new String(originalData);

			String modifiedContent = modifyManifestFileContent(original, token, subscriberId, subscriberCode, regex);
			responseWrapper.resetBuffer();
			responseWrapper.getOutputStream().write(modifiedContent.getBytes());
			//responseWrapper.copyBodyToResponse() is called in finaally block
		}

	}

	public String addParamSeparator(String current) {
		if (current.contains("?")) {
			String lastChar = current.substring(current.length() - 1);
			if (lastChar.equals("&")){
				return "";
			}
			return "&";
		}
		return "?";
	}

	public String modifyManifestFileContent(String original, String token, String subscriberId, String subscriberCode,
			String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(original);

		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String replacementString = matcher.group();

			if (!StringUtils.isNullOrEmpty(subscriberCode)) {
				replacementString += (addParamSeparator(replacementString) + WebSocketConstants.SUBSCRIBER_CODE + "=" + subscriberCode);
			}

			if (!StringUtils.isNullOrEmpty(subscriberId)) {
				replacementString += (addParamSeparator(replacementString) + WebSocketConstants.SUBSCRIBER_ID + "="
						+ subscriberId);
			}

			if (!StringUtils.isNullOrEmpty(token)) {
				replacementString += (addParamSeparator(replacementString) + WebSocketConstants.TOKEN + "=" + token);
			}

			matcher.appendReplacement(result, replacementString);
		}
		matcher.appendTail(result);

		return result.toString();
	}
	
	public static boolean isHLSIntervalQuery(HttpServletRequest request) {
		String startDate = request.getParameter(START);
		String endDate = request.getParameter(END);
		return request.getRequestURI().endsWith("m3u8") 
				&& !StringUtils.isNullOrEmpty(startDate)
				&& !StringUtils.isNullOrEmpty(endDate);
	}
}
