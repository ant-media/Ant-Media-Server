package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.Vod;

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
		testBugGetExternalStreamsList(dataStore);
		testGetPagination(dataStore);
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);
		testRemoveEndpoint(dataStore);
		testRTMPURL(dataStore);
		testStreamWithId(dataStore);
		testSaveDetection(dataStore);
		testFilterSearchOperations(dataStore);
		testAddSocialEndpointCredentials(dataStore);
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);


	}

	@Test
	public void testMemoryDataStore() {

		IDataStore dataStore = new InMemoryDataStore("testdb");
		testBugGetExternalStreamsList(dataStore);
		testGetPagination(dataStore);
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);
		testRemoveEndpoint(dataStore);
		testRTMPURL(dataStore);
		testStreamWithId(dataStore);
		testSaveDetection(dataStore);
		testFilterSearchOperations(dataStore);
		testAddSocialEndpointCredentials(dataStore);
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);
		
	}

	@Test
	public void testMongoStore() {

		IDataStore dataStore = new MongoStore("testdb");
		Datastore store = ((MongoStore) dataStore).getDataStore();
		Query<Broadcast> deleteQuery = store.find(Broadcast.class);
		store.delete(deleteQuery);
		
		Query<TensorFlowObject> detectedObjects = store.find(TensorFlowObject.class);
		store.delete(detectedObjects);

		store = ((MongoStore) dataStore).getEndpointCredentialsDS();
		Query<SocialEndpointCredentials> deleteQuery2 = store.find(SocialEndpointCredentials.class);
		store.delete(deleteQuery2);
		
		store = ((MongoStore)dataStore).getVodDatastore();
		Query<Vod> deleteVodQuery = store.find(Vod.class);
		store.delete(deleteVodQuery);
		

		testBugGetExternalStreamsList(dataStore);
		testGetPagination(dataStore);
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);
		testRemoveEndpoint(dataStore);
		testRTMPURL(dataStore);
		testStreamWithId(dataStore);
		testSaveDetection(dataStore);
		testFilterSearchOperations(dataStore);
		testAddSocialEndpointCredentials(dataStore);
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);

	}
	
	
	
	public void testBugGetExternalStreamsList(IDataStore datastore) {
		
		
		// add ip camera 
		Broadcast broadcast = new Broadcast("name", "ipAddr", "username", "password", "rtspUrl", AntMediaApplicationAdapter.IP_CAMERA);
		datastore.save(broadcast);
		
		//add stream source 
		Broadcast streamSource = new Broadcast("name_stream_source");
		streamSource.setStreamUrl("rtsp urdfdfdl");
		streamSource.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		datastore.save(streamSource);
		
		//get external list
		List<Broadcast> streamsList = datastore.getExternalStreamsList();
		assertNotNull(streamsList);
		
		assertEquals(2, streamsList.size());
		
		//check that there are two streams and values are same as added above
		
	}
	
	public void testSaveStreamInDirectory(IDataStore datastore) {
		
		
		File f = new File("src/test/resources");
		
		long totalVodCount = datastore.getTotalVodNumber();
		assertEquals(0, totalVodCount);
		assertEquals(4, datastore.fetchUserVodList(f));
		
		//we know there are 4 files there
		//test_short.flv
		//test_video_360p_subtitle.flv
		//test_Video_360p.flv
		//test.flv
		
		totalVodCount = datastore.getTotalVodNumber();
		assertEquals(4, totalVodCount);
		
		//List<Vod> vodList = datastore.getVodList(0, 10);
		
		/*
		for (Vod vod : vodList) {
			System.out.println("File path: " + vod.getFilePath());
		}
		*/
	
		
		f = new File("not_exist");
		assertEquals(0, datastore.fetchUserVodList(f));
		
		
		assertEquals(0, datastore.fetchUserVodList(null));
		
		
	}

	public void testStreamWithId(IDataStore dataStore) {
		try {
			Broadcast broadcast = new Broadcast();
			broadcast.setName("stream_having_id");
			String streamId = "stream_id";
			broadcast.setStreamId(streamId);

			String streamIdReturn = dataStore.save(broadcast);

			assertEquals(streamId, streamIdReturn);

			assertEquals(streamId, broadcast.getStreamId());

		} catch (Exception e) {
			e.printStackTrace();
		}
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

		assertEquals(dataStore.get(key3).getRtmpURL(), rtmpURL + key3);

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
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	public void testVoDFunctions(IDataStore datastore) {
		//fail("Write test codes about saveVod, AddVod, AddUserVod, delete vod ");
		
		//create a vod
		
		Vod streamVod=new Vod("streamName", "streamId", "filePath", "vodName", 111, 111, 111, Vod.STREAM_VOD);
		
		//save stream vod
		
		datastore.addVod(streamVod);
		
		//check vod number
		
		assertEquals(1, datastore.getTotalVodNumber());
		
		//add uservod
		
		Vod userVod=new Vod("streamName", "streamId", "filePath", "vodName", 111, 111, 111, Vod.USER_VOD);
		
		datastore.addUserVod(userVod);
		
		//check vod number
		
		assertEquals(2, datastore.getTotalVodNumber());
		
		//delete streamVod
		datastore.deleteVod(streamVod.getVodId());
		
		assertEquals(1, datastore.getTotalVodNumber());
		
		//delete userVod
		datastore.deleteVod(userVod.getVodId());
		
		//check vod number
		assertEquals(0, datastore.getTotalVodNumber());
		
	}
	
	public void testEditCameraInfo(IDataStore datastore) {
		
		//fail("Write test codes about getCamera, getExternalStreamList ");
		
		//create an IP Camera
		
		Broadcast camera= new Broadcast("old_name", "0.0.0.0", "username", "password", "rtspUrl", AntMediaApplicationAdapter.IP_CAMERA);	
		
		//save this cam
		
		datastore.save(camera);
		
		//check it is saved
		assertNotNull(camera.getStreamId());
		
		//change cam info
		
		camera.setName("new_name");
		camera.setIpAddr("1.1.1.1");
	
		datastore.editCameraInfo(camera);
		
		//check whether is changed or not
		
		assertEquals("1.1.1.1", camera.getIpAddr());
		assertEquals("new_name", camera.getName());
		
		
		datastore.delete(camera.getStreamId());
		
	}

	public void testGetPagination(IDataStore dataStore) {
		
		List<Broadcast> broadcastList2 = dataStore.getBroadcastList(0, 50);
		for (Iterator iterator = broadcastList2.iterator(); iterator.hasNext();) {
			Broadcast broadcast = (Broadcast) iterator.next();
			dataStore.delete(broadcast.getStreamId());
			
		}

		for (int i = 0; i < 36; i++) {
			Broadcast broadcast = new Broadcast(null, null);
			broadcast.setName(i + "");
			String key = dataStore.save(broadcast);
			assertNotNull(key);
			assertNotNull(broadcast.getStreamId());

			assertEquals(dataStore.get(key).getName(), i + "");
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
			// int count = 10 + i;
			// assertEquals(count +"", broadcastList.get(i).getName());
			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));

			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}

		broadcastList = dataStore.getBroadcastList(20, 10);
		assertNotNull(broadcastList);
		assertEquals(10, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {
			/*
			 * int count = 20 + i; assertEquals(count +"",
			 * broadcastList.get(i).getName());
			 */

			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));

			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}

		broadcastList = dataStore.getBroadcastList(30, 10);
		assertNotNull(broadcastList);
		assertEquals(6, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {
			/*
			 * int count = 30 + i; assertEquals(count +"",
			 * broadcastList.get(i).getName());
			 */

			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));

			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}
	}

	public void testRemoveEndpoint(IDataStore dataStore) {
		Broadcast broadcast = new Broadcast(null, null);
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
		Endpoint endPoint = new Endpoint("broacdast id", "stream id", broadcast2.getName(), rtmpUrl, "generic", null);

		boolean result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
		assertTrue(result);

		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		String endpointStreamId = "stream id 2";
		Endpoint endPoint2 = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl,
				"facebook", null);

		result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint2);
		assertTrue(result);

		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		assertEquals(broadcast2.getEndPointList().size(), 2);

		// remove end point
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		// its size should be 1
		assertEquals(broadcast2.getEndPointList().size(), 1);

		// endpoint2 should be in the list, check stream id
		assertEquals(broadcast2.getEndPointList().get(0).streamId, endpointStreamId);

		//
		Endpoint endPoint3Clone = new Endpoint(endPoint2.broadcastId, endPoint2.streamId, endPoint2.name,
				endPoint2.rtmpUrl, endPoint2.type, null);

		// remove end point2
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint3Clone);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);

		// add new enpoints
		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl, "facebook", null);

		assertTrue(dataStore.addEndpoint(broadcast2.getStreamId(), endPoint));

		String rtmpUrl2 = "rtmp:(sdfsfskmkmkmkmf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint2 = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl2, "facebook", null);

		assertTrue(dataStore.addEndpoint(broadcast2.getStreamId(), endPoint2));

		assertTrue(dataStore.removeAllEndpoints(broadcast2.getStreamId()));

		broadcast2 = dataStore.get(broadcast2.getStreamId());
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);

	}

	public void testSimpleOperations(IDataStore dataStore) {
		try {

			Broadcast broadcast = new Broadcast(null, null);
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
			assertEquals(100000, (long) broadcast2.getDuration());

			result = dataStore.updateStatus(broadcast.getStreamId().toString(),
					AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(description, broadcast2.getDescription());
			assertEquals(100000, (long) broadcast2.getDuration());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcast2.getStatus());

			result = dataStore.updateStatus(broadcast.getStreamId().toString(),
					AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(description, broadcast2.getDescription());
			assertEquals(100000, (long) broadcast2.getDuration());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast2.getStatus());

			assertEquals(null, broadcast2.getEndPointList());

			String rtmpUrl = "rtmp:((ksklasjflakjflaskjflsadfkjsal";
			Endpoint endPoint = new Endpoint("broacdast id", "stream id", broadcast2.getName(), rtmpUrl, "generic", null);

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
			endPoint = new Endpoint("broacdast id 2", "stream id 2", broadcast2.getName(), rtmpUrl, "facebook", null);

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(broadcast2.getEndPointList().size(), 2);
			assertEquals(broadcast2.getEndPointList().get(1).name, broadcast2.getName());
			assertEquals(broadcast2.getEndPointList().get(1).rtmpUrl, rtmpUrl);

			Broadcast broadcast3=new Broadcast("test3");

			broadcast3.setQuality("poor");

			assertNotNull(broadcast3.getQuality());

			dataStore.save(broadcast3);

			result=dataStore.updateSourceQuality(broadcast3.getStreamId(), "good");



			assertTrue(result);

			assertEquals(dataStore.get(broadcast3.getStreamId()).getQuality(),"good");

			result = dataStore.delete(key);
			assertTrue(result);

			assertNull(dataStore.get(key));




		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	private void testFilterSearchOperations(IDataStore dataStore) {

		Broadcast cameraBroadcast = new Broadcast("test", "192.168.1.100", "admin", "admin", "rtspUrl", "ipCamera");
		Broadcast liveBroadcast = new Broadcast("live_test");
		liveBroadcast.setType("liveStream");

		assertNotNull(dataStore.save(cameraBroadcast));
		assertNotNull(dataStore.save(liveBroadcast));

		assertNotNull(cameraBroadcast.getType());
		assertNotNull(liveBroadcast.getStreamId());

		String type = dataStore.get(cameraBroadcast.getStreamId()).getType();
		String live_type = dataStore.get(liveBroadcast.getStreamId()).getType();

		assertEquals(type, "ipCamera");
		assertEquals(live_type, "liveStream");

		List<Broadcast> returnList = dataStore.filterBroadcastList(0, 10, "ipCamera");

		assertEquals(returnList.size(), 1);

		Vod newVod =  new Vod("streamName", "1112233" + (int)(Math.random() * 1000), "path", "vod", 1517239908, 17933, 1190425, "streamVod");
		Vod newVod2 = new Vod("davut", "111223" + (int)(Math.random() * 1000),  "path", "vod", 1517239808, 17933, 1190525, "streamVod");
		Vod newVod3 = new Vod("oguz", "11122" + (int)(Math.random() * 1000),  "path", "vod", 1517239708, 17933, 1190625, "streamVod");
		Vod newVod4 = new Vod("ahmet", "111" + (int)(Math.random() * 1000),  "path", "vod", 1517239608, 17933, 1190725, "streamVod");
		Vod newVod5 = new Vod("mehmet", "11" + (int)(Math.random() * 1000), "path", "vod", 1517239508, 17933, 1190825, "streamVod");

		assertTrue(dataStore.addVod(newVod));
		assertTrue(dataStore.addVod(newVod2));
		assertTrue(dataStore.addVod(newVod3));
		assertTrue(dataStore.addVod(newVod4));
		assertTrue(dataStore.addVod(newVod5));

		
		long totalVodNumber = dataStore.getTotalVodNumber();
		assertEquals(5, totalVodNumber);
		
		assertTrue(dataStore.deleteVod(newVod.getVodId()));
		assertTrue(dataStore.deleteVod(newVod2.getVodId()));
		totalVodNumber = dataStore.getTotalVodNumber();
		assertEquals(3, totalVodNumber);
		assertTrue(dataStore.deleteVod(newVod3.getVodId()));
		assertTrue(dataStore.deleteVod(newVod4.getVodId()));
		assertTrue(dataStore.deleteVod(newVod5.getVodId()));
		
		totalVodNumber = dataStore.getTotalVodNumber();
		assertEquals(0, totalVodNumber);
		
		
	}


	public void testAddSocialEndpointCredentials(IDataStore dataStore) 
	{
		// add social endpoint credential 

		assertNull(dataStore.addSocialEndpointCredentials(null));
		
		String name = "name" + (int)(Math.random()*10000000);
		String serviceName = "serviceName"  + (int)(Math.random()*10000000);
		String authTime = "authtime" + (int)(Math.random()*10000000);
		String expireTimeInSeconds = "expireTimeInSeconds" + (int)(Math.random()*10000000);
		String tokenType = "tokenType" + (int)(Math.random()*10000000);
		String accessToken = "accessToken" + (int)(Math.random()*10000000);
		String refreshToken = "refreshToken" + (int)(Math.random()*10000000);
		SocialEndpointCredentials credentials = new SocialEndpointCredentials(name, serviceName, authTime, expireTimeInSeconds, tokenType, accessToken, refreshToken);


		SocialEndpointCredentials addedCredential = dataStore.addSocialEndpointCredentials(credentials);

		assertNotNull(addedCredential);
		assertNotNull(addedCredential.getId());
		assertTrue(addedCredential.getId().length() >= 6);
		
		credentials.setServiceName(null);
		assertNull(dataStore.addSocialEndpointCredentials(credentials));
		//restore service name because it is used below to check values
		credentials.setServiceName(serviceName);
		
		// get id of the social endpoint
		SocialEndpointCredentials socialEndpointCredentials = dataStore.getSocialEndpointCredentials(addedCredential.getId());

		assertNotNull(socialEndpointCredentials);
		// check fields
		assertEquals(socialEndpointCredentials.getAccountName(), credentials.getAccountName());
		assertEquals(socialEndpointCredentials.getServiceName(), credentials.getServiceName());
		assertEquals(socialEndpointCredentials.getId(), addedCredential.getId());
		assertEquals(socialEndpointCredentials.getAccessToken(), credentials.getAccessToken());
		assertEquals(socialEndpointCredentials.getRefreshToken(), credentials.getRefreshToken());
		assertEquals(socialEndpointCredentials.getTokenType(), credentials.getTokenType());
		assertEquals(socialEndpointCredentials.getExpireTimeInSeconds(), credentials.getExpireTimeInSeconds());
		assertEquals(socialEndpointCredentials.getAuthTimeInMilliseconds(), credentials.getAuthTimeInMilliseconds());

		// add social endpoint 
		name = "name" + (int)(Math.random()*10000000);
		serviceName = "serviceName"  + (int)(Math.random()*10000000);
		authTime = "authtime" + (int)(Math.random()*10000000);
		expireTimeInSeconds = "expireTimeInSeconds" + (int)(Math.random()*10000000);
		tokenType = null;
		accessToken = "accessToken" + (int)(Math.random()*10000000);
		refreshToken = null;
		credentials = new SocialEndpointCredentials(name, serviceName, authTime, expireTimeInSeconds, tokenType, accessToken, refreshToken);

		addedCredential = dataStore.addSocialEndpointCredentials(credentials);

		assertNotNull(addedCredential);
		assertNotNull(addedCredential.getId());
		assertTrue(addedCredential.getId().length() >= 6);

		//get credentials
		socialEndpointCredentials = dataStore.getSocialEndpointCredentials(addedCredential.getId());

		// check fields
		assertEquals(socialEndpointCredentials.getAccountName(), credentials.getAccountName());
		assertEquals(socialEndpointCredentials.getServiceName(), credentials.getServiceName());
		assertEquals(socialEndpointCredentials.getId(), addedCredential.getId());
		assertEquals(socialEndpointCredentials.getAccessToken(), credentials.getAccessToken());
		assertEquals(socialEndpointCredentials.getRefreshToken(), credentials.getRefreshToken());
		assertEquals(socialEndpointCredentials.getTokenType(), credentials.getTokenType());
		assertEquals(socialEndpointCredentials.getExpireTimeInSeconds(), credentials.getExpireTimeInSeconds());
		assertEquals(socialEndpointCredentials.getAuthTimeInMilliseconds(), credentials.getAuthTimeInMilliseconds());

		// add other social endpoint
		name = "name" + (int)(Math.random()*10000000);
		serviceName = "serviceName"  + (int)(Math.random()*10000000);
		authTime = "authtime" + (int)(Math.random()*10000000);
		expireTimeInSeconds = "expireTimeInSeconds" + (int)(Math.random()*10000000);
		tokenType = "tokenType" + (int)(Math.random()*10000000);
		accessToken = "accessToken" + (int)(Math.random()*10000000);
		refreshToken = "refreshToken" + (int)(Math.random()*10000000);
		credentials = new SocialEndpointCredentials(name, serviceName, authTime, expireTimeInSeconds, tokenType, accessToken, refreshToken);

		addedCredential = dataStore.addSocialEndpointCredentials(credentials);

		assertNotNull(addedCredential);
		assertNotNull(addedCredential.getId());
		assertTrue(addedCredential.getId().length() >= 6);
		
		//it should not accept credential having id because there is already one in the db
		assertNotNull(dataStore.addSocialEndpointCredentials(credentials));
		
		//get credentials
		socialEndpointCredentials = dataStore.getSocialEndpointCredentials(addedCredential.getId());

		// check fields
		assertEquals(socialEndpointCredentials.getAccountName(), credentials.getAccountName());
		assertEquals(socialEndpointCredentials.getServiceName(), credentials.getServiceName());
		assertEquals(socialEndpointCredentials.getId(), addedCredential.getId());
		assertEquals(socialEndpointCredentials.getAccessToken(), credentials.getAccessToken());
		assertEquals(socialEndpointCredentials.getRefreshToken(), credentials.getRefreshToken());
		assertEquals(socialEndpointCredentials.getTokenType(), credentials.getTokenType());
		assertEquals(socialEndpointCredentials.getExpireTimeInSeconds(), credentials.getExpireTimeInSeconds());
		assertEquals(socialEndpointCredentials.getAuthTimeInMilliseconds(), credentials.getAuthTimeInMilliseconds());

		//it should not save
		credentials = new SocialEndpointCredentials(name, serviceName, authTime, expireTimeInSeconds, tokenType, accessToken, refreshToken);
		credentials.setId("not_id_in_db");
		assertNull(dataStore.addSocialEndpointCredentials(credentials));


		// get list of the social endpoint
		List<SocialEndpointCredentials> socialEndpoints = dataStore.getSocialEndpoints(0, 10);

		// check the count
		assertEquals(socialEndpoints.size(), 3);

		// remove social endpoint
		assertTrue(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));
		
		//remove same social endpoint
		assertFalse(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));
		
		assertFalse(dataStore.removeSocialEndpointCredentials("any_id_not_exist"));

		// get list of the social endpoint
		socialEndpoints = dataStore.getSocialEndpoints(0, 10);

		// check that the count
		assertEquals(socialEndpoints.size(), 2);
		
		// remove social endpoint
		assertTrue(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));
		// get list of the social endpoint
		socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		// check that the count
		assertEquals(socialEndpoints.size(), 1);
		
		// remove social endpoint
		assertTrue(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));
		// get list of the social endpoint
		socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		// check that the count
		assertEquals(socialEndpoints.size(), 0);
	}
	
	
	public void testSaveDetection(IDataStore dataStore){
		String item1 = "item1";
		long detectionTime = 434234L;
		float probability1 = 0.1f;
		
		List<TensorFlowObject> detectedObjects = new ArrayList<>();
		detectedObjects.add(new TensorFlowObject(item1, probability1, "imageId"));
		dataStore.saveDetection("id", detectionTime, detectedObjects);
		
		List<TensorFlowObject> list = dataStore.getDetectionList("id", 0, 10);
		assertEquals(1,list.size());
		assertEquals(item1, list.get(0).objectName);
		assertEquals(probability1, list.get(0).probability,0.1F);
		assertEquals(detectionTime, list.get(0).detectionTime);	
	}

}
