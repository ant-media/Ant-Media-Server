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

package org.red5.server.net.rtmpt.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.net.ICommand;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.status.Status;

/**
 * RTMPT protocol encoder.
 */
public class RTMPTProtocolEncoder extends RTMPProtocolEncoder {

    @Override
    protected void encodeCommand(IoBuffer out, ICommand command) {
        // if we get an InsufficientBW message for the client, we'll reduce the base tolerance and set drop live to true
        final IServiceCall call = command.getCall();
        if ("onStatus".equals(call.getServiceMethodName()) && call.getArguments().length >= 1) {
            Object arg0 = call.getArguments()[0];
            if ("NetStream.Play.InsufficientBW".equals(((Status) arg0).getCode())) {
                long baseT = getBaseTolerance();
                try {
                    // drop the tolerances by half but not less than 500
                    setBaseTolerance(Math.max(baseT / 2, 500));
                } catch (Exception e) {
                    log.debug("Problem setting base tolerance: {}", e.getMessage());
                }
                setDropLiveFuture(true);
            }
        }
        super.encodeCommand(out, command);
    }

}
