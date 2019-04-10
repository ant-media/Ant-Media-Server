package io.antmedia.test.license;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.licence.CommunityLicenceService;
import io.antmedia.licence.ILicenceService;
import io.antmedia.settings.ServerSettings;

public class CommunityLicenseServiceTest {

	protected static Logger logger = LoggerFactory.getLogger(CommunityLicenseServiceTest.class);
	private CommunityLicenceService licenseService;

	@Before
	public void before() {
		licenseService = new CommunityLicenceService();

	}

	@After
	public void after() {
		licenseService = null;

	}

	@Test
	public void testCheckLicence() {

		//create server settings
		ServerSettings serverSettings = new ServerSettings();
		
		//define license key
		serverSettings.setLicenceKey("test-test");
		
		licenseService.setServerSettings(serverSettings);
		
		Licence result = licenseService.checkLicence(serverSettings.getLicenceKey());
		
		//this should be always null, because community version does not check license status
		assertNull(result);
		
		assertNotNull(ILicenceService.BeanName.LICENCE_SERVICE.toString());
	}
	
}
