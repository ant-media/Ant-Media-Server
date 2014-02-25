package org.red5.server.net.rtmp.event;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Control message used to set a buffer.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SetBuffer extends Ping {

	private static final long serialVersionUID = -6478248060425544924L;
	
	private int streamId;
	
	private int bufferLength;
		
	public SetBuffer() {
		super();
		this.eventType = Ping.CLIENT_BUFFER;
	}

	public SetBuffer(int streamId, int bufferLength) {
		this();
		this.streamId = streamId;
		this.bufferLength = bufferLength;
	}

	/**
	 * @return the streamId
	 */
	public int getStreamId() {
		return streamId;
	}

	/**
	 * @param streamId the streamId to set
	 */
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	/**
	 * @return the bufferLength
	 */
	public int getBufferLength() {
		return bufferLength;
	}

	/**
	 * @param bufferLength the bufferLength to set
	 */
	public void setBufferLength(int bufferLength) {
		this.bufferLength = bufferLength;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		eventType = in.readShort();
		streamId = in.readInt();
		bufferLength = in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeShort(eventType);
		out.writeInt(streamId);
		out.writeInt(bufferLength);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SetBuffer [streamId=" + streamId + ", bufferLength=" + bufferLength + "]";
	}
	
}
