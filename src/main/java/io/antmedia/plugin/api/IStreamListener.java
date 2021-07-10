package io.antmedia.plugin.api;

/*
 * Interface class to inform the plugins with stream start/start event.
 */
public interface IStreamListener {
	/*
	 * AMS inform the plugins when a stream is started with this method.
	 * @param streamId is the id of the stream
	 */
	public void streamStarted(String streamId);
	
	/*
	 * AMS inform the plugins when a stream is finished with this method.
	 * @param streamId is the id of the stream
	 */
	public void streamFinished(String streamId);
}
