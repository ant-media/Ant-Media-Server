package io.antmedia.licence;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.settings.ServerSettings;

public class CommunityLicenceService implements ILicenceService{
	
	public void start() {
		//no need to implement for Community Edition
	}

	public Licence checkLicence (String key) {
		//no need to implement for Community Edition
		return null;
	}


	public void setServerSettings(ServerSettings serverSettings) {
		//no need to implement for Community Edition
	}

	@Override
	public Licence getLastLicenseStatus() {
		//no need to implement for Community Edition
		return null;
	}

}
