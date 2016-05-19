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

package org.red5.server.service.mp3.impl;

import java.io.File;
import java.io.IOException;

import org.red5.io.IStreamableFile;
import org.red5.io.mp3.impl.MP3;
import org.red5.server.service.BaseStreamableFileService;
import org.red5.server.service.mp3.IMP3Service;

/**
 * Streamable file service extension for MP3
 */
public class MP3Service extends BaseStreamableFileService implements IMP3Service {

    /** {@inheritDoc} */
    @Override
    public String getPrefix() {
        return "mp3";
    }

    /** {@inheritDoc} */
    @Override
    public String getExtension() {
        return ".mp3";
    }

    /** {@inheritDoc} */
    @Override
    public IStreamableFile getStreamableFile(File file) throws IOException {
        return new MP3(file);
    }

}
