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

package org.red5.server.net.proxy;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy filter
 */
public class ProxyFilter extends IoFilterAdapter {
    /**
     * Forwarding key constant
     */
    public static final String FORWARD_KEY = "proxy_forward_key";

    /**
     * Logger
     */
    protected static Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    /**
     * Filter name
     */
    protected String name;

    /**
     * Create proxy filter with given name
     * 
     * @param name
     *            name
     */
    public ProxyFilter(String name) {
        this.name = name;
    }

    /** {@inheritDoc} */
    @Override
    public void messageReceived(NextFilter next, IoSession session, Object message) throws Exception {
        // Create forwarding IO session
        IoSession forward = (IoSession) session.getAttribute(FORWARD_KEY);
        if (forward != null && forward.isConnected()) {

            if (message instanceof IoBuffer) {
                final IoBuffer buf = (IoBuffer) message;
                if (log.isDebugEnabled()) {
                    log.debug("[{}] RAW >> {}", name, buf.getHexDump());
                }
                IoBuffer copy = IoBuffer.allocate(buf.limit());
                int limit = buf.limit();
                copy.put(buf);
                copy.flip();
                forward.write(copy);
                buf.flip();
                buf.position(0);
                buf.limit(limit);
            }

        }
        next.messageReceived(session, message);
    }

    /** {@inheritDoc} */
    @Override
    public void sessionClosed(NextFilter next, IoSession session) throws Exception {
        IoSession forward = (IoSession) session.getAttribute(FORWARD_KEY);
        if (forward != null && forward.isConnected() && !forward.isClosing()) {
            if (log.isDebugEnabled()) {
                log.debug("[{}] Closing: {}", name, forward);
            }
            forward.closeNow();
        }
        next.sessionClosed(session);
    }

}