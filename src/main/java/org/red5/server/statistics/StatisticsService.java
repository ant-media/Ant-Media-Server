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

package org.red5.server.statistics;

import java.util.Set;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.statistics.IStatisticsService;
import org.red5.server.exception.ScopeNotFoundException;
import org.red5.server.util.ScopeUtils;

/**
 * Implementation of the statistics service.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class StatisticsService implements IStatisticsService {

    private static final String SCOPE_STATS_SO_NAME = "red5ScopeStatistics";

    private static final String SO_STATS_SO_NAME = "red5SharedObjectStatistics";

    private IScope globalScope;

    public void setGlobalScope(IScope scope) {
        globalScope = scope;
    }

    private IScope getScope(String path) throws ScopeNotFoundException {
        IScope scope;
        if (!"".equals(path)) {
            scope = ScopeUtils.resolveScope(globalScope, path);
        } else {
            scope = globalScope;
        }

        if (scope == null) {
            throw new ScopeNotFoundException(globalScope, path);
        }

        return scope;
    }

    public Set<String> getScopes() {
        return getScopes(null);
    }

    public Set<String> getScopes(String path) throws ScopeNotFoundException {
        IScope scope = getScope(path);
        Set<String> result = scope.getScopeNames();
        return result;
    }

   

}
