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

import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IRtmpSampleAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default RtmpSampleAccess bean
 * 
 * @see org.red5.server.api.stream.IRtmpSampleAccess
 */
public class RtmpSampleAccess implements IRtmpSampleAccess {

    private static Logger logger = LoggerFactory.getLogger(RtmpSampleAccess.class);

    private boolean audioAllowed = false;

    private boolean videoAllowed = false;

    /**
     * Setter audioAllowed.
     * 
     * @param permission
     *            permission
     */
    public void setAudioAllowed(boolean permission) {
        logger.debug("setAudioAllowed: {}", permission);
        audioAllowed = permission;
    }

    /**
     * Setter videoAllowed
     * 
     * @param permission
     *            permission
     */
    public void setVideoAllowed(boolean permission) {
        logger.debug("setVideoAllowed: {}", permission);
        videoAllowed = permission;
    }

    /** {@inheritDoc} */
    public boolean isAudioAllowed(IScope scope) {
        logger.debug("isAudioAllowed: {}", audioAllowed);
        return audioAllowed;
    }

    /** {@inheritDoc} */
    public boolean isVideoAllowed(IScope scope) {
        logger.debug("isVideoAllowed: {}", videoAllowed);
        return videoAllowed;
    }

}
