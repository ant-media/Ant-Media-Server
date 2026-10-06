package io.antmedia.test.plugin;

import org.springframework.stereotype.Component;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/**
 * Minimal Spring {@code @Component} REST plugin used in {@code PluginDeployerTest}. Has both
 * {@code @Component} and {@code @Path} — the deployer should detect the {@code @Path} and
 * register a REST dispatcher key derived from its last segment ({@code "test-plugin"}).
 */
@Component
@Path("/test-plugin")
public class MinimalSpringRestComponent {

    @Context
    private ServletContext servletContext;

    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public String ping() {
        return "{\"status\":\"ok\"}";
    }
}
