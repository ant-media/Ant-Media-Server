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

package org.red5.server.net.remoting.codec;

/**
 * Factory for remoting codec
 */
public class RemotingCodecFactory {

    /**
     * Remoting protocol decoder
     */
    protected RemotingProtocolDecoder decoder;

    /**
     * Remoting protocol encoder
     */
    protected RemotingProtocolEncoder encoder;

    /**
     * Initialization, creates and binds encoder and decoder
     */
    public void init() {
        decoder = new RemotingProtocolDecoder();
        encoder = new RemotingProtocolEncoder();
    }

    /**
     * Returns the remoting decoder.
     * 
     * @return decoder
     */
    public RemotingProtocolDecoder getRemotingDecoder() {
        return decoder;
    }

    /**
     * Returns the remoting encoder.
     * 
     * @return encoder
     */
    public RemotingProtocolEncoder getRemotingEncoder() {
        return encoder;
    }

}
