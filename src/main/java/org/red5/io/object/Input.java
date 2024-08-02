/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.Vector;

import org.red5.io.amf3.ByteArray;
import org.w3c.dom.Document;

/**
 * Interface for Input which defines the contract methods which are to be implemented. Input object provides ways to read primitives, 
 * complex object and object references from byte buffer.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface Input {

    /**
     * Read type of data
     * 
     * @return Type of data as byte
     */
    byte readDataType();

    /**
     * Read a string without the string type header.
     * 
     * @return String
     */
    String getString();

    /**
     * Read Null data type
     * 
     * @return Null datatype (AS)
     */
    Object readNull();

    /**
     * Read Boolean value
     * 
     * @return Boolean
     */
    Boolean readBoolean();

    /**
     * Read Number object
     * 
     * @return Number
     */
    Number readNumber();

    /**
     * Read String object
     * 
     * @return String
     */
    String readString();

    /**
     * Read date object
     * 
     * @return Date
     */
    Date readDate();

    /**
     * Read an array. This can result in a List or Map being deserialized depending on the array type found.
     * 
     * @param target
     *            target type
     * @return array
     */
    Object readArray(Type target);

    /**
     * Read a map containing key - value pairs. This can result in a List or Map being deserialized depending on the map type found.
     * 
     * @return Map
     */
    Object readMap();

    /**
     * Read an object.
     * 
     * @return object
     */
    Object readObject();

    /**
     * Read XML document
     * 
     * @return XML DOM document
     */
    Document readXML();

    /**
     * Read custom object
     * 
     * @return Custom object
     */
    Object readCustom();

    /**
     * Read ByteArray object.
     * 
     * @return ByteArray object
     */
    ByteArray readByteArray();

    /**
     * Read reference to Complex Data Type. Objects that are collaborators (properties) of other objects must be stored as references in map of id-reference pairs.
     * 
     * @return object
     */
    Object readReference();

    /**
     * Clears all references
     */
    void clearReferences();

    /**
     * Read key - value pairs. This is required for the RecordSet deserializer.
     * 
     * @return key-value pairs
     */
    Map<String, Object> readKeyValues();

    /**
     * Read Vector&lt;int&gt; object.
     * 
     * @return Vector&lt;Integer&gt;
     */
    Vector<Integer> readVectorInt();

    /**
     * Read Vector&lt;uint&gt; object.
     * 
     * @return Vector&lt;Long&gt;
     */
    Vector<Long> readVectorUInt();

    /**
     * Read Vector&lt;Number&gt; object.
     * 
     * @return Vector&lt;Double&gt;
     */
    Vector<Double> readVectorNumber();

    /**
     * Read Vector&lt;Object&gt; object.
     * 
     * @return Vector&lt;Object&gt;
     */
    Vector<Object> readVectorObject();

    /**
     * Resets internals.
     */
    void reset();

}
