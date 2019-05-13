package io.antmedia.test.statistic.control;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.settings.EmailSettings;
import io.antmedia.statistic.control.Notification;

public class SendEmailTest {

	protected static Logger logger = LoggerFactory.getLogger(SendEmailTest.class);

	private EmailSettings emailSettings;

	@Test
	public void testSendEmail()  {

		Notification emailSender = Mockito.spy(new Notification());

		//test check email values is empty

		emailSender.setEmailSettings(fillEmptyEmailSettings());

		assertEquals(false, emailSender.checkEmailValues());

		// test check email values is full

		emailSender.setEmailSettings(fillFullEmailSettings());

		assertEquals(true, emailSender.checkEmailValues());

		// test check email values is random full

		emailSender.setEmailSettings(fillRandomEmailSettings());

		assertEquals(false, emailSender.checkEmailValues());

		// get back default fulled values

		emailSender.setEmailSettings(fillFullEmailSettings());

		assertEquals(true, emailSender.checkEmailValues());

		// test Configs

		emailSender.sendEmail("test Subject", "test Content");

		verify(emailSender,never()).addSSLConfigs();

		verify(emailSender,never()).addTLSConfigs();

		verify(emailSender,times(1)).callSendEmail(anyString(), anyString());

		// SSL Configs check

		emailSender.emailSettings.setEmailSmtpEncryption("SSL");

		emailSender.sendEmail("test Subject", "test Content");

		verify(emailSender,times(1)).addSSLConfigs();

		verify(emailSender,times(2)).callSendEmail(anyString(), anyString());

		// TLS Configs check

		emailSender.emailSettings.setEmailSmtpEncryption("TLS");

		emailSender.sendEmail("test Subject", "test Content");

		verify(emailSender,times(1)).addTLSConfigs();

		verify(emailSender,times(3)).callSendEmail(anyString(), anyString());



	}

	private EmailSettings fillEmptyEmailSettings(){

		emailSettings = new EmailSettings();

		emailSettings.setEmailPassword("");
		emailSettings.setEmailSmtpEncryption("");
		emailSettings.setEmailSmtpHost("");
		emailSettings.setEmailSmtpPort("");
		emailSettings.setEmailUsername("");

		return emailSettings;
	}

	private EmailSettings fillFullEmailSettings(){

		emailSettings = new EmailSettings();

		emailSettings.setEmailPassword("test1");
		emailSettings.setEmailSmtpEncryption("test1");
		emailSettings.setEmailSmtpHost("test1");
		emailSettings.setEmailSmtpPort("test1");
		emailSettings.setEmailUsername("test1");

		return emailSettings;
	}

	private EmailSettings fillRandomEmailSettings(){

		emailSettings = new EmailSettings();

		emailSettings.setEmailPassword("");
		emailSettings.setEmailSmtpEncryption("");
		emailSettings.setEmailSmtpHost("test1");
		emailSettings.setEmailSmtpPort("test1");
		emailSettings.setEmailUsername("test1");

		return emailSettings;
	}


}