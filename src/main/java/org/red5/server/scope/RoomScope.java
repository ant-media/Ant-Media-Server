package org.red5.server.scope;

import org.red5.server.api.scope.ScopeType;

/**
 * Represents a subscope to other scopes.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RoomScope extends Scope {

    {
        type = ScopeType.ROOM;
    }

}
