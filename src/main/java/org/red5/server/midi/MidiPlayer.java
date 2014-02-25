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

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;

import org.red5.logging.Red5LoggerFactory;
import org.slf4j.Logger;

/** Plays a midi file provided on command line */
public class MidiPlayer {

	protected static Logger log = Red5LoggerFactory.getLogger(MidiPlayer.class);
	
	public static void main(String args[]) {
		// Argument check
		if (args.length == 0) {
			helpAndExit();
		}
		String file = args[0];
		if (!file.endsWith(".mid")) {
			helpAndExit();
		}
		File midiFile = new File(file);
		if (!midiFile.exists() || midiFile.isDirectory() || !midiFile.canRead()) {
			helpAndExit();
		}
	}

	public MidiPlayer(File midiFile) {

		// Play once
		try {
			Sequencer sequencer = MidiSystem.getSequencer();
			sequencer.setSequence(MidiSystem.getSequence(midiFile));
			sequencer.open();
			sequencer.start();
			/*
			 while(true) {
			 if(sequencer.isRunning()) {
			 try {
			 Thread.sleep(1000); // Check every second
			 } catch(InterruptedException ignore) {
			 break;
			 }
			 } else {
			 break;
			 }
			 }
			 // Close the MidiDevice & free resources
			 sequencer.stop();
			 sequencer.close();
			 */
		} catch (MidiUnavailableException mue) {
			log.error("Midi device unavailable!", mue);
		} catch (InvalidMidiDataException imde) {
			log.error("Invalid Midi data!", imde);
		} catch (IOException ioe) {
			log.error("I/O Error!", ioe);
		}

	}

	/** Provides help message and exits the program */
	private static void helpAndExit() {
		log.error("Usage: java MidiPlayer midifile.mid");
		//System.exit(1);
	}
}
