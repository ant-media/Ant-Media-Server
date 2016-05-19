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

import java.io.IOException;

import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;

/**
 * Interface represents streamable file with tag reader and writers (one for plain mode and one for append)
 */
public interface IStreamableFile {

    /**
     * Returns a reader to parse and read the tags inside the file.
     * 
     * @return the reader Tag reader
     * @throws java.io.IOException
     *             I/O exception
     */
    public ITagReader getReader() throws IOException;

    /**
     * Returns a writer that creates a new file or truncates existing contents.
     * 
     * @return the writer Tag writer
     * @throws java.io.IOException
     *             I/O exception
     */
    public ITagWriter getWriter() throws IOException;

    /**
     * Returns a Writer which is setup to append to the file.
     * 
     * @return the writer Tag writer used for append mode
     * @throws java.io.IOException
     *             I/O exception
     */
    public ITagWriter getAppendWriter() throws IOException;

}
