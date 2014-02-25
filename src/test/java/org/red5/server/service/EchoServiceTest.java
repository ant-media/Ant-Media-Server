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

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author The Red5 Project
 * @author Chris Allen (mrchrisallen@gmail.com)
 */
public class EchoServiceTest extends TestCase {

	final private Logger log = LoggerFactory.getLogger(this.getClass());

	private EchoService echoService;

	/** {@inheritDoc} */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		echoService = new EchoService();
	}

	/** {@inheritDoc} */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		echoService = null;
	}

	public void testEchoArray() {
		Object[] startArray = { "first", "second", "third" };
		Object[] resultArray = echoService.echoArray(startArray);
		assertEquals(startArray[0], resultArray[0]);
		assertEquals(startArray[1], resultArray[1]);
		assertEquals(startArray[2], resultArray[2]);
	}

	public void testEchoBoolean() {
		boolean b = true;
		assertTrue(echoService.echoBoolean(b));
	}

	public void testEchoDate() throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
		Date startDate = dateFormat.parse("01-26-1974");
		Date returnDate = echoService.echoDate(startDate);
		assertEquals(startDate.getTime(), returnDate.getTime());
	}

	@SuppressWarnings("unchecked")
	public void testEchoList() {
		List<String> startList = new ArrayList<String>();
		startList.add(0, "first");
		startList.add(1, "second");
		List<String> resultList = echoService.echoList(startList);
		assertEquals(startList.get(0), resultList.get(0));
		assertEquals(startList.get(1), resultList.get(1));
	}

	public void testEchoNumber() {
		double num = 100;
		assertEquals(200, echoService.echoNumber(num), echoService.echoNumber(num));
	}

	@SuppressWarnings("unchecked")
	public void testEchoObject() {
		String str = "entry one";
		Date date = new Date();
		Map<String, Comparable<?>> startMap = new HashMap<String, Comparable<?>>();
		startMap.put("string", str);
		startMap.put("date", date);
		Map<String, Comparable<?>> resultMap = (Map<String, Comparable<?>>) echoService.echoObject(startMap);
		assertEquals(startMap.get("string"), resultMap.get("string"));
		assertEquals(startMap.get("date"), resultMap.get("date"));
	}

	public void testEchoString() {
		String str = "This is a test.";
		assertEquals("This is a test.", echoService.echoString(str));
	}

	public void testEchoXML() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		String xmlStr = "<root testAttribute=\"test value\">this is a test</root>";
		StringReader reader = new StringReader(xmlStr);
		InputSource source = new InputSource(reader);
		Document xml = builder.parse(source);
		Document resultXML = echoService.echoXML(xml);
		assertEquals(xml.getFirstChild().getNodeValue(), resultXML.getFirstChild().getNodeValue());
	}

	public void testEchoMultibyteStrings() {
		java.nio.ByteBuffer buf = ByteBuffer.allocate(7);
		buf.put((byte) 0xE4);
		buf.put((byte) 0xF6);
		buf.put((byte) 0xFC);
		buf.put((byte) 0xC4);
		buf.put((byte) 0xD6);
		buf.put((byte) 0xDC);
		buf.put((byte) 0xDF);
		buf.flip();

		final Charset cs = Charset.forName("iso-8859-1");
		assertNotNull(cs);
		final String inputStr = cs.decode(buf).toString();
		log.debug("passing input string: {}", inputStr);
		final String outputStr = echoService.echoString(inputStr);
		assertEquals("unequal strings", inputStr, outputStr);
		log.debug("got output string: {}", outputStr);

		java.nio.ByteBuffer outputBuf = cs.encode(outputStr);

		for (int i = 0; i < 7; i++) {
			assertEquals("unexpected byte", buf.get(i), outputBuf.get(i));
		}
	}
}
