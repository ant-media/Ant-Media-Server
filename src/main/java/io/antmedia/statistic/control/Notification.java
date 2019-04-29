package io.antmedia.statistic.control;

import java.util.Properties;

import javax.annotation.Nonnull;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.red5.spring.Red5ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.IResourceMonitor;
import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.settings.EmailSettings;
import io.vertx.core.Vertx;


public class Notification implements ApplicationContextAware{

	protected static Logger logger = LoggerFactory.getLogger(Notification.class);
	
    /**
     * Spring Application context
     */
    private ApplicationContext applicationContext;
	
    @Context
	private ServletContext servletContext;

	private EmailSettings emailSettings;

	private static final String EMAIL_USERNAME = "emailUsername";

	private static final String EMAIL_PASS = "emailPassword";

	private static final String EMAIL_SMTP_HOST = "emailSmtpHost";

	private static final String EMAIL_SMTP_PORT = "emailSmtpPort";

	private static final String EMAIL_SMTP_ENCRYPTION = "emailSmtpEncryption";

	private static final String EMAIL_SMTP_SSL = "SSL";

	private static final String EMAIL_SMTP_TLS = "TLS";

	private Session session;

	public Properties prop;


	public void sendEmail(String subjectMessage,String textMessage){

		emailSettings = getEmailSettings(); 
		
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
			logger.error(ae.toString()); 
		}
		catch (MessagingException me) {
			logger.error(me.toString());
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
	
	private EmailSettings getEmailSettings() {	
		
		emailSettings = (EmailSettings)applicationContext.getBean(EmailSettings.BEAN_NAME);
		
		return emailSettings;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
