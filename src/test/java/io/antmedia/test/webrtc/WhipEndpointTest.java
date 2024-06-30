package io.antmedia.test.webrtc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.rest.model.Result;
import io.antmedia.webrtc.PublishParameters;
import io.antmedia.whip.WhipEndpoint;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

public class WhipEndpointTest {
	
	
	private static final Logger logger = LoggerFactory.getLogger(WhipEndpointTest.class);


	@Test
	public void testPublishParameters() {
		PublishParameters params = new PublishParameters("stream123");

		// Test streamId getter
		assertEquals("stream123", params.getStreamId());

		// Test default values for enableVideo and enableAudio
		assertTrue(params.isEnableVideo());
		assertTrue(params.isEnableAudio());

		// Test setting and getting enableVideo
		params.setEnableVideo(false);
		assertFalse(params.isEnableVideo());

		// Test setting and getting enableAudio
		params.setEnableAudio(false);
		assertFalse(params.isEnableAudio());

		// Test setting and getting subscriberId
		params.setSubscriberId("sub123");
		assertEquals("sub123", params.getSubscriberId());

		// Test setting and getting subscriberCode
		params.setSubscriberCode("code123");
		assertEquals("code123", params.getSubscriberCode());

		// Test setting and getting streamName
		params.setStreamName("myStream");
		assertEquals("myStream", params.getStreamName());

		// Test setting and getting mainTrack
		params.setMainTrack("mainTrack123");
		assertEquals("mainTrack123", params.getMainTrack());

		// Test setting and getting metaData
		params.setMetaData("metaData123");
		assertEquals("metaData123", params.getMetaData());

		// Test setting and getting linkedSessionForSignaling
		params.setLinkedSessionForSignaling("linkedSession123");
		assertEquals("linkedSession123", params.getLinkedSessionForSignaling());

		// Test setting and getting token
		params.setToken("token123");
		assertEquals("token123", params.getToken());
	}
	
	@Test
	public void testStartWhip() 
	{
		{
		
			WhipEndpoint whipEndpoint = Mockito.spy(new WhipEndpoint());
			
			AntMediaApplicationAdapter app = Mockito.mock(AntMediaApplicationAdapter.class);
			Mockito.doReturn(app).when(whipEndpoint).getApplication();
			
			CompletableFuture<Result> resultFuture = new CompletableFuture<>();
			
			Mockito.when(app.startHttpSignaling(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(resultFuture);
					
			// Test startWhipPublish
			whipEndpoint.startWhipPublish(null, "stream123", null, null, null, null, null, null, null, null, null, null);
			
			resultFuture.complete(new Result(true));
			
			ArgumentCaptor<PublishParameters> publishParamsCaptor = ArgumentCaptor.forClass(PublishParameters.class);
			Mockito.verify(app).startHttpSignaling(publishParamsCaptor.capture(), Mockito.any(), Mockito.anyString());
			Mockito.verify(whipEndpoint, Mockito.timeout(1000)).prepareResponse(Mockito.any(), Mockito.anyString(), Mockito.any());
			
			assertEquals("stream123", publishParamsCaptor.getValue().getStreamId());
			assertTrue(publishParamsCaptor.getValue().isEnableVideo());
			assertTrue(publishParamsCaptor.getValue().isEnableAudio());
		
		}	
	}
	
	@Test
	public void testPrepareResponse() 
	{
        WhipEndpoint whipEndpoint = Mockito.spy(new WhipEndpoint());
        
        Response response = whipEndpoint.prepareResponse(new Result(false, "message"), "sessionId123", null);
        assertEquals(403, response.getStatus());
        assertEquals("message", response.getEntity());
        
        //because null pointer exception
        response = whipEndpoint.prepareResponse(new Result(true, "message"), "sessionId123", null);
        assertEquals(500, response.getStatus());
        
        AntMediaApplicationAdapter app = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(app).when(whipEndpoint).getApplication();
        
		AppSettings appSettings = new AppSettings();
        Mockito.doReturn(appSettings).when(app).getAppSettings();
        
        response = whipEndpoint.prepareResponse(new Result(true, "message"), "sessionId123", null);
        assertEquals(500, response.getStatus());
        
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        
        {
	        Mockito.doReturn(URI.create("http://localhost:5080")).when(uriInfo).getRequestUri();
	        response = whipEndpoint.prepareResponse(new Result(true, "message"), "sessionId123", uriInfo);
	        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
	        
	        assertEquals("sessionId123", response.getHeaders().get("ETag").get(0));
	        assertEquals("stun:stun1.l.google.com:19302; rel=ice-server", response.getHeaders().get("Link").get(0));
	        assertEquals("application/sdp", response.getMediaType().toString());
        }
        
        {
        
	        appSettings.setTurnServerUsername("testuser");
	        appSettings.setTurnServerCredential("testpass");
	        response = whipEndpoint.prepareResponse(new Result(true, "message"), "sessionId123", uriInfo);
	        assertEquals(Status.CREATED.getStatusCode(), response.getStatus());
	        
	        assertEquals("sessionId123", response.getHeaders().get("ETag").get(0));
	     
	        logger.info("Link: {}", response.getHeaders().get("Link").get(0));
	        assertEquals("stun:stun1.l.google.com:19302; rel=ice-server,stun:stun1.l.google.com:19302?transport=udp; rel=\"ice-server\" username=testuser; credential=testpass", response.getHeaders().get("Link").get(0));
	        assertEquals("application/sdp", response.getMediaType().toString());
        }
        
    	

	}
	
	
	@Test
	public void testStopWhip() {
		WhipEndpoint whipEndpoint = Mockito.spy(new WhipEndpoint());

		AntMediaApplicationAdapter app = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(app).when(whipEndpoint).getApplication();

		Mockito.when(app.stopWhipBroadcast(Mockito.anyString(), Mockito.any())).thenReturn(new Result(false, "error message"));

		// Test stopWhipPublish
		Response stopWhipPublish = whipEndpoint.stopWhipPublish("stream123", "etag");
		assertEquals(Status.NOT_FOUND.getStatusCode(), stopWhipPublish.getStatus());
		Result result = (Result) stopWhipPublish.getEntity();
		assertFalse(result.isSuccess());
		assertEquals("error message", result.getMessage());
		
		
		
		
		Mockito.when(app.stopWhipBroadcast(Mockito.anyString(), Mockito.any())).thenReturn(new Result(true));
		stopWhipPublish = whipEndpoint.stopWhipPublish("stream123", "etag");
		assertEquals(Status.OK.getStatusCode(), stopWhipPublish.getStatus());


	}
	
}
