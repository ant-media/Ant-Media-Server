package org.red5.server.tomcat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.catalina.Context;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.ServletContext;

/**
 * Covers {@link TomcatApplicationContext#getSpringContext()}, which the plugin deployer relies
 * on to reach each webapp's bean factory.
 */
public class TomcatApplicationContextTest {

	@Test
	public void testGetSpringContext_returnsContextFromServletAttribute() {
		ApplicationContext springCtx = mock(ApplicationContext.class);
		TomcatApplicationContext appCtx = contextWithAttribute(springCtx);

		assertEquals(springCtx, appCtx.getSpringContext());
	}

	@Test
	public void testGetSpringContext_nullWhenAttributeMissing() {
		assertNull(contextWithAttribute(null).getSpringContext());
	}

	/** Something else stored under the well-known key must not be cast blindly. */
	@Test
	public void testGetSpringContext_nullWhenAttributeIsWrongType() {
		assertNull(contextWithAttribute("not an application context").getSpringContext());
	}

	@Test
	public void testGetSpringContext_nullWhenServletContextUnavailable() {
		Context catalinaCtx = mock(Context.class);
		when(catalinaCtx.getServletContext()).thenThrow(new IllegalStateException("not started"));
		when(catalinaCtx.getName()).thenReturn("/LiveApp");

		assertNull(new TomcatApplicationContext(catalinaCtx).getSpringContext());
	}

	private TomcatApplicationContext contextWithAttribute(Object attribute) {
		ServletContext servletContext = mock(ServletContext.class);
		when(servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE))
				.thenReturn(attribute);

		Context catalinaCtx = mock(Context.class);
		when(catalinaCtx.getServletContext()).thenReturn(servletContext);

		return new TomcatApplicationContext(catalinaCtx);
	}
}
