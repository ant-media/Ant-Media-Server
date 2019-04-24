package io.antmedia.checkserver;

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

import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.settings.EmailSettings;

public class EmailSender {

	protected static Logger logger = LoggerFactory.getLogger(EmailSender.class);

	private static final String RED5_PROPERTIES = "red5.properties";

	private static final String RED5_PROPERTIES_PATH = "conf/red5.properties";

	public EmailSettings emailSettings;

	private static final String EMAIL_USERNAME = "emailUsername";

	private static final String EMAIL_PASS = "emailPassword";

	private static final String EMAIL_SMTP_HOST = "emailSmtpHost";

	private static final String EMAIL_SMTP_PORT = "emailSmtpPort";

	private static final String EMAIL_SMTP_ENCRYPTION = "emailSmtpEncryption";

	private static final String EMAIL_SMTP_SSL = "SSL";

	private static final String EMAIL_SMTP_TLS = "TLS";

	public PreferenceStore store;

	private Session session;

	public Properties prop;



	public void sendEmail(String subjectMessage,String textMessage){

		store = new PreferenceStore(RED5_PROPERTIES);
		store.setFullPath(RED5_PROPERTIES_PATH);

		emailSettings = new EmailSettings();

		//Fill Email Values in Store 
		fillEmailValues();

		//check email values not null
		try{
			if(checkEmailValues()) {

				prop = new Properties();
				prop.put("mail.smtp.host", emailSettings.getEmailSmtpHost());
				prop.put("mail.smtp.port", emailSettings.getEmailSmtpPort());
				prop.put("mail.smtp.auth", "true");

				if(emailSettings.getEmailSmtpEncryption().equals(EMAIL_SMTP_SSL)) {
					prop.put("mail.smtp.socketFactory.port", emailSettings.getEmailSmtpPort());
					prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
					prop.put("mail.smtp.ssl.checkserveridentity", "true");

				}

				else if (emailSettings.getEmailSmtpEncryption().equals(EMAIL_SMTP_TLS)) {
					prop.put("mail.smtp.starttls.enable", "true");
				}


				callSendEmail(subjectMessage,textMessage);


			}
			else {
				logger.warn("Could you provide your Email Address, Password, Smtp Host, Smtp Port, Smtp Encryption values in conf/red5.properties");
			}
		}
		catch(NullPointerException e) {
			logger.warn("Could you please check your "
					+ "emailUsername=\n" + 
					"emailPassword\n" + 
					"emailSmtpHost\n" + 
					"emailSmtpPort\n" + 
					"emailSmtpEncryption\n" + 
					" values in conf/red5.properties");
		}
	}

	public void callSendEmail(String subjectMessage, String textMessage) {

		session = Session.getInstance(prop,
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
			Transport.send(message);

			logger.info(textMessage);

		}
		catch (AddressException ae) {
			logger.warn(ae.toString());
		}
		catch (MessagingException me) {
			logger.warn(me.toString());
		}

	}

	public boolean checkEmailValues(){

		if(!emailSettings.getEmailUsername().equals("") && !emailSettings.getEmailPassword().equals("") &&
				!emailSettings.getEmailSmtpHost().equals("") && !emailSettings.getEmailSmtpPort().equals("") &&
				!emailSettings.getEmailSmtpEncryption().equals("")){
			return true;

		}
		else {
			return false;
		}

	}

	public void fillEmailValues(){

		if (store.get(EMAIL_USERNAME) != null) {
			emailSettings.setEmailUsername(String.valueOf(store.get(EMAIL_USERNAME)));
		}

		if (store.get(EMAIL_PASS) != null) {
			emailSettings.setEmailPassword(String.valueOf(store.get(EMAIL_PASS)));
		}

		if (store.get(EMAIL_SMTP_HOST) != null) {
			emailSettings.setEmailSmtpHost(String.valueOf(store.get(EMAIL_SMTP_HOST)));
		}

		if (store.get(EMAIL_SMTP_PORT) != null) {
			emailSettings.setEmailSmtpPort(String.valueOf(store.get(EMAIL_SMTP_PORT)));
		}

		if (store.get(EMAIL_SMTP_ENCRYPTION) != null) {
			emailSettings.setEmailSmtpEncryption(String.valueOf(store.get(EMAIL_SMTP_ENCRYPTION)));
		}

	}

}
