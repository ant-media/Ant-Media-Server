/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

package org.red5.server.tomcat;

import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11Nio2Protocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;
import org.bouncycastle.util.Arrays;
import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model object to contain a connector, socket address, and connection properties for a Tomcat connection.
 * 
 * @author Paul Gregoire
 */
public class TomcatConnector {

	private static Logger log = LoggerFactory.getLogger(TomcatConnector.class);

	private Connector connector;

	private Map<String, String> connectionProperties = new HashMap<>();

	private String protocol = "org.apache.coyote.http11.Http11NioProtocol";

	private InetSocketAddress address;

	private int redirectPort = 443;

	private boolean useIPVHosts = true;

	private String URIEncoding = "UTF-8";

	private boolean secure;

	private boolean initialized;

	private boolean upgradeHttp2Protocol;

	public void init() {
		try 
		{
			// create a connector
			connector = new Connector(protocol);
			connector.setRedirectPort(redirectPort);
			connector.setUseIPVHosts(useIPVHosts);
			connector.setURIEncoding(URIEncoding);
			// set the bind address to local if we dont have an address property
			if (address == null) {
				address = bindLocal(connector.getPort());
			}
			// set port
			connector.setPort(address.getPort());

			// set connection properties
			if (connectionProperties != null) {
				for (String key : connectionProperties.keySet()) {
					connector.setProperty(key, connectionProperties.get(key));
				}
			}
			//TODO
			//HTTP/2 is not compatible with multipart post requests
			//https://bz.apache.org/bugzilla/show_bug.cgi?id=65051
			
			if (upgradeHttp2Protocol) {
				connector.addUpgradeProtocol(new Http2Protocol());
			}
			AprLifecycleListener listener = new AprLifecycleListener();
			listener.setUseOpenSSL(true);
			listener.setSSLEngine("off");
			connector.addLifecycleListener(listener);
			// determine if https support is requested

			if (secure) {
				// set connection properties
				listener.setSSLEngine("on");

				connector.setSecure(true);
				connector.setScheme("https");


				SSLHostConfig sslHostConfig = new SSLHostConfig();

				String enabledProtocols = connectionProperties.get("sslEnabledProtocols");
				if (enabledProtocols != null) {
					sslHostConfig.setEnabledProtocols(enabledProtocols.split(","));
				}
				log.info("sslEnabledProtocols:{} and ciphers: {}",enabledProtocols, connectionProperties.get("ciphers"));

				String ciphers = connectionProperties.get("ciphers");
				if (ciphers != null) 
					sslHostConfig.setCiphers(ciphers);

				String useServerCipherSuitesOrder = connectionProperties.get("useServerCipherSuitesOrder");
				if (useServerCipherSuitesOrder != null) 
					sslHostConfig.setHonorCipherOrder(Boolean.valueOf(useServerCipherSuitesOrder));

				String clientAuth = connectionProperties.get("clientAuth");
				if (clientAuth != null) 
					sslHostConfig.setCertificateVerification(clientAuth);

				SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.RSA);

				//SSLCertificateFile
				String sSLCertificateFile = connectionProperties.get("SSLCertificateFile");
				if (sSLCertificateFile != null) 
					certificate.setCertificateFile(sSLCertificateFile);
				else
					log.warn("No certificate file configured for SSL");

				//SSLCertificateChainFile
				String sSLCertificateChainFile = connectionProperties.get("SSLCertificateChainFile");
				if (sSLCertificateChainFile != null) 
					certificate.setCertificateChainFile(sSLCertificateChainFile);
				else
					log.warn("No certificate chain file configured for SSL");

				
				//SSLCertificateKeyFile
				String sSLCertificateKeyFile = connectionProperties.get("SSLCertificateKeyFile");
				if (sSLCertificateKeyFile != null) 
					certificate.setCertificateKeyFile(sSLCertificateKeyFile);
				else
					log.warn("No certificate key file configured for SSL");
				

				sslHostConfig.addCertificate(certificate);

				connector.addSslHostConfig(sslHostConfig);

			}
			// apply the bind address to the handler
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof Http11Nio2Protocol) {
				((Http11Nio2Protocol)handler).setAddress(address.getAddress());
			} else if (handler instanceof Http11NioProtocol) {
				((Http11NioProtocol)handler).setAddress(address.getAddress());
			}


			// set initialized flag
			initialized = true;
		} 
		catch (Exception t) {
			log.error("Exception during connector creation", t);
		}
	}

	/**
	 * Returns a local address and port.
	 * 
	 * @param port
	 * @return InetSocketAddress
	 */
	private InetSocketAddress bindLocal(int port) throws Exception {
		return new InetSocketAddress("127.0.0.1", port);
	}

	/**
	 * @return the connector
	 */
	public Connector getConnector() {
		if (!initialized) {
			init();
		}
		return connector;
	}

	/**
	 * Set connection properties for the connector.
	 * 
	 * @param props
	 *            connection properties to set
	 */
	public void setConnectionProperties(Map<String, String> props) {
		this.connectionProperties.putAll(props);
	}

	/**
	 * @return the connectionProperties
	 */
	public Map<String, String> getConnectionProperties() {
		return connectionProperties;
	}

	/**
	 * @param protocol
	 *            the protocol to set
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * @param useIPVHosts
	 *            the useIPVHosts to set
	 */
	public void setUseIPVHosts(boolean useIPVHosts) {
		this.useIPVHosts = useIPVHosts;
	}

	/**
	 * @param uRIEncoding
	 *            the uRIEncoding to set
	 */
	public void setURIEncoding(String uRIEncoding) {
		URIEncoding = uRIEncoding;
	}

	/**
	 * The address and port to which we will bind the connector. If the port is not supplied the default of 5080 will be used. The address and port are to be separated by a colon ':'.
	 * 
	 * @param addressAndPort
	 */
	public void setAddress(String addressAndPort) {
		try {
			String addr = "0.0.0.0";
			int port = 5080;
			if (addressAndPort != null && addressAndPort.indexOf(':') != -1) {
				String[] parts = addressAndPort.split(":");
				addr = parts[0];
				port = Integer.valueOf(parts[1]);
			}
			this.address = new InetSocketAddress(addr, port);
		} catch (Exception e) {
			log.warn("Exception configuring address", e);
		}
	}

	/**
	 * @return the socket address as string
	 */
	public String getAddress() {
		return String.format("%s:%d", address.getHostName(), address.getPort());
	}

	/**
	 * @return the socket address
	 */
	public InetSocketAddress getSocketAddress() {
		return address;
	}

	/**
	 * @param redirectPort
	 *            the redirectPort to set
	 */
	public void setRedirectPort(int redirectPort) {
		this.redirectPort = redirectPort;
	}

	/**
	 * @return the secure
	 */
	public boolean isSecure() {
		return secure;
	}

	/**
	 * @param secure
	 *            the secure to set
	 */
	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "TomcatConnector [connector=" + connector + ", connectionProperties=" + connectionProperties + ", address=" + address + "]";
	}

	public void setUpgradeHttp2Protocol(boolean upgradeHttp2Protocol) {
		this.upgradeHttp2Protocol = upgradeHttp2Protocol;
	}

	public boolean isInitialized() {
		return initialized;
	}
	
	

}
