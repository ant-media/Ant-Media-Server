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

package org.red5.server.service;

import java.io.File;
import java.io.IOException;

import org.red5.io.IStreamableFile;
import org.red5.server.api.service.IStreamableFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for streamable file services.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public abstract class BaseStreamableFileService implements IStreamableFileService {

    private static final Logger log = LoggerFactory.getLogger(BaseStreamableFileService.class);

    /** {@inheritDoc} */
    public void setPrefix(String prefix) {
    }

    /** {@inheritDoc} */
    public abstract String getPrefix();

    /** {@inheritDoc} */
    public void setExtension(String extension) {
    }

    /** {@inheritDoc} */
    public abstract String getExtension();

    /** {@inheritDoc} */
    public String prepareFilename(String name) {
        String prefix = getPrefix() + ':';
        if (name.startsWith(prefix)) {
            name = name.substring(prefix.length());
            // if there is no extension on the file add the first one
            log.debug("prepareFilename - lastIndexOf: {} length: {}", name.lastIndexOf('.'), name.length());
            if ((name.lastIndexOf('.') == -1) || (name.lastIndexOf('.') != name.length() - 4)) {
                name = name + getExtension().split(",")[0];
            }
        }

        return name;
    }

    /** {@inheritDoc} */
    public boolean canHandle(File file) {
        boolean valid = false;
        if (file.exists()) {
            String absPath = file.getAbsolutePath().toLowerCase();
            int dotIndex = absPath.lastIndexOf('.');
            if (dotIndex > -1) {
                String fileExt = absPath.substring(dotIndex);
                log.debug("canHandle - Path: {} Ext: {}", absPath, fileExt);
                String[] exts = getExtension().split(",");
                for (String ext : exts) {
                    if (ext.equals(fileExt)) {
                        valid = true;
                        break;
                    }
                }
            } else {
                log.warn("No file extension was detected, please retry with a supported extension: {}", getExtension());
            }
        }
        return valid;
    }

    /** {@inheritDoc} */
    public abstract IStreamableFile getStreamableFile(File file) throws IOException;

}
