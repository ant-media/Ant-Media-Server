package io.antmedia.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

			// Resolve the viewer identity and any old fingerprint that needs cleanup
			String[] identity = resolveViewerIdentity(httpRequest, httpResponse);
			String sessionId = identity[0];
			String oldFingerprint = identity[1];

			if (isViewerCountExceeded(httpRequest, httpResponse, streamId)) { 
				logger.info("Number of viewers limits has exceeded so it's returning forbidden for streamId:{} and class:{}", streamId, getClass().getSimpleName());
				return; 
			}

			chain.doFilter(request, response);

			int status = httpResponse.getStatus();

			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST && streamId != null)
			{
				logger.debug("req ip {} viewer id {} stream id {} status {}", request.getRemoteHost(), sessionId, streamId, status);
				IStreamStats stats = getStreamStats(getBeanName());
				if (stats != null) {
					// Clean up old fingerprint entry if transitioning to cookie-based identity
					if (oldFingerprint != null) {
						stats.removeViewerEntry(streamId, oldFingerprint);
					}
					stats.registerNewViewer(streamId, sessionId, subscriberId);
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
	 *   <li>{@code subscriberId} query parameter — used as-is</li>
	 *   <li>{@code viewerId} cookie — contains {@code uuid|fingerprint}; the UUID is used as identity
	 *       and the fingerprint is returned for cleanup</li>
	 *   <li>SHA-256 fingerprint of client IP + User-Agent + Accept-Language — fallback;
	 *       a new {@code viewerId} cookie is set for subsequent requests</li>
	 * </ol>
	 *
	 * @param request  the incoming HTTP request
	 * @param response the HTTP response (used to set the cookie on first visit)
	 * @return a two-element array: [0] = viewer identity key, [1] = old fingerprint to remove (or null)
	 */
	static String[] resolveViewerIdentity(HttpServletRequest request, HttpServletResponse response) {
		String subscriberId = request.getParameter("subscriberId");
		if (subscriberId != null && !subscriberId.isEmpty()) {
			return new String[] { subscriberId, null };
		}

		String cookieValue = getViewerIdCookie(request);
		String fingerprint = computeFingerprint(request);

		if (cookieValue != null && cookieValue.contains(COOKIE_SEPARATOR)) {
			int separatorIndex = cookieValue.indexOf(COOKIE_SEPARATOR);
			String uuid = cookieValue.substring(0, separatorIndex);
			String cookieFingerprint = cookieValue.substring(separatorIndex + 1);

			if (!uuid.isEmpty()) {
				// Cookie is valid — use UUID as identity.
				// Always return the stored fingerprint for cleanup because the
				// first visit (before the cookie existed) registered the viewer
				// under that fingerprint.  It must be removed regardless of
				// whether the client's IP/UA has changed since then.
				return new String[] { uuid, cookieFingerprint };
			}
		}

		// No cookie or invalid cookie — use fingerprint and set cookie for next time
		String uuid = UUID.randomUUID().toString();
		setViewerIdCookie(request, response, uuid + COOKIE_SEPARATOR + fingerprint);
		return new String[] { fingerprint, null };
	}

	/**
	 * Reads the {@code viewerId} cookie from the request.
	 */
	static String getViewerIdCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (VIEWER_ID_COOKIE_NAME.equals(cookie.getName())) {
					return cookie.getValue();
				}
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
			byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(64);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			logger.warn("SHA-256 not available, using raw fingerprint");
			return raw;
		}
	}

	public abstract boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException;


	public abstract boolean isFilterMatching(String requestURI);

	public abstract String getBeanName();
}
