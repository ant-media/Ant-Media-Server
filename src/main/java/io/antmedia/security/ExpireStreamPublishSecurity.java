package io.antmedia.security;

import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.types.Broadcast;

public class ExpireStreamPublishSecurity implements IStreamPublishSecurity {

	private IDataStore dataStore;



	@Override
	public boolean isPublishAllowed(IScope scope, String name, String mode) {

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
