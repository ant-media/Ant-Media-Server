package io.antmedia.security;

import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.licence.ILicenceService;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

public abstract class AbstractStreamSecurity implements IStreamPublishSecurity  {

	public ILicenceService getLicenceService(IScope scope) {
		return null;
	}

	public DataStore getDatastore() {
		return null;
	}


	public void setDataStore(DataStore dataStore) {

	}


	public boolean isEnabled() {
		return false;
	}


	public void setEnabled(boolean enabled) {

	}

	public DataStoreFactory getDataStoreFactory() {
		return null;
	}


	public void setDataStoreFactory(DataStoreFactory dataStoreFactory) {

	}

	
}
