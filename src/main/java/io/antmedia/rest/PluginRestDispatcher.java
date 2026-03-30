package io.antmedia.rest;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

import org.springframework.stereotype.Component;

/**
 * Pre-registered JAX-RS sub-resource locator that routes requests to
 * hot-loaded plugin REST services.
 *
 * Registered in Spring (and therefore Jersey) at server startup via the
 * {@code io.antmedia.rest} component-scan in {@code red5-web.xml}.
 *
 * <h3>Why a static registry?</h3>
 * AMS streaming apps do not use the jersey-spring bridge. Jersey creates its
 * own instance of this class independently from the Spring bean. Using a
 * static map keyed by webapp context path lets {@link org.red5.server.plugin.PluginDeployer}
 * (which works with the Spring instance) and Jersey (which works with its own
 * instance) share state without needing to be the same object.
 *
 * <h3>URL shape</h3>
 * {@code /{appName}/rest/plugins/{pluginKey}/{method-paths...}}
 *
 * The {@code pluginKey} is the last segment of the plugin REST service's
 * class-level {@code @Path} value (e.g. {@code @Path("/v1/hls-merger")} →
 * key {@code "hls-merger"}).
 */
@Component
@Path("/plugins")
public class PluginRestDispatcher {

    /**
     * Static registry: webapp context path (e.g. "/LiveApp") →
     * (plugin key → REST handler instance).
     * Static so both Jersey-managed and Spring-managed instances share state.
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>
            REGISTRY = new ConcurrentHashMap<>();

    @Context
    private ServletContext servletContext;

    @Context
    private ResourceContext resourceContext;

    // -------------------------------------------------------------------------
    // Static API — called by PluginDeployer
    // -------------------------------------------------------------------------

    public static void registerHandler(String contextPath, String pluginKey, Object handler) {
        REGISTRY.computeIfAbsent(contextPath, k -> new ConcurrentHashMap<>())
                .put(pluginKey, handler);
    }

    public static void unregisterHandler(String contextPath, String pluginKey) {
        ConcurrentHashMap<String, Object> map = REGISTRY.get(contextPath);
        if (map != null) {
            map.remove(pluginKey);
        }
    }

    // -------------------------------------------------------------------------
    // JAX-RS sub-resource locator
    // -------------------------------------------------------------------------

    /**
     * Sub-resource locator — Jersey routes the remaining path into the
     * returned object's method-level {@code @Path} annotations.
     * Returns {@code null} (→ 404) if no plugin is registered for the key.
     */
    @Path("/{pluginKey}")
    public Object route(@PathParam("pluginKey") String pluginKey) {
        String contextPath = servletContext.getContextPath();
        ConcurrentHashMap<String, Object> handlers = REGISTRY.get(contextPath);
        Object handler = handlers != null ? handlers.get(pluginKey) : null;
        if (handler == null) {
            return null;
        }
        // initResource injects @Context fields (e.g. ServletContext) into the
        // sub-resource instance before Jersey routes into its methods.
        return resourceContext.initResource(handler);
    }
}
