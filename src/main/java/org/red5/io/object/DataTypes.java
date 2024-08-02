/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

/**
 * The core datatypes supported by red5, I have left out undefined (this is up for debate). If a codec returns one of these datatypes its handled by the base serializer.
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class DataTypes {

    /**
     * End marker
     */
    public static final byte CORE_END_OBJECT = (byte) 0xff;

    /**
     * Switch decoding marker
     */
    public static final byte CORE_SWITCH = (byte) 0xef;

    /**
     * Padding marker
     */
    public static final byte CORE_SKIP = 0x00; // padding

    /**
     * Null type marker
     */
    public static final byte CORE_NULL = 0x01; // no undefined type

    /**
     * Boolean type marker
     */
    public static final byte CORE_BOOLEAN = 0x02;

    /**
     * Number type marker
     */
    public static final byte CORE_NUMBER = 0x03;

    /**
     * String type marker
     */
    public static final byte CORE_STRING = 0x04;

    /**
     * Date type marker
     */
    public static final byte CORE_DATE = 0x05;

    // Basic stuctures

    /**
     * Array type marker
     */
    public static final byte CORE_ARRAY = 0x06;

    /**
     * Map type marker
     */
    public static final byte CORE_MAP = 0x07;

    /**
     * XML type marker
     */
    public static final byte CORE_XML = 0x08;

    /**
     * Object (Hash) type marker
     */
    public static final byte CORE_OBJECT = 0x09;

    /**
     * ByteArray type marker (AMF3 only)
     */
    public static final byte CORE_BYTEARRAY = 0x10; //16

    /**
     * Reference type, this is optional for codecs to support
     */
    public static final byte OPT_REFERENCE = 0x11; //17

    // More datatypes can be added but they should be prefixed by the type
    // If a codec returns one of these datatypes its handled by a custom serializer

    /**
     * Custom datatype mock mask marker
     */
    public static final byte CUSTOM_MOCK_MASK = 0x20;

    /**
     * Custom datatype AMF mask
     */
    public static final byte CUSTOM_AMF_MASK = 0x30;

    /**
     * Custom datatype RTMP mask
     */
    public static final byte CUSTOM_RTMP_MASK = 0x40;

    /**
     * Custom datatype JSON mask
     */
    public static final byte CUSTOM_JSON_MASK = 0x50;

    /**
     * Custom datatype XML mask
     */
    public static final byte CUSTOM_XML_MASK = 0x60;

    /**
     * Vector type markers
     */
    public static final byte CORE_VECTOR_INT = 0x0D + 0x30; //61

    public static final byte CORE_VECTOR_UINT = 0x0E + 0x30; //62

    public static final byte CORE_VECTOR_NUMBER = 0x0F + 0x30; //63

    public static final byte CORE_VECTOR_OBJECT = 0x10 + 0x30; //64

    // Some helper methods..

    /**
     * Returns the string value of the data type
     *
     * @return String String value of given ActionScript data type
     * @param dataType
     *            AS data type as byte
     */
    public static String toStringValue(byte dataType) {
        switch (dataType) {
            case CORE_SKIP:
                return "skip";
            case CORE_NULL:
                return "null";
            case CORE_BOOLEAN:
                return "Boolean";
            case CORE_NUMBER:
                return "Number";
            case CORE_STRING:
                return "String";
            case CORE_DATE:
                return "Date";
            case CORE_ARRAY:
                return "Array";
            case CORE_MAP:
                return "List";
            case CORE_XML:
                return "XML";
            case CORE_OBJECT:
                return "Object";
            case CORE_BYTEARRAY:
                return "ByteArray";
            case CORE_VECTOR_INT:
                return "Vector<int>";
            case CORE_VECTOR_UINT:
                return "Vector<uint>";
            case CORE_VECTOR_NUMBER:
                return "Vector<Number>";
            case CORE_VECTOR_OBJECT:
                return "Vector<Object>";
            case OPT_REFERENCE:
                return "Reference";
        }
        if (dataType >= CUSTOM_MOCK_MASK && dataType < CUSTOM_AMF_MASK) {
            return "MOCK[" + (dataType - CUSTOM_MOCK_MASK) + ']';
        }
        if (dataType >= CUSTOM_AMF_MASK && dataType < CUSTOM_RTMP_MASK) {
            return "AMF[" + (dataType - CUSTOM_AMF_MASK) + ']';
        }
        if (dataType >= CUSTOM_RTMP_MASK && dataType < CUSTOM_JSON_MASK) {
            return "RTMP[" + (dataType - CUSTOM_RTMP_MASK) + ']';
        }
        if (dataType >= CUSTOM_JSON_MASK && dataType < CUSTOM_XML_MASK) {
            return "JSON[" + (dataType - CUSTOM_JSON_MASK) + ']';
        }
        return "XML[" + (dataType - CUSTOM_XML_MASK) + ']';
    }

    /**
     * Returns whether it is a basic data type
     *
     * @param type
     *            Data type as byte
     * @return boolean <code>true</code> if data type is primitive, <code>false</code> otherwise
     */
    public static boolean isBasicType(byte type) {
        return type <= CORE_DATE;
    }

    /**
     * Returns whether it is a complex data type
     *
     * @param type
     *            Data type as byte
     * @return boolean <code>true</code> if data type is complex (non-primitive), <code>false</code> otherwise
     */
    public static boolean isComplexType(byte type) {
        return type >= CORE_ARRAY || type <= CORE_OBJECT;
    }

    /**
     * Returns whether it is a custom data type
     *
     * @param type
     *            Data type as byte
     * @return boolean Whether given type is of custom type
     */
    public static boolean isCustomType(byte type) {
        return type >= CUSTOM_AMF_MASK;
    }

}
