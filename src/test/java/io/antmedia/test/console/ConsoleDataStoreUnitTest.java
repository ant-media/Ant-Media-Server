package io.antmedia.test.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.MapDBStore;
import io.antmedia.console.datastore.MongoStore;
import io.antmedia.console.datastore.RedisStore;
import io.antmedia.datastore.db.types.User;
import io.antmedia.rest.model.UserType;
import io.vertx.core.Vertx;


public class ConsoleDataStoreUnitTest {
	
	@Test
	public void testMongoStore() {
		AbstractConsoleDataStore dt = new MongoStore("127.0.0.1", "", "");
		simpleDBOperations(dt);
	}
	
	@Test
	public void testRedisStore() {
		AbstractConsoleDataStore dt = new RedisStore("redis://127.0.0.1:6379");
		simpleDBOperations(dt);
	}
	
	@Test
	public void testMapDBStore() {
		Vertx vertx = Vertx.vertx();
		AbstractConsoleDataStore dt = new MapDBStore(vertx);
		simpleDBOperations(dt);
		vertx.close();
	}
	
	
	public void simpleDBOperations(AbstractConsoleDataStore dtStore) {
		dtStore.clear();
		
		assertEquals(0, dtStore.getNumberOfUserRecords());
		
		String username = "test";
		String password = "pass" + (Math.random()*10000);
		User user = new User(username, password, UserType.ADMIN , "system");
		assertTrue(dtStore.addUser(user));
		assertFalse(dtStore.addUser(user));
		assertEquals(1, dtStore.getNumberOfUserRecords());
		
		assertTrue(dtStore.doesUsernameExist(username));
		assertFalse(dtStore.doesUsernameExist(username+"123"));
		
		
		assertTrue(dtStore.doesUserExist(username, password));
		assertFalse(dtStore.doesUserExist(username, password + "123"));
		assertFalse(dtStore.doesUserExist(username + "123", password));
		
		assertEquals(null, dtStore.getUser(username + "123"));
		
		user = dtStore.getUser(username);
		assertEquals(password, user.getPassword());
		assertEquals(UserType.ADMIN, user.getUserType());
		
		assertTrue(dtStore.deleteUser(username));
		assertFalse(dtStore.deleteUser(username+"123"));
		
		assertEquals(0, dtStore.getNumberOfUserRecords());
		
		
	}

}
