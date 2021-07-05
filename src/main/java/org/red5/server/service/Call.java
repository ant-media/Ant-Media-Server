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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.red5.server.api.service.IServiceCall;

/**
 * Basic service call (remote call) implementation
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Call implements IServiceCall, Externalizable {

    private static final long serialVersionUID = -3699712251588013875L;

    /**
     * Pending status constant
     */
    public static final byte STATUS_PENDING = 0x01;

    /**
     * Success result constant
     */
    public static final byte STATUS_SUCCESS_RESULT = 0x02;

    /**
     * Returned value is null constant
     */
    public static final byte STATUS_SUCCESS_NULL = 0x03;

    /**
     * Service returns no value constant
     */
    public static final byte STATUS_SUCCESS_VOID = 0x04;

    /**
     * Service not found constant
     */
    public static final byte STATUS_SERVICE_NOT_FOUND = 0x10;

    /**
     * Service's method not found constant
     */
    public static final byte STATUS_METHOD_NOT_FOUND = 0x11;

    /**
     * Access denied constant
     */
    public static final byte STATUS_ACCESS_DENIED = 0x12;

    /**
     * Exception on invocation constant
     */
    public static final byte STATUS_INVOCATION_EXCEPTION = 0x13;

    /**
     * General exception constant
     */
    public static final byte STATUS_GENERAL_EXCEPTION = 0x14;

    /**
     * The application for this service is currently shutting down
     */
    public static final byte STATUS_APP_SHUTTING_DOWN = 0x15;

    /**
     * The remote method cannot be invoked because the client is not connected. NOTE that it is possible that this error is returned in the situation where
     * the method has been invoked on the server the connection has failed before the result returned could be read. There is no way to establish whether this has happened.
     */
    public static final byte STATUS_NOT_CONNECTED = 0x20;

    /**
     * Service name
     */
    protected String serviceName;

    /**
     * Service method name
     */
    protected String serviceMethodName;

    /**
     * Call arguments
     */
    protected Object[] arguments = null;

    /**
     * Call status, initial one is pending
     */
    protected byte status = STATUS_PENDING;

    /**
     * Call exception if any, null by default
     */
    protected Exception exception;

    /**
     * Timestamp at which the call was deserialized.
     */
    private long readTime;

    /**
     * Timestamp at which the call was serialized.
     */
    private long writeTime;

    public Call() {
    }

    /**
     * Creates call from method name
     * 
     * @param method
     *            Method name
     */
    public Call(String method) {
        serviceMethodName = method;
    }

    /**
     * Creates call from method name and array of call parameters
     * 
     * @param method
     *            Method name
     * @param args
     *            Call parameters
     */
    public Call(String method, Object[] args) {
        serviceMethodName = method;
        arguments = args;
    }

    /**
     * Creates call from given service name, method name and array of call parameters
     * 
     * @param name
     *            Service name
     * @param method
     *            Service method name
     * @param args
     *            Call parameters
     */
    public Call(String name, String method, Object[] args) {
        serviceName = name;
        serviceMethodName = method;
        arguments = args;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSuccess() {
        return (status == STATUS_SUCCESS_RESULT) || (status == STATUS_SUCCESS_NULL) || (status == STATUS_SUCCESS_VOID);
    }

    /**
     * {@inheritDoc}
     */
    public String getServiceMethodName() {
        return serviceMethodName;
    }

    /**
     * Setter for service method name
     *
     * @param serviceMethodName
     *            New service method name value
     */
    public void setServiceMethodName(String serviceMethodName) {
        this.serviceMethodName = serviceMethodName;
    }

    /**
     * {@inheritDoc}
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Setter for service name
     *
     * @param serviceName
     *            New service name value
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    /**
     * {@inheritDoc}
     */
    public Object[] getArguments() {
        return arguments;
    }

    /**
     * Setter for arguments.
     *
     * @param args
     *            Arguments.
     */
    public void setArguments(Object[] args) {
        arguments = args;
    }

    /**
     * {@inheritDoc}
     */
    public byte getStatus() {
        return status;
    }

    /**
     * {@inheritDoc}
     */
    public void setStatus(byte status) {
        this.status = status;
    }

    /**
     * {@inheritDoc}
     */
    public long getReadTime() {
        return readTime;
    }

    /**
     * {@inheritDoc}
     */
    public long getWriteTime() {
        return writeTime;
    }

    /**
     * {@inheritDoc}
     */
    public Exception getException() {
        return exception;
    }

    /**
     * {@inheritDoc}
     */
    public void setException(Exception exception) {
        this.exception = exception;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Service: ");
        sb.append(serviceName);
        sb.append(" Method: ");
        sb.append(serviceMethodName);
        if (arguments != null) {
            sb.append(" Num Params: ");
            sb.append(arguments.length);
            for (int i = 0; i < arguments.length; i++) {
                sb.append(' ');
                sb.append(i);
                sb.append(": ");
                sb.append(arguments[i]);
            }
        } else {
            sb.append(" No params");
        }
        return sb.toString();
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        // keep a time of receipt
        readTime = System.currentTimeMillis();
        // read-in properties
        serviceName = (String) in.readObject();
        serviceMethodName = (String) in.readObject();
        arguments = (Object[]) in.readObject();
        status = in.readByte();
        exception = (Exception) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        // keep a time of receipt
        writeTime = System.currentTimeMillis();
        // write-out properties
        out.writeObject(serviceName);
        out.writeObject(serviceMethodName);
        out.writeObject(arguments);
        out.writeByte(status);
        out.writeObject(exception);
    }
}
