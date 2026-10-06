package io.antmedia.plugin.api;

public enum PluginState {
    INSTALLING,
    INSTALLED_PENDING_RESTART,
    ACTIVE,
    FAILED,
    /**
     * Files removed, but the plugin's classes and any threads it started are still resident.
     * Cleared at the next boot scan, which finds no jar and so rebuilds no record.
     */
    UNINSTALLED_PENDING_RESTART
}
