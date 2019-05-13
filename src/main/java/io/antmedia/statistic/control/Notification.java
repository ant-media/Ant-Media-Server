package io.antmedia.statistic.control;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.settings.EmailSettings;


public class Notification{

	protected static Logger logger = LoggerFactory.getLogger(Notification.class);

	public EmailSettings emailSettings;

	private static final String EMAIL_SMTP_SSL_UPPERCASE = "SSL";
	
	private static final String EMAIL_SMTP_SSL_LOWERCASE = "ssl";

	private static final String EMAIL_SMTP_TLS_UPPERCASE = "TLS";
	
	private static final String EMAIL_SMTP_TLS_LOWERCASE = "tls";
	
	private static final String COMPLETE_TEXT_MESSAGE = "Text Message was sent successfully.";

	private Properties prop;


	public void sendEmail(String subjectMessage,String textMessage){

			if(checkEmailValues()) {

				prop = new Properties();
				prop.put("mail.smtp.host", emailSettings.getEmailSmtpHost());
				prop.put("mail.smtp.port", emailSettings.getEmailSmtpPort());
				prop.put("mail.smtp.auth", "true");

				if(emailSettings.getEmailSmtpEncryption().equals(EMAIL_SMTP_SSL_UPPERCASE) || emailSettings.getEmailSmtpEncryption().equals(EMAIL_SMTP_SSL_LOWERCASE) ) {
					addSSLConfigs();
				}

				else if (emailSettings.getEmailSmtpEncryption().equals(EMAIL_SMTP_TLS_UPPERCASE) || emailSettings.getEmailSmtpEncryption().equals(EMAIL_SMTP_TLS_LOWERCASE)) {
					addTLSConfigs();
				}


				callSendEmail(subjectMessage,textMessage);


			}
			else {
				logger.warn("Could you provide your Email Address, Password, Smtp Host, Smtp Port, Smtp Encryption values in conf/red5.properties");
			}
		
	}

	public void callSendEmail(String subjectMessage, String textMessage) {

		Session session = Session.getInstance(prop,
				new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(emailSettings.getEmailUsername(), emailSettings.getEmailPassword());
			}
		});

		try {


			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(emailSettings.getEmailUsername()));
			message.setRecipients(
					Message.RecipientType.TO,
					InternetAddress.parse(emailSettings.getEmailUsername())
					);
			message.setSubject(subjectMessage);
			message.setText(textMessage);
			
			mailSended(message);

			logger.info(COMPLETE_TEXT_MESSAGE);

		}
		catch (AddressException ae) {
			logger.error(ae.toString()); 
		}
		catch (MessagingException me) {
			logger.error(me.toString());
		}

	}
	
	public void mailSended(Message message) throws MessagingException {
		Transport.send(message);
	}
	
	public void addSSLConfigs(){
		
	prop.put("mail.smtp.socketFactory.port", emailSettings.getEmailSmtpPort());
	prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
	prop.put("mail.smtp.ssl.checkserveridentity", "true");
	
	}
	
	public void addTLSConfigs(){
	prop.put("mail.smtp.starttls.enable", "true");
	}

	public boolean checkEmailValues(){
		
		boolean result = false;

		if(!emailSettings.getEmailUsername().equals("") && !emailSettings.getEmailPassword().equals("") &&
				!emailSettings.getEmailSmtpHost().equals("") && !emailSettings.getEmailSmtpPort().equals("") &&
				!emailSettings.getEmailSmtpEncryption().equals("")){
			result = true;
		}
		
		return result;
	}
	
	public void setEmailSettings(EmailSettings emailSettings) {
		this.emailSettings = emailSettings;
	}

}
