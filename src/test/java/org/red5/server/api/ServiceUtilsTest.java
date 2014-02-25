package org.red5.server.api;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.JUnit4TestAdapter;
import net.sourceforge.groboutils.junit.v1.MultiThreadedTestRunner;
import net.sourceforge.groboutils.junit.v1.TestRunnable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.ClientRegistry;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.service.ServiceUtils;
import org.red5.server.scope.Scope;

public class ServiceUtilsTest extends BaseTest {
	
	private static ClientRegistry registry;

	private static IScope scope;
	
	private static AtomicInteger callbackCounter;

	private static AtomicBoolean testCompleted;

	private static AtomicInteger ipCounter;
	
	private int threads = 100; //510 == Ok on 02/06

	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(ServiceUtilsTest.class);
	}

	@Before
	public void setUp() throws Exception {
		// get context and registry
		context = (IContext) applicationContext.getBean("web.context");
		registry = (ClientRegistry) applicationContext.getBean("global.clientRegistry");
		// get the scope
		scope = (IScope) context.resolveScope("/app/test");
		// callback counter
		callbackCounter = new AtomicInteger();
		// set sentinel
		testCompleted = new AtomicBoolean(false);
		// set up ip counter
		ipCounter = new AtomicInteger(1);
	}

	@After
	public void tearDown() throws Exception {
		if (scope != null) {
			Set<IConnection> conns = scope.getClientConnections();
			for (IConnection conn : conns) {
				conn.close();
			}
		}
	}

	@Test
	public void testInvokeOnConnectionStringObjectArray() {
		System.out.println("------------------------------------------------------------------------\ntestInvokeOnConnectionStringObjectArray");
		// create a connection
		TestConnection conn = new TestConnection(host, "/", null);
		conn.setClient(registry.newClient(null));
		// add the connection to thread local
		Red5.setConnectionLocal(conn);
		// invoke on it
		assertTrue(ServiceUtils.invokeOnConnection("echo", new Object[] { "test 123" }, new GenericCallback()));
		long loopStart = System.currentTimeMillis();
		do {
			try {
				Thread.sleep(100L);
			} catch (InterruptedException e) {
			}
			if ((System.currentTimeMillis() - loopStart) > 120000) {
				break;
			}
		} while (callbackCounter.get() < 1);
		assertTrue(callbackCounter.get() == 1);
	}

	@Test
	public void testInvokeOnAllConnectionsStringObjectArray() throws InterruptedException {
		System.out.println("------------------------------------------------------------------------\ntestInvokeOnAllConnectionsStringObjectArray");
		log.debug("{}", scope);
		// create a few connections
		TestConnection conn1 = new TestConnection(host, "/app", null, "127.0.0.1");
		conn1.setClient(registry.newClient(null));
		// add the first connection to thread local
		Red5.setConnectionLocal(conn1);
		// connect
		conn1.connect(scope);
		//
		TestConnection conn2 = new TestConnection(host, "/app", null, "127.0.0.2");
		conn2.setClient(registry.newClient(null));
		conn2.connect(scope);
		//
		TestConnection conn3 = new TestConnection(host, "/app", null, "127.0.0.3");
		conn3.setClient(registry.newClient(null));
		conn3.connect(scope);
		// give a moment for connect to settle
		Thread.sleep(250L);
		// invoke on it
		ServiceUtils.invokeOnAllConnections("echo", new Object[] { "test 123" }, new GenericCallback());
		long loopStart = System.currentTimeMillis();
		do {
			Thread.sleep(100L);
			// run for only up to 2 mins
			if ((System.currentTimeMillis() - loopStart) > 120000) {
				break;
			}
			if (scope.getClientConnections().size() == 0) {
				fail("No connections on scope: " + scope.getName());
			}
		} while (callbackCounter.get() < 3);
		assertTrue(callbackCounter.get() == 3);
	}

	@Test
	public void testMultiThreaded() throws InterruptedException {
		System.out.println("------------------------------------------------------------------------\ntestMultiThreaded");
		// create some clients
		TestRunnable[] trs = new TestRunnable[threads + 1];
		for (int t = 0; t < threads; t++) {
			trs[t] = new ClientWorker();
		}
		trs[threads] = new InvokeWorker(trs);
		System.out.println("Created workers: " + trs.length);
		MultiThreadedTestRunner mttr = new MultiThreadedTestRunner(trs);
		try {
			mttr.runTestRunnables();
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
		System.out.println("Run complete");
		for (TestRunnable r : trs) {
			if (r instanceof ClientWorker) {
				((ClientWorker) r).close();
			}
		}
		System.out.println("Callback count: " + callbackCounter.get());
		assertTrue(callbackCounter.get() == threads);
	}

	private class ClientWorker extends TestRunnable {

		TestConnection conn;

		boolean active;

		@SuppressWarnings("deprecation")
		public void runTest() throws Exception {
			try {
				// create a few connections
				int octet = ipCounter.getAndIncrement();
				if (octet <= 255) {
					conn = new TestConnection(host, "/app", null, "127.0.0." + octet);
				} else {
					conn = new TestConnection(host, "/app", null, "127.0.1." + (octet % 256));
				}
				IClient client = registry.newClient(null);
				conn.setClient(client);
				// add the connection to thread local
				Red5.setConnectionLocal(conn);
				// connect
				if (conn.connect(scope)) {
					active = true;
					System.out.printf("Connected to scope: %s\n", scope);
					System.out.printf("Scope clients: %d active conns: %d\n", scope.getClients().size(), ((Scope) scope).getActiveConnections());
				} else {
					fail("Scope connect failed");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			while (!testCompleted.get()) {
				Thread.sleep(10L);
			}
			active = false;
			//System.out.println("Client worker exit");
		}

		public void close() {
			//System.out.println("close");
			conn.close();
		}

		public boolean isActive() {
			return active;
		}
		
	}

	private class InvokeWorker extends TestRunnable {

		TestRunnable[] trs;

		InvokeWorker(TestRunnable[] trs) {
			this.trs = trs;
		}

		public void runTest() throws Exception {
			// make sure all the client workers are connected before we invoke
			int connected;
			do {
				connected = 0;
				for (TestRunnable r : trs) {
					if (r instanceof ClientWorker && ((ClientWorker) r).isActive()) {
						connected++;
					}
				}
				Thread.sleep(10L);
			} while (connected < threads);
			//assertTrue(((Scope) scope).getActiveConnections() == threads);			
			ServiceUtils.invokeOnAllScopeConnections(scope, "echo", new Object[] { "test 123" }, new GenericCallback());
			//ServiceUtils.invokeOnClient(null, scope, "echo", new Object[] { "test 123" }, new GenericCallback());
			//ServiceUtils.invokeOnAllConnections("echo", new Object[] { "test 123" }, new GenericCallback());
			System.out.println("Invoke complete");
			Thread.sleep(500L);
			System.out.println("Sleeping for a moment to let callbacks happen");			
			// allow the threads to join / finish up
			testCompleted.set(true);
		}

	}

	private final class GenericCallback implements IPendingServiceCallback {

		@Override
		public void resultReceived(IPendingServiceCall call) {
			log.debug("resultReceived - success: {} call: {}", call.isSuccess(), call);
			if (call.isSuccess()) {
				callbackCounter.incrementAndGet();
			}
		}

	}

}
