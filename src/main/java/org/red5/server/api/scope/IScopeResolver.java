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

package org.red5.server.api.scope;

import org.red5.server.exception.ScopeNotFoundException;

/**
 * Resolve the scope from given a host and path. Resolver implementations depend on context naming strategy and so forth.
 */
public interface IScopeResolver {

    /**
     * Return the global scope.
     * 
     * @return Global scope
     */
    public IGlobalScope getGlobalScope();

    /**
     * Get the scope for a given path.
     * 
     * @param path
     *            Path to return the scope for
     * @return Scope for passed path
     * @throws ScopeNotFoundException
     *             If scope doesn't exist an can't be created
     */
    public IScope resolveScope(String path);

    /**
     * Get the scope for a given path from a root scope.
     * 
     * @param root
     *            The scope to start traversing from.
     * @param path
     *            Path to return the scope for.
     * @return Scope for passed path.
     */
    public IScope resolveScope(IScope root, String path);

}