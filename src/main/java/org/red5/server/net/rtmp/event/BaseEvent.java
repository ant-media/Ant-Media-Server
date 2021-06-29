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

package org.red5.server.net.rtmp.event;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.concurrent.atomic.AtomicInteger;

import org.red5.server.api.event.IEventListener;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base abstract class for all RTMP events
 */
public abstract class BaseEvent implements Constants, IRTMPEvent, Externalizable {

    protected Logger log = LoggerFactory.getLogger(getClass());

    // XXX we need a better way to inject allocation debugging
    // (1) make it configurable in xml
    // (2) make it aspect oriented
    private static final boolean allocationDebugging = false;

    /**
     * Event type
     */
    private Type type;

    /**
     * Source type
     */
    private byte sourceType;

    /**
     * Event target object
     */
    protected Object object;

    /**
     * Event listener
     */
    protected IEventListener source;

    /**
     * Event timestamp
     */
    protected int timestamp;

    /**
     * Event RTMP packet header
     */
    protected Header header = null;

    /**
     * Event references count
     */
    protected AtomicInteger refcount = new AtomicInteger(1);

    public BaseEvent() {
        // set a default type
        this(Type.SERVER, null);
    }

    /**
     * Create new event of given type
     * 
     * @param type
     *            Event type
     */
    public BaseEvent(Type type) {
        this(type, null);
    }

    /**
     * Create new event of given type
     * 
     * @param type
     *            Event type
     * @param source
     *            Event source
     */
    public BaseEvent(Type type, IEventListener source) {
        this.type = type;
        this.source = source;
        if (allocationDebugging) {
            AllocationDebugger.getInstance().create(this);
        }
    }

    /** {@inheritDoc} */
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public byte getSourceType() {
        return sourceType;
    }

    public void setSourceType(byte sourceType) {
        this.sourceType = sourceType;
    }

    /** {@inheritDoc} */
    public Object getObject() {
        return object;
    }

    /** {@inheritDoc} */
    public Header getHeader() {
        return header;
    }

    /** {@inheritDoc} */
    public void setHeader(Header header) {
        this.header = header;
    }

    /** {@inheritDoc} */
    public boolean hasSource() {
        return source != null;
    }

    /** {@inheritDoc} */
    public IEventListener getSource() {
        return source;
    }

    /** {@inheritDoc} */
    public void setSource(IEventListener source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    public abstract byte getDataType();

    /** {@inheritDoc} */
    public int getTimestamp() {
        return timestamp;
    }

    /** {@inheritDoc} */
    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
    public void retain() {
        if (allocationDebugging) {
            AllocationDebugger.getInstance().retain(this);
        }
        final int baseCount = refcount.getAndIncrement();
        if (allocationDebugging && baseCount < 1) {
            throw new RuntimeException("attempt to retain object with invalid ref count");
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
    public void release() {
        if (allocationDebugging) {
            AllocationDebugger.getInstance().release(this);
        }
        final int baseCount = refcount.decrementAndGet();
        if (baseCount == 0) {
            releaseInternal();
        } else if (allocationDebugging && baseCount < 0) {
            throw new RuntimeException("attempt to retain object with invalid ref count");
        }
    }

    /**
     * Release event
     */
    protected abstract void releaseInternal();

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = (Type) in.readObject();
        sourceType = in.readByte();
        timestamp = in.readInt();
        if (log.isTraceEnabled()) {
            log.trace("readExternal - type: {} sourceType: {} timestamp: {}", type, sourceType, timestamp);
        }
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        if (log.isTraceEnabled()) {
            log.trace("writeExternal - type: {} sourceType: {} timestamp: {}", type, sourceType, timestamp);
        }
        out.writeObject(type);
        out.writeByte(sourceType);
        out.writeInt(timestamp);
    }

}
