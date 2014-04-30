package org.red5.server.net.rtmp;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.red5.server.api.Red5;
import org.red5.server.net.rtmp.message.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ReceivedMessageTask implements Callable<Boolean> {

	private final static Logger log = LoggerFactory.getLogger(ReceivedMessageTask.class);
	
	private RTMPConnection conn;
	
	private final IRTMPHandler handler;

	private final String sessionId;
	
	private Packet message;

	// flag representing handling status
	private AtomicBoolean done = new AtomicBoolean(false);

	// deadlock guard thread
	private Thread guard;
	
	// maximum time allowed to process received message
	private long maxHandlingTimeout = 500L;
	
	public ReceivedMessageTask(String sessionId, Packet message, IRTMPHandler handler) {
		this(sessionId, message, handler, (RTMPConnection) RTMPConnManager.getInstance().getConnectionBySessionId(sessionId));
	}
	
	public ReceivedMessageTask(String sessionId, Packet message, IRTMPHandler handler, RTMPConnection conn) {
		this.sessionId = sessionId;
		this.message = message;
		this.handler = handler;
		this.conn = conn;
	}	

	public Boolean call() throws Exception {
		// set connection to thread local
		Red5.setConnectionLocal(conn);
		try {
			//log.trace("[{}] run begin", sessionId);
			// don't run the deadlock guard if we're in debug mode
			if (!Red5.isDebug()) {
				// run a deadlock guard so hanging tasks will be interrupted
				guard = new Thread(new DeadlockGuard(Thread.currentThread()));
				guard.start();
			}
			// pass message to the handler
			handler.messageReceived(conn, message);
		} catch (Exception e) {
			log.error("Error processing received message {}", sessionId, e);
		} finally {
			//log.info("[{}] run end", sessionId);
			// clear thread local
			Red5.setConnectionLocal(null);
			// set done / completed flag
			done.set(true);
			if (guard != null) {
				// interrupt and join on deadlock guard
				guard.interrupt();
				guard.join();
			}
		}
		return Boolean.valueOf(done.get());
	}
	
	public void setMaxHandlingTimeout(long maxHandlingTimeout) {
		this.maxHandlingTimeout = maxHandlingTimeout;
	}

	/**
	 * Prevents deadlocked message handling.
	 */
	private class DeadlockGuard implements Runnable {
		
		Thread thread;
		
		DeadlockGuard(Thread thread) {
			this.thread = thread;
		}
		
		public void run() {
			try {
				Thread.sleep(maxHandlingTimeout);
			} catch (InterruptedException e) {
			}
			if (!done.get()) {
				log.info("Interrupting unfinished task on {}", sessionId);
				thread.interrupt();
			}
		}
		
	}
	
}