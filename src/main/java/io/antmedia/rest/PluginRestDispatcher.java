package io.antmedia.rest;

import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;

import org.springframework.stereotype.Component;

/**
 * JAX-RS sub-resource locator for hot-loaded plugin REST services.
 * URL: {@code /{appName}/rest/plugins/{pluginKey}/...}
 *
 * Uses a static registry because Jersey creates its own instance independently
 * from the Spring bean — static state is the only reliable shared channel.
 */
@Component
@Path("/plugins")
public class PluginRestDispatcher {

    // contextPath → (pluginKey → handler)
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>
            REGISTRY = new ConcurrentHashMap<>();

    @Context
    private ServletContext servletContext;

    @Context
    private ResourceContext resourceContext;

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

    @Path("/{pluginKey}")
    public Object route(@PathParam("pluginKey") String pluginKey) {
        String contextPath = servletContext.getContextPath();
        ConcurrentHashMap<String, Object> handlers = REGISTRY.get(contextPath);
        Object handler = handlers != null ? handlers.get(pluginKey) : null;
        if (handler == null) {
            return null;
        }
        return resourceContext.initResource(handler); // injects @Context fields into handler
    }
}
