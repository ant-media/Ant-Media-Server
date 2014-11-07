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

	// deadlock guard instance
	private DeadlockGuard guard;
		
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
				guard = new DeadlockGuard(Thread.currentThread());
				new Thread(guard).start();
			}
			// pass message to the handler
			handler.messageReceived(conn, message);
		} catch (Exception e) {
			log.error("Error processing received message {} on {}", message, sessionId, e);
		} finally {
			//log.info("[{}] run end", sessionId);
			// clear thread local
			Red5.setConnectionLocal(null);
			// set done / completed flag
			done.set(true);
			if (guard != null) {
				// interrupt deadlock guard if sleeping
				if (guard.isSleeping()) {
					guard.interrupt();
				}
				// join it
				guard.join();
			}
		}
		return Boolean.valueOf(done.get());
	}
	
	/**
	 * Sets maximum handling time for an incoming message.
	 * 
	 * @param maxHandlingTimeout
	 */
	public void setMaxHandlingTimeout(long maxHandlingTimeout) {
		this.maxHandlingTimeout = maxHandlingTimeout;
	}

	/**
	 * Prevents deadlocked message handling.
	 */
	private class DeadlockGuard implements Runnable {
		
		Thread ownerThread;

		Thread taskThread;
		
		boolean sleeping;
		
		DeadlockGuard(Thread thread) {
			this.taskThread = thread;
			this.ownerThread = Thread.currentThread();
		}
		
		public void join() {
			try {
				ownerThread.join(maxHandlingTimeout / 4);
			} catch (InterruptedException e) {
				log.debug("Deadlock guard interrupted on {} during join", sessionId);	
			}
		}

		public void interrupt() {
			ownerThread.interrupt();			
		}

		public void run() {
			try {
				sleeping = true;
				Thread.sleep(maxHandlingTimeout);
			} catch (InterruptedException e) {
				log.debug("Deadlock guard interrupted on {} during sleep", sessionId);	
			} finally {
				sleeping = false;
			}
			if (!done.get()) {
				if (!taskThread.isInterrupted()) {
					if (taskThread.isAlive()) {
						log.warn("Interrupting unfinished active task on {}", sessionId);
						taskThread.interrupt();
					}				
				} else {
					log.debug("Unfinished active task on {} already interrupted", sessionId);					
				}
			}
		}

		public boolean isSleeping() {
			return sleeping;
		}
		
	}
	
}