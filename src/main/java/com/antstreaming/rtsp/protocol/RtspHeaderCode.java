package com.antstreaming.rtsp.protocol;

/**
 * RtspHeaderCode.
 */
public enum RtspHeaderCode {
  CSEQ("CSeq"), REQUIRE("Require"), SESSIONGROUP("SessionGroup"), ENCRYPTIONTYPE("EncryptionType"), CAS_ID(
      "CAS_ID"), ENCRYPTCONTROL("EncryptControl"), ENCRYPTION_SCHEME("encryption_scheme"), TRANSPORT(
      "Transport"), DATE("Date"), SESSION("Session"), USERAGENT("User-Agent"), ACCEPT("Accept"), MAYNOTIFY(
      "May-Notify"), UUDATA("UUData"), EMBEDDED_ENCRYPTOR("EmbeddedEncryptor"), EVENT_DATE(
      "event-date"), SECURE_DATA("SecureData"), LOCATION("Location"), USER_NOTIFICATION(
      "User-Notifiacation"), RANGE("Range"), SCALE("Scale"), NOTICE("Notice"), PUBLIC("Public"), SERVER(
      "Server"), CONTENT_LENGTH("Content-Length"), CONTENT_TYPE("Content-Type"), CONNECTION(
      "Connection"), ON_DEMAND_SESSION_ID("OnDemandSessionId"), INBAND_MARKER("InbandMarker"), INSERT_DURATION(
      "insertDuration"), POLICY("Policy"), REASON("Reason");


  private final String value;

  private RtspHeaderCode(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

}
