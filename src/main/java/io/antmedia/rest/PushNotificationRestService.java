package io.antmedia.rest;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.filter.JWTFilter;
import io.antmedia.pushnotification.IPushNotificationService;
import io.antmedia.rest.model.Result;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

@Component
@Path("/v2/push-notification")
public class PushNotificationRestService {
	
	@Context
	protected ServletContext servletContext;
	
	protected ApplicationContext appCtx;

	private IPushNotificationService pushNotificationService;
	
	private AppSettings appSettings;

	
	
	public ApplicationContext getAppContext() {
		if (servletContext != null) {
			appCtx = (ApplicationContext) servletContext
					.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
		}
		return appCtx;
	}
	
	
	public IPushNotificationService getPushNotificationService() {
		if (pushNotificationService == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				pushNotificationService = (IPushNotificationService) appContext.getBean(IPushNotificationService.BEAN_NAME);
			}
		}
		return pushNotificationService;
	}
	
	public AppSettings getAppSettings() {
		if (appSettings == null) {
			ApplicationContext appContext = getAppContext();
			if (appContext != null) {
				appSettings = (AppSettings) appContext.getBean(AppSettings.BEAN_NAME);
			}
		}
		return appSettings;
	}
	
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/subscriber-auth-token")
	public Result getSubscriberAuthenticationToken(@QueryParam("subscriberId") String subscriberId, @QueryParam("timeoutSeconds") int timeoutDurationInSeconds) {
		if (timeoutDurationInSeconds <= 0) {
			//one hour default - 3600 seconds
			timeoutDurationInSeconds = 3600;
		}
		long expireTimeMs = System.currentTimeMillis() + (timeoutDurationInSeconds * 1000);
		String jwtToken = JWTFilter.generateJwtToken(getAppSettings().getSubscriberAuthenticationKey(), expireTimeMs, subscriberId);
		
		return new Result(true, jwtToken, "Token is available in dataId field");
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/subscribers")
	public Result sendPushNotification(@FormParam("subscribers") List<String> subscriberIdList, String jsonMessage, @QueryParam("serviceName") String serviceName) {
		if (StringUtils.isBlank(serviceName)) {
			return getPushNotificationService().sendNotification(subscriberIdList, jsonMessage);
		}
		else {
			return getPushNotificationService().sendNotification(subscriberIdList, jsonMessage, serviceName);
		}
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/topics/{topic}/{serviceName}")
	public Result sendPushNotification(@PathParam("topic") String topic, String jsonMessage, @PathParam("serviceName") String serviceName) {
		if (StringUtils.isBlank(serviceName)) {
			return getPushNotificationService().sendNotification(topic, jsonMessage);
		}
		else {
			return getPushNotificationService().sendNotification(topic, jsonMessage, serviceName);
		}
	}
		
		

}
