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

package org.red5.server.service.mp4.impl;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.io.ITag;
import org.red5.io.flv.IKeyFrameDataAnalyzer.KeyFrameMeta;
import org.red5.io.mp4.impl.MP4Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP4ReaderTest extends TestCase {

	private static Logger log = LoggerFactory.getLogger(MP4ReaderTest.class);

	@Test
	public void testCtor() throws Exception {
		// use for the internal unit tests
		//File file = new File("target/test-classes/fixtures/sample.mp4");
		// test clip - bugs
		//File file = new File("C:/TEMP/red5-server-1.0/webapps/oflaDemo/streams/test_480_aac.f4v");
		//File file = new File("M:/backup/media/test_clips/dartmoor.mp4");
		//File file = new File("M:/backup/media/test_clips/ratatouille.mp4");
		//File file = new File("M:/backup/media/test_clips/0608071221.3g2");
		//File file = new File("M:/backup/media/test_clips/test1.3gp");
		//File file = new File("M:/backup/media/test_clips/ANewHope.mov");
		File file = new File("M:/Movies/Hitman.m4v"); // has pasp atom

		MP4Reader reader = new MP4Reader(file);

		KeyFrameMeta meta = reader.analyzeKeyFrames();
		log.debug("Meta: {}", meta);

		ITag tag = null;
		for (int t = 0; t < 32; t++) {
			tag = reader.readTag();
			log.debug("Tag: {}", tag);
		}

		log.info("----------------------------------------------------------------------------------");

		//File file2 = new File("E:/media/test_clips/IronMan.mov");
		//MP4Reader reader2 = new MP4Reader(file2, false);

	}

	@Test
	public void testBytes() throws Exception {
		//00 40 94 00 00 00 00 00 00 00 06 == 
		byte width[] = { (byte) 0x00, (byte) 0x40, (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		System.out.println("width: {}" + bytesToLong(width));

		//		byte height[] = { (byte) 0x40, (byte) 0x86, (byte) 0x80, (byte) 0x00 };
		//		System.out.println("height: {}" + bytesToInt(height));
		//
		//		byte timescale[] = { (byte) 0x40, (byte) 0xA7, (byte) 0x6A, (byte) 0x00 };
		//		System.out.println("timescale: {}" + bytesToInt(timescale));
		//
		//		byte duration[] = { (byte) 0x40, (byte) 0x6D, (byte) 0xE9, (byte) 0x03,
		//				(byte) 0x22, (byte) 0x7B, (byte) 0x4C, (byte) 0x47 };
		//		System.out.println("duration: {}" + bytesToLong(duration));
		//
		//		byte avcprofile[] = { (byte) 0x40, (byte) 0x53, (byte) 0x40,
		//				(byte) 0x00 };
		//		System.out.println("avcprofile: {}" + bytesToInt(avcprofile));
		//
		//		byte avclevel[] = { (byte) 0x40, (byte) 0x49, (byte) 0x80, (byte) 0x00 };
		//		System.out.println("avclevel: {}" + bytesToInt(avclevel));
		//
		//		byte aacaot[] = { (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40 };
		//		System.out.println("aacaot: {}" + bytesToLong(aacaot));
		//
		//		byte videoframerate[] = { (byte) 0x40, (byte) 0x37, (byte) 0xF9,
		//				(byte) 0xDB, (byte) 0x22, (byte) 0xD0, (byte) 0xE5, (byte) 0x60 };
		//		System.out.println("videoframerate: {}" + bytesToLong(videoframerate));
		//
		//		byte audiochannels[] = { (byte) 0x40, (byte) 0x00, (byte) 0x00,
		//				(byte) 0x00 };
		//		System.out.println("audiochannels: {}" + bytesToInt(audiochannels));
		//
		//		byte moovposition[] = { (byte) 0x40, (byte) 0x40, (byte) 0x00,
		//				(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		//		System.out.println("moovposition: {}" + bytesToLong(moovposition));
		//
		//		
		//byte[] arr = {(byte) 0x0f};
		//System.out.println("bbb: {}" + bytesToByte(arr));
		//byte[] arr = {(byte) 0xE5, (byte) 0x88, (byte) 0x80, (byte) 0x00, 
		//(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 };
		//System.out.println("bbb: {}" + bytesToLong(arr));
		byte[] arr = { (byte) 0, (byte) 0, (byte) 0x10, (byte) 0 };
		System.out.println("bbb: {}" + bytesToInt(arr));
	}

	public static long bytesToLong(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(8);
		buf.put(data);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.flip();
		return buf.getLong();
	}

	public static int bytesToInt(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(4);
		buf.put(data);
		buf.order(ByteOrder.BIG_ENDIAN);
		buf.flip();
		return buf.getInt();
	}

	public static short bytesToShort(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.put(data);
		buf.flip();
		return buf.getShort();
	}

	public static byte bytesToByte(byte[] data) {
		ByteBuffer buf = ByteBuffer.allocate(1);
		buf.put(data);
		buf.flip();
		return buf.get();
	}
}
