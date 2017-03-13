package com.antstreaming.rtsp;

import static org.bytedeco.javacpp.avformat.av_sdp_create;
import static org.bytedeco.javacpp.avformat.avformat_close_input;
import static org.bytedeco.javacpp.avformat.avformat_open_input;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.DatagramSessionConfig;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avformat.AVFormatContext;
import org.bytedeco.javacpp.avformat.AVOutputFormat;
import org.bytedeco.javacpp.avutil.AVRational;
import org.red5.server.api.IContext;
import org.red5.server.api.IServer;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.StreamService;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.antstreaming.rtsp.protocol.RTSPTransport;
import com.antstreaming.rtsp.protocol.RTSPTransport.LowerTransport;
import com.antstreaming.rtsp.protocol.RtspCode;
import com.antstreaming.rtsp.protocol.RtspHeaderCode;
import com.antstreaming.rtsp.protocol.RtspRequest;
import com.antstreaming.rtsp.protocol.RtspRequest.Verb;
import com.antstreaming.rtsp.protocol.RtspResponse;
import com.antstreaming.rtsp.session.DateUtil;

import static org.bytedeco.javacpp.avcodec.*;
import static org.bytedeco.javacpp.avformat.*;
import static org.bytedeco.javacpp.avutil.*;
import static org.bytedeco.javacpp.swscale.*;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacpp.avdevice.*;

@Component
public class RtspMinaIoHandler extends IoHandlerAdapter {

	private Logger logger = LoggerFactory.getLogger(RtspMinaIoHandler.class);
	private String serverAddr;
	private int serverPort;

	/**
	 * Red5 server instance.
	 */
	protected IServer server;

	/**
	 * Setter for server object.
	 * 
	 * @param server
	 *            Red5 server instance
	 */
	public void setServer(IServer server) {
		this.server = server;
		logger.debug("Server is set");
	}


	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		super.messageSent(session, message);
		logger.debug("sent message: "  + message.toString());
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		RtspConnection conn = (RtspConnection) RtspConnectionManager.getInstance().createConnection(RtspConnection.class);
		// add session to the connection
		conn.setIoSession(session);
		conn.setSession(session);
		conn.setServer(server);
		// add the handler
		//conn.setHandler();
		// add the connections session id for look up using the connection manager
		session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
		logger.debug("connection session id:" + conn.getSessionId());
	}


	@Override
	public void sessionOpened(IoSession session) throws Exception {
		super.sessionOpened(session);
		logger.debug("session opened with id " + session.getId());
		//RtspConnection rtspConnection = RtspConnectionManager.getInstance().createConnection(RtspConnection.class);
		//ogger.debug("rtsp connection is created " + rtspConnection);

		//rtspConnection.setSessionId(session.getId());
		//rtspConnection.setSession(session);
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		super.sessionClosed(session);
		logger.debug("session closed " + session);
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		RtspConnection rtspConnection = RtspConnectionManager.getInstance().removeConnection(sessionId);
		session.closeNow();
		rtspConnection.close();
	}

	public void exceptionCaught(IoSession session, Throwable cause) {
		logger.error(cause.getMessage(), cause);
		handleError(session, "0", RtspCode.InternalServerError);
		// session.close(true);
	}

	private void handleError(IoSession session, String cseq, RtspCode code) {
		RtspResponse response = new RtspResponse();
		response.setCode(code);
		response.setHeader(RtspHeaderCode.CSeq, cseq);
		session.write(response);
	}


	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		logger.debug("message received from sessiong with id " + session.getId());
		String sessionId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
		logger.debug("connection session id: " + sessionId);
		logger.debug("RTSP Server Receive Message: \n{}", message);

		RtspConnection rtspConnection = RtspConnectionManager.getInstance().getConnectionBySessionId(sessionId);
		if (rtspConnection != null) {
			rtspConnection.handleMessage(session, message);
		}
	}


	/*-----------Setter And Getter --------------*/
	public String getServerAddr() {
		return serverAddr;
	}

	public void setServerAddr(String serverAddr) {
		this.serverAddr = serverAddr;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
}
