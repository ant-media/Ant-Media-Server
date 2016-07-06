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

package org.red5.server.net.proxy;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.WritableByteChannel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.SocketSessionConfig;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

public class DebugProxyHandler extends IoHandlerAdapter implements ResourceLoaderAware {

    protected static Logger log = LoggerFactory.getLogger(DebugProxyHandler.class);

    private ResourceLoader loader;

    private ProtocolCodecFactory codecFactory;

    private InetSocketAddress forward;

    private String dumpTo = "./dumps/";

    /** {@inheritDoc} */
    public void setResourceLoader(ResourceLoader loader) {
        this.loader = loader;
    }

    /**
     * Setter for property 'codecFactory'.
     *
     * @param codecFactory
     *            Value to set for property 'codecFactory'.
     */
    public void setCodecFactory(ProtocolCodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    /**
     * Setter for property 'forward'.
     *
     * @param forward
     *            Value to set for property 'forward'.
     */
    public void setForward(String forward) {
        int split = forward.indexOf(':');
        String host = forward.substring(0, split);
        int port = Integer.parseInt(forward.substring(split + 1, forward.length()));
        this.forward = new InetSocketAddress(host, port);
    }

    /**
     * Setter for property 'dumpTo'.
     *
     * @param dumpTo
     *            Value to set for property 'dumpTo'.
     */
    public void setDumpTo(String dumpTo) {
        this.dumpTo = dumpTo;
    }

    /** {@inheritDoc} */
    @Override
    public void sessionOpened(IoSession session) throws Exception {
        SocketSessionConfig ssc = (SocketSessionConfig) session.getConfig();
        ssc.setTcpNoDelay(true);
        super.sessionOpened(session);
    }

    /** {@inheritDoc} */
    @Override
    public void sessionCreated(IoSession session) throws Exception {
        boolean isClient = session.getRemoteAddress().equals(forward);
        if (log.isDebugEnabled()) {
            log.debug("Is downstream: " + isClient);
            session.getFilterChain().addFirst("protocol", new ProtocolCodecFilter(codecFactory));
        }
        session.getFilterChain().addFirst("proxy", new ProxyFilter(isClient ? "client" : "server"));
        String fileName = System.currentTimeMillis() + '_' + forward.getHostName() + '_' + forward.getPort() + '_' + (isClient ? "DOWNSTREAM" : "UPSTREAM");
        File headersFile = loader.getResource(dumpTo + fileName + ".cap").getFile();
        headersFile.createNewFile();
        File rawFile = loader.getResource(dumpTo + fileName + ".raw").getFile();
        rawFile.createNewFile();
        FileOutputStream headersFos = null;
        FileOutputStream rawFos = null;
        try {
            headersFos = new FileOutputStream(headersFile);
            rawFos = new FileOutputStream(rawFile);
            WritableByteChannel headers = headersFos.getChannel();
            WritableByteChannel raw = rawFos.getChannel();
            IoBuffer header = IoBuffer.allocate(1);
            header.put((byte) (isClient ? 0x00 : 0x01));
            header.flip();
            headers.write(header.buf());
            session.getFilterChain().addFirst("dump", new NetworkDumpFilter(headers, raw));
        } finally {
            if (headersFos != null) {
                headersFos.close();
            }
            if (rawFos != null) {
                rawFos.close();
            }
        }
        //session.getFilterChain().addLast("logger", new LoggingFilter() );
        if (!isClient) {
            log.debug("Connecting..");
            IoConnector connector = new NioSocketConnector();
            connector.setHandler(this);
            ConnectFuture future = connector.connect(forward);
            future.awaitUninterruptibly(); // wait for connect, or timeout
            if (future.isConnected()) {
                if (log.isDebugEnabled()) {
                    log.debug("Connected: {}", forward);
                }
                IoSession client = future.getSession();
                client.setAttribute(ProxyFilter.FORWARD_KEY, session);
                session.setAttribute(ProxyFilter.FORWARD_KEY, client);
            }
        }
        super.sessionCreated(session);
    }

    /** {@inheritDoc} */
    @Override
    public void messageReceived(IoSession session, Object in) {
        if (log.isDebugEnabled()) {
            if (in instanceof IoBuffer) {
                log.debug("Handskake");
                return;
            }
            try {
                final Packet packet = (Packet) in;
                final Object message = packet.getMessage();
                final Header source = packet.getHeader();
                log.debug("{}", source);
                log.debug("{}", message);
            } catch (RuntimeException e) {
                log.error("Exception", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Exception caught", cause);
        }
    }

}
