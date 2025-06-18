package io.antmedia.webrtc.datachannel;
import org.webrtc.DataChannel.Buffer;

import io.antmedia.webrtc.datachannel.event.ControlEvent;

public interface IDataChannelMessageSender 
{
	public abstract void sendMessageViaDataChannel(Buffer buffer);

	public default void sendControlEvent(ControlEvent controlEvent) {
		//default implementation
	}
}
