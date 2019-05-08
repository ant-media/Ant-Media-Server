package io.antmedia.test.checkserver;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.statistic.control.DiskSizeControl;
import io.antmedia.statistic.control.Notification;


public class DiskSizeControlTest {

	private int diskSizeOver95 = 95;

	private int diskSizeOver85 = 85;

	private int diskSizeOver75 = 75;

	private int diskSizeOver65 = 65;

	protected static Logger logger = LoggerFactory.getLogger(DiskSizeControlTest.class);

	@Test
	public void testDiskSizeCheck()  {

		DiskSizeControl diskSizeControl = Mockito.spy(new DiskSizeControl());	

		Notification emailSender = mock(Notification.class);

		diskSizeControl.setEmailSender(emailSender); 

		long twoHour= System.currentTimeMillis() - (2 * 60 * 60 * 1000);

		long oneDaytwoHour= System.currentTimeMillis() - (26 * 60 * 60 * 1000);

		// Check Under 1 day(passed 2 Hour) & diskSizeOver95

		diskSizeControl.setLastEmailSentTime(twoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver95);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(1)).serviceStarted();

		verify(diskSizeControl,times(1)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,never()).sendEmail(anyString(), anyString());

		// Check Over 1 day(passed 1 day and 2 hour) & diskSizeOver95

		diskSizeControl.setLastEmailSentTime(oneDaytwoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver95);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(2)).serviceStarted();

		verify(diskSizeControl,times(2)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,times(1)).sendEmail(anyString(), anyString());

		// Check Under 1 day(passed 2 Hour) & diskSizeOver65

		diskSizeControl.setLastEmailSentTime(twoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver65);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(3)).serviceStarted();

		verify(diskSizeControl,times(2)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,times(1)).sendEmail(anyString(), anyString());

		// Check Over 1 day(passed 1 day and 2 hour) & diskSizeOver65

		diskSizeControl.setLastEmailSentTime(oneDaytwoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver65);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(4)).serviceStarted();

		verify(diskSizeControl,times(2)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,times(1)).sendEmail(anyString(), anyString());

		// Check Under 1 day(passed 2 Hour) &  diskSizeOver85

		diskSizeControl.setLastEmailSentTime(twoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver85);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(5)).serviceStarted();

		verify(diskSizeControl,times(3)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,times(1)).sendEmail(anyString(), anyString());

		// Check Over 1 day(passed 1 day and 2 hour) & diskSizeOver85

		diskSizeControl.setLastEmailSentTime(oneDaytwoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver85);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(6)).serviceStarted();

		verify(diskSizeControl,times(4)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,times(2)).sendEmail(anyString(), anyString());

		// Check Under 1 day(passed 2 Hour) & diskSizeOver75

		diskSizeControl.setLastEmailSentTime(twoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver75);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(7)).serviceStarted();

		verify(diskSizeControl,times(5)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,times(2)).sendEmail(anyString(), anyString());

		// Check Over 1 day(passed 1 day and 2 hour) & diskSizeOver75

		diskSizeControl.setLastEmailSentTime(oneDaytwoHour);

		when(diskSizeControl.getDiskSize()).thenReturn(diskSizeOver75);

		diskSizeControl.serviceStarted();

		verify(diskSizeControl,times(8)).serviceStarted();

		verify(diskSizeControl,times(6)).diskUsageExceeded(anyString(), anyString());

		verify(emailSender,times(3)).sendEmail(anyString(), anyString());


	}
}
