package io.antmedia.shutdown;

import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.tribes.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;

public class AMSShutdownManager {
	private static AMSShutdownManager instance = new AMSShutdownManager();
	protected static Logger logger = LoggerFactory.getLogger(AMSShutdownManager.class);

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
		logger.info("\n\nserverState:"+serverState+"\n\n"+org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace(new Exception()));
		if(serverState == RUNNING) {
			logger.info("\ndddddddeneme 1");

			serverState = SHUTTING_DOWN;
			for (IShutdownListener listener : getListeners()) {
				logger.info("\ndddddddeneme 2");

				listener.serverShuttingdown();
				
				logger.info("\ndddddddeneme 3");

			}
			
			logger.info("\ndddddddeneme 4");

			//We know listeners wait until shut down
			serverState = SHUT_DOWN;
		}
		
		logger.info("\ndddddddeneme 5");

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
