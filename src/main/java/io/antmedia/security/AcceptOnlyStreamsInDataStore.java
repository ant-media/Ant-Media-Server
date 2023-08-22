package io.antmedia.security;

import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.licence.ILicenceService;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;

public class AcceptOnlyStreamsInDataStore implements IStreamPublishSecurity  {
	
	@Autowired
	private DataStoreFactory dataStoreFactory;
	
	private DataStore dataStore;
	
	@Value("${settings.acceptOnlyStreamsInDataStore:true}")
	private boolean enabled = true;

	private ILicenceService licenService = null;
	
	public static final String BEAN_NAME = "acceptOnlyStreamsInDataStore"; 
	
	protected static Logger logger = LoggerFactory.getLogger(AcceptOnlyStreamsInDataStore.class);

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams, String metaData) {
		
		boolean result = false;
		
		
		Broadcast broadcast = getDatastore().get(name);

		if (enabled) 
		{
		    if (broadcast == null) 
		    {
		        result = false;
		    } 
		    else
		    {
		        result = isStreamingActive(name, broadcast);
		    } 
		   
		} else {
		    logger.info("AcceptOnlyStreamsInDataStore is not activated. Accepting all streams {}", name);
		    
		    if (broadcast == null) 
		    {
		        result = true;
		    } 
		    else 
		    {
				result = isStreamingActive(name, broadcast);
		    }
		}
		
	
		if (result) 
		{
			//check license suspended
			ILicenceService licenceService = getLicenceService(scope);
			if (licenceService.isLicenceSuspended()) 
			{
				logger.info("License is suspended and not accepting connection for {}", name);
				result = false;
			}
		}
		
		
		if (!result) {
			IConnection connectionLocal = Red5.getConnectionLocal();
			if (connectionLocal != null) {
				connectionLocal.close();
			}
			else {
				logger.warn("Connection object is null for {}", name);
			}
			
		}
		
		
		return result;
	}

	private boolean isStreamingActive(String name, Broadcast broadcast) {
		boolean result;
		if (!broadcast.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING) 
		        && !broadcast.getStatus().equals(IAntMediaStreamHandler.BROADCAST_STATUS_PREPARING)) 
		{
		    result = true;
		} 
		else 
		{
			//if it's not streaming, it means its status is not updated and it's stuck in broadcasting state
		    result = !AntMediaApplicationAdapter.isStreaming(broadcast);
		    if (!result) {
		    	logger.info("Not accepting streamId:{} because it's streaming", name);
		    }
		}
		return result;
	}

	public ILicenceService getLicenceService(IScope scope) {
		return (ILicenceService)scope.getContext().getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());
	}

	public DataStore getDatastore() {
		if (dataStore == null) {
			dataStore = dataStoreFactory.getDataStore();
		}
		return dataStore;
	}


	public void setDataStore(DataStore dataStore) {
		this.dataStore = dataStore;
	}


	public boolean isEnabled() {
		return enabled;
	}


	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	
}
