package io.antmedia.console.servlet;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.message.BasicHeader;
import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;
import org.springframework.http.HttpHeaders;


public class ProxyServlet extends URITemplateProxyServlet {
	
	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		servletResponse.reset();
		hopByHopHeaders.addHeader(new BasicHeader("X-Forwarded-For", null));
		
		super.service(servletRequest, servletResponse);
		
	}

}
