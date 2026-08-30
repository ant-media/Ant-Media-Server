package io.antmedia.rest;

import org.red5.server.api.scope.IGlobalScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import io.antmedia.AntMediaApplicationAdapter;
import io.antmedia.console.AdminApplication;
import io.antmedia.console.datastore.AbstractConsoleDataStore;
import io.antmedia.datastore.db.types.User;
import io.antmedia.statistic.StatsCollector;
import jakarta.servlet.ServletContext;

/**
 * Looks up a v3 JWT subject in the console user store for {@link JWTFilterV3} (algorithm step 3).
 * Users live in the root (console) webapp, so this walks up from the app scope to the global scope
 * and into the root webapp's console datastore. Returns null when the user is absent or the store
 * can't be reached, so the filter fails closed.
 */
public class ConsoleUserResolver {

	private static final Logger logger = LoggerFactory.getLogger(ConsoleUserResolver.class);

	/** Scope name of the console/management webapp. */
	private static final String ROOT_WEBAPP = "root";

	private final ServletContext servletContext;

	public ConsoleUserResolver(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	/** Returns the user for the JWT sub, or null if absent / store unreachable. */
	public User getUser(String userId) {
		AbstractConsoleDataStore store = getConsoleDataStore();
		return store != null ? store.getUser(userId) : null;
	}

	private AbstractConsoleDataStore getConsoleDataStore() {
		try {
			WebApplicationContext appContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
			if (appContext == null || !appContext.containsBean(AntMediaApplicationAdapter.BEAN_NAME)) {
				return null;
			}

			AntMediaApplicationAdapter app = (AntMediaApplicationAdapter) appContext.getBean(AntMediaApplicationAdapter.BEAN_NAME);
			IScope appScope = app.getScope();
			if (appScope == null) {
				return null;
			}

			IScope rootScope = ScopeUtils.findRoot(appScope);
			if (!(rootScope instanceof IGlobalScope)) {
				return null;
			}

			IScope consoleScope = ((IGlobalScope) rootScope).getScope(ROOT_WEBAPP);
			if (consoleScope == null || consoleScope.getContext() == null) {
				return null;
			}

			ApplicationContext consoleContext = consoleScope.getContext().getApplicationContext();
			if (consoleContext == null) {
				return null;
			}

			AdminApplication admin = StatsCollector.getAdminAppAdaptor(consoleContext);
			if (admin == null || admin.getDataStoreFactory() == null) {
				return null;
			}

			return admin.getDataStoreFactory().getDataStore();
		}
		catch (Exception e) {
			logger.warn("Could not reach the console user store for v3 user verification: {}", e.getMessage());
			return null;
		}
	}
}
