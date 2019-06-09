package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IResourceMonitor;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.datastore.db.types.Endpoint;
import io.antmedia.rest.StreamsSourceRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.social.endpoint.VideoServiceEndpoint;
import io.antmedia.streamsource.StreamFetcher;
import io.antmedia.test.StreamFetcherUnitTest;

@ContextConfiguration(locations = { "test.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class StreamSourceRestServiceUnitTest {

	protected static Logger logger = LoggerFactory.getLogger(StreamSourceRestServiceUnitTest.class);
	private StreamsSourceRestService restService = null;
	public AntMediaApplicationAdapter app = null;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", ".");
	}


	@Before
	public void before() {
		restService = new StreamsSourceRestService();
	}

	@After
	public void after() {
		restService = null;
	}


	@Test
	public void testAddIPCamera()  {

		Result result = new Result(false);

		Broadcast newCam = new Broadcast("testAddIPCamera", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);

		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher fetcher = mock (StreamFetcher.class);
		Result connResult = new Result(true);
		connResult.setMessage("rtsp://11.2.40.63:8554/live1.sdp");

		Mockito.doReturn(connResult).when(streamSourceRest).connectToCamera(newCam);
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(fetcher).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(new InMemoryDataStore("testAddIPCamera")).when(streamSourceRest).getDataStore();


		ApplicationContext appContext = mock(ApplicationContext.class);

		streamSourceRest.setAppCtx(appContext);

		IResourceMonitor monitorService = mock(IResourceMonitor.class);

		when(appContext.getBean(IResourceMonitor.BEAN_NAME)).thenReturn(monitorService);

		//define CPU load above limit
		int cpuLoad = 90;
		int cpuLimit = 80;


		when(monitorService.getAvgCpuUsage()).thenReturn(cpuLoad);
		when(monitorService.getCpuLimit()).thenReturn(cpuLimit);

		//try to add IP camera
		result = streamSourceRest.addStreamSource(newCam,"");

		//should be false because load is above limit
		assertFalse(result.isSuccess());


		//should be -3 because it is CPU Load Error Code
		assertEquals(String.valueOf(-3), result.getMessage());

		//define CPU load below limit
		int cpuLoad2 = 70;
		int cpuLimit2 = 80;


		when(monitorService.getAvgCpuUsage()).thenReturn(cpuLoad2);
		when(monitorService.getCpuLimit()).thenReturn(cpuLimit2);

		result = streamSourceRest.addStreamSource(newCam,"");

		//should be true because load is below limit
		assertTrue(result.isSuccess());

	}


	@Test
	public void testStartStopStreamSource()  {

		//start ONVIF Camera emulator
		StreamFetcherUnitTest.startCameraEmulator();

		//create an IP Camera for emulator
		Broadcast newCam = new Broadcast("startStopIPCamera", "127.0.0.1:8080", "admin", "admin",
				null, AntMediaApplicationAdapter.IP_CAMERA);

		//simulate required operations
		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher fetcher = mock (StreamFetcher.class);
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(fetcher).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(new Result(true)).when(adaptor).stopStreaming(newCam);
		Mockito.doReturn(new InMemoryDataStore("startStopStreamSource")).when(streamSourceRest).getDataStore();

		//add IP Camera first
		assertTrue(streamSourceRest.addIPCamera(newCam, null).isSuccess());

		//stream URL should be defined after ONVIF operations
		//this assignment also ensures that, connection is successful to IP Camera via rest service using ONVIF operations
		assertEquals("rtsp://admin:admin@127.0.0.1:6554/test.flv", newCam.getStreamUrl());
		
		//stop request should trigger application adaptor stopStreaming
		assertTrue(streamSourceRest.stopStreamSource(newCam.getStreamId()).isSuccess());
		
		//reset stream URL and check whether start rest service is able to get stream URL by connecting to camera using ONVIF
		newCam.setStreamUrl(null);
		streamSourceRest.getDataStore().save(newCam);

		//start again via rest service
		assertTrue(streamSourceRest.startStreamSource(newCam.getStreamId()).isSuccess());
				
		newCam = streamSourceRest.getDataStore().get(newCam.getStreamId());
		
		//check that streamURL is set again successfully
		assertEquals("rtsp://admin:admin@127.0.0.1:6554/test.flv", newCam.getStreamUrl());

		//stop camera emulator
		StreamFetcherUnitTest.stopCameraEmulator();
	}

	@Test
	public void testConnectToCamera()  {
		//start ONVIF Camera emulator
		StreamFetcherUnitTest.startCameraEmulator();

		//create a cam broadcast
		Broadcast newCam = new Broadcast("testAddIPCamera", "127.0.0.1:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);

		//simulate required operations
		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher fetcher = mock (StreamFetcher.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(fetcher).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(new InMemoryDataStore("testConnectToCamera")).when(streamSourceRest).getDataStore();

		//try to connect to camera
		Result result =	streamSourceRest.connectToCamera(newCam);

		//message should be RTSP address because it is reachable
		assertEquals("rtsp://127.0.0.1:6554/test.flv", result.getMessage());

		//set wrong IP Address
		newCam.setIpAddr("127.0.0.11:8080");

		//try to connect to camera
		result = streamSourceRest.connectToCamera(newCam);

		//message should be connection error code (-1) because IP is set
		assertEquals(String.valueOf(-1), result.getMessage());


		//stop camera emulator
		StreamFetcherUnitTest.stopCameraEmulator();


	}

	@Test
	public void testSearchOnvifDevices()  {

		//start ONVIF Cam emulator
		StreamFetcherUnitTest.startCameraEmulator();

		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);

		//start ONVIF discovery
		String result[] = streamSourceRest.searchOnvifDevices();

		//it should not null because discovery is performed
		assertNotNull(result);

		//stop camera emulator
		StreamFetcherUnitTest.stopCameraEmulator();

	}

	@Test
	public void testAddStreamSource()  {

		Result result = new Result(false);

		Broadcast newCam = new Broadcast("testAddStreamSource", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.STREAM_SOURCE);

		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);


		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new InMemoryDataStore("testAddStreamSource")).when(streamSourceRest).getDataStore();

		ApplicationContext appContext = mock(ApplicationContext.class);

		streamSourceRest.setAppCtx(appContext);

		IResourceMonitor monitorService = mock(IResourceMonitor.class);

		when(appContext.getBean(IResourceMonitor.BEAN_NAME)).thenReturn(monitorService);

		//define CPU load below limit
		int cpuLoad2 = 70;
		int cpuLimit2 = 80;


		when(monitorService.getAvgCpuUsage()).thenReturn(cpuLoad2);
		when(monitorService.getCpuLimit()).thenReturn(cpuLimit2);

		result = streamSourceRest.addStreamSource(newCam, "");

		//should be true, because CPU load is above limit and other parameters defined correctly
		assertTrue(result.isSuccess());
	}

	@Test
	public void testUpdateCamInfo()  {

		Result result = new Result(false);

		Broadcast newCam = new Broadcast("testUpdateCamInfo", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.IP_CAMERA);


		newCam.setStatus(AntMediaApplicationAdapter.BROADCAST_STATUS_CREATED);
		try {
			newCam.setStreamId("streamId");
		} catch (Exception e) {
			e.printStackTrace();
		}

		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		StreamFetcher fetcher = mock (StreamFetcher.class);
		InMemoryDataStore store = new InMemoryDataStore("test");

		Result connResult = new Result(true);
		connResult.setMessage("rtsp://11.2.40.63:8554/live1.sdp");

		Mockito.doReturn(connResult).when(streamSourceRest).connectToCamera(newCam);
		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(fetcher).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(store).when(streamSourceRest).getDataStore();

		store.save(newCam);

		result = streamSourceRest.updateCamInfo(newCam, "");

		assertTrue(result.isSuccess());
	}

	@Test
	public void synchUserVodList()  {

		Result result = new Result(false);

		String vodFolder = "vodFolder";
		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		InMemoryDataStore store = new InMemoryDataStore("test");
		AppSettings settings = mock(AppSettings.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(store).when(streamSourceRest).getDataStore();
		Mockito.doReturn(settings).when(adaptor).getAppSettings();
		when(settings.getVodFolder()).thenReturn(vodFolder);
		Mockito.doReturn(true).when(adaptor).synchUserVoDFolder(null, vodFolder);


		result = streamSourceRest.synchUserVodList();

		assertTrue(result.isSuccess());
	}

	@Test
	public void testAddStreamSourceWithEndPoint()  {

		Result result = new Result(false);
		Map<String, VideoServiceEndpoint> videoServiceEndpoints = new HashMap<>();

		//When there is no endpoint defined
		Broadcast source = new Broadcast("test_1");
		source.setDescription("");
		source.setIs360(false);
		source.setPublicStream(false);
		source.setType(AntMediaApplicationAdapter.STREAM_SOURCE);

		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);

		Mockito.doReturn(adaptor).when(streamSourceRest).getApplication();
		Mockito.doReturn(new InMemoryDataStore("testAddStreamSourceWithEndPoint")).when(streamSourceRest).getDataStore();
		Mockito.doReturn(true).when(streamSourceRest).checkStreamUrl(any());
		Mockito.doReturn(videoServiceEndpoints).when(adaptor).getVideoServiceEndpoints();


		ApplicationContext appContext = mock(ApplicationContext.class);

		streamSourceRest.setAppCtx(appContext);

		IResourceMonitor monitorService = mock(IResourceMonitor.class);

		when(appContext.getBean(IResourceMonitor.BEAN_NAME)).thenReturn(monitorService);

		//define CPU load below limit
		int cpuLoad2 = 70;
		int cpuLimit2 = 80;


		when(monitorService.getAvgCpuUsage()).thenReturn(cpuLoad2);
		when(monitorService.getCpuLimit()).thenReturn(cpuLimit2);

		result = streamSourceRest.addStreamSource(source, "endpoint_1");
		assertNull(source.getEndPointList());


		//Now we add an endpoint
		VideoServiceEndpoint mockVSEndpoint = mock(VideoServiceEndpoint.class, Mockito.CALLS_REAL_METHODS);
		Endpoint dummyEndpoint = new Endpoint();

		try {
			doReturn(dummyEndpoint).when(mockVSEndpoint).createBroadcast(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt(), anyBoolean());
		} catch (IOException e) {
			e.printStackTrace();
		}

		videoServiceEndpoints.put("endpoint_1", mockVSEndpoint);

		//When there is an endpoint defined
		Broadcast source2 = new Broadcast("test_2");
		source2.setDescription("");
		source2.setIs360(false);
		source2.setPublicStream(false);
		source2.setType(AntMediaApplicationAdapter.STREAM_SOURCE);

		result = streamSourceRest.addStreamSource(source2, "endpoint_1");
		assertEquals(1, source2.getEndPointList().size());

		//Now we add second endpoint
		videoServiceEndpoints.put("endpoint_2", mockVSEndpoint);

		Broadcast source3 = new Broadcast("test_3");
		source3.setDescription("");
		source3.setIs360(false);
		source3.setPublicStream(false);
		source3.setType(AntMediaApplicationAdapter.STREAM_SOURCE);

		//When there is two endpoints defined
		result = streamSourceRest.addStreamSource(source3, "endpoint_1,endpoint_2");
		assertEquals(2, source3.getEndPointList().size());

		//update first source now. At the moment we have endpoint_1
		result = streamSourceRest.updateCamInfo(source, "endpoint_1");
		assertEquals(1, source.getEndPointList().size());
	}



}
