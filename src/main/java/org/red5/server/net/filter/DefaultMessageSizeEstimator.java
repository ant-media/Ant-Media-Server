/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.net.filter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoEvent;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.executor.IoEventSizeEstimator;

/**
 * A default {@link IoEventSizeEstimator} implementation.
 * <p>
 * Martin's Java Notes were used for estimation of the size of non-IoBuffers. For unknown types, it inspects declaring 
 * fields of the class of the specified message. The size of unknown declaring fields are approximated to the specified 
 * <tt>averageSizePerField</tt> (default: 64).
 * <p>
 * All the estimated sizes of classes are cached for performance improvement.
 * 
 * <br />
 * This originated from the Mina sandbox.
 */
public class DefaultMessageSizeEstimator implements IoEventSizeEstimator {

	private final ConcurrentMap<Class<?>, Integer> class2size = new ConcurrentHashMap<Class<?>, Integer>();

	public DefaultMessageSizeEstimator() {
		class2size.put(boolean.class, 4); // Probably an integer.
		class2size.put(byte.class, 1);
		class2size.put(char.class, 2);
		class2size.put(short.class, 2);
		class2size.put(int.class, 4);
		class2size.put(long.class, 8);
		class2size.put(float.class, 4);
		class2size.put(double.class, 8);
		class2size.put(void.class, 0);
	}
	
    /**
     * {@inheritDoc}
     */
    public int estimateSize(IoEvent event) {
        return estimateSize((Object) event) + estimateSize(event.getParameter());
    }	
	
	public int estimateSize(Object message) {
		if (message == null) {
			return 8;
		}
		int answer = 8 + estimateSize(message.getClass(), null);
		if (message instanceof IoBuffer) {
			answer += ((IoBuffer) message).remaining();
        } else if (message instanceof WriteRequest) {
            answer += estimateSize(((WriteRequest) message).getMessage());			
		} else if (message instanceof CharSequence) {
			answer += ((CharSequence) message).length() << 1;
		} else if (message instanceof Iterable<?>) {
			for (Object m : (Iterable<?>) message) {
				answer += estimateSize(m);
			}
		}
		return align(answer);
	}
	
	private int estimateSize(Class<?> clazz, Set<Class<?>> visitedClasses) {
		Integer objectSize = class2size.get(clazz);
		if (objectSize != null) {
			return objectSize;
		}
		if (visitedClasses != null) {
			if (visitedClasses.contains(clazz)) {
				return 0;
			}
		} else {
			visitedClasses = new HashSet<Class<?>>();
		}
		visitedClasses.add(clazz);
		int answer = 8; // basic overhead
		for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
			Field[] fields = c.getDeclaredFields();
			for (Field f : fields) {
				// ignore static fields
				if ((f.getModifiers() & Modifier.STATIC) != 0) {
					continue;
				}
				answer += estimateSize(f.getType(), visitedClasses);
			}
		}
		visitedClasses.remove(clazz);
		// some alignment
		answer = align(answer);
        // put the final answer
        Integer tmpAnswer = class2size.putIfAbsent(clazz, answer);
        if (tmpAnswer != null) {
            answer = tmpAnswer;
        }
		return answer;
	}

	private static int align(int size) {
		if (size % 8 != 0) {
			size /= 8;
			size++;
			size *= 8;
		}
		return size;
	}
}
