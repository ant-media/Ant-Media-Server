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
 * A pipe is an object that connects message providers and
 * message consumers. Its main function is to transport messages
 * in kind of ways it provides.
 *
 * Pipes fire events as they go, these events are common way to work with pipes for
 * higher level parts of server.
 * 
 * @author The Red5 Project
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IPipe extends IMessageInput, IMessageOutput {
    /**
     * Add connection event listener to pipe
     * @param listener          Connection event listener
     */
    void addPipeConnectionListener(IPipeConnectionListener listener);

    /**
     * Add connection event listener to pipe
     * @param listener          Connection event listener
     */
	void removePipeConnectionListener(IPipeConnectionListener listener);
}
