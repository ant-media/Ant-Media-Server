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

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.server.api.IClient;
import org.red5.server.exception.ClientNotFoundException;
import org.red5.server.exception.ClientRejectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple registry for unit tests
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public final class DummyClientRegistry extends ClientRegistry {

	protected static Logger log = LoggerFactory.getLogger(DummyClientRegistry.class);

	private ConcurrentMap<String, IClient> clients = new ConcurrentHashMap<String, IClient>();

	@Override
	public boolean hasClient(String id) {
		return clients.containsKey(id);
	}

	@Override
	public IClient lookupClient(String id) throws ClientNotFoundException {
		return clients.get(id);
	}
	
	@Override
	public IClient newClient(Object[] params) throws ClientNotFoundException, ClientRejectedException {
		String id = null;
		if (params != null) {
			id = params[0].toString();
		} else {
			id = UUID.randomUUID().toString();
		}
		IClient client = new DummyClient(id, this);
		log.debug("New client: {}", client);		
		//add it
		clients.put(client.getId(), client);
		return client;
	}

}