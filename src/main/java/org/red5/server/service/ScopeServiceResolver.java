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

/**
 * Resolves service names in custom configured services of a scope.
 *
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @see org.red5.server.api.service.IServiceHandlerProvider#registerServiceHandler(String, Object)
 */
public class ScopeServiceResolver implements IServiceResolver {

    /** {@inheritDoc} */
    public Object resolveService(IScope scope, String serviceName) {
        return scope.getServiceHandler(serviceName);
    }

}
