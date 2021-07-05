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

package org.red5.server.net.rtmp.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IStreamPacket;
import org.red5.server.net.ICommand;
import org.red5.server.stream.IStreamData;

/**
 * Stream notification event. The invoke / transaction id is "always" equal to zero for a Notify.
 */
public class Notify extends BaseEvent implements ICommand, IStreamData<Notify>, IStreamPacket {

    private static final long serialVersionUID = -6085848257275156569L;

    /**
     * Service call
     */
    protected IServiceCall call;

    /**
     * Event data
     */
    protected IoBuffer data;

    /**
     * Event data type
     */
    protected byte dataType = TYPE_NOTIFY;

    /**
     * Invoke id / transaction id
     */
    protected int transactionId = 0;

    /**
     * Connection parameters
     */
    private Map<String, Object> connectionParams;

    private String action;

    /** Constructs a new Notify */
    public Notify() {
        super(Type.SERVICE_CALL);
    }

    /**
     * Create new notification event with given byte buffer
     * 
     * @param data
     *            Byte buffer
     */
    public Notify(IoBuffer data) {
        super(Type.STREAM_DATA);
        this.data = data;
    }

    /**
     * Create new notification event with given byte buffer and action.
     * 
     * @param data Byte buffer
     * @param action Action / method
     */
    public Notify(IoBuffer data, String action) {
        super(Type.STREAM_DATA);
        this.data = data;
        this.action = action;
    }

    /**
     * Create new notification event with given service call
     * 
     * @param call
     *            Service call
     */
    public Notify(IServiceCall call) {
        super(Type.SERVICE_CALL);
        this.call = call;
    }

    /** {@inheritDoc} */
    public byte getDataType() {
        return dataType;
    }

    /**
     * Setter for data
     *
     * @param data
     *            Data
     */
    public void setData(IoBuffer data) {
        this.data = data;
    }

    /**
     * Setter for call
     *
     * @param call
     *            Service call
     */
    public void setCall(IServiceCall call) {
        this.call = call;
    }

    /**
     * Getter for service call
     *
     * @return Service call
     */
    public IServiceCall getCall() {
        return this.call;
    }

    /** {@inheritDoc} */
    public IoBuffer getData() {
        return data;
    }

    /**
     * Getter for transaction id
     *
     * @return Transaction id
     */
    public int getTransactionId() {
        return transactionId;
    }

    /**
     * Release event (nullify call object)
     */
    protected void doRelease() {
        call = null;
    }

    /**
     * Getter for connection parameters
     *
     * @return Connection parameters
     */
    public Map<String, Object> getConnectionParams() {
        return connectionParams;
    }

    /**
     * Setter for connection parameters
     *
     * @param connectionParams
     *            Connection parameters
     */
    public void setConnectionParams(Map<String, Object> connectionParams) {
        this.connectionParams = connectionParams;
    }

    public void setAction(String onCueOrOnMeta) {
        this.action = onCueOrOnMeta;
    }

    public String getAction() {
        return action;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return call != null ? String.format("%s: %s", getClass().getSimpleName(), call) : (action != null ? String.format("%s action: %s", getClass().getSimpleName(), action) : getClass().getSimpleName());
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Notify)) {
            return false;
        }
        Notify other = (Notify) obj;
        if (getConnectionParams() == null && other.getConnectionParams() != null) {
            return false;
        }
        if (getConnectionParams() != null && other.getConnectionParams() == null) {
            return false;
        }
        if (getConnectionParams() != null && !getConnectionParams().equals(other.getConnectionParams())) {
            return false;
        }
        if (getTransactionId() != other.getTransactionId()) {
            return false;
        }
        if (getCall() == null && other.getCall() != null) {
            return false;
        }
        if (getCall() != null && other.getCall() == null) {
            return false;
        }
        if (getCall() != null && !getCall().equals(other.getCall())) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    protected void releaseInternal() {
        if (data != null) {
            data.free();
            data = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        call = (IServiceCall) in.readObject();
        connectionParams = (Map<String, Object>) in.readObject();
        transactionId = in.readInt();
        byte[] byteBuf = (byte[]) in.readObject();
        if (byteBuf != null) {
            data = IoBuffer.allocate(0);
            data.setAutoExpand(true);
            SerializeUtils.ByteArrayToByteBuffer(byteBuf, data);
        }
        if (log.isTraceEnabled()) {
            log.trace("readExternal - transactionId: {} connectionParams: {} call: {}", transactionId, connectionParams, call);
        }
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        if (log.isTraceEnabled()) {
            log.trace("writeExternal - transactionId: {} connectionParams: {} call: {}", transactionId, connectionParams, call);
        }
        out.writeObject(call);
        out.writeObject(connectionParams);
        out.writeInt(transactionId);
        if (data != null) {
            out.writeObject(SerializeUtils.ByteBufferToByteArray(data));
        } else {
            out.writeObject(null);
        }
    }

    /**
     * Duplicate this Notify message to future injection Serialize to memory and deserialize, safe way.
     * 
     * @return duplicated Notify event
     */
    public Notify duplicate() throws IOException, ClassNotFoundException {
        Notify result = new Notify();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        writeExternal(oos);
        oos.close();
        byte[] buf = baos.toByteArray();
        baos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        result.readExternal(ois);
        ois.close();
        bais.close();
        // set the action if it exists
        result.setAction(getAction());
        return result;
    }

}
