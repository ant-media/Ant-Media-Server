package org.red5.server.net.rtmps;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.message.Constants;

/**
 * Test cases for RTMPSIoFilter to verify suspendRead() is called on handshake failures.
 */
@RunWith(MockitoJUnitRunner.class)
public class RTMPSIoFilterTest {

    @Mock
    private IoSession mockSession;

    @Mock
    private NextFilter mockNextFilter;

    @Mock
    private RTMPMinaConnection mockConnection;

    @Mock
    private RTMP mockRtmpState;

    @Mock
    private InboundHandshake mockHandshake;

    @Mock
    private IoFilterChain mockFilterChain;

    @Mock
    private RTMPConnManager mockConnManager;

    private RTMPSIoFilter rtmpsIoFilter;
    private String sessionId = "TEST_SESSION_ID";

    @Before
    public void setUp() {
        rtmpsIoFilter = new RTMPSIoFilter();
        when(mockSession.getAttribute(RTMPConnection.RTMP_SESSION_ID)).thenReturn(sessionId);
        when(mockSession.isSecured()).thenReturn(true);
        when(mockSession.getFilterChain()).thenReturn(mockFilterChain);
        when(mockConnection.getState()).thenReturn(mockRtmpState);
    }

    /**
     * Test that suspendRead() is called when handshake fails in both STATE_CONNECT and STATE_HANDSHAKE.
     */
    @Test
    public void testSuspendReadCalledOnHandshakeFailures() throws Exception {
        try (MockedStatic<RTMPConnManager> mockedConnManager = org.mockito.Mockito.mockStatic(RTMPConnManager.class)) {
            mockedConnManager.when(RTMPConnManager::getInstance).thenReturn(mockConnManager);
            when(mockConnManager.getConnectionBySessionId(sessionId)).thenReturn(mockConnection);
            when(mockSession.getAttribute(RTMPConnection.RTMP_HANDSHAKE)).thenReturn(mockHandshake);

            when(mockConnection.getStateCode()).thenReturn(RTMP.STATE_CONNECT);
            
            byte[] c0c1Bytes = new byte[Constants.HANDSHAKE_SIZE + 1];
            c0c1Bytes[0] = 0x03; // C0: RTMP version
            for (int i = 1; i < c0c1Bytes.length; i++) {
                c0c1Bytes[i] = (byte) (i % 256);
            }
            IoBuffer c1Message = IoBuffer.wrap(c0c1Bytes);
            
            when(mockHandshake.getBufferSize()).thenReturn(Constants.HANDSHAKE_SIZE + 1);
            IoBuffer c1HandshakeBuffer = IoBuffer.allocate(Constants.HANDSHAKE_SIZE + 1);
            c1HandshakeBuffer.put(c0c1Bytes);
            c1HandshakeBuffer.flip();
            when(mockHandshake.getBufferAsIoBuffer()).thenReturn(c1HandshakeBuffer);
            when(mockHandshake.decodeClientRequest1(any(IoBuffer.class))).thenReturn(null);

            rtmpsIoFilter.messageReceived(mockNextFilter, mockSession, c1Message);

            verify(mockSession, times(1)).suspendRead();
            verify(mockConnection, times(1)).close();
            verify(mockSession, never()).write(any());

            org.mockito.Mockito.reset(mockSession, mockConnection, mockHandshake);
            when(mockSession.getAttribute(RTMPConnection.RTMP_SESSION_ID)).thenReturn(sessionId);
            when(mockSession.isSecured()).thenReturn(true);
            when(mockSession.getAttribute(RTMPConnection.RTMP_HANDSHAKE)).thenReturn(mockHandshake);
            when(mockConnManager.getConnectionBySessionId(sessionId)).thenReturn(mockConnection);

            when(mockConnection.getStateCode()).thenReturn(RTMP.STATE_HANDSHAKE);
            
            byte[] c2Bytes = new byte[Constants.HANDSHAKE_SIZE];
            for (int i = 0; i < c2Bytes.length; i++) {
                c2Bytes[i] = (byte) (i % 256);
            }
            IoBuffer c2Message = IoBuffer.wrap(c2Bytes);
            
            when(mockHandshake.getBufferSize()).thenReturn(Constants.HANDSHAKE_SIZE);
            IoBuffer c2HandshakeBuffer = IoBuffer.allocate(Constants.HANDSHAKE_SIZE);
            c2HandshakeBuffer.put(c2Bytes);
            c2HandshakeBuffer.flip();
            when(mockHandshake.getBufferAsIoBuffer()).thenReturn(c2HandshakeBuffer);
            when(mockHandshake.decodeClientRequest2(any(IoBuffer.class))).thenReturn(false);

            rtmpsIoFilter.messageReceived(mockNextFilter, mockSession, c2Message);

            verify(mockSession, times(1)).suspendRead();
            verify(mockConnection, times(1)).close();
            verify(mockFilterChain, never()).addAfter(anyString(), anyString(), any());
        }
    }
}

