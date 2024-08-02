/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.messaging.messages;

import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

/**
 * An externalizable version of a given CommandMessage. The class alias for this class within flex is "DSC".
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class CommandMessageExt extends CommandMessage implements IExternalizable {

    private static final long serialVersionUID = -5371460213241777011L;

    private CommandMessage message;

    public CommandMessageExt() {
    }

    public CommandMessageExt(CommandMessage message) {
        this.message = message;
    }

    @Override
    public void writeExternal(IDataOutput out) {
        if (this.message != null) {
            this.message.writeExternal(out);
        } else {
            super.writeExternal(out);
        }
    }
}
