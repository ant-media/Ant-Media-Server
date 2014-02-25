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

package org.red5.server.stream;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.message.RTMPMessage;

/**
 * TODO: extend testcase
 * 
 * @author m.j.milicevic <marijan at info.nl>
 */
public class PlayBufferTest extends TestCase {
	
	public static Test suite() {
		return new JUnit4TestAdapter(PlayBufferTest.class);
	}

	PlayBuffer playBuffer;

	private RTMPMessage rtmpMessage;

	private void dequeue() throws Exception {
		setUp();
	}

	/**
	 * enqueue with messages
	 */
	private void enqueue() {
		boolean success = playBuffer.putMessage(rtmpMessage);
		assertTrue("message successfully put into play buffer", success);
	}

	/** {@inheritDoc} */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		VideoData message = new VideoData(IoBuffer.allocate(100));
		playBuffer = new PlayBuffer(1000);
		rtmpMessage = RTMPMessage.build(message);
	}

	public void testClear() {
		enqueue();
		playBuffer.clear();
		assertTrue(playBuffer.getMessageCount() == 0);
	}

	public void testPeekMessage() throws Exception {
		enqueue();
		assertTrue(playBuffer.peekMessage().equals(rtmpMessage));
		dequeue();
	}

	public void testPlayBuffer() {
		assertTrue("player buffer should be initialized", playBuffer != null);
	}

	public void testPutMessage() throws Exception {
		enqueue();
		RTMPMessage peek_message = playBuffer.peekMessage();
		assertNotNull("message shouldn't be null", peek_message);
		assertTrue(peek_message.equals(rtmpMessage));
		dequeue();
	}

	public void testTakeMessage() throws Exception {
		enqueue();
		assertTrue(playBuffer.takeMessage().equals(rtmpMessage));
		dequeue();
	}

}