/*
 * RED5 Open Source Media Server - https://github.com/Red5/
 * 
 * Copyright 2006-2016 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.server.api.service;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.net.rtmp.event.ClientInvokeEvent;
import org.red5.server.net.rtmp.event.ClientNotifyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility functions to invoke methods on connections.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ServiceUtils {

    private static final Logger log = LoggerFactory.getLogger(ServiceUtils.class);

    /**
     * Invoke a method on the current connection.
     * 
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the connection supports method calls, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public static boolean invokeOnConnection(String method, Object[] params) {
        return invokeOnConnection(method, params, null);
    }

    /**
     * Invoke a method on the current connection and handle result.
     * 
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @param callback
     *            object to notify when result is received
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the connection supports method calls, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public static boolean invokeOnConnection(String method, Object[] params, IPendingServiceCallback callback) {
        IConnection conn = Red5.getConnectionLocal();
        if (conn != null) {
            log.debug("Connection for invoke: {}", conn);
            return invokeOnConnection(conn, method, params, callback);
        } else {
            log.warn("Connection was null (thread local), cannot execute invoke request");
            return false;
        }
    }

    /**
     * Invoke a method on a given connection.
     * 
     * @param conn
     *            connection to invoke method on
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the connection supports method calls, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public static boolean invokeOnConnection(IConnection conn, String method, Object[] params) {
        return invokeOnConnection(conn, method, params, null);
    }

    /**
     * Invoke a method on a given connection and handle result.
     * 
     * @param conn
     *            connection to invoke method on
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @param callback
     *            object to notify when result is received
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the connection supports method calls, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public static boolean invokeOnConnection(IConnection conn, String method, Object[] params, IPendingServiceCallback callback) {
        if (conn instanceof IServiceCapableConnection) {
            if (callback == null) {
                ((IServiceCapableConnection) conn).invoke(method, params);
            } else {
                ((IServiceCapableConnection) conn).invoke(method, params, callback);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Invoke a method on all connections to the current scope.
     * 
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     */
    public static void invokeOnAllConnections(String method, Object[] params) {
        invokeOnAllConnections(method, params, null);
    }

    /**
     * Invoke a method on all connections to the current scope and handle result.
     * 
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @param callback
     *            object to notify when result is received
     */
    public static void invokeOnAllConnections(String method, Object[] params, IPendingServiceCallback callback) {
        IConnection conn = Red5.getConnectionLocal();
        if (conn != null) {
            log.debug("Connection for invoke on all: {}", conn);
            IScope scope = conn.getScope();
            log.debug("Scope for invoke on all: {}", scope);
            invokeOnAllScopeConnections(scope, method, params, callback);
        } else {
            log.warn("Connection was null (thread local), scope cannot be located and cannot execute invoke request");
        }
    }

    /**
     * Invoke a method on all connections to a given scope.
     * 
     * @param scope
     *            scope to get connections for
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @deprecated Use {@link ServiceUtils#invokeOnAllScopeConnections(IScope, String, Object[], IPendingServiceCallback)} instead
     */
    @Deprecated
    public static void invokeOnAllConnections(IScope scope, String method, Object[] params) {
        invokeOnAllScopeConnections(scope, method, params, null);
    }

    /**
     * Invoke a method on all connections to a given scope and handle result.
     * 
     * @param scope
     *            scope to get connections for
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @param callback
     *            object to notify when result is received
     */
    public static void invokeOnAllConnections(IScope scope, String method, Object[] params, IPendingServiceCallback callback) {
        invokeOnClient(null, scope, method, params, callback);
    }

    /**
     * Invoke a method on all connections of a scope and handle result.
     * 
     * @param scope
     *            scope to get connections from
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @param callback
     *            object to notify when result is received
     */
    public static void invokeOnAllScopeConnections(IScope scope, String method, Object[] params, IPendingServiceCallback callback) {
        ClientInvokeEvent event = ClientInvokeEvent.build(method, params, callback);
        scope.dispatchEvent(event);
    }

    /**
     * Invoke a method on all connections of a client to a given scope.
     * 
     * @param client
     *            client to get connections for
     * @param scope
     *            scope to get connections of the client from
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     */
    public static void invokeOnClient(IClient client, IScope scope, String method, Object[] params) {
        invokeOnClient(client, scope, method, params, null);
    }

    /**
     * Invoke a method on all connections of a client to a given scope and handle result.
     * 
     * @param client
     *            client to get connections for
     * @param scope
     *            scope to get connections of the client from
     * @param method
     *            name of the method to invoke
     * @param params
     *            parameters to pass to the method
     * @param callback
     *            object to notify when result is received
     * @deprecated Use {@link ServiceUtils#invokeOnAllScopeConnections(IScope, String, Object[], IPendingServiceCallback)} instead
     */
    @Deprecated
    public static void invokeOnClient(IClient client, IScope scope, String method, Object[] params, IPendingServiceCallback callback) {
        if (client == null) {
            invokeOnAllScopeConnections(scope, method, params, callback);
        } else {
            IConnection conn = scope.lookupConnection(client);
            if (conn != null) {
                if (callback == null) {
                    invokeOnConnection(conn, method, params);
                } else {
                    invokeOnConnection(conn, method, params, callback);
                }
            }
        }
    }

    /**
     * Notify a method on the current connection.
     * 
     * @param method
     *            name of the method to notify
     * @param params
     *            parameters to pass to the method
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the connection supports method calls, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public static boolean notifyOnConnection(String method, Object[] params) {
        IConnection conn = Red5.getConnectionLocal();
        if (conn != null) {
            log.debug("Connection for notify: {}", conn);
            return notifyOnConnection(conn, method, params);
        } else {
            log.warn("Connection was null (thread local), cannot execute notify request");
            return false;
        }
    }

    /**
     * Notify a method on a given connection.
     * 
     * @param conn
     *            connection to notify method on
     * @param method
     *            name of the method to notify
     * @param params
     *            parameters to pass to the method
     * @return <pre>
     * true
     * </pre>
     * 
     *         if the connection supports method calls, otherwise
     * 
     *         <pre>
     * false
     * </pre>
     */
    public static boolean notifyOnConnection(IConnection conn, String method, Object[] params) {
        if (conn instanceof IServiceCapableConnection) {
            ((IServiceCapableConnection) conn).notify(method, params);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Notify a method on all connections to the current scope.
     * 
     * @param method
     *            name of the method to notify
     * @param params
     *            parameters to pass to the method
     */
    public static void notifyOnAllConnections(String method, Object[] params) {
        IConnection conn = Red5.getConnectionLocal();
        if (conn != null) {
            log.debug("Connection for notify on all: {}", conn);
            IScope scope = conn.getScope();
            log.debug("Scope for notify on all: {}", scope);
            notifyOnAllScopeConnections(scope, method, params);
        } else {
            log.warn("Connection was null (thread local), scope cannot be located and cannot execute notify request");
        }
    }

    /**
     * Notify a method on all connections to a given scope.
     * 
     * @param scope
     *            scope to get connections for
     * @param method
     *            name of the method to notify
     * @param params
     *            parameters to pass to the method
     * @deprecated Use {@link ServiceUtils#notifyOnAllScopeConnections(IScope, String, Object[])} instead
     */
    @Deprecated
    public static void notifyOnAllConnections(IScope scope, String method, Object[] params) {
        notifyOnAllScopeConnections(scope, method, params);
    }

    /**
     * Notify a method on all connections of a scope.
     * 
     * @param scope
     *            scope to dispatch event
     * @param method
     *            name of the method to notify
     * @param params
     *            parameters to pass to the method
     */
    public static void notifyOnAllScopeConnections(IScope scope, String method, Object[] params) {
        ClientNotifyEvent event = ClientNotifyEvent.build(method, params);
        scope.dispatchEvent(event);
    }

    /**
     * Notify a method on all connections of a client to a given scope.
     * 
     * @param client
     *            client to get connections for
     * @param scope
     *            scope to get connections of the client from
     * @param method
     *            name of the method to notify
     * @param params
     *            parameters to pass to the method
     * @deprecated Use {@link ServiceUtils#notifyOnAllScopeConnections(IScope, String, Object[])} instead
     */
    @Deprecated
    public static void notifyOnClient(IClient client, IScope scope, String method, Object[] params) {
        if (client == null) {
            notifyOnAllScopeConnections(scope, method, params);
        } else {
            IConnection conn = scope.lookupConnection(client);
            if (conn != null) {
                notifyOnConnection(conn, method, params);
            }
        }
    }

}
