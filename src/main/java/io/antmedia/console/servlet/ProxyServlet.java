package io.antmedia.console.servlet;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.mitre.dsmiley.httpproxy.URITemplateProxyServlet;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.AppSettings;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.rest.AuthenticationFilter;
import io.antmedia.filter.JWTFilter;


public class ProxyServlet extends URITemplateProxyServlet {

	public static final long DASHBOARD_JWT_EXPIRE_MS = 60000;

	@Override
	protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		servletResponse.reset();
		hopByHopHeaders.addHeader(new BasicHeader("X-Forwarded-For", null));

		super.service(servletRequest, servletResponse);

	}

	/**
	 * Signs the app JWT for the web panel with the server clock.
	 * Request is already authenticated by AuthenticationFilter. Browser-signed tokens are rejected when browser clock is ahead
	 */
	@Override
	protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
		super.copyRequestHeaders(servletRequest, proxyRequest);

		AppSettings appSettings = getAppSettings(servletRequest);
		if (appSettings != null && appSettings.isJwtControlEnabled() && StringUtils.isBlank(appSettings.getJwksURL())) {
			proxyRequest.setHeader(JWTFilter.JWT_TOKEN_AUTHORIZATION_HEADER,
					JWTFilter.generateJwtToken(appSettings.getJwtSecretKey(), System.currentTimeMillis() + DASHBOARD_JWT_EXPIRE_MS));
		}
	}

	public AppSettings getAppSettings(HttpServletRequest req) {
		//_path is like live/rest/v2/broadcasts/list/0/10
		String appName = StringUtils.substringBefore(StringUtils.stripStart(req.getParameter(AuthenticationFilter.DISPATCH_PATH_URL), "/"), "/");
		WebApplicationContext ctxt = (WebApplicationContext) req.getServletContext().getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		if (StringUtils.isBlank(appName) || ctxt == null) {
			return null;
		}
		ApplicationContext appContext = ((AdminApplication) ctxt.getBean("web.handler")).getApplicationContext(appName);
		if (appContext == null) {
			return null;
		}
		return ((AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME)).getAppSettings();
	}

}
