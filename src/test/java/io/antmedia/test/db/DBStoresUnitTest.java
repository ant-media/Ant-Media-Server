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
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.query.filters.Filters;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.DataStore;
import io.antmedia.datastore.db.DataStoreFactory;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.MongoStore;
import io.antmedia.datastore.db.RedisStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.datastore.db.types.ConferenceRoom;
import io.antmedia.datastore.db.types.ConnectionEvent;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.P2PConnection;
import io.antmedia.datastore.db.types.PushNotificationToken;
import io.antmedia.datastore.db.types.StreamInfo;
import io.antmedia.datastore.db.types.Subscriber;
import io.antmedia.datastore.db.types.SubscriberMetadata;
import io.antmedia.datastore.db.types.SubscriberStats;
import io.antmedia.datastore.db.types.TensorFlowObject;
import io.antmedia.datastore.db.types.Token;
import io.antmedia.datastore.db.types.VoD;
import io.antmedia.datastore.db.types.WebRTCViewerInfo;
import io.antmedia.muxer.IAntMediaStreamHandler;
import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.pushnotification.IPushNotificationService.PushNotificationServiceTypes;
import io.antmedia.settings.ServerSettings;
import io.vertx.core.Vertx;

public class DBStoresUnitTest {

	protected static Logger logger = LoggerFactory.getLogger(DBStoresUnitTest.class);

	private Vertx vertx = Vertx.vertx();

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
	public void testMapDBStore() throws Exception {

		DataStore dataStore = new MapDBStore("testdb", vertx);
		
		
		testSubscriberMetaData(dataStore);
		testGetActiveBroadcastCount(dataStore);
		testBlockSubscriber(dataStore);
		testBugFreeStreamId(dataStore);
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
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);
		testUpdateMetadata(dataStore);	
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
		testRemoveTrack(dataStore);
		testClearAtStart(dataStore);
    	testGetVoDIdByStreamId(dataStore);
    	testBroadcastListSorting(dataStore);
		testFullTextSearch(dataStore);
		testTotalWebRTCViewerCount(dataStore);
		testBroadcastListSearch(dataStore);
		testVodSearch(dataStore);
		testConferenceRoomSorting(dataStore);
		testConferenceRoomSearch(dataStore);
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);
		testStreamSourceList(dataStore);
		
		dataStore.close(false);
		

	}
	
	@Test
	public void testMapDBPersistent() {
		DataStore dataStore = new MapDBStore("testdb", vertx);
		
		Broadcast broadcast = new Broadcast(null, null);
		String key = dataStore.save(broadcast);
		
		assertNotNull(key);
		assertNotNull(broadcast.getStreamId());

		assertEquals(broadcast.getStreamId().toString(), key);
		assertNull(dataStore.get(broadcast.getStreamId()).getQuality());

		Broadcast broadcast2 = dataStore.get(key);
		assertEquals(broadcast.getStreamId(), broadcast2.getStreamId());
		assertTrue(broadcast2.isPublish());
		
		
		dataStore.close(false);
		
		dataStore = new MapDBStore("testdb", vertx);
		Broadcast broadcast3 = dataStore.get(key);
		assertEquals(broadcast.getStreamId(), broadcast3.getStreamId());
		assertTrue(broadcast3.isPublish());
		
		dataStore.close(false);
		
	}

	@Test
	public void testMemoryDataStore() throws Exception {
		DataStore dataStore = new InMemoryDataStore("testdb");
		
		
		testSubscriberMetaData(dataStore);
		testBlockSubscriber(dataStore);
		testBugFreeStreamId(dataStore);
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
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);
		testUpdateMetadata(dataStore);
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
		testRemoveTrack(dataStore);
		testClearAtStart(dataStore);
    	testGetVoDIdByStreamId(dataStore);
    	testBroadcastListSorting(dataStore);
		testFullTextSearch(dataStore);
		testTotalWebRTCViewerCount(dataStore);
		testBroadcastListSearch(dataStore);
		testVodSearch(dataStore);
		testConferenceRoomSorting(dataStore);
		testConferenceRoomSearch(dataStore);
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);
		testStreamSourceList(dataStore);

		dataStore.close(false);


	}


	@Test
	public void testMongoStore() throws Exception {

		DataStore dataStore = new MongoStore("127.0.0.1", "", "", "testdb");
		//delete db
		dataStore.close(true);
		
		dataStore = new MongoStore("127.0.0.1", "", "", "testdb");

		testSubscriberMetaData(dataStore);
		testBlockSubscriber(dataStore);
		testTimeBasedSubscriberOperations(dataStore);
		testBugFreeStreamId(dataStore);
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
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);
		testUpdateMetadata(dataStore);
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
		testAddTrack(dataStore);
		testRemoveTrack(dataStore);
		testGetVoDIdByStreamId(dataStore);
		testBroadcastListSorting(dataStore);
		testFullTextSearch(dataStore);
		testTotalWebRTCViewerCount(dataStore);
		testBroadcastListSearch(dataStore);
		testVodSearch(dataStore);
		testConferenceRoomSorting(dataStore);
		testConferenceRoomSearch(dataStore);
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);
		
		dataStore.close(true);
	}
	
	@Test
	public void testRedisStore() throws Exception {

		DataStore dataStore = new RedisStore("redis://127.0.0.1:6379", "testdb");
		//delete db
		dataStore.close(true);
		dataStore = new RedisStore("redis://127.0.0.1:6379", "testdb");
		
		testSubscriberMetaData(dataStore);
		testBlockSubscriber(dataStore);
		testBugFreeStreamId(dataStore);
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
		testVoDFunctions(dataStore);
		testSaveStreamInDirectory(dataStore);
		testEditCameraInfo(dataStore);
		testUpdateMetadata(dataStore);
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
		testFullTextSearch(dataStore);
		testTotalWebRTCViewerCount(dataStore);
		testBroadcastListSearch(dataStore);
		testVodSearch(dataStore);
		testConferenceRoomSorting(dataStore);
		testConferenceRoomSearch(dataStore);
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);
		
		dataStore.close(true);
	}
	
	@Test
	public void testBug() {
		
		MapDBStore dataStore = new MapDBStore("src/test/resources/damaged_webrtcappee.db", vertx);
		
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
			assertNotNull(dataStore.addVod(new VoD("stream", "111223" + (int)(Math.random() * 9100000),  "path", "vod", 1517239808, 111, 17933, 1190525, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000), null)));
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


		long streamCount = 10 + (int)(Math.random()  * 500);


		System.out.println("Stream count to be added: " + streamCount);

		for (int i = 0; i < streamCount; i++) {
			dataStore.save(new Broadcast(null, null));
		}

		assertEquals(streamCount, dataStore.getBroadcastCount());

		//check that no active broadcast exist
		assertEquals(0, dataStore.getActiveBroadcastCount());

		//change random number of streams status to broadcasting
		long numberOfStatusChangeStreams = 10 + (int)(Math.random() * 500);
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
				logger.info("Updating streamId:{}", broadcast.getStreamId());  //log stream and check it if all of them different when this test fails
				assertTrue(dataStore.updateStatus(broadcast.getStreamId(), AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));
			}

		}

		assertTrue(numberOfCall > 0);
		assertEquals(numberOfCall, numberOfStatusChangeStreams);
		//check that active broadcast exactly the same as changed above
		
		//////this test is sometimes failing below, I think streamId may not be unique so I logged above to confirm it - mekya
		assertEquals(numberOfStatusChangeStreams, dataStore.getActiveBroadcastCount());
		
		assertEquals(numberOfStatusChangeStreams, dataStore.getLocalLiveBroadcastCount(ServerSettings.getLocalHostAddress()));
		
		List<Broadcast> localLiveBroadcasts = dataStore.getLocalLiveBroadcasts(ServerSettings.getLocalHostAddress());
		assertEquals(numberOfStatusChangeStreams, localLiveBroadcasts.size());

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
		assertEquals(0, dataStore.getLocalLiveBroadcastCount(ServerSettings.getLocalHostAddress()));
		localLiveBroadcasts = dataStore.getLocalLiveBroadcasts(ServerSettings.getLocalHostAddress());
		assertEquals(0, localLiveBroadcasts.size());


		
	}
	
	
	public void testBugFreeStreamId(DataStore datastore) {
		// add ip camera 
		Broadcast broadcast = new Broadcast();
		
		try 
		{
			broadcast.setStreamId("");
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		String streamId = datastore.save(broadcast);
		assertNotEquals("", streamId);
		
		
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
		
		streamsList = datastore.getExternalStreamsList();
		assertNotNull(streamsList);

		assertEquals(0, streamsList.size());

		//check that there are two streams and values are same as added above

	}

	public void testSaveStreamInDirectory(DataStore datastore) {


		File f = new File("src/test/resources");

		long totalVodCount = datastore.getTotalVodNumber();
		assertEquals(0, totalVodCount);
		assertEquals(7, datastore.fetchUserVodList(f));

		//we know there are files there
		//test_short.flv
		//test_video_360p_subtitle.flv
		//test_Video_360p.flv
		//test.flv
		//sample_MP4_480.mp4
		//high_profile_delayed_video.flv
		//test_video_360p_pcm_audio.mkv

		totalVodCount = datastore.getTotalVodNumber();
		assertEquals(7, totalVodCount);

		List<VoD> vodList = datastore.getVodList(0, 50, null, null, null, null);
		assertEquals(7, vodList.size());
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
		VoD streamVod=new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, 111, VoD.STREAM_VOD,vodId,null);

		//save stream vod

		datastore.addVod(streamVod);

		//check vod number

		assertEquals(1, datastore.getTotalVodNumber());
		VoD voD = datastore.getVoD(vodId);
		assertEquals(streamVod.getFilePath(), voD.getFilePath());
		assertEquals(streamVod.getStreamId(), voD.getStreamId());
		assertEquals(streamVod.getStreamName(), voD.getStreamName());
		assertEquals(streamVod.getStartTime(), voD.getStartTime());
		assertEquals(streamVod.getDuration(), voD.getDuration());
		assertEquals(streamVod.getFileSize(), voD.getFileSize());
		assertEquals(streamVod.getCreationDate(), voD.getCreationDate());
		assertEquals(streamVod.getType(), voD.getType());

		//add uservod
		vodId = RandomStringUtils.randomNumeric(24);
		VoD userVod=new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, 111, VoD.USER_VOD,vodId,null);

		datastore.addVod(userVod);

		//check vod number

		assertEquals(2, datastore.getTotalVodNumber());
		voD = datastore.getVoD(userVod.getVodId());
		assertEquals(userVod.getFilePath(), voD.getFilePath());
		assertEquals(userVod.getStreamId(), voD.getStreamId());
		assertEquals(userVod.getStreamName(), voD.getStreamName());
		assertEquals(streamVod.getStartTime(), voD.getStartTime());
		assertEquals(streamVod.getDuration(), voD.getDuration());
		assertEquals(streamVod.getFileSize(), voD.getFileSize());
		assertEquals(streamVod.getCreationDate(), voD.getCreationDate());
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

	public void testUpdateMetadata(DataStore datastore) throws Exception {
		String streamId = RandomStringUtils.randomNumeric(24);
		String metadata = "{metadata:metadata}";
		String newMetadata = "{metadata:metadata2}";

		//create a new broadcast
		Broadcast broadcast= new Broadcast();
		broadcast.setStreamId(streamId);
		broadcast.setMetaData(metadata);

		//save this broadcast
		datastore.save(broadcast);

		//check it is saved
		assertNotNull(broadcast.getMetaData());

		//update metadata
		broadcast.setMetaData(newMetadata);

		datastore.updateBroadcastFields(broadcast.getStreamId(), broadcast);

		Broadcast broadcast2 = datastore.get(streamId);

		//check whether is changed or not
		assertEquals(newMetadata, broadcast2.getMetaData());
		datastore.delete(broadcast2.getStreamId());
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

	public void testFullTextSearch(DataStore dataStore) {
		String searchQueryMatched = "\"ConnectorComponentId\":\"[6b8d7491-a86a-4c64-a982-0f8a2d3d393b.9c9df16c-eac1-4593-8010-d28e92f8a694]\"";
		String searchQueryNotMatched = "\"ConnectorComponentId\":\"[6b8d7491.9c9df16c]\"";

		Broadcast broadcast = new Broadcast(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, "{\"tenantId\":\"fd216128-e4da-11ed-9c89-bfe723cdc436\",\"erCollectionId\":\"5fb69a0a-26d7-4ce7-81c5-ac7ab4949a27\",\"ConnectorComponentId\":\"[6b8d7491-a86a-4c64-a982-0f8a2d3d393b.9c9df16c-eac1-4593-8010-d28e92f8a694]\"}");
		broadcast.setDate(100000);
		broadcast.setType(AntMediaApplicationAdapter.LIVE_STREAM);

		dataStore.save(broadcast);

		List<Broadcast> broadcastList0 = dataStore.getBroadcastList(0, 50, null, null, null, searchQueryMatched);
		assertFalse(broadcastList0.isEmpty());

		List<Broadcast> broadcastList1 = dataStore.getBroadcastList(0, 50, null, null, null, searchQueryNotMatched);
		assertTrue(broadcastList1.isEmpty());
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

		//case insensitive test
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
		Endpoint endPoint = new Endpoint(rtmpUrl, "generic", "customEndpointServiceXRiJgU", "finished");

		boolean result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
		assertTrue(result);

		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		String endpointStreamId = "stream id 2";
		Endpoint endPoint2 = new Endpoint(rtmpUrl,
				"facebook", null, "finished");

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
		assertEquals(broadcast2.getEndPointList().get(0).getRtmpUrl(), rtmpUrl);

		//
		Endpoint endPoint3Clone = new Endpoint(
				endPoint2.getRtmpUrl(), endPoint2.getType(), null, "finished");

		// remove end point2
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint3Clone, true);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);

		// add new enpoints
		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint = new Endpoint(rtmpUrl, "facebook", null, "finished");

		assertTrue(dataStore.addEndpoint(broadcast2.getStreamId(), endPoint));

		String rtmpUrl2 = "rtmp:(sdfsfskmkmkmkmf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint2 = new Endpoint(rtmpUrl2, "facebook", null, "finished");

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
		Endpoint endPoint = new Endpoint(rtmpUrl, "generic", "test234", "finished");

		boolean result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
		assertTrue(result);

		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		String endpointStreamId = "stream id 2";
		Endpoint endPoint2 = new Endpoint( rtmpUrl,
				"facebook", "generic_2", "finished");

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
		assertEquals(broadcast2.getEndPointList().get(0).getRtmpUrl(), rtmpUrl);

		//
		Endpoint endPoint3Clone = new Endpoint(
				endPoint2.getRtmpUrl(), endPoint2.getType(), "generic_2", "finished");

		// remove end point2
		result = dataStore.removeEndpoint(broadcast2.getStreamId(), endPoint3Clone, false);
		assertTrue(result);
		broadcast2 = dataStore.get(key);
		assertTrue(broadcast2.getEndPointList() == null || broadcast2.getEndPointList().size() == 0);

		// add new enpoints
		rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint = new Endpoint(rtmpUrl, "facebook", "generic_2", "finished");

		assertTrue(dataStore.addEndpoint(broadcast2.getStreamId(), endPoint));

		String rtmpUrl2 = "rtmp:(sdfsfskmkmkmkmf(ksklasjflakjflaskjflsadfkjsal";
		endpointStreamId = "stream id 2";
		endPoint2 = new Endpoint(rtmpUrl2, "facebook", "generic_3", "finished");

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
			long now = System.currentTimeMillis();
			Broadcast tmp = new Broadcast();
			tmp.setName(name);
			tmp.setDescription(description);
			tmp.setUpdateTime(now);
			tmp.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			tmp.setStartTime(now);
			tmp.setOriginAdress(ServerSettings.getLocalHostAddress());
			String subFolder = "test_folder";
			tmp.setSubFolder(subFolder);
			String listenerHookURL = "test_listener_hook_url";
			tmp.setListenerHookURL(listenerHookURL);
			assertTrue(tmp.isPlaylistLoopEnabled());
			tmp.setPlaylistLoopEnabled(false);
			double speed = 1.0;
			tmp.setSpeed(speed);
			tmp.setSeekTimeInMs(136);
			boolean result = dataStore.updateBroadcastFields(broadcast.getStreamId(), tmp);
			assertTrue(result);

			broadcast2 = dataStore.get(key);

			assertEquals(name, broadcast2.getName());
			assertEquals(now, broadcast2.getUpdateTime());
			assertEquals(subFolder, broadcast2.getSubFolder());
			assertEquals(description, broadcast2.getDescription());
			assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING, broadcast2.getStatus());
			assertEquals(now, broadcast2.getStartTime());
			assertEquals(ServerSettings.getLocalHostAddress(), tmp.getOriginAdress());
			assertEquals(listenerHookURL, broadcast2.getListenerHookURL());
			assertFalse(broadcast2.isPlaylistLoopEnabled());
			assertEquals(speed, broadcast2.getSpeed(), 0.1);

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
			Endpoint endPoint = new Endpoint(rtmpUrl, "generic", null, "finished");

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), null);
			assertFalse(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(1, broadcast2.getEndPointList().size());
			assertEquals(broadcast2.getEndPointList().get(0).getRtmpUrl(), rtmpUrl);

			rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
			endPoint = new Endpoint(rtmpUrl, "facebook", null, "finished");

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(2, broadcast2.getEndPointList().size());
			assertEquals(broadcast2.getEndPointList().get(1).getRtmpUrl(), rtmpUrl);

			Broadcast broadcast3 = new Broadcast("test3");
			broadcast3.setQuality("poor");
			assertNotNull(broadcast3.getQuality());
			dataStore.save(broadcast3);
			
			logger.info("Saved id {}", broadcast3.getStreamId());
			
			assertEquals(broadcast3.getStreamId(), dataStore.get(broadcast3.getStreamId()).getStreamId());
			
			result = dataStore.updateSourceQualityParameters(broadcast3.getStreamId(), null, 0.1, 0);
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

		VoD newVod =  new VoD("streamName", "1112233" + (int)(Math.random() * 1000), "path", "aaVod", 1517239908, 123, 17933, 1190425, VoD.STREAM_VOD, "1149253" + (int)(Math.random() * 91000),null);
		VoD newVod2 = new VoD("oguz", "123456" + (int)(Math.random() * 1000),  "path", "cCVod", 1517239708, 456, 17933, 1190625, VoD.STREAM_VOD, "11503943" + (int)(Math.random() * 91000),null);
		VoD newVod3 = new VoD("ahmet", "2341" + (int)(Math.random() * 1000),  "path", "TahIr", 1517239608, 17933, 789, 1190725, VoD.STREAM_VOD, "11259243" + (int)(Math.random() * 91000),null);
		VoD newVod4 = new VoD(null, null,  "path", null, 1517239608, 345, 17933, 1190725, VoD.STREAM_VOD, "11827485" + (int)(Math.random() * 91000), null);
		VoD newVod5 = new VoD("denem", null,  "path", null, 1517239608, 678, 17933, 1190725, VoD.STREAM_VOD, null, null);

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

		VoD newVod =  new VoD("streamName", "1112233" + (int)(Math.random() * 1000), "path", "vod", 1517239908, 123, 17933, 1190425, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000), null);
		VoD newVod2 = new VoD("davut", "111223" + (int)(Math.random() * 1000),  "path", "vod", 1517239808, 456, 17933, 1190525, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000), null);
		VoD newVod3 = new VoD("oguz", "11122" + (int)(Math.random() * 1000),  "path", "vod", 1517239708, 789, 17933, 1190625, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000), null);
		VoD newVod4 = new VoD("ahmet", "111" + (int)(Math.random() * 1000),  "path", "vod", 1517239608, 234, 17933, 1190725, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000), null);
		VoD newVod5 = new VoD("mehmet", "11" + (int)(Math.random() * 1000), "path", "vod", 1517239508, 567, 17933, 1190825, VoD.STREAM_VOD, "1112233" + (int)(Math.random() * 91000), null);

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
		assertEquals(item1, list.get(0).getObjectName());
		assertEquals(probability1, list.get(0).getProbability(),0.1F);
		assertEquals(detectionTime, list.get(0).getDetectionTime());	
		
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
		connected.setType(Subscriber.PLAY_TYPE);
		String hostAddress = ServerSettings.getLocalHostAddress();
		connected.setInstanceIP(hostAddress);
		
		ConnectionEvent disconnected = new ConnectionEvent();
		disconnected.setEventType(ConnectionEvent.DISCONNECTED_EVENT);
		long eventTimeDisconnect = 21;
		disconnected.setTimestamp(eventTimeDisconnect);		
		
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
		assertEquals(0, written.getCurrentConcurrentConnections());
		
		// there should be two events with correct order
		List<ConnectionEvent> events = written.getStats().getConnectionEvents();
		assertEquals(2, events.size());  
		
		assertEquals(ConnectionEvent.CONNECTED_EVENT, events.get(0).getEventType());
		assertEquals(Subscriber.PLAY_TYPE, events.get(0).getType());
		assertEquals(hostAddress, events.get(0).getInstanceIP());
		assertEquals(eventTime, events.get(0).getTimestamp());
		assertEquals(ConnectionEvent.DISCONNECTED_EVENT, events.get(1).getEventType());
		
		
		// add connected event
		store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), connected);
		// isConnected should be true again
		assertTrue(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		
		// reset connection status
		assertTrue(store.resetSubscribersConnectedStatus());
		// connection status should false again
		assertFalse(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		
		store.revokeSubscribers(streamId);
		
		{
			//save subscriber again 
			
			String subscriberId = "subscriberId" + (int)(Math.random()*112313);
			Subscriber subscriber = new Subscriber();
			subscriber.setStreamId(streamId);
			subscriber.setSubscriberId(subscriberId);
			subscriber.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
			subscriber.setType(Subscriber.PLAY_TYPE);
			assertTrue(store.addSubscriber(subscriber.getStreamId(), subscriber));
			
			Subscriber subscriberFromDB = store.getSubscriber(streamId, subscriberId);
			
			assertNull(subscriberFromDB.getRegisteredNodeIp());
			String nodeIp = "nodeip" + (int)(Math.random()*112313);
			subscriberFromDB.setRegisteredNodeIp(nodeIp);
			
			assertTrue(store.addSubscriber(subscriber.getStreamId(), subscriberFromDB));
			
			subscriberFromDB = store.getSubscriber(streamId, subscriberId);
			assertEquals(nodeIp, subscriberFromDB.getRegisteredNodeIp());
			
			if (store instanceof MongoStore) {
				MongoStore mongoStore = (MongoStore) store;
				Datastore subscriberDatastore = mongoStore.getSubscriberDatastore();
				long count = subscriberDatastore.find(Subscriber.class).filter(Filters.eq("streamId", streamId), Filters.eq("subscriberId", subscriberId)).count();
			
				assertEquals(1, count);
			}
			
		}
	}
	
	@Test
	public void testDontWriteStatsToDB () {
		/*
		DataStore ds = createDB("memorydb", false);
		assertTrue(ds instanceof InMemoryDataStore);	
		testDontWriteStatsToDB(ds);
		*/

		DataStore ds = createDB("mapdb", false);
		assertTrue(ds instanceof MapDBStore);	
		testDontWriteStatsToDB(ds);

		ds = createDB("mongodb", false);
		assertTrue(ds instanceof MongoStore);	
		testDontWriteStatsToDB(ds);


	}

	public void testDontWriteStatsToDB (DataStore dataStore) {
		testDontUpdateRtmpViewerStats(dataStore);
		testDontUpdateHLSViewerStats(dataStore);
		testDontUpdateDASHViewerStats(dataStore);
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
	
	public void testDontUpdateDASHViewerStats(DataStore dataStore) {
		Broadcast broadcast = new Broadcast();
		broadcast.setName("test");
		String key = dataStore.save(broadcast);

		assertFalse(dataStore.updateDASHViewerCount(key, 1));
		assertEquals(0, dataStore.get(key).getDashViewerCount());
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
		dsf.setDbHost("127.0.0.1");
		ApplicationContext context = Mockito.mock(ApplicationContext.class);
		Mockito.when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		AppSettings appSettings = new AppSettings();
		appSettings.setWriteStatsToDatastore(writeStats);
		Mockito.when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);	
		Mockito.when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());	
		
		
		dsf.setApplicationContext(context);
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
		MongoStore dataStore = new MongoStore("127.0.0.1", "", "", "testdb");
		deleteStreamInfos(dataStore);
		assertEquals(0, dataStore.getDataStore().find(StreamInfo.class).count());

		//same ports different host => there will be 2 SIs
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host2", 1000, 2000, 0);
		assertEquals(2, dataStore.getDataStore().find(StreamInfo.class).count());
		deleteStreamInfos(dataStore);

		//different ports same host => there will be 2 SIs
		saveStreamInfo(dataStore, "host1", 1000, 2000, 0, "host1", 1100, 2100, 0);
		assertEquals(2, dataStore.getDataStore().find(StreamInfo.class).count());
		deleteStreamInfos(dataStore);		
	}

	public void deleteStreamInfos(MongoStore datastore) {
		datastore.getDataStore().find(StreamInfo.class).delete(new DeleteOptions()
                .multi(true));
		
	}
	
	public void deleteBroadcast(MongoStore dataStore) {
		dataStore.getDataStore().find(Broadcast.class).delete(new DeleteOptions()
                .multi(true));
	}

	public void saveStreamInfo(DataStore dataStore, String host1, int videoPort1, int audioPort1, int dataPort1,
			String host2, int videoPort2, int audioPort2, int dataPort2) {

		StreamInfo si = new StreamInfo();
		si.setHost(host1);
		si.setStreamId("test1");
		si.setOriginPort(5858);
		dataStore.saveStreamInfo(si);
		

		List<StreamInfo> siList = dataStore.getStreamInfoList("test1");
		assertEquals(1, siList.size());
		
		assertEquals(host1, siList.get(0).getHost());
		assertEquals(5858, siList.get(0).getOriginPort());


		si = new StreamInfo();
		si.setHost(host2);
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

	private void testStreamSourceList(DataStore dataStore) {
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

		Broadcast ss1 = new Broadcast("ss1");
		ss1.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss1.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);

		Broadcast ss2 = new Broadcast("ss2");
		ss2.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

		Broadcast ss3 = new Broadcast("ss3");
		ss3.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss3.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_PREPARING);

		Broadcast ss4 = new Broadcast("ss4");
		ss4.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss4.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);

		Broadcast ss5 = new Broadcast("ss5");
		ss5.setType(AntMediaApplicationAdapter.IP_CAMERA);
		ss5.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);

		Broadcast ss6 = new Broadcast("ss6");
		ss6.setType(AntMediaApplicationAdapter.LIVE_STREAM);
		ss6.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);

		dataStore.save(ss1);
		dataStore.save(ss2);
		dataStore.save(ss3);
		dataStore.save(ss4);
		dataStore.save(ss5);
		dataStore.save(ss6);

		List<Broadcast> list = dataStore.getExternalStreamsList();
		assertEquals(3, list.size());

		assertNotEquals("ss6", list.get(0).getName());
		assertNotEquals("ss6", list.get(1).getName());
		assertNotEquals("ss6", list.get(2).getName());

		List<Broadcast> list2 = dataStore.getExternalStreamsList();
		assertEquals(0, list2.size());
	}

	private void testUpdateEndpointStatus(DataStore dataStore)
	{
		Broadcast broadcast = new Broadcast(null, null);
		String name = "name 1";
		String description = "description 2";
		broadcast.setName(name);
		broadcast.setDescription(description);
		dataStore.save(broadcast);

		assertNotNull(broadcast.getStreamId());

		//add endpoint
		String rtmpUrl = "rtmp://rtmp1";
		Endpoint endPoint = new Endpoint(rtmpUrl, "generic", null, "finished");
		boolean result = dataStore.addEndpoint(broadcast.getStreamId().toString(), endPoint);
		assertTrue(result);

		//add endpoint
		String rtmpUrl2 = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
		Endpoint endPoint2 = new Endpoint( rtmpUrl2,
				"generic", null, "finished");
		result = dataStore.addEndpoint(broadcast.getStreamId().toString(), endPoint2);
		assertTrue(result);

		//add endpoint
		String rtmpUrl3 = "rtmp:(sdfsfasafadgsgsf(ksklasjflakjflaskjflsadfkjsal";
		Endpoint endPoint3 = new Endpoint(rtmpUrl3,
				"generic", null, "finished");



		Broadcast tmpBroadcast = dataStore.get(broadcast.getStreamId());
		List<Endpoint> endPointList = tmpBroadcast.getEndPointList();
		for (Endpoint tmpEndpoint : endPointList) {
			if (tmpEndpoint.getRtmpUrl().equals(rtmpUrl)) {
				tmpEndpoint.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);
				break;
			}
		}
		//update rtmpurl
		result = dataStore.updateBroadcastFields(broadcast.getStreamId(), tmpBroadcast); 
		assertTrue(result);
		
		
		
		tmpBroadcast = dataStore.get(broadcast.getStreamId());
		endPointList = tmpBroadcast.getEndPointList();
		for (Endpoint tmpEndpoint : endPointList) {
			if (tmpEndpoint.getRtmpUrl().equals(rtmpUrl2)) {
				tmpEndpoint.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				break;
			}
		}
		result = dataStore.updateBroadcastFields(broadcast.getStreamId(), tmpBroadcast); 
		assertTrue(result);
		
		
		
		Broadcast updated = dataStore.get(broadcast.getStreamId());
		List<Endpoint> endpList = updated.getEndPointList();
		for(int i = 0; i < endpList.size(); i++){
			Endpoint e = endpList.get(i);
			if(e.getRtmpUrl().equals(rtmpUrl)){
				assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED, e.getStatus());
			}
			else if(e.getRtmpUrl().equals(rtmpUrl2)){
				assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING, e.getStatus());
			}
			else{
				assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_CREATED, e.getStatus());
			}
		}
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
		if(dataStore instanceof MongoStore || dataStore instanceof RedisStore) {
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

		List<PlayListItem> broadcastList = new ArrayList<>();
		
		broadcastList.add(new PlayListItem("", null));
		
		Broadcast broadcast = new Broadcast();
		broadcast.setName("playlistName");
		broadcast.setType(AntMediaApplicationAdapter.PLAY_LIST);
		broadcast.setPlayListItemList(broadcastList);
		

		//create playlist
		String streamId = dataStore.save(broadcast);
		
		Broadcast broadcast2 = dataStore.get(streamId);
		assertNotNull(streamId);
		assertEquals(AntMediaApplicationAdapter.PLAY_LIST, broadcast2.getType());
		assertEquals(1, broadcast2.getPlayListItemList().size());
		assertNull(broadcast2.getPlayListStatus());
		
		//update playlist
		
		broadcast.setPlayListStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		broadcastList.clear();
		broadcast.setPlayListItemList(broadcastList);
		broadcast.setCurrentPlayIndex(10);
		assertTrue(dataStore.updateBroadcastFields(streamId, broadcast));
		
		broadcast2 = dataStore.get(streamId);
		assertTrue(broadcast2.getPlayListItemList() == null || broadcast2.getPlayListItemList().isEmpty());
		assertEquals(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED, broadcast2.getPlayListStatus());
		assertEquals(10, broadcast.getCurrentPlayIndex());

		//get new playlist		
		Broadcast playlist2 = dataStore.get(streamId);

		assertNotNull(playlist2);
		
		assertEquals("playlistName", broadcast.getName());

		//delete playlist
		assertTrue(dataStore.delete(streamId));

		assertNull(dataStore.get(streamId));
		
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

		assertTrue(mainTrack.getSubTrackStreamIds().isEmpty());
		assertNull(subtrack.getMainTrackStreamId());

		subtrack.setMainTrackStreamId(mainTrackId);
		assertTrue(dataStore.updateBroadcastFields(subTrackId, subtrack));

		boolean result = dataStore.addSubTrack(mainTrackId, subTrackId);
		assertTrue(result);
		mainTrack = dataStore.get(mainTrackId);
		subtrack = dataStore.get(subTrackId);
		assertEquals(1, mainTrack.getSubTrackStreamIds().size());
		assertEquals(subTrackId, mainTrack.getSubTrackStreamIds().get(0));
		assertEquals(mainTrackId, subtrack.getMainTrackStreamId());
		
		
		result = dataStore.addSubTrack("Not exists", subTrackId);
		assertFalse(result);
		
		result = dataStore.addSubTrack(mainTrackId, null);
		assertFalse(result);
		

	}

	/*
	 * Test remove track
	 * In the test, we create 2 main track and 1 sub track. Then we add sub track to main tracks.
	 * We set sub track's main track id to the first main track id.
	 * After that we remove sub track from main tracks.
	 * We assert that sub track is removed from both main tracks
	 */
	public void testRemoveTrack(DataStore dataStore) {

		String mainTrackId1 = RandomStringUtils.randomAlphanumeric(8);
		String mainTrackId2 = RandomStringUtils.randomAlphanumeric(8);
		String subTrackId = RandomStringUtils.randomAlphanumeric(8);

		Broadcast mainTrack1= new Broadcast();
		try {
			mainTrack1.setStreamId(mainTrackId1);
			mainTrack1.setSubTrackStreamIds(new ArrayList<>(Arrays.asList(subTrackId)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Broadcast mainTrack2= new Broadcast();
		try {
			mainTrack2.setStreamId(mainTrackId2);
			mainTrack2.setSubTrackStreamIds(new ArrayList<>(Arrays.asList(subTrackId)));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Broadcast subtrack= new Broadcast();
		try {
			subtrack.setStreamId(subTrackId);
			subtrack.setMainTrackStreamId(mainTrackId1);
		} catch (Exception e) {
			e.printStackTrace();
		}

		dataStore.save(mainTrack1);
		dataStore.save(mainTrack2);
		dataStore.save(subtrack);

		assertTrue(dataStore.get(mainTrackId1).getSubTrackStreamIds().size() == 1);
		assertEquals(subTrackId, mainTrack1.getSubTrackStreamIds().get(0));
		assertTrue(dataStore.get(mainTrackId2).getSubTrackStreamIds().size() == 1);
		assertEquals(subTrackId, mainTrack2.getSubTrackStreamIds().get(0));
		assertEquals(mainTrackId1, dataStore.get(subTrackId).getMainTrackStreamId());

		assertTrue(dataStore.removeSubTrack(mainTrackId1, subTrackId));
		assertTrue(dataStore.removeSubTrack(mainTrackId2, subTrackId));

		assertTrue(dataStore.get(mainTrackId1).getSubTrackStreamIds().isEmpty());
		assertTrue(dataStore.get(mainTrackId2).getSubTrackStreamIds().isEmpty());

		assertFalse(dataStore.removeSubTrack("nonExistedStreamID", subTrackId));
		assertFalse(dataStore.removeSubTrack(mainTrackId1, null));


	}

	public void testGetVoDIdByStreamId(DataStore dataStore) {
		String streamId=RandomStringUtils.randomNumeric(24);
		String vodId1="vod_1";
		String vodId2="vod_2";
		String vodId3="vod_3";
		VoD vod1 = new VoD("streamName", streamId, "filePath", "vodName2", 333, 111, 111, 111, VoD.STREAM_VOD, vodId1, null);
		VoD vod2 = new VoD("streamName", streamId, "filePath", "vodName1", 222, 111, 111, 111, VoD.STREAM_VOD, vodId2, null);
		VoD vod3 = new VoD("streamName", "streamId123", "filePath", "vodName3", 111, 111, 111, 111, VoD.STREAM_VOD, vodId3, null);

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
	
	@Test
	public void testDeleteMapDB() {
		String dbName = "deleteMapdb";
		DataStore dataStore = new MapDBStore(dbName, vertx);
		assertTrue(new File(dbName).exists());
		dataStore.close(true);
		assertFalse(new File(dbName).exists());
	}
	
	@Test
	public void testDeleteMongoDBCollection() {
		String dbName = "deleteMapdb";
		MongoStore dataStore = new MongoStore("127.0.0.1", "", "", dbName);
		
		MongoClientURI mongoUri = new MongoClientURI(dataStore.getMongoConnectionUri("127.0.0.1", "", ""));
		MongoClient client = new MongoClient(mongoUri);
		
		
		ArrayList<String> dbNames = new ArrayList<String>();
		client.listDatabaseNames().forEach(c-> dbNames.add(c));
		assertTrue(dbNames.contains(dbName));
		
		dataStore.close(true);

		dbNames.clear();
		client.listDatabaseNames().forEach(c-> dbNames.add(c));
		assertFalse(dbNames.contains(dbName));

	}
	
	
	public void testWebRTCViewerOperations(DataStore dataStore) {
		
		ArrayList<String> idList = new ArrayList<String>();
		
		int total = RandomUtils.nextInt(10, DataStore.MAX_ITEM_IN_ONE_LIST);
		for (int i = 0; i < total; i++) {
			WebRTCViewerInfo info = new WebRTCViewerInfo();
			String streamId = RandomStringUtils.randomAlphabetic(5);
			info.setStreamId(streamId);
			String id = RandomStringUtils.randomAlphabetic(5);
			info.setViewerId(id);
			
			dataStore.saveViewerInfo(info);
			
			idList.add(id);
		}
		
		List<WebRTCViewerInfo> returningList = dataStore.getWebRTCViewerList(0, DataStore.MAX_ITEM_IN_ONE_LIST+10, "viewerId", "asc", "");
		assertEquals(total,  returningList.size());	
		
		
	    Collections.sort(idList);
	    
	    for (int i = 0; i < total; i++) {
			assertEquals(idList.get(i),  returningList.get(i).getViewerId());	
		}
	    
		List<WebRTCViewerInfo> returningList2 = dataStore.getWebRTCViewerList(0, total, "viewerId", "asc", "a");
		for (WebRTCViewerInfo webRTCViewerInfo : returningList2) {
			assertTrue(webRTCViewerInfo.getViewerId().contains("a")||webRTCViewerInfo.getViewerId().contains("A"));
		}
	    
		
	    int deleted = 0;
	    for (String id : idList) {
			dataStore.deleteWebRTCViewerInfo(id);
		    List<WebRTCViewerInfo> tempList = dataStore.getWebRTCViewerList(0, total, "viewerId", "asc", "");
		    
			assertEquals(total - (++deleted),  tempList.size());	
		}
	}	
	
	public void testUpdateMetaData(DataStore dataStore) {

		final String INITIAL_DATA  = "initial meta data";
		final String UPDATED_DATA  = "updated meta data";

		String id = RandomStringUtils.randomAlphanumeric(8);

		Broadcast broadcast= new Broadcast();
		try {
			broadcast.setStreamId(id);
		} catch (Exception e) {
			e.printStackTrace();
		}

		broadcast.setMetaData(INITIAL_DATA);
		dataStore.save(broadcast);

		assertEquals(INITIAL_DATA, dataStore.get(id).getMetaData());
		
		assertTrue(dataStore.updateStreamMetaData(id, UPDATED_DATA));
		
		assertEquals(UPDATED_DATA, dataStore.get(id).getMetaData());
		
		assertFalse(dataStore.updateStreamMetaData("someDummyStream"+RandomStringUtils.randomAlphanumeric(8), UPDATED_DATA));

	}

	public void testBlockSubscriber(DataStore dataStore){

		Broadcast broadcast = new Broadcast();
		String streamId = "teststream";
		try
		{
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

		dataStore.save(broadcast);
		Subscriber subscriber = new Subscriber();
		String subscriberId = "subscriberId";
		subscriber.setSubscriberId(subscriberId);
		subscriber.setStreamId(streamId);
		long currTime = System.currentTimeMillis();

		dataStore.addSubscriber(streamId, subscriber);
		
		Subscriber subscriberFromDB = dataStore.getSubscriber(streamId, subscriberId);
		
		assertNull(subscriberFromDB.getBlockedType());
		assertFalse(subscriberFromDB.isBlocked(Subscriber.PLAY_TYPE));
		assertFalse(subscriberFromDB.isBlocked(Subscriber.PUBLISH_TYPE));
		assertFalse(subscriberFromDB.isBlocked(Subscriber.PUBLISH_AND_PLAY_TYPE));

		assertTrue(dataStore.blockSubscriber(streamId, subscriberId, Subscriber.PLAY_TYPE, 10));
		subscriberFromDB = dataStore.getSubscriber(streamId, subscriberId);
		assertEquals(Subscriber.PLAY_TYPE, subscriberFromDB.getBlockedType());
		
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PLAY_TYPE));
		
		assertFalse(subscriberFromDB.isBlocked(Subscriber.PUBLISH_TYPE));
		assertFalse(subscriberFromDB.isBlocked(Subscriber.PUBLISH_AND_PLAY_TYPE));

		assertTrue(subscriberFromDB.getBlockedUntilUnitTimeStampMs() - System.currentTimeMillis() <= 10000);
		
		
		assertFalse(dataStore.blockSubscriber(null, subscriberId, Subscriber.PLAY_TYPE, 10));
		assertFalse(dataStore.blockSubscriber(streamId, null, Subscriber.PLAY_TYPE, 10));


		
		assertTrue(dataStore.blockSubscriber(streamId, subscriberId, Subscriber.PUBLISH_TYPE, 50));
		subscriberFromDB = dataStore.getSubscriber(streamId, subscriberId);
		assertEquals(Subscriber.PUBLISH_TYPE, subscriberFromDB.getBlockedType());
		assertFalse(subscriberFromDB.isBlocked(Subscriber.PLAY_TYPE));
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PUBLISH_TYPE));
		assertFalse(subscriberFromDB.isBlocked(Subscriber.PUBLISH_AND_PLAY_TYPE));

		assertTrue(subscriberFromDB.getBlockedUntilUnitTimeStampMs() - System.currentTimeMillis() <= 50000);


		assertTrue(dataStore.blockSubscriber(streamId, subscriberId, Subscriber.PUBLISH_AND_PLAY_TYPE, 50));
		subscriberFromDB = dataStore.getSubscriber(streamId, subscriberId);
		assertEquals(Subscriber.PUBLISH_AND_PLAY_TYPE, subscriberFromDB.getBlockedType());
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PLAY_TYPE));
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PUBLISH_TYPE));
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PUBLISH_AND_PLAY_TYPE));

		assertTrue(subscriberFromDB.getBlockedUntilUnitTimeStampMs() - System.currentTimeMillis() <= 50000);
		
		
		//if subscriber is not in datastore, still blockUser should be supported
		subscriberId = "subscriberNotInDB";
		assertTrue(dataStore.blockSubscriber(streamId, subscriberId, Subscriber.PUBLISH_AND_PLAY_TYPE, 50));
		subscriberFromDB = dataStore.getSubscriber(streamId, subscriberId);
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PLAY_TYPE));
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PUBLISH_TYPE));
		assertTrue(subscriberFromDB.isBlocked(Subscriber.PUBLISH_AND_PLAY_TYPE));
		assertTrue(subscriberFromDB.getBlockedUntilUnitTimeStampMs() - System.currentTimeMillis() <= 50000);


	}
	
	private void testSubscriberMetaData(DataStore dataStore) {
		//save subscriberMetadata to the data store
		
		String subscriberId = RandomStringUtils.randomAlphanumeric(12);
		
		SubscriberMetadata subscriberMetaData = dataStore.getSubscriberMetaData(subscriberId);
		assertNull(subscriberMetaData);
		
		SubscriberMetadata metadata = new SubscriberMetadata();
		Map<String, PushNotificationToken> pushNotificationTokens = new HashMap<>();
		String tokenValue = RandomStringUtils.randomAlphabetic(65);
		
		PushNotificationToken token = new PushNotificationToken(tokenValue, PushNotificationServiceTypes.FIREBASE_CLOUD_MESSAGING.toString());
		pushNotificationTokens.put(tokenValue, token);
		
		metadata.setPushNotificationTokens(pushNotificationTokens);
		dataStore.putSubscriberMetaData(subscriberId, metadata);
		
		//get the value with the id 
		subscriberMetaData = dataStore.getSubscriberMetaData(subscriberId);
		assertNotNull(subscriberMetaData);
		assertEquals(subscriberId, subscriberMetaData.getSubscriberId());
		assertEquals(1, subscriberMetaData.getPushNotificationTokens().size());
		assertEquals(tokenValue, subscriberMetaData.getPushNotificationTokens().get(tokenValue).getToken());
		assertEquals("fcm", subscriberMetaData.getPushNotificationTokens().get(tokenValue).getServiceName());
		assertNull(subscriberMetaData.getPushNotificationTokens().get(tokenValue).getExtraData());

		
		String tokenValue2 = RandomStringUtils.randomAlphabetic(65);
		
		PushNotificationToken token2 = new PushNotificationToken(tokenValue2, PushNotificationServiceTypes.APPLE_PUSH_NOTIFICATION.toString());
		String extraData = RandomStringUtils.randomAlphanumeric(12);
		token2.setExtraData(extraData);
		subscriberMetaData.getPushNotificationTokens().put(tokenValue2, token2);
		
		
		dataStore.putSubscriberMetaData(subscriberId, subscriberMetaData);
		
		subscriberMetaData = dataStore.getSubscriberMetaData(subscriberId);
		
		assertNotNull(subscriberMetaData);
		assertEquals(subscriberId, subscriberMetaData.getSubscriberId());
		assertEquals(2, subscriberMetaData.getPushNotificationTokens().size());
		assertEquals(tokenValue, subscriberMetaData.getPushNotificationTokens().get(tokenValue).getToken());
		assertEquals("fcm", subscriberMetaData.getPushNotificationTokens().get(tokenValue).getServiceName());
		assertNull(subscriberMetaData.getPushNotificationTokens().get(tokenValue).getExtraData());
		
		assertEquals(tokenValue2, subscriberMetaData.getPushNotificationTokens().get(tokenValue2).getToken());
		assertEquals("apn", subscriberMetaData.getPushNotificationTokens().get(tokenValue2).getServiceName());
		assertEquals(extraData, subscriberMetaData.getPushNotificationTokens().get(tokenValue2).getExtraData());
		
	}
}
