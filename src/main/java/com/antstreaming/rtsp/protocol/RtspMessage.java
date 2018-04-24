package com.antstreaming.rtsp.protocol;

import java.nio.CharBuffer;

/**
 * Base abstract class for RTSP messages.
 */
public abstract class RtspMessage {

  // CRLF
  public static final String CRLF = "\r\n";

  /**
   * RTSP Message Type
   */
  public enum Type {
    /** Generic message (internal use) */
    TypeNone,
    /** Request message */
    TypeRequest,
    /** Response message */
    TypeResponse
  };

  private Long sequenceNumber;
  private SafeProperties headers;
  private StringBuilder buffer;
  private StringBuilder originRequest;

  /**
   * Constructor.
   */
  public RtspMessage() {
    sequenceNumber = 0L;
    headers = new SafeProperties();
    buffer = new StringBuilder();
    originRequest = new StringBuilder();
  }

  /**
   * @return the RTSP type of the message
   */
  public Type getType() {
    return Type.TypeNone;
  }

  /**
   * Adds a new header to the RTSP message.
   * 
   * @param key The name of the header
   * @param value Its value
   */
  public void setHeader(String key, String value) {
    // Handle some bad formatted headers
    if (key.compareToIgnoreCase("content-length") == 0) {
      headers.put("Content-Length", value);
    } else if (value != null) {
      if (value.startsWith(" ")) {
        headers.put(key, value);
      } else {
        headers.put(key, " " + value);
      }
    }
  }

  /**
   * Adds a new header to the RTSP message.
   * 
   * @param key The name of the header
   * @param value Its value
   */
  public void setHeader(RtspHeaderCode key, String value) {
    // Handle some bad formatted headers
    if (key.value().compareToIgnoreCase("content-length") == 0) {
      headers.put("Content-Length", value);
    } else if (value != null) {
      if (value.startsWith(" ")) {
        headers.put(key.value(), value);
      } else {
        headers.put(key.value(), " " + value);
      }
    }
  }

  /**
   * @param key Header name
   * @return the value of the header
   */
  public String getHeader(String key) {
    String value = headers.getProperty(key);
    if (value != null) return value.trim();
    return value;
  }

  /**
   * @param key Header name
   * @return the value of the header
   */
  public String getHeader(RtspHeaderCode key) {
    String value = headers.getProperty(key.value());
    if (value != null) return value.trim();
    return value;
  }

  /**
   * 
   * @param key Header name
   * @param defaultValue the default value
   * @return the value of the header of <i>defaultValue</i> if header is not found
   */
  public String getHeader(String key, String defaultValue) {
    String value = getHeader(key);
    if (value == null)
      return defaultValue;
    else
      return value;
  }

  /**
   * Formats all the headers into a string ready to be sent in a RTSP message.
   * 
   * <pre>
	 * Header1: Value1
	 * Header2: value 2
	 * ... 
	 * </pre>
   * 
   * @return a string containing the serialzed headers
   */
  public String getHeadersString() {
    return headers.getString();
  }

  /**
   * 
   * @return the number of headers owned by the message
   */
  public int getHeadersCount() {
    return headers.size();
  }

  public SafeProperties getHeaders() {
    return headers;
  }

  /**
   * Remove an header from the message headers collection
   * 
   * @param key the name of the header
   */
  public void removeHeader(String key) {
    headers.remove(key);
  }

  /**
   * 
   * @param buffer StringBuilder containing the contents
   */
  public void setBuffer(StringBuilder buffer) {
    this.buffer = buffer;
  }

  /**
   * @return the content buffer
   */
  public StringBuilder getBuffer() {
    return buffer;
  }

  /**
   * @param other buffer with content to be appended
   */
  public void appendToBuffer(StringBuilder other) {
    this.buffer.append(other);
  }

  /**
   * @param other buffer with content to be appended
   */
  public void appendToBuffer(CharBuffer other) {
    this.buffer.append(other);
  }

  /**
   * @return the size of the content buffer
   */
  public int getBufferSize() {
    return buffer.toString().length();
  }

  /**
   * @return Returns the sequenceNumber.
   */
  public Long getSequenceNumber() {
    return sequenceNumber;
  }

  /**
   * @param sequenceNumber The sequenceNumber to set.
   */
  public void setSequenceNumber(Long sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public void saveOriginRequest(String line) {
    originRequest.append(line);
  }

  public void saveOriginRequest(StringBuilder body) {
    originRequest.append(body);
  }

  public StringBuilder getOriginRequest() {
    return originRequest;
  }
}
