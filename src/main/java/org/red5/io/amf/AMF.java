/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf;

import java.nio.charset.Charset;

/**
 * These are the core AMF data types supported by Red5.
 * 
 * For detailed specification please see the link below.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Action_Message_Format">Action Message Format</a>
 * 
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class AMF {

    /**
     * UTF-8 is used
     */
    public static final Charset CHARSET = Charset.forName("UTF-8");

    /**
     * Max string length constant
     */
    public static final int LONG_STRING_LENGTH = 65535;

    /**
     * Number marker constant
     */
    public static final byte TYPE_NUMBER = 0x00;

    /**
     * Boolean value marker constant
     */
    public static final byte TYPE_BOOLEAN = 0x01;

    /**
     * String marker constant
     */
    public static final byte TYPE_STRING = 0x02;

    /**
     * Object marker constant
     */
    public static final byte TYPE_OBJECT = 0x03;

    /**
     * MovieClip marker constant
     */
    public static final byte TYPE_MOVIECLIP = 0x04;

    /**
     * Null marker constant
     */
    public static final byte TYPE_NULL = 0x05;

    /**
     * Undefined marker constant
     */
    public static final byte TYPE_UNDEFINED = 0x06;

    /**
     * Object reference marker constant
     */
    public static final byte TYPE_REFERENCE = 0x07;

    /**
     * Mixed array marker constant
     */
    public static final byte TYPE_MIXED_ARRAY = 0x08;

    /**
     * End of object marker constant
     */
    public static final byte TYPE_END_OF_OBJECT = 0x09;

    /**
     * End of object byte sequence
     */
    public static final byte[] END_OF_OBJECT_SEQUENCE = new byte[] { (byte) 0x00, (byte) 0x00, TYPE_END_OF_OBJECT };

    /**
     * Array marker constant
     */
    public static final byte TYPE_ARRAY = 0x0A;

    /**
     * Date marker constant
     */
    public static final byte TYPE_DATE = 0x0B;

    /**
     * Long string marker constant
     */
    public static final byte TYPE_LONG_STRING = 0x0C;

    /**
     * Unsupported type marker constant
     */
    public static final byte TYPE_UNSUPPORTED = 0x0D;

    /**
     * Recordset marker constant
     */
    public static final byte TYPE_RECORDSET = 0x0E;

    /**
     * XML marker constant
     */
    public static final byte TYPE_XML = 0x0F;

    /**
     * Class marker constant
     */
    public static final byte TYPE_CLASS_OBJECT = 0x10;

    /**
     * Object marker constant (for AMF3)
     */
    public static final byte TYPE_AMF3_OBJECT = 0x11;

    /**
     * true marker constant
     */
    public static final byte VALUE_TRUE = 0x01;

    /**
     * false marker constant
     */
    public static final byte VALUE_FALSE = 0x00;

}
