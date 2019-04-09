package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.antmedia.datastore.DBUtils;

public class DBUtilsTest {
	
	public final String IP4_REGEX = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
	
	@Before
	public void before() {
	}

	@After
	public void after() {
	}
	
    @Test
    public void testDBUtils() {
    	assertNotEquals(DBUtils.getHostAddress(), DBUtils.getGlobalHostAddress());
    	assertEquals(DBUtils.getHostAddress(), DBUtils.getLocalHostAddress());
    }
    
    @Test
    public void testIpFormat() {
    	String gip = DBUtils.getGlobalHostAddress();
    	
    	assertTrue(gip.matches(IP4_REGEX));
    }
    
}
