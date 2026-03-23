package io.antmedia.test.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import io.antmedia.datastore.db.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avutil.tm;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.gson.Gson;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import dev.morphia.Datastore;
import dev.morphia.DeleteOptions;
import dev.morphia.query.filters.Filters;
import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.EncoderSettings;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Broadcast.PlayListItem;
import io.antmedia.datastore.db.types.BroadcastUpdate;
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

	private AppSettings appSettings;

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
	public void testConferenceRoomWrapper() throws Exception {

		ConferenceRoom conferenceRoom = new ConferenceRoom();
		conferenceRoom.setRoomId("roomId");
		long startDate = Instant.now().getEpochSecond();
		long endDate = startDate + 1000;
		conferenceRoom.setStartDate(startDate);
		conferenceRoom.setEndDate(endDate);
		conferenceRoom.setMode(ConferenceRoom.LEGACY_MODE);
		conferenceRoom.setRoomStreamList(Arrays.asList("stream1", "stream2"));
		conferenceRoom.setOriginAdress("originAdress");

		assertEquals("roomId", conferenceRoom.getRoomId());
		assertEquals(ConferenceRoom.LEGACY_MODE, conferenceRoom.getMode());
		assertEquals("originAdress", conferenceRoom.getOriginAdress());
		assertEquals(2, conferenceRoom.getRoomStreamList().size());

		Broadcast broadcast = DataStore.conferenceToBroadcast(conferenceRoom);
		assertEquals("roomId", broadcast.getStreamId());
		assertEquals("originAdress", broadcast.getOriginAdress());
		assertEquals(ConferenceRoom.LEGACY_MODE, broadcast.getConferenceMode());
		assertEquals(2, broadcast.getSubTrackStreamIds().size());
		assertEquals("stream1", broadcast.getSubTrackStreamIds().get(0));
		assertEquals("stream2", broadcast.getSubTrackStreamIds().get(1));
		assertEquals(startDate, broadcast.getPlannedStartDate());
		assertEquals(endDate, broadcast.getPlannedEndDate());


		ConferenceRoom conferenceRoom2 = DataStore.broadcastToConference(broadcast);
		assertEquals("roomId", conferenceRoom2.getRoomId());
		assertEquals(ConferenceRoom.LEGACY_MODE, conferenceRoom2.getMode());
		assertEquals("originAdress", conferenceRoom2.getOriginAdress());
		assertEquals(2, conferenceRoom2.getRoomStreamList().size());
		assertEquals("stream1", conferenceRoom2.getRoomStreamList().get(0));
		assertEquals("stream2", conferenceRoom2.getRoomStreamList().get(1));
		assertEquals(startDate, conferenceRoom2.getStartDate());
		assertEquals(endDate, conferenceRoom2.getEndDate());
	}





	@Test
	public void testMapDBStore() throws Exception {

		DataStore dataStore = new MapDBStore("testdb", vertx);
		appSettings = new AppSettings();
		dataStore.setAppSettings(appSettings);

		testConnectionEventsBug(dataStore);
		testSubscriberAvgBitrate(dataStore);
		testLocalLiveBroadcast(dataStore);
		testUpdateBroadcastEncoderSettings(dataStore);
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
		testUpdateRole(dataStore);
		testUpdateHLSViewerCount(dataStore);
		testUpdateDashViewerCount(dataStore);
		testWebRTCViewerCount(dataStore);
		testRTMPViewerCount(dataStore);
		testTokenOperations(dataStore);
		testTimeBasedSubscriberOperations(dataStore);
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
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);
		testStreamSourceList(dataStore);
		testGetSubtracks(dataStore);
		testGetSubtracksWithStatus(dataStore);
		testGetSubtracksWithOrdering(dataStore);
		testGetSubtracksWithSearch(dataStore);
		testConnectedSubscribers(dataStore);
		testCustomTotpExpiry(dataStore);

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

		appSettings = new AppSettings();

		dataStore.setAppSettings(appSettings);

		testConnectionEventsBug(dataStore);
		testSubscriberAvgBitrate(dataStore);
		testVoDFunctions(dataStore);
		testLocalLiveBroadcast(dataStore);
		testUpdateBroadcastEncoderSettings(dataStore);
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
		testUpdateRole(dataStore);
		testGetActiveBroadcastCount(dataStore);
		testUpdateHLSViewerCount(dataStore);
		testUpdateDashViewerCount(dataStore);
		testWebRTCViewerCount(dataStore);
		testRTMPViewerCount(dataStore);
		testTokenOperations(dataStore);
		testTimeBasedSubscriberOperations(dataStore);
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
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);
		testStreamSourceList(dataStore);
		testGetSubtracks(dataStore);
		testGetSubtracksWithStatus(dataStore);
		testGetSubtracksWithOrdering(dataStore);
		testGetSubtracksWithSearch(dataStore);
		testConnectedSubscribers(dataStore);
		testCustomTotpExpiry(dataStore);


		dataStore.close(false);


	}


	@Test
	public void testMongoStore() throws Exception {

		DataStore dataStore = new MongoStore("127.0.0.1", "", "", "testdb");
		//delete db
		dataStore.close(true);

		dataStore = new MongoStore("127.0.0.1", "", "", "testdb");

		appSettings = new AppSettings();

		dataStore.setAppSettings(appSettings);
		
		testBroadcastCache(dataStore);

		testConnectionEventsBug(dataStore);
		testSubscriberAvgBitrate(dataStore);
		testSaveDuplicateStreamId((MongoStore)dataStore);

		testLocalLiveBroadcast(dataStore);
		testUpdateBroadcastEncoderSettings(dataStore);
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
		testUpdateRole(dataStore);
		testGetActiveBroadcastCount(dataStore);
		testUpdateHLSViewerCount(dataStore);
		testUpdateDashViewerCount(dataStore);
		testWebRTCViewerCount(dataStore);
		testRTMPViewerCount(dataStore);
		testTokenOperations(dataStore);
		testClearAtStart(dataStore);
		testClearAtStartCluster(dataStore);
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
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);


		testGetSubtracks(dataStore);
		testGetSubtracksWithStatus(dataStore);

		testSubscriberCache(dataStore);
		
		testGetSubtracksWithOrdering(dataStore);
		testGetSubtracksWithSearch(dataStore);
		testConnectedSubscribers(dataStore);
		testCustomTotpExpiry(dataStore);

		dataStore.close(true);

	}

	@Test
	public void testRedisStore() throws Exception {

		DataStore dataStore = new RedisStore("redis://127.0.0.1:6379", "testdb");
		//delete db
		dataStore.close(true);
		dataStore = new RedisStore("redis://127.0.0.1:6379", "testdb");

		appSettings = new AppSettings();

		dataStore.setAppSettings(appSettings);
		
		testConnectionEventsBug(dataStore);
		testSubscriberAvgBitrate(dataStore);
		testLocalLiveBroadcast(dataStore);
		testUpdateBroadcastEncoderSettings(dataStore);
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
		testUpdateRole(dataStore);
		testGetActiveBroadcastCount(dataStore);
		testUpdateHLSViewerCount(dataStore);
		testUpdateDashViewerCount(dataStore);
		testWebRTCViewerCount(dataStore);
		testRTMPViewerCount(dataStore);
		testTokenOperations(dataStore);
		testTimeBasedSubscriberOperations(dataStore);
		testClearAtStart(dataStore);
		testClearAtStartCluster(dataStore);
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
		testUpdateEndpointStatus(dataStore);
		testWebRTCViewerOperations(dataStore);
		testUpdateMetaData(dataStore);

		testGetSubtracks(dataStore);
		testGetSubtracksWithStatus(dataStore);

		dataStore.close(true);
	}




	public void testSaveDuplicateStreamId(MongoStore mongoStore ) throws Exception {
		Broadcast broadcast = new Broadcast(null, null);
		assertNotNull(mongoStore.save(broadcast));

		Broadcast broadcast2 = new Broadcast(null, null);
		broadcast2.setStreamId(broadcast.getStreamId());
		assertNull(mongoStore.save(broadcast2));


		mongoStore.deleteDuplicateStreamIds(mongoStore.getDataStore().getCollection(Broadcast.class));
	}


	@Test
	public void testBug() {

		MapDBStore dataStore = new MapDBStore("src/test/resources/damaged_webrtcappee.db", vertx);

		appSettings = new AppSettings();

		dataStore.setAppSettings(appSettings);

		//Following methods does not return before the bug is fixed
		dataStore.fetchUserVodList(new File(""));

		dataStore.getVodList(0, 10, "name", "asc", null, null);
		dataStore.getBroadcastList(0, 10, "asc", null, null, null);
	}

	@Test
	public void testConferenceRoomMigrationMapBased() {

		MapDBStore dataStore = new MapDBStore("testdb" + RandomStringUtils.randomAlphanumeric(12) , vertx);

		appSettings = new AppSettings();

		dataStore.setAppSettings(appSettings);

		Map<String, String> conferenceRoomMap = dataStore.getConferenceRoomMap();

		Gson gson = new Gson();
		ConferenceRoom conferenceRoom = new ConferenceRoom();
		conferenceRoom.setRoomId("roomId");
		long startDate = Instant.now().getEpochSecond();
		long endDate = startDate + 1000;
		conferenceRoom.setStartDate(startDate);
		conferenceRoom.setEndDate(endDate);
		conferenceRoom.setMode(ConferenceRoom.LEGACY_MODE);
		conferenceRoom.setRoomStreamList(Arrays.asList("stream1", "stream2"));
		conferenceRoom.setOriginAdress("originAdress");

		conferenceRoomMap.put("roomId", gson.toJson(conferenceRoom));


		ConferenceRoom conferenceRoom2 = new ConferenceRoom();
		conferenceRoom2.setRoomId("roomId2");
		long startDate2 = Instant.now().getEpochSecond();
		long endDate2 = startDate + 1000;
		conferenceRoom2.setStartDate(startDate2);
		conferenceRoom2.setEndDate(endDate2);
		conferenceRoom2.setMode(ConferenceRoom.MULTI_TRACK_MODE);
		conferenceRoom2.setRoomStreamList(Arrays.asList("stream3", "stream4"));
		conferenceRoom2.setOriginAdress("originAdress2");

		conferenceRoomMap.put("roomId2", gson.toJson(conferenceRoom2));


		dataStore.migrateConferenceRoomsToBroadcasts();

		assertEquals(2, dataStore.getTotalBroadcastNumber());
		Broadcast broadcast = dataStore.get("roomId");
		assertNotNull(broadcast);
		assertEquals("roomId", broadcast.getStreamId());
		assertEquals("originAdress", broadcast.getOriginAdress());
		assertEquals(ConferenceRoom.LEGACY_MODE, broadcast.getConferenceMode());
		assertEquals(2, broadcast.getSubTrackStreamIds().size());
		assertEquals("stream1", broadcast.getSubTrackStreamIds().get(0));
		assertEquals("stream2", broadcast.getSubTrackStreamIds().get(1));


		assertEquals(0, conferenceRoomMap.size());

	}

	@Test
	public void testConferenceRoomMigrationMongo() {

		MongoStore dataStore = new MongoStore("127.0.0.1", "", "", "testdb");

		//delete db
		dataStore.close(true);

		dataStore = new MongoStore("127.0.0.1", "", "", "testdb");

		appSettings = new AppSettings();

		dataStore.setAppSettings(appSettings);

		Datastore conferenceRoomMap = dataStore.getConferenceRoomDatastore();

		Gson gson = new Gson();
		ConferenceRoom conferenceRoom = new ConferenceRoom();
		conferenceRoom.setRoomId("roomId");
		long startDate = Instant.now().getEpochSecond();
		long endDate = startDate + 1000;
		conferenceRoom.setStartDate(startDate);
		conferenceRoom.setEndDate(endDate);
		conferenceRoom.setMode(ConferenceRoom.LEGACY_MODE);
		conferenceRoom.setRoomStreamList(Arrays.asList("stream1", "stream2"));
		conferenceRoom.setOriginAdress("originAdress");

		conferenceRoomMap.save(conferenceRoom);


		ConferenceRoom conferenceRoom2 = new ConferenceRoom();
		conferenceRoom2.setRoomId("roomId2");
		long startDate2 = Instant.now().getEpochSecond();
		long endDate2 = startDate + 1000;
		conferenceRoom2.setStartDate(startDate2);
		conferenceRoom2.setEndDate(endDate2);
		conferenceRoom2.setMode(ConferenceRoom.MULTI_TRACK_MODE);
		conferenceRoom2.setRoomStreamList(Arrays.asList("stream3", "stream4"));
		conferenceRoom2.setOriginAdress("originAdress2");

		conferenceRoomMap.save(conferenceRoom2);


		dataStore.migrateConferenceRoomsToBroadcasts();

		assertEquals(2, dataStore.getTotalBroadcastNumber());
		Broadcast broadcast = dataStore.get("roomId");
		assertNotNull(broadcast);
		assertEquals("roomId", broadcast.getStreamId());
		assertEquals("originAdress", broadcast.getOriginAdress());
		assertEquals(ConferenceRoom.LEGACY_MODE, broadcast.getConferenceMode());
		assertEquals(2, broadcast.getSubTrackStreamIds().size());
		assertEquals("stream1", broadcast.getSubTrackStreamIds().get(0));
		assertEquals("stream2", broadcast.getSubTrackStreamIds().get(1));


		assertEquals(0, conferenceRoomMap.find(ConferenceRoom.class).count());

	}

	public void clear(DataStore dataStore) 
	{
		long numberOfStreams = dataStore.getBroadcastCount();
		int numberOfCall = 0;
		int pageSize = 50;

		List<Broadcast> totalBroadcastList = new ArrayList<>();
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			List<Broadcast> broadcastList = dataStore.getBroadcastList(i * pageSize, pageSize, null, null, null, null);
			if (broadcastList == null || broadcastList.size() == 0) {
				break;
			}
			totalBroadcastList.addAll(broadcastList);
		}



		for (Broadcast broadcast : totalBroadcastList) {
			numberOfCall++;
			assertTrue(dataStore.delete(broadcast.getStreamId()));
		}

		assertEquals(numberOfCall, numberOfStreams);

		numberOfCall = 0;
		List<VoD> totalVoDList = new ArrayList<>();
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			List<VoD> vodList = dataStore.getVodList(i * pageSize, pageSize, null, null, null, null);
			if (vodList == null || vodList.size() == 0) {
				break;
			}
			totalVoDList.addAll(vodList);
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

	public void testLocalLiveBroadcast(DataStore dataStore) {
		clear(dataStore);

		assertEquals(0, dataStore.getBroadcastCount());

		long streamCount = 10;
		String streamId = null;
		for (int i = 0; i < streamCount; i++) {
			Broadcast broadcast = new Broadcast(null, null);
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setUpdateTime(System.currentTimeMillis());
			streamId = dataStore.save(broadcast);
			logger.info("Saved streamId:{}", streamId);
		}


		assertEquals(streamCount, dataStore.getActiveBroadcastCount());

		if (dataStore instanceof MapDBStore || dataStore instanceof InMemoryDataStore) {
			assertEquals(streamCount, dataStore.getLocalLiveBroadcastCount(ServerSettings.getLocalHostAddress()));

			List<Broadcast> localLiveBroadcasts = dataStore.getLocalLiveBroadcasts(ServerSettings.getLocalHostAddress());
			assertEquals(streamCount, localLiveBroadcasts.size());
		} else {

			//because there is no origin address registered
			assertEquals(0, dataStore.getLocalLiveBroadcastCount(ServerSettings.getLocalHostAddress()));
			List<Broadcast> localLiveBroadcasts = dataStore.getLocalLiveBroadcasts(ServerSettings.getLocalHostAddress());
			assertEquals(0, localLiveBroadcasts.size());
		}

		clear(dataStore);

		assertEquals(0, dataStore.getBroadcastCount());

		streamCount = 15;
		for (int i = 0; i < streamCount; i++) {
			Broadcast broadcast = new Broadcast(null, null);
			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setUpdateTime(System.currentTimeMillis());
			broadcast.setOriginAdress(ServerSettings.getLocalHostAddress());
			streamId = dataStore.save(broadcast);
			logger.info("Saved streamId:{}", streamId);
		}

		assertEquals(streamCount, dataStore.getLocalLiveBroadcastCount(ServerSettings.getLocalHostAddress()));

		List<Broadcast> localLiveBroadcasts = dataStore.getLocalLiveBroadcasts(ServerSettings.getLocalHostAddress());
		assertEquals(streamCount, localLiveBroadcasts.size());

	}

	public void testGetActiveBroadcastCount(DataStore dataStore) {

		//save random number of streams with status created
		//long broadcastCountInDataStore = dataStore.getBroadcastCount();
		clear(dataStore);

		assertEquals(0, dataStore.getBroadcastCount());


		long streamCount = 10 + (int)(Math.random()  * 500);


		System.out.println("Stream count to be added: " + streamCount);

		String streamId = null;
		for (int i = 0; i < streamCount; i++) {
			Broadcast broadcast = new Broadcast(null, null);
			broadcast.setOriginAdress(ServerSettings.getLocalHostAddress());
			streamId = dataStore.save(broadcast);
			logger.info("Saved streamId:{}", streamId);
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
				BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
				broadcastUpdate.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
				broadcastUpdate.setUpdateTime(System.currentTimeMillis());
;				assertTrue(dataStore.updateBroadcastFields(broadcast.getStreamId(), broadcastUpdate));
			}

		}

		assertTrue(numberOfCall > 0);
		assertEquals(numberOfCall, numberOfStatusChangeStreams);
		//check that active broadcast exactly the same as changed above

		//////this test is sometimes failing below, I think streamId may not be unique so I logged above to confirm it - mekya
		//yes the streamId is not unique, we need to improve  - mekya Aug 11, 2024
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
		File[] files = f.listFiles();
		int numberOfFiles = 0;
		for (File file : files) 
		{
			String fileExtension = FilenameUtils.getExtension(file.getName());
			if (file.isFile() && ("mp4".equals(fileExtension) || "flv".equals(fileExtension)
					|| "mkv".equals(fileExtension))) 
			{
				numberOfFiles++;	
			}
		}

		assertTrue(numberOfFiles > 7);

		long totalVodCount = datastore.getTotalVodNumber();
		assertEquals(0, totalVodCount);
		assertEquals(numberOfFiles, datastore.fetchUserVodList(f));

		//we know there are files there
		//test_short.flv
		//test_video_360p_subtitle.flv
		//test_Video_360p.flv
		//test.flv
		//sample_MP4_480.mp4
		//high_profile_delayed_video.flv
		//test_video_360p_pcm_audio.mkv
		//test_hevc.flv

		totalVodCount = datastore.getTotalVodNumber();
		assertEquals(numberOfFiles, totalVodCount);

		List<VoD> vodList = datastore.getVodList(0, 50, null, null, null, null);
		assertEquals(numberOfFiles, vodList.size());
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

		assertNull(voD.getProcessStatus());
		datastore.updateVoDProcessStatus(voD.getVodId(), VoD.PROCESS_STATUS_INQUEUE);

		voD = datastore.getVoD(userVod.getVodId());
		assertEquals(VoD.PROCESS_STATUS_INQUEUE, voD.getProcessStatus());

		assertEquals(0, voD.getProcessStartTime());
		assertEquals(0, voD.getProcessEndTime());

		datastore.updateVoDProcessStatus(voD.getVodId(), VoD.PROCESS_STATUS_PROCESSING);
		voD = datastore.getVoD(userVod.getVodId());
		assertNotEquals(0, voD.getProcessStartTime());
		assertEquals(0, voD.getProcessEndTime());

		datastore.updateVoDProcessStatus(voD.getVodId(), VoD.PROCESS_STATUS_FAILED);
		voD = datastore.getVoD(userVod.getVodId());
		assertNotEquals(0, voD.getProcessStartTime());
		assertNotEquals(0, voD.getProcessEndTime());




		//delete streamVod
		datastore.deleteVod(streamVod.getVodId());
		assertNull(datastore.getVoD(streamVod.getVodId()));

		assertEquals(1, datastore.getTotalVodNumber());

		//delete userVod
		datastore.deleteVod(userVod.getVodId());
		assertNull(datastore.getVoD(voD.getVodId()));

		//check vod number
		assertEquals(0, datastore.getTotalVodNumber());


		//check finished time
		VoD userVod2 =new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, 111, VoD.USER_VOD,vodId,null);

		datastore.addVod(userVod2);
		voD = datastore.getVoD(userVod2.getVodId());
		assertEquals(0, voD.getProcessStartTime());
		assertEquals(0, voD.getProcessEndTime());

		datastore.updateVoDProcessStatus(userVod2.getVodId(), VoD.PROCESS_STATUS_FINISHED);
		voD = datastore.getVoD(userVod2.getVodId());
		assertEquals(0, voD.getProcessStartTime());
		assertNotEquals(0, voD.getProcessEndTime());

		datastore.deleteVod(userVod2.getVodId());
		assertEquals(0, datastore.getTotalVodNumber());

		VoD userVod3 =new VoD("streamName", "streamId", "filePath", "vodName", 111, 111, 111, 111, VoD.USER_VOD,vodId,null);
		userVod3.setLatitude("10");
		userVod3.setLongitude("15");
		userVod3.setDescription("my vod");
		userVod3.setMetadata("my metadata");
		userVod3.setAltitude("20");
		datastore.addVod(userVod3);
		voD = datastore.getVoD(userVod3.getVodId());

		assertEquals("10", voD.getLatitude());
		assertEquals("15", voD.getLongitude());
		assertEquals("20", voD.getAltitude());
		assertEquals("my vod", voD.getDescription());
		assertEquals("my metadata", voD.getMetadata());

		datastore.deleteVod(userVod3.getVodId());
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

		BroadcastUpdate cameraUpdate = new BroadcastUpdate();
		cameraUpdate.setName("new_name");
		cameraUpdate.setIpAddr("1.1.1.1");


		datastore.updateBroadcastFields(camera.getStreamId(), cameraUpdate);

		//check whether is changed or not
		camera = datastore.get(camera.getStreamId());
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

		BroadcastUpdate broadcastUpdate = new BroadcastUpdate();
		broadcastUpdate.setMetaData(newMetadata);
		datastore.updateBroadcastFields(broadcast.getStreamId(), broadcastUpdate);

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
	
	public void testUpdateDashViewerCount(DataStore dataStore) {
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
			assertTrue(dataStore.updateDASHViewerCount(key, viewerCount));

			totalCountFor1 += viewerCount;

			int viewerCount2 = (int)(Math.random()*99999);
			if (viewerCount2 % 2 == 0) {
				viewerCount2 = -1 * viewerCount2;
			}
			assertTrue(dataStore.updateDASHViewerCount(key2, viewerCount2));
			totalCountFor2 += viewerCount2;

			assertEquals(totalCountFor1, dataStore.get(key).getDashViewerCount());
			assertEquals(totalCountFor2, dataStore.get(key2).getDashViewerCount());

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
		broadcast1.setUpdateTime(System.currentTimeMillis());
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
		assertEquals(broadcast2.getEndPointList().get(0).getEndpointUrl(), rtmpUrl);

		//
		Endpoint endPoint3Clone = new Endpoint(
				endPoint2.getEndpointUrl(), endPoint2.getType(), null, "finished");

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
		assertEquals(broadcast2.getEndPointList().get(0).getEndpointUrl(), rtmpUrl);

		//
		Endpoint endPoint3Clone = new Endpoint(
				endPoint2.getEndpointUrl(), endPoint2.getType(), "generic_2", "finished");

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
			BroadcastUpdate tmp = new BroadcastUpdate();
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
			assertNull(tmp.getPlaylistLoopEnabled());
			tmp.setPlaylistLoopEnabled(false);
			double speed = 1.0;
			tmp.setSpeed(speed);
			tmp.setSeekTimeInMs(136);
			tmp.setHlsViewerCount(10);
			tmp.setWebRTCViewerCount(12);
			tmp.setDashViewerCount(13);
			tmp.setUsername("user");
			tmp.setPassword("pass");
			tmp.setStreamUrl("url");
			tmp.setConferenceMode("mode");
			tmp.setPlannedStartDate(100L);
			tmp.setPlannedEndDate(200L);
			tmp.setAbsoluteStartTimeMs(197L);
			tmp.setReceivedBytes(1234L);
			tmp.setBitrate(2000L);
			tmp.setUserAgent("agent");
			tmp.setHlsViewerLimit(100);
			tmp.setWebRTCViewerLimit(200);
			tmp.setDashViewerLimit(300);
			tmp.setWidth(1280);
			tmp.setHeight(720);
			tmp.setEncoderQueueSize(250);
			tmp.setDropPacketCountInIngestion(50);
			tmp.setDropFrameCountInEncoding(60);
			tmp.setJitterMs(130);
			tmp.setRttMs(120);
			tmp.setPacketLostRatio(3.5);
			tmp.setPacketsLost(123);
			tmp.setRemoteIp("remip");
			tmp.setVirtual(false);
			tmp.setAutoStartStopEnabled(false);
			tmp.setSubtracksLimit(13);
			List<String> ids = Arrays.asList("st1", "st2");
			tmp.setSubTrackStreamIds(ids);
			tmp.setSubTrackStreamIds(ids);


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
			assertEquals(10, broadcast2.getHlsViewerCount());
			assertEquals(12, broadcast2.getWebRTCViewerCount());
			assertEquals(13, broadcast2.getDashViewerCount());
			
			assertEquals("user", broadcast2.getUsername());
			assertEquals("pass", broadcast2.getPassword());
			assertEquals("url", broadcast2.getStreamUrl());
			assertEquals("mode", broadcast2.getConferenceMode());
			assertEquals(100L, broadcast2.getPlannedStartDate());
			assertEquals(200L, broadcast2.getPlannedEndDate());
			assertEquals(197L, broadcast2.getAbsoluteStartTimeMs());
			assertEquals(1234L, broadcast2.getReceivedBytes());
			assertEquals(2000L, broadcast2.getBitrate());
			assertEquals("agent", broadcast2.getUserAgent());
			assertEquals(100, broadcast2.getHlsViewerLimit());
			assertEquals(200, broadcast2.getWebRTCViewerLimit());
			assertEquals(300, broadcast2.getDashViewerLimit());
			assertEquals(1280, broadcast2.getWidth());
			assertEquals(720, broadcast2.getHeight());
			assertEquals(250, broadcast2.getEncoderQueueSize());
			assertEquals(50, broadcast2.getDropPacketCountInIngestion());
			assertEquals(60, broadcast2.getDropFrameCountInEncoding());
			assertEquals(3.5, broadcast2.getPacketLostRatio(), 0.0001); // Note: overwritten value
			assertEquals(123, broadcast2.getPacketsLost());
			assertEquals(130, broadcast2.getJitterMs());
			assertEquals(120, broadcast2.getRttMs());
			assertEquals("remip", broadcast2.getRemoteIp());
			assertFalse(broadcast2.isVirtual());
			assertFalse(broadcast2.isAutoStartStopEnabled());
			assertEquals(13, broadcast2.getSubtracksLimit());
			assertEquals(2, broadcast2.getSubTrackStreamIds().size());
			assertEquals("st1", broadcast2.getSubTrackStreamIds().get(0));
			assertEquals("st2", broadcast2.getSubTrackStreamIds().get(1));



			BroadcastUpdate update = new BroadcastUpdate();
			update.setDuration(100000L);

			result = dataStore.updateBroadcastFields(broadcast.getStreamId().toString(), update);
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
			assertEquals(broadcast2.getEndPointList().get(0).getEndpointUrl(), rtmpUrl);

			rtmpUrl = "rtmp:(sdfsfsf(ksklasjflakjflaskjflsadfkjsal";
			endPoint = new Endpoint(rtmpUrl, "facebook", null, "finished");

			result = dataStore.addEndpoint(broadcast2.getStreamId().toString(), endPoint);
			assertTrue(result);

			broadcast2 = dataStore.get(key);
			assertNotNull(broadcast2.getEndPointList());
			assertEquals(2, broadcast2.getEndPointList().size());
			assertEquals(broadcast2.getEndPointList().get(1).getEndpointUrl(), rtmpUrl);

			Broadcast broadcast3 = new Broadcast("test3");
			broadcast3.setQuality("poor");
			assertNotNull(broadcast3.getQuality());
			dataStore.save(broadcast3);

			logger.info("Saved id {}", broadcast3.getStreamId());

			assertEquals(broadcast3.getStreamId(), dataStore.get(broadcast3.getStreamId()).getStreamId());

			update = new BroadcastUpdate();
			update.setQuality("poor");
			update.setSpeed(0.1);
			update.setPendingPacketSize(0);

			result = dataStore.updateBroadcastFields(broadcast.getStreamId().toString(), update);
			assertTrue(result);
			//it's poor because it's not updated because of null
			assertEquals("poor", dataStore.get(broadcast3.getStreamId()).getQuality());


			update = new BroadcastUpdate();
			update.setQuality("good");
			update.setSpeed(0.0);
			update.setPendingPacketSize(0);
			result = dataStore.updateBroadcastFields(broadcast3.getStreamId().toString(), update);
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

		// Test search by metadata
		VoD vodWithMetadata = new VoD("metaStream", "meta123" + (int)(Math.random() * 1000), "path", "metaVodName", 1517239908, 123, 17933, 1190425, VoD.STREAM_VOD, "metaVodId" + (int)(Math.random() * 91000), null);
		vodWithMetadata.setMetadata("team=A,event=championship");
		dataStore.addVod(vodWithMetadata);

		vodList = dataStore.getVodList(0, 50, null, null, null, "team=A");
		assertEquals(1, vodList.size());
		assertEquals(vodWithMetadata.getVodId(), vodList.get(0).getVodId());

		vodList = dataStore.getVodList(0, 50, null, null, null, "championship");
		assertEquals(1, vodList.size());
		assertEquals(vodWithMetadata.getVodId(), vodList.get(0).getVodId());

		partialVodNumber = dataStore.getPartialVodNumber("team=A");
		assertEquals(1, partialVodNumber);

		// Test search by description
		VoD vodWithDescription = new VoD("descStream", "desc123" + (int)(Math.random() * 1000), "path", "descVodName", 1517239908, 123, 17933, 1190425, VoD.STREAM_VOD, "descVodId" + (int)(Math.random() * 91000), null);
		vodWithDescription.setDescription("Important recorded segment from Event B");
		dataStore.addVod(vodWithDescription);

		vodList = dataStore.getVodList(0, 50, null, null, null, "Event B");
		assertEquals(1, vodList.size());
		assertEquals(vodWithDescription.getVodId(), vodList.get(0).getVodId());

		vodList = dataStore.getVodList(0, 50, null, null, null, "recorded segment");
		assertEquals(1, vodList.size());
		assertEquals(vodWithDescription.getVodId(), vodList.get(0).getVodId());

		partialVodNumber = dataStore.getPartialVodNumber("Event B");
		assertEquals(1, partialVodNumber);

		// Test case insensitive search for metadata and description
		vodList = dataStore.getVodList(0, 50, null, null, null, "TEAM=A");
		assertEquals(1, vodList.size());

		vodList = dataStore.getVodList(0, 50, null, null, null, "event b");
		assertEquals(1, vodList.size());

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


	public  void testSubscriberAvgBitrate(DataStore store) {
		String streamId = "stream1";

		appSettings.setWriteSubscriberEventsToDatastore(true);


		// create a subscriber play
		Subscriber subscriberPlay = new Subscriber();
		subscriberPlay.setStreamId(streamId);
		subscriberPlay.setSubscriberId("subscriber1");
		subscriberPlay.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
		subscriberPlay.setType(Subscriber.PLAY_TYPE);
		assertTrue(store.addSubscriber(subscriberPlay.getStreamId(), subscriberPlay));

		assertTrue(store.updateSubscriberBitrateEvent(streamId, subscriberPlay.getSubscriberId(), 1000, 2000));

		Subscriber subscriber = store.getSubscriber(streamId, subscriberPlay.getSubscriberId());
		assertEquals(1000, subscriber.getAvgVideoBitrate());
		assertEquals(2000, subscriber.getAvgAudioBitrate());


		appSettings.setWriteSubscriberEventsToDatastore(true);



	}

	public void testConnectionEventsBug(DataStore store) {
		String streamIdPlay = "stream1";
		String streamIdPublish = "stream2";
		store.revokeSubscribers(streamIdPlay);
		store.revokeSubscribers(streamIdPublish);

		appSettings.setWriteSubscriberEventsToDatastore(true);

		// create a subscriber play
		Subscriber subscriberPlay = new Subscriber();
		subscriberPlay.setStreamId(streamIdPlay);
		subscriberPlay.setSubscriberId("subscriber1");
		subscriberPlay.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
		subscriberPlay.setType(Subscriber.PLAY_TYPE);
		assertTrue(store.addSubscriber(subscriberPlay.getStreamId(), subscriberPlay));

		// create a subscriber publish
		Subscriber subscriberPub = new Subscriber();
		subscriberPub.setStreamId(streamIdPublish);
		subscriberPub.setSubscriberId("subscriber2");
		subscriberPub.setB32Secret("6qsp6qhndryqs56zjmvs37i6gqtjsdvc");
		subscriberPub.setType(Subscriber.PUBLISH_TYPE);
		assertTrue(store.addSubscriber(subscriberPub.getStreamId(), subscriberPub));

		ConnectionEvent connected = new ConnectionEvent();
		connected.setEventType(ConnectionEvent.CONNECTED_EVENT);
		long eventTime = 20;
		connected.setTimestamp(eventTime);
		connected.setType(Subscriber.PLAY_TYPE);
		String hostAddress = ServerSettings.getLocalHostAddress();
		connected.setInstanceIP(hostAddress);


		assertTrue(store.addSubscriberConnectionEvent(subscriberPub.getStreamId(), subscriberPub.getSubscriberId(), connected));

		connected = new ConnectionEvent();
		connected.setEventType(ConnectionEvent.CONNECTED_EVENT);
		eventTime = 20;
		connected.setTimestamp(eventTime);
		connected.setType(Subscriber.PLAY_TYPE);
		hostAddress = ServerSettings.getLocalHostAddress();
		connected.setInstanceIP(hostAddress);

		assertTrue(store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), connected));
		
		
		assertEquals(1, store.getConnectionEvents(subscriberPub.getStreamId(), null, 0, 50).size());
		assertEquals(1, store.getConnectionEvents(subscriberPlay.getStreamId(), null, 0, 50).size());



	}

	public void testTimeBasedSubscriberOperations(DataStore store) {
		// clean db in the begining of the test
		String streamId = "stream1";
		store.revokeSubscribers(streamId);

		//default value must be false
		appSettings.setWriteSubscriberEventsToDatastore(true);


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
		subscriberPub.setSubscriberName("subscriber2Name");
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
		assertEquals(subscriberPub.getSubscriberName(), written.getSubscriberName());
		assertEquals("subscriber2Name", written.getSubscriberName());


		ConnectionEvent connected = new ConnectionEvent();
		connected.setEventType(ConnectionEvent.CONNECTED_EVENT);
		long eventTime = 20;
		connected.setTimestamp(eventTime);
		connected.setType(Subscriber.PLAY_TYPE);
		String hostAddress = ServerSettings.getLocalHostAddress();
		connected.setInstanceIP(hostAddress);


		store.addSubscriberConnectionEvent(subscriberPub.getStreamId(), subscriberPub.getSubscriberId(), connected);

		assertEquals(1, store.getConnectionEvents(subscriberPub.getStreamId(), subscriberPub.getSubscriberId(), 0, 50).size());
		assertEquals(1, store.getConnectionEvents(subscriberPub.getStreamId(), null, 0, 50).size());


		//delete this subscriber
		assertTrue(store.deleteSubscriber(subscriberPub.getStreamId(), subscriberPub.getSubscriberId()));

		assertEquals(0, store.getConnectionEvents(subscriberPub.getStreamId(), subscriberPub.getSubscriberId(), 0, 50).size());
		assertEquals(0, store.getConnectionEvents(subscriberPub.getStreamId(), null, 0, 50).size());

		subscribers = store.listAllSubscribers(streamId, 0, 10);
		subscriberStats = store.listAllSubscriberStats(streamId, 0, 10);


		//it should be zero because subscriber is deleted
		assertEquals(0, subscribers.size());
		assertEquals(0, subscriberStats.size());
		assertEquals(0, store.getConnectionEvents(streamId, written.getSubscriberId(), 0, 50).size());




		//create subscriber again
		assertTrue(store.addSubscriber(subscriberPlay.getStreamId(), subscriberPlay));

		connected = new ConnectionEvent();
		connected.setEventType(ConnectionEvent.CONNECTED_EVENT);
		eventTime = 20;
		connected.setTimestamp(eventTime);
		connected.setType(Subscriber.PLAY_TYPE);
		hostAddress = ServerSettings.getLocalHostAddress();
		connected.setInstanceIP(hostAddress);

		ConnectionEvent disconnected = new ConnectionEvent();
		disconnected.setEventType(ConnectionEvent.DISCONNECTED_EVENT);
		long eventTimeDisconnect = 21;
		disconnected.setTimestamp(eventTimeDisconnect);		

		// add connected event
		store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), connected);
		// isConnected should be true
		assertTrue(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		assertEquals(1, store.getConnectionEvents(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), 0, 50).size());
		assertEquals(1, store.getConnectionEvents(subscriberPlay.getStreamId(), null, 0, 50).size());

		// add disconnected event
		store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), disconnected);
		written = store.getSubscriber(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId());

		// isConnected should return false
		assertFalse(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		assertFalse(written.isConnected());
		assertEquals(0, written.getCurrentConcurrentConnections());

		// there should be two events with correct order
		List<ConnectionEvent> events = store.getConnectionEvents(written.getStreamId(), written.getSubscriberId(), 0, 50);

		assertEquals(2, events.size());  

		assertEquals(ConnectionEvent.CONNECTED_EVENT, events.get(0).getEventType());
		assertEquals(Subscriber.PLAY_TYPE, events.get(0).getType());
		assertEquals(hostAddress, events.get(0).getInstanceIP());
		assertEquals(eventTime, events.get(0).getTimestamp());
		assertEquals(ConnectionEvent.DISCONNECTED_EVENT, events.get(1).getEventType());

		connected = new ConnectionEvent();
		connected.setEventType(ConnectionEvent.CONNECTED_EVENT);
		eventTime = 20;
		connected.setTimestamp(eventTime);
		connected.setType(Subscriber.PLAY_TYPE);
		hostAddress = ServerSettings.getLocalHostAddress();
		connected.setInstanceIP(hostAddress);

		// add connected event
		store.addSubscriberConnectionEvent(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), connected);
		// isConnected should be true again
		assertTrue(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));

		// reset connection status
		assertTrue(store.resetSubscribersConnectedStatus());
		// connection status should false again
		assertFalse(store.isSubscriberConnected(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId()));
		events = store.getConnectionEvents(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), 0, 50);

		assertEquals(3, events.size());

		events = store.getConnectionEvents(subscriberPlay.getStreamId(), null, 0, 50);
		assertEquals(3, events.size());


		store.revokeSubscribers(streamId);

		events = store.getConnectionEvents(subscriberPlay.getStreamId(), subscriberPlay.getSubscriberId(), 0, 50);
		assertEquals(0, events.size());
		events = store.getConnectionEvents(subscriberPlay.getStreamId(), null, 0, 50);
		assertEquals(0, events.size());


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


		appSettings.setWriteSubscriberEventsToDatastore(false);


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

	private DataStore createDB(String type, boolean writeStats) {
		DataStoreFactory dsf = new DataStoreFactory();

		dsf.setWriteStatsToDatastore(writeStats);
		dsf.setDbType(type);
		dsf.setDbName("testdb");
		dsf.setDbHost("127.0.0.1");
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBean(IAntMediaStreamHandler.VERTX_BEAN_NAME)).thenReturn(vertx);
		AppSettings appSettings = new AppSettings();
		appSettings.setWriteStatsToDatastore(writeStats);
		when(context.getBean(AppSettings.BEAN_NAME)).thenReturn(appSettings);
		when(context.getBean(ServerSettings.BEAN_NAME)).thenReturn(new ServerSettings());


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
		broadcast3.setUpdateTime(System.currentTimeMillis());
		broadcast3.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);	
		broadcast3.setWebRTCViewerCount(104);
		broadcast3.setHlsViewerCount(305);
		broadcast3.setRtmpViewerCount(506);
		dataStore.save(broadcast3);

		Broadcast broadcast4 = new Broadcast();
		broadcast4.setUpdateTime(System.currentTimeMillis());
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
		ss2.setUpdateTime(System.currentTimeMillis());
		ss2.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

		Broadcast ss3 = new Broadcast("ss3");
		ss3.setType(AntMediaApplicationAdapter.STREAM_SOURCE);
		ss3.setUpdateTime(System.currentTimeMillis());
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

		BroadcastUpdate updateData = new BroadcastUpdate();
		for (Endpoint tmpEndpoint : endPointList) {
			if (tmpEndpoint.getEndpointUrl().equals(rtmpUrl)) {
				tmpEndpoint.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED);

				break;
			}
		}

		updateData.setEndPointList(endPointList);

		//update rtmpurl
		result = dataStore.updateBroadcastFields(broadcast.getStreamId(), updateData); 
		assertTrue(result);



		tmpBroadcast = dataStore.get(broadcast.getStreamId());
		endPointList = tmpBroadcast.getEndPointList();
		for (Endpoint tmpEndpoint : endPointList) {
			if (tmpEndpoint.getEndpointUrl().equals(rtmpUrl2)) {
				tmpEndpoint.setStatus(IAntMediaStreamHandler.BROADCAST_STATUS_BROADCASTING);
				break;
			}
		}
		updateData = new BroadcastUpdate();
		updateData.setEndPointList(endPointList);

		result = dataStore.updateBroadcastFields(broadcast.getStreamId(), updateData); 
		assertTrue(result);



		Broadcast updated = dataStore.get(broadcast.getStreamId());
		List<Endpoint> endpList = updated.getEndPointList();
		for(int i = 0; i < endpList.size(); i++){
			Endpoint e = endpList.get(i);
			if(e.getEndpointUrl().equals(rtmpUrl)){
				assertEquals(IAntMediaStreamHandler.BROADCAST_STATUS_FAILED, e.getStatus());
			}
			else if(e.getEndpointUrl().equals(rtmpUrl2)){
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
		BroadcastUpdate updateData = new BroadcastUpdate();
		updateData.setUpdateTime(now);
	    updateData.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
	    updateData.setStartTime(now);
		dataStore.updateBroadcastFields(streamId, updateData);

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

		BroadcastUpdate updateData = new BroadcastUpdate();
		updateData.setLatitude(latitude);
		updateData.setLongitude(longitude);
		updateData.setAltitude(altitude);
		updateData.setStatus(null);
		assertTrue(dataStore.updateBroadcastFields(streamId, updateData));

		Broadcast broadcastFromStore2 = dataStore.get(streamId);
		assertEquals(latitude, broadcastFromStore2.getLatitude());
		assertEquals(longitude, broadcastFromStore2.getLongitude());
		assertEquals(altitude, broadcastFromStore2.getAltitude());

		if (!(dataStore instanceof InMemoryDataStore) &&
				!(dataStore instanceof MongoStore) //because of caching
				) {
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

		BroadcastUpdate updateData = new BroadcastUpdate();
		updateData.setPlayListStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
		updateData.setPlayListItemList(broadcastList);
		updateData.setCurrentPlayIndex(10);
		assertTrue(dataStore.updateBroadcastFields(streamId, updateData));

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
		BroadcastUpdate updateData = new BroadcastUpdate();
		updateData.setMainTrackStreamId(mainTrackId);
		assertTrue(dataStore.updateBroadcastFields(subTrackId, updateData));

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

	public void testUpdateRole(DataStore dataStore) {

		final String INITIAL_ROLE  = "INITIAL_ROLE";
		final String UPDATED_ROLE  = "UPDATED_ROLE";

		String id = RandomStringUtils.randomAlphanumeric(8);

		Broadcast broadcast= new Broadcast();
		try {
			broadcast.setStreamId(id);
		} catch (Exception e) {
			e.printStackTrace();
		}

		broadcast.setRole(INITIAL_ROLE);
		dataStore.save(broadcast);

		assertEquals(INITIAL_ROLE, dataStore.get(id).getRole());

		BroadcastUpdate updateData = new BroadcastUpdate();

		updateData.setRole(UPDATED_ROLE);

		assertTrue(dataStore.updateBroadcastFields(id, updateData));

		assertEquals(UPDATED_ROLE, dataStore.get(id).getRole());
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

	public void testUpdateBroadcastEncoderSettings(DataStore dataStore) {

		String id = RandomStringUtils.randomAlphanumeric(16);

		Broadcast broadcast= new Broadcast();
		try {
			broadcast.setStreamId(id);
		} catch (Exception e) {
			e.printStackTrace();
		}


		assertNotNull(dataStore.save(broadcast));

		assertNull(dataStore.get(id).getEncoderSettingsList());

		List<EncoderSettings> settingsList = new ArrayList<>();
		settingsList.add(new EncoderSettings(720, 50000, 32000, true));

		broadcast.setEncoderSettingsList(settingsList);
		
		BroadcastUpdate updateData = new BroadcastUpdate();
		updateData.setEncoderSettingsList(settingsList);

		assertTrue(dataStore.updateBroadcastFields(id, updateData));

		assertEquals(32000, dataStore.get(id).getEncoderSettingsList().get(0).getAudioBitrate());
		assertEquals(50000, dataStore.get(id).getEncoderSettingsList().get(0).getVideoBitrate());

		if (!(dataStore instanceof InMemoryDataStore) &&
				!(dataStore instanceof MongoStore) //because of cache
				) {
			//because inmemorydata store just keeps the reference, it will be updated
			broadcast.setEncoderSettingsList(null);
		} 

		//it will not be updated because encoder settings is null
		assertEquals(32000, dataStore.get(id).getEncoderSettingsList().get(0).getAudioBitrate());
		assertEquals(50000, dataStore.get(id).getEncoderSettingsList().get(0).getVideoBitrate());


		updateData.setEncoderSettingsList(new ArrayList<>());
		assertTrue(dataStore.updateBroadcastFields(id, updateData));
		assertTrue(dataStore.get(id).getEncoderSettingsList().isEmpty());


	}

	@Test
	public void testInfinityLoopBug() {

		Map mockMap = mock(Map.class);
		Set mockValues = mock(Set.class);
		when(mockMap.values()).thenReturn(mockValues);
		when(mockMap.size()).thenReturn(RandomUtils.nextInt(10, 100));
		Iterator<String> mockIterator = mock(Iterator.class);

		//create an infinite loop, because some corrupted files cause this
		when(mockValues.iterator()).thenReturn(mockIterator);
		when(mockIterator.hasNext()).thenReturn(true);
		when(mockIterator.next()).thenReturn("{\"streamId\":\"aaa\",\"name\":\"bbb\",\"type\":\"playlist\"}\n");

		class MyDB extends MapBasedDataStore {
			public MyDB(String dbName) {
				super(dbName);
				map = mockMap;
			}

			public void close(boolean deleteDB) {}
			public long getLocalLiveBroadcastCount(String hostAddress) {return 0;}
			public List<Broadcast> getLocalLiveBroadcasts(String hostAddress) {return null;}
		};


		MyDB db = new MyDB("test");
		List<Broadcast> list = db.getBroadcastListV2("playlist", null);

		assertEquals(mockMap.size()+1, list.size());
	}

	private void testGetSubtracks(DataStore dataStore) 
	{
		String mainTrackId = RandomStringUtils.randomAlphanumeric(8);
		String role1 = "role1";
		String role2 = "role2";

		for (int i = 0; i < 10; i++) {
			Broadcast broadcast = new Broadcast();
			broadcast.setName("subtrackTrackName"+i);
			broadcast.setType(AntMediaApplicationAdapter.LIVE_STREAM);
			try {
				broadcast.setStreamId("subtrackTrackId"+i);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			broadcast.setMainTrackStreamId(mainTrackId);
			broadcast.setRole(i%2 == 0 ? role1 : role2);
			dataStore.save(broadcast);
		}

		List<Broadcast> subtracks = dataStore.getSubtracks(mainTrackId, 0, 3, null);
		assertEquals(3, subtracks.size());
		for (Broadcast broadcast : subtracks) {
			assertEquals(mainTrackId, broadcast.getMainTrackStreamId());
		}

		subtracks = dataStore.getSubtracks(mainTrackId, 0, 3, role1);
		assertEquals(3, subtracks.size());
		for (Broadcast broadcast : subtracks) {
			assertEquals(mainTrackId, broadcast.getMainTrackStreamId());
			assertEquals(role1, broadcast.getRole());
		}

		subtracks = dataStore.getSubtracks(mainTrackId, 0, 3, role2);
		assertEquals(3, subtracks.size());
		for (Broadcast broadcast : subtracks) {
			assertEquals(mainTrackId, broadcast.getMainTrackStreamId());
			assertEquals(role2, broadcast.getRole());
		}

		subtracks = dataStore.getSubtracks(mainTrackId, 2, 3, role1);
		assertEquals(3, subtracks.size());
		for (Broadcast broadcast : subtracks) {
			assertEquals(mainTrackId, broadcast.getMainTrackStreamId());
			assertEquals(role1, broadcast.getRole());
		}


		subtracks = dataStore.getSubtracks(mainTrackId, 0, 10, role1);
		assertEquals(5, subtracks.size());
		for (Broadcast broadcast : subtracks) {
			assertEquals(mainTrackId, broadcast.getMainTrackStreamId());
			assertEquals(role1, broadcast.getRole());
		}

		subtracks = dataStore.getSubtracks(mainTrackId, 5, 20, null);
		assertEquals(5, subtracks.size());
		for (Broadcast broadcast : subtracks) {
			assertEquals(mainTrackId, broadcast.getMainTrackStreamId());
		}
	}


	public void testGetSubtracksWithStatus(DataStore dataStore) {

		String role1 = "role1";
		String role2 = "role2";

		String mainTrackId = RandomStringUtils.randomAlphanumeric(8);

		for (int i = 0; i < 100; i++) {
			Broadcast broadcast = new Broadcast();
			broadcast.setType(AntMediaApplicationAdapter.LIVE_STREAM);
			try {
				broadcast.setStreamId("subtrack" + RandomStringUtils.randomAlphanumeric(24));
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			if (i < 50) {
				broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);

				
				broadcast.setUpdateTime(System.currentTimeMillis());
				
			}
			else {
				broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED);
			}
			broadcast.setMainTrackStreamId(mainTrackId);
			broadcast.setRole(i%2 == 0 ? role1 : role2);
			dataStore.save(broadcast);
		}

		assertEquals(100, dataStore.getSubtrackCount(mainTrackId, null, null));
		assertEquals(50, dataStore.getSubtrackCount(mainTrackId, null, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));
		assertEquals(25, dataStore.getSubtrackCount(mainTrackId, role1, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));
		assertEquals(25, dataStore.getSubtrackCount(mainTrackId, role2, AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING));

		assertEquals(50, dataStore.getSubtrackCount(mainTrackId, null, AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED));
		assertEquals(25, dataStore.getSubtrackCount(mainTrackId, role1, AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED));
		assertEquals(25, dataStore.getSubtrackCount(mainTrackId, role2, AntMediaApplicationAdapter.BROADCAST_STATUS_FINISHED));


		assertEquals(0, dataStore.getSubtrackCount("nonExistentMainTrack", null, null));

		// it is 50 because 50 subtracks has broadcasting status and their update time is up to date
		assertEquals(50, dataStore.getActiveSubtracksCount(mainTrackId, null));

		assertEquals(25, dataStore.getActiveSubtracksCount(mainTrackId, role1));
		assertEquals(25, dataStore.getActiveSubtracksCount(mainTrackId, role2));
		assertEquals(0, dataStore.getActiveSubtracksCount("nonExistentMainTrack", null));


		List<Broadcast> activeSubtracks = dataStore.getActiveSubtracks(mainTrackId, null);		
		assertEquals(50, activeSubtracks.size());

		activeSubtracks = dataStore.getActiveSubtracks(mainTrackId, role1);		
		assertEquals(25, activeSubtracks.size());

		activeSubtracks = dataStore.getActiveSubtracks(mainTrackId, role2);		
		assertEquals(25, activeSubtracks.size());

		activeSubtracks = dataStore.getActiveSubtracks("nonExistentMainTrack", null);
		assertEquals(0, activeSubtracks.size());

		assertTrue(dataStore.hasSubtracks(mainTrackId));
		assertFalse(dataStore.hasSubtracks("nonExistentMainTrack"));



	}
	
	public void testGetSubtracksWithOrdering(DataStore dataStore) {

		String mainTrackId = RandomStringUtils.randomAlphanumeric(8);

		for (int i = 0; i < 100; i++) {
			Broadcast broadcast = new Broadcast();
			broadcast.setType(AntMediaApplicationAdapter.LIVE_STREAM);
			try {
				broadcast.setStreamId("subtrack" + i);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setUpdateTime(System.currentTimeMillis());
			broadcast.setDate(i+10000);
			broadcast.setMainTrackStreamId(mainTrackId);
			dataStore.save(broadcast);
		}

		List<Broadcast> subtracks = dataStore.getSubtracks(mainTrackId, 3, 5, null, null, "date", "asc", null);
		
		assertEquals(5, subtracks.size());
		
		for (int i = 0; i < 5; i++) {
			assertEquals("subtrack"+(i+3), subtracks.get(i).getStreamId());
		}

		subtracks = dataStore.getSubtracks(mainTrackId, 3, 5, null, null, "date", "desc", null);
		assertEquals(5, subtracks.size());
		
		for (int i = 0; i < 5; i++) {
			assertEquals("subtrack"+(99-3-i), subtracks.get(i).getStreamId());
		}
	}
	
	public void testGetSubtracksWithSearch(DataStore dataStore) {

		String mainTrackId = RandomStringUtils.randomAlphanumeric(8);

		for (int i = 0; i < 100; i++) {
			Broadcast broadcast = new Broadcast();
			broadcast.setType(AntMediaApplicationAdapter.LIVE_STREAM);
			try {
				if(i%10 == 0 ) {
					broadcast.setStreamId("goodsubtrack" + i);
				}
				else {
					broadcast.setStreamId("subtrack" + i);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			broadcast.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_BROADCASTING);
			broadcast.setUpdateTime(System.currentTimeMillis());
			broadcast.setDate(i+10000);
			broadcast.setMainTrackStreamId(mainTrackId);
			dataStore.save(broadcast);
		}

		List<Broadcast> subtracks = dataStore.getSubtracks(mainTrackId, 3, 5, null, null, "date", "asc", "good");
		
		assertEquals(5, subtracks.size());
		
		//goodsubtrack30, goodsubtrack40, goodsubtrack50, goodsubtrack60, goodsubtrack70
		for (int i = 0; i < 5; i++) {
			assertEquals("goodsubtrack"+((i+3)*10), subtracks.get(i).getStreamId());
		}

		subtracks = dataStore.getSubtracks(mainTrackId, 3, 5, null, null, "date", "desc", "good");
		assertEquals(5, subtracks.size());
		
		//goodsubtrack60, goodsubtrack50, goodsubtrack40, goodsubtrack30, goodsubtrack20
		for (int i = 0; i < 5; i++) {
			assertEquals("goodsubtrack"+(90-(3+i)*10), subtracks.get(i).getStreamId());
		}

		
		

	}

	public void testSubscriberCache(DataStore dataStore) {
		long initalExecutedQueryCount = dataStore.getExecutedQueryCount();

		String streamId = "stream"+RandomStringUtils.randomNumeric(6);;

		Subscriber subscriber1 = new Subscriber();
		String subscriberId1 = "subscriberId1";
		subscriber1.setSubscriberId(subscriberId1);
		subscriber1.setStreamId(streamId);
		subscriber1.setSubscriberName("subscriberName1");

		dataStore.addSubscriber(streamId, subscriber1); //executedQueryCount+1

		Subscriber subscriberFromDB = dataStore.getSubscriber(streamId, subscriberId1);

		long executedQueryCount = dataStore.getExecutedQueryCount();

		assertEquals(initalExecutedQueryCount + 1, executedQueryCount );

		String subscriberCacheKey = ((MongoStore) dataStore).getSubscriberCacheKey(streamId,subscriberId1);

		Subscriber subscriberFromCache = ((MongoStore) dataStore).getSubscriberCache().get(subscriberCacheKey,Subscriber.class);

		assertNotNull(subscriberFromCache);

		assertEquals(subscriberFromDB.getSubscriberId(), subscriberFromCache.getSubscriberId());

		subscriberFromDB = dataStore.getSubscriber(streamId,subscriberId1);

		executedQueryCount = dataStore.getExecutedQueryCount();

		assertEquals(initalExecutedQueryCount + 1, executedQueryCount );

		subscriberFromDB = dataStore.getSubscriber(streamId,subscriberId1);

		executedQueryCount = dataStore.getExecutedQueryCount();

		assertEquals(initalExecutedQueryCount + 1, executedQueryCount );

		Subscriber nullSubscriber = dataStore.getSubscriber(streamId, "nullSubscriber"); //executedQueryCount+1

		assertNull(nullSubscriber);

		executedQueryCount = dataStore.getExecutedQueryCount();

		assertEquals(initalExecutedQueryCount + 2, executedQueryCount );


		subscriberCacheKey = ((MongoStore) dataStore).getSubscriberCacheKey(streamId,"nullSubscriber");

		subscriberFromCache =  ((MongoStore) dataStore).getSubscriberCache().get(subscriberCacheKey,Subscriber.class);

		assertNotNull(subscriberFromCache);

		assertNull(subscriberFromCache.getSubscriberId());

		executedQueryCount = dataStore.getExecutedQueryCount();

		assertEquals(initalExecutedQueryCount + 2, executedQueryCount );

		nullSubscriber = dataStore.getSubscriber(streamId, "nullSubscriber");

		assertNull(nullSubscriber);

		executedQueryCount = dataStore.getExecutedQueryCount();

		assertEquals(initalExecutedQueryCount + 2, executedQueryCount );

		assertTrue(dataStore.blockSubscriber(streamId, subscriberId1, Subscriber.PLAY_TYPE, 30));

		subscriberCacheKey = ((MongoStore) dataStore).getSubscriberCacheKey(streamId,subscriberId1);


		subscriberFromCache = ((MongoStore) dataStore).getSubscriberCache().get(subscriberCacheKey, Subscriber.class);

		assertTrue(subscriberFromCache.isBlocked(Subscriber.PLAY_TYPE));

		assertTrue(dataStore.blockSubscriber(streamId, "somesub", Subscriber.PLAY_TYPE, 30));

		subscriberCacheKey =  ((MongoStore) dataStore).getSubscriberCacheKey(streamId,"somesub");

		subscriberFromCache = ((MongoStore) dataStore).getSubscriberCache().get(subscriberCacheKey, Subscriber.class);

		assertTrue(subscriberFromCache.isBlocked(Subscriber.PLAY_TYPE));

		subscriberCacheKey =  ((MongoStore) dataStore).getSubscriberCacheKey(streamId,subscriberId1);

		assertTrue(dataStore.deleteSubscriber(streamId, subscriberId1));

		subscriberFromCache = ((MongoStore) dataStore).getSubscriberCache().get(subscriberCacheKey, Subscriber.class);

		assertNull(subscriberFromCache);

		assertTrue(dataStore.revokeSubscribers(streamId));

		subscriberCacheKey =  ((MongoStore) dataStore).getSubscriberCacheKey(streamId,"somesub");

		subscriberFromCache = ((MongoStore) dataStore).getSubscriberCache().get(subscriberCacheKey, Subscriber.class);

		assertNull(subscriberFromCache);

	}
	
	public void testBroadcastCache(DataStore dataStore) {
		MongoStore mongoDataStore = (MongoStore) dataStore;
		
		assertNotEquals(mongoDataStore.getBroadcastCache(), mongoDataStore.getSubscriberCache());


		String streamId = "stream"+RandomStringUtils.randomNumeric(6);;

		Broadcast broadcast = new Broadcast();
		try {
			broadcast.setStreamId(streamId);
		} catch (Exception e) {
			e.printStackTrace();
		}

		dataStore.save(broadcast);
		
		Broadcast broadcastFromDB = dataStore.get(streamId);

		String broadcastCacheKey = mongoDataStore.getBroadcastCacheKey(streamId);
		Broadcast broadcastFromCache1 = (Broadcast) mongoDataStore.getBroadcastCache().get(broadcastCacheKey, Broadcast.class);
		assertNotNull(broadcastFromCache1);
		assertEquals(broadcastFromDB.getStreamId(), broadcastFromCache1.getStreamId());

		Broadcast broadcastFromDB2 = dataStore.get(streamId);
		assertEquals(broadcastFromDB2, broadcastFromCache1);
		
		try {
			Thread.sleep((MongoStore.BROADCAST_CACHE_EXPIRE_SECONDS + 1) * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Broadcast broadcastFromCache2 = (Broadcast) mongoDataStore.getBroadcastCache().get(broadcastCacheKey, Broadcast.class);
		assertNull(broadcastFromCache2);
		
		Broadcast broadcastFromDB3 = dataStore.get(streamId);
		assertNotEquals(broadcastFromDB3, broadcastFromCache1);
		
		Broadcast broadcastFromCache3 = (Broadcast) mongoDataStore.getBroadcastCache().get(broadcastCacheKey, Broadcast.class);
		assertNotNull(broadcastFromCache3);
		
		dataStore.delete(streamId);
		
		Broadcast broadcastFromCache4 = (Broadcast) mongoDataStore.getBroadcastCache().get(broadcastCacheKey, Broadcast.class);
		assertNull(broadcastFromCache4);
		
		
		
	}
	
	
	public void testConnectedSubscribers(DataStore dataStore) {
		String streamId = "stream"+RandomStringUtils.randomNumeric(6);
		
		int connectedSubscriberCount = (int) (Math.random()*100);
		
		for (int i = 0; i < connectedSubscriberCount; i++) {
			Subscriber subscriber = new Subscriber();
			subscriber.setStreamId(streamId);
			subscriber.setConnected(true);
			subscriber.setSubscriberId("conn"+i);
			dataStore.addSubscriber(streamId, subscriber);
		}
		
		int inconnectedSubscriberCount = (int) (Math.random()*100);
		
		for (int i = 0; i < inconnectedSubscriberCount; i++) {
			Subscriber subscriber = new Subscriber();
			subscriber.setStreamId(streamId);
			subscriber.setConnected(false);
			subscriber.setSubscriberId("inconn"+i);
			dataStore.addSubscriber(streamId, subscriber);
		}
		
		int someOtherSubscribersCount = (int) (Math.random()*100);
		for (int i = 0; i < inconnectedSubscriberCount; i++) {
			Subscriber subscriber = new Subscriber();
			subscriber.setStreamId("something"+RandomStringUtils.randomNumeric(6));
			subscriber.setConnected(inconnectedSubscriberCount%2 == 0);
			subscriber.setSubscriberId("oth"+i);
			dataStore.addSubscriber(streamId, subscriber);
		}
		
		assertEquals(connectedSubscriberCount, dataStore.getConnectedSubscriberCount(streamId));
		
		List<Subscriber> connectedSubscribers = dataStore.getConnectedSubscribers(streamId, 0, 200);
		assertEquals(connectedSubscriberCount, connectedSubscribers.size());
		
		for (Subscriber subscriber : connectedSubscribers) {
			assertTrue(subscriber.isConnected());
			assertEquals(streamId, subscriber.getStreamId());

		}

		
		
	}
	

	/**
	 * Test custom TOTP expiry periods per subscriber
	 */
	public void testCustomTotpExpiry(DataStore dataStore) {
		String streamId = "stream" + RandomStringUtils.randomNumeric(6);
		
		// Test 1: Subscriber with custom expiry time
		Subscriber subscriber1 = new Subscriber();
		subscriber1.setSubscriberId("subscriber1");
		subscriber1.setStreamId(streamId);
		subscriber1.setTotpExpiryPeriodSeconds(60); // Custom 1-minute expiry
		
		assertTrue(dataStore.addSubscriber(streamId, subscriber1));
		
		Subscriber retrievedSubscriber1 = dataStore.getSubscriber(streamId, "subscriber1");
		assertNotNull(retrievedSubscriber1);
		assertEquals(Integer.valueOf(60), retrievedSubscriber1.getTotpExpiryPeriodSeconds());
		
		// Test 2: Subscriber without custom expiry (should be null)
		Subscriber subscriber2 = new Subscriber();
		subscriber2.setSubscriberId("subscriber2");
		subscriber2.setStreamId(streamId);
		// Not setting totpExpiryPeriodSeconds - should remain null
		
		assertTrue(dataStore.addSubscriber(streamId, subscriber2));
		
		Subscriber retrievedSubscriber2 = dataStore.getSubscriber(streamId, "subscriber2");
		assertNotNull(retrievedSubscriber2);
		assertNull(retrievedSubscriber2.getTotpExpiryPeriodSeconds());
	}

}
