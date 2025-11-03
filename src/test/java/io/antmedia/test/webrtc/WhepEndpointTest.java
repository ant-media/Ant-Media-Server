package io.antmedia.test.webrtc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.model.Result;
import io.antmedia.webrtc.PlayParameters;
import io.antmedia.whep.WhepEndpoint;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

public class WhepEndpointTest {

	private static final Logger logger = LoggerFactory.getLogger(WhepEndpointTest.class);

	@Test
	public void testStartWhepPlay() {
		WhepEndpoint whepEndpoint = Mockito.spy(new WhepEndpoint());

		AntMediaApplicationAdapter app = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(app).when(whepEndpoint).getApplication();

		CompletableFuture<Result> resultFuture = new CompletableFuture<>();

		Mockito.when(app.startWhepHttpSignaling(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(resultFuture);

		UriInfo uriInfo = Mockito.mock(UriInfo.class);
		Mockito.doReturn(URI.create("http://localhost:5080")).when(uriInfo).getRequestUri();

		whepEndpoint.startWhepPlay(uriInfo, "stream456", "subscriber456", "viewerInfo", "Bearer token456", "sdpData");
		resultFuture.complete(new Result(true));

		ArgumentCaptor<PlayParameters> playParamsCaptor = ArgumentCaptor.forClass(PlayParameters.class);
		Mockito.verify(app).startWhepHttpSignaling(playParamsCaptor.capture(), Mockito.any(), Mockito.anyString());
		assertEquals("stream456", playParamsCaptor.getValue().streamId);
		assertEquals("subscriber456", playParamsCaptor.getValue().subscriberId);
		assertEquals("viewerInfo", playParamsCaptor.getValue().viewerInfo);
		assertEquals("Bearer token456", playParamsCaptor.getValue().tokenId);
        assertNotNull(playParamsCaptor.getValue().linkedSessionForSignaling);
        assertEquals("default", playParamsCaptor.getValue().role);
	}

	@Test
	public void testPrepareResponse() {
		WhepEndpoint whepEndpoint = Mockito.spy(new WhepEndpoint());

		Response response = whepEndpoint.prepareResponse(new Result(false, "forbidden")	, null);
		assertEquals(403, response.getStatus());
		assertEquals("forbidden", response.getEntity());

		response = whepEndpoint.prepareResponse(new Result(true, "success"), null);
		assertEquals(500, response.getStatus());

		UriInfo uriInfo = Mockito.mock(UriInfo.class);
		Mockito.doReturn(URI.create("http://localhost:5080")).when(uriInfo).getRequestUri();

		response = whepEndpoint.prepareResponse(new Result(true, "success"), uriInfo);
		assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
		assertEquals("application/sdp", response.getMediaType().toString());
	}

	@Test
	public void testStopWhepPlay() {
		WhepEndpoint whepEndpoint = Mockito.spy(new WhepEndpoint());

		AntMediaApplicationAdapter app = Mockito.mock(AntMediaApplicationAdapter.class);
		Mockito.doReturn(app).when(whepEndpoint).getApplication();

		Mockito.when(app.stopWhepPlay(Mockito.anyString(), Mockito.any())).thenReturn(new Result(false, "not found"));

		Response response = whepEndpoint.stopWhepPlay("stream456", "etag123");
		assertEquals(Status.NOT_FOUND.getStatusCode(), response.getStatus());
		Result result = (Result) response.getEntity();
		assertFalse(result.isSuccess());
		assertEquals("not found", result.getMessage());

		Mockito.when(app.stopWhepPlay(Mockito.anyString(), Mockito.any())).thenReturn(new Result(true));

		response = whepEndpoint.stopWhepPlay("stream456", "etag123");
		assertEquals(Status.OK.getStatusCode(), response.getStatus());
		result = (Result) response.getEntity();
		assertTrue(result.isSuccess());
	}
}
