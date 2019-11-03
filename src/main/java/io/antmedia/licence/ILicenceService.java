package io.antmedia.licence;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.settings.ServerSettings;

public interface ILicenceService {
	
	public enum BeanName {
		
		LICENCE_SERVICE("ant.media.licence.service");
		
		private String licenceBeanName;
		
		BeanName(String name) {
		    this.licenceBeanName =  name;
		 }
		
		@Override
		public String toString() {
			return this.licenceBeanName;
		}

	}

	/**
	 * Starts License operations
	 */
	public void start();
	
	
	/**
	 * Check License Status
	 * @param key of the license
	 * @return result of the operation and message
	 */
	public Licence checkLicence (String key);
	

	
	/**
	 * Sets Server Settings 
	 * @param serverSettings
	 */
	public void setServerSettings(ServerSettings serverSettings);

	
	/**
	 * Returns the last license status checked
	 * @return
	 */
	public Licence getLastLicenseStatus();
	
	
	
	

}
