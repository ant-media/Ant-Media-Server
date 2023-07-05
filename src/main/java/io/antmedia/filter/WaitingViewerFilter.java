package io.antmedia.filter;

import io.antmedia.datastore.db.types.Broadcast;
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

public class WaitingViewerFilter extends AbstractFilter{


    protected static Logger logger = LoggerFactory.getLogger(WaitingViewerFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest httpRequest =(HttpServletRequest)request;
        final String method = httpRequest.getMethod();
        if(HttpMethod.HEAD.equals(method) && (httpRequest.getRequestURI().endsWith("m3u8") || httpRequest.getRequestURI().endsWith("m4s"))){
            final String streamId = TokenFilterManager.getStreamId(httpRequest.getRequestURI());
            final Broadcast broadcast = getBroadcast((HttpServletRequest) request, streamId);
            if(broadcast != null){
                final String broadcastStatus = broadcast.getStatus();

                if(broadcast.isAutoStartStopEnabled() && (broadcastStatus.equals(IAntMediaStreamHandler.BROADCAST_STATUS_STOPPED) || broadcastStatus.equals(IAntMediaStreamHandler.BROADCAST_STATUS_FINISHED))){
                    final int status = ((HttpServletResponse) response).getStatus();
                    if (HttpServletResponse.SC_OK <= status && status <= HttpServletResponse.SC_NOT_FOUND)
                    {
                        getAntMediaApplicationAdapter().autoStartBroadcast(broadcast);
                    }

                }
            }
        }

        chain.doFilter(request, response);

    }
}
