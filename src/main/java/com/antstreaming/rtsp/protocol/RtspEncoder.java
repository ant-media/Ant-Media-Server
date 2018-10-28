package com.antstreaming.rtsp.protocol;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import java.nio.charset.StandardCharsets;

public class RtspEncoder extends ProtocolEncoderAdapter {
  public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
    String request = message.toString();
    byte[] bs = request.getBytes(StandardCharsets.UTF_8);
    IoBuffer buffer = IoBuffer.allocate(bs.length, false);
    buffer.put(bs);
    buffer.flip();
    out.write(buffer);
  }
}
