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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * The Echo service is used to test all of the different datatypes 
 * and to make sure that they are being returned properly.
 *
 * @author The Red5 Project
 * @author Chris Allen (mrchrisallen@gmail.com)
 */
public class EchoService {

	private Logger log = LoggerFactory.getLogger(EchoService.class);

	public void startUp() {
		log.info("The Echo Service has started...");
	}

	public boolean echoBoolean(boolean bool) {
		log.info("echoBoolean: {}", bool);
		return bool;
	}

	public double echoNumber(double number) {
		log.info("echoNumber: {}", number);
		return number;
	}

	public String echoString(String string) {
		log.info("echoString: {}", string);
		return string;
	}

	public Date echoDate(Date date) {
		log.info("echoDate: {}", date);
		return date;
	}

	public Object echoObject(Object obj) {
		log.info("echoObject: {}", obj);
		return obj;
	}

	public Object[] echoArray(Object[] array) {
		log.info("echoArray: {}", array);
		return array;
	}

	@SuppressWarnings({ "rawtypes" })
	public List echoList(List list) {
		log.info("echoList: {}", list);
		return list;
	}

	public Document echoXML(Document xml) {
		log.info("echoXML: {}", xml);
		return xml;
	}

	public Object[] echoMultiParam(Map<?, ?> team, List<?> words, String str) {
		Object[] result = new Object[3];
		result[0] = team;
		result[1] = words;
		result[2] = str;
		log.info("echoMultiParam: {}, {}, {}", new Object[] { team, words, str });
		return result;
	}

	public Object echoAny(Object any) {
		log.info("echoAny: " + any);
		return any;
	}

	/**
	 * Test serialization of arbitrary objects.
	 * 
	 * @param any object to echo
	 * @return list containing distinct objects
	 */
	public List<Object> returnDistinctObjects(Object any) {
		List<Object> result = new ArrayList<Object>();
		for (int i = 0; i < 4; i++) {
			result.add(new SampleObject());
		}
		return result;
	}

	/**
	 * Test references.
	 * 
	 * @param any object to echo
	 * @return list containing same objects
	 */
	public List<Object> returnSameObjects(Object any) {
		List<Object> result = new ArrayList<Object>();
		SampleObject object = new SampleObject();
		for (int i = 0; i < 4; i++) {
			result.add(object);
		}
		return result;
	}

	/**
	 * Test returning of internal objects.
	 * 
	 * @param any object to echo
	 * @return the current connection
	 */
	public IConnection returnConnection(Object any) {
		return Red5.getConnectionLocal();
	}

	/**
	 * Sample object that contains attributes with all access possibilities.
	 * This will test the serializer of arbitrary objects. 
	 */
	public class SampleObject {

		public String value1 = "one";

		public int value2 = 2;

		protected int value4 = 4;
	}

}
