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

import org.red5.server.api.service.IServiceHandlerProvider;

/**
 * Supports registration and lookup of shared object handlers.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 *
 */
public interface ISharedObjectHandlerProvider extends IServiceHandlerProvider {

    /**
     * Register an object that provides methods which handle calls without a service name to a shared object.
     * 
     * @param handler
     *            the handler object
     */
    public void registerServiceHandler(Object handler);

    /**
     * Unregister the shared object handler for calls without a service name.
     */
    public void unregisterServiceHandler(String name);

}
