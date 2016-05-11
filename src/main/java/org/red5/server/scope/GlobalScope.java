/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

package org.red5.server.scope;

import org.red5.server.api.IServer;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.persistence.PersistenceUtils;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.ScopeType;
import org.red5.server.jmx.mxbeans.GlobalScopeMXBean;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Global scope is a top level scope. Server instance is meant to be injected with Spring before initialization (otherwise NullPointerException is thrown).
 * 
 * @see org.red5.server.api.scope.IGlobalScope
 * @see org.red5.server.api.scope.IScope
 * @see org.red5.server.scope.Scope
 */
@ManagedResource
public class GlobalScope extends Scope implements IGlobalScope, GlobalScopeMXBean {

    // Red5 Server instance
    protected transient IServer server;

    {
        type = ScopeType.GLOBAL;
        name = "default";
    }

    /**
     * 
     * @param persistenceClass Persistent class name
     * @throws Exception Exception
     */
    @Override
    public void setPersistenceClass(String persistenceClass) throws Exception {
        this.persistenceClass = persistenceClass;
        // We'll have to wait for creation of the store object
        // until all classes have been initialized.
    }

    /**
     * Get persistence store for scope
     * 
     * @return Persistence store
     */
    @Override
    public IPersistenceStore getStore() {
        if (store == null) {
            try {
                store = PersistenceUtils.getPersistenceStore(this, this.persistenceClass);
            } catch (Exception error) {
                log.error("Could not create persistence store.", error);
                store = null;
            }
        }
        return store;
    }

    /**
     * Setter for server
     * 
     * @param server Server
     */
    public void setServer(IServer server) {
        this.server = server;
    }

    /** {@inheritDoc} */
    @Override
    public IServer getServer() {
        return server;
    }

    /**
     * Register global scope in server instance, then call initialization
     */
    public void register() {
        server.registerGlobal(this);
        init();
    }

}
