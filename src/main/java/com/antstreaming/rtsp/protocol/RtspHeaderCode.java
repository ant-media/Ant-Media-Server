package com.antstreaming.rtsp.protocol;

/**
 * RtspHeaderCode.
 */
public enum RtspHeaderCode {
  CSeq("CSeq"), Require("Require"), SessionGroup("SessionGroup"), EncryptionType("EncryptionType"), CAS_ID(
      "CAS_ID"), EncryptControl("EncryptControl"), encryption_scheme("encryption_scheme"), Transport(
      "Transport"), Date("Date"), Session("Session"), UserAgent("User-Agent"), Accept("Accept"), MayNotify(
      "May-Notify"), UUData("UUData"), EmbeddedEncryptor("EmbeddedEncryptor"), eventDate(
      "event-date"), SecureData("SecureData"), Location("Location"), UserNotifiacation(
      "User-Notifiacation"), Range("Range"), Scale("Scale"), Notice("Notice"), Public("Public"), Server(
      "Server"), ContentLength("Content-Length"), ContentType("Content-Type"), Connection(
      "Connection"), OnDemandSessionId("OnDemandSessionId"), InbandMarker("InbandMarker"), insertDuration(
      "insertDuration"), Policy("Policy"), Reason("Reason");


  private final String value;

  private RtspHeaderCode(String value) {
    this.value = value;
  }

  public String value() {
    return this.value;
  }

}
