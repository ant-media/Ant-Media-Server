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
	private long maxHandlingTime = 500L;
	
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
			//  // don't run the deadlock guard if timeout is <= 0
			if (maxHandlingTime <= 0) {
				// don't run the deadlock guard if we're in debug mode
				if (!Red5.isDebug()) {
					// run a deadlock guard so hanging tasks will be interrupted
					guard = new DeadlockGuard();
					new Thread(guard, String.format("DeadlockGuard#%s", sessionId)).start();
				}
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
				// calls internal to the deadlock guard to join its thread from this executor task thread
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
		this.maxHandlingTime = maxHandlingTimeout;
	}

	/**
	 * Prevents deadlocked message handling.
	 */
	private class DeadlockGuard implements Runnable {
		
		// executor task thread
		private Thread taskThread;
		
		// deadlock guard thread
		private Thread guardThread = null;

		AtomicBoolean sleeping = new AtomicBoolean(false);
		
		/**
		 * Creates the deadlock guard to prevent a message task from taking too long to process.
		 * @param thread
		 */
		DeadlockGuard() {
			// executor thread ref
			this.taskThread = Thread.currentThread();
		}
		
		/**
		 * Save the reference to the thread, and wait until the maxHandlingTimeout has elapsed.
		 * If it elapsed, kill the other thread.
		 * */
		public void run() {
			try {
				this.guardThread = Thread.currentThread();
				if (log.isDebugEnabled()) {
					log.debug("Threads - task: {} guard: {}", taskThread.getName(), guardThread.getName());
				}
				sleeping.compareAndSet(false, true);
				Thread.sleep(maxHandlingTime);
			} catch (InterruptedException e) {
				log.debug("Deadlock guard interrupted on {} during sleep", sessionId);	
			} finally {
				sleeping.set(false);
			}
			// if the message task is not yet done interrupt
			if (!done.get()) {
				// if the task thread hasn't been interrupted check its live-ness
				if (!taskThread.isInterrupted()) {
					// if the task thread is alive, interrupt it
					if (taskThread.isAlive()) {
						log.warn("Interrupting unfinished active task on {}", sessionId);
						taskThread.interrupt();
					}				
				} else {
					log.debug("Unfinished active task on {} already interrupted", sessionId);					
				}
			}
		}
		
		/**
		 * Joins the deadlock guard thread.
		 * It's called when the task finish before the maxHandlingTimeout
		 */
		public void join() {
			// interrupt deadlock guard if sleeping
			if (sleeping.get()) {
				guardThread.interrupt();
			}
			// Wait only a 1/4 of the max handling time
			// TDJ: Not really needed to wait guard die to release taskMessage processing
			//guardThread.join(maxHandlingTimeout / 4);
		}
		
	}
	
}