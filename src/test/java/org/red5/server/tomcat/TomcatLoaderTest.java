package org.red5.server.tomcat;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class TomcatLoaderTest {

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
}
