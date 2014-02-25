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

package org.red5.server.api.stream.support;

import static org.junit.Assert.fail;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Test;
import org.red5.server.stream.NoSyncServerStream;
import org.red5.server.stream.ServerStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamUtilsTest {

	protected static Logger log = LoggerFactory.getLogger(StreamUtilsTest.class);

	/* ------- workers ------- */
	private class SynchedWorker extends TestRunnable {
		private ServerStream stream;

		public SynchedWorker(ServerStream stream) {
			this.stream = stream;
		}

		public void runTest() throws Throwable {
			for (int i = 0; i < callsPerThread; i++) {
				// set item
				stream.addItem(SimplePlayItem.build("stream" + i));
				// get item
				stream.getItem(1);
				// next item
				stream.nextItem();
				// stop
				stream.stop();
			}
		}

	}

	private class UnSynchedWorker extends TestRunnable {
		private NoSyncServerStream stream;

		public UnSynchedWorker(NoSyncServerStream stream) {
			this.stream = stream;
		}

		public void runTest() throws Throwable {
			for (int i = 0; i < callsPerThread; i++) {
				// set item
				stream.addItem(SimplePlayItem.build("stream" + i));
				// get item
				stream.getItem(1);
				// next item
				stream.nextItem();
				// stop
				stream.stop();
			}
		}

	}

	private static int callsPerThread = 1000;

	private static int threads = 3;

	@Test
	public void testCreateServerStream() {
		System.out.println("Not yet implemented"); // TODO
	}

	@Test
	public void testForDeadlock() {
		// test synchronized
		String name = "synchronized";
		ServerStream stream = new ServerStream();
		stream.setName(name);
		stream.setPublishedName(name);

		// pass that instance to the MTTR
		TestRunnable[] synced = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			synced[t] = new SynchedWorker(stream);
		}

		MultiThreadedTestRunner syncedrunner = new MultiThreadedTestRunner(synced);

		// kickstarts the MTTR & fires off threads
		long start = System.nanoTime();
		try {
			syncedrunner.runTestRunnables();
		} catch (Throwable e) {
			log.warn("Exception {}", e);
			fail();
		}
		System.out.println("Runtime for synced runner: " + (System.nanoTime() - start) + "ns");

		// test unsynchronized
		name = "non-synchronized";
		NoSyncServerStream nstream = new NoSyncServerStream();
		nstream.setName(name);
		nstream.setPublishedName(name);

		// pass that instance to the MTTR
		TestRunnable[] unsynced = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			unsynced[t] = new UnSynchedWorker(nstream);
		}

		MultiThreadedTestRunner unsyncedrunner = new MultiThreadedTestRunner(unsynced);

		// kickstarts the MTTR & fires off threads
		start = System.nanoTime();
		try {
			unsyncedrunner.runTestRunnables();
		} catch (Throwable e) {
			log.warn("Exception {}", e);
			fail();
		}
		System.out.println("Runtime for unsynced runner: " + (System.nanoTime() - start) + "ns");

	}

	@Test
	public void testGetServerStream() {
		System.out.println("Not yet implemented"); // TODO
	}

}
