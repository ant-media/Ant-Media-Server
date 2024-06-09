package io.antmedia.test.analytic.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import io.antmedia.analytic.model.PlayerStatsEvent;

public class PlayerStatsEventTest {

    private PlayerStatsEvent playerStatsEvent;

    @Before
    public void setUp() {
        playerStatsEvent = new PlayerStatsEvent();
    }

    @Test
    public void testSubscriberId() {
        playerStatsEvent.setSubscriberId("sub123");
        assertEquals("sub123", playerStatsEvent.getSubscriberId());
    }

    @Test
    public void testTotalBytesTransferred() {
        playerStatsEvent.setTotalBytesTransferred(1000L);
        assertEquals(1000L, playerStatsEvent.getTotalBytesTransferred());
    }

    @Test
    public void testProtocol() {
        playerStatsEvent.setProtocol("HTTP");
        assertEquals("HTTP", playerStatsEvent.getProtocol());
    }

    @Test
    public void testByteTransferred() {
        playerStatsEvent.setByteTransferred(500L);
        assertEquals(500L, playerStatsEvent.getByteTransferred());
    }

    @Test
    public void testUri() {
        playerStatsEvent.setUri("http://example.com");
        assertEquals("http://example.com", playerStatsEvent.getUri());
    }

    @Test
    public void testClientIP() {
        playerStatsEvent.setClientIP("192.168.1.1");
        assertEquals("192.168.1.1", playerStatsEvent.getClientIP());
    }

    @Test
    public void testEventInitialization() {
        assertEquals("playerStats", playerStatsEvent.getEvent());
    }
}

