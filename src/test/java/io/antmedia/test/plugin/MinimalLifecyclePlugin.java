package io.antmedia.test.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.antmedia.plugin.api.AmsPlugin;
import io.antmedia.plugin.api.IPluginLifecycle;
import io.antmedia.plugin.api.Listener;
import io.antmedia.plugin.api.Scope;

/**
 * Minimal V2 plugin used by PluginDeployerTest to verify the {@link IPluginLifecycle}
 * activation/deactivation hooks fire per active webapp context.
 *
 * <p>Records the order in which {@code onActivated} and {@code onDeactivated} are called
 * across all instances of this class so the test can assert symmetry. The records are
 * static because each per-app context creates its own instance — the test needs a single
 * vantage point to observe all of them.</p>
 */
@AmsPlugin
@Listener(scope = Scope.APPLICATION)
public class MinimalLifecyclePlugin implements IPluginLifecycle {

    private static final List<String> ACTIVATIONS = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> DEACTIVATIONS = Collections.synchronizedList(new ArrayList<>());

    public static void resetCalls() {
        ACTIVATIONS.clear();
        DEACTIVATIONS.clear();
    }

    public static List<String> activations() {
        return new ArrayList<>(ACTIVATIONS);
    }

    public static List<String> deactivations() {
        return new ArrayList<>(DEACTIVATIONS);
    }

    @Override
    public void onActivated(String contextPath) {
        ACTIVATIONS.add(contextPath);
    }

    @Override
    public void onDeactivated(String contextPath) {
        DEACTIVATIONS.add(contextPath);
    }
}
