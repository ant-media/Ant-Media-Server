package io.antmedia.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.ws.rs.HttpMethod;

public abstract class StatisticsFilter extends AbstractFilter {

	protected static Logger logger = LoggerFactory.getLogger(StatisticsFilter.class);

	static final String VIEWER_ID_COOKIE_NAME = "viewerId";
	static final String COOKIE_SEPARATOR = "|";


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest =(HttpServletRequest)request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		String method = httpRequest.getMethod();
		if (HttpMethod.GET.equals(method) && isFilterMatching(httpRequest.getRequestURI())) {

			String hlsSegmentFileSuffixFormat = "";
			AppSettings appSettings = getAppSettings();
			if(appSettings != null) {
				hlsSegmentFileSuffixFormat = appSettings.getHlsSegmentFileSuffixFormat();
			}

			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI(), hlsSegmentFileSuffixFormat);
			String subscriberId = httpRequest.getParameter("subscriberId");

			//resolve the viewer identity and the fingerprint key it has to be migrated from, if any
			String[] identity = resolveViewerIdentity(httpRequest, httpResponse);
			String viewerId = identity[0];
			String previousViewerId = identity[1];

			if (isViewerCountExceeded(httpRequest, httpResponse, streamId)) { 
				logger.info("Number of viewers limits has exceeded so it's returning forbidden for streamId:{} and class:{}", streamId, getClass().getSimpleName());
				return; 
			}

			chain.doFilter(request, response);

			int status = httpResponse.getStatus();

			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST && streamId != null)
			{
				logger.debug("req ip {} viewer id {} stream id {} status {}", request.getRemoteHost(), viewerId, streamId, status);
				IStreamStats stats = getStreamStats(getBeanName());
				if (stats != null) {
					//this viewer was first counted under its fingerprint, move that entry over to the
					//cookie based id instead of registering the same client a second time
					if (previousViewerId != null) {
						stats.migrateViewerEntry(streamId, previousViewerId, viewerId);
					}
					stats.registerNewViewer(streamId, viewerId, subscriberId);
				}
			}
			startStreamingIfAutoStartStopEnabled(httpRequest, streamId);

		}
		else if (HttpMethod.HEAD.equals(method) && isFilterMatching(httpRequest.getRequestURI())) {
			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI(), getAppSettings().getHlsSegmentFileSuffixFormat());

			chain.doFilter(request, response);

			startStreamingIfAutoStartStopEnabled(httpRequest, streamId);

		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}

	public void startStreamingIfAutoStartStopEnabled(HttpServletRequest request, String streamId) {
		Broadcast broadcast = getBroadcast(request, streamId);
		if (broadcast != null && broadcast.isAutoStartStopEnabled() && !AntMediaApplicationAdapter.isStreaming(broadcast.getStatus())) 
		{
			logger.info("http play request(hls, dash) is received for stream id:{} and it's not streaming, so it's trying to start the stream", streamId);
			getAntMediaApplicationAdapter().startStreaming(broadcast);
		}
	}

	/**
	 * Resolves the viewer identity from the request.
	 * <p>
	 * Priority:
	 * <ol>
	 *   <li>{@code viewerId} cookie — {@code uuid} for a viewer that is already counted under its
	 *       uuid, or {@code uuid|fingerprint} on the single request where the cookie comes back for
	 *       the first time. In that second case the fingerprint is returned so the entry created for
	 *       it can be migrated, and the cookie is rewritten to the plain {@code uuid} form so the
	 *       migration is requested once and not on every following segment request</li>
	 *   <li>SHA-256 fingerprint of client IP + User-Agent + Accept-Language — used when there is no
	 *       usable cookie</li>
	 * </ol>
	 * <p>
	 * The {@code subscriberId} parameter is deliberately not used as the identity, a subscriber
	 * watching from two devices is two viewers. It is reported separately to the statistics.
	 *
	 * @param request  the incoming HTTP request
	 * @param response the HTTP response (used to set the cookie)
	 * @return a two-element array: [0] = viewer identity key, [1] = fingerprint key to migrate from (or null)
	 */
	static String[] resolveViewerIdentity(HttpServletRequest request, HttpServletResponse response) {
		String cookieValue = getViewerIdCookie(request);
		if (cookieValue != null && !cookieValue.isEmpty()) {
			int separatorIndex = cookieValue.indexOf(COOKIE_SEPARATOR);
			if (separatorIndex < 0) {
				return new String[] { cookieValue, null };
			}

			String uuid = cookieValue.substring(0, separatorIndex);
			if (!uuid.isEmpty()) {
				//first request carrying the cookie back, the viewer is still counted under the
				//fingerprint. Drop the fingerprint from the cookie so this is a one shot migration
				setViewerIdCookie(request, response, uuid);

				String previousViewerId = cookieValue.substring(separatorIndex + 1);
				return new String[] { uuid, previousViewerId.isEmpty() ? null : previousViewerId };
			}
		}

		//no usable cookie, count this viewer under its fingerprint and hand out a cookie so the
		//following requests can be attributed to this client alone. Only playlist requests hand it
		//out, a client that cannot return the cookie would otherwise be issued a new one for every
		//single segment request
		String fingerprint = computeFingerprint(request);
		if (isPlaylistRequest(request.getRequestURI())) {
			setViewerIdCookie(request, response, UUID.randomUUID() + COOKIE_SEPARATOR + fingerprint);
		}
		return new String[] { fingerprint, null };
	}

	/**
	 * True for the playlist of a stream (m3u8/mpd), false for its segments.
	 */
	static boolean isPlaylistRequest(String requestURI) {
		return requestURI != null && (requestURI.endsWith("m3u8") || requestURI.endsWith("mpd"));
	}

	/**
	 * Reads the {@code viewerId} cookie from the request.
	 */
	static String getViewerIdCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (VIEWER_ID_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	/**
	 * Sets the {@code viewerId} cookie on the response.
	 * <p>
	 * On HTTPS: sets {@code Secure; SameSite=None} so the cookie is sent on cross-origin requests.
	 * On HTTP: sets {@code SameSite=Lax} so the cookie works for same-origin requests.
	 */
	static void setViewerIdCookie(HttpServletRequest request, HttpServletResponse response, String value) {
		boolean secure = request.isSecure()
				|| "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));

		StringBuilder sb = new StringBuilder();
		sb.append(VIEWER_ID_COOKIE_NAME).append("=").append(value);
		sb.append("; Path=/");
		sb.append("; HttpOnly");
		if (secure) {
			sb.append("; Secure");
			sb.append("; SameSite=None");
		} else {
			sb.append("; SameSite=Lax");
		}

		response.addHeader("Set-Cookie", sb.toString());
	}

	/**
	 * Computes a SHA-256 fingerprint from the client's IP, User-Agent, and Accept-Language.
	 * <p>
	 * Only called for a request that carries no usable {@code viewerId} cookie, so once per viewer
	 * rather than once per segment request.
	 */
	static String computeFingerprint(HttpServletRequest request) {
		String xff = request.getHeader("X-Forwarded-For");
		String ip = (xff != null && !xff.isEmpty()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
		String ua = request.getHeader("User-Agent");
		String lang = request.getHeader("Accept-Language");

		String raw = (ip != null ? ip : "") + "|"
				+ (ua != null ? ua : "") + "|"
				+ (lang != null ? lang : "");

		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			logger.warn("SHA-256 not available, using raw fingerprint");
			return raw;
		}
	}

	public abstract boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException;


	public abstract boolean isFilterMatching(String requestURI);

	public abstract String getBeanName();
}
