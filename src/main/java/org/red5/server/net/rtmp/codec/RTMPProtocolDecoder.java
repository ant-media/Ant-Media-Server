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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.amf.Output;
import org.red5.io.amf3.AMF3;
import org.red5.io.object.DataTypes;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Input;
import org.red5.io.object.StreamAction;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.Red5;
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
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.SWFResponse;
import org.red5.server.net.rtmp.event.ServerBW;
import org.red5.server.net.rtmp.event.SetBuffer;
import org.red5.server.net.rtmp.event.Unknown;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.net.rtmp.message.ChunkHeader;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.service.PendingCall;
import org.red5.server.stream.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP protocol decoder.
 */
public class RTMPProtocolDecoder implements Constants, IEventDecoder {

    protected static final Logger log = LoggerFactory.getLogger(RTMPProtocolDecoder.class);

    // close when header errors occur
    protected boolean closeOnHeaderError;

    // maximum size for an RTMP packet in Mb
    protected static int MAX_PACKET_SIZE = 3145728; // 3MB

    /** Constructs a new RTMPProtocolDecoder. */
    public RTMPProtocolDecoder() {
    }

    /**
     * Decode all available objects in buffer.
     * 
     * @param conn
     *            RTMP connection
     * @param buffer
     *            IoBuffer of data to be decoded
     * @return a list of decoded objects, may be empty if nothing could be decoded
     */
    public List<Object> decodeBuffer(RTMPConnection conn, IoBuffer buffer) {
        final int position = buffer.position();
        if (log.isTraceEnabled()) {
            log.trace("decodeBuffer: {}", Hex.encodeHexString(Arrays.copyOfRange(buffer.array(), position, buffer.limit())));
        }
        // decoded results
        List<Object> result = null;
        if (conn != null) {
            log.trace("Decoding for connection - session id: {}", conn.getSessionId());
            try {
                // instance list to hold results
                result = new LinkedList<Object>();
                // get the local decode state
                RTMPDecodeState state = conn.getDecoderState();
                log.trace("RTMP decode state {}", state);
                if (!conn.getSessionId().equals(state.getSessionId())) {
                    log.warn("Session decode overlap: {} != {}", conn.getSessionId(), state.getSessionId());
                }
                int remaining;
                while ((remaining = buffer.remaining()) > 0) {
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
                    
                    if (conn.getStateCode() == RTMP.STATE_DISCONNECTED) {
                    	log.trace("breaking the loop because connection is disconnected");
                    	break;
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to decodeBuffer: pos {}, limit {}, chunk size {}, buffer {}", position, buffer.limit(), conn.getState().getReadChunkSize(), Hex.encodeHexString(Arrays.copyOfRange(buffer.array(), position, buffer.limit())));
                // catch any non-handshake exception in the decoding; close the connection
                log.warn("Closing connection because decoding failed: {}", conn, ex);
                // clear the buffer to eliminate memory leaks when we can't parse protocol
                buffer.clear();
                // close connection because we can't parse data from it
                conn.close();
            } finally {
                if (log.isTraceEnabled()) {
                    log.trace("decodeBuffer - post decode input buffer position: {} remaining: {}", buffer.position(), buffer.remaining());
                }
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
     * @param conn
     *            RTMP connection
     * @param state
     *            Stores state for the protocol, ProtocolState is just a marker interface
     * @param in
     *            IoBuffer of data to be decoded
     * @return one of three possible values:
     * 
     *         <pre>
     * 1. null : the object could not be decoded, or some data was skipped, just continue 
     * 2. ProtocolState : the decoder was unable to decode the whole object, refer to the protocol state 
     * 3. Object : something was decoded, continue
     * </pre>
     * @throws ProtocolException
     *             on error
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
                case RTMP.STATE_ERROR:
                case RTMP.STATE_DISCONNECTING:
                case RTMP.STATE_DISCONNECTED:
                    // throw away any remaining input data:
                    in.clear();
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
     * Decodes an IoBuffer into a Packet.
     * 
     * @param conn
     *            Connection
     * @param state
     *            RTMP protocol state
     * @param in
     *            IoBuffer
     * @return Packet
     */
    public Packet decodePacket(RTMPConnection conn, RTMPDecodeState state, IoBuffer in) {
        final int position = in.position();
        if (log.isTraceEnabled()) {
            //log.trace("decodePacket - state: {} buffer: {}", state, in);
            //log.trace("decodePacket: position {}, limit {}, {}", position, in.limit(), Hex.encodeHexString(Arrays.copyOfRange(in.array(), position, in.limit())));
            log.trace("decodePacket: position {}, limit {}", position, in.limit());
        }
        // get RTMP state holder
        RTMP rtmp = conn.getState();
        // read the chunk header (variable from 1-3 bytes)
        final ChunkHeader chunkHeader = ChunkHeader.read(in);
        // represents "packet" header length via "format" only 1 byte in the chunk header is needed here
        int headerLength = RTMPUtils.getHeaderLength(chunkHeader.getFormat());
        headerLength += chunkHeader.getSize() - 1;
        //minus 1 because the first byte is already read in  ChunkHeader.read(in)
        
        //Check if extended timestamp exists -> https://rtmp.veriskope.com/docs/spec/#54-protocol-control-messages
        
        int extendedTimestampLength = 0;
        int lastPositionBeforeCheck = in.position();

        switch (chunkHeader.getFormat()) {
	        case  HEADER_NEW:
	        case  HEADER_SAME_SOURCE:
	        case  HEADER_TIMER_CHANGE:
	            /*
	             * The presence of this field is indicated by setting the timestamp field of a Type 0 chunk, or the timestamp delta field of a 
	             * Type 1 or 2 chunk, to 16777215 (0xFFFFFF). 
	             * This field is present in Type 3 chunks when the most recent Type 0, 1, or 2 chunk for the same chunk stream ID indicated 
	             * the presence of an extended timestamp field.
	             */
	        	
	        	//according to the reference first 3 bytes are timestamp
	        	//if timestamp is 0xFFFFFF, then it means there is extended timestamp with 4 bytes
	        	if (in.remaining() >= 3) 
	        	{
	        		//following read is 3 byte so make sure there are 3 bytes in the buffer
		        	int timeValue = RTMPUtils.readUnsignedMediumInt(in);
		        	if (timeValue == 0xffffff) {
		        		log.info("Extended timestamp exists because timevalue is 0xffffff chunkheader:{}",chunkHeader);
		        		extendedTimestampLength = 4;
		        	}
	        	}
	        	break;
	        case HEADER_CONTINUE:
	        	Header lastHeader = rtmp.getLastReadHeader(chunkHeader.getChannelId());
	        	/*
	        	* This field is present in Type 3 chunks when the most recent Type 0, 1, or 2 chunk for the same chunk stream ID indicated 
	            * the presence of an extended timestamp field.
	            * */
	        	if (lastHeader != null && lastHeader.getExtendedTimestamp() != 0) {
	        		log.info("Extended timestamp exists in HEADER_CONTINUE last header:{} chunk header:{}", lastHeader, chunkHeader);
	        		extendedTimestampLength = 4;
	        	}
	        	break;
	        default:
	        	break;
        }
        
        //reset the position to not break anything
        in.position(lastPositionBeforeCheck);
        
        headerLength += extendedTimestampLength;
        
        
        if (in.remaining() < headerLength) {
        	log.trace("Buffer remaining:{} is smaller than required header length: {}", in.remaining(), headerLength);
            state.bufferDecoding(headerLength - in.remaining());
            in.position(position);
            return null;
        }
        
        final Header header = decodeHeader(chunkHeader, state, in, rtmp);
        // get the channel id
        final int channelId = header != null ? header.getChannelId() : chunkHeader.getChannelId();
        if (header == null || header.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("Header was null or empty - chh: {}", chunkHeader);
            }
            // clear / compact the input and close the channel
            in.clear();
            in.compact();
            // send a NetStream.Failed message
            StreamService.sendNetStreamStatus(conn, StatusCodes.NS_FAILED, "Bad data on channel: " + channelId, "no-name", Status.ERROR, conn.getStreamIdForChannelId(channelId));
            // close the channel on which the issue occurred, until we find a way to exclude the current data
            conn.closeChannel(channelId);
            return null;
        }
        // ensure that we dont exceed maximum packet size
        int size = header.getSize();
        if (size > MAX_PACKET_SIZE) {
            // Reject packets that are too big, to protect against OOM when decoding has failed in some way
            log.warn("Packet size exceeded. size={}, max={}, connId={}", header.getSize(), MAX_PACKET_SIZE, conn.getSessionId());
            // send a NetStream.Failed message
            StreamService.sendNetStreamStatus(conn, StatusCodes.NS_FAILED, "Data exceeded maximum allowed by " + (size - MAX_PACKET_SIZE) + " bytes", "no-name", Status.ERROR, conn.getStreamIdForChannelId(channelId));
            throw new ProtocolException(String.format("Packet size exceeded. size: %s", header.getSize()));
        }
        // get the size of our chunks
        int readChunkSize = rtmp.getReadChunkSize();
        // store the header based on its channel id
        rtmp.setLastReadHeader(channelId, header);
        // check to see if this is a new packet or continue decoding an existing one
        Packet packet = rtmp.getLastReadPacket(channelId);
        if (packet == null) {
            // create a new packet
            packet = new Packet(header.clone());
            // store the packet based on its channel id
            rtmp.setLastReadPacket(channelId, packet);
        }
        // get the packet data
        IoBuffer buf = packet.getData();
        if (log.isTraceEnabled()) {
            log.trace("Source buffer position: {}, limit: {}, packet-buf.position {}, packet size: {}", new Object[] { in.position(), in.limit(), buf.position(), header.getSize() });
        }
        // read chunk
        int length = Math.min(buf.remaining(), readChunkSize);
        if (in.remaining() < length) {
            log.debug("Chunk too small, buffering ({},{})", in.remaining(), length);
            // how much more data we need to continue?
            state.bufferDecoding(in.position() - position + length);
            // we need to move back position so header will be available during another decode round
            in.position(position);
            return null;
        }
        // get the chunk from our input
        byte[] chunk = Arrays.copyOfRange(in.array(), in.position(), in.position() + length);
        if (log.isTraceEnabled()) {
            log.trace("Read chunkSize: {}, length: {}, chunk: {}", readChunkSize, length, Hex.encodeHexString(chunk));
        }
        // move the position
        in.skip(length);
        // put the chunk into the packet
        buf.put(chunk);
        if (buf.hasRemaining()) {
            log.trace("Packet is incomplete ({},{})", buf.remaining(), buf.limit());
            return null;
        }
        // flip so we can read / decode the packet data inot a message
        buf.flip();
        try {
            final IRTMPEvent message = decodeMessage(conn, packet.getHeader(), buf);
            if (log.isTraceEnabled()) {
                log.trace("Decoded message: {}", message);
            }
            // flash will send an earlier time stamp when resetting a video stream with a new key frame. To avoid dropping it,
            // we give it the minimal increment since the last message. To avoid relative time stamps being mis-computed, we
            // don't reset the header we stored.
            final Header lastReadHeader = rtmp.getLastReadPacketHeader(channelId);
            if (lastReadHeader != null && (message instanceof AudioData || message instanceof VideoData) && RTMPUtils.compareTimestamps(lastReadHeader.getTimer(), packet.getHeader().getTimer()) >= 0) {
                log.trace("Non-monotonically increasing timestamps; type: {}; adjusting to {}; ts: {}; last: {}", new Object[] { header.getDataType(), lastReadHeader.getTimer() + 1, header.getTimer(), lastReadHeader.getTimer() });
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
                // client is aborting a message, reset the packet because the next chunk will start a new packet
                Abort abort = (Abort) message;
                packet = null;
                rtmp.setLastReadPacket(abort.getChannelId(), packet);
            }
            // collapse the time stamps on the last packet so that it works right for chunk type 3 later
            Header lastHeader = rtmp.getLastReadHeader(channelId);
            lastHeader.setTimerBase(header.getTimer());
        } finally {
            rtmp.setLastReadPacket(channelId, null);
        }
        return packet;
    }

    /**
     * Decodes packet header.
     * 
     * @param chh
     *            chunk header
     * @param state
     *            RTMP decode state
     * @param in
     *            Input IoBuffer
     * @param rtmp
     *            RTMP object to get last header
     * @return Decoded header
     */
    public Header decodeHeader(ChunkHeader chh, RTMPDecodeState state, IoBuffer in, RTMP rtmp) {
        if (log.isTraceEnabled()) {
            //log.trace("decodeHeader - chh: {} input: {}", chh, Hex.encodeHexString(Arrays.copyOfRange(in.array(), in.position(), in.limit())));
            log.trace("decodeHeader - chh: {}", chh);
        }
        final int remaining = in.remaining();
        final int channelId = chh.getChannelId();
        final byte headerSize = chh.getFormat();
        Header lastHeader = rtmp.getLastReadHeader(channelId);
        if (log.isTraceEnabled()) {
            log.trace("lastHeader: {}", lastHeader);
        }
        // got a non-new header for a channel which has no last-read header
        if (headerSize != HEADER_NEW && lastHeader == null) {
            String detail = String.format("Last header null: %s, channelId %s", Header.HeaderType.values()[headerSize], channelId);
            log.debug("{}", detail);
            // if the op prefers to exit or kill the connection, we should allow based on configuration param
            if (closeOnHeaderError) {
                // this will trigger an error status, which in turn will disconnect the "offending" flash player
                // preventing a memory leak and bringing the whole server to its knees
                throw new ProtocolException(detail);
            } else {
                // we need to skip the current channel data and continue until a new header is sent
                return null;
            }
        }
        int headerLength = RTMPUtils.getHeaderLength(headerSize);
        headerLength += chh.getSize() - 1;
        if (log.isTraceEnabled()) {
            log.trace("headerLength: {}", headerLength);
        }
        if (remaining < headerLength) {
            log.trace("Header too small (hlen: {}), buffering. remaining: {}", headerLength, remaining);
            state.bufferDecoding(headerLength);
            return null;
        }
        int timeValue;
        Header header = new Header();
        header.setChannelId(channelId);
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
                    log.trace("Audio with zero delta; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(), headerSize });
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
                    log.trace("Audio with zero delta; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(), headerSize });
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
                throw new ProtocolException(String.format("Unexpected header size: %s", headerSize));
        }
        log.trace("CHUNK, D, {}, {}", header, headerSize);
        return header;
    }

    /**
     * Decodes RTMP message event.
     * 
     * @param conn
     *            RTMP connection
     * @param header
     *            RTMP header
     * @param in
     *            Input IoBuffer
     * @return RTMP event
     */
    public IRTMPEvent decodeMessage(RTMPConnection conn, Header header, IoBuffer in) {
        IRTMPEvent message = null;
        byte dataType = header.getDataType();
        switch (dataType) {
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
            case TYPE_FLEX_MESSAGE:
                message = decodeFlexMessage(in);
                break;
            case TYPE_INVOKE:
                message = decodeAction(conn.getEncoding(), in, header);
                break;
            case TYPE_FLEX_STREAM_SEND:
                if (log.isTraceEnabled()) {
                    log.trace("Decoding flex stream send on stream id: {}", header.getStreamId());
                }
                // skip first byte
                in.get();
                // decode stream data; slice from the current position
                message = decodeStreamData(in.slice());
                break;
            case TYPE_NOTIFY:
                if (log.isTraceEnabled()) {
                    log.trace("Decoding notify on stream id: {}", header.getStreamId());
                }
                if (header.getStreamId().doubleValue() != 0.0d) {
                    message = decodeStreamData(in);
                } else {
                    message = decodeAction(conn.getEncoding(), in, header);
                }
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
            case TYPE_FLEX_SHARED_OBJECT:
            case TYPE_SHARED_OBJECT:// represents an SO in an AMF3 container
            	log.warn("shared object is not supported");
            	message = decodeUnknown(dataType, in);
            	break;
            default:
                log.warn("Unknown object type: {}", dataType);
                message = decodeUnknown(dataType, in);
                break;
        }
        // add the header to the message
        
        message.setHeader(header);
        return message;
    }

    public IRTMPEvent decodeAbort(IoBuffer in) {
        return new Abort(in.getInt());
    }

    /**
     * Decodes server bandwidth.
     * 
     * @param in
     *            IoBuffer
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
        if (log.isDebugEnabled()) {
            log.debug("decodeUnknown: {}", dataType);
        }
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

    /**
     * Decode the 'action' for a supplied an Invoke.
     * 
     * @param encoding
     *            AMF encoding
     * @param in
     *            buffer
     * @param header
     *            data header
     * @return notify
     */
    private Invoke decodeAction(Encoding encoding, IoBuffer in, Header header) {
        // for response, the action string and invokeId is always encoded as AMF0 we use the first byte to decide which encoding to use
        in.mark();
        byte tmp = in.get();
        in.reset();
        Input input;
        if (encoding == Encoding.AMF3 && tmp == AMF.TYPE_AMF3_OBJECT) {
            input = new org.red5.io.amf3.Input(in);
            ((org.red5.io.amf3.Input) input).enforceAMF3();
        } else {
            input = new org.red5.io.amf.Input(in);
        }
        // get the action
        String action = Deserializer.deserialize(input, String.class);
        if (action == null) {
            throw new RuntimeException("Action was null");
        }
        if (log.isTraceEnabled()) {
            log.trace("Action: {}", action);
        }
        // instance the invoke
        Invoke invoke = new Invoke();
        // set the transaction id
        invoke.setTransactionId(Deserializer.<Number> deserialize(input, Number.class).intValue());
        // reset and decode parameters
        input.reset();
        // get / set the parameters if there any
        Object[] params = in.hasRemaining() ? handleParameters(in, invoke, input) : new Object[0];
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
        // create the pending call for invoke
        PendingCall call = new PendingCall(serviceName, serviceMethod, params);
        invoke.setCall(call);
        return invoke;
    }

    /**
     * Decodes ping event.
     * 
     * @param in
     *            IoBuffer
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
     * Decodes stream data, to include onMetaData, onCuePoint, and onFI.
     * 
     * @param in
     *            input buffer
     * @return Notify
     */
    @SuppressWarnings("unchecked")
    public Notify decodeStreamData(IoBuffer in) {
        if (log.isDebugEnabled()) {
            log.debug("decodeStreamData");
        }
        // our result is a notify
        Notify ret = null;
        // check the encoding, if its AMF3 check to see if first byte is set to AMF0
        Encoding encoding = ((RTMPConnection) Red5.getConnectionLocal()).getEncoding();
        log.trace("Encoding: {}", encoding);
        // set mark
        in.mark();
        // create input using AMF0 to start with
        Input input = new org.red5.io.amf.Input(in);
        if (encoding == Encoding.AMF3) {
            log.trace("Client indicates its using AMF3");
        }
        //get the first datatype
        byte dataType = input.readDataType();
        log.debug("Data type: {}", dataType);
        if (dataType == DataTypes.CORE_STRING) {
            String action = input.readString();
            if ("@setDataFrame".equals(action)) {
                // get the second datatype
                byte dataType2 = input.readDataType();
                log.debug("Dataframe method type: {}", dataType2);
                String onCueOrOnMeta = input.readString();
                // get the params datatype
                byte object = input.readDataType();
                if (object == DataTypes.CORE_SWITCH) {
                    log.trace("Switching decoding to AMF3");
                    input = new org.red5.io.amf3.Input(in);
                    ((org.red5.io.amf3.Input) input).enforceAMF3();
                    // re-read data type after switching decode
                    object = input.readDataType();
                }
                log.debug("Dataframe params type: {}", object);
                Map<Object, Object> params = Collections.EMPTY_MAP;
                if (object == DataTypes.CORE_MAP) {
                    // the params are sent as a Mixed-Array. Required to support the RTMP publish provided by ffmpeg
                    params = (Map<Object, Object>) input.readMap();
                } else if (object == DataTypes.CORE_ARRAY) {
                    params = (Map<Object, Object>) input.readArray(Object[].class);
                } else if (object == DataTypes.CORE_STRING) {
                    // decode the string and drop-in as first map entry since we dont know how its encoded
                    String str = input.readString();
                    log.debug("String params: {}", str);
                    params = new HashMap<>();
                    params.put("0", str);
                    //} else if (object == DataTypes.CORE_OBJECT) {
                    //    params = (Map<Object, Object>) input.readObject();
                } else {
                    try {
                        // read the params as a standard object
                        params = (Map<Object, Object>) input.readObject();
                    } catch (Exception e) {
                        log.warn("Dataframe decode error", e);
                        params = Collections.EMPTY_MAP;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Dataframe: {} params: {}", onCueOrOnMeta, params.toString());
                }
                IoBuffer buf = IoBuffer.allocate(64);
                buf.setAutoExpand(true);
                Output out = new Output(buf);
                out.writeString(onCueOrOnMeta);
                out.writeMap(params);
                buf.flip();
                // instance a notify with action
                ret = new Notify(buf, onCueOrOnMeta);
            } else {
                byte object = input.readDataType();
                if (object == DataTypes.CORE_SWITCH) {
                    log.trace("Switching decoding to AMF3");
                    input = new org.red5.io.amf3.Input(in);
                    ((org.red5.io.amf3.Input) input).enforceAMF3();
                    // re-read data type after switching decode
                    object = input.readDataType();
                }
                // onFI
                // the onFI request contains 2 items relative to the publishing client application
                // sd = system date (12-07-2011) st = system time (09:11:33.387)
                log.info("Stream send: {}", action);
                Map<Object, Object> params = Collections.EMPTY_MAP;
                log.debug("Params type: {}", object);
                if (object == DataTypes.CORE_MAP) {
                    params = (Map<Object, Object>) input.readMap();
                    if (log.isDebugEnabled()) {
                        log.debug("Map params: {}", params.toString());
                    }
                } else if (object == DataTypes.CORE_ARRAY) {
                    params = (Map<Object, Object>) input.readArray(Object[].class);
                    if (log.isDebugEnabled()) {
                        log.debug("Array params: {}", params);
                    }
                } else if (object == DataTypes.CORE_STRING) {
                    String str = input.readString();
                    if (log.isDebugEnabled()) {
                        log.debug("String params: {}", str);
                    }
                    params = new HashMap<>();
                    params.put("0", str);
                } else if (object == DataTypes.CORE_OBJECT) {
                    params = (Map<Object, Object>) input.readObject();
                    if (log.isDebugEnabled()) {
                        log.debug("Object params: {}", params);
                    }
                } else if (log.isDebugEnabled()) {
                    log.debug("Stream send did not provide a parameter map");
                }
                // need to debug this further
                if ("onFI".equals(action)) {
	                IoBuffer buf = IoBuffer.allocate(64);
	                buf.setAutoExpand(true);
	                Output out = null;
	                if (encoding == Encoding.AMF3) {
	                    out = new org.red5.io.amf3.Output(buf);
	                } else {
	                    out = new Output(buf);
	                }
	                out.writeString(action);
	                out.writeMap(params);
	                buf.flip();
	                // instance a notify with action
	                ret = new Notify(buf, action);
                }
                else {
	                // go back to the beginning
	                in.reset();
	                // instance a notify with action
	                ret = new Notify(in.asReadOnlyBuffer(), action);
                }
            }
        } else {
            // go back to the beginning
            in.reset();
            // instance a notify
            ret = new Notify(in.asReadOnlyBuffer());
        }
        return ret;
    }

    /**
     * Decodes FlexMessage event.
     * 
     * @param in
     *            IoBuffer
     * @return FlexMessage event
     */
    public FlexMessage decodeFlexMessage(IoBuffer in) {
        if (log.isDebugEnabled()) {
            log.debug("decodeFlexMessage");
        }
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
            ArrayList<Object> paramList = new ArrayList<>();
            final Object obj = Deserializer.deserialize(input, Object.class);
            if (obj != null) {
                paramList.add(obj);
            }
            while (in.hasRemaining()) {
                // Check for AMF3 encoding of parameters
                byte objectEncodingType = in.get();
                log.debug("Object encoding: {}", objectEncodingType);
                in.position(in.position() - 1);
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

    /**
     * Sets whether or not a header error on any channel should result in a closed connection.
     * 
     * @param closeOnHeaderError
     *            true to close on header decode errors
     */
    public void setCloseOnHeaderError(boolean closeOnHeaderError) {
        this.closeOnHeaderError = closeOnHeaderError;
    }

    /**
     * Checks if the passed action is a reserved stream method.
     * 
     * @param action
     *            Action to check
     * @return true if passed action is a reserved stream method, false otherwise
     */
    @SuppressWarnings("unused")
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
        List<Object> paramList = new ArrayList<>();
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
        return params;
    }

    /**
     * Set the maximum allowed packet size. Default is 3 Mb.
     * 
     * @param maxPacketSize
     */
    public static void setMaxPacketSize(int maxPacketSize) {
        MAX_PACKET_SIZE = maxPacketSize;
        if (log.isDebugEnabled()) {
            log.debug("Max packet size: {}", MAX_PACKET_SIZE);
        }
    }

}
