package io.antmedia.test.statistic.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.antmedia.statistic.type.StreamMetricsHistory;

@Tag("fast")
public class StreamMetricsHistoryTest {

	// Distinct values per array so a swapped constructor assignment fails (four int[] and two double[] fields).
	@Test
	public void testGettersReturnConstructorArrays() {
		long[] bitrate = {1000L, 2000L};
		int[] viewers = {1, 2};
		double[] speed = {1.0, 1.5};
		int[] encoderQueueSize = {3, 4};
		int[] droppedPackets = {5, 6};
		int[] droppedFrames = {7, 8};
		double[] packetLostRatio = {0.01, 0.02};

		StreamMetricsHistory history = new StreamMetricsHistory(bitrate, viewers, speed,
				encoderQueueSize, droppedPackets, droppedFrames, packetLostRatio);

		assertArrayEquals(bitrate, history.getBitrate());
		assertArrayEquals(viewers, history.getViewers());
		assertArrayEquals(speed, history.getSpeed(), 0.0);
		assertArrayEquals(encoderQueueSize, history.getEncoderQueueSize());
		assertArrayEquals(droppedPackets, history.getDroppedPackets());
		assertArrayEquals(droppedFrames, history.getDroppedFrames());
		assertArrayEquals(packetLostRatio, history.getPacketLostRatio(), 0.0);
	}

	@Test
	public void testEmptyHasZeroLengthArrays() {
		StreamMetricsHistory empty = StreamMetricsHistory.empty();

		assertEquals(0, empty.getBitrate().length);
		assertEquals(0, empty.getViewers().length);
		assertEquals(0, empty.getSpeed().length);
		assertEquals(0, empty.getEncoderQueueSize().length);
		assertEquals(0, empty.getDroppedPackets().length);
		assertEquals(0, empty.getDroppedFrames().length);
		assertEquals(0, empty.getPacketLostRatio().length);
	}
}
