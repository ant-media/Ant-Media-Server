package io.antmedia.test.db;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.red5.server.adapter.AntMediaApplicationAdapter;

import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;


public class DBStoresUnitTest {


	@After
	public void after() {
		File f = new File("testdb");
		if (f.exists()) {
			try {
				Files.delete(f.toPath());
			} catch (IOException e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
	}
	
	
	@Test
	public void testMapDBStore() {
		
		IDataStore dataStore = new MapDBStore("testdb");
		testGetPagination(dataStore);	
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);  

	}
	
	@Test
	public void testMemoryDataStore() {
		
		IDataStore dataStore = new InMemoryDataStore("testdb");
		testGetPagination(dataStore);	
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);  

	}
	
	@Test
	public void testMongoStore() {
		
		IDataStore dataStore = new MongoStore("testdb");
		Datastore store = ((MongoStore)dataStore).getDataStore();
		Query<Broadcast> deleteQuery = store.find(Broadcast.class);
		store.delete(deleteQuery);
		
		testGetPagination(dataStore);	
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);  

	}

	public void testNullCheck(IDataStore dataStore) {


		try {
			String save = dataStore.save(null);

			assertNull(save);

			assertNull(dataStore.get(null));

			assertFalse(dataStore.updateName(null, "name", "description"));

			assertFalse(dataStore.updateDuration(null, 100000));

			assertFalse(dataStore.updateStatus(null, "created"));

			assertFalse(dataStore.addEndpoint(null, null));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGetPagination(IDataStore dataStore) {

		for (int i = 0; i < 36;i++) {
			Broadcast broadcast = new Broadcast();
			broadcast.setName(i+"");
			String key = dataStore.save(broadcast);
			assertNotNull(key);
			assertNotNull(broadcast.getStreamId());
			
			assertEquals(dataStore.get(key).getName(), i+"");
		}
		
		List<Broadcast> broadcastList = dataStore.getBroadcastList(0, 10);
		assertNotNull(broadcastList);
		assertEquals(10, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {
			
			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));
			
			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}
		
		
		broadcastList = dataStore.getBroadcastList(10, 10);
		assertNotNull(broadcastList);
		assertEquals(10, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {
			//int count = 10 + i;
			//assertEquals(count +"", broadcastList.get(i).getName());
			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));
			
			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}
		
		
		broadcastList = dataStore.getBroadcastList(20, 10);
		assertNotNull(broadcastList);
		assertEquals(10, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {
			/*int count = 20 + i;
			assertEquals(count +"", broadcastList.get(i).getName());
			*/
			
			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));
			
			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}
		
		broadcastList = dataStore.getBroadcastList(30, 10);
		assertNotNull(broadcastList);
		assertEquals(6, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {
			/*
			int count = 30 + i;
			assertEquals(count +"", broadcastList.get(i).getName());
			*/
			
			assertTrue(0 < Integer.valueOf(broadcastList.get(i).getName()));
			
			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}




	}

	public void testSimpleOperations(IDataStore dataStore) {
		try {


			Broadcast broadcast = new Broadcast();
			String key = dataStore.save(broadcast);

			assertNotNull(key);
			assertNotNull(broadcast.getStreamId());

			assertEquals(broadcast.getStreamId().toString(), key);

			Broadcast broadcast2 = dataStore.get(key);
			assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());
			assertTrue(broadcast2.isPublish());

			String name = "name 1";
			String description = "description 2";
			boolean result = dataStore.updateName(broadcast.getStreamId().toString(), name, description);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(description, broadcast2.getDescription());

			result = dataStore.updateDuration(broadcast.getStreamId().toString(), 100000);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(description, broadcast2.getDescription());
			assertEquals(100000, (long)broadcast2.getDuration());


			result = dataStore.updateStatus(broadcast.getStreamId().toString(), AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(description, broadcast2.getDescription());
			assertEquals(100000, (long)broadcast2.getDuration());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcast2.getStatus());


			result = dataStore.updateStatus(broadcast.getStreamId().toString(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(description, broadcast2.getDescription());
			assertEquals(100000, (long)broadcast2.getDuration());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast2.getStatus());

			assertEquals(null, broadcast2.getEndPointList());

			String rtmpUrl = "rtmp:((ksklasjflakjflaskjflsadfkjsal";
			Endpoint endPoint = new Endpoint("broacdast id", "stream id", broadcast2.getName(), rtmpUrl, "generic");

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), null);
			assertFalse(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(broadcast2.getEndPointList().size(), 1);
			assertEquals(broadcast2.getEndPointList().get(0).name, broadcast2.getName());
			assertEquals(broadcast2.getEndPointList().get(0).rtmpUrl, rtmpUrl);


			rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
			endPoint = new Endpoint("broacdast id 2", "stream id 2", broadcast2.getName(), rtmpUrl, "facebook");

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(broadcast2.getEndPointList().size(), 2);
			assertEquals(broadcast2.getEndPointList().get(1).name, broadcast2.getName());
			assertEquals(broadcast2.getEndPointList().get(1).rtmpUrl, rtmpUrl);


			result = dataStore.delete(key);
			assertTrue(result);

			assertNull(dataStore.get(key));
		}
		catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}

}
