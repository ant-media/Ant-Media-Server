package io.antmedia.test.checkserver;

import static org.mockito.Mockito.mock;

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

import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.settings.EmailSettings;
import io.antmedia.statistic.control.Notification;

public class SendEmailTest {
	
	private static final String EMAIL_USERNAME = "emailUsername";

	private static final String EMAIL_PASS = "emailPassword";

	private static final String EMAIL_SMTP_HOST = "emailSmtpHost";

	private static final String EMAIL_SMTP_PORT = "emailSmtpPort";

	private static final String EMAIL_SMTP_ENCRYPTION = "emailSmtpEncryption";

	private static final String EMAIL_SMTP_SSL = "SSL";

	private static final String EMAIL_SMTP_TLS = "TLS";
	
	private static final String RED5_PROPERTIES = "red5.properties";
	
	protected static Logger logger = LoggerFactory.getLogger(SendEmailTest.class);
	
	private EmailSettings emailSettings;
	
	@Test
	public void testSendEmail()  {

		Notification emailSender = Mockito.spy(new Notification());
		
		EmailSettings emailSettings = mock(EmailSettings.class);
		
		PreferenceStore store = mock(PreferenceStore.class);
		
		//store.setFullPath(RED5_PROPERTIES);
		
		store.put(EMAIL_USERNAME, "");
		
		store.put(EMAIL_PASS, "");
		
		store.put(EMAIL_SMTP_HOST, "");
		
		store.put(EMAIL_SMTP_PORT, "");
		
		store.put(EMAIL_SMTP_ENCRYPTION, "");
		
	//	when(emailSender.store).thenReturn(store);
		
		// empty Email Settings
		
		//when(emailSender.emailSettings).thenReturn(emailSettings);
		
		//verify(emailSender,times(1)).checkEmailValues();
		
		
		//filled Email Settings
		
		//when(emailSender.emailSettings).thenReturn(emptyEmailSettings());
		
		
		
	}
	
	private EmailSettings emptyEmailSettings(){
		
		emailSettings = new EmailSettings();
		
		return emailSettings;
	}


}
