package io.antmedia.shutdown;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMSShutdownManager {
	private static AMSShutdownManager instance = new AMSShutdownManager();
	protected static Logger logger = LoggerFactory.getLogger(AMSShutdownManager.class);

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
	
	public void setShutDo(IShutdownListener listener) {
		listeners.add(listener);
	}
	
	public void notifyShutdown() {
		for (IShutdownListener listener : getListeners()) {
			listener.serverShuttingdown();
		}
		shutdownServer.serverShuttingdown();
	}

	public List<IShutdownListener> getListeners() {
		return listeners;
	}

	public void setShutdownServer(IShutdownListener shutdownServer) {
		this.shutdownServer = shutdownServer;
	}
}
