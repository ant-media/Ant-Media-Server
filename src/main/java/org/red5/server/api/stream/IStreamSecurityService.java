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

package org.red5.server.api.stream;

import java.util.Set;

import org.red5.server.api.scope.IScopeService;

/**
 * Service that supports protecting access to streams.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IStreamSecurityService extends IScopeService {

    /**
     * Name of a bean defining that scope service.
     * */
    public static final String BEAN_NAME = "streamSecurityService";

    /**
     * Add handler that protects stream publishing.
     * 
     * @param handler
     *            Handler to add.
     */
    public void registerStreamPublishSecurity(IStreamPublishSecurity handler);

    /**
     * Remove handler that protects stream publishing.
     * 
     * @param handler
     *            Handler to remove.
     */
    public void unregisterStreamPublishSecurity(IStreamPublishSecurity handler);

    /**
     * Get handlers that protect stream publishing.
     * 
     * @return list of handlers
     */
    public Set<IStreamPublishSecurity> getStreamPublishSecurity();

    /**
     * Add handler that protects stream playback.
     * 
     * @param handler
     *            Handler to add.
     */
    public void registerStreamPlaybackSecurity(IStreamPlaybackSecurity handler);

    /**
     * Remove handler that protects stream playback.
     * 
     * @param handler
     *            Handler to remove.
     */
    public void unregisterStreamPlaybackSecurity(IStreamPlaybackSecurity handler);

    /**
     * Get handlers that protect stream plaback.
     * 
     * @return list of handlers
     */
    public Set<IStreamPlaybackSecurity> getStreamPlaybackSecurity();

}
