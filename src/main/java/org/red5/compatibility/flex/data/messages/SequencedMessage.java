/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.compatibility.flex.data.messages;

import org.red5.compatibility.flex.messaging.messages.AsyncMessage;

/**
 * Response to <code>DataMessage</code> requests.
 * 
 * @see DataMessage
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class SequencedMessage extends AsyncMessage {

    private static final long serialVersionUID = 5607350918278510061L;

    public long sequenceId;

    public Object sequenceProxies;

    public long sequenceSize;

    public String dataMessage;

    /** {@inheritDoc} */
    @Override
    protected void addParameters(StringBuilder result) {
        super.addParameters(result);
        result.append(",sequenceId=" + sequenceId);
        result.append(",sequenceProxies=" + sequenceProxies);
        result.append(",sequenceSize=" + sequenceSize);
        result.append(",dataMessage=" + dataMessage);
    }

}
