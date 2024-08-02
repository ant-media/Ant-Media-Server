/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.amf3;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import org.apache.commons.beanutils.BeanMap;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.annotations.Anonymous;
import org.red5.compatibility.flex.messaging.io.ObjectProxy;
import org.red5.io.amf.AMF;
import org.red5.io.object.RecordSet;
import org.red5.io.object.Serializer;
import org.red5.io.object.UnsignedInt;
import org.red5.io.utils.HexDump;
import org.red5.io.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * AMF3 output writer
 *
 * @see org.red5.io.amf3.AMF3
 * @see org.red5.io.amf3.Input
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Harald Radi (harald.radi@nme.at)
 */
public class Output extends org.red5.io.amf.Output implements org.red5.io.object.Output {

	protected static Logger log = LoggerFactory.getLogger(Output.class);

	/**
	 * Set to a value above 0 to disable writing of the AMF3 object tag.
	 */
	private int amf3_mode;

	/**
	 * List of strings already written.
	 * */
	private ConcurrentMap<String, Integer> stringReferences;

	/**
	 * Constructor of AMF3 output.
	 *
	 * @param buf
	 *            instance of IoBuffer
	 * @see IoBuffer
	 */
	public Output(IoBuffer buf) {
		super(buf);
		amf3_mode = 0;
		stringReferences = new ConcurrentHashMap<String, Integer>(8, 0.9f, 2);
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

	protected void writeAMF3() {
		if (amf3_mode == 0) {
			buf.put(AMF.TYPE_AMF3_OBJECT);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void writeBoolean(Boolean bol) {
		writeAMF3();
		buf.put(bol ? AMF3.TYPE_BOOLEAN_TRUE : AMF3.TYPE_BOOLEAN_FALSE);
	}

	/** {@inheritDoc} */
	@Override
	public void writeNull() {
		writeAMF3();
		buf.put(AMF3.TYPE_NULL);
	}

	protected void putInteger(long value) {
		if ((value >= -268435456) && (value <= 268435455)) {
			value &= 0x1FFFFFFF;
		}
		if (value < 128) {
			buf.put((byte) value);
		} else if (value < 16384) {
			buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value < 2097152) {
			buf.put((byte) (((value >> 14) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value < 1073741824) {
			buf.put((byte) (((value >> 22) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 15) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 8) & 0x7F) | 0x80));
			buf.put((byte) (value & 0xFF));
		} else {
			log.error("Integer out of range: {}", value);
		}
	}

	protected static byte[] encodeString(String string) {
		//I've removed the caching library(ehcache) because ehcache is not up to date and old versions has critical vulnerabilities
		//newer versions(3.10.8) has still java.xml dependencies java 11, we use java 17  - mekya
	
		ByteBuffer buf = AMF.CHARSET.encode(string);
		byte[] encoded = new byte[buf.limit()];
		buf.get(encoded);

		return encoded;
	}

	protected void putString(String str, byte[] encoded) {
		final int len = encoded.length;
		Integer pos = stringReferences.get(str);
		if (pos != null) {
			// Reference to existing string
			putInteger(pos << 1);
			return;
		}
		putInteger(len << 1 | 1);
		buf.put(encoded);
		stringReferences.put(str, stringReferences.size());
	}

	/** {@inheritDoc} */
	@Override
	public void putString(String string) {
		// empty string
		if ("".equals(string)) {
			putInteger(1);
			return;
		}
		final byte[] encoded = encodeString(string);
		putString(string, encoded);
	}

	/** {@inheritDoc} */
	@Override
	public void writeNumber(Number num) {
		writeAMF3();
		if (num.longValue() < AMF3.MIN_INTEGER_VALUE || num.longValue() > AMF3.MAX_INTEGER_VALUE) {
			// out of range for integer encoding
			buf.put(AMF3.TYPE_NUMBER);
			buf.putDouble(num.doubleValue());
		} else if (num instanceof Long || num instanceof Integer || num instanceof Short || num instanceof Byte) {
			buf.put(AMF3.TYPE_INTEGER);
			putInteger(num.longValue());
		} else {
			buf.put(AMF3.TYPE_NUMBER);
			buf.putDouble(num.doubleValue());
		}
	}

	/** {@inheritDoc} */
	@Override
	public void writeString(String string) {
		writeAMF3();
		buf.put(AMF3.TYPE_STRING);
		if ("".equals(string)) {
			putInteger(1);
		} else {
			final byte[] encoded = encodeString(string);
			putString(string, encoded);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void writeDate(Date date) {
		writeAMF3();
		buf.put(AMF3.TYPE_DATE);
		if (hasReference(date)) {
			putInteger(getReferenceId(date) << 1);
			return;
		}
		storeReference(date);
		putInteger(1);
		buf.putDouble(date.getTime());
	}

	/** {@inheritDoc} */
	@Override
	public void writeArray(Collection<?> array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		amf3_mode += 1;
		int count = array.size();
		putInteger(count << 1 | 1);
		putString("");
		for (Object item : array) {
			Serializer.serialize(this, item);
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	@Override
	public void writeArray(Object[] array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		amf3_mode += 1;
		int count = array.length;
		putInteger(count << 1 | 1);
		putString("");
		for (Object item : array) {
			Serializer.serialize(this, item);
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	@Override
	public void writeArray(Object array) {
		Class<?> componentType = array.getClass().getComponentType();
		if (componentType.equals(Character.TYPE)) {
			// write the char[] as a string
			writeString(new String((char[]) array));
		} else if (componentType.equals(Byte.TYPE)) {
			writePrimitiveByteArray((byte[]) array);
		} else {
			writePrimitiveArrayFallback(array);
		}
	}

	/**
	 * Use the specialized BYTEARRAY type.
	 */
	private void writePrimitiveByteArray(byte[] bytes) {
		writeAMF3();
		this.buf.put(AMF3.TYPE_BYTEARRAY);

		if (hasReference(bytes)) {
			putInteger(getReferenceId(bytes) << 1);
			return;
		}
		storeReference(bytes);
		int length = bytes.length;
		putInteger(length << 1 | 0x1);

		this.buf.put(bytes);
	}

	/**
	 * Use the general ARRAY type, writing the primitive array as an array of objects (the boxed primitives) instead.
	 */
	private void writePrimitiveArrayFallback(Object array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		amf3_mode += 1;
		int count = Array.getLength(array);
		putInteger(count << 1 | 1);
		putString("");
		for (int i = 0; i < count; i++) {
			Serializer.serialize(this, Array.get(array, i));
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	@Override
	public void writeMap(Map<Object, Object> map) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(map)) {
			putInteger(getReferenceId(map) << 1);
			return;
		}
		storeReference(map);
		// Search number of starting integer keys
		int count = 0;
		for (int i = 0; i < map.size(); i++) {
			try {
				if (!map.containsKey(i)) {
					break;
				}
			} catch (ClassCastException err) {
				// Map has non-number keys.
				break;
			}
			count++;
		}
		amf3_mode += 1;
		if (count == map.size()) {
			// All integer keys starting from zero: serialize as regular array
			putInteger(count << 1 | 1);
			putString("");
			for (int i = 0; i < count; i++) {
				Serializer.serialize(this, map.get(i));
			}
			amf3_mode -= 1;
			return;
		}
		putInteger(count << 1 | 1);
		// Serialize key-value pairs first
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			Object key = entry.getKey();
			if ((key instanceof Number) && !(key instanceof Float) && !(key instanceof Double) && ((Number) key).longValue() >= 0 && ((Number) key).longValue() < count) {
				// Entry will be serialized later
				continue;
			}
			putString(key.toString());
			Serializer.serialize(this, entry.getValue());
		}
		putString("");
		// Now serialize integer keys starting from zero
		for (int i = 0; i < count; i++) {
			Serializer.serialize(this, map.get(i));
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	@Override
	public void writeMap(Collection<?> array) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		// TODO: we could optimize this by storing the first integer
		//       keys after the key-value pairs
		amf3_mode += 1;
		putInteger(1);
		int idx = 0;
		for (Object item : array) {
			if (item != null) {
				putString(String.valueOf(idx));
				Serializer.serialize(this, item);
			}
			idx++;
		}
		amf3_mode -= 1;
		putString("");
	}

	/** {@inheritDoc} */
	@Override
	protected void writeArbitraryObject(Object object) {
		log.debug("writeArbitraryObject: {}", object);
		Class<?> objectClass = object.getClass();
		// If we need to serialize class information...
		if (!objectClass.isAnnotationPresent(Anonymous.class)) {
			putString(Serializer.getClassName(objectClass));
		} else {
			putString("");
		}
		// Store key/value pairs
		amf3_mode += 1;
		// Iterate thru fields of an object to build "name-value" map from it
		for (Field field : objectClass.getFields()) {
			String fieldName = field.getName();
			log.debug("Field: {} class: {}", field, objectClass);
			// Check if the Field corresponding to the getter/setter pair is transient
			if (!Serializer.serializeField(fieldName, field, null)) {
				continue;
			}
			Object value;
			try {
				// Get field value
				value = field.get(object);
			} catch (IllegalAccessException err) {
				// Swallow on private and protected properties access exception
				continue;
			}
			// Write out prop name
			putString(fieldName);
			// Write out
			Serializer.serialize(this, field, null, object, value);
		}
		amf3_mode -= 1;
		// Write out end of object marker
		putString("");
	}

	/** {@inheritDoc} */
	@SuppressWarnings({ "rawtypes" })
	@Override
	public void writeObject(Object object) {
		log.debug("writeObject: {} {}", object.getClass().getName(), object);
		writeAMF3();
		buf.put(AMF3.TYPE_OBJECT);
		if (hasReference(object)) {
			log.debug("Object reference found");
			putInteger(getReferenceId(object) << 1);
			return;
		}
		log.debug("Storing object reference");
		storeReference(object);
		if (object instanceof IExternalizable) {
			log.debug("Object is IExternalizable");
			// the object knows how to serialize itself
			int type = 1 << 1 | 1;
			if (object instanceof ObjectProxy) {
				type |= AMF3.TYPE_OBJECT_PROXY << 2;
			} else {
				type |= AMF3.TYPE_OBJECT_EXTERNALIZABLE << 2;
			}
			putInteger(type);
			putString(Serializer.getClassName(object.getClass()));
			amf3_mode += 1;
			((IExternalizable) object).writeExternal(new DataOutput(this));
			amf3_mode -= 1;
			return;
		} else {
			log.debug("Object is NOT IExternalizable");
		}
		// we have an inline class that is not a reference, store the properties using key/value pairs
		int type = AMF3.TYPE_OBJECT_VALUE << 2 | 1 << 1 | 1;
		putInteger(type);
		// create new map out of bean properties
		BeanMap beanMap = new BeanMap(object);
		// set of bean attributes
		Set set = beanMap.keySet();
		if ((set.size() == 0) || (set.size() == 1 && beanMap.containsKey("class"))) {
			// beanMap is empty or can only access "class" attribute, skip it
			writeArbitraryObject(object);
			return;
		}
		// write out either start of object marker for class name or "empty" start of object marker
		Class<?> objectClass = object.getClass();
		if (!objectClass.isAnnotationPresent(Anonymous.class)) {
			log.debug("Object is annotated as Anonymous");
			putString(Serializer.getClassName(object.getClass()));
		} else {
			putString("");
		}
		// store key/value pairs
		amf3_mode += 1;
		for (Object key : set) {
			String fieldName = key.toString();
			log.debug("Field name: {} class: {}", fieldName, objectClass);
			Field field = getField(objectClass, fieldName);
			Method getter = getGetter(objectClass, beanMap, fieldName);
			// check if the Field corresponding to the getter/setter pair is transient
			if (!Serializer.serializeField(fieldName, field, getter)) {
				continue;
			}
			putString(fieldName);
			Serializer.serialize(this, field, getter, object, beanMap.get(key));
		}
		amf3_mode -= 1;
		// end of object marker
		putString("");
	}

	/** {@inheritDoc} */
	@Override
	public void writeObject(Map<Object, Object> map) {
		log.debug("writeObject: {}", map);
		writeAMF3();
		buf.put(AMF3.TYPE_OBJECT);
		if (hasReference(map)) {
			putInteger(getReferenceId(map) << 1);
			return;
		}
		storeReference(map);
		// we have an inline class that is not a reference, store the properties using key/value pairs
		int type = AMF3.TYPE_OBJECT_VALUE << 2 | 1 << 1 | 1;
		putInteger(type);
		// no classname
		putString("");
		// store key/value pairs
		amf3_mode += 1;
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			putString(entry.getKey().toString());
			Serializer.serialize(this, entry.getValue());
		}
		amf3_mode -= 1;
		// end of object marker
		putString("");
	}

	/** {@inheritDoc} */
	@Override
	public void writeRecordSet(RecordSet recordset) {
		writeString("Not implemented.");
	}

	/** {@inheritDoc} */
	@Override
	public void writeXML(Document xml) {
		writeAMF3();
		buf.put(AMF3.TYPE_XML);
		if (hasReference(xml)) {
			putInteger(getReferenceId(xml) << 1);
			return;
		}
		final byte[] encoded = encodeString(XMLUtils.docToString(xml));
		putInteger(encoded.length << 1 | 1);
		buf.put(encoded);
		storeReference(xml);
	}

	/** {@inheritDoc} */
	@Override
	public void writeByteArray(ByteArray array) {
		writeAMF3();
		buf.put(AMF3.TYPE_BYTEARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}
		storeReference(array);
		IoBuffer data = array.getData();
		putInteger(data.limit() << 1 | 1);
		byte[] tmp = new byte[data.limit()];
		int old = data.position();
		try {
			data.position(0);
			data.get(tmp);
			buf.put(tmp);
		} finally {
			data.position(old);
		}
	}

	/**
	 * Write a Vector&lt;int&gt;.
	 * 
	 * @param vector
	 *            vector
	 */
	@Override
	public void writeVectorInt(Vector<Integer> vector) {
		log.debug("writeVectorInt: {}", vector);
		writeAMF3();
		buf.put(AMF3.TYPE_VECTOR_INT);
		if (hasReference(vector)) {
			putInteger(getReferenceId(vector) << 1);
			return;
		}
		storeReference(vector);
		putInteger(vector.size() << 1 | 1);
		buf.put((byte) 0x00);
		for (Integer v : vector) {
			buf.putInt(v);
		}
		// debug
		if (log.isDebugEnabled()) {
			int pos = buf.position();
			buf.position(0);
			StringBuilder sb = new StringBuilder();
			HexDump.dumpHex(sb, buf.array());
			log.debug("\n{}", sb);
			buf.position(pos);
		}
	}

	/**
	 * Write a Vector&lt;uint&gt;.
	 * 
	 * @param vector
	 *            vector
	 */
	@Override
	public void writeVectorUInt(Vector<Long> vector) {
		log.debug("writeVectorUInt: {}", vector);
		writeAMF3();
		buf.put(AMF3.TYPE_VECTOR_UINT);
		if (hasReference(vector)) {
			putInteger(getReferenceId(vector) << 1);
			return;
		}
		storeReference(vector);
		putInteger(vector.size() << 1 | 1);
		buf.put((byte) 0x00);
		for (Long v : vector) {
			// update this class to implement valueOf like Long.valueOf
			UnsignedInt uint = new UnsignedInt(v);
			byte[] arr = uint.getBytes();
			buf.put(arr);
		}
	}

	/**
	 * Write a Vector&lt;Number&gt;.
	 * 
	 * @param vector
	 *            vector
	 */
	@Override
	public void writeVectorNumber(Vector<Double> vector) {
		log.debug("writeVectorNumber: {}", vector);
		buf.put(AMF3.TYPE_VECTOR_NUMBER);
		if (hasReference(vector)) {
			putInteger(getReferenceId(vector) << 1);
			return;
		}
		storeReference(vector);
		putInteger(vector.size() << 1 | 1);
		putInteger(0);
		buf.put((byte) 0x00);
		for (Double v : vector) {
			buf.putDouble(v);
		}
	}

	/**
	 * Write a Vector&lt;Object&gt;.
	 * 
	 * @param vector
	 *            vector
	 */
	@Override
	public void writeVectorObject(Vector<Object> vector) {
		log.debug("writeVectorObject: {}", vector);
		buf.put(AMF3.TYPE_VECTOR_OBJECT);
		if (hasReference(vector)) {
			putInteger(getReferenceId(vector) << 1);
			return;
		}
		storeReference(vector);
		putInteger(vector.size() << 1 | 1);
		putInteger(0);
		buf.put((byte) 0x01);
		for (Object v : vector) {
			Serializer.serialize(this, v);
		}
	}

}
