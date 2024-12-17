/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.messaging.messages;

import java.util.UUID;

import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flex compatibility message that is returned to the client.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AcknowledgeMessage extends AsyncMessage {

    private static final long serialVersionUID = 228072709981643313L;

    static Logger log = LoggerFactory.getLogger(AcknowledgeMessage.class);

    public AcknowledgeMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public void readExternal(IDataInput in) {
        super.readExternal(in);
        short[] flagsArray = readFlags(in);
        for (int i = 0; i < flagsArray.length; ++i) {
            short flags = flagsArray[i];
            short reservedPosition = 0;
            if (flags >> reservedPosition == 0) {
                continue;
            }
            for (short j = reservedPosition; j < 6; j = (short) (j + 1)) {
                if ((flags >> j & 0x1) == 0) {
                    continue;
                }
                in.readObject();
            }
        }
    }

    @Override
    public void writeExternal(IDataOutput output) {
        super.writeExternal(output);
        output.writeByte((byte) 0);
    }

}
