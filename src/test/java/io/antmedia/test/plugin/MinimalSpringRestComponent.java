package io.antmedia.test.plugin;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.springframework.stereotype.Component;

/**
 * Minimal Spring @Component + JAX-RS @Path class used only in unit tests for
 * PluginDeployer REST-key derivation.
 *
 * The class-level @Path("/test-plugin") means the deployer will derive the
 * dispatcher key "test-plugin" and register it with PluginRestDispatcher.
 */
@Component("plugin.minimal-rest")
@Path("/test-plugin")
public class MinimalSpringRestComponent {

    @GET
    public String ping() {
        return "pong";
    }
}
