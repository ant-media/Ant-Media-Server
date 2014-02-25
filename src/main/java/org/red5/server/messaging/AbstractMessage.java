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

package org.red5.server.messaging;

import java.util.Map;

/**
 * Abstract base for all messages
 *
 * @see org.red5.server.messaging.IMessage
 */
public class AbstractMessage implements IMessage {

	protected String messageID;

	protected String correlationID;

	protected String messageType;

	protected Map<?, ?> extraHeaders = null;

	/** {@inheritDoc} */
	public String getMessageID() {
		return messageID;
	}

	/** {@inheritDoc} */
	public void setMessageID(String id) {
		this.messageID = id;
	}

	/** {@inheritDoc} */
	public String getCorrelationID() {
		return correlationID;
	}

	/** {@inheritDoc} */
	public void setCorrelationID(String id) {
		this.correlationID = id;
	}

	/** {@inheritDoc} */
	public String getMessageType() {
		return messageType;
	}

	/** {@inheritDoc} */
	public void setMessageType(String type) {
		this.messageType = type;
	}

	/** {@inheritDoc} */
	public boolean getBooleanProperty(String name) {
		return false;
	}

	/** {@inheritDoc} */
	public void setBooleanProperty(String name, boolean value) {
	}

	/** {@inheritDoc} */
	public byte getByteProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
	public void setByteProperty(String name, byte value) {
	}

	/** {@inheritDoc} */
	public double getDoubleProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
	public void setDoubleProperty(String name, double value) {
	}

	/** {@inheritDoc} */
	public float getFloatProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
	public void setFloatProperty(String name, float value) {
	}

	/** {@inheritDoc} */
	public int getIntProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
	public void setIntProperty(String name, int value) {
	}

	/** {@inheritDoc} */
	public long getLongProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
	public void setLongProperty(String name, long value) {
	}

	/** {@inheritDoc} */
	public short getShortProperty(String name) {
		return 0;
	}

	/** {@inheritDoc} */
	public void setShortProperty(String name, short value) {
	}

	/** {@inheritDoc} */
	public String getStringProperty(String name) {
		return null;
	}

	/** {@inheritDoc} */
	public void setStringProperty(String name, String value) {
	}

	/** {@inheritDoc} */
	public Object getObjectProperty(String name) {
		return null;
	}

	/** {@inheritDoc} */
	public void setObjectProperty(String name, Object value) {
	}

}
