package io.antmedia.test.plugin;

import org.springframework.stereotype.Component;

/**
 * Minimal Spring @Component used only in unit tests for PluginDeployer.
 * Must live in the {@code io.antmedia} package hierarchy so the deployer's
 * scanner (base package {@code io.antmedia}) can discover it.
 */
@Component("plugin.minimal-component")
public class MinimalSpringComponent {
}
