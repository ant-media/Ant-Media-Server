package com.antstreaming.rtsp.protocol;

import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antstreaming.rtsp.protocol.RtspRequest.Verb;


/**
 * rtsp response.
 */
public class RtspResponse extends RtspMessage {
  private static Logger logger = LoggerFactory.getLogger(RtspResponse.class);
  RtspCode code;
  Verb requestVerb = Verb.None;

  public RtspResponse() {
    super();
    code = RtspCode.OK;
  }

  @Override
  public Type getType() {
    return Type.TypeResponse;
  }

  public RtspCode getCode() {
    return code;
  }

  public void setCode(RtspCode code) {
    this.code = code;
  }

  public void setRequestVerb(Verb requestVerb) {
    this.requestVerb = requestVerb;
  }

  public Verb getRequestVerb() {
    return requestVerb;
  }

  /**
   * Serialize the RTSP response to a string.
   * 
   * <pre>
	 *    &quot;RTSP/1.0&quot; SP [code] SP [reason] CRLF
	 *    [headers] CRLF
	 *    CRLF
	 *    [buf] 
	 * </pre>
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("RTSP/1.0 ").append(code.value()).append(" ");
    sb.append(code.description()).append(CRLF);
    sb.append(getHeadersString());
    // Insert a blank line
    sb.append(CRLF);
    if (getBufferSize() > 0) {
      sb.append(getBuffer());
      logger.debug("Buffer Size: " + getBufferSize());
    }
    return sb.toString();
  }


  /*
   * serialize the RTSP response message into a byte buffer.
   * @return
   * @throws Exception
   */
  public ByteBuffer toByteBuffer() throws Exception {
    try {
      String msg = this.toString();
      ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes("UTF-8"));
      return buffer;
    } catch (Exception e) {
      logger.error("failed to serialize message to byte buffer", e);
      throw e;
    }
  }

  /**
   * Construct a new RtspResponse error message.
   * 
   * @param errorCode the RTSP error code to be sent
   * @return a RTSP response message
   */
  public static RtspResponse errorResponse(RtspCode errorCode) {
    RtspResponse response = new RtspResponse();
    response.setCode(errorCode);
    return response;
  }
}
