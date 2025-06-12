package io.antmedia.webrtc.datachannel;

public interface IDataChannelRouter 
{
	public void playerMessageReceived(IDataChannelMessageSender source, String streamId, byte[] data, boolean binary);
	
	public void addPublisher(String streamId, IDataChannelMessagePublisher publisher);
	
	public void removePublisher(String streamId, IDataChannelMessageSender publisher);
}
