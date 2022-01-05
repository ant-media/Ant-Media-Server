package io.antmedia.test.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.MapDBStore;
import io.antmedia.console.datastore.MongoStore;
import io.antmedia.datastore.db.types.User;
import io.antmedia.rest.model.UserType;


public class ConsoleDataStoreUnitTest {
	
	@Test
	public void testMongoStore() {
		AbstractConsoleDataStore dt = new MongoStore("127.0.0.1", "", "");
		simpleDBOperations(dt);
	}
	
	@Test
	public void testMapDBStore() {
		AbstractConsoleDataStore dt = new MapDBStore();
		simpleDBOperations(dt);
	}
	
	
	public void simpleDBOperations(AbstractConsoleDataStore dtStore) {
		dtStore.clear();
		
		assertEquals(0, dtStore.getNumberOfUserRecords());
		
		String username = "test";
		String password = "pass" + (Math.random()*10000);
		assertTrue(dtStore.addUser(username, password, UserType.ADMIN));
		assertEquals(1, dtStore.getNumberOfUserRecords());
		
		assertTrue(dtStore.doesUsernameExist(username));
		assertFalse(dtStore.doesUsernameExist(username+"123"));
		
		
		assertTrue(dtStore.doesUserExist(username, password));
		assertFalse(dtStore.doesUserExist(username, password + "123"));
		assertFalse(dtStore.doesUserExist(username + "123", password));
		
		User user = dtStore.getUser(username);
		assertEquals(password, user.getPassword());
		assertEquals(UserType.ADMIN, user.getUserType());
		
		assertTrue(dtStore.deleteUser(username));
		assertFalse(dtStore.deleteUser(username+"123"));
		
		assertEquals(0, dtStore.getNumberOfUserRecords());
		
		
	}

}
