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

import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.red5.io.flv.meta.MetaData;

/**
 * MetaData TestCase
 * 
 * @author The Red5 Project
 * @author daccattato (daccattato@gmail.com)
 */
@Tag("fast")
public class MetaDataTest {

    MetaData<?, ?> data;

    /** Constructs a new MetaDataTest. */
    public MetaDataTest() {
        data = new MetaData<Object, Object>();
    }

	/** {@inheritDoc} */
	@BeforeEach
	public void setUp() throws Exception {

        data.setCanSeekToEnd(true);
        data.setDuration(7.347);
        data.setFrameRate(15);
        data.setHeight(333);
        data.setVideoCodecId(4);
        data.setVideoDataRate(400);
        data.setWidth(300);
    }

	/** {@inheritDoc} */
	@AfterEach
	public void tearDown() {
        data = null;
    }

	@Test
	public void testCanSeekToEnd() {
        assertEquals(true, data.getCanSeekToEnd());
    }

	@Test
	public void testDuration() {
        assertEquals(7.347, data.getDuration(), 0);
    }

	@Test
	public void testFrameRate() {
        assertEquals(15.0, data.getFrameRate());
    }

	@Test
	public void testHeight() {
        assertEquals(333, data.getHeight());
    }

	@Test
	public void testVideoCodecId() {
        assertEquals(4, data.getVideoCodecId());
    }

	@Test
	public void testVideoDataRate() {
        assertEquals(400, data.getVideoDataRate());
    }

	@Test
	public void testWidth() {
        assertEquals(300, data.getWidth());
    }

}
