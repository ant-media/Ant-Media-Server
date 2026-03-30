package org.red5.server.plugin;

import org.red5.server.Server;
import org.red5.server.api.plugin.IRed5Plugin;
import org.springframework.context.ApplicationContext;

/**
 * Minimal IRed5Plugin implementation used only in unit tests (e.g. to
 * simulate a startup-loaded plugin in PluginRegistry for duplicate-check tests).
 */
public class MinimalTestPlugin implements IRed5Plugin {

    public static String PLUGIN_NAME = "MinimalTestPlugin";

    public static MinimalTestPlugin getInstance() {
        return new MinimalTestPlugin();
    }

    @Override
    public String getName() {
        return PLUGIN_NAME;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) {
        // no-op
    }

    @Override
    public void setServer(Server server) {
        // no-op
    }

    @Override
    public void doStart() throws Exception {
        // no-op
    }

    @Override
    public void doStop() throws Exception {
        // no-op
    }
}
