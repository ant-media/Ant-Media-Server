package io.antmedia.ipcamera;


import java.io.IOException;

public interface IStreamMuxerListener {

	public void errorOccured(Exception e1, String streamUrl);

	void muxingFinished(String streamName);

}
