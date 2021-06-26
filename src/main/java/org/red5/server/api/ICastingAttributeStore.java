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

package org.red5.server.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attribute storage with automatic object casting support.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface ICastingAttributeStore extends IAttributeStore {

    /**
     * Get Boolean attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Boolean getBoolAttribute(String name);

    /**
     * Get Byte attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Byte getByteAttribute(String name);

    /**
     * Get Double attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Double getDoubleAttribute(String name);

    /**
     * Get Integer attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Integer getIntAttribute(String name);

    /**
     * Get List attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public List<?> getListAttribute(String name);

    /**
     * Get boolean attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Long getLongAttribute(String name);

    /**
     * Get Long attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Map<?, ?> getMapAttribute(String name);

    /**
     * Get Set attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Set<?> getSetAttribute(String name);

    /**
     * Get Short attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public Short getShortAttribute(String name);

    /**
     * Get String attribute by name
     * 
     * @param name
     *            Attribute name
     * @return Attribute
     */
    public String getStringAttribute(String name);

}
