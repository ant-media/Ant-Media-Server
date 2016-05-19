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

import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeHandler;
import org.red5.server.api.service.IServiceHandlerProvider;
import org.red5.server.api.service.IServiceHandlerProviderAware;

/**
 * Allow scope handlers to create service handlers dynamically.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class HandlerServiceResolver implements IServiceResolver {

    /** {@inheritDoc} */
    public Object resolveService(IScope scope, String serviceName) {
        IScopeHandler handler = scope.getHandler();
        if (handler instanceof IServiceHandlerProvider) {
            // TODO: deprecate this?
            Object result = ((IServiceHandlerProvider) handler).getServiceHandler(serviceName);
            if (result != null) {
                return result;
            }
        }
        if (handler instanceof IServiceHandlerProviderAware) {
            IServiceHandlerProvider shp = ((IServiceHandlerProviderAware) handler).getServiceHandlerProvider();
            if (shp != null) {
                return shp.getServiceHandler(serviceName);
            }
        }
        return null;
    }

}
