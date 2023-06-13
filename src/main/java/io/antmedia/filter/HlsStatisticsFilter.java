package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;

public class HlsStatisticsFilter extends AbstractFilter {

	protected static Logger logger = LoggerFactory.getLogger(HlsStatisticsFilter.class);
	

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if (HttpMethod.GET.equals(method) && httpRequest.getRequestURI().endsWith("m3u8")) {
			//only accept GET methods
			String sessionId = httpRequest.getSession().getId();

			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());
			String subscriberId = ((HttpServletRequest) request).getParameter("subscriberId");

			if (isViewerCountExceeded((HttpServletRequest) request, (HttpServletResponse) response, streamId)) return;

			chain.doFilter(request, response);

			int status = ((HttpServletResponse) response).getStatus();

			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST && streamId != null) 
			{				
				logger.debug("req ip {} session id {} stream id {} status {}", request.getRemoteHost(), sessionId, streamId, status);
				IStreamStats stats = getStreamStats(HlsViewerStats.BEAN_NAME);
				if (stats != null) {
					stats.registerNewViewer(streamId, sessionId, subscriberId);
					
				}
			}
		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}
	
	

	public boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException {
		Broadcast broadcast = getBroadcast(request, streamId); 

		if(broadcast != null
				&& broadcast.getHlsViewerLimit() != -1
				&& broadcast.getHlsViewerCount() >= broadcast.getHlsViewerLimit()) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "Viewer Limit Reached");
			return true;
		}
		return false;
	}


}
