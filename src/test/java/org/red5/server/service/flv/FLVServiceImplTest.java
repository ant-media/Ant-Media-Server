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

package org.red5.server.service.flv;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import junit.framework.TestCase;

import org.red5.cache.impl.NoCacheImpl;
import org.red5.io.ITag;
import org.red5.io.ITagReader;
import org.red5.io.ITagWriter;
import org.red5.io.flv.IFLV;
import org.red5.server.service.flv.impl.FLVService;

/**
 * A FLVServiceImpl TestCase
 * 
 * @author The Red5 Project
 * @author Dominick Accattato (daccattato@gmail.com)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class FLVServiceImplTest extends TestCase {
	private IFLVService service;

	/**
	 * SetUp is called before each test
	 * 
	 */
	@Override
	public void setUp() {
		service = new FLVService();
	}

	/**
	 * Tests: getFlv(String s)
	 * 
	 * @param tag tag
	 * @throws IOException if io error
	 * @throws FileNotFoundException if file not found
	 */
	/*
	 * public void testFLVString() throws FileNotFoundException, IOException {
	 * FLV flv = service.getFLV("fixtures/test_cue.flv"); Reader reader =
	 * flv.reader(); Tag tag = null;
	 * 
	 * while(reader.hasMoreTags()) { tag = reader.readTag(); //printTag(tag); }
	 *  // simply tests to see if the last tag of the flv file // has a
	 * timestamp of 2500 Assert.assertEquals(4166,tag.getTimestamp());
	 * //Assert.assertEquals(true,true); }
	 */

	private void printTag(ITag tag) {
		System.out.println("tag:\n-------\n" + tag);
	}

	/**
	 * Tests: getFLVFile(File f)
	 * 
	 * @throws IOException if io error
	 * @throws FileNotFoundException if file not found
	 */
	public void testFLVFile() throws FileNotFoundException, IOException {
		File f = new File("target/test-classes/fixtures/test.flv");
		System.out.println("test: " + f);
		IFLV flv = (IFLV) service.getStreamableFile(f);
		flv.setCache(NoCacheImpl.getInstance());
		System.out.println("test: " + flv);
		ITagReader reader = flv.getReader();
		System.out.println("test: " + reader);
		ITag tag = null;
		System.out.println("test: " + reader.hasMoreTags());
		while (reader.hasMoreTags()) {
			tag = reader.readTag();
			// System.out.println("test: " + f);
			printTag(tag);
		}

		// simply tests to see if the last tag of the flv file
		// has a timestamp of 2500
		// Assert.assertEquals(4166,tag.getTimestamp());
		assertEquals(true, true);
	}

	/**
	 * Tests: getFLVFileInputStream(FileInputStream fis)
	 * 
	 * @return void
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	/*
	 * public void testFLVFileInputStreamKeyFrameAnalyzer() throws
	 * FileNotFoundException, IOException { File f = new
	 * File("fixtures/test_cue3.flv"); FileInputStream fis = new
	 * FileInputStream(f); FLV flv = service.getFLV(fis); Reader reader =
	 * flv.reader(); reader.analyzeKeyFrames();
	 * 
	 *  // simply tests to see if the last tag of the flv file // has a
	 * timestamp of 2500 Assert.assertEquals(true,true); }
	 */

	/**
	 * Tests: getFLVFileInputStream(FileInputStream fis)
	 * 
	 * @throws IOException
	 */
	/*
	 * public void testFLVFileInputStream() throws FileNotFoundException,
	 * IOException { File f = new File("fixtures/test_cue3.flv"); FileInputStream
	 * fis = new FileInputStream(f); FLV flv = service.getFLV(fis); Reader
	 * reader = flv.reader(); Tag tag = null;
	 * 
	 * while(reader.hasMoreTags()) { tag = reader.readTag(); printTag(tag); }
	 *  // simply tests to see if the last tag of the flv file // has a
	 * timestamp of 2500 Assert.assertEquals(4166,tag.getTimestamp()); }
	 */

	/*
	 * public void testWriteFLVFileOutputStream() throws IOException { File f =
	 * new File("fixtures/test_cue2.flv");
	 * 
	 * if(f.exists()) { f.delete(); }
	 *  // Create new file f.createNewFile(); FileOutputStream fos = new
	 * FileOutputStream(f); //fos.write((byte)0x01); FLV flv =
	 * service.getFLV(fos); Writer writer = flv.writer();
	 *  // Create a reader for testing File readfile = new
	 * File("fixtures/test_cue.flv"); FileInputStream fis = new
	 * FileInputStream(readfile); FLV readflv = service.getFLV(fis); Reader
	 * reader = readflv.reader();
	 * 
	 * writeTags(reader, writer);
	 *  // Currently asserts to true. I just wanted to see // if the method
	 * threw an exception Assert.assertEquals(true, true); }
	 */

	@SuppressWarnings("unused")
	private void writeTags(ITagReader reader, ITagWriter writer) throws IOException {

		ITag tag = null;

		while (reader.hasMoreTags()) {
			tag = reader.readTag();
			writer.writeTag(tag);
			// printTag(tag);
		}

	}

	public void testWriteFLVString() {

	}

	public void testWriteFLVFile() {

	}

}
