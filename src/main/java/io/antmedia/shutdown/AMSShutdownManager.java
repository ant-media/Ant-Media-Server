package io.antmedia.shutdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AMSShutdownManager {
	
	protected Logger logger = LoggerFactory.getLogger(AMSShutdownManager.class);
	private static AMSShutdownManager instance = new AMSShutdownManager();

	private volatile boolean isShuttingDown = false;

	private Queue<IShutdownListener> listeners = new ConcurrentLinkedQueue<>();

	public static AMSShutdownManager getInstance() {
		return instance;
	}

	//make a private constructor for singleton instance
	private AMSShutdownManager() {
	}

	public void subscribe(IShutdownListener listener) {
		listeners.add(listener);
	}

	public synchronized void notifyShutdown() {
		if(!isShuttingDown) 
		{
			isShuttingDown = true;
			for (IShutdownListener listener : listeners) {
				try {
					listener.serverShuttingdown();
				}
				catch (Exception e) {
					logger.error(ExceptionUtils.getStackTrace(e));
				}
			}
		}

	}

	public Queue<IShutdownListener> getListeners() {
		return listeners;
	}

}
