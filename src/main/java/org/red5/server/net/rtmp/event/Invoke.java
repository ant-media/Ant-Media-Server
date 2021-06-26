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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.api.service.IPendingServiceCall;

/**
 * Remote invocation event
 */
public class Invoke extends Notify {

    private static final long serialVersionUID = -769677790148010729L;

    /** Constructs a new Invoke. */
    public Invoke() {
        super();
        dataType = TYPE_INVOKE;
    }

    /**
     * Create new invocation event with given data
     * 
     * @param data
     *            Event data
     */
    public Invoke(IoBuffer data) {
        super(data);
        dataType = TYPE_INVOKE;
    }

    /**
     * Create new invocation event with given pending service call
     * 
     * @param call
     *            Pending call
     */
    public Invoke(IPendingServiceCall call) {
        super(call);
        dataType = TYPE_INVOKE;
    }

    /**
     * Setter for transaction id
     * 
     * @param transactionId
     *            the transactionId to set
     */
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    /** {@inheritDoc} */
    @Override
    public IPendingServiceCall getCall() {
        return (IPendingServiceCall) call;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Invoke #%d: %s", transactionId, (call != null ? call.toString() : "null"));
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Invoke)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Duplicate this Invoke message to future injection. Serialize to memory and deserialize, safe way.
     * 
     * @return duplicated Invoke event
     */
    @Override
    public Invoke duplicate() throws IOException, ClassNotFoundException {
        Invoke result = new Invoke();
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

        return result;
    }

}
