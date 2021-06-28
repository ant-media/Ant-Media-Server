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

package org.red5.server.api.stream;

import java.io.IOException;
import java.util.Collection;

import org.red5.server.messaging.IProvider;
import org.red5.server.net.rtmp.event.Notify;

/**
 * A broadcast stream is a stream source to be subscribed to by clients. To subscribe to a stream from your client Flash application use NetStream.play method. Broadcast stream can be saved at the server-side.
 * 
 * 
 * @author The Red5 Project
 * @author Luke Hubbard (luke@codegent.com)
 * @author Steven Gong (steven.gong@gmail.com)
 */
public interface IBroadcastStream extends IStream {

    /**
     * Save the broadcast stream as a file.
     * 
     * @param filePath
     *            The path of the file relative to the scope.
     * @param isAppend
     *            Whether to append to the end of file.
     * @throws IOException
     *             File could not be created/written to.
     * @throws ResourceExistException
     *             Resource exist when trying to create.
     * @throws ResourceNotFoundException
     *             Resource not exist when trying to append.
     */
    void saveAs(String filePath, boolean isAppend) throws IOException, ResourceNotFoundException, ResourceExistException;

    /**
     * Get the filename the stream is being saved as.
     * 
     * @return The filename relative to the scope or
     * 
     *         <pre>
     * null
     * </pre>
     * 
     *         if the stream is not being saved.
     */
    String getSaveFilename();

    /**
     * Get the provider corresponding to this stream. Provider objects are object that
     * 
     * @return the provider
     */
    IProvider getProvider();

    /**
     * Get stream publish name. Publish name is the value of the first parameter had been passed to
     * 
     * <pre>
     * NetStream.publish
     * </pre>
     * 
     * on client side in SWF.
     * 
     * @return Stream publish name
     */
    String getPublishedName();

    /**
     * 
     * @param name
     *            Set stream publish name
     */
    void setPublishedName(String name);

    /**
     * Add a listener to be notified about received packets.
     * 
     * @param listener
     *            the listener to add
     */
    public void addStreamListener(IStreamListener listener);

    /**
     * Remove a listener from being notified about received packets.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeStreamListener(IStreamListener listener);

    /**
     * Return registered stream listeners.
     * 
     * @return the registered listeners
     */
    public Collection<IStreamListener> getStreamListeners();

    /**
     * Returns the metadata for the associated stream, if it exists.
     * 
     * @return stream meta data
     */
    public Notify getMetaData();

}