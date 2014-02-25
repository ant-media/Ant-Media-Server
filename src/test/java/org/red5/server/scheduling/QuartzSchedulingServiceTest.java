package org.red5.server.scheduling;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

@ContextConfiguration(locations = { "context.xml" })
public class QuartzSchedulingServiceTest extends AbstractJUnit4SpringContextTests {

	private QuartzSchedulingService service;

	static {
		System.setProperty("red5.deployment.type", "junit");
		System.setProperty("red5.root", "target/test-classes");
		System.setProperty("red5.config_root", "src/main/server/conf");
		System.setProperty("logback.ContextSelector", "org.red5.logging.LoggingContextSelector");
	}

	@Before
	public void setUp() throws Exception {
		service = (QuartzSchedulingService) applicationContext.getBean("schedulingService");
	}

	@After
	public void tearDown() throws Exception {
		//service.scheduler.shutdown(true);
	}

	@Test
	public void testAddScheduledJob() throws InterruptedException {
		CountingJob job = new CountingJob();
		service.addScheduledJob(100, job);
		Thread.sleep(1000L);
		Assert.assertTrue(job.getCount() >= 10);
	}

	@Test
	public void testAddScheduledOnceJobLongIScheduledJob() {
		service.addScheduledOnceJob(1000, new DelayedJob(System.currentTimeMillis() + 1000L));
	}

	@Test
	public void testAddScheduledJobAfterDelay() throws InterruptedException {
		DelayedJob job = new DelayedJob(System.currentTimeMillis() + 1000L);
		@SuppressWarnings("unused")
		String jobName = service.addScheduledJobAfterDelay(10, job, 100);
		Thread.sleep(1000L);
		// get the job and check the execution count
		System.out.println("Executions: " + job.getExecutions());
		Assert.assertTrue(job.getExecutions() >= 85);
	}

	@Test
	public void testGetScheduledJobNames() {
		Assert.assertFalse(service.getScheduledJobNames().isEmpty());
	}

	private class CountingJob implements IScheduledJob {
		
		int count;
		
		public void execute(ISchedulingService service) {
			count++;
		}

		int getCount() {
			return count;
		}
	}

	private class DelayedJob implements IScheduledJob {

		long targetTimeMs;

		int executions;

		DelayedJob(long targetTimeMs) {
			this.targetTimeMs = targetTimeMs;
		}

		public void execute(ISchedulingService service) {
			long now = System.currentTimeMillis();
			if (targetTimeMs < now) {
				// too early, fail
				Assert.fail(String.format("Run too early by: %s ms", (now - targetTimeMs)));
			}
			executions++;
		}

		int getExecutions() {
			return executions;
		}

	}

}
