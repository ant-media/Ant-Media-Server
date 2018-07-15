package io.antmedia.webrtc;

import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.red5.net.websocket.WebSocketConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WebSocketClientConnection implements IClientConnection {

	private static Logger logger = LoggerFactory.getLogger(WebSocketClientConnection.class);

	private WebSocketConnection wsConnection;

	public WebSocketClientConnection(@Nonnull WebSocketConnection wsConnection) {
		this.wsConnection = wsConnection;
	} 
	
	@Override
	public void send(String data) {
		try {
			this.wsConnection.send(data);
		} catch (UnsupportedEncodingException e) {
			logger.error(ExceptionUtils.getStackTrace(e));
		}
	}

	@Override
	public String getId() {
		return String.valueOf(this.wsConnection.getId());
	}

}
