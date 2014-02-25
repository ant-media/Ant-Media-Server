package org.red5.server.midi;

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

import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.MidiDevice.Info;

import org.red5.server.api.so.ISharedObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SharedMidiObject {

	private static final Logger log = LoggerFactory.getLogger(SharedMidiObject.class);

	protected String deviceName;

	protected ISharedObject so;

	protected MidiDevice dev;

	public SharedMidiObject(String deviceName, ISharedObject so) {
		this.deviceName = deviceName;
		this.so = so;
	}

	public boolean connect() {
		try {
			dev = getMidiDevice(deviceName);
			if (dev == null) {
				log.error("Midi device not found: " + deviceName);
				return false;
			}
			if (!dev.isOpen()) {
				dev.open();
			}
			dev.getTransmitter().setReceiver(new MidiReceiver());
			return true;
		} catch (MidiUnavailableException e) {
			log.error("Error connecting to midi device", e);
		}
		return false;
	}

	public void close() {
		if (dev != null && dev.isOpen()) {
			dev.close();
		}
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

	public class MidiReceiver extends Object implements Receiver {

		/** {@inheritDoc} */
        public void send(MidiMessage midi, long time) {

			byte[] msg = midi.getMessage();
			int len = midi.getLength();
			if (len <= 1) {
				return;
			}

			List<Object> list = new ArrayList<Object>(3);
			list.add(time);
			list.add(len);
			list.add(msg);
			so.beginUpdate();
			so.sendMessage("midi", list);
			so.endUpdate();

			StringBuilder out = new StringBuilder("Midi >> Status: ");
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
			log.debug("Midi device closed");
		}

	}

}
