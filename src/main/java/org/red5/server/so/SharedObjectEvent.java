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

package org.red5.server.so;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class SharedObjectEvent implements ISharedObjectEvent, Externalizable {

    private static final long serialVersionUID = -4129018814289863535L;

    /**
     * Event type
     */
    private Type type;

    /**
     * Changed pair key
     */
    private String key;

    /**
     * Changed pair value
     */
    private Object value;

    public SharedObjectEvent() {
    }

    /**
     * 
     * @param type
     *            type
     * @param key
     *            key
     * @param value
     *            value
     */
    public SharedObjectEvent(Type type, String key, Object value) {
        this.type = type;
        this.key = key;
        this.value = value;
    }

    /** {@inheritDoc} */
    public String getKey() {
        return key;
    }

    /** {@inheritDoc} */
    public Type getType() {
        return type;
    }

    /** {@inheritDoc} */
    public Object getValue() {
        return value;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SharedObjectEvent other = (SharedObjectEvent) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (type != other.type)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("SharedObjectEvent(%s, key: %s value: %s)", getType(), getKey(), getValue());
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = (Type) in.readObject();
        key = (String) in.readObject();
        value = in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(type);
        out.writeObject(key);
        out.writeObject(value);
    }
}
