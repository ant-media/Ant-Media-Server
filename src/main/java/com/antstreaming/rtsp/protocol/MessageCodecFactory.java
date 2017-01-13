package com.antstreaming.rtsp.protocol;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class MessageCodecFactory implements ProtocolCodecFactory {

  public ProtocolDecoder getDecoder(IoSession session) throws Exception {
    return new RtspDecoder();
  }

  public ProtocolEncoder getEncoder(IoSession session) throws Exception {
    return new RtspEncoder();
  }

}
