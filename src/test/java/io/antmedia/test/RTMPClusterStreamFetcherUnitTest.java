package io.antmedia.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.bytedeco.ffmpeg.avformat.AVStream;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.ffmpeg.global.avutil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.messaging.IConsumer;
import org.red5.server.scope.BroadcastScope;
import org.red5.server.scope.WebScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.integration.AppFunctionalV2Test;
import io.antmedia.muxer.RtmpProvider;
import io.antmedia.streamsource.RTMPClusterStreamFetcher;
import io.antmedia.streamsource.StreamFetcher.IStreamFetcherListener;
import io.vertx.core.Vertx;
@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class RTMPClusterStreamFetcherUnitTest extends AbstractJUnit4SpringContextTests  {
	
	private WebScope appScope;
	protected static Logger logger = LoggerFactory.getLogger(StreamFetcherUnitTest.class);
	public AntMediaApplicationAdapter app = null;
	private AntMediaApplicationAdapter appInstance;
	private AppSettings appSettings;
	private Vertx vertx;

	
	@Before
	public void before() {

		try {
			AppFunctionalV2Test.delete(new File("webapps/junit/streams"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		File webApps = new File("webapps");
		if (!webApps.exists()) {
			webApps.mkdirs();
		}
		File junit = new File(webApps, "junit");
		if (!junit.exists()) {
			junit.mkdirs();
		}

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

		vertx = (Vertx) applicationContext.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);


		avutil.av_log_set_level(avutil.AV_LOG_INFO);

	}
	
	@Test
	public void testStopIfNoViewer() {
		File flvFile = new File("src/test/resources/test.flv");

		RTMPClusterStreamFetcher fetcher = Mockito.spy(new RTMPClusterStreamFetcher(flvFile.getAbsolutePath(), "stream1", appScope));
		
		
		for (int i=0; i<499; i++) {
			boolean stopIfNoViewer = fetcher.stopIfNoViewer();
			assertFalse(stopIfNoViewer);
		}
		
		RtmpProvider rtmpProvider = Mockito.mock(RtmpProvider.class);
		fetcher.setRtmpProvider(rtmpProvider);
		
		boolean stopIfNoViewer = fetcher.stopIfNoViewer();
		assertFalse(stopIfNoViewer);
		
		BroadcastScope bs = Mockito.mock(BroadcastScope.class);
		Mockito.when(rtmpProvider.getBroadcastScope()).thenReturn(bs);
		
		fetcher.setCheckNumberOfViewers(499);
		stopIfNoViewer = fetcher.stopIfNoViewer();
		//because there is no viewer, it should return true
		assertTrue(stopIfNoViewer);
		
		
		fetcher.setCheckNumberOfViewers(499);
		Mockito.when(bs.getConsumers()).thenReturn(Arrays.asList());
		stopIfNoViewer = fetcher.stopIfNoViewer();
		//because there is no viewer, it should return true
		assertTrue(stopIfNoViewer);
		
		
		fetcher.setCheckNumberOfViewers(499);
		Mockito.when(bs.getConsumers()).thenReturn(Arrays.asList(Mockito.mock(IConsumer.class)));
		stopIfNoViewer = fetcher.stopIfNoViewer();
		//because there is one viewer, it should return true
		assertFalse(stopIfNoViewer);
		
		

	}
	
	@Test
	public void testFetch() 
	{
		
		File flvFile = new File("src/test/resources/test.flv");
		assertTrue(flvFile.exists());
		RTMPClusterStreamFetcher fetcher = Mockito.spy(new RTMPClusterStreamFetcher(flvFile.getAbsolutePath(), "stream1", appScope));
		Mockito.doReturn(flvFile.getAbsolutePath()).when(fetcher).getStreamUrl();

		RtmpProvider rtmpProvider = Mockito.mock(RtmpProvider.class);
		Mockito.doReturn(rtmpProvider).when(fetcher).initRtmpProvider(Mockito.any(), Mockito.any());
		
		logger.info("rtmpProvider: {}", rtmpProvider);
		
		IStreamFetcherListener listener = Mockito.mock(IStreamFetcherListener.class);
		fetcher.setStreamFetcherListener(listener);
		
		fetcher.startStream();
				
		Awaitility.await().atMost(2000, TimeUnit.MILLISECONDS).until(() -> {
			return fetcher.getThreadActive().get() == false;
		});
		
		Mockito.verify(rtmpProvider, Mockito.timeout(5000).atLeast(0)).writePacket(Mockito.any(), (AVStream)Mockito.any());

		
		
		
		Mockito.doReturn(flvFile.getAbsolutePath()).when(fetcher).getStreamUrl();
		fetcher.startStream();
				
		Awaitility.await().atMost(2000, TimeUnit.MILLISECONDS).until(() -> {
			return fetcher.getThreadActive().get() == false;
		});
		
		Mockito.verify(rtmpProvider, Mockito.timeout(5000).atLeast(0)).writePacket(Mockito.any(), (AVStream)Mockito.any());

		Mockito.when(rtmpProvider.prepareIO()).thenReturn(true);
		fetcher.startStream();
				
		Awaitility.await().atMost(2000, TimeUnit.MILLISECONDS).until(() -> {
			return fetcher.getThreadActive().get() == false;
		});
		

		Mockito.verify(rtmpProvider, Mockito.timeout(5000).atLeast(4)).writePacket(Mockito.any(), (AVRational)Mockito.any(), (AVRational)Mockito.any(), Mockito.anyInt());

		
		Mockito.verify(listener, Mockito.timeout(5000).atLeast(4)).streamFinished(Mockito.any());
		
	}

}
