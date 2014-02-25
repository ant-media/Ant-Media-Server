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

import static junit.framework.Assert.assertTrue;
import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.so.SharedObjectService;

public class SharedObjectTest extends BaseTest implements IEventListener {
	
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(SharedObjectTest.class);
	}

	protected String name = "testso";

	/** {@inheritDoc} */
	public void notifyEvent(IEvent event) {
		log.debug("Event: {}", event);
	}

	@SuppressWarnings("deprecation")
	@Test
	public void sharedObjectService() {
		IScope scope = context.resolveScope(path_app);
		ISharedObjectService service = new SharedObjectService();
		assertTrue("should be empty", !service.hasSharedObject(scope, "blah"));
		assertTrue("create so", service.createSharedObject(scope, name, false));
		assertTrue("so exists?", service.hasSharedObject(scope, name));
		ISharedObject so = service.getSharedObject(scope, name);
		assertTrue("so not null", so != null);
		assertTrue("name same", so.getName().equals(name));
		//assertTrue("persistent",!so.isPersistent());
		so.addEventListener(this);
		so.setAttribute("this", "that");
	}

}
