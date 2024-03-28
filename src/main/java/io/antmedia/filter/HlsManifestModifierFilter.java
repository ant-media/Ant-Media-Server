package io.antmedia.filter;

import java.io.ByteArrayOutputStream;
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

import org.springframework.util.StreamUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;

import static io.antmedia.muxer.MuxAdaptor.ADAPTIVE_SUFFIX;

public class HlsManifestModifierFilter extends AbstractFilter {

	public static final String START = "start";
	public static final String END = "end";
	protected static Logger logger = LoggerFactory.getLogger(HlsManifestModifierFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if (HttpMethod.GET.equals(method) && httpRequest.getRequestURI().endsWith("m3u8")) {
			//only accept GET methods
			String startDate = request.getParameter(START);
			String endDate = request.getParameter(END);
			String token = request.getParameter("token");
			String subscriberId = request.getParameter("subscriberId");
			String subscriberCode = request.getParameter("subscriberCode");

			if(httpRequest.getRequestURI().contains(ADAPTIVE_SUFFIX) &&
					(!StringUtils.isNullOrEmpty(subscriberId) ||
					!StringUtils.isNullOrEmpty(subscriberCode) ||
					!StringUtils.isNullOrEmpty(token))){
				addSecurityParametersToAdaptiveM3u8File(token, subscriberId, subscriberCode, request, response, chain);
			}

			if(StringUtils.isNullOrEmpty(startDate) || StringUtils.isNullOrEmpty(endDate)) {
				chain.doFilter(httpRequest, response);
			}
			else {
				long start = Long.parseLong(startDate);
				long end = Long.parseLong(endDate);

				ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper((HttpServletResponse) response);

				chain.doFilter(request, responseWrapper);

				int status = responseWrapper.getStatus();

				if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST) 
				{				
					try {
						// Get the original response data
						final byte[] originalData = responseWrapper.getContentAsByteArray();
						String original = new String(originalData);

						MediaPlaylistParser parser = new MediaPlaylistParser();
						MediaPlaylist playList = parser.readPlaylist(original);

						List<MediaSegment> segments = new ArrayList<>();

						for (MediaSegment segment : playList.mediaSegments()) {
							segment.programDateTime().ifPresent(dateTime -> {
								long time = dateTime.toEpochSecond();
								if(time >= start && time <= end) {
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

						// Modify the original data
						MediaPlaylistParser parser2 = new MediaPlaylistParser();

						final String newData = parser2.writePlaylistAsString(newPlayList);

						// Write the data into the output stream
						response.setContentLength(newData.length());
						response.getOutputStream().write(newData.getBytes());

						// Commit the written data
						response.getWriter().flush();


					} catch (Exception e) {
						response.setContentLength(responseWrapper.getContentSize());
						response.getOutputStream().write(responseWrapper.getContentAsByteArray());
						response.flushBuffer();

					}
				}
			}
		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}

	public void addSecurityParametersToAdaptiveM3u8File(String token, String subscriberId, String subscriberCode, ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);

		try {
			chain.doFilter(request, responseWrapper);
			int status = responseWrapper.getStatus();

			if (status >= HttpServletResponse.SC_OK && status <= HttpServletResponse.SC_BAD_REQUEST) {
				byte[] originalData = responseWrapper.getContentAsByteArray();
				String original = new String(originalData);

				String regex = "\\b\\S+\\.m3u8\\b";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(original);

				StringBuffer result = new StringBuffer();
				while (matcher.find()) {
					String replacementString = matcher.group() + "?";

					if (!StringUtils.isNullOrEmpty(subscriberCode)) {
						replacementString += WebSocketConstants.SUBSCRIBER_CODE + "=" + subscriberCode;
					}

					if (!StringUtils.isNullOrEmpty(subscriberId)) {
						replacementString += "&"+ WebSocketConstants.SUBSCRIBER_ID + "=" + subscriberId;
					}

					if (!StringUtils.isNullOrEmpty(token)) {
						replacementString += "&"+ WebSocketConstants.TOKEN + "=" + token;
					}

					matcher.appendReplacement(result, replacementString);
				}
				matcher.appendTail(result);

				String modifiedContent = result.toString();
				response.setContentLength(modifiedContent.length());
				response.getOutputStream().write(modifiedContent.getBytes());
				response.getWriter().flush();
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


}