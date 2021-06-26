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

package org.red5.server.net.rtmp.codec;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolDecoderAdapter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.red5.server.api.Red5;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP protocol decoder.
 */
public class RTMPMinaProtocolDecoder extends ProtocolDecoderAdapter {

    protected static Logger log = LoggerFactory.getLogger(RTMPMinaProtocolDecoder.class);

    private RTMPProtocolDecoder decoder = new RTMPProtocolDecoder();

    /** {@inheritDoc} */
    public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws ProtocolCodecException {
        // get the connection from the session
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.trace("Session id: {}", sessionId);
        // connection verification routine
        @SuppressWarnings("unchecked")
        IConnectionManager<RTMPConnection> connManager = (IConnectionManager<RTMPConnection>) ((WeakReference<?>) session.getAttribute(RTMPConnection.RTMP_CONN_MANAGER)).get();
        RTMPConnection conn = (RTMPConnection) connManager.getConnectionBySessionId(sessionId);
        RTMPConnection connLocal = (RTMPConnection) Red5.getConnectionLocal();
        if (connLocal == null || !conn.getSessionId().equals(connLocal.getSessionId())) {
            if (log.isDebugEnabled() && connLocal != null) {
                log.debug("Connection local didn't match session");
            }
        }
        if (conn != null) {
            // set the connection to local if its referred to by this session
            Red5.setConnectionLocal(conn);
            // copy data range from incoming
            if (log.isTraceEnabled()) {
                log.trace("Incoming: in.position {}, in.limit {}, in.remaining {}", new Object[] { in.position(), in.limit(), in.remaining() });
            }
            byte[] arr = new byte[in.remaining()];
            in.get(arr);
            // create a buffer and store it on the session
            IoBuffer buf = (IoBuffer) session.getAttribute("buffer");
            if (buf == null) {
                buf = IoBuffer.allocate(arr.length);
                buf.setAutoExpand(true);
                session.setAttribute("buffer", buf);
            }
            // copy incoming into buffer
            buf.put(arr);
            // flip so we can read
            buf.flip();
            if (log.isTraceEnabled()) {
                //log.trace("Buffer before: {}", Hex.encodeHexString(arr));
                log.trace("Buffers info before: buf.position {}, buf.limit {}, buf.remaining {}", new Object[] { buf.position(), buf.limit(), buf.remaining() });
            }
            // get the connections decoder lock
            final Semaphore lock = conn.getDecoderLock();
            try {
                // acquire the decoder lock
                lock.acquire();
                // construct any objects from the decoded bugger
                List<?> objects = decoder.decodeBuffer(conn, buf);
                log.trace("Decoded: {}", objects);
                if (objects != null) {
                    int writeCount = 0;
                    for (Object object : objects) {
                        out.write(object);
                        writeCount++;
                    }
                    log.trace("Wrote {} objects", writeCount);
                }
            } catch (Exception e) {
                log.error("Error during decode", e);
            } finally {
                lock.release();
                // clear local
                Red5.setConnectionLocal(null);
            }
            if (log.isTraceEnabled()) {
                //log.trace("Buffer after: {}", Hex.encodeHexString(Arrays.copyOfRange(buf.array(), buf.position(), buf.limit())));
                log.trace("Buffers info after: buf.position {}, buf.limit {}, buf.remaining {}", new Object[] { buf.position(), buf.limit(), buf.remaining() });
            }
        } else {
            log.debug("Closing and skipping decode for unregistered connection: {}", sessionId);
            session.closeNow();
            log.debug("Session closing: {} reading: {} writing: {}", session.isClosing(), session.isReadSuspended(), session.isWriteSuspended());
        }
    }

    /**
     * Sets the RTMP protocol decoder.
     * 
     * @param decoder
     *            RTMP decoder
     */
    public void setDecoder(RTMPProtocolDecoder decoder) {
        this.decoder = decoder;
    }

    /**
     * Returns an RTMP decoder.
     * 
     * @return RTMP decoder
     */
    public RTMPProtocolDecoder getDecoder() {
        return decoder;
    }

}
