/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Deserializer class reads data input and handles the data according to the core data types
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class Deserializer {

    private static final Logger log = LoggerFactory.getLogger(Deserializer.class);

    private static Set<String> BLACK_LIST;

    private Deserializer() {
    }

    public synchronized static void loadBlackList() throws IOException {
        try (InputStream is = Deserializer.class.getClassLoader().getResourceAsStream("org/red5/io/object/black-list.properties")) {
            Properties bl = new Properties();
            bl.load(is);
            Set<String> set = new HashSet<>();
            for (Entry<?, ?> e : bl.entrySet()) {
                set.add((String) e.getKey());
            }
            BLACK_LIST = Collections.unmodifiableSet(set);
        }
    }

    /**
     * Deserializes the input parameter and returns an Object which must then be cast to a core data type
     * 
     * @param <T>
     *            type
     * @param in
     *            input
     * @param target
     *            target
     * @return Object object
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T deserialize(Input in, Type target) {
        if (BLACK_LIST == null) {
            //log.info("Black list is not yet initialized");
            try {
                loadBlackList();
            } catch (IOException e) {
                throw new RuntimeException("Failed to init black-list");
            }
        }
        byte type = in.readDataType();
        if (log.isTraceEnabled()) {
            log.trace("Type: {} target: {}", type, (target != null ? target.toString() : "Target not specified"));
        } else if (log.isDebugEnabled()) {
            log.debug("Datatype: {}", DataTypes.toStringValue(type));
        }
        Object result = null;
        switch (type) {
            case DataTypes.CORE_NULL:
                result = in.readNull();
                break;
            case DataTypes.CORE_BOOLEAN:
                result = in.readBoolean();
                break;
            case DataTypes.CORE_NUMBER:
                result = in.readNumber();
                break;
            case DataTypes.CORE_STRING:
                try {
                    if (target != null && ((Class) target).isEnum()) {
                        log.warn("Enum target specified");
                        String name = in.readString();
                        result = Enum.valueOf((Class) target, name);
                    } else {
                        result = in.readString();
                    }
                } catch (RuntimeException e) {
                    log.error("failed to deserialize {}", target, e);
                    throw e;
                }
                break;
            case DataTypes.CORE_DATE:
                result = in.readDate();
                break;
            case DataTypes.CORE_ARRAY:
                result = in.readArray(target);
                break;
            case DataTypes.CORE_MAP:
                result = in.readMap();
                break;
            case DataTypes.CORE_XML:
                result = in.readXML();
                break;
            case DataTypes.CORE_OBJECT:
                result = in.readObject();
                break;
            case DataTypes.CORE_BYTEARRAY:
                result = in.readByteArray();
                break;
            case DataTypes.CORE_VECTOR_INT:
                result = in.readVectorInt();
                break;
            case DataTypes.CORE_VECTOR_UINT:
                result = in.readVectorUInt();
                break;
            case DataTypes.CORE_VECTOR_NUMBER:
                result = in.readVectorNumber();
                break;
            case DataTypes.CORE_VECTOR_OBJECT:
                result = in.readVectorObject();
                break;
            case DataTypes.OPT_REFERENCE:
                result = in.readReference();
                break;
            case DataTypes.CORE_END_OBJECT:
                // end-of-object returned, not sure that we should ever get here
                log.debug("End-of-object detected");
                break;
            default:
                result = in.readCustom();
                break;
        }
        return (T) result;
    }

    /**
     * Checks to see if a given class is blacklisted or not.
     * 
     * @param className class name/package
     * @return true if not blacklisted and false if it is blacklisted
     */
    public static boolean classAllowed(String className) {
        for (String name : BLACK_LIST) {
            if (className.startsWith(name)) {
                return false;
            }
        }
        return true;
    }

}
