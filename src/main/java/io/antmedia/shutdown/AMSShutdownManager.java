package io.antmedia.shutdown;

import java.util.ArrayList;
import java.util.List;

public class AMSShutdownManager {
	private static AMSShutdownManager instance = new AMSShutdownManager();

	private boolean isShuttingDown = false;

	private ArrayList<IShutdownListener> listeners = new ArrayList<>();

	//this is not included to the list to guarantee called at the last
	private IShutdownListener shutdownServer;

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
				System.out.println("before serverShutdown -- " + listener.getClass().getCanonicalName() + " time: " + System.currentTimeMillis());
				try {
					listener.serverShuttingdown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public List<IShutdownListener> getListeners() {
		return listeners;
	}

}
