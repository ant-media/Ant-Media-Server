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

package org.red5.server.net.rtmp.event;

import java.nio.ByteBuffer;
import org.apache.mina.core.buffer.IoBuffer;

/**
 * The utility class provides conversion methods to ease the use of
 * byte arrays, Mina IoBuffers, and NIO ByteBuffers.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class SerializeUtils {

	public static byte[] ByteBufferToByteArray(IoBuffer buf) {
		byte[] byteBuf = new byte[buf.limit()];
		int pos = buf.position();
		buf.rewind();
		buf.get(byteBuf);
		buf.position(pos);
		return byteBuf;
	}
	
	public static byte[] NioByteBufferToByteArray(ByteBuffer buf) {
		byte[] byteBuf = new byte[buf.limit()];
		int pos = buf.position();
		buf.position(0);
		buf.get(byteBuf);
		buf.position(pos);
		return byteBuf;
	}	
	
	public static void ByteArrayToByteBuffer(byte[] byteBuf, IoBuffer buf) {
		buf.put(byteBuf);
		buf.flip();
	}
	
	public static void ByteArrayToNioByteBuffer(byte[] byteBuf, ByteBuffer buf) {
		buf.put(byteBuf);
		buf.flip();
	}
	
}