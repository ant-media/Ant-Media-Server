package io.antmedia.saml;

import io.antmedia.AppSettings;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.*;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.metadata.*;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

/**
 * Main configuration class for wiring together DB + SAML authentication
 *
 * @author vdenotaris
 * @author jcavazos
 * @see <a href="https://github.com/vdenotaris/spring-boot-security-saml-sample/blob/master/src/main/java/com/vdenotaris/spring/boot/security/saml/web/config/WebSecurityConfig.java">WebSecurityConfig</a>
 */
@PropertySource("/WEB-INF/red5-web.properties")
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    private String samlAudience;
            //"http://localhost:5080/LiveApp/saml/metadata";

    private AppSettings appSettings;

    @Autowired(required=false)
    @Qualifier("saml")
    private SavedRequestAwareAuthenticationSuccessHandler samlAuthSuccessHandler;

    @Autowired(required=false)
    @Qualifier("saml")
    private SimpleUrlAuthenticationFailureHandler samlAuthFailureHandler;

    @Autowired(required=false)
    @Qualifier("saml")
    private Timer samlBackgroundTaskTimer;

    @Autowired(required=false)
    @Qualifier("saml")
    private SecurityContextLogoutHandler logoutHandler;

    @Autowired(required=false)
    @Qualifier("saml")
    private MultiThreadedHttpConnectionManager samlMultiThreadedHttpConnectionManager;

    @Autowired(required=false)
    private SAMLEntryPoint samlEntryPoint;

    @Autowired(required=false)
    private SAMLLogoutFilter samlLogoutFilter;

    @Autowired(required=false)
    private MetadataDisplayFilter metadataDisplayFilter;

    @Autowired(required=false)
    private SAMLLogoutProcessingFilter samlLogoutProcessingFilter;

    @Autowired(required=false)
    private SAMLDiscovery samlDiscovery;

    @Autowired(required=false)
    private SAMLAuthenticationProvider samlAuthenticationProvider;

    @Autowired(required=false)
    private ExtendedMetadata extendedMetadata;

    @Autowired(required=false)
    private CachingMetadataManager cachingMetadataManager;

    @Autowired(required=false)
    private KeyManager keyManager;

    public boolean checkEnable(){
        if(appSettings.isSamlEnabled()){
            return true;
        }
        else{
            return false;
        }
    }

    //Metadata generator for SP metadata
    public MetadataGenerator metadataGenerator() {
        if(checkEnable()){
            samlAudience = appSettings.getSamlAudience();
            MetadataGenerator metadataGenerator = new MetadataGenerator();
            metadataGenerator.setEntityId(samlAudience);
            metadataGenerator.setExtendedMetadata(extendedMetadata);
            metadataGenerator.setIncludeDiscoveryExtension(false);
            metadataGenerator.setKeyManager(keyManager);
            return metadataGenerator;
        }
        return null;
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter() throws Exception {
        SAMLWebSSOHoKProcessingFilter samlWebSSOHoKProcessingFilter = new SAMLWebSSOHoKProcessingFilter();
        samlWebSSOHoKProcessingFilter.setAuthenticationSuccessHandler(samlAuthSuccessHandler);
        samlWebSSOHoKProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOHoKProcessingFilter.setAuthenticationFailureHandler(samlAuthFailureHandler);
        return samlWebSSOHoKProcessingFilter;
    }
    public AppSettings getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    // SSO filter processes the incoming authorization from IDP
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public SAMLProcessingFilter samlWebSSOProcessingFilter() throws Exception {
        SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
        samlWebSSOProcessingFilter.setAuthenticationManager(authenticationManager());
        samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(samlAuthSuccessHandler);
        samlWebSSOProcessingFilter.setAuthenticationFailureHandler(samlAuthFailureHandler);
        return samlWebSSOProcessingFilter;
    }

    /**
     * Incoming requests are going through this filter chain for SAML SSO authentication.
     * TODO : Enable IDP discovery filter.
     * @return Filter chain proxy
     */
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public FilterChainProxy samlFilter() throws Exception {
        List<SecurityFilterChain> chains = new ArrayList<>();
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/login/**"),
                samlEntryPoint));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/logout/**"),
                samlLogoutFilter));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/metadata/**"),
                metadataDisplayFilter));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSO/**"),
                samlWebSSOProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SSOHoK/**"),
                samlWebSSOHoKProcessingFilter()));
        chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/SingleLogout/**"),
                samlLogoutProcessingFilter));
        //chains.add(new DefaultSecurityFilterChain(new AntPathRequestMatcher("/saml/discovery/**"),
        // samlDiscovery));
        return new FilterChainProxy(chains);
    }

    /**
     * Returns the authentication manager currently used by Spring.
     */
    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Bean
    @ConditionalOnProperty(name="settings.saml.enabled", havingValue="true")
    public MetadataGeneratorFilter metadataGeneratorFilter() {
        return new MetadataGeneratorFilter(metadataGenerator());
    }

    /**
     * Adjusts entry point and logout, permits if user is authenticated.
     * @param http Allows configuring web based security for specific http requests.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if(checkEnable()){
            http
                    .csrf()
                    .disable();
            http
                    .authorizeRequests()
                    .antMatchers("/logout.html").permitAll();

            http
                    .httpBasic().authenticationEntryPoint(samlEntryPoint);

            http
                    .addFilterBefore(metadataGeneratorFilter(), ChannelProcessingFilter.class)
                    .addFilterAfter(samlFilter(), BasicAuthenticationFilter.class)
                    .addFilterBefore(samlFilter(), CsrfFilter.class);
            http
                    .authorizeRequests()
                    .antMatchers("/").permitAll()
                    .antMatchers("/pre-auth**").permitAll()
                    .antMatchers("/form-login**").permitAll()
                    .antMatchers("/error").permitAll()
                    .antMatchers("/saml/**").permitAll()
                    .antMatchers("/css/**").permitAll()
                    .antMatchers("/img/**").permitAll()
                    .antMatchers("/js/**").permitAll()
                    .antMatchers("/sw.js").permitAll()
                    .anyRequest().authenticated();

            http
                    .logout()
                    .logoutUrl("/logout")
                    .addLogoutHandler((request, response, authentication) -> {
                        logoutHandler.logout(request, response, authentication);
                        try {
                            String appName = appSettings.getAppName();
                            response.sendRedirect("/"+ appName + "/logout.html");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }

    /**
     * Authentication provider is only saml currently.
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        if(checkEnable()){
            auth.authenticationProvider(samlAuthenticationProvider);
        }
    }

    @Override
    public void destroy() throws Exception {
        if(checkEnable()){
            this.samlBackgroundTaskTimer.purge();
            this.samlBackgroundTaskTimer.cancel();
            this.samlMultiThreadedHttpConnectionManager.shutdown();
        }
    }
}
