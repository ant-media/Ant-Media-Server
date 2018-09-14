package io.antmedia.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.statistic.HlsViewerStats;
import io.antmedia.statistic.IStreamStats;

public class HlsStatisticsFilter implements javax.servlet.Filter {

	protected static Logger logger = LoggerFactory.getLogger(HlsStatisticsFilter.class);
	private IStreamStats streamStats;
	private FilterConfig filterConfig;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {


		HttpServletRequest httpRequest =(HttpServletRequest)request;

		String method = httpRequest.getMethod();
		if (method.equals("GET")) {
			//only accept GET methods
			String sessionId = httpRequest.getSession().getId();

		
			chain.doFilter(request, response);

			int status = ((HttpServletResponse) response).getStatus();
			
			if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_BAD_REQUEST) 
			{
				String streamId = TokenFilter.getStreamId(httpRequest.getRequestURI());
				
				if (streamId != null) {
					logger.info("session id {} stream id {} status {}", sessionId, streamId, status);
					getStreamStats().registerNewViewer(streamId, sessionId);
				}
			}
		}
		else {
			chain.doFilter(httpRequest, response);
		}

	}



	@Override
	public void destroy() {
		//There is no need to implement destroy right now
	}


	public IStreamStats getStreamStats() {
		if (streamStats == null) {
			ApplicationContext context = (ApplicationContext) filterConfig.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
			streamStats = (IStreamStats)context.getBean(HlsViewerStats.BEAN_NAME);

		}
		return streamStats;
	}

}
