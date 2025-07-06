package io.antmedia.test.rtmp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bytedeco.ffmpeg.avutil.AVRational;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.scope.BroadcastScope;

import io.antmedia.muxer.MuxAdaptor;
import io.antmedia.plugin.PacketFeeder;
import io.antmedia.plugin.api.IPacketListener;
import io.antmedia.rtmp.InProcessRtmpPublisher;
import io.antmedia.rtmp.InternalPublishBootstrapper;
import org.red5.server.messaging.IProvider;

public class InternalPublishBootstrapperUnitTest {

    @Mock
    private IScope mockAppScope;
    
    @Mock
    private IBroadcastScope mockBroadcastScope;
    
    @Mock
    private BroadcastScope mockConcreteBroadcastScope;
    
    @Mock
    private PacketFeeder mockPacketFeeder;
    
    @Mock
    private MuxAdaptor mockMuxAdaptor;
    
    private AVRational videoTimebase;
    private AVRational audioTimebase;
    
    private static final String STREAM_ID = "test_stream_id";

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create real AVRational objects instead of mocking
        videoTimebase = new AVRational().num(1).den(90000);
        audioTimebase = new AVRational().num(1).den(48000);
        
        when(mockMuxAdaptor.getVideoTimeBase()).thenReturn(videoTimebase);
        when(mockMuxAdaptor.getAudioTimeBase()).thenReturn(audioTimebase);
    }
    
    @Test
    public void testAttachWithExistingBroadcastScope() {
        // Setup - use concrete BroadcastScope mock to avoid ClassCastException
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        when(mockConcreteBroadcastScope.getName()).thenReturn(STREAM_ID);
        
        // Execute
        InProcessRtmpPublisher result = InternalPublishBootstrapper.attach(
            mockAppScope, STREAM_ID, mockPacketFeeder, mockMuxAdaptor);
        
        // Verify
        assertNotNull("Should return a valid InProcessRtmpPublisher", result);
        verify(mockAppScope, times(1)).getBroadcastScope(STREAM_ID);
        verify(mockAppScope, never()).addChildScope(any(IBroadcastScope.class));
        verify(mockConcreteBroadcastScope, times(1)).subscribe(any(InProcessRtmpPublisher.class), eq(null));
        verify(mockPacketFeeder, times(1)).addListener(any(InProcessRtmpPublisher.class));
    }
    
    @Test
    public void testAttachWithNewBroadcastScope() {
        // Setup - no existing broadcast scope
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(null);
        
        // Execute
        InProcessRtmpPublisher result = InternalPublishBootstrapper.attach(
            mockAppScope, STREAM_ID, mockPacketFeeder, mockMuxAdaptor);
        
        // Verify
        assertNotNull("Should return a valid InProcessRtmpPublisher", result);
        verify(mockAppScope, times(1)).getBroadcastScope(STREAM_ID);
        verify(mockAppScope, times(1)).addChildScope(any(IBroadcastScope.class));
        verify(mockPacketFeeder, times(1)).addListener(any(InProcessRtmpPublisher.class));
    }
    
    @Test
    public void testAttachWithNullAppScope() {
        // Execute - expect NullPointerException because implementation doesn't handle null appScope
        try {
            InProcessRtmpPublisher result = InternalPublishBootstrapper.attach(
                null, STREAM_ID, mockPacketFeeder, mockMuxAdaptor);
            // If no exception is thrown, it means the implementation handles null gracefully
        } catch (NullPointerException e) {
            // Expected behavior - the implementation doesn't handle null appScope gracefully
            assertTrue("Should throw NullPointerException for null AppScope", true);
            return; // Exit early since we expected this exception
        }
        
        // If we reach here, no exception was thrown (unexpected)
        fail("Expected NullPointerException for null AppScope, but none was thrown");
    }
    
    @Test
    public void testAttachWithNullStreamId() {
        // Setup
        when(mockAppScope.getBroadcastScope(null)).thenReturn(null);
        
        // Execute
        InProcessRtmpPublisher result = InternalPublishBootstrapper.attach(
            mockAppScope, null, mockPacketFeeder, mockMuxAdaptor);
        
        // Verify
        assertNotNull("Should return a valid InProcessRtmpPublisher even with null streamId", result);
        verify(mockAppScope, times(1)).getBroadcastScope(null);
        verify(mockAppScope, times(1)).addChildScope(any(IBroadcastScope.class));
        verify(mockPacketFeeder, times(1)).addListener(any(InProcessRtmpPublisher.class));
    }
    
    @Test
    public void testAttachWithNullPacketFeeder() {
        // Setup
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        
        // Execute - expect NullPointerException because implementation doesn't handle null feeder
        try {
            InProcessRtmpPublisher result = InternalPublishBootstrapper.attach(
                mockAppScope, STREAM_ID, null, mockMuxAdaptor);

            fail("Expected NullPointerException for null PacketFeeder, but none was thrown");
        } catch (NullPointerException e) {
            // Expected behavior - the implementation doesn't handle null PacketFeeder gracefully
            assertTrue("Should throw NullPointerException for null PacketFeeder", true);
        }
    }
    
    @Test
    public void testAttachWithNullMuxAdaptor() {
        // Setup
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        
        // Execute - expect NullPointerException 
        try {
            InProcessRtmpPublisher result = InternalPublishBootstrapper.attach(
                mockAppScope, STREAM_ID, mockPacketFeeder, null);
            fail("Expected NullPointerException for null MuxAdaptor, but none was thrown");
        } catch (NullPointerException e) {
            // Expected behavior - method requires non-null MuxAdaptor
            assertTrue("Should throw NullPointerException for null MuxAdaptor", true);
        }
    }
    
    @Test
    public void testDetachWithValidPublisher() {
        // Setup
        InProcessRtmpPublisher mockPublisher = mock(InProcessRtmpPublisher.class);
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        
        // Execute
        InternalPublishBootstrapper.detach(mockAppScope, STREAM_ID, mockPacketFeeder, mockPublisher);
        
        // Verify
        verify(mockAppScope, times(1)).getBroadcastScope(STREAM_ID);
        verify(mockConcreteBroadcastScope, times(1)).unsubscribe(mockPublisher);
        verify(mockPacketFeeder, times(1)).removeListener(mockPublisher);
    }
    
    @Test
    public void testDetachWithNullPublisher() {
        // Execute
        InternalPublishBootstrapper.detach(mockAppScope, STREAM_ID, mockPacketFeeder, null);
        
        // Verify - should return early without doing anything
        verify(mockAppScope, never()).getBroadcastScope(anyString());
        verify(mockPacketFeeder, never()).removeListener(any(IPacketListener.class));
    }
    
    @Test
    public void testDetachWithNullBroadcastScope() {
        // Setup
        InProcessRtmpPublisher mockPublisher = mock(InProcessRtmpPublisher.class);
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(null);
        
        // Execute
        InternalPublishBootstrapper.detach(mockAppScope, STREAM_ID, mockPacketFeeder, mockPublisher);
        
        // Verify
        verify(mockAppScope, times(1)).getBroadcastScope(STREAM_ID);
        verify(mockPacketFeeder, times(1)).removeListener(mockPublisher);
        // Should not call unsubscribe on null scope
    }
    
    @Test
    public void testDetachWithNullPacketFeeder() {
        // Setup
        InProcessRtmpPublisher mockPublisher = mock(InProcessRtmpPublisher.class);
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        
        // Execute - expect NullPointerException because implementation doesn't handle null feeder
        try {
            InternalPublishBootstrapper.detach(mockAppScope, STREAM_ID, null, mockPublisher);
            // If no exception is thrown, it means the implementation handles null gracefully
            fail("Expected NullPointerException for null PacketFeeder, but none was thrown");
        } catch (NullPointerException e) {
            // Expected behavior - the implementation doesn't handle null feeder gracefully
            assertTrue("Should throw NullPointerException for null PacketFeeder", true);
        }
    }
    
    @Test
    public void testEndToEndWorkflow() {
        // Setup for attach
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(null);
        
        // Execute attach
        InProcessRtmpPublisher publisher = InternalPublishBootstrapper.attach(
            mockAppScope, STREAM_ID, mockPacketFeeder, mockMuxAdaptor);
        
        // Verify attach worked
        assertNotNull("Publisher should be created", publisher);
        verify(mockAppScope, times(1)).getBroadcastScope(STREAM_ID);
        verify(mockAppScope, times(1)).addChildScope(any(IBroadcastScope.class));
        verify(mockPacketFeeder, times(1)).addListener(any(InProcessRtmpPublisher.class));
        
        // Setup for detach
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        
        // Execute detach
        InternalPublishBootstrapper.detach(mockAppScope, STREAM_ID, mockPacketFeeder, publisher);
        
        // Verify detach worked
        verify(mockAppScope, times(2)).getBroadcastScope(STREAM_ID); // Once for attach, once for detach
        verify(mockConcreteBroadcastScope, times(1)).unsubscribe(publisher);
        verify(mockPacketFeeder, times(1)).removeListener(publisher);
    }
    
    @Test
    public void testAttachReturnedPublisherProperties() {
        // Setup
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        
        // Execute
        InProcessRtmpPublisher result = InternalPublishBootstrapper.attach(
            mockAppScope, STREAM_ID, mockPacketFeeder, mockMuxAdaptor);
        
        // Verify
        assertNotNull("Should return a valid InProcessRtmpPublisher", result);
        
        assertTrue("Publisher should be instance of InProcessRtmpPublisher", 
                   result instanceof InProcessRtmpPublisher);
    }
    
    @Test
    public void testMultipleAttachDetachCycles() {
        // Setup
        when(mockAppScope.getBroadcastScope(STREAM_ID)).thenReturn(mockConcreteBroadcastScope);
        
        // Execute multiple attach/detach cycles
        for (int i = 0; i < 3; i++) {
            InProcessRtmpPublisher publisher = InternalPublishBootstrapper.attach(
                mockAppScope, STREAM_ID, mockPacketFeeder, mockMuxAdaptor);
            
            assertNotNull("Publisher should be created in cycle " + i, publisher);
            
            InternalPublishBootstrapper.detach(mockAppScope, STREAM_ID, mockPacketFeeder, publisher);
        }
        
        // Verify all calls were made
        verify(mockAppScope, times(6)).getBroadcastScope(STREAM_ID); // 3 attach + 3 detach
        verify(mockConcreteBroadcastScope, times(3)).subscribe(any(InProcessRtmpPublisher.class), eq(null));
        verify(mockConcreteBroadcastScope, times(3)).unsubscribe(any(InProcessRtmpPublisher.class));
        verify(mockPacketFeeder, times(3)).addListener(any(InProcessRtmpPublisher.class));
        verify(mockPacketFeeder, times(3)).removeListener(any(InProcessRtmpPublisher.class));
    }
} 