package io.antmedia.checkserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.SystemUtils;

public class DiskSizeControl {

	protected static Logger logger = LoggerFactory.getLogger(DiskSizeControl.class);

	private Integer diskSpacePercent;

	private EmailSender emailSender = new EmailSender();

	private long lastEmailSentTime;

	private static final long ONEDAY = 24*60*60*1000;


	public void startService() {


		diskSpacePercent = getDiskSize();

		if(diskSpacePercent>90) {	
			diskUsageExceeded("Disk Usage Over %90 - Ant Media Server","Disk Usage Over %90 - Ant Media Server \n\n The system will disable enablemMp4 Recording Setting in Applications.");
		}
		else if(diskSpacePercent>80) {
			diskUsageExceeded("Disk Usage Over %80 - Ant Media Server","%80 above your Disk Size");
		}
		else if(diskSpacePercent>70) {
			diskUsageExceeded("Disk Usage Over %70 - Ant Media Server","%70 above your Disk Size");
		}

	}

	public void diskUsageExceeded(String textSubject, String textMessage) {

		long now = System.currentTimeMillis();

		if(now-getLastEmailSentTime() >= ONEDAY) {
			getEmailSender().sendEmail(textSubject,textMessage);
			setLastEmailSentTime(now);
		}
	}

	public Integer getDiskSize() {
		diskSpacePercent = Integer.valueOf(SystemUtils.osHDInUseSpace(null, "MB", false))*100 /  Integer.valueOf(SystemUtils.osHDTotalSpace(null, "MB", false));
		return diskSpacePercent;
	}

	public EmailSender getEmailSender() {
		return emailSender;
	}

	public void setEmailSender(EmailSender emailSender) {
		this.emailSender = emailSender;
	}

	public long getLastEmailSentTime() {
		return lastEmailSentTime;
	}

	public void setLastEmailSentTime(long lastEmailSentTime) {
		this.lastEmailSentTime = lastEmailSentTime;
	}

}
