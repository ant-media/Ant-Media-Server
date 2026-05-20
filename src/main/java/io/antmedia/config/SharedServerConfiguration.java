package io.antmedia.config;

import io.antmedia.settings.ServerSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans related to both the community and the enterprise
 */
@Configuration
public class SharedServerConfiguration {

    @Bean(ServerSettings.BEAN_NAME)
    public ServerSettings serverSettings() {
        return new ServerSettings();
    }
}
