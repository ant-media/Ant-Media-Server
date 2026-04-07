package io.antmedia.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.antmedia.plugin.api.IPluginServlet;
import io.antmedia.plugin.api.IPluginStaticFileProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Permanent gateway servlet for hot-loaded plugin servlet endpoints (V2 plugin system).
 *
 * <p>URL shape: {@code /{appName}/plugins/{pluginKey}/{remaining-path...}}</p>
 *
 * <p>Wired into StreamApp's {@code web.xml} at {@code /plugins/*} with {@code load-on-startup=1}
 * and {@code async-supported=true}, so every streaming application created from the StreamApp
 * template gets the dispatcher automatically.</p>
 *
 * <h3>Two registration shapes</h3>
 *
 * <p>Plugins can register two kinds of handlers per (context, key) tuple:</p>
 * <ol>
 *   <li>An {@link IPluginServlet} for dynamic request handling (parses query params, blocks
 *       on stream state, talks to plugin internals)</li>
 *   <li>An {@link IPluginStaticFileProvider} for serving files from disk — the dispatcher
 *       does the path validation, MIME type lookup, and byte streaming itself, so plugins
 *       don't reimplement this for every static asset they expose</li>
 * </ol>
 *
 * <p>A plugin can register either, or both. When both are registered, the dispatcher checks
 * the static file provider <em>first</em> on each request: if it returns a non-null file
 * root, the dispatcher serves the file from disk. If it returns {@code null} (or there is no
 * static file provider), the dispatcher delegates to the dynamic {@link IPluginServlet}.</p>
 *
 * <h3>Why a static registry</h3>
 *
 * <p>Same reason as {@code PluginRestDispatcher}: there is no shared bean factory between
 * the dispatcher servlet (instantiated by Tomcat per webapp) and the {@code PluginDeployer}
 * (lives in the AMS core Spring context, loaded by {@code ServerClassLoader}). Static state
 * is the only reliable shared channel. This class must be loaded by {@code ServerClassLoader}
 * (not a webapp classloader) so the static registries are single maps shared across webapps;
 * StreamApp's {@code web.xml} only references it by name and Tomcat resolves the class via
 * parent classloader delegation.</p>
 */
public class PluginServletDispatcher extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(PluginServletDispatcher.class);

    private static final int FILE_BUFFER_SIZE = 8192;

    /**
     * Webapp context path → (plugin key → dynamic handler).
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, IPluginServlet>>
            SERVLET_REGISTRY = new ConcurrentHashMap<>();

    /**
     * Webapp context path → (plugin key → static file provider).
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, IPluginStaticFileProvider>>
            STATIC_REGISTRY = new ConcurrentHashMap<>();

    /**
     * Register a dynamic servlet handler for the given (context path, plugin key). Overwrites
     * any existing handler at the same key — the dispatcher does not warn about collisions.
     */
    public static void register(String contextPath, String pluginKey, IPluginServlet handler) {
        SERVLET_REGISTRY.computeIfAbsent(contextPath, k -> new ConcurrentHashMap<>()).put(pluginKey, handler);
        logger.info("Registered plugin servlet handler '{}' in context '{}'", pluginKey, contextPath);
    }

    /**
     * Register a static file provider for the given (context path, plugin key). The dispatcher
     * will check this before falling through to any dynamic handler at the same key.
     */
    public static void registerStaticProvider(String contextPath, String pluginKey,
                                              IPluginStaticFileProvider provider) {
        STATIC_REGISTRY.computeIfAbsent(contextPath, k -> new ConcurrentHashMap<>()).put(pluginKey, provider);
        logger.info("Registered plugin static file provider '{}' in context '{}'", pluginKey, contextPath);
    }

    /**
     * Remove the dynamic handler at (context path, plugin key). No-op if nothing is registered.
     */
    public static void unregister(String contextPath, String pluginKey) {
        ConcurrentHashMap<String, IPluginServlet> map = SERVLET_REGISTRY.get(contextPath);
        if (map != null && map.remove(pluginKey) != null) {
            logger.info("Unregistered plugin servlet handler '{}' from context '{}'", pluginKey, contextPath);
        }
    }

    /**
     * Remove the static file provider at (context path, plugin key). No-op if nothing is registered.
     */
    public static void unregisterStaticProvider(String contextPath, String pluginKey) {
        ConcurrentHashMap<String, IPluginStaticFileProvider> map = STATIC_REGISTRY.get(contextPath);
        if (map != null && map.remove(pluginKey) != null) {
            logger.info("Unregistered plugin static file provider '{}' from context '{}'", pluginKey, contextPath);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // pathInfo for /LiveApp/plugins/ll-hls/stream1/x.m3u8 == "/ll-hls/stream1/x.m3u8"
        // Extract the first segment ("ll-hls") as the plugin key, and the remainder
        // ("stream1/x.m3u8") as the path that will be resolved against the file root.
        int slash = pathInfo.indexOf('/', 1);
        String pluginKey;
        String remainder;
        if (slash < 0) {
            pluginKey = pathInfo.substring(1);
            remainder = "";
        } else {
            pluginKey = pathInfo.substring(1, slash);
            remainder = pathInfo.substring(slash + 1); // strip the leading slash
        }

        String contextPath = req.getServletContext().getContextPath();

        // 1. Try the static file provider first. If it accepts the request, serve from disk.
        ConcurrentHashMap<String, IPluginStaticFileProvider> staticMap = STATIC_REGISTRY.get(contextPath);
        IPluginStaticFileProvider staticProvider = (staticMap != null) ? staticMap.get(pluginKey) : null;
        if (staticProvider != null) {
            File fileRoot = staticProvider.resolveFileRoot(req);
            if (fileRoot != null) {
                serveStaticFile(req, resp, fileRoot, remainder);
                return;
            }
        }

        // 2. Fall through to the dynamic handler.
        ConcurrentHashMap<String, IPluginServlet> handlers = SERVLET_REGISTRY.get(contextPath);
        IPluginServlet handler = (handlers != null) ? handlers.get(pluginKey) : null;
        if (handler == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                    "No plugin handler registered for key: " + pluginKey);
            return;
        }

        handler.service(req, resp);
    }

    /**
     * Resolve {@code remainder} against {@code fileRoot}, validate it stays inside the root,
     * set the MIME type from the file extension, and stream the bytes back. Path-traversal
     * attempts (resolved canonical path that escapes the root) result in 404, not 403, so an
     * attacker cannot probe whether a path is "valid but outside" vs "doesn't exist at all".
     */
    private void serveStaticFile(HttpServletRequest req, HttpServletResponse resp,
                                 File fileRoot, String remainder) throws IOException {
        File requested = new File(fileRoot, remainder);

        String rootCanonical;
        String requestedCanonical;
        try {
            rootCanonical = fileRoot.getCanonicalPath();
            requestedCanonical = requested.getCanonicalPath();
        } catch (IOException e) {
            logger.warn("Cannot canonicalize plugin file path: {}", e.getMessage());
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Path-traversal guard. The trailing separator on rootCanonical defends against the
        // edge case where fileRoot is "/x/foo" and a request crafts "/x/foobar" — the prefix
        // check would otherwise pass.
        String rootWithSep = rootCanonical.endsWith(File.separator) ? rootCanonical
                : rootCanonical + File.separator;
        if (!requestedCanonical.equals(rootCanonical) && !requestedCanonical.startsWith(rootWithSep)) {
            logger.warn("Plugin static file request escaped root: requested={} root={}",
                    requestedCanonical, rootCanonical);
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!requested.exists() || !requested.isFile() || !requested.canRead()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = req.getServletContext().getMimeType(requested.getName());
        if (mimeType != null) {
            resp.setContentType(mimeType);
        }
        resp.setContentLengthLong(requested.length());

        // Plain non-range streaming. We deliberately do not implement Range request handling
        // here — HLS segment requests are not range requests, they fetch full segment files,
        // and the dispatcher's job is the common case for plugin static assets. Plugins that
        // need range support can implement IPluginServlet directly.
        try (InputStream in = new FileInputStream(requested);
             ServletOutputStream out = resp.getOutputStream()) {
            byte[] buf = new byte[FILE_BUFFER_SIZE];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
    }
}
