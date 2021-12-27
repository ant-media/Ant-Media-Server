package io.antmedia.rest;

import io.antmedia.AppSettings;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.IDataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.filter.AbstractFilter;
import io.antmedia.rest.servlet.EndpointProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class RestProxyFilter extends AbstractFilter {

    protected static Logger log = LoggerFactory.getLogger(RestProxyFilter.class);

    private DataStore dataStore;

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        String reqURI = httpReq.getRequestURI();
        AppSettings settings = getAppSettings();
        String streamId = getStreamId(reqURI);
        Broadcast broadcast = getDataStore().get(streamId);
        log.debug("STREAM ID = {} BROADCAST = {} ", streamId, broadcast);

        //If it is not related with the broadcast, we can skip this filter
        if (isInSameNodeInCluster(request.getRemoteAddr()) || broadcast == null) {
            chain.doFilter(request, response);
            return;
        }
        else{
            String originAdress = "http://" + broadcast.getOriginAdress() + ":5080/" + settings.getAppName() + "/rest";
            log.info("Redirecting request to origin {}", originAdress);
            EndpointProxy endpointProxy = new EndpointProxy();
            endpointProxy.initTarget(originAdress);
            endpointProxy.service(request, response);
            return;
        }
    }

    public String getStreamId(String reqURI){
        try{
            reqURI = reqURI.split("broadcasts/")[1];
        }
        catch (ArrayIndexOutOfBoundsException e){
            return reqURI;
        }
        if(reqURI.contains("/"))
            reqURI = reqURI.substring(0, reqURI.indexOf("/"));
        return reqURI;
    }

    public  boolean isInSameNodeInCluster(String originAddress) {
        ApplicationContext context = getAppContext();
        boolean isCluster = context.containsBean(IClusterNotifier.BEAN_NAME);
        return !isCluster || originAddress.equals(getServerSetting().getHostAddress());
    }

    public DataStore getDataStore() {
        if(dataStore == null) {
            ApplicationContext context = getAppContext();
            if(context != null){
                dataStore = ((DataStoreFactory) context.getBean(IDataStoreFactory.BEAN_NAME)).getDataStore();
            }
            else{
                log.error("RestProxyFilter is not initialized because context returns null");
            }
        }
        return dataStore;
    }

}