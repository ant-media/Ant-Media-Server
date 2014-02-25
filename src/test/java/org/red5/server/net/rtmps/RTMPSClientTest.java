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

package org.red5.server.net.rtmps;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * Testing RTMPS client.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ContextConfiguration(locations={"../../service/testcontext.xml"})
public class RTMPSClientTest extends AbstractJUnit4SpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(RTMPSClientTest.class);
		
	static {
		String userDir = System.getProperty("user.dir");
		System.out.println("User dir: " + userDir);
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "file:" + userDir + "/target/classes");
		System.setProperty("red5.config_root", "file:" + userDir + "/src/main/server/conf");
	}
	
	{
		log.debug("Property - user.dir: {}", System.getProperty("user.dir"));
		log.debug("Property - red5.root: {}", System.getProperty("red5.root"));
		log.debug("Property - red5.config_root: {}", System.getProperty("red5.config_root"));
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void testStartup() {
		assertTrue(true);
	}
	
}
