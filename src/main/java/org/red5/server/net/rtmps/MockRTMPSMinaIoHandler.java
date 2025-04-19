package org.red5.server.net.rtmps;

import org.apache.mina.core.session.IoSession;
import org.red5.server.net.rtmp.IRTMPHandler;

public class MockRTMPSMinaIoHandler extends RTMPSMinaIoHandler {


	@Override
	public void setHandler(IRTMPHandler handler) {
		//no-op
	}

	@Override
	public void setKeystorePassword(String keystorePassword) {
		//no-op
	}

	@Override
	public void setKeystoreFile(String keystoreFile) {
		//no-op
	}

	@Override
	public void setTruststorePassword(String truststorePassword) {
		//no-op
	}
	
	@Override
	public void setTruststoreFile(String truststoreFile) {
		//no-op
	}
	
	@Override
	public void setCipherSuites(String[] cipherSuites) {
		//no-op
	}
	
	@Override
	public void setProtocols(String[] protocols) {
		//no-op
	}
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		//no-op
	}



}
