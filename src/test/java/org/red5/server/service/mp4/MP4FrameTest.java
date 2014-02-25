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

package org.red5.server.service.mp4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;
import org.red5.io.mp4.MP4Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MP4FrameTest extends TestCase {

	private static Logger log = LoggerFactory.getLogger(MP4FrameTest.class);

	@Test
	public void testSort() {
		log.debug("Test sort");
		
		List<MP4Frame> frames = new ArrayList<MP4Frame>(6);
		
		//create some frames
		MP4Frame frame1 = new MP4Frame();
		frame1.setTime(1);
		frame1.setOffset(1);
		frames.add(frame1);
		
		MP4Frame frame2 = new MP4Frame();
		frame2.setTime(6);
		frame2.setOffset(6);
		frames.add(frame2);
		
		MP4Frame frame3 = new MP4Frame();
		frame3.setTime(660);
		frame3.setOffset(660);
		frames.add(frame3);
		
		MP4Frame frame4 = new MP4Frame();
		frame4.setTime(3);
		frame4.setOffset(3);
		frames.add(frame4);
		
		MP4Frame frame5 = new MP4Frame();
		frame5.setTime(400);
		frame5.setOffset(400);		
		frames.add(frame5);

		MP4Frame frame6 = new MP4Frame();
		frame6.setTime(1000);
		frame6.setOffset(1010);
		frames.add(frame6);
		
		MP4Frame frame7 = new MP4Frame();
		frame7.setTime(1000);
		frame7.setOffset(1000);
		frames.add(frame7);		

		MP4Frame frame8 = new MP4Frame();
		frame8.setTime(1000);
		frame8.setOffset(900);
		frames.add(frame8);			
		
		System.out.printf("Frame 1 - time: %s (should be 660)\n", frames.get(2).getTime());

		Collections.sort(frames);

		System.out.println("After sorting");
		
		int f = 1;
		for (MP4Frame frame : frames) {
			System.out.printf("Frame %s - time: %s offset: %s\n", f++, frame.getTime(), frame.getOffset());
		}
		
	}

	
	
}
