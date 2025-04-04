package io.antmedia.valves;

import java.io.IOException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.analytic.model.PlayerStatsEvent;
import io.antmedia.filter.TokenFilterManager;
import io.antmedia.logger.LoggerUtils;
import io.antmedia.rest.RestServiceBase;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.HttpMethod;

/**
 * This class just logs the data transfered for http requests to 
 * @author mekya
 *
 */
public class DataTransferValve extends ValveBase {
	
	
	private Logger logger = LoggerFactory.getLogger(DataTransferValve.class);

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

		getNext().invoke(request, response);
		
		String streamId = TokenFilterManager.getStreamId(request.getRequestURI());
		String method = request.getMethod();

		if (StringUtils.isNotBlank(streamId) && (HttpMethod.GET.equals(method) || HttpMethod.HEAD.equals(method))) 
		{
			
			ConfigurableWebApplicationContext context = (ConfigurableWebApplicationContext) request.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);

			if (context == null || !context.isRunning()) {
				logger.debug("Context is not ready yet for request: {}", request.getRequestURI());
				return;
			}
			
			String subscriberId = ((HttpServletRequest) request).getParameter("subscriberId");

			if (subscriberId != null) {
				subscriberId = subscriberId.replaceAll(RestServiceBase.REPLACE_CHARS, "_");
			}
			
			String clientIP = request.getRemoteAddr().replaceAll(RestServiceBase.REPLACE_CHARS, "_");

			long bytesWritten = response.getBytesWritten(false);
			

			PlayerStatsEvent playerStatsEvent = new PlayerStatsEvent();
			playerStatsEvent.setStreamId(streamId);
			playerStatsEvent.setUri(request.getRequestURI());
			playerStatsEvent.setSubscriberId(subscriberId);
			playerStatsEvent.setApp(((AntMediaApplicationAdapter)context.getBean(AntMediaApplicationAdapter.BEAN_NAME)).getScope().getName());
			playerStatsEvent.setByteTransferred(bytesWritten);
			playerStatsEvent.setClientIP(clientIP);
			
			
			log(playerStatsEvent);
			
		}
	}
	
	public void log(PlayerStatsEvent playerStatsEvent) {
		LoggerUtils.logAnalyticsFromServer(playerStatsEvent);
	}

}
