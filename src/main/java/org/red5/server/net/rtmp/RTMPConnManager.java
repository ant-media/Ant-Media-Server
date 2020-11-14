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

package org.red5.server.net.rtmp;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.JMX;
import javax.management.ObjectName;

import org.apache.mina.core.session.IoSession;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.jmx.mxbeans.RTMPMinaTransportMXBean;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Responsible for management and creation of RTMP based connections.
 * 
 * @author The Red5 Project
 */
public class RTMPConnManager implements IConnectionManager<RTMPConnection>, ApplicationContextAware, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RTMPConnManager.class);

    protected static ApplicationContext applicationContext;

    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1, new CustomizableThreadFactory("ConnectionChecker-"));

    protected ConcurrentMap<String, RTMPConnection> connMap = new ConcurrentHashMap<String, RTMPConnection>();

    protected AtomicInteger conns = new AtomicInteger();

    protected static IConnectionManager<RTMPConnection> instance;

    protected boolean debug;

    {
        // create a scheduled job to check for dead or hung connections
        executor.scheduleAtFixedRate(new Runnable() {
            public void run() {
                // count the connections that need closing
                int closedConnections = 0;
                // get all the current connections
                Collection<RTMPConnection> allConns = getAllConnections();
                log.debug("Checking {} connections", allConns.size());
                for (RTMPConnection conn : allConns) {
                    if (log.isTraceEnabled()) {
                        log.trace("{} session: {} state: {} keep-alive running: {}", new Object[] { conn.getClass().getSimpleName(), conn.getSessionId(), RTMP.states[conn.getStateCode()], conn.running });
                        log.trace("Decoder lock - permits: {} queue length: {}", conn.decoderLock.availablePermits(), conn.decoderLock.getQueueLength());
                        log.trace("Encoder lock - permits: {} queue length: {}", conn.encoderLock.availablePermits(), conn.encoderLock.getQueueLength());
                        log.trace("Client streams: {} used: {}", conn.getStreams().size(), conn.getUsedStreamCount());
                        if (!conn.getAttributes().isEmpty()) {
                            log.trace("Attributes: {}", conn.getAttributes());
                        }
                        Iterator<IBasicScope> scopes = conn.getBasicScopes();
                        while (scopes.hasNext()) {
                            IBasicScope scope = scopes.next();
                            log.trace("Scope: {}", scope);
                        }
                    }
                    String sessionId = conn.getSessionId();
                    RTMP rtmp = conn.getState();
                    switch (rtmp.getState()) {
                        case RTMP.STATE_DISCONNECTED:
                        case RTMP.STATE_DISCONNECTING:
                            removeConnection(sessionId);
                            break;
                        default:
                            // XXX implement as a task and fix logic
                            // ghost clean up 
//                            if (max time allowed for no response from client exceeded, ping)
//                                // Ping client
//                                conn.ping();
//                                // FIXME: getLastPingTime doesn't get updated right after ping
//                                // wait x time for lastPingTime and if exceeded, disconnect
//                                if (conn.getLastPingTime() > clientTTL * 1000) {
//                                    log.info("TTL exceeded, disconnecting {}", conn);
//                                    conn.close();
//                                }
//                            }
                            long ioTime = 0L;
                            IoSession session = conn.getIoSession();
                            if (conn instanceof RTMPMinaConnection) {
                                // get io time
                                ioTime = System.currentTimeMillis() - session.getLastIoTime();
                                if (log.isTraceEnabled()) {
                                    log.trace("Session - write queue: {} session count: {}", session.getWriteRequestQueue().size(), session.getService().getManagedSessionCount());
                                }
                            } else if (conn instanceof RTMPTConnection) {
                                ioTime = System.currentTimeMillis() - ((RTMPTConnection) conn).getLastDataReceived();
                            }
                            if (log.isDebugEnabled()) {
                                log.debug("Session last io time: {} ms", ioTime);
                            }
                            // if exceeds max inactivity kill and clean up
                            if (ioTime >= conn.maxInactivity) {
                                log.warn("Connection {} has exceeded the max inactivity threshold of {} ms", conn.getSessionId(), conn.maxInactivity);
                                if (session != null) {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Prepared to clear write queue, if session is connected: {}; closing? {}", session.isConnected(), session.isClosing());
                                    }
                                    if (session.isConnected()) {
                                        // clear the write queue
                                        session.getWriteRequestQueue().clear(session);
                                    }
                                }
                                // call onInactive on the connection, this should cleanly close everything out
                                conn.onInactive();
                                if (!conn.isClosed()) {
                                    log.debug("Connection {} is not closed", conn.getSessionId());
                                } else {
                                    closedConnections++;
                                }
                            }
                    }
                }
                // if there is more than one connection that needed to be closed, request a GC to clean up memory.
                if (closedConnections > 0) {
                    System.gc();
                }
            }
        }, 7000, 30000, TimeUnit.MILLISECONDS);
    }

    public static IConnectionManager<RTMPConnection> getInstance() {
        if (instance == null) {
            log.trace("Connection manager instance does not exist");
            if (applicationContext != null && applicationContext.containsBean("rtmpConnManager")) {
                log.trace("Connection manager bean exists");
                instance = (RTMPConnManager) applicationContext.getBean("rtmpConnManager");
            } else {
                log.trace("Connection manager bean doesnt exist, creating new instance");
                instance = new RTMPConnManager();
            }
        }
        return instance;
    }

    /** {@inheritDoc} */
    public RTMPConnection createConnection(Class<?> connCls) {
        RTMPConnection conn = null;
        if (RTMPConnection.class.isAssignableFrom(connCls)) {
            try {
                // create connection
                conn = createConnectionInstance(connCls);
                // add to local map
                connMap.put(conn.getSessionId(), conn);
                log.trace("Connections: {}", conns.incrementAndGet());
                // set the scheduler
                if (applicationContext.containsBean("rtmpScheduler") && conn.getScheduler() == null) {
                    conn.setScheduler((ThreadPoolTaskScheduler) applicationContext.getBean("rtmpScheduler"));
                }
                log.trace("Connection created: {}", conn);
            } catch (Exception ex) {
                log.warn("Exception creating connection", ex);
            }
        }
        return conn;
    }

    /** {@inheritDoc} */
    public RTMPConnection createConnection(Class<?> connCls, String sessionId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Adds a connection.
     * 
     * @param conn
     *            connection
     */
    public void setConnection(RTMPConnection conn) {
        log.trace("Adding connection: {}", conn);
        int id = conn.getId();
        if (id == -1) {
            log.debug("Connection has unsupported id, using session id hash");
            id = conn.getSessionId().hashCode();
        }
        log.debug("Connection id: {} session id hash: {}", conn.getId(), conn.getSessionId().hashCode());
        if (debug) {
            log.info("Connection count (map): {}", connMap.size());
            try {
                RTMPMinaTransportMXBean proxy = JMX.newMXBeanProxy(ManagementFactory.getPlatformMBeanServer(), new ObjectName("org.red5.server:type=RTMPMinaTransport"), RTMPMinaTransportMXBean.class, true);
                if (proxy != null) {
                    log.info("{}", proxy.getStatistics());
                }
            } catch (Exception e) {
                log.warn("Error on jmx lookup", e);
            }
        }
    }

    /**
     * Returns a connection for a given client id.
     * 
     * @param clientId
     *            client id
     * @return connection if found and null otherwise
     */
    public RTMPConnection getConnection(int clientId) {
        log.trace("Getting connection by client id: {}", clientId);
        for (RTMPConnection conn : connMap.values()) {
            if (conn.getId() == clientId) {
                return connMap.get(conn.getSessionId());
            }
        }
        return null;
    }

    /**
     * Returns a connection for a given session id.
     * 
     * @param sessionId session id
     * @return connection if found and null otherwise
     */
    public RTMPConnection getConnectionBySessionId(String sessionId) {
        log.trace("Getting connection by session id: {}", sessionId);
        RTMPConnection conn = connMap.get(sessionId);
        if (conn == null && log.isDebugEnabled()) {
            log.debug("Connection not found for {}", sessionId);
            if (log.isTraceEnabled()) {
                log.trace("Connections ({}) {}", connMap.size(), connMap.values());
            }
        }
        return conn;
    }

    /** {@inheritDoc} */
    public RTMPConnection removeConnection(int clientId) {
        log.trace("Removing connection with id: {}", clientId);
        // remove from map
        for (RTMPConnection conn : connMap.values()) {
            if (conn.getId() == clientId) {
                // remove the conn
                return removeConnection(conn.getSessionId());
            }
        }
        log.warn("Connection was not removed by id: {}", clientId);
        return null;
    }

    /** {@inheritDoc} */
    public RTMPConnection removeConnection(String sessionId) {
        log.trace("Removing connection with session id: {}", sessionId);
        if (log.isTraceEnabled()) {
            log.trace("Connections ({}) at pre-remove: {}", connMap.size(), connMap.values());
        }
        // remove from map
        RTMPConnection conn = connMap.remove(sessionId);
        if (conn != null) {
            log.trace("Connections: {}", conns.decrementAndGet());
            Red5.setConnectionLocal(null);
        }
        return conn;
    }

    /** {@inheritDoc} */
    public Collection<RTMPConnection> getAllConnections() {
        ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>(connMap.size());
        list.addAll(connMap.values());
        return list;
    }

    /** {@inheritDoc} */
    public Collection<RTMPConnection> removeConnections() {
        ArrayList<RTMPConnection> list = new ArrayList<RTMPConnection>(connMap.size());
        list.addAll(connMap.values());
        connMap.clear();
        conns.set(0);
        return list;
    }

    /**
     * Creates a connection instance based on the supplied type.
     * 
     * @param cls
     *            class
     * @return connection
     * @throws Exception
     *             on error
     */
    public RTMPConnection createConnectionInstance(Class<?> cls) throws Exception {
        RTMPConnection conn = null;
        if (cls == RTMPMinaConnection.class) {
            conn = (RTMPMinaConnection) applicationContext.getBean(RTMPMinaConnection.class);
        } else if (cls == RTMPTConnection.class) {
            conn = (RTMPTConnection) applicationContext.getBean(RTMPTConnection.class);
        } else {
            conn = (RTMPConnection) cls.newInstance();
        }
        return conn;
    }

    /**
     * @param debug
     *            the debug to set
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RTMPConnManager.applicationContext = applicationContext;
    }

    public void destroy() throws Exception {
        executor.shutdownNow();
    }

}
