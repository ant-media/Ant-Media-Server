package io.antmedia.test.db;


import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.antmedia.settings.ServerSettings;

@Tag("fast")
public class DBUtilsTest {
	
	public final String IP4_REGEX = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
	
	@BeforeEach
	public void before() {
	}

	@AfterEach
	public void after() {
	}
	
    @Test
    public void testDBUtils() {
    		ServerSettings serverSettings = new ServerSettings();
    		assertNotEquals(ServerSettings.getLocalHostAddress(), ServerSettings.getGlobalHostAddress());
    		assertEquals(serverSettings.getHostAddress(), ServerSettings.getLocalHostAddress());
    }
    
    @Test
    public void testIpFormat() {
    		String gip = ServerSettings.getGlobalHostAddress();
    	
    		assertTrue(gip.matches(IP4_REGEX));
    }
    
}
