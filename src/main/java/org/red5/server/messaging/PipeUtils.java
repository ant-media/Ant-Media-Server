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

package org.red5.server.messaging;

/**
 * Helper class for pipe structure.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public class PipeUtils {
	/**
	 * Connect a provider/consumer with a pipe.
	 * 
	 * @param provider         Provider
	 * @param pipe             Pipe that used to estabilish connection
	 * @param consumer         Consumer
	 */
	public static void connect(IProvider provider, IPipe pipe,
			IConsumer consumer) {
		pipe.subscribe(provider, null);
		pipe.subscribe(consumer, null);
	}

	/**
	 * Disconnect a provider/consumer from a pipe.
	 * 
	 * @param provider         Provider
	 * @param pipe             Pipe to disconnect from
	 * @param consumer         Consumer
	 */
	public static void disconnect(IProvider provider, IPipe pipe,
			IConsumer consumer) {
		pipe.unsubscribe(provider);
		pipe.unsubscribe(consumer);
	}
}
