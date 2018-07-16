package io.antmedia.webrtc;

public interface IClientConnection {

	void send(String data);
	
	String getId();
	
}
