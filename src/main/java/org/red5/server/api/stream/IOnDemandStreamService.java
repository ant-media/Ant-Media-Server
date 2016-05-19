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

import org.red5.server.api.scope.IScope;
import org.red5.server.api.scope.IScopeService;

public interface IOnDemandStreamService extends IScopeService {

    public static String BEAN_NAME = "onDemandStreamService";

    /**
     * Has the service an on-demand stream with the passed name?
     * 
     * @param scope
     *            the scope to check for the stream
     * @param name
     *            the name of the stream
     * @return true if the stream exists, false otherwise
     */
    public boolean hasOnDemandStream(IScope scope, String name);

    /**
     * Get a stream that can be used for playback of the on-demand stream
     * 
     * @param scope
     *            the scope to return the stream from
     * @param name
     *            the name of the stream
     * @return the on-demand stream
     */
    public IOnDemandStream getOnDemandStream(IScope scope, String name);

}
