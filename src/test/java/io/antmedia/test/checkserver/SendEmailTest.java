package io.antmedia.test.checkserver;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;

import io.antmedia.AppSettings;
import io.antmedia.checkserver.SendEmail;
import io.antmedia.filter.IPFilter;
import io.antmedia.settings.EmailSettings;

public class SendEmailTest {
	
	protected static Logger logger = LoggerFactory.getLogger(SendEmailTest.class);
	
	 @Test
	    public void testSendEmail()  {
	 
		 SendEmail sendEmail = Mockito.spy(new SendEmail());
		 
		 EmailSettings emailConfigs = new EmailSettings();
		 
		 //emailCon
		 
	//	 Mockito.doReturn().when(sendEmail.fillEmailValues());

	//	 Mockito.doReturn(emailConfigs.getEmailCheckDate()).when(sendEmail.fillEmailValues());
	     
		 
	 }
	 


}
