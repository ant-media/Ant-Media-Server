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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanMap;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.Output;
import org.red5.io.object.Serializer;
import org.red5.io.utils.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Service that works with status objects. Note all status object should aim to be under 128 bytes.
 *
 * @author The Red5 Project
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class StatusObjectService implements StatusCodes, InitializingBean {

    /**
     * Logger
     */
    protected static Logger log = LoggerFactory.getLogger(StatusObjectService.class);

    /**
     * Status objects map
     */
    protected Map<String, StatusObject> statusObjects;

    /**
     * Cached status objects map
     */
    protected Map<String, byte[]> cachedStatusObjects;

    /**
     * Initialization
     */
    public void afterPropertiesSet() throws Exception {
        log.trace("Loading status objects");
        loadStatusObjects();
        log.trace("Caching status objects");
        cacheStatusObjects();
        log.debug("Status service ready");
    }

    /**
     * Creates all status objects and adds them to status objects map
     */
    public void loadStatusObjects() {
        statusObjects = new HashMap<>();

        statusObjects.put(NC_CALL_FAILED, new StatusObject(NC_CALL_FAILED, StatusObject.ERROR, ""));
        statusObjects.put(NC_CALL_BADVERSION, new StatusObject(NC_CALL_BADVERSION, StatusObject.ERROR, ""));

        statusObjects.put(NC_CONNECT_APPSHUTDOWN, new StatusObject(NC_CONNECT_APPSHUTDOWN, StatusObject.ERROR, ""));
        statusObjects.put(NC_CONNECT_CLOSED, new StatusObject(NC_CONNECT_CLOSED, StatusObject.STATUS, ""));
        statusObjects.put(NC_CONNECT_FAILED, new StatusObject(NC_CONNECT_FAILED, StatusObject.ERROR, ""));
        statusObjects.put(NC_CONNECT_REJECTED, new StatusObject(NC_CONNECT_REJECTED, StatusObject.ERROR, ""));
        statusObjects.put(NC_CONNECT_SUCCESS, new StatusObject(NC_CONNECT_SUCCESS, StatusObject.STATUS, "Connection succeeded."));
        statusObjects.put(NC_CONNECT_INVALID_APPLICATION, new StatusObject(NC_CONNECT_INVALID_APPLICATION, StatusObject.ERROR, ""));

        statusObjects.put(NS_INVALID_ARGUMENT, new StatusObject(NS_INVALID_ARGUMENT, StatusObject.ERROR, ""));
        statusObjects.put(NS_CLEAR_SUCCESS, new StatusObject(NS_CLEAR_SUCCESS, StatusObject.STATUS, ""));
        statusObjects.put(NS_CLEAR_FAILED, new StatusObject(NS_CLEAR_FAILED, StatusObject.ERROR, ""));

        statusObjects.put(NS_PUBLISH_START, new StatusObject(NS_PUBLISH_START, StatusObject.STATUS, ""));
        statusObjects.put(NS_PUBLISH_BADNAME, new StatusObject(NS_PUBLISH_BADNAME, StatusObject.ERROR, ""));
        statusObjects.put(NS_FAILED, new StatusObject(NS_FAILED, StatusObject.ERROR, ""));
        statusObjects.put(NS_UNPUBLISHED_SUCCESS, new StatusObject(NS_UNPUBLISHED_SUCCESS, StatusObject.STATUS, ""));

        statusObjects.put(NS_RECORD_START, new StatusObject(NS_RECORD_START, StatusObject.STATUS, ""));
        statusObjects.put(NS_RECORD_NOACCESS, new StatusObject(NS_RECORD_NOACCESS, StatusObject.ERROR, ""));
        statusObjects.put(NS_RECORD_STOP, new StatusObject(NS_RECORD_STOP, StatusObject.STATUS, ""));
        statusObjects.put(NS_RECORD_FAILED, new StatusObject(NS_RECORD_FAILED, StatusObject.ERROR, ""));

        statusObjects.put(NS_PLAY_INSUFFICIENT_BW, new RuntimeStatusObject(NS_PLAY_INSUFFICIENT_BW, StatusObject.WARNING, ""));
        statusObjects.put(NS_PLAY_START, new RuntimeStatusObject(NS_PLAY_START, StatusObject.STATUS, ""));
        statusObjects.put(NS_PLAY_STREAMNOTFOUND, new RuntimeStatusObject(NS_PLAY_STREAMNOTFOUND, StatusObject.ERROR, ""));
        statusObjects.put(NS_PLAY_STOP, new RuntimeStatusObject(NS_PLAY_STOP, StatusObject.STATUS, ""));
        statusObjects.put(NS_PLAY_FAILED, new RuntimeStatusObject(NS_PLAY_FAILED, StatusObject.ERROR, ""));
        statusObjects.put(NS_PLAY_RESET, new RuntimeStatusObject(NS_PLAY_RESET, StatusObject.STATUS, ""));
        statusObjects.put(NS_PLAY_PUBLISHNOTIFY, new RuntimeStatusObject(NS_PLAY_PUBLISHNOTIFY, StatusObject.STATUS, ""));
        statusObjects.put(NS_PLAY_UNPUBLISHNOTIFY, new RuntimeStatusObject(NS_PLAY_UNPUBLISHNOTIFY, StatusObject.STATUS, ""));

        statusObjects.put(NS_DATA_START, new StatusObject(NS_DATA_START, StatusObject.STATUS, ""));

        statusObjects.put(APP_SCRIPT_ERROR, new StatusObject(APP_SCRIPT_ERROR, StatusObject.STATUS, ""));
        statusObjects.put(APP_SCRIPT_WARNING, new StatusObject(APP_SCRIPT_WARNING, StatusObject.STATUS, ""));
        statusObjects.put(APP_RESOURCE_LOWMEMORY, new StatusObject(APP_RESOURCE_LOWMEMORY, StatusObject.STATUS, ""));
        statusObjects.put(APP_SHUTDOWN, new StatusObject(APP_SHUTDOWN, StatusObject.STATUS, ""));
        statusObjects.put(APP_GC, new StatusObject(APP_GC, StatusObject.STATUS, ""));

        statusObjects.put(NS_PLAY_FILE_STRUCTURE_INVALID, new StatusObject(NS_PLAY_FILE_STRUCTURE_INVALID, StatusObject.ERROR, ""));
        statusObjects.put(NS_PLAY_NO_SUPPORTED_TRACK_FOUND, new StatusObject(NS_PLAY_NO_SUPPORTED_TRACK_FOUND, StatusObject.ERROR, ""));

    }

    /**
     * Cache status objects
     */
    public void cacheStatusObjects() {

        cachedStatusObjects = new HashMap<String, byte[]>();

        String statusCode;
        IoBuffer out = IoBuffer.allocate(256);
        out.setAutoExpand(true);

        for (String s : statusObjects.keySet()) {
            statusCode = s;
            StatusObject statusObject = statusObjects.get(statusCode);
            if (statusObject instanceof RuntimeStatusObject) {
                continue;
            }
            serializeStatusObject(out, statusObject);
            out.flip();
            if (log.isTraceEnabled()) {
                log.trace(HexDump.formatHexDump(out.getHexDump()));
            }
            byte[] cachedBytes = new byte[out.limit()];
            out.get(cachedBytes);
            out.clear();
            cachedStatusObjects.put(statusCode, cachedBytes);
        }
        out.free();
        out = null;
    }

    /**
     * Serializes status object
     * 
     * @param out
     *            Byte buffer for output object
     * @param statusObject
     *            Status object to serialize
     */
    public void serializeStatusObject(IoBuffer out, StatusObject statusObject) {
        Map<?, ?> statusMap = new BeanMap(statusObject);
        Output output = new Output(out);
        Serializer.serialize(output, statusMap);
    }

    /**
     * Return status object by code
     * 
     * @param statusCode
     *            Status object code
     * @return Status object with given code
     */
    public StatusObject getStatusObject(String statusCode) {
        return statusObjects.get(statusCode);
    }

    /**
     * Return status object by code as byte array
     * 
     * @param statusCode
     *            Status object code
     * @return Status object with given code as byte array
     */
    public byte[] getCachedStatusObjectAsByteArray(String statusCode) {
        return cachedStatusObjects.get(statusCode);
    }

}
