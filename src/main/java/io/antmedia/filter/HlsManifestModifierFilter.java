package io.antmedia.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.HttpMethod;

import org.springframework.web.util.ContentCachingResponseWrapper;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;

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
			if(startDate == null || endDate == null || startDate.isBlank() || endDate.isBlank()) {
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


}