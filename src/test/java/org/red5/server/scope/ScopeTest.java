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

package org.red5.server.scope;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.NoSuchElementException;
import java.util.Set;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.red5.server.ClientRegistry;
import org.red5.server.Context;
import org.red5.server.Server;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.TestConnection;
import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.stream.IProviderService;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * This is for testing Scope issues. First created to address:
 * http://jira.red5.org/browse/APPSERVER-278
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@ContextConfiguration(locations = { "ScopeTest.xml" })
public class ScopeTest extends AbstractJUnit4SpringContextTests {

	protected static Logger log = LoggerFactory.getLogger(ScopeTest.class);

	private static TestRunnable[] trs;

	@SuppressWarnings("unused")
	private QuartzSchedulingService service;

	private ClientRegistry registry;
	
	private Context context;

	private WebScope appScope;

	private String host = "localhost";

	private String appPath = "junit";

	private String roomPath = "/junit/room1";

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "target/test-classes");
		System.setProperty("red5.config_root", "src/main/server/conf");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}

	@Before
	public void setUp() throws Exception {
		service = (QuartzSchedulingService) applicationContext.getBean("schedulingService");
		registry = (ClientRegistry) applicationContext.getBean("global.clientRegistry");
		context = (Context) applicationContext.getBean("web.context");
		Server server = (Server) applicationContext.getBean("red5.server");
		server.addListener(new IScopeListener() {
			public void notifyScopeCreated(IScope scope) {
				log.debug("Scope created: {}", scope);
			}

			public void notifyScopeRemoved(IScope scope) {
				log.debug("Scope removed: {}", scope);
			}
		});
		appScope = (WebScope) applicationContext.getBean("web.scope");
		log.debug("Application / web scope: {}", appScope);
		assertTrue(appScope.getDepth() == 1);
	}

	@After
	public void tearDown() throws Exception {
		appScope.getScope("room1").removeChildren();
		appScope = null;
	}

	private void setupScopes() {
		log.debug("-------------------------------------------------------------setupScopes");
		//Room 1
		// /default/junit/room1
		assertFalse(appScope.createChildScope("room1")); // room1 is defined in context xml file, this should fail
		IScope room1 = appScope.getScope("room1");
		log.debug("Room 1: {}", room1);
		assertTrue(room1.getDepth() == 2);
		IContext rmCtx1 = room1.getContext();
		log.debug("Context 1: {}", rmCtx1);
		//Room 2
		// /default/junit/room1/room2
		if (room1.getScope("room2") == null) {
			assertTrue(room1.createChildScope("room2"));
		}
		IScope room2 = room1.getScope("room2");
		log.debug("Room 2: {}", room2);
		assertNotNull(room2);
		assertTrue(room2.getDepth() == 3);
		IContext rmCtx2 = room2.getContext();
		log.debug("Context 2: {}", rmCtx2);
		//Room 3
		// /default/junit/room1/room2/room3
		if (room2.getScope("room3") == null) {
			assertTrue(room2.createChildScope("room3"));
		}
		IScope room3 = room2.getScope("room3");
		log.debug("Room 3: {}", room3);
		assertNotNull(room3);
		assertTrue(room3.getDepth() == 4);
		IContext rmCtx3 = room3.getContext();
		log.debug("Context 3: {}", rmCtx3);
		//Room 4 attaches at Room 1 (per bug example)
		// /default/junit/room1/room4
		if (room1.getScope("room4") == null) {
			assertTrue(room1.createChildScope("room4"));
		}
		IScope room4 = room1.getScope("room4");
		log.debug("Room 4: {}", room4);
		assertNotNull(room4);
		assertTrue(room4.getDepth() == 3);
		IContext rmCtx4 = room4.getContext();
		log.debug("Context 4: {}", rmCtx4);
		//Room 5
		// /default/junit/room1/room4/room5
		if (room4.getScope("room5") == null) {
			assertTrue(room4.createChildScope("room5"));
		}
		IScope room5 = room4.getScope("room5");
		log.debug("Room 5: {}", room5);
		assertNotNull(room5);
		assertTrue(room5.getDepth() == 4);
		IContext rmCtx5 = room5.getContext();
		log.debug("Context 5: {}", rmCtx5);
	}

	@Test
	public void client() {
		log.debug("-----------------------------------------------------------------client");
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		assertTrue("client should not be null", client != null);
	}

	@Test
	public void connectionHandler() {
		log.debug("-----------------------------------------------------------------connectionHandler");
		TestConnection conn = new TestConnection(host, "/", null);
		// add the connection to thread local
		Red5.setConnectionLocal(conn);
		// resolve root
		IScope scope = context.resolveScope("/");
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		assertNotNull(client);
		conn.initialize(client);
		if (conn.connect(scope)) {
			assertTrue("should have a scope", conn.getScope() != null);
			conn.close();
			assertTrue("should not be connected", !conn.isConnected());
		} else {
			assertTrue("didnt connect", false);
		}
		Red5.setConnectionLocal(null);
	}

	@Test
	public void context() {
		log.debug("-----------------------------------------------------------------context");
		IScope testRoom = context.resolveScope(roomPath);
		IContext context = testRoom.getContext();
		assertTrue("context should not be null", context != null);
		log.debug("{}", testRoom.getContext().getResource(""));
		log.debug("{}", testRoom.getResource(""));
		log.debug("{}", testRoom.getParent().getResource(""));
	}

	/**
	 * TODO figure out why this fails to connect, doesnt make any sense.
	 * 
	 * @throws InterruptedException
	 */
	public void handler() throws InterruptedException {
		log.debug("-----------------------------------------------------------------handler");
		IScope testApp = context.resolveScope(appPath);
		assertTrue("should have a handler", testApp.hasHandler());
		log.debug("App: {}", testApp);

		TestConnection conn = new TestConnection(host, "/junit", null);
		Red5.setConnectionLocal(conn);

		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		assertTrue("client should not be null", client != null);
		log.debug("{}", client);
		String key = "key";
		String value = "value";
		client.setAttribute(key, value);
		assertTrue("attributes not working", client.getAttribute(key) == value);

		conn.initialize(client);

		if (conn.connect(testApp)) {
			// give connect a moment to settle
			Thread.sleep(100L);
			assertTrue("Should have a scope", conn.getScope() != null);
			assertTrue("app should have 1 client", ((Scope) testApp).getActiveClients() == 1);
			assertTrue("host should have 1 client", testApp.getParent().getClients().size() == 1);
			conn.close();
			assertTrue("Should not be connected", !conn.isConnected());
			assertTrue("app should have 0 client", ((Scope) testApp).getActiveClients() == 0);
			assertTrue("host should have 0 client", testApp.getParent().getClients().size() == 0);
		} else {
			fail("Didnt connect");
		}
		//client.disconnect();
		Red5.setConnectionLocal(null);
	}

	@Test
	public void scopeResolver() {
		log.debug("-----------------------------------------------------------------scopeResolver");
		// Global
		IScope global = context.getGlobalScope();
		assertNotNull("global scope should be set", global);
		assertTrue("should be global", ScopeUtils.isGlobal(global));
		log.debug("{}", global);
		// Test App
		IScope testApp = context.resolveScope(appPath);
		assertTrue("testApp scope not null", testApp != null);
		log.debug("{}", testApp);
		// Test Room
		IScope testRoom = context.resolveScope(roomPath);
		log.debug("{}", testRoom);
		// Test App Not Found
		try {
			IScope notFoundApp = context.resolveScope(appPath + "notfound");
			log.debug("{}", notFoundApp);
			assertTrue("should have thrown an exception", false);
		} catch (RuntimeException e) {
		}
	}

	@Test
	public void testScopeConnection() {
		log.debug("-----------------------------------------------------------------testScopeConnection");
		setupScopes();
		IScope room5 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4/room5");
		log.debug("Room 5 scope: {}", room5);
		// test section for issue #259
		// a little pre-setup is needed first
		IClientRegistry reg = context.getClientRegistry();
		IClient client = reg.newClient(null);
		TestConnection conn = new TestConnection(host, appPath, client.getId());
		conn.initialize(client);
		Red5.setConnectionLocal(conn);
		assertTrue(conn.connect(room5));
		// their code
		IScope scope = Red5.getConnectionLocal().getScope();
		for (IConnection tempConn : scope.getClientConnections()) {
			if (tempConn instanceof IServiceCapableConnection) {
				try {
					@SuppressWarnings("unused")
					IServiceCapableConnection sc = (IServiceCapableConnection) tempConn;
					//sc.invoke(methodName, objArrays);
				} catch (NoSuchElementException e) {
					log.warn("Previous scope connection is unavailable", e);
				}
			}
		}
	}

	@Test
	public void testGetScopeNames() throws Exception {
		log.debug("-----------------------------------------------------------------testGetScopeNames");
		setupScopes();
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		log.debug("Room 1 scope: {}", room1);
		assertTrue(room1.getDepth() == 2);
		Set<String> names = room1.getScopeNames();
		log.debug("Scope: {}", names);
		IScope room5 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4/room5");
		if (room5 != null) {
			log.debug("Room 5 scope: {}", room5);
			assertTrue(room5.getDepth() == 4);
			names = room1.getScopeNames();
			log.debug("Scope: {}", names);
		} else {
			log.warn("Room 5 scope was not found");
		}
	}

	@Test
	public void testRemoveScope() throws Exception {
		log.debug("-----------------------------------------------------------------testRemoveScope");
		setupScopes();
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		IScope room4 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4");
		log.debug("Room 4 scope: {}", room4);
		assertTrue(room4.getDepth() == 3);
		log.debug("Room 4 child scope exists: {}", room1.hasChildScope("room4"));
		room1.removeChildScope(room4);
		log.debug("Room 4 child scope exists: {}", room1.hasChildScope("room4"));
	}

	/**
	 * Test for Issue 73
	 * http://code.google.com/p/red5/issues/detail?id=73
	 * 
	 */
	@Test
	public void testGetContextPath() throws Exception {
		log.debug("-----------------------------------------------------------------testGetContextPath");
		log.debug("Context path: {}", appScope.getContextPath());
	}

	/**
	 * Test created to address multi-thread adding and removal of scopes.
	 * @throws Throwable 
	 */
	@Test
	public void testScopeMultiThreadHandling() throws Throwable {
		log.debug("-----------------------------------------------------------------testScopeMultiThreadHandling");
		setupScopes();
		boolean persistent = false;
		// create app
		MultiThreadedApplicationAdapter app = new MultiThreadedApplicationAdapter();
		// change the handler
		appScope.setHandler(app);
		// start
		app.start(appScope);
		// get our room
		IScope room = ScopeUtils.resolveScope(appScope, "/junit/mt");
		if (room == null) {
			assertTrue(app.createChildScope("mt"));
			room = ScopeUtils.resolveScope(appScope, "/junit/mt");
		}
		// create the SO
		assertTrue(app.createSharedObject(room, "mtSO", persistent));
		ISharedObject so = app.getSharedObject(room, "mtSO");
		// acquire only works with non-persistent so's
		if (!persistent) {
			so.acquire();
			assertTrue(so.isAcquired());
		}
		// test runnables represent clients
		trs = new TestRunnable[21];
		for (int t = 0; t < trs.length; t++) {
			trs[t] = new ScopeClientWorker(t, app, room);
		}
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		// fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime: " + (System.nanoTime() - start) + "ns");
		// go to sleep
		try {
			Thread.sleep(3000);
		} catch (Exception e) {
		}
		for (TestRunnable r : trs) {
			ScopeClientWorker cl = (ScopeClientWorker) r;
			log.debug("Worker: {} shared object: {}", cl.getId(), cl.getSharedObject().getAttributes());
		}
		if (!persistent) {
			assertTrue(so.isAcquired());
			so.release();
			assertFalse(so.isAcquired());
		}
		app.stop(appScope);

		//		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room1");
		//		IScope room4 = ScopeUtils.resolveScope(appScope, "/junit/room1/room4");
		//		log.debug("Room 4 scope: {}", room4);
		//		assertTrue(room4.getDepth() == 3);
		//		log.debug("Room 4 child scope exists: {}", room1.hasChildScope("room4"));
		//		room1.removeChildScope(room4);
		//		log.debug("Room 4 child scope exists: {}", room1.hasChildScope("room4"));
	}

	/**
	 * Test created to address missing handler when a subscope child is removed. The handler seems to get
	 * removed from other children in the set.
	 * 
	 * @throws Throwable 
	 */
	@Test
	public void testScopeMissingHandler() throws Throwable {
		log.debug("-----------------------------------------------------------------testScopeMissingHandler");
		// create app
		MultiThreadedApplicationAdapter app = new MultiThreadedApplicationAdapter();
		// change the handler
		appScope.setHandler(app);
		// start
		app.start(appScope);
		// create our additional scopes
		assertTrue(appScope.hasHandler());
		IScope top = ScopeUtils.resolveScope(appScope, "/junit");
		assertTrue(top.hasHandler());
		IScope room = ScopeUtils.resolveScope(appScope, "/junit/room13");
		if (room == null) {
			assertTrue(top.createChildScope("room13"));
			room = ScopeUtils.resolveScope(appScope, "/junit/room13");
			assertNotNull(room);
		}
		assertTrue(room.hasHandler());
		// get rooms
		IScope room1 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomA");
		if (room1 == null) {
			assertTrue(room.createChildScope("subroomA"));
			room1 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomA");
			assertNotNull(room1);
		}
		Thread.sleep(10);
		IScope room2 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomB");
		if (room2 == null) {
			assertTrue(room.createChildScope("subroomB"));
			room2 = ScopeUtils.resolveScope(appScope, "/junit/room13/subroomB");
			assertNotNull(room2);
		}
		// let it settle for a moment
		Thread.sleep(50L);
		// create the SOs
		String soName = "messager";
		if (!app.hasSharedObject(room1, soName)) {
			app.createSharedObject(room1, soName, false);
		}
		assertNotNull(app.getSharedObject(room1, soName, false));
		if (!app.hasSharedObject(room2, soName)) {
			app.createSharedObject(room2, soName, false);
		}
		assertNotNull(app.getSharedObject(room2, soName, false));
		// test runnables represent clients
		trs = new TestRunnable[2];
		trs[0] = new ScopeClientWorkerA(0, app, room1);
		trs[1] = new ScopeClientWorkerB(1, app, room2);
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		// fires off threads
		long start = System.nanoTime();
		mttr.runTestRunnables();
		System.out.println("Runtime: " + (System.nanoTime() - start) + "ns");
		ScopeClientWorkerA soa = (ScopeClientWorkerA) trs[0];
		log.debug("Worker: {} shared object: {}", soa.getId(), soa.getSharedObject().getAttributes());
		ScopeClientWorkerB sob = (ScopeClientWorkerB) trs[1];
		log.debug("Worker: {} shared object: {}", sob.getId(), sob.getSharedObject().getAttributes());
		Thread.sleep(300L);
		// clean up / stop
		app.stop(appScope);
	}

	// Used to ensure all the test-runnables are in "runTest" block.
	private static boolean allThreadsRunning() {
		for (TestRunnable r : trs) {
			if (!((ScopeClientWorker) r).isRunning()) {
				return false;
			}
		}
		return true;
	}

	private class ScopeClientWorker extends TestRunnable {

		private int id;

		private ISharedObject so;

		private ClientBroadcastStream stream;

		private volatile boolean running = false;

		public ScopeClientWorker(int id, MultiThreadedApplicationAdapter app, IScope room) {
			this.id = id;
			this.so = app.getSharedObject(room, "mtSO", true);
			if (id % 2 == 0) {
				TestStreamConnection conn = new TestStreamConnection("localhost", "/junit/mt", "session" + id);
				conn.setClient(registry.newClient(null));
				// create a few streams
				this.stream = new ClientBroadcastStream();
				String name = "stream" + id;
				stream.setRegisterJMX(false);
				stream.setPublishedName(name);
				stream.setScope(room);
				stream.setConnection(conn);				
				// 
				IProviderService providerService = (IProviderService) room.getContext().getBean(IProviderService.BEAN_NAME);
				// publish this server-side stream
				providerService.registerBroadcastStream(room, name, stream);
				// start it
				app.streamBroadcastStart(stream);
				if (room.getBroadcastScope(name) == null) {
					log.warn("Stream scope failed");
				}
			}
		}

		public void runTest() throws Throwable {
			log.debug("runTest#{}", id);
			running = true;
			// start any stream
			if (stream != null) {
				stream.start();
				stream.startPublishing();
			}
			do {
				Thread.sleep(33);
			} while (!allThreadsRunning());
			// set a value
			so.setAttribute("time", System.currentTimeMillis() + Integer.valueOf(RandomStringUtils.randomNumeric(3)));
			Thread.sleep(64);
			// stop any stream
			if (stream != null) {
				stream.close();
			}
			log.debug("runTest-end#{}", id);
			running = false;
		}

		public int getId() {
			return id;
		}

		public ISharedObject getSharedObject() {
			return so;
		}

		@SuppressWarnings("unused")
		public IBroadcastStream getStream() {
			return stream;
		}

		public boolean isRunning() {
			return running;
		}
	}

	private final class TestStreamConnection extends TestConnection implements IStreamCapableConnection {

		public TestStreamConnection(String host, String path, String sessionId) {
			super(host, path, sessionId);
		}

		public int reserveStreamId() {
			return 0;
		}

		public int reserveStreamId(int id) {
			return 0;
		}

		public void unreserveStreamId(int streamId) {
		}

		public void deleteStreamById(int streamId) {
		}

		public IClientStream getStreamById(int streamId) {
			return null;
		}

		public ISingleItemSubscriberStream newSingleItemSubscriberStream(int streamId) {
			return null;
		}

		public IPlaylistSubscriberStream newPlaylistSubscriberStream(int streamId) {
			return null;
		}

		public IClientBroadcastStream newBroadcastStream(int streamId) {
			return null;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "TestStreamConnection [path=" + path + ", sessionId=" + sessionId + ", client=" + client + ", scope=" + scope + ", closed=" + isClosed() + "]";
		}

	}

	private class ScopeClientWorkerA extends TestRunnable {

		private int id;

		private IScope room;

		private IConnection conn;

		private ISharedObject so;

		public ScopeClientWorkerA(int id, MultiThreadedApplicationAdapter app, IScope room) {
			this.id = id;
			this.room = room;
			this.so = app.getSharedObject(room, "messager", false);
			System.out.println("Connect path: " + room.getContextPath());
			conn = new TestStreamConnection("localhost", room.getContextPath(), "session" + id);
			conn.setClient(registry.newClient(null));
		}

		@SuppressWarnings("deprecation")
		public void runTest() throws Throwable {
			log.debug("runTest#{}", id);
			Red5.setConnectionLocal(conn);
			assertTrue(conn.connect(room));
			Thread.sleep(50);
			// set a value
			so.setAttribute("client-id", id);
			so.setAttribute("time", System.currentTimeMillis() + Integer.valueOf(RandomStringUtils.randomNumeric(3)));
			Thread.sleep(50);
			conn.close();
			Red5.setConnectionLocal(null);
			log.debug("runTest-end#{}", id);
		}

		public int getId() {
			return id;
		}

		public ISharedObject getSharedObject() {
			return so;
		}

	}

	private class ScopeClientWorkerB extends TestRunnable {

		private int id;

		private IScope room;

		private IConnection conn;

		private ISharedObject so;

		public ScopeClientWorkerB(int id, MultiThreadedApplicationAdapter app, IScope room) {
			this.id = id;
			this.room = room;
			this.so = app.getSharedObject(room, "messager", false);
			System.out.println("Connect path: " + room.getContextPath());
			conn = new TestStreamConnection("localhost", room.getContextPath(), "session" + id);
			conn.setClient(registry.newClient(null));
		}

		@SuppressWarnings("deprecation")
		public void runTest() throws Throwable {
			log.debug("runTest#{}", id);
			Red5.setConnectionLocal(conn);
			assertTrue(conn.connect(room));
			Thread.sleep(50);
			// set a value
			so.setAttribute("client-id", id);
			so.setAttribute("time", System.currentTimeMillis() + Integer.valueOf(RandomStringUtils.randomNumeric(3)));
			Thread.sleep(100);
			so.sendMessage("sendMessage", null);
			Thread.sleep(50);
			conn.close();
			Red5.setConnectionLocal(null);
			log.debug("runTest-end#{}", id);
		}

		public int getId() {
			return id;
		}

		public ISharedObject getSharedObject() {
			return so;
		}

	}

}
