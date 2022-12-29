package io.antmedia.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;

@PropertySource("/conf/red5.properties")
@JsonIgnoreProperties(ignoreUnknown=true)
public class SslSettings {

    public static final String SSL_CONFIGURATION_TYPE = "http.sslConfigurationType";

    public static final String SSL_DOMAIN = "http.sslDomain";

    public static final String SSL_CERTIFICATE_FILE_PATH = "http.ssl_certificate_file"; // FULL CHAIN FILE

    public static final String SSL_CHAIN_FILE_PATH = "http.ssl_certificate_chain_file"; //CHAIN FILE

    public static final String SSL_KEY_FILE_PATH = "http.ssl_certificate_key_file"; // KEY FILE

    public static final String DEFAULT_KEY_FILE_PATH = "conf/privkey.pem";
    public static final String DEFAULT_FULL_CHAIN_FILE_PATH = "conf/fullchain.pem";
    public static final String DEFAULT_CHAIN_FILE_PATH = "conf/chain.pem";


    @Value( "${"+SSL_CONFIGURATION_TYPE+":}")
    private String configurationType;


    @Value( "${"+SSL_DOMAIN+":}")
    private String customDomain;

    @Value( "${"+SSL_DOMAIN+":}")
    private String antMediaSubDomain;

    private String fullChainFileContent;

    private String chainFileContent;

    private String keyFileContent;

    private String fullChainFileName;

    private String chainFileName;

    private String keyFileName;

    @Value( "${"+ SSL_CERTIFICATE_FILE_PATH +":}" )
    private String certificateFilePath;
    @Value( "${"+ SSL_CHAIN_FILE_PATH +":}" )
    private String chainFilePath;
    @Value( "${"+ SSL_KEY_FILE_PATH +":}" )
    private String keyFilePath;

    public String getConfigurationType() {
        return configurationType;
    }

    public String getCustomDomain() {
        return customDomain;
    }

    public String getAntMediaSubDomain() {
        return antMediaSubDomain;
    }


    public SslSettings(){

    }

    public void setConfigurationType(String configurationType) {
        this.configurationType = configurationType;
    }

    public String getCertificateFilePath() {
        return certificateFilePath;
    }

    public String getChainFilePath() {
        return chainFilePath;
    }

    public String getKeyFilePath() {
        return keyFilePath;
    }

    public void setCertificateFilePath(String certificateFilePath) {
        this.certificateFilePath = certificateFilePath;
    }

    public void setChainFilePath(String chainFilePath) {
        this.chainFilePath = chainFilePath;
    }

    public void setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
    }

    public void setCustomDomain(String customDomain) {
        this.customDomain = customDomain;
    }

    public void setAntMediaSubDomain(String antMediaSubDomain) {
        this.antMediaSubDomain = antMediaSubDomain;
    }

    public String getFullChainFileContent() {
        return fullChainFileContent;
    }

    public void setFullChainFileContent(String fullChainFileContent) {
        this.fullChainFileContent = fullChainFileContent;
    }

    public String getChainFileContent() {
        return chainFileContent;
    }

    public void setChainFileContent(String chainFileContent) {
        this.chainFileContent = chainFileContent;
    }

    public String getKeyFileContent() {
        return keyFileContent;
    }

    public void setKeyFileContent(String keyFileContent) {
        this.keyFileContent = keyFileContent;
    }
    public String getFullChainFileName() {
        return fullChainFileName;
    }

    public String getChainFileName() {
        return chainFileName;
    }

    public String getKeyFileName() {
        return keyFileName;
    }
    public void setFullChainFileName(String fullChainFileName) {
        this.fullChainFileName = fullChainFileName;
    }

    public void setChainFileName(String chainFileName) {
        this.chainFileName = chainFileName;
    }

    public void setKeyFileName(String keyFileName) {
        this.keyFileName = keyFileName;
    }

}
