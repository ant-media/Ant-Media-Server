package io.antmedia.test.rest;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.ClusterNotificationService;
import io.antmedia.rest.model.Result;

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
}
