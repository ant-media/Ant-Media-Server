package io.antmedia.shutdown;

import java.util.ArrayList;
import java.util.List;

public class AMSShutdownManager {
	private static AMSShutdownManager instance = new AMSShutdownManager();
	
	public static final int RUNNING = 0;
	public static final int SHUTTING_DOWN = 1;
	public static final int SHUT_DOWN = 2;

	
	private volatile int serverState = RUNNING;
	
	private ArrayList<IShutdownListener> listeners = new ArrayList<>();

	public static AMSShutdownManager getInstance() {
		return instance;
	}
	
	//make a private constructor for singleton instance
	private AMSShutdownManager() {
	}
	
	public void subscribe(IShutdownListener listener) {
		getListeners().add(listener);
	}
	
	public void notifyShutdown() {
		if(serverState == RUNNING) {
			serverState = SHUTTING_DOWN;
			for (IShutdownListener listener : getListeners()) {
				listener.serverShuttingdown();
			}
			
			//We know listeners wait until shut down
			serverState = SHUT_DOWN;
		}
	}

	public List<IShutdownListener> getListeners() {
		return listeners;
	}

	public int getServerState() {
		return serverState;
	}

	public void setServerState(int serverState) {
		this.serverState = serverState;
	}
}
