package org.red5.server.scope;

import org.red5.server.api.listeners.IScopeListener;
import org.red5.server.api.scope.IScope;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Read-only dynamically updated list of current list of scopes.
 * This is used in cases when we need to iterate over the list of current scopes.
 */
public class ScopeList implements IScopeListener {

    private final CopyOnWriteArrayList<IScope> scopes = new CopyOnWriteArrayList<>();


    @Override
    public void notifyScopeCreated(IScope scope) {
        scopes.add(scope);
    }

    @Override
    public void notifyScopeRemoved(IScope scope) {
        scopes.remove(scope);
    }

    /**
     * Returns an immutable view of the current scopes, safe to iterate over it
     * @return immutable list of scopes
     */
    public List<IScope> getScopes() {
        return Collections.unmodifiableList(scopes);
    }
}
