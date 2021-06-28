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
 * Statistics informations about a shared object.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ISharedObjectStatistics extends IStatisticsBase {

    /**
     * Return the name of the shared object.
     * 
     * @return the name of the shared object
     */
    public String getName();

    /**
     * Check if the shared object is persistent.
     * 
     * @return <pre>
     * True
     * </pre>
     * 
     *         if the shared object is persistent, otherwise
     * 
     *         <pre>
     * False
     * </pre>
     */
    public boolean isPersistent();

    /**
     * Return the version number of the shared object.
     * 
     * @return the version
     */
    public int getVersion();

    /**
     * Return total number of subscribed listeners.
     * 
     * @return number of listeners
     */
    public int getTotalListeners();

    /**
     * Return maximum number of concurrent subscribed listenes.
     * 
     * @return number of listeners
     */
    public int getMaxListeners();

    /**
     * Return current number of subscribed listeners.
     * 
     * @return number of listeners
     */
    public int getActiveListeners();

    /**
     * Return number of attribute changes.
     * 
     * @return number of changes
     */
    public int getTotalChanges();

    /**
     * Return number of attribute deletes.
     * 
     * @return number of deletes
     */
    public int getTotalDeletes();

    /**
     * Return number of times a message was sent.
     * 
     * @return number of sends
     */
    public int getTotalSends();

}
