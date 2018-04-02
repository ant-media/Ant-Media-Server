package com.antstreaming.rtsp;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.red5.server.jmx.mxbeans.RTMPMinaTransportMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antstreaming.rtsp.protocol.MessageCodecFactory;


public class RtspMinaTransport implements RTMPMinaTransportMXBean {

	private static final Logger log = LoggerFactory.getLogger(RtspMinaTransport.class);

	private NioSocketAcceptor acceptor;
	private IoHandler rtspMessageHandler;
	protected Set<String> addresses = new HashSet<String>();
	
	private static final int MIN_READ_BUFFER_SIZE = 4096;

	protected IoHandlerAdapter ioHandler;

	public void setIoHandler(IoHandlerAdapter rtmpIOHandler) {
		this.ioHandler = rtmpIOHandler;
		log.info("setting IoHandler");
	}


	@Override
	public void setIoThreads(int ioThreads) {

	}

	@Override
	public void setTcpNoDelay(boolean tcpNoDelay) {

	}

	@Override
	public void setUseHeapBuffers(boolean useHeapBuffers) {

	}

	@Override
	public String getAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStatistics() {

		return null;
	}

	public void setAddress(String address) {
		addresses.add(address);
		log.info("RTSP will be bound to {}", address);
	}

	public void setAddresses(List<String> addrs) {
		for (String addr : addrs) {
			addresses.add(addr);
		}
		log.info("RTSP will be bound to {}", addresses);
	}


	@Override
	public void start() throws Exception {
		
		acceptor = new NioSocketAcceptor(Runtime.getRuntime().availableProcessors() + 1);
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(new MessageCodecFactory()));
		acceptor.getFilterChain().addLast("threadPool",
				new ExecutorFilter(Executors.newCachedThreadPool()));
		acceptor.setHandler(ioHandler);
		acceptor.getSessionConfig().setMinReadBufferSize(MIN_READ_BUFFER_SIZE);


		Set<InetSocketAddress> socketAddresses = new HashSet<InetSocketAddress>();
		for (String addr : addresses) {
			if (addr.indexOf(':') != -1) {
				String[] parts = addr.split(":");
				socketAddresses.add(new InetSocketAddress(parts[0], Integer.valueOf(parts[1])));
			} else {
				socketAddresses.add(new InetSocketAddress(addr, 5554));
			}
		}
		log.info("RTSP Binding to {}", socketAddresses.toString());
		acceptor.bind(socketAddresses);
	}

	/*
	private void initIoHandler() {
		 if (ioHandler == null) {
	            log.info("No RTMP IO Handler associated - using defaults");
	            ioHandler = new RtspMinaIoHandler();
	        }
		
	}
	*/




	@Override
	public void stop() {
		acceptor.unbind();

	}

}
