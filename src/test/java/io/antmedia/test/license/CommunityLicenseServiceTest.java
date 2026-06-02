package io.antmedia.test.license;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.licence.CommunityLicenceService;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class CommunityLicenseServiceTest {


	@Test
	public void testCheckLicence() {
		Licence result = new CommunityLicenceService().checkLicence("anything");
		
		//this should be always null, because community version does not check license status
		assertNull(result);
	}

}
