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

package org.red5.server.net.rtmp.codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.amf.Output;
import org.red5.io.amf3.AMF3;
import org.red5.io.object.DataTypes;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.StreamAction;
import org.red5.io.utils.BufferUtils;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.Red5;
import org.red5.server.net.protocol.HandshakeFailedException;
import org.red5.server.net.protocol.ProtocolException;
import org.red5.server.net.protocol.RTMPDecodeState;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPUtils;
import org.red5.server.net.rtmp.event.Abort;
import org.red5.server.net.rtmp.event.Aggregate;
import org.red5.server.net.rtmp.event.AudioData;
import org.red5.server.net.rtmp.event.BytesRead;
import org.red5.server.net.rtmp.event.ChunkSize;
import org.red5.server.net.rtmp.event.ClientBW;
import org.red5.server.net.rtmp.event.FlexMessage;
import org.red5.server.net.rtmp.event.FlexStreamSend;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.SWFResponse;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.SetBuffer;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.message.SharedObjectTypeMapping;
import org.red5.server.service.Call;
import org.red5.server.service.PendingCall;
import org.red5.server.so.FlexSharedObjectMessage;
import org.red5.server.so.ISharedObjectEvent;
import org.red5.server.so.ISharedObjectMessage;
import org.red5.server.so.SharedObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP protocol decoder.
 */
public class RTMPProtocolDecoder implements Constants, IEventDecoder {

	protected static Logger log = LoggerFactory.getLogger(RTMPProtocolDecoder.class);

	/** Constructs a new RTMPProtocolDecoder. */
	public RTMPProtocolDecoder() {
	}

	/**
	 * Decode all available objects in buffer.
	 * 
	 * @param conn RTMP connection
	 * @param buffer IoBuffer of data to be decoded
	 * @return a list of decoded objects, may be empty if nothing could be decoded
	 */
	public List<Object> decodeBuffer(RTMPConnection conn, IoBuffer buffer) {
		// decoded results
		List<Object> result = null;
		if (conn != null) {
			log.trace("Decoding for connection - session id: {}", conn.getSessionId());
			try {
				// instance list to hold results
				result = new LinkedList<Object>();
				// get the local decode state
				RTMPDecodeState state = conn.getDecoderState();
				log.trace("{}", state);
				if (!conn.getSessionId().equals(state.getSessionId())) {
					log.warn("Session decode overlap: {} != {}", conn.getSessionId(), state.getSessionId());
				}
				while (buffer.hasRemaining()) {
					final int remaining = buffer.remaining();
					if (state.canStartDecoding(remaining)) {
						log.trace("Can start decoding");
						state.startDecoding();
					} else {
						log.trace("Cannot start decoding");
						break;
					}
					final Object decodedObject = decode(conn, state, buffer);
					if (state.hasDecodedObject()) {
						log.trace("Has decoded object");
						if (decodedObject != null) {
							result.add(decodedObject);
						}
					} else if (state.canContinueDecoding()) {
						log.trace("Can continue decoding");
						continue;
					} else {
						log.trace("Cannot continue decoding");
						break;
					}
				}
			} catch (HandshakeFailedException hfe) {
				// close the connection
				log.warn("Closing connection because decoding failed during handshake: {}", conn, hfe);
				// clear buffer if something is wrong in protocol decoding
				buffer.clear();
				// close connection because we can't parse data from it
				conn.close();
			} catch (Exception ex) {
				// catch any non-handshake exception in the decoding
				// close the connection
				log.warn("Closing connection because decoding failed: {}", conn, ex);
				// clear the buffer to eliminate memory leaks when we can't parse protocol
				buffer.clear();
				// close connection because we can't parse data from it
				conn.close();
			} finally {
				buffer.compact();
			}
		} else {
			log.error("Decoding buffer failed, no current connection!?");
		}
		return result;
	}

	/**
	 * Decodes the buffer data.
	 * 
	 * @param conn RTMP connection
	 * @param state Stores state for the protocol, ProtocolState is just a marker interface
	 * @param in IoBuffer of data to be decoded
	 * @return one of three possible values: 
	 * 		 1. null : the object could not be decoded, or some data was skipped, just continue
	 *       2. ProtocolState : the decoder was unable to decode the whole object, refer to the protocol state 
	 *       3. Object : something was decoded, continue
	 * @throws Exception on error
	 */
	public Object decode(RTMPConnection conn, RTMPDecodeState state, IoBuffer in) throws ProtocolException {
		if (log.isTraceEnabled()) {
			log.trace("Decoding for {}", conn.getSessionId());
		}
		try {
			final byte connectionState = conn.getStateCode();
			switch (connectionState) {
				case RTMP.STATE_CONNECTED:
					return decodePacket(conn, state, in);
				case RTMP.STATE_CONNECT:
					return decodeHandshakeS1(conn, state, in);
				case RTMP.STATE_HANDSHAKE:
					return decodeHandshakeS2(conn, state, in);
				case RTMP.STATE_ERROR:
				case RTMP.STATE_DISCONNECTING:
				case RTMP.STATE_DISCONNECTED:
					// throw away any remaining input data:
					in.position(in.limit());
					return null;
				default:
					throw new IllegalStateException("Invalid RTMP state: " + connectionState);
			}
		} catch (ProtocolException pe) {
			// raise to caller unmodified
			throw pe;
		} catch (RuntimeException e) {
			throw new ProtocolException("Error during decoding", e);
		} finally {
			if (log.isTraceEnabled()) {
				log.trace("Decoding finished for {}", conn.getSessionId());
			}
		}
	}

	/**
	 * Decodes handshake message for step 1, RTMP.STATE_CONNECT.
	 * 
	 * @param conn Connection
	 * @param state protocol decode state
	 * @param in IoBuffer
	 * @return IoBuffer
	 */
	public IoBuffer decodeHandshakeS1(RTMPConnection conn, RTMPDecodeState state, IoBuffer in) {
		// first step: client has connected and handshaking is not complete
		if (log.isDebugEnabled()) {
			log.debug("decodeHandshake - state: {} buffer: {}", state, in);
		}
		// number of byte remaining in the buffer
		int remaining = in.remaining();
		if (remaining < HANDSHAKE_SIZE + 1) {
			log.debug("Handshake init too small, buffering. remaining: {}", remaining);
			state.bufferDecoding(HANDSHAKE_SIZE + 1);
		} else {
			final IoBuffer hs = IoBuffer.allocate(HANDSHAKE_SIZE);
			in.get(); // skip the header byte
			BufferUtils.put(hs, in, HANDSHAKE_SIZE);
			hs.flip();
			conn.setStateCode(RTMP.STATE_HANDSHAKE);
			return hs;
		}
		return null;
	}

	/**
	 * Decodes handshake message for step 2, RTMP.STATE_HANDSHAKE.
	 * 
	 * @param conn Connection
	 * @param state protocol decode state
	 * @param in IoBuffer
	 * @return IoBuffer
	 */
	public IoBuffer decodeHandshakeS2(RTMPConnection conn, RTMPDecodeState state, IoBuffer in) {
		// second step: all handshake data received, collecting handshake reply data
		log.debug("decodeHandshake - state: {} buffer: {}", state, in);
		// number of byte remaining in the buffer
		int remaining = in.remaining();
		// TODO Paul: re-examine how remaining data is buffered between handshake reply and next message when using rtmpe
		// connections sending partial tcp are getting dropped
		log.debug("Handshake reply");
		// how many bytes left to get
		int required = state.getDecoderBufferAmount();
		log.trace("Handshake reply - required: {} remaining: {}", required, remaining);
		if (remaining < HANDSHAKE_SIZE) {
			log.debug("Handshake reply too small, buffering. remaining: {}", remaining);
			state.bufferDecoding(HANDSHAKE_SIZE);
		} else {
			in.skip(HANDSHAKE_SIZE);
			conn.setStateCode(RTMP.STATE_CONNECTED);
			state.continueDecoding();
		}
		return null;
	}

	/**
	 * Decodes an IoBuffer into a Packet.
	 * 
	 * @param conn Connection
	 * @param rtmp RTMP protocol state
	 * @param in IoBuffer
	 * @return Packet
	 */
	public Packet decodePacket(RTMPConnection conn, RTMPDecodeState state, IoBuffer in) {
		if (log.isTraceEnabled()) {
			log.trace("decodePacket - state: {} buffer: {}", state, in);
		}
		final int remaining = in.remaining();
		// We need at least one byte
		if (remaining < 1) {
			state.bufferDecoding(1);
			return null;
		}
		final int position = in.position();
		byte headerByte = in.get();
		int headerValue;
		int byteCount;
		if ((headerByte & 0x3f) == 0) {
			// Two byte header
			if (remaining < 2) {
				in.position(position);
				state.bufferDecoding(2);
				return null;
			}
			headerValue = (headerByte & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 2;
		} else if ((headerByte & 0x3f) == 1) {
			// Three byte header
			if (remaining < 3) {
				in.position(position);
				state.bufferDecoding(3);
				return null;
			}
			headerValue = (headerByte & 0xff) << 16 | (in.get() & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 3;
		} else {
			// Single byte header
			headerValue = headerByte & 0xff;
			byteCount = 1;
		}
		final int channelId = RTMPUtils.decodeChannelId(headerValue, byteCount);
		if (channelId < 0) {
			throw new ProtocolException("Bad channel id: " + channelId);
		}
		RTMP rtmp = conn.getState();
		// Get the header size and length
		byte headerSize = RTMPUtils.decodeHeaderSize(headerValue, byteCount);
		int headerLength = RTMPUtils.getHeaderLength(headerSize);
		Header lastHeader = rtmp.getLastReadHeader(channelId);
		headerLength += byteCount - 1;
		switch (headerSize) {
			case HEADER_NEW:
			case HEADER_SAME_SOURCE:
			case HEADER_TIMER_CHANGE:
				if (remaining >= headerLength) {
					int timeValue = RTMPUtils.readUnsignedMediumInt(in);
					if (timeValue == 0xffffff) {
						headerLength += 4;
					}
				}
				break;
			case HEADER_CONTINUE:
				if (lastHeader != null && lastHeader.getExtendedTimestamp() != 0) {
					headerLength += 4;
				}
				break;
			default:
				throw new ProtocolException("Unexpected header size " + headerSize + " check for error");
		}
		if (remaining < headerLength) {
			log.trace("Header too small (hlen: {}), buffering. remaining: {}", headerLength, remaining);
			in.position(position);
			state.bufferDecoding(headerLength);
			return null;
		}
		// Move the position back to the start
		in.position(position);
		final Header header = decodeHeader(in, lastHeader);
		if (header == null) {
			throw new ProtocolException("Header is null, check for error");
		}
		rtmp.setLastReadHeader(channelId, header);
		// check to see if this is a new packets or continue decoding an existing one
		Packet packet = rtmp.getLastReadPacket(channelId);
		if (packet == null) {
			packet = new Packet(header.clone());
			rtmp.setLastReadPacket(channelId, packet);
		}
		final IoBuffer buf = packet.getData();
		final int readRemaining = header.getSize() - buf.position();
		final int chunkSize = rtmp.getReadChunkSize();
		final int readAmount = (readRemaining > chunkSize) ? chunkSize : readRemaining;
		if (in.remaining() < readAmount) {
			log.debug("Chunk too small, buffering ({},{})", in.remaining(), readAmount);
			// skip the position back to the start
			in.position(position);
			state.bufferDecoding(headerLength + readAmount);
			return null;
		}
		BufferUtils.put(buf, in, readAmount);
		if (buf.position() < header.getSize()) {
			state.continueDecoding();
			return null;
		}
		if (buf.position() > header.getSize()) {
			log.warn("Packet size expanded from {} to {} ({})", new Object[] { (header.getSize()), buf.position(), header });
		}
		buf.flip();
		try {
			final IRTMPEvent message = decodeMessage(conn, packet.getHeader(), buf);
			message.setHeader(packet.getHeader());
			// Unfortunately flash will, especially when resetting a video stream with a new key frame, sometime 
			// send an earlier time stamp.  To avoid dropping it, we just give it the minimal increment since the 
			// last message.  But to avoid relative time stamps being mis-computed, we don't reset the header we stored.
			final Header lastReadHeader = rtmp.getLastReadPacketHeader(channelId);
			if (lastReadHeader != null && (message instanceof AudioData || message instanceof VideoData)
					&& RTMPUtils.compareTimestamps(lastReadHeader.getTimer(), packet.getHeader().getTimer()) >= 0) {
				log.trace("Non-monotonically increasing timestamps; type: {}; adjusting to {}; ts: {}; last: {}", new Object[] { header.getDataType(),
						lastReadHeader.getTimer() + 1, header.getTimer(), lastReadHeader.getTimer() });
				message.setTimestamp(lastReadHeader.getTimer() + 1);
			} else {
				message.setTimestamp(header.getTimer());
			}
			rtmp.setLastReadPacketHeader(channelId, packet.getHeader());
			packet.setMessage(message);
			if (message instanceof ChunkSize) {
				ChunkSize chunkSizeMsg = (ChunkSize) message;
				rtmp.setReadChunkSize(chunkSizeMsg.getSize());
			} else if (message instanceof Abort) {
				log.debug("Abort packet detected");
				// The client is aborting a message; reset the packet
				// because the next chunk on that stream will start a new packet.
				Abort abort = (Abort) message;
				rtmp.setLastReadPacket(abort.getChannelId(), null);
				packet = null;
			}
			// collapse the time stamps on the last packet so that it works right for chunk type 3 later
			lastHeader = rtmp.getLastReadHeader(channelId);
			lastHeader.setTimerBase(header.getTimer());
		} finally {
			rtmp.setLastReadPacket(channelId, null);
		}
		return packet;
	}

	/**
	 * Decodes packet header.
	 * 
	 * @param in Input IoBuffer
	 * @param lastHeader Previous header
	 * @return Decoded header
	 */
	public Header decodeHeader(IoBuffer in, Header lastHeader) {
		if (log.isTraceEnabled()) {
			log.trace("decodeHeader - lastHeader: {} buffer: {}", lastHeader, in);
		}
		byte headerByte = in.get();
		int headerValue;
		int byteCount = 1;
		if ((headerByte & 0x3f) == 0) {
			// Two byte header
			headerValue = (headerByte & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 2;
		} else if ((headerByte & 0x3f) == 1) {
			// Three byte header
			headerValue = (headerByte & 0xff) << 16 | (in.get() & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 3;
		} else {
			// Single byte header
			headerValue = headerByte & 0xff;
			byteCount = 1;
		}
		final int channelId = RTMPUtils.decodeChannelId(headerValue, byteCount);
		final int headerSize = RTMPUtils.decodeHeaderSize(headerValue, byteCount);
		Header header = new Header();
		header.setChannelId(channelId);
		if (headerSize != HEADER_NEW && lastHeader == null) {
			log.error("Last header null not new, headerSize: {}, channelId {}", headerSize, channelId);
			//this will trigger an error status, which in turn will disconnect the "offending" flash player
			//preventing a memory leak and bringing the whole server to its knees
			return null;
		}
		int timeValue;
		switch (headerSize) {
			case HEADER_NEW:
				// an absolute time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(RTMPUtils.readUnsignedMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(RTMPUtils.readReverseInt(in));
				if (timeValue == 0xffffff) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
				}
				header.setTimerBase(timeValue);
				header.setTimerDelta(0);
				break;
			case HEADER_SAME_SOURCE:
				// a delta time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(RTMPUtils.readUnsignedMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(lastHeader.getStreamId());
				if (timeValue == 0xffffff) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
				} else if (timeValue == 0 && header.getDataType() == TYPE_AUDIO_DATA) {
					// header.setIsGarbage(true);
					log.trace("Audio with zero delta; setting to garbage; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(),
							headerSize });
				}
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(timeValue);
				break;
			case HEADER_TIMER_CHANGE:
				// a delta time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				if (timeValue == 0xffffff) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
				} else if (timeValue == 0 && header.getDataType() == TYPE_AUDIO_DATA) {
					// header.setIsGarbage(true);
					log.trace("Audio with zero delta; setting to garbage; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(),
							headerSize });
				}
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(timeValue);
				break;
			case HEADER_CONTINUE:
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(lastHeader.getTimerDelta());
				if (lastHeader.getExtendedTimestamp() != 0) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
					log.trace("HEADER_CONTINUE with extended timestamp: {}", timeValue);
				}
				break;
			default:
				log.error("Unexpected header size: {}", headerSize);
				return null;
		}
		log.trace("CHUNK, D, {}, {}", header, headerSize);
		return header;
	}

	/**
	 * Decodes RTMP message event.
	 * 
	 * @param conn RTMP connection
	 * @param header RTMP header
	 * @param in Input IoBuffer
	 * @return RTMP event
	 */
	public IRTMPEvent decodeMessage(RTMPConnection conn, Header header, IoBuffer in) {
		IRTMPEvent message;
		byte dataType = header.getDataType();
		switch (dataType) {
			case TYPE_INVOKE:
				message = decodeInvoke(conn.getEncoding(), in);
				break;
			case TYPE_NOTIFY:
				if (header.getStreamId() == 0) {
					message = decodeNotify(conn.getEncoding(), in, header);
				} else {
					message = decodeStreamMetadata(in);
				}
				break;
			case TYPE_AUDIO_DATA:
				message = decodeAudioData(in);
				message.setSourceType(Constants.SOURCE_TYPE_LIVE);
				break;
			case TYPE_VIDEO_DATA:
				message = decodeVideoData(in);
				message.setSourceType(Constants.SOURCE_TYPE_LIVE);
				break;
			case TYPE_AGGREGATE:
				message = decodeAggregate(in);
				break;
			case TYPE_FLEX_SHARED_OBJECT: // represents an SO in an AMF3 container
				message = decodeFlexSharedObject(in);
				break;
			case TYPE_SHARED_OBJECT:
				message = decodeSharedObject(in);
				break;
			case TYPE_FLEX_MESSAGE:
				message = decodeFlexMessage(in);
				break;
			case TYPE_FLEX_STREAM_SEND:
				message = decodeFlexStreamSend(in);
				break;
			case TYPE_PING:
				message = decodePing(in);
				break;
			case TYPE_BYTES_READ:
				message = decodeBytesRead(in);
				break;
			case TYPE_CHUNK_SIZE:
				message = decodeChunkSize(in);
				break;
			case TYPE_SERVER_BANDWIDTH:
				message = decodeServerBW(in);
				break;
			case TYPE_CLIENT_BANDWIDTH:
				message = decodeClientBW(in);
				break;
			case TYPE_ABORT:
				message = decodeAbort(in);
				break;
			default:
				log.warn("Unknown object type: {}", dataType);
				message = decodeUnknown(dataType, in);
				break;
		}
		return message;
	}

	public IRTMPEvent decodeAbort(IoBuffer in) {
		return new Abort(in.getInt());
	}

	/**
	 * Decodes server bandwidth.
	 * 
	 * @param in IoBuffer
	 * @return RTMP event
	 */
	private IRTMPEvent decodeServerBW(IoBuffer in) {
		return new ServerBW(in.getInt());
	}

	/**
	 * Decodes client bandwidth.
	 * 
	 * @param in
	 *            Byte buffer
	 * @return RTMP event
	 */
	private IRTMPEvent decodeClientBW(IoBuffer in) {
		return new ClientBW(in.getInt(), in.get());
	}

	/** {@inheritDoc} */
	public Unknown decodeUnknown(byte dataType, IoBuffer in) {
		return new Unknown(dataType, in);
	}

	/** {@inheritDoc} */
	public Aggregate decodeAggregate(IoBuffer in) {
		return new Aggregate(in);
	}

	/** {@inheritDoc} */
	public ChunkSize decodeChunkSize(IoBuffer in) {
		int chunkSize = in.getInt();
		log.debug("Decoded chunk size: {}", chunkSize);
		return new ChunkSize(chunkSize);
	}

	/** {@inheritDoc} */
	public ISharedObjectMessage decodeFlexSharedObject(IoBuffer in) {
		byte encoding = in.get();
		Input input;
		if (encoding == 0) {
			input = new org.red5.io.amf.Input(in);
		} else if (encoding == 3) {
			input = new org.red5.io.amf3.Input(in);
		} else {
			throw new RuntimeException("Unknown SO encoding: " + encoding);
		}
		String name = input.getString();
		// Read version of SO to modify
		int version = in.getInt();
		// Read persistence informations
		boolean persistent = in.getInt() == 2;
		// Skip unknown bytes
		in.skip(4);
		// create our shared object message
		final SharedObjectMessage so = new FlexSharedObjectMessage(null, name, version, persistent);
		doDecodeSharedObject(so, in, input);
		return so;
	}

	/** {@inheritDoc} */
	public ISharedObjectMessage decodeSharedObject(IoBuffer in) {
		final Input input = new org.red5.io.amf.Input(in);
		String name = input.getString();
		// Read version of SO to modify
		int version = in.getInt();
		// Read persistence informations
		boolean persistent = in.getInt() == 2;
		// Skip unknown bytes
		in.skip(4);
		// create our shared object message
		final SharedObjectMessage so = new SharedObjectMessage(null, name, version, persistent);
		doDecodeSharedObject(so, in, input);
		return so;
	}

	/**
	 * Perform the actual decoding of the shared object contents.
	 * 
	 * @param so
	 * @param in
	 * @param input
	 */
	protected void doDecodeSharedObject(SharedObjectMessage so, IoBuffer in, Input input) {
		// Parse request body
		Input amf3Input = new org.red5.io.amf3.Input(in);
		while (in.hasRemaining()) {
			final ISharedObjectEvent.Type type = SharedObjectTypeMapping.toType(in.get());
			if (type == null) {
				in.skip(in.remaining());
				return;
			}
			String key = null;
			Object value = null;

			// if(log.isDebugEnabled())
			// log.debug("type: "+SharedObjectTypeMapping.toString(type));

			// SharedObjectEvent event = new SharedObjectEvent(,null,null);
			final int length = in.getInt();
			if (type == ISharedObjectEvent.Type.CLIENT_STATUS) {
				// Status code
				key = input.getString();
				// Status level
				value = input.getString();
			} else if (type == ISharedObjectEvent.Type.CLIENT_UPDATE_DATA) {
				key = null;
				// Map containing new attribute values
				final Map<String, Object> map = new HashMap<String, Object>();
				final int start = in.position();
				while (in.position() - start < length) {
					String tmp = input.getString();
					map.put(tmp, Deserializer.deserialize(input, Object.class));
				}
				value = map;
			} else if (type != ISharedObjectEvent.Type.SERVER_SEND_MESSAGE && type != ISharedObjectEvent.Type.CLIENT_SEND_MESSAGE) {
				if (length > 0) {
					key = input.getString();
					if (length > key.length() + 2) {
						// determine if the object is encoded with amf3
						byte objType = in.get();
						in.position(in.position() - 1);
						Input propertyInput;
						if (objType == AMF.TYPE_AMF3_OBJECT && !(input instanceof org.red5.io.amf3.Input)) {
							// The next parameter is encoded using AMF3
							propertyInput = amf3Input;
						} else {
							// The next parameter is encoded using AMF0
							propertyInput = input;
						}
						value = Deserializer.deserialize(propertyInput, Object.class);
					}
				}
			} else {
				final int start = in.position();
				// the "send" event seems to encode the handler name as complete AMF string including the string type byte
				key = Deserializer.deserialize(input, String.class);
				// read parameters
				final List<Object> list = new LinkedList<Object>();
				while (in.position() - start < length) {
					byte objType = in.get();
					in.position(in.position() - 1);
					// determine if the object is encoded with amf3
					Input propertyInput;
					if (objType == AMF.TYPE_AMF3_OBJECT && !(input instanceof org.red5.io.amf3.Input)) {
						// The next parameter is encoded using AMF3
						propertyInput = amf3Input;
					} else {
						// The next parameter is encoded using AMF0
						propertyInput = input;
					}
					Object tmp = Deserializer.deserialize(propertyInput, Object.class);
					list.add(tmp);
				}
				value = list;
			}
			so.addEvent(type, key, value);
		}
	}

	/** {@inheritDoc} */
	public Notify decodeNotify(Encoding encoding, IoBuffer in) {
		return decodeNotify(encoding, in, null);
	}

	/**
	 * Decode a Notify.
	 * 
	 * @param encoding 
	 * @param in
	 * @param header
	 * @return decoded notify result
	 */
	public Notify decodeNotify(Encoding encoding, IoBuffer in, Header header) {
		Notify notify = new Notify();
		int start = in.position();
		Input input;
		// for response, the action string and invokeId is always encoded as AMF0 we use the first byte to decide which encoding to use
		byte tmp = in.get();
		in.position(start);
		if (encoding == Encoding.AMF3 && tmp == AMF.TYPE_AMF3_OBJECT) {
			input = new org.red5.io.amf3.Input(in);
			((org.red5.io.amf3.Input) input).enforceAMF3();
		} else {
			input = new org.red5.io.amf.Input(in);
		}
		// get the action
		String action = Deserializer.deserialize(input, String.class);
		if (log.isTraceEnabled()) {
			log.trace("Action " + action);
		}
		//throw a runtime exception if there is no action
		if (action != null) {
			//TODO Handle NetStream.send? Where and how?
			if (header != null && header.getStreamId() != 0 && !isStreamCommand(action)) {
				// don't decode "NetStream.send" requests
				in.position(start);
				notify.setData(in.asReadOnlyBuffer());
				return notify;
			}
			if (header == null || header.getStreamId() == 0) {
				int invokeId = Deserializer.<Number> deserialize(input, Number.class).intValue();
				if (invokeId != 0) {
					throw new RuntimeException("Notify invoke / transaction id was non-zero");
				}
			}
			// now go back to the actual encoding to decode parameters
			if (encoding == Encoding.AMF3) {
				input = new org.red5.io.amf3.Input(in);
				((org.red5.io.amf3.Input) input).enforceAMF3();
			} else {
				input = new org.red5.io.amf.Input(in);
			}
			// get / set the parameters if there any
			Object[] params = handleParameters(in, notify, input);
			// determine service information
			final int dotIndex = action.lastIndexOf('.');
			String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
			// pull off the prefixes since java doesn't allow this on a method name
			if (serviceName != null && (serviceName.startsWith("@") || serviceName.startsWith("|"))) {
				serviceName = serviceName.substring(1);
			}
			String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());
			// pull off the prefixes since java doesnt allow this on a method name
			if (serviceMethod.startsWith("@") || serviceMethod.startsWith("|")) {
				serviceMethod = serviceMethod.substring(1);
			}
			Call call = new Call(serviceName, serviceMethod, params);
			notify.setCall(call);
			return notify;
		} else {
			//TODO replace this with something better as time permits
			throw new RuntimeException("Action was null");
		}
	}

	/** {@inheritDoc} */
	public Invoke decodeInvoke(Encoding encoding, IoBuffer in) {
		Invoke invoke = new Invoke();
		int start = in.position();
		Input input;
		// for response, the action string and invokeId is always encoded as AMF0 we use the first byte to decide which encoding to use.
		byte tmp = in.get();
		in.position(start);
		if (encoding == Encoding.AMF3 && tmp == AMF.TYPE_AMF3_OBJECT) {
			input = new org.red5.io.amf3.Input(in);
			((org.red5.io.amf3.Input) input).enforceAMF3();
		} else {
			input = new org.red5.io.amf.Input(in);
		}
		// get the action
		String action = Deserializer.deserialize(input, String.class);
		if (log.isTraceEnabled()) {
			log.trace("Action " + action);
		}
		//throw a runtime exception if there is no action
		if (action != null) {
			invoke.setTransactionId(Deserializer.<Number> deserialize(input, Number.class).intValue());
			// now go back to the actual encoding to decode parameters
			if (encoding == Encoding.AMF3) {
				input = new org.red5.io.amf3.Input(in);
				((org.red5.io.amf3.Input) input).enforceAMF3();
			} else {
				input = new org.red5.io.amf.Input(in);
			}
			// get / set the parameters if there any
			Object[] params = handleParameters(in, invoke, input);
			// determine service information
			final int dotIndex = action.lastIndexOf('.');
			String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
			// pull off the prefixes since java doesnt allow this on a method name
			if (serviceName != null && (serviceName.startsWith("@") || serviceName.startsWith("|"))) {
				serviceName = serviceName.substring(1);
			}
			String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());
			// pull off the prefixes since java doesn't allow this on a method name
			if (serviceMethod.startsWith("@") || serviceMethod.startsWith("|")) {
				serviceMethod = serviceMethod.substring(1);
			}
			PendingCall call = new PendingCall(serviceName, serviceMethod, params);
			invoke.setCall(call);
			return invoke;
		} else {
			//TODO replace this with something better as time permits
			throw new RuntimeException("Action was null");
		}
	}

	/**
	 * Decodes ping event.
	 * 
	 * @param in IoBuffer
	 * @return Ping event
	 */
	public Ping decodePing(IoBuffer in) {
		Ping ping = null;
		if (log.isTraceEnabled()) {
			// gets the raw data as hex without changing the data or pointer
			String hexDump = in.getHexDump();
			log.trace("Ping dump: {}", hexDump);
		}
		// control type
		short type = in.getShort();
		switch (type) {
			case Ping.CLIENT_BUFFER:
				ping = new SetBuffer(in.getInt(), in.getInt());
				break;
			case Ping.PING_SWF_VERIFY:
				// only contains the type (2 bytes)
				ping = new Ping(type);
				break;
			case Ping.PONG_SWF_VERIFY:
				byte[] bytes = new byte[42];
				in.get(bytes);
				ping = new SWFResponse(bytes);
				break;
			default:
				//STREAM_BEGIN, STREAM_PLAYBUFFER_CLEAR, STREAM_DRY, RECORDED_STREAM
				//PING_CLIENT, PONG_SERVER
				//BUFFER_EMPTY, BUFFER_FULL
				ping = new Ping(type, in.getInt());
				break;
		}
		return ping;
	}

	/** {@inheritDoc} */
	public BytesRead decodeBytesRead(IoBuffer in) {
		return new BytesRead(in.getInt());
	}

	/** {@inheritDoc} */
	public AudioData decodeAudioData(IoBuffer in) {
		return new AudioData(in.asReadOnlyBuffer());
	}

	/** {@inheritDoc} */
	public VideoData decodeVideoData(IoBuffer in) {
		return new VideoData(in.asReadOnlyBuffer());
	}

	/**
	 * Decodes stream meta data, to include onMetaData, onCuePoint, and onFI.
	 * 
	 * @param in
	 * @return Notify
	 */
	@SuppressWarnings("unchecked")
	public Notify decodeStreamMetadata(IoBuffer in) {
		Encoding encoding = ((RTMPConnection) Red5.getConnectionLocal()).getEncoding();
		Input input = null;

		// check to see if the encoding is set to AMF3. 
		// if it is then check to see if first byte is set to AMF0
		byte amfVersion = 0x00;
		if (encoding == Encoding.AMF3) {
			amfVersion = in.get();
		}
		
		// reset the position back to 0
		in.position(0);
		
		//make a pre-emptive copy of the incoming buffer here to prevent issues that occur fairly often
		IoBuffer copy = in.duplicate();
		
		
		if (encoding == Encoding.AMF0 || amfVersion != AMF.TYPE_AMF3_OBJECT ) {
			input = new org.red5.io.amf.Input(copy);
		} else {
			org.red5.io.amf3.Input.RefStorage refStorage = new org.red5.io.amf3.Input.RefStorage();
			input = new org.red5.io.amf3.Input(copy, refStorage);
		}
		//get the first datatype
		byte dataType = input.readDataType();
		if (dataType == DataTypes.CORE_STRING) {
			String setData = input.readString(String.class);
			if ("@setDataFrame".equals(setData)) {
				// get the second datatype
				byte dataType2 = input.readDataType();
				log.debug("Dataframe method type: {}", dataType2);
				String onCueOrOnMeta = input.readString(String.class);
				// get the params datatype
				byte object = input.readDataType();
				log.debug("Dataframe params type: {}", object);
				Map<Object, Object> params;
				if (object == DataTypes.CORE_MAP) {
					// the params are sent as a Mixed-Array. Required to support the RTMP publish provided by ffmpeg/xuggler
					params = (Map<Object, Object>) input.readMap(null);
				} else {
					// read the params as a standard object
					params = (Map<Object, Object>) input.readObject(Object.class);
				}
				log.debug("Dataframe: {} params: {}", onCueOrOnMeta, params.toString());

				IoBuffer buf = IoBuffer.allocate(1024);
				buf.setAutoExpand(true);
				Output out = new Output(buf);
				out.writeString(onCueOrOnMeta);
				out.writeMap(params);

				buf.flip();
				return new Notify(buf);
			} else if ("onFI".equals(setData)) {
				// the onFI request contains 2 items relative to the publishing client application
				// sd = system date (12-07-2011)
				// st = system time (09:11:33.387)
				byte object = input.readDataType();
				log.debug("onFI params type: {}", object);
				Map<Object, Object> params;
				if (object == DataTypes.CORE_MAP) {
					// the params are sent as a Mixed-Array
					params = (Map<Object, Object>) input.readMap(null);
				} else {
					// read the params as a standard object
					params = (Map<Object, Object>) input.readObject(Object.class);
				}
				log.debug("onFI params: {}", params.toString());
			} else {
				log.info("Unhandled request: {}", setData);
				if (log.isDebugEnabled()) {
					byte object = input.readDataType();
					log.debug("Params type: {}", object);
					if (object == DataTypes.CORE_MAP) {
						Map<Object, Object> params = (Map<Object, Object>) input.readMap(null);
						log.debug("Params: {}", params.toString());
					} else {
						log.debug("The unknown request was did not provide a parameter map");
					}
				}
			}
		}
		return new Notify(in.asReadOnlyBuffer());
	}

	/**
	 * Decodes FlexMessage event.
	 * 
	 * @param in IoBuffer
	 * @return FlexMessage event
	 */
	public FlexMessage decodeFlexMessage(IoBuffer in) {
		// TODO: Unknown byte, probably encoding as with Flex SOs?
		byte flexByte = in.get();
		log.trace("Flex byte: {}", flexByte);
		// Encoding of message params can be mixed - some params may be in AMF0, others in AMF3,
		// but according to AMF3 spec, we should collect AMF3 references for the whole message body (through all params)
		org.red5.io.amf3.Input.RefStorage refStorage = new org.red5.io.amf3.Input.RefStorage();

		Input input = new org.red5.io.amf.Input(in);
		String action = Deserializer.deserialize(input, String.class);
		int transactionId = Deserializer.<Number> deserialize(input, Number.class).intValue();
		FlexMessage msg = new FlexMessage();
		msg.setTransactionId(transactionId);
		Object[] params = new Object[] {};
		if (in.hasRemaining()) {
			ArrayList<Object> paramList = new ArrayList<Object>();
			final Object obj = Deserializer.deserialize(input, Object.class);
			if (obj != null) {
				paramList.add(obj);
			}
			while (in.hasRemaining()) {
				// Check for AMF3 encoding of parameters
				byte objectEncodingType = in.get();
				in.position(in.position() - 1);
				log.debug("Object encoding: {}", objectEncodingType);
				switch (objectEncodingType) {
					case AMF.TYPE_AMF3_OBJECT:
					case AMF3.TYPE_VECTOR_NUMBER:
					case AMF3.TYPE_VECTOR_OBJECT:
						// The next parameter is encoded using AMF3
						input = new org.red5.io.amf3.Input(in, refStorage);
						// Vectors with number and object have to have AMF3 forced
						((org.red5.io.amf3.Input) input).enforceAMF3();
						break;
					case AMF3.TYPE_VECTOR_INT:
					case AMF3.TYPE_VECTOR_UINT:
						// The next parameter is encoded using AMF3
						input = new org.red5.io.amf3.Input(in, refStorage);
						break;
					default:
						// The next parameter is encoded using AMF0
						input = new org.red5.io.amf.Input(in);
				}
				paramList.add(Deserializer.deserialize(input, Object.class));
			}
			params = paramList.toArray();
			if (log.isTraceEnabled()) {
				log.trace("Parameter count: {}", paramList.size());
				for (int i = 0; i < params.length; i++) {
					log.trace(" > {}: {}", i, params[i]);
				}
			}
		}
		final int dotIndex = action.lastIndexOf('.');
		String serviceName = (dotIndex == -1) ? null : action.substring(0, dotIndex);
		String serviceMethod = (dotIndex == -1) ? action : action.substring(dotIndex + 1, action.length());
		log.debug("Service name: {} method: {}", serviceName, serviceMethod);
		PendingCall call = new PendingCall(serviceName, serviceMethod, params);
		msg.setCall(call);
		return msg;
	}

	public Notify decodeFlexStreamSend(IoBuffer in) {
		// grab the initial limit
		int limit = in.limit();
		
		// remove the first byte
		in.position(1);
		in.compact();
		in.rewind();
		
		// set the limit back to the original minus the one
		// byte that we removed from the buffer
		in.limit(limit-1);
		
		return new FlexStreamSend(in.asReadOnlyBuffer());
	}

	/**
	 * Checks if the passed action is a reserved stream method.
	 * 
	 * @param action
	 *            Action to check
	 * @return <code>true</code> if passed action is a reserved stream method,
	 *         <code>false</code> otherwise
	 */
	private boolean isStreamCommand(String action) {
		switch (StreamAction.getEnum(action)) {
			case CREATE_STREAM:
			case DELETE_STREAM:
			case RELEASE_STREAM:
			case PUBLISH:
			case PLAY:
			case PLAY2:
			case SEEK:
			case PAUSE:
			case PAUSE_RAW:
			case CLOSE_STREAM:
			case RECEIVE_VIDEO:
			case RECEIVE_AUDIO:
				return true;
			default:
				log.debug("Stream action {} is not a recognized command", action);
				return false;
		}
	}

	/**
	 * Sets incoming connection parameters and / or returns encoded parameters for use in a call.
	 * 
	 * @param in
	 * @param notify
	 * @param input
	 * @return parameters array
	 */
	private Object[] handleParameters(IoBuffer in, Notify notify, Input input) {
		Object[] params = new Object[] {};
		if (in.hasRemaining()) {
			List<Object> paramList = new ArrayList<Object>();
			final Object obj = Deserializer.deserialize(input, Object.class);
			if (obj instanceof Map) {
				// Before the actual parameters we sometimes (connect) get a map of parameters, this is usually null, but if set should be
				// passed to the connection object.
				@SuppressWarnings("unchecked")
				final Map<String, Object> connParams = (Map<String, Object>) obj;
				notify.setConnectionParams(connParams);
			} else if (obj != null) {
				paramList.add(obj);
			}
			while (in.hasRemaining()) {
				paramList.add(Deserializer.deserialize(input, Object.class));
			}
			params = paramList.toArray();
			if (log.isDebugEnabled()) {
				log.debug("Num params: {}", paramList.size());
				for (int i = 0; i < params.length; i++) {
					log.debug(" > {}: {}", i, params[i]);
				}
			}
		}
		return params;
	}

}
