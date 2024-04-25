package io.antmedia.analytic;

import org.red5.server.api.scope.IScope;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.analytic.model.AnalyticEvent;
import io.antmedia.analytic.model.PlayEvent;
import io.antmedia.analytic.model.WatchTimeEvent;
import io.antmedia.filter.JWTFilter;
import io.antmedia.logger.LoggerUtils;
import io.antmedia.rest.model.Result;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;


@OpenAPIDefinition(
		info = @Info(
				description = "Ant Media Server - Player Analytic Events Endpoint. It just logs the incoming player events",
				version = "v2.0",
				title = "Ant Media Server - Player Analytic Events Endpoint",
				contact = @Contact(name = "Ant Media Info", email = "contact@antmedia.io", url = "https://antmedia.io"),
				license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0"))
		)
@Component
@Path("/")
public class AnalyticEventLogger {


	@Context
	protected ServletContext servletContext;
	protected ApplicationContext appCtx;
	protected AntMediaApplicationAdapter appInstance;

	public ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}

	public AntMediaApplicationAdapter getApplication() {
		if (appInstance == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				appInstance = (AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			}
		}
		return appInstance;
	}
	

	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/events/play")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postEvent(@Context HttpServletRequest request, PlayEvent event) {
		
		if (!isAuthorized(event)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		event.setApp(getApplication().getScope().getName());
		if (event.getTimeMs() == 0) {
			event.setTimeMs(System.currentTimeMillis());
		}
		event.setClientIP(getClientIpAddress(request));
		
		LoggerUtils.logAnalyticsFromClient( event);

		return Response.ok(new Result(true)).build();
	}
	
	@POST
	@Consumes({ MediaType.APPLICATION_JSON })
	@Path("/events/watch-time")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postEvent(@Context HttpServletRequest request, WatchTimeEvent event) 
	{
		if (!isAuthorized(event)) {
			return Response.status(Status.UNAUTHORIZED).build();
		}
		
		event.setApp(getApplication().getScope().getName());
		if (event.getTimeMs() == 0) {
			event.setTimeMs(System.currentTimeMillis());
		}
		event.setClientIP(getClientIpAddress(request));
		
		LoggerUtils.logAnalyticsFromClient(event);

		return Response.ok(new Result(true)).build();
	}

	private boolean isAuthorized(PlayEvent event) {
		return (!getApplication().getAppSettings().isSecureAnalyticEndpoint() ||  
				(JWTFilter.isJWTTokenValid(getApplication().getAppSettings().getJwtSecretKey(), event.getToken())));
	}

	private String getClientIpAddress(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-Forwarded-For");
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}
		return ipAddress;
	}
	

}
