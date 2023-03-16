package io.antmedia.rest.model;

public enum SslConfigurationType {
	
	
	/*
	 * PAY ATTENTION: Don't refactor the following names(NO_SSL, CUSTOM_DOMAIN, ANTMEDIA_SUBDOMAIN, CUSTOM_CERTIFICATE) 
	 * because it's save to the local files with their names.
	 * If you refactor the names, it breaks compatibility with old versions
	 */
    NO_SSL("No SSL configuration"),
    CUSTOM_DOMAIN("Auto SSL configuration using your domain"),
    ANTMEDIA_SUBDOMAIN("Auto SSL configuration using subdomain of antmedia.cloud"),
    CUSTOM_CERTIFICATE("SSL configuration using imported certificate");


    private final String configurationTypeDesc;

    SslConfigurationType(String configurationTypeDesc) {
        this.configurationTypeDesc = configurationTypeDesc;
    }

    @Override
    public String toString() {
        return configurationTypeDesc;
    }

}
