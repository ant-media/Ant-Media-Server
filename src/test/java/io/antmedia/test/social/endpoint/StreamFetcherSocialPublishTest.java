package io.antmedia.test.social.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.bytedeco.javacpp.avformat;
import org.bytedeco.javacpp.avutil;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.MapDBStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.BroadcastStatus;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.datastore.db.types.SocialEndpointCredentials;
import io.antmedia.social.endpoint.PeriscopeEndpoint;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.test.StreamFetcherUnitTest;

@ContextConfiguration(locations = { "../../test.xml" })
public class StreamFetcherSocialPublishTest extends AbstractJUnit4SpringContextTests {

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}
	
	protected static Logger logger = LoggerFactory.getLogger(StreamFetcherSocialPublishTest.class);
	
	public AntMediaApplicationAdapter app = null;
	private WebScope appScope;
	private QuartzSchedulingService scheduler;
	
	@BeforeClass
	public static void beforeClass() {
		avformat.av_register_all();
		avformat.avformat_network_init();
		avutil.av_log_set_level(avutil.AV_LOG_INFO);

	}
	
	@Before
	public void before() {
		
		if (appScope == null) {
			appScope = (WebScope) applicationContext.getBean("web.scope");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}
		
		if (app == null) 
		{

			app = (AntMediaApplicationAdapter) applicationContext.getBean("web.handler");
			logger.debug("Application / web scope: {}", appScope);
			assertTrue(appScope.getDepth() == 1);
		}

		scheduler = (QuartzSchedulingService) applicationContext.getBean(QuartzSchedulingService.BEAN_NAME);
	}
	
	@Test
	public void testCreateBroadcastWithStreamFetcher() {
		try {

			IDataStore dataStoreSocial = new MapDBStore(PeriscopeEndpointTest.TARGET_TEST_PROPERTIES);
			List<SocialEndpointCredentials> socialEndpoints = dataStoreSocial.getSocialEndpoints(0, 10);
			assertEquals(1, socialEndpoints.size());

			IDataStore testDataStore = app.getDataStore();
			PeriscopeEndpoint pscpEndPoint = new PeriscopeEndpoint(PeriscopeEndpointTest.CLIENT_ID, PeriscopeEndpointTest.CLIENT_SECRET, testDataStore, socialEndpoints.get(0), null);
			String name = "stream fetch event name";
			Endpoint endpoint = pscpEndPoint.createBroadcast(name, null, null, false, false, 720, true);
			
			logger.info("rtmp url: {}", endpoint.getRtmpUrl());
			assertNotNull(endpoint.getRtmpUrl());
		
			Broadcast remoteStream = new Broadcast("radio_only", null, null, null, "rtmp://37.247.100.100/shoutcast/karadenizfm.stream",
					AntMediaApplicationAdapter.STREAM_SOURCE);
			String id = testDataStore.save(remoteStream);
			
			testDataStore.addEndpoint(remoteStream.getStreamId(), endpoint);
			
			StreamFetcher fetcher = new StreamFetcher(remoteStream, appScope, scheduler);
			
			fetcher.setRestartStream(false);
			
			fetcher.startStream();
			
			Awaitility.await().atMost(60, TimeUnit.SECONDS)
			.pollInterval(2, TimeUnit.SECONDS)
			.until(() -> {
				return pscpEndPoint.getBroadcast(endpoint).equals(BroadcastStatus.LIVE_NOW);
			});
			
			fetcher.stopStream();
			
			
			Awaitility.await().atMost(60, TimeUnit.SECONDS)
			.pollInterval(2, TimeUnit.SECONDS)
			.until(() -> {
				return pscpEndPoint.getBroadcast(endpoint).equals(BroadcastStatus.UNPUBLISHED);
			});
		
		
		} catch (IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}


	}
	
}
