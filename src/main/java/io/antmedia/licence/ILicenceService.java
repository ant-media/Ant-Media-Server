package io.antmedia.licence;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.settings.ServerSettings;

public interface ILicenceService {
	
	public static final String LICENCE_TYPE_COMMUNITY = "community";
	public static final String LICENCE_TYPE_STANDARD = "standard";
	public static final String LICENCE_TYPE_OFFLINE = "offline";
	public static final String LICENCE_TYPE_MARKETPLACE = "marketplace";

	
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
	
	/**
	 * Returns if license is blocked
	 * 
	 * @return true if license is blocked
	 *         false if license can be used
	 */
	public boolean isLicenceSuspended();
	
	
	/**
	 * 
	 * @return LICENCE_TYPE_COMMUNITY, LICENCE_TYPE_STANDARD, LICENCE_TYPE_OFFLINE, LICENCE_TYPE_MARKETPLACE
	 */
	public String getLicenseType();
	

}
