package io.antmedia.rest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.ws.rs.container.ResourceContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PluginRestDispatcherTest {

    private PluginRestDispatcher dispatcher;
    private ServletContext servletContext;
    private ResourceContext resourceContext;

    @Before
    public void before() throws Exception {
        dispatcher = new PluginRestDispatcher();
        servletContext = mock(ServletContext.class);
        resourceContext = mock(ResourceContext.class);
        inject("servletContext", servletContext);
        inject("resourceContext", resourceContext);
    }

    @After
    public void after() throws Exception {
        // Clear static registry so tests don't bleed into each other
        Field f = PluginRestDispatcher.class.getDeclaredField("REGISTRY");
        f.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) f.get(null)).clear();
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field f = PluginRestDispatcher.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(dispatcher, value);
    }

    @Test
    public void testRoute_handlerFound_initResourceCalled() {
        Object handler = new Object();
        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(resourceContext.initResource(handler)).thenReturn(handler);

        PluginRestDispatcher.registerHandler("/LiveApp", "my-plugin", handler);
        Object result = dispatcher.route("my-plugin");

        assertSame(handler, result);
        verify(resourceContext).initResource(handler);
    }

    @Test
    public void testRoute_unknownKey_returnsNull() {
        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        PluginRestDispatcher.registerHandler("/LiveApp", "other-plugin", new Object());

        Object result = dispatcher.route("no-such-plugin");

        assertNull(result);
        verify(resourceContext, never()).initResource(any());
    }

    @Test
    public void testRoute_noContextRegistered_returnsNull() {
        when(servletContext.getContextPath()).thenReturn("/unregistered");

        Object result = dispatcher.route("my-plugin");

        assertNull(result);
        verify(resourceContext, never()).initResource(any());
    }

    @Test
    public void testUnregisterHandler_removesRoute() {
        Object handler = new Object();
        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        PluginRestDispatcher.registerHandler("/LiveApp", "my-plugin", handler);

        PluginRestDispatcher.unregisterHandler("/LiveApp", "my-plugin");

        assertNull(dispatcher.route("my-plugin"));
    }

    @Test
    public void testUnregisterHandler_unknownContext_noException() {
        PluginRestDispatcher.unregisterHandler("/nonexistent", "my-plugin");
    }

    @Test
    public void testRegisterHandler_multipleContexts_isolatedPerContext() {
        Object handlerA = new Object();
        Object handlerB = new Object();
        when(resourceContext.initResource(handlerA)).thenReturn(handlerA);
        when(resourceContext.initResource(handlerB)).thenReturn(handlerB);

        PluginRestDispatcher.registerHandler("/LiveApp", "my-plugin", handlerA);
        PluginRestDispatcher.registerHandler("/live", "my-plugin", handlerB);

        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        assertSame(handlerA, dispatcher.route("my-plugin"));

        when(servletContext.getContextPath()).thenReturn("/live");
        assertSame(handlerB, dispatcher.route("my-plugin"));
    }

    @Test
    public void testRegisterHandler_overwritesExisting() {
        Object first = new Object();
        Object second = new Object();
        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(resourceContext.initResource(second)).thenReturn(second);

        PluginRestDispatcher.registerHandler("/LiveApp", "my-plugin", first);
        PluginRestDispatcher.registerHandler("/LiveApp", "my-plugin", second);

        assertSame(second, dispatcher.route("my-plugin"));
    }
}
