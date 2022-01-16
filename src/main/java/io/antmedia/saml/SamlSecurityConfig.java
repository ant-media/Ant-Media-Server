package io.antmedia.saml;

import io.antmedia.AppSettings;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
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

    private final Timer backgroundTaskTimer = new Timer(true);
    private final MultiThreadedHttpConnectionManager multiThreadedHttpConnectionManager
            = new MultiThreadedHttpConnectionManager();

    public AppSettings getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    public boolean checkEnable(){
        if(appSettings.isHlsMuxingEnabled()){
            return true;
        }
        else{
            return false;
        }
    }

    @Bean
    @Qualifier("saml")
    public Timer getBackgroundTaskTimer() {
        return backgroundTaskTimer;
    }

    @Bean
    @Qualifier("saml")
    public MultiThreadedHttpConnectionManager getMultiThreadedHttpConnectionManager() {
        return multiThreadedHttpConnectionManager;
    }

    // Initialization of the velocity engine
    @Bean
    public VelocityEngine velocityEngine() {
        return VelocityFactory.getEngine();
    }

    // XML parser pool needed for OpenSAML parsing
    @Bean(initMethod = "initialize")
    public StaticBasicParserPool parserPool() {
        return new StaticBasicParserPool();
    }

    @Bean(name = "parserPoolHolder")
    public ParserPoolHolder parserPoolHolder() {
        return new ParserPoolHolder();
    }

    // Bindings, encoders and decoders used for creating and parsing messages
    @Bean
    public HttpClient httpClient() {
        return new HttpClient(this.multiThreadedHttpConnectionManager);
    }

    /**
     * Authentication provider is autowired in websecurity config.
     */
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLAuthenticationProvider samlAuthenticationProvider() {
        SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
        samlAuthenticationProvider.setForcePrincipalAsString(false);
        return samlAuthenticationProvider;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLContextProviderImpl contextProvider() {
        SAMLContextProviderImpl samlContextProviderImpl = new SAMLContextProviderImpl();
        samlContextProviderImpl.setStorageFactory(new EmptyStorageFactory());
        return new SAMLContextProviderImpl();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public static SAMLBootstrap sAMLBootstrap() {
        return new SAMLBootstrap();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLDefaultLogger samllogger() {
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

    //We use default profile
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

    // Key storage needs to be correctly adjusted to get server up with saml2.0
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public KeyManager keyManager() {
        samlKeystoreLocation = appSettings.getSamlKeystoreLocation();
        samlKeystorePassword = appSettings.getSamlKeystorePassword();
        samlKeystoreAlias = appSettings.getSamlKeystoreAlias();
        DefaultResourceLoader loader = new DefaultResourceLoader();
        File file = new File(samlKeystoreLocation);
        //Resource storeFile = loader.getResource(samlKeystoreLocation);
        FileSystemResource storeFile = new FileSystemResource(file);
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

    // These profiles and entry points are default implementations
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLEntryPoint samlEntryPoint() {
        SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
        samlEntryPoint.setDefaultProfileOptions(defaultWebSSOProfileOptions());
        return samlEntryPoint;
    }

    // Extended metadata
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public ExtendedMetadata extendedMetadata() {
        ExtendedMetadata extendedMetadata = new ExtendedMetadata();
        extendedMetadata.setIdpDiscoveryEnabled(false);
        extendedMetadata.setSigningAlgorithm("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256");
        extendedMetadata.setSignMetadata(true);
        extendedMetadata.setEcpEnabled(true);
        return extendedMetadata;
    }

    // TODO: Enable IDP discovery service for different IDPs
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLDiscovery samlIDPDiscovery() {
        SAMLDiscovery idpDiscovery = new SAMLDiscovery();
        idpDiscovery.setIdpSelectionPath("/saml/discovery");
        return idpDiscovery;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("okta")
    public ExtendedMetadataDelegate oktaExtendedMetadataProvider() throws MetadataProviderException {
        metadataUrl = appSettings.getSamlMetadata();
        logger.info("Provided metadata = {} ",metadataUrl);
        HTTPMetadataProvider metadataProvider
                = new HTTPMetadataProvider(this.backgroundTaskTimer, httpClient(), metadataUrl);
        metadataProvider.setParserPool(parserPool());
        metadataProvider.initialize();

        ExtendedMetadataDelegate extendedMetadataDelegate =
                new ExtendedMetadataDelegate(metadataProvider, extendedMetadata());
        extendedMetadataDelegate.setMetadataTrustCheck(true);
        extendedMetadataDelegate.setMetadataRequireSignature(false);

        backgroundTaskTimer.purge();
        return extendedMetadataDelegate;
    }

    // IDP Metadata configuration - more than one IDP can be configured but we configured only OKTA for now.
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

    // The filter is waiting for connections on URL suffixed with filterSuffix
    // and presents SP metadata there
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public MetadataDisplayFilter metadataDisplayFilter() {
        return new MetadataDisplayFilter();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("saml")
    public SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler() {
        SavedRequestAwareAuthenticationSuccessHandler successRedirectHandler =
                new SavedRequestAwareAuthenticationSuccessHandler() {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
                        super.onAuthenticationSuccess(request, response, authentication);
                        logger.info("Successfully logged in, redirecting");
                    }
                };
        successRedirectHandler.setDefaultTargetUrl("/");
        return successRedirectHandler;
    }

    // Handler deciding where to redirect user after failed login
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("saml")
    public SimpleUrlAuthenticationFailureHandler authenticationFailureHandler() {
        SimpleUrlAuthenticationFailureHandler failureHandler =
                new SimpleUrlAuthenticationFailureHandler();
        failureHandler.setUseForward(true);
        failureHandler.setDefaultFailureUrl("/error");
        return failureHandler;
    }

    // Handler for successful logout
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SimpleUrlLogoutSuccessHandler successLogoutHandler() {
        SimpleUrlLogoutSuccessHandler successLogoutHandler = new SimpleUrlLogoutSuccessHandler();
        successLogoutHandler.setDefaultTargetUrl("/");
        return successLogoutHandler;
    }

    // Logout handler terminating local session, local session does not terminate with single logout.
    // It should be terminated via url /logout
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Qualifier("saml")
    public SecurityContextLogoutHandler logoutHandler() {
        SecurityContextLogoutHandler logoutHandler =
                new SecurityContextLogoutHandler();
        logoutHandler.setInvalidateHttpSession(true);
        logoutHandler.setClearAuthentication(true);
        return logoutHandler;
    }

    //We may support single logout in future.
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLLogoutProcessingFilter samlLogoutProcessingFilter() {
        return new SAMLLogoutProcessingFilter(successLogoutHandler(),
                logoutHandler());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLLogoutFilter samlLogoutFilter() {
        return new SAMLLogoutFilter(successLogoutHandler(),
                new LogoutHandler[] { logoutHandler() },
                new LogoutHandler[] { logoutHandler() });
    }

    private ArtifactResolutionProfile artifactResolutionProfile() {
        final ArtifactResolutionProfileImpl artifactResolutionProfile =
                new ArtifactResolutionProfileImpl(httpClient());
        artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding()));
        return artifactResolutionProfile;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPArtifactBinding artifactBinding(ParserPool parserPool, VelocityEngine velocityEngine) {
        return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPSOAP11Binding soapBinding() {
        return new HTTPSOAP11Binding(parserPool());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPPostBinding httpPostBinding() {
        return new HTTPPostBinding(parserPool(), velocityEngine());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPRedirectDeflateBinding httpRedirectDeflateBinding() {
        return new HTTPRedirectDeflateBinding(parserPool());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPSOAP11Binding httpSOAP11Binding() {
        return new HTTPSOAP11Binding(parserPool());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public HTTPPAOS11Binding httpPAOS11Binding() {
        return new HTTPPAOS11Binding(parserPool());
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLProcessorImpl processor() {
        Collection<SAMLBinding> bindings = new ArrayList<>();
        bindings.add(httpRedirectDeflateBinding());
        bindings.add(httpPostBinding());
        bindings.add(artifactBinding(parserPool(), velocityEngine()));
        bindings.add(httpSOAP11Binding());
        bindings.add(httpPAOS11Binding());
        return new SAMLProcessorImpl(bindings);
    }
}

