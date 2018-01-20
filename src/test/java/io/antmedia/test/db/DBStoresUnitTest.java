package io.antmedia.test.db;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.Fail;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;


public class DBStoresUnitTest {


	@Before
	public void before() {
		deleteMapDBFile();
		
	}
	
	@After
	public void after() {
		deleteMapDBFile();
	}
	
	public void deleteMapDBFile() {
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
		testRemoveEndpoint(dataStore);
		testRTMPURL(dataStore);

	}

	
	@Test
	public void testMemoryDataStore() {

		IDataStore dataStore = new InMemoryDataStore("testdb");
		testGetPagination(dataStore);	
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);  
		testRemoveEndpoint(dataStore);
		testRTMPURL(dataStore);

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
		testRemoveEndpoint(dataStore);
		
		testRTMPURL(dataStore);

	}
	
	
	public void testRTMPURL(IDataStore dataStore) {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);
		
		assertNull(dataStore.get(key).getRtmpURL());
		
		broadcast = new Broadcast();
		broadcast.setName("test2");
		broadcast.setRtmpURL(null);
		
		String key2 = dataStore.save(broadcast);
		
		assertNotEquals(key, key2);
		
		assertNull(dataStore.get(key2).getRtmpURL());
		
		
		broadcast = new Broadcast();
		broadcast.setName("test3");
		String rtmpURL = "content_is_not_important";
		broadcast.setRtmpURL(rtmpURL);
		
		String key3 = dataStore.save(broadcast);
		
		assertEquals(dataStore.get(key3).getRtmpURL(), rtmpURL+key3);
		
		
		
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

			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));

			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}
	}

	public void testRemoveEndpoint(IDataStore dataStore) {
		Broadcast broadcast = new Broadcast();
		String name = "name 1";
		String description = "description 2";
		broadcast.setName(name);
		broadcast.setDescription(description);
		String key = dataStore.save(broadcast);

		assertNotNull(key);
		assertNotNull(broadcast.getStreamId());

		Broadcast broadcast2 = dataStore.get(key);

		assertEquals(name, broadcast2.getName());
		assertEquals(description, broadcast2.getDescription());

		String rtmpUrl = "rtmp:((ksklasjflakjflaskjflsadfkjsal";
		Endpoint endPoint = new Endpoint("broacdast id", "stream id", broadcast2.getName(), rtmpUrl, "generic");

		boolean result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
		assertTrue(result);

		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		String endpointStreamId = "stream id 2";
		Endpoint endPoint2 = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl, "facebook");

		result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint2);
		assertTrue(result);

		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		assertEquals(broadcast2.getEndPointList().size(), 2);

		//remove end point
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		//its size should be 1
		assertEquals(broadcast2.getEndPointList().size(), 1);


		//endpoint2 should be in the list, check stream id
		assertEquals(broadcast2.getEndPointList().get(0).streamId, endpointStreamId);

		//
		Endpoint endPoint3Clone = new Endpoint(endPoint2.broadcastId, endPoint2.streamId, endPoint2.name, endPoint2.rtmpUrl, endPoint2.type);

		//remove end point2
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint3Clone);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);
		
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
