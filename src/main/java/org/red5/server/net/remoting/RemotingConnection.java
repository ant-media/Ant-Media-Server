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

package org.red5.server.net.remoting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.red5.server.Client;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.remoting.IRemotingConnection;
import org.red5.server.api.remoting.IRemotingHeader;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.net.remoting.message.RemotingPacket;
import org.red5.server.net.servlet.ServletUtils;

/**
 * Connection class so the Red5 object works in methods invoked through remoting. Attributes are stored in the session of the implementing servlet container.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class RemotingConnection implements IRemotingConnection {

    /**
     * Session attribute holding an IClient object for this connection.
     */
    private final static String CLIENT = "red5.client";

    /**
     * Scope
     */
    protected IScope scope;

    /**
     * Servlet request
     */
    protected HttpServletRequest request;

    /**
     * Remoting packet that triggered the connection.
     */
    protected RemotingPacket packet;

    /**
     * Session used to store properties.
     */
    protected HttpSession session;

    /**
     * Headers to be returned to the client.
     */
    protected List<IRemotingHeader> headers = new ArrayList<IRemotingHeader>();

    /**
     * Listeners
     */
    protected CopyOnWriteArrayList<IConnectionListener> connectionListeners = new CopyOnWriteArrayList<IConnectionListener>();

    /**
     * Create servlet connection from request and scope.
     *
     * @param request
     *            Servlet request
     * @param scope
     *            Scope
     * @param packet
     *            packet
     */
    public RemotingConnection(HttpServletRequest request, IScope scope, RemotingPacket packet) {
        this.request = request;
        this.scope = scope;
        this.packet = packet;
        this.session = request.getSession();
        RemotingClient client = (RemotingClient) session.getAttribute(CLIENT);
        if (client == null) {
            client = new RemotingClient(session.getId());
            session.setAttribute(CLIENT, client);
        }
        client.register(this);
    }

    /**
     * Return string representation of the connection.
     * 
     * @return string
     */
    public String toString() {
        return getClass().getSimpleName() + " from " + getRemoteAddress() + ':' + getRemotePort() + " to " + getHost() + " (session: " + session.getId() + ')';
    }

    /**
     * Update the current packet.
     * 
     * @param packet
     *            remoting packet
     */
    protected void setPacket(RemotingPacket packet) {
        this.packet = packet;
    }

    /**
     * Throws Not supported runtime exception
     */
    private void notSupported() {
        throw new RuntimeException("Not supported for this type of connection");
    }

    /**
     * Return encoding (AMF0 or AMF3).
     *
     * @return Encoding, currently AMF0
     */
    public Encoding getEncoding() {
        return packet.getEncoding();
    }

    /** {@inheritDoc} */
    public String getType() {
        return IConnection.TRANSIENT;
    }

    /** {@inheritDoc} */
    public void initialize(IClient client) {
        notSupported();
    }

    /** {@inheritDoc} */
    public boolean connect(IScope scope) {
        notSupported();
        return false;
    }

    /** {@inheritDoc} */
    public boolean connect(IScope scope, Object[] params) {
        notSupported();
        return false;
    }

    /** {@inheritDoc} */
    public boolean isConnected() {
        return false;
    }

    /** {@inheritDoc} */
    public void close() {
        session.invalidate();
        // set a quick local ref
        final IConnection con = this;
        String threadId = String.format("RemotingCloseNotifier-%s", con.getClient().getId());
        // alert our listeners
        Thread t = new Thread(new Runnable() {
            public void run() {
                for (IConnectionListener listener : connectionListeners) {
                    listener.notifyDisconnected(con);
                }
                connectionListeners.clear();
            }
        }, threadId);
        t.setDaemon(true);
        t.start();
    }

    /** {@inheritDoc} */
    public Map<String, Object> getConnectParams() {
        return packet.getHeaders();
    }

    /** {@inheritDoc} */
    public void setClient(IClient client) {
        session.setAttribute(CLIENT, client);
    }

    /** {@inheritDoc} */
    public IClient getClient() {
        return (IClient) session.getAttribute(CLIENT);
    }

    /** {@inheritDoc} */
    public String getHost() {
        return request.getLocalName();
    }

    /** {@inheritDoc} */
    public String getRemoteAddress() {
        return request.getRemoteAddr();
    }

    /** {@inheritDoc} */
    public List<String> getRemoteAddresses() {
        return ServletUtils.getRemoteAddresses(request);
    }

    /** {@inheritDoc} */
    public int getRemotePort() {
        return request.getRemotePort();
    }

    /** {@inheritDoc} */
    public String getPath() {
        String path = request.getContextPath();
        if (request.getPathInfo() != null) {
            path += request.getPathInfo();
        }
        if (path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }

    /** {@inheritDoc} */
    public String getSessionId() {
        return null;
    }

    /** {@inheritDoc} */
    public long getReadBytes() {
        return request.getContentLength();
    }

    /** {@inheritDoc} */
    public long getWrittenBytes() {
        return 0;
    }

    /** {@inheritDoc} */
    public long getPendingMessages() {
        return 0;
    }

    /**
     * Return pending video messages number.
     *
     * @return Pending video messages number
     */
    public long getPendingVideoMessages() {
        return 0;
    }

    /** {@inheritDoc} */
    public long getReadMessages() {
        return 1;
    }

    /** {@inheritDoc} */
    public long getWrittenMessages() {
        return 0;
    }

    /** {@inheritDoc} */
    public long getDroppedMessages() {
        return 0;
    }

    /** {@inheritDoc} */
    public void ping() {
        notSupported();
    }

    /** {@inheritDoc} */
    public int getLastPingTime() {
        return -1;
    }

    /** {@inheritDoc} */
    public IScope getScope() {
        return scope;
    }

    /** {@inheritDoc} */
    public Iterator<IBasicScope> getBasicScopes() {
        notSupported();
        return null;
    }

    public void dispatchEvent(Object event) {
        notSupported();
    }

    /** {@inheritDoc} */
    public void dispatchEvent(IEvent event) {
        notSupported();
    }

    /** {@inheritDoc} */
    public boolean handleEvent(IEvent event) {
        notSupported();
        return false;
    }

    /** {@inheritDoc} */
    public void notifyEvent(IEvent event) {
        notSupported();
    }

    /** {@inheritDoc} */
    public Boolean getBoolAttribute(String name) {
        return (Boolean) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Byte getByteAttribute(String name) {
        return (Byte) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Double getDoubleAttribute(String name) {
        return (Double) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Integer getIntAttribute(String name) {
        return (Integer) getAttribute(name);
    }

    /** {@inheritDoc} */
    public List<?> getListAttribute(String name) {
        return (List<?>) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Long getLongAttribute(String name) {
        return (Long) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Map<?, ?> getMapAttribute(String name) {
        return (Map<?, ?>) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Set<?> getSetAttribute(String name) {
        return (Set<?>) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Short getShortAttribute(String name) {
        return (Short) getAttribute(name);
    }

    /** {@inheritDoc} */
    public String getStringAttribute(String name) {
        return (String) getAttribute(name);
    }

    /** {@inheritDoc} */
    public Object getAttribute(String name) {
        if (name == null) {
            return null;
        }

        return session.getAttribute(name);
    }

    /** {@inheritDoc} */
    public Object getAttribute(String name, Object defaultValue) {
        if (name == null) {
            return null;
        }
        // Synchronize so default value doesn't override other default value 
        synchronized (session) {
            Object result = session.getAttribute(name);
            if (result == null && defaultValue != null) {
                session.setAttribute(name, defaultValue);
                result = defaultValue;
            }
            return result;
        }
    }

    /** {@inheritDoc} */
    public Set<String> getAttributeNames() {
        final Set<String> result = new HashSet<String>();
        // Synchronize to prevent parallel modifications
        synchronized (session) {
            final Enumeration<String> names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                result.add(names.nextElement());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /** {@inheritDoc} */
    public Map<String, Object> getAttributes() {
        final Map<String, Object> result = new HashMap<String, Object>();
        // Synchronize to prevent parallel modifications
        synchronized (session) {
            final Enumeration<String> names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                final String name = names.nextElement();
                result.put(name, session.getAttribute(name));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /** {@inheritDoc} */
    public boolean hasAttribute(String name) {
        if (name == null) {
            return false;
        }
        return (getAttribute(name) != null);
    }

    /** {@inheritDoc} */
    public boolean removeAttribute(String name) {
        if (name == null) {
            return false;
        }
        // synchronize to prevent parallel modifications
        synchronized (session) {
            if (!hasAttribute(name)) {
                return false;
            }
            session.removeAttribute(name);
        }
        return true;
    }

    /** {@inheritDoc} */
    public void removeAttributes() {
        // synchronize to prevent parallel modifications
        synchronized (session) {
            final Enumeration<String> names = session.getAttributeNames();
            while (names.hasMoreElements()) {
                session.removeAttribute(names.nextElement());
            }
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("deprecation")
    public int size() {
        return session != null ? session.getValueNames().length : 0;
    }

    /** {@inheritDoc} */
    public boolean setAttribute(String name, Object value) {
        if (name == null) {
            return false;
        }
        if (value == null) {
            session.removeAttribute(name);
        } else {
            session.setAttribute(name, value);
        }
        return true;
    }

    /** {@inheritDoc} */
    public boolean setAttributes(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            final String name = entry.getKey();
            final Object value = entry.getValue();
            if (name != null && value != null) {
                session.setAttribute(name, value);
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    public boolean setAttributes(IAttributeStore values) {
        return setAttributes(values.getAttributes());
    }

    /** {@inheritDoc} */
    public void setBandwidth(int mbits) {
        throw new UnsupportedOperationException("Not supported in this class");
    }

    /** {@inheritDoc} */
    public void addHeader(String name, Object value) {
        addHeader(name, value, false);
    }

    /** {@inheritDoc} */
    public void addHeader(String name, Object value, boolean mustUnderstand) {
        synchronized (headers) {
            headers.add(new RemotingHeader(name, mustUnderstand, value));
        }
    }

    /** {@inheritDoc} */
    public void removeHeader(String name) {
        addHeader(name, null, false);
    }

    /** {@inheritDoc} */
    public Collection<IRemotingHeader> getHeaders() {
        return headers;
    }

    /** {@inheritDoc} */
    public long getClientBytesRead() {
        // This is not supported for Remoting connections
        return 0;
    }

    /**
     * Cleans up the remoting connection client from the HttpSession and client registry. This should also fix APPSERVER-328
     */
    public void cleanup() {
        if (session != null) {
            RemotingClient rc = (RemotingClient) session.getAttribute(CLIENT);
            session.removeAttribute(CLIENT);
            if (rc != null) {
                rc.unregister(this);
            }
        }
    }

    /** Internal class for clients connected through Remoting. */
    private static class RemotingClient extends Client {

        private RemotingClient(String id) {
            super(id, null);
        }

        /** {@inheritDoc} */
        @Override
        protected void register(IConnection conn) {
            // We only have one connection per client
            for (IConnection c : getConnections()) {
                unregister(c);
            }
            super.register(conn);
        }

        /** {@inheritDoc} */
        @Override
        protected void unregister(IConnection conn) {
            super.unregister(conn);
        }

    }

    /** {@inheritDoc} */
    public void addListener(IConnectionListener listener) {
        this.connectionListeners.add(listener);
    }

    /** {@inheritDoc} */
    public void removeListener(IConnectionListener listener) {
        this.connectionListeners.remove(listener);
    }

    public Number getStreamId() {
        notSupported();
        return -1;
    }

    public void setStreamId(Number id) {
        notSupported();
    }

    @Override
    public String getProtocol() {
        return "http";
    }

}
