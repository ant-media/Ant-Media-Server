package com.antstreaming.rtsp;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.red5.server.api.IServer;
import org.red5.server.net.rtmp.RTMPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.antstreaming.rtsp.protocol.RtspCode;
import com.antstreaming.rtsp.protocol.RtspHeaderCode;
import com.antstreaming.rtsp.protocol.RtspResponse;

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
		response.setHeader(RtspHeaderCode.CSEQ, cseq);
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
