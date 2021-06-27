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

package org.red5.server.api.so;

import java.util.List;
import java.util.Map;

import org.red5.server.api.IAttributeStore;

/**
 * Notifications about shared object updates.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ISharedObjectListener {

    /**
     * Called when a client connects to a shared object.
     * 
     * @param so
     *            the shared object
     */
    void onSharedObjectConnect(ISharedObjectBase so);

    /**
     * Called when a client disconnects from a shared object.
     * 
     * @param so
     *            the shared object
     */
    void onSharedObjectDisconnect(ISharedObjectBase so);

    /**
     * Called when a shared object attribute is updated.
     * 
     * @param so
     *            the shared object
     * @param key
     *            the name of the attribute
     * @param value
     *            the value of the attribute
     */
    void onSharedObjectUpdate(ISharedObjectBase so, String key, Object value);

    /**
     * Called when multiple attributes of a shared object are updated.
     * 
     * @param so
     *            the shared object
     * @param values
     *            the new attributes of the shared object
     */
    void onSharedObjectUpdate(ISharedObjectBase so, IAttributeStore values);

    /**
     * Called when multiple attributes of a shared object are updated.
     * 
     * @param so
     *            the shared object
     * @param values
     *            the new attributes of the shared object
     */
    void onSharedObjectUpdate(ISharedObjectBase so, Map<String, Object> values);

    /**
     * Called when an attribute is deleted from the shared object.
     * 
     * @param so
     *            the shared object
     * @param key
     *            the name of the attribute to delete
     */
    void onSharedObjectDelete(ISharedObjectBase so, String key);

    /**
     * Called when all attributes of a shared object are removed.
     * 
     * @param so
     *            the shared object
     */
    void onSharedObjectClear(ISharedObjectBase so);

    /**
     * Called when a shared object method call is sent.
     * 
     * @param so
     *            the shared object
     * @param method
     *            the method name to call
     * @param params
     *            the arguments
     */
    void onSharedObjectSend(ISharedObjectBase so, String method, List<?> params);

}
