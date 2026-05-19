package io.antmedia.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Configuration;

/**
 * Beans related to both the community and the enterprise
 */
@Configuration
@ConditionalOnMissingClass("io.antmedia.enterprise.adaptive.EncoderAdaptor")
public class SharedServerConfiguration {
}
