package io.antmedia.security;

import java.time.Instant;
import java.util.Map;

import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;

public class ExpireStreamPublishSecurity implements IStreamPublishSecurity {

	private DataStoreFactory dataStoreFactory;
	private DataStore dataStore;


	protected static Logger logger = LoggerFactory.getLogger(ExpireStreamPublishSecurity.class);

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams, String metaData) {

		boolean result = false;

		Broadcast broadcast = getDatastore().get(name);
		if (broadcast != null) 
		{
			int expireDurationMS = broadcast.getExpireDurationMS();

			if (expireDurationMS != 0) 
			{
				if (System.currentTimeMillis() < (broadcast.getDate() + expireDurationMS)) {
					result = true;
				}
				else {
					logger.info("Not allowing the stream "+ broadcast.getStreamId() +" to publish. It is expired.");
				}
			}
			else {
				result = true;
			}
		}
		else {
			result = true;
		}
		
		if (result) {
			result = checkStreamDateValid(broadcast);
		}
		
		if (!result) {
			IConnection connectionLocal = Red5.getConnectionLocal();
			if (connectionLocal != null) {
				connectionLocal.close();
			}
			else {
				logger.warn("Connection is null so that it cannot be closed");
			}
		}

		return result;
	}
	
	public boolean checkStreamDateValid(Broadcast broadcast) 
	{
		// if there is a broadcast previously added to db before
		// check that its date parameters
		if (broadcast != null && broadcast.getPlannedStartDate() != 0 && broadcast.getPlannedEndDate() != 0)
		{
			long now = Instant.now().getEpochSecond();
			// check that its date parameters (unix timestamp)
			if (now < broadcast.getPlannedStartDate() || now > broadcast.getPlannedEndDate()) {
				logger.error("Stream Planned Date Parameters are unacceptable. Stream Start Date({}) & Stream Start Date({})", broadcast.getPlannedStartDate(), broadcast.getPlannedEndDate());
				return false;
			}
		}
		return true;
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

}
