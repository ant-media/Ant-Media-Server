package io.antmedia.filter;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.muxer.IAntMediaStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import java.io.IOException;

public class SubscriberBlockFilter extends AbstractFilter{


    protected static Logger logger = LoggerFactory.getLogger(SubscriberBlockFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest =(HttpServletRequest)request;
        final String method = httpRequest.getMethod();
        
        
        if(HttpMethod.GET.equals(method) && (httpRequest.getRequestURI().endsWith("m3u8") || httpRequest.getRequestURI().endsWith("m4s")))
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
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Subscriber unauthorized");
            }
        } else {
            chain.doFilter(request, response);
        }

    }
}