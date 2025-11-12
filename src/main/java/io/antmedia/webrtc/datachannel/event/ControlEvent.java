package io.antmedia.webrtc.datachannel.event;

import java.io.Serializable;

public class ControlEvent implements Serializable {
	
	private String streamId;
		
	public ControlEvent(String streamId) {
		this.streamId = streamId;
	}

	/**
	 * @return the streamId
	 */
	public String getStreamId() {
		return streamId;
	}

	/**
	 * @param streamId the streamId to set
	 */
	public void setStreamId(String streamId) {
		this.streamId = streamId;
	}
}
