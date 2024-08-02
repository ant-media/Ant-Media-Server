/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.meta;

import java.io.File;
import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * IMetaService Defines the MetaData Service API
 * 
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface IMetaService {

    // Get FLV from FLVService
    // grab a reader from FLV	
    // Set up CuePoints
    // Set up MetaData
    // Pass CuePoint array into MetaData
    // read in current MetaData if there is MetaData
    // if there isn't MetaData, write new MetaData
    // Call writeMetaData method on MetaService
    // that in turn will write the current metadata
    // and the cuepoint data
    // after that, call writeMetaCue()
    // this will loop through all the tags making
    // sure that the cuepoints are inserted

    /**
     * Initiates writing of the MetaData
     * 
     * @param meta
     *            Metadata
     * @throws IOException
     *             I/O exception
     */
    public void write(IMetaData<?, ?> meta) throws IOException;

    /**
     * Writes the MetaData
     * 
     * @param metaData
     *            Metadata
     */
    public void writeMetaData(IMetaData<?, ?> metaData);

    /**
     * Writes the Meta Cue Points
     */
    public void writeMetaCue();

    /**
     * Read the MetaData
     * 
     * @return metaData Metadata
     * @param buffer
     *            IoBuffer source
     */
    public MetaData<?, ?> readMetaData(IoBuffer buffer);

    /**
     * Read the Meta Cue Points
     * 
     * @return Meta cue points
     */
    public IMetaCue[] readMetaCue();

    /**
     * Media file to be accessed
     * 
     * @param file
     *            file
     */
    public void setFile(File file);

    /**
     * Returns the file being accessed
     * 
     * @return file
     */
    public File getFile();

}
