package io.antmedia.config;

import io.antmedia.licence.CommunityLicenceService;
import io.antmedia.licence.ILicenceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans related to only the community version of the server.
 */
@Configuration
@ConditionalOnMissingClass("io.antmedia.enterprise.adaptive.EncoderAdaptor")
public class CommunityOnlyServerConfiguration {

    @Bean(ILicenceService.BEAN_NAME)
    public ILicenceService communityLicenseService() {
        return new CommunityLicenceService();
    }
}
