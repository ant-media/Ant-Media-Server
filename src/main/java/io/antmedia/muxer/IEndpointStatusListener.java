package io.antmedia.muxer;

/**
 * The listener interface to update the Endpoint status. 
 * It's basically used in RTMP endpoint status updates
 *
 */
public interface IEndpointStatusListener 
{
	
	/**
	 * It's called when the endpoint status is updated
	 * @param url is the URL of the endpoint 
	 * @param status is the current status of the rtmp endpoint
	 */
    public void endpointStatusUpdated(String url, String status);
}
