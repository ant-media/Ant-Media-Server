/*
 * RED5 Open Source Media Server - https://github.com/Red5/ Copyright 2006-2016 by respective authors (see below). All rights reserved. Licensed under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.red5.io.object;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents all the actions which may be permitted on a stream. Some actions are called by client implementations other than a Flash Player itself; ex "getStreamLength".
 * If an action is not specified here, the "CUSTOM" enum will be returned.
 * 
 * @author Paul Gregoire
 */
public enum StreamAction {

    CONNECT("connect"), DISCONNECT("disconnect"), CREATE_STREAM("createStream"), DELETE_STREAM("deleteStream"), CLOSE_STREAM("closeStream"), INIT_STREAM("initStream"), RELEASE_STREAM("releaseStream"), PUBLISH("publish"), PAUSE("pause"), PAUSE_RAW("pauseRaw"), SEEK("seek"), PLAY("play"), PLAY2("play2"), STOP("stop"), RECEIVE_VIDEO("receiveVideo"), RECEIVE_AUDIO("receiveAudio"), GET_STREAM_LENGTH(
            "getStreamLength"), CUSTOM("");

    // presize to fit all enums in
    private final static Map<String, StreamAction> map = new HashMap<>(StreamAction.values().length);

    // the stream action this enum is for
    private final String actionString;

    StreamAction(String actionString) {
        this.actionString = actionString;
    }

    public String getActionString() {
        return actionString;
    }

    public static StreamAction getEnum(String actionString) {
        // fill the map if its empty
        if (map.isEmpty()) {
            // do this only once
            for (StreamAction action : values()) {
                map.put(action.getActionString(), action);
            }
        }
        // look up the action from the predefined set
        StreamAction match = map.get(actionString);
        if (match != null) {
            return match;
        }
        // return an action representing a custom type
        return CUSTOM;
    }

    public boolean equals(StreamAction action) {
        return action.getActionString().equals(actionString);
    }

    public boolean equals(String actionString) {
        return getActionString().equals(actionString);
    }

    @Override
    public String toString() {
        return actionString;
    }

}
