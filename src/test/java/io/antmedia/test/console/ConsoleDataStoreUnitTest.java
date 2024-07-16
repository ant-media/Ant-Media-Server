package io.antmedia.test.console;

import org.junit.Test;

import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.console.datastore.MapDBStore;
import io.antmedia.console.datastore.MongoStore;
import io.antmedia.console.datastore.RedisStore;
import io.antmedia.datastore.db.types.User;
import io.antmedia.datastore.db.types.UserType;
import io.vertx.core.Vertx;

import java.util.HashMap;

import static org.junit.Assert.*;


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
		HashMap<String, String> appNameUserTypeMap = new HashMap<>();

		appNameUserTypeMap.put("system", UserType.ADMIN.toString());
		User user = new User(username, password, UserType.ADMIN , "system", appNameUserTypeMap);
		assertTrue(dtStore.addUser(user));
		assertFalse(dtStore.addUser(user));
		assertEquals(1, dtStore.getNumberOfUserRecords());
		
		assertTrue(dtStore.doesUsernameExist(username));
		assertFalse(dtStore.doesUsernameExist(username+"123"));

		appNameUserTypeMap = new HashMap<>();
		appNameUserTypeMap.put("LiveApp", UserType.USER.toString());
		user.setAppNameUserType(appNameUserTypeMap);
		assertTrue(dtStore.editUser(user));

		user = dtStore.getUser(user.getEmail());
		assertTrue(user.getAppNameUserType().containsKey("LiveApp"));
		assertTrue(user.getAppNameUserType().get("LiveApp").equals(UserType.USER.toString()));

		
		assertTrue(dtStore.doesUserExist(username, password));
		assertFalse(dtStore.doesUserExist(username, password + "123"));
		assertFalse(dtStore.doesUserExist(username + "123", password));

        assertNull(dtStore.getUser(username + "123"));

		user = dtStore.getUser(username);
		assertEquals(password, user.getPassword());
		assertEquals(UserType.ADMIN, user.getUserType());
		
		assertTrue(dtStore.deleteUser(username));
		assertFalse(dtStore.deleteUser(username+"123"));
		
		assertEquals(0, dtStore.getNumberOfUserRecords());

	}

}
