/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.rtmp.event;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Server bandwidth event. Also known as a Window Acknowledgement size message.  
 */
public class ServerBW extends BaseEvent {
	
	private static final long serialVersionUID = 24487902555977210L;

	/**
	 * Bandwidth
	 */
	private int bandwidth;

	public ServerBW() {
	}

	/**
	 * Server bandwidth event
	 * @param bandwidth      Bandwidth
	 */
	public ServerBW(int bandwidth) {
		super(Type.STREAM_CONTROL);
		this.bandwidth = bandwidth;
	}

	/** {@inheritDoc} */
	@Override
	public byte getDataType() {
		return TYPE_SERVER_BANDWIDTH;
	}

	/**
	 * Getter for bandwidth
	 *
	 * @return  Bandwidth
	 */
	public int getBandwidth() {
		return bandwidth;
	}

	/**
	 * Setter for bandwidth
	 *
	 * @param bandwidth  New bandwidth.
	 */
	public void setBandwidth(int bandwidth) {
		this.bandwidth = bandwidth;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "ServerBW: " + bandwidth;
	}

	/** {@inheritDoc} */
	@Override
	protected void releaseInternal() {

	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		bandwidth = in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(bandwidth);
	}
}
