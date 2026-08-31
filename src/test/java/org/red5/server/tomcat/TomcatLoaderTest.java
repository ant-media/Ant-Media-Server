package org.red5.server.tomcat;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.Loader;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import io.antmedia.cluster.IClusterNotifier;
import io.antmedia.test.UnitTestBase;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.*;

public class TomcatLoaderTest extends UnitTestBase<TomcatLoader> {

    private static final Object UNSET = new Object();

    /**
     * This Test checks handling of parse exception while creation
     * Previously if there is a parse exception while creating tomcat, it was not handled
     * and it cannot create any application after that exception
     */
    @Test
    public void testHandleParseException() {
        TomcatLoader tomcatLoader = spy(new TomcatLoader());
        Host host = mock(Host.class);
        when(host.getName()).thenReturn("localhost");

        Context context = mock(Context.class);
        try {
            doReturn(context).when(tomcatLoader).addContext(any(), any());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }

        ServletContext servletContext = mock(ServletContext.class);
        when(context.getServletContext()).thenReturn(servletContext);

        when(servletContext.getRealPath("/")).thenThrow(new IllegalStateException("Parse Exception"));

        tomcatLoader.setBaseHost(host);

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            //normally it didn't return in the bug case
            assertFalse(tomcatLoader.startWebApplication("test"));
            assertEquals(originalClassLoader, Thread.currentThread().getContextClassLoader());
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testClusterNotifier() {
    	
        TomcatLoader tomcatLoader = spy(new TomcatLoader());

        assertNull(tomcatLoader.getClusterNotifier());
        
        IClusterNotifier clusterNotifier = mock(IClusterNotifier.class);
        tomcatLoader.setClusterNotifier(clusterNotifier);
        
        
        assertEquals(clusterNotifier, tomcatLoader.getClusterNotifier());
    	
    }
    
    @Test
    public void testGetValves() {
    	
        TomcatLoader tomcatLoader = spy(new TomcatLoader());

        assertTrue(tomcatLoader.getValves().isEmpty());
    	
    }

    @Test
    public void testStartWebApplicationPublishesContextAfterRefresh() throws Exception {
        RootContextProbe.rootContextDuringRefresh.set(UNSET);

        TomcatLoader tomcatLoader = spy(new TomcatLoader());
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.containsBean(anyString())).thenReturn(false);
        when(applicationContext.getDisplayName()).thenReturn("test-context");
        tomcatLoader.setApplicationContext(applicationContext);

        Host host = mock(Host.class);
        Context context = mock(Context.class);
        Loader loader = mock(Loader.class);
        ServletContext servletContext = mock(ServletContext.class);
        AtomicReference<Object> publishedContext = new AtomicReference<>();

        Path webappRoot = Files.createTempDirectory("tomcat-loader-webapp");
        Path webInf = Files.createDirectories(webappRoot.resolve("WEB-INF"));
        Path contextXml = webInf.resolve("red5-test.xml");
        Files.writeString(contextXml, """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans xmlns="http://www.springframework.org/schema/beans"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.springframework.org/schema/beans https://www.springframework.org/schema/beans/spring-beans.xsd">
                    <bean class="org.red5.server.tomcat.TomcatLoaderTest$RootContextProbe" />
                </beans>
                """, StandardCharsets.UTF_8);

        when(host.findChild("/LiveApp")).thenReturn((Container) context);
        when(context.getServletContext()).thenReturn(servletContext);
        when(context.getLoader()).thenReturn(loader);
        when(loader.getDelegate()).thenReturn(false);
        when(loader.getClassLoader()).thenReturn(getClass().getClassLoader());
        when(servletContext.getContextPath()).thenReturn("/LiveApp");
        when(servletContext.getRealPath("/")).thenReturn(webappRoot.toString());
        when(servletContext.getInitParameter("contextConfigLocation")).thenReturn(contextXml.toUri().toString());
        when(servletContext.getInitParameterNames()).thenReturn(Collections.emptyEnumeration());
        when(servletContext.getAttributeNames()).thenReturn(Collections.emptyEnumeration());
        when(servletContext.getServletContextName()).thenReturn("LiveApp");
        when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
                .thenAnswer(invocation -> publishedContext.get());
        doAnswer(invocation -> {
            publishedContext.set(invocation.getArgument(1));
            return null;
        }).when(servletContext).setAttribute(eq(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE), any());

        tomcatLoader.setBaseHost(host);

        assertTrue(tomcatLoader.startWebApplication("LiveApp"));

        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertNull(RootContextProbe.rootContextDuringRefresh.get()));
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertTrue(publishedContext.get() instanceof WebApplicationContext));
    }

    public static class RootContextProbe implements ServletContextAware, InitializingBean {

        private static final AtomicReference<Object> rootContextDuringRefresh = new AtomicReference<>(UNSET);

        private ServletContext servletContext;

        @Override
        public void setServletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
        }

        @Override
        public void afterPropertiesSet() {
            rootContextDuringRefresh.set(
                    servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE));
        }
    }
}
