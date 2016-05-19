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

package org.red5.server.service.m4a.impl;

import java.io.File;
import java.io.IOException;

import org.red5.io.IStreamableFile;
import org.red5.io.m4a.impl.M4A;
import org.red5.server.service.BaseStreamableFileService;
import org.red5.server.service.m4a.IM4AService;

/**
 * A M4AServiceImpl sets up the service and hands out M4A objects to its callers.
 * 
 * @author The Red5 Project
 * @author Paul Gregoire, (mondain@gmail.com)
 */
public class M4AService extends BaseStreamableFileService implements IM4AService {

    /**
     * File extensions handled by this service. If there are more than one, they are comma separated.
     */
    private static String extension = ".f4a,.m4a,.aac";

    private static String prefix = "f4a";

    /** {@inheritDoc} */
    @Override
    public void setPrefix(String prefix) {
        M4AService.prefix = prefix;
    }

    /** {@inheritDoc} */
    @Override
    public String getPrefix() {
        return prefix;
    }

    /** {@inheritDoc} */
    @Override
    public void setExtension(String extension) {
        M4AService.extension = extension;
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
        return new M4A(file);
    }

}
