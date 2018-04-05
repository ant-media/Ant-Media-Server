package io.antmedia.ipcamera;

public interface IStreamMuxerListener {

	public void errorOccured(Exception e1, String streamUrl);

	void muxingFinished(String streamName);

}
