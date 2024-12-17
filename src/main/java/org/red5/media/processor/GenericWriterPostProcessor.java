/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.media.processor;

import java.io.File;
import java.util.Arrays;

import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.io.flv.impl.FLVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example post-processor implementation which counts data types in a given file.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class GenericWriterPostProcessor implements IPostProcessor {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private File file;

    @Override
    public void init(Object... objs) {
        log.info("init: {}", Arrays.toString(objs));
        // we expect a file path to which a writer wrote to
        file = new File(objs[0].toString());
    }

    @Override
    public void run() {
        if (file != null) {
            try {
                FLVReader reader = new FLVReader(file);
                ITag tag = null;
                int audio = 0, video = 0, meta = 0;
                while (reader.hasMoreTags()) {
                    tag = reader.readTag();
                    if (tag != null) {
                        switch (tag.getDataType()) {
                            case IoConstants.TYPE_AUDIO:
                                audio++;
                                break;
                            case IoConstants.TYPE_VIDEO:
                                video++;
                                break;
                            case IoConstants.TYPE_METADATA:
                                meta++;
                                break;
                        }
                    }
                }
                reader.close();
                log.info("Data type counts - audio: {} video: {} metadata: {}", audio, video, meta);
            } catch (Exception e) {
                log.error("Exception reading: {}", file.getName(), e);
            }
        } else {
            log.warn("File for parsing was not found");
        }
    }

}
