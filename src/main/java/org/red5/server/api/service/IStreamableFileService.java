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

package org.red5.server.api.service;

import java.io.File;
import java.io.IOException;

import org.red5.io.IStreamableFile;

/**
 * Provides access to files that can be streamed.
 */
public interface IStreamableFileService {

    /**
     * Sets the prefix.
     * 
     * @param prefix
     *            prefix
     */
    public void setPrefix(String prefix);

    /**
     * Getter for prefix. Prefix is used in filename composition to fetch real file name.
     *
     * @return prefix
     */
    public String getPrefix();

    /**
     * Sets the file extensions serviced. If there are more than one, they are separated by commas.
     * 
     * @param extension
     *            extension
     */
    public void setExtension(String extension);

    /**
     * Getter for extension of file
     *
     * @return File extension that is used
     */
    public String getExtension();

    /**
     * Prepair given string to conform filename requirements, for example, add extension to the end if missing.
     * 
     * @param name
     *            String to format
     * @return Correct filename
     */
    public String prepareFilename(String name);

    /**
     * Check whether file can be used by file service, that is, it does exist and have valid extension
     * 
     * @param file
     *            File object
     * @return true if file exist and has valid extension, false otherwise
     */
    public boolean canHandle(File file);

    /**
     * Return streamable file reference. For FLV files returned streamable file already has generated metadata injected.
     *
     * @param file
     *            File resource
     * @return Streamable file resource
     * @throws IOException
     *             Thrown if there were problems accessing given file
     */
    public IStreamableFile getStreamableFile(File file) throws IOException;

}
