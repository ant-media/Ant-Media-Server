package io.antmedia.checkserver;

import java.time.LocalDate;

import io.antmedia.SystemUtils;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.settings.EmailSettings;

public class DiskSizeControl {

	private static final String RED5_PROPERTIES = "red5.properties";

	private static final String RED5_PROPERTIES_PATH = "conf/red5.properties";

	private static final String EMAIL_CHECK_DATE = "emailCheckDate";
	
	public String todayString;
	
	private PreferenceStore store = new PreferenceStore(RED5_PROPERTIES);
	
	private EmailSettings emailSettings = new EmailSettings();
	
	public Integer diskSpacePercent;
	
	public SendEmail sendEmail;
	

	public void startService() {
		
		sendEmail = new SendEmail();

		todayString = getTodayString();

		if(!getTodayStringDb().equals(todayString)) {

			diskSpacePercent = getDiskSize();
			
			saveTodayStringDb(getTodayString());
			
			//Check Disk Size

			if(diskSpacePercent>90) {
				sendEmail.sendEmail("Disk Usage Over %90 - Ant Media Server","Disk Usage Over %90 - Ant Media Server \n\n The system will disable enablemMp4 Recording Setting in Applications.");
			}
			else if(diskSpacePercent>80) {
				sendEmail.sendEmail("Disk Usage Over %80 - Ant Media Server","%80 above your Disk Size");	
			}
			else if(diskSpacePercent>70) {
				sendEmail.sendEmail("Disk Usage Over %70 - Ant Media Server","%70 above your Disk Size");	
			}
			
		}
	}
	
	public String getTodayStringDb() {
		if (store.get(EMAIL_CHECK_DATE) != null) {
			emailSettings.setEmailCheckDate(String.valueOf(store.get(EMAIL_CHECK_DATE)));
		}
		return emailSettings.getEmailCheckDate();
	}
	
	public void saveTodayStringDb(String getTodayString) {
		emailSettings.setEmailCheckDate(getTodayString);
		store.setFullPath(RED5_PROPERTIES_PATH);
		store.put(EMAIL_CHECK_DATE, todayString);
		store.save();
	
	}

	public String getTodayString() {
		todayString = LocalDate.now().toString();
		return todayString;
	}

	public Integer getDiskSize() {
		diskSpacePercent = Integer.valueOf(SystemUtils.osHDInUseSpace(null, "MB", false))*100 /  Integer.valueOf(SystemUtils.osHDTotalSpace(null, "MB", false));
		return diskSpacePercent;
	}

}
