package io.antmedia.shutdown;

import java.util.ArrayList;

public class AMSShutdownManager {
	private static AMSShutdownManager instance = new AMSShutdownManager();
	
	private boolean isShuttingDown = false;
	
	private ArrayList<IShutdownListener> listeners = new ArrayList<>();

	public static AMSShutdownManager getInstance() {
		return instance;
	}
	
	//make a private constructor for singleton instance
	private AMSShutdownManager() {
	}
	
	public void subscribe(IShutdownListener listener) {
		listeners.add(listener);
	}
	
	public void notifyShutdown() {
		if(!isShuttingDown) {
			isShuttingDown = true;
			for (IShutdownListener listener : listeners) {
				listener.serverShuttingdown();
			}
		}
	}
}
