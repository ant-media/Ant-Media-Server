package org.red5.server.net.rtmps;

import java.util.List;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.red5.server.net.rtmp.RTMPMinaTransport;

public class MockRTMPMinaTransport extends RTMPMinaTransport {


	@Override
	public void setIoHandler(IoHandlerAdapter rtmpIOHandler) {
		//no-op
	}
	@Override
	public void setAddresses(List<String> addrs) {
		//no-op
	}
	@Override
	public void setIoThreads(int ioThreads) {
		//no-op
	}
	@Override
	public void setTcpNoDelay(boolean tcpNoDelay) {
		//no-op
	}


	@Override
	public void start() throws Exception {
		//no-op
	}

	@Override
	public void stop() {
		//no-op
	}

}
