/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for using HTTP connections.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire (mondain@gmail.com)
 */
@SuppressWarnings("deprecation")
public class HttpConnectionUtil {

	private static Logger log = LoggerFactory.getLogger(HttpConnectionUtil.class);

	private static final String userAgent = "Mozilla/4.0 (compatible; Red5 Server)";
	
	private static ThreadSafeClientConnManager connectionManager;
	
	private static int connectionTimeout = 7000;
	
	static {
		// Create an HttpClient with the ThreadSafeClientConnManager.
		// This connection manager must be used if more than one thread will
		// be using the HttpClient.
		connectionManager = new ThreadSafeClientConnManager();
		connectionManager.setMaxTotal(40);
	}

	/**
	 * Returns a client with all our selected properties / params.
	 * 
	 * @return client
	 */
	public static final DefaultHttpClient getClient() {
		// create a singular HttpClient object
		DefaultHttpClient client = new DefaultHttpClient(connectionManager);
		// dont retry
		client.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
		// get the params for the client
		HttpParams params = client.getParams();
		// establish a connection within x seconds
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, connectionTimeout);
		// no redirects
		params.setParameter(ClientPNames.HANDLE_REDIRECTS, false);
		// set custom ua
		params.setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
		// set the proxy if the user has one set
		if ((System.getProperty("http.proxyHost") != null) && (System.getProperty("http.proxyPort") != null)) {
            HttpHost proxy = new HttpHost(System.getProperty("http.proxyHost").toString(), Integer.valueOf(System.getProperty("http.proxyPort")));
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
		return client;
	}
	
	/**
	 * Logs details about the request error.
	 * 
	 * @param response
	 * @throws IOException 
	 * @throws ParseException 
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
	 * @param connectionTimeout the connectionTimeout to set
	 */
	public void setConnectionTimeout(int connectionTimeout) {
		HttpConnectionUtil.connectionTimeout = connectionTimeout;
	}
	
}
