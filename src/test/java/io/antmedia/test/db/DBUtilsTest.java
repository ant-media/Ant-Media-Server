package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.antmedia.datastore.DBUtils;

public class DBUtilsTest {
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
    
}
