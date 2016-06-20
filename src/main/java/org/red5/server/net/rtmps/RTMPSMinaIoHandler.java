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

package org.red5.server.net.rtmps;

import java.io.File;
import java.io.NotActiveException;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.KeyStoreFactory;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.apache.mina.filter.ssl.SslFilter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Native RTMPS protocol events fired by the MINA framework.
 * 
 * <pre>
 * var nc:NetConnection = new NetConnection();
 * nc.proxyType = "best";
 * nc.connect("rtmps:\\localhost\app");
 * </pre>
 * 
 * https://issues.apache.org/jira/browse/DIRMINA-272 https://issues.apache.org/jira/browse/DIRMINA-997
 * 
 * Transport Layer Security (TLS) Renegotiation Issue http://www.oracle.com/technetwork/java/javase/documentation/tlsreadme2-176330.html
 * Secure renegotiation https://jce.iaik.tugraz.at/sic/Products/Communication-Messaging-Security/iSaSiLk/documentation/Secure-Renegotiation
 * Troubleshooting a HTTPS TLSv1 handshake http://integr8consulting.blogspot.com/2012/02/troubleshooting-https-tlsv1-handshake.html
 * How to analyze Java SSL errors http://www.smartjava.org/content/how-analyze-java-ssl-errors
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
     * Names of the SSL cipher suites which are currently enabled for use.
     */
    private String[] cipherSuites;

    /**
     * Names of the protocol versions which are currently enabled for use.
     */
    private String[] protocols;

    /**
     * Use client (or server) mode when handshaking.
     */
    private boolean useClientMode;

    /**
     * Request the need of client authentication.
     */
    private boolean needClientAuth;

    /**
     * Indicates that we would like to authenticate the client but if client certificates are self-signed or have no certificate chain then
     * we are still good
     */
    private boolean wantClientAuth;

    static {
        // add bouncycastle security provider
        Security.addProvider(new BouncyCastleProvider());
        if (log.isTraceEnabled()) {
            Provider[] providers = Security.getProviders();
            for (Provider provider : providers) {
                log.trace("Provider: {} = {}", provider.getName(), provider.getInfo());
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        log.debug("Session created: RTMPS");
        if (keystoreFile == null || truststoreFile == null) {
            throw new NotActiveException("Keystore or truststore are null");
        }
        // create the ssl context
        SSLContext sslContext = null;
        try {
            log.debug("Keystore: {}", keystoreFile);
            File keyStore = new File(keystoreFile);
            log.trace("Keystore - read: {} path: {}", keyStore.canRead(), keyStore.getCanonicalPath());
            log.debug("Truststore: {}", truststoreFile);
            File trustStore = new File(truststoreFile);
            log.trace("Truststore - read: {} path: {}", trustStore.canRead(), trustStore.getCanonicalPath());
            if (keyStore.exists() && trustStore.exists()) {
                // keystore
                final KeyStoreFactory keyStoreFactory = new KeyStoreFactory();
                keyStoreFactory.setDataFile(keyStore);
                keyStoreFactory.setPassword(keystorePassword);
                // truststore
                final KeyStoreFactory trustStoreFactory = new KeyStoreFactory();
                trustStoreFactory.setDataFile(trustStore);
                trustStoreFactory.setPassword(truststorePassword);
                // ssl context factory
                final SslContextFactory sslContextFactory = new SslContextFactory();
                //sslContextFactory.setProtocol("TLS");
                // get keystore
                final KeyStore ks = keyStoreFactory.newInstance();
                sslContextFactory.setKeyManagerFactoryKeyStore(ks);
                // get truststore
                final KeyStore ts = trustStoreFactory.newInstance();
                sslContextFactory.setTrustManagerFactoryKeyStore(ts);
                sslContextFactory.setKeyManagerFactoryKeyStorePassword(keystorePassword);
                // get ssl context
                sslContext = sslContextFactory.newInstance();
                log.debug("SSL provider is: {}", sslContext.getProvider());
                // get ssl context parameters
                SSLParameters params = sslContext.getDefaultSSLParameters();
                if (log.isDebugEnabled()) {
                    log.debug("SSL context params - need client auth: {} want client auth: {} endpoint id algorithm: {}", params.getNeedClientAuth(), params.getWantClientAuth(), params.getEndpointIdentificationAlgorithm());
                    String[] supportedProtocols = params.getProtocols();
                    for (String protocol : supportedProtocols) {
                        log.debug("SSL context supported protocol: {}", protocol);
                    }
                }
                // compatibility: remove the SSLv2Hello message in the available protocols - some systems will fail 
                // to handshake if TSLv1 messages are enwrapped with SSLv2 messages, Java 6 tries to send TSLv1 embedded in SSLv2
            } else {
                log.warn("Keystore or Truststore file does not exist");
            }
        } catch (Exception ex) {
            log.error("Exception getting SSL context", ex);
        }
        // create the ssl filter using server mode
        SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.setUseClientMode(useClientMode);
        sslFilter.setNeedClientAuth(needClientAuth);
        sslFilter.setWantClientAuth(wantClientAuth);
        if (cipherSuites != null) {
            sslFilter.setEnabledCipherSuites(cipherSuites);
        }
        if (protocols != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using these protocols: {}", Arrays.toString(protocols));
            }
            sslFilter.setEnabledProtocols(protocols);
        }
        // the filter chain for this session
        IoFilterChain chain = session.getFilterChain();
        // add ssl first
        chain.addFirst("sslFilter", sslFilter);
        // use notification messages
        session.setAttribute(SslFilter.USE_NOTIFICATION, Boolean.TRUE);
        log.debug("isSslStarted: {}", sslFilter.isSslStarted(session));
        //if (log.isTraceEnabled()) {
        //    chain.addLast("logger", new LoggingFilter());
        //}
        // add rtmps filter
        session.getFilterChain().addAfter("sslFilter", "rtmpsFilter", new RTMPSIoFilter());
        // create a connection
        RTMPMinaConnection conn = createRTMPMinaConnection();
        // add session to the connection
        conn.setIoSession(session);
        // add the handler
        conn.setHandler(handler);
        // add the connections session id for look up using the connection manager
        session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
        // create an inbound handshake
        InboundHandshake handshake = new InboundHandshake();
        // set whether or not unverified will be allowed
        handshake.setUnvalidatedConnectionAllowed(((RTMPHandler) handler).isUnvalidatedConnectionAllowed()); 
        // add the in-bound handshake, defaults to non-encrypted mode
        session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, handshake);
    }

    /**
     * Password used to access the keystore file.
     * 
     * @param password
     *            keystore password
     */
    public void setKeystorePassword(String password) {
        this.keystorePassword = password;
    }

    /**
     * Password used to access the truststore file.
     * 
     * @param password
     *            truststore password
     */
    public void setTruststorePassword(String password) {
        this.truststorePassword = password;
    }

    /**
     * Set keystore data from a file.
     * 
     * @param path
     *            contains keystore
     */
    public void setKeystoreFile(String path) {
        this.keystoreFile = path;
    }

    /**
     * Set truststore file path.
     * 
     * @param path
     *            contains truststore
     */
    public void setTruststoreFile(String path) {
        this.truststoreFile = path;
    }

    public String[] getCipherSuites() {
        return cipherSuites;
    }

    public void setCipherSuites(String[] cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public String[] getProtocols() {
        return protocols;
    }

    public void setProtocols(String[] protocols) {
        this.protocols = protocols;
    }

    public void setUseClientMode(boolean useClientMode) {
        this.useClientMode = useClientMode;
    }

    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }

}
