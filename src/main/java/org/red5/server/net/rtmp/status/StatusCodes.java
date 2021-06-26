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

package org.red5.server.net.rtmp.status;

/**
 * Collection of commonly used constants with status codes. Descriptions provided as in FMS 2.0.1 documentation available at adobe.com with some minor additions and comments.
 */
public interface StatusCodes {
    /**
     * The NetConnection.call method was not able to invoke the server-side method or command.
     */
    public static final String NC_CALL_FAILED = "NetConnection.Call.Failed";

    /**
     * The URI specified in the NetConnection.connect method did not specify 'rtmp' as the protocol. 'rtmp' must be specified when connecting to FMS and Red5. Either not supported version of AMF was used (3 when only 0 is supported)
     */
    public static final String NC_CALL_BADVERSION = "NetConnection.Call.BadVersion";

    /**
     * The application has been shut down (for example, if the application is out of memory resources and must shut down to prevent the server from crashing) or the server has shut down.
     */
    public static final String NC_CONNECT_APPSHUTDOWN = "NetConnection.Connect.AppShutdown";

    /**
     * The connection was closed successfully
     */
    public static final String NC_CONNECT_CLOSED = "NetConnection.Connect.Closed";

    /**
     * The connection attempt failed.
     */
    public static final String NC_CONNECT_FAILED = "NetConnection.Connect.Failed";

    /**
     * The client does not have permission to connect to the application, the application expected different parameters from those that were passed, or the application name specified during the connection attempt was not found on the server.
     */
    public static final String NC_CONNECT_REJECTED = "NetConnection.Connect.Rejected";

    /**
     * The connection attempt succeeded.
     */
    public static final String NC_CONNECT_SUCCESS = "NetConnection.Connect.Success";

    /**
     * The application name specified during connect is invalid.
     */
    public static final String NC_CONNECT_INVALID_APPLICATION = "NetConnection.Connect.InvalidApp";

    /**
     * Invalid arguments were passed to a NetStream method.
     */
    public static final String NS_INVALID_ARGUMENT = "NetStream.InvalidArg";

    /**
     * A recorded stream was deleted successfully.
     */
    public static final String NS_CLEAR_SUCCESS = "NetStream.Clear.Success";

    /**
     * A recorded stream failed to delete.
     */
    public static final String NS_CLEAR_FAILED = "NetStream.Clear.Failed";

    /**
     * An attempt to publish was successful.
     */
    public static final String NS_PUBLISH_START = "NetStream.Publish.Start";

    /**
     * An attempt was made to publish a stream that is already being published by someone else.
     */
    public static final String NS_PUBLISH_BADNAME = "NetStream.Publish.BadName";

    /**
     * An attempt to use a Stream method (at client-side) failed
     */
    public static final String NS_FAILED = "NetStream.Failed";

    /**
     * An attempt to unpublish was successful
     */
    public static final String NS_UNPUBLISHED_SUCCESS = "NetStream.Unpublish.Success";

    /**
     * Recording was started
     */
    public static final String NS_RECORD_START = "NetStream.Record.Start";

    /**
     * An attempt was made to record a read-only stream
     */
    public static final String NS_RECORD_NOACCESS = "NetStream.Record.NoAccess";

    /**
     * Recording was stopped
     */
    public static final String NS_RECORD_STOP = "NetStream.Record.Stop";

    /**
     * An attempt to record a stream failed
     */
    public static final String NS_RECORD_FAILED = "NetStream.Record.Failed";

    /**
     * The buffer is empty (sent from server to client)
     */
    public static final String NS_BUFFER_EMPTY = "NetStream.Buffer.Empty";

    /**
     * Data is playing behind the normal speed
     */
    public static final String NS_PLAY_INSUFFICIENT_BW = "NetStream.Play.InsufficientBW";

    /**
     * Play was started
     */
    public static final String NS_PLAY_START = "NetStream.Play.Start";

    /**
     * An attempt was made to play a stream that does not exist
     */
    public static final String NS_PLAY_STREAMNOTFOUND = "NetStream.Play.StreamNotFound";

    /**
     * Play was stopped
     */
    public static final String NS_PLAY_STOP = "NetStream.Play.Stop";

    /**
     * An attempt to play back a stream failed
     */
    public static final String NS_PLAY_FAILED = "NetStream.Play.Failed";

    /**
     * A playlist was reset
     */
    public static final String NS_PLAY_RESET = "NetStream.Play.Reset";

    /**
     * The initial publish to a stream was successful. This message is sent to all subscribers
     */
    public static final String NS_PLAY_PUBLISHNOTIFY = "NetStream.Play.PublishNotify";

    /**
     * An unpublish from a stream was successful. This message is sent to all subscribers
     */
    public static final String NS_PLAY_UNPUBLISHNOTIFY = "NetStream.Play.UnpublishNotify";

    /**
     * Playlist playback switched from one stream to another.
     */
    public static final String NS_PLAY_SWITCH = "NetStream.Play.Switch";

    /**
     * Transition to another stream has been initiated.
     */
    public static final String NS_PLAY_TRANSITION = "NetStream.Play.Transition";

    /**
     * Transition to another stream is processing normally. This is set as the "reason" property of the NetStatusEvent.
     */
    public static final String NS_TRANSITION_SUCCESS = "NetStream.Transition.Success";

    /**
     * Transition to another stream has been forced.
     * 
     * If streams that are being switched do not have aligned content/timelines, or if the keyframes are not aligned between the two streams, it is possible that the server will have to force a hard transition. This can also happen with broadcast (live) dynamic streaming if the server cannot find a synchronization point within the two streams. This is set as the "reason" property of the NetStatusEvent.
     */
    public static final String NS_TRANSITION_FORCED = "NetStream.Transition.Forced";

    /**
     * Transition to another stream is complete.
     */
    public static final String NS_PLAY_TRANSITION_COMPLETE = "NetStream.Play.TransitionComplete";

    /**
     * Playlist playback is complete.
     */
    public static final String NS_PLAY_COMPLETE = "NetStream.Play.Complete";

    /**
     * The subscriber has used the seek command to move to a particular location in the recorded stream.
     */
    public static final String NS_SEEK_NOTIFY = "NetStream.Seek.Notify";

    /**
     * The stream doesn't support seeking.
     */
    public static final String NS_SEEK_FAILED = "NetStream.Seek.Failed";

    /**
     * The subscriber has used the seek command to move to a particular location in the recorded stream.
     */
    public static final String NS_PAUSE_NOTIFY = "NetStream.Pause.Notify";

    /**
     * Publishing has stopped
     */
    public static final String NS_UNPAUSE_NOTIFY = "NetStream.Unpause.Notify";

    /**
	 *
	 */
    public static final String NS_DATA_START = "NetStream.Data.Start";

    /**
     * The ActionScript engine has encountered a runtime error. In addition to the standard infoObject properties, the following properties are set:
     *
     * filename: name of the offending ASC file. lineno: line number where the error occurred. linebuf: source code of the offending line.
     */
    public static final String APP_SCRIPT_ERROR = "Application.Script.Error";

    /**
     * The ActionScript engine has encountered a runtime warning. In addition to the standard infoObject properties, the following properties are set:
     * 
     * filename: name of the offending ASC file. lineno: line number where the error occurred. linebuf: source code of the offending line
     */
    public static final String APP_SCRIPT_WARNING = "Application.Script.Warning";

    /**
     * The ActionScript engine is low on runtime memory. This provides an opportunity for the application instance to free some resources or take suitable action. If the application instance runs out of memory, it is unloaded and all users are disconnected. In this state, the server will not invoke the Application.onDisconnect event handler or the Application.onAppStop event handler
     */
    public static final String APP_RESOURCE_LOWMEMORY = "Application.Resource.LowMemory";

    /**
     * This information object is passed to the onAppStop handler when the application is being shut down
     */
    public static final String APP_SHUTDOWN = "Application.Shutdown";

    /**
     * This information object is passed to the onAppStop event handler when the application instance is about to be destroyed by the server.
     */
    public static final String APP_GC = "Application.GC";

    /**
     * Read access to a shared object was denied.
     */
    public static final String SO_NO_READ_ACCESS = "SharedObject.NoReadAccess";

    /**
     * Write access to a shared object was denied.
     */
    public static final String SO_NO_WRITE_ACCESS = "SharedObject.NoWriteAccess";

    /**
     * The creation of a shared object was denied.
     */
    public static final String SO_CREATION_FAILED = "SharedObject.ObjectCreationFailed";

    /**
     * The persistence parameter passed to SharedObject.getRemote() is different from the one used when the shared object was created.
     */
    public static final String SO_PERSISTENCE_MISMATCH = "SharedObject.BadPersistence";

    /**
     * This event is sent if the player detects an MP4 with an invalid file structure. Flash Player cannot play files that have invalid file structures.
     */
    public static final String NS_PLAY_FILE_STRUCTURE_INVALID = "NetStream.Play.FileStructureInvalid";

    /**
     * This event is sent if the player does not detect any supported tracks. If there aren't any supported video, audio or data tracks found, Flash Player does not play the file.
     */
    public static final String NS_PLAY_NO_SUPPORTED_TRACK_FOUND = "NetStream.Play.NoSupportedTrackFound";

}
