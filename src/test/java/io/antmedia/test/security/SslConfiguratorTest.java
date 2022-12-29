package io.antmedia.test.security;

import static io.antmedia.settings.SslSettings.SSL_CONFIGURATION_TYPE;
import static org.mockito.Mockito.*;

import io.antmedia.datastore.preference.PreferenceStore;
import io.antmedia.rest.model.SslConfigurationType;
import io.antmedia.rest.model.SslConfigurationResult;
import io.antmedia.security.SslConfigurator;
import io.antmedia.settings.SslSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class SslConfiguratorTest {
    protected static Logger logger = LoggerFactory.getLogger(TokenFilterTest.class);


    @Before
    public void before() {
    }

    @After
    public void after() {

    }

    @Rule
    public TestRule watcher = new TestWatcher() {
        protected void starting(Description description) {
            System.out.println("Starting test: " + description.getMethodName());
        }

        protected void failed(Throwable e, Description description) {
            System.out.println("Failed test: " + description.getMethodName());
        };
        protected void finished(Description description) {
            System.out.println("Finishing test: " + description.getMethodName());
        };
    };

    @Test
    public void testSslConfigurator(){

        configure(SslConfigurationType.CUSTOM_DOMAIN);
        configure(SslConfigurationType.ANTMEDIA_SUBDOMAIN);
        configure(SslConfigurationType.CUSTOM_CERTIFICATE);

    }

    public void configure(SslConfigurationType configurationType) {
        SslSettings currentSslSettings = new SslSettings();
        SslSettings sslSettingsToConfigure = new SslSettings();
        sslSettingsToConfigure.setConfigurationType(configurationType.name());
        final String dummyDomain = "ams.antmedia.io";
        final String dummyCloudDomain = "ams-11.antmedia.cloud";
        if(configurationType == SslConfigurationType.CUSTOM_DOMAIN){
            sslSettingsToConfigure.setCustomDomain(dummyDomain);

        }else if(configurationType == SslConfigurationType.CUSTOM_CERTIFICATE){
            sslSettingsToConfigure.setCustomDomain(dummyDomain);
            sslSettingsToConfigure.setFullChainFileContent("fullChainContent");
            sslSettingsToConfigure.setChainFileContent("chainContent");
            sslSettingsToConfigure.setKeyFileContent("keyContent");
            sslSettingsToConfigure.setCertificateFilePath("");
            sslSettingsToConfigure.setFullChainFileName("fullchain.pem");
            sslSettingsToConfigure.setChainFileName("chain.pem");
            sslSettingsToConfigure.setKeyFileName("privkey.pem");


        }else if(configurationType == SslConfigurationType.ANTMEDIA_SUBDOMAIN){
            sslSettingsToConfigure.setAntMediaSubDomain(dummyCloudDomain);
        }
        PreferenceStore store = mock(PreferenceStore.class);

        SslConfigurator sslConfigurator = spy(new SslConfigurator(currentSslSettings,sslSettingsToConfigure,store));
        SslConfigurationResult sslConfigurationResult = new SslConfigurationResult(true,"");

        doReturn(sslConfigurationResult).when(sslConfigurator).runCommandWithOutput(anyString());

        sslConfigurator.configure();

        final int expectedRunCommandWithOutputCount = 1;

        verify(sslConfigurator,times(expectedRunCommandWithOutputCount)).runCommandWithOutput(anyString());

        verify(store,times(1)).put(SSL_CONFIGURATION_TYPE, configurationType.name());


        final int expectedStoragePutCount = 5;
        final int expectedStorageSaveCount = 1;


        verify(store,times(expectedStoragePutCount)).put(anyString(),anyString());
        verify(store,times(expectedStorageSaveCount)).save();

        if(configurationType == SslConfigurationType.CUSTOM_CERTIFICATE){
            final int expectedSslFileCreationCount = 3;
            try{
                verify(sslConfigurator,times(expectedSslFileCreationCount)).createSslFile(anyString(),anyString());
            }catch (IOException e){

            }
        }

    }




}
