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

package org.red5.server.net;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.io.utils.HexDump;
import org.red5.server.net.rtmp.RTMPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class RTMPUtilsTest extends TestCase {

	protected static Logger log = LoggerFactory.getLogger(RTMPUtilsTest.class);

	public void testDecodingHeader() {

		if (log.isDebugEnabled()) {
			log.debug("Testing");
			/*
			 log.debug(""+(0x03 >> 6));
			 log.debug(""+(0x43 >> 6));
			 log.debug(""+(0x83 >> 6));
			 log.debug(""+((byte)(((byte)0xC3) >> 6)));
			 */
		}
		byte test;
		test = 0x03;
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(0, RTMPUtils.decodeHeaderSize(test, 1));
		test = (byte) (0x43);
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(1, RTMPUtils.decodeHeaderSize(test, 1));
		test = (byte) (0x83);
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(-2, RTMPUtils.decodeHeaderSize(test, 1));
		test = (byte) (0xC3 - 256);
		if (log.isDebugEnabled()) {
			log.debug(HexDump.byteArrayToHexString(new byte[] { test }));
			log.debug("" + test);
			log.debug("" + RTMPUtils.decodeHeaderSize(test, 1));
		}
		assertEquals(-1, RTMPUtils.decodeHeaderSize(test, 1));
	}

	@Test
	public void testDiffTimestamps() {
		int a;
		int b;

		a = 0;
		b = 1;
		assertEquals(-1, RTMPUtils.diffTimestamps(a, b));
		assertEquals(1, RTMPUtils.diffTimestamps(b, a));

		a = Integer.MAX_VALUE;
		b = Integer.MIN_VALUE;
		assertEquals(-1, RTMPUtils.diffTimestamps(a, b));
		assertEquals(1, RTMPUtils.diffTimestamps(b, a));

		a = Integer.MAX_VALUE;
		b = 0;
		assertEquals(Integer.MAX_VALUE, RTMPUtils.diffTimestamps(a, b));
		assertEquals(-Integer.MAX_VALUE, RTMPUtils.diffTimestamps(b, a));

		a = -1;
		b = 0;
		assertEquals(0xFFFFFFFFL, RTMPUtils.diffTimestamps(a, b));
		assertEquals(-0xFFFFFFFFL, RTMPUtils.diffTimestamps(b, a));

		a = Integer.MIN_VALUE;
		b = 0;
		assertEquals(0x80000000L, RTMPUtils.diffTimestamps(a, b));
		assertEquals(-0x80000000L, RTMPUtils.diffTimestamps(b, a));

	}
}
