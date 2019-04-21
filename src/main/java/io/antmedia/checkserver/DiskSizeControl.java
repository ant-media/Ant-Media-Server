package io.antmedia.checkserver;

import java.time.LocalDate;

import io.antmedia.SystemUtils;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.settings.EmailSettings;

public class DiskSizeControl {

	private static final String RED5_PROPERTIES = "red5.properties";

	private static final String RED5_PROPERTIES_PATH = "conf/red5.properties";

	private static final String EMAIL_CHECK_DATE = "emailCheckDate";

	public void startService() {
		
		SendEmail sendEmail = new SendEmail();

		String todayString = LocalDate.now().toString();

		PreferenceStore store = new PreferenceStore(RED5_PROPERTIES);
		store.setFullPath(RED5_PROPERTIES_PATH);

		EmailSettings emailSettings = new EmailSettings();

		if (store.get(EMAIL_CHECK_DATE) != null) {
			emailSettings.setEmailCheckDate(String.valueOf(store.get(EMAIL_CHECK_DATE)));
		}

		if(!emailSettings.getEmailCheckDate().equals(todayString)) {

			Integer diskSpacePercent = Integer.valueOf(SystemUtils.osHDInUseSpace(null, "MB", false))*100 /  Integer.valueOf(SystemUtils.osHDTotalSpace(null, "MB", false));

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

			emailSettings.setEmailCheckDate(todayString);
			store.put(EMAIL_CHECK_DATE, todayString);
			store.save();
			
		}
	}

}
