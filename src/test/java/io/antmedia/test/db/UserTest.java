package io.antmedia.test.db;


import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.antmedia.datastore.db.types.User;
import io.antmedia.settings.ServerSettings;
import org.junit.jupiter.api.Test;

@Tag("fast")
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
