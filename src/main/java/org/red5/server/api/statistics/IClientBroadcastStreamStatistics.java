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

package org.red5.server.api.statistics;

/**
 * Statistical informations about a stream that is broadcasted by a client.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IClientBroadcastStreamStatistics extends IStreamStatistics {

    /**
     * Get the filename the stream is being saved as.
     * 
     * @return The filename relative to the scope or
     * 
     *         <pre>
     * null
     * </pre>
     * 
     *         if the stream is not being saved.
     */
    public String getSaveFilename();

    /**
     * Get stream publish name. Publish name is the value of the first parameter had been passed to
     * 
     * <pre>
     * NetStream.publish
     * </pre>
     * 
     * on client side in SWF.
     * 
     * @return Stream publish name
     */
    public String getPublishedName();

    /**
     * Return total number of subscribers.
     * 
     * @return number of subscribers
     */
    public int getTotalSubscribers();

    /**
     * Return maximum number of concurrent subscribers.
     * 
     * @return number of subscribers
     */
    public int getMaxSubscribers();

    /**
     * Return current number of subscribers.
     * 
     * @return number of subscribers
     */
    public int getActiveSubscribers();

    /**
     * Return total number of bytes received from client for this stream.
     * 
     * @return number of bytes
     */
    public long getBytesReceived();

}
