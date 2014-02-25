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

package org.red5.server.api.stream.support;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IServerStream;
import org.red5.server.stream.ServerStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stream helper methods.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public abstract class StreamUtils {

	private static final Logger logger = LoggerFactory.getLogger(StreamUtils.class);

	/* Map to hold reference to the instanced server streams */
	private static volatile ConcurrentMap<String, IServerStream> serverStreamMap = new ConcurrentHashMap<String, IServerStream>(16, 0.75f, 4);

	/**
	 * Creates server stream
	 * 
	 * @param scope Scope of stream
	 * @param name Name of stream
	 * @return		IServerStream object
	 */
	public static IServerStream createServerStream(IScope scope, String name) {
		logger.debug("Creating server stream: {} scope: {}", name, scope);
		ServerStream stream = new ServerStream();
		stream.setScope(scope);
		stream.setName(name);
		stream.setPublishedName(name);
		//save to the list for later lookups
		String key = scope.getName() + '/' + name;
		serverStreamMap.put(key, stream);
		return stream;
	}

	/**
	 * Looks up a server stream in the stream map. Null will be returned if the 
	 * stream is not found.
	 *
	 * @param scope Scope of stream
	 * @param name Name of stream
	 * @return		IServerStream object
	 */
	public static IServerStream getServerStream(IScope scope, String name) {
		logger.debug("Looking up server stream: {} scope: {}", name, scope);
		String key = scope.getName() + '/' + name;
		if (serverStreamMap.containsKey(key)) {
			return serverStreamMap.get(key);
		} else {
			logger.warn("Server stream not found with key: {}", key);
			return null;
		}
	}

	/**
	 * Puts a server stream in the stream map
	 *
	 * @param scope Scope of stream
	 * @param name Name of stream
	 * @param stream ServerStream object
	 */
	public static void putServerStream(IScope scope, String name, IServerStream stream) {
		logger.debug("Putting server stream in the map - name: {} scope: {} stream: {}", new Object[] { name, scope, stream });
		String key = scope.getName() + '/' + name;
		if (!serverStreamMap.containsKey(key)) {
			serverStreamMap.put(key, stream);
		} else {
			logger.warn("Server stream already exists in the map with key: {}", key);
		}
	}

	/**
	 * Removes a server stream from the stream map
	 *
	 * @param scope Scope of stream
	 * @param name Name of stream
	 */
	public static void removeServerStream(IScope scope, String name) {
		logger.debug("Removing server stream from the map - name: {} scope: {}", name, scope);
		String key = scope.getName() + '/' + name;
		if (serverStreamMap.containsKey(key)) {
			serverStreamMap.remove(key);
		} else {
			logger.warn("Server stream did not exist in the map with key: {}", key);
		}
	}

}
