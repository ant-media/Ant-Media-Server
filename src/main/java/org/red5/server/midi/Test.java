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

package org.red5.server.midi;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiDevice.Info;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Test {

	// Initialize Logging
	protected static Logger log = LoggerFactory.getLogger(Test.class);

	public static void main(String[] args) throws Exception {
		@SuppressWarnings("unused") 
		Test t = new Test();
	}

	public static MidiDevice getMidiDevice(String name) {

		MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

		for (Info element : info) {
			if (element.getName().equals(name)) {
				try {
					return MidiSystem.getMidiDevice(element);
				} catch (MidiUnavailableException e) {
					log.error("{}", e);
				}
			}
		}

		return null;

	}

	/** Constructs a new Test. 
	 * @throws Exception if it feels like it.
	 */
    public Test() throws Exception {

		String MIDI_NAME = "USB Uno MIDI  In";
		MidiDevice dev = getMidiDevice(MIDI_NAME);
		dev.open();
		MyReceiver rec = new MyReceiver();
		dev.getTransmitter().setReceiver(rec);
		Thread.sleep(20000);
		dev.close();

	}

	public static class MyReceiver extends Object implements Receiver {

		/** {@inheritDoc} */
        public void send(MidiMessage midi, long time) {
			byte[] msg = midi.getMessage();
			int len = midi.getLength();
			if (len <= 1) {
				return;
			}
			StringBuilder out = new StringBuilder("Status: ");
			out.append(msg[0]);
			out.append(" Data: [");
			for (int i = 1; i < len; i++) {
				out.append(msg[i]);
				if (i == len - 1) {
					out.append("");
				} else {
					out.append(',');
				}
			}
			out.append(']');
			
			log.debug(out.toString());
		}

		/** {@inheritDoc} */
        public void close() {
			log.debug("Closing");
		}
	}

}