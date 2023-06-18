package io.antmedia.security;


import java.io.File;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.rest.model.SslConfigurationType;

public class SslConfigurator {

	private static final Logger logger = LoggerFactory.getLogger(SslConfigurator.class);

	private SslConfigurationType type;
	private String domain;
	private File fullChainFile;
	private File privateKeyFile;
	private File chainFile;

	public String getCommand()
	{
		logger.info("SSL configuration with configuration type {} has started.", this.type);

		String installDirectory = Paths.get("").toAbsolutePath().toString();

		switch (type)
		{
			case CUSTOM_DOMAIN:
				return "sudo /bin/bash enable_ssl.sh -d " + domain + " -i " + installDirectory;

			case ANTMEDIA_SUBDOMAIN:
				return "sudo /bin/bash enable_ssl.sh -i " + installDirectory;
				
			case CUSTOM_CERTIFICATE:
				return "sudo /bin/bash enable_ssl.sh -f " + fullChainFile.getAbsolutePath() + " -p " + privateKeyFile.getAbsolutePath() + " -c " + chainFile.getAbsolutePath() + " -d " + domain + " -i " + installDirectory;

			default:
				logger.warn("No SSL configuration type. SSL configuration failed.");
				return null;
		}
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setType(SslConfigurationType type) {
		this.type = type;
	}

	public void setFullChainFile(File fullChainFile) {
		this.fullChainFile = fullChainFile;
	}

	public void setPrivateKeyFile(File privateKeyFile) {
		this.privateKeyFile = privateKeyFile;
	}

	public void setChainFile(File chainFile) {
		this.chainFile = chainFile;
	}

}
