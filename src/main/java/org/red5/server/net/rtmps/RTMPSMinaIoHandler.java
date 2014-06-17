/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2014 by respective authors (see below). All rights reserved.
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

package org.red5.server.net.rtmps;

import java.io.File;
import java.io.NotActiveException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.ssl.KeyStoreFactory;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Native RTMPS protocol events fired by the MINA framework.
 * <pre>
 * var nc:NetConnection = new NetConnection();
 * nc.proxyType = "best";
 * nc.connect("rtmps:\\localhost\app");
 * </pre>
 * Originally created by: Kevin Green
 *  
 *  http://tomcat.apache.org/tomcat-6.0-doc/ssl-howto.html
 *  http://java.sun.com/j2se/1.5.0/docs/guide/security/CryptoSpec.html#AppA
 *  http://java.sun.com/j2se/1.5.0/docs/api/java/security/KeyStore.html
 *  http://tomcat.apache.org/tomcat-3.3-doc/tomcat-ssl-howto.html
 *  
 * @author Kevin Green (kevygreen@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPSMinaIoHandler extends RTMPMinaIoHandler {

	private static Logger log = LoggerFactory.getLogger(RTMPSMinaIoHandler.class);

	/**
	 * Password for accessing the keystore.
	 */
	private String keystorePassword;
	
	/**
	 * Password for accessing the truststore.
	 */
	private String truststorePassword;

	/**
	 * Stores the keystore path.
	 */
	private String keystoreFile;

	/**
	 * Stores the truststore path.
	 */
	private String truststoreFile;

	/**
	 * The keystore type, valid options are JKS and PKCS12
	 */
	@SuppressWarnings("unused")
	private String keyStoreType = "JKS";

	/** {@inheritDoc} */
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.trace("Session created");
		if (keystoreFile == null || truststoreFile == null) {
			throw new NotActiveException("Keystore or truststore are null");
		}
		//create the ssl filter
		SslFilter sslFilter = getSslFilter();
		session.getFilterChain().addFirst("sslFilter", sslFilter);
		// END OF NATIVE SSL STUFF	
		// add rtmpe filter after ssl
		session.getFilterChain().addAfter("sslFilter", "rtmpeFilter", new RTMPEIoFilter());
		// add protocol filter next
		session.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(codecFactory));
		// create a connection
		RTMPMinaConnection conn = createRTMPMinaConnection();
		// add session to the connection
		conn.setIoSession(session);
		// add the handler
		conn.setHandler(handler);
		// add the connections session id for look up using the connection manager
		session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
		// add the in-bound handshake
		session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, new InboundHandshake());
	}	

	public SslFilter getSslFilter() throws Exception {
		SSLContext context = getSslContext();
		// create the ssl filter using server mode
		SslFilter sslFilter = new SslFilter(context);
		return sslFilter;
	}

	private SSLContext getSslContext() {
		SSLContext sslContext = null;
		try {
			File keyStore = new File(keystoreFile);
			File trustStore = new File(truststoreFile);
			if (keyStore.exists() && trustStore.exists()) {
				final KeyStoreFactory keyStoreFactory = new KeyStoreFactory();
				keyStoreFactory.setDataFile(keyStore);
				keyStoreFactory.setPassword(keystorePassword);

				final KeyStoreFactory trustStoreFactory = new KeyStoreFactory();
				trustStoreFactory.setDataFile(trustStore);
				trustStoreFactory.setPassword(truststorePassword);

				final SslContextFactory sslContextFactory = new SslContextFactory();
				final KeyStore ks = keyStoreFactory.newInstance();
				sslContextFactory.setKeyManagerFactoryKeyStore(ks);

				final KeyStore ts = trustStoreFactory.newInstance();
				sslContextFactory.setTrustManagerFactoryKeyStore(ts);
				sslContextFactory.setKeyManagerFactoryKeyStorePassword(keystorePassword);
				sslContext = sslContextFactory.newInstance();
				log.debug("SSL provider is: {}", sslContext.getProvider());
			} else {
				log.warn("Keystore or Truststore file does not exist");
			}
		} catch (Exception ex) {
			log.error("Exception getting SSL context", ex);
		}
		return sslContext;
	}

	/**
	 * Password used to access the keystore file.
	 * 
	 * @param password
	 */
	public void setKeystorePassword(String password) {
		this.keystorePassword = password;
	}

	/**
	 * Password used to access the truststore file.
	 * 
	 * @param password
	 */
	public void setTruststorePassword(String password) {
		this.truststorePassword = password;
	}	
	
	/**
	 * Set keystore data from a file.
	 * 
	 * @param path contains keystore
	 */
	public void setKeystoreFile(String path) {
		this.keystoreFile = path;
	}

	/**
	 * Set truststore file path.
	 * 
	 * @param path contains truststore
	 */
	public void setTruststoreFile(String path) {
		this.truststoreFile = path;
	}

	/**
	 * Set the key store type, JKS or PKCS12.
	 * 
	 * @param keyStoreType
	 */
	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

}
