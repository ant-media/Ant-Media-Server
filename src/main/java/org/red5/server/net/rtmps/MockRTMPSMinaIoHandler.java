package org.red5.server.net.rtmps;

import java.util.Set;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.red5.server.net.rtmp.IRTMPHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockRTMPSMinaIoHandler extends IoHandlerAdapter {

  private static Logger log = LoggerFactory.getLogger(MockRTMPSMinaIoHandler.class);

  /**
   * Mock RTMP events handler
   */
  protected IRTMPHandler handler;

  private String keystorePassword;

  private String keystoreFile;

  private String truststorePassword;

  private String truststoreFile;

  private String cipherSuites[];

  private String protocols[];

  private IoHandler ioHandler;

  protected Set<String> addresses;

  protected int ioThreads;

  protected boolean tcpNoDelay;

  public void setHandler(IRTMPHandler handler) {
    this.handler = handler;
  }

  public void setKeystorePassword(String keystorePassword) {
    this.keystorePassword = keystorePassword;
  }

  public void setKeystoreFile(String keystoreFile) {
    this.keystoreFile = keystoreFile;
  }

  public void setTruststorePassword(String truststorePassword) {
    this.truststorePassword = truststorePassword;
  }

  public void setTruststoreFile(String truststoreFile) {
    this.truststoreFile = truststoreFile;
  }

  public void setCipherSuites(String[] cipherSuites) {
    this.cipherSuites = cipherSuites;
  }

  public void setProtocols(String[] protocols) {
    this.protocols = protocols;
  }

  public void setIoHandler(IoHandler ioHandler) {
    this.ioHandler = ioHandler;
  }

  public void setAddresses(Set<String> addresses) {
    this.addresses = addresses;
  }

  public void setIoThreads(int ioThreads) {
    this.ioThreads = ioThreads;
  }

  public void setTcpNoDelay(boolean tcpNoDelay) {
    this.tcpNoDelay = tcpNoDelay;
  }

  public void start(){}

  public void stop(){}
}
