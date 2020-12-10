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
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.Playlist;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberStats;
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
		
		
		testUnexpectedBroadcastOffset(dataStore);
		testUnexpectedVodOffset(dataStore);
		
		testBugGetExternalStreamsList(dataStore);
		testGetPagination(dataStore);
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);
		testRemoveEndpoint(dataStore);
		testRemoveEndpointWithServiceEndpoint(dataStore);
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
		testTimeBasedSubscriberOperations(dataStore);
		testConferenceRoom(dataStore);
		testUpdateStatus(dataStore);
		testP2PConnection(dataStore);
		testUpdateLocationParams(dataStore);
		testPlaylist(dataStore);
		testAddTrack(dataStore);
		testClearAtStart(dataStore);
    	testGetVoDIdByStreamId(dataStore);
    	testBroadcastListSorting(dataStore);	
		testTotalWebRTCViewerCount(dataStore);
		testBroadcastListSearch(dataStore);
		testVodSearch(dataStore);
		testConferenceRoomSorting(dataStore);
		testConferenceRoomSearch(dataStore);

	}

	@Test
	public void testMemoryDataStore() {
		DataStore dataStore = new InMemoryDataStore("testdb");
		
		testUnexpectedBroadcastOffset(dataStore);
		testUnexpectedVodOffset(dataStore);
		
		testBugGetExternalStreamsList(dataStore);
		testGetPagination(dataStore);
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);
		testRemoveEndpoint(dataStore);
		testRemoveEndpointWithServiceEndpoint(dataStore);
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
		testTimeBasedSubscriberOperations(dataStore);
		testConferenceRoom(dataStore);
		testUpdateStatus(dataStore);
		testP2PConnection(dataStore);
		testUpdateLocationParams(dataStore);
		testPlaylist(dataStore);
		testAddTrack(dataStore);
		testClearAtStart(dataStore);
    	testGetVoDIdByStreamId(dataStore);
    	testBroadcastListSorting(dataStore);
		testTotalWebRTCViewerCount(dataStore);
		testBroadcastListSearch(dataStore);
		testVodSearch(dataStore);
		testConferenceRoomSorting(dataStore);
		testConferenceRoomSearch(dataStore);

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


		testUnexpectedBroadcastOffset(dataStore);
		testUnexpectedVodOffset(dataStore);		
		testBugGetExternalStreamsList(dataStore);
		testGetPagination(dataStore);
		testNullCheck(dataStore);
		testSimpleOperations(dataStore);
		testRemoveEndpoint(dataStore);
		testRemoveEndpointWithServiceEndpoint(dataStore);
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
		testTimeBasedSubscriberOperations(dataStore);
		testClearAtStart(dataStore);
		testClearAtStartCluster(dataStore);
		testConferenceRoom(dataStore);
		testStreamSourceList(dataStore);
		testUpdateStatus(dataStore);
		testP2PConnection(dataStore);
		testUpdateLocationParams(dataStore);
		testPlaylist(dataStore);
		testAddTrack(dataStore);
		testGetVoDIdByStreamId(dataStore);
		testBroadcastListSorting(dataStore);
		testTotalWebRTCViewerCount(dataStore);
		testBroadcastListSearch(dataStore);
		testVodSearch(dataStore);
		testConferenceRoomSorting(dataStore);
		testConferenceRoomSearch(dataStore);
	}
	
	@Test
	public void testBug() {
		
		MapDBStore dataStore = new MapDBStore("src/test/resources/damaged_webrtcappee.db");
		
		//Following methods does not return before the bug is fixed
		dataStore.fetchUserVodList(new File(""));
		
		dataStore.getVodList(0, 10, "name", "asc", null, null);
		dataStore.getConferenceRoomList(0, 10, "asc", null, null);
	}

	public void clear(DataStore dataStore) 
	{
		long numberOfStreams = dataStore.getBroadcastCount();
		int pageSize = 10;
		long pageCount = numberOfStreams / pageSize + ((numberOfStreams % pageSize) > 0 ? 1 : 0);
		int numberOfCall = 0;
		List<Broadcast> totalBroadcastList = new ArrayList<>();
		for (int i = 0; i < pageCount; i++) {
			totalBroadcastList.addAll(dataStore.getBroadcastList(i * pageSize, pageSize, null, null, null, null));
		}

		for (Broadcast broadcast : totalBroadcastList) {
			numberOfCall++;
			assertTrue(dataStore.delete(broadcast.getStreamId()));
		}

		assertEquals(numberOfCall, numberOfStreams);
		
		long numberOfVods = dataStore.getTotalVodNumber();
		pageSize = 50;
		pageCount = numberOfVods / pageSize + ((numberOfVods % pageSize) > 0 ? 1 : 0);
		numberOfCall = 0;
		List<VoD> totalVoDList = new ArrayList<>();
		for (int i = 0; i < pageCount; i++) {
			totalVoDList.addAll(dataStore.getVodList(i * pageSize, pageSize, null, null, null, null));
		}
		
		for (VoD vod : totalVoDList) {
			numberOfCall++;
			assertTrue(dataStore.deleteVod(vod.getVodId()));
		}

	}
	
	public void testUnexpectedVodOffset(DataStore dataStore) {
		clear(dataStore);
		
		assertEquals(0, dataStore.getTotalVodNumber());
		
		
		List<VoD> vodList = dataStore.getVodList(50, 50, null, null, null, null);
		assertNotNull(vodList);
		assertEquals(0, vodList.size());
		
		
		vodList = dataStore.getVodList(50, 0, null, null, null, null);
		assertNotNull(vodList);
		assertEquals(0, vodList.size());
		
		for (int i = 0; i < 10; i++) {
			dataStore.addVod(new VoD("stream", "111223" + (int)(Math.random() * 1000),  "path", "vod", 1517239808, 17933, 1190525, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000)));			
		}
		
		vodList = dataStore.getVodList(6, 4, null, null, null, null);
		assertNotNull(vodList);
		assertEquals(4, vodList.size());
		
		vodList = dataStore.getVodList(20, 5, null, null, null, null);
		assertNotNull(vodList);
		assertEquals(0, vodList.size());
	}
	
	public void testUnexpectedBroadcastOffset(DataStore dataStore) {
		clear(dataStore);
		
		assertEquals(0, dataStore.getBroadcastCount());
		
		
		List<Broadcast> broadcastList = dataStore.getBroadcastList(50, 50, null, null, null, null);
		assertNotNull(broadcastList);
		assertEquals(0, broadcastList.size());
		
		
		broadcastList = dataStore.getBroadcastList(50, 0, null, null, null, null);
		assertNotNull(broadcastList);
		assertEquals(0, broadcastList.size());
		
		for (int i = 0; i < 10; i++) {
			dataStore.save(new Broadcast(null, null));
		}
		
		broadcastList = dataStore.getBroadcastList(6, 4, null, null, null, null);
		assertNotNull(broadcastList);
		assertEquals(4, broadcastList.size());
		
		broadcastList = dataStore.getBroadcastList(20, 5, null, null, null, null);
		assertNotNull(broadcastList);
		assertEquals(0, broadcastList.size());
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

			List<Broadcast> broadcastList = dataStore.getBroadcastList(i * pageSize, pageSize, null, null, null, null);
			for (Broadcast broadcast : broadcastList) {
				numberOfCall++;
				assertTrue(dataStore.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));
			}

		}

		assertEquals(numberOfCall, numberOfStatusChangeStreams);
		//check that active broadcast exactly the same as changed above
		assertEquals(numberOfStatusChangeStreams, dataStore.getActiveBroadcastCount());

		//change all streams to finished
		streamCount = dataStore.getBroadcastCount();
		pageCount = streamCount / pageSize + ((streamCount % pageSize) > 0 ? 1 : 0);
		for (int i = 0; i < pageCount; i++) {

			List<Broadcast> broadcastList = dataStore.getBroadcastList(i * pageSize, pageSize, null, null, null, null);
			for (Broadcast broadcast : broadcastList) {
				assertTrue(dataStore.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED));
				assertEquals(0, broadcast.getWebRTCViewerCount());
				assertEquals(0, broadcast.getHlsViewerCount());
				assertEquals(0, broadcast.getRtmpViewerCount());
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

		List<VoD> vodList = datastore.getVodList(0, 50, null, null, null, null);
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
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
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
			
			// If broadcast finished
			
			
		}
	}

	public void testWebRTCViewerCount(DataStore dataStore) {
		//create a stream
		Broadcast broadcast = new Broadcast();
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
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
			
			if(dataStore.get(key).getWebRTCViewerCount()>0 || (dataStore.get(key).getWebRTCViewerCount()==0 && increment)) {
				assertTrue(dataStore.updateWebRTCViewerCount(key, increment));
			}
			else {
				assertFalse(dataStore.updateWebRTCViewerCount(key, increment));
				totalViewerCountFor1 = 0;
			}

			increment = false; 
			randomValue = (int)(Math.random()*99999);
			if (randomValue % 2 == 0) {
				increment = true;
				totalViewerCountFor2++;
			}
			else {
				totalViewerCountFor2--;
			}

			if(dataStore.get(key2).getWebRTCViewerCount()>0 || (dataStore.get(key2).getWebRTCViewerCount()==0 && increment)) {
				assertTrue(dataStore.updateWebRTCViewerCount(key2, increment));
			}
			else {
				assertFalse(dataStore.updateWebRTCViewerCount(key2, increment));
				totalViewerCountFor2 = 0;
			}

			assertEquals(totalViewerCountFor1, dataStore.get(key).getWebRTCViewerCount());
			assertEquals(totalViewerCountFor2, dataStore.get(key2).getWebRTCViewerCount());
		}
	}

	public void testRTMPViewerCount(DataStore dataStore) {
		//create a stream
		Broadcast broadcast = new Broadcast();
		broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
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
			
			if(dataStore.get(key).getRtmpViewerCount()>0 || (dataStore.get(key).getRtmpViewerCount()==0 && increment)) {
				assertTrue(dataStore.updateRtmpViewerCount(key, increment));
			}
			else {
				assertFalse(dataStore.updateRtmpViewerCount(key, increment));
				totalViewerCountFor1 = 0;
			}
			
			increment = false; 
			randomValue = (int)(Math.random()*99999);
			if (randomValue % 2 == 0) {
				increment = true;
				totalViewerCountFor2++;
			}
			else {
				totalViewerCountFor2--;
			}
			
			if(dataStore.get(key2).getRtmpViewerCount()>0 || (dataStore.get(key2).getRtmpViewerCount()==0 && increment)) {
				assertTrue(dataStore.updateRtmpViewerCount(key2, increment));
			}
			else {
				assertFalse(dataStore.updateRtmpViewerCount(key2, increment));
				totalViewerCountFor2 = 0;
			}
			
			assertEquals(totalViewerCountFor1, dataStore.get(key).getRtmpViewerCount());
			assertEquals(totalViewerCountFor2, dataStore.get(key2).getRtmpViewerCount());
		}
	}

	public void testGetPagination(DataStore dataStore) {

		List<Broadcast> broadcastList2 = dataStore.getBroadcastList(0, 50, null, null, null,null);
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

		List<Broadcast> broadcastList = dataStore.getBroadcastList(0, 10, null, null, null, null);
		assertNotNull(broadcastList);
		assertEquals(10, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {

			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));

			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}

		broadcastList = dataStore.getBroadcastList(10, 10, null, null, null, null);
		assertNotNull(broadcastList);
		assertEquals(10, broadcastList.size());
		for (int i = 0; i < broadcastList.size(); i++) {
			// int count = 10 + i;
			// assertEquals(count +"", broadcastList.get(i).getName());
			assertTrue(0 <= Integer.valueOf(broadcastList.get(i).getName()));

			assertTrue(36 > Integer.valueOf(broadcastList.get(i).getName()));
		}

		broadcastList = dataStore.getBroadcastList(20, 10, null, null, null, null);
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

		broadcastList = dataStore.getBroadcastList(30, 10, null, null, null, null);
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

	public void testBroadcastListSearch(DataStore dataStore){
		long broadcastCount = dataStore.getBroadcastCount();
		int pageCount = (int)(broadcastCount/50 + 1);
		
		for (int i = 0; i < pageCount; i++) {
			List<Broadcast> broadcastList2 = dataStore.getBroadcastList(0, 50, null, null, null, null);
			for (Iterator iterator = broadcastList2.iterator(); iterator.hasNext();) {
				Broadcast broadcast = (Broadcast) iterator.next();
				dataStore.delete(broadcast.getStreamId());
			}
		}
		
	

		Broadcast broadcast1 = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, "bbbStream");
		broadcast1.setDate(1000);
		broadcast1.setType(AntMediaApplicationAdapter.LIVE_STREAM);
		Broadcast broadcast2 = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, "aaaStream");
		broadcast2.setDate(100000);
		broadcast2.setType(AntMediaApplicationAdapter.IP_CAMERA); // Ip Camera
		Broadcast broadcast3 = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING, "cccStream");
		broadcast3.setDate(100000000);
		broadcast3.setType(AntMediaApplicationAdapter.STREAM_SOURCE); //Stream Source
		Broadcast broadcast4 = new Broadcast(null);
		broadcast4.setDate(100000);
		broadcast4.setType(AntMediaApplicationAdapter.LIVE_STREAM); //Null check

		Broadcast broadcast5 = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, "aaaStm");
		broadcast5.setDate(100000);
		broadcast5.setType(AntMediaApplicationAdapter.LIVE_STREAM);


		dataStore.save(broadcast1);
		dataStore.save(broadcast2);
		dataStore.save(broadcast3);
		dataStore.save(broadcast4);
		dataStore.save(broadcast5);

		long count = dataStore.getPartialBroadcastNumber("ast");
		assertEquals(2, count);

		count = dataStore.getPartialBroadcastNumber(null);
		assertEquals(5, count);

		List<Broadcast> broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, broadcast2.getStreamId());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(0).getName(), broadcast2.getName());

		broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, broadcast4.getStreamId());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast4.getStreamId());
		assertEquals(null, broadcast4.getName());

		broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, "tream");
		assertEquals(3, broadcastList.size());

		broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, "bstr");
		assertEquals(1, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(0).getName(), broadcast1.getName());

		broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, "antme");
		assertEquals(0, broadcastList.size());

	}

	public void testConferenceRoomSearch(DataStore dataStore) {
		List<ConferenceRoom> roomList2 = dataStore.getConferenceRoomList(0, 50, null, null, null);
		for (Iterator iterator = roomList2.iterator(); iterator.hasNext();) {
			ConferenceRoom room = (ConferenceRoom) iterator.next();
			dataStore.deleteConferenceRoom(room.getRoomId());
		}

		long now = Instant.now().getEpochSecond();
		//Create rooms to check sorting
		ConferenceRoom room = new ConferenceRoom();

		String roomId = "aaaroom";
		room.setRoomId(roomId);
		room.setStartDate(now);
		room.setEndDate(now + 3600);

		ConferenceRoom room2 = new ConferenceRoom();
		roomId = "bbbtahir";
		room2.setRoomId(roomId);
		room2.setStartDate(now + 150);
		room2.setEndDate(now + 360);

		ConferenceRoom room3 = new ConferenceRoom();
		roomId = "cctast";
		room3.setRoomId(roomId);
		room3.setStartDate(now + 10);
		room3.setEndDate(now + 36000);

		dataStore.createConferenceRoom(room);
		dataStore.createConferenceRoom(room2);
		dataStore.createConferenceRoom(room3);

		List<ConferenceRoom> roomList = dataStore.getConferenceRoomList(0,50,null,null,null);
		assertEquals(3, roomList.size());

		roomList = dataStore.getConferenceRoomList(0,50,"","","");
		assertEquals(3, roomList.size());

		roomList = dataStore.getConferenceRoomList(0,50,"","","ta");
		assertEquals(2, roomList.size());

		roomList = dataStore.getConferenceRoomList(0,50,"","","tahir");
		assertEquals(1, roomList.size());
		assertEquals(roomList.get(0).getRoomId(), room2.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"","","ccta");
		assertEquals(1, roomList.size());
		assertEquals(roomList.get(0).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"roomId","asc","ta");
		assertEquals(2, roomList.size());
		assertEquals(roomList.get(0).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(1).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"roomId","desc","ta");
		assertEquals(2, roomList.size());
		assertEquals(roomList.get(1).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(0).getRoomId(), room3.getRoomId());

	}

	public void testConferenceRoomSorting(DataStore dataStore){
		List<ConferenceRoom> roomList2 = dataStore.getConferenceRoomList(0, 50, null, null, null);
		for (Iterator iterator = roomList2.iterator(); iterator.hasNext();) {
			ConferenceRoom room = (ConferenceRoom) iterator.next();
			dataStore.deleteConferenceRoom(room.getRoomId());
		}

		long now = Instant.now().getEpochSecond();
		//Create rooms to check sorting
		ConferenceRoom room = new ConferenceRoom();

		String roomId = "aaaroom";
		room.setRoomId(roomId);
		room.setStartDate(now);
		room.setEndDate(now + 3600);

		ConferenceRoom room2 = new ConferenceRoom();
		roomId = "bbbroom";
		room2.setRoomId(roomId);
		room2.setStartDate(now + 150);
		room2.setEndDate(now + 360);

		ConferenceRoom room3 = new ConferenceRoom();
		roomId = "cccroom";
		room3.setRoomId(roomId);
		room3.setStartDate(now + 10);
		room3.setEndDate(now + 36000);

		dataStore.createConferenceRoom(room);
		dataStore.createConferenceRoom(room2);
		dataStore.createConferenceRoom(room3);

		List<ConferenceRoom> roomList = dataStore.getConferenceRoomList(0,50,null,null,null);
		assertEquals(3, roomList.size());

		roomList = dataStore.getConferenceRoomList(0,50,"","","");
		assertEquals(3, roomList.size());

		roomList = dataStore.getConferenceRoomList(0,50,"roomId","asc", null);
		assertEquals(3, roomList.size());
		assertEquals(roomList.get(0).getRoomId(), room.getRoomId());
		assertEquals(roomList.get(1).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(2).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"roomId","desc", null);
		assertEquals(3, roomList.size());
		assertEquals(roomList.get(2).getRoomId(), room.getRoomId());
		assertEquals(roomList.get(1).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(0).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"startDate","asc", null);
		assertEquals(3, roomList.size());
		assertEquals(roomList.get(0).getRoomId(), room.getRoomId());
		assertEquals(roomList.get(2).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(1).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"startDate","desc", null);
		assertEquals(3, roomList.size());
		assertEquals(roomList.get(2).getRoomId(), room.getRoomId());
		assertEquals(roomList.get(0).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(1).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"startDate","desc", "room");
		assertEquals(3, roomList.size());
		assertEquals(roomList.get(2).getRoomId(), room.getRoomId());
		assertEquals(roomList.get(0).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(1).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"endDate","asc", null);
		assertEquals(3, roomList.size());
		assertEquals(roomList.get(1).getRoomId(), room.getRoomId());
		assertEquals(roomList.get(0).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(2).getRoomId(), room3.getRoomId());

		roomList = dataStore.getConferenceRoomList(0,50,"endDate","desc", null);
		assertEquals(3, roomList.size());
		assertEquals(roomList.get(1).getRoomId(), room.getRoomId());
		assertEquals(roomList.get(2).getRoomId(), room2.getRoomId());
		assertEquals(roomList.get(0).getRoomId(), room3.getRoomId());

	}
	
	public void testBroadcastListSorting(DataStore dataStore) {
		
		List<Broadcast> broadcastList2 = dataStore.getBroadcastList(0, 50, null, null, null, null);
		for (Iterator iterator = broadcastList2.iterator(); iterator.hasNext();) {
			Broadcast broadcast = (Broadcast) iterator.next();
			dataStore.delete(broadcast.getStreamId());
		}

		Broadcast broadcast1 = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, "bbbStream");
		broadcast1.setDate(1000);
		broadcast1.setType(AntMediaApplicationAdapter.LIVE_STREAM);
		Broadcast broadcast2 = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, "aaaStream");
		broadcast2.setDate(100000);
		broadcast2.setType(AntMediaApplicationAdapter.IP_CAMERA); // Ip Camera
		Broadcast broadcast3 = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING, "cccStream");
		broadcast3.setDate(100000000); 
		broadcast3.setType(AntMediaApplicationAdapter.STREAM_SOURCE); //Stream Source
		
		dataStore.save(broadcast1);
		dataStore.save(broadcast2);
		dataStore.save(broadcast3);
		
		List<Broadcast> broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, null);
		assertEquals(3, broadcastList.size());
		
		broadcastList = dataStore.getBroadcastList(0, 50, "", "", "", "");
		assertEquals(3, broadcastList.size());
		
		broadcastList = dataStore.getBroadcastList(0, 50, null, "name", "asc", null);
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast3.getStreamId());

		broadcastList = dataStore.getBroadcastList(0, 50, null, "name", "asc", "Stream");
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast3.getStreamId());
		
		
		broadcastList = dataStore.getBroadcastList(0, 50, null, "name", "desc", null);
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast3.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast2.getStreamId());

		broadcastList = dataStore.getBroadcastList(0, 50, null, "name", "desc", "str");
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast3.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast2.getStreamId());
		
		
		broadcastList = dataStore.getBroadcastList(0, 50, null, "date", "asc" , null);
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast3.getStreamId());

		broadcastList = dataStore.getBroadcastList(0, 50, null, "date", "asc" , "st");
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast3.getStreamId());
		
		broadcastList = dataStore.getBroadcastList(0, 50, null, "date", "desc", null);
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast3.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast1.getStreamId());
		
		
		broadcastList = dataStore.getBroadcastList(0, 50, null, "status", "asc", null);
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast1.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast3.getStreamId());
		
		broadcastList = dataStore.getBroadcastList(0, 50, null, "status", "desc", null);
		assertEquals(3, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast3.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast2.getStreamId());
		assertEquals(broadcastList.get(2).getStreamId(), broadcast1.getStreamId());
		
		
		broadcastList = dataStore.getBroadcastList(0, 2, null, "status", "desc", null);
		assertEquals(2, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast3.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast2.getStreamId());

		broadcastList = dataStore.getBroadcastList(0, 2, null, "status", "desc", "stream");
		assertEquals(2, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast3.getStreamId());
		assertEquals(broadcastList.get(1).getStreamId(), broadcast2.getStreamId());
		
		broadcastList = dataStore.getBroadcastList(2, 3, null, "status", "desc" ,null);
		assertEquals(1, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast1.getStreamId());
		
		
		broadcastList = dataStore.getBroadcastList(-10, 100, AntMediaApplicationAdapter.IP_CAMERA, "status", "desc", null);
		assertEquals(1, broadcastList.size());
		assertEquals(broadcastList.get(0).getStreamId(), broadcast2.getStreamId());
		
		dataStore.delete(broadcast1.getStreamId());
		dataStore.delete(broadcast2.getStreamId());
		dataStore.delete(broadcast3.getStreamId());
		
		broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, null);
		assertEquals(0, broadcastList.size());
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
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint, true);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		// its size should be 1
		assertEquals(1, broadcast2.getEndPointList().size());

		// endpoint2 should be in the list, check stream id
		assertEquals(broadcast2.getEndPointList().get(0).getStreamId(), endpointStreamId);

		//
		Endpoint endPoint3Clone = new Endpoint(endPoint2.getBroadcastId(), endPoint2.getStreamId(), endPoint2.getName(),
				endPoint2.getRtmpUrl(), endPoint2.getType(), null, null);

		// remove end point2
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint3Clone, true);
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
	
	public void testRemoveEndpointWithServiceEndpoint(DataStore dataStore) {
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
				"facebook", "generic_2", null);

		result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint2);
		assertTrue(result);

		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		assertEquals(2, broadcast2.getEndPointList().size());

		// remove end point
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint, false);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertNotNull(broadcast2.getEndPointList());
		// its size should be 1
		assertEquals(1, broadcast2.getEndPointList().size());

		// endpoint2 should be in the list, check stream id
		assertEquals(broadcast2.getEndPointList().get(0).getStreamId(), endpointStreamId);

		//
		Endpoint endPoint3Clone = new Endpoint(endPoint2.getBroadcastId(), endPoint2.getStreamId(), endPoint2.getName(),
				endPoint2.getRtmpUrl(), endPoint2.getType(), "generic_2", null);

		// remove end point2
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint3Clone, false);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);

		// add new enpoints
		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint = new Endpoint("broacdast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl, "facebook", "generic_2", null);

		assertTrue(dataStore.addEndpoint(broadcast2.getStreamId(), endPoint));

		String rtmpUrl2 = "rtmp:(sdfsfskmkmkmkmf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint2 = new Endpoint("broadcast id 2", endpointStreamId, broadcast2.getName(), rtmpUrl2, "facebook", "generic_3", null);

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
			assertNull(dataStore.get(broadcast.getStreamId()).getQuality());

			Broadcast broadcast2 = dataStore.get(key);
			assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());
			assertTrue(broadcast2.isPublish());

			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcast.getStatus());
			assertEquals(0, broadcast.getStartTime());
			assertNull(broadcast.getOriginAdress());
			
			String name = "name 1";
			String description = "description 2";
			Broadcast tmp = new Broadcast();
			tmp.setName(name);
			tmp.setDescription(description);
			tmp.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			long now = System.currentTimeMillis();
			tmp.setStartTime(now);
			tmp.setOriginAdress(ServerSettings.getLocalHostAddress());
			boolean result = dataStore.updateBroadcastFields(broadcast.getStreamId(), tmp);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(description, broadcast2.getDescription());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast2.getStatus());
			assertEquals(now, broadcast2.getStartTime());
			assertEquals(ServerSettings.getLocalHostAddress(), tmp.getOriginAdress());

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
			assertEquals(0, broadcast2.getWebRTCViewerCount());
			assertEquals(0, broadcast2.getHlsViewerCount());
			assertEquals(0, broadcast2.getRtmpViewerCount());

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
			
			result = dataStore.updateSourceQualityParameters(broadcast3.getStreamId(), null, 0, 0);
			assertTrue(result);
			//it's poor because it's not updated because of null
			assertEquals("poor", dataStore.get(broadcast3.getStreamId()).getQuality());

			
			result = dataStore.updateSourceQualityParameters(broadcast3.getStreamId(), "good", 0, 0);
			assertTrue(result);
			assertEquals("good", dataStore.get(broadcast3.getStreamId()).getQuality());

			//set mp4 muxing to true
			result = dataStore.setMp4Muxing(key, MuxAdaptor.RECORDING_ENABLED_FOR_STREAM);

			//check that setting is saved
			assertTrue(result);

			//check that setting is saved correctly
			assertEquals(MuxAdaptor.RECORDING_ENABLED_FOR_STREAM, dataStore.get(key).getMp4Enabled());


			//check null case
			result = dataStore.setMp4Muxing(null, MuxAdaptor.RECORDING_DISABLED_FOR_STREAM);

			assertFalse(result);


			//set mp4 muxing to false
			result = dataStore.setMp4Muxing(key, MuxAdaptor.RECORDING_DISABLED_FOR_STREAM);

			//check that setting is saved
			assertTrue(result);

			//check that setting is saved correctly
			assertEquals(MuxAdaptor.RECORDING_DISABLED_FOR_STREAM, dataStore.get(key).getMp4Enabled());

			//set webm muxing to true
			result = dataStore.setWebMMuxing(key, MuxAdaptor.RECORDING_ENABLED_FOR_STREAM);

			//check that setting is saved
			assertTrue(result);

			//check that setting is saved correctly
			assertEquals(MuxAdaptor.RECORDING_ENABLED_FOR_STREAM, dataStore.get(key).getWebMEnabled());


			//check null case
			result = dataStore.setWebMMuxing(null, MuxAdaptor.RECORDING_DISABLED_FOR_STREAM);

			assertFalse(result);


			//set webm muxing to false
			result = dataStore.setWebMMuxing(key, MuxAdaptor.RECORDING_DISABLED_FOR_STREAM);

			//check that setting is saved
			assertTrue(result);

			//check that setting is saved correctly
			assertEquals(MuxAdaptor.RECORDING_DISABLED_FOR_STREAM, dataStore.get(key).getWebMEnabled());

			
			result = dataStore.delete(key);
			assertTrue(result);

			assertNull(dataStore.get(key));




		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

	private void testVodSearch(DataStore dataStore){
		clear(dataStore);

		VoD newVod =  new VoD("streamName", "1112233" + (int)(Math.random() * 1000), "path", "aaVod", 1517239908, 17933, 1190425, VoD.STREAM_VOD, "1149253" + (int)(Math.random() * 91000));
		VoD newVod2 = new VoD("oguz", "123456" + (int)(Math.random() * 1000),  "path", "cCVod", 1517239708, 17933, 1190625, VoD.STREAM_VOD, "11503943" + (int)(Math.random() * 91000));
		VoD newVod3 = new VoD("ahmet", "2341" + (int)(Math.random() * 1000),  "path", "TahIr", 1517239608, 17933, 1190725, VoD.STREAM_VOD, "11259243" + (int)(Math.random() * 91000));
		VoD newVod4 = new VoD(null, null,  "path", null, 1517239608, 17933, 1190725, VoD.STREAM_VOD, "11827485" + (int)(Math.random() * 91000));
		VoD newVod5 = new VoD("denem", null,  "path", null, 1517239608, 17933, 1190725, VoD.STREAM_VOD, null);

		dataStore.addVod(newVod);
		dataStore.addVod(newVod2);
		dataStore.addVod(newVod3);
		dataStore.addVod(newVod4);
		dataStore.addVod(newVod5);

		long totalVodNumber = dataStore.getTotalVodNumber();
		assertEquals(5, totalVodNumber);

		long partialVodNumber = dataStore.getPartialVodNumber("vod");
		assertEquals(2, partialVodNumber);

		partialVodNumber = dataStore.getPartialVodNumber(null);
		assertEquals(5, partialVodNumber);

		List<VoD> vodList = dataStore.getVodList(0, 50, null, null, null, newVod4.getVodId());
		assertEquals(1, vodList.size());
		assertEquals(newVod4.getVodName(), vodList.get(0).getVodName());
		assertEquals(newVod4.getStreamName(), vodList.get(0).getStreamName());
		assertEquals(newVod4.getStreamId(), vodList.get(0).getStreamId());
		assertEquals(newVod4.getVodId(), vodList.get(0).getVodId());

		vodList = dataStore.getVodList(0, 50, null, null, null, newVod5.getVodId());
		assertNotNull(newVod5.getVodId());
		assertEquals(1, vodList.size()); // VodId should never come null even if initialized as null.
		assertEquals(vodList.get(0).getVodId(), newVod5.getVodId());
		assertNull(vodList.get(0).getVodName());

		vodList = dataStore.getVodList(0, 50, null, null, null, newVod.getVodName());
		assertEquals(1, vodList.size());
		assertEquals(newVod.getVodName(), vodList.get(0).getVodName());
		assertEquals(newVod.getStreamName(), vodList.get(0).getStreamName());
		assertEquals(newVod.getStreamId(), vodList.get(0).getStreamId());
		assertEquals(newVod.getVodId(), vodList.get(0).getVodId());

		vodList = dataStore.getVodList(0, 50, null, null, null, newVod.getStreamId());
		assertEquals(1, vodList.size());
		assertEquals(newVod.getVodName(), vodList.get(0).getVodName());
		assertEquals(newVod.getStreamName(), vodList.get(0).getStreamName());
		assertEquals(newVod.getStreamId(), vodList.get(0).getStreamId());
		assertEquals(newVod.getVodId(), vodList.get(0).getVodId());

		vodList = dataStore.getVodList(0, 50, null, null, null, newVod2.getVodId());
		assertEquals(1, vodList.size());
		assertEquals(newVod2.getVodName(), vodList.get(0).getVodName());
		assertEquals(newVod2.getStreamName(), vodList.get(0).getStreamName());
		assertEquals(newVod2.getStreamId(), vodList.get(0).getStreamId());
		assertEquals(newVod2.getVodId(), vodList.get(0).getVodId());

		vodList = dataStore.getVodList(0, 50, null, null, null, "vod");
		assertEquals(2, vodList.size());

		vodList = dataStore.getVodList(0, 50, null, null, null, "ahir");
		assertEquals(1, vodList.size());
		assertEquals(newVod3.getVodName(), vodList.get(0).getVodName());
		assertEquals(newVod3.getStreamName(), vodList.get(0).getStreamName());
		assertEquals(newVod3.getStreamId(), vodList.get(0).getStreamId());
		assertEquals(newVod3.getVodId(), vodList.get(0).getVodId());

		vodList = dataStore.getVodList(0, 50, null, null, null, "vassdfsdgs");
		assertEquals(0, vodList.size());

	}


	private void testFilterSearchOperations(DataStore dataStore) {
		
		clear(dataStore);

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

		List<Broadcast> returnList = dataStore.getBroadcastList(0, 10, "ipCamera", null, null, null);

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
		
		double minX = 5.5;
		double minY = 4.4;
		double maxX = 3.3;
		double maxY = 2.2;

		List<TensorFlowObject> detectedObjects = new ArrayList<>();
		TensorFlowObject tfObject = new TensorFlowObject(item1, probability1, "imageId");
		tfObject.setMinX(minX);
		tfObject.setMinY(minY);
		tfObject.setMaxX(maxX);
		tfObject.setMaxY(maxY);

		detectedObjects.add(tfObject);
		dataStore.saveDetection("id", detectionTime, detectedObjects);

		List<TensorFlowObject> list = dataStore.getDetectionList("id", 0, 10);
		assertEquals(1,list.size());
		assertEquals(item1, list.get(0).objectName);
		assertEquals(probability1, list.get(0).probability,0.1F);
		assertEquals(detectionTime, list.get(0).detectionTime);	
		
		assertEquals(minX, list.get(0).getMinX(), 0.0001);	
		assertEquals(minY, list.get(0).getMinY(), 0.0001);	
		assertEquals(maxX, list.get(0).getMaxX(), 0.0001);	
		assertEquals(maxY, list.get(0).getMaxY(), 0.0001);	
	}

	public void testTokenOperations(DataStore store) {

		//create token
		Token testToken = new Token();
		
		//define a valid expire date
		long expireDate = Instant.now().getEpochSecond() + 1000;

		Random r = new Random();
		String streamId = "streamId" + r.nextInt();
		testToken.setStreamId(streamId);
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

		testToken.setStreamId(streamId);
		testToken.setExpireDate(expireDate);
		testToken.setType(Token.PLAY_TOKEN);
		String tokenId = "tokenId" + (int)(Math.random()*99999);
		testToken.setTokenId(tokenId);
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

		testToken.setStreamId(streamId);
		testToken.setExpireDate(expireDate);
		testToken.setType(Token.PLAY_TOKEN);
		testToken.setTokenId("tokenID" + r.nextInt());


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

		testToken.setStreamId(streamId);
		testToken.setExpireDate(expireDate);
		testToken.setType(Token.PLAY_TOKEN);
		testToken.setTokenId("tokenID" + r.nextInt());
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
	
	public void testTimeBasedSubscriberOperations(DataStore store) {
		// clean db in the begining of the test
		String streamId = "stream1";
		store.revokeSubscribers(streamId);
		// null checks
		assertFalse(store.addSubscriber("stream1", null));
		
		assertFalse(store.isSubscriberConnected("stream1", null));
		assertNull(store.getSubscriber("stream1", null));
		assertFalse(store.addSubscriberConnectionEvent("stream1", null, null));
		
		
		// create a subscriber play
		Subscriber subscriberPlay = new Subscriber();
		subscriberPlay.setStreamId(streamId);
		subscriberPlay.setSubscriberId("subscriber1");
		subscriberPlay.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
		subscriberPlay.setType(Subscriber.PLAY_TYPE);
		assertTrue(store.addSubscriber(subscriberPlay.getStreamId(), subscriberPlay));
		
		// create a subscriber publish
		Subscriber subscriberPub = new Subscriber();
		subscriberPub.setStreamId(streamId);
		subscriberPub.setSubscriberId("subscriber2");
		subscriberPub.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
		subscriberPub.setType(Subscriber.PUBLISH_TYPE);
		assertTrue(store.addSubscriber(subscriberPub.getStreamId(), subscriberPub));
		
		//get subscribers of stream
		List <Subscriber> subscribers = store.listAllSubscribers(streamId, 0, 10);
		assertEquals(2, subscribers.size());
		List <SubscriberStats> subscriberStats = store.listAllSubscriberStats(streamId, 0, 10);
		assertEquals(2, subscriberStats.size());
		
		//revoke subscribers
		store.revokeSubscribers(subscriberPlay.getStreamId());

		//get subscribers of stream
		subscribers = store.listAllSubscribers(streamId, 0, 10);
		subscriberStats = store.listAllSubscriberStats(streamId, 0, 10);
		
		
		//it should be zero because all subscribers are revoked
		assertEquals(0, subscribers.size());
		assertEquals(0, subscriberStats.size());
		
		//create subscriber again
		assertTrue(store.addSubscriber(subscriberPub.getStreamId(), subscriberPub));

		//get this subscriber
		Subscriber written = store.getSubscriber(subscriberPub.getStreamId(), subscriberPub.getSubscriberId());
		
		assertNotNull(written);
		assertEquals(subscriberPub.getSubscriberId(), written.getSubscriberId());
		assertEquals(subscriberPub.getType(), written.getType());
		
		//delete this subscriber
		assertTrue(store.deleteSubscriber(streamId, written.getSubscriberId()));
		
		subscribers = store.listAllSubscribers(streamId, 0, 10);
		subscriberStats = store.listAllSubscriberStats(streamId, 0, 10);
		
		//it should be zero because subscriber is deleted
		assertEquals(0, subscribers.size());
		assertEquals(0, subscriberStats.size());
		
		//create subscriber again
		assertTrue(store.addSubscriber(subscriberPlay.getStreamId(), subscriberPlay));

		ConnectionEvent connected = new ConnectionEvent();
		connected.setEventType(ConnectionEvent.CONNECTED_EVENT);
		long eventTime = 20;
		connected.setTimestamp(eventTime);
		
		ConnectionEvent disconnected = new ConnectionEvent();
		disconnected.setEventType(ConnectionEvent.DISCONNECTED_EVENT);
		eventTime = 21;
		disconnected.setTimestamp(eventTime);		
		
		// add connected event
		store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), connected);
		// isConnected should be true
		assertTrue(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		
		// add disconnected event
		store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), disconnected);
		written = store.getSubscriber(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId());
		
		// isConnected should return false
		assertFalse(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		assertFalse(written.isConnected());
		
		// there should be two events with correct order
		List<ConnectionEvent> events = written.getStats().getConnectionEvents();
		assertEquals(2, events.size());  
		
		assertEquals(ConnectionEvent.CONNECTED_EVENT, events.get(0).getEventType());
		assertEquals(ConnectionEvent.DISCONNECTED_EVENT, events.get(1).getEventType());
		
		// add connected event
		store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), connected);
		// isConnected should be true again
		assertTrue(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		
		// reset connection status
		store.resetSubscribersConnectedStatus();
		// connection status should false again
		assertFalse(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		
		store.revokeSubscribers(streamId);
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
		
		if (dataStore instanceof MongoStore) {
			deleteBroadcast((MongoStore) dataStore);
			assertEquals(0, dataStore.getBroadcastCount());
		}
		else  {
			long broadcastCount = dataStore.getBroadcastCount();
			System.out.println("broadcast count: " + broadcastCount);
			int j = 0;
			List<Broadcast> broadcastList;
			while ((broadcastList = dataStore.getBroadcastList(0, 50, null, null, null, null)) != null)
			{
				if (broadcastList.size() == 0) {
					break;
				}
				for (Broadcast broadcast : broadcastList) {
					assertTrue(dataStore.delete(broadcast.getStreamId()));
					
				}
			}
			
		}
		
		assertEquals(0, dataStore.getBroadcastCount());
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test1");
		broadcast.setZombi(true);
		dataStore.save(broadcast);

		Broadcast broadcast2 = new Broadcast();
		broadcast2.setName("test2");
		broadcast2.setZombi(true);
		dataStore.save(broadcast2);
		
		Broadcast broadcast3 = new Broadcast();
		broadcast3.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);	
		broadcast3.setWebRTCViewerCount(104);
		broadcast3.setHlsViewerCount(305);
		broadcast3.setRtmpViewerCount(506);
		dataStore.save(broadcast3);
		
		Broadcast broadcast4 = new Broadcast();
		broadcast4.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING);
		broadcast4.setWebRTCViewerCount(10);
		broadcast4.setHlsViewerCount(30);
		broadcast4.setRtmpViewerCount(50);
		dataStore.save(broadcast4);
		
		assertEquals(4, dataStore.getBroadcastCount());

		dataStore.resetBroadcasts(ServerSettings.getLocalHostAddress());

		assertEquals(2, dataStore.getBroadcastCount());
		List<Broadcast> broadcastList = dataStore.getBroadcastList(0, 10, null, null, null, null);
		for (Broadcast tmp : broadcastList) {
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, tmp.getStatus());
			assertEquals(0, tmp.getWebRTCViewerCount());
			assertEquals(0, tmp.getHlsViewerCount());
			assertEquals(0, tmp.getRtmpViewerCount());
		}
		
	}

	public void testClearAtStartCluster(DataStore dataStore) {
		
		
		deleteBroadcast((MongoStore) dataStore);
		
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

		dataStore.resetBroadcasts(ServerSettings.getLocalHostAddress());

		assertEquals(0, dataStore.getBroadcastCount());
		assertEquals(0, dataStore.getStreamInfoList(broadcast.getStreamId()).size());
		
	}

	@Test
	public void testMongoDBSaveStreamInfo() {
		MongoStore dataStore = new MongoStore("localhost", "", "", "testdb");
		deleteStreamInfos(dataStore);

		//same ports different host => there will be 2 SIs
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host2", 1000, 2000, 0);
		assertEquals(2, dataStore.getDataStore().find(StreamInfo.class).count());
		deleteStreamInfos(dataStore);

		//different ports same host => there will be 2 SIs
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host1", 1100, 2100, 0);
		assertEquals(2, dataStore.getDataStore().find(StreamInfo.class).count());
		deleteStreamInfos(dataStore);

		//same video ports same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host1", 1000, 2100, 0);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);

		//same audio ports same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host1", 1100, 2000, 0);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);

		//first video port same with second audio port and same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host1", 1100, 1000, 0);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);

		//first audio port same with second video port and same host => first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host1", 2000, 2100, 0);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);
		
		//host and port duplication exist so first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, 2100, "host1", 2000, 2100, 3000);
		assertEquals(1, dataStore.getDataStore().find(StreamInfo.class).count());
		assertTrue(dataStore.getStreamInfoList("test1").isEmpty());
		deleteStreamInfos(dataStore);
		
		//host and port duplication exist so first SI should be deleted
		saveStreamInfo(dataStore, "host1", 1000, 2000, 3000, "host1", 4000, 5000, 1000);
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

	public void saveStreamInfo(DataStore dataStore, String host1, int videoPort1, int audioPort1, int dataPort1,
			String host2, int videoPort2, int audioPort2, int dataPort2) {

		StreamInfo si = new StreamInfo();
		si.setHost(host1);
		si.setVideoPort(videoPort1);
		si.setAudioPort(audioPort1);
		si.setDataChannelPort(dataPort1);
		si.setStreamId("test1");
		dataStore.saveStreamInfo(si);

		assertEquals(1, dataStore.getStreamInfoList("test1").size());

		si = new StreamInfo();
		si.setHost(host2);
		si.setVideoPort(videoPort2);
		si.setAudioPort(audioPort2);
		si.setDataChannelPort(dataPort2);
		si.setStreamId("test2");
		dataStore.saveStreamInfo(si);
	}


	public void testConferenceRoom(DataStore datastore) {

		ConferenceRoom room = new ConferenceRoom();

		long now = Instant.now().getEpochSecond();

		String roomId = "roomId" + RandomStringUtils.random(10);
		room.setRoomId(roomId);
		room.setStartDate(now);
		//1 hour later
		room.setEndDate(now + 3600);

		//create room
		assertTrue(datastore.createConferenceRoom(room));

		//get room		
		ConferenceRoom dbRoom = datastore.getConferenceRoom(room.getRoomId());

		//test null
		ConferenceRoom nullRoom = datastore.getConferenceRoom(null);
		assertNull(nullRoom);

		assertNotNull(dbRoom);
		assertEquals(roomId, dbRoom.getRoomId());

		dbRoom.setEndDate(now + 7200);

		//edit room
		assertTrue(datastore.editConferenceRoom(dbRoom.getRoomId(), dbRoom));
		
		ConferenceRoom conferenceRoom = datastore.getConferenceRoom("room_not_exist");
		assertNull(conferenceRoom);
		
		assertFalse(datastore.editConferenceRoom("room_not_exist", dbRoom));


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
		
		broadcast.setWebRTCViewerCount(10);
		broadcast.setHlsViewerCount(1000);
		broadcast.setRtmpViewerCount(100);
		
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
		
		assertEquals(0, broadcastFromStore.getWebRTCViewerCount());
		assertEquals(0, broadcastFromStore.getRtmpViewerCount());
		assertEquals(0, broadcastFromStore.getHlsViewerCount());
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

		String streamId = "test"+Math.random()*100;
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
		
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcastFromStore.getStatus());
		
		broadcastFromStore.setLatitude(latitude);
		broadcastFromStore.setLongitude(longitude);
		broadcastFromStore.setAltitude(altitude);
		broadcastFromStore.setStatus(null);
		assertTrue(dataStore.updateBroadcastFields(streamId, broadcastFromStore));
		
		Broadcast broadcastFromStore2 = dataStore.get(streamId);
		assertEquals(latitude, broadcastFromStore2.getLatitude());
		assertEquals(longitude, broadcastFromStore2.getLongitude());
		assertEquals(altitude, broadcastFromStore2.getAltitude());
		
		if (!(dataStore instanceof InMemoryDataStore)) {
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED, broadcastFromStore2.getStatus());
		}
	}
	
	public void testPlaylist(DataStore dataStore) {
		
		//create a broadcast
		Broadcast broadcast=new Broadcast();
		
		List<Broadcast> broadcastList = new ArrayList<>();
		
		broadcastList.add(broadcast);
		
		Playlist playlist = new Playlist("12312",0,"playlistName",AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED,111,111,broadcastList);

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

	public void testAddTrack(DataStore dataStore) {

		String mainTrackId = RandomStringUtils.randomAlphanumeric(8);
		String subTrackId = RandomStringUtils.randomAlphanumeric(8);

		Broadcast mainTrack= new Broadcast();
		try {
			mainTrack.setStreamId(mainTrackId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Broadcast subtrack= new Broadcast();
		try {
			subtrack.setStreamId(subTrackId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		dataStore.save(mainTrack);
		dataStore.save(subtrack);

		assertNull(mainTrack.getSubTrackStreamIds());
		assertNull(subtrack.getMainTrackStreamId());

		subtrack.setMainTrackStreamId(mainTrackId);
		assertTrue(dataStore.updateBroadcastFields(subTrackId, subtrack));

		dataStore.addSubTrack(mainTrackId, subTrackId);
		mainTrack = dataStore.get(mainTrackId);
		subtrack = dataStore.get(subTrackId);
		assertEquals(1, mainTrack.getSubTrackStreamIds().size());
		assertEquals(subTrackId, mainTrack.getSubTrackStreamIds().get(0));
		assertEquals(mainTrackId, subtrack.getMainTrackStreamId());

	}
	public void testGetVoDIdByStreamId(DataStore dataStore) {
		String streamId=RandomStringUtils.randomNumeric(24);
		String vodId1="vod_1";
		String vodId2="vod_2";
		String vodId3="vod_3";
		VoD vod1 = new VoD("streamName", streamId, "filePath", "vodName2", 333, 111, 111, VoD.STREAM_VOD, vodId1);
		VoD vod2 = new VoD("streamName", streamId, "filePath", "vodName1", 222, 111, 111, VoD.STREAM_VOD, vodId2);
		VoD vod3 = new VoD("streamName", "streamId123", "filePath", "vodName3", 111, 111, 111, VoD.STREAM_VOD, vodId3);

		dataStore.addVod(vod1);
		dataStore.addVod(vod2);
		dataStore.addVod(vod3);

		List<VoD> vodResult = dataStore.getVodList(0, 50, null, null, streamId, null);

		boolean vod1Match = false, vod2Match = false;
		for (VoD vod : vodResult) {
			if (vod.getVodId().equals(vod1.getVodId())) {
				vod1Match = true;
			}
			else if (vod.getVodId().equals(vod2.getVodId())) {
				vod2Match = true;
			}
			else if (vod.getVodId().equals(vod3.getVodId())) {
				fail("vod3 should not be matched");
			}
		}
		assertTrue(vod1Match);
		assertTrue(vod2Match);
	}

	
	public void testTotalWebRTCViewerCount(DataStore dataStore) {
		int total = 0;
		for (int i = 0; i < 150; i++) {
			Broadcast broadcast = new Broadcast();
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setName("test"+i);
			int count = RandomUtils.nextInt(0, 50);
			total += count;
			broadcast.setWebRTCViewerCount(count);
			dataStore.save(broadcast);
		}
		
		assertEquals(total, dataStore.getTotalWebRTCViewersCount());	
		
		int total2 = 0;
		for (int i = 0; i < 150; i++) {
			Broadcast broadcast = new Broadcast();
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setName("test"+i);
			int count = RandomUtils.nextInt(0, 50);
			total2 += count;
			broadcast.setWebRTCViewerCount(count);
			dataStore.save(broadcast);
		}
		
		//totalWebRTCViewersCount is still total but not total+total2 due to cache
		assertEquals(total, dataStore.getTotalWebRTCViewersCount());	
		

		int finalTotal = total+total2;
		
		//Alter cache time it solud be total+total2
		Awaitility.await().atMost(DataStore.TOTAL_WEBRTC_VIEWER_COUNT_CACHE_TIME+1100, TimeUnit.MILLISECONDS)
			.pollDelay(1000, TimeUnit.MILLISECONDS)
			.until(() -> (finalTotal == dataStore.getTotalWebRTCViewersCount()));
	}	
}
