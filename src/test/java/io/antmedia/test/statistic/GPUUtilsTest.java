package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.antmedia.statistic.GPUUtils;

public class GPUUtilsTest {
	
	//it is assumed that no gpu available on the machine on which these tests run
	
	@Test
	public void getGPUDeviceCount() {
		assertEquals(0, GPUUtils.getInstance().getDeviceCount());
	}
	
	@Test
	public void getUtilizations() {
		assertEquals(-1, GPUUtils.getInstance().getMemoryUtilization(0));
		assertEquals(-1, GPUUtils.getInstance().getGPUUtilization(0));
	}
}
