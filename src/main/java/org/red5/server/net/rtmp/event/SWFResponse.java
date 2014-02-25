package org.red5.server.net.rtmp.event;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * Control message used in response to a SWF verification request.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SWFResponse extends Ping {

	private static final long serialVersionUID = -6478248060425544925L;
	
	private byte[] bytes;
	
	public SWFResponse() {
		super();
		this.eventType = Ping.PONG_SWF_VERIFY;
	}
	
	public SWFResponse(byte[] bytes) {
		this();
		this.bytes = bytes;
	}

	/**
	 * @return the bytes
	 */
	public byte[] getBytes() {
		return bytes;
	}

	/**
	 * @param bytes the bytes to set
	 */
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		eventType = in.readShort();
		if (bytes == null) {
			bytes = new byte[42];
		}
		in.read(bytes);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeShort(eventType);
		out.write(bytes);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SWFResponse [bytes=" + Arrays.toString(bytes) + "]";
	}
	
}
