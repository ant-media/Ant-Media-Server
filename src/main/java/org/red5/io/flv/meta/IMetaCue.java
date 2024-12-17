/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.meta;

/**
 * ICuePoint defines contract methods for use with cuepoints
 * 
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 */
public interface IMetaCue extends IMeta, Comparable<Object> {

    /**
     * Sets the name
     * 
     * @param name
     *            Cue point name
     * 
     */
    public void setName(String name);

    /**
     * Gets the name
     * 
     * @return name Cue point name
     * 
     */
    public String getName();

    /**
     * Sets the type type can be "event" or "navigation"
     * 
     * @param type
     *            Cue point type
     *
     */
    public void setType(String type);

    /**
     * Gets the type
     * 
     * @return type Cue point type
     *
     */
    public String getType();

    /**
     * Sets the time
     * 
     * @param d
     *            Timestamp
     *
     */
    public void setTime(double d);

    /**
     * Gets the time
     * 
     * @return time Timestamp
     *
     */
    public double getTime();
}
