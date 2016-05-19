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

package org.red5.server.api.stream;

public interface IStreamHandler {

    /**
     * Called when the client begins publishing
     * 
     * @param stream
     *            the stream object
     */
    void onStreamPublishStart(IStream stream);

    /**
     * Called when the client stops publishing
     * 
     * @param stream
     *            the stream object
     */
    void onStreamPublishStop(IStream stream);

    /**
     * Called when the broadcast starts
     * 
     * @param stream
     *            the stream object
     */
    void onBroadcastStreamStart(IStream stream);

    /**
     * Called when a recording starts
     * 
     * @param stream
     *            the stream object
     */
    void onRecordStreamStart(IStream stream);

    /**
     * Called when a recording stops
     * 
     * @param stream
     *            the stream object
     */
    void onRecordStreamStop(IStream stream);

    /**
     * Called when a client subscribes to a broadcast
     * 
     * @param stream
     *            the stream object
     */
    void onBroadcastStreamSubscribe(IBroadcastStream stream);

    /**
     * Called when a client unsubscribes from a broadcast
     * 
     * @param stream
     *            the stream object
     */
    void onBroadcastStreamUnsubscribe(IBroadcastStream stream);

    /**
     * Called when a client connects to an on demand stream
     * 
     * @param stream
     *            the stream object
     */
    void onOnDemandStreamConnect(IOnDemandStream stream);

    /**
     * Called when a client disconnects from an on demand stream
     * 
     * @param stream
     *            the stream object
     */
    void onOnDemandStreamDisconnect(IOnDemandStream stream);

}
