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
import org.springframework.context.annotation.PropertySource;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.licence.ILicenceService;

public class AcceptOnlyStreamsInDataStore implements IStreamPublishSecurity  {

	@Autowired
	private DataStoreFactory dataStoreFactory;
	
	@Autowired
	private AppSettings appSettings;

	private DataStore dataStore;

	private ILicenceService licenService = null;

	public static final String BEAN_NAME = "acceptOnlyStreamsInDataStore"; 

	protected static Logger logger = LoggerFactory.getLogger(AcceptOnlyStreamsInDataStore.class);

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams, String metaData, String token, String subscriberId, String subscriberCode) {

		boolean result = false;


		Broadcast broadcast = getDatastore().get(name);

		if (broadcast == null) 
		{
			if (appSettings.isAcceptOnlyStreamsInDataStore()) {
				logger.info("OnlyStreamsInDataStore is allowed and accepting streamId:{}", name);
				result = false;
			}
			else {
				logger.info("AcceptOnlyStreamsInDataStore is not activated. Accepting stream {}", name);
				result = true;
			}
		} 
		else 
		{
			result = true;
			if (AntMediaApplicationAdapter.isStreaming(broadcast.getStatus()) && 			
					AntMediaApplicationAdapter.isInstanceAlive(broadcast.getOriginAdress(), getAppAdaptor(scope).getServerSettings().getHostAddress(),  getAppAdaptor(scope).getServerSettings().getDefaultHttpPort(), scope.getName()) 
				) 
		    {
				logger.info("Does not accept stream:{} because it's streaming", name);
				result = false;
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

	public ILicenceService getLicenceService(IScope scope) {
		return (ILicenceService)scope.getContext().getBean(ILicenceService.BeanName.LICENCE_SERVICE.toString());
	}
	
	public AntMediaApplicationAdapter getAppAdaptor(IScope scope) {
		return (AntMediaApplicationAdapter)scope.getContext().getApplicationContext().getBean(AntMediaApplicationAdapter.BEAN_NAME);
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


	public DataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {
		this.dataStoreFactory = dataStoreFactory;
	}

	//this is for test usage
	public void setEnabledForTest(boolean enabled) {
		if(appSettings == null) {
			appSettings = new AppSettings();
		}
		appSettings.setAcceptOnlyStreamsInDataStore(enabled);
	}


}
