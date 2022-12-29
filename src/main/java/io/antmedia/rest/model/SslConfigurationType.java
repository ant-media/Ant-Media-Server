package io.antmedia.rest.model;

public enum SslConfigurationType {
    NO_SSL("No SSL configuration."),
    CUSTOM_DOMAIN("Auto SSL configuration using custom domain."),
    ANTMEDIA_SUBDOMAIN("Auto SSL configuration using antmedia cloud subdomain."),

    CUSTOM_CERTIFICATE("SSL configuration using imported custom certificate.");


    private String configurationTypeDesc;

    SslConfigurationType(String configurationTypeDesc) {
        this.configurationTypeDesc = configurationTypeDesc;
    }

    @Override
    public String toString() {
        return configurationTypeDesc;
    }

}
