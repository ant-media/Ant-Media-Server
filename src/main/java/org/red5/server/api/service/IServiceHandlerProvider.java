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
 * Supports registration and lookup of service handlers.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IServiceHandlerProvider {

    /**
     * Register an object that provides methods which can be called from a client. <br>
     * Example: <br>
     * If you registered a handler with the name "
     * 
     * <pre>
     * one.two
     * </pre>
     * 
     * " that provides a method "
     * 
     * <pre>
     * callMe
     * </pre>
     * 
     * ", you can call a method "
     * 
     * <pre>
     * one.two.callMe
     * </pre>
     * 
     * " from the client.
     * 
     * @param name
     *            the name of the handler
     * @param handler
     *            the handler object
     */
    public void registerServiceHandler(String name, Object handler);

    /**
     * Unregister service handler.
     * 
     * @param name
     *            the name of the handler
     */
    public void unregisterServiceHandler(String name);

    /**
     * Return a previously registered service handler.
     * 
     * @param name
     *            the name of the handler to return
     * @return the previously registered handler
     */
    public Object getServiceHandler(String name);

    /**
     * Get list of registered service handler names.
     * 
     * @return the names of the registered handlers
     */
    public Set<String> getServiceHandlerNames();

}
