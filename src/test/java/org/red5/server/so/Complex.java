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

package org.red5.server.so;

import java.util.HashMap;
import java.util.Map;

import org.red5.io.amf3.IDataInput;
import org.red5.io.amf3.IDataOutput;
import org.red5.io.amf3.IExternalizable;

@SuppressWarnings({ "rawtypes" })
public class Complex implements IExternalizable {

	private long x = System.currentTimeMillis();

	private String s = "Complex object";

	private Map map = new HashMap();

	public Complex() {
	}
	
	public long getX() {
		return x;
	}

	public void setX(long x) {
		this.x = x;
	}

	public String getS() {
		return s;
	}

	public void setS(String s) {
		this.s = s;
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

	@Override
	public String toString() {
		return "Complex [x=" + x + ", s=" + s + ", map=" + map + "]";
	}

	@Override
	public void readExternal(IDataInput input) {
		x = input.readUnsignedInt();
		s = input.readUTF();
		map = (HashMap) input.readObject();
	}

	@Override
	public void writeExternal(IDataOutput output) {
		output.writeUnsignedInt(x);
		output.writeUTF(s);
		output.writeObject(map);
	}

}
