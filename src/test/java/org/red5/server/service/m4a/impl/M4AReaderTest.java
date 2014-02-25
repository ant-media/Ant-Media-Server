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

package org.red5.server.service.m4a.impl;

import java.io.File;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.m4a.impl.M4AReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M4AReaderTest extends TestCase {

	private static Logger log = LoggerFactory.getLogger(M4AReaderTest.class);

	@Test
	public void testCtor() throws Exception {
		
		File file = new File("target/test-classes/fixtures/sample.m4a");
		M4AReader reader = new M4AReader(file);
		
		ITag tag = reader.readTag();
		log.debug("Tag: {}", tag);
		tag = reader.readTag();		
		log.debug("Tag: {}", tag);

	}
}
