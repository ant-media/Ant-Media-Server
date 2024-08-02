/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.messaging.messages;

import java.util.UUID;

import org.red5.io.amf3.ByteArray;
import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.utils.RandomGUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command message as sent by the <code>mx:RemoteObject</code> tag.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Action_Message_Format">Action Message Format</a>
 * @see <a href="http://flex.apache.org/asdoc/mx/messaging/messages/CommandMessage.html">Apache Flex</a>
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class CommandMessage extends AsyncMessage {

    private static final long serialVersionUID = 8805045741686625945L;

    protected static byte OPERATION_FLAG = 1;

    public String messageRefType;

    /** Command id to execute. */
    public int operation = Constants.UNKNOWN_OPERATION;

    public CommandMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    /** {@inheritDoc} */
    @Override
    protected void addParameters(StringBuilder result) {
        super.addParameters(result);
        result.append(",messageRefType=");
        result.append(messageRefType);
        result.append(",operation=");
        result.append(operation);
    }

    public int getOperation() {
        return operation;
    }

    public void setOperation(int operation) {
        this.operation = operation;
    }

    static Logger log = LoggerFactory.getLogger(CommandMessage.class);

    @Override
    public void readExternal(IDataInput in) {
        log.debug("CommandMessage - Read external");
        super.readExternal(in);
        short[] flagsArray = readFlags(in);
        for (int i = 0; i < flagsArray.length; ++i) {
            short flags = flagsArray[i];
            log.debug("Unsigned byte: {}", flags);
            short reservedPosition = 0;
            if (i == 0) {
                if ((flags & OPERATION_FLAG) != 0) {
                    Integer obj = (Integer) in.readObject();
                    log.debug("Operation object: {} name: {}", obj, obj.getClass().getName());
                    this.operation = obj.intValue();
                }
                reservedPosition = 1;
            }
            if (flags >> reservedPosition == 0) {
                continue;
            }
            for (short j = reservedPosition; j < 6; j = (short) (j + 1)) {
                if ((flags >> j & 0x1) == 0) {
                    continue;
                }
                Object obj = in.readObject();
                log.debug("Object2: {} name: {}", obj, obj.getClass().getName());
                if (obj instanceof ByteArray) {
                    ByteArray ba = (ByteArray) obj;
                    byte[] arr = new byte[ba.length()];
                    ba.readBytes(arr);
                    log.debug("Array length: {} Data: {}", arr.length, RandomGUID.fromByteArray(arr));
                }
            }
        }
        log.debug("Operation: {}", operation);
    }

    @Override
    public void writeExternal(IDataOutput out) {
        super.writeExternal(out);

        short flags = 0;

        if (this.operation != Constants.UNKNOWN_OPERATION) {
            flags = (short) (flags | OPERATION_FLAG);
        }
        out.writeByte((byte) flags);

        if (this.operation != Constants.UNKNOWN_OPERATION) {
            out.writeObject(Integer.valueOf(this.operation));
        }
    }
}
