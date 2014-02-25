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

import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;

/**
 * The Echo service is used to test all of the different datatypes 
 * and to make sure that they are being returned properly.
 *
 * @author The Red5 Project
 * @author Chris Allen (mrchrisallen@gmail.com)
 */
public interface IEchoService {

	/**
	 * Used to verify that Spring has loaded the bean.
	 *
	 */
	public abstract void startUp();

	/**
	 * Verifies that a boolean that is passed in returns correctly.
	 * 
	 * @param bool object to echo
	 * @return input value
	 */
	public abstract boolean echoBoolean(boolean bool);

	/**
	 * Verifies that a Number that is passed in returns correctly.
	 * 
	 * Flash Number = double
	 * 
	 * @param num object to echo
	 * @return input value
	 */
	public abstract double echoNumber(double num);

	/**
	 * Verifies that a String that is passed in returns correctly.
	 * 
	 * @param string object to echo
	 * @return input value
	 */
	public abstract String echoString(String string);

	/**
	 * Verifies that a Date that is passed in returns correctly.
	 * 
	 * @param date object to echo
	 * @return input value
	 */
	public abstract Date echoDate(Date date);

	/**
	 * Verifies that a Flash Object that is passed in returns correctly.
	 * Flash Object = java.utils.Map. Let Apache bean utils do the magic
	 * conversions.
	 * 
	 * @param obj object to echo
	 * @return input value
	 */
	public abstract Object echoObject(Object obj);

	/**
	 * Verifies that a Flash simple Array that is passed in returns correctly.
	 * Flash simple Array = Object[]
	 * 
	 * @param array object to echo
	 * @return input value
	 */
	public abstract Object[] echoArray(Object[] array);

	/**
	 * Verifies that a Flash multi-dimensional Array that is passed in returns itself.
	 * Flash multi-dimensional Array = java.utils.List
	 * @param <T> type of list
	 * 
	 * @param list object to echo
	 * @return input value
	 */
	public abstract <T> List<T> echoList(List<? extends T> list);

	/**
	 * Verifies that Flash XML that is passed in returns itself.
	 * Flash XML = org.w3c.dom.Document
	 * 
	 * @param xml object to echo
	 * @return input value
	 */
	public abstract Document echoXML(Document xml);

}
