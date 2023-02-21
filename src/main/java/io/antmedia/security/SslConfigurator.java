package io.antmedia.security;

import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.rest.model.SslConfigurationType;
import io.antmedia.rest.model.SslConfigurationResult;
import io.antmedia.settings.SslSettings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.antmedia.settings.SslSettings.*;

public class SslConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(SslConfigurator.class);
    private static final String SSL_CONFIGURATION_FAILED_MESSAGE = "SSL configuration has failed. {}";
    private static final String SSL_CONFIGURATION_SUCCESS_MESSAGE = "SSL configuration has been completed successfully.";

    private final SslSettings currentSslSettings;
    private final SslSettings sslSettingsToConfigure;
    private final PreferenceStore store;

    public SslConfigurator(final SslSettings currentSslSettings, final SslSettings sslSettingsToConfigure, final PreferenceStore store) {

        this.currentSslSettings = currentSslSettings;
        this.sslSettingsToConfigure = sslSettingsToConfigure;
        this.store = store;
    }

    public String getDomainNameFromDomain(final String domain) {
        return domain.split("\\.")[0];
    }

    public void createSslFile(final String filePath, final String fileContent) throws IOException {
        final Path file = Paths.get(filePath);
        Files.writeString(file, fileContent);
    }

    private String extractAmsCloudDomainFromCommandOutput(final String commandOutput) {
        final String domainLineIdentifier = "domain:";
        final String domainMatcherRegex = "^.*" + domainLineIdentifier + ".*$";
        final Pattern pattern = Pattern.compile(domainMatcherRegex, Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(commandOutput);
        String amsCloudUrl = "";
        while (matcher.find()) {
            final String foundDomainLine = matcher.group();
            final String amsCloudDomainIdentifier = "antmedia.cloud";
            if (foundDomainLine.contains(amsCloudDomainIdentifier)) {
                amsCloudUrl = foundDomainLine.replace(domainLineIdentifier, "").trim();
                break;
            }
        }
        return amsCloudUrl;
    }

    private SslConfigurationResult configureSslWithCustomDomain(final String configureSslCommand) {

        SslConfigurationResult sslConfigurationResult = runCommandWithOutput(configureSslCommand);
        if (sslConfigurationResult.isSuccess()) {
            logger.info(SSL_CONFIGURATION_SUCCESS_MESSAGE);
            currentSslSettings.setConfigurationType(sslSettingsToConfigure.getConfigurationType());
            currentSslSettings.setCustomDomain(sslSettingsToConfigure.getCustomDomain());

            store.put(SSL_CONFIGURATION_TYPE, SslConfigurationType.CUSTOM_DOMAIN.name());
            store.put(SSL_DOMAIN, sslSettingsToConfigure.getCustomDomain());
            store.put(SSL_CHAIN_FILE_PATH, DEFAULT_CHAIN_FILE_PATH);
            store.put(SSL_CERTIFICATE_FILE_PATH, DEFAULT_FULL_CHAIN_FILE_PATH);
            store.put(SSL_KEY_FILE_PATH, DEFAULT_KEY_FILE_PATH);
            store.save();
        } else {
            logger.warn(SSL_CONFIGURATION_FAILED_MESSAGE, sslConfigurationResult.getMessage());
        }

        return sslConfigurationResult;

    }

    private SslConfigurationResult configureSslWithAntMediaSubDomain(final String configureSslCommand) {

        SslConfigurationResult sslConfigurationResult = runCommandWithOutput(configureSslCommand);

        if (sslConfigurationResult.isSuccess()) {
            logger.info(SSL_CONFIGURATION_SUCCESS_MESSAGE);
            String amsCloudDomain = extractAmsCloudDomainFromCommandOutput(sslConfigurationResult.getMessage());
            sslConfigurationResult.setAmsCloudDomain(amsCloudDomain);
            currentSslSettings.setConfigurationType(sslSettingsToConfigure.getConfigurationType());
            currentSslSettings.setAntMediaSubDomain(amsCloudDomain);
            store.put(SSL_CONFIGURATION_TYPE, SslConfigurationType.ANTMEDIA_SUBDOMAIN.name());
            store.put(SSL_DOMAIN, amsCloudDomain);
            store.put(SSL_CHAIN_FILE_PATH, DEFAULT_CHAIN_FILE_PATH);
            store.put(SSL_CERTIFICATE_FILE_PATH, DEFAULT_FULL_CHAIN_FILE_PATH);
            store.put(SSL_KEY_FILE_PATH, DEFAULT_KEY_FILE_PATH);
            store.save();
        } else {
            logger.warn(SSL_CONFIGURATION_FAILED_MESSAGE, sslConfigurationResult.getMessage());
        }

        return sslConfigurationResult;

    }

    private SslConfigurationResult configureSslWithCustomCertificate(final String configureSslCommand, final String domain) {

        SslConfigurationResult sslConfigurationResult = runCommandWithOutput(configureSslCommand);
        if (sslConfigurationResult.isSuccess()) {
            logger.info(SSL_CONFIGURATION_SUCCESS_MESSAGE);

            currentSslSettings.setConfigurationType(sslSettingsToConfigure.getConfigurationType());
            currentSslSettings.setCustomDomain(domain);
            store.put(SSL_CONFIGURATION_TYPE, SslConfigurationType.CUSTOM_CERTIFICATE.name());
            store.put(SSL_DOMAIN, domain);
            store.put(SSL_CHAIN_FILE_PATH, DEFAULT_CHAIN_FILE_PATH);
            store.put(SSL_CERTIFICATE_FILE_PATH, DEFAULT_FULL_CHAIN_FILE_PATH);
            store.put(SSL_KEY_FILE_PATH, DEFAULT_KEY_FILE_PATH);
            store.save();

        } else {
            logger.warn(SSL_CONFIGURATION_FAILED_MESSAGE, sslConfigurationResult.getMessage());
        }
        return sslConfigurationResult;

    }

    private boolean createKeyFile(final String sslFilePath, final String keyFileContent){
        final String keyFileExtension = ".key";
        final String keyFilePath = sslFilePath + keyFileExtension;
        try {
            createSslFile(keyFilePath, keyFileContent);
            return true;

        } catch (final IOException ex) {
            logger.warn("Could not create key file on server side.");
            return false;
        }
    }

    private boolean createFullChainFile(final String sslFilePath, final String fullChainFileContent){
        try {
            createSslFile(sslFilePath, fullChainFileContent);
            return true;

        } catch (final IOException ex) {
            logger.warn("Could not create full chain file on server side.");
            return false;
        }
    }

    private boolean createChainFile(final String sslFilePath, final String chainFileContent){
        try {
            createSslFile(sslFilePath, chainFileContent);
            return true;

        } catch (final IOException ex) {
            logger.warn("Could not create chain file on server side.");
            return false;
        }
    }

    private boolean createCustomCertificateFiles(final File tempSslDir, final String domainName, final String crtFileExtension, final String pemFileExtension) {
        final String keyFileContent = sslSettingsToConfigure.getKeyFileContent();
        final String fullChainFileContent = sslSettingsToConfigure.getFullChainFileContent();
        final String chainFileContent = sslSettingsToConfigure.getChainFileContent();
        if (tempSslDir.mkdir()) {
            final String sslFilePath = tempSslDir.getAbsolutePath() + File.separator + domainName;
            boolean keyFileCreated = false;
            boolean fullChainFileCreated = false;
            boolean chainFileCreated = false;

            if (keyFileContent != null) {
                keyFileCreated = createKeyFile(sslFilePath, keyFileContent);
            }


            if (fullChainFileContent != null) {
                String fullChainFilePath = "";
                final String fullChainFileName = sslSettingsToConfigure.getFullChainFileName();
                if (fullChainFileName.contains(crtFileExtension)) {
                    fullChainFilePath = sslFilePath + crtFileExtension;
                } else if (fullChainFileName.contains(pemFileExtension)) {
                    fullChainFilePath = sslFilePath + pemFileExtension;
                }

                fullChainFileCreated = createFullChainFile(fullChainFilePath, fullChainFileContent);

            }

            if (chainFileContent != null) {
                String chainFilePath = "";
                final String chainStr = "chain";
                final String chainFileName = sslSettingsToConfigure.getChainFileName();
                if (chainFileName.contains(crtFileExtension)) {
                    chainFilePath = sslFilePath + chainStr + crtFileExtension;
                } else if (chainFileName.contains(pemFileExtension)) {
                    chainFilePath = sslFilePath + chainStr + pemFileExtension;
                }

                chainFileCreated = createChainFile(chainFilePath, chainFileContent);

            }

            return keyFileCreated && fullChainFileCreated && chainFileCreated;
        }else{
            logger.warn("Could not create temp ssl folder under system tmp directory.");
        }
        return false;
    }

    public SslConfigurationResult configure() {
        logger.info("SSL configuration with configuration type {} has started.", sslSettingsToConfigure.getConfigurationType());
        SslConfigurationResult sslConfigurationResult = new SslConfigurationResult(false, "");
        final SslConfigurationType configurationType = SslConfigurationType.valueOf(sslSettingsToConfigure.getConfigurationType());
        String configureSslCommand = "";
        final Path path = Paths.get("");
        final String installDirectory = path.toAbsolutePath().toString();

        switch (configurationType) {
            case CUSTOM_DOMAIN:
                configureSslCommand = "/bin/bash enable_ssl.sh -d " + sslSettingsToConfigure.getCustomDomain() + " -i " + installDirectory;
                sslConfigurationResult = configureSslWithCustomDomain(configureSslCommand);
                break;
            case ANTMEDIA_SUBDOMAIN:
                configureSslCommand = "/bin/bash enable_ssl.sh -i " + installDirectory;
                sslConfigurationResult = configureSslWithAntMediaSubDomain(configureSslCommand);
                break;
            case CUSTOM_CERTIFICATE:
                final String systemTempDir = System.getProperty("java.io.tmpdir");
                final String sslTempDirName = "sslTemp";
                final File sslTempDir = new File(systemTempDir + File.separator + sslTempDirName);
                final String domain = sslSettingsToConfigure.getCustomDomain();
                final String domainName = getDomainNameFromDomain(domain);
                String fullChainFileExtension = "";
                String chainFileExtension = "";
                final String crtFileExtension = ".crt";
                final String pemFileExtension = ".pem";
                String fullChainFileName = sslSettingsToConfigure.getFullChainFileName();

                if (fullChainFileName.contains(crtFileExtension)) {
                    fullChainFileExtension = crtFileExtension;

                } else if (fullChainFileName.contains(pemFileExtension)) {
                    fullChainFileExtension = pemFileExtension;
                }

                String chainFileName = sslSettingsToConfigure.getChainFileName();
                if (chainFileName.contains(crtFileExtension)) {
                    chainFileExtension = crtFileExtension;

                } else if (chainFileName.contains(pemFileExtension)) {
                    chainFileExtension = pemFileExtension;

                }

                boolean certificateFilesCreated = createCustomCertificateFiles(sslTempDir, domainName, crtFileExtension, pemFileExtension);

                if (certificateFilesCreated) {
                    fullChainFileName = domainName + fullChainFileExtension;
                    chainFileName = domainName + "chain" + chainFileExtension;
                    configureSslCommand = "/bin/bash enable_ssl.sh -f " + fullChainFileName + " -p " + domainName + ".key" + " -c " + chainFileName + " -d " + domain + " -i " + installDirectory;
                    sslConfigurationResult = configureSslWithCustomCertificate(configureSslCommand, domain);

                } else {
                    logger.warn("Creating certificate files on server side failed.");
                    sslConfigurationResult.setSuccess(false);
                    sslConfigurationResult.setMessage("Error while creating certificate files on server side.");
                }
                try {
                    FileUtils.forceDelete(sslTempDir);
                } catch (final IOException e) {
                    logger.warn("Unable to remove temp SSL folder in system tmp directory. {}", e.getMessage());
                }
                break;
            default:
                logger.warn("No SSL configuration type. SSL configuration failed.");
                break;
        }
        return sslConfigurationResult;
    }

    public SslConfigurationResult runCommandWithOutput(final String command) {
        logger.debug("Executing enable_ssl script with command {}", command);
        final SslConfigurationResult commandResult = new SslConfigurationResult(false, "");
        try {
            final Process process = getProcess(command);
            final ByteArrayOutputStream inputStreamOut = new ByteArrayOutputStream();
            final ByteArrayOutputStream errorStreamOut = new ByteArrayOutputStream();

            process.getInputStream().transferTo(inputStreamOut);
            process.getErrorStream().transferTo(errorStreamOut);
            final boolean success = process.waitFor() == 0;
            commandResult.setSuccess(success);
            commandResult.setMessage(inputStreamOut + "\n" + errorStreamOut);
        } catch (final IOException e) {
            commandResult.setSuccess(false);
            commandResult.setMessage(ExceptionUtils.getStackTrace(e));
        } catch (final InterruptedException e) {
            commandResult.setSuccess(false);
            commandResult.setMessage(ExceptionUtils.getStackTrace(e));
            Thread.currentThread().interrupt();
        }
        return commandResult;
    }

    public Process getProcess(final String command) throws IOException {
        final ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        return pb.start();
    }

}
