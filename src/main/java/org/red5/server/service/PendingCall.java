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

package org.red5.server.service;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;

/**
 * Pending call is remote call operation that is in pending state. Remote calls to services are asynchronous, that is, after call but before result callback remote calls are in pending state.
 */
public class PendingCall extends Call implements IPendingServiceCall {

    private static final long serialVersionUID = 3219267601240355335L;

    /**
     * Result object
     */
    private Object result;

    /**
     * List of callbacks (event listeners)
     */
    private HashSet<IPendingServiceCallback> callbacks = new HashSet<IPendingServiceCallback>();

    public PendingCall() {
    }

    /**
     * Creates pending call with given method name
     * 
     * @param method
     *            Method name
     */
    public PendingCall(String method) {
        super(method);
    }

    /**
     * Creates pending call with given method name and array of parameters
     * 
     * @param method
     *            Method name
     * @param args
     *            Parameters
     */
    public PendingCall(String method, Object[] args) {
        super(method, args);
    }

    /**
     * Creates pending call with given method name, service name and array of parameters
     *
     * @param name
     *            Service name
     * @param method
     *            Method name
     * @param args
     *            Parameters
     */
    public PendingCall(String name, String method, Object[] args) {
        super(name, method, args);
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult() {
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void setResult(Object result) {
        this.result = result;
        setStatus(result == null ? STATUS_SUCCESS_NULL : STATUS_SUCCESS_RESULT);
    }

    /** {@inheritDoc} */
    public void registerCallback(IPendingServiceCallback callback) {
        callbacks.add(callback);
    }

    /** {@inheritDoc} */
    public void unregisterCallback(IPendingServiceCallback callback) {
        callbacks.remove(callback);
    }

    /** {@inheritDoc} */
    public Set<IPendingServiceCallback> getCallbacks() {
        return callbacks;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        result = in.readObject();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(result);
    }
}
