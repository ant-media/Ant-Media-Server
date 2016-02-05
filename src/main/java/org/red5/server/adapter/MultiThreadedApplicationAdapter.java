/*
 * RED5 Open Source Flash Server - https://github.com/Red5/
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.red5.io.IStreamableFile;
import org.red5.io.ITagReader;
import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IClient;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.plugin.IRed5Plugin;
import org.red5.server.api.plugin.IRed5PluginHandler;
import org.red5.server.api.scheduling.IScheduledJob;
import org.red5.server.api.scheduling.ISchedulingService;
import org.red5.server.api.scope.IBroadcastScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IBroadcastStreamService;
import org.red5.server.api.service.IOnDemandStreamService;
import org.red5.server.api.service.IStreamSecurityService;
import org.red5.server.api.service.IStreamableFileService;
import org.red5.server.api.service.ISubscriberStreamService;
import org.red5.server.api.service.ServiceUtils;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectSecurity;
import org.red5.server.api.so.ISharedObjectSecurityService;
import org.red5.server.api.so.ISharedObjectService;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.api.stream.IOnDemandStream;
import org.red5.server.api.stream.IPlayItem;
import org.red5.server.api.stream.IStreamAwareScopeHandler;
import org.red5.server.api.stream.IStreamPlaybackSecurity;
import org.red5.server.api.stream.IStreamPublishSecurity;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.api.stream.IStreamableFileFactory;
import org.red5.server.api.stream.ISubscriberStream;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.jmx.mxbeans.ApplicationMXBean;
import org.red5.server.messaging.AbstractPipe;
import org.red5.server.messaging.IMessageInput;
import org.red5.server.plugin.PluginDescriptor;
import org.red5.server.plugin.PluginRegistry;
import org.red5.server.plugin.Red5Plugin;
import org.red5.server.scheduling.QuartzSchedulingService;
import org.red5.server.so.SharedObjectService;
import org.red5.server.stream.IProviderService;
import org.red5.server.stream.PlaylistSubscriberStream;
import org.red5.server.stream.ProviderService;
import org.red5.server.stream.StreamService;
import org.red5.server.stream.StreamableFileFactory;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;

/**
 * ApplicationAdapter class serves as a base class for your Red5 applications. It provides methods to work with SharedObjects and streams,
 * as well as connections and scheduling services. <br>
 * ApplicationAdapter is an application level IScope. To handle streaming processes in your application you should implement
 * {@link IStreamAwareScopeHandler} interface and implement handling methods. <br>
 * Application adapter provides you with useful event handlers that can be used to intercept streams, authorize users, etc. Also, all
 * methods added in subclasses can be called from client side with NetConnection.call method. Unlike to Flash Media server which requires
 * you to keep methods on Client object at server side, Red5 offers much more convenient way to add methods <br>
 * <strong>EXAMPLE:</strong> <br>
 * 
 * <code>
 * public List&lt;String&gt; getLiveStreams() {
 *     // Implementation goes here, say, use Red5 object to obtain scope and all it's streams
 * }
 * </code>
 * 
 * <br>
 * This method added to ApplicationAdapter subclass can be called from client side with the following code: <br>
 * 
 * <code>
 * var nc:NetConnection = new NetConnection();
 * nc.connect(...);
 * nc.call("getLiveStreams", resultHandlerObj);
 * </code>
 * 
 * <br>
 * If you want to build a server-side framework this is a place to start and wrap it around ApplicationAdapter subclass.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Paul Gregoire (mondain@gmail.com)
 * @author Michael Klishin
 */
public class MultiThreadedApplicationAdapter extends StatefulScopeWrappingAdapter implements ISharedObjectService, IBroadcastStreamService, IOnDemandStreamService, ISubscriberStreamService, ISchedulingService, IStreamSecurityService, ISharedObjectSecurityService, IStreamAwareScopeHandler, ApplicationMXBean {

    /**
     * Logger object
     */
    protected Logger log = null;

    /**
     * List of application listeners.
     */
    private CopyOnWriteArraySet<IApplication> listeners = new CopyOnWriteArraySet<IApplication>();

    /**
     * Scheduling service. Uses Quartz. Adds and removes scheduled jobs.
     */
    protected ISchedulingService schedulingService;

    /**
     * List of handlers that protect stream publishing.
     */
    private Set<IStreamPublishSecurity> publishSecurity = new HashSet<IStreamPublishSecurity>();

    /**
     * List of handlers that protect stream playback.
     */
    private Set<IStreamPlaybackSecurity> playbackSecurity = new HashSet<IStreamPlaybackSecurity>();

    /**
     * List of handlers that protect shared objects.
     */
    private Set<ISharedObjectSecurity> sharedObjectSecurity = new HashSet<ISharedObjectSecurity>();

    /**
     * Register a listener that will get notified about application events.
     * 
     * @param listener
     *            object to register
     */
    public void addListener(IApplication listener) {
        listeners.add(listener);
    }

    /**
     * Unregister handler that will not get notified about application events any longer.
     * 
     * @param listener
     *            object to unregister
     */
    public void removeListener(IApplication listener) {
        listeners.remove(listener);
    }

    /**
     * Return handlers that get notified about application events.
     * 
     * @return list of handlers
     */
    public Set<IApplication> getListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    /** {@inheritDoc} */
    public void registerStreamPublishSecurity(IStreamPublishSecurity handler) {
        publishSecurity.add(handler);
    }

    /** {@inheritDoc} */
    public void unregisterStreamPublishSecurity(IStreamPublishSecurity handler) {
        publishSecurity.remove(handler);
    }

    /** {@inheritDoc} */
    public Set<IStreamPublishSecurity> getStreamPublishSecurity() {
        return publishSecurity;
    }

    /** {@inheritDoc} */
    public void registerStreamPlaybackSecurity(IStreamPlaybackSecurity handler) {
        playbackSecurity.add(handler);
    }

    /** {@inheritDoc} */
    public void unregisterStreamPlaybackSecurity(IStreamPlaybackSecurity handler) {
        playbackSecurity.remove(handler);
    }

    /** {@inheritDoc} */
    public Set<IStreamPlaybackSecurity> getStreamPlaybackSecurity() {
        return playbackSecurity;
    }

    /** {@inheritDoc} */
    public void registerSharedObjectSecurity(ISharedObjectSecurity handler) {
        sharedObjectSecurity.add(handler);
    }

    /** {@inheritDoc} */
    public void unregisterSharedObjectSecurity(ISharedObjectSecurity handler) {
        sharedObjectSecurity.remove(handler);
    }

    /** {@inheritDoc} */
    public Set<ISharedObjectSecurity> getSharedObjectSecurity() {
        return sharedObjectSecurity;
    }

    /**
     * Reject the currently connecting client without a special error message. This method throws {@link ClientRejectedException} exception.
     * 
     * @return never returns
     * @throws org.red5.server.exception.ClientRejectedException
     *             Thrown when client connection must be rejected by application logic
     */
    protected boolean rejectClient() throws ClientRejectedException {
        throw new ClientRejectedException();
    }

    /**
     * Reject the currently connecting client with an error message.
     * 
     * The passed object will be available as "application" property of the information object that is returned to the caller.
     * 
     * @param reason
     *            Additional error message to return to client-side Flex/Flash application
     * @return never returns
     * 
     * @throws org.red5.server.exception.ClientRejectedException
     *             Thrown when client connection must be rejected by application logic
     */
    protected boolean rejectClient(Object reason) throws ClientRejectedException {
        throw new ClientRejectedException(reason);
    }

    /**
     * Returns connection result for given scope and parameters. Whether the scope is room or app level scope, this method distinguishes it
     * and acts accordingly. You override {@link ApplicationAdapter#appConnect(IConnection, Object[])} or
     * {@link ApplicationAdapter#roomConnect(IConnection, Object[])} in your application to make it act the way you want.
     * 
     * @param conn
     *            Connection object
     * @return <code>true</code> if connect is successful, <code>false</code> otherwise
     */
    public boolean connect(IConnection conn) {
        // ensure the log is not null at this point
        if (log == null) {
            log = Red5LoggerFactory.getLogger(this.getClass());
        }
        // get the scope from the connection
        IScope scope = conn.getScope();
        log.debug("connect: {} > {}", conn, scope);
        return connect(conn, scope, null);
    }

    /**
     * Returns connection result for given scope and parameters. Whether the scope is room or app level scope, this method distinguishes it
     * and acts accordingly. You override {@link ApplicationAdapter#appConnect(IConnection, Object[])} or
     * {@link ApplicationAdapter#roomConnect(IConnection, Object[])} in your application to make it act the way you want.
     * 
     * @param conn
     *            Connection object
     * @param scope
     *            Scope
     * @param params
     *            List of params passed to connection handler
     * @return <code>
     * true
     * </code>
     * 
     *         if connect is successful,
     * 
     *         <code>
     * false
     * </code>
     * 
     *         otherwise
     */
    @Override
    public boolean connect(IConnection conn, IScope scope, Object[] params) {
        // ensure the log is not null at this point
        if (log == null) {
            log = Red5LoggerFactory.getLogger(this.getClass());
        }
        log.debug("connect: {} > {}", conn, scope);
        boolean success = false;
        // hit the super class first
        if (super.connect(conn, scope, params)) {
            if (log.isInfoEnabled() && ScopeUtils.isApp(scope)) {
                // log w3c connect event
                IClient client = conn.getClient();
                if (client == null) {
                    log.info("W3C x-category:session x-event:connect c-ip:{}", conn.getRemoteAddress());
                } else {
                    log.info("W3C x-category:session x-event:connect c-ip:{} c-client-id:{}", conn.getRemoteAddress(), client.getId());
                }
            }
            if (ScopeUtils.isApp(scope)) {
                success = appConnect(conn, params);
            } else if (ScopeUtils.isRoom(scope)) {
                success = roomConnect(conn, params);
            } else {
                log.warn("Scope was not of app or room type, connect failed");
            }
        }
        return success;
    }

    /**
     * Starts scope. Scope can be both application or room level.
     * 
     * @param scope
     *            Scope object
     * @return <code>
     * true
     * </code>
     * 
     *         if scope can be started,
     * 
     *         <code>
     * false
     * </code>
     * 
     *         otherwise. See {@link AbstractScopeAdapter#start(IScope)} for details.
     */
    @Override
    public boolean start(IScope scope) {
        //set the log here so that stuff is logged in the correct place
        if (log == null) {
            log = Red5LoggerFactory.getLogger(this.getClass());
        }
        log.debug("start: {}", scope);
        //setup plug-ins
        log.trace("Plugins: {}", plugins);
        if (plugins != null) {
            for (PluginDescriptor desc : plugins) {
                log.debug("Plugin: {}", desc);
                try {
                    //ensure plug-in class can be resolved
                    ClassLoader classLoader = scope.getClassLoader();
                    Class<?> clazz = Class.forName(desc.getPluginType(), true, classLoader);
                    log.trace("Class: {}", clazz);
                    //get the plug-in from the registry
                    IRed5Plugin plugin = PluginRegistry.getPlugin(desc.getPluginName());
                    log.debug("Got plugin from the registry: {}", plugin);
                    //if the plugin class extends the red5 plug-in we will add the application to it
                    if (plugin instanceof Red5Plugin) {
                        //pass the app to the plugin so that it may be manipulated directly by the plug-in
                        ((Red5Plugin) plugin).setApplication(this);
                    }
                    //when a plug-in method is specified do invokes
                    if (desc.getMethod() != null) {
                        Method method = plugin.getClass().getMethod(desc.getMethod(), (Class[]) null);
                        //if a return type is not specified for the plug-in method just invoke it
                        if (desc.getMethodReturnType() == null) {
                            log.debug("Invoking plugin");
                            method.invoke(plugin, (Object[]) null);
                        } else {
                            log.debug("Invoking plugin");
                            Object returnClass = method.invoke(plugin, (Object[]) null);
                            if (returnClass instanceof IRed5PluginHandler) {
                                //if there are props add them
                                Map<String, Object> props = desc.getProperties();
                                if (props != null) {
                                    Method setProps = returnClass.getClass().getMethod("setProperties", new Class[] { Map.class });
                                    setProps.invoke(returnClass, new Object[] { props });
                                }
                                //initialize
                                Method init = returnClass.getClass().getMethod("init", (Class[]) null);
                                init.invoke(returnClass, (Object[]) null);
                            }
                            if (returnClass instanceof IApplication) {
                                //if its an IApplcation add it to the listeners
                                log.debug("Adding result class to listeners");
                                addListener((IApplication) returnClass);
                            } else {
                                log.info("Returned class did not implement IApplication");
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Exception setting up a plugin", e);
                }
            }
        }
        boolean started = false;
        // verify that we can start
        if (super.start(scope)) {
            if (ScopeUtils.isApp(scope)) {
                started = appStart(scope);
            } else if (ScopeUtils.isRoom(scope)) {
                started = roomStart(scope);
            } else {
                log.warn("Scope wasn't of app or room type, it was not started");
            }
            // fix for issue 91
            if (started) {
                // we dont allow connections until we are started
                super.setCanConnect(true);
                // we also dont allow service calls until started
                super.setCanCallService(true);
            }
        }
        return started;
    }

    /**
     * Returns disconnection result for given scope and parameters. Whether the scope is room or app level scope, this method distinguishes
     * it and acts accordingly.
     * 
     * @param conn
     *            Connection object
     * @param scope
     *            Scope
     */
    @Override
    public void disconnect(IConnection conn, IScope scope) {
        log.debug("disconnect: {} < {}", conn, scope);
        if (log.isInfoEnabled() && ScopeUtils.isApp(scope)) {
            // log w3c connect event
            IClient client = conn.getClient();
            if (client == null) {
                // log w3c connect event
                log.info("W3C x-category:session x-event:disconnect c-ip:{}", conn.getRemoteAddress());
            } else {
                // log w3c connect event
                log.info("W3C x-category:session x-event:disconnect c-ip:{} c-client-id:{}", conn.getRemoteAddress(), client.getId());
            }
        }
        if (ScopeUtils.isApp(scope)) {
            appDisconnect(conn);
        } else if (ScopeUtils.isRoom(scope)) {
            roomDisconnect(conn);
        }
        super.disconnect(conn, scope);
    }

    /**
     * Stops scope handling (that is, stops application if given scope is app level scope and stops room handling if given scope has lower
     * scope level). This method calls {@link ApplicationAdapter#appStop(IScope)} or {@link ApplicationAdapter#roomStop(IScope)} handlers
     * respectively.
     * 
     * @param scope
     *            Scope to stop
     */
    @Override
    public void stop(IScope scope) {
        log.debug("stop: {}", scope.getName());
        // stop the app / room / etc
        if (ScopeUtils.isApp(scope)) {
            // we don't allow connections after we stop
            super.setCanConnect(false);
            // we also don't allow service calls 
            super.setCanCallService(false);
            // stop the app
            appStop(scope);
        } else if (ScopeUtils.isRoom(scope)) {
            roomStop(scope);
        }
        super.stop(scope);
    }

    /**
     * Adds client to scope. Scope can be both application or room. Can be applied to both application scope and scopes of lower level. This
     * method calls {@link ApplicationAdapter#appJoin(IClient, IScope)} or {@link ApplicationAdapter#roomJoin(IClient, IScope)} handlers
     * respectively.
     * 
     * @param client
     *            Client object
     * @param scope
     *            Scope object
     */
    @Override
    public boolean join(IClient client, IScope scope) {
        if (!super.join(client, scope)) {
            return false;
        }
        if (ScopeUtils.isApp(scope)) {
            return appJoin(client, scope);
        } else {
            return ScopeUtils.isRoom(scope) && roomJoin(client, scope);
        }
    }

    /**
     * Disconnects client from scope. Can be applied to both application scope and scopes of lower level. This method calls
     * {@link ApplicationAdapter#appLeave(IClient, IScope)} or {@link ApplicationAdapter#roomLeave(IClient, IScope)} handlers respectively.
     * 
     * @param client
     *            Client object
     * @param scope
     *            Scope object
     */
    @Override
    public void leave(IClient client, IScope scope) {
        log.debug("leave: {} << {}", client, scope);
        if (ScopeUtils.isApp(scope)) {
            appLeave(client, scope);
        } else if (ScopeUtils.isRoom(scope)) {
            roomLeave(client, scope);
        }
        super.leave(client, scope);
    }

    /**
     * Called once on scope (that is, application or application room) start. You override {@link ApplicationAdapter#appStart(IScope)} or
     * {@link ApplicationAdapter#roomStart(IScope)} in your application to make it act the way you want.
     * 
     * @param app
     *            Application scope object
     * @return <code>
     * true
     * </code>
     * 
     *         if scope can be started,
     * 
     *         <code>
     * false
     * </code>
     * 
     *         otherwise
     */
    public boolean appStart(IScope app) {
        log.debug("appStart: {}", app);
        for (IApplication listener : listeners) {
            if (!listener.appStart(app)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handler method. Called when application is stopped.
     * 
     * @param app
     *            Scope object
     */
    public void appStop(IScope app) {
        log.debug("appStop: {}", app);
        for (IApplication listener : listeners) {
            listener.appStop(app);
        }
    }

    /**
     * Handler method. Called when room scope is started.
     * 
     * @param room
     *            Room scope
     * @return Boolean value
     */
    public boolean roomStart(IScope room) {
        log.debug("roomStart: {}", room);
        // TODO : Get to know what does roomStart return mean
        for (IApplication listener : listeners) {
            if (!listener.roomStart(room)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handler method. Called when room scope is stopped.
     * 
     * @param room
     *            Room scope.
     */
    public void roomStop(IScope room) {
        log.debug("roomStop: {}", room);
        for (IApplication listener : listeners) {
            listener.roomStop(room);
        }
    }

    /**
     * Handler method. Called every time new client connects (that is, new IConnection object is created after call from a SWF movie) to the
     * application. <br>
     * You override this method to pass additional data from client to server application using
     * 
     * <code>
     * NetConnection.connect
     * </code>
     * 
     * method. <br>
     * <strong>EXAMPLE:</strong> <br>
     * In this simple example we pass user's skin of choice identifier from client to the server. <br>
     * <strong>Client-side:</strong> <br>
     * 
     * <code>
     * NetConnection.connect(&quot;rtmp://localhost/killerred5app&quot;, &quot;silver&quot;);
     * </code>
     * 
     * <br>
     * <strong>Server-side:</strong> <br>
     * 
     * <code>
     * if (params.length &gt; 0)
     *     log.debug(&quot;Theme selected: {}&quot;, params[0]);
     * </code>
     * 
     * @param conn
     *            Connection object
     * @param params
     *            List of parameters after connection URL passed to
     * 
     *            <code>
     * NetConnection.connect
     * </code>
     * 
     *            method.
     * @return Boolean value
     */
    public boolean appConnect(IConnection conn, Object[] params) {
        log.debug("appConnect: {}", conn);
        for (IApplication listener : listeners) {
            if (!listener.appConnect(conn, params)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handler method. Called every time new client connects (that is, new IConnection object is created after call from a SWF movie) to the
     * application.
     * 
     * You override this method to pass additional data from client to server application using
     * 
     * <code>
     * NetConnection.connect
     * </code>
     * 
     * method.
     * 
     * See {@link ApplicationAdapter#appConnect(IConnection, Object[])} for code example.
     * 
     * @param conn
     *            Connection object
     * @param params
     *            List of params passed to room scope
     * @return Boolean value
     */
    public boolean roomConnect(IConnection conn, Object[] params) {
        log.debug("roomConnect: {}", conn);
        for (IApplication listener : listeners) {
            if (!listener.roomConnect(conn, params)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handler method. Called every time client disconnects from the application.
     * 
     * @param conn
     *            Disconnected connection object
     */
    public void appDisconnect(IConnection conn) {
        log.debug("appDisconnect: {}", conn);
        for (IApplication listener : listeners) {
            listener.appDisconnect(conn);
        }
    }

    /**
     * Handler method. Called every time client disconnects from the room.
     * 
     * @param conn
     *            Disconnected connection object
     */
    public void roomDisconnect(IConnection conn) {
        log.debug("roomDisconnect: {}", conn);
        for (IApplication listener : listeners) {
            listener.roomDisconnect(conn);
        }
    }

    public boolean appJoin(IClient client, IScope app) {
        log.debug("appJoin: {} >> {}", client, app);
        for (IApplication listener : listeners) {
            if (!listener.appJoin(client, app)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handler method. Called every time client leaves application scope.
     * 
     * @param client
     *            Client object that left
     * @param app
     *            Application scope
     */
    public void appLeave(IClient client, IScope app) {
        log.debug("appLeave: {} << {}", client, app);
        for (IApplication listener : listeners) {
            listener.appLeave(client, app);
        }
    }

    public boolean roomJoin(IClient client, IScope room) {
        log.debug("roomJoin: {} >> {}", client, room);
        for (IApplication listener : listeners) {
            if (!listener.roomJoin(client, room)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Handler method. Called every time client leaves room scope.
     * 
     * @param client
     *            Disconnected client object
     * @param room
     *            Room scope
     */
    public void roomLeave(IClient client, IScope room) {
        log.debug("roomLeave: {} << {}", client, room);
        for (IApplication listener : listeners) {
            listener.roomLeave(client, room);
        }
    }

    /**
     * Try to measure bandwidth of current connection.
     * 
     * This is required for some FLV player to work because they require the "onBWDone" method to be called on the connection.
     */
    public void measureBandwidth() {
        measureBandwidth(Red5.getConnectionLocal());
    }

    /**
     * Try to measure bandwidth of given connection.
     * 
     * This is required for some FLV player to work because they require the "onBWDone" method to be called on the connection.
     * 
     * @param conn
     *            the connection to measure the bandwidth for
     */
    public void measureBandwidth(IConnection conn) {
        // dummy for now, this makes flv player work
        // they dont wait for connected status they wait for onBWDone
        ServiceUtils.invokeOnConnection(conn, "onBWDone", new Object[] {});
    }

    /* Wrapper around ISharedObjectService */

    /**
     * Creates a new shared object for given scope. Server-side shared objects (also known as Remote SO) are special kind of objects those
     * variable are synchronized between clients. To get an instance of RSO at client-side, use
     * 
     * <code>
     * SharedObject.getRemote()
     * </code>
     * 
     * . SharedObjects can be persistent and transient. Persistent RSO are stateful, i.e. store their data between sessions. If you need to
     * store some data on server while clients go back and forth use persistent SO (just use
     * 
     * <code>
     * true
     * </code>
     * 
     * ), otherwise prefer usage of transient for extra performance.
     * 
     * @param scope
     *            Scope that shared object belongs to
     * @param name
     *            Name of SharedObject
     * @param persistent
     *            Whether SharedObject instance should be persistent or not
     * @return true if SO was created, false otherwise
     */
    public boolean createSharedObject(IScope scope, String name, boolean persistent) {
        ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, SharedObjectService.class, false);
        return service.createSharedObject(scope, name, persistent);
    }

    /**
     * Returns shared object from given scope by name.
     * 
     * @param scope
     *            Scope that shared object belongs to
     * @param name
     *            Name of SharedObject
     * @return Shared object instance with name given
     */
    public ISharedObject getSharedObject(IScope scope, String name) {
        ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, SharedObjectService.class, false);
        return service.getSharedObject(scope, name);
    }

    /**
     * Returns shared object from given scope by name.
     * 
     * @param scope
     *            Scope that shared object belongs to
     * @param name
     *            Name of SharedObject
     * @param persistent
     *            Whether SharedObject instance should be persistent or not
     * @return Shared object instance with name given
     */
    public ISharedObject getSharedObject(IScope scope, String name, boolean persistent) {
        ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, SharedObjectService.class, false);
        return service.getSharedObject(scope, name, persistent);
    }

    /**
     * Returns available SharedObject names as List
     * 
     * @param scope
     *            Scope that SO belong to
     */
    public Set<String> getSharedObjectNames(IScope scope) {
        ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, SharedObjectService.class, false);
        return service.getSharedObjectNames(scope);
    }

    /**
     * Checks whether there's a SO with given scope and name
     * 
     * @param scope
     *            Scope that SO belong to
     * @param name
     *            Name of SharedObject
     */
    public boolean hasSharedObject(IScope scope, String name) {
        ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, SharedObjectService.class, false);
        return service.hasSharedObject(scope, name);
    }

    /* Wrapper around the stream interfaces */

    /** {@inheritDoc} */
    public boolean hasBroadcastStream(IScope scope, String name) {
        IProviderService service = (IProviderService) ScopeUtils.getScopeService(scope, IProviderService.class, ProviderService.class);
        return (service.getLiveProviderInput(scope, name, false) != null);
    }

    /** {@inheritDoc} */
    public IBroadcastStream getBroadcastStream(IScope scope, String name) {
        IStreamService service = (IStreamService) ScopeUtils.getScopeService(scope, IStreamService.class, StreamService.class);
        if (service instanceof StreamService) {
            IBroadcastScope bs = ((StreamService) service).getBroadcastScope(scope, name);
            if (bs != null) {
                return bs.getClientBroadcastStream();
            }
        }
        return null;
    }

    /**
     * Returns list of stream names broadcasted in scope. Broadcast stream name is somewhat different from server stream name. Server stream
     * name is just an ID assigned by Red5 to every created stream. Broadcast stream name is the name that is being used to subscribe to the
     * stream at client side, that is, in
     * 
     * <code>
     * NetStream.play
     * </code>
     * 
     * call.
     * 
     * @param scope
     *            Scope to retrieve broadcasted stream names
     * @return List of broadcasted stream names.
     */
    public Set<String> getBroadcastStreamNames(IScope scope) {
        IProviderService service = (IProviderService) ScopeUtils.getScopeService(scope, IProviderService.class, ProviderService.class);
        return service.getBroadcastStreamNames(scope);
    }

    /**
     * Check whether scope has VOD stream with given name or not
     * 
     * @param scope
     *            Scope
     * @param name
     *            VOD stream name
     * 
     * @return true if scope has VOD stream with given name, false otherwise.
     */
    public boolean hasOnDemandStream(IScope scope, String name) {
        IProviderService service = (IProviderService) ScopeUtils.getScopeService(scope, IProviderService.class, ProviderService.class);
        IMessageInput msgIn = service.getVODProviderInput(scope, name);
        if (msgIn instanceof AbstractPipe) {
            ((AbstractPipe) msgIn).close();
        }
        return (msgIn != null);
    }

    /**
     * Returns VOD stream with given name from specified scope.
     * 
     * @param scope
     *            Scope object
     * @param name
     *            VOD stream name
     * 
     * @return IOnDemandStream object that represents stream that can be played on demand, seekable and so forth. See
     *         {@link IOnDemandStream} for details.
     */
    public IOnDemandStream getOnDemandStream(IScope scope, String name) {
        log.warn("This won't work until the refactoring of the streaming code is complete.");
        IOnDemandStreamService service = (IOnDemandStreamService) ScopeUtils.getScopeService(scope, IOnDemandStreamService.class, StreamService.class, false);
        return service.getOnDemandStream(scope, name);
    }

    /**
     * Returns subscriber stream with given name from specified scope. Subscriber stream is a stream that clients can subscribe to.
     * 
     * @param scope
     *            Scope
     * @param name
     *            Stream name
     * 
     * @return ISubscriberStream object
     */
    public ISubscriberStream getSubscriberStream(IScope scope, String name) {
        log.warn("This won't work until the refactoring of the streaming code is complete.");
        ISubscriberStreamService service = (ISubscriberStreamService) ScopeUtils.getScopeService(scope, ISubscriberStreamService.class, StreamService.class, false);
        return service.getSubscriberStream(scope, name);
    }

    /**
     * Wrapper around ISchedulingService, adds a scheduled job to be run periodically. We store this service in the scope as it can be
     * shared across all rooms of the applications.
     * 
     * @param interval
     *            Time interval to run the scheduled job
     * @param job
     *            Scheduled job object
     * 
     * @return Name of the scheduled job
     */
    public String addScheduledJob(int interval, IScheduledJob job) {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        return service.addScheduledJob(interval, job);
    }

    /**
     * Adds a scheduled job that's gonna be executed once. Please note that the jobs are not saved if Red5 is restarted in the meantime.
     * 
     * @param timeDelta
     *            Time offset in milliseconds from the current date when given job should be run
     * @param job
     *            Scheduled job object
     * 
     * @return Name of the scheduled job
     */
    public String addScheduledOnceJob(long timeDelta, IScheduledJob job) {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        return service.addScheduledOnceJob(timeDelta, job);
    }

    /**
     * Adds a scheduled job that's gonna be executed once on given date. Please note that the jobs are not saved if Red5 is restarted in the
     * meantime.
     * 
     * @param date
     *            When to run scheduled job
     * @param job
     *            Scheduled job object
     * 
     * @return Name of the scheduled job
     */
    public String addScheduledOnceJob(Date date, IScheduledJob job) {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        return service.addScheduledOnceJob(date, job);
    }

    /**
     * Adds a scheduled job which starts after the specified delay period and fires periodically.
     * 
     * @param interval
     *            time in milliseconds between two notifications of the job
     * @param job
     *            the job to trigger periodically
     * @param delay
     *            time in milliseconds to pass before first execution.
     * @return the name of the scheduled job
     */
    public String addScheduledJobAfterDelay(int interval, IScheduledJob job, int delay) {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        return service.addScheduledJobAfterDelay(interval, job, delay);
    }

    /**
     * Pauses a scheduled job
     * 
     * @param name
     *            Scheduled job name
     */
    public void pauseScheduledJob(String name) {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        service.pauseScheduledJob(name);
    }

    /**
     * Resumes a scheduled job
     * 
     * @param name
     *            Scheduled job name
     */
    public void resumeScheduledJob(String name) {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        service.resumeScheduledJob(name);
    }

    /**
     * Removes scheduled job from scheduling service list
     * 
     * @param name
     *            Scheduled job name
     */
    public void removeScheduledJob(String name) {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        service.removeScheduledJob(name);
    }

    /**
     * Returns list of scheduled job names
     * 
     * @return List of scheduled job names as list of Strings.
     */
    public List<String> getScheduledJobNames() {
        ISchedulingService service = (ISchedulingService) ScopeUtils.getScopeService(scope, ISchedulingService.class, QuartzSchedulingService.class, false);
        return service.getScheduledJobNames();
    }

    /**
     * Returns stream length. This is a hook so it may be removed.
     * 
     * @param name
     *            Stream name
     * @return Stream length in seconds (?)
     */
    public double getStreamLength(String name) {
        double duration = 0;
        IProviderService provider = (IProviderService) ScopeUtils.getScopeService(scope, IProviderService.class, ProviderService.class);
        File file = provider.getVODProviderFile(scope, name);
        if (file != null && file.canRead()) {
            IStreamableFileFactory factory = (IStreamableFileFactory) ScopeUtils.getScopeService(scope, IStreamableFileFactory.class, StreamableFileFactory.class);
            IStreamableFileService service = factory.getService(file);
            if (service != null) {
                ITagReader reader = null;
                try {
                    IStreamableFile streamFile = service.getStreamableFile(file);
                    reader = streamFile.getReader();
                    duration = (double) reader.getDuration() / 1000;
                } catch (IOException e) {
                    log.error("Error read stream file {}. {}", file.getAbsolutePath(), e);
                } finally {
                    if (reader != null) {
                        reader.close();
                    }
                }
            } else {
                log.error("No service found for {}", file.getAbsolutePath());
            }
            file = null;
        }
        return duration;
    }

    /** {@inheritDoc} */
    public boolean clearSharedObjects(IScope scope, String name) {
        ISharedObjectService service = (ISharedObjectService) ScopeUtils.getScopeService(scope, ISharedObjectService.class, SharedObjectService.class, false);
        return service.clearSharedObjects(scope, name);
    }

    /**
     * Client time to live is max allowed connection ping return time in seconds
     * 
     * @return TTL value used in seconds
     */
    @Deprecated
    public long getClientTTL() {
        return -1;
    }

    /**
     * Client time to live is max allowed connection ping return time in seconds
     * 
     * @param clientTTL
     *            New TTL value in seconds
     */
    @Deprecated
    public void setClientTTL(int clientTTL) {
    }

    /**
     * Return period of ghost connections cleanup task call
     * 
     * @return Ghost connections cleanup period
     */
    @Deprecated
    public int getGhostConnsCleanupPeriod() {
        return -1;
    }

    /**
     * Set new ghost connections cleanup period
     * 
     * @param ghostConnsCleanupPeriod
     *            New ghost connections cleanup period
     */
    @Deprecated
    public void setGhostConnsCleanupPeriod(int ghostConnsCleanupPeriod) {
    }

    /**
     * Start transmission notification from Flash Player 11.1+. This command asks the server to transmit more data because the buffer is
     * running low.
     * 
     * http://help.adobe.com/en_US/flashmediaserver/devguide/WSd391de4d9c7bd609-569139412a3743e78e-8000.html
     * 
     * @param bool
     *            boolean
     * @param num
     *            number
     */
    public void startTransmit(Boolean bool, int num) {
    }

    /**
     * Stop transmission notification from Flash Player 11.1+. This command asks the server to suspend transmission until the client sends a
     * startTransmit event because there is enough data in the buffer.
     */
    public void stopTransmit() {
    }

    /**
     * Stop transmission notification from Flash Player 11.1+. This command asks the server to suspend transmission until the client sends a
     * startTransmit event because there is enough data in the buffer.
     * 
     * @param bool
     *            boolean
     * @param num
     *            number
     */
    public void stopTransmit(Boolean bool, int num) {
    }

    /**
     * Notification method that is sent by FME just before publishing starts.
     * 
     * @param streamName
     *            Name of stream that is about to be published.
     */
    public void FCPublish(String streamName) {
    }

    /**
     * Notification method that is sent by FME when publishing of a stream ends.
     */
    public void FCUnpublish() {
    }

    /**
     * Notification method that is sent by FME when publishing of a stream ends.
     * 
     * @param streamName
     *            Name of stream that is about to be un-published.
     */
    public void FCUnpublish(String streamName) {
    }

    /**
     * Notification method that is sent by some clients just before playback starts.
     * 
     * @param streamName
     *            Name of stream that is about to be played.
     */
    public void FCSubscribe(String streamName) {
    }

    /**
     * Notification that a broadcasting stream is closing.
     * 
     * @param stream
     *            stream
     */
    public void streamBroadcastClose(IBroadcastStream stream) {
        // log w3c connect event
        IConnection conn = Red5.getConnectionLocal();
        // converted to seconds
        long publishDuration = (System.currentTimeMillis() - stream.getCreationTime()) / 1000;
        if (conn != null) {
            log.info("W3C x-category:stream x-event:unpublish c-ip:{} cs-bytes:{} sc-bytes:{} x-sname:{} x-file-length:{} x-name:{}", new Object[] { conn.getRemoteAddress(), conn.getReadBytes(), conn.getWrittenBytes(), stream.getName(), publishDuration, stream.getPublishedName() });
        } else {
            log.info("W3C x-category:stream x-event:unpublish x-sname:{} x-file-length:{} x-name:{}", new Object[] { stream.getName(), publishDuration, stream.getPublishedName() });
        }
        String recordingName = stream.getSaveFilename();
        // if its not null then we did a recording
        if (recordingName != null) {
            if (conn != null) {
                // use cs-bytes for file size for now
                log.info("W3C x-category:stream x-event:recordstop c-ip:{} cs-bytes:{} sc-bytes:{} x-sname:{} x-file-name:{} x-file-length:{} x-file-size:{}", new Object[] { conn.getRemoteAddress(), conn.getReadBytes(), conn.getWrittenBytes(), stream.getName(), recordingName, publishDuration, conn.getReadBytes() });
            } else {
                log.info("W3C x-category:stream x-event:recordstop x-sname:{} x-file-name:{} x-file-length:{}", new Object[] { stream.getName(), recordingName, publishDuration });
            }
            // if the stream length is 0 bytes then delete it, this is a fix for SN-20
            // get the web root
            String webappsPath = System.getProperty("red5.webapp.root");
            // add context name
            File file = new File(webappsPath, getName() + '/' + recordingName);
            if (file != null) {
                log.debug("File path: {}", file.getAbsolutePath());
                if (file.exists()) {
                    // if publish duration or length are zero
                    if (publishDuration == 0 || file.length() == 0) {
                        if (file.delete()) {
                            log.info("File {} was deleted", file.getName());
                        } else {
                            log.info("File {} was not deleted, it will be deleted on exit", file.getName());
                            file.deleteOnExit();
                        }
                    }
                }
                file = null;
            }
        }
    }

    public void streamBroadcastStart(IBroadcastStream stream) {
    }

    public void streamPlayItemPlay(ISubscriberStream stream, IPlayItem item, boolean isLive) {
        // log w3c connect event
        log.info("W3C x-category:stream x-event:play c-ip:{} x-sname:{} x-name:{}", new Object[] { Red5.getConnectionLocal().getRemoteAddress(), stream.getName(), item.getName() });
    }

    public void streamPlayItemStop(ISubscriberStream stream, IPlayItem item) {
        // since there is a fair amount of processing below we will check log
        // level prior to proceeding
        if (log.isInfoEnabled()) {
            // log w3c connect event
            String remoteAddress = "";
            long readBytes = -1;
            long writtenBytes = -1;
            IConnection conn = Red5.getConnectionLocal();
            if (conn != null) {
                remoteAddress = conn.getRemoteAddress();
                readBytes = conn.getReadBytes();
                writtenBytes = conn.getWrittenBytes();
            }
            long playDuration = -1;
            if (stream instanceof PlaylistSubscriberStream) {
                // converted to seconds
                playDuration = (System.currentTimeMillis() - ((PlaylistSubscriberStream) stream).getCreationTime()) / 1000;
            }
            long playItemSize = -1;
            String playItemName = "";
            if (item != null) {
                playItemName = item.getName();
                //get file size in bytes if available
                IProviderService providerService = (IProviderService) scope.getContext().getBean(IProviderService.BEAN_NAME);
                if (providerService != null) {
                    File file = providerService.getVODProviderFile(scope, playItemName);
                    if (file != null) {
                        playItemSize = file.length();
                    } else {
                        log.debug("File was null, this is ok for live streams");
                    }
                } else {
                    log.debug("ProviderService was null");
                }
            }
            log.info("W3C x-category:stream x-event:stop c-ip:{} cs-bytes:{} sc-bytes:{} x-sname:{} x-file-length:{} x-file-size:{} x-name:{}", new Object[] { remoteAddress, readBytes, writtenBytes, stream.getName(), playDuration, playItemSize, playItemName });
        }
    }

    public void streamPlayItemPause(ISubscriberStream stream, IPlayItem item, int position) {
        // log w3c connect event
        log.info("W3C x-category:stream x-event:pause c-ip:{} x-sname:{}", Red5.getConnectionLocal().getRemoteAddress(), stream.getName());
    }

    public void streamPlayItemResume(ISubscriberStream stream, IPlayItem item, int position) {
        // log w3c connect event
        log.info("W3C x-category:stream x-event:unpause c-ip:{} x-sname:{}", Red5.getConnectionLocal().getRemoteAddress(), stream.getName());
    }

    public void streamPlayItemSeek(ISubscriberStream stream, IPlayItem item, int position) {
        // Override if necessary.
    }

    public void streamPublishStart(IBroadcastStream stream) {
        // log w3c connect event
        IConnection connection = Red5.getConnectionLocal();
        log.info("W3C x-category:stream x-event:publish c-ip:{} x-sname:{} x-name:{}", new Object[] { connection != null ? connection.getRemoteAddress() : "0.0.0.0", stream.getName(), stream.getPublishedName() });
    }

    public void streamRecordStart(IBroadcastStream stream) {
        // log w3c connect event
        IConnection connection = Red5.getConnectionLocal();
        log.info("W3C x-category:stream x-event:record-start c-ip:{} x-sname:{} x-file-name:{}", new Object[] { connection != null ? connection.getRemoteAddress() : "0.0.0.0", stream.getName(), stream.getSaveFilename() });
    }

    public void streamRecordStop(IBroadcastStream stream) {
        // log w3c connect event
        IConnection connection = Red5.getConnectionLocal();
        log.info("W3C x-category:stream x-event:record-stop c-ip:{} x-sname:{} x-file-name:{}", new Object[] { connection != null ? connection.getRemoteAddress() : "0.0.0.0", stream.getName(), stream.getSaveFilename() });
    }

    public void streamSubscriberClose(ISubscriberStream stream) {
        // log w3c connect event
        IConnection conn = Red5.getConnectionLocal();
        log.info("W3C x-category:stream x-event:stop c-ip:{} cs-bytes:{} sc-bytes:{} x-sname:{}", new Object[] { conn.getRemoteAddress(), conn.getReadBytes(), conn.getWrittenBytes(), stream.getName() });
    }

    public void streamSubscriberStart(ISubscriberStream stream) {
        // log w3c connect event
        log.info("W3C x-category:stream x-event:play c-ip:{} x-sname:{}", Red5.getConnectionLocal().getRemoteAddress(), stream.getName());
    }

    @Override
    public boolean handleEvent(IEvent event) {
        log.debug("handleEvent: {}", event);
        return super.handleEvent(event);
    }

}
