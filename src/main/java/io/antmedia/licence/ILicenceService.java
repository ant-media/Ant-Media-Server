package io.antmedia.licence;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.rest.model.Result;
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
	 * Saves License to the server
	 * @param licence
	 * @return result of the operation and message
	 */
	
	public Result saveLicence (Licence licence);
	
	
	/**
	 * Check License Status
	 * @param key of the license
	 * @return result of the operation and message
	 */
	public Licence checkLicence (String key);
	
	/**
	 * Retrieves Server Settings
	 * @return ServerSettings including server name and license key
	 */

	public ServerSettings fetchServerSettings();
	
	/**
	 * Gets active license retrieved from license service
	 * @return
	 */
	public Licence getActiveLicence();
	
	/**
	 * Sets Server Settings 
	 * @param serverSettings
	 */
	public void setServerSettings(ServerSettings serverSettings);

	
	
	
	
	
	

}
