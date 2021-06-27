/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.util;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for using HTTP connections.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class HttpConnectionUtil {

    private static Logger log = LoggerFactory.getLogger(HttpConnectionUtil.class);

    private static final String userAgent = "Mozilla/4.0 (compatible; Red5 Server)";

    private static PoolingHttpClientConnectionManager connectionManager;

    private static int connectionTimeout = 7000;

    static {
        // Create an HttpClient with the PoolingHttpClientConnectionManager.
        // This connection manager must be used if more than one thread will
        // be using the HttpClient.
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(40);
    }

    /**
     * Returns a client with all our selected properties / params.
     * 
     * @return client
     */
    public static final HttpClient getClient() {
        return getClient(connectionTimeout);
    }

    /**
     * Returns a client with all our selected properties / params.
     * 
     * @param timeout
     *            - socket timeout to set
     * @return client
     */
    public static final HttpClient getClient(int timeout) {
        HttpClientBuilder client = HttpClientBuilder.create();
        // set the connection manager
        client.setConnectionManager(connectionManager);
        // dont retry
        client.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        // establish a connection within x seconds
        RequestConfig config = RequestConfig.custom().setSocketTimeout(timeout).build();
        client.setDefaultRequestConfig(config);
        // no redirects
        client.disableRedirectHandling();
        // set custom ua
        client.setUserAgent(userAgent);
        // set the proxy if the user has one set
        if ((System.getProperty("http.proxyHost") != null) && (System.getProperty("http.proxyPort") != null)) {
            HttpHost proxy = new HttpHost(System.getProperty("http.proxyHost").toString(), Integer.valueOf(System.getProperty("http.proxyPort")));
            client.setProxy(proxy);
        }
        return client.build();
    }

    /**
     * Returns a client with all our selected properties / params and SSL enabled.
     * 
     * @return client
     */
    public static final HttpClient getSecureClient() {
        HttpClientBuilder client = HttpClientBuilder.create();
        // set the ssl verifier to accept all
        client.setSSLHostnameVerifier(new NoopHostnameVerifier());
        // set the connection manager
        client.setConnectionManager(connectionManager);
        // dont retry
        client.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
        // establish a connection within x seconds
        RequestConfig config = RequestConfig.custom().setSocketTimeout(connectionTimeout).build();
        client.setDefaultRequestConfig(config);
        // no redirects
        client.disableRedirectHandling();
        // set custom ua
        client.setUserAgent(userAgent);
        return client.build();
    }

    /**
     * Logs details about the request error.
     * 
     * @param response
     *            http response
     * @throws IOException
     *             on IO error
     * @throws ParseException
     *             on parse error
     */
    public static void handleError(HttpResponse response) throws ParseException, IOException {
        log.debug("{}", response.getStatusLine().toString());
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            log.debug("{}", EntityUtils.toString(entity));
        }
    }

    /**
     * @return the connectionTimeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * @param connectionTimeout
     *            the connectionTimeout to set
     */
    public void setConnectionTimeout(int connectionTimeout) {
        HttpConnectionUtil.connectionTimeout = connectionTimeout;
    }

}
