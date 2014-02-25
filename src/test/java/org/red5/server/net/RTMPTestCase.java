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

package org.red5.server.net;

import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMPProtocolDecoder;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RTMPTestCase extends TestCase implements Constants {

	protected static Logger log = LoggerFactory.getLogger(RTMPTestCase.class);

	protected RTMPProtocolDecoder decoder;

	protected RTMPProtocolEncoder encoder;

	/** {@inheritDoc} */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		encoder = new RTMPProtocolEncoder();
		decoder = new RTMPProtocolDecoder();
	}

	public void testHeaders() {
		// set dummy connection local to prevent npe when setting last header size below
		Red5.setConnectionLocal(RTMPConnManager.getInstance().createConnection(RTMPConnection.class));
		Header header = new Header();
		header.setChannelId((byte) 0x12);
		header.setDataType(TYPE_INVOKE);
		header.setStreamId(100);
		header.setTimer(2);
		header.setSize(320);
		IoBuffer buf = encoder.encodeHeader(header, null);
		buf.flip();
		log.debug(buf.getHexDump());
		assertNotNull(buf);
		Header result = decoder.decodeHeader(buf, null);
		assertEquals(header, result);
		Red5.setConnectionLocal(null);
	}

	public void testInvokePacket() {
		@SuppressWarnings("unused")
		Invoke invoke = new Invoke();
	}

}
