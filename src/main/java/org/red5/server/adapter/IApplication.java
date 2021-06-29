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

package org.red5.server.adapter;

import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;

/**
 * IApplication provides lifecycle methods that most communication applications will use. This interface defines the methods that are called by Red5 through an applications life. It is suggested that you NOT implement this interface yourself, but instead you should subclass <tt>org.red5.server.adapter.MultiThreadedApplicationAdapter</tt> or <tt>org.red5.server.adapter.ApplicationAdapter</tt>.
 * 
 * @author Dominick Accattato
 */
public interface IApplication {

    /**
     * Called once when application or room starts
     * 
     * @param app
     *            Application or room level scope. See {@link org.red5.server.api.scope.IScope} for details
     * @return <pre>
     * true
     * </pre>
     * 
     *         continues application run,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         terminates
     */
    public boolean appStart(IScope app);

    /**
     * Called per each client connect
     * 
     * @param conn
     *            Connection object used to provide basic connection methods. See {@link org.red5.server.api.IConnection}
     * @param params
     *            List of params sent from client with NetConnection.connect call
     * @return <pre>
     * true
     * </pre>
     * 
     *         accepts the connection,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         rejects it
     */
    public boolean appConnect(IConnection conn, Object[] params);

    /**
     * Called every time client joins app level scope
     * 
     * @param client
     *            Client object
     * @param app
     *            Scope object
     * @return <pre>
     * true
     * </pre>
     * 
     *         accepts the client,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         rejects it
     */
    public boolean appJoin(IClient client, IScope app);

    /**
     * Called every time client disconnects from the application
     * 
     * @param conn
     *            Connection object See {@link org.red5.server.api.IConnection}
     */
    public void appDisconnect(IConnection conn);

    /**
     * Called every time client leaves the application scope
     * 
     * @param client
     *            Client object
     * @param app
     *            Scope object
     */
    public void appLeave(IClient client, IScope app);

    /**
     * Called on application stop
     * 
     * @param app
     *            Scope object
     */
    public void appStop(IScope app);

    /**
     * Called on application room start
     * 
     * @param room
     *            Scope object
     * @return <pre>
     * true
     * </pre>
     * 
     *         if scope can be started,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         otherwise
     */
    public boolean roomStart(IScope room);

    /**
     * Called every time client connects to the room
     * 
     * @param conn
     *            Connection object
     * @param params
     *            List of params sent from client with NetConnection.connect call
     * @return <pre>
     * true
     * </pre>
     * 
     *         accepts the connection,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         rejects it
     */
    public boolean roomConnect(IConnection conn, Object[] params);

    /**
     * Called when user joins room scope
     * 
     * @param client
     *            Client object
     * @param room
     *            Scope object
     * @return <pre>
     * true
     * </pre>
     * 
     *         accepts the client,
     * 
     *         <pre>
     * false
     * </pre>
     * 
     *         rejects it
     */
    public boolean roomJoin(IClient client, IScope room);

    /**
     * Called when client disconnects from room scope
     * 
     * @param conn
     *            Connection object used to provide basic connection methods. See {@link org.red5.server.api.IConnection}
     */
    public void roomDisconnect(IConnection conn);

    /**
     * Called when user leaves room scope
     * 
     * @param client
     *            Client object
     * @param room
     *            Scope object
     */
    public void roomLeave(IClient client, IScope room);

    /**
     * Called on room scope stop
     * 
     * @param room
     *            Scope object
     */
    public void roomStop(IScope room);

}
