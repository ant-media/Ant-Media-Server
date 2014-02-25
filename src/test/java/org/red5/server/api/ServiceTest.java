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

package org.red5.server.api;

import static org.junit.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.service.EchoService;
import org.red5.server.service.PendingCall;

public class ServiceTest extends BaseTest {

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ServiceTest.class);
	}

	@Test
	public void simpletest() {
		if (context.hasBean("echoService")) {
    		EchoService service = (EchoService) context.getBean("echoService");
    		IPendingServiceCall call = new PendingCall("echoService", "echoString",
    				new Object[] { "My String" });
    		context.getServiceInvoker().invoke(call, service);
    		assertTrue("result null", call.getResult() != null);
		} else {
			System.out.println("No echo service found");
			assertTrue(false);
		}
	}

}
