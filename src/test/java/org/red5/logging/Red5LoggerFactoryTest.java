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

package org.red5.logging;

import static org.junit.Assert.*;

import org.junit.Test;
import org.slf4j.Logger;

/**
 * @author aclarke
 *
 */
public class Red5LoggerFactoryTest {

	/**
	 * Test method for {@link org.red5.logging.Red5LoggerFactory#getLogger(java.lang.Class)}.
	 */
	@Test
	public void testGetLoggerClass() {
		  final Logger log = Red5LoggerFactory.getLogger(this.getClass());
		  assertNotNull(log);
	}

	/**
	 * Test method for {@link org.red5.logging.Red5LoggerFactory#getLogger(java.lang.Class, java.lang.String)}.
	 * 
	 * This test will fail before http://jira.red5.org/browse/APPSERVER-341 is fixed
	 * with a NullPointerException
	 */
	@Test
	public void testGetLoggerClassString() {
		  final Logger log = Red5LoggerFactory.getLogger(this.getClass(), "doesnotexist");
		  assertNotNull("should fall back to default logger", log);
	}

}
