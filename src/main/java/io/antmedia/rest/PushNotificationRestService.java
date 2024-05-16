package io.antmedia.rest;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import io.antmedia.AppSettings;
import io.antmedia.filter.JWTFilter;
import io.antmedia.pushnotification.IPushNotificationService;
import io.antmedia.rest.model.PushNotificationToSubscribers;
import io.antmedia.rest.model.Result;
import io.antmedia.websocket.WebSocketConstants;
import io.grpc.internal.JsonParser;
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
		if (StringUtils.isNotBlank(subscriberId)) {
			long expireTimeMs = System.currentTimeMillis() + (timeoutDurationInSeconds * 1000);
			String jwtToken = JWTFilter.generateJwtToken(getAppSettings().getSubscriberAuthenticationKey(), expireTimeMs, WebSocketConstants.SUBSCRIBER_ID, subscriberId);

			return new Result(true, jwtToken, "Token is available in dataId field");
		}
		else {
			return new Result(false, "subscriberId is blank. Please give subscriberId as query parameter");

		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/subscribers")
	public Result sendPushNotification(PushNotificationToSubscribers pushNotificationToSubcribers, @QueryParam("serviceName") String serviceName) {
		JSONParser parser = new JSONParser();
		try {
			if (StringUtils.isBlank(serviceName)) 
			{
				return getPushNotificationService().sendNotification(pushNotificationToSubcribers.getSubscribers() , (JSONObject)parser.parse(pushNotificationToSubcribers.getJsonMessage()));
			}
			else 
			{
				return getPushNotificationService().sendNotification(pushNotificationToSubcribers.getSubscribers(), (JSONObject)parser.parse(pushNotificationToSubcribers.getJsonMessage()), serviceName);
			}
		} catch (ParseException e) {
			return new Result(false, "JSON content cannot be parsed. Make sure JSON content is in correct format");
		}
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/topics/{topic}")
	public Result sendPushNotification(@PathParam("topic") String topic, String jsonMessage, @QueryParam("serviceName") String serviceName) {
		JSONParser parser = new JSONParser();
		try {
			if (StringUtils.isBlank(serviceName)) {
				return getPushNotificationService().sendNotification(topic,  (JSONObject)parser.parse(jsonMessage));

			}
			else {
				return getPushNotificationService().sendNotification(topic,  (JSONObject)parser.parse(jsonMessage), serviceName);
			}
		} catch (ParseException e) {
			return new Result(false, "JSON content cannot be parsed");
		}
	}


	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}



}
