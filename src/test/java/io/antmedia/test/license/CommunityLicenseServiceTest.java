package io.antmedia.test.license;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.datastore.db.types.Licence;
import io.antmedia.licence.CommunityLicenceService;

import static org.junit.Assert.*;

@Tag("fast")
public class CommunityLicenseServiceTest {

    @Test
    public void testCheckLicence() {
        Licence result = new CommunityLicenceService().checkLicence("anything");

        //this should be always null, because community version does not check license status
        assertNull(result);
    }
}
