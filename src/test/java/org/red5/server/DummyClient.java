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

package org.red5.server;

import org.red5.server.api.IConnection;

/**
 * Client is an abstraction representing user connected to Red5 application.
 * Clients are tied to connections and registred in ClientRegistry
 */
public class DummyClient extends Client implements Comparable<DummyClient> {
	
	private IConnection conn;
	
	public DummyClient(String id, ClientRegistry registry) {
		super(id, registry);
	}
	
	public void registerConnection(IConnection conn) {
		this.conn = conn;
		register(this.conn);
	}
	
	public void unregisterConnection(IConnection conn) {
		unregister(conn);
		disconnect();
	}

	@Override
	public int compareTo(DummyClient that) {
		if (id.equals(that.getId())) {
			return 0;
		} else if (creationTime > that.getCreationTime()) {
			return 1;
		} else {
			return -1;
		}
	}
	
}