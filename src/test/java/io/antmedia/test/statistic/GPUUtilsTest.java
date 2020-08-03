package io.antmedia.test.statistic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.antmedia.statistic.GPUUtils;
import io.antmedia.statistic.GPUUtils.MemoryStatus;

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
	
	@Test
	public void testGetDeviceName() {
		assertNull(GPUUtils.getInstance().getDeviceName(0));
	}
	
	@Test
	public void testGetMemoryStatus() {
		assertNull(GPUUtils.getInstance().getMemoryStatus(0));
		
		long total = (long)(Math.random()*10000);
		long used = (long)(Math.random()*10000);
		long free = (long)(Math.random()*10000);
		MemoryStatus status = new MemoryStatus(total,used, free);
		assertEquals(total, status.getMemoryTotal());
		assertEquals(used, status.getMemoryUsed());
		assertEquals(free, status.getMemoryFree());
	}

}
