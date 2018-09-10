package io.antmedia.security;

import java.util.Map;

import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;

public class AcceptOnlyStreamsInDataStore implements IStreamPublishSecurity  {
	
	private IDataStore dataStore;
	
	private boolean enabled = true;
	
	public static final String BEAN_NAME = "acceptOnlyStreamsInDataStore"; 
	
	protected static Logger logger = LoggerFactory.getLogger(AcceptOnlyStreamsInDataStore.class);

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams) {
		
		boolean result = false;
		if (enabled) {
			Broadcast broadcast = dataStore.get(name);
			if (broadcast != null) 
			{
				result = true;
			}
			else {
				logger.info("No stream in data store not allowing the stream {}", name);
				Red5.getConnectionLocal().close();
			}
		}
		else {
			logger.info("AcceptOnlyStreamsInDataStore is not activated. Accepting all streams {}", name);
			result = true;
		}
		
		
		return result;
	}
	
	
	public IDataStore getDataStore() {
		return dataStore;
	}
	
	
	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}


	public boolean isEnabled() {
		return enabled;
	}


	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
