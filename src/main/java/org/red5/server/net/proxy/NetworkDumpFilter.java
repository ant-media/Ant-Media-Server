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

package org.red5.server.net.proxy;

import java.nio.channels.WritableByteChannel;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network dump filter, performs raw data and headers dump on message recieve
 */
public class NetworkDumpFilter extends IoFilterAdapter {
    /**
     * Logger
     */
	protected static Logger log = LoggerFactory.getLogger(ProxyFilter.class);

    /**
     * Raw data byte channel
     */
    protected WritableByteChannel raw;

    /**
     * Headers byte channel
     */
    protected WritableByteChannel headers;

    /**
     * Create network dump filter from given dump channels
     * @param headers           Channel to dump headers
     * @param raw               Channel to dump raw data
     */
    public NetworkDumpFilter(WritableByteChannel headers,
			WritableByteChannel raw) {
		this.raw = raw;
		this.headers = headers;
	}

	/** {@inheritDoc} */
    @Override
	public void messageReceived(NextFilter next, IoSession session,
			Object message) throws Exception {
		if (message instanceof IoBuffer) {
			IoBuffer out = (IoBuffer) message;
			if (headers != null) {
				IoBuffer header = IoBuffer.allocate(12);
				header.putLong(System.currentTimeMillis());
				header.putInt(out.limit() - out.position());
				header.flip();
				headers.write(header.buf());
			}
			if (raw != null) {
				raw.write(out.asReadOnlyBuffer().buf());
			}
		}
		next.messageReceived(session, message);
	}

	/** {@inheritDoc} */
    @Override
	public void sessionClosed(NextFilter next, IoSession session)
			throws Exception {
		if (headers.isOpen()) {
			headers.close();
		}
		if (raw.isOpen()) {
			raw.close();
		}
		next.sessionClosed(session);
	}

}
