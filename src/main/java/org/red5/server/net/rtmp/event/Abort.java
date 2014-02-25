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
 * RTMP Abort event.
 * 
 * @author aclarke@xuggle.com
 */
public class Abort extends BaseEvent {

	private int channelId = 0;

	public Abort() {
		super(Type.SYSTEM);
	}

	public Abort(int channelId) {
		this.channelId = channelId;
	}

	public byte getDataType() {
		return TYPE_ABORT;
	}

	protected void releaseInternal() {

	}

	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}

	public int getChannelId() {
		return channelId;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "Abort Channel: " + channelId;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		channelId = in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(channelId);
	}

}