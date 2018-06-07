package io.antmedia.webrtc;

import java.io.UnsupportedEncodingException;

import javax.annotation.Nonnull;

import org.red5.net.websocket.WebSocketConnection;

public class WebSocketClientConnection implements IClientConnection {

	private WebSocketConnection wsConnection;

	public WebSocketClientConnection(@Nonnull WebSocketConnection wsConnection) {
		this.wsConnection = wsConnection;
	} 
	
	@Override
	public void send(String data) {
		try {
			this.wsConnection.send(data);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getId() {
		return String.valueOf(this.wsConnection.getId());
	}

}
