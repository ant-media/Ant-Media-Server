package io.antmedia.console.servlet;

import java.io.IOException;
import java.util.Collection;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.message.BasicHeader;
import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;
import org.springframework.http.HttpHeaders;

import io.antmedia.console.rest.AuthenticationFilter;


public class ProxyServlet extends URITemplateProxyServlet {

	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		if (servletRequest.getAttribute(AuthenticationFilter.PANEL_AUTHENTICATED_ATTRIBUTE) == null) {
			servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		servletResponse.reset();
		hopByHopHeaders.addHeader(new BasicHeader("X-Forwarded-For", null));
		
		super.service(servletRequest, servletResponse);
		
	}

}
