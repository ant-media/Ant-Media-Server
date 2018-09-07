package org.red5.server.net.rtmp;

import java.net.SocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestWrapper;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderException;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class RTMPChannelFilter extends ProtocolCodecFilter {

    public RTMPChannelFilter(ProtocolCodecFactory factory) {
        super(factory);
    }

    @Override
    public void messageSent(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        if (writeRequest instanceof EndOfMessage) {
            nextFilter.messageSent(session, ((EndOfMessage) writeRequest).getParentRequest());
        }
    }

    @Override
    public void filterWrite(NextFilter nextFilter, IoSession session, WriteRequest writeRequest) throws Exception {
        Object message = writeRequest.getMessage();
        ProtocolEncoder encoder = getEncoder(session);
        ProtocolEncoderOutput encoderOut = new DirectOutput(session, nextFilter, writeRequest);
        try {
            encoder.encode(session, message, encoderOut);
            nextFilter.filterWrite(session, new EndOfMessage(writeRequest));
        } catch (ProtocolEncoderException e) {
            throw e;
        } catch (Throwable t) {
            throw new ProtocolEncoderException(t);
        }
    }

    private static class DirectOutput implements ProtocolEncoderOutput {

        private final IoSession session;

        private final IoFilter.NextFilter nextFilter;

        private final WriteRequest writeRequest;

        public DirectOutput(IoSession session, IoFilter.NextFilter nextFilter, WriteRequest writeRequest) {
            this.session = session;
            this.nextFilter = nextFilter;
            this.writeRequest = writeRequest;
        }

        @Override
        public void write(Object encodedMessage) {
            IoBuffer buf = (IoBuffer) encodedMessage;
            if (buf.hasRemaining()) {
                SocketAddress destination = writeRequest.getDestination();
                WriteRequest writeRequest = new DefaultWriteRequest(encodedMessage, null, destination);
                nextFilter.filterWrite(session, writeRequest);
            } else {
                throw new IllegalArgumentException("encodedMessage is empty. Forgot to call flip?");
            }
        }

        @Override
        public WriteFuture flush() {
            return null;
        }

        @Override
        public void mergeAll() {
        }

    }

    private static class EndOfMessage extends WriteRequestWrapper {

        private static final IoBuffer EMPTY_BUFFER = IoBuffer.wrap(new byte[0]);

        public EndOfMessage(WriteRequest writeRequest) {
            super(writeRequest);
        }

        @Override
        public Object getMessage() {
            return EMPTY_BUFFER;
        }

    }

}
