/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf3.ByteArray;
import org.red5.io.object.BaseInput;
import org.red5.io.object.DataTypes;
import org.red5.io.object.Deserializer;
import org.red5.io.object.RecordSet;
import org.red5.io.object.RecordSetPage;
import org.red5.io.utils.ArrayUtils;
import org.red5.io.utils.ConversionUtils;
import org.red5.io.utils.ObjectMap;
import org.red5.io.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Input for Red5 data types
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@SuppressWarnings("serial")
public class Input extends BaseInput implements org.red5.io.object.Input {

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected static Map<String, String> classAliases = new HashMap<String, String>(3) {
        {
            put("DSA", "org.red5.compatibility.flex.messaging.messages.AsyncMessageExt");
            put("DSC", "org.red5.compatibility.flex.messaging.messages.CommandMessageExt");
            put("DSK", "org.red5.compatibility.flex.messaging.messages.AcknowledgeMessageExt");
        }
    };

    protected IoBuffer buf;

    protected byte currentDataType;

    /**
     * Creates Input object from byte buffer
     *
     * @param buf
     *            Byte buffer
     */
    public Input(IoBuffer buf) {
        super();
        this.buf = buf;
        if (log.isTraceEnabled()) {
            log.trace("Input: {}", Hex.encodeHexString(Arrays.copyOfRange(buf.array(), buf.position(), buf.limit())));
        }
    }

    /**
     * Reads the data type.
     *
     * @return One of AMF class constants with type
     * @see org.red5.io.amf.AMF
     */
    @Override
    public byte readDataType() {
        // prevent the handling of an empty Object
        if (buf.hasRemaining()) {
            do {
                // get the data type
                currentDataType = buf.get();
                log.trace("Data type: {}", currentDataType);
                switch (currentDataType) {
                    case AMF.TYPE_NULL:
                    case AMF.TYPE_UNDEFINED:
                        return DataTypes.CORE_NULL;
                    case AMF.TYPE_NUMBER:
                        return DataTypes.CORE_NUMBER;
                    case AMF.TYPE_BOOLEAN:
                        return DataTypes.CORE_BOOLEAN;
                    case AMF.TYPE_STRING:
                    case AMF.TYPE_LONG_STRING:
                        return DataTypes.CORE_STRING;
                    case AMF.TYPE_CLASS_OBJECT:
                    case AMF.TYPE_OBJECT:
                        return DataTypes.CORE_OBJECT;
                    case AMF.TYPE_MIXED_ARRAY:
                        return DataTypes.CORE_MAP;
                    case AMF.TYPE_ARRAY:
                        return DataTypes.CORE_ARRAY;
                    case AMF.TYPE_DATE:
                        return DataTypes.CORE_DATE;
                    case AMF.TYPE_XML:
                        return DataTypes.CORE_XML;
                    case AMF.TYPE_REFERENCE:
                        return DataTypes.OPT_REFERENCE;
                    case AMF.TYPE_UNSUPPORTED:
                    case AMF.TYPE_MOVIECLIP:
                    case AMF.TYPE_RECORDSET:
                        // These types are not handled by core datatypes
                        // So add the amf mask to them, this way the deserializer
                        // will call back to readCustom, we can then handle or
                        // return null
                        return (byte) (currentDataType + DataTypes.CUSTOM_AMF_MASK);
                    case AMF.TYPE_AMF3_OBJECT:
                        log.debug("Switch to AMF3");
                        return DataTypes.CORE_SWITCH;
                }
            } while (hasMoreProperties());
            log.trace("No more data types available");
            return DataTypes.CORE_END_OBJECT;
        }
        // empty object, may as well be null
        return DataTypes.CORE_NULL;
    }

    /**
     * Reads a null.
     *
     * @return Object
     */
    @Override
    public Object readNull() {
        return null;
    }

    /**
     * Reads a boolean.
     *
     * @return boolean
     */
    @Override
    public Boolean readBoolean() {
        return (buf.get() == AMF.VALUE_TRUE) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Reads a Number. In ActionScript 1 and 2 Number type represents all numbers, both floats and integers.
     *
     * @return Number
     */
    @Override
    public Number readNumber() {
        int remaining = buf.remaining();
        log.debug("readNumber from {} bytes", remaining);
        // look to see if big enough for double
        if (remaining >= 8) {
            double d = buf.getDouble();
            log.debug("Number: {}", d);
            return d;
        }
        if (log.isDebugEnabled()) {
            log.debug("Remaining not big enough for number - offset: {} limit: {} {}", buf.position(), buf.limit(), Hex.encodeHexString(buf.array()));
        }
        return 0;
    }

    /**
     * Reads string from buffer
     * 
     * @return String
     */
    @Override
    public String getString() {
        log.trace("getString - currentDataType: {}", currentDataType);
        byte lastDataType = currentDataType;
        // temporarily set to string for reading
        if (currentDataType != AMF.TYPE_STRING) {
            currentDataType = AMF.TYPE_STRING;
        }
        String result = readString();
        // set data type back to what it was
        currentDataType = lastDataType;
        return result;
    }

    /**
     * Reads a string
     *
     * @return String
     */
    @Override
    public String readString() {
        int limit = buf.limit();
        int len = 0;
        switch (currentDataType) {
            case AMF.TYPE_LONG_STRING:
                log.trace("Long string type");
                len = buf.getInt();
                if (len > limit) {
                    len = limit;
                }
                break;
            case AMF.TYPE_STRING:
                log.trace("Std string type");
                len = buf.getUnsignedShort();
                break;
            default:
                log.debug("Unknown AMF type: {}", currentDataType);
        }
        log.debug("Length: {} limit: {}", len, limit);
        byte[] str = new byte[len];
        buf.get(str);
        String string = bufferToString(str);
        return string;
    }

    /**
     * Converts the bytes into a string.
     * 
     * @param str
     *            string bytes
     * @return decoded String
     */
    private final String bufferToString(byte[] str) {
        String string = null;
        if (str != null) {
            string = AMF.CHARSET.decode(ByteBuffer.wrap(str)).toString();
            log.debug("String: {}", string);
        } else {
            log.warn("ByteBuffer was null attempting to read String");
        }
        return string;
    }

    /**
     * Returns a date
     *
     * @return Date Decoded string object
     */
    @Override
    public Date readDate() {
        /*
         * Date: 0x0B T7 T6 .. T0 Z1 Z2 T7 to T0 form a 64 bit Big Endian number that specifies the number of nanoseconds that have passed since 1/1/1970 0:00 to the specified time. This
         * format is UTC 1970. Z1 an Z0 for a 16 bit Big Endian number indicating the indicated time's timezone in minutes.
         */
        long ms = (long) buf.getDouble();
        // The timezone can be ignored as the date always is encoded in UTC
        @SuppressWarnings("unused")
        short timeZoneMins = buf.getShort();
        Date date = new Date(ms);
        storeReference(date);
        return date;
    }

    @Override
    public Object readArray(Type target) {
        log.debug("readArray - target: {}", target);
        Object result = null;
        int count = buf.getInt();
        log.debug("Count: {}", count);
        // To conform to the Input API, we should convert the output into an Array if the Type asks us to.
        Class<?> collection = Collection.class;
        if (target instanceof Class<?>) {
            collection = (Class<?>) target;
        }
        List<Object> resultCollection = new ArrayList<>(count);
        if (collection.isArray()) {
            result = ArrayUtils.getArray(collection.getComponentType(), count);
        } else {
            result = resultCollection;
        }
        storeReference(result); // reference should be stored before reading of objects to get correct refIds
        for (int i = 0; i < count; i++) {
            resultCollection.add(Deserializer.deserialize(this, Object.class));
        }
        if (collection.isArray()) {
            ArrayUtils.fillArray(collection.getComponentType(), result, resultCollection);
        }
        return result;
    }

    /**
     * Read key - value pairs. This is required for the RecordSet deserializer.
     */
    @Override
    public Map<String, Object> readKeyValues() {
        Map<String, Object> result = new HashMap<String, Object>();
        readKeyValues(result);
        return result;
    }

    /**
     * Read key - value pairs into Map object
     * 
     * @param result
     *            Map to put resulting pair to
     */
    protected void readKeyValues(Map<String, Object> result) {
        while (hasMoreProperties()) {
            String name = readPropertyName();
            log.debug("property: {}", name);
            Object property = Deserializer.deserialize(this, Object.class);
            log.debug("val: {}", property);
            result.put(name, property);
            if (hasMoreProperties()) {
                skipPropertySeparator();
            } else {
                break;
            }
        }
    }

    @Override
    public Object readMap() {
        // the maximum number used in this mixed array
        int maxNumber = buf.getInt();
        log.debug("Read start mixed array: {}", maxNumber);
        ObjectMap<Object, Object> result = new ObjectMap<Object, Object>();
        // we must store the reference before we deserialize any items in it to
        // ensure that reference IDs are correct
        int reference = storeReference(result);
        while (hasMoreProperties()) {
            String key = getString();
            Object item = Deserializer.deserialize(this, Object.class);
            //log.info("key: {} item: {}", key, item);
            if (!NumberUtils.isParsable(key)) {
                result.put(key, item);
            } else {
                // map keys are either integers or strings, none will be doubles
                if (key.contains(".")) {
                    result.put(key, item);
                } else {
                    result.put(Integer.valueOf(key), item);
                }
            }
        }
        result.remove("length");
        // replace the original reference with the final result
        storeReference(reference, result);
        return result;
    }

    /**
     * Creates a new instance of the className parameter and returns as an Object
     *
     * @param className
     *            Class name as String
     * @return Object New object instance (for given class)
     */
    @SuppressWarnings("all")
    protected Object newInstance(String className) {
        log.debug("Loading class: {}", className);
        Object instance = null;
        Class<?> clazz = null;
        if ("".equals(className) || className == null)
            return instance;
        try {
            // check for special DS class aliases
            if (className.length() == 3) {
                className = classAliases.get(className);
            }
            if (className.startsWith("flex.")) {
                // Use Red5 compatibility class instead
                className = "org.red5.compatibility." + className;
                log.debug("Modified classname: {}", className);
            }
            if (!classAllowed(className)) {
                log.error("Class creation is not allowed {}", className);
            } else {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
                instance = clazz.newInstance();
            }
        } catch (InstantiationException iex) {
            try {
                // check for default ctor
                clazz.getConstructor(null);
                log.error("Error loading class: {}", className);
            } catch (NoSuchMethodException nse) {
                log.error("Error loading class: {}; this can be resolved by adding a default constructor to your class", className);
            }
            log.debug("Exception was: {}", iex);
        } catch (Exception ex) {
            log.error("Error loading class: {}", className);
            log.debug("Exception was: {}", ex);
        }
        return instance;
    }

    /**
     * Reads the input as a bean and returns an object
     *
     * @param bean
     *            Input as bean
     * @return Decoded object
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object readBean(Object bean) {
        log.debug("readBean: {}", bean);
        storeReference(bean);
        Class theClass = bean.getClass();
        while (hasMoreProperties()) {
            String name = readPropertyName();
            Type type = getPropertyType(bean, name);
            log.debug("property: {} type: {}", name, type);
            Object property = Deserializer.deserialize(this, type);
            log.debug("val: {}", property);
            // log.debug("val: "+property.getClass().getName());
            if (property != null) {
                try {
                    if (type instanceof Class) {
                        Class t = (Class) type;
                        if (!t.isAssignableFrom(property.getClass())) {
                            property = ConversionUtils.convert(property, t);
                        }
                    }
                    final Field field = theClass.getField(name);
                    field.set(bean, property);
                } catch (Exception ex2) {
                    try {
                        BeanUtils.setProperty(bean, name, property);
                    } catch (Exception ex) {
                        log.error("Error mapping property: {} ({})", name, property);
                    }
                }
            } else {
                log.debug("Skipping null property: {}", name);
            }
            if (hasMoreProperties()) {
                skipPropertySeparator();
            } else {
                break; // hasMoreProperties == false, position moved to +3
            }
        }
        return bean;
    }

    /**
     * Reads the input as a map and returns a Map
     *
     * @return Read map
     */
    protected Map<String, Object> readSimpleObject() {
        log.debug("readSimpleObject");
        Map<String, Object> result = new ObjectMap<>();
        readKeyValues(result);
        storeReference(result);
        return result;
    }

    /**
     * Reads start object
     *
     * @return Read object
     */
    @Override
    public Object readObject() {
        String className;
        if (currentDataType == AMF.TYPE_CLASS_OBJECT) {
            className = getString();
            log.debug("readObject: {}", className);
            if (className != null) {
                log.debug("read class object");
                Object result = null;
                Object instance;
                if (className.equals("RecordSet")) {
                    result = new RecordSet(this);
                    storeReference(result);
                } else if (className.equals("RecordSetPage")) {
                    result = new RecordSetPage(this);
                    storeReference(result);
                } else if (!classAllowed(className)) {
                    log.debug("Class creation is not allowed {}", className);
                    result = readSimpleObject();
                } else {
                    instance = newInstance(className);
                    if (instance != null) {
                        result = readBean(instance);
                    } else {
                        log.debug("Forced to use simple object for class {}", className);
                        result = readSimpleObject();
                    }
                }
                return result;
            }
        }
        return readSimpleObject();
    }

    /**
     * Returns a boolean stating whether there are more properties
     *
     * @return boolean <code>true</code> if there are more properties to read, <code>false</code> otherwise
     */
    public boolean hasMoreProperties() {
        if (buf.remaining() >= 3) {
            byte[] threeBytes = new byte[3];
            int pos = buf.position();
            buf.get(threeBytes);
            if (Arrays.equals(AMF.END_OF_OBJECT_SEQUENCE, threeBytes)) {
                log.trace("End of object");
                return false;
            }
            buf.position(pos);
            return true;
        }
        // an end-of-object marker can't occupy less than 3 bytes so return true
        return true;
    }

    /**
     * Reads property name
     *
     * @return String Object property name
     */
    public String readPropertyName() {
        return getString();
    }

    /**
     * Skips property separator
     */
    public void skipPropertySeparator() {
        // SKIP
    }

    /**
     * Reads XML
     *
     * @return String XML as string
     */
    @Override
    public Document readXML() {
        final String xmlString = readString();
        Document doc = null;
        try {
            doc = XMLUtils.stringToDoc(xmlString);
        } catch (IOException ioex) {
            log.error("IOException converting xml to dom", ioex);
        }
        storeReference(doc);
        return doc;
    }

    /**
     * Reads Custom
     *
     * @return Object Custom type object
     */
    @Override
    public Object readCustom() {
        // return null for now
        return null;
    }

    /**
     * Read ByteArray object. This is not supported by the AMF0 deserializer.
     *
     * @return ByteArray object
     */
    @Override
    public ByteArray readByteArray() {
        throw new RuntimeException("ByteArray objects not supported with AMF0");
    }

    /**
     * Read Vector&lt;int&gt; object. This is not supported by the AMF0 deserializer.
     *
     * @return Vector&lt;Integer&gt; object
     */
    @Override
    public Vector<Integer> readVectorInt() {
        throw new RuntimeException("Vector objects not supported with AMF0");
    }

    /**
     * Read Vector&lt;Long&gt; object. This is not supported by the AMF0 deserializer.
     *
     * @return Vector&lt;Long&gt; object
     */
    @Override
    public Vector<Long> readVectorUInt() {
        throw new RuntimeException("Vector objects not supported with AMF0");
    }

    /**
     * Read Vector&lt;Number&gt; object. This is not supported by the AMF0 deserializer.
     *
     * @return Vector&lt;Double&gt; object
     */
    @Override
    public Vector<Double> readVectorNumber() {
        throw new RuntimeException("Vector objects not supported with AMF0");
    }

    /**
     * Read Vector&lt;Object&gt; object. This is not supported by the AMF0 deserializer.
     *
     * @return Vector&lt;Object&gt; object
     */
    @Override
    public Vector<Object> readVectorObject() {
        throw new RuntimeException("Vector objects not supported with AMF0");
    }

    /**
     * Reads Reference
     *
     * @return Object Read reference to object
     */
    @Override
    public Object readReference() {
        return getReference(buf.getUnsignedShort());
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        clearReferences();
    }

    protected Type getPropertyType(Object instance, String propertyName) {
        try {
            if (instance != null) {
                Field field = instance.getClass().getField(propertyName);
                return field.getGenericType();
            } else {
                // instance is null for anonymous class, use default type
            }
        } catch (NoSuchFieldException e1) {
            try {
                BeanUtilsBean beanUtilsBean = BeanUtilsBean.getInstance();
                PropertyUtilsBean propertyUtils = beanUtilsBean.getPropertyUtils();
                PropertyDescriptor propertyDescriptor = propertyUtils.getPropertyDescriptor(instance, propertyName);
                return propertyDescriptor.getReadMethod().getGenericReturnType();
            } catch (Exception e2) {
                // nothing
            }
        } catch (Exception e) {
            // ignore other exceptions
        }
        // return Object class type by default
        return Object.class;
    }
}
