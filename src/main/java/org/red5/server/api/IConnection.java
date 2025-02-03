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

package org.red5.server.api;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;

/**
 * The connection object.
 * 
 * Each connection has an associated client and scope. Connections may be persistent, polling, or transient. The aim of this interface is to provide basic connection methods shared between different types of connections.
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public interface IConnection extends ICoreObject, ICastingAttributeStore {

    /**
     * Encoding type.
     */
    public static enum Encoding {
        AMF0, AMF3, WEBSOCKET, SOCKETIO, RTP, SRTP, RAW
    };

    /**
     * Persistent connection type, eg RTMP.
     */
    public static final String PERSISTENT = "persistent";

    /**
     * Polling connection type, eg RTMPT.
     */
    public static final String POLLING = "polling";

    /**
     * Transient connection type, eg Remoting, HTTP, etc.
     */
    public static final String TRANSIENT = "transient";

    /**
     * Get the connection type.
     * 
     * @return string containing one of connection types
     */
    public String getType(); // PERSISTENT | POLLING | TRANSIENT

    /**
     * Get the object encoding in use for this connection.
     * 
     * @return encoding type
     */
    public Encoding getEncoding();

    /**
     * Initialize the connection.
     * 
     * @param client Client object associated with connection
     */
    public void initialize(IClient client);

    /**
     * Try to connect to the scope.
     * 
     * @return true on success, false otherwise
     * @param scope Scope object
     */
    public boolean connect(IScope scope);

    /**
     * Try to connect to the scope with a list of connection parameters.
     * 
     * @param params Connections parameters
     * @return true on success, false otherwise
     * @param scope Scope object
     */
    public boolean connect(IScope scope, Object[] params);

    /**
     * Is the client connected to the scope. Result depends on connection type, true for persistent and polling connections,
     * false for transient.
     * 
     * @return true if the connection is persistent or polling, otherwise false
     */
    public boolean isConnected();

    /**
     * Close this connection. This will disconnect the client from the associated scope.
     */
    public void close();

    /**
     * Return the parameters that were given in the call to "connect".
     * 
     * @return Connection parameters passed from client-side (Flex/Flash application)
     */
    public Map<String, Object> getConnectParams();

    /**
     * Sets the Client.
     * 
     * @param client client
     */
    public void setClient(IClient client);

    /**
     * Get the client object associated with this connection.
     * 
     * @return Client object
     */
    public IClient getClient();

    /**
     * Get the hostname that the client is connected to. If they are connected to an IP, the IP address will be returned as a String.
     * 
     * @return String containing the hostname
     */
    public String getHost();

    /**
     * Get the IP address the client is connected from.
     * 
     * @return The IP address of the client
     */
    public String getRemoteAddress();

    /**
     * Get the IP addresses the client is connected from. If a client is connected through RTMPT and uses a proxy to connect, this will contain all hosts the client used to connect to the server.
     * 
     * @return The IP addresses of the client
     */
    public List<String> getRemoteAddresses();

    /**
     * Get the port the client is connected from.
     * 
     * @return The port of the client
     */
    public int getRemotePort();

    /**
     * Get the path for this connection. This is not updated if you switch scope.
     * 
     * @return path Connection path
     */
    public String getPath();

    /**
     * Get the session id, this may be null.
     * 
     * @return Session id
     */
    public String getSessionId();

    /**
     * Total number of bytes read from the connection.
     * 
     * @return Number of read bytes
     */
    public long getReadBytes();

    /**
     * Total number of bytes written to the connection.
     * 
     * @return Number of written bytes
     */
    public long getWrittenBytes();

    /**
     * Total number of messages read from the connection.
     * 
     * @return Number of read messages
     */
    public long getReadMessages();

    /**
     * Total number of messages written to the connection.
     * 
     * @return Number of written messages
     */
    public long getWrittenMessages();

    /**
     * Total number of messages that have been dropped.
     * 
     * @return Number of dropped messages
     */
    public long getDroppedMessages();

    /**
     * Total number of messages that are pending to be sent to the connection.
     * 
     * @return Number of pending messages
     */
    public long getPendingMessages();

    /**
     * Return number of written bytes the client reports to have received. This is the last value of the BytesRead message received from a client.
     * 
     * @see org.red5.server.net.rtmp.event.BytesRead
     * @return number of written bytes received by the client
     */
    public long getClientBytesRead();

    /**
     * Start measuring the round-trip time for a packet on the connection.
     */
    public void ping();

    /**
     * Return round-trip time of last ping command.
     * 
     * @return round-trip time in milliseconds
     */
    public int getLastPingTime();

    /**
     * Get the scope this is connected to.
     * 
     * @return The connected scope
     */
    public IScope getScope();

    /**
     * Get the basic scopes this connection has subscribed. This list will contain the shared objects and broadcast streams the connection connected to.
     * 
     * @return List of basic scopes
     */
    public Iterator<IBasicScope> getBasicScopes();

    /**
     * Sets the bandwidth using a mbit/s value.
     * 
     * @param mbits target
     */
    public void setBandwidth(int mbits);

    /**
     * Adds a listener to this object
     * 
     * @param listener connection listener
     */
    public void addListener(IConnectionListener listener);

    /**
     * Removes the listener from this object
     * 
     * @param listener connection listener
     */
    public void removeListener(IConnectionListener listener);

    /**
     * Returns the current stream id.
     * 
     * @return stream id
     */
    public Number getStreamId();

    /**
     * Sets the current stream id.
     * 
     * @param id stream id
     */
    public void setStreamId(Number id);

    /**
     * Returns the protocol type for this connection. eg. rtmp, rtmpt, http
     * 
     * @return protocol type
     */
    public String getProtocol();

}
