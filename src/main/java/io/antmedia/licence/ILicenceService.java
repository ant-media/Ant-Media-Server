package io.antmedia.licence;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.settings.ServerSettings;

public interface ILicenceService {
	
	String LICENCE_TYPE_COMMUNITY = "community";
	String LICENCE_TYPE_STANDARD = "standard";
	String LICENCE_TYPE_OFFLINE = "offline";
	String LICENCE_TYPE_MARKETPLACE = "marketplace";
	String LICENCE_TYPE_LOCAL_SERVER = "local_server";

    /**
     * The code should use injection instead of lookup by name
     */
    @Deprecated
    String BEAN_NAME = "ant.media.licence.service";

	/**
	 * Starts License operations
	 */
	void start();

	/**
	 * Check License Status
	 * @param key of the license
	 * @return result of the operation and message
	 */
	Licence checkLicence (String key);
	
	/**
	 * Returns the last license status checked
	 * @return
	 */
	Licence getLastLicenseStatus();
	
	/**
	 * Returns if license is blocked
	 * 
	 * @return true if license is blocked
	 *         false if license can be used
	 */
	boolean isLicenceSuspended();

	/**
	 * @return LICENCE_TYPE_COMMUNITY, LICENCE_TYPE_STANDARD, LICENCE_TYPE_OFFLINE, LICENCE_TYPE_MARKETPLACE
	 */
	String getLicenseType();
}
