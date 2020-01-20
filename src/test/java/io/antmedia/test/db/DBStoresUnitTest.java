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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.settings.ServerSettings;

public class DBStoresUnitTest {

	protected static Logger logger = LoggerFactory.getLogger(DBStoresUnitTest.class);


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

		DataStore dataStore = new MapDBStore("testdb");
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
		testGetActiveBroadcastCount(dataStore);
		testUpdateHLSViewerCount(dataStore);
		testWebRTCViewerCount(dataStore);
		testRTMPViewerCount(dataStore);
		testTokenOperations(dataStore);
		testConferenceRoom(dataStore);
		testUpdateStatus(dataStore);
		testP2PConnection(dataStore);
		testUpdateLocationParams(dataStore);
		testPlaylist(dataStore);
	}

	@Test
	public void testMemoryDataStore() {

		DataStore dataStore = new InMemoryDataStore("testdb");
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
		testGetActiveBroadcastCount(dataStore);
		testUpdateHLSViewerCount(dataStore);
		testWebRTCViewerCount(dataStore);
		testRTMPViewerCount(dataStore);
		testTokenOperations(dataStore);
		testConferenceRoom(dataStore);
		testUpdateStatus(dataStore);
		testP2PConnection(dataStore);
		testUpdateLocationParams(dataStore);
		testPlaylist(dataStore);
	}

	@Test
	public void testMongoStore() {

		DataStore dataStore = new MongoStore("localhost", "", "", "testdb");
		Datastore store = ((MongoStore) dataStore).getDataStore();
		Query<Broadcast> deleteQuery = store.find(Broadcast.class);
		store.delete(deleteQuery);

		Query<TensorFlowObject> detectedObjects = store.find(TensorFlowObject.class);
		store.delete(detectedObjects);

		store = ((MongoStore) dataStore).getEndpointCredentialsDS();
		Query<SocialEndpointCredentials> deleteQuery2 = store.find(SocialEndpointCredentials.class);
		store.delete(deleteQuery2);

		store = ((MongoStore)dataStore).getVodDatastore();
		Query<VoD> deleteVodQuery = store.find(VoD.class);
		store.delete(deleteVodQuery);

		testBugGetExternalStreamsList(dataStore);
		testGetPagination(dataStore);
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);
		testRemoveEndpoint(dataStore);
		testRTMPURL(dataStore);
		testStreamWithId(dataStore);
		//testSaveDetection(dataStore);
		testFilterSearchOperations(dataStore);
		testAddSocialEndpointCredentials(dataStore);
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);
		testGetActiveBroadcastCount(dataStore);
		testUpdateHLSViewerCount(dataStore);
		testWebRTCViewerCount(dataStore);
		testRTMPViewerCount(dataStore);
		testTokenOperations(dataStore);
		testClearAtStart(dataStore);
		testClearAtStartCluster(dataStore);
		testConferenceRoom(dataStore);
		testStreamSourceList(dataStore);
		testUpdateStatus(dataStore);
		testP2PConnection(dataStore);
		testUpdateLocationParams(dataStore);
		testPlaylist(dataStore);
	}
	
	@Test
	public void testBug() {
		
		MapDBStore dataStore = new MapDBStore("src/test/resources/damaged_webrtcappee.db");
		
		//Following methods does not return before the bug is fixed
		dataStore.fetchUserVodList(new File(""));
		
		dataStore.getVodList(0, 10, "name", "asc");
	}

	public void clear(DataStore dataStore) 
	{
		long numberOfStreams = dataStore.getBroadcastCount();
		int pageSize = 10;
		long pageCount = numberOfStreams / pageSize + ((numberOfStreams % pageSize) > 0 ? 1 : 0);
		int numberOfCall = 0;
		List<Broadcast> totalBroadcastList = new ArrayList<>();
		for (int i = 0; i < pageCount; i++) {
			totalBroadcastList.addAll(dataStore.getBroadcastList(i * pageSize, pageSize));
		}

		for (Broadcast broadcast : totalBroadcastList) {
			numberOfCall++;
			assertTrue(dataStore.delete(broadcast.getStreamId()));
		}

		assertEquals(numberOfCall, numberOfStreams);

	}

	public void testGetActiveBroadcastCount(DataStore dataStore) {

		//save random number of streams with status created
		//long broadcastCountInDataStore = dataStore.getBroadcastCount();
		clear(dataStore);

		assertEquals(0, dataStore.getBroadcastCount());


		long streamCount = (int)(Math.random()  * 500);

		if (streamCount < 10) {
			streamCount = 10;
		}

		System.out.println("Stream count to be added: " + streamCount);

		for (int i = 0; i < streamCount; i++) {
			dataStore.save(new Broadcast(null, null));
		}

		assertEquals(streamCount, dataStore.getBroadcastCount());

		//check that no active broadcast exist
		assertEquals(0, dataStore.getActiveBroadcastCount());

		//change random number of streams status to broadcasting
		long numberOfStatusChangeStreams = (int)(Math.random() * 500);
		if (streamCount < numberOfStatusChangeStreams) {
			numberOfStatusChangeStreams = streamCount;
		}

		int pageSize = 10;
		numberOfStatusChangeStreams = (numberOfStatusChangeStreams / pageSize) * pageSize; //normalize
		long pageCount = numberOfStatusChangeStreams / pageSize;
		int numberOfCall = 0;
		System.out.println("Number of status change stream count: " + numberOfStatusChangeStreams + 
				" page Count: " + pageCount);
		for (int i = 0; i < pageCount; i++) {

			List<Broadcast> broadcastList = dataStore.getBroadcastList(i * pageSize, pageSize);
			for (Broadcast broadcast : broadcastList) {
				numberOfCall++;
				assertTrue(dataStore.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));;
			}

		}

		assertEquals(numberOfCall, numberOfStatusChangeStreams);
		//check that active broadcast exactly the same as changed above
		assertEquals(numberOfStatusChangeStreams, dataStore.getActiveBroadcastCount());

		//change all streams to finished
		streamCount = dataStore.getBroadcastCount();
		pageCount = streamCount / pageSize + ((streamCount % pageSize) > 0 ? 1 : 0);
		for (int i = 0; i < pageCount; i++) {

			List<Broadcast> broadcastList = dataStore.getBroadcastList(i * pageSize, pageSize);
			for (Broadcast broadcast : broadcastList) {
				assertTrue(dataStore.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED));
			}

		}

		//check that no active broadcast
		assertEquals(0, dataStore.getActiveBroadcastCount());
	}


	public void testBugGetExternalStreamsList(DataStore datastore) {


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

	public void testSaveStreamInDirectory(DataStore datastore) {


		File f = new File("src/test/resources");

		long totalVodCount = datastore.getTotalVodNumber();
		assertEquals(0, totalVodCount);
		assertEquals(5, datastore.fetchUserVodList(f));

		//we know there are 5 files there
		//test_short.flv
		//test_video_360p_subtitle.flv
		//test_Video_360p.flv
		//test.flv
		//sample_MP4_480.mp4

		totalVodCount = datastore.getTotalVodNumber();
		assertEquals(5, totalVodCount);

		List<VoD> vodList = datastore.getVodList(0, 50, null, null);
		assertEquals(5, vodList.size());
		for (VoD voD : vodList) {
			assertEquals("streams/resources/"+voD.getVodName(), voD.getFilePath());
		}


		f = new File("not_exist");
		assertEquals(0, datastore.fetchUserVodList(f));


		assertEquals(0, datastore.fetchUserVodList(null));


	}

	public void testStreamWithId(DataStore dataStore) {
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

	public void testRTMPURL(DataStore dataStore) {
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

	public void testNullCheck(DataStore dataStore) {

		try {
			String save = dataStore.save(null);

			assertNull(save);

			assertNull(dataStore.get(null));

			assertFalse(dataStore.updateDuration(null, 100000));

			assertFalse(dataStore.updateStatus(null, "created"));

			assertFalse(dataStore.addEndpoint(null, null));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testVoDFunctions(DataStore datastore) {
		//fail("Write test codes about saveVod, AddVod, AddUserVod, delete vod ");

		//create a vod
		String vodId = RandomStringUtils.randomNumeric(24);
		VoD streamVod=new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, VoD.STREAM_VOD,vodId);

		//save stream vod

		datastore.addVod(streamVod);

		//check vod number

		assertEquals(1, datastore.getTotalVodNumber());
		VoD voD = datastore.getVoD(vodId);
		assertEquals(streamVod.getFilePath(), voD.getFilePath());
		assertEquals(streamVod.getStreamId(), voD.getStreamId());
		assertEquals(streamVod.getStreamName(), voD.getStreamName());
		assertEquals(streamVod.getType(), voD.getType());

		//add uservod
		vodId = RandomStringUtils.randomNumeric(24);
		VoD userVod=new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, VoD.USER_VOD,vodId);

		datastore.addVod(userVod);

		//check vod number

		assertEquals(2, datastore.getTotalVodNumber());
		voD = datastore.getVoD(userVod.getVodId());
		assertEquals(userVod.getFilePath(), voD.getFilePath());
		assertEquals(userVod.getStreamId(), voD.getStreamId());
		assertEquals(userVod.getStreamName(), voD.getStreamName());
		assertEquals(userVod.getType(), voD.getType());

		//delete streamVod
		datastore.deleteVod(streamVod.getVodId());
		assertNull(datastore.getVoD(streamVod.getVodId()));

		assertEquals(1, datastore.getTotalVodNumber());

		//delete userVod
		datastore.deleteVod(userVod.getVodId());
		assertNull(datastore.getVoD(voD.getVodId()));

		//check vod number
		assertEquals(0, datastore.getTotalVodNumber());

	}

	public void testEditCameraInfo(DataStore datastore) {

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

		datastore.updateBroadcastFields(camera.getStreamId(), camera);

		//check whether is changed or not
		assertEquals("1.1.1.1", camera.getIpAddr());
		assertEquals("new_name", camera.getName());
		datastore.delete(camera.getStreamId());
	}

	public void testUpdateHLSViewerCount(DataStore dataStore) {
		//create a stream
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setName("test2");
		String key2 = dataStore.save(broadcast2);

		//update hls viewer several times 
		//check hls viewer count
		int totalCountFor1 = 0;
		int totalCountFor2 = 0;
		for (int i = 0; i < 50; i++) {
			int viewerCount = (int)(Math.random()*99999);
			if (viewerCount % 2 == 0) {
				viewerCount = -1 * viewerCount;
			}
			assertTrue(dataStore.updateHLSViewerCount(key, viewerCount));

			totalCountFor1 += viewerCount;

			int viewerCount2 = (int)(Math.random()*99999);
			if (viewerCount2 % 2 == 0) {
				viewerCount2 = -1 * viewerCount2;
			}
			assertTrue(dataStore.updateHLSViewerCount(key2, viewerCount2));
			totalCountFor2 += viewerCount2;

			assertEquals(totalCountFor1, dataStore.get(key).getHlsViewerCount());
			assertEquals(totalCountFor2, dataStore.get(key2).getHlsViewerCount());
		}
	}

	public void testWebRTCViewerCount(DataStore dataStore) {
		//create a stream
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setName("test2");
		String key2 = dataStore.save(broadcast2);

		int totalViewerCountFor1 = 0;
		int totalViewerCountFor2 = 0;
		for (int i = 0; i < 150; i++) {

			boolean increment = false; 
			int randomValue = (int)(Math.random()*99999);
			if (randomValue % 2 == 0) {
				increment = true;
				totalViewerCountFor1++;
			}
			else {
				totalViewerCountFor1--;
			}
			assertTrue(dataStore.updateWebRTCViewerCount(key, increment));

			increment = false; 
			randomValue = (int)(Math.random()*99999);
			if (randomValue % 2 == 0) {
				increment = true;
				totalViewerCountFor2++;
			}
			else {
				totalViewerCountFor2--;
			}

			assertTrue(dataStore.updateWebRTCViewerCount(key2, increment));

			assertEquals(totalViewerCountFor1, dataStore.get(key).getWebRTCViewerCount());
			assertEquals(totalViewerCountFor2, dataStore.get(key2).getWebRTCViewerCount());
		}
	}

	public void testRTMPViewerCount(DataStore dataStore) {
		//create a stream
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setName("test2");
		String key2 = dataStore.save(broadcast2);

		int totalViewerCountFor1 = 0;
		int totalViewerCountFor2 = 0;
		for (int i = 0; i < 150; i++) {

			boolean increment = false; 
			int randomValue = (int)(Math.random()*99999);
			if (randomValue % 2 == 0) {
				increment = true;
				totalViewerCountFor1++;
			}
			else {
				totalViewerCountFor1--;
			}
			assertTrue(dataStore.updateRtmpViewerCount(key, increment));

			increment = false; 
			randomValue = (int)(Math.random()*99999);
			if (randomValue % 2 == 0) {
				increment = true;
				totalViewerCountFor2++;
			}
			else {
				totalViewerCountFor2--;
			}

			assertTrue(dataStore.updateRtmpViewerCount(key2, increment));

			assertEquals(totalViewerCountFor1, dataStore.get(key).getRtmpViewerCount());
			assertEquals(totalViewerCountFor2, dataStore.get(key2).getRtmpViewerCount());
		}
	}

	public void testGetPagination(DataStore dataStore) {

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

	public void testRemoveEndpoint(DataStore dataStore) {
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
		Endpoint endPoint = new Endpoint("broacdast id", "stream id", null, broadcast2.getName(), rtmpUrl, "generic", null);

		boolean result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
		assertTrue(result);

		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		String endpointStreamId = "stream id 2";
		Endpoint endPoint2 = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl,
				"facebook", null, null);

		result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint2);
		assertTrue(result);

		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		assertEquals(2, broadcast2.getEndPointList().size());

		// remove end point
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		// its size should be 1
		assertEquals(1, broadcast2.getEndPointList().size());

		// endpoint2 should be in the list, check stream id
		assertEquals(broadcast2.getEndPointList().get(0).getStreamId(), endpointStreamId);

		//
		Endpoint endPoint3Clone = new Endpoint(endPoint2.getBroadcastId(), endPoint2.getStreamId(), endPoint2.getName(),
				endPoint2.getRtmpUrl(), endPoint2.type, null, null);

		// remove end point2
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint3Clone);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);

		// add new enpoints
		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl, "facebook", null, null);

		assertTrue(dataStore.addEndpoint(broadcast2.getStreamId(), endPoint));

		String rtmpUrl2 = "rtmp:(sdfsfskmkmkmkmf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint2 = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl2, "facebook", null, null);

		assertTrue(dataStore.addEndpoint(broadcast2.getStreamId(), endPoint2));

		assertTrue(dataStore.removeAllEndpoints(broadcast2.getStreamId()));

		broadcast2 = dataStore.get(broadcast2.getStreamId());
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);

	}

	public void testSimpleOperations(DataStore dataStore) {
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
			Broadcast tmp = new Broadcast();
			tmp.setName(name);
			tmp.setDescription(description);
			boolean result = dataStore.updateBroadcastFields(broadcast.getStreamId(), tmp);
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
			Endpoint endPoint = new Endpoint("broacdast id", "stream id", broadcast2.getName(), rtmpUrl, "generic", null, null);

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), null);
			assertFalse(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(1, broadcast2.getEndPointList().size());
			assertEquals(broadcast2.getEndPointList().get(0).getName(), broadcast2.getName());
			assertEquals(broadcast2.getEndPointList().get(0).getRtmpUrl(), rtmpUrl);

			rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
			endPoint = new Endpoint("broacdast id 2", "stream id 2", broadcast2.getName(), rtmpUrl, "facebook", null, null);

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(2, broadcast2.getEndPointList().size());
			assertEquals(broadcast2.getEndPointList().get(1).getName(), broadcast2.getName());
			assertEquals(broadcast2.getEndPointList().get(1).getRtmpUrl(), rtmpUrl);

			Broadcast broadcast3 = new Broadcast("test3");

			broadcast3.setQuality("poor");

			assertNotNull(broadcast3.getQuality());

			dataStore.save(broadcast3);

			result = dataStore.updateSourceQualityParameters(broadcast3.getStreamId(), "good", 0, 0);

			assertTrue(result);

			assertEquals("good", dataStore.get(broadcast3.getStreamId()).getQuality());

			//set mp4 muxing to true
			result = dataStore.setMp4Muxing(key, MuxAdaptor.MP4_ENABLED_FOR_STREAM);

			//check that setting is saved
			assertTrue(result);

			//check that setting is saved correctly
			assertEquals(MuxAdaptor.MP4_ENABLED_FOR_STREAM, dataStore.get(key).getMp4Enabled());


			//check null case
			result = dataStore.setMp4Muxing(null, MuxAdaptor.MP4_DISABLED_FOR_STREAM);

			assertFalse(result);


			//set mp4 muxing to false
			result = dataStore.setMp4Muxing(key, MuxAdaptor.MP4_DISABLED_FOR_STREAM);

			//check that setting is saved
			assertTrue(result);

			//check that setting is saved correctly
			assertEquals(MuxAdaptor.MP4_DISABLED_FOR_STREAM, dataStore.get(key).getMp4Enabled());

			result = dataStore.delete(key);
			assertTrue(result);

			assertNull(dataStore.get(key));




		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}


	private void testFilterSearchOperations(DataStore dataStore) {

		Broadcast cameraBroadcast = new Broadcast("test", "192.168.1.100", "admin", "admin", "rtspUrl", "ipCamera");
		Broadcast liveBroadcast = new Broadcast("live_test");
		liveBroadcast.setType("liveStream");

		assertNotNull(dataStore.save(cameraBroadcast));
		assertNotNull(dataStore.save(liveBroadcast));

		assertNotNull(cameraBroadcast.getType());
		assertNotNull(liveBroadcast.getStreamId());

		String type = dataStore.get(cameraBroadcast.getStreamId()).getType();
		String live_type = dataStore.get(liveBroadcast.getStreamId()).getType();

		assertEquals("ipCamera", type);
		assertEquals("liveStream", live_type);

		List<Broadcast> returnList = dataStore.filterBroadcastList(0, 10, "ipCamera");

		assertEquals(1, returnList.size());

		VoD newVod =  new VoD("streamName", "1112233" + (int)(Math.random() * 1000), "path", "vod", 1517239908, 17933, 1190425, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000));
		VoD newVod2 = new VoD("davut", "111223" + (int)(Math.random() * 1000),  "path", "vod", 1517239808, 17933, 1190525, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000));
		VoD newVod3 = new VoD("oguz", "11122" + (int)(Math.random() * 1000),  "path", "vod", 1517239708, 17933, 1190625, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000));
		VoD newVod4 = new VoD("ahmet", "111" + (int)(Math.random() * 1000),  "path", "vod", 1517239608, 17933, 1190725, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000));
		VoD newVod5 = new VoD("mehmet", "11" + (int)(Math.random() * 1000), "path", "vod", 1517239508, 17933, 1190825, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000));

		String vodId = dataStore.addVod(newVod);
		assertNotNull(vodId);
		System.out.println("Vod id 1 " + vodId);

		vodId = dataStore.addVod(newVod2);
		assertNotNull(vodId);
		System.out.println("Vod id 2 " + vodId);

		vodId = dataStore.addVod(newVod3);
		assertNotNull(vodId);
		System.out.println("Vod id 3 " + vodId);

		vodId = dataStore.addVod(newVod4);
		assertNotNull(vodId);
		System.out.println("Vod id 4 " + vodId);

		vodId = dataStore.addVod(newVod5);
		assertNotNull(vodId);
		System.out.println("Vod id 5 " + vodId);


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


	public void testAddSocialEndpointCredentials(DataStore dataStore) 
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
		assertEquals(3, socialEndpoints.size());

		// remove social endpoint
		assertTrue(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));

		//remove same social endpoint
		assertFalse(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));

		assertFalse(dataStore.removeSocialEndpointCredentials("any_id_not_exist"));

		// get list of the social endpoint
		socialEndpoints = dataStore.getSocialEndpoints(0, 10);

		// check that the count
		assertEquals(2, socialEndpoints.size());

		// remove social endpoint
		assertTrue(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));
		// get list of the social endpoint
		socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		// check that the count
		assertEquals(1, socialEndpoints.size());

		// remove social endpoint
		assertTrue(dataStore.removeSocialEndpointCredentials(socialEndpoints.get(0).getId()));
		// get list of the social endpoint
		socialEndpoints = dataStore.getSocialEndpoints(0, 10);
		// check that the count
		assertEquals(0, socialEndpoints.size());
	}


	public void testSaveDetection(DataStore dataStore){
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

	public void testTokenOperations(DataStore store) {

		//create token
		Token testToken = new Token();
		
		//define a valid expire date
		long expireDate = Instant.now().getEpochSecond() + 1000;

		testToken.setStreamId("1234");
		testToken.setExpireDate(expireDate);
		testToken.setType(Token.PLAY_TOKEN);
		testToken.setTokenId("tokenID");

		store.saveToken(testToken);

		assertNotNull(testToken.getTokenId());

		//get tokens of stream
		List <Token> tokens = store.listAllTokens(testToken.getStreamId(), 0, 10);

		assertEquals(1, tokens.size());

		//revoke tokens
		store.revokeTokens(testToken.getStreamId());

		//get tokens of stream
		tokens = store.listAllTokens(testToken.getStreamId(), 0, 10);

		//it should be zero because all tokens are revoked
		assertEquals(0, tokens.size());

		//create token again
		testToken = new Token();

		testToken.setStreamId("1234");
		testToken.setExpireDate(expireDate);
		testToken.setType(Token.PLAY_TOKEN);
		testToken.setTokenId("tokenID");
		testToken.setRoomId("testRoom");

		store.saveToken(testToken);
		
		//get this token
		Token retrievedToken = store.getToken(testToken.getTokenId());
		
		assertNotNull(retrievedToken);
		assertEquals("testRoom", retrievedToken.getRoomId());
		
		
		//delete this token
		assertTrue(store.deleteToken(testToken.getTokenId()));
		
		tokens = store.listAllTokens(testToken.getStreamId(),0 , 10);
		
		//it should be zero because all tokens are revoked
		assertEquals(0, tokens.size());
		
		
		
		//create token again
		testToken = new Token();

		testToken.setStreamId("1234");
		testToken.setExpireDate(expireDate);
		testToken.setType(Token.PLAY_TOKEN);
		testToken.setTokenId("tokenID");


		store.saveToken(testToken);
		
		//validate token
		Token validatedToken = store.validateToken(testToken);

		//token should be validated and returned
		assertNotNull(validatedToken);

		//this should be false, because validated token is deleted after consumed
		Token expiredToken = store.validateToken(testToken);

		assertNull(expiredToken);

		
		//create token again, this time create a room token
		testToken = new Token();

		testToken.setStreamId("1234");
		testToken.setExpireDate(expireDate);
		testToken.setType(Token.PLAY_TOKEN);
		testToken.setTokenId("tokenID");
		testToken.setRoomId("testRoom");

		store.saveToken(testToken);
		
		//validate token
		validatedToken = store.validateToken(testToken);

		//token should be validated and returned
		assertNotNull(validatedToken);

		//this is again not null, because validated token is not deleted because it is a room token
		expiredToken = store.validateToken(testToken);

		assertNotNull(expiredToken);
		
		//change stream id of token
		
		testToken.setStreamId("changed");
		
		//validate token
		validatedToken = store.validateToken(testToken);

		//token should be validated and returned
		assertNotNull(validatedToken);
		
		
	}

	@Test
	public void testDontWriteStatsToDB () {
		DataStore ds = createDB("memorydb", false);
		assertTrue(ds instanceof InMemoryDataStore);	
		testDontWriteStatsToDB(ds);

		ds = createDB("mapdb", false);
		assertTrue(ds instanceof MapDBStore);	
		testDontWriteStatsToDB(ds);

		ds = createDB("mongodb", false);
		assertTrue(ds instanceof MongoStore);	
		testDontWriteStatsToDB(ds);


	}

	public void testDontWriteStatsToDB (DataStore dataStore) {
		testDontUpdateRtmpViewerStats(dataStore);
		testDontUpdateHLSViewerStats(dataStore);
		testDontUpdateWebRTCViewerStats(dataStore);
		testDontUpdateSourceQualityParameters(dataStore);
	}

	public void testDontUpdateRtmpViewerStats(DataStore dataStore) {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		assertFalse(dataStore.updateRtmpViewerCount(key, true));
		assertEquals(0, dataStore.get(key).getRtmpViewerCount());
	}

	public void testDontUpdateHLSViewerStats(DataStore dataStore) {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		assertFalse(dataStore.updateHLSViewerCount(key, 1));
		assertEquals(0, dataStore.get(key).getHlsViewerCount());
	}

	public void testDontUpdateWebRTCViewerStats(DataStore dataStore) {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		assertFalse(dataStore.updateWebRTCViewerCount(key, true));
		assertEquals(0, dataStore.get(key).getWebRTCViewerCount());
	}

	public void testDontUpdateSourceQualityParameters(DataStore dataStore) {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		broadcast.setQuality("poor");
		String key = dataStore.save(broadcast);
		assertFalse(dataStore.updateSourceQualityParameters(key, "good", 0, 0));
		assertEquals("poor", dataStore.get(key).getQuality());
	}

	private DataStore createDB(String type, boolean writeStats) {
		DataStoreFactory dsf = new DataStoreFactory();
		dsf.setWriteStatsToDatastore(writeStats);
		dsf.setDbType(type);
		dsf.setDbName("testdb");
		dsf.setDbHost("localhost");
		dsf.init();
		return dsf.getDataStore();
	}

	public void testClearAtStart(DataStore dataStore) {
		deleteBroadcast((MongoStore) dataStore);
		assertEquals(0, dataStore.getBroadcastCount());


		Broadcast broadcast = new Broadcast();
		broadcast.setName("test1");
		broadcast.setZombi(true);
		dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setName("test2");
		broadcast2.setZombi(true);
		dataStore.save(broadcast2);

		assertEquals(2, dataStore.getBroadcastCount());

		dataStore.clearStreamsOnThisServer(ServerSettings.getLocalHostAddress());

		assertEquals(0, dataStore.getBroadcastCount());
	}

	public void testClearAtStartCluster(DataStore dataStore) {
		
		dataStore.clearStreamsOnThisServer(ServerSettings.getLocalHostAddress());
		assertEquals(0, dataStore.getBroadcastCount());

		Broadcast broadcast = new Broadcast();
		broadcast.setOriginAdress(ServerSettings.getLocalHostAddress());
		broadcast.setName("test1");
		try {
			broadcast.setStreamId("test1");
		} catch (Exception e) {
			e.printStackTrace();
		}
		broadcast.setZombi(true);
		dataStore.save(broadcast);

		StreamInfo si = new StreamInfo();
		si.setHost(ServerSettings.getLocalHostAddress());
		si.setStreamId(broadcast.getStreamId());

		dataStore.saveStreamInfo(si);

		StreamInfo si2 = new StreamInfo();
		si2.setHost(ServerSettings.getLocalHostAddress());
		si2.setStreamId(broadcast.getStreamId());
		si2.setVideoPort(1000);
		si2.setAudioPort(1100);


		dataStore.saveStreamInfo(si2);

		dataStore.getStreamInfoList(broadcast.getStreamId());

		assertEquals(1, dataStore.getBroadcastCount());
		assertEquals(2, dataStore.getStreamInfoList(broadcast.getStreamId()).size());

		dataStore.clearStreamsOnThisServer(ServerSettings.getLocalHostAddress());

		assertEquals(0, dataStore.getBroadcastCount());
		assertEquals(0, dataStore.getStreamInfoList(broadcast.getStreamId()).size());
	}

	@Test
	public void testMongoDBSaveStreamInfo() {
		MongoStore dataStore = new MongoStore("localhost", "", "", "testdb");
		deleteStreamInfos(dataStore);

		//same ports different host => there will be 2 SIs
		saveStreamInfo(dataStore, "host1", 1000, 2000, "host2", 1000, 2000);
		assertEquals(2, dataStore.getDataStore().find(StreamInfo.class).count());
		deleteStreamInfos(dataStore);

		//different ports same host => there will be 2 SIs
		saveStreamInfo(dataStore, "host1", 1000, 2000, "host1", 1100, 2100);
		assertEquals(2, dataStore.getDataStore().find(StreamInfo.class).count());
		deleteStreamInfos(dataStore);

		//same video ports same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, "host1", 1000, 2100);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);

		//same audio ports same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, "host1", 1100, 2000);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);

		//first video port same with second audio port and same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, "host1", 1100, 1000);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);

		//first audio port same with second video port and same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, "host1", 2000, 2100);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);
	}

	public void deleteStreamInfos(MongoStore dataStore) {
		Query<StreamInfo> deleteQuery = dataStore.getDataStore().find(StreamInfo.class);
		dataStore.getDataStore().delete(deleteQuery);
	}
	
	public void deleteBroadcast(MongoStore dataStore) {
		Query<Broadcast> deleteQuery = dataStore.getDataStore().find(Broadcast.class);
		dataStore.getDataStore().delete(deleteQuery);
	}

	public void saveStreamInfo(DataStore dataStore, String host1, int videoPort1, int audioPort1,
			String host2, int videoPort2, int audioPort2) {

		StreamInfo si = new StreamInfo();
		si.setHost(host1);
		si.setVideoPort(videoPort1);
		si.setAudioPort(audioPort1);
		si.setStreamId("test1");
		dataStore.saveStreamInfo(si);

		assertEquals(1, dataStore.getStreamInfoList("test1").size());

		si = new StreamInfo();
		si.setHost(host2);
		si.setVideoPort(videoPort2);
		si.setAudioPort(audioPort2);
		si.setStreamId("test2");
		dataStore.saveStreamInfo(si);
	}


	public void testConferenceRoom(DataStore datastore) {

		ConferenceRoom room = new ConferenceRoom();

		long now = Instant.now().getEpochSecond();

		room.setRoomId("roomName");
		room.setStartDate(now);
		//1 hour later
		room.setEndDate(now + 3600);

		//create room
		assertTrue(datastore.createConferenceRoom(room));

		//get room		
		ConferenceRoom dbRoom = datastore.getConferenceRoom(room.getRoomId());

		assertNotNull(dbRoom);
		assertEquals("roomName", dbRoom.getRoomId());

		dbRoom.setEndDate(now + 7200);

		//edit room
		assertTrue(datastore.editConferenceRoom(dbRoom.getRoomId(), dbRoom));


		ConferenceRoom editedRoom = datastore.getConferenceRoom(dbRoom.getRoomId());

		assertNotNull(editedRoom);
		assertEquals(now + 7200, editedRoom.getEndDate());

		//delete room
		assertTrue(datastore.deleteConferenceRoom(editedRoom.getRoomId()));

		assertNull(datastore.getConferenceRoom(editedRoom.getRoomId()));
	}
	
	/*
	 * This test is written for mongostore
	 */
	private void testStreamSourceList(DataStore dataStore) {
		deleteBroadcast((MongoStore) dataStore);
		
		Broadcast ss1 = new Broadcast("ss1");
		ss1.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss1.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		
		Broadcast ss2 = new Broadcast("ss2");
		ss2.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		
		Broadcast ss3 = new Broadcast("ss3");
		ss3.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss3.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING);
		  
		dataStore.save(ss1);
		dataStore.save(ss2);
		dataStore.save(ss3);
		
		List<Broadcast> list = dataStore.getExternalStreamsList();
		assertEquals(1, list.size());

		List<Broadcast> list2 = dataStore.getExternalStreamsList();
		assertEquals(0, list2.size());

		
	}
	
	private void testUpdateStatus(DataStore dataStore) {
		String streamId = "test";
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
		dataStore.save(broadcast);
		
		Broadcast broadcastFromStore = dataStore.get(streamId);
		assertNotNull(broadcastFromStore);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcastFromStore.getStatus());
		assertEquals(0, broadcastFromStore.getStartTime());

		long now = System.currentTimeMillis();
		dataStore.updateStatus(streamId, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		
		broadcastFromStore = dataStore.get(streamId);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcastFromStore.getStatus());
		assertTrue(Math.abs(now-broadcastFromStore.getStartTime()) < 100);
		
		//wait to be sure time changed from we set now
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		dataStore.updateStatus(streamId, AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		
		broadcastFromStore = dataStore.get(streamId);
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcastFromStore.getStatus());
		assertTrue(Math.abs(now-broadcastFromStore.getStartTime()) < 100);
	}
	
	private void testP2PConnection(DataStore dataStore) {
		String streamId = "p2pstream"+Math.random()*100;
		P2PConnection p2pConn = new P2PConnection(streamId, "dummy");
		if(dataStore instanceof MongoStore) {
			assertNull(dataStore.getP2PConnection(streamId));
			assertTrue(dataStore.createP2PConnection(p2pConn));
			P2PConnection conn = dataStore.getP2PConnection(streamId);
			assertNotNull(conn);
			assertEquals(streamId, conn.getStreamId());
			assertEquals("dummy", conn.getOriginNode());
			assertTrue(dataStore.deleteP2PConnection(streamId));
			assertNull(dataStore.getP2PConnection(streamId));
			
			
			assertFalse(dataStore.createP2PConnection(null));
			assertNull(dataStore.getP2PConnection(streamId));
			assertFalse(dataStore.deleteP2PConnection(streamId));
			
			
		}
		else {
			assertFalse(dataStore.createP2PConnection(p2pConn));
			assertNull(dataStore.getP2PConnection(streamId));
			assertFalse(dataStore.deleteP2PConnection(streamId));
		}
	}
	
	public void testUpdateLocationParams(DataStore dataStore) {
		logger.info("testUpdateLocationParams for {}", dataStore.getClass());

		String streamId = "test"+Math.random()*100;;
		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		dataStore.save(broadcast);
		
		Broadcast broadcastFromStore = dataStore.get(streamId);
		assertNull(broadcastFromStore.getLatitude());
		assertNull(broadcastFromStore.getLongitude());
		assertNull(broadcastFromStore.getAltitude());
		
		String latitude = "51.507351";
		String longitude = "-0.127758";
		String altitude = "58.58";
		
		broadcastFromStore.setLatitude(latitude);
		broadcastFromStore.setLongitude(longitude);
		broadcastFromStore.setAltitude(altitude);
		
		assertTrue(dataStore.updateBroadcastFields(streamId, broadcastFromStore));
		
		Broadcast broadcastFromStore2 = dataStore.get(streamId);
		assertEquals(latitude, broadcastFromStore2.getLatitude());
		assertEquals(longitude, broadcastFromStore2.getLongitude());
		assertEquals(altitude, broadcastFromStore2.getAltitude());
	}
	
	public void testPlaylist(DataStore dataStore) {
		
		//create a broadcast
		Broadcast broadcast=new Broadcast();
		
		List<Broadcast> broadcastList = new ArrayList<>();
		
		broadcastList.add(broadcast);
		
		Playlist playlist = new Playlist("12312",0,"playlistName",111,111,broadcastList);

		//create playlist
		assertTrue(dataStore.createPlaylist(playlist));
		
		//update playlist
		assertTrue(dataStore.editPlaylist(playlist.getPlaylistId(), playlist));

		//get new playlist		
		Playlist playlist2 = dataStore.getPlaylist(playlist.getPlaylistId());

		assertNotNull(playlist2);
		
		assertEquals("playlistName", playlist.getPlaylistName());

		//delete playlist
		assertTrue(dataStore.deletePlaylist(playlist.getPlaylistId()));

		assertNull(dataStore.getPlaylist(playlist.getPlaylistId()));
		
	}

}
