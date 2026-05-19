package io.antmedia.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Configuration;

/**
 * Beans related to only the community version of the server.
 */
@Configuration
@ConditionalOnMissingClass("io.antmedia.enterprise.adaptive.EncoderAdaptor")
public class CommunityOnlyServerConfiguration {
}
