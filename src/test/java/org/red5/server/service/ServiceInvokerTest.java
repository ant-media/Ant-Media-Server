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

package org.red5.server.service;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.Test;
import org.red5.server.Context;
import org.red5.server.DummyClient;
import org.red5.server.api.IClient;
import org.red5.server.api.IClientRegistry;
import org.red5.server.api.IConnection;
import org.red5.server.api.IContext;
import org.red5.server.api.Red5;
import org.red5.server.api.TestConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.scope.Scope;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
@ContextConfiguration(locations = { "testcontext.xml" })
public class ServiceInvokerTest extends AbstractJUnit4SpringContextTests {

	// TODO: Add more tests!
	// we dont have to test all the echo methods, more test the call object works as expected
	// the correct types of status are returned (method not found) etc.
	// Also, we need to add tests which show the way the parameter conversion works.
	// So have a few methods with the same name, and try with diff params, making sure right one gets called.

	protected static Logger log = LoggerFactory.getLogger(ServiceInvokerTest.class);

	private final static List<String> workerList = new ArrayList<String>(11);

	private static AtomicInteger finishedCount = new AtomicInteger(0);

	private final static Random rnd = new Random();
	
	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "target/classes");
		System.setProperty("red5.config_root", "src/main/server/conf");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}

	@Test
	public void testAppContextLoaded() {
		assertNotNull(applicationContext);
		//assertNotNull(applicationContext.getBean("serviceInvoker"));
		assertNotNull(applicationContext.getBean("echoService"));
	}

	@Test
	public void testExceptionStatus() {
		ServiceInvoker invoker = null;
		if (applicationContext.containsBean(ServiceInvoker.SERVICE_NAME)) {
			invoker = (ServiceInvoker) applicationContext.getBean(ServiceInvoker.SERVICE_NAME);
		} else {
			invoker = (ServiceInvoker) applicationContext.getBean("global.serviceInvoker");
		}
		Object service = applicationContext.getBean("echoService");
		Object[] params = new Object[] { "Woot this is cool" };
		Call call = new Call("echoService", "doesntExist", params);
		invoker.invoke(call, service);
		assertEquals(false, call.isSuccess());
		assertEquals(Call.STATUS_METHOD_NOT_FOUND, call.getStatus());
		params = new Object[] { "too", "many", "params" };
		call = new Call("echoService", "echoNumber", params);
		invoker.invoke(call, service);
		assertEquals(false, call.isSuccess());
		assertEquals(Call.STATUS_METHOD_NOT_FOUND, call.getStatus());
	}

	@Test
	public void testSimpleEchoCall() {
		ServiceInvoker invoker = null;
		if (applicationContext.containsBean(ServiceInvoker.SERVICE_NAME)) {
			invoker = (ServiceInvoker) applicationContext.getBean(ServiceInvoker.SERVICE_NAME);
		} else {
			invoker = (ServiceInvoker) applicationContext.getBean("global.serviceInvoker");
		}
		Object[] params = new Object[] { "Woot this is cool" };
		Object service = applicationContext.getBean("echoService");
		PendingCall call = new PendingCall("echoService", "echoString", params);
		invoker.invoke(call, service);
		assertEquals(true, call.isSuccess());
		assertEquals(params[0], call.getResult());
	}

	/**
	 * Test for memory leak bug #631
	 * http://trac.red5.org/ticket/631
	 * @throws InterruptedException 
	 */
	@Test
	public void testBug631() throws InterruptedException {
		log.debug("-----------------------------------------------------------------testBug631");

		final String message = "This is a test";

		//create our sender conn and set local
		IClientRegistry dummyReg = (IClientRegistry) applicationContext.getBean("global.clientRegistry");

		final IScope scp = (WebScope) applicationContext.getBean("web.scope"); //conn.getScope();
		final IContext ctx = (Context) applicationContext.getBean("web.context"); //scope.getContext();

		final IConnection recipient = new SvcCapableTestConnection("localhost", "/junit", "1");//host, path, session id
		IClient rClient = dummyReg.newClient(new Object[] { "recipient" });
		((TestConnection) recipient).setClient(rClient);
		((TestConnection) recipient).setScope((Scope) scp);
		((DummyClient) rClient).registerConnection(recipient);

		final IConnection sender = new SvcCapableTestConnection("localhost", "/junit", "2");//host, path, session id
		IClient sClient = dummyReg.newClient(new Object[] { "sender" });
		((TestConnection) sender).setClient(sClient);
		((TestConnection) sender).setScope((Scope) scp);
		((DummyClient) sClient).registerConnection(sender);

		Thread r = new Thread(new Runnable() { 
			public void run() {
				Red5.setConnectionLocal(recipient);
				IConnection conn = Red5.getConnectionLocal();
				assertTrue(scp.connect(conn));
				try {
					Thread.sleep(120L);
				} catch (InterruptedException e) {
				}
				log.debug("Check s/c -\n s1: {} s2: {}\n c1: {} c2: {}", new Object[] { scp, conn.getScope(), ctx, conn.getScope().getContext() });
			}
		});
		Thread s = new Thread(new Runnable() { 
			public void run() {
				Red5.setConnectionLocal(sender);
				IConnection conn = Red5.getConnectionLocal();
				assertTrue(scp.connect(conn));
				try {
					Thread.sleep(10L);
				} catch (InterruptedException e) {
				}
				Object[] sendobj = new Object[] { conn.getClient().getId(), message };
				// get the recipient
				IClientRegistry reg = ctx.getClientRegistry();
				IConnection rcon = reg.lookupClient("recipient").getConnections(scp).iterator().next();
				((IServiceCapableConnection) rcon).invoke("privMessage", sendobj);
				try {
					Thread.sleep(100L);
				} catch (InterruptedException e) {
				}
				log.debug("Check s/c -\n s1: {} s2: {}\n c1: {} c2: {}", new Object[] { scp, conn.getScope(), ctx, conn.getScope().getContext() });
			}
		});
		r.start();
		s.start();
		
		r.join();
		s.join();
		
		IClientRegistry reg = ctx.getClientRegistry();
		log.debug("Client registry: {}", reg.getClass().getName());
		if (reg.hasClient("recipient")) {
			IClient recip = reg.lookupClient("recipient");
			Set<IConnection> rcons = recip.getConnections(scp);
			log.debug("Recipient has {} connections", rcons.size());
		} else {
			fail("Recipient not found");
		}

		assertTrue(((SvcCapableTestConnection) recipient).getPrivateMessageCount() == 1);
		
	}

	/**
	 * Test for memory leak bug #631 with multiple threads
	 * http://trac.red5.org/ticket/631
	 */
	//@Test
	public void testMultiThreadBug631() throws Throwable {
		log.debug("-----------------------------------------------------------------testMultiThreadBug631");

		//leak doesnt appear unless this is around 1000 and running outside eclipse
		int threadCount = 2000;

		//init and run

		TestRunnable[] trs = new TestRunnable[threadCount];
		for (int t = 0; t < threadCount; t++) {
			ConnectionWorker worker = new ConnectionWorker(createConnection(t), t);
			trs[t] = worker;
			workerList.add(worker.getName());
		}

		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);

		long start = System.nanoTime();
		mttr.runTestRunnables(40000L); // max runtime 40s
		log.info("Runtime: {} ns", (System.nanoTime() - start));

		for (TestRunnable r : trs) {
			ConnectionWorker wkr = (ConnectionWorker) r;
			String name = (wkr.getName());
			assertNotNull(name);
			//close them all down
			wkr.close();
			//
			int msgCount = wkr.getServiceCapableConnection().getPrivateMessageCount();
			log.debug("Message count: {}", msgCount);
			assertTrue(msgCount > threadCount);
		}

		//make sure all threads finished
		assertEquals(threadCount, finishedCount.get());

	}

	private IConnection createConnection(int id) {
		//create our sender conn and set local
		IClientRegistry dummyReg = (IClientRegistry) applicationContext.getBean("global.clientRegistry");

		IScope scp = (WebScope) applicationContext.getBean("web.scope");
		//IContext ctx = (Context) applicationContext.getBean("web.context");

		IConnection conn = new SvcCapableTestConnection("localhost", "/junit", id + "");//host, path, session id
		IClient rClient = dummyReg.newClient(new Object[] { "client-" + id });
		((TestConnection) conn).setClient(rClient);
		((TestConnection) conn).setScope((Scope) scp);
		((DummyClient) rClient).registerConnection(conn);

		return conn;
	}

	final static class ConnectionWorker extends TestRunnable {

		IConnection conn;

		String name;

		public ConnectionWorker(IConnection conn, int index) {
			this.conn = conn;
			this.name = "client-" + index;
		}

		@SuppressWarnings("deprecation")
		public void runTest() throws Throwable {
			Red5.setConnectionLocal(conn);

			IClient client = conn.getClient();
			IScope scope = (WebScope) conn.getScope();
			IContext context = (Context) scope.getContext();

			IClientRegistry reg = context.getClientRegistry();

			IServiceCapableConnection serviceCapCon = null;

			//start standard process
			assertTrue(conn.connect(scope));

			//go through the client list and send at least one message to everyone
			for (String worker : workerList) {
				//dont send to ourself
				if (name.equals(worker)) {
					log.debug("Dont send to self");
					continue;
				}
				if (reg.hasClient(worker)) {
					IClient recip = reg.lookupClient(worker);
					Set<IConnection> rcons = recip.getConnections(scope);
					//log.debug("Recipient has {} connections", rcons.size());
					Object[] sendobj = new Object[] { client.getId(), "This is a message from " + name };
					for (IConnection rcon : rcons) {
						if (rcon instanceof IServiceCapableConnection) {
							serviceCapCon = (IServiceCapableConnection) rcon;
							serviceCapCon.invoke("privMessage", sendobj);
							break;
						} else {
							log.info("Connection is not service capable");
						}
					}
				} else {
					log.warn("Client not registered {}", worker);
				}
			}

			//number of connections
			int connectionCount = workerList.size();

			//now send N messages to random recipients
			for (int i = 0; i < 4000; i++) {
				String worker = workerList.get(rnd.nextInt(connectionCount));
				//dont send to ourself
				if (name.equals(worker)) {
					//log.debug("Dont send to self");
					continue;
				}
				if (reg.hasClient(worker)) {
					IClient recip = reg.lookupClient(worker);
					Set<IConnection> rcons = recip.getConnections(scope);
					//log.debug("Recipient has {} connections", rcons.size());
					Object[] sendobj = new Object[] { client.getId(), "This is a message from " + name };
					for (IConnection rcon : rcons) {
						if (rcon instanceof IServiceCapableConnection) {
							serviceCapCon = (IServiceCapableConnection) rcon;
							serviceCapCon.invoke("privMessage", sendobj);
							break;
						} else {
							log.info("Connection is not service capable");
						}
					}
				} else {
					log.warn("Client not registered {}", worker);
				}
			}

			finishedCount.incrementAndGet();

		}

		public void close() {
			DummyClient client = (DummyClient) conn.getClient();
			client.unregisterConnection(conn);
			conn.close();
		}

		public String getName() {
			return name;
		}

		public SvcCapableTestConnection getServiceCapableConnection() {
			return (SvcCapableTestConnection) conn;
		}
	}

	final class SvcCapableTestConnection extends TestConnection implements IServiceCapableConnection {

		private int privateMessageCount = 0;

		public SvcCapableTestConnection(String host, String path, String sessionId) {
			super(host, path, sessionId);
		}

		public int getPrivateMessageCount() {
			return privateMessageCount;
		}

		public void invoke(String method, Object[] params) {
			log.debug("Invoke on connection: {}", this.client.getId());
			if ("privMessage".equals(method)) {
				log.info("Got a private message from: {} message: {}", params);
				privateMessageCount++;
			} else {
				log.warn("Method: {} not implemented", method);
			}
		}

		@Override
		public void invoke(IServiceCall call) {
		}

		@Override
		public void invoke(IServiceCall call, int channel) {
		}

		@Override
		public void invoke(String method) {
		}

		@Override
		public void invoke(String method, IPendingServiceCallback callback) {
		}

		@Override
		public void invoke(String method, Object[] params, IPendingServiceCallback callback) {
		}

		@Override
		public void notify(IServiceCall call) {
		}

		@Override
		public void notify(IServiceCall call, int channel) {
		}

		@Override
		public void notify(String method) {
		}

		@Override
		public void notify(String method, Object[] params) {
		}

		@Override
		public void status(Status status) {

		}

		@Override
		public void status(Status status, int channel) {
			
		}

	}

}
