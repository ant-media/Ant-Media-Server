package io.antmedia.webrtc.datachannel;

import org.webrtc.DataChannel.Buffer;

public interface IDataChannelMessagePublisher extends IDataChannelMessageSender {
	public abstract void sendMessageToDataChannelWebHook(Buffer buffer);
}
