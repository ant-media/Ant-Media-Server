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

package org.red5.server.net.rtmpe;

import javax.crypto.Cipher;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandshake;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMPE IO filter
 * 
 * @author Peter Thomas (ptrthomas@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPEIoFilter extends IoFilterAdapter {

	private static final Logger log = LoggerFactory.getLogger(RTMPEIoFilter.class);

	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session, Object obj) throws Exception {
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		log.trace("Session id: {}", sessionId);
		RTMPMinaConnection conn = (RTMPMinaConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId);		
		RTMP rtmp = conn.getState();
		//if there is a handshake on the session, ensure the type has been set
		if (session.containsAttribute(RTMPConnection.RTMP_HANDSHAKE)) {
			log.trace("Handshake exists on the session");
			//get the handshake from the session
			RTMPHandshake handshake = (RTMPHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
			int handshakeType = handshake.getHandshakeType();
			if (handshakeType == 0) {
				log.trace("Handshake type is not currently set");
				// holds the handshake type, default is un-encrypted
				byte handshakeByte = RTMPConnection.RTMP_NON_ENCRYPTED;
				//get the current message
				if (obj instanceof IoBuffer) {
					IoBuffer message = (IoBuffer) obj;
					message.mark();
					handshakeByte = message.get();
					message.reset();
				}
				//set the type
				handshake.setHandshakeType(handshakeByte);
				//set on the rtmp state
				rtmp.setEncrypted(handshakeByte == RTMPConnection.RTMP_ENCRYPTED ? true : false);
			} else if (handshakeType == 3) {
				if (rtmp.getState() == RTMP.STATE_CONNECTED) {
					log.debug("In connected state");
					// remove handshake from session now that we are connected
					session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
					log.debug("Using non-encrypted communications");
				}
			} else if (handshakeType == 6) {
				// ensure we have received enough bytes to be encrypted
				long readBytesCount = conn.getReadBytes();
				long writeBytesCount = conn.getWrittenBytes();
				log.trace("Bytes read: {} written: {}", readBytesCount, writeBytesCount);
				// don't remove the handshake when using RTMPE until we've written all the handshake data
				if (writeBytesCount >= (Constants.HANDSHAKE_SIZE * 2)) {
					//if we are connected and doing encryption, add the ciphers
					log.debug("Assumed to be in a connected state");
					// remove handshake from session now that we are connected
					session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
					log.debug("Using encrypted communications");
					//make sure they are not already on the session
					if (session.containsAttribute(RTMPConnection.RTMPE_CIPHER_IN)) {
						log.debug("Ciphers already exist on the session");
					} else {
						log.debug("Adding ciphers to the session");
						session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
						session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
					}					
				}
			}
		}
		Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN);
		if (cipher != null) { //may want to verify handshake is complete as well
			// assume message is an IoBuffer
			IoBuffer message = (IoBuffer) obj;
			if (rtmp.getState() == RTMP.STATE_HANDSHAKE) {
				// ensure there are enough bytes to skip
				if (message.limit() > Constants.HANDSHAKE_SIZE) {
    				//skip the first 1536
    				byte[] handshakeReply = new byte[Constants.HANDSHAKE_SIZE];
    				message.get(handshakeReply);
    				// TODO verify reply, for now just set to connected
    				rtmp.setState(RTMP.STATE_CONNECTED);
				} else {
					log.warn("There may be a network issue on this RTMPE connection: {}", conn);
					return;
				}
			}
			log.debug("Decrypting buffer: {}", message);
			byte[] encrypted = new byte[message.remaining()];
			message.get(encrypted);
			message.clear();
			message.free();
			byte[] plain = cipher.update(encrypted);
			IoBuffer messageDecrypted = IoBuffer.wrap(plain);
			log.debug("Decrypted buffer: {}", messageDecrypted);
			nextFilter.messageReceived(session, messageDecrypted);
		} else {
			log.trace("Not decrypting message received: {}", obj);
			nextFilter.messageReceived(session, obj);
		}
	}

	@Override
	public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest request) throws Exception {
		Cipher cipher = (Cipher) session.getAttribute(RTMPConnection.RTMPE_CIPHER_OUT);
		if (cipher != null) { //may want to verify handshake is complete as well
			IoBuffer message = (IoBuffer) request.getMessage();
			if (!message.hasRemaining()) {
				// Ignore empty buffers
				log.debug("Buffer was empty");
			} else {
				log.debug("Encrypting buffer: {}", message);
				byte[] plain = new byte[message.remaining()];
				message.get(plain);
				message.clear();
				message.free();
				//encrypt and write
				byte[] encrypted = cipher.update(plain);
				IoBuffer messageEncrypted = IoBuffer.wrap(encrypted);
				log.debug("Encrypted buffer: {}", messageEncrypted);
				nextFilter.filterWrite(session, new EncryptedWriteRequest(request, messageEncrypted));
			}
		} else {
			log.trace("Not encrypting write request");
			nextFilter.filterWrite(session, request);
		}
	}

	private static class EncryptedWriteRequest extends WriteRequestWrapper {
		private final IoBuffer encryptedMessage;

		private EncryptedWriteRequest(WriteRequest writeRequest, IoBuffer encryptedMessage) {
			super(writeRequest);
			this.encryptedMessage = encryptedMessage;
		}

		@Override
		public Object getMessage() {
			return encryptedMessage;
		}
	}

}
