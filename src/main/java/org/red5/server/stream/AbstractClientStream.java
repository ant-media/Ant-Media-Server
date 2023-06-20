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

package org.red5.server.stream;

import java.lang.ref.WeakReference;

import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract base for client streams
 */
public abstract class AbstractClientStream extends AbstractStream implements IClientStream {
    
	@Autowired(required=false)
    /**
     * Stream identifier. Unique across server.
     */
    private Number streamId = 0.0d;

    /**
     * Stream name of the broadcasting stream.
     */
    private String broadcastStreamPublishName;

    /**
     * Connection that works with streams
     */
    private WeakReference<IStreamCapableConnection> conn;

    /**
     * Buffer duration in ms as requested by the client
     */
    private int clientBufferDuration;

    /**
     * Setter for stream id
     * 
     * @param streamId
     *            Stream id
     */
    public void setStreamId(Number streamId) {
        this.streamId = streamId;
    }

    /**
     * Return stream id
     * 
     * @return Stream id
     */
    public Number getStreamId() {
        return streamId;
    }

    /**
     * Setter for stream capable connection
     * 
     * @param conn
     *            IStreamCapableConnection object
     */
    public void setConnection(IStreamCapableConnection conn) {
        this.conn = new WeakReference<IStreamCapableConnection>(conn);
    }

    /**
     * Return connection associated with stream
     * 
     * @return Stream capable connection object
     */
    public IStreamCapableConnection getConnection() {
        return conn != null ? conn.get() : null;
    }

    /** {@inheritDoc} */
    public void setClientBufferDuration(int duration) {
        clientBufferDuration = duration;
    }

    /**
     * Get duration in ms as requested by the client.
     *
     * @return value
     */
    public int getClientBufferDuration() {
        return clientBufferDuration;
    }

    /**
     * Sets the broadcasting streams name.
     * 
     * @param broadcastStreamPublishName
     *            name of the broadcasting stream
     */
    public void setBroadcastStreamPublishName(String broadcastStreamPublishName) {
        this.broadcastStreamPublishName = broadcastStreamPublishName;
    }

    /** {@inheritDoc} */
    public String getBroadcastStreamPublishName() {
        return broadcastStreamPublishName;
    }

}
