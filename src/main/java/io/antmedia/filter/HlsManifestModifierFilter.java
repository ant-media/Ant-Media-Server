package io.antmedia.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.amazonaws.util.StringUtils;
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
		if (HttpMethod.GET.equals(method) && httpRequest.getRequestURI().endsWith("m3u8")) {
			
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
					chain.doFilter(request, responseWrapper);

					int status = responseWrapper.getStatus();
					if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST) {
						try {
							final byte[] originalData = responseWrapper.getContentAsByteArray();
							String original = new String(originalData, StandardCharsets.UTF_8);
							
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
						} catch (Exception e) {
							logger.error("Error processing m3u8 filter:", e);
							// Write the original response content if there's an exception
							responseWrapper.copyBodyToResponse();
						}
					} else {
						responseWrapper.copyBodyToResponse(); // Pass-through in case of error status
					}
				}
			} 
			finally 
			{
				// Ensure the response body is copied back after all modifications
				responseWrapper.copyBodyToResponse();
			}
		} 
		else 
		{
			chain.doFilter(httpRequest, response);
		}
	}

	private void addSecurityParametersToSegmentUrls(String token, String subscriberId, String subscriberCode, ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {

		addSecurityParametersToURLs(token, subscriberId, subscriberCode, request, response, chain, SEGMENT_FILE_REGEX);
	}

	public void addSecurityParametersToAdaptiveM3u8File(String token, String subscriberId, String subscriberCode, ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
		addSecurityParametersToURLs(token, subscriberId, subscriberCode, request, response, chain, MANIFEST_FILE_REGEX);
	}

	public void addSecurityParametersToURLs(String token, String subscriberId, String subscriberCode,
											ServletRequest request, ServletResponse response, FilterChain chain,
											String regex) throws IOException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

		try {
			chain.doFilter(request, responseWrapper);
			int status = responseWrapper.getStatus();

			if (status >= HttpServletResponse.SC_OK && status <= HttpServletResponse.SC_BAD_REQUEST) {
				byte[] originalData = responseWrapper.getContentAsByteArray();
				String original = new String(originalData);

				String modifiedContent = modifyManifestFileContent(original, token, subscriberId, subscriberCode, regex);
				response.setContentLength(modifiedContent.length());
				response.getOutputStream().write(modifiedContent.getBytes());
				response.getOutputStream().flush();
			}
		} catch (Exception e) {
			logger.error(ExceptionUtils.getStackTrace(e));
			// In case of an error, revert to the original response content
			byte[] originalContent = responseWrapper.getContentAsByteArray();
			response.setContentLength(originalContent.length);
			response.getOutputStream().write(originalContent);
			response.flushBuffer();
		}
	}

	private String modifyManifestFileContent(String original, String token, String subscriberId, String subscriberCode, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(original);

		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			String replacementString = matcher.group();

			if (replacementString.contains("?")) {
				replacementString += "&";
			}
			else {
				replacementString += "?";
			}

			if (!StringUtils.isNullOrEmpty(subscriberCode)) {
				replacementString += WebSocketConstants.SUBSCRIBER_CODE + "=" + subscriberCode;
			}

			if (!StringUtils.isNullOrEmpty(subscriberId)) {
				replacementString += "&" + WebSocketConstants.SUBSCRIBER_ID + "=" + subscriberId;
			}

			if (!StringUtils.isNullOrEmpty(token)) {
				replacementString += "&" + WebSocketConstants.TOKEN + "=" + token;
			}

			matcher.appendReplacement(result, replacementString);
		}
		matcher.appendTail(result);

        return result.toString();
	}
}