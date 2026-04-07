package io.antmedia.servlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import io.antmedia.plugin.api.IPluginServlet;
import io.antmedia.plugin.api.IPluginStaticFileProvider;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PluginServletDispatcherTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private PluginServletDispatcher dispatcher;
    private HttpServletRequest req;
    private HttpServletResponse resp;
    private ServletContext servletContext;

    @Before
    public void before() {
        dispatcher = new PluginServletDispatcher();
        req = mock(HttpServletRequest.class);
        resp = mock(HttpServletResponse.class);
        servletContext = mock(ServletContext.class);
        when(req.getServletContext()).thenReturn(servletContext);
    }

    @After
    public void after() throws Exception {
        // Clear both static registries so tests don't bleed into each other.
        for (String fieldName : new String[] { "SERVLET_REGISTRY", "STATIC_REGISTRY" }) {
            Field f = PluginServletDispatcher.class.getDeclaredField(fieldName);
            f.setAccessible(true);
            ((ConcurrentHashMap<?, ?>) f.get(null)).clear();
        }
    }

    @Test
    public void testService_routesToHandler() throws Exception {
        IPluginServlet handler = mock(IPluginServlet.class);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/stream1/stream1__lowlatency.m3u8");

        dispatcher.service(req, resp);

        verify(handler).service(req, resp);
        verify(resp, never()).sendError(anyInt(), anyString());
        verify(resp, never()).sendError(anyInt());
    }

    @Test
    public void testService_keyOnlyNoTrailingSlash_routes() throws Exception {
        IPluginServlet handler = mock(IPluginServlet.class);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls");

        dispatcher.service(req, resp);

        verify(handler).service(req, resp);
    }

    @Test
    public void testService_unknownPluginKey_returns404() throws Exception {
        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/no-such-plugin/whatever");

        dispatcher.service(req, resp);

        verify(resp).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testService_unknownContextPath_returns404() throws Exception {
        IPluginServlet handler = mock(IPluginServlet.class);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);

        when(servletContext.getContextPath()).thenReturn("/SomeOtherApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/stream1");

        dispatcher.service(req, resp);

        verify(handler, never()).service(any(), any());
        verify(resp).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testService_nullPathInfo_returns404() throws Exception {
        when(req.getPathInfo()).thenReturn(null);

        dispatcher.service(req, resp);

        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testService_emptyPathInfo_returns404() throws Exception {
        when(req.getPathInfo()).thenReturn("/");

        dispatcher.service(req, resp);

        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testService_multipleContextsIsolated() throws Exception {
        IPluginServlet handlerA = mock(IPluginServlet.class);
        IPluginServlet handlerB = mock(IPluginServlet.class);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handlerA);
        PluginServletDispatcher.register("/WebRTCAppEE", "ll-hls", handlerB);

        when(req.getPathInfo()).thenReturn("/ll-hls/stream1");

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        dispatcher.service(req, resp);
        verify(handlerA, times(1)).service(req, resp);
        verify(handlerB, never()).service(any(), any());

        when(servletContext.getContextPath()).thenReturn("/WebRTCAppEE");
        dispatcher.service(req, resp);
        verify(handlerB, times(1)).service(req, resp);
        verify(handlerA, times(1)).service(req, resp); // unchanged from before
    }

    @Test
    public void testRegister_overwritesExisting() throws Exception {
        IPluginServlet first = mock(IPluginServlet.class);
        IPluginServlet second = mock(IPluginServlet.class);

        PluginServletDispatcher.register("/LiveApp", "ll-hls", first);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", second);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/x");
        dispatcher.service(req, resp);

        verify(first, never()).service(any(), any());
        verify(second).service(req, resp);
    }

    @Test
    public void testUnregister_removesRoute() throws Exception {
        IPluginServlet handler = mock(IPluginServlet.class);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);
        PluginServletDispatcher.unregister("/LiveApp", "ll-hls");

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/x");
        dispatcher.service(req, resp);

        verify(handler, never()).service(any(), any());
        verify(resp).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testUnregister_unknownContext_noException() {
        PluginServletDispatcher.unregister("/nonexistent", "ll-hls");
        // No assertion needed — pass condition is "no exception thrown".
    }

    @Test
    public void testUnregister_unknownKeyInExistingContext_noException() {
        PluginServletDispatcher.register("/LiveApp", "other", mock(IPluginServlet.class));
        PluginServletDispatcher.unregister("/LiveApp", "ll-hls");
    }

    @Test
    public void testService_handlerSeesFullPathInfo() throws Exception {
        // The dispatcher must NOT strip the /pluginKey prefix from the request — handlers
        // should see the full pathInfo so they can parse it themselves (LL-HLS does this).
        AtomicReference<String> seenPathInfo = new AtomicReference<>();
        IPluginServlet handler = (request, response) -> seenPathInfo.set(request.getPathInfo());
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/stream1/stream1__lowlatency.m3u8");

        dispatcher.service(req, resp);

        assertNotNull(seenPathInfo.get());
        assertEquals("/ll-hls/stream1/stream1__lowlatency.m3u8", seenPathInfo.get());
    }

    // ====================================================================================
    // Static file provider tests
    // ====================================================================================

    /**
     * Captures the bytes the dispatcher writes via {@code resp.getOutputStream()} so tests
     * can assert on file content. Mockito does not provide a default ServletOutputStream
     * implementation; we have to roll one.
     */
    private static class CapturingServletOutputStream extends ServletOutputStream {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        @Override public boolean isReady() { return true; }
        @Override public void setWriteListener(WriteListener writeListener) { }
        @Override public void write(int b) { buf.write(b); }
        @Override public void write(byte[] b, int off, int len) { buf.write(b, off, len); }
    }

    private CapturingServletOutputStream stubOutputStream() throws IOException {
        CapturingServletOutputStream out = new CapturingServletOutputStream();
        when(resp.getOutputStream()).thenReturn(out);
        return out;
    }

    @Test
    public void testStaticFile_servesExistingFile() throws Exception {
        File root = tempFolder.newFolder("ll-hls-root");
        File streamDir = new File(root, "stream1");
        streamDir.mkdirs();
        File segment = new File(streamDir, "stream1_001.ts");
        Files.write(segment.toPath(), new byte[] { 1, 2, 3, 4, 5 });

        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(root);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(servletContext.getMimeType("stream1_001.ts")).thenReturn("video/MP2T");
        when(req.getPathInfo()).thenReturn("/ll-hls/stream1/stream1_001.ts");
        CapturingServletOutputStream out = stubOutputStream();

        dispatcher.service(req, resp);

        verify(resp).setContentType("video/MP2T");
        verify(resp).setContentLengthLong(5L);
        verify(resp, never()).sendError(anyInt());
        verify(resp, never()).sendError(anyInt(), anyString());
        org.junit.Assert.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, out.buf.toByteArray());
    }

    @Test
    public void testStaticFile_missingFile_returns404() throws Exception {
        File root = tempFolder.newFolder("ll-hls-root");

        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(root);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/nope.ts");

        dispatcher.service(req, resp);

        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testStaticFile_pathTraversal_returns404() throws Exception {
        File root = tempFolder.newFolder("ll-hls-root");
        // Create a sibling file outside the root that the attacker is trying to read.
        File sibling = new File(root.getParentFile(), "secret.txt");
        Files.write(sibling.toPath(), new byte[] { 9 });

        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(root);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/../secret.txt");

        dispatcher.service(req, resp);

        // Must return 404 (not 403) so an attacker cannot distinguish "outside root" from
        // "doesn't exist".
        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
        verify(resp, never()).setContentType(anyString());
    }

    @Test
    public void testStaticFile_pathTraversalWithSiblingPrefix_returns404() throws Exception {
        // Defends against the edge case where root is "/x/foo" and a request crafts "/x/foobar".
        // A naive prefix check (without trailing separator) would let "foobar" through.
        File parent = tempFolder.newFolder("parent");
        File root = new File(parent, "foo");
        root.mkdirs();
        File sibling = new File(parent, "foobar");
        sibling.mkdirs();
        File siblingFile = new File(sibling, "leaked.txt");
        Files.write(siblingFile.toPath(), new byte[] { 7 });

        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(root);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        // ../foobar/leaked.txt — traversal that escapes via a sibling prefix.
        when(req.getPathInfo()).thenReturn("/ll-hls/../foobar/leaked.txt");

        dispatcher.service(req, resp);

        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testStaticFile_directoryRequested_returns404() throws Exception {
        File root = tempFolder.newFolder("ll-hls-root");
        File subdir = new File(root, "stream1");
        subdir.mkdirs();

        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(root);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/stream1");

        dispatcher.service(req, resp);

        // Directories are not files; refuse with 404 (no directory listings).
        verify(resp).sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testStaticFile_providerReturnsNull_fallsThroughToHandler() throws Exception {
        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(null); // declines static handling
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        IPluginServlet handler = mock(IPluginServlet.class);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/stream1__lowlatency.m3u8");

        dispatcher.service(req, resp);

        verify(handler).service(req, resp);
        verify(resp, never()).sendError(anyInt());
    }

    @Test
    public void testStaticFile_providerReturnsNullAndNoDynamicHandler_returns404() throws Exception {
        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(null);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);
        // No IPluginServlet registered.

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/anything");

        dispatcher.service(req, resp);

        verify(resp).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testStaticFile_unknownMimeType_serveWithoutContentType() throws Exception {
        File root = tempFolder.newFolder("ll-hls-root");
        File file = new File(root, "weird.xyz");
        Files.write(file.toPath(), new byte[] { 42 });

        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(root);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(servletContext.getMimeType("weird.xyz")).thenReturn(null);
        when(req.getPathInfo()).thenReturn("/ll-hls/weird.xyz");
        stubOutputStream();

        dispatcher.service(req, resp);

        verify(resp, never()).setContentType(anyString());
        verify(resp).setContentLengthLong(1L);
        verify(resp, never()).sendError(anyInt());
    }

    @Test
    public void testUnregisterStaticProvider_removesProvider() throws Exception {
        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);
        PluginServletDispatcher.unregisterStaticProvider("/LiveApp", "ll-hls");

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/x");

        dispatcher.service(req, resp);

        verify(provider, never()).resolveFileRoot(any());
        verify(resp).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testStaticAndDynamic_registeredTogether_staticTakesPriority() throws Exception {
        File root = tempFolder.newFolder("ll-hls-root");
        File file = new File(root, "served.ts");
        Files.write(file.toPath(), new byte[] { 1 });

        IPluginStaticFileProvider provider = mock(IPluginStaticFileProvider.class);
        when(provider.resolveFileRoot(any())).thenReturn(root);
        PluginServletDispatcher.registerStaticProvider("/LiveApp", "ll-hls", provider);

        IPluginServlet handler = mock(IPluginServlet.class);
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/served.ts");
        stubOutputStream();

        dispatcher.service(req, resp);

        // Static path served the file; dynamic handler must not be called for this request.
        verify(handler, never()).service(any(), any());
        verify(resp).setContentLengthLong(1L);
    }

    // ====================================================================================
    // Original servlet handler tests continue below
    // ====================================================================================

    @Test
    public void testService_handlerExceptionPropagates() throws Exception {
        // If the handler throws, the exception should propagate to the servlet container —
        // the dispatcher must not swallow it.
        IPluginServlet handler = mock(IPluginServlet.class);
        java.io.IOException boom = new java.io.IOException("boom");
        org.mockito.Mockito.doThrow(boom).when(handler).service(any(), any());
        PluginServletDispatcher.register("/LiveApp", "ll-hls", handler);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(req.getPathInfo()).thenReturn("/ll-hls/x");

        try {
            dispatcher.service(req, resp);
            org.junit.Assert.fail("expected IOException to propagate");
        } catch (java.io.IOException e) {
            assertEquals("boom", e.getMessage());
        }
        verify(handler, atLeastOnce()).service(req, resp);
    }
}
