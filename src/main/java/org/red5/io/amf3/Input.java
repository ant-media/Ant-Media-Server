/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.DataTypes;
import org.red5.io.object.Deserializer;
import org.red5.io.utils.ArrayUtils;
import org.red5.io.utils.ConversionUtils;
import org.red5.io.utils.ObjectMap;
import org.red5.io.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Input for Red5 data (AMF3) types
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class Input extends org.red5.io.amf.Input implements org.red5.io.object.Input {

    /**
     * Holds informations about already deserialized classes.
     */
    protected static class ClassReference {

        private static Logger log = LoggerFactory.getLogger(ClassReference.class);

        /** Name of the deserialized class. */
        protected String className;

        /** Type of the class. */
        protected int type;

        /** Names of the attributes of the class. */
        protected List<String> attributeNames;

        /**
         * Create new information about a class.
         * 
         * @param className
         *            class name
         * @param type
         *            type
         * @param attributeNames
         *            attributes
         */
        public ClassReference(String className, int type, List<String> attributeNames) {
            if (log.isDebugEnabled()) {
                log.debug("Class reference - className: {} type: {} attributeNames: {}", new Object[] { className, type, attributeNames });
            }
            this.className = className;
            this.type = type;
            this.attributeNames = attributeNames;
        }
    }

    /**
     * Dummy class that is stored as reference for objects currently being deserialized that reference themselves.
     */
    protected static class PendingObject {

        private static Logger log = LoggerFactory.getLogger(PendingObject.class);

        public PendingObject() {
            if (log.isDebugEnabled()) {
                log.debug("PendingObject");
            }
        }

        static final class PendingProperty {
            Object obj;

            Class<?> klass;

            String name;

            PendingProperty(Object obj, Class<?> klass, String name) {
                if (log.isDebugEnabled()) {
                    log.debug("Pending property - obj: {} class: {} name: {}", new Object[] { obj, klass, name });
                }
                this.obj = obj;
                this.klass = klass;
                this.name = name;
            }
        }

        private List<PendingProperty> properties;

        public void addPendingProperty(Object obj, Class<?> klass, String name) {
            if (properties == null) {
                properties = new ArrayList<PendingProperty>();
            }
            properties.add(new PendingProperty(obj, klass, name));
        }

        public void resolveProperties(Object result) {
            if (properties != null) {
                for (PendingProperty prop : properties) {
                    try {
                        prop.klass.getField(prop.name).set(prop.obj, result);
                    } catch (Exception e) {
                        try {
                            BeanUtils.setProperty(prop.obj, prop.name, result);
                        } catch (Exception ex) {
                            log.warn("Error mapping property: {} ({})", prop.name, result);
                        }
                    }
                }
                properties.clear();
            } else {
                // No pending properties
                return;
            }
        }
    }

    /**
     * Class used to collect AMF3 references. In AMF3 references should be collected through the whole "body" (across several Input
     * objects).
     */
    public static class RefStorage {
        // informations about previously deserialized classes
        private List<ClassReference> classReferences = new ArrayList<>();

        // list of string values found in the input stream
        private List<String> stringReferences = new ArrayList<>();

        private ConcurrentMap<Integer, Object> refMap = new ConcurrentHashMap<>(4);
    }

    /**
     * Set to a value above 0 to enforce AMF3 decoding mode.
     */
    private int amf3_mode;

    /**
     * Stores references declared in this input of previous ones in the same message body
     */
    private RefStorage refStorage;

    /**
     * Creates Input object for AMF3 from byte buffer
     * 
     * @param buf
     *            Byte buffer
     */
    public Input(IoBuffer buf) {
        this(buf, new RefStorage());
    }

    /**
     * Creates Input object for AMF3 from byte buffer and initializes references from passed RefStorage
     * 
     * @param buf
     *            buffer
     * @param refStorage
     *            ref storage
     */
    public Input(IoBuffer buf, RefStorage refStorage) {
        super(buf);
        this.refStorage = refStorage;
        this.refMap = refStorage.refMap;
    }

    /**
     * Force using AMF3 everywhere
     */
    public void enforceAMF3() {
        amf3_mode++;
    }

    /**
     * Provide access to raw data.
     * 
     * @return IoBuffer
     */
    protected IoBuffer getBuffer() {
        return buf;
    }

    /**
     * Reads the data type
     * 
     * @return byte Data type
     */
    @Override
    public byte readDataType() {
        log.trace("readDataType - amf3_mode: {}", amf3_mode);
        byte coreType = AMF3.TYPE_UNDEFINED;
        if (buf != null) {
            currentDataType = buf.get();
            log.debug("Current data type: {}", currentDataType);
            switch (currentDataType) {
                case AMF.TYPE_AMF3_OBJECT:
                    log.trace("Got AMF3 object indicator");
                    amf3_mode = 1;
                    currentDataType = buf.get();
                    break;
                case AMF3.TYPE_VECTOR_INT:
                case AMF3.TYPE_VECTOR_UINT:
                case AMF3.TYPE_VECTOR_NUMBER:
                case AMF3.TYPE_VECTOR_OBJECT:
                    log.trace("Assuming Vector type");
                    amf3_mode = 1;
                    break;
            }
            if (amf3_mode == 0) {
                // AMF0 object
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
                }
            }
            log.debug("Current data type (after amf checks): {}", currentDataType);
            switch (currentDataType) {
                case AMF3.TYPE_UNDEFINED:
                case AMF3.TYPE_NULL:
                    coreType = DataTypes.CORE_NULL;
                    break;
                case AMF3.TYPE_INTEGER:
                case AMF3.TYPE_NUMBER:
                    coreType = DataTypes.CORE_NUMBER;
                    break;
                case AMF3.TYPE_BOOLEAN_TRUE:
                case AMF3.TYPE_BOOLEAN_FALSE:
                    coreType = DataTypes.CORE_BOOLEAN;
                    break;
                case AMF3.TYPE_STRING:
                    coreType = DataTypes.CORE_STRING;
                    break;
                // TODO check XML_SPECIAL
                case AMF3.TYPE_XML:
                case AMF3.TYPE_XML_DOCUMENT:
                    coreType = DataTypes.CORE_XML;
                    break;
                case AMF3.TYPE_OBJECT:
                    coreType = DataTypes.CORE_OBJECT;
                    break;
                case AMF3.TYPE_ARRAY:
                    // should we map this to list or array?
                    coreType = DataTypes.CORE_ARRAY;
                    break;
                case AMF3.TYPE_DATE:
                    coreType = DataTypes.CORE_DATE;
                    break;
                case AMF3.TYPE_BYTEARRAY:
                    coreType = DataTypes.CORE_BYTEARRAY;
                    break;
                case AMF3.TYPE_VECTOR_INT:
                    coreType = DataTypes.CORE_VECTOR_INT;
                    break;
                case AMF3.TYPE_VECTOR_UINT:
                    coreType = DataTypes.CORE_VECTOR_UINT;
                    break;
                case AMF3.TYPE_VECTOR_NUMBER:
                    coreType = DataTypes.CORE_VECTOR_NUMBER;
                    break;
                case AMF3.TYPE_VECTOR_OBJECT:
                    coreType = DataTypes.CORE_VECTOR_OBJECT;
                    break;
                default:
                    log.info("Unknown datatype: {}", currentDataType);
                    // End of object, and anything else lets just skip
                    coreType = DataTypes.CORE_SKIP;
                    break;
            }
            log.debug("Core type: {}", coreType);
        } else {
            log.error("Why is buf null?");
        }
        return coreType;
    }

    /**
     * Reads a null (value)
     * 
     * @return Object null
     */
    @Override
    public Object readNull() {
        return null;
    }

    /**
     * Reads a boolean
     * 
     * @return boolean Boolean value
     */
    @Override
    public Boolean readBoolean() {
        return (currentDataType == AMF3.TYPE_BOOLEAN_TRUE) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Reads a Number
     * 
     * @return Number Number
     */
    @Override
    public Number readNumber() {
        if (buf.hasRemaining()) {
            int remaining = buf.remaining();
            if (log.isTraceEnabled()) {
                log.trace("Remaining bytes for Number: {}", remaining);
            }
            Number v;
            if (currentDataType == AMF3.TYPE_NUMBER) {
                // prevent buffer underrun if we dont have 8 bytes for the expected double
                if (remaining >= 8) {
                    v = buf.getDouble();
                } else {
                    v = buf.getInt();
                }
            } else {
                // we are decoding an int
                v = readInteger();
            }
            if (log.isTraceEnabled()) {
                log.trace("readNumber - value: {}", v);
            }
            return v;
        } else {
            log.warn("No remaining bytes for buffer readNumber");
        }
        return null;
    }

    /**
     * Reads a string
     * 
     * @return String String
     */
    @Override
    public String readString() {
        log.trace("readString - amf3_mode: {}", amf3_mode);
        String string;
        // check current type to ensure reading the string as AMF3 is intended
        if (currentDataType == AMF3.TYPE_STRING || amf3_mode > 0) {
            int len = readInteger();
            log.debug("readString - length: {}", len);
            // get the length of the string (0x03 = 1, 0x05 = 2, 0x07 = 3 etc..)
            // 0x01 is special and it means "empty"
            if (len == 1) {
                // Empty string
                return "";
            }
            if ((len & 1) == 0) {
                //if the refs are empty an IndexOutOfBoundsEx will be thrown
                if (refStorage.stringReferences.isEmpty()) {
                    log.debug("String reference list is empty");
                }
                // Reference
                return refStorage.stringReferences.get(len >> 1);
            }
            len >>= 1;
            log.debug("readString - new length: {}", len);
            int limit = buf.limit();
            log.debug("readString - limit: {}", limit);
            final ByteBuffer strBuf = buf.buf();
            strBuf.limit(strBuf.position() + len);
            string = AMF.CHARSET.decode(strBuf).toString();
            log.debug("String: {}", string);
            buf.limit(limit); // reset the limit
            refStorage.stringReferences.add(string);
        } else {
            // read the string as AMF0
            string = super.readString();
        }
        return string;
    }

    /**
     * Reads a string of a set length. This does not use the string reference table.
     * 
     * @param length the length of the string
     * @return String
     */
    public String readString(int length) {
        log.debug("readString - length: {}", length);
        int limit = buf.limit();
        final ByteBuffer strBuf = buf.buf();
        strBuf.limit(strBuf.position() + length);
        final String string = AMF.CHARSET.decode(strBuf).toString();
        log.debug("String: {}", string);
        buf.limit(limit);
        // check for null termination
        byte b = buf.get();
        if (b != 0) {
            buf.position(buf.position() - 1);
        }
        return string;
    }

    public RefStorage getRefStorage() {
        return refStorage;
    }

    @Override
    public String getString() {
        return readString();
    }

    /**
     * Returns a date
     * 
     * @return Date Date object
     */
    @Override
    public Date readDate() {
        int ref = readInteger();
        if ((ref & 1) == 0) {
            // reference to previously found date
            return (Date) getReference(ref >> 1);
        }
        long ms = (long) buf.getDouble();
        Date date = new Date(ms);
        storeReference(date);
        return date;
    }

    // Array

    /**
     * Returns an array
     * 
     * @return int Length of array
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Object readArray(Type target) {
        int count = readInteger();
        log.debug("Count: {} and {} ref {}", new Object[] { count, (count & 1), (count >> 1) });
        if ((count & 1) == 0) {
            //Reference
            Object ref = getReference(count >> 1);
            if (ref != null) {
                return ref;
            }
        }
        count = (count >> 1);
        String key = readString();
        amf3_mode += 1;
        Object result;
        if (key.equals("")) {
            Class<?> nested = Object.class;
            Class<?> collection = Collection.class;
            Collection resultCollection;
            if (target instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) target;
                Type[] actualTypeArguments = t.getActualTypeArguments();
                if (actualTypeArguments.length == 1) {
                    nested = (Class<?>) actualTypeArguments[0];
                }
                target = t.getRawType();
            }
            if (target instanceof Class) {
                collection = (Class) target;
            }
            if (collection.isArray()) {
                nested = ArrayUtils.getGenericType(collection.getComponentType());
                result = Array.newInstance(nested, count);
                storeReference(result);
                for (int i = 0; i < count; i++) {
                    final Object value = Deserializer.deserialize(this, nested);
                    Array.set(result, i, value);
                }
            } else {
                if (SortedSet.class.isAssignableFrom(collection)) {
                    resultCollection = new TreeSet();
                } else if (Set.class.isAssignableFrom(collection)) {
                    resultCollection = new HashSet(count);
                } else {
                    resultCollection = new ArrayList(count);
                }
                result = resultCollection;
                storeReference(result);
                for (int i = 0; i < count; i++) {
                    final Object value = Deserializer.deserialize(this, nested);
                    resultCollection.add(value);
                }
            }
        } else {
            Class<?> v = Object.class;
            Class<?> collection = Collection.class;
            if (target instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) target;
                Type[] actualTypeArguments = t.getActualTypeArguments();
                if (actualTypeArguments.length == 2) {
                    //k = (Class<?>) actualTypeArguments[0];
                    v = (Class<?>) actualTypeArguments[1];
                }
                target = t.getRawType();
            }
            if (target instanceof Class) {
                collection = (Class) target;
            }
            if (SortedMap.class.isAssignableFrom(collection)) {
                collection = TreeMap.class;
            } else {
                collection = HashMap.class;
            }
            Map resultMap;
            try {
                resultMap = (Map) collection.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                resultMap = new HashMap(count);
            }
            // associative array
            storeReference(resultMap);
            while (!key.equals("")) {
                final Object value = Deserializer.deserialize(this, v);
                resultMap.put(key, value);
                key = readString();
            }
            for (int i = 0; i < count; i++) {
                final Object value = Deserializer.deserialize(this, v);
                resultMap.put(i, value);
            }
            result = resultMap;
        }
        amf3_mode -= 1;
        return result;
    }

    @Override
    public Object readMap() {
        //throw new UnsupportedOperationException("AMF3 doesn't support maps");
        return super.readMap();
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "serial" })
    @Override
    public Object readObject() {
        log.trace("readObject - amf3_mode: {}", amf3_mode);
        int type = readInteger();
        log.debug("Type: {} and {} ref {}", new Object[] { type, (type & 1), (type >> 1) });
        if ((type & 1) == 0) {
            // Reference
            Object ref = getReference(type >> 1);
            if (ref != null) {
                return ref;
            }
            byte b = buf.get();
            if (b == 7) {
                log.debug("BEL: {}", b); //7
            } else {
                log.debug("Non-BEL byte: {} extra byte: {}", b, buf.get());
            }
        }
        type >>= 1;
        List<String> attributes = null;
        String className;
        Object result = null;
        boolean inlineClass = (type & 1) == 1;
        log.debug("Class is in-line? {}", inlineClass);
        if (!inlineClass) {
            ClassReference info = refStorage.classReferences.get(type >> 1);
            className = info.className;
            attributes = info.attributeNames;
            type = info.type;
            if (attributes != null) {
                type |= attributes.size() << 2;
            }
        } else {
            type >>= 1;
            className = readString();
            log.debug("Type: {} classname: {}", type, className);
            //check for flex class alias since these wont be detected as externalizable
            if (classAliases.containsKey(className)) {
                //make sure type is externalizable
                type = 1;
            } else if (className.startsWith("flex")) {
                //set the attributes for messaging classes
                if (className.endsWith("CommandMessage")) {
                    attributes = new LinkedList<String>() {
                        {
                            add("timestamp");
                            add("headers");
                            add("operation");
                            add("body");
                            add("correlationId");
                            add("messageId");
                            add("timeToLive");
                            add("clientId");
                            add("destination");
                        }
                    };
                } else {
                    log.debug("Attributes for {} were not set", className);
                }
            }
        }
        amf3_mode += 1;
        Object instance = newInstance(className);
        Map<String, Object> properties = null;
        PendingObject pending = new PendingObject();
        int tempRefId = storeReference(pending);
        log.debug("Object type: {}", (type & 0x03));
        switch (type & 0x03) {
            case AMF3.TYPE_OBJECT_PROPERTY:
                log.debug("Detected: Object property type");
                // Load object properties into map
                int count = type >> 2;
                log.debug("Count: {}", count);
                if (attributes == null) {
                    attributes = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        attributes.add(readString());
                    }
                    refStorage.classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_PROPERTY, attributes));
                }
                properties = new ObjectMap<>();
                for (int i = 0; i < count; i++) {
                    String name = attributes.get(i);
                    properties.put(name, Deserializer.deserialize(this, getPropertyType(instance, name)));
                }
                break;
            case AMF3.TYPE_OBJECT_EXTERNALIZABLE:
                log.debug("Detected: Externalizable type");
                // Use custom class to deserialize the object
                if ("".equals(className)) {
                    throw new RuntimeException("Classname is required to load an Externalizable object");
                }
                log.debug("Externalizable class: {}", className);
                if (className.length() == 3) {
                    //check for special DS class aliases
                    className = classAliases.get(className);
                }
                result = newInstance(className);
                if (result == null) {
                    throw new RuntimeException(String.format("Could not instantiate class: %s", className));
                }
                if (!(result instanceof IExternalizable)) {
                    throw new RuntimeException(String.format("Class must implement the IExternalizable interface: %s", className));
                }
                refStorage.classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_EXTERNALIZABLE, null));
                storeReference(tempRefId, result);
                ((IExternalizable) result).readExternal(new DataInput(this));
                break;
            case AMF3.TYPE_OBJECT_VALUE:
                if (log.isDebugEnabled()) {
                    log.debug("Detected: Object value type");
                }
                // First, we should read typed (non-dynamic) properties ("sealed traits" according to AMF3 specification).
                // Property names are stored in the beginning, then values are stored.
                count = type >> 2;
                if (log.isDebugEnabled()) {
                    log.debug("Count: {}", count);
                }
                if (attributes == null) {
                    attributes = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        attributes.add(readString());
                    }
                }
                // use the size of the attributes if we have no count
                if (count == 0 && attributes != null) {
                    count = attributes.size();
                    if (log.isDebugEnabled()) {
                        log.debug("Using class attribute size for property count: {}", count);
                    }
                    //read the attributes from the stream and log if count doesnt match
                    List<String> tmpAttributes = new ArrayList<String>(count);
                    for (int i = 0; i < count; i++) {
                        tmpAttributes.add(readString());
                    }
                    if (log.isDebugEnabled()) {
                        if (count != tmpAttributes.size()) {
                            log.debug("Count and attributes length does not match!");
                        }
                    }
                }
                // create a single reference for attributes
                refStorage.classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_VALUE, attributes));
                // create props
                properties = new ObjectMap<>();
                for (String key : attributes) {
                    if (log.isDebugEnabled()) {
                        log.debug("Looking for property: {}", key);
                    }
                    Object value = Deserializer.deserialize(this, getPropertyType(instance, key));
                    if (log.isDebugEnabled()) {
                        log.debug("Key: {} Value: {}", key, value);
                    }
                    properties.put(key, value);
                }
                if (log.isTraceEnabled()) {
                    log.trace("Buffer - position: {} limit: {}", buf.position(), buf.limit());
                }
                // no more items to read if we are at the end of the buffer
                if (buf.hasRemaining()) {
                    // Now we should read dynamic properties which are stored as name-value pairs.
                    // Dynamic properties are NOT remembered in 'classReferences'.
                    String key = readString();
                    if (log.isDebugEnabled()) {
                        log.debug("Looking for property: {}", key);
                    }
                    while (!"".equals(key)) {
                        Object value = Deserializer.deserialize(this, getPropertyType(instance, key));
                        properties.put(key, value);
                        if (!buf.hasRemaining()) {
                            break;
                        }
                        key = readString();
                    }
                }
                break;
            default:
            case AMF3.TYPE_OBJECT_PROXY:
                if (log.isDebugEnabled()) {
                    log.debug("Detected: Object proxy type");
                }
                if ("".equals(className)) {
                    throw new RuntimeException("Classname is required to load an Externalizable object");
                }
                if (log.isDebugEnabled()) {
                    log.debug("Externalizable class: {}", className);
                }
                result = newInstance(className);
                if (result == null) {
                    throw new RuntimeException(String.format("Could not instantiate class: %s", className));
                }
                if (!(result instanceof IExternalizable)) {
                    throw new RuntimeException(String.format("Class must implement the IExternalizable interface: %s", className));
                }
                refStorage.classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_PROXY, null));
                storeReference(tempRefId, result);
                ((IExternalizable) result).readExternal(new DataInput(this));
        }
        amf3_mode -= 1;
        if (result == null) {
            // Create result object based on classname
            if ("".equals(className)) {
                // "anonymous" object, load as Map
                // Resolve circular references
                for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    if (entry.getValue() == pending) {
                        entry.setValue(properties);
                    }
                }
                storeReference(tempRefId, properties);
                result = properties;
            } else if ("RecordSet".equals(className)) {
                // TODO: how are RecordSet objects encoded?
                throw new RuntimeException("Objects of type RecordSet not supported yet.");
            } else if ("RecordSetPage".equals(className)) {
                // TODO: how are RecordSetPage objects encoded?
                throw new RuntimeException("Objects of type RecordSetPage not supported yet.");
            } else {
                // Apply properties to object
                result = newInstance(className);
                if (result != null) {
                    storeReference(tempRefId, result);
                    Class resultClass = result.getClass();
                    pending.resolveProperties(result);
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        // Resolve circular references
                        final String key = entry.getKey();
                        Object value = entry.getValue();
                        if (value == pending) {
                            value = result;
                        }
                        if (value instanceof PendingObject) {
                            // Defer setting of value until real object is created
                            ((PendingObject) value).addPendingProperty(result, resultClass, key);
                            continue;
                        }
                        if (value != null) {
                            try {
                                final Field field = resultClass.getField(key);
                                final Class fieldType = field.getType();
                                if (!fieldType.isAssignableFrom(value.getClass())) {
                                    value = ConversionUtils.convert(value, fieldType);
                                } else if (value instanceof Enum) {
                                    value = Enum.valueOf(fieldType, value.toString());
                                }
                                field.set(result, value);
                            } catch (Exception e) {
                                try {
                                    BeanUtils.setProperty(result, key, value);
                                } catch (IllegalAccessException ex) {
                                    log.warn("Error mapping key: {} value: {}", key, value);
                                } catch (InvocationTargetException ex) {
                                    log.warn("Error mapping key: {} value: {}", key, value);
                                }
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Skipping null property: {}", key);
                            }
                        }
                    }
                } // else fall through
            }
        }
        return result;
    }

    /**
     * Read ByteArray object.
     *
     * @return ByteArray object
     */
    @Override
    public ByteArray readByteArray() {
        int type = readInteger();
        if ((type & 1) == 0) {
            return (ByteArray) getReference(type >> 1);
        }
        type >>= 1;
        ByteArray result = new ByteArray(buf, type);
        storeReference(result);
        return result;
    }

    /**
     * Read Vector&lt;Integer&gt; object.
     *
     * @return Vector&lt;Integer&gt; object
     */
    @SuppressWarnings("unchecked")
    @Override
    public Vector<Integer> readVectorInt() {
        log.debug("readVectorInt");
        int type = readInteger();
        if ((type & 1) == 0) {
            return (Vector<Integer>) getReference(type >> 1);
        }
        int len = type >> 1;
        Vector<Integer> array = new Vector<Integer>(len);
        storeReference(array);
        @SuppressWarnings("unused")
        int ref2 = readInteger();
        for (int j = 0; j < len; ++j) {
            array.add(buf.getInt());
        }
        return array;
    }

    /**
     * Read Vector&lt;uint&gt; object.
     *
     * @return Vector&lt;Long&gt; object
     */
    @SuppressWarnings("unchecked")
    @Override
    public Vector<Long> readVectorUInt() {
        log.debug("readVectorUInt");
        int type = readInteger();
        if ((type & 1) == 0) {
            return (Vector<Long>) getReference(type >> 1);
        }
        int len = type >> 1;
        Vector<Long> array = new Vector<Long>(len);
        storeReference(array);
        @SuppressWarnings("unused")
        int ref2 = readInteger();
        for (int j = 0; j < len; ++j) {
            long value = (buf.get() & 0xff) << 24L;
            value += (buf.get() & 0xff) << 16L;
            value += (buf.get() & 0xff) << 8L;
            value += (buf.get() & 0xff);
            array.add(value);
        }
        return array;
    }

    /**
     * Read Vector&lt;Number&gt; object.
     *
     * @return Vector&lt;Double&gt; object
     */
    @SuppressWarnings("unchecked")
    @Override
    public Vector<Double> readVectorNumber() {
        log.debug("readVectorNumber");
        int type = readInteger();
        log.debug("Type: {}", type);
        if ((type & 1) == 0) {
            return (Vector<Double>) getReference(type >> 1);
        }
        int len = type >> 1;
        log.debug("Length: {}", len);
        Vector<Double> array = new Vector<Double>(len);
        storeReference(array);
        int ref2 = readInteger();
        log.debug("Ref2: {}", ref2);
        for (int j = 0; j < len; ++j) {
            Double d = buf.getDouble();
            log.debug("Double: {}", d);
            array.add(d);
        }
        return array;
    }

    /**
     * Read Vector&lt;Object&gt; object.
     *
     * @return Vector&lt;Object&gt; object
     */
    @SuppressWarnings("unchecked")
    @Override
    public Vector<Object> readVectorObject() {
        log.debug("readVectorObject");
        int type = readInteger();
        log.debug("Type: {}", type);
        if ((type & 1) == 0) {
            return (Vector<Object>) getReference(type >> 1);
        }
        int len = type >> 1;
        log.debug("Length: {}", len);
        Vector<Object> array = new Vector<Object>(len);
        storeReference(array);
        int ref2 = readInteger();
        log.debug("Ref2: {}", ref2);
        buf.skip(1);
        Object object = null;
        for (int j = 0; j < len; ++j) {
            byte objectType = buf.get();
            log.debug("Object type: {}", objectType);
            switch (objectType) {
                case AMF3.TYPE_UNDEFINED:
                case AMF3.TYPE_NULL:
                    object = null;
                    break;
                case AMF3.TYPE_STRING:
                    object = readString();
                    break;
                case AMF3.TYPE_NUMBER:
                    object = readNumber();
                    break;
                case AMF3.TYPE_INTEGER:
                    object = readInteger();
                    break;
                case AMF3.TYPE_BYTEARRAY:
                    object = readByteArray();
                    break;
                case AMF3.TYPE_VECTOR_INT:
                    object = readVectorInt();
                    break;
                case AMF3.TYPE_VECTOR_UINT:
                    object = readVectorUInt();
                    break;
                case AMF3.TYPE_VECTOR_NUMBER:
                    object = readVectorNumber();
                    break;
                case AMF3.TYPE_VECTOR_OBJECT:
                    object = readVectorObject();
                    break;
                default:
                    object = readObject();
            }
            array.add(object);
        }
        log.debug("Vector: {}", array);
        return array;
    }

    /**
     * Reads Custom
     * 
     * @return Object Custom type object
     */
    @Override
    public Object readCustom() {
        // Return null for now
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Object readReference() {
        throw new RuntimeException("AMF3 doesn't support direct references.");
    }

    /**
     * Parser of AMF3 "compressed" integer data type
     * 
     * @return a converted integer value
     */
    private int readInteger() {
        int b = buf.get();
        if (buf.hasRemaining()) {
            int n = 0;
            int result = 0;
            while ((b & 0x80) != 0 && n < 3) {
                result <<= 7;
                result |= (b & 0x7f);
                b = buf.get();
                n++;
            }
            if (n < 3) {
                result <<= 7;
                result |= b;
            } else {
                // use all 8 bits from the 4th byte
                result <<= 8;
                result |= b & 0x0ff;
                // check if the integer should be negative
                if ((result & 0x10000000) != 0) {
                    // extend the sign bit
                    result |= 0xe0000000;
                }
            }
            return result;
        }
        return b;
    }

    //Read UInt29 style
    @SuppressWarnings("unused")
    private int readAMF3IntegerNew() {
        int b = buf.get() & 0xFF;
        if (b < 128) {
            return b;
        }
        int value = (b & 0x7F) << 7;
        b = buf.get() & 0xFF;
        if (b < 128) {
            return (value | b);
        }
        value = (value | b & 0x7F) << 7;
        b = buf.get() & 0xFF;
        if (b < 128) {
            return (value | b);
        }
        value = (value | b & 0x7F) << 8;
        b = buf.get() & 0xFF;
        return (value | b);
    }

    /** {@inheritDoc} */
    @Override
    public Document readXML() {
        int len = readInteger();
        if (len == 1) {
            // Empty string, should not happen
            return null;
        }
        if ((len & 1) == 0) {
            // Reference
            return (Document) getReference(len >> 1);
        }
        len >>= 1;
        int limit = buf.limit();
        final ByteBuffer strBuf = buf.buf();
        strBuf.limit(strBuf.position() + len);
        final String xmlString = AMF.CHARSET.decode(strBuf).toString();
        buf.limit(limit); // Reset the limit
        Document doc = null;
        try {
            doc = XMLUtils.stringToDoc(xmlString);
        } catch (IOException ioex) {
            log.error("IOException converting xml to dom", ioex);
        }
        storeReference(doc);
        return doc;
    }

    /** {@inheritDoc} */
    @Override
    public void reset() {
        super.reset();
        // input must keep the String references for all parameters
        //refStorage.stringReferences.clear();
    }

}
