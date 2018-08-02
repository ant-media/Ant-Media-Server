package io.antmedia.test.rest;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.red5.server.scope.Scope;
import org.springframework.test.context.ContextConfiguration;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.datastore.db.IDataStore;
import io.antmedia.datastore.db.InMemoryDataStore;
import io.antmedia.datastore.db.types.Broadcast;
import io.antmedia.rest.StreamsSourceRestService;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcher;

@ContextConfiguration(locations = { "test.xml" })
public class StreamSourceRestServiceUnitTest {
	
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
		
		
		Mockito.doReturn("rtsp://11.2.40.63:8554/live1.sdp").when(streamSourceRest).getRTSPSteramURI(newCam);
		Mockito.doReturn(adaptor).when(streamSourceRest).getInstance();
		Mockito.doReturn(fetcher).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(new InMemoryDataStore("testAddIPCamera")).when(streamSourceRest).getStore();

	
		result = streamSourceRest.addStreamSource(newCam);
		
		assertTrue(result.isSuccess());
	}
	
	@Test
	public void testAddStreamSource()  {
		
		Result result = new Result(false);
		
		Broadcast newCam = new Broadcast("testAddStreamSource", "10.2.40.64:8080", "admin", "admin",
				"rtsp://11.2.40.63:8554/live1.sdp", AntMediaApplicationAdapter.STREAM_SOURCE);
		
		StreamsSourceRestService streamSourceRest = Mockito.spy(restService);
		AntMediaApplicationAdapter adaptor = mock (AntMediaApplicationAdapter.class);
		
		
		Mockito.doReturn(adaptor).when(streamSourceRest).getInstance();
		Mockito.doReturn(new InMemoryDataStore("testAddStreamSource")).when(streamSourceRest).getStore();

	
		result = streamSourceRest.addStreamSource(newCam);
		
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
		
		Mockito.doReturn("rtsp://11.2.40.63:8554/live1.sdp").when(streamSourceRest).getRTSPSteramURI(newCam);
		Mockito.doReturn(adaptor).when(streamSourceRest).getInstance();
		Mockito.doReturn(fetcher).when(adaptor).startStreaming(newCam);
		Mockito.doReturn(store).when(streamSourceRest).getStore();
		
		store.save(newCam);

		result = streamSourceRest.updateCamInfo(newCam);
		
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
		
		Mockito.doReturn(adaptor).when(streamSourceRest).getInstance();
		Mockito.doReturn(store).when(streamSourceRest).getStore();
		Mockito.doReturn(settings).when(adaptor).getAppSettings();
		when(settings.getVodFolder()).thenReturn(vodFolder);
		Mockito.doReturn(true).when(adaptor).synchUserVoDFolder(null, vodFolder);


		result = streamSourceRest.synchUserVodList();
		
		assertTrue(result.isSuccess());
	}
	
	

}
