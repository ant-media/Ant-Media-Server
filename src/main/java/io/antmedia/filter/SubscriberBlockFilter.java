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

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Subscriber;
import jakarta.ws.rs.HttpMethod;

public class SubscriberBlockFilter extends AbstractFilter{

	/**
	 * We have this filter because we block subscriber according to the subscriberId and streamId.
	 * In other words, subscriber can be blocked even if TOTP is not enabled
	 */

    protected static Logger logger = LoggerFactory.getLogger(SubscriberBlockFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest =(HttpServletRequest)request;
        final String method = httpRequest.getMethod();
        
        
        if(HttpMethod.GET.equals(method) && (httpRequest.getRequestURI().endsWith("m3u8") 
        		|| httpRequest.getRequestURI().endsWith("ts") 
        		|| httpRequest.getRequestURI().endsWith("m4s")
        		|| httpRequest.getRequestURI().endsWith("mpd")))
        {
            final String subscriberId = request.getParameter("subscriberId");
            final String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());
            final DataStore dataStore = getDataStore();
            final Broadcast broadcast = getBroadcast(httpRequest, streamId);
            final Subscriber subscriber = dataStore.getSubscriber(streamId, subscriberId);

            if (broadcast == null || subscriber == null) {
                chain.doFilter(request, response);
                return;
            }

            if (!subscriber.isBlocked(Subscriber.PLAY_TYPE)) {
                chain.doFilter(request, response);
            } else {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Subscriber is blocked");
            }
        } else {
            chain.doFilter(request, response);
        }

    }
}