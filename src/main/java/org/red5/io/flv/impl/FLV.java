/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.cache.ICacheStore;
import org.red5.cache.ICacheable;
import org.red5.cache.impl.NoCacheImpl;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.IoConstants;
import org.red5.io.flv.IFLV;
import org.red5.io.flv.meta.IMetaData;
import org.red5.io.flv.meta.IMetaService;
import org.red5.io.flv.meta.MetaData;
import org.red5.io.flv.meta.MetaService;
import org.red5.media.processor.IPostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FLVImpl implements the FLV api
 * 
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class FLV implements IFLV {

    protected static Logger log = LoggerFactory.getLogger(FLV.class);

    private static ICacheStore cache;

    // stores (in-order) the writer post-process implementations
    private static LinkedList<Class<IPostProcessor>> writePostProcessors;

    private File file;

    private boolean generateMetadata;

    private IMetaService metaService;

    private IMetaData<?, ?> metaData;

    /*
     * 0x08 AUDIO Contains an audio packet similar to a SWF SoundStreamBlock plus codec information 0x09 VIDEO Contains a video packet similar to a SWF VideoFrame plus codec
     * information 0x12 META Contains two AMF packets, the name of the event and the data to go with it soundType (byte & 0x01) == 0 0: mono, 1: stereo soundSize (byte & 0x02) == 1 0:
     * 8-bit, 2: 16-bit soundRate (byte & 0x0C) == 2 0: 5.5kHz, 1: 11kHz, 2: 22kHz, 3: 44kHz soundFormat (byte & 0xf0) == 4 0: Uncompressed, 1: ADPCM, 2: MP3, 5: Nellymoser 8kHz mono,
     * 6: Nellymoser codecID (byte & 0x0f) == 0 2: Sorensen H.263, 3: Screen video, 4: On2 VP6 frameType (byte & 0xf0) == 4 1: keyframe, 2: inter frame, 3: disposable inter frame
     * http://www.adobe.com/devnet/flv/pdf/video_file_format_spec_v10.pdf
     */

    /**
     * Default constructor, used by Spring so that parameters may be injected.
     */
    public FLV() {
    }

    /**
     * Create FLV from given file source
     * 
     * @param file
     *            File source
     */
    public FLV(File file) {
        this(file, false);
    }

    /**
     * Create FLV from given file source and with specified metadata generation option
     * 
     * @param file
     *            File source
     * @param generateMetadata
     *            Metadata generation option
     */
    public FLV(File file, boolean generateMetadata) {
        this.file = file;
        this.generateMetadata = generateMetadata;
        if (!generateMetadata) {
            try {
                FLVReader reader = new FLVReader(this.file);
                ITag tag = null;
                int count = 0;
                while (reader.hasMoreTags() && (++count < 5)) {
                    tag = reader.readTag();
                    if (tag.getDataType() == IoConstants.TYPE_METADATA) {
                        if (metaService == null) {
                            metaService = new MetaService();
                        }
                        metaData = metaService.readMetaData(tag.getBody());
                    }
                }
                reader.close();
            } catch (Exception e) {
                log.error("An error occured looking for metadata", e);
            }
        }
    }

    /**
     * Sets the cache implementation to be used.
     * 
     * @param cache
     *            Cache store
     */
    @Override
    public void setCache(ICacheStore cache) {
        FLV.cache = cache;
    }

    /**
     * Sets a writer post processor.
     * 
     * @param writerPostProcessor IPostProcess implementation class name
     */
    @SuppressWarnings("unchecked")
    public void setWriterPostProcessor(String writerPostProcessor) {
        if (writePostProcessors == null) {
            writePostProcessors = new LinkedList<>();
        }
        try {
            writePostProcessors.add((Class<IPostProcessor>) Class.forName(writerPostProcessor));
        } catch (Exception e) {
            log.debug("Write post process implementation: {} was not found", writerPostProcessor);
        }
    }

    /**
     * Sets a group of writer post processors.
     * 
     * @param writerPostProcessors IPostProcess implementation class names
     */
    @SuppressWarnings("unchecked")
    public void setWriterPostProcessors(Set<String> writerPostProcessors) {
        if (writePostProcessors == null) {
            writePostProcessors = new LinkedList<>();
        }
        for (String writerPostProcessor : writerPostProcessors) {
            try {
                writePostProcessors.add((Class<IPostProcessor>) Class.forName(writerPostProcessor));
            } catch (Exception e) {
                log.debug("Write post process implementation: {} was not found", writerPostProcessor);
            }
        }
    }

    public LinkedList<Class<IPostProcessor>> getWritePostProcessors() {
        return writePostProcessors;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasMetaData() {
        return metaData != null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public IMetaData getMetaData() throws FileNotFoundException {
        metaService.setFile(file);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasKeyFrameData() {
        //if (hasMetaData()) {
        //    return !((MetaData) metaData).getKeyframes().isEmpty();
        //}
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void setKeyFrameData(Map keyframedata) {
        if (!hasMetaData()) {
            metaData = new MetaData();
        }
        //The map is expected to contain two entries named "times" and "filepositions",
        //both of which contain a map keyed by index and time or position values.
        Map<String, Double> times = new HashMap<>();
        Map<String, Double> filepositions = new HashMap<>();
        if (keyframedata.containsKey("times")) {
            Map inTimes = (Map) keyframedata.get("times");
            for (Object o : inTimes.entrySet()) {
                Map.Entry<String, Double> entry = (Map.Entry<String, Double>) o;
                times.put(entry.getKey(), entry.getValue());
            }
        }
        ((MetaData) metaData).put("times", times);
        //
        if (keyframedata.containsKey("filepositions")) {
            Map inPos = (Map) keyframedata.get("filepositions");
            for (Object o : inPos.entrySet()) {
                Map.Entry<String, Double> entry = (Map.Entry<String, Double>) o;
                filepositions.put(entry.getKey(), entry.getValue());
            }
        }
        ((MetaData) metaData).put("filepositions", filepositions);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Map getKeyFrameData() {
        Map keyframes = null;
        //if (hasMetaData()) {
        //    keyframes = ((MetaData) metaData).getKeyframes();
        //}
        return keyframes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshHeaders() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flushHeaders() throws IOException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITagReader getReader() throws IOException {
        FLVReader reader = null;
        IoBuffer fileData;
        String fileName = file.getName();
        // if no cache is set an NPE will be thrown
        if (cache == null) {
            log.info("FLV cache is null, forcing NoCacheImpl instance");
            cache = NoCacheImpl.getInstance();
        }
        ICacheable ic = cache.get(fileName);
        // look in the cache before reading the file from the disk
        if (null == ic || (null == ic.getByteBuffer())) {
            if (file.exists()) {
                log.debug("File size: {}", file.length());
                reader = new FLVReader(file, generateMetadata);
                // get a ref to the mapped byte buffer
                fileData = reader.getFileData();
                // offer the uncached file to the cache
                if (fileData != null && cache.offer(fileName, fileData)) {
                    log.debug("Item accepted by the cache: {}", fileName);
                } else {
                    log.debug("Item will not be cached: {}", fileName);
                }
            } else {
                log.info("Creating new file: {}", file);
                file.createNewFile();
            }
        } else {
            fileData = IoBuffer.wrap(ic.getBytes());
            reader = new FLVReader(fileData, generateMetadata);
        }
        return reader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITagReader readerFromNearestKeyFrame(int seekPoint) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITagWriter getWriter() throws IOException {
        log.info("getWriter: {}", file);
        return new FLVWriter(file.toPath(), false);
    }

    /** {@inheritDoc} */
    @Override
    public ITagWriter getAppendWriter() throws IOException {
        log.info("getAppendWriter: {}", file);
        return new FLVWriter(file.toPath(), true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITagWriter writerFromNearestKeyFrame(int seekPoint) {
        return null;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public void setMetaData(IMetaData meta) throws IOException {
        log.info("setMetaData: {}", meta);
        if (metaService == null) {
            metaService = new MetaService(file);
        }
        //if the file is not checked the write may produce an NPE
        if (metaService.getFile() == null) {
            metaService.setFile(file);
        }
        metaService.write(meta);
        metaData = meta;
    }

    /** {@inheritDoc} */
    @Override
    public void setMetaService(IMetaService service) {
        metaService = service;
    }
}
