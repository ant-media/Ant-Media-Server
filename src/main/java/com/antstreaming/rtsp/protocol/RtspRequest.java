package com.antstreaming.rtsp.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * rtsp request.
 */
public class RtspRequest extends RtspMessage {
  private static Logger logger = LoggerFactory.getLogger(RtspRequest.class);

  public enum Verb {
    None, ANNOUNCE, DESCRIBE, GET_PARAMETER, OPTIONS, PAUSE, PLAY, RECORD, REDIRECT, SETUP, SET_PARAMETER, TEARDOWN, PING
  };

  private Verb verb;
  private String url;

  public RtspRequest() {
    super();
    verb = Verb.None;
  }

  /* ~~~~~~~~~~ Setter And Getter ~~~~~~~~~~~~~~~ */

  @Override
  public Type getType() {
    return Type.TypeRequest;
  }

  public String getVerbString() {
    return verb.toString();
  }

  public void setVerb(Verb verb) {
    this.verb = verb;
  }

  public Verb getVerb() {
    return verb;
  }

  public void setVerb(String strVerb) {
    try {
      this.verb = Verb.valueOf(strVerb);
    } catch (Exception e) {
      this.verb = Verb.None;
      logger.error("Invalid verb: " + strVerb);
    }
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /* ~~~~~~~~~~ Overide Method ~~~~~~~~~~~~~~~ */

  /**
   * Return a serialized version of the RTSP request message that will be sent over the network. The
   * message is in the form:
   * 
   * <pre>
	 * [verb] SP [url] SP "RTSP/1.0" CRLF
	 * [headers] CRLF
	 * CRLF 
	 * [buffer]
	 * </pre>
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getVerbString() + " ");
    sb.append(url != null ? url : "*");
    sb.append(" RTSP/1.0\r\n");
    sb.append(getHeadersString());

    // Insert a blank line
    sb.append(CRLF);

    if (getBufferSize() > 0) {
      sb.append(getBuffer());
    }

    return sb.toString();
  }
}
