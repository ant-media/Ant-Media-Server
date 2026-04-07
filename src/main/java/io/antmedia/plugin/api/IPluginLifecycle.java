package io.antmedia.plugin.api;

/**
 * Optional per-application lifecycle hook for V2 plugins.
 *
 * <p>{@link IServerListener#onPluginEnabled(String)} fires once at the server level when a
 * plugin is loaded. {@code IPluginLifecycle} fires <em>per active streaming application</em>:
 * the deployer iterates over every webapp context (LiveApp, WebRTCAppEE, ...) and calls
 * {@link #onActivated(String)} for each, then symmetrically calls {@link #onDeactivated(String)}
 * during unload.</p>
 *
 * <p>This is the right place for plugins to do per-app setup that depends on the context path:</p>
 * <ul>
 *   <li>Registering handlers in {@code PluginServletDispatcher} keyed by context path</li>
 *   <li>Starting per-app schedulers, watchers, or background tasks</li>
 *   <li>Recording cluster valves or other per-app resources discovered from the Spring context</li>
 * </ul>
 *
 * <p>Implementations should be idempotent — {@code onActivated} may be called once per app at
 * load time and again at server restart. {@code onDeactivated} is called for every previously
 * activated context during unload, but the deployer does not guarantee strict ordering between
 * {@code onPluginDisabled} (server-level) and {@code onDeactivated} (per-app); plugins must
 * handle both being called and not assume one fires before the other.</p>
 */
public interface IPluginLifecycle {

    /**
     * Called once per active streaming application context after the plugin is instantiated
     * and its {@code @Inject} fields are resolved, but before the plugin handles any traffic.
     * The {@code contextPath} matches Tomcat's context path for the app (e.g. {@code "/LiveApp"}).
     */
    default void onActivated(String contextPath) { }

    /**
     * Called once per previously activated application context when the plugin is being
     * unloaded, before {@code IServerListener.onPluginDisabled} fires. Plugins should release
     * per-app resources here (unregister servlet handlers, cancel timers, close per-app
     * sockets) so that the server-level disable hook can do final cleanup.
     */
    default void onDeactivated(String contextPath) { }
}
