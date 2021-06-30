package org.red5.server.net.rtmp.event;

import org.red5.io.object.StreamAction;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.event.IEventListener;

/**
 * Represents a stream action occurring on a connection or stream. This event is used to notify an IEventHandler; it is not meant to be sent over the wire to clients.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class StreamActionEvent implements IEvent {

    private final StreamAction action;

    public StreamActionEvent(StreamAction action) {
        this.action = action;
    }

    public Type getType() {
        return Type.STREAM_ACTION;
    }

    public Object getObject() {
        return action;
    }

    public boolean hasSource() {
        return false;
    }

    public IEventListener getSource() {
        return null;
    }

    @Override
    public String toString() {
        return "StreamActionEvent [action=" + action + "]";
    }

}
