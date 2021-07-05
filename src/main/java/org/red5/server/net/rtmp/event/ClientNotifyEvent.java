package org.red5.server.net.rtmp.event;

import java.util.Arrays;

/**
 * Represents an notify to be executed on a connected client.
 * 
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class ClientNotifyEvent extends BaseEvent {

    private final String method;

    private final Object[] params;

    public ClientNotifyEvent(String method, Object[] params) {
        super(Type.CLIENT_NOTIFY);
        this.method = method;
        this.params = params;
    }

    public final static ClientNotifyEvent build(String method, Object[] params) {
        ClientNotifyEvent event = new ClientNotifyEvent(method, params);
        return event;
    }

    @Override
    public byte getDataType() {
        return TYPE_NOTIFY;
    }

    @Override
    protected void releaseInternal() {
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /**
     * @return the params
     */
    public Object[] getParams() {
        return params;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "ClientNotifyEvent [method=" + method + ", params=" + Arrays.toString(params) + "]";
    }

}
