/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
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

package org.red5.server.stream.bandwidth;

import java.util.HashMap;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.service.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author The Red5 Project
 * @author Dan Rossi
 */
public class ClientServerDetection implements IPendingServiceCallback {

    protected static Logger log = LoggerFactory.getLogger(ClientServerDetection.class);

    /**
     * Handle callback from service call.
     */
    public void resultReceived(IPendingServiceCall call) {
        // if we aren't connection, skip any further testing
        if (Call.STATUS_NOT_CONNECTED != call.getStatus()) {

        } else {
            log.debug("Pending call skipped due to being no longer connected");
        }
    }

    private IStreamCapableConnection getStats() {
        IConnection conn = Red5.getConnectionLocal();
        if (conn instanceof IStreamCapableConnection) {
            return (IStreamCapableConnection) conn;
        }
        return null;
    }

    public Map<String, Object> checkBandwidth(Object[] params) {
        final IStreamCapableConnection stats = getStats();
        Map<String, Object> statsValues = new HashMap<>();
        Number time = 0; 
        if (params.length > 0) {
            if (params[0] instanceof Double) {
                time = (Double) params[0];
            } else {
                time = (Integer) params[0];
            }
        }
        statsValues.put("cOutBytes", stats.getReadBytes());
        statsValues.put("cInBytes", stats.getWrittenBytes());
        statsValues.put("time", time.intValue());
        log.debug("cOutBytes: {} cInBytes: {} time: {}", new Object[] { stats.getReadBytes(), stats.getWrittenBytes(), time });
        return statsValues;
    }

}
