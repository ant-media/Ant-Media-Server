/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

/**
 * Interface that needs to be implemented by classes that serialize / deserialize themselves.
 * 
 * @see <a href="http://livedocs.adobe.com/flex/2/langref/flash/utils/IExternalizable.html">Adobe Livedocs (external)</a>
 */
public interface IExternalizable {

    /**
     * Load custom object from stream.
     * 
     * @param input
     *            object to be used for data loading
     */
    public void readExternal(IDataInput input);

    /**
     * Store custom object to stream.
     * 
     * @param output
     *            object to be used for data storing
     */
    public void writeExternal(IDataOutput output);

}
