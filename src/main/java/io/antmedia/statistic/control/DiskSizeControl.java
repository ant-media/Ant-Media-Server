package io.antmedia.statistic.control;

import org.red5.spring.Red5ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.SystemUtils;
import io.antmedia.settings.EmailSettings;
import io.vertx.core.Vertx;

public class DiskSizeControl implements ApplicationContextAware{

	protected static Logger logger = LoggerFactory.getLogger(DiskSizeControl.class);

	private int diskSpacePercent;

	private Notification emailSender = new Notification();

	private long lastEmailSentTime;

	private EmailSettings emailSettings;

	private static final int ONEDAY = 24*60*60*1000;

	public static final String BEAN_NAME = "diskSizeControl";

	private Vertx vertx;

	private static final int CHECK_DISK_SIZE_PERIOD = 6*60*60*1000;




	public void startService() {

		emailSender.setEmailSettings(emailSettings);

		vertx.setPeriodic(CHECK_DISK_SIZE_PERIOD, l -> 
			serviceStarted()
		);
	}
	
	public void serviceStarted(){
		
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

	public int getDiskSize() {
		diskSpacePercent = Integer.valueOf(SystemUtils.osHDInUseSpace(null, "MB", false))*100 /  Integer.valueOf(SystemUtils.osHDTotalSpace(null, "MB", false));
		return diskSpacePercent;
	}

	public Notification getEmailSender() {
		return emailSender;
	}

	public void setEmailSender(Notification emailSender) {
		this.emailSender = emailSender;
	}

	public long getLastEmailSentTime() {
		return lastEmailSentTime;
	}

	public void setLastEmailSentTime(long lastEmailSentTime) {
		this.lastEmailSentTime = lastEmailSentTime;
	}


	public EmailSettings getEmailSettings() {
		return emailSettings;
	}

	public void setEmailSettings(EmailSettings emailSettings) {
		this.emailSettings = emailSettings;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {

		if (applicationContext.containsBean("red5.common")) 
		{
			Red5ApplicationContext red5Common = (Red5ApplicationContext)applicationContext.getBean("red5.common");

			if (red5Common.containsBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME)) {
				vertx = (Vertx)red5Common.getBean(AntMediaApplicationAdapter.VERTX_BEAN_NAME);
				logger.info("vertx in context");
			}
			else {
				throw new ApplicationContextException("No Vertx bean in application context");
			}
		}
		else {
			logger.info("No server in application context");
		}
	}


}
