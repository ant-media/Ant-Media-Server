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

package org.red5.server.net.rtmpt;

import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmpt.codec.RTMPTCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for RTMPT messages.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RTMPTHandler extends RTMPHandler {

    private static final Logger log = LoggerFactory.getLogger(RTMPTHandler.class);

    /**
     * Protocol codec factory
     */
    protected RTMPTCodecFactory codecFactory;

    /**
     * Setter for codec factory
     *
     * @param factory
     *            Codec factory to use
     */
    public void setCodecFactory(RTMPTCodecFactory factory) {
        this.codecFactory = factory;
    }

    /**
     * Getter for codec factory
     *
     * @return Codec factory
     */
    public RTMPTCodecFactory getCodecFactory() {
        return this.codecFactory;
    }

    /**
     * Return hostname for URL.
     * 
     * @param url
     *            URL
     * @return Hostname from that URL
     */
    @Override
    protected String getHostname(String url) {
        log.debug("url: {}", url);
        String[] parts = url.split("/");
        if (parts.length == 2) {
            return "";
        } else {
            String host = parts[2];
            // strip out default port if the client added it
            if (host.endsWith(":80")) {
                // Remove default port from connection string
                return host.substring(0, host.length() - 3);
            }
            return host;
        }
    }

}
