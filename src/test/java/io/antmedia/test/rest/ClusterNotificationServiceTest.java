package io.antmedia.test.rest;


import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.ClusterNotificationService;
import io.antmedia.rest.model.Result;
import io.antmedia.streamsource.StreamFetcherManager;
import org.junit.jupiter.api.Test;

@Tag("fast")
public class ClusterNotificationServiceTest {

    @Test
    public void testPublishStarted() {
    	ClusterNotificationService clusterNotificationService = new ClusterNotificationService();
    	AntMediaApplicationAdapter webRTCApplication = mock(AntMediaApplicationAdapter.class);
    	clusterNotificationService.setApplication(webRTCApplication);
    	String streamId = "stream1";
    	String role = "publisher";
    	String mainTrackId = "mainTrackId";
        Result result = clusterNotificationService.publishStarted(streamId, role, mainTrackId);
        verify(webRTCApplication, times(1)).publishStarted(streamId, role, mainTrackId);
        
        assertFalse(result.isSuccess());
    }
    
    @Test
    public void testPublishStopped() {
		ClusterNotificationService clusterNotificationService = new ClusterNotificationService();
		AntMediaApplicationAdapter webRTCApplication = mock(AntMediaApplicationAdapter.class);
		clusterNotificationService.setApplication(webRTCApplication);
		String streamId = "stream1";
		String role = "publisher";
		String mainTrackId = "mainTrackId";
		Result result = clusterNotificationService.publishStopped(streamId, role, mainTrackId);
		verify(webRTCApplication, times(1)).publishStopped(streamId, role, mainTrackId);

		assertFalse(result.isSuccess());
	}

	@Test
	public void testClaimAndStartStreamSource() {
		ClusterNotificationService clusterNotificationService = new ClusterNotificationService();
		AntMediaApplicationAdapter application = mock(AntMediaApplicationAdapter.class);
		StreamFetcherManager streamFetcherManager = mock(StreamFetcherManager.class);
		when(application.getStreamFetcherManager()).thenReturn(streamFetcherManager);
		clusterNotificationService.setApplication(application);

		//the answer of this node's claim goes back as it is, the caller reads it
		Result started = new Result(true, "stream1", "Stream fetcher is started");
		when(streamFetcherManager.claimAndStart("stream1", "10.0.0.2", true)).thenReturn(started);
		assertSame(started, clusterNotificationService.claimAndStartStreamSource("stream1", "10.0.0.2", true));
	}
}
