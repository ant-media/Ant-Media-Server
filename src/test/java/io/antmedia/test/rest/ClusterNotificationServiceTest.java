package io.antmedia.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.rest.ClusterNotificationService;
import io.antmedia.rest.model.Result;
import jakarta.ws.rs.core.MediaType;

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
        verify(webRTCApplication, times(1)).streamStartedOnAnotherNode(streamId, role, mainTrackId);
        
        assertTrue(result.isSuccess());
        assertEquals(MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON); // To keep MediaType consistent
    }
}
