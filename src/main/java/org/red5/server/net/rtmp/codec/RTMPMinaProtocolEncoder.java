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
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecException;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.red5.server.api.Red5;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mina protocol encoder for RTMP.
 */
public class RTMPMinaProtocolEncoder extends ProtocolEncoderAdapter {

    protected static Logger log = LoggerFactory.getLogger(RTMPMinaProtocolEncoder.class);

    private RTMPProtocolEncoder encoder = new RTMPProtocolEncoder();

    private int targetChunkSize = 2048;

    /** {@inheritDoc} */
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws ProtocolCodecException {
        // get the connection from the session
        String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
        log.trace("Session id: {}", sessionId);
        @SuppressWarnings("unchecked")
        IConnectionManager<RTMPConnection> connManager = (IConnectionManager<RTMPConnection>) ((WeakReference<?>) session.getAttribute(RTMPConnection.RTMP_CONN_MANAGER)).get();
        RTMPConnection conn = (RTMPConnection) connManager.getConnectionBySessionId(sessionId);
        if (conn != null) {
            // look for and compare the connection local; set it from the session
            RTMPConnection localConn = (RTMPConnection) Red5.getConnectionLocal();
            if (!conn.equals(localConn)) {
                if (localConn != null) {
                    log.debug("Connection local ({}) didn't match io session ({})", localConn.getSessionId(), sessionId);
                }
                // replace conn with the one from the session id lookup
                Red5.setConnectionLocal(conn);
            }
            Boolean interrupted = false;
            Semaphore lock = conn.getEncoderLock();
            try {
                // acquire the encoder lock
                //log.trace("Encoder lock acquiring.. {}", conn.getSessionId());
                lock.acquire();
                log.trace("Encoder lock acquired {}", conn.getSessionId());
                // get the buffer
                final IoBuffer buf = message instanceof IoBuffer ? (IoBuffer) message : encoder.encode(message);
                if (buf != null) {
                    int requestedWriteChunkSize = conn.getState().getWriteChunkSize();
                    log.trace("Requested chunk size: {} target chunk size: {}", requestedWriteChunkSize, targetChunkSize);
                    if (buf.remaining() <= targetChunkSize * 2) {
                        log.trace("Writing output data");
                        out.write(buf);
                    } else {
                        int sentChunks = Chunker.chunkAndWrite(out, buf, requestedWriteChunkSize, targetChunkSize);
                        log.trace("Wrote {} chunks", sentChunks);
                    }
                } else {
                    log.trace("Response buffer was null after encoding");
                }
            } catch (InterruptedException ex) {
                log.error("InterruptedException during encode", ex);
                interrupted = true;
            } catch (Exception ex) {
                log.error("Exception during encode", ex);
            } finally {
                log.trace("Encoder lock releasing.. {}", conn.getSessionId());
                lock.release();
                if (interrupted && log.isInfoEnabled()) {
                    log.info("Released lock after interruption. session {}, permits {}", conn.getSessionId(), lock.availablePermits());
                }
            }
            // set connection local back to previous value
            if (localConn != null) {
                Red5.setConnectionLocal(localConn);
            }
        } else {
            log.debug("Connection is no longer available for encoding, may have been closed already");
        }
    }

    /**
     * Sets an RTMP protocol encoder
     * 
     * @param encoder
     *            the RTMP encoder
     */
    public void setEncoder(RTMPProtocolEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Returns an RTMP encoder
     * 
     * @return RTMP encoder
     */
    public RTMPProtocolEncoder getEncoder() {
        return encoder;
    }

    /**
     * Setter for baseTolerance
     * 
     * @param baseTolerance
     *            base tolerance
     */
    public void setBaseTolerance(long baseTolerance) {
        encoder.setBaseTolerance(baseTolerance);
    }

    /**
     * Setter for dropLiveFuture
     * 
     * @param dropLiveFuture
     *            drop live future
     */
    public void setDropLiveFuture(boolean dropLiveFuture) {
        encoder.setDropLiveFuture(dropLiveFuture);
    }

    /**
     * @return the targetChunkSize
     */
    public int getTargetChunkSize() {
        return targetChunkSize;
    }

    /**
     * @param targetChunkSize
     *            the targetChunkSize to set
     */
    public void setTargetChunkSize(int targetChunkSize) {
        this.targetChunkSize = targetChunkSize;
    }

    /**
     * Output data chunker.
     */
    private static final class Chunker {

        @SuppressWarnings("unused")
        public static LinkedList<IoBuffer> chunk(IoBuffer message, int chunkSize, int desiredSize) {
            LinkedList<IoBuffer> chunks = new LinkedList<IoBuffer>();
            int targetSize = desiredSize > chunkSize ? desiredSize : chunkSize;
            int limit = message.limit();
            do {
                int length = 0;
                int pos = message.position();
                while (length < targetSize && pos < limit) {
                    byte basicHeader = message.get(pos);
                    length += getDataSize(basicHeader) + chunkSize;
                    pos += length;
                }
                int remaining = message.remaining();
                log.trace("Length: {} remaining: {} pos+len: {} limit: {}", new Object[] { length, remaining, (message.position() + length), limit });
                if (length > remaining) {
                    length = remaining;
                }
                // add a chunk
                chunks.add(message.getSlice(length));
            } while (message.hasRemaining());
            return chunks;
        }

        public static int chunkAndWrite(ProtocolEncoderOutput out, IoBuffer message, int chunkSize, int desiredSize) {
            int sentChunks = 0;
            int targetSize = desiredSize > chunkSize ? desiredSize : chunkSize;
            int limit = message.limit();
            do {
                int length = 0;
                int pos = message.position();
                while (length < targetSize && pos < limit) {
                    byte basicHeader = message.get(pos);
                    length += getDataSize(basicHeader) + chunkSize;
                    pos += length;
                }
                int remaining = message.remaining();
                log.trace("Length: {} remaining: {} pos+len: {} limit: {}", new Object[] { length, remaining, (message.position() + length), limit });
                if (length > remaining) {
                    length = remaining;
                }
                // send it
                out.write(message.getSlice(length));
                sentChunks++;
            } while (message.hasRemaining());
            return sentChunks;
        }

        private static int getDataSize(byte basicHeader) {
            final int streamId = basicHeader & 0x0000003F;
            final int headerType = (basicHeader >> 6) & 0x00000003;
            int size = 0;
            switch (headerType) {
                case 0:
                    size = 12;
                    break;
                case 1:
                    size = 8;
                    break;
                case 2:
                    size = 4;
                    break;
                default:
                    size = 1;
                    break;
            }
            if (streamId == 0) {
                size += 1;
            } else if (streamId == 1) {
                size += 2;
            }
            return size;
        }
    }

}
