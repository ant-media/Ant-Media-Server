package io.antmedia.test.plugin;

import io.antmedia.plugin.api.Inject;
import io.antmedia.plugin.api.Rest;
import io.antmedia.plugin.api.Scope;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Minimal V2 @Rest class used in unit tests.
 */
@Rest(scope = Scope.APPLICATION)
@Path("/test-v2-plugin")
public class MinimalPluginRest {

	@Inject
	private MinimalAmsPlugin plugin;

	@GET
	public String ping() {
		return "pong";
	}

	public MinimalAmsPlugin getPlugin() { return plugin; }
}
