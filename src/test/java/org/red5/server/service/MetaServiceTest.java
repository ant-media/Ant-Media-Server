/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright 2006-2013 by respective authors (see below). All rights reserved.
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

import junit.framework.TestCase;

import org.red5.cache.impl.NoCacheImpl;
import org.red5.io.flv.IFLV;
import org.red5.io.flv.meta.ICueType;
import org.red5.io.flv.meta.IMetaCue;
import org.red5.io.flv.meta.MetaCue;
import org.red5.io.flv.meta.MetaData;
import org.red5.io.flv.meta.MetaService;
import org.red5.server.service.flv.impl.FLVService;

public class MetaServiceTest extends TestCase {

    private FLVService service;

    private MetaService metaService;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create a FLV Service
        service = new FLVService();

        // Create a Meta Service
        metaService = new MetaService();
    }

    /**
     * Test writing meta data
     * 
     * @throws IOException
     *             if io exception
     */
    public void testWrite() throws IOException {
        String path = "target/test-classes/fixtures/test.flv";
        File f = new File(path);
        System.out.println("Path: " + f.getAbsolutePath());
        if (!f.exists()) {
            // try test subdirectory
            path = "target/test-classes/fixtures/test.flv";
            f = new File(path);
            System.out.println("Path: " + f.getAbsolutePath());
        }
        // Get MetaData to embed
        MetaData<?, ?> meta = createMeta();
        // Read in a FLV file for reading tags
        IFLV flv = (IFLV) service.getStreamableFile(f);
        flv.setCache(NoCacheImpl.getInstance());
        // set the MetaService
        flv.setMetaService(metaService);
        // set the MetaData
        flv.setMetaData(meta);
    }

    /**
     * Create some test Metadata for insertion.
     *
     * @return MetaData meta
     */
    private MetaData<?, ?> createMeta() {
        IMetaCue metaCue[] = new MetaCue[2];

        IMetaCue cp = new MetaCue<Object, Object>();
        cp.setName("cue_1");
        cp.setTime(0.01);
        cp.setType(ICueType.EVENT);

        IMetaCue cp1 = new MetaCue<Object, Object>();
        cp1.setName("cue_2");
        cp1.setTime(0.03);
        cp1.setType(ICueType.EVENT);

        // add cuepoints to array
        metaCue[0] = cp;
        metaCue[1] = cp1;

        MetaData<?, ?> meta = new MetaData<Object, Object>();
        meta.setMetaCue(metaCue);
        meta.setCanSeekToEnd(true);
        meta.setDuration(300);
        meta.setFrameRate(15);
        meta.setHeight(400);
        meta.setWidth(300);

        return meta;
    }

}
