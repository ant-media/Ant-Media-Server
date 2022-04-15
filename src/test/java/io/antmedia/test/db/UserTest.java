package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.antmedia.datastore.db.types.User;
import io.antmedia.settings.ServerSettings;

public class UserTest {
	
	
    @Test
    public void testUserName() {
    	User user = new User();
    	
    	assertNull(user.getFirstName());
    	assertNull(user.getLastName());
    	
    	user.setFirstName("Firstname");
    	user.setLastName("Lastname");
    	
    	assertEquals("Firstname", user.getFirstName());
    	assertEquals("Lastname", user.getLastName());
    	
    }
    
}
