package io.antmedia.settings;

public class EmailSettings {
	
	public static final String BEAN_NAME = "ant.media.email.settings";

	private String emailUsername;

	public String getEmailUsername() {
		return emailUsername;
	}

	public void setEmailUsername(String emailUsername) {
		this.emailUsername = emailUsername;
	}

	private String emailPassword;

	public String getEmailPassword() {
		return emailPassword;
	}

	public void setEmailPassword(String emailPassword) {
		this.emailPassword = emailPassword;
	}

	private String emailSmtpHost;

	public String getEmailSmtpHost() {
		return emailSmtpHost;
	}

	public void setEmailSmtpHost(String emailSmtpHost) {
		this.emailSmtpHost = emailSmtpHost;
	}

	private String emailSmtpEncryption;

	public String getEmailSmtpEncryption() {
		return emailSmtpEncryption;
	}

	public void setEmailSmtpEncryption(String emailSmtpEncryption) {
		this.emailSmtpEncryption = emailSmtpEncryption;
	}
	
	private String emailSmtpPort;
	
	public String getEmailSmtpPort() {
		return emailSmtpPort;
	}

	public void setEmailSmtpPort(String emailSmtpPort) {
		this.emailSmtpPort = emailSmtpPort;
	}

}
