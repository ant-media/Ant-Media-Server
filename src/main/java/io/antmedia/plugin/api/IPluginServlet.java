package io.antmedia.plugin.api;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * SPI for plugin-provided servlet endpoints in V2 hot-loaded plugins.
 *
 * <p>Implementations are registered with {@code io.antmedia.servlet.PluginServletDispatcher}
 * during plugin activation (typically from {@link IServerListener#onPluginEnabled(String)})
 * and unregistered on plugin disable. The dispatcher is a permanent servlet pre-mounted at
 * {@code /plugins/*} in every streaming application; it routes requests to the registered
 * handler whose plugin key matches the first path segment after {@code /plugins/}.</p>
 *
 * <p>The dispatcher owns the standard servlet init/destroy lifecycle. Implementations are
 * just request handlers — a single instance serves all concurrent requests for its plugin
 * key, so implementations must be thread-safe.</p>
 *
 * <p>Example URL: {@code /LiveApp/plugins/ll-hls/stream1/stream1__lowlatency.m3u8} routes to
 * the handler registered under key {@code "ll-hls"} in the {@code /LiveApp} context.</p>
 */
public interface IPluginServlet {

    void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException;
}
