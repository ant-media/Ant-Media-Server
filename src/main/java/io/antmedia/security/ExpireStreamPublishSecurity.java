package io.antmedia.security;

import java.util.Map;

import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;

public class ExpireStreamPublishSecurity implements IStreamPublishSecurity {

	private IDataStore dataStore;


	protected static Logger logger = LoggerFactory.getLogger(ExpireStreamPublishSecurity.class);

	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode, Map<String, String> queryParams) {

		boolean result = false;

		Broadcast broadcast = dataStore.get(name);
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
		
		if (!result) {
			Red5.getConnectionLocal().close();
		}

		return result;
	}


	public IDataStore getDataStore() {
		return dataStore;
	}


	public void setDataStore(IDataStore dataStore) {
		this.dataStore = dataStore;
	}


}
