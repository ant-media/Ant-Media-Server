package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.antmedia.statistic.ResourceMonitor;

public class ResourceMonitorTest {

	@Test
	public void testCpuAverage() {
		ResourceMonitor monitor = new ResourceMonitor();
		monitor.setWindowSize(3);

		monitor.addCpuMeasurement(5);
		assertEquals(5, monitor.getAvgCpuUsage());

		monitor.addCpuMeasurement(7);
		assertEquals(6, monitor.getAvgCpuUsage());

		monitor.addCpuMeasurement(9);
		assertEquals(7, monitor.getAvgCpuUsage());

		monitor.addCpuMeasurement(11);
		assertEquals(9, monitor.getAvgCpuUsage());
	}

}
