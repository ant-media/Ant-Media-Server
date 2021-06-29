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

package org.red5.server.persistence;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.server.api.persistence.IPersistable;
import org.red5.server.api.persistence.IPersistenceStore;
import org.red5.server.api.scope.IScope;
import org.red5.server.util.ScopeUtils;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Persistence implementation that stores the objects in memory. This serves as default persistence if nothing has been configured.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Michael Klishin (michael@novemberain.com)
 */
public class RamPersistence implements IPersistenceStore {

    /**
     * This is used in the id for objects that have a name of
     * 
     * <pre>
     * null
     * </pre>
     **/
    protected static final String PERSISTENCE_NO_NAME = "__null__";

    /**
     * Map for persistable objects
     */
    protected ConcurrentMap<String, IPersistable> objects = new ConcurrentHashMap<String, IPersistable>();

    /**
     * Resource pattern resolver. Resolves resources from patterns, loads resources.
     */
    protected ResourcePatternResolver resources;

    /**
     * Creates RAM persistence object from resource pattern resolvers
     * 
     * @param resources
     *            Resource pattern resolver and loader
     */
    public RamPersistence(ResourcePatternResolver resources) {
        this.resources = resources;
    }

    /**
     * Creates RAM persistence object from scope
     * 
     * @param scope
     *            Scope
     */
    public RamPersistence(IScope scope) {
        this((ResourcePatternResolver) ScopeUtils.findApplication(scope));
    }

    /**
     * Get resource name from path. The format of the object id is
     * 
     * <pre>
     * type / path / objectName
     * </pre>
     * 
     * @param id
     *            object id
     * @return resource name
     */
    protected String getObjectName(String id) {
        // The format of the object id is <type>/<path>/<objectName>
        String result = id.substring(id.lastIndexOf('/') + 1);
        if (result.equals(PERSISTENCE_NO_NAME)) {
            result = null;
        }
        return result;
    }

    /**
     * Get object path for given id and name. The format of the object id is
     * 
     * <pre>
     * type / path / objectName
     * </pre>
     * 
     * @param id
     *            object id
     * @param name
     *            object name
     * @return resource path
     */
    protected String getObjectPath(String id, String name) {
        // The format of the object id is <type>/<path>/<objectName>
        id = id.substring(id.indexOf('/') + 1);
        if (id.charAt(0) == '/') {
            id = id.substring(1);
        }
        if (id.lastIndexOf(name) <= 0) {
            return id;
        }
        return id.substring(0, id.lastIndexOf(name) - 1);
    }

    /**
     * Get object id
     * 
     * @param object
     *            Persistable object whose id is asked for
     * @return Given persistable object id
     */
    protected String getObjectId(IPersistable object) {
        // The format of the object id is <type>/<path>/<objectName>
        String result = object.getType();
        if (object.getPath().charAt(0) != '/') {
            result += '/';
        }
        result += object.getPath();
        if (!result.endsWith("/")) {
            result += '/';
        }
        String name = object.getName();
        if (name == null) {
            name = PERSISTENCE_NO_NAME;
        }
        if (name.charAt(0) == '/') {
            // "result" already ends with a slash
            name = name.substring(1);
        }
        return result + name;
    }

    /** {@inheritDoc} */
    public boolean save(IPersistable object) {
        final String key = getObjectId(object);
        objects.put(key, object);
        return true;
    }

    /** {@inheritDoc} */
    public IPersistable load(String name) {
        return objects.get(name);
    }

    /** {@inheritDoc} */
    public boolean load(IPersistable obj) {
        return obj.isPersistent();
    }

    /** {@inheritDoc} */
    public boolean remove(IPersistable object) {
        return remove(getObjectId(object));
    }

    /** {@inheritDoc} */
    public boolean remove(String name) {
        if (!objects.containsKey(name)) {
            return false;
        }
        objects.remove(name);
        return true;
    }

    /** {@inheritDoc} */
    public Set<String> getObjectNames() {
        return objects.keySet();
    }

    /** {@inheritDoc} */
    public Collection<IPersistable> getObjects() {
        return objects.values();
    }

    /** {@inheritDoc} */
    public void notifyClose() {
        objects.clear();
    }
}
