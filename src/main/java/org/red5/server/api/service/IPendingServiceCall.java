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

package org.red5.server.api.service;

import java.util.Set;

/**
 * IPendingServiceCall is a call that have a list of callbacks.
 * 
 *
 */
public interface IPendingServiceCall extends IServiceCall {

    /**
     * Returns service call result
     * 
     * @return Remote call result
     */
    public abstract Object getResult();

    /**
     * Setter for property 'result'.
     *
     * @param result
     *            Value to set for property 'result'.
     */
    public abstract void setResult(Object result);

    /**
     * Registers callback object usually represented as an anonymous class instance that implements IPendingServiceCallback interface.
     * 
     * @param callback
     *            Callback object
     */
    public void registerCallback(IPendingServiceCallback callback);

    /**
     * Unregisters callback object usually represented as an anonymous class instance that implements IPendingServiceCallback interface.
     * 
     * @param callback
     *            Callback object
     */
    public void unregisterCallback(IPendingServiceCallback callback);

    /**
     * Returns list of callback objects, usually callback object represented as an anonymous class instance that implements IPendingServiceCallback interface.
     * 
     * @return Set of pending operations callbacks
     */
    public Set<IPendingServiceCallback> getCallbacks();
}
