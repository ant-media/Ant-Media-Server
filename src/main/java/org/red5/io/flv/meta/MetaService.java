/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.flv.meta;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.io.amf.Input;
import org.red5.io.amf.Output;
import org.red5.io.flv.impl.FLVReader;
import org.red5.io.flv.impl.FLVWriter;
import org.red5.io.flv.impl.Tag;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MetaService represents a MetaData service in Spring
 * 
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class MetaService implements IMetaService {

    protected static Logger log = LoggerFactory.getLogger(MetaService.class);

    /**
     * Source file
     */
    File file;

    /**
     * MetaService constructor
     */
    public MetaService() {
        super();
    }

    public MetaService(File poFil) {
        this();
        this.file = poFil;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(IMetaData<?, ?> meta) throws IOException {
        // Get cue points, FLV reader and writer
        IMetaCue[] metaArr = meta.getMetaCue();
        FLVReader reader = new FLVReader(file, false);
        FLVWriter writer = new FLVWriter(file, false);
        ITag tag = null;
        // Read first tag
        if (reader.hasMoreTags()) {
            tag = reader.readTag();
            if (tag.getDataType() == IoConstants.TYPE_METADATA) {
                if (!reader.hasMoreTags()) {
                    throw new IOException("File we're writing is metadata only?");
                }
            }
        }
        if (tag == null) {
            throw new IOException("Tag was null");
        }
        meta.setDuration(((double) reader.getDuration() / 1000));
        meta.setVideoCodecId(reader.getVideoCodecId());
        meta.setAudioCodecId(reader.getAudioCodecId());

        ITag injectedTag = injectMetaData(meta, tag);
        injectedTag.setPreviousTagSize(0);
        tag.setPreviousTagSize(injectedTag.getBodySize());

        // TODO look into why this fails in the unit test
        try {
            writer.writeTag(injectedTag);
            writer.writeTag(tag);
        } catch (Exception e) {
            log.warn("Metadata insert failed", e);
            return;
        }

        int cuePointTimeStamp = 0;
        int counter = 0;

        if (metaArr != null) {
            Arrays.sort(metaArr);
            cuePointTimeStamp = getTimeInMilliseconds(metaArr[0]);
        }
        while (reader.hasMoreTags()) {
            tag = reader.readTag();
            // if there are cuePoints in the array
            if (counter < metaArr.length) {
                // If the tag has a greater timestamp than the
                // cuePointTimeStamp, then inject the tag
                while (tag.getTimestamp() > cuePointTimeStamp) {
                    injectedTag = injectMetaCue(metaArr[counter], tag);
                    writer.writeTag(injectedTag);
                    tag.setPreviousTagSize(injectedTag.getBodySize());
                    // Advance to the next CuePoint
                    counter++;
                    if (counter > (metaArr.length - 1)) {
                        break;
                    }
                    cuePointTimeStamp = getTimeInMilliseconds(metaArr[counter]);
                }
            }
            if (tag.getDataType() != IoConstants.TYPE_METADATA) {
                writer.writeTag(tag);
            }
        }
        writer.close();
    }

    /**
     * Merges the two Meta objects
     * 
     * @param metaData1
     *            First metadata object
     * @param metaData2
     *            Second metadata object
     * @return Merged metadata
     */
    @SuppressWarnings({ "unchecked" })
    public static IMeta mergeMeta(IMetaData<?, ?> metaData1, IMetaData<?, ?> metaData2) {
        //walk the entries and merge them
        //1. higher number values trump lower ones
        //2. true considered higher than false
        //3. strings are not replaced
        Map<String, Object> map1 = ((Map<String, Object>) metaData1);
        Set<Entry<String, Object>> set1 = map1.entrySet();
        Map<String, Object> map2 = ((Map<String, Object>) metaData2);
        Set<Entry<String, Object>> set2 = map2.entrySet();
        //map to hold updates / replacements
        Map<String, Object> rep = new HashMap<String, Object>();
        //loop to update common elements
        for (Entry<String, Object> entry1 : set1) {
            String key1 = entry1.getKey();
            if (map2.containsKey(key1)) {
                Object value1 = map1.get(key1);
                Object value2 = map2.get(key1);
                //we dont replace strings
                //check numbers
                if (value1 instanceof Double) {
                    if (Double.valueOf(value1.toString()).doubleValue() < Double.valueOf(value2.toString()).doubleValue()) {
                        rep.put(key1, value2);
                    }
                } else if (value1 instanceof Integer) {
                    if (Integer.valueOf(value1.toString()).intValue() < Integer.valueOf(value2.toString()).intValue()) {
                        rep.put(key1, value2);
                    }
                } else if (value1 instanceof Long) {
                    if (Long.valueOf(value1.toString()).longValue() < Long.valueOf(value2.toString()).longValue()) {
                        rep.put(key1, value2);
                    }
                }
                //check boolean
                if (value1 instanceof Boolean) {
                    //consider true > false
                    if (!Boolean.valueOf(value1.toString()) && Boolean.valueOf(value2.toString())) {
                        rep.put(key1, value2);
                    }
                }
            }
        }
        //remove all changed
        set1.removeAll(rep.entrySet());
        //add the updates
        set1.addAll(rep.entrySet());
        //perform a union / adds all elements missing from set1
        set1.addAll(set2);
        //return the original object with merges
        return metaData1;
    }

    /**
     * Injects metadata (other than Cue points) into a tag
     * 
     * @param meta
     *            Metadata
     * @param tag
     *            Tag
     * @return New tag with injected metadata
     */
    private static ITag injectMetaData(IMetaData<?, ?> meta, ITag tag) {
        IoBuffer bb = IoBuffer.allocate(1000);
        bb.setAutoExpand(true);
        Output out = new Output(bb);
        Serializer.serialize(out, "onMetaData");
        Serializer.serialize(out, meta);
        IoBuffer tmpBody = out.buf().flip();
        int tmpBodySize = out.buf().limit();
        int tmpPreviousTagSize = tag.getPreviousTagSize();
        return new Tag(IoConstants.TYPE_METADATA, 0, tmpBodySize, tmpBody, tmpPreviousTagSize);
    }

    /**
     * Injects metadata (Cue Points) into a tag
     * 
     * @param meta
     *            Metadata (cue points)
     * @param tag
     *            Tag
     * @return ITag tag New tag with injected metadata
     */
    private static ITag injectMetaCue(IMetaCue meta, ITag tag) {
        // IMeta meta = (MetaCue) cue;
        Output out = new Output(IoBuffer.allocate(1000));
        Serializer.serialize(out, "onCuePoint");
        Serializer.serialize(out, meta);

        IoBuffer tmpBody = out.buf().flip();
        int tmpBodySize = out.buf().limit();
        int tmpPreviousTagSize = tag.getPreviousTagSize();
        int tmpTimestamp = getTimeInMilliseconds(meta);

        return new Tag(IoConstants.TYPE_METADATA, tmpTimestamp, tmpBodySize, tmpBody, tmpPreviousTagSize);
    }

    /**
     * Returns a timestamp of cue point in milliseconds
     * 
     * @param metaCue
     *            Cue point
     * @return int time Timestamp of given cue point (in milliseconds)
     */
    private static int getTimeInMilliseconds(IMetaCue metaCue) {
        return (int) (metaCue.getTime() * 1000.00);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetaData(IMetaData<?, ?> metaData) {
        IMetaCue meta = (MetaCue<?, ?>) metaData;
        Output out = new Output(IoBuffer.allocate(1000));
        Serializer.serialize(out, "onCuePoint");
        Serializer.serialize(out, meta);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetaCue() {
    }

    /**
     * @return Returns the file.
     */
    @Override
    public File getFile() {
        return file;
    }

    /**
     * @param file
     *            The file to set.
     */
    @Override
    public void setFile(File file) {
        this.file = file;
    }

    /** {@inheritDoc} */
    // TODO need to fix
    @Override
    public MetaData<?, ?> readMetaData(IoBuffer buffer) {
        MetaData<?, ?> retMeta = new MetaData<String, Object>();
        Input input = new Input(buffer);
        String metaType = Deserializer.deserialize(input, String.class);
        log.debug("Metadata type: {}", metaType);
        Map<String, ?> m = Deserializer.deserialize(input, Map.class);
        retMeta.putAll(m);
        return retMeta;
    }

    /** {@inheritDoc} */
    @Override
    public IMetaCue[] readMetaCue() {
        return null;
    }

}
