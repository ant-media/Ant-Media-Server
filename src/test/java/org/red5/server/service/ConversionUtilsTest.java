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

package org.red5.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.commons.beanutils.ConversionException;
import org.junit.Test;
import org.red5.io.utils.ConversionUtils;

/**
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class ConversionUtilsTest extends TestCase {

	class TestJavaBean {

	}

	//private static final Logger log = LoggerFactory.getLogger(ConversionUtilsTest.class);

	@Test
	public void testBasic() {
		Object result = ConversionUtils.convert(new Integer(42), String.class);
		if (!(result instanceof String)) {
			fail("Should be a string");
		}
		String str = (String) result;
		assertEquals("42", str);
	}

	@Test
	public void testConvertListToStringArray() {
		ArrayList<String> source = new ArrayList<String>();

		source.add("Testing 1");
		source.add("Testing 2");
		source.add("Testing 3");

		Class<? extends String[]> target = (new String[0]).getClass();

		Object result = ConversionUtils.convert(source, target);
		if (!(result.getClass().isArray() && result.getClass()
				.getComponentType().equals(String.class))) {
			fail("Should be String[]");
		}
		String[] results = (String[]) result;

		assertEquals(results.length, source.size());
		assertEquals(results[2], source.get(2));

	}
	
	@Test
	public void testConvertListToPrimitiveArray() {
		List<Integer> source = new ArrayList<Integer>();
		source.add(1);
		source.add(2);
		source.add(3);

		Class<? extends int[]> target = (new int[0]).getClass();

		Object result = ConversionUtils.convert(source, target);
		if (!(result.getClass().isArray() && result.getClass()
				.getComponentType().equals(int.class))) {
			fail("Should be int[]");
		}
		int[] results = (int[]) result;

		assertEquals(results.length, source.size());
		assertEquals(results[2], source.get(2).intValue());
	}
	
	@Test
	public void testConvertObjectArrayToStringArray() {
		Object[] source = new Object[3];

		source[0] = new Integer(21);
		source[1] = Boolean.FALSE;
		source[2] = "Woot";

		Class<? extends String[]> target = (new String[0]).getClass();

		Object result = ConversionUtils.convert(source, target);
		if (!(result.getClass().isArray() && result.getClass()
				.getComponentType().equals(String.class))) {
			fail("Should be String[]");
		}
		String[] results = (String[]) result;

		assertEquals(results.length, source.length);
		assertEquals(results[2], source[2]);

	}

	@Test
	public void testConvertToSet() {
		Object[] source = new Object[3];
		source[0] = new Integer(21);
		source[1] = Boolean.FALSE;
		source[2] = "Woot";
		Object result = ConversionUtils.convert(source, Set.class);
		if (!(result instanceof Set<?>)) {
			fail("Should be a set");
		}
		Set<?> results = (Set<?>) result;
		assertEquals(results.size(), source.length);

	}

	@Test
	public void testConvertArrayListToSet() {
		List<String> source = new ArrayList<String>(3);
		source.add("a");
		source.add("b");
		source.add("c");
		Object result = ConversionUtils.convert(source, Set.class);
		if (!(result instanceof Set<?>)) {
			fail("Should be a set");
		}
		Set<?> results = (Set<?>) result;
		assertEquals(source.size(),results.size());
	}

	@Test
	public void testNoOppConvert() {
		TestJavaBean source = new TestJavaBean();
		Object result = ConversionUtils.convert(source, TestJavaBean.class);
		assertEquals(result, source);
	}

	@Test
	public void testNullConvert() {
		Object result = ConversionUtils.convert(null, TestJavaBean.class);
		assertNull(result);
	}
	
	@Test(expected=ConversionException.class)
	public void testNullConvertNoClass() {
		// should throw exception
		ConversionUtils.convert(new TestJavaBean(), null);
	}

}
