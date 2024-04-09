package io.antmedia.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;
import jakarta.ws.rs.HttpMethod;

public abstract class StatisticsFilter extends AbstractFilter {

	protected static Logger logger = LoggerFactory.getLogger(StatisticsFilter.class);


	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if (HttpMethod.GET.equals(method) && isFilterMatching(httpRequest.getRequestURI())) {
			//only accept GET methods
			String sessionId = httpRequest.getSession().getId();

			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());
			String subscriberId = ((HttpServletRequest) request).getParameter("subscriberId");

			if (isViewerCountExceeded((HttpServletRequest) request, (HttpServletResponse) response, streamId)) { 
				logger.info("Number of viewers limits has exceeded so it's returning forbidden for streamId:{} and class:{}", streamId, getClass().getSimpleName());
				return; 
			}

			chain.doFilter(request, response);

			int status = ((HttpServletResponse) response).getStatus();

			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST && streamId != null)
			{
				logger.debug("req ip {} session id {} stream id {} status {}", request.getRemoteHost(), sessionId, streamId, status);
				IStreamStats stats = getStreamStats(getBeanName());
				if (stats != null) {
					stats.registerNewViewer(streamId, sessionId, subscriberId);

				}
			}
			startStreamingIfAutoStartStopEnabled((HttpServletRequest) request, streamId);

		}
		else if (HttpMethod.HEAD.equals(method) && isFilterMatching(httpRequest.getRequestURI())) {
			String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());

			chain.doFilter(request, response);

			startStreamingIfAutoStartStopEnabled((HttpServletRequest) request, streamId);

		}
		else {
			chain.doFilter(httpRequest, response);


		}

	}

	public void startStreamingIfAutoStartStopEnabled(HttpServletRequest request, String streamId) {
		//start if it's not found, it may be started 
		Broadcast broadcast = getBroadcast(request, streamId);
		if (broadcast != null && broadcast.isAutoStartStopEnabled() && !AntMediaApplicationAdapter.isStreaming(broadcast)) 
		{
			//startStreaming method starts streaming if stream is not streaming in local or in any node in the cluster
			logger.info("http play request(hls, dash) is received for stream id:{} and it's not streaming, so it's trying to start the stream", streamId);
			getAntMediaApplicationAdapter().startStreaming(broadcast);
		}

	}



	public abstract boolean isViewerCountExceeded(HttpServletRequest request, HttpServletResponse response, String streamId) throws IOException;


	public abstract boolean isFilterMatching(String requestURI);

	public abstract String getBeanName();
}