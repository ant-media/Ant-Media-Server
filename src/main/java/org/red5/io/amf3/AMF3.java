/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

/**
 * AMF3 data type definitions.
 *
 * For detailed specification please see the link below.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Action_Message_Format">Action Message Format</a>
 * @see <a href="http://download.macromedia.com/pub/labs/amf/amf3_spec_121207.pdf">Official Adobe AMF3 Specification</a>
 * 
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class AMF3 {

    /**
     * Minimum possible value for integer number encoding.
     */
    public static final long MIN_INTEGER_VALUE = -268435456;

    /**
     * Maximum possible value for integer number encoding.
     */
    public static final long MAX_INTEGER_VALUE = 268435455;

    /**
     * Max string length
     */
    public static final int LONG_STRING_LENGTH = 65535;

    /**
     * Undefined marker
     */
    public static final byte TYPE_UNDEFINED = 0x00;

    /**
     * Null marker
     */
    public static final byte TYPE_NULL = 0x01;

    /**
     * Boolean false marker
     */
    public static final byte TYPE_BOOLEAN_FALSE = 0x02;

    /**
     * Boolean true marker
     */
    public static final byte TYPE_BOOLEAN_TRUE = 0x03;

    /**
     * Integer marker
     */
    public static final byte TYPE_INTEGER = 0x04;

    /**
     * Number / Double marker
     */
    public static final byte TYPE_NUMBER = 0x05;

    /**
     * String marker
     */
    public static final byte TYPE_STRING = 0x06;

    /**
     * XML document marker <br>
     * This is for the legacy XMLDocument type is retained in the language as flash.xml.XMLDocument. Similar to AMF 0, the structure of an XMLDocument needs to be flattened into a string representation for serialization. As with other strings in AMF, the content is encoded in UTF-8. XMLDocuments can be sent as a reference to a previously occurring XMLDocument instance by using an index to the implicit object
     * reference table.
     */
    public static final byte TYPE_XML_DOCUMENT = 0x07;

    /**
     * Date marker
     */
    public static final byte TYPE_DATE = 0x08;

    /**
     * Array start marker
     */
    public static final byte TYPE_ARRAY = 0x09;

    /**
     * Object start marker
     */
    public static final byte TYPE_OBJECT = 0x0A;

    /**
     * XML start marker
     */
    public static final byte TYPE_XML = 0x0B;

    /**
     * ByteArray marker
     */
    public static final byte TYPE_BYTEARRAY = 0x0C;

    /**
     * Vector&lt;int&gt; marker
     */
    public static final byte TYPE_VECTOR_INT = 0x0D;

    /**
     * Vector&lt;uint&gt; marker
     */
    public static final byte TYPE_VECTOR_UINT = 0x0E;

    /**
     * Vector&lt;Number&gt; marker
     */
    public static final byte TYPE_VECTOR_NUMBER = 0x0F;

    /**
     * Vector&lt;Object&gt; marker
     */
    public static final byte TYPE_VECTOR_OBJECT = 0x10;

    /**
     * Dictionary
     */
    public static final byte TYPE_DICTIONARY = 0x11;

    /**
     * Property list encoding.
     * 
     * The remaining integer-data represents the number of class members that exist. The property names are read as string-data. The values are then read as AMF3-data.
     */
    public static final byte TYPE_OBJECT_PROPERTY = 0x00;

    /**
     * Externalizable object.
     * 
     * What follows is the value of the "inner" object, including type code. This value appears for objects that implement IExternalizable, such as ArrayCollection and ObjectProxy.
     */
    public static final byte TYPE_OBJECT_EXTERNALIZABLE = 0x01;

    /**
     * Name-value encoding.
     * 
     * The property names and values are encoded as string-data followed by AMF3-data until there is an empty string property name. If there is a class-def reference there are no property names and the number of values is equal to the number of properties in the class-def.
     */
    public static final byte TYPE_OBJECT_VALUE = 0x02;

    /**
     * Proxy object.
     */
    public static final byte TYPE_OBJECT_PROXY = 0x03;

}
