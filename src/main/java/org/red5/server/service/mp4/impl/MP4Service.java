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

package org.red5.server.service.mp4.impl;

import java.io.File;
import java.io.IOException;

import org.red5.io.IStreamableFile;
import org.red5.io.mp4.impl.MP4;
import org.red5.server.service.BaseStreamableFileService;
import org.red5.server.service.mp4.IMP4Service;

/**
 * A MP4ServiceImpl sets up the service and hands out MP4 objects to its callers.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class MP4Service extends BaseStreamableFileService implements IMP4Service {

    /**
     * File extensions handled by this service. If there are more than one, they are comma separated. '.mp4' must be the first on the list because it is the default file extension for mp4 files.
     * 
     * @see http://help.adobe.com/en_US/flashmediaserver/devguide/WS5b3ccc516d4fbf351e63e3d11a0773d117-7fc8.html
     */
    private static String extension = ".mp4,.f4v,.mov,.3gp,.3g2";

    private static String prefix = "mp4";

    /** {@inheritDoc} */
    @Override
    public void setPrefix(String prefix) {
        MP4Service.prefix = prefix;
    }

    /** {@inheritDoc} */
    @Override
    public String getPrefix() {
        return prefix;
    }

    /** {@inheritDoc} */
    @Override
    public void setExtension(String extension) {
        MP4Service.extension = extension;
    }

    /** {@inheritDoc} */
    @Override
    public String getExtension() {
        return extension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStreamableFile getStreamableFile(File file) throws IOException {
        return new MP4(file);
    }

}
