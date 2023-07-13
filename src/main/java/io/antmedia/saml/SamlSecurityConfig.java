package io.antmedia.saml;

import io.antmedia.AppSettings;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.util.resource.HttpResource;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.FileSystemResource;
import java.io.File;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.saml.*;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.parser.ParserPoolHolder;
import org.springframework.security.saml.processor.*;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.*;
import org.springframework.security.core.Authentication;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.saml.storage.EmptyStorageFactory;

import java.util.*;

@Configuration
@PropertySource("/WEB-INF/red5-web.properties")
public class SamlSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SamlSecurityConfig.class);

    private AppSettings appSettings;

    private String samlKeystoreLocation;
    //= "/home/karinca/softwares/ant-media-server/saml/keystore.jks";

    private String samlKeystorePassword;
    //= "oktaiscool";

    private String samlKeystoreAlias;
    //= "oktasaml";

    private String defaultIdp;
    //= "http://www.okta.com/exk1mfkni2bpGXA6k5d7";

    private String metadataUrl;
    //= "https://dev-75335055.okta.com/app/exk1mfkni2bpGXA6k5d7/sso/saml/metadata";


    @Autowired
    public AppSettings getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    public boolean checkEnable(){
        return appSettings.isSamlEnabled();
    }

    @Bean(initMethod = "initialize")
    public StaticBasicParserPool parserPool() {
        return new StaticBasicParserPool();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        return new CustomSAMLAuthenticationProvider();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLContextProviderImpl contextProvider() {
        return new SAMLContextProviderImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public static SAMLBootstrap samlBootstrap() {
        return new SAMLBootstrap();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLDefaultLogger samlLogger() {
        return new SAMLDefaultLogger();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public WebSSOProfileConsumer webSSOprofileConsumer() {
        return new WebSSOProfileConsumerImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("hokWebSSOprofileConsumer")
    public WebSSOProfileConsumerHoKImpl hokWebSSOProfileConsumer() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public WebSSOProfile webSSOprofile() {
        return new WebSSOProfileImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public WebSSOProfileConsumerHoKImpl hokWebSSOProfile() {
        return new WebSSOProfileConsumerHoKImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public WebSSOProfileECPImpl ecpProfile() {
        return new WebSSOProfileECPImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SingleLogoutProfile logoutProfile() {
        return new SingleLogoutProfileImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public KeyManager keyManager() {
        samlKeystoreLocation = appSettings.getSamlKeystoreLocation();
        samlKeystorePassword = appSettings.getSamlKeystorePassword();
        samlKeystoreAlias = appSettings.getSamlKeystoreAlias();
        DefaultResourceLoader loader = new DefaultResourceLoader();
        //Resource storeFile = loader.getResource(samlKeystoreLocation);
        FileSystemResource storeFile = new FileSystemResource(new File(samlKeystoreLocation));
        Map<String, String> passwords = new HashMap<>();
        passwords.put(samlKeystoreAlias, samlKeystorePassword);
        return new JKSKeyManager(storeFile, samlKeystorePassword, passwords, samlKeystoreAlias);
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public WebSSOProfileOptions defaultWebSSOProfileOptions() {
        WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
        webSSOProfileOptions.setIncludeScoping(false);
        return webSSOProfileOptions;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLEntryPoint samlEntryPoint() {
        SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
        samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
        return samlEntryPoint;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public ExtendedMetadata extendedMetadata() {
        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(false);
        extendedMetadata.setSignMetadata(false);
        return extendedMetadata;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("okta")
    public ExtendedMetadataDelegate oktaExtendedMetadataProvider() throws MetadataProviderException {
        metadataUrl = appSettings.getSamlMetadata();

        // Use the Spring Security SAML resource mechanism to load
        // metadata from the Java classpath.  This works from Spring Boot
        // self contained JAR file.
        org.opensaml.util.resource.Resource resource = null;


        /*
		try {
            resource = new ClasspathResource("/saml/metadata/sso.xml");
		} catch (ResourceException e) {
			 e.printStackTrace();
		}
		*/
        resource = new HttpResource(metadataUrl);

        Timer timer = new Timer("saml-metadata");
        ResourceBackedMetadataProvider provider = new ResourceBackedMetadataProvider(timer,resource);
        provider.setParserPool(parserPool());
        return new ExtendedMetadataDelegate(provider, extendedMetadata());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("metadata")
    public CachingMetadataManager metadata() throws MetadataProviderException, ResourceException {
        defaultIdp = appSettings.getSamlDefaultIdp();
        List<MetadataProvider> providers = new ArrayList<>();
        providers.add(oktaExtendedMetadataProvider());
        CachingMetadataManager metadataManager = new CachingMetadataManager(providers);
        metadataManager.setDefaultIDP(defaultIdp);
        return metadataManager;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("saml")
    public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
        SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successRedirectHandler.setDefaultTargetUrl("/home");
        return successRedirectHandler;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("saml")
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setUseForward(true);
        failureHandler.setDefaultFailureUrl("/error");
        return failureHandler;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
        SimpleUrlLogoutSuccessHandler successLogoutHandler = new SimpleUrlLogoutSuccessHandler();
        successLogoutHandler.setDefaultTargetUrl("/");
        return successLogoutHandler;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SecurityContextLogoutHandler logoutHandler() {
        SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        return logoutHandler;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
        return new SAMLLogoutProcessingFilter(successLogoutHandler(), logoutHandler());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLLogoutFilter samlLogoutFilter() {
        return new SAMLLogoutFilter(successLogoutHandler(),
                new LogoutHandler[] { logoutHandler() },
                new LogoutHandler[] { logoutHandler() });
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPPostBinding httpPostBinding() {
        return new HTTPPostBinding(parserPool(), VelocityFactory.getEngine());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
        return new HTTPRedirectDeflateBinding(parserPool());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLProcessorImpl processor() {
        ArrayList<SAMLBinding> bindings = new ArrayList<>();
        bindings.add(httpRedirectDeflateBinding());
        bindings.add(httpPostBinding());
        return new SAMLProcessorImpl(bindings);
    }
}

