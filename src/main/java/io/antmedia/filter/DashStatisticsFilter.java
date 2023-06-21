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
import io.antmedia.statistic.DashViewerStats;
import io.antmedia.statistic.IStreamStats;

public class DashStatisticsFilter extends AbstractFilter {
	
	protected static Logger logger = LoggerFactory.getLogger(DashStatisticsFilter.class);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {


		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if (HttpMethod.GET.equals(method) && httpRequest.getRequestURI().endsWith("m4s")) {
			//only accept GET methods
			String sessionId = httpRequest.getSession().getId();

			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());
			String subscriberId = ((HttpServletRequest) request).getParameter("subscriberId");
			Broadcast broadcast = getBroadcast((HttpServletRequest)request, streamId);
			if(broadcast != null 
					&& broadcast.getDashViewerLimit() != -1
					&& broadcast.getDashViewerCount() >= broadcast.getDashViewerLimit()) {
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Viewer Limit Reached");
				return;
			}
		
			chain.doFilter(request, response);

			int status = ((HttpServletResponse) response).getStatus();

			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST && streamId != null) 
			{				
				logger.debug("req ip {} session id {} stream id {} status {}", request.getRemoteHost(), sessionId, streamId, status);
				IStreamStats stats = getStreamStats(DashViewerStats.BEAN_NAME);
				if (stats != null) {
					stats.registerNewViewer(streamId, sessionId, subscriberId);
					
				}
			}
		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}

}