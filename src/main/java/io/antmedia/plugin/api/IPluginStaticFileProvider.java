package io.antmedia.plugin.api;

import java.io.File;

import jakarta.servlet.http.HttpServletRequest;

/**
 * SPI for V2 plugins that need to serve files from disk over HTTP.
 *
 * <p>Implementations are registered alongside an {@link IPluginServlet} in
 * {@code io.antmedia.servlet.PluginServletDispatcher}. When a request arrives, the dispatcher
 * checks the static file provider <em>first</em>: it asks the provider to resolve a file root
 * for the request, and if a non-null root is returned, the dispatcher itself reads the file
 * from disk, validates the resolved path stays inside the root (path-traversal protection),
 * sets the MIME type from the file extension via the servlet container, and streams the bytes
 * back to the client.</p>
 *
 * <p>If the provider returns {@code null} for a given request, the dispatcher falls through
 * to the registered {@link IPluginServlet#service} method for dynamic handling. This lets a
 * plugin selectively handle some paths via static file serving and others via custom logic
 * (for example, LL-HLS serves segment files statically and {@code __lowlatency.m3u8} requests
 * dynamically).</p>
 *
 * <p><strong>Why this exists:</strong> static file serving is the same code in every plugin —
 * read a path, validate it doesn't escape the root, look up a MIME type, stream the bytes.
 * Putting it in the dispatcher means plugin authors get it for free, the path-traversal check
 * is audited once instead of being re-implemented per plugin, and the MIME type comes from
 * StreamApp's web.xml mappings rather than each plugin maintaining its own table.</p>
 */
public interface IPluginStaticFileProvider {

    /**
     * Returns the on-disk root directory this plugin serves files from for the given request,
     * or {@code null} to decline static handling and fall through to {@link IPluginServlet}.
     *
     * <p>The dispatcher will resolve the request's path (everything after
     * {@code /{appName}/plugins/{pluginKey}/}) against this root, refuse the request with 404
     * if the canonicalized result escapes the root, and stream the file if it exists.</p>
     *
     * <p>Implementations may inspect the request URI, query parameters, or any header to
     * decide whether to serve statically or dynamically. They must <strong>not</strong> write
     * to the response — that's the dispatcher's job once the root is returned.</p>
     */
    File resolveFileRoot(HttpServletRequest req);
}
