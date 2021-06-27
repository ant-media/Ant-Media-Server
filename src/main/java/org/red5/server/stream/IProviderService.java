/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
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

package org.red5.server.stream;

import java.io.File;
import java.util.Set;

import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeService;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.messaging.IMessageInput;

/**
 * Central unit to get access to different types of provider inputs
 */
public interface IProviderService extends IScopeService {

    public final static String BEAN_NAME = "providerService";

    enum INPUT_TYPE {
        NOT_FOUND, LIVE, LIVE_WAIT, VOD;
    };

    /**
     * Returns the input type for a named provider if a source of input exists. Live is checked first and VOD second.
     * 
     * @param scope
     *            Scope of provider
     * @param name
     *            Name of provider
     * @param type
     *            Type of video stream
     * @return LIVE if live, VOD if VOD, and NOT_FOUND otherwise
     */
    INPUT_TYPE lookupProviderInput(IScope scope, String name, int type);

    /**
     * Get a named provider as the source of input. Live stream first, VOD stream second.
     * 
     * @param scope
     *            Scope of provider
     * @param name
     *            Name of provider
     * @return <tt>null</tt> if nothing found.
     */
    IMessageInput getProviderInput(IScope scope, String name);

    /**
     * Get a named Live provider as the source of input.
     * 
     * @param scope
     *            Scope of provider
     * @param name
     *            Name of provider
     * @param needCreate
     *            Whether there's need to create basic scope / live provider if they don't exist
     * @return <tt>null</tt> if not found.
     */
    IMessageInput getLiveProviderInput(IScope scope, String name, boolean needCreate);

    /**
     * Get a named VOD provider as the source of input.
     * 
     * @param scope
     *            Scope of provider
     * @param name
     *            Name of provider
     * @return <tt>null</tt> if not found.
     */
    IMessageInput getVODProviderInput(IScope scope, String name);

    /**
     * Get a named VOD source file.
     * 
     * @param scope
     *            Scope of provider
     * @param name
     *            Name of provider
     * @return <tt>null</tt> if not found.
     */
    File getVODProviderFile(IScope scope, String name);

    /**
     * Register a broadcast stream to a scope.
     * 
     * @param scope
     *            Scope
     * @param name
     *            Name of stream
     * @param stream
     *            Broadcast stream to register
     * @return <tt>true</tt> if register successfully.
     */
    boolean registerBroadcastStream(IScope scope, String name, IBroadcastStream stream);

    /**
     * Get names of existing broadcast streams in a scope.
     * 
     * @param scope
     *            Scope to get stream names from
     * @return List of stream names
     */
    Set<String> getBroadcastStreamNames(IScope scope);

    /**
     * Unregister a broadcast stream of a specific name from a scope.
     * 
     * @param scope
     *            Scope
     * @param name
     *            Stream name
     * @return <tt>true</tt> if unregister successfully.
     */
    boolean unregisterBroadcastStream(IScope scope, String name);

    /**
     * Unregister a broadcast stream of a specific name from a scope.
     * 
     * @param scope
     *            Scope
     * @param name
     *            Stream name
     * @param stream
     *            Broadcast stream
     * @return <tt>true</tt> if unregister successfully.
     */
    boolean unregisterBroadcastStream(IScope scope, String name, IBroadcastStream stream);

}
