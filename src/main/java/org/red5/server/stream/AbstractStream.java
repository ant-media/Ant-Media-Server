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

package org.red5.server.stream;

import java.util.concurrent.Semaphore;

import org.red5.codec.IStreamCodecInfo;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.stream.IStream;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.StreamState;
import org.red5.server.net.rtmp.event.Notify;

/**
 * Abstract base implementation of IStream. Contains codec information, stream name, scope, event handling
 * meand, provides stream start and stop operations.
 *
 * @see  org.red5.server.api.stream.IStream
 */
public abstract class AbstractStream implements IStream {
    
    /**
     * Current state
     */
	protected StreamState state = StreamState.UNINIT;
    
	/**
     *  Stream name
     */
    private String name;
    
    /**
     *  Stream audio and video codec information
     */
	private IStreamCodecInfo codecInfo;
    
	/**
	 * Stores the streams metadata
	 */
	protected Notify metaData;
	
	/**
     *  Stream scope
     */
	private IScope scope;
	
	/**
	 * Timestamp the stream was created.
	 */
	protected long creationTime;
	
	/**
	 * Lock for protecting critical sections
	 */
	protected final Semaphore lock = new Semaphore(1, true);
	
    /**
     *  Return stream name
     *  @return     Stream name
     */
	public String getName() {
		return name;
	}

    /**
     * Return codec information
     * @return              Stream codec information
     */
    public IStreamCodecInfo getCodecInfo() {
		return codecInfo;
	}

	/**
	 * Returns the metadata for the associated stream, if it exists.
	 * 
	 * @return stream meta data
	 */
	public Notify getMetaData() {
		return metaData;
	}    
    
    /**
     * Return scope
     * @return         Scope
     */
    public IScope getScope() {
		return scope;
	}
    
	/**
	 * Returns timestamp at which the stream was created.
	 * 
	 * @return creation timestamp
	 */
	public long getCreationTime() {
		return creationTime;
	}

    /**
     * Setter for name
     * @param name     Stream name
     */
	public void setName(String name) {
		this.name = name;
	}

    /**
     * Setter for codec info
     * @param codecInfo     Codec info
     */
    public void setCodecInfo(IStreamCodecInfo codecInfo) {
		this.codecInfo = codecInfo;
	}

    /**
     * Setter for scope
     * @param scope         Scope
     */
	public void setScope(IScope scope) {
		this.scope = scope;
	}

	/**
	 * Return stream state
	 * @return StreamState
	 */
	public StreamState getState() {
		try {
			lock.acquireUninterruptibly();
			return state;
		} finally {
			lock.release();
		}
	}

	/**
	 * Sets the stream state
	 * @param state
	 */
	public void setState(StreamState state) {
		if (!this.state.equals(state)) {
			try {
				lock.acquireUninterruptibly();
				this.state = state;
			} finally {
				lock.release();
			}
		}
	}	
	
    /**
     * Return stream aware scope handler or null if scope is null
     * @return      IStreamAwareScopeHandler implementation
     */
	protected IStreamAwareScopeHandler getStreamAwareHandler() {
		if (scope != null) {
			IScopeHandler handler = scope.getHandler();
			if (handler instanceof IStreamAwareScopeHandler) {
				return (IStreamAwareScopeHandler) handler;
			}
		}
		return null;
	}
}
