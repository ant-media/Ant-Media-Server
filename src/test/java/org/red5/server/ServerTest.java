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

package org.red5.server;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.scope.GlobalScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test exercises the Server class. 
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ServerTest {

	protected static Logger log = LoggerFactory.getLogger(ServerTest.class);
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "target/classes");
		System.setProperty("red5.config_root", "src/main/server/conf");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGlobalLookupsForVirtualHostsWithSameIP() {
		final Server server = new Server();

		IGlobalScope g0 = new DummyGlobalScope("default");
		IGlobalScope g1 = new DummyGlobalScope("default.vhost1");
		IGlobalScope g2 = new DummyGlobalScope("default.vhost2");
		
		//local server
		server.registerGlobal(g0);
		
		server.addMapping("localhost", "", "default");
		server.addMapping("localhost", "oflaDemo", "default");

		//virtual host 1
		
		server.registerGlobal(g1);

		server.addMapping("", "", "default.vhost1");
		server.addMapping("localhost", "oflaDemo", "default.vhost1");
		server.addMapping("localhost:8088", "", "default.vhost1");
		server.addMapping("127.0.0.1", "oflaDemo", "default.vhost1");
		//
		server.addMapping("vhost1.localdomain", "", "default.vhost1");
		server.addMapping("vhost1.localdomain", "oflaDemo", "default.vhost1");
		
		//virtual host 2
		
		server.registerGlobal(g2);

		server.addMapping("", "", "default.vhost2");
		server.addMapping("localhost", "oflaDemo", "default.vhost2");
		server.addMapping("localhost:8088", "", "default.vhost2");
		server.addMapping("127.0.0.1", "oflaDemo", "default.vhost2");
		//
		server.addMapping("vhost2.localdomain", "", "default.vhost2");
		server.addMapping("vhost2.localdomain", "oflaDemo", "default.vhost2");

		//assertions
		
		Assert.assertTrue(server.lookupGlobal("vhost2.localdomain", "blah") != null);
		Assert.assertTrue(server.lookupGlobal("vhost2.localdomain", "oflaDemo") != null);
		
		IGlobalScope tmp = server.lookupGlobal("vhost2.localdomain", "oflaDemo");
		log.debug("Global 2: {}", tmp);
		Assert.assertTrue(tmp.getName().equals("default.vhost2"));
		
		tmp = server.lookupGlobal("vhost1.localdomain", "oflaDemo");
		log.debug("Global 1: {}", tmp);
		Assert.assertTrue(tmp.getName().equals("default.vhost1"));

	}
	
	@Test
	public void testMultiThreaded() throws Throwable {

		int threads = 10;
		
		final Server server = new Server();

		IGlobalScope g0 = new DummyGlobalScope("default");
		
		//local server
		server.registerGlobal(g0);
		
		server.addMapping("localhost", "", "default");
		server.addMapping("localhost", "oflaDemo", "default");
		
		TestRunnable[] trs = new TestRunnable[threads];
		for (int t = 0; t < threads; t++) {
			trs[t] = new HostAddWorker(server, t);
		}

		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		//kickstarts the MTTR & fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		log.info("Runtime: {} ns", (System.nanoTime() - start));

		for (TestRunnable r : trs) {
			String name = ((HostAddWorker) r).getName();
			Assert.assertTrue(server.lookupGlobal(name + ".localdomain", "nonexistentscope") != null);
			IGlobalScope tmp = server.lookupGlobal(name + ".localdomain", "oflaDemo");
			Assert.assertTrue(tmp.getName().equals("default." + name));
		}		
	}
	
	private class HostAddWorker extends TestRunnable {
		
		Server server;
		String name;
		
		public HostAddWorker(Server server, int index) {
			this.server = server;
			this.name = "vhost" + index;
		}

		public void runTest() throws Throwable {
			IGlobalScope gs = new DummyGlobalScope("default." + name);

			server.registerGlobal(gs);
			
			for (int i = 0; i < 6; i++) {
				server.addMapping("", "", "default." + name);
				server.addMapping("localhost", "oflaDemo", "default." + name);
				server.addMapping("localhost:8088", "", "default." + name);
				server.addMapping("127.0.0.1", "oflaDemo", "default." + name);
				//
				server.addMapping(name + ".localdomain", "", "default." + name);
				server.addMapping(name + ".localdomain", "oflaDemo", "default." + name);				
			}
		}

		public String getName() {
			return name;
		}
	}
	
	
	private final static class DummyGlobalScope extends GlobalScope {
		public DummyGlobalScope(String name) {
			super();
			this.name = name;
		}
	}
	
}
